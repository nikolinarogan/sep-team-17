package com.ws.backend.controller;

import com.ws.backend.model.Vehicle;
import com.ws.backend.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> addVehicle(
            @RequestParam String model,
            @RequestParam Boolean isAvailable,
            @RequestParam Double pricePerDay,
            @RequestParam MultipartFile imageFile
    ) {
        try {
            Vehicle vehicle = vehicleService.addVehicle(model, isAvailable, pricePerDay, imageFile);
            return ResponseEntity.ok(vehicle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create vehicle: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(
            @PathVariable Long id,
            @RequestPart("vehicle") Vehicle vehicle,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) throws IOException {
        Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle, imageFile);
        return ResponseEntity.ok(updatedVehicle);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok("Vehicle deleted successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        List<Vehicle> availableVehicles = vehicleService.getAvailableVehicles();
        return ResponseEntity.ok(availableVehicles);
    }
}
