package controller;

import dto.*;
import exception.UnknownPaymentmethodException;
import model.PaymentMethod;
import model.PaymentTransaction;
import model.TransactionStatus;
import repository.PaymentMethodRepository;
import repository.PaymentTransactionRepository;
import service.PaymentService;
import service.*;
import tools.AuditLogger; 
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
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRegistry paymentRegistry;
    private final GenericPaymentService genericPaymentService;
    private final AuditLogger auditLogger; 

    public PaymentController(PaymentService paymentService,
                              PaymentTransactionRepository paymentTransactionRepository,
                             PaymentMethodRepository paymentMethodRepository,
                             PaymentRegistry paymentRegistry,
                             GenericPaymentService genericPaymentService,
                             AuditLogger auditLogger) {
      
        this.paymentService = paymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRegistry = paymentRegistry;
        this.genericPaymentService = genericPaymentService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/init")
    public ResponseEntity<PaymentResponseDTO> initializePayment(@Valid @RequestBody PaymentRequestDTO request) {
        auditLogger.logEvent("PAYMENT_INIT_REQUEST", "START",
                "Merchant: " + request.getMerchantId() + " | OrderID: " + request.getMerchantOrderId());

        return ResponseEntity.ok(paymentService.createTransaction(request));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CheckoutResponseDTO> getCheckoutPageData(@PathVariable String uuid) {
        // Logujemo pristup checkout strani (IP klijenta se hvata automatski)
        auditLogger.logEvent("CHECKOUT_DATA_REQUEST", "SUCCESS", "UUID: " + uuid);

        return ResponseEntity.ok(paymentService.getCheckoutData(uuid));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelTransaction(@Valid @RequestBody CancelRequestDTO request) {
        auditLogger.logEvent("TRANSACTION_CANCEL_ATTEMPT", "PENDING",
                "Merchant: " + request.getMerchantId() + " | OrderID: " + request.getMerchantOrderId());

        paymentService.cancelTransactionByMerchant(
                request.getMerchantId(),
                request.getMerchantPassword(),
                request.getMerchantOrderId()
        );

        auditLogger.logEvent("TRANSACTION_CANCEL_FINISHED", "SUCCESS", "OrderID: " + request.getMerchantOrderId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{merchantId}/{merchantOrderId}")
    public ResponseEntity<PaymentStatusDTO> checkStatus(
            @PathVariable String merchantId,
            @PathVariable String merchantOrderId) {

        auditLogger.logEvent("STATUS_CHECK_POLLING", "SUCCESS",
                "Merchant: " + merchantId + " | OrderID: " + merchantOrderId);

        return ResponseEntity.ok(paymentService.checkTransactionStatus(merchantId, merchantOrderId));
    }

    /**
     * UNIVERZALNI ENDPOINT ZA POKRETANJE PLAĆANJA.
     */
    /*@PostMapping("/checkout/{uuid}/init/{methodName}")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @PathVariable String uuid,
            @PathVariable String methodName) {

        // PCI DSS: Logujemo odabir metode (Kupac bira, IP adresa identifikuje)
        auditLogger.logEvent("PAYMENT_METHOD_SELECTION", "START",
                "UUID: " + uuid + " | Method: " + methodName);

        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("INIT_FAILED_INVALID_UUID", "UUID: " + uuid);
                    return new RuntimeException("Transakcija nije pronađena: " + uuid);
                });

        try {
            PaymentMethod methodConfig = paymentMethodRepository.findByName(methodName)
                    .orElseThrow(() -> new UnknownPaymentmethodException(methodName));

            PaymentProvider provider = paymentRegistry.get(methodName);
            PaymentInitResult result = provider.initiate(tx);

            auditLogger.logEvent("PAYMENT_METHOD_INIT_SUCCESS", "SUCCESS",
                    "UUID: " + uuid + " | Redirecting to provider.");

            Map<String, Object> response = new HashMap<>();
            if (result.getRedirectUrl() != null) {
                response.put("paymentUrl", result.getRedirectUrl());
            }
            if (result.getQrData() != null) {
                response.put("qrData", result.getQrData());
            }
            return ResponseEntity.ok(response);

        } catch (UnknownPaymentmethodException e) {
            auditLogger.logSecurityAlert("UNKNOWN_METHOD_REQUEST", "Method: " + methodName);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            auditLogger.logEvent("PAYMENT_METHOD_INIT_FAILED", "ERROR",
                    "UUID: " + uuid + " | Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servis trenutno nije dostupan: " + e.getMessage(), "retryable", true));
        }
    }
*/
    @PostMapping("/checkout/{uuid}/init/{methodName}")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @PathVariable String uuid,
            @PathVariable String methodName) {

        // PCI DSS: Logujemo odabir metode (Kupac bira, IP adresa identifikuje)
        auditLogger.logEvent("PAYMENT_METHOD_SELECTION", "START",
                "UUID: " + uuid + " | Method: " + methodName);

        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("INIT_FAILED_INVALID_UUID", "UUID: " + uuid);
                    return new RuntimeException("Transakcija nije pronađena: " + uuid);
                });

        try {

            PaymentMethod methodConfig = paymentMethodRepository.findByName(methodName)
                    .orElseThrow(() -> new UnknownPaymentmethodException(methodName));

            PaymentInitResult result;

            if (methodConfig.getServiceName() != null && !methodConfig.getServiceName().isEmpty()) {
                result = genericPaymentService.initiate(tx, methodName);
            } else {
                PaymentProvider provider = paymentRegistry.get(methodName);
                result = provider.initiate(tx);
            }

            auditLogger.logEvent("PAYMENT_METHOD_INIT_SUCCESS", "SUCCESS",
                    "UUID: " + uuid + " | Redirecting to provider.");

            Map<String, Object> response = new HashMap<>();
            if (result.getRedirectUrl() != null) {
                response.put("paymentUrl", result.getRedirectUrl());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            auditLogger.logEvent("PAYMENT_METHOD_INIT_FAILED", "ERROR",
                    "UUID: " + uuid + " | Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servis trenutno nije dostupan: " + e.getMessage()));
        }
    }
    /**
     * UNIVERZALNI CALLBACK ZA MIKROSERVISE
     */
    @GetMapping("/external/capture")
    public ResponseEntity<?> captureExternal(
            @RequestParam("method") String methodName,
            @RequestParam("token") String executionToken,
            @RequestParam("uuid") String uuid) {

        auditLogger.logEvent("EXTERNAL_CAPTURE_CALLBACK", "START",
                "Method: " + methodName + " | UUID: " + uuid);

        try {
            boolean isCaptured = genericPaymentService.capture(methodName, executionToken);

            if (!isCaptured) {
                auditLogger.logEvent("CAPTURE_FAILED", "RETRY_REQUIRED", "UUID: " + uuid);
                String retryUrl = "https://localhost:4201/checkout/" + uuid + "?error=retry_method";

                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(retryUrl))
                        .build();
            }

            dto.PaymentCallbackDTO callback = new dto.PaymentCallbackDTO();
            callback.setPaymentId(uuid);
            callback.setStatus("SUCCESS");
            callback.setExecutionId(executionToken);

            String redirectUrl = paymentService.finaliseTransaction(callback, methodName);

            auditLogger.logEvent("EXTERNAL_CAPTURE_SUCCESS", "SUCCESS", "UUID: " + uuid);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();

        } catch (Exception e) {
            auditLogger.logSecurityAlert("EXTERNAL_CAPTURE_CRITICAL_FAIL",
                    "UUID: " + uuid + " | Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("https://localhost:4200/payment-failed?uuid=" + uuid))
                    .build();
        }
    }

    /**
     * Callback za BANKU (PCC)
     */
    @GetMapping("/payment-callback")
    public ResponseEntity<Void> handleBrowserCallback(@RequestParam String paymentId,
                                                      @RequestParam(required = false) String status) {
        auditLogger.logEvent("BANK_BROWSER_RETURN", status != null ? status : "FAILED", "PaymentID: " + paymentId);

        String finalStatus = (status != null) ? status : "FAILED";
        String webShopUrl = paymentService.getRedirectUrl(paymentId, finalStatus);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(webShopUrl))
                .build();
    }

    /**
     * Server-to-Server callback za BANKU (Card)
     */
    @PostMapping("/finalize")
    public ResponseEntity<String> finalizeCard(@RequestBody PaymentCallbackDTO callback) {
        auditLogger.logEvent("S2S_CARD_FINALIZE", "START", "UUID: " + callback.getPaymentId());

        String redirectUrl = paymentService.finaliseTransaction(callback, "CARD");

        auditLogger.logEvent("S2S_CARD_FINALIZE_SUCCESS", "SUCCESS", "UUID: " + callback.getPaymentId());
        return ResponseEntity.ok(redirectUrl);
    }

    /**
     * Server-to-Server callback za BANKU (QR)
     */
    @PostMapping("/finalize/qr")
    public ResponseEntity<String> finalizeQr(@RequestBody PaymentCallbackDTO callback) {
        auditLogger.logEvent("S2S_QR_FINALIZE", "START", "UUID: " + callback.getPaymentId());

        String redirectUrl = paymentService.finaliseTransaction(callback, "QR_CODE");

        auditLogger.logEvent("S2S_QR_FINALIZE_SUCCESS", "SUCCESS", "UUID: " + callback.getPaymentId());
        return ResponseEntity.ok(redirectUrl);
    }

    @GetMapping("/checkout/{uuid}/details/{methodName}") // Ili PostMapping, svejedno
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable String uuid, @PathVariable String methodName) {
        // Ovde pozivaš mikroservis da ti ponovo vrati podatke (iz keša ili baze)
        return ResponseEntity.ok(genericPaymentService.getDetails(uuid, methodName));
    }

    @GetMapping("/checkout/{uuid}/status/{methodName}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable String uuid, @PathVariable String methodName) {

        // 1. Prvo učitamo transakciju iz NAŠE baze (Core baza)
        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji"));

        Map<String, Object> response = new HashMap<>();

        // A) Ako je u našoj bazi već SUCCESS, ne smaramo mikroservis, odmah vraćamo URL
        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            response.put("status", "SUCCESS");
            response.put("redirectUrl", tx.getSuccessUrl());
            return ResponseEntity.ok(response);
        }

        // B) Ako je FAILED, isto vraćamo odmah
        if (tx.getStatus() == TransactionStatus.FAILED) {
            response.put("status", "FAILED");
            response.put("redirectUrl", tx.getFailedUrl());
            return ResponseEntity.ok(response);
        }

        // C) Ako je CREATED/PENDING, moramo pitati Mikroservis
        boolean isConfirmed = genericPaymentService.checkTransactionStatus(uuid, methodName);

        if (isConfirmed) {
            // 🎉 MIKROSERVIS KAŽE DA SU PARE LEGLE!

            // 1. Ažuriramo status u Core bazi
            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setServiceTimestamp(java.time.LocalDateTime.now());
            paymentTransactionRepository.save(tx);

            // 2. Logujemo uspeh
            auditLogger.logEvent("PAYMENT_SUCCESS_CONFIRMED", "SUCCESS", "UUID: " + uuid);

            // 3. Opciono: Obavesti WebShop preko Webhook-a (ako imaš tu metodu u servisu)
            // paymentService.notifyWebShop(tx, "CRYPTO");

            // 4. Vraćamo Frontendu URL za uspeh
            response.put("status", "SUCCESS");
            response.put("redirectUrl", tx.getSuccessUrl());
        } else {
            response.put("status", "PENDING");
            response.put("redirectUrl", null);
        }

        return ResponseEntity.ok(response);
    }
}