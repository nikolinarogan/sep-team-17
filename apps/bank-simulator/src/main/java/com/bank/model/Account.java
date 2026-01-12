package com.bank.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Broj računa (npr. 1234567890)
    @Column(nullable = false, unique = true)
    private String accountNumber;

    // Ime vlasnika računa (može biti ime osobe ili ime firme Web Shop-a)
    @Column(nullable = false)
    private String ownerName;

    @Column(name = "email") // Nije nullable=false, jer po specifikaciji nije obavezno
    private String email;

    // Stanje na računu
    @Column(nullable = false)
    private BigDecimal balance;

    // Rezervisana sredstva (bitno za stavku 5 iz specifikacije - "banka rezerviše sredstva" [cite: 76])
    @Column(nullable = false)
    private BigDecimal reservedFunds;

    @Column(name = "pin") // <--- OVO DODAJEŠ
    private String pin;

    // Obavezan Getter i Setter
    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
