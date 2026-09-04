package com.maslul.assets.web

import com.maslul.assets.entity.Site
import com.maslul.assets.repository.SiteRepository
import com.maslul.identity.tenant.TenantContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/v1/sites")
class SiteController(
    private val siteRepository: SiteRepository,
    private val tenantContext: TenantContext,
) {

    @GetMapping
    fun list(): List<Site> =
        siteRepository.findAllByTenantId(tenantContext.currentTenantId())

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<Site> {
        val site = siteRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(site)
    }

    // Rebuilds the entity server-side rather than saving the deserialized body directly,
    // so a client-supplied id or tenant_id in the payload can never be honored.
    @PostMapping
    fun create(@RequestBody site: Site): ResponseEntity<Site> {
        val created = Site(
            tenantId = tenantContext.currentTenantId(),
            name = site.name,
            type = site.type,
            location = site.location,
        )
        val saved = siteRepository.save(created)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody site: Site): ResponseEntity<Site> {
        val existing = siteRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        existing.name = site.name
        existing.type = site.type
        existing.location = site.location
        existing.updatedAt = Instant.now()

        return ResponseEntity.ok(siteRepository.save(existing))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val existing = siteRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        siteRepository.delete(existing)
        return ResponseEntity.noContent().build()
    }
}
