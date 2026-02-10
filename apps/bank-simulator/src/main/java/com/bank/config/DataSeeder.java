package com.bank.config;

import com.bank.model.Account;
import com.bank.model.Card;
import com.bank.model.Merchant;
import com.bank.repository.AccountRepository;
import com.bank.repository.CardRepository;
import com.bank.repository.MerchantRepository;
import com.bank.tools.CryptoUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final CryptoUtil cryptoUtil;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AccountRepository accountRepository,
                      CardRepository cardRepository,
                      MerchantRepository merchantRepository,
                      CryptoUtil cryptoUtil,
                      PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.merchantRepository = merchantRepository;
        this.cryptoUtil = cryptoUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Ako ima podataka, ne radi ništa (zato moraš prvo obrisati bazu!)
        if (accountRepository.count() > 0) {
            return;
        }

        System.out.println("--- KREIRANJE PODATAKA (SA SLIKE + ZAŠTITA) ---");

        // 1. RAČUN PRODAVCA (Rent-A-Car) - Podaci sa tvoje slike
        Account shopAccount = new Account();
        shopAccount.setOwnerName("Rent-A-Car Agency");
        shopAccount.setAccountNumber("310123456789121186"); // ✅ Tvoj broj računa
        shopAccount.setBalance(new BigDecimal("36973.00")); // ✅ Tvoj saldo
        shopAccount.setReservedFunds(new BigDecimal("0.00"));
        shopAccount.setEmail("shop@example.com");

        // 🔒 HEŠIRAN PIN (1111) - Sa slike, ali zaštićen
        shopAccount.setPin(passwordEncoder.encode("1111"));

        accountRepository.save(shopAccount);

        // 2. PRODAVAC (Merchant)
        Merchant merchant = new Merchant();
        merchant.setMerchantId("shop_123");
        merchant.setMerchantPassword(passwordEncoder.encode("sifra123"));
        merchant.setAccount(shopAccount);
        merchantRepository.save(merchant);

        // 3. RAČUN KUPCA (Pera Peric) - Podaci sa tvoje slike
        Account buyerAccount = new Account();
        buyerAccount.setOwnerName("Pera Peric");
        buyerAccount.setAccountNumber("222-222222-22"); // ✅ Tvoj broj računa
        buyerAccount.setBalance(new BigDecimal("63027.00")); // ✅ Tvoj saldo
        buyerAccount.setReservedFunds(new BigDecimal("0.00"));
        buyerAccount.setEmail("pera@example.com");

        // 🔒 HEŠIRAN PIN (2222) - Sa slike, ali zaštićen
        buyerAccount.setPin(passwordEncoder.encode("2222"));

        accountRepository.save(buyerAccount);

        // 4. KARTICA KUPCA
        String rawPan = "4111111111111111";
        String rawCvv = "123";

        Card card = new Card();
        card.setPan(rawPan); // Enkripcija (automatska)
        card.setPanMasked(cryptoUtil.maskPan(rawPan)); // Maskiranje **** 5678
        card.setPanHash(cryptoUtil.hashForSearch(rawPan)); // Hash za pretragu
        card.setCvvHash(passwordEncoder.encode(rawCvv)); // Hashovan CVV

        card.setCardHolderName("PERA PERIC");
        card.setExpirationDate("12/30");
        card.setAccount(buyerAccount);
        cardRepository.save(card);

        System.out.println("--- PODACI UPISANI ---");
        System.out.println("Shop ACC: 310123456789121186 | PIN: 1111 (sada radi!)");
        System.out.println("Pera ACC: 222-222222-22      | PIN: 2222 (sada radi!)");
    }
}