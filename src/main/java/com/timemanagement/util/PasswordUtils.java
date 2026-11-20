package com.timemanagement.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Cryptographic utility for password hashing and verification using BCrypt.
 * Protects user credentials with adaptive salt and cost factor.
 */
public final class PasswordUtils {

    private static final int BCRYPT_WORK_FACTOR = 12;

    private PasswordUtils() {
        // Utility class
    }

    /**
     * Determines whether the given string conforms to standard BCrypt hash format.
     */
    public static boolean isBCryptHash(String password) {
        if (password == null) {
            return false;
        }
        // BCrypt hashes are 60 characters long and start with $2a$, $2b$, or $2y$
        return password.length() == 60 &&
               (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    /**
     * Hashes a plain-text password using BCrypt with salt.
     * If the input is already a valid BCrypt hash, returns it unchanged to prevent double-hashing.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        if (isBCryptHash(plainPassword)) {
            return plainPassword; // Avoid double-hashing
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    /**
     * Verifies candidate password against stored password representation.
     * Supports BCrypt hash verification with legacy plain-text fallback for smooth demo migration.
     */
    public static boolean verifyPassword(String candidatePassword, String storedPassword) {
        if (candidatePassword == null || storedPassword == null) {
            return false;
        }
        if (isBCryptHash(storedPassword)) {
            try {
                return BCrypt.checkpw(candidatePassword, storedPassword);
            } catch (Exception ex) {
                return false;
            }
        }
        // Legacy plain-text fallback comparison
        return candidatePassword.equals(storedPassword);
    }
}
