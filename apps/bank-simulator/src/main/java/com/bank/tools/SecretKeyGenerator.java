package com.bank.tools;

import javax.crypto.KeyGenerator;
import java.util.Base64;

public class SecretKeyGenerator {
    public static void main(String[] args) throws Exception {
        // 1. Generiši AES-256 ključ za enkripciju
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        String encryptionKey = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());

        // 2. Generiši HMAC-SHA256 ključ za heširanje (salt)
        KeyGenerator hmacGen = KeyGenerator.getInstance("HmacSHA256");
        hmacGen.init(256);
        String hashingSalt = Base64.getEncoder().encodeToString(hmacGen.generateKey().getEncoded());

        System.out.println("--- KOPIRAJ OVE VREDNOSTI U ENVIRONMENT VARIJABLE ---");
        System.out.println("BANK_ENCRYPTION_KEY=" + encryptionKey);
        System.out.println("BANK_HASHING_SALT=" + hashingSalt);
    }
}
