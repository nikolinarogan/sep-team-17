package service;

import dto.CheckoutResponseDTO;
import dto.PaymentMethodDTO;
import dto.PaymentRequestDTO;
import dto.PaymentResponseDTO;
import model.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.MerchantRepository;
import repository.MerchantSubscriptionRepository;
import repository.PaymentTransactionRepository;
import repository.PspConfigRepository; // Novi import

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

    // Constructor Injection
    public PaymentService(MerchantRepository merchantRepository,
                          PaymentTransactionRepository transactionRepository,
                          MerchantSubscriptionRepository subscriptionRepository,
                          PspConfigRepository pspConfigRepository,
                          PasswordEncoder passwordEncoder) {
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pspConfigRepository = pspConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Kreiranje transakcije (inicijalizacija)
     */
    @Transactional
    public PaymentResponseDTO createTransaction(PaymentRequestDTO request) {

        // 1. Validacija prodavca
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac sa ID-jem " + request.getMerchantId() + " ne postoji."));

        // 2. Provjera lozinke
        if (!passwordEncoder.matches(request.getMerchantPassword(), merchant.getMerchantPassword())) {
            throw new RuntimeException("Pogrešna lozinka za prodavca.");
        }

        // 3. Sprečavanje dvostrukog plaćanja
        if (transactionRepository.existsByMerchantIdAndMerchantOrderId(request.getMerchantId(), request.getMerchantOrderId())) {
            throw new RuntimeException("Transakcija sa Order ID: " + request.getMerchantOrderId() + " već postoji!");
        }

        // 4. Kreiranje entiteta transakcije
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUuid(UUID.randomUUID().toString());
        tx.setMerchantId(request.getMerchantId());
        tx.setMerchantOrderId(request.getMerchantOrderId());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setMerchantTimestamp(request.getMerchantTimestamp());
        tx.setStatus(TransactionStatus.CREATED);

        // URL-ovi na koje se korisnik vraća
        tx.setSuccessUrl(request.getSuccessUrl());
        tx.setFailedUrl(request.getFailedUrl());
        tx.setErrorUrl(request.getErrorUrl());

        transactionRepository.save(tx);

        System.out.println("Nova transakcija kreirana: " + tx.getUuid());

        String urlTemplate = pspConfigRepository.findByConfigName("PAYMENT_LINK_TEMPLATE")
                .map(PspConfig::getConfigValue)
                .orElseThrow(() -> new RuntimeException("Sistemska greška: Nedostaje PAYMENT_LINK_TEMPLATE konfiguracija!"));

        String fullUrl = urlTemplate.replace("{uuid}", tx.getUuid());

        return new PaymentResponseDTO(fullUrl, tx.getUuid());
    }

    /**
     * Dobavljanje podataka za Checkout stranu
     */
    public CheckoutResponseDTO getCheckoutData(String uuid) {
        PaymentTransaction tx = transactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena ili je istekla."));

        if (tx.getStatus() != TransactionStatus.CREATED) {
            throw new RuntimeException("Ova transakcija je već obrađena ili nije validna.");
        }

        List<MerchantSubscription> subscriptions = subscriptionRepository.findByMerchantMerchantId(tx.getMerchantId());

        if (subscriptions.isEmpty()) {
            throw new RuntimeException("Prodavac nema aktivnih metoda plaćanja!");
        }

        List<PaymentMethodDTO> availableMethods = subscriptions.stream()
                .map(sub -> new PaymentMethodDTO(
                        sub.getPaymentMethod().getName(),
                        sub.getPaymentMethod().getServiceUrl() // Uzimamo URL iz baze
                ))
                .collect(Collectors.toList());

        return new CheckoutResponseDTO(
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchantId(),
                availableMethods
        );
    }
    /**
     * Finalizacija transakcije
     */
    @Transactional
    public void finaliseTransaction(dto.PaymentCallbackDTO callback) {
        // 1. Pronađi transakciju u bazi
        PaymentTransaction tx = transactionRepository.findByUuid(callback.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena."));

        // 2. Parsiraj i postavi novi status
        try {
            TransactionStatus newStatus = TransactionStatus.valueOf(callback.getStatus());
            tx.setStatus(newStatus);
        } catch (Exception e) {
            // Ako stigne nepoznat status, stavi ERROR
            tx.setStatus(TransactionStatus.ERROR);
        }

        // 3. Sačuvaj bitne podatke iz banke/servisa (STAN, Global ID)
        tx.setExternalTransactionId(callback.getExternalTransactionId());
        tx.setExecutionId(callback.getExecutionId());
        tx.setServiceTimestamp(callback.getServiceTimestamp());

        // 4. Snimi promjene
        transactionRepository.save(tx);



        // MARO, OVAKO NESTO TREBA DA ODRADIS, DA BI MI POSLALA OVAJ DTO. POGLEDAJ KAKO SAM U SHOPU NAPRAVILA OVAJ REST ZA KOMUNIKACIJU
        // MOZES ISKORISITI ISTI, SAMO DODAJ SVE ONO ZA SERTIFIKATE....
// u bazi cuvaj ono za url za bekend napisala sam negdje u komentaru u modelu

//        String callbackUrl;
//        if (tx.getStatus() == TransactionStatus.SUCCESS) {
//            callbackUrl = tx.getSuccessUrl();
//        } else if (tx.getStatus() == TransactionStatus.FAILED ||
//                tx.getStatus() == TransactionStatus.INSUFFICIENT_FUNDS) {
//            callbackUrl = tx.getFailedUrl();
//        } else {
//            callbackUrl = tx.getErrorUrl();
//        }
//
//// Kreiraj DTO sa podacima ii treba mi paymentMethod
//        PaymentStatusDTO statusDTO = new PaymentStatusDTO();
//        statusDTO.setMerchantOrderId(tx.getMerchantOrderId());
//        statusDTO.setPspTransactionId(tx.getUuid());
//        statusDTO.setStatus(tx.getStatus().toString());
//        statusDTO.setTimestamp(LocalDateTime.now());
//
//// Pozovi web shop API
//        restTemplate.postForEntity(callbackUrl, statusDTO, Void.class);
        // 3 urla koja sam ti poslala u dto su linkovi od stranica web shopa na frontu. Ili ih vrati sebi na fe pa tamo preusmjeri
        // ili vrati 3xx status sa linkom, ovaj kod iznad provjeri nisam sihurna
// O
        System.out.println("Transakcija " + tx.getUuid() + " finalizovana sa statusom: " + tx.getStatus());
    }
}