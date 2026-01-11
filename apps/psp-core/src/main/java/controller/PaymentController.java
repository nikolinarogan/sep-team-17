    package controller;

import dto.CheckoutResponseDTO;
import dto.PaymentRequestDTO;
import dto.PaymentResponseDTO;
import model.PaymentTransaction;
import repository.PaymentTransactionRepository;
import service.CardPaymentService;
import service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.QrPaymentService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "https://localhost:4201", allowedHeaders = "*", allowCredentials = "true")
public class PaymentController {

    private final PaymentService paymentService;
    private final CardPaymentService cardPaymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final QrPaymentService qrPaymentService;

    public PaymentController(PaymentService paymentService, CardPaymentService cardPaymentService, PaymentTransactionRepository paymentTransactionRepository
    , QrPaymentService qrPaymentService) {
        this.paymentService = paymentService;
        this.cardPaymentService = cardPaymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.qrPaymentService = qrPaymentService;
    }

        /**
         * KORAK 1: Inicijalizacija plaćanja
         * ---------------------------------------------------------
         * Zove Web Shop
         * Kada korisnik na sajtu prodavca klikne "Plati"
         * ULAZ: Iznos, MerchantID, OrderID...
         * IZLAZ: URL ka PSP Checkout stranici (npr. http://localhost:4201/checkout/uuid...)
         */
        @PostMapping("/init")
        public ResponseEntity<PaymentResponseDTO> initializePayment(@Valid @RequestBody PaymentRequestDTO request) {
            PaymentResponseDTO response = paymentService.createTransaction(request);
            return ResponseEntity.ok(response);
        }

        /**
         * KORAK 2: Podaci za Checkout stranicu
         * ---------------------------------------------------------
         * Zove PSP Frontend
         * Kada se korisniku otvori link iz prethodnog koraka
         * ULAZ: UUID transakcije (iz URL-a)
         * IZLAZ: Iznos, valuta i dostupne metode plaćanja (npr. ["CARD", "QR"])
         */
        @GetMapping("/{uuid}")
        public ResponseEntity<CheckoutResponseDTO> getCheckoutPageData(@PathVariable String uuid) {
            CheckoutResponseDTO data = paymentService.getCheckoutData(uuid);
            return ResponseEntity.ok(data);
        }

    /**
     * CALLBACK ENDPOINT
     * Ovdje gađaju Bank Service i QR Service kad završe posao
     */
    @PutMapping("/status")
    public ResponseEntity<Void> updateStatus(@RequestBody dto.PaymentCallbackDTO callback) {
        paymentService.finaliseTransaction(callback);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout/{uuid}/card")
    public ResponseEntity<?> initCardPayment(@PathVariable String uuid) {
        // 1. Nađi transakciju u bazi
        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji"));

        // 2. Pozovi servis da kontaktira Banku
        String bankUrl = cardPaymentService.initializePayment(tx);

        // 3. Vrati URL Angularu
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", bankUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{uuid}/qr")
    public ResponseEntity<?> initQrPayment(@PathVariable String uuid) {
        // 1. Nađi transakciju
        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji"));

        // 2. Pozovi servis da dobiješ validan NBS string od banke
        String qrData = qrPaymentService.getIpsQrData(tx);

        // 3. Vrati string Angularu (Angular će od ovoga napraviti sliku)
        Map<String, String> response = new HashMap<>();
        response.put("qrData", qrData);
        return ResponseEntity.ok(response);
    }
}
