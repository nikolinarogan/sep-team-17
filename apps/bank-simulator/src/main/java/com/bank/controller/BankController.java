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
    public ResponseEntity<?> processPayment(@Valid @RequestBody BankPaymentFormDTO paymentForm) {
        System.out.println("--- PRIMLJEN ZAHTEV ZA PLAĆANJE (KARTICA) ---");
        System.out.println("Payment ID: " + paymentForm.getPaymentId());
        System.out.println("Maskiran PAN: **** **** **** " + paymentForm.getPan().substring(paymentForm.getPan().length() - 4));

        try {
            bankService.processPayment(paymentForm);

            // Vraćamo JSON odgovor da je sve OK
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Plaćanje uspešno izvršeno!");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Ako je odbijeno (nema para, loš CVV...), vraćamo grešku 400
            System.out.println("Greška pri plaćanju: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}