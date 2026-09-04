package com.maslul.identity.tenant

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTenantResolver(
    @Value("\${app.jwt.secret}") secret: String,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun resolveTenantId(request: HttpServletRequest): UUID? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        val token = header.removePrefix("Bearer ")

        return try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            (claims["tenant_id"] as? String)?.let(UUID::fromString)
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
