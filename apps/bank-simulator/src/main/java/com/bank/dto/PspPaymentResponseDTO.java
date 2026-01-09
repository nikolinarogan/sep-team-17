package com.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PspPaymentResponseDTO {
    private String paymentUrl; // Npr. http://localhost:8082/pay/{paymentId}
    private String paymentId;  // ID transakcije u banci
}