package com.bank.controller;

import com.bank.dto.*;
import com.bank.service.BankService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank") // <--- OVO JE ONAJ DEO KOJI TRAŽIŠ
@CrossOrigin(origins = "*")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    // 1. OVO JE ENDPOINT KOJI GAĐAŠ IZ POSTMANA
    @PostMapping("/payment-url")
    public ResponseEntity<PspPaymentResponseDTO> createPaymentUrl(@RequestBody PspPaymentRequestDTO request) {
        System.out.println("Primljen zahtev za URL od: " + request.getMerchantId());
        PspPaymentResponseDTO response = bankService.createPaymentUrl(request);
        return ResponseEntity.ok(response);
    }

    // 2. OVO JE ENDPOINT KOJI GAĐAŠ SA FORME (HTML)
    @PostMapping("/pay")
    public ResponseEntity<String> processPayment(@Valid @RequestBody BankPaymentFormDTO paymentForm) {
        System.out.println("Primljen zahtev za plaćanje karticom: " + paymentForm.getPan());
        try {
            bankService.processPayment(paymentForm);
            return ResponseEntity.ok("Plaćanje uspešno!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}