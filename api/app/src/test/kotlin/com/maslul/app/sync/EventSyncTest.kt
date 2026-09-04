package com.maslul.app.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.maslul.app.testsupport.AppRoleInitializer
import com.maslul.identity.tenant.RequestScopedTenantContext
import com.maslul.sync.entity.DeviceEventStatus
import com.maslul.sync.repository.DeviceEventRepository
import com.maslul.sync.repository.SyncCursorRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.zip.GZIPOutputStream
import javax.crypto.SecretKey

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventSyncTest {

    companion object {
        private const val JWT_SECRET = "test-only-hs256-signing-key-not-used-anywhere-real-32bytes+"
        private val jwtKey: SecretKey = Keys.hmacShaKeyFor(JWT_SECRET.toByteArray(StandardCharsets.UTF_8))

        private val postgisImage = DockerImageName.parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres")

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
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    lateinit var deviceEventRepository: DeviceEventRepository

    @Autowired
    lateinit var syncCursorRepository: SyncCursorRepository

    private val objectMapper = ObjectMapper()

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

    // device is RLS-protected (V4) - a plain jdbcTemplate call has no transaction of its own
    // to carry the app.tenant_id session GUC, so this insert needs an explicit transaction
    // through the tenant-aware manager, same as RouteInstanceMaterializationTest.insertDriver.
    private fun insertDevice(tenantId: UUID): UUID {
        val id = UUID.randomUUID()
        withTenant(tenantId) {
            TransactionTemplate(transactionManager).executeWithoutResult {
                jdbcTemplate.update(
                    "INSERT INTO device (id, tenant_id, device_code, status) VALUES (?, ?, ?, 'ACTIVE')",
                    id, tenantId, "device-$id",
                )
            }
        }
        return id
    }

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

    private fun eventMap(
        seq: Long,
        eventId: UUID = UUID.randomUUID(),
        type: String = "SHIFT_START",
        payload: Map<String, Any?> = mapOf("seq" to seq),
    ): Map<String, Any?> = mapOf(
        "eventId" to eventId.toString(),
        "deviceSeq" to seq,
        "deviceTime" to Instant.now().toString(),
        "monotonicMs" to seq * 1000,
        "type" to type,
        "payload" to payload,
    )

    private fun idOf(event: Map<String, Any?>): UUID = UUID.fromString(event["eventId"] as String)

    private fun postEvents(tenantId: UUID, deviceId: UUID, events: List<Map<String, Any?>>): Map<*, *> {
        val body = mapOf("deviceId" to deviceId.toString(), "events" to events)
        val response = restTemplate.postForEntity(
            "/v1/sync/events", HttpEntity(body, headersFor(tenantId)), Map::class.java,
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        return response.body!!
    }

    private fun watermarkOf(response: Map<*, *>): Long = (response["watermark"] as Number).toLong()

    private fun postBreadcrumbsGzip(tenantId: UUID, deviceId: UUID, points: List<Map<String, Any?>>): ResponseEntity<Void> {
        val json = objectMapper.writeValueAsBytes(mapOf("deviceId" to deviceId.toString(), "points" to points))
        val gzipped = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(json) }
        }.toByteArray()

        val headers = headersFor(tenantId)
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Content-Encoding", "gzip")

        return restTemplate.exchange(
            "/v1/sync/breadcrumbs", HttpMethod.POST, HttpEntity(gzipped, headers), Void::class.java,
        )
    }

    @Test
    fun `replaying a batch after a partial persist converges to the same state as one clean run`() {
        val tenantId = insertTenant("Tenant A")
        val deviceId = insertDevice(tenantId)

        // Stable event_ids generated once - a real device resends the exact same batch it
        // couldn't confirm, it doesn't regenerate event_ids on retry.
        val allEvents = (1L..200L).map { eventMap(seq = it) }

        // Simulates the device losing the connection after only the first 100 landed: the
        // server only ever saw the first half of the batch on this attempt.
        val partialResponse = postEvents(tenantId, deviceId, allEvents.subList(0, 100))
        assertEquals(100L, watermarkOf(partialResponse))

        // The device doesn't know 100 landed, so it resends the whole 200-event batch.
        val fullResponse = postEvents(tenantId, deviceId, allEvents)
        assertEquals(200L, watermarkOf(fullResponse))

        val serverReceivedAtAfterFirstFullSend = withTenantResult(tenantId) {
            deviceEventRepository.findByTenantIdAndEventId(tenantId, idOf(allEvents[0]))!!.serverReceivedAt
        }

        // CLAUDE.md: every sync endpoint gets an idempotency test sending the same batch
        // three times and asserting identical state - this is the third send.
        val thirdResponse = postEvents(tenantId, deviceId, allEvents)
        assertEquals(200L, watermarkOf(thirdResponse))

        withTenant(tenantId) {
            val rows = deviceEventRepository.findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantId, deviceId)
            assertEquals(200, rows.size)
            assertTrue(rows.all { it.status == DeviceEventStatus.ACCEPTED })

            // Replaying never re-stamps server_received_at - proof the second and third
            // sends were true no-ops, not silent overwrites.
            val firstRow = rows.first { it.eventId == idOf(allEvents[0]) }
            assertEquals(serverReceivedAtAfterFirstFullSend, firstRow.serverReceivedAt)

            val cursor = syncCursorRepository.findByTenantIdAndDeviceId(tenantId, deviceId)!!
            assertEquals(200L, cursor.lastDeviceSeq)
        }
    }

    @Test
    fun `events ahead of a gap are quarantined and cascade-promote in order once the gap fills`() {
        val tenantId = insertTenant("Tenant A")
        val deviceId = insertDevice(tenantId)

        val seq1 = eventMap(seq = 1)
        val seq2 = eventMap(seq = 2)
        val seq4 = eventMap(seq = 4)
        val seq5 = eventMap(seq = 5)

        val firstBatch = postEvents(tenantId, deviceId, listOf(seq1, seq2, seq4, seq5))
        assertEquals(2L, watermarkOf(firstBatch))

        withTenant(tenantId) {
            val row4 = deviceEventRepository.findByTenantIdAndEventId(tenantId, idOf(seq4))!!
            assertEquals(DeviceEventStatus.QUARANTINED, row4.status)
            assertNotNull(row4.quarantinedAt)

            val row5 = deviceEventRepository.findByTenantIdAndEventId(tenantId, idOf(seq5))!!
            assertEquals(DeviceEventStatus.QUARANTINED, row5.status)
            assertNotNull(row5.quarantinedAt)
        }

        val seq3 = eventMap(seq = 3)
        // Real device behavior: it resends the gap-filler together with the still-unacked
        // tail (4 and 5), not seq 3 in isolation - the idempotency check must treat the
        // already-quarantined 4 and 5 as no-ops here, not as fresh inserts.
        val secondBatch = postEvents(tenantId, deviceId, listOf(seq3, seq4, seq5))
        assertEquals(5L, watermarkOf(secondBatch))

        withTenant(tenantId) {
            val rows = deviceEventRepository.findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantId, deviceId)
            assertEquals(5, rows.size)
            assertTrue(rows.all { it.status == DeviceEventStatus.ACCEPTED })

            assertNull(rows[2].quarantinedAt) // seq 3 was never out of order
            assertNotNull(rows[3].quarantinedAt) // seq 4 - promoted, trail preserved
            assertNotNull(rows[4].quarantinedAt) // seq 5 - promoted, trail preserved

            val cursor = syncCursorRepository.findByTenantIdAndDeviceId(tenantId, deviceId)!!
            assertEquals(5L, cursor.lastDeviceSeq)
        }
    }

    @Test
    fun `a resent event_id is a no-op even if the retry claims a different device_seq or payload`() {
        val tenantId = insertTenant("Tenant A")
        val deviceId = insertDevice(tenantId)

        val eventId = UUID.randomUUID()
        val original = eventMap(seq = 1, eventId = eventId, payload = mapOf("a" to 1))
        val firstResponse = postEvents(tenantId, deviceId, listOf(original))
        assertEquals(1L, watermarkOf(firstResponse))

        // Same event_id, a wildly different device_seq and payload - e.g. a device that
        // reinstalled and reused an event_id/payload combination it already sent. event_id
        // is the idempotency key (spec §5), so this must be a no-op, not an exception and
        // not a second row.
        val resent = eventMap(seq = 50, eventId = eventId, payload = mapOf("a" to 999))
        val secondResponse = postEvents(tenantId, deviceId, listOf(resent))
        assertEquals(1L, watermarkOf(secondResponse)) // no gap opened at seq 50

        withTenant(tenantId) {
            val rows = deviceEventRepository.findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantId, deviceId)
            assertEquals(1, rows.size)
            assertEquals(1L, rows[0].deviceSeq)
            assertEquals(1, objectMapper.readTree(rows[0].payload)["a"].asInt()) // first write wins
        }
    }

    @Test
    fun `sync tables enforce tenant isolation`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")
        val deviceA = insertDevice(tenantA)
        val deviceB = insertDevice(tenantB)

        postEvents(tenantA, deviceA, listOf(eventMap(seq = 1)))
        postEvents(tenantB, deviceB, listOf(eventMap(seq = 1)))

        withTenant(tenantA) {
            assertEquals(1, deviceEventRepository.findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantA, deviceA).size)
            assertEquals(0, deviceEventRepository.findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantA, deviceB).size)
            assertNotNull(syncCursorRepository.findByTenantIdAndDeviceId(tenantA, deviceA))
            assertNull(syncCursorRepository.findByTenantIdAndDeviceId(tenantA, deviceB))
        }
        withTenant(tenantB) {
            assertEquals(1, deviceEventRepository.findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantB, deviceB).size)
            assertNull(syncCursorRepository.findByTenantIdAndDeviceId(tenantB, deviceA))
        }
    }

    @Test
    fun `a gzip-compressed breadcrumb batch is accepted`() {
        val tenantId = insertTenant("Tenant A")
        val deviceId = insertDevice(tenantId)

        val response = postBreadcrumbsGzip(
            tenantId, deviceId,
            listOf(
                mapOf(
                    "lat" to 32.08, "lng" to 34.78, "t" to Instant.now().toString(),
                    "speed" to 8.3, "heading" to 90.0, "accuracy" to 5.0,
                ),
            ),
        )
        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
    }

    private fun <T> withTenantResult(tenantId: UUID, block: () -> T): T {
        var result: T? = null
        withTenant(tenantId) { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
