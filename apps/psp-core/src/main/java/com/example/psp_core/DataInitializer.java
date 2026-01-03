package com.example.psp_core;

import model.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import repository.*;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantSubscriptionRepository subscriptionRepository;
    private final PspConfigRepository pspConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataInitializer(AdminRepository adminRepository,
                           PaymentMethodRepository paymentMethodRepository,
                           MerchantRepository merchantRepository,
                           MerchantSubscriptionRepository subscriptionRepository,
                           PspConfigRepository pspConfigRepository,
                           PasswordEncoder passwordEncoder,
                           ObjectMapper objectMapper) {
        this.adminRepository = adminRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.merchantRepository = merchantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pspConfigRepository = pspConfigRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. KREIRANJE ADMINA (Ako ne postoji)
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            adminRepository.save(admin);
            System.out.println("✅ ADMIN KREIRAN: user='admin', pass='admin123'");
        }

        // 2. KREIRANJE GLOBALNIH METODA PLAĆANJA
        if (paymentMethodRepository.count() == 0) {
            createMethod("CARD", "http://localhost:8082/api/card");   // Banka servis
            createMethod("QR", "http://localhost:8082/api/qr");       // Banka servis (IPS)
            createMethod("PAYPAL", "http://localhost:8083/api/paypal"); // PayPal simulacija
            createMethod("CRYPTO", "http://localhost:8084/api/crypto"); // Crypto simulacija
            System.out.println("✅ METODE PLAĆANJA KREIRANE (Card, QR, PayPal, Crypto)");
        }

        // 3. KREIRANJE KONFIGURACIJE
        if (pspConfigRepository.count() == 0) {
            PspConfig linkConfig = new PspConfig();
            linkConfig.setConfigName("PAYMENT_LINK_TEMPLATE");
            // Ovo je link na koji redirektujemo Web Shop. {uuid} se dinamički menja.
            linkConfig.setConfigValue("http://localhost:4201/checkout/{uuid}");
            pspConfigRepository.save(linkConfig);
            System.out.println("✅ KONFIGURACIJA KREIRANA (Frontend Link)");
        }

        // 4. KREIRANJE TEST PRODAVCA (Rent-A-Car)
        if (merchantRepository.count() == 0) {
            Merchant m = new Merchant();
            m.setName("Rent A Car Demo");
            m.setWebShopUrl("http://localhost:4200"); // URL Web Shopa
            m.setMerchantId("shop_123");       // API KEY

            m.setMerchantPassword(passwordEncoder.encode("shop_pass"));

            merchantRepository.save(m);
            System.out.println("✅ TEST MERCHANT KREIRAN: id='shop_123', pass='shop_pass'");

            // 5. PRETPLATA: Povezujemo Shop sa Karticom i QR kodom
            // Simuliramo da je Shop unio svoje bankovne podatke

            // a) Dodajemo CARD
            Map<String, String> cardCreds = new HashMap<>();
            cardCreds.put("bankMerchantId", "111122223333");
            cardCreds.put("bankPassword", "bank_secret");
            addSubscription(m, "CARD", cardCreds);

            // b) Dodajemo QR
            Map<String, String> qrCreds = new HashMap<>();
            qrCreds.put("ipsId", "999000");
            addSubscription(m, "QR", qrCreds);

            System.out.println("✅ PRETPLATE KREIRANE (Shop ima CARD i QR)");
        }
    }

    // Pomoćna metoda za pravljenje metoda
    private void createMethod(String name, String url) {
        PaymentMethod pm = new PaymentMethod();
        pm.setName(name);
        pm.setServiceUrl(url);
        paymentMethodRepository.save(pm);
    }

    // Pomoćna metoda za dodavanje pretplate
    private void addSubscription(Merchant merchant, String methodName, Map<String, String> creds) throws Exception {
        PaymentMethod method = paymentMethodRepository.findByName(methodName)
                .orElseThrow(() -> new RuntimeException("Metod ne postoji: " + methodName));

        MerchantSubscription sub = new MerchantSubscription();
        sub.setMerchant(merchant);
        sub.setPaymentMethod(method);
        sub.setCredentialsJson(objectMapper.writeValueAsString(creds));

        subscriptionRepository.save(sub);
    }
}