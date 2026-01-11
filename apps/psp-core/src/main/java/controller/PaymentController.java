package controller;

import dto.*;
import model.PaymentTransaction;
import repository.PaymentTransactionRepository;
import service.CardPaymentService;
import service.PaymentService;
import service.QrPaymentService;
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
    private final CardPaymentService cardPaymentService;
    private final QrPaymentService qrPaymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentController(PaymentService paymentService,
                             CardPaymentService cardPaymentService,
                             QrPaymentService qrPaymentService,
                             PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentService = paymentService;
        this.cardPaymentService = cardPaymentService;
        this.qrPaymentService = qrPaymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
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

    @PostMapping("/checkout/{uuid}/card")
    public ResponseEntity<?> initCardPayment(@PathVariable String uuid) {
        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Nema transakcije"));

        String bankUrl = cardPaymentService.initializePayment(tx);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", bankUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{uuid}/qr")
    public ResponseEntity<?> initQrPayment(@PathVariable String uuid) {
        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Nema transakcije"));

        String qrData = qrPaymentService.getIpsQrData(tx);

        Map<String, String> response = new HashMap<>();
        response.put("qrData", qrData);
        return ResponseEntity.ok(response);
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
}