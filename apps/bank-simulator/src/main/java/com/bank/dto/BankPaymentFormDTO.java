package com.bank.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BankPaymentFormDTO {

    private String paymentId;

    @Pattern(regexp = "^[0-9]{16}$", message = "PAN mora imati 16 cifara")
    private String pan;

    @Pattern(regexp = "^[0-9]{3}$", message = "CVV mora imati 3 cifre")
    private String securityCode;

    private String cardHolderName;

    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Format mora biti MM/YY")
    private String expirationDate;
}