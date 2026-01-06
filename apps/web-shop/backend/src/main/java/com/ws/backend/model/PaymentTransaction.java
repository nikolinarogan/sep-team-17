package com.ws.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Jedinstveni ID koji saljem PSP-u
    // Ovo je MERCHANT_ORDER_ID iz specifikacije
    @Column(unique = true, nullable = false)
    private String merchantOrderId;

    // ID koji dobijam nazad od PSP-a (PAYMENT_ID)
    private String pspPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // jedan order moze imati status pending, ali je prije toga imao status failed
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Način plaćanja (popunjava se kasnije kad PSP javi šta je korišćeno)
    private String paymentMethod;

//    // Opciono: Globalni ID iz banke ako ga dobijem
//    private String globalTransactionId;
}
