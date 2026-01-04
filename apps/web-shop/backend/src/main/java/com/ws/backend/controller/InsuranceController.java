package com.ws.backend.controller;

import com.ws.backend.dto.InsuranceRequestDTO;
import com.ws.backend.model.Insurance;
import com.ws.backend.service.InsuranceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/insurances")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createInsurance(@RequestBody InsuranceRequestDTO request) {
        try {
            Insurance insurance = insuranceService.addInsurance(
                request.getPrice(),
                request.getType(),
                request.getAvailable()
            );
            return ResponseEntity.ok(insurance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create insurance: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Insurance> updateInsurance(
            @PathVariable Long id,
            @RequestBody Insurance updatedInsurance) {
        try {
            Insurance insurance = insuranceService.updateInsurance(id, updatedInsurance);
            return ResponseEntity.ok(insurance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteInsurance(@PathVariable Long id) {
        try {
            insuranceService.deleteInsurance(id);
            return ResponseEntity.ok("Insurance deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete insurance: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Insurance> getInsuranceById(@PathVariable Long id) {
        Insurance insurance = insuranceService.getInsuranceById(id);
        return ResponseEntity.ok(insurance);
    }

    @GetMapping
    public ResponseEntity<List<Insurance>> getAllInsurances() {
        List<Insurance> insurances = insuranceService.getAllInsurances();
        return ResponseEntity.ok(insurances);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Insurance>> getAvailableInsurances() {
        List<Insurance> availableInsurances = insuranceService.getAvailableInsurances();
        return ResponseEntity.ok(availableInsurances);
    }
}
