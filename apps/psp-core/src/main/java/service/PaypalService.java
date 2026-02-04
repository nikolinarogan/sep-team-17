package service;

import model.PaymentTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import repository.PaymentTransactionRepository;

import java.util.*;

@Service
public class PaypalService {
    @Value("${PAYPAL_CLIENT_ID}")
    private String clientId;

    @Value("${PAYPAL_CLIENT_SECRET}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final PaymentTransactionRepository transactionRepository;

    // PayPal Sandbox URL-ovi za API v2
    private static final String PAYPAL_API = "https://api-m.sandbox.paypal.com";

    public PaypalService(RestTemplate restTemplate, PaymentTransactionRepository transactionRepository) {
        this.restTemplate = restTemplate;
        this.transactionRepository = transactionRepository;
    }

    // 1. Dobavljanje Access Tokena (OAuth2)
    private String getAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(encodedAuth);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(PAYPAL_API + "/v1/oauth2/token", request, Map.class);

        return response.getBody().get("access_token").toString();
    }

    // 2. Inicijalizacija plaćanja (Kreira Order)
    public String initializePayment(PaymentTransaction tx) {
        String token = getAccessToken();

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("intent", "CAPTURE");

        // Detalji o iznosu
        Map<String, Object> purchaseUnit = new HashMap<>();
        Map<String, Object> amount = new HashMap<>();
        amount.put("currency_code", "USD");
        amount.put("value", tx.getAmount().toString());
        purchaseUnit.put("amount", amount);
        orderRequest.put("purchase_units", Collections.singletonList(purchaseUnit));

        // URL-ovi za povratak (HTTPS obavezan)
        Map<String, String> appContext = new HashMap<>();
        appContext.put("return_url", "https://localhost:8443/api/payments/paypal/capture?uuid=" + tx.getUuid());
        appContext.put("cancel_url", tx.getFailedUrl());
        orderRequest.put("application_context", appContext);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(PAYPAL_API + "/v2/checkout/orders", entity, Map.class);

        Map<String, Object> body = response.getBody();
        String paypalOrderId = body.get("id").toString();

        // Čuvamo PayPal ID u bazu (executionId)
        tx.setExecutionId(paypalOrderId);
        transactionRepository.save(tx);

        // Vraćamo 'approve' link korisniku
        List<Map<String, String>> links = (List<Map<String, String>>) body.get("links");
        return links.stream()
                .filter(l -> "approve".equals(l.get("rel")))
                .findFirst()
                .get().get("href");
    }

    // 3. Finalizacija (Capture) - Kada se korisnik vrati
    public boolean captureOrder(String orderId) {
        String token = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PAYPAL_API + "/v2/checkout/orders/" + orderId + "/capture", entity, Map.class);
            return response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
