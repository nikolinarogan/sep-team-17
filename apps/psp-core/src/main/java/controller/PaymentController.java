    package controller;

    import dto.CheckoutResponseDTO;
    import dto.PaymentRequestDTO;
    import dto.PaymentResponseDTO;
    import service.PaymentService;
    import jakarta.validation.Valid;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/payments")
    @CrossOrigin(origins = "*")
    public class PaymentController {

        private final PaymentService paymentService;

        public PaymentController(PaymentService paymentService) {
            this.paymentService = paymentService;
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
    }