package com.wenmin.prometheus.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static String encrypt(String plainText, String key) {
        try {
            byte[] keyBytes = deriveKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data. Tries the new SHA-256 derived key first; if that fails,
     * falls back to the legacy zero-padded key for backward compatibility
     * with data encrypted before the key derivation fix.
     */
    public static String decrypt(String encryptedText, String key) {
        // Try new key derivation first
        try {
            return decryptWithKeyBytes(encryptedText, deriveKey(key));
        } catch (Exception e) {
            // Fallback to legacy key derivation for backward compatibility
            try {
                return decryptWithKeyBytes(encryptedText, legacyNormalizeKey(key));
            } catch (Exception fallbackEx) {
                throw new RuntimeException("Decryption failed", e);
            }
        }
    }

    private static String decryptWithKeyBytes(String encryptedText, byte[] keyBytes) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        byte[] cipherText = new byte[combined.length - iv.length];
        System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    /**
     * Derive a 32-byte AES key from the input key using SHA-256.
     * This provides a cryptographically strong key regardless of input length.
     */
    private static byte[] deriveKey(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    /**
     * Legacy key normalization (zero-padded). Retained only for backward-compatible
     * decryption of data encrypted before the SHA-256 key derivation fix.
     * @deprecated Use {@link #deriveKey(String)} for new encryption.
     */
    @Deprecated
    private static byte[] legacyNormalizeKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[32];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
        return normalized;
    }
}
