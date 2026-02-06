package service;

import dto.PaymentInitResult;
import model.PaymentTransaction;

public interface PaymentProvider {
    // Vraća ime provajdera (npr. "PAYPAL", "CARD", "CRYPTO")
    String getProviderName();

    // Inicijalizuje plaćanje i vraća URL za redirect ili podatke
    PaymentInitResult initiate(PaymentTransaction tx);

    // Proverava da li je eksterni servis dostupan (High Availability)
//    boolean isAvailable();
}
