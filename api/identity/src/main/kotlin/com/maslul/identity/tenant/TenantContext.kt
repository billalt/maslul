package com.maslul.identity.tenant

import java.util.UUID

interface TenantContext {
    fun currentTenantId(): UUID
}
