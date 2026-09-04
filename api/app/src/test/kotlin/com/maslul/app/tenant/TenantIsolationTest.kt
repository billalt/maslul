package com.maslul.app.tenant

import com.maslul.app.testsupport.AppRoleInitializer
import com.maslul.assets.entity.Zone
import com.maslul.assets.repository.ZoneRepository
import com.maslul.identity.tenant.RequestScopedTenantContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
import java.util.UUID

@Testcontainers
@SpringBootTest
class TenantIsolationTest {

    companion object {
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
        }
    }

    @Autowired
    lateinit var zoneRepository: ZoneRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

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
}
