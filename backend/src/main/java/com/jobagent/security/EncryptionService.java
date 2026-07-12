package com.jobagent.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptionService {

    public static final String PREFIX = "ENC(";
    public static final String SUFFIX = ")";

    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 210_000;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EncryptionService() {
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    public static String encrypt(String plainText, String masterKey) {
        validateMasterKey(masterKey);
        try {
            byte[] salt = randomBytes(SALT_BYTES);
            byte[] iv = randomBytes(IV_BYTES);
            SecretKey key = deriveKey(masterKey, salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(salt.length + iv.length + encrypted.length);
            payload.put(salt);
            payload.put(iv);
            payload.put(encrypted);

            return PREFIX + Base64.getEncoder().encodeToString(payload.array()) + SUFFIX;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    public static String decrypt(String encryptedValue, String masterKey) {
        validateMasterKey(masterKey);
        if (!isEncrypted(encryptedValue)) {
            return encryptedValue;
        }

        try {
            String encoded = encryptedValue.substring(PREFIX.length(), encryptedValue.length() - SUFFIX.length());
            byte[] payload = Base64.getDecoder().decode(encoded);
            if (payload.length <= SALT_BYTES + IV_BYTES) {
                throw new IllegalArgumentException("Encrypted value payload is invalid");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] salt = new byte[SALT_BYTES];
            byte[] iv = new byte[IV_BYTES];
            byte[] cipherText = new byte[payload.length - SALT_BYTES - IV_BYTES];
            buffer.get(salt);
            buffer.get(iv);
            buffer.get(cipherText);

            SecretKey key = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt encrypted configuration value", e);
        }
    }

    private static SecretKey deriveKey(String masterKey, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(masterKey.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static void validateMasterKey(String masterKey) {
        if (masterKey == null || masterKey.length() < 32) {
            throw new IllegalArgumentException("ENCRYPTION_KEY must be at least 32 characters");
        }
    }
}
