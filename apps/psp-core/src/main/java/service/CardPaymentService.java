package service;

import model.PaymentTransaction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CardPaymentService {

    private static final String BANK_URL = "https://localhost:8082/api/bank/card";

    // 1. Definiši polje za RestTemplate
    private final RestTemplate restTemplate;

    // 2. Ubrizgaj ga kroz konstruktor (Dependency Injection)
    public CardPaymentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String initializePayment(PaymentTransaction transaction) {
        // 3. OBRIŠI liniju: RestTemplate restTemplate = new RestTemplate();
        // Koristimo "this.restTemplate" koji je konfigurisan da veruje HTTPS-u

        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", "prodavac123");
        request.put("merchantPassword", "sifra123");
        request.put("amount", transaction.getAmount());
        request.put("currency", "EUR");
        request.put("pspTransactionId", transaction.getUuid());
        request.put("pspTimestamp", LocalDateTime.now());

        try {
            // Pozivamo banku preko konfigurisanog templejta
            ResponseEntity<String> response = this.restTemplate.postForEntity(BANK_URL, request, String.class);

            String responseBody = response.getBody();
            System.out.println("---- ODGOVOR BANKE: " + responseBody);
            return responseBody;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Neuspešna komunikacija sa bankom: " + e.getMessage());
        }
    }
}