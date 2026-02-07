package com.example.psp_paypal.service;

import com.example.psp_paypal.dto.MicroservicePaymentRequest;
import com.example.psp_paypal.dto.MicroservicePaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PayPalLogicService {
    @Value("${PAYPAL_CLIENT_ID}")
    private String clientId;

    @Value("${PAYPAL_CLIENT_SECRET}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    // PayPal Sandbox URL-ovi za API v2
    private static final String PAYPAL_API = "https://api-m.sandbox.paypal.com";

    public PayPalLogicService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 1. Dobavljanje Access Tokena (OAuth2)
    private String getAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(encodedAuth);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
        int maxAttempts = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        PAYPAL_API + "/v1/oauth2/token", request, Map.class);
                return response.getBody().get("access_token").toString();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        long delayMs = 1000L * (1 << (attempt - 1));
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new RuntimeException("PayPal OAuth greška nakon " + maxAttempts + " pokušaja: " + e.getMessage());
                }
            }
        }
        throw new RuntimeException("Nepoznata greška pri dohvatanju tokena.");
    }

    // 2. Kreiranje Order-a (glavna logika)
    public MicroservicePaymentResponse createOrder(MicroservicePaymentRequest req) {
        String token = getAccessToken();

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("intent", "CAPTURE");

        // Detalji o iznosu (Iz DTO-a)
        Map<String, Object> purchaseUnit = new HashMap<>();
        Map<String, Object> amount = new HashMap<>();

        // Koristimo valutu iz zahteva, ili fallback na USD
        String currencyCode = (req.getCurrency() != null) ? req.getCurrency() : "USD";
        amount.put("currency_code", currencyCode);
        amount.put("value", req.getAmount().toString());

        purchaseUnit.put("amount", amount);
        orderRequest.put("purchase_units", Collections.singletonList(purchaseUnit));

        // URL-ovi za povratak (Iz DTO-a - ovo šalje Core)
        Map<String, String> appContext = new HashMap<>();
        appContext.put("return_url", req.getReturnUrl()); // Ovo je sada URL Gateway-a
        appContext.put("cancel_url", req.getCancelUrl());
        orderRequest.put("application_context", appContext);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxAttempts = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        PAYPAL_API + "/v2/checkout/orders", entity, Map.class);

                Map<String, Object> body = response.getBody();

                // 1. Uzimamo PayPal Order ID
                String paypalOrderId = body.get("id").toString();

                // 2. Nalazimo link za redirekciju
                List<Map<String, String>> links = (List<Map<String, String>>) body.get("links");
                String redirectUrl = links.stream()
                        .filter(l -> "approve".equals(l.get("rel")))
                        .findFirst()
                        .get().get("href");

                // 3. Vraćamo odgovor Core-u (uspeh)
                return MicroservicePaymentResponse.builder()
                        .success(true)
                        .externalId(paypalOrderId) // Core će ovo upisati u bazu
                        .redirectUrl(redirectUrl)
                        .build();

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        long delayMs = 1000L * (1 << (attempt - 1));
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // Vraćamo odgovor Core-u (neuspeh) umesto da rušimo servis
                    return MicroservicePaymentResponse.builder()
                            .success(false)
                            .message("PayPal greška: " + e.getMessage())
                            .build();
                }
            }
        }

        return MicroservicePaymentResponse.builder()
                .success(false)
                .message("Nepoznata greška prilikom komunikacije sa PayPal-om.")
                .build();
    }

    // 3. Finalizacija (Capture)
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
            System.err.println("Capture failed: " + e.getMessage());
            return false;
        }
    }
}
