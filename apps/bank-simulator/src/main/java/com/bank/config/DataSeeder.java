package com.bank.config;

import com.bank.model.Account;
import com.bank.model.Card;
import com.bank.model.Merchant;
import com.bank.repository.AccountRepository;
import com.bank.repository.CardRepository;
import com.bank.repository.MerchantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;

    public DataSeeder(AccountRepository accountRepository, CardRepository cardRepository, MerchantRepository merchantRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // PROVJERA: Ako već imamo podatke, ne ubacuj ništa da ne bi došlo do greške
        if (accountRepository.count() > 0) {
            System.out.println("Podaci već postoje u bazi. Preskačem inicijalizaciju.");
            return;
        }

        System.out.println("--- KREIRANJE POČETNIH PODATAKA ZA BANKU ---");

        // 1. Kreiramo RAČUN ZA PRODAVCA (Web Shop - Rent A Car)
        Account shopAccount = new Account();
        shopAccount.setOwnerName("Rent-A-Car Agency");
        shopAccount.setAccountNumber("111-111111-11");
        shopAccount.setBalance(new BigDecimal("0.00"));
        shopAccount.setReservedFunds(new BigDecimal("0.00"));
        shopAccount.setEmail("shop@example.com");
        accountRepository.save(shopAccount);

        // 2. Registrujemo PRODAVCA u sistemu Banke
        Merchant merchant = new Merchant();
        merchant.setMerchantId("prodavac123");
        merchant.setMerchantPassword("sifra123");
        merchant.setAccount(shopAccount);
        merchantRepository.save(merchant);

        // 3. Kreiramo RAČUN ZA KUPCA
        Account buyerAccount = new Account();
        buyerAccount.setOwnerName("Pera Peric");
        buyerAccount.setAccountNumber("222-222222-22");
        buyerAccount.setBalance(new BigDecimal("100000.00"));
        buyerAccount.setReservedFunds(new BigDecimal("0.00"));
        buyerAccount.setEmail("pera@example.com");
        accountRepository.save(buyerAccount);

        // 4. Izdajemo KARTICU kupcu
        Card card = new Card();
        card.setPan("1234567812345678");
        card.setCardHolderName("PERA PERIC");
        card.setExpirationDate("12/30");
        card.setSecurityCode("123");
        card.setAccount(buyerAccount);  
        cardRepository.save(card);

        System.out.println("--- PODACI USPEŠNO UPISANI ---");
        System.out.println("Merchant ID za PSP: prodavac123");
        System.out.println("Merchant Pass za PSP: sifra123");
        System.out.println("Tvoj broj kartice: 1234567812345678");
    }
}