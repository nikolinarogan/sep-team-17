package com.bank.model;

import com.bank.tools.EncryptDecryptConverter;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cards")
@Data
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. ENKRIPTOVAN PAN (PCI DSS Req 3.4)
    @Convert(converter = EncryptDecryptConverter.class)
    @Column(name = "pan_encrypted", nullable = false)
    private String pan;

    // 2. MASKIRAN PAN (PCI DSS Req 3.3)
    @Column(name = "pan_masked", nullable = false)
    private String panMasked;

    // 3. HASH PAN (Za pretragu)
    @Column(name = "pan_hash", nullable = false, unique = true)
    private String panHash;

    @Column(nullable = false)
    private String cardHolderName;

    // Datum isteka (MM/YY)
    @Column(nullable = false, length = 5)
    private String expirationDate;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
