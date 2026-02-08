package controller;

import dto.LoginRequestDTO;
import dto.MerchantConfigDTO;
import model.Admin;
import model.Merchant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import repository.AdminRepository;
import jwt.JwtService; // Dodato
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import repository.MerchantRepository;
import service.MerchantService;
import tools.AuditLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "https://localhost:4201")
public class AdminController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;
    private final AuditLogger auditLogger;
    private final JwtService jwtService;

    public AdminController(AdminRepository adminRepository,
                           MerchantRepository merchantRepository,
                           MerchantService merchantService,
                           PasswordEncoder passwordEncoder,
                           AuditLogger auditLogger,
                           JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.merchantRepository = merchantRepository;
        this.merchantService = merchantService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        auditLogger.logEvent("ADMIN_LOGIN_ATTEMPT", "PENDING", "Username: " + request.getUsername());

        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("ADMIN_LOGIN_FAILED", "User not found: " + request.getUsername());
                    return new RuntimeException("Korisnik ne postoji.");
                });

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            auditLogger.logSecurityAlert("ADMIN_LOGIN_FAILED", "Invalid password for user: " + request.getUsername());
            return ResponseEntity.status(401).body("Pogrešna lozinka.");
        }
        if (admin.isActive() == false) {
            auditLogger.logSecurityAlert("ADMIN_LOGIN_FAILED", "Deactivated account for user: " + request.getUsername());
            return ResponseEntity.status(401).body("Nalog ovog korisnika je deaktiviran.");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        // GENERIŠEMO TOKEN KAO NA WEB SHOPU
        String token = jwtService.generateToken(admin);

        auditLogger.logEvent("ADMIN_LOGIN_SUCCESS", "SUCCESS", "User: " + request.getUsername());

        // Vraćamo mapu ili DTO sa tokenom
        return ResponseEntity.ok(Map.of(
                "message", "Uspešna prijava.",
                "token", token
        ));
    }

    @GetMapping("/merchants/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Merchant> getMerchant(@PathVariable String id) {
        auditLogger.logEvent("ADMIN_VIEW_MERCHANT", "SUCCESS", "Accessed merchant ID: " + id);
        return ResponseEntity.ok(merchantRepository.findByMerchantId(id)
                .orElseThrow(() -> new RuntimeException("Prodavac nije pronađen")));
    }

    @PostMapping("/merchants/{id}/services")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<String> updateMerchantServices(@PathVariable String id,
                                                         @RequestBody List<MerchantConfigDTO> configs) {
        auditLogger.logEvent("ADMIN_UPDATE_SERVICES_START", "PENDING", "Merchant: " + id);
        merchantService.updateServicesByAdmin(id, configs);
        auditLogger.logEvent("ADMIN_UPDATE_SERVICES_SUCCESS", "SUCCESS", "Configuration changed for merchant: " + id);
        return ResponseEntity.ok("Servisi uspješno ažurirani.");
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        return adminRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    adminRepository.save(user);

                    return ResponseEntity.ok("Korisnikov nalog je uspešno deaktiviran.");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}