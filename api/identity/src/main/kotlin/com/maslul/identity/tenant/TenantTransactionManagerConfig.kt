package com.maslul.identity.tenant

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class TenantTransactionManagerConfig {

    // Named "transactionManager" so it replaces Spring Boot's auto-configured one
    // (JpaBaseConfiguration only defines its own when none is already present).
    @Bean
    fun transactionManager(
        entityManagerFactory: EntityManagerFactory,
        tenantContext: TenantContext,
    ): PlatformTransactionManager = TenantAwareJpaTransactionManager(entityManagerFactory, tenantContext)
}
