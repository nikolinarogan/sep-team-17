package controller;

import dto.*;
import model.PaymentTransaction;
import repository.PaymentTransactionRepository;
import service.CardPaymentService;
import service.PaymentService;
import service.PaypalService;
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
    private final PaypalService paypalService;

    public PaymentController(PaymentService paymentService,
                             CardPaymentService cardPaymentService,
                             QrPaymentService qrPaymentService,
                             PaymentTransactionRepository paymentTransactionRepository,
                             PaypalService paypalService) {
        this.paymentService = paymentService;
        this.cardPaymentService = cardPaymentService;
        this.qrPaymentService = qrPaymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paypalService = paypalService;
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

    @GetMapping("/status/{merchantId}/{merchantOrderId}")
    public ResponseEntity<PaymentStatusDTO> checkStatus(
            @PathVariable String merchantId,
            @PathVariable String merchantOrderId) {
        return ResponseEntity.ok(paymentService.checkTransactionStatus(merchantId, merchantOrderId));
    }

    /**
     * Endpoint koji Frontend poziva kada korisnik klikne na PayPal dugme.
     * Odgovara serviceUrl-u: /api/payments/paypal/checkout/{uuid}
     */
    @PostMapping("/paypal/checkout/{uuid}")
    public ResponseEntity<Map<String, String>> initiatePaypal(@PathVariable String uuid) {
        // 1. Pronađi transakciju u bazi koju je Web Shop inicijalizovao
        PaymentTransaction tx = paymentTransactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena: " + uuid));

        // 2. Pozovi PaypalService da kreira Order na PayPal-u
        // Ova metoda će vratiti URL na koji korisnik treba da ode da se uloguje
        String approvalUrl = paypalService.initializePayment(tx);

        // 3. Vrati taj URL frontendu kako bi on mogao da uradi redirect
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", approvalUrl);

        return ResponseEntity.ok(response);
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
}