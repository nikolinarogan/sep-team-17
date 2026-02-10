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

    /*// PAN broj (16 cifara)
    @Column(nullable = false, unique = true, length = 16)
    private String pan;

    // CVV/Security Code
    @Column(nullable = false, length = 3)
    private String securityCode;

    @Column(nullable = false)
    private String cardHolderName;

    // Datum isteka (MM/YY)
    @Column(nullable = false, length = 5)
    private String expirationDate; */

    // 1. ENKRIPTOVAN PAN (PCI DSS Req 3.4)
    // Ovo čuvaš samo ako ti je apsolutno neophodno da nekad dekriptuješ broj.
    // U bazi će biti nečitljiv string (npr. "U2FsdGVkX1...").
    // Konverter automatski radi encrypt pri upisu i decrypt pri čitanju.
    @Convert(converter = EncryptDecryptConverter.class)
    @Column(name = "pan_encrypted", nullable = false)
    private String pan;

    // 2. MASKIRAN PAN (PCI DSS Req 3.3)
    // Ovo služi za prikaz na ekranu i u logovima (npr. "**** **** **** 1234").
    @Column(name = "pan_masked", nullable = false)
    private String panMasked;

    // 3. HASH PAN (Za pretragu)
    // Pošto je 'pan_encrypted' svaki put drugačiji (zbog IV-a), ne možeš raditi SELECT * WHERE pan = ...
    // Zato koristimo Hash (SHA-256) za pretragu.
    @Column(name = "pan_hash", nullable = false, unique = true)
    private String panHash;

    // 4. CVV - SAMO HASH (PCI DSS Req 3.2)
    // Nikada ne čuvamo pravi CVV. Čuvamo BCrypt ili SHA-256 hash.
    // Kada korisnik unese CVV, hashujemo ga i poredimo sa ovim.
    @Column(name = "cvv_hash", nullable = false)
    private String cvvHash;

    // Ovo polje služi samo da primi vrednost iz DTO-a pre nego što je heširamo.
    // @Transient znači "Ne upisuj ovo u bazu".
    @Transient
    private String tempRawCvv;

    @Column(nullable = false)
    private String cardHolderName;

    // Datum isteka (MM/YY) - Ovo nije kritično kao PAN, ali može ostati ovako
    @Column(nullable = false, length = 5)
    private String expirationDate;

    // VEZA: Kartica mora biti vezana za neki račun sa kojeg se skida novac
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
