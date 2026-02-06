package service;

import dto.PaymentInitResult;
import model.Merchant;
import model.PaymentTransaction;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import repository.MerchantRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class QrPaymentService implements  PaymentProvider{
    private static final String BANK_QR_URL = "https://localhost:8082/api/bank/qr-initialize";
    private final RestTemplate restTemplate;
    private final MerchantRepository merchantRepository;

    public QrPaymentService(RestTemplate restTemplate, MerchantRepository merchantRepository) {
        this.restTemplate = restTemplate;
        this.merchantRepository = merchantRepository;
    }
    @Override
    public String getProviderName() {
        return "QR";
    }

    @Override
    public PaymentInitResult initiate(PaymentTransaction transaction) {
        String qrData = getIpsQrData(transaction);
        return PaymentInitResult.builder().qrData(qrData).build();
    }

    public String getIpsQrData(PaymentTransaction transaction) {
        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac sa ID-jem " + transaction.getMerchantId() + " nije pronađen!"));

        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", merchant.getMerchantId());
        request.put("merchantPassword", merchant.getMerchantPassword());
        request.put("amount", transaction.getAmount());
        request.put("currency", transaction.getCurrency());
        request.put("pspTransactionId", transaction.getUuid());

        int maxAttempts = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            ResponseEntity<Map> response = this.restTemplate.postForEntity(BANK_QR_URL, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("qrData")) {
                String qrData = body.get("qrData").toString();

                // VALIDACIJA PREMA NBS (PDF dokumentacija)
                if (validateIpsString(qrData)) {
                    return qrData;
                } else {
                    throw new RuntimeException("Dobijeni QR podaci nisu u skladu sa NBS standardom!");
                }
            }
            throw new RuntimeException("Banka nije vratila qrData!");
        } catch (Exception e) {
            lastException = e;
            if (attempt < maxAttempts) {
                try {
                    long delayMs = 1000L * (1 << (attempt - 1));
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry prekinut.", ie);
                }
            }
        }
        }
        throw new RuntimeException("Greška u komunikaciji sa Bankom (QR) nakon " + maxAttempts + " pokušaja: " +
                (lastException != null ? lastException.getMessage() : "Nepoznata greška"));
    }

    // Metoda koja implementira pravila iz PDF-a [cite: 5, 11, 22, 65]
    private boolean validateIpsString(String s) {
        if (s == null || !s.startsWith("K:PR|V:01|C:1")) return false; // [cite: 14, 16, 17]
        if (s.endsWith("|")) return false; // [cite: 13]

        // Provera računa (Tag R: mora imati 18 cifara)
        if (!s.contains("|R:") || s.split("\\|R:")[1].substring(0, 18).matches(".*\\D.*")) return false;

        // Provera iznosa (Mora RSD i zarez) [cite: 64, 65]
        if (!s.contains("|I:RSD") || !s.contains(",")) return false;

        return true;
    }
}
