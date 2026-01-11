package service;

import model.PaymentTransaction;
import model.TransactionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // <--- BITNO!
import repository.PaymentTransactionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CardPaymentService {

    private static final String BANK_URL = "https://localhost:8082/api/bank/card";

    // Polje za RestTemplate
    private final RestTemplate restTemplate;
    private final PaymentTransactionRepository transactionRepository;

    // Konstruktor (Spring ovde ubacuje onaj konfigurisani RestTemplate)
    public CardPaymentService(RestTemplate restTemplate, PaymentTransactionRepository paymentTransactionRepository) {
        this.restTemplate = restTemplate;
        this.transactionRepository = paymentTransactionRepository;
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

                if (body.containsKey("paymentId")) {
                    String bankPaymentId = body.get("paymentId").toString();
                    transaction.setExecutionId(bankPaymentId); // Čuvamo ga kao executionId
                    transactionRepository.save(transaction);   // Snimamo u bazu
                }
                // Vraćamo SAMO URL (čist string: "https://localhost:8082/...")
                return url;
            }

            throw new RuntimeException("Banka nije vratila paymentUrl!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška: " + e.getMessage());
        }
    }

    public String handleCallback(String bankPaymentId, String status) {
        // 1. Tražimo transakciju po ID-u koji nam je banka vratila
        PaymentTransaction tx = transactionRepository.findByExecutionId(bankPaymentId)
                .orElseThrow(() -> new RuntimeException("Nepoznata transakcija u PSP-u!"));

        // 2. Ažuriramo status na osnovu onoga što je banka rekla
        if ("SUCCESS".equals(status)) {
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);

            // Vraćamo korisnika na Success URL Web Shopa
            return tx.getSuccessUrl();

        } else {
            tx.setStatus(TransactionStatus.FAILED); // ili ERROR
            transactionRepository.save(tx);

            // Vraćamo korisnika na Failed URL Web Shopa
            return tx.getFailedUrl();
        }
    }
}