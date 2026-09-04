package com.maslul.identity.tenant

import java.util.UUID

class RequestScopedTenantContext : TenantContext {
    override fun currentTenantId(): UUID {
        TODO("not implemented - approach pending review")
    }
}
