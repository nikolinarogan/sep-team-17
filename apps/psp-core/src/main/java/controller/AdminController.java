package controller;

import dto.LoginRequestDTO;
import dto.MerchantConfigDTO;
import model.Admin;
import model.Merchant;
import repository.AdminRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import repository.MerchantRepository;
import service.MerchantService;
import tools.AuditLogger; // Dodato

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;
    private final AuditLogger auditLogger;

    public AdminController(AdminRepository adminRepository,
                           MerchantRepository merchantRepository,
                           MerchantService merchantService,
                           PasswordEncoder passwordEncoder,
                           AuditLogger auditLogger) {
        this.adminRepository = adminRepository;
        this.merchantRepository = merchantRepository;
        this.merchantService = merchantService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO request) {
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

        auditLogger.logEvent("ADMIN_LOGIN_SUCCESS", "SUCCESS", "User: " + request.getUsername());
        return ResponseEntity.ok("Uspešna prijava.");
    }

    @GetMapping("/merchants/{id}")
    public ResponseEntity<Merchant> getMerchant(@PathVariable String id) {
        auditLogger.logEvent("ADMIN_VIEW_MERCHANT", "SUCCESS", "Accessed merchant ID: " + id);

        return ResponseEntity.ok(merchantRepository.findByMerchantId(id)
                .orElseThrow(() -> new RuntimeException("Prodavac nije pronađen")));
    }

    @PostMapping("/merchants/{id}/services")
    public ResponseEntity<String> updateMerchantServices(@PathVariable String id,
                                                         @RequestBody List<MerchantConfigDTO> configs) {
        auditLogger.logEvent("ADMIN_UPDATE_SERVICES_START", "PENDING", "Merchant: " + id);

        merchantService.updateServicesByAdmin(id, configs);

        auditLogger.logEvent("ADMIN_UPDATE_SERVICES_SUCCESS", "SUCCESS", "Configuration changed for merchant: " + id);
        return ResponseEntity.ok("Servisi uspješno ažurirani.");
    }
}