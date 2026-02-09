package com.example.pspcrypto.controller;

import com.example.pspcrypto.dto.MicroservicePaymentRequest;
import com.example.pspcrypto.dto.MicroservicePaymentResponse;
import com.example.pspcrypto.service.CryptoLogicService;
import com.example.pspcrypto.tools.AuditLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/connector")
public class CryptoConnectorController {

    private final CryptoLogicService logicService;
    private final AuditLogger auditLogger;

    public CryptoConnectorController(CryptoLogicService logicService, AuditLogger auditLogger) {
        this.logicService = logicService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/init")
    public ResponseEntity<MicroservicePaymentResponse> init(@RequestBody MicroservicePaymentRequest req) {
        // Logujemo dolazni zahtev
        auditLogger.logEvent("CRYPTO_INIT_REQUEST", "START",
                "TransactionUUID: " + req.getTransactionUuid());

        MicroservicePaymentResponse response = logicService.initializeCryptoPayment(req);
        return ResponseEntity.ok(response);
    }

    // Endpoint koji će Frontend (ili psp-core) zvati da proveri da li je legla uplata
    @GetMapping("/check-status/{uuid}")
    public ResponseEntity<Boolean> checkStatus(@PathVariable String uuid) {
        boolean isPaid = logicService.checkPaymentStatus(uuid);
        return ResponseEntity.ok(isPaid);
    }

    @GetMapping("/details/{uuid}")
    public ResponseEntity<Map<String, Object>> getTransactionDetails(@PathVariable String uuid) {
        // Poziva servis da izvuče podatke iz keša (mape)
        return ResponseEntity.ok(logicService.getDetailsFromCache(uuid));
    }
}