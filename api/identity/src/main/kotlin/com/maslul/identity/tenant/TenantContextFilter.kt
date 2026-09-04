package com.maslul.identity.tenant

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantContextFilter(
    private val jwtTenantResolver: JwtTenantResolver,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tenantId = jwtTenantResolver.resolveTenantId(request)
        if (tenantId != null) {
            request.setAttribute(RequestScopedTenantContext.TENANT_ID_ATTRIBUTE, tenantId)
        }
        filterChain.doFilter(request, response)
    }
}
