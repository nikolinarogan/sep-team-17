package com.example.psp_paypal.service;

import com.example.psp_paypal.dto.MicroservicePaymentRequest;
import com.example.psp_paypal.dto.MicroservicePaymentResponse;
import com.example.psp_paypal.tools.AuditLogger;
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
    private final AuditLogger auditLogger; // Dodato

    // PayPal Sandbox URL-ovi za API v2
    private static final String PAYPAL_API = "https://api-m.sandbox.paypal.com";

    public PayPalLogicService(RestTemplate restTemplate, AuditLogger auditLogger) { // Dodato u konstruktor
        this.restTemplate = restTemplate;
        this.auditLogger = auditLogger;
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

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                auditLogger.logEvent("PAYPAL_AUTH_ATTEMPT", "RETRY", "Attempt: " + attempt);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                        PAYPAL_API + "/v1/oauth2/token", request, Map.class);

                String token = response.getBody().get("access_token").toString();
                auditLogger.logEvent("PAYPAL_AUTH_SUCCESS", "SUCCESS", "OAuth Token secured.");
                return token;
            } catch (Exception e) {
                auditLogger.logEvent("PAYPAL_AUTH_ERROR", "ERROR", "Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        long delayMs = 1000L * (1 << (attempt - 1));
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    auditLogger.logSecurityAlert("PAYPAL_AUTH_CRITICAL_FAIL", "All retry attempts failed.");
                    throw new RuntimeException("PayPal OAuth greška nakon " + maxAttempts + " pokušaja: " + e.getMessage());
                }
            }
        }
        throw new RuntimeException("Nepoznata greška pri dohvatanju tokena.");
    }

    // 2. Kreiranje Order-a (glavna logika)
    public MicroservicePaymentResponse createOrder(MicroservicePaymentRequest req) {
        auditLogger.logEvent("PAYPAL_ORDER_CREATION", "START", "UUID: " + req.getTransactionUuid());

        String token = getAccessToken();

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("intent", "CAPTURE");

        Map<String, Object> purchaseUnit = new HashMap<>();
        Map<String, Object> amount = new HashMap<>();

        String currencyCode = (req.getCurrency() != null) ? req.getCurrency() : "USD";
        amount.put("currency_code", currencyCode);
        amount.put("value", req.getAmount().toString());

        purchaseUnit.put("amount", amount);
        orderRequest.put("purchase_units", Collections.singletonList(purchaseUnit));

        Map<String, String> appContext = new HashMap<>();
        appContext.put("return_url", req.getReturnUrl());
        appContext.put("cancel_url", req.getCancelUrl());
        orderRequest.put("application_context", appContext);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                auditLogger.logEvent("PAYPAL_API_POST_ORDER", "RETRY", "Attempt: " + attempt + " | UUID: " + req.getTransactionUuid());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        PAYPAL_API + "/v2/checkout/orders", entity, Map.class);

                Map<String, Object> body = response.getBody();
                String paypalOrderId = body.get("id").toString();

                List<Map<String, String>> links = (List<Map<String, String>>) body.get("links");
                String redirectUrl = links.stream()
                        .filter(l -> "approve".equals(l.get("rel")))
                        .findFirst()
                        .get().get("href");

                auditLogger.logEvent("PAYPAL_ORDER_READY", "SUCCESS", "PayPalID: " + paypalOrderId);

                return MicroservicePaymentResponse.builder()
                        .success(true)
                        .externalId(paypalOrderId)
                        .redirectUrl(redirectUrl)
                        .build();

            } catch (Exception e) {
                auditLogger.logEvent("PAYPAL_API_ERROR", "ERROR", "Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        long delayMs = 1000L * (1 << (attempt - 1));
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    auditLogger.logSecurityAlert("PAYPAL_ORDER_FAILED_PERMANENTLY", "UUID: " + req.getTransactionUuid());
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
        auditLogger.logEvent("PAYPAL_CAPTURE_EXECUTE", "START", "OrderID: " + orderId);

        try {
            String token = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PAYPAL_API + "/v2/checkout/orders/" + orderId + "/capture", entity, Map.class);

            boolean isCaptured = response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK;

            auditLogger.logEvent("PAYPAL_CAPTURE_RESULT", isCaptured ? "SUCCESS" : "FAILED", "OrderID: " + orderId);
            return isCaptured;
        } catch (Exception e) {
            auditLogger.logSecurityAlert("PAYPAL_CAPTURE_EXCEPTION", "OrderID: " + orderId + " | Error: " + e.getMessage());
            return false;
        }
    }
}