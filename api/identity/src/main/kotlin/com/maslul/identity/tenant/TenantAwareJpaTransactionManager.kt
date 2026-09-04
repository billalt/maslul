package com.maslul.identity.tenant

import jakarta.persistence.EntityManagerFactory
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.TransactionDefinition

// Sets the RLS session variable as the first statement of every transaction, on the same
// EntityManager/connection Spring just bound for that transaction. set_config(..., true) is
// the parameterizable equivalent of SET LOCAL - it resets automatically at commit/rollback,
// so a pooled connection never carries one request's tenant into the next.
class TenantAwareJpaTransactionManager(
    private val targetEntityManagerFactory: EntityManagerFactory,
    private val tenantContext: TenantContext,
) : JpaTransactionManager(targetEntityManagerFactory) {

    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        super.doBegin(transaction, definition)

        val tenantId = tenantContext.currentTenantIdOrNull() ?: return
        val entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(targetEntityManagerFactory)
            ?: return

        entityManager
            .createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, true)")
            .setParameter("tenantId", tenantId.toString())
            .singleResult
    }
}
