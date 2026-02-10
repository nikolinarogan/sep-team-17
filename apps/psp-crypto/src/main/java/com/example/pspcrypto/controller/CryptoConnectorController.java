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
        auditLogger.logEvent("CRYPTO_INIT_REQUEST", "START",
                "TransactionUUID: " + req.getTransactionUuid());

        MicroservicePaymentResponse response = logicService.initializeCryptoPayment(req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-status/{token}")
    public ResponseEntity<Boolean> checkStatus(@PathVariable String token) {
        // Token sadrži adresu i iznos
        return ResponseEntity.ok(logicService.checkPaymentStatus(token));
    }

    @GetMapping("/details/{token}")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable String token) {
        return ResponseEntity.ok(logicService.getDetailsFromToken(token));
    }
}