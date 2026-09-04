package com.maslul.assets.repository

import com.maslul.assets.entity.Vehicle
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VehicleRepository : JpaRepository<Vehicle, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): Vehicle?

    fun findAllByTenantId(tenantId: UUID): List<Vehicle>
}
