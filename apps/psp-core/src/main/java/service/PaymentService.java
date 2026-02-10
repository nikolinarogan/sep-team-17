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
import tools.AuditLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final AuditLogger auditLogger;

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

        // --- 1. PROMENA: PAMETNA PROVERA POSTOJEĆE TRANSAKCIJE (IDEMPOTENCIJA) ---
        Optional<PaymentTransaction> existingTx = transactionRepository
                .findByMerchantIdAndMerchantOrderId(request.getMerchantId(), request.getMerchantOrderId());

        if (existingTx.isPresent()) {
            PaymentTransaction tx = existingTx.get();

            // SCENARIO A: Transakcija je već uspešna (korisnik ili WebShop je izgubio info)
            if (tx.getStatus() == TransactionStatus.SUCCESS) {
                auditLogger.logEvent("DUPLICATE_PAYMENT_PREVENTED", "SUCCESS",
                        "Order " + request.getMerchantOrderId() + " already PAID. Redirecting to success.");
                // Odmah vraćamo uspeh - ne pravimo novu transakciju
                return new PaymentResponseDTO(tx.getSuccessUrl(), tx.getUuid());
            }

            // SCENARIO B: Transakcija je započeta, ali nije završena (Pending/Created)
            if (tx.getStatus() == TransactionStatus.CREATED) {
                auditLogger.logEvent("TRANSACTION_RESUME", "PENDING",
                        "Resuming existing transaction: " + tx.getUuid());

                String urlTemplate = pspConfigRepository.findByConfigName("PAYMENT_LINK_TEMPLATE")
                        .map(PspConfig::getConfigValue)
                        .orElseThrow(() -> new RuntimeException("Config missing"));

                // Vraćamo isti link da nastavi gde je stao
                return new PaymentResponseDTO(urlTemplate.replace("{uuid}", tx.getUuid()), tx.getUuid());
            }

            // SCENARIO C: Stara je propala (FAILED), dozvoljavamo novu (nastavljamo kod ispod)
            auditLogger.logEvent("RETRYING_FAILED_TRANSACTION", "INFO", "Order: " + request.getMerchantOrderId());
        }

        // --- KREIRANJE NOVE TRANSAKCIJE (Samo ako ne postoji ili je prethodna FAILED) ---

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

        // Idempotencija: Ako je već SUCCESS, ne menjaj ništa, samo javi Shopu opet (za svaki slučaj)
        if (oldStatus == TransactionStatus.SUCCESS) {
            notifyWebShop(tx, paymentMethod);
            return tx.getSuccessUrl();
        }

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
            // Ne bacamo grešku ovde da ne bi prekinuli flow korisniku. Scheduled task će popraviti ovo.
        }

        return (tx.getStatus() == TransactionStatus.SUCCESS) ? tx.getSuccessUrl() : tx.getFailedUrl();
    }

    // --- 2. PROMENA: POSEBNA METODA ZA RETRY (Da bismo je mogli zvati iz Scheduled taska) ---
    public void retryWebhook(PaymentTransaction tx) {
        String method = tx.getChosenMethod() != null ? tx.getChosenMethod() : "UNKNOWN";
        try {
            notifyWebShop(tx, method);
        } catch (Exception e) {
            // Ignorišemo grešku jer je ovo background task
        }
    }

    private void notifyWebShop(PaymentTransaction tx, String paymentMethod) {
        Merchant merchant = merchantRepository.findByMerchantId(tx.getMerchantId()).orElseThrow();
        String targetUrl = tx.getStatus() == TransactionStatus.SUCCESS ? merchant.getWebShopUrl() + "/success" : merchant.getWebShopUrl() + "/failed";

        PaymentStatusDTO statusDTO = new PaymentStatusDTO(tx.getMerchantOrderId(), tx.getUuid(), paymentMethod, tx.getStatus().toString(), LocalDateTime.now());

        // Pokušavamo odmah, ali ako ne uspe, Scheduled task "syncWebhooks" će pokušati kasnije
        try {
            auditLogger.logEvent("WEBHOOK_SENDING", "ATTEMPT", "OrderID: " + tx.getMerchantOrderId());
            restTemplate.postForEntity(targetUrl, statusDTO, Void.class);
            auditLogger.logEvent("WEBHOOK_SENT", "SUCCESS", "OrderID: " + tx.getMerchantOrderId());
        } catch (Exception e) {
            auditLogger.logEvent("WEBHOOK_FAIL_IMMEDIATE", "WARNING", "Will retry later. OrderID: " + tx.getMerchantOrderId());
            throw e; // Prosleđujemo grešku da bi pozivalac znao
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

    @Scheduled(fixedRate = 300000) // Svakih 5 minuta
    @Transactional
    public void syncWebhooksAndExpire() {

        // POKUŠAJ PONOVO DA POŠALJEŠ WEBHOOK ZA "SUCCESS" TRANSAKCIJE
        // (Uzimamo one koje su se desile u poslednjih 15 min da ne spamujemo stare)

        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        List<PaymentTransaction> recentSuccess = transactionRepository
                .findByStatusAndCreatedAtAfter(TransactionStatus.SUCCESS, fifteenMinutesAgo);
        for (PaymentTransaction tx : recentSuccess) {
            auditLogger.logEvent("WEBHOOK_RETRY_CRON", "START", "Checking WebShop sync for: " + tx.getMerchantOrderId());
            retryWebhook(tx);
        }

        // ČIŠĆENJE NAPUŠTENIH
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