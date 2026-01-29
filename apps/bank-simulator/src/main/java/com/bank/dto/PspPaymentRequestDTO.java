package com.bank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PspPaymentRequestDTO {
    private String merchantId;      // ID prodavca u banci
    private String merchantPassword;// Password prodavca
    private BigDecimal amount;
    private String currency;
    private String pspTransactionId; // STAN ili ID iz PSP-a
    private LocalDateTime pspTimestamp;
    private String stan;
}