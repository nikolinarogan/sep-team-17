package com.bank.tools;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CryptoUtil {

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final String HMAC_ALGO = "HmacSHA256"; // HMAC je obavezan za pretragu
    private static final int GCM_TAG_LENGTH = 128; // 128 bita auth tag
    private static final int GCM_IV_LENGTH = 12; // 12 bajtova (96 bita) je standard za GCM

    @Value("${security.encryption.key}")
    private String encryptionKeyBase64;

    @Value("${security.hashing.salt}")
    private String hashingSaltBase64;

    private static SecretKey aesKey;
    private SecretKey hmacKey;

    @PostConstruct
    public void init() {
        // Dekodiramo ključeve pri startovanju aplikacije
        // Ako pukne ovde, aplikacija ne sme da se podigne (Fail Fast)
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyBase64);
            this.aesKey = new SecretKeySpec(decodedKey, "AES");

            byte[] decodedSalt = Base64.getDecoder().decode(hashingSaltBase64);
            this.hmacKey = new SecretKeySpec(decodedSalt, HMAC_ALGO);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("CRITICAL SECURITY ERROR: Invalid Key Format in Configuration!");
        }
    }

    /**
     * ENKRIPCIJA (AES-256-GCM)
     * Koristi se za 'pan_encrypted'.
     * Svaki put generiše novi random IV, tako da isti PAN uvek daje drugačiji rezultat.
     */
    public static String encrypt(String plainData) {
        if (plainData == null || plainData.isEmpty()) return null;

        try {
            // 1. Generiši Random IV (Initialization Vector)
            // SecureRandom je obavezan, ne Random!
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. Setup Cipher-a
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

            // 3. Enkripcija
            byte[] cipherText = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));

            // 4. Spajanje: IV + CipherText (IV nam treba za dekripciju, nije tajan, ali mora biti unique)
            byte[] output = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);

            // Vraćamo kao Base64 string
            return Base64.getEncoder().encodeToString(output);

        } catch (Exception e) {
            // Logujemo samo tip greške, NIKAD podatke koji su pukli!
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * DEKRIPCIJA (AES-256-GCM)
     * Koristi se samo kada je SISTEMU potreban čist broj (npr. slanje ka kartičarskoj šemi).
     */
    public static String decrypt(String encryptedDataBase64) {
        if (encryptedDataBase64 == null || encryptedDataBase64.isEmpty()) return null;

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedDataBase64);

            // Validacija dužine
            if (decoded.length < GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8)) {
                throw new SecurityException("Corrupted encrypted data");
            }

            // 1. Izdvajanje IV-a
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            // 2. Izdvajanje CipherText-a
            byte[] cipherText = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            // 3. Setup Cipher-a
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);

            // 4. Dekripcija
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * HASHING (HMAC-SHA256)
     * Koristi se za 'pan_hash'.
     * Deterministički je (uvek isti izlaz za isti ulaz) što omogućava pretragu (findByPanHash).
     * Siguran je od Rainbow tabela jer koristi tajni 'hashingSaltBase64'.
     */
    public String hashForSearch(String plainData) {
        if (plainData == null) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(hmacKey);
            byte[] hashBytes = mac.doFinal(plainData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * MASKIRANJE (PCI DSS Req 3.3)
     * Prikazuje maksimalno prvih 6 i zadnjih 4.
     * Najsigurnije: Prikazati samo zadnjih 4.
     */
    public String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        // Uzimamo samo zadnja 4
        return "**** **** **** " + pan.substring(pan.length() - 4);
    }
}
