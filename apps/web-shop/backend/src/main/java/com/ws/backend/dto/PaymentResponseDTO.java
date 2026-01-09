package com.ws.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // Dodaj NoArgsConstructor da bi Jackson mogao da ga instancira
public class PaymentResponseDTO {
    // URL na koji tvoj Angular treba da uradi redirekciju (Marijin frontend)
    private String paymentUrl;

    // UUID transakcije koji je Marija generisala u svojoj bazi
    private String paymentId;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
}