package com.ws.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentStatusDTO {
    private String merchantOrderId;  // ID narudžbine iz web shop-a
    private String pspTransactionId; // UUID transakcije iz PSP-a
    private String status;           // SUCCESS, FAILED, ERROR
    private LocalDateTime timestamp;
    private String paymentMethod;

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public void setMerchantOrderId(String merchantOrderId) {
        this.merchantOrderId = merchantOrderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPspTransactionId() {
        return pspTransactionId;
    }

    public void setPspTransactionId(String pspTransactionId) {
        this.pspTransactionId = pspTransactionId;
    }
}