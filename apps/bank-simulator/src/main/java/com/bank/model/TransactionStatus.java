package com.bank.model;

public enum TransactionStatus {
    CREATED,    // Zahtev primljen od PSP-a
    SUCCESS,    // Pare skinute
    FAILED,     // Greška u podacima
    INSUFFICIENT_FUNDS, // Nema para
    ERROR       // Tehnička greška
}