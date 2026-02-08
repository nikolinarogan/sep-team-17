package com.example.psp_paypal.controller;

import com.example.psp_paypal.dto.MicroservicePaymentRequest;
import com.example.psp_paypal.dto.MicroservicePaymentResponse;
import com.example.psp_paypal.service.PayPalLogicService;
import com.example.psp_paypal.tools.AuditLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connector")
public class PayPalConnectorController {
    private final PayPalLogicService logicService;
    private final AuditLogger auditLogger;

    public PayPalConnectorController(PayPalLogicService logicService, AuditLogger auditLogger) { // Dodato u konstruktor
        this.logicService = logicService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/init")
    public ResponseEntity<MicroservicePaymentResponse> init(@RequestBody MicroservicePaymentRequest req) {
        // Logujemo dolazni zahtev od PSP-Core za inicijalizaciju PayPal naloga
        auditLogger.logEvent("PAYPAL_INIT_START", "PENDING",
                "TransactionUUID: " + req.getTransactionUuid() + " | Amount: " + req.getAmount());

        try {
            MicroservicePaymentResponse response = logicService.createOrder(req);

            auditLogger.logEvent("PAYPAL_INIT_SUCCESS", "SUCCESS",
                    "PayPalOrderID: " + response.getExternalId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Logujemo neuspeh kao bezbednosni incident (PCI DSS 10.2.4)
            auditLogger.logSecurityAlert("PAYPAL_INIT_FAILED",
                    "UUID: " + req.getTransactionUuid() + " | Reason: " + e.getMessage());
            throw e;
        }
    }

    @PostMapping("/capture/{orderId}")
    public ResponseEntity<Boolean> capture(@PathVariable String orderId) {
        // Logujemo pokušaj finalizacije novca
        auditLogger.logEvent("PAYPAL_CAPTURE_START", "PENDING", "OrderID: " + orderId);

        try {
            Boolean result = logicService.captureOrder(orderId);

            auditLogger.logEvent("PAYPAL_CAPTURE_FINISHED", result ? "SUCCESS" : "FAILED",
                    "OrderID: " + orderId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            auditLogger.logSecurityAlert("PAYPAL_CAPTURE_ERROR",
                    "OrderID: " + orderId + " | Error: " + e.getMessage());
            throw e;
        }
    }
}