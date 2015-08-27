package com.android305.lights.util.encryption;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.text.BasicTextEncryptor;

public class Encryption {
    String secret;

    public Encryption(String secret) {
        this.secret = secret;
    }

    public static String decrypt(String input, String secret) throws EncryptionOperationNotPossibleException {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(secret);
        return textEncryptor.decrypt(input);
    }

    public static String encrypt(String input, String secret) throws EncryptionOperationNotPossibleException {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(secret);
        return textEncryptor.encrypt(input);
    }

    /**
     * Encrypt a string
     *
     * @param input string to encrypt
     * @return encrypted string
     */
    public String encrypt(String input) throws EncryptionOperationNotPossibleException {
        return Encryption.encrypt(input, secret);
    }

    /**
     * Decrypt a string
     *
     * @param input string to decrypt
     * @return decrypted string
     */
    public String decrypt(String input) throws EncryptionOperationNotPossibleException {
        return Encryption.decrypt(input, secret);
    }

}
