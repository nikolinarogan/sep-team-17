package com.example.pspcrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicroservicePaymentResponse {
    private boolean success;
    private String message;

    // --- STARI NAZIVI (Zbog Frontenda) ---
    private String btcAmount;      // Ranije bilo cryptoAmount
    private String walletAddress;  // Ranije bilo cryptoAddress
    private String qrCodeUrl;      // Ranije bilo qrCodeData

    // Opciono: URL za redirekciju ako koristiš success/fail page
    private String redirectUrl;
}