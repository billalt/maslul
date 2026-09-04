package com.maslul.assets.web

import com.maslul.assets.entity.Container
import com.maslul.assets.repository.ContainerRepository
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
@RequestMapping("/v1/containers")
class ContainerController(
    private val containerRepository: ContainerRepository,
    private val tenantContext: TenantContext,
) {

    @GetMapping
    fun list(): List<Container> =
        containerRepository.findAllByTenantId(tenantContext.currentTenantId())

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<Container> {
        val container = containerRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(container)
    }

    // Rebuilds the entity server-side rather than saving the deserialized body directly,
    // so a client-supplied id or tenant_id in the payload can never be honored.
    @PostMapping
    fun create(@RequestBody container: Container): ResponseEntity<Container> {
        val created = Container(
            tenantId = tenantContext.currentTenantId(),
            servicePointId = container.servicePointId,
            externalRef = container.externalRef,
            rfidTag = container.rfidTag,
            wasteStream = container.wasteStream,
            volumeLiters = container.volumeLiters,
            containerType = container.containerType,
            requiredBodyType = container.requiredBodyType,
            status = container.status,
            accessNotes = container.accessNotes,
        )
        val saved = containerRepository.save(created)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody container: Container): ResponseEntity<Container> {
        val existing = containerRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        existing.servicePointId = container.servicePointId
        existing.externalRef = container.externalRef
        existing.rfidTag = container.rfidTag
        existing.wasteStream = container.wasteStream
        existing.volumeLiters = container.volumeLiters
        existing.containerType = container.containerType
        existing.requiredBodyType = container.requiredBodyType
        existing.status = container.status
        existing.accessNotes = container.accessNotes
        existing.updatedAt = Instant.now()

        return ResponseEntity.ok(containerRepository.save(existing))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val existing = containerRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        containerRepository.delete(existing)
        return ResponseEntity.noContent().build()
    }
}
