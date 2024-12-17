package de.liebki.simplecrosschat.utils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class ConversationUtils {

    private ConversationUtils() {
    }

    public static String encrypt(String input, String salt) {
        try {
            Cipher cipher = getCipher(salt, Cipher.ENCRYPT_MODE);
            byte[] cipherText = cipher.doFinal(input.getBytes());

            return Base64.getEncoder().encodeToString(cipherText);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    public static String decrypt(String input, String salt) {
        try {
            Cipher cipher = getCipher(salt, Cipher.DECRYPT_MODE);
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(input));

            return new String(plainText);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    private static Cipher getCipher(String salt, int decryptMode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        String algorithm = "AES/CBC/PKCS5Padding";
        Cipher cipher = Cipher.getInstance(algorithm);

        SecretKey key = generateKey(salt);
        IvParameterSpec iv = generateIv(salt);

        cipher.init(decryptMode, key, iv);
        return cipher;
    }

    private static SecretKey generateKey(String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(salt.toCharArray(), salt.getBytes(), 65536, 256);

        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static IvParameterSpec generateIv(String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] ivBytes = digest.digest(salt.getBytes());

        return new IvParameterSpec(ivBytes, 0, 16);
    }
}