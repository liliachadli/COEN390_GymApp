package com.example.smartgymtracker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.util.Base64;

public class Password {
    private static final int ITERATIONS = 600000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public static String hashPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {

        // Generate the random 16-byte salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        // Combines the password and salt with 600,000 iterations and a 256-bit key
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        try{
            // Generates the hash
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            // Makes the hash into a byte array
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Converts the salt byte array and the hash byte array
            String saltString = Base64.encodeToString(salt, Base64.NO_WRAP);
            String hashString = Base64.encodeToString(hash, Base64.NO_WRAP);

            // Combines all elements to store in the db
            String storedPassword = ITERATIONS + ":" + saltString + ":" + hashString;

            return storedPassword;

        } finally {
            spec.clearPassword();
        }

    }

    public static boolean checkPassword(String password, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] parts = storedPassword.split(":");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] hash = Base64.decode(parts[2], Base64.NO_WRAP);

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] newHash = factory.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(hash, newHash);

        } finally {
            spec.clearPassword();
        }
    }
}
