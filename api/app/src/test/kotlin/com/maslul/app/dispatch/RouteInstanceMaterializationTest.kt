package com.maslul.app.dispatch

import com.maslul.assets.entity.BodyType
import com.maslul.assets.entity.Container as ContainerAsset
import com.maslul.assets.entity.ContainerType
import com.maslul.assets.entity.ServicePoint
import com.maslul.assets.entity.ServicePointStatus
import com.maslul.assets.entity.ServicePointType
import com.maslul.assets.entity.Vehicle
import com.maslul.assets.entity.WasteStream
import com.maslul.assets.repository.ContainerRepository
import com.maslul.assets.repository.ServicePointRepository
import com.maslul.assets.repository.VehicleRepository
import com.maslul.app.testsupport.AppRoleInitializer
import com.maslul.dispatch.entity.MaterializationIssue
import com.maslul.dispatch.entity.RouteInstanceStatus
import com.maslul.dispatch.entity.RouteTemplate
import com.maslul.dispatch.entity.RouteTemplateStop
import com.maslul.dispatch.entity.Shift
import com.maslul.dispatch.entity.ShiftStatus
import com.maslul.dispatch.entity.StopOutcome
import com.maslul.dispatch.materialization.RouteInstanceMaterializationService
import com.maslul.dispatch.repository.RouteInstanceRepository
import com.maslul.dispatch.repository.RouteTemplateRepository
import com.maslul.dispatch.repository.RouteTemplateStopRepository
import com.maslul.dispatch.repository.ShiftRepository
import com.maslul.dispatch.repository.StopRepository
import com.maslul.identity.tenant.RequestScopedTenantContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
import java.time.LocalDate
import java.util.UUID

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RouteInstanceMaterializationTest {

    companion object {
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
            registry.add("app.jwt.secret") { "test-only-hs256-signing-key-not-used-anywhere-real-32bytes+" }
        }
    }

    private val geometryFactory = GeometryFactory()

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    lateinit var servicePointRepository: ServicePointRepository

    @Autowired
    lateinit var containerRepository: ContainerRepository

    @Autowired
    lateinit var vehicleRepository: VehicleRepository

    @Autowired
    lateinit var routeTemplateRepository: RouteTemplateRepository

    @Autowired
    lateinit var routeTemplateStopRepository: RouteTemplateStopRepository

    @Autowired
    lateinit var shiftRepository: ShiftRepository

    @Autowired
    lateinit var routeInstanceRepository: RouteInstanceRepository

    @Autowired
    lateinit var stopRepository: StopRepository

    @Autowired
    lateinit var materializationService: RouteInstanceMaterializationService

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

    // app_user is RLS-protected (V4) - a plain jdbcTemplate call has no transaction of its own
    // to carry the app.tenant_id session GUC that TenantAwareJpaTransactionManager sets, so
    // this insert needs an explicit transaction through that same manager.
    private fun insertDriver(tenantId: UUID): UUID {
        val id = UUID.randomUUID()
        TransactionTemplate(transactionManager).executeWithoutResult {
            jdbcTemplate.update(
                "INSERT INTO app_user (id, tenant_id, email, display_name, role) VALUES (?, ?, ?, ?, 'DRIVER')",
                id, tenantId, "driver-$id@example.com", "Test Driver",
            )
        }
        return id
    }

    private fun point(lng: Double, lat: Double) = geometryFactory.createPoint(Coordinate(lng, lat))

    @Test
    fun `materialize copies template stop order and flags known problems, never skipping a stop`() {
        val tenantId = insertTenant("Tenant A")

        lateinit var goodPointId: UUID
        lateinit var wrongBodyTypePointId: UUID
        lateinit var inactivePointId: UUID
        lateinit var vehicleId: UUID
        lateinit var driverId: UUID
        lateinit var templateId: UUID
        lateinit var shiftId: UUID

        withTenant(tenantId) {
            vehicleId = vehicleRepository.save(
                Vehicle(
                    tenantId = tenantId, plate = "AA-000-01", type = "REAR_LOADER_26T",
                    capacityM3 = 16.0, capacityKg = 12000.0, heightCm = 350, widthCm = 250,
                    bodyType = BodyType.REAR_LOADER,
                ),
            ).id!!

            val goodPoint = servicePointRepository.save(
                ServicePoint(
                    tenantId = tenantId, type = ServicePointType.POINT, geometry = point(34.78, 32.08),
                    address = "Compatible stop", status = ServicePointStatus.ACTIVE,
                ),
            )
            goodPointId = goodPoint.id!!
            containerRepository.save(
                ContainerAsset(
                    tenantId = tenantId, servicePointId = goodPointId, wasteStream = WasteStream.GENERAL,
                    volumeLiters = 1100, containerType = ContainerType.COMMUNAL,
                    requiredBodyType = BodyType.REAR_LOADER,
                ),
            )

            val wrongBodyTypePoint = servicePointRepository.save(
                ServicePoint(
                    tenantId = tenantId, type = ServicePointType.POINT, geometry = point(34.79, 32.09),
                    address = "Needs a crane truck", status = ServicePointStatus.ACTIVE,
                ),
            )
            wrongBodyTypePointId = wrongBodyTypePoint.id!!
            containerRepository.save(
                ContainerAsset(
                    tenantId = tenantId, servicePointId = wrongBodyTypePointId, wasteStream = WasteStream.GENERAL,
                    volumeLiters = 5000, containerType = ContainerType.UNDERGROUND,
                    requiredBodyType = BodyType.CRANE,
                ),
            )

            val inactivePoint = servicePointRepository.save(
                ServicePoint(
                    tenantId = tenantId, type = ServicePointType.POINT, geometry = point(34.80, 32.10),
                    address = "Removed stop", status = ServicePointStatus.REMOVED,
                ),
            )
            inactivePointId = inactivePoint.id!!

            driverId = insertDriver(tenantId)

            val template = routeTemplateRepository.save(RouteTemplate(tenantId = tenantId, name = "Morning route"))
            templateId = template.id!!
            routeTemplateStopRepository.save(
                RouteTemplateStop(tenantId = tenantId, routeTemplateId = templateId, servicePointId = goodPointId, sequence = 1),
            )
            routeTemplateStopRepository.save(
                RouteTemplateStop(tenantId = tenantId, routeTemplateId = templateId, servicePointId = wrongBodyTypePointId, sequence = 2),
            )
            routeTemplateStopRepository.save(
                RouteTemplateStop(tenantId = tenantId, routeTemplateId = templateId, servicePointId = inactivePointId, sequence = 3),
            )

            shiftId = shiftRepository.save(
                Shift(
                    tenantId = tenantId, vehicleId = vehicleId, driverId = driverId,
                    shiftDate = LocalDate.of(2026, 9, 6), status = ShiftStatus.PLANNED,
                ),
            ).id!!
        }

        lateinit var routeInstanceId: UUID
        withTenant(tenantId) {
            val routeInstance = materializationService.materialize(tenantId, shiftId, templateId, LocalDate.of(2026, 9, 6))
            routeInstanceId = routeInstance.id!!
            assertEquals(1, routeInstance.version)
            assertEquals(RouteInstanceStatus.DRAFT, routeInstance.status)
            assertEquals(LocalDate.of(2026, 9, 6), routeInstance.serviceDate)
        }

        withTenant(tenantId) {
            val stops = stopRepository.findAllByTenantIdAndRouteInstanceIdOrderBySequenceAsc(tenantId, routeInstanceId)

            assertEquals(3, stops.size)
            stops.forEach { assertEquals(StopOutcome.PENDING, it.outcome) }

            assertEquals(goodPointId, stops[0].servicePointId)
            assertNull(stops[0].materializationIssue)

            assertEquals(wrongBodyTypePointId, stops[1].servicePointId)
            assertEquals(MaterializationIssue.BODY_TYPE_MISMATCH, stops[1].materializationIssue)

            assertEquals(inactivePointId, stops[2].servicePointId)
            assertEquals(MaterializationIssue.SERVICE_POINT_INACTIVE, stops[2].materializationIssue)
        }
    }

    // Builds one vehicle, one active ServicePoint, one driver, a single-stop RouteTemplate,
    // a Shift, and a materialized RouteInstance for the given tenant - returns
    // (routeTemplateId, shiftId, routeInstanceId).
    private fun materializeSimpleRouteFor(tenantId: UUID, label: String): Triple<UUID, UUID, UUID> {
        lateinit var vehicleId: UUID
        lateinit var servicePointId: UUID
        lateinit var driverId: UUID
        lateinit var templateId: UUID
        lateinit var shiftId: UUID

        withTenant(tenantId) {
            vehicleId = vehicleRepository.save(
                Vehicle(
                    tenantId = tenantId, plate = "$label-000-01", type = "REAR_LOADER_26T",
                    capacityM3 = 16.0, capacityKg = 12000.0, heightCm = 350, widthCm = 250,
                    bodyType = BodyType.REAR_LOADER,
                ),
            ).id!!
            servicePointId = servicePointRepository.save(
                ServicePoint(
                    tenantId = tenantId, type = ServicePointType.POINT, geometry = point(34.78, 32.08),
                    address = "$label stop", status = ServicePointStatus.ACTIVE,
                ),
            ).id!!
            driverId = insertDriver(tenantId)
            templateId = routeTemplateRepository.save(RouteTemplate(tenantId = tenantId, name = "$label route")).id!!
            routeTemplateStopRepository.save(
                RouteTemplateStop(tenantId = tenantId, routeTemplateId = templateId, servicePointId = servicePointId, sequence = 1),
            )
            shiftId = shiftRepository.save(
                Shift(
                    tenantId = tenantId, vehicleId = vehicleId, driverId = driverId,
                    shiftDate = LocalDate.of(2026, 9, 6), status = ShiftStatus.PLANNED,
                ),
            ).id!!
        }

        lateinit var routeInstanceId: UUID
        withTenant(tenantId) {
            routeInstanceId = materializationService.materialize(tenantId, shiftId, templateId, LocalDate.of(2026, 9, 6)).id!!
        }
        return Triple(templateId, shiftId, routeInstanceId)
    }

    @Test
    fun `dispatch tables enforce tenant isolation`() {
        val tenantA = insertTenant("Tenant A")
        val tenantB = insertTenant("Tenant B")

        val (templateA, shiftA, routeInstanceA) = materializeSimpleRouteFor(tenantA, "A")
        val (templateB, shiftB, routeInstanceB) = materializeSimpleRouteFor(tenantB, "B")

        withTenant(tenantA) {
            assertEquals(listOf(templateA), routeTemplateRepository.findAllByTenantId(tenantA).map { it.id })
            assertEquals(listOf(shiftA), shiftRepository.findAllByTenantId(tenantA).map { it.id })
            assertNull(routeInstanceRepository.findByTenantIdAndId(tenantA, routeInstanceB))
            assertNull(routeInstanceRepository.findByTenantIdAndShiftId(tenantA, shiftB))
            assertTrue(stopRepository.findAllByTenantIdAndRouteInstanceIdOrderBySequenceAsc(tenantA, routeInstanceB).isEmpty())
        }

        withTenant(tenantB) {
            assertEquals(listOf(templateB), routeTemplateRepository.findAllByTenantId(tenantB).map { it.id })
            assertEquals(listOf(shiftB), shiftRepository.findAllByTenantId(tenantB).map { it.id })
            assertNull(routeInstanceRepository.findByTenantIdAndId(tenantB, routeInstanceA))
            assertEquals(1, stopRepository.findAllByTenantIdAndRouteInstanceIdOrderBySequenceAsc(tenantB, routeInstanceB).size)
        }
    }
}
