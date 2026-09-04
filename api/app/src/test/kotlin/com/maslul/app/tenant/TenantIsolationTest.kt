package com.maslul.app.tenant

import com.maslul.app.testsupport.AppRoleInitializer
import com.maslul.assets.entity.Zone
import com.maslul.assets.repository.ZoneRepository
import com.maslul.identity.tenant.RequestScopedTenantContext
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.SecretKey

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantIsolationTest {

    companion object {
        private const val JWT_SECRET = "test-only-hs256-signing-key-not-used-anywhere-real-32bytes+"
        private val jwtKey: SecretKey = Keys.hmacShaKeyFor(JWT_SECRET.toByteArray(StandardCharsets.UTF_8))

        private val postgisImage = DockerImageName.parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres")

        // Bootstraps as the "postgres" superuser, then 01-create-app-role.sql creates the
        // non-superuser "maslul" role the app actually connects as - see that script for why.
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(postgisImage)
            .withDatabaseName("maslul")
            .withUsername("postgres")
            .withPassword("postgres")

        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            AppRoleInitializer.ensureAppRole(postgres.jdbcUrl)
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { "maslul" }
            registry.add("spring.datasource.password") { "maslul" }
            registry.add("app.jwt.secret") { JWT_SECRET }
        }
    }

    @Autowired
    lateinit var zoneRepository: ZoneRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @AfterEach
    fun clearTenantContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    private fun withTenant(tenantId: UUID, block: () -> Unit) {
        val request = MockHttpServletRequest()
        request.setAttribute(RequestScopedTenantContext.TENANT_ID_ATTRIBUTE, tenantId)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        block()
    }

    private fun insertTenant(name: String): UUID {
        val id = UUID.randomUUID()
        jdbcTemplate.update("INSERT INTO tenant (id, name) VALUES (?, ?)", id, name)
        return id
    }

    // Same claim JwtTenantResolver reads in production; only the signing key differs.
    private fun tokenFor(tenantId: UUID): String =
        Jwts.builder()
            .claim("tenant_id", tenantId.toString())
            .signWith(jwtKey)
            .compact()

    private fun headersFor(tenantId: UUID): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBearerAuth(tokenFor(tenantId))
        return headers
    }

    private fun post(path: String, tenantId: UUID, body: Map<String, Any?>): Map<*, *> {
        val response = restTemplate.postForEntity(path, HttpEntity(body, headersFor(tenantId)), Map::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        return response.body!!
    }

    private fun list(path: String, tenantId: UUID): List<Map<*, *>> {
        val response = restTemplate.exchange(
            path,
            HttpMethod.GET,
            HttpEntity<Void>(headersFor(tenantId)),
            List::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        @Suppress("UNCHECKED_CAST")
        return response.body as List<Map<*, *>>
    }

    @Test
    fun `a tenant cannot see or create rows belonging to another tenant`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")

        withTenant(tenantA) {
            zoneRepository.save(Zone(tenantId = tenantA, name = "Zone A"))
        }
        withTenant(tenantB) {
            zoneRepository.save(Zone(tenantId = tenantB, name = "Zone B"))
        }

        withTenant(tenantA) {
            val zones = zoneRepository.findAll()
            assertEquals(1, zones.size)
            assertEquals("Zone A", zones[0].name)
        }
        withTenant(tenantB) {
            val zones = zoneRepository.findAll()
            assertEquals(1, zones.size)
            assertEquals("Zone B", zones[0].name)
        }
    }

    @Test
    fun `service point list via the API is isolated per tenant`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")

        post(
            "/v1/service-points", tenantA,
            mapOf("type" to "POINT", "geometry" to "POINT (34.78 32.08)", "address" to "Tenant A address"),
        )
        post(
            "/v1/service-points", tenantB,
            mapOf("type" to "POINT", "geometry" to "POINT (35.21 31.77)", "address" to "Tenant B address"),
        )

        val seenByA = list("/v1/service-points", tenantA)
        assertEquals(1, seenByA.size)
        assertEquals("Tenant A address", seenByA[0]["address"])

        val seenByB = list("/v1/service-points", tenantB)
        assertEquals(1, seenByB.size)
        assertEquals("Tenant B address", seenByB[0]["address"])
    }

    @Test
    fun `container list via the API is isolated per tenant`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")

        val servicePointA = post(
            "/v1/service-points", tenantA,
            mapOf("type" to "POINT", "geometry" to "POINT (34.78 32.08)", "address" to "Tenant A address"),
        )["id"] as String
        val servicePointB = post(
            "/v1/service-points", tenantB,
            mapOf("type" to "POINT", "geometry" to "POINT (35.21 31.77)", "address" to "Tenant B address"),
        )["id"] as String

        post(
            "/v1/containers", tenantA,
            mapOf(
                "servicePointId" to servicePointA,
                "wasteStream" to "GENERAL",
                "volumeLiters" to 1100,
                "containerType" to "COMMUNAL",
                "requiredBodyType" to "REAR_LOADER",
                "externalRef" to "Tenant A container",
            ),
        )
        post(
            "/v1/containers", tenantB,
            mapOf(
                "servicePointId" to servicePointB,
                "wasteStream" to "GENERAL",
                "volumeLiters" to 1100,
                "containerType" to "COMMUNAL",
                "requiredBodyType" to "REAR_LOADER",
                "externalRef" to "Tenant B container",
            ),
        )

        val seenByA = list("/v1/containers", tenantA)
        assertEquals(1, seenByA.size)
        assertEquals("Tenant A container", seenByA[0]["externalRef"])

        val seenByB = list("/v1/containers", tenantB)
        assertEquals(1, seenByB.size)
        assertEquals("Tenant B container", seenByB[0]["externalRef"])
    }

    @Test
    fun `vehicle list via the API is isolated per tenant`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")

        post(
            "/v1/vehicles", tenantA,
            mapOf(
                "plate" to "AA-000-01", "type" to "REAR_LOADER_26T",
                "capacityM3" to 16.0, "capacityKg" to 12000.0,
                "heightCm" to 350, "widthCm" to 250, "bodyType" to "REAR_LOADER",
            ),
        )
        post(
            "/v1/vehicles", tenantB,
            mapOf(
                "plate" to "BB-000-02", "type" to "REAR_LOADER_26T",
                "capacityM3" to 16.0, "capacityKg" to 12000.0,
                "heightCm" to 350, "widthCm" to 250, "bodyType" to "REAR_LOADER",
            ),
        )

        val seenByA = list("/v1/vehicles", tenantA)
        assertEquals(1, seenByA.size)
        assertEquals("AA-000-01", seenByA[0]["plate"])

        val seenByB = list("/v1/vehicles", tenantB)
        assertEquals(1, seenByB.size)
        assertEquals("BB-000-02", seenByB[0]["plate"])
    }

    @Test
    fun `site list via the API is isolated per tenant`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")

        post(
            "/v1/sites", tenantA,
            mapOf("name" to "Tenant A depot", "type" to "DEPOT", "location" to "POINT (34.78 32.08)"),
        )
        post(
            "/v1/sites", tenantB,
            mapOf("name" to "Tenant B depot", "type" to "DEPOT", "location" to "POINT (35.21 31.77)"),
        )

        val seenByA = list("/v1/sites", tenantA)
        assertEquals(1, seenByA.size)
        assertEquals("Tenant A depot", seenByA[0]["name"])

        val seenByB = list("/v1/sites", tenantB)
        assertEquals(1, seenByB.size)
        assertEquals("Tenant B depot", seenByB[0]["name"])
        assertTrue(seenByA[0]["name"] != seenByB[0]["name"])
    }
}
