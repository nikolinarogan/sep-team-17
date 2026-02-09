package service;

import dto.CryptoPaymentResponseDTO;
import dto.PaymentInitResult;
import model.PaymentTransaction;
import model.TransactionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import repository.PaymentTransactionRepository;
import repository.PspConfigRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CryptoPaymentService implements PaymentProvider {

    private final GenericPaymentService genericPaymentService;

    public CryptoPaymentService(GenericPaymentService genericPaymentService) {
        this.genericPaymentService = genericPaymentService;
    }

    @Override
    public String getProviderName() {
        return "CRYPTO";
    }

    @Override
    public PaymentInitResult initiate(PaymentTransaction tx) {
        // Ovde je ključna razlika u odnosu na PayPal!
        // PayPal servis sam pravi HTTP zahtev ka PayPal API-ju.
        // Crypto servis DELEGIRA posao Generic servisu, koji zove tvoj psp-crypto mikroservis.

        return genericPaymentService.initiate(tx, "CRYPTO");
    }
}