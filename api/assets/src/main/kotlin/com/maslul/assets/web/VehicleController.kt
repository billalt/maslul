package com.maslul.assets.web

import com.maslul.assets.entity.Vehicle
import com.maslul.assets.repository.VehicleRepository
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
@RequestMapping("/v1/vehicles")
class VehicleController(
    private val vehicleRepository: VehicleRepository,
    private val tenantContext: TenantContext,
) {

    @GetMapping
    fun list(): List<Vehicle> =
        vehicleRepository.findAllByTenantId(tenantContext.currentTenantId())

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<Vehicle> {
        val vehicle = vehicleRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(vehicle)
    }

    // Rebuilds the entity server-side rather than saving the deserialized body directly,
    // so a client-supplied id or tenant_id in the payload can never be honored.
    @PostMapping
    fun create(@RequestBody vehicle: Vehicle): ResponseEntity<Vehicle> {
        val created = Vehicle(
            tenantId = tenantContext.currentTenantId(),
            plate = vehicle.plate,
            type = vehicle.type,
            capacityM3 = vehicle.capacityM3,
            capacityKg = vehicle.capacityKg,
            heightCm = vehicle.heightCm,
            widthCm = vehicle.widthCm,
            bodyType = vehicle.bodyType,
        )
        val saved = vehicleRepository.save(created)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody vehicle: Vehicle): ResponseEntity<Vehicle> {
        val existing = vehicleRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        existing.plate = vehicle.plate
        existing.type = vehicle.type
        existing.capacityM3 = vehicle.capacityM3
        existing.capacityKg = vehicle.capacityKg
        existing.heightCm = vehicle.heightCm
        existing.widthCm = vehicle.widthCm
        existing.bodyType = vehicle.bodyType
        existing.updatedAt = Instant.now()

        return ResponseEntity.ok(vehicleRepository.save(existing))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val existing = vehicleRepository.findByTenantIdAndId(tenantContext.currentTenantId(), id)
            ?: return ResponseEntity.notFound().build()

        vehicleRepository.delete(existing)
        return ResponseEntity.noContent().build()
    }
}
