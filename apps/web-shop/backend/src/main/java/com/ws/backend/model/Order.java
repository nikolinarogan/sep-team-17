package com.ws.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private Insurance insurance;

    @ManyToOne
    private Equipment equipment;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Column
    private Double pricePerDay; // za pregled istorije, cijena u trenutku kupovine

    @Column
    private Double price; // cijena za osiguranje

    @Column (nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String currency = "EUR"; // NISAM SIGURNA DA LI TREBA OVAKO!!!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;


}
