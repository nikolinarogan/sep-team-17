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

    // 1. OVO JE ENDPOINT KOJI GAĐAŠ IZ POSTMANA
    @PostMapping("/card")
    public ResponseEntity<PspPaymentResponseDTO> createPaymentUrl(@RequestBody PspPaymentRequestDTO request) {
        System.out.println("--- PRIMLJEN ZAHTEV OD PSP-a ---");
        System.out.println("Merchant ID: " + request.getMerchantId());
        System.out.println("Iznos: " + request.getAmount());

        PspPaymentResponseDTO response = bankService.createPaymentUrl(request);

        System.out.println("Vraćen URL: " + response.getPaymentUrl());
        return ResponseEntity.ok(response);
    }

    // 2. OVO JE ENDPOINT KOJI GAĐAŠ SA FORME (HTML)
    @PostMapping("/pay")
    public ResponseEntity<Map<String, String>> processPayment(@Valid @RequestBody BankPaymentFormDTO paymentForm) {
        System.out.println("--- OBRADA PLAĆANJA ZA ID: " + paymentForm.getPaymentId() + " ---");

        Map<String, String> response = new HashMap<>();

        try {
            // 1. Pozivamo servis da skine novac
            bankService.processPayment(paymentForm);

            // 2. Ako je sve prošlo bez greške (SUCCESS)
            // Pravimo URL na koji Frontend treba da preusmeri korisnika
            String redirectUrl = PSP_CALLBACK_URL +
                    "?paymentId=" + paymentForm.getPaymentId() +
                    "&status=SUCCESS";

            response.put("status", "SUCCESS");
            response.put("redirectUrl", redirectUrl); // <--- OVO JE KLJUČNO

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // 3. Ako je puklo (npr. Nema sredstava, Loš PIN...), hvatamo grešku
            System.out.println("Transakcija neuspešna: " + e.getMessage());

            // Čak i kad je greška, moramo vratiti korisnika na PSP sa statusom FAILED
            String redirectUrl = PSP_CALLBACK_URL +
                    "?paymentId=" + paymentForm.getPaymentId() +
                    "&status=FAILED";

            response.put("status", "FAILED");
            response.put("message", e.getMessage());
            response.put("redirectUrl", redirectUrl); // Šaljemo ga nazad da vidi grešku na WebShopu

            // Vraćamo 200 OK jer smo uspešno obradili zahtev (iako je ishod plaćanja neuspeh)
            // Frontend samo čita "redirectUrl" i radi posao.
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

    // 3. OVO JE ENDPOINT ZA IPS SKENIRAJ (Dolazi sa mbanking.html)
    @PostMapping("/transfer")
    public ResponseEntity<?> processQrPayment(@RequestBody QrTransferRequestDTO request) {
        System.out.println("--- PRIMLJEN ZAHTEV ZA QR TRANSFER (mBanking) ---");

        try {
            // 👇 IZMENA: Hvatamo URL koji servis vrati
            String redirectUrl = bankService.processInternalTransfer(request);

            // Pakujemo ga u odgovor
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "QR Plaćanje uspešno izvršeno!");
            response.put("redirectUrl", redirectUrl); // <--- ŠALJEMO GA FRONTENDU

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("Greška pri QR plaćanju: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}