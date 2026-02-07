package com.example.psp_paypal.dto;

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
    private String redirectUrl;    // URL za redirekciju korisnika (npr. PayPal login)
    private String externalId;     // ID sa PayPal-a
    private String message;        // Poruka greške ako ima
}
