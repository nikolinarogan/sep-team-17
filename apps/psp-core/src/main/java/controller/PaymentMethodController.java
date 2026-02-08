package controller;

import dto.PaymentMethodRequestDTO;
import model.PaymentMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import repository.PaymentMethodRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.AuditLogger;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "https://localhost:4201", allowedHeaders = "*", allowCredentials = "true")
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;
    private final AuditLogger auditLogger; // Dodato

    public PaymentMethodController(PaymentMethodRepository paymentMethodRepository,
                                   AuditLogger auditLogger) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.auditLogger = auditLogger;
    }

    // Vraća sve dostupne metode
    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getAllMethods() {
        auditLogger.logEvent("VIEW_PAYMENT_METHODS", "SUCCESS", "Requested list of all payment methods");
        return ResponseEntity.ok(paymentMethodRepository.findAll());
    }

    // Dodaje novi globalni metod (npr. CRYPTO)
    @PostMapping
    public ResponseEntity<PaymentMethod> createMethod(@RequestBody PaymentMethodRequestDTO request) {
        auditLogger.logEvent("CREATE_PAYMENT_METHOD_START", "PENDING", "Method Name: " + request.getName());

        if (paymentMethodRepository.findByName(request.getName()).isPresent()) {
            auditLogger.logSecurityAlert("DUPLICATE_METHOD_CREATION", "Method already exists: " + request.getName());
            throw new RuntimeException("Metod " + request.getName() + " već postoji.");
        }

        PaymentMethod pm = new PaymentMethod();
        pm.setName(request.getName());
        pm.setServiceUrl(request.getServiceUrl());

        PaymentMethod saved = paymentMethodRepository.save(pm);

        auditLogger.logEvent("CREATE_PAYMENT_METHOD_SUCCESS", "SUCCESS", "New global method added: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    // Brisanje metoda
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMethod(@PathVariable Long id) {
        auditLogger.logEvent("DELETE_PAYMENT_METHOD_START", "PENDING", "Method ID: " + id);

        paymentMethodRepository.deleteById(id);

        auditLogger.logEvent("DELETE_PAYMENT_METHOD_SUCCESS", "SUCCESS", "Method ID " + id + " removed from system");
        return ResponseEntity.ok().build();
    }
}