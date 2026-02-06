package controller;

import dto.*;
import exception.UnknownPaymentmethodException;
import model.PaymentTransaction;
import repository.PaymentTransactionRepository;
import service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaypalService paypalService;
    private final PaymentRegistry paymentRegistry;

    public PaymentController(PaymentService paymentService,
                             PaymentTransactionRepository paymentTransactionRepository,
                             PaypalService paypalService,
                             PaymentRegistry paymentRegistry) {
        this.paymentService = paymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paypalService = paypalService;
        this.paymentRegistry = paymentRegistry;
    }

    @PostMapping("/init")
    public ResponseEntity<PaymentResponseDTO> initializePayment(@Valid @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.createTransaction(request));
    }

    @GetMapping("/payment-callback")
    public ResponseEntity<Void> handleBrowserCallback(@RequestParam String paymentId,
                                                      @RequestParam(required = false) String status) { // <--- Dodali smo status

        System.out.println("--- BROWSER SE VRATIO IZ BANKE ---");
        System.out.println("ID: " + paymentId);
        System.out.println("STATUS: " + status);

        String finalStatus = (status != null) ? status : "FAILED";

        // Šaljemo status u servis
        String webShopUrl = paymentService.getRedirectUrl(paymentId, finalStatus);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(webShopUrl))
                .build();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CheckoutResponseDTO> getCheckoutPageData(@PathVariable String uuid) {
        return ResponseEntity.ok(paymentService.getCheckoutData(uuid));
    }

    @PostMapping("/finalize")
    public ResponseEntity<String> finalizeCard(@RequestBody PaymentCallbackDTO callback) {
        System.out.println("Stigao odgovor od Banke (Server-to-Server)!");
        String redirectUrl = paymentService.finaliseTransaction(callback, "CARD");
        return ResponseEntity.ok(redirectUrl);
    }

    @PostMapping("/finalize/qr")
    public ResponseEntity<String> finalizeQr(@RequestBody PaymentCallbackDTO callback) {
        System.out.println("Stigao odgovor za QR (Server-to-Server)!");
        String redirectUrl = paymentService.finaliseTransaction(callback, "QR_CODE");
        return ResponseEntity.ok(redirectUrl);
    }

    @GetMapping("/status/{merchantId}/{merchantOrderId}")
    public ResponseEntity<PaymentStatusDTO> checkStatus(
            @PathVariable String merchantId,
            @PathVariable String merchantOrderId) {
        return ResponseEntity.ok(paymentService.checkTransactionStatus(merchantId, merchantOrderId));
    }

    @GetMapping("/paypal/capture")
    public ResponseEntity<Void> capturePaypal(@RequestParam("token") String paypalOrderId, @RequestParam("uuid") String uuid) {
        // 1. Izvrši capture na PayPal-u (stvarno povlačenje novca)
        boolean isCaptured = paypalService.captureOrder(paypalOrderId);

        // 2. Pripremi callback za centralnu logiku (PaymentService)
        dto.PaymentCallbackDTO callback = new dto.PaymentCallbackDTO();
        callback.setPaymentId(uuid); // Naš interni UUID
        callback.setStatus(isCaptured ? "SUCCESS" : "FAILED");
        callback.setExecutionId(paypalOrderId); // PayPal ID

        // 3. Pozovi tvoj centralni servis koji:
        //    - Ažurira bazu PSP-a
        //    - Obaveštava Web Shop preko Webhooka (RETRY mehanizam je već tamo!)
        //    - Vraća URL Web Shopa (success ili failed stranu)
        String redirectUrl = paymentService.finaliseTransaction(callback, "PAYPAL");

        // 4. Preusmeri kupca nazad na Web Shop
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelTransaction(@Valid @RequestBody CancelRequestDTO request) {
        paymentService.cancelTransactionByMerchant(
                request.getMerchantId(),
                request.getMerchantPassword(),
                request.getMerchantOrderId()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Generički endpoint za inicijalizaciju plaćanja bilo kojom metodom.
     * Nova metoda = novi PaymentProvider bean, bez izmene ovog kontrolera.
     */
    @PostMapping("/checkout/{uuid}/init/{methodName}")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @PathVariable String uuid,
            @PathVariable String methodName) {

        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena: " + uuid));

        try {
            PaymentProvider provider = paymentRegistry.get(methodName);
            PaymentInitResult result = provider.initiate(tx);

            Map<String, Object> response = new HashMap<>();
            if (result.getRedirectUrl() != null) {
                response.put("paymentUrl", result.getRedirectUrl());
            }
            if (result.getQrData() != null) {
                response.put("qrData", result.getQrData());
            }
            return ResponseEntity.ok(response);
        } catch (UnknownPaymentmethodException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Pad providera – PSP ostaje upaljen
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Metoda plaćanja trenutno nije dostupna.",
                            "retryable", true
                    ));
        }
    }
}