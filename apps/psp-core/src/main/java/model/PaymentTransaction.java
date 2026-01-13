package model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"merchant_id", "merchant_order_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID koji se vraća shopu i koji ide u URL: /checkout/{uuid}
    // UUID je da neko ne bi mogao da pogađa ID-jeve
    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @Column(name = "merchant_order_id", nullable = false)
    private String merchantOrderId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency; // "RSD", "EUR", "USD"

    @Column(name = "merchant_timestamp")
    private LocalDateTime merchantTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "chosen_method")
    private String chosenMethod;

    @Column(name = "success_url")
    private String successUrl;

    @Column(name = "failed_url")
    private String failedUrl;

    @Column(name = "error_url")
    private String errorUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    // Čuva STAN (banka)
    @Column(name = "execution_id")
    private String executionId;

    @Column(name = "stan")
    private String stan;

    // Čuva ACQUIRER_TIMESTAMP
    @Column(name = "service_timestamp")
    private LocalDateTime serviceTimestamp;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.uuid == null) {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
    }

    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }
}
