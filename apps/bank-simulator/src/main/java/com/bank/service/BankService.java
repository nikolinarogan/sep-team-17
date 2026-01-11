package com.bank.service;

import com.bank.dto.*;
import com.bank.model.*;
import com.bank.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BankService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;

    public BankService(AccountRepository accountRepository, CardRepository cardRepository,
                       MerchantRepository merchantRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
    }

    // 1. METODA ZA PSP: Kreiranje URL-a za plaćanje
    public PspPaymentResponseDTO createPaymentUrl(PspPaymentRequestDTO request) {
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac ne postoji u banci!"));

        if (!merchant.getMerchantPassword().equals(request.getMerchantPassword())) {
            throw new RuntimeException("Pogrešna lozinka prodavca!");
        }

        Transaction tx = new Transaction();
        tx.setPspTransactionId(request.getPspTransactionId());
        tx.setMerchant(merchant);
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(TransactionStatus.CREATED);

        String internalPaymentId = UUID.randomUUID().toString();
        tx.setPaymentId(internalPaymentId);

        transactionRepository.save(tx);

        // Vraćamo URL ka našem HTML-u
        String paymentUrl = "https://localhost:8082/pay.html?paymentId=" + internalPaymentId;

        return new PspPaymentResponseDTO(paymentUrl, internalPaymentId);
    }

    // 2. METODA ZA KUPCA: Obrada plaćanja (skidanje novca)
    @Transactional
    public void processPayment(BankPaymentFormDTO form) {
        Transaction tx = transactionRepository.findByPaymentId(form.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transakcija ne postoji ili je istekla!"));

        if (tx.getStatus() != TransactionStatus.CREATED) {
            throw new RuntimeException("Transakcija je već obrađena!");
        }

        // Luhn validacija
        if (!luhnCheck(form.getPan())) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new RuntimeException("Neispravan broj kartice (Luhn check failed)!");
        }

        Card card = cardRepository.findByPan(form.getPan())
                .orElseThrow(() -> new RuntimeException("Kartica ne postoji u banci!"));

        if (!card.getSecurityCode().equals(form.getSecurityCode())) {
            throw new RuntimeException("Pogrešan CVV kod!");
        }

        Account buyerAccount = card.getAccount();
        if (buyerAccount.getBalance().compareTo(tx.getAmount()) < 0) {
            tx.setStatus(TransactionStatus.INSUFFICIENT_FUNDS);
            transactionRepository.save(tx);
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
    }

    // Pomoćna metoda za Luhn algoritam
    private boolean luhnCheck(String pan) {
        int nDigits = pan.length();
        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--) {
            int d = pan.charAt(i) - '0';
            if (isSecond == true)
                d = d * 2;
            nSum += d / 10;
            nSum += d % 10;
            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    public String generateIpsQrString(PspPaymentRequestDTO request) {
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Prodavac ne postoji!"));

        // 1. Priprema računa - čistimo ga od crtica ako ih ima (mora biti 18 cifara)
        String rawAccount = merchant.getAccount().getAccountNumber().replaceAll("-", "");
        // Dopuna nulama ako je račun kraći (prema primerima iz dokumentacije) [cite: 33, 36]
        // Proveri da li sadrži samo cifre
        if (!rawAccount.matches("\\d+")) {
            throw new RuntimeException("Broj računa mora sadržati samo cifre!");
        }

        String formattedAccount;

        if (rawAccount.length() == 18) {
            // Ako već ima 18 cifara, koristi direktno
            formattedAccount = rawAccount;
        } else if (rawAccount.length() < 18) {
            // Dopuna nulama sa leve strane do 18 cifara
            // Ovo je tačno kako dokumentacija kaže
            formattedAccount = String.format("%018d", Long.parseLong(rawAccount));
        } else {
            // Ako je duži od 18 cifara, uzmi poslednjih 18
            formattedAccount = rawAccount.substring(rawAccount.length() - 18);
        }

        // 2. Priprema iznosa (Tag I) - NBS zahteva zarez umesto tačke
        String formattedAmount = String.format("%.2f", request.getAmount()).replace(".", ",");

        // 3. Priprema opisa (Tag S) - MORA BITI MAKSIMALNO 35 KARAKTERA
        String description = "Placanje porudzbine " + request.getPspTransactionId();
        if (description.length() > 35) {
            // Skrati UUID na prvih 8 karaktera ako je predugačak
            String shortUuid = request.getPspTransactionId().substring(0, 8);
            description = "Placanje porudzbine " + shortUuid;

            // Ako je i dalje predugačak, skrati opis
            if (description.length() > 35) {
                description = description.substring(0, 35);
            }
        }

        // 4. Sklapanje stringa koristeći pipe (|) kao separator
        // VAŽNO: String ne sme početi niti se završiti pipe karakterom
        StringBuilder ips = new StringBuilder();
        ips.append("K:PR");         // Tag K - obavezan
        ips.append("|V:01");        // Tag V - obavezan, verzija 01
        ips.append("|C:1");         // Tag C - obavezan, UTF-8
        ips.append("|R:").append(formattedAccount); // Tag R - obavezan, 18 cifara
        ips.append("|N:").append(merchant.getAccount().getOwnerName()); // Tag N - obavezan, max 70 karaktera
        ips.append("|I:RSD").append(formattedAmount); // Tag I - obavezan, format: RSDiznos,decimale
        ips.append("|SF:289");      // Tag SF - obavezan, šifra plaćanja (289 = bezgotovinsko)
        ips.append("|S:").append(description); // Tag S - opcioni, max 35 karaktera

        return ips.toString();
    }
}