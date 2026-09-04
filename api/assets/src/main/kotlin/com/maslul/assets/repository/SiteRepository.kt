package com.maslul.assets.repository

import com.maslul.assets.entity.Site
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SiteRepository : JpaRepository<Site, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): Site?

    fun findAllByTenantId(tenantId: UUID): List<Site>
}
