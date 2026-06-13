package com.example.myapplication.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashing dyal password b SHA-256 + salt sabit.
 * Hadi ma kaملش perfect (BCrypt 7sen), walakin bzaf 7sen mn plaintext.
 * L'app m7alliya (SQLite) donc kafi l had l'7ala.
 */
public final class PasswordUtil {

    // Salt sabit dyal l'app — kayzid se3oba 3la rainbow tables basit.
    private static final String SALT = "HabitFlow$2024#salt";

    private PasswordUtil() {}

    /**
     * Kayrje3 hash hexadecimal dyal password.
     */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((SALT + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 deja mawjoud f kol Android, hada ma ywq3ch.
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
