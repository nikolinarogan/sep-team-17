package service;

import dto.CryptoPaymentResponseDTO;
import model.PaymentTransaction;
import model.TransactionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import repository.PaymentTransactionRepository;
import repository.PspConfigRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CryptoPaymentService {

    private final WebClient webClient;
    private final PaymentTransactionRepository transactionRepository;
    private final PspConfigRepository configRepository;

    @Value("${blockcypher.api.token}")
    private String blockCypherToken;

    public CryptoPaymentService(WebClient.Builder webClientBuilder,
                                PaymentTransactionRepository transactionRepository,
                                PspConfigRepository configRepository) {
        this.webClient = webClientBuilder.build();
        this.transactionRepository = transactionRepository;
        this.configRepository = configRepository;
    }

    public CryptoPaymentResponseDTO initializeCryptoPayment(PaymentTransaction tx) {
        // 1. Konfiguracija
        String coinGeckoUrl = getConfigValue("COINGECKO_BASE_URL");
        String blockCypherUrl = getConfigValue("BLOCKCYPHER_BASE_URL");
        String qrProvider = getConfigValue("CRYPTO_QR_PROVIDER");
        String walletName = getConfigValue("WEBSHOP_WALLET_NAME");

        // 2. Cena
        BigDecimal btcPrice = fetchCurrentBtcPrice(coinGeckoUrl, tx.getCurrency().toLowerCase());
        BigDecimal amountInBtc = tx.getAmount().divide(btcPrice, 8, RoundingMode.CEILING);

        // 3. GENERISANJE ADRESE (BLOCKCYPHER!) ✅
        // BlockCypher generiše adresu matematicki, to radi bez obzira na mrezu.
        Map<String, Object> addressData = generateDerivedAddress(blockCypherUrl, walletName);

        String testnetAddress = null;
        try {
            java.util.List chains = (java.util.List) addressData.get("chains");
            java.util.Map chain = (java.util.Map) chains.get(0);
            java.util.List chainAddresses = (java.util.List) chain.get("chain_addresses");
            java.util.Map addressObj = (java.util.Map) chainAddresses.get(0);
            testnetAddress = (String) addressObj.get("address");
        } catch (Exception e) {
            throw new RuntimeException("Greška kod BlockCyphera: " + addressData);
        }

        System.out.println("🚀 BlockCypher generisao adresu: " + testnetAddress);

        // 4. Perzistencija
        tx.setCryptoAddress(testnetAddress);
        tx.setAmountInCrypto(amountInBtc);
        tx.setCryptoCurrency("BTC");
        transactionRepository.save(tx);

        return new CryptoPaymentResponseDTO(
                amountInBtc.toPlainString(),
                testnetAddress,
                qrProvider + testnetAddress
        );
    }

    // --- NOVA METODA: PROVERA STANJA PREKO DRUGOG API-JA (MEMPOOL) ---
    // Ovu metodu Frontend mora da "pinguje" svake 3-5 sekunde
    public String checkPaymentStatus(String transactionUuid) {
        PaymentTransaction tx = transactionRepository.findByUuid(transactionUuid)
                .orElseThrow(() -> new RuntimeException("Nema transakcije"));

        // Ako je već plaćeno, vrati success URL odmah
        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            return tx.getSuccessUrl();
        }

        String address = tx.getCryptoAddress();

        // 🚀 OVDE JE MAGIJA: Pitamo Mempool.space (koji radi!) za adresu koju je dao BlockCypher
        String url = "https://mempool.space/testnet/api/address/" + address;

        try {
            Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();

            if (response == null) return tx.getErrorUrl();

            Map chainStats = (Map) response.get("chain_stats");     // Potvrđeno
            Map mempoolStats = (Map) response.get("mempool_stats"); // Nepotvrđeno (u mempoolu)

            long confirmed = ((Number) chainStats.get("funded_txo_sum")).longValue();
            long unconfirmed = ((Number) mempoolStats.get("funded_txo_sum")).longValue();

            long totalReceivedSats = confirmed + unconfirmed;

            // Očekivani iznos u Satoshijima
            long expectedSats = tx.getAmountInCrypto().multiply(new BigDecimal(100_000_000)).longValue();

            // --- POPRAVKA OVDE ---
            long difference = Math.abs(totalReceivedSats - expectedSats);

            System.out.println("🔍 Provera: Stiglo=" + totalReceivedSats + ", Čekamo=" + expectedSats + ", Razlika=" + difference);

            // Dozvoli razliku od 5000 satoshija (to je par centi)
            if (totalReceivedSats >= expectedSats || difference <= 5000) {

                tx.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(tx);
                System.out.println("✅ PLAĆANJE USPEŠNO (uz toleranciju)!");

                return tx.getSuccessUrl();
            }
        } catch (Exception e) {
            System.out.println("Greška pri proveri Mempool-a: " + e.getMessage());
        }
        return null;
    }

    // --- POMOĆNE METODE ---

    private String getConfigValue(String name) {
        return configRepository.findById(name)
                .orElseThrow(() -> new RuntimeException("Config " + name + " nije pronađen u bazi!"))
                .getConfigValue();
    }

    private BigDecimal fetchCurrentBtcPrice(String baseUrl, String fiat) {
        try {
            Map response = webClient.get()
                    .uri(baseUrl + "/simple/price?ids=bitcoin&vs_currencies=" + fiat)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map bitcoin = (Map) response.get("bitcoin");
            return new BigDecimal(bitcoin.get(fiat).toString());
        } catch (Exception e) {
            // Fallback ako CoinGecko padne (da ne pukne app na prezentaciji)
            System.err.println("CoinGecko error: " + e.getMessage());
            return new BigDecimal("6500000"); // Neka hardkodovana vrednost za test
        }
    }

    // KLJUČNA IZMJENA: Umjesto /addrs, koristimo wallet endpoint
    private Map generateDerivedAddress(String baseUrl, String walletName) {
        // POST /wallets/hd/{WALLET_NAME}/addresses/derive
        // Ovo generiše novu adresu koja je ZAUVIJEK vezana za tvoj webshop wallet
        return webClient.post()
                .uri(baseUrl + "/wallets/hd/" + walletName + "/addresses/derive?token=" + blockCypherToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private void registerWebhook(String baseUrl, String address, String transactionUuid) {
        String publicBaseUrl = getConfigValue("PSP_PUBLIC_CALLBACK_URL");
        String callbackUrl = publicBaseUrl + "/api/payments/crypto-callback/" + transactionUuid;

        System.out.println("🚀 Registrujem Webhook za adresu: " + address);
        System.out.println("🚀 Callback url: " + callbackUrl);


        Map<String, Object> webhookData = Map.of(
                "event", "unconfirmed-tx",
                "address", address,
                "url", callbackUrl
        );

        webClient.post()
                .uri(baseUrl + "/hooks?token=" + blockCypherToken)
                .bodyValue(webhookData)
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(response -> System.out.println("✅ Webhook registrovan!"));
    }

    // VALIDACIJA IZNOSA I POTVRDA
    public void processCallback(String uuid, Map<String, Object> payload) {
        PaymentTransaction tx = transactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena"));

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            return; // Već obrađeno
        }

        // 1. Validacija iznosa (Payload vraća vrednost u Satoshijima!)
        // Satoshiji su celi brojevi. 1 BTC = 100,000,000 Satoshija.
        long receivedSatoshis = Long.parseLong(payload.get("total").toString()); // BlockCypher šalje 'total'

        // Pretvaramo naše očekivane BTC u Satoshije za poređenje
        BigDecimal expectedBtc = tx.getAmountInCrypto();
        long expectedSatoshis = expectedBtc.multiply(new BigDecimal(100_000_000)).longValue();

        // Dozvoljavamo malu razliku (npr. zbog fee-ova ili greške u zaokruživanju +/- 1000 satošija)
        long difference = Math.abs(receivedSatoshis - expectedSatoshis);

        System.out.println("Očekivano (sats): " + expectedSatoshis + ", Primljeno (sats): " + receivedSatoshis);

        if (difference <= 5000) { // Tolerancija greške
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);
            System.out.println("💰 Kripto uplata USPEŠNA za UUID: " + uuid);
        } else {
            System.err.println("❌ Uplata stigla ali iznos nije tačan! Očekivano: " + expectedBtc + " BTC");
        }
    }
}