package service;

import dto.*;
import model.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import repository.MerchantRepository;
import repository.MerchantSubscriptionRepository;
import repository.PaymentTransactionRepository;
import repository.PspConfigRepository;
import tools.AuditLogger; // Dodato

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final MerchantRepository merchantRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final MerchantSubscriptionRepository subscriptionRepository;
    private final PspConfigRepository pspConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final AuditLogger auditLogger; // Dodato

    public PaymentService(MerchantRepository merchantRepository,
                          PaymentTransactionRepository transactionRepository,
                          MerchantSubscriptionRepository subscriptionRepository,
                          PspConfigRepository pspConfigRepository,
                          PasswordEncoder passwordEncoder,
                          RestTemplate restTemplate,
                          AuditLogger auditLogger) {
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pspConfigRepository = pspConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public PaymentResponseDTO createTransaction(PaymentRequestDTO request) {
        auditLogger.logEvent("TRANSACTION_INIT_ATTEMPT", "PENDING", "Merchant: " + request.getMerchantId());

        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("INVALID_MERCHANT", "Merchant not found: " + request.getMerchantId());
                    return new RuntimeException("Prodavac sa ID-jem " + request.getMerchantId() + " ne postoji.");
                });

        if (!passwordEncoder.matches(request.getMerchantPassword(), merchant.getMerchantPassword())) {
            auditLogger.logSecurityAlert("AUTH_FAILED", "Invalid password for merchant: " + request.getMerchantId());
            throw new RuntimeException("Pogrešna lozinka za prodavca.");
        }

        if (transactionRepository.existsByMerchantIdAndMerchantOrderId(request.getMerchantId(), request.getMerchantOrderId())) {
            auditLogger.logEvent("DUPLICATE_ORDER", "REJECTED", "Order ID exists: " + request.getMerchantOrderId());
            throw new RuntimeException("Transakcija sa Order ID: " + request.getMerchantOrderId() + " već postoji!");
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setUuid(UUID.randomUUID().toString());
        tx.setMerchantId(request.getMerchantId());
        tx.setMerchantOrderId(request.getMerchantOrderId());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setMerchantTimestamp(request.getMerchantTimestamp());
        tx.setStatus(TransactionStatus.CREATED);
        tx.setSuccessUrl(request.getSuccessUrl());
        tx.setFailedUrl(request.getFailedUrl());
        tx.setErrorUrl(request.getErrorUrl());

        transactionRepository.save(tx);
        auditLogger.logEvent("TRANSACTION_CREATED", "SUCCESS", "UUID: " + tx.getUuid());

        String urlTemplate = pspConfigRepository.findByConfigName("PAYMENT_LINK_TEMPLATE")
                .map(PspConfig::getConfigValue)
                .orElseThrow(() -> new RuntimeException("Sistemska greška: Nedostaje PAYMENT_LINK_TEMPLATE konfiguracija!"));

        return new PaymentResponseDTO(urlTemplate.replace("{uuid}", tx.getUuid()), tx.getUuid());
    }

    public CheckoutResponseDTO getCheckoutData(String uuid) {
        auditLogger.logEvent("CHECKOUT_DATA_ACCESS", "SUCCESS", "UUID: " + uuid);

        PaymentTransaction tx = transactionRepository.findByUuid(uuid)
                .or(() -> transactionRepository.findByExecutionId(uuid))
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("CHECKOUT_NOT_FOUND", "UUID: " + uuid);
                    return new RuntimeException("Transakcija nije pronađena: " + uuid);
                });

        List<MerchantSubscription> subscriptions = subscriptionRepository.findByMerchantMerchantId(tx.getMerchantId());

        List<PaymentMethodDTO> availableMethods = subscriptions.stream()
                .map(sub -> new PaymentMethodDTO(sub.getPaymentMethod().getName(), sub.getPaymentMethod().getServiceUrl()))
                .collect(Collectors.toList());

        return new CheckoutResponseDTO(tx.getAmount(), tx.getCurrency(), tx.getMerchantId(), availableMethods);
    }

    @Transactional
    public String finaliseTransaction(dto.PaymentCallbackDTO callback, String paymentMethod) {
        auditLogger.logEvent("FINALISING_TRANSACTION", "START", "UUID: " + callback.getPaymentId());

        PaymentTransaction tx = transactionRepository.findByUuid(callback.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena."));

        TransactionStatus oldStatus = tx.getStatus();
        try {
            tx.setStatus(TransactionStatus.valueOf(callback.getStatus()));
        } catch (Exception e) {
            tx.setStatus(TransactionStatus.ERROR);
        }

        tx.setExternalTransactionId(callback.getExternalTransactionId());
        tx.setExecutionId(callback.getExecutionId());
        tx.setServiceTimestamp(callback.getServiceTimestamp());
        tx.setChosenMethod(paymentMethod);

        transactionRepository.save(tx);
        auditLogger.logEvent("STATUS_UPDATE", tx.getStatus().toString(), "Old: " + oldStatus + " | UUID: " + tx.getUuid());

        try {
            notifyWebShop(tx, paymentMethod);
        } catch (Exception e) {
            auditLogger.logEvent("WEBHOOK_NOTIFICATION_FAILED", "ERROR", "UUID: " + tx.getUuid());
        }

        return (tx.getStatus() == TransactionStatus.SUCCESS) ? tx.getSuccessUrl() : tx.getFailedUrl();
    }

    private void notifyWebShop(PaymentTransaction tx, String paymentMethod) {
        Merchant merchant = merchantRepository.findByMerchantId(tx.getMerchantId()).orElseThrow();
        String targetUrl = tx.getStatus() == TransactionStatus.SUCCESS ? merchant.getWebShopUrl() + "/success" : merchant.getWebShopUrl() + "/failed";

        PaymentStatusDTO statusDTO = new PaymentStatusDTO(tx.getMerchantOrderId(), tx.getUuid(), paymentMethod, tx.getStatus().toString(), LocalDateTime.now());

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                auditLogger.logEvent("WEBHOOK_SENDING", "ATTEMPT_" + attempt, "OrderID: " + tx.getMerchantOrderId());
                restTemplate.postForEntity(targetUrl, statusDTO, Void.class);
                auditLogger.logEvent("WEBHOOK_SENT", "SUCCESS", "OrderID: " + tx.getMerchantOrderId());
                return;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    auditLogger.logSecurityAlert("WEBHOOK_PERMANENT_FAIL", "OrderID: " + tx.getMerchantOrderId());
                }
            }
        }
    }

    public String getRedirectUrl(String bankPaymentId, String statusFromBank) {
        auditLogger.logEvent("BANK_REDIRECT_PROCESS", "START", "BankID: " + bankPaymentId);

        PaymentTransaction tx = transactionRepository.findByExecutionId(bankPaymentId)
                .or(() -> transactionRepository.findByUuid(bankPaymentId))
                .orElseThrow(() -> new RuntimeException("Nepoznata transakcija: " + bankPaymentId));

        if ("SUCCESS".equalsIgnoreCase(statusFromBank)) {
            if (tx.getStatus() != TransactionStatus.SUCCESS) {
                tx.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(tx);
                auditLogger.logEvent("BANK_STATUS_UPDATE", "SUCCESS", "UUID: " + tx.getUuid());
                try { notifyWebShop(tx, "CARD"); } catch (Exception e) {}
            }
            return tx.getSuccessUrl();
        } else {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logEvent("BANK_STATUS_UPDATE", "FAILED", "UUID: " + tx.getUuid());
            try { notifyWebShop(tx, "CARD"); } catch (Exception e) {}
            return tx.getFailedUrl();
        }
    }

    public PaymentStatusDTO checkTransactionStatus(String merchantId, String merchantOrderId) {
        auditLogger.logEvent("STATUS_POLLING", "SUCCESS", "Merchant: " + merchantId + " | Order: " + merchantOrderId);
        PaymentTransaction tx = transactionRepository.findByMerchantIdAndMerchantOrderId(merchantId, merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji"));

        return new PaymentStatusDTO(tx.getMerchantOrderId(), tx.getUuid(), tx.getChosenMethod() != null ? tx.getChosenMethod() : "Unknown", tx.getStatus().toString(), LocalDateTime.now());
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void expireAbandonedTransactions() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        List<PaymentTransaction> abandoned = transactionRepository.findByStatusAndCreatedAtBefore(TransactionStatus.CREATED, thirtyMinutesAgo);

        if(!abandoned.isEmpty()) {
            auditLogger.logEvent("CRON_EXPIRATION", "START", "Count: " + abandoned.size());
            for (PaymentTransaction tx : abandoned) {
                tx.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(tx);
                auditLogger.logEvent("AUTO_EXPIRE", "FAILED", "UUID: " + tx.getUuid());
                try { notifyWebShop(tx, "UNKNOWN"); } catch (Exception e) {}
            }
        }
    }

    @Transactional
    public void cancelTransactionByMerchant(String merchantId, String merchantPassword, String merchantOrderId) {
        auditLogger.logEvent("MERCHANT_CANCEL", "START", "Order: " + merchantOrderId);

        Merchant merchant = merchantRepository.findByMerchantId(merchantId).orElseThrow();
        if (!passwordEncoder.matches(merchantPassword, merchant.getMerchantPassword())) {
            auditLogger.logSecurityAlert("CANCEL_AUTH_FAIL", "Merchant: " + merchantId);
            throw new RuntimeException("Pogrešna lozinka.");
        }

        PaymentTransaction tx = transactionRepository.findByMerchantIdAndMerchantOrderId(merchantId, merchantOrderId).orElseThrow();
        if (tx.getStatus() == TransactionStatus.CREATED) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logEvent("CANCEL_SUCCESS", "SUCCESS", "UUID: " + tx.getUuid());
        }
    }

}