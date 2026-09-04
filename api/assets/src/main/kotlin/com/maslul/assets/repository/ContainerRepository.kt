package com.maslul.assets.repository

import com.maslul.assets.entity.Container
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Custom derived-query methods below don't inherit SimpleJpaRepository's class-level
// @Transactional(readOnly = true) the way JpaRepository's own methods (save, findAll, ...)
// do - without this, they run with no Spring-managed transaction, so the RLS session
// variable (set in TenantAwareJpaTransactionManager.doBegin) never gets applied and every
// row is silently filtered out instead of erroring.
@Transactional(readOnly = true)
interface ContainerRepository : JpaRepository<Container, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): Container?

    fun findAllByTenantId(tenantId: UUID): List<Container>

    fun findAllByTenantIdAndServicePointId(tenantId: UUID, servicePointId: UUID): List<Container>

    fun findByTenantIdAndExternalRef(tenantId: UUID, externalRef: String): Container?
}
