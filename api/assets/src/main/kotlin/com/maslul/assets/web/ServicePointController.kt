package com.maslul.assets.web

import com.maslul.assets.entity.ServicePoint
import com.maslul.assets.repository.ServicePointRepository
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
@RequestMapping("/v1/service-points")
class ServicePointController(
    private val servicePointRepository: ServicePointRepository,
    private val tenantContext: TenantContext,
) {

    @GetMapping
    fun list(): List<ServicePoint> =
        servicePointRepository.findAllByTenantId(tenantContext.currentTenantId())

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<ServicePoint> {
        val servicePoint = servicePointRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(servicePoint)
    }

    // Rebuilds the entity server-side rather than saving the deserialized body directly,
    // so a client-supplied id or tenant_id in the payload can never be honored.
    @PostMapping
    fun create(@RequestBody servicePoint: ServicePoint): ResponseEntity<ServicePoint> {
        val created = ServicePoint(
            tenantId = tenantContext.currentTenantId(),
            externalRef = servicePoint.externalRef,
            name = servicePoint.name,
            type = servicePoint.type,
            geometry = servicePoint.geometry,
            side = servicePoint.side,
            address = servicePoint.address,
            status = servicePoint.status,
        )
        val saved = servicePointRepository.save(created)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody servicePoint: ServicePoint): ResponseEntity<ServicePoint> {
        val existing = servicePointRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        existing.externalRef = servicePoint.externalRef
        existing.name = servicePoint.name
        existing.type = servicePoint.type
        existing.geometry = servicePoint.geometry
        existing.side = servicePoint.side
        existing.address = servicePoint.address
        existing.status = servicePoint.status
        existing.updatedAt = Instant.now()

        return ResponseEntity.ok(servicePointRepository.save(existing))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val existing = servicePointRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        servicePointRepository.delete(existing)
        return ResponseEntity.noContent().build()
    }
}
