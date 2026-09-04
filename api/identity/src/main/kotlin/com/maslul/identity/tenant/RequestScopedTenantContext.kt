package com.maslul.identity.tenant

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

@Component
class RequestScopedTenantContext : TenantContext {

    override fun currentTenantId(): UUID =
        currentTenantIdOrNull() ?: throw IllegalStateException("no tenant resolved for the current request")

    override fun currentTenantIdOrNull(): UUID? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
        return attributes.request.getAttribute(TENANT_ID_ATTRIBUTE) as? UUID
    }

    companion object {
        const val TENANT_ID_ATTRIBUTE = "com.maslul.identity.tenant.tenantId"
    }
}
