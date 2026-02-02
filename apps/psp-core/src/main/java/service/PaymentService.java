package service;

import dto.*;
import model.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import repository.MerchantRepository;
import repository.MerchantSubscriptionRepository;
import repository.PaymentTransactionRepository;
import repository.PspConfigRepository; // Novi import

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

    // Constructor Injection
    public PaymentService(MerchantRepository merchantRepository,
                          PaymentTransactionRepository transactionRepository,
                          MerchantSubscriptionRepository subscriptionRepository,
                          PspConfigRepository pspConfigRepository,
                          PasswordEncoder passwordEncoder,
                          RestTemplate restTemplate) {
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pspConfigRepository = pspConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
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
     * IZMENA: Sada tražimo i po Bankinom ID-u (executionId) i ne pucamo ako je status SUCCESS
     */
    public CheckoutResponseDTO getCheckoutData(String uuid) {
        // 1. Prvo probaj da nađeš po našem UUID-u
        PaymentTransaction tx = transactionRepository.findByUuid(uuid)
                // 2. Ako ne nađeš, probaj po Bankinom ID-u (executionId)
                .or(() -> transactionRepository.findByExecutionId(uuid))
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena: " + uuid));

        if (tx.getMerchantTimestamp().plusMinutes(30).isBefore(LocalDateTime.now())) {
            if (tx.getStatus() == TransactionStatus.CREATED) {
                tx.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(tx);
            }
            throw new RuntimeException("Link za plaćanje je istekao! (Maksimalno vreme: 30 min)");
        }
        // 3. Olabavili smo proveru. Ako je status SUCCESS, samo ćemo to i vratiti frontendu,
        // umesto da bacimo grešku. Frontend će znati šta s tim.
        /*
        if (tx.getStatus() != TransactionStatus.CREATED) {
            throw new RuntimeException("Ova transakcija je već obrađena ili nije validna.");
        }
        */

        List<MerchantSubscription> subscriptions = subscriptionRepository.findByMerchantMerchantId(tx.getMerchantId());

        if (subscriptions.isEmpty()) {
            throw new RuntimeException("Prodavac nema aktivnih metoda plaćanja!");
        }

        List<PaymentMethodDTO> availableMethods = subscriptions.stream()
                .map(sub -> new PaymentMethodDTO(
                        sub.getPaymentMethod().getName(),
                        sub.getPaymentMethod().getServiceUrl()
                ))
                .collect(Collectors.toList());

        return new CheckoutResponseDTO(
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchantId(),
                availableMethods
        );
    }

    @Transactional
    public String finaliseTransaction(dto.PaymentCallbackDTO callback, String paymentMethod) {

        // 1. Pronađi transakciju
        PaymentTransaction tx = transactionRepository.findByUuid(callback.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena."));

        // 2. Ažuriraj status
        try {
            TransactionStatus newStatus = TransactionStatus.valueOf(callback.getStatus());
            tx.setStatus(newStatus);
        } catch (Exception e) {
            tx.setStatus(TransactionStatus.ERROR);
        }

        // Čuvamo podatke od banke/servisa
        tx.setExternalTransactionId(callback.getExternalTransactionId());
        tx.setExecutionId(callback.getExecutionId());
        tx.setServiceTimestamp(callback.getServiceTimestamp());

        transactionRepository.save(tx);

        // 3. OBAVESTI WEB SHOP (Webhook)
        try {
            notifyWebShop(tx, paymentMethod);
        } catch (Exception e) {
            System.err.println("GRESKA: Nismo uspeli da obavestimo Web Shop: " + e.getMessage());
            // Nastavljamo dalje, ne rušimo redirect zbog ovoga
        }

        // 4. Vrati URL za redirect korisnika (Frontend)
        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            return tx.getSuccessUrl();
        } else if (tx.getStatus() == TransactionStatus.FAILED) {
            return tx.getFailedUrl();
        } else {
            return tx.getErrorUrl();
        }
    }

    // Pomoćna metoda za slanje na Web Shop
    private void notifyWebShop(PaymentTransaction tx, String paymentMethod) {
        Merchant merchant = merchantRepository.findByMerchantId(tx.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac ne postoji"));

        String baseWebhookUrl = merchant.getWebShopUrl();
        String targetUrl;

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            targetUrl = baseWebhookUrl + "/success";
        } else if (tx.getStatus() == TransactionStatus.FAILED) {
            targetUrl = baseWebhookUrl + "/failed";
        } else {
            targetUrl = baseWebhookUrl + "/error";
        }

        // Pakujemo podatke
        PaymentStatusDTO statusDTO = new PaymentStatusDTO(
                tx.getMerchantOrderId(),
                tx.getUuid(),
                paymentMethod,
                tx.getStatus().toString(),
                LocalDateTime.now()
        );

//        System.out.println("Saljem notifikaciju na: " + targetUrl);
//        restTemplate.postForEntity(targetUrl, statusDTO, Void.class);
// RETRY MEHANIZAM U SLUCAJU DA WEBSHOP NE BUDE DOSTUPAN U TRENUTNKU KAD SALJEMO
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("Webhook attempt " + attempt + "/" + maxRetries + " -> " + targetUrl);
                restTemplate.postForEntity(targetUrl, statusDTO, Void.class);
                System.out.println("Webhook uspešno poslat.");
                return; // uspeh, izlazimo
            } catch (Exception e) {
                System.err.println("Webhook attempt " + attempt + "/" + maxRetries + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    long delayMs = 2000L * attempt; // 2s, 4s
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Webhook retry prekinut.");
                        break;
                    }
                } else {
                    System.err.println("Webhook nije uspeo ni posle " + maxRetries + " pokušaja. Web Shop će možda dobiti status preko pollinga.");
                }
            }
        }
    }

    /**
     * Prima ID i STATUS od banke.
     */
    public String getRedirectUrl(String bankPaymentId, String statusFromBank) {

        // 1. Nađi transakciju
        PaymentTransaction tx = transactionRepository.findByExecutionId(bankPaymentId)
                .or(() -> transactionRepository.findByUuid(bankPaymentId))
                .orElseThrow(() -> new RuntimeException("Nepoznata transakcija: " + bankPaymentId));
        System.out.println("Status je: " + statusFromBank);
        System.out.println("Šaljem korisnika na: " + (statusFromBank.equals("SUCCESS") ? tx.getSuccessUrl() : tx.getFailedUrl()));
        // 2. Proveri šta kaže Banka
        if ("SUCCESS".equalsIgnoreCase(statusFromBank)) {

            if (tx.getStatus() != TransactionStatus.SUCCESS) {
                System.out.println("--- BANKA KAŽE SUCCESS -> ODOBRAVAM TRANSAKCIJU ---");
                tx.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(tx);

                // Javljamo Web Shopu
                try {
                    notifyWebShop(tx, "CARD");
                } catch (Exception e) {
                    System.err.println("Greska pri javljanju shopu: " + e.getMessage());
                }
            }
            return tx.getSuccessUrl();

        } else {
            // Ako banka kaže FAILED ili bilo šta drugo
            System.out.println("--- BANKA KAŽE FAILED -> ODBIJAM TRANSAKCIJU ---");
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);

            // I ovo javljamo shopu (da znaju da je propalo)
            try {
                notifyWebShop(tx, "CARD");
            } catch (Exception e) {}

            return tx.getFailedUrl();
        }
    }

    // trazim transakciju po Merchant Order ID-u (jer to Web Shop zna)
    public PaymentStatusDTO checkTransactionStatus(String merchantId, String merchantOrderId) {
        PaymentTransaction tx = transactionRepository.findByMerchantIdAndMerchantOrderId(merchantId, merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji"));
        String method = tx.getChosenMethod() == null ? "Unknown" : tx.getChosenMethod();
        // Vraćamo status u istom formatu kao i callback
        return new PaymentStatusDTO(
                tx.getMerchantOrderId(),
                tx.getUuid(),
                method, // ili izvuci iz baze ako imaš polje paymentMethod
                tx.getStatus().toString(),
                LocalDateTime.now()
        );
    }
}