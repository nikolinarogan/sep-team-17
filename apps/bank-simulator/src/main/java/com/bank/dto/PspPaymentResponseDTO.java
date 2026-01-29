package com.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PspPaymentResponseDTO {
    private String paymentUrl;
    private String paymentId;  // ID transakcije u banci
    private String stan;
    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }
}