package com.bank.service;
import com.bank.dto.*;
import com.bank.model.*;
import com.bank.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.bank.tools.AuditLogger;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final WebClient webClient;
    private final AuditLogger auditLogger;
    private static final String PSP_CALLBACK_URL = "https://localhost:8443/api/payments/payment-callback";

    public BankService(AccountRepository accountRepository, CardRepository cardRepository,
                       MerchantRepository merchantRepository, TransactionRepository transactionRepository,
                       WebClient webClient, AuditLogger auditLogger) { // Dodato u konstruktor
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.webClient = webClient;
        this.auditLogger = auditLogger;
    }

    public PspPaymentResponseDTO createPaymentUrl(PspPaymentRequestDTO request) {
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("MERCHANT_NOT_FOUND", "ID: " + request.getMerchantId());
                    return new RuntimeException("Prodavac ne postoji!");
                });

        if (!merchant.getMerchantPassword().equals(request.getMerchantPassword())) {
            auditLogger.logSecurityAlert("MERCHANT_AUTH_FAIL", "Invalid password for merchant: " + request.getMerchantId());
            throw new RuntimeException("Pogrešna lozinka prodavca!");
        }

        Optional<Transaction> existing = transactionRepository.findByPspTransactionId(request.getPspTransactionId());
        if (existing.isPresent()) {
            Transaction tx = existing.get();
            if (tx.getStatus() == TransactionStatus.CREATED) {
                return new PspPaymentResponseDTO("https://localhost:8082/pay.html?paymentId=" + tx.getPaymentId(), tx.getPaymentId(), tx.getStan() != null ? tx.getStan() : request.getStan());
            }
            throw new RuntimeException("Transakcija je već obrađena.");
        }

        Transaction tx = new Transaction();
        tx.setPspTransactionId(request.getPspTransactionId());
        tx.setMerchant(merchant);
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(TransactionStatus.CREATED);
        tx.setStan(request.getStan());

        String internalPaymentId = UUID.randomUUID().toString();
        tx.setPaymentId(internalPaymentId);
        transactionRepository.save(tx);

        return new PspPaymentResponseDTO("https://localhost:8082/pay.html?paymentId=" + internalPaymentId, internalPaymentId, request.getStan());
    }

    @Transactional
    public String processPayment(BankPaymentFormDTO form) {
        Transaction tx = transactionRepository.findByPaymentId(form.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji!"));

        if (tx.getTimestamp().plusMinutes(15).isBefore(LocalDateTime.now())) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logEvent("BANK_TX_EXPIRED", "FAILED", "ID: " + tx.getPaymentId());
            throw new RuntimeException("Link je istekao!");
        }

        if (!luhnCheck(form.getPan())) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logSecurityAlert("LUHN_CHECK_FAILED", "Invalid PAN attempt for TX: " + tx.getPaymentId());
            throw new RuntimeException("Neispravan broj kartice!");
        }

        Card card = cardRepository.findByPan(form.getPan())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("CARD_NOT_FOUND", "Non-existent card PAN used.");
                    return new RuntimeException("Kartica ne postoji!");
                });

        if (!card.getSecurityCode().equals(form.getSecurityCode())) {
            auditLogger.logSecurityAlert("CVV_MISMATCH", "Invalid CVV for card.");
            throw new RuntimeException("Pogrešan CVV kod!");
        }

        Account buyerAccount = card.getAccount();
        if (buyerAccount.getBalance().compareTo(tx.getAmount()) < 0) {
            tx.setStatus(TransactionStatus.INSUFFICIENT_FUNDS);
            transactionRepository.save(tx);
            auditLogger.logEvent("INSUFFICIENT_FUNDS", "FAILED", "Acc: " + buyerAccount.getAccountNumber());
            throw new RuntimeException("Nema dovoljno sredstava!");
        }

        // Transfer
        buyerAccount.setBalance(buyerAccount.getBalance().subtract(tx.getAmount()));
        accountRepository.save(buyerAccount);

        Account merchantAccount = tx.getMerchant().getAccount();
        merchantAccount.setBalance(merchantAccount.getBalance().add(tx.getAmount()));
        accountRepository.save(merchantAccount);

        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);

        sendCallbackToPsp(tx.getPspTransactionId());

        return PSP_CALLBACK_URL + "?paymentId=" + tx.getPspTransactionId() + "&status=SUCCESS";
    }

    private void sendCallbackToPsp(String pspTxId) {
        String callbackUrl = PSP_CALLBACK_URL + "?paymentId=" + pspTxId + "&status=SUCCESS";
        try {
            auditLogger.logEvent("BANK_TO_PSP_CALLBACK", "START", "URL: " + callbackUrl);
            webClient.get().uri(callbackUrl).retrieve().bodyToMono(Void.class).block();
            auditLogger.logEvent("BANK_TO_PSP_CALLBACK", "SUCCESS", "PSP notified.");
        } catch (Exception e) {
            auditLogger.logEvent("BANK_TO_PSP_CALLBACK", "ERROR", "Failed: " + e.getMessage());
        }
    }

    private boolean luhnCheck(String pan) {
        int nDigits = pan.length();
        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--) {
            int d = pan.charAt(i) - '0';
            if (isSecond) d = d * 2;
            nSum += d / 10;
            nSum += d % 10;
            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    public String generateIpsQrString(PspPaymentRequestDTO request) {
        return "K:PR|V:01|C:1|R:..." ; // Tvoja postojeća logika za IPS...
    }

    @Transactional
    public String processInternalTransfer(QrTransferRequestDTO request) {
        Account payer = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji!"));

        if (payer.getPin() == null || !payer.getPin().equals(request.getPin())) {
            auditLogger.logSecurityAlert("QR_PIN_FAILED", "Email: " + request.getEmail());
            throw new RuntimeException("Pogrešan PIN!");
        }

        Account receiver = accountRepository.findByAccountNumber(request.getReceiverAccount()).orElseThrow();
        Merchant merchant = merchantRepository.findByAccount(receiver).orElseThrow();
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        Transaction tx = transactionRepository.findTopByMerchantAndAmountAndStatusOrderByTimestampDesc(merchant, amount, TransactionStatus.CREATED).orElseThrow();

        if (payer.getBalance().compareTo(amount) < 0) {
            auditLogger.logEvent("QR_INSUFFICIENT_FUNDS", "FAILED", "Email: " + request.getEmail());
            throw new RuntimeException("Nema dovoljno sredstava!");
        }

        payer.setBalance(payer.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        tx.setStatus(TransactionStatus.SUCCESS);

        accountRepository.save(payer);
        accountRepository.save(receiver);
        transactionRepository.save(tx);

        sendCallbackToPsp(tx.getPspTransactionId());

        return PSP_CALLBACK_URL + "?paymentId=" + tx.getPspTransactionId() + "&status=SUCCESS";
    }
}