package service;

import dto.MerchantConfigDTO;
import model.Merchant;
import model.MerchantSubscription;
import model.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.MerchantRepository;
import repository.MerchantSubscriptionRepository;
import repository.PaymentMethodRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantSubscriptionRepository subscriptionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ObjectMapper objectMapper;

    public MerchantService(MerchantRepository merchantRepository,
                           MerchantSubscriptionRepository subscriptionRepository,
                           PaymentMethodRepository paymentMethodRepository,
                           ObjectMapper objectMapper) {
        this.merchantRepository = merchantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Ažuriranje pretplate za prodavca
     */
    @Transactional
    public void updateMerchantServices(String merchantId, List<MerchantConfigDTO> configs) {
        // 1. Nađi prodavca
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Prodavac ne postoji: " + merchantId));

        // 2. Prođi kroz svaki zahtijevani metod
        for (MerchantConfigDTO config : configs) {

            // Provjeri da li taj metod uopšte postoji u sistemu (u tabeli payment_methods)
            PaymentMethod method = paymentMethodRepository.findByName(config.getMethodName())
                    .orElseThrow(() -> new RuntimeException("Nepoznat metod plaćanja: " + config.getMethodName()));

            // Pretvori Mapu kredencijala u JSON string (npr. {"bankId":"123"} ->String)
            String credentialsJson;
            try {
                credentialsJson = objectMapper.writeValueAsString(config.getCredentials());
            } catch (Exception e) {
                throw new RuntimeException("Greška pri obradi kredencijala za " + config.getMethodName());
            }

            // 3. Provjeri da li već postoji pretplata
            Optional<MerchantSubscription> existingSub = subscriptionRepository
                    .findByMerchantMerchantIdAndPaymentMethodName(merchantId, config.getMethodName());

            if (existingSub.isPresent()) {
                // UPDATE
                MerchantSubscription sub = existingSub.get();
                sub.setCredentialsJson(credentialsJson);
                subscriptionRepository.save(sub);
            } else {
                // INSERT
                MerchantSubscription newSub = new MerchantSubscription();
                newSub.setMerchant(merchant);
                newSub.setPaymentMethod(method);
                newSub.setCredentialsJson(credentialsJson);
                subscriptionRepository.save(newSub);
            }
        }
    }

    /**
     * Metoda za brisanje (odjavu) servisa
     */
    @Transactional
    public void removeMerchantService(String merchantId, String methodName) {
        MerchantSubscription sub = subscriptionRepository
                .findByMerchantMerchantIdAndPaymentMethodName(merchantId, methodName)
                .orElseThrow(() -> new RuntimeException("Pretplata nije pronađena."));

        subscriptionRepository.delete(sub);
    }
}