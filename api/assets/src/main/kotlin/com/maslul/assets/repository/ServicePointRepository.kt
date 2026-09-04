package com.maslul.assets.repository

import com.maslul.assets.entity.ServicePoint
import com.maslul.assets.entity.ServicePointType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ServicePointRepository : JpaRepository<ServicePoint, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): ServicePoint?

    fun findAllByTenantId(tenantId: UUID): List<ServicePoint>

    fun findAllByTenantIdAndType(tenantId: UUID, type: ServicePointType): List<ServicePoint>

    // ST_DWithin is polymorphic over Point and LineString - one query, no per-type branching.
    @Query(
        value = "SELECT * FROM service_point sp " +
            "WHERE sp.tenant_id = :tenantId " +
            "AND ST_DWithin(sp.geometry::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)",
        nativeQuery = true,
    )
    fun findWithinRadius(
        @Param("tenantId") tenantId: UUID,
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radiusMeters") radiusMeters: Double,
    ): List<ServicePoint>

    // ST_Intersects also works the same way regardless of the row's underlying geometry subtype.
    @Query(
        value = "SELECT sp.* FROM service_point sp, zone z " +
            "WHERE sp.tenant_id = :tenantId AND z.id = :zoneId AND ST_Intersects(sp.geometry, z.boundary)",
        nativeQuery = true,
    )
    fun findWithinZone(
        @Param("tenantId") tenantId: UUID,
        @Param("zoneId") zoneId: UUID,
    ): List<ServicePoint>
}
