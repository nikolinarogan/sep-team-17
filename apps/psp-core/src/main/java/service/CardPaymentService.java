package service;

import model.PaymentTransaction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // <--- BITNO!

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CardPaymentService {

    private static final String BANK_URL = "https://localhost:8082/api/bank/card";

    // Polje za RestTemplate
    private final RestTemplate restTemplate;

    // Konstruktor (Spring ovde ubacuje onaj konfigurisani RestTemplate)
    public CardPaymentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String initializePayment(PaymentTransaction transaction) {
        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", "prodavac123");
        request.put("merchantPassword", "sifra123");
        request.put("amount", transaction.getAmount());
        request.put("currency", "EUR");
        request.put("pspTransactionId", transaction.getUuid());
        request.put("pspTimestamp", LocalDateTime.now());

        try {
            // IZMENA: Umesto String.class, tražimo Map.class (da Java odmah parsira JSON)
            ResponseEntity<Map> response = this.restTemplate.postForEntity(BANK_URL, request, Map.class);

            // Uzimamo telo odgovora kao Mapu
            Map<String, Object> body = response.getBody();

            // Proveravamo da li smo dobili ono što nam treba
            if (body != null && body.containsKey("paymentUrl")) {
                String url = body.get("paymentUrl").toString();
                System.out.println("---- IZVUČEN URL: " + url);

                // Vraćamo SAMO URL (čist string: "https://localhost:8082/...")
                return url;
            }

            throw new RuntimeException("Banka nije vratila paymentUrl!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška: " + e.getMessage());
        }
    }
}