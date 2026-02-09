package com.example.pspcrypto.service;

import com.example.pspcrypto.dto.MicroservicePaymentRequest;
import com.example.pspcrypto.dto.MicroservicePaymentResponse;
import com.example.pspcrypto.tools.AuditLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CryptoLogicService {

    private final WebClient webClient;
    private final AuditLogger auditLogger;

    @Value("${blockcypher.api.token}")
    private String blockCypherToken;

    @Value("${blockcypher.base-url:https://api.blockcypher.com/v1/btc/test3}")
    private String blockCypherUrl;

    @Value("${coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String coinGeckoUrl;

    @Value("${webshop.wallet-name:novcanik_prodavca}")
    private String walletName;

    // Privremena memorija za mapiranje UUID -> CryptoAddress (u produkciji bi ovo bila baza)
    private final Map<String, String> transactionAddressMap = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> transactionAmountMap = new ConcurrentHashMap<>();
    // Privremena memorija za status transakcije
    private final Map<String, Boolean> transactionStatusMap = new ConcurrentHashMap<>();

    public CryptoLogicService(WebClient.Builder webClientBuilder, AuditLogger auditLogger) {
        this.webClient = webClientBuilder.build();
        this.auditLogger = auditLogger;
    }

    public MicroservicePaymentResponse initializeCryptoPayment(MicroservicePaymentRequest req) {
        auditLogger.logEvent("CRYPTO_INIT_START", "PENDING",
                "UUID: " + req.getTransactionUuid() + " | Amount: " + req.getAmount());

        try {
            // 1. Konverzija valute u BTC
            BigDecimal btcPrice = fetchCurrentBtcPrice(req.getCurrency().toLowerCase());
            BigDecimal amountInBtc = req.getAmount().divide(btcPrice, 8, RoundingMode.CEILING);

            String cryptoAddress = generateDerivedAddress(walletName);

            // Čuvamo i iznos da bismo mogli kasnije da proverimo
            transactionAddressMap.put(req.getTransactionUuid(), cryptoAddress);
            transactionAmountMap.put(req.getTransactionUuid(), amountInBtc); // <--- NOVO
            transactionStatusMap.put(req.getTransactionUuid(), false);

            auditLogger.logEvent("CRYPTO_INIT_SUCCESS", "SUCCESS",
                    "Address: " + cryptoAddress + " | BTC: " + amountInBtc);

            return MicroservicePaymentResponse.builder()
                    .success(true)
                    .walletAddress(cryptoAddress)
                    .btcAmount(amountInBtc.toPlainString())
                    .qrCodeUrl("bitcoin:" + cryptoAddress + "?amount=" + amountInBtc.toPlainString())
                    // Ovde vraćamo URL ka tvom frontendu gde se prikazuje QR kod
                    .redirectUrl("https://localhost:4201/crypto-checkout/" + req.getTransactionUuid())
                    .build();

        } catch (Exception e) {
            auditLogger.logSecurityAlert("CRYPTO_INIT_FAILED",
                    "UUID: " + req.getTransactionUuid() + " | Reason: " + e.getMessage());
            return MicroservicePaymentResponse.builder()
                    .success(false)
                    .message("Crypto Error: " + e.getMessage())
                    .build();
        }
    }

    // Provera statusa (poziva se sa frontenda ili periodično)
    public boolean checkPaymentStatus(String transactionUuid) { // Sklonili smo expectedAmountBtc iz argumenta jer ga imamo u mapi
        String address = transactionAddressMap.get(transactionUuid);
        BigDecimal expectedAmountBtc = transactionAmountMap.get(transactionUuid);

        if (address == null || expectedAmountBtc == null) return false;

        if (transactionStatusMap.getOrDefault(transactionUuid, false)) {
            return true;
        }

        String url = "https://mempool.space/testnet/api/address/" + address;

        try {
            Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();

            if (response != null) {
                Map chainStats = (Map) response.get("chain_stats");
                Map mempoolStats = (Map) response.get("mempool_stats");

                long confirmed = ((Number) chainStats.get("funded_txo_sum")).longValue();
                long unconfirmed = ((Number) mempoolStats.get("funded_txo_sum")).longValue();
                long totalReceivedSats = confirmed + unconfirmed;

                // Konverzija očekivanog iznosa u Satoshije
                long expectedSats = expectedAmountBtc.multiply(new BigDecimal(100_000_000)).longValue();

                // Tolerancija od 5000 satoshija
                long difference = Math.abs(totalReceivedSats - expectedSats);

                // Logika iz starog servisa
                if (totalReceivedSats > 0 && (totalReceivedSats >= expectedSats || difference <= 5000)) {
                    transactionStatusMap.put(transactionUuid, true);
                    auditLogger.logEvent("CRYPTO_PAYMENT_CONFIRMED", "SUCCESS", "UUID: " + transactionUuid);
                    return true;
                }
            }
        } catch (Exception e) {
            auditLogger.logEvent("CRYPTO_CHECK_ERROR", "ERROR", e.getMessage());
        }
        return false;
    }

    // --- Pomoćne metode (BlockCypher & CoinGecko) ---

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
            throw new RuntimeException("BlockCypher Address Gen Error: " + e.getMessage());
        }
    }

    public Map<String, Object> getDetailsFromCache(String uuid) {
        Map<String, Object> details = new HashMap<>();

        if (transactionAddressMap.containsKey(uuid)) {
            String address = transactionAddressMap.get(uuid);
            BigDecimal amount = transactionAmountMap.get(uuid);

            details.put("walletAddress", address);
            details.put("btcAmount", amount.toPlainString());
            details.put("qrCodeUrl", "bitcoin:" + address + "?amount=" + amount.toPlainString());
            details.put("success", true);
        } else {
            // Ako nema u kešu (npr. restartovan servis), možda treba vratiti grešku ili regenerisati
            details.put("success", false);
            details.put("error", "Transakcija nije pronađena ili je istekla.");
        }
        return details;
    }

}