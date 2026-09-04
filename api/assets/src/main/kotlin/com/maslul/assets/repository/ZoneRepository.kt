package com.maslul.assets.repository

import com.maslul.assets.entity.Zone
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ZoneRepository : JpaRepository<Zone, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): Zone?

    fun findAllByTenantId(tenantId: UUID): List<Zone>
}
