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

    // Vrati na http ako si ugasila SSL, ili ostavi https ako koristiš onaj "TrustAll" hack
    private static final String BANK_URL = "https://localhost:8082/api/bank/card";

    public String initializePayment(PaymentTransaction transaction) {
        // OVO JE ONAJ STARI NAČIN (pravimo novi objekat svaki put)
        RestTemplate restTemplate = new RestTemplate();

        // 1. Priprema podataka
        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", "prodavac123");
        request.put("merchantPassword", "sifra123");
        request.put("amount", transaction.getAmount());
        request.put("currency", "EUR");
        request.put("pspTransactionId", transaction.getUuid());
        request.put("pspTimestamp", LocalDateTime.now());

        try {
            // 2. Pozivamo Banku
            // Ovde očekujemo String.class jer banka vraća čist URL
            ResponseEntity<String> response = restTemplate.postForEntity(BANK_URL, request, String.class);

            String responseBody = response.getBody();

            System.out.println("---- ODGOVOR BANKE: " + responseBody);

            // 3. Vraćamo URL frontendu
            return responseBody;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Neuspešna komunikacija sa bankom: " + e.getMessage());
        }
    }
}