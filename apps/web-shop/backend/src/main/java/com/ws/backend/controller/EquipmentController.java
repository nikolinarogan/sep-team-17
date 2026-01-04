package com.ws.backend.controller;

import com.ws.backend.dto.EquipmentRequestDTO;
import com.ws.backend.model.Equipment;
import com.ws.backend.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createEquipment(@RequestBody EquipmentRequestDTO request) {
        try {
            Equipment equipment = equipmentService.addEquipment(
                request.getPricePerDay(),
                request.getEquipmentType(),
                request.getAvailable() != null ? request.getAvailable() : true
            );
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create equipment: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(
            @PathVariable Long id,
            @RequestBody Equipment updatedEquipment) {
        try {
            Equipment equipment = equipmentService.updateEquipment(id, updatedEquipment);
            return ResponseEntity.ok(equipment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEquipment(@PathVariable Long id) {
        try {
            equipmentService.deleteEquipment(id);
            return ResponseEntity.ok("Equipment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete equipment: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable Long id) {
        Equipment equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(equipment);
    }

    @GetMapping
    public ResponseEntity<List<Equipment>> getAllEquipment() {
        List<Equipment> equipment = equipmentService.getAllEquipment();
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Equipment>> getAvailableEquipment() {
        List<Equipment> availableEquipment = equipmentService.getAvailableEquipment();
        return ResponseEntity.ok(availableEquipment);
    }
}

