package com.bank.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID koji generiše PSP i šalje banci (STAN ili PaymentID)
    @Column(name = "psp_transaction_id", unique = true)
    private String pspTransactionId;

    // ID koji generiše Banka (Internal Payment ID)
    @Column(name = "payment_id", unique = true)
    private String paymentId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant; // Kome se plaća

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    // Podaci o kartici kojom je plaćeno
    private String maskedPan;
    @Column(name = "stan")
    private String stan;

    @Column(name = "callback_url")
    private String callbackUrl;

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }
}