package controller;

import dto.MerchantConfigDTO;
import dto.MerchantCreateDTO;
import dto.MerchantCredentialsDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import repository.MerchantSubscriptionRepository;
import service.MerchantService;
import tools.AuditLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@CrossOrigin(origins = "*")
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantSubscriptionRepository subscriptionRepository;
    private final AuditLogger auditLogger;

    public MerchantController(MerchantService merchantService,
                              MerchantSubscriptionRepository subscriptionRepository,
                              AuditLogger auditLogger) {
        this.merchantService = merchantService;
        this.subscriptionRepository = subscriptionRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * AŽURIRANJE SERVISA (Zahtijeva Header X-Merchant-Password)
     * PUT /api/merchants/{id}/services
     */
    @PutMapping("/{merchantId}/services")
    public ResponseEntity<String> updateServices(@PathVariable String merchantId,
                                                 @RequestHeader("X-Merchant-Password") String password,
                                                 @RequestBody List<MerchantConfigDTO> configs) {

        // Logujemo pokušaj promene konfiguracije (Identifikator "KO" je merchantId)
        auditLogger.logEvent("MERCHANT_SERVICES_UPDATE_ATTEMPT", "PENDING", "Merchant: " + merchantId);

        merchantService.updateMerchantServices(merchantId, password, configs);

        auditLogger.logEvent("MERCHANT_SERVICES_UPDATE_SUCCESS", "SUCCESS", "Merchant: " + merchantId);
        return ResponseEntity.ok("Servisi uspješno ažurirani.");
    }

    /**
     * Endpoint za uklanjanje servisa
     * DELETE /api/merchants/{id}/services/{methodName}
     */
    @DeleteMapping("/{merchantId}/services/{methodName}")
    public ResponseEntity<String> removeService(@PathVariable String merchantId,
                                                @PathVariable String methodName) {

        auditLogger.logEvent("MERCHANT_SERVICE_REMOVAL", "START",
                "Merchant: " + merchantId + " | Method: " + methodName);

        merchantService.removeMerchantService(merchantId, methodName);

        auditLogger.logEvent("MERCHANT_SERVICE_REMOVAL_SUCCESS", "SUCCESS",
                "Merchant: " + merchantId + " | Method: " + methodName);
        return ResponseEntity.ok("Servis uspješno uklonjen.");
    }

    /**
     * KREIRANJE NOVOG PRODAVCA
     * POST /api/merchants/create
     */
    @PostMapping("/create")
    public ResponseEntity<MerchantCredentialsDTO> createMerchant(@RequestBody MerchantCreateDTO request) {
        // Logujemo kreiranje novog entiteta u sistemu (PCI DSS 10.2.2)
        auditLogger.logEvent("MERCHANT_CREATION_REQUEST", "START", "Merchant Name: " + request.getName());

        MerchantCredentialsDTO credentials = merchantService.createMerchant(request);

        auditLogger.logEvent("MERCHANT_CREATION_SUCCESS", "SUCCESS", "Generated ID: " + credentials.getMerchantId());
        return ResponseEntity.ok(credentials);
    }

    @GetMapping
    public ResponseEntity<List<model.Merchant>> getAllMerchants() {
        // Logujemo pristup listi svih prodavaca (Administrativni uvid)
        auditLogger.logEvent("VIEW_ALL_MERCHANTS", "SUCCESS", "Accessed by admin/system");
        return ResponseEntity.ok(merchantService.findAll());
    }
    @GetMapping("/{merchantId}/subscriptions")
    public ResponseEntity<List<model.MerchantSubscription>> getSubscriptions(@PathVariable String merchantId) {
        // Logujemo uvid u pretplate specifičnog prodavca
        auditLogger.logEvent("VIEW_MERCHANT_SUBSCRIPTIONS", "SUCCESS", "Merchant: " + merchantId);
        return ResponseEntity.ok(subscriptionRepository.findByMerchantMerchantId(merchantId));
    }
}