package com.maslul.assets.web

import com.maslul.assets.entity.Zone
import com.maslul.assets.repository.ZoneRepository
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
@RequestMapping("/v1/zones")
class ZoneController(
    private val zoneRepository: ZoneRepository,
    private val tenantContext: TenantContext,
) {

    @GetMapping
    fun list(): List<Zone> =
        zoneRepository.findAllByTenantId(tenantContext.currentTenantId())

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<Zone> {
        val zone = zoneRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(zone)
    }

    // Rebuilds the entity server-side rather than saving the deserialized body directly,
    // so a client-supplied id or tenant_id in the payload can never be honored.
    @PostMapping
    fun create(@RequestBody zone: Zone): ResponseEntity<Zone> {
        val created = Zone(
            tenantId = tenantContext.currentTenantId(),
            name = zone.name,
            boundary = zone.boundary,
        )
        val saved = zoneRepository.save(created)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody zone: Zone): ResponseEntity<Zone> {
        val existing = zoneRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        existing.name = zone.name
        existing.boundary = zone.boundary
        existing.updatedAt = Instant.now()

        return ResponseEntity.ok(zoneRepository.save(existing))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val existing = zoneRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        zoneRepository.delete(existing)
        return ResponseEntity.noContent().build()
    }
}
