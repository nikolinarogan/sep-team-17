package model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchant_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    // Ovdje čuvamo kredencijale za taj metod u JSON formatu
    // {"bankMerchantId": "xxx", "bankPassword": "yyy"}
    @Column(name = "credentials_json", columnDefinition = "TEXT")
    private String credentialsJson;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
