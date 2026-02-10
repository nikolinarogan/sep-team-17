package com.bank.service;

import com.bank.dto.*;
import com.bank.model.*;
import com.bank.repository.*;
import com.bank.tools.AuditLogger;
import com.bank.tools.CryptoUtil; // ✅ Dodato
import org.springframework.security.crypto.password.PasswordEncoder; // ✅ Dodato
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

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

    // ✅ NOVI ALATI ZA ZAŠTITU
    private final CryptoUtil cryptoUtil;
    private final PasswordEncoder passwordEncoder;

    public BankService(AccountRepository accountRepository,
                       CardRepository cardRepository,
                       MerchantRepository merchantRepository,
                       TransactionRepository transactionRepository,
                       WebClient webClient,
                       AuditLogger auditLogger,
                       CryptoUtil cryptoUtil,
                       PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.webClient = webClient;
        this.auditLogger = auditLogger;
        this.cryptoUtil = cryptoUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. METODA ZA PSP: Kreiranje URL-a za plaćanje
    public PspPaymentResponseDTO createPaymentUrl(PspPaymentRequestDTO request) {
        auditLogger.logEvent("BANK_PAYMENT_INIT", "START", "PSP_TX: " + request.getPspTransactionId());

        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("MERCHANT_NOT_FOUND", "ID: " + request.getMerchantId());
                    return new RuntimeException("Prodavac ne postoji u banci!");
                });

        // 🔒 IZMENA: Provera heširane lozinke
        if (!request.getMerchantPassword().equals(merchant.getMerchantPassword())) {
            auditLogger.logSecurityAlert("MERCHANT_AUTH_FAILED", "ID: " + request.getMerchantId());
            throw new RuntimeException("Pogrešna lozinka prodavca!");
        }

        Optional<Transaction> existing = transactionRepository.findByPspTransactionId(request.getPspTransactionId());
        if (existing.isPresent()) {
            Transaction tx = existing.get();
            if (tx.getStatus() == TransactionStatus.CREATED) {
                String paymentUrl = "https://localhost:8082/pay.html?paymentId=" + tx.getPaymentId();
                return new PspPaymentResponseDTO(paymentUrl, tx.getPaymentId(), tx.getStan() != null ? tx.getStan() : request.getStan());
            }
            throw new RuntimeException("Transakcija je već obrađena (status: " + tx.getStatus() + ").");
        }

        Transaction tx = new Transaction();
        tx.setPspTransactionId(request.getPspTransactionId());
        tx.setMerchant(merchant);
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(TransactionStatus.CREATED);
        tx.setStan(request.getStan());
        tx.setCallbackUrl(request.getCallbackUrl());

        String internalPaymentId = UUID.randomUUID().toString();
        tx.setPaymentId(internalPaymentId);

        transactionRepository.save(tx);
        auditLogger.logEvent("BANK_TX_CREATED", "SUCCESS", "PaymentID: " + internalPaymentId);

        String paymentUrl = "https://localhost:8082/pay.html?paymentId=" + internalPaymentId;
        return new PspPaymentResponseDTO(paymentUrl, internalPaymentId, request.getStan());
    }

    // 2. METODA ZA KUPCA: Obrada plaćanja (skidanje novca)
    @Transactional
    public String processPayment(BankPaymentFormDTO form) {
        auditLogger.logEvent("CARD_PROCESSING_START", "PENDING", "PaymentID: " + form.getPaymentId());

        Transaction tx = transactionRepository.findByPaymentId(form.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji ili je istekla!"));

        if (tx.getTimestamp().plusMinutes(15).isBefore(LocalDateTime.now())) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logEvent("BANK_TX_EXPIRED", "FAILED", "ID: " + tx.getPaymentId());
            throw new RuntimeException("Link za plaćanje je istekao! Imali ste 15 minuta.");
        }

        if (tx.getStatus() != TransactionStatus.CREATED) {
            throw new RuntimeException("Transakcija je već obrađena!");
        }

        // Luhn provera radi nad sirovim brojem koji stiže sa Frontenda (to je OK, ne čuvamo ga)
        if (!luhnCheck(form.getPan())) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            auditLogger.logSecurityAlert("LUHN_FAILED", "ID: " + tx.getPaymentId());
            throw new RuntimeException("Neispravan broj kartice (Luhn check failed)!");
        }

        // 🔒 IZMENA: Pretraga po HASH-u (jer je PAN u bazi enkriptovan i nepretraživ)
        String panHash = cryptoUtil.hashForSearch(form.getPan());

        Card card = cardRepository.findByPanHash(panHash) // <--- Moraš dodati ovu metodu u CardRepository
                .orElseThrow(() -> {
                    auditLogger.logSecurityAlert("CARD_NOT_FOUND", "Hash check failed");
                    return new RuntimeException("Kartica ne postoji u banci!");
                });

        // 🔒 IZMENA: Provera heširanog CVV koda
        if (!passwordEncoder.matches(form.getSecurityCode(), card.getCvvHash())) {
            auditLogger.logSecurityAlert("CVV_INVALID", "PaymentID: " + tx.getPaymentId());
            throw new RuntimeException("Pogrešan CVV kod!");
        }

        // Provera datuma isteka
        String expDate = form.getExpirationDate();
        if (expDate == null || !expDate.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            throw new RuntimeException("Neispravan format datuma isteka (MM/YY)!");
        }
        String[] parts = expDate.split("/");
        int expMonth = Integer.parseInt(parts[0]);
        int expYear = Integer.parseInt("20" + parts[1]);
        LocalDateTime now = LocalDateTime.now();
        if (expYear < now.getYear() || (expYear == now.getYear() && expMonth < now.getMonthValue())) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new RuntimeException("Kartica je istekla!");
        }

        Account buyerAccount = card.getAccount();
        if (buyerAccount.getBalance().compareTo(tx.getAmount()) < 0) {
            tx.setStatus(TransactionStatus.INSUFFICIENT_FUNDS);
            transactionRepository.save(tx);
            auditLogger.logEvent("INSUFFICIENT_FUNDS", "FAILED", "Acc: " + buyerAccount.getAccountNumber());
            throw new RuntimeException("Nema dovoljno sredstava na računu!");
        }

        // Transfer novca
        buyerAccount.setBalance(buyerAccount.getBalance().subtract(tx.getAmount()));
        accountRepository.save(buyerAccount);

        Account merchantAccount = tx.getMerchant().getAccount();
        merchantAccount.setBalance(merchantAccount.getBalance().add(tx.getAmount()));
        accountRepository.save(merchantAccount);

        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);
        auditLogger.logEvent("BANK_TX_SUCCESS", "SUCCESS", "PaymentID: " + tx.getPaymentId());

        sendCallbackToPsp(tx);

        return tx.getCallbackUrl() + "?paymentId=" + tx.getPspTransactionId() + "&status=SUCCESS";
    }

    private void sendCallbackToPsp(Transaction tx) {
        String url = tx.getCallbackUrl() + "?paymentId=" + tx.getPspTransactionId() + "&status=SUCCESS";
        try {
            auditLogger.logEvent("BANK_TO_PSP_CALLBACK", "START", "Target URL: " + url);
            webClient.get().uri(url).retrieve().bodyToMono(Void.class).block();
            auditLogger.logEvent("BANK_TO_PSP_CALLBACK", "SUCCESS", "PSP instance notified.");
        } catch (Exception e) {
            auditLogger.logEvent("BANK_TO_PSP_CALLBACK_ERROR", "ERROR", e.getMessage());
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
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac ne postoji!"));

        String rawAccount = merchant.getAccount().getAccountNumber().replaceAll("-", "");
        if (!rawAccount.matches("\\d+")) throw new RuntimeException("Broj računa mora sadržati samo cifre!");

        String formattedAccount = rawAccount.length() >= 18 ? rawAccount.substring(rawAccount.length() - 18) : String.format("%018d", Long.parseLong(rawAccount));
        String formattedAmount = String.format("%.2f", request.getAmount()).replace(".", ",");

        String description = "Placanje porudzbine " + request.getPspTransactionId();
        if (description.length() > 35) {
            description = "Placanje porudzbine " + request.getPspTransactionId().substring(0, 8);
        }

        StringBuilder ips = new StringBuilder();
        ips.append("K:PR|V:01|C:1|R:").append(formattedAccount);
        ips.append("|N:").append(merchant.getAccount().getOwnerName());
        ips.append("|I:RSD").append(formattedAmount);
        ips.append("|SF:289|S:").append(description);

        return ips.toString();
    }

    @Transactional
    public String processInternalTransfer(QrTransferRequestDTO request) {
        auditLogger.logEvent("QR_TRANSFER_START", "PENDING", "User: " + request.getEmail());

        Account payer = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Korisnik ne postoji!"));

        // 🔒 IZMENA: Provera heširanog PIN-a
        if (payer.getPin() == null || !passwordEncoder.matches(request.getPin(), payer.getPin())) {
            auditLogger.logSecurityAlert("QR_PIN_INVALID", "User: " + request.getEmail());
            throw new RuntimeException("Pogrešan PIN!");
        }

        Account receiver = accountRepository.findByAccountNumber(request.getReceiverAccount()).orElseThrow();
        Merchant merchant = merchantRepository.findByAccount(receiver).orElseThrow();
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        Transaction tx = transactionRepository.findTopByMerchantAndAmountAndStatusOrderByTimestampDesc(merchant, amount, TransactionStatus.CREATED)
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena!"));

        if (payer.getBalance().compareTo(amount) < 0) {
            auditLogger.logEvent("QR_INSUFFICIENT_FUNDS", "FAILED", "User: " + request.getEmail());
            throw new RuntimeException("Nema dovoljno sredstava!");
        }

        payer.setBalance(payer.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        tx.setStatus(TransactionStatus.SUCCESS);

        accountRepository.save(payer);
        accountRepository.save(receiver);
        transactionRepository.save(tx);

        auditLogger.logEvent("QR_TRANSFER_SUCCESS", "SUCCESS", "TX_ID: " + tx.getPaymentId());

        sendCallbackToPsp(tx);

        return tx.getCallbackUrl() + "?paymentId=" + tx.getPspTransactionId() + "&status=SUCCESS";
    }
}