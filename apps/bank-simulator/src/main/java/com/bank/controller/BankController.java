package com.bank.controller;

import com.bank.dto.*;
import com.bank.service.BankService;
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
    private static final String PSP_CALLBACK_URL = "https://localhost:8443/api/payments/payment-callback";

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @PostMapping("/card")
    public ResponseEntity<PspPaymentResponseDTO> createPaymentUrl(@RequestBody PspPaymentRequestDTO request) {
        System.out.println("--- PRIMLJEN ZAHTEV OD PSP-a ---");
        System.out.println("Merchant ID: " + request.getMerchantId());
        System.out.println("Iznos: " + request.getAmount());

        PspPaymentResponseDTO response = bankService.createPaymentUrl(request);

        System.out.println("Vraćen URL: " + response.getPaymentUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> processPayment(@Valid @RequestBody BankPaymentFormDTO paymentForm) {
        System.out.println("--- OBRADA PLAĆANJA ZA ID: " + paymentForm.getPaymentId() + " ---");

        Map<String, String> response = new HashMap<>();

        try {
            bankService.processPayment(paymentForm);
            String redirectUrl = PSP_CALLBACK_URL +
                    "?paymentId=" + paymentForm.getPaymentId() +
                    "&status=SUCCESS";

            response.put("status", "SUCCESS");
            response.put("redirectUrl", redirectUrl);

            //GLOBAL_TRANSACTION_ID (To je naš interni ID transakcije)
            response.put("GLOBAL_TRANSACTION_ID", paymentForm.getPaymentId());

            //ACQUIRER_TIMESTAMP (Trenutno vrijeme banke)
            response.put("ACQUIRER_TIMESTAMP", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("Transakcija neuspešna: " + e.getMessage());

            String redirectUrl = PSP_CALLBACK_URL +
                    "?paymentId=" + paymentForm.getPaymentId() +
                    "&status=FAILED";

            response.put("status", "FAILED");
            response.put("message", e.getMessage());
            response.put("redirectUrl", redirectUrl);
            response.put("ACQUIRER_TIMESTAMP", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/qr-initialize")
    public ResponseEntity<Map<String, String>> initializeQr(@RequestBody PspPaymentRequestDTO request) {
        // Generišemo IPS string
        String qrData = bankService.generateIpsQrString(request);

        // Čuvamo transakciju kao PENDING u bazi (isto kao za karticu)
        PspPaymentResponseDTO bankResponse = bankService.createPaymentUrl(request);

        Map<String, String> response = new HashMap<>();
        response.put("qrData", qrData);
        response.put("paymentId", bankResponse.getPaymentId());

        return ResponseEntity.ok(response);
    }

    //ENDPOINT ZA IPS SKENIRAJ (Dolazi sa mbanking.html)
    @PostMapping("/transfer")
    public ResponseEntity<?> processQrPayment(@RequestBody QrTransferRequestDTO request) {
        System.out.println("--- PRIMLJEN ZAHTEV ZA QR TRANSFER (mBanking) ---");

        try {
            String redirectUrl = bankService.processInternalTransfer(request);

            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "QR Plaćanje uspešno izvršeno!");
            response.put("redirectUrl", redirectUrl);
            response.put("GLOBAL_TRANSACTION_ID", java.util.UUID.randomUUID().toString()); // Ili izvuci iz servisa ako možeš
            response.put("ACQUIRER_TIMESTAMP", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("Greška pri QR plaćanju: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}