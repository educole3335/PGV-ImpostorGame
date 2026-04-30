package com.impostor.common;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utilidad de cifrado simétrico para proteger el tráfico cliente/servidor.
 *
 * El ejemplo usa AES/GCM con una clave compartida derivada de una frase
 * secreta común para cliente y servidor.
 */
public final class CryptoUtil {

    private static final String PASSPHRASE = "PGV-ImpostorGame::shared-secret-v1";
    private static final byte[] SALT = "PGV-ImpostorGame::salt".getBytes(StandardCharsets.UTF_8);
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 128;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SecretKey SECRET_KEY = buildSecretKey();

    private CryptoUtil() {
    }

    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el mensaje", e);
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return "";
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            if (payload.length < IV_LENGTH) {
                throw new IllegalStateException("El mensaje cifrado es demasiado corto");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el mensaje", e);
        }
    }

    private static SecretKey buildSecretKey() {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(PASSPHRASE.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
            SecretKey secret = factory.generateSecret(spec);
            return new SecretKeySpec(secret.getEncoded(), "AES");
        } catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}