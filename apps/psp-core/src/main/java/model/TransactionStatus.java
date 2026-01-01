package model;

public enum TransactionStatus {
    CREATED,            // Inicirana od strane Shopa
    WAITING_FOR_PAYMENT,// Korisnik preusmjeren na banku/paypal/...
    SUCCESS,            // Novac legao
    FAILED,             // Banka odbila (nema sredstava)
    ERROR               // Tehnička greška
}
