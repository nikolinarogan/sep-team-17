package com.example.pspcrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroservicePaymentRequest {
    private BigDecimal amount;       // Iznos u originalnoj valuti (npr. USD)
    private String currency;         // Valuta (npr. USD, EUR)
    private String transactionUuid;  // UUID iz psp-core baze
    private String merchantId;       // Potrebno za dohvatanje konfiguracije

    private String successUrl;
    private String failedUrl;
    private String errorUrl;
}
