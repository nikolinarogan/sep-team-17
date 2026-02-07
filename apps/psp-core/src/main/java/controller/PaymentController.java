package controller;

import dto.*;
import exception.UnknownPaymentmethodException;
import model.PaymentMethod;
import model.PaymentTransaction;
import repository.PaymentMethodRepository;
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
    private final PaymentMethodRepository paymentMethodRepository;

    private final PaymentRegistry paymentRegistry;
    private final GenericPaymentService genericPaymentService;

    public PaymentController(PaymentService paymentService,
                             PaymentTransactionRepository paymentTransactionRepository,
                             PaymentMethodRepository paymentMethodRepository,
                             PaymentRegistry paymentRegistry,
                             GenericPaymentService genericPaymentService) {
        this.paymentService = paymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRegistry = paymentRegistry;
        this.genericPaymentService = genericPaymentService;
    }

    @PostMapping("/init")
    public ResponseEntity<PaymentResponseDTO> initializePayment(@Valid @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.createTransaction(request));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CheckoutResponseDTO> getCheckoutPageData(@PathVariable String uuid) {
        return ResponseEntity.ok(paymentService.getCheckoutData(uuid));
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

    @GetMapping("/status/{merchantId}/{merchantOrderId}")
    public ResponseEntity<PaymentStatusDTO> checkStatus(
            @PathVariable String merchantId,
            @PathVariable String merchantOrderId) {
        return ResponseEntity.ok(paymentService.checkTransactionStatus(merchantId, merchantOrderId));
    }

    /**
     * UNIVERZALNI ENDPOINT ZA POKRETANJE PLAĆANJA.
     */
    @PostMapping("/checkout/{uuid}/init/{methodName}")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @PathVariable String uuid,
            @PathVariable String methodName) {

        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena: " + uuid));

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

            Map<String, Object> response = new HashMap<>();
            if (result.getRedirectUrl() != null) {
                response.put("paymentUrl", result.getRedirectUrl());
            }
            if (result.getQrData() != null) {
                response.put("qrData", result.getQrData());
            }
            return ResponseEntity.ok(response);

        } catch (UnknownPaymentmethodException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servis trenutno nije dostupan: " + e.getMessage(), "retryable", true));
        }
    }

    /**
     * UNIVERZALNI CALLBACK ZA MIKROSERVISE (PayPal, Crypto...)
     * Mikroservisi vraćaju korisnika ovde nakon plaćanja.
     * URL primjer: /api/payments/external/capture?method=CRYPTO&token=xyz&uuid=...
     */
    @GetMapping("/external/capture")
    public ResponseEntity<?> captureExternal(
            @RequestParam("method") String methodName,
            @RequestParam("token") String executionToken,
            @RequestParam("uuid") String uuid) {

        try {
            boolean isCaptured = genericPaymentService.capture(methodName, executionToken);

            if (!isCaptured) {
                System.out.println("CAPTURE NIJE USPEO - VRAĆAM KORISNIKA NA IZBOR METODE");

                String retryUrl = "https://localhost:4201/checkout/" + uuid + "?error=retry_method";

                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(retryUrl))
                        .build();
            }

            // 2. Ako je capture USPEO, onda završavamo regularno
            dto.PaymentCallbackDTO callback = new dto.PaymentCallbackDTO();
            callback.setPaymentId(uuid);
            callback.setStatus("SUCCESS");
            callback.setExecutionId(executionToken);

            String redirectUrl = paymentService.finaliseTransaction(callback, methodName);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();

        } catch (Exception e) {
            // U slučaju totalne katastrofe (npr. pukla baza), ipak ide na failed
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
        String redirectUrl = paymentService.finaliseTransaction(callback, "CARD");
        return ResponseEntity.ok(redirectUrl);
    }

    /**
     * Server-to-Server callback za BANKU (QR)
     */
    @PostMapping("/finalize/qr")
    public ResponseEntity<String> finalizeQr(@RequestBody PaymentCallbackDTO callback) {
        String redirectUrl = paymentService.finaliseTransaction(callback, "QR_CODE");
        return ResponseEntity.ok(redirectUrl);
    }
}