package com.bank.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cards")
@Data
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PAN broj (16 cifara)
    @Column(nullable = false, unique = true, length = 16)
    private String pan;

    // CVV/Security Code
    @Column(nullable = false, length = 3)
    private String securityCode;

    // Ime na kartici
    @Column(nullable = false)
    private String cardHolderName;

    // Datum isteka (MM/YY)
    @Column(nullable = false, length = 5)
    private String expirationDate;

    // VEZA: Kartica mora biti vezana za neki račun sa kojeg se skida novac
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
