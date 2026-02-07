package service;

import dto.PaymentInitResult;
import model.Merchant;
import model.PaymentTransaction;
import model.TransactionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import repository.PaymentTransactionRepository;
import repository.MerchantRepository;
import tools.AuditLogger;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CardPaymentService implements PaymentProvider {

    private static final String BANK_URL = "https://localhost:8082/api/bank/card";

    private final RestTemplate restTemplate;
    private final PaymentTransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final AuditLogger auditLogger;
    private final Registration registration;

    public CardPaymentService(RestTemplate restTemplate,
                              PaymentTransactionRepository paymentTransactionRepository,
                              MerchantRepository merchantRepository,
                              AuditLogger auditLogger, @Qualifier("eurekaRegistration") Registration registration) {



        this.restTemplate = restTemplate;
        this.transactionRepository = paymentTransactionRepository;
        this.merchantRepository = merchantRepository;
        this.auditLogger = auditLogger;
        this.registration = registration;
    }

    @Override
    public String getProviderName() {
        return "CARD";
    }

    @Override
    public PaymentInitResult initiate(PaymentTransaction transaction) {
        auditLogger.logEvent("CARD_PAYMENT_INIT_START", "PENDING", "UUID: " + transaction.getUuid());
        String url = initializePayment(transaction);
        return PaymentInitResult.builder().redirectUrl(url).build();
    }

    public String initializePayment(PaymentTransaction transaction) {
        String stan = String.valueOf((int) (Math.random() * 900000) + 100000);

        transaction.setStan(stan);
        transactionRepository.save(transaction);

        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("MERCHANT_NOT_FOUND", "ID: " + transaction.getMerchantId());
                    return new RuntimeException("Prodavac nije pronađen!");
                });

        String myHost = registration.getHost();
        int myPort = registration.getPort();
        String myCallbackUrl = "https://" + myHost + ":" + myPort + "/api/payments/payment-callback";

        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", merchant.getMerchantId());
        request.put("merchantPassword", merchant.getMerchantPassword());
        request.put("amount", transaction.getAmount());
        request.put("currency", transaction.getCurrency());
        request.put("pspTransactionId", transaction.getUuid());
        request.put("pspTimestamp", LocalDateTime.now());
        request.put("stan", stan);
        request.put("callbackUrl", myCallbackUrl);

        int maxAttempts = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // PCI DSS 10.2.4: Beleženje pokušaja komunikacije sa bankom
                auditLogger.logEvent("BANK_COMMUNICATION_ATTEMPT", "RETRY",
                        "Attempt: " + attempt + " | UUID: " + transaction.getUuid());

                ResponseEntity<Map> response = this.restTemplate.postForEntity(BANK_URL, request, Map.class);
                Map<String, Object> body = response.getBody();

                if (body != null && body.containsKey("paymentUrl")) {
                    String url = body.get("paymentUrl").toString();

                    if (body.containsKey("paymentId")) {
                        String bankPaymentId = body.get("paymentId").toString();
                        transaction.setExecutionId(bankPaymentId);
                        transactionRepository.save(transaction);
                    }

                    auditLogger.logEvent("BANK_COMMUNICATION_SUCCESS", "SUCCESS", "URL received for UUID: " + transaction.getUuid());
                    return url;
                }

                throw new RuntimeException("Banka nije vratila paymentUrl!");

            } catch (Exception e) {
                auditLogger.logEvent("BANK_COMMUNICATION_ERROR", "ERROR",
                        "Attempt " + attempt + " failed: " + e.getMessage());

                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        long delayMs = 1000L * (1 << (attempt - 1));
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry prekinut.", ie);
                    }
                }
            }
        }

        auditLogger.logSecurityAlert("BANK_UNAVAILABLE", "Failed to reach bank after " + maxAttempts + " attempts for UUID: " + transaction.getUuid());
        throw new RuntimeException("Greška nakon " + maxAttempts + " pokušaja.");
    }

    public String handleCallback(String bankPaymentId, String status) {
        // Beleženje povratnog poziva od banke (PCI DSS 10.2.1)
        auditLogger.logEvent("BANK_CALLBACK_RECEIVED", status, "BankPaymentID: " + bankPaymentId);

        PaymentTransaction tx = transactionRepository.findByExecutionId(bankPaymentId)
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("UNKNOWN_BANK_CALLBACK", "BankPaymentID: " + bankPaymentId);
                    return new RuntimeException("Nepoznata transakcija!");
                });

        if ("SUCCESS".equals(status)) {
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);
            auditLogger.logEvent("TRANSACTION_STATUS_UPDATE", "SUCCESS", "UUID: " + tx.getUuid());
            return tx.getSuccessUrl();
        } else {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logEvent("TRANSACTION_STATUS_UPDATE", "FAILED", "UUID: " + tx.getUuid());
            return tx.getFailedUrl();
        }
    }
}