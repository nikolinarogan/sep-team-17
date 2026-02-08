package service;

import dto.MerchantConfigDTO;
import dto.MerchantCreateDTO;
import dto.MerchantCredentialsDTO;
import model.Merchant;
import model.MerchantSubscription;
import model.PaymentMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.MerchantRepository;
import repository.MerchantSubscriptionRepository;
import repository.PaymentMethodRepository;
import tools.jackson.databind.ObjectMapper;
import tools.AuditLogger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantSubscriptionRepository subscriptionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    public MerchantService(MerchantRepository merchantRepository,
                           MerchantSubscriptionRepository subscriptionRepository,
                           PaymentMethodRepository paymentMethodRepository,
                           ObjectMapper objectMapper,
                           PasswordEncoder passwordEncoder,
                           AuditLogger auditLogger) {
        this.merchantRepository = merchantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }

    /**
     * Ažuriranje pretplate za prodavca
     */
    @Transactional
    public void updateMerchantServices(String merchantId, String password, List<MerchantConfigDTO> configs) {
        auditLogger.logEvent("MERCHANT_CONFIG_UPDATE_ATTEMPT", "PENDING", "Merchant: " + merchantId);

        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("INVALID_MERCHANT_ACCESS", "Attempted config update for non-existent merchant: " + merchantId);
                    return new RuntimeException("Prodavac ne postoji: " + merchantId);
                });

        if (!passwordEncoder.matches(password, merchant.getMerchantPassword())) {
            auditLogger.logSecurityAlert("MERCHANT_AUTH_FAILED", "Invalid password for config update. Merchant: " + merchantId);
            throw new RuntimeException("Neispravna lozinka! Nemate pravo izmjene servisa.");
        }

        for (MerchantConfigDTO config : configs) {
            PaymentMethod method = paymentMethodRepository.findByName(config.getMethodName())
                    .orElseThrow(() -> new RuntimeException("Nepoznat metod plaćanja: " + config.getMethodName()));

            String credentialsJson;
            try {
                credentialsJson = objectMapper.writeValueAsString(config.getCredentials());
            } catch (Exception e) {
                throw new RuntimeException("Greška pri obradi kredencijala za " + config.getMethodName());
            }

            Optional<MerchantSubscription> existingSub = subscriptionRepository
                    .findByMerchantMerchantIdAndPaymentMethodName(merchantId, config.getMethodName());

            if (existingSub.isPresent()) {
                MerchantSubscription sub = existingSub.get();
                sub.setCredentialsJson(credentialsJson);
                subscriptionRepository.save(sub);
                auditLogger.logEvent("SUBSCRIPTION_UPDATED", "SUCCESS", "Method: " + config.getMethodName() + " for Merchant: " + merchantId);
            } else {
                MerchantSubscription newSub = new MerchantSubscription();
                newSub.setMerchant(merchant);
                newSub.setPaymentMethod(method);
                newSub.setCredentialsJson(credentialsJson);
                subscriptionRepository.save(newSub);
                auditLogger.logEvent("SUBSCRIPTION_CREATED", "SUCCESS", "Method: " + config.getMethodName() + " for Merchant: " + merchantId);
            }
        }
    }

    /**
     * Metoda za brisanje (odjavu) servisa
     */
    @Transactional
    public void removeMerchantService(String merchantId, String methodName) {
        auditLogger.logEvent("SUBSCRIPTION_DELETE_ATTEMPT", "PENDING", "Method: " + methodName + " for Merchant: " + merchantId);

        MerchantSubscription sub = subscriptionRepository
                .findByMerchantMerchantIdAndPaymentMethodName(merchantId, methodName)
                .orElseThrow(() -> new RuntimeException("Pretplata nije pronađena."));

        subscriptionRepository.delete(sub);
        auditLogger.logEvent("SUBSCRIPTION_DELETED", "SUCCESS", "Method: " + methodName + " for Merchant: " + merchantId);
    }

    /**
     * KREIRANJE NOVOG PRODAVCA
     */
    @Transactional
    public MerchantCredentialsDTO createMerchant(MerchantCreateDTO request) {
        auditLogger.logEvent("MERCHANT_REGISTRATION", "PENDING", "Name: " + request.getName());

        Merchant m = new Merchant();
        m.setName(request.getName());
        m.setWebShopUrl(request.getWebShopUrl());

        String cleanName = request.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
        if (cleanName.length() > 20) cleanName = cleanName.substring(0, 20);
        String generatedId = cleanName + "_" + UUID.randomUUID().toString().substring(0, 5);

        String rawPassword = UUID.randomUUID().toString();
        m.setMerchantId(generatedId);
        m.setMerchantPassword(passwordEncoder.encode(rawPassword));

        merchantRepository.save(m);

        auditLogger.logEvent("MERCHANT_CREATED", "SUCCESS", "ID: " + m.getMerchantId());
        return new MerchantCredentialsDTO(m.getMerchantId(), rawPassword);
    }

    public List<Merchant> findAll() {
        return merchantRepository.findAll();
    }

    @Transactional
    public void updateServicesByAdmin(String merchantId, List<MerchantConfigDTO> configs) {
        auditLogger.logEvent("ADMIN_CONFIG_OVERRIDE", "START", "Target Merchant: " + merchantId);

        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Prodavac ne postoji: " + merchantId));

        for (MerchantConfigDTO config : configs) {
            PaymentMethod method = paymentMethodRepository.findByName(config.getMethodName())
                    .orElseThrow(() -> new RuntimeException("Nepoznat metod plaćanja: " + config.getMethodName()));

            String credentialsJson;
            try {
                credentialsJson = objectMapper.writeValueAsString(config.getCredentials());
            } catch (Exception e) {
                throw new RuntimeException("Greška pri obradi kredencijala");
            }

            Optional<MerchantSubscription> existingSub = subscriptionRepository
                    .findByMerchantMerchantIdAndPaymentMethodName(merchantId, config.getMethodName());

            if (existingSub.isPresent()) {
                MerchantSubscription sub = existingSub.get();
                sub.setCredentialsJson(credentialsJson);
                subscriptionRepository.save(sub);
            } else {
                MerchantSubscription newSub = new MerchantSubscription();
                newSub.setMerchant(merchant);
                newSub.setPaymentMethod(method);
                newSub.setCredentialsJson(credentialsJson);
                subscriptionRepository.save(newSub);
            }
        }
        auditLogger.logEvent("ADMIN_CONFIG_OVERRIDE_FINISHED", "SUCCESS", "Merchant: " + merchantId);
    }
}