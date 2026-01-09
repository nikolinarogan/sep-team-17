package com.bank.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "merchants")
@Data
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID koji PSP šalje banci (Tabela 2 iz specifikacije )
    @Column(nullable = false, unique = true)
    private String merchantId;

    // Password za proveru (Tabela 2 - implicirano validacijom [cite: 70])
    @Column(nullable = false)
    private String merchantPassword;

    // VEZA: Na koji račun u Banci ležu pare ovom prodavcu?
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}