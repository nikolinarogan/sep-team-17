package com.bank.config;

import com.bank.model.Account;
import com.bank.model.Card;
import com.bank.model.Merchant;
import com.bank.repository.AccountRepository;
import com.bank.repository.CardRepository;
import com.bank.repository.MerchantRepository;
import com.bank.service.BankService;
import com.bank.tools.CryptoUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final BankService bankService;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final CryptoUtil cryptoUtil;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AccountRepository accountRepository,
                      CardRepository cardRepository,
                      MerchantRepository merchantRepository,
                      CryptoUtil cryptoUtil,
                      PasswordEncoder passwordEncoder, BankService bankService) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.merchantRepository = merchantRepository;
        this.cryptoUtil = cryptoUtil;
        this.passwordEncoder = passwordEncoder;
        this.bankService = bankService;
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

        String rawPan = "4111111111111111";
        String expDate = "12/30";

        String validTestCvv = bankService.generateCvv(rawPan, expDate);

        Card card = new Card();
        card.setPan(rawPan);
        card.setPanMasked(cryptoUtil.maskPan(rawPan));
        card.setPanHash(cryptoUtil.hashForSearch(rawPan));

        card.setCardHolderName("PERA PERIC");
        card.setExpirationDate(expDate);
        card.setAccount(buyerAccount);
        cardRepository.save(card);

        System.out.println("--------------------------------------------------");
        System.out.println("✅ PODACI UPISANI USPEŠNO");
        System.out.println("Shop: 310123456789121186 | PIN: 1111");
        System.out.println("Kupac: 222-222222-22     | PIN: 2222");
        System.out.println("--------------------------------------------------");
        System.out.println("💳 TEST KARTICA (Kopiraj ovo za Frontend):");
        System.out.println("PAN: " + rawPan);
        System.out.println("EXP: " + expDate);
        System.out.println("👉 CVV: " + validTestCvv + "  <--- OVO KORISTI PRI PLAĆANJU!");
        System.out.println("--------------------------------------------------");
    }
}