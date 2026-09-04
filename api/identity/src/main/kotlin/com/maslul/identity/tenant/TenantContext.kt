package com.maslul.identity.tenant

import java.util.UUID

interface TenantContext {
    fun currentTenantId(): UUID

    // For infrastructure code (the transaction manager) that must tolerate no tenant
    // being in scope yet, rather than throw. RLS defaults to deny when unset - fail closed.
    fun currentTenantIdOrNull(): UUID?
}
