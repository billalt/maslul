package com.maslul.assets.repository

import com.maslul.assets.entity.Container
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ContainerRepository : JpaRepository<Container, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): Container?

    fun findAllByTenantId(tenantId: UUID): List<Container>

    fun findAllByTenantIdAndServicePointId(tenantId: UUID, servicePointId: UUID): List<Container>
}
