package service;

import model.Merchant;
import model.PaymentTransaction;
import model.TransactionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import repository.PaymentTransactionRepository;
import repository.MerchantRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CardPaymentService {

    private static final String BANK_URL = "https://localhost:8082/api/bank/card";

    private final RestTemplate restTemplate;
    private final PaymentTransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;

    public CardPaymentService(RestTemplate restTemplate, PaymentTransactionRepository paymentTransactionRepository, MerchantRepository merchantRepository) {
        this.restTemplate = restTemplate;
        this.transactionRepository = paymentTransactionRepository;
        this.merchantRepository = merchantRepository;
    }

    public String initializePayment(PaymentTransaction transaction) {
        String stan = String.valueOf((int) (Math.random() * 900000) + 100000);

        // 2. ČUVAMO GA U BAZI
        transaction.setStan(stan);
        transactionRepository.save(transaction);

        // 3. DOBAVLJAMO PRAVOG PRODAVCA IZ BAZE (Kako treba) 🏆
        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac sa ID-jem " + transaction.getMerchantId() + " nije pronađen!"));

        Map<String, Object> request = new HashMap<>();

        request.put("merchantId", merchant.getMerchantId());
        request.put("merchantPassword", merchant.getMerchantPassword());
        request.put("amount", transaction.getAmount());
        request.put("currency", transaction.getCurrency());
        request.put("pspTransactionId", transaction.getUuid());
        request.put("pspTimestamp", LocalDateTime.now());

        // Šaljemo STAN banci
        request.put("stan", stan);

        try {
            ResponseEntity<Map> response = this.restTemplate.postForEntity(BANK_URL, request, Map.class);

            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("paymentUrl")) {
                String url = body.get("paymentUrl").toString();
                System.out.println("---- IZVUČEN URL: " + url);

                if (body.containsKey("paymentId")) {
                    String bankPaymentId = body.get("paymentId").toString();
                    transaction.setExecutionId(bankPaymentId);
                    transactionRepository.save(transaction);
                }
                return url;
            }

            throw new RuntimeException("Banka nije vratila paymentUrl!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška: " + e.getMessage());
        }
    }

    public String handleCallback(String bankPaymentId, String status) {
        PaymentTransaction tx = transactionRepository.findByExecutionId(bankPaymentId)
                .orElseThrow(() -> new RuntimeException("Nepoznata transakcija u PSP-u!"));

        if ("SUCCESS".equals(status)) {
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);

            return tx.getSuccessUrl();

        } else {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);

            return tx.getFailedUrl();
        }
    }
}