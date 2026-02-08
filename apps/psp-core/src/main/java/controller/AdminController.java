package controller;

import dto.ChangePasswordDTO;
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
import service.LoginAttemptService;
import service.MerchantService;
import service.SessionActivityService;
import tools.AuditLogger;

import java.util.HashMap;
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
    private final SessionActivityService sessionActivityService;
    private final LoginAttemptService loginAttemptService;

    public AdminController(AdminRepository adminRepository,
                           MerchantRepository merchantRepository,
                           MerchantService merchantService,
                           PasswordEncoder passwordEncoder,
                           AuditLogger auditLogger,
                           JwtService jwtService,
                           SessionActivityService sessionActivityService,
                           LoginAttemptService loginAttemptService) {
        this.adminRepository = adminRepository;
        this.merchantRepository = merchantRepository;
        this.merchantService = merchantService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
        this.jwtService = jwtService;
        this.sessionActivityService = sessionActivityService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        String username = request.getUsername();
        auditLogger.logEvent("ADMIN_LOGIN_ATTEMPT", "PENDING", "Username: " + username);

        if (username != null && !username.isBlank()) {
            if (loginAttemptService.isLockedOut(username)) {
                long remaining = loginAttemptService.getRemainingLockoutMinutes(username);
                auditLogger.logSecurityAlert("ADMIN_LOGIN_LOCKED", "Lockout: " + username + " (" + remaining + " min)");
                return ResponseEntity.status(429).body(
                        "Prijava onemogućena. Previše neuspešnih pokušaja. Pokušajte ponovo za " + remaining + " minuta.");
            }
        }

        Admin admin = adminRepository.findByUsername(username).orElse(null);

        if (admin == null) {
            if (username != null && !username.isBlank()) {
                loginAttemptService.recordFailedAttempt(username);
            }
            auditLogger.logSecurityAlert("ADMIN_LOGIN_FAILED", "User not found: " + username);
            return ResponseEntity.status(401).body("Pogrešno korisničko ime ili lozinka.");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            loginAttemptService.recordFailedAttempt(username);
            auditLogger.logSecurityAlert("ADMIN_LOGIN_FAILED", "Invalid password for user: " + username);
            return ResponseEntity.status(401).body("Pogrešna lozinka.");
        }

        loginAttemptService.clearAttempts(username);
        if (admin.isActive() == false) {
            auditLogger.logSecurityAlert("ADMIN_LOGIN_FAILED", "Deactivated account for user: " + request.getUsername());
            return ResponseEntity.status(401).body("Nalog ovog korisnika je deaktiviran.");
        }

        // Prvi login – mora da promeni lozinku
        if (Boolean.FALSE.equals(admin.getHasChangedPassword())) {
            auditLogger.logEvent("ADMIN_FIRST_LOGIN", "MUST_CHANGE_PASSWORD", "User: " + request.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Morate promeniti lozinku pri prvom prijavljivanju.");
            response.put("token", null);
            response.put("mustChangePassword", true);
            return ResponseEntity.ok(response);
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        sessionActivityService.updateActivity(admin.getId());

        String token = jwtService.generateToken(admin);

        auditLogger.logEvent("ADMIN_LOGIN_SUCCESS", "SUCCESS", "User: " + request.getUsername());

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

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO dto) {
        auditLogger.logEvent("ADMIN_CHANGE_PASSWORD_ATTEMPT", "PENDING", "Username: " + dto.getUsername());

        Admin admin = adminRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("CHANGE_PASSWORD_FAILED", "Admin not found: " + dto.getUsername());
                    return new IllegalArgumentException("Korisnik nije pronađen.");
                });

        boolean isFirstTime = Boolean.FALSE.equals(admin.getHasChangedPassword());

        if (!isFirstTime) {
            if (dto.getOldPassword() == null || dto.getOldPassword().isBlank()) {
                return ResponseEntity.badRequest().body("Trenutna lozinka je obavezna.");
            }
            if (!passwordEncoder.matches(dto.getOldPassword(), admin.getPassword())) {
                auditLogger.logSecurityAlert("CHANGE_PASSWORD_FAILED", "Wrong old password: " + dto.getUsername());
                return ResponseEntity.badRequest().body("Trenutna lozinka nije ispravna.");
            }
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest().body("Nova lozinka mora imati najmanje 8 karaktera.");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), admin.getPassword())) {
            return ResponseEntity.badRequest().body("Nova lozinka ne sme biti ista kao prethodna.");
        }

        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        admin.setHasChangedPassword(true);
        adminRepository.save(admin);

        auditLogger.logEvent("ADMIN_CHANGE_PASSWORD_SUCCESS", "SUCCESS", "User: " + dto.getUsername());

        return ResponseEntity.ok(Map.of("message", "Lozinka uspešno promenjena. Prijavite se ponovo."));
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