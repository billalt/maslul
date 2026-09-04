package com.maslul.assets.web

import com.maslul.assets.entity.Vehicle
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/vehicles")
class VehicleController {

    @GetMapping
    fun list(): List<Vehicle> {
        TODO("not implemented - skeleton pending review")
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<Vehicle> {
        TODO("not implemented - skeleton pending review")
    }

    @PostMapping
    fun create(@RequestBody vehicle: Vehicle): ResponseEntity<Vehicle> {
        TODO("not implemented - skeleton pending review")
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @RequestBody vehicle: Vehicle): ResponseEntity<Vehicle> {
        TODO("not implemented - skeleton pending review")
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        TODO("not implemented - skeleton pending review")
    }
}
