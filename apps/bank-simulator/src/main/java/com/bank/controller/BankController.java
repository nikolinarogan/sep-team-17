package com.bank.controller;

import com.bank.dto.*;
import com.bank.service.BankService;

import com.bank.tools.AuditLogger;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bank")
@CrossOrigin(origins = "*")
public class BankController {

    private final BankService bankService;
    private final AuditLogger auditLogger;
    private static final String PSP_CALLBACK_URL = "https://localhost:8443/api/payments/payment-callback";

    public BankController(BankService bankService, AuditLogger auditLogger) { // Dodato u konstruktor
        this.bankService = bankService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/card")
    public ResponseEntity<PspPaymentResponseDTO> createPaymentUrl(@RequestBody PspPaymentRequestDTO request) {
        auditLogger.logEvent("BANK_PAYMENT_INIT_START", "PENDING",
                "Merchant: " + request.getMerchantId() + " | PSP_TX: " + request.getPspTransactionId());

        PspPaymentResponseDTO response = bankService.createPaymentUrl(request);

        auditLogger.logEvent("BANK_PAYMENT_INIT_SUCCESS", "SUCCESS", "InternalID: " + response.getPaymentId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> processPayment(@Valid @RequestBody BankPaymentFormDTO paymentForm) {
        auditLogger.logEvent("BANK_CARD_PAYMENT_ATTEMPT", "PENDING", "PaymentID: " + paymentForm.getPaymentId());

        Map<String, String> response = new HashMap<>();

        try {
            String callbackUrl = bankService.processPayment(paymentForm);

            response.put("status", "SUCCESS");
            response.put("redirectUrl", callbackUrl);
            response.put("GLOBAL_TRANSACTION_ID", paymentForm.getPaymentId());
            response.put("ACQUIRER_TIMESTAMP", java.time.LocalDateTime.now().toString());

            auditLogger.logEvent("BANK_CARD_PAYMENT_FINISHED", "SUCCESS", "PaymentID: " + paymentForm.getPaymentId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            auditLogger.logSecurityAlert("BANK_CARD_PAYMENT_REJECTED", "ID: " + paymentForm.getPaymentId() + " | Reason: " + e.getMessage());

            String redirectUrl = PSP_CALLBACK_URL + "?paymentId=" + paymentForm.getPaymentId() + "&status=FAILED";

            response.put("status", "FAILED");
            response.put("message", e.getMessage());
            response.put("redirectUrl", redirectUrl);
            response.put("ACQUIRER_TIMESTAMP", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/qr-initialize")
    public ResponseEntity<Map<String, String>> initializeQr(@RequestBody PspPaymentRequestDTO request) {
        auditLogger.logEvent("BANK_QR_INIT_START", "PENDING", "PSP_TX: " + request.getPspTransactionId());

        String qrData = bankService.generateIpsQrString(request);
        PspPaymentResponseDTO bankResponse = bankService.createPaymentUrl(request);

        Map<String, String> response = new HashMap<>();
        response.put("qrData", qrData);
        response.put("paymentId", bankResponse.getPaymentId());

        auditLogger.logEvent("BANK_QR_INIT_SUCCESS", "SUCCESS", "PaymentID: " + bankResponse.getPaymentId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> processQrPayment(@RequestBody QrTransferRequestDTO request) {
        auditLogger.logEvent("BANK_QR_TRANSFER_ATTEMPT", "PENDING", "User: " + request.getEmail());

        try {
            String redirectUrl = bankService.processInternalTransfer(request);

            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "QR Plaćanje uspešno!");
            response.put("redirectUrl", redirectUrl);
            response.put("GLOBAL_TRANSACTION_ID", java.util.UUID.randomUUID().toString());
            response.put("ACQUIRER_TIMESTAMP", java.time.LocalDateTime.now().toString());

            auditLogger.logEvent("BANK_QR_TRANSFER_SUCCESS", "SUCCESS", "User: " + request.getEmail());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            auditLogger.logSecurityAlert("BANK_QR_TRANSFER_FAILED", "User: " + request.getEmail() + " | Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}