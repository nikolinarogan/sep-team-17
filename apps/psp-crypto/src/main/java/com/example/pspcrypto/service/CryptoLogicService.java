package com.example.pspcrypto.service;

import com.example.pspcrypto.dto.MicroservicePaymentRequest;
import com.example.pspcrypto.dto.MicroservicePaymentResponse;
import com.example.pspcrypto.tools.AuditLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class CryptoLogicService {

    private final WebClient webClient;
    private final AuditLogger auditLogger;

    @Value("${blockcypher.api.token}")
    private String blockCypherToken;

    @Value("${blockcypher.base-url}")
    private String blockCypherUrl;

    @Value("${coingecko.base-url}")
    private String coinGeckoUrl;

    @Value("${webshop.wallet-name}")
    private String walletName;

    public CryptoLogicService(WebClient.Builder webClientBuilder, AuditLogger auditLogger) {
        this.webClient = webClientBuilder.build();
        this.auditLogger = auditLogger;
    }

    // --- 1. INICIJALIZACIJA (Stateless) ---
    public MicroservicePaymentResponse initializeCryptoPayment(MicroservicePaymentRequest req) {
        auditLogger.logEvent("CRYPTO_INIT_START", "PENDING", "UUID: " + req.getTransactionUuid());

        try {
            // A) Računamo iznos
            auditLogger.logEvent("FETCH_BTC_PRICE_ATTEMPT", "START", "Currency: " + req.getCurrency());
            BigDecimal btcPrice = fetchCurrentBtcPrice(req.getCurrency().toLowerCase());
            BigDecimal amountInBtc = req.getAmount().divide(btcPrice, 8, RoundingMode.CEILING);
            auditLogger.logEvent("FETCH_BTC_PRICE_SUCCESS", "SUCCESS", "BTC Price: " + btcPrice + " | Amount BTC: " + amountInBtc);

            // B) Generišemo novu adresu
            auditLogger.logEvent("GENERATE_ADDRESS_ATTEMPT", "START", "Wallet: " + walletName);
            String cryptoAddress = generateDerivedAddress(walletName);
            auditLogger.logEvent("GENERATE_ADDRESS_SUCCESS", "SUCCESS", "Address: " + cryptoAddress);

            // C) KREIRAMO "TOKEN" (Stateless)
            String rawToken = cryptoAddress + ":" + amountInBtc.toPlainString();
            String executionToken = Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));

            auditLogger.logEvent("CRYPTO_INIT_SUCCESS", "SUCCESS", "Generated Token: " + executionToken);

            return MicroservicePaymentResponse.builder()
                    .success(true)
                    .externalId(executionToken) // Token šaljemo PSP-u
                    .walletAddress(cryptoAddress)
                    .btcAmount(amountInBtc.toPlainString())
                    .qrCodeUrl("bitcoin:" + cryptoAddress + "?amount=" + amountInBtc.toPlainString())
                    .redirectUrl("https://localhost:4201/crypto-checkout/" + req.getTransactionUuid())
                    .build();

        } catch (Exception e) {
            auditLogger.logSecurityAlert("CRYPTO_INIT_FAILED", "UUID: " + req.getTransactionUuid() + " | Error: " + e.getMessage());
            return MicroservicePaymentResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    // --- 2. PROVERA STATUSA (Token Based) ---
    public boolean checkPaymentStatus(String token) {
        if (token == null || token.isEmpty()) {
            auditLogger.logEvent("CRYPTO_STATUS_CHECK_FAIL", "ERROR", "Token is null or empty");
            return false;
        }

        try {
            // A) Raspakivanje Tokena
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 2) {
                auditLogger.logSecurityAlert("INVALID_TOKEN_FORMAT", "Token content: " + decoded);
                return false;
            }

            String address = parts[0];
            BigDecimal expectedAmount = new BigDecimal(parts[1]);

            // B) Provera na Blockchain-u
            return checkBlockchain(address, expectedAmount);

        } catch (Exception e) {
            auditLogger.logEvent("CRYPTO_CHECK_ERROR", "ERROR", "Invalid Token: " + e.getMessage());
            return false;
        }
    }

    // --- 3. REKONSTRUKCIJA DETALJA (QR Kod) ---
    public Map<String, Object> getDetailsFromToken(String token) {
        auditLogger.logEvent("GET_DETAILS_FROM_TOKEN", "START", "Token received");
        Map<String, Object> details = new HashMap<>();
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            String address = parts[0];
            String amount = parts[1];

            details.put("walletAddress", address);
            details.put("btcAmount", amount);
            details.put("qrCodeUrl", "bitcoin:" + address + "?amount=" + amount);

            boolean isPaid = checkBlockchain(address, new BigDecimal(amount));
            details.put("status", isPaid ? "SUCCESS" : "PENDING");
            details.put("success", true);

            auditLogger.logEvent("GET_DETAILS_SUCCESS", "SUCCESS", "Addr: " + address);

        } catch (Exception e) {
            auditLogger.logEvent("GET_DETAILS_FAILED", "ERROR", "Reason: " + e.getMessage());
            details.put("success", false);
            details.put("error", "Nevalidan token transakcije.");
        }
        return details;
    }

    private boolean checkBlockchain(String address, BigDecimal expectedAmountBtc) {
        String url = "https://mempool.space/testnet/api/address/" + address;
        try {
            Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();
            if (response != null) {
                Map chainStats = (Map) response.get("chain_stats");
                Map mempoolStats = (Map) response.get("mempool_stats");

                long confirmed = ((Number) chainStats.get("funded_txo_sum")).longValue();
                long unconfirmed = ((Number) mempoolStats.get("funded_txo_sum")).longValue();
                long totalReceivedSats = confirmed + unconfirmed;

                long expectedSats = expectedAmountBtc.multiply(new BigDecimal(100_000_000)).longValue();
                long difference = Math.abs(totalReceivedSats - expectedSats);

                boolean isPaid = totalReceivedSats > 0 && (totalReceivedSats >= expectedSats || difference <= 5000);

                if (isPaid) {
                    auditLogger.logEvent("BLOCKCHAIN_CONFIRMED", "SUCCESS", "Address: " + address + " | Sats: " + totalReceivedSats);
                }

                return isPaid;
            }
        } catch (Exception e) {
            auditLogger.logEvent("BLOCKCHAIN_API_ERROR", "ERROR", "Mempool API failed: " + e.getMessage());
        }
        return false;
    }

    private BigDecimal fetchCurrentBtcPrice(String fiat) {
        try {
            Map response = webClient.get()
                    .uri(coinGeckoUrl + "/simple/price?ids=bitcoin&vs_currencies=" + fiat)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map bitcoin = (Map) response.get("bitcoin");
            return new BigDecimal(bitcoin.get(fiat).toString());
        } catch (Exception e) {
            auditLogger.logEvent("COINGECKO_API_ERROR", "ERROR", "Failed to fetch price: " + e.getMessage());
            System.err.println("CoinGecko error: " + e.getMessage());
            return new BigDecimal("65000"); // Fallback cena
        }
    }

    private String generateDerivedAddress(String walletName) {
        try {
            Map response = webClient.post()
                    .uri(blockCypherUrl + "/wallets/hd/" + walletName + "/addresses/derive?token=" + blockCypherToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("chains")) {
                java.util.List chains = (java.util.List) response.get("chains");
                java.util.Map chain = (java.util.Map) chains.get(0);
                java.util.List chainAddresses = (java.util.List) chain.get("chain_addresses");
                java.util.Map addressObj = (java.util.Map) chainAddresses.get(0);
                return (String) addressObj.get("address");
            }
            throw new RuntimeException("Invalid BlockCypher response");
        } catch (Exception e) {
            auditLogger.logSecurityAlert("BLOCKCYPHER_GEN_FAIL", "Wallet: " + walletName + " | Error: " + e.getMessage());
            throw new RuntimeException("BlockCypher Address Gen Error: " + e.getMessage());
        }
    }
}