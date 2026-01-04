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

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;

    public AdminController(AdminRepository adminRepository,
                           MerchantRepository merchantRepository,
                           MerchantService merchantService,
                           PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.merchantRepository = merchantRepository;
        this.merchantService = merchantService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji."));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(401).body("Pogrešna lozinka.");
        }

        return ResponseEntity.ok("Uspešna prijava.");
    }

    @GetMapping("/merchants/{id}")
    public ResponseEntity<Merchant> getMerchant(@PathVariable String id) {
        return ResponseEntity.ok(merchantRepository.findByMerchantId(id)
                .orElseThrow(() -> new RuntimeException("Prodavac nije pronađen")));
    }

    @PostMapping("/merchants/{id}/services")
    public ResponseEntity<String> updateMerchantServices(@PathVariable String id,
                                                         @RequestBody List<MerchantConfigDTO> configs) {
        merchantService.updateServicesByAdmin(id, configs);
        return ResponseEntity.ok("Servisi uspješno ažurirani.");
    }

}