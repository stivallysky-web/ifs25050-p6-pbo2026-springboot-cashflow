package org.delcom.ifs25050springbootcashflow.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        MessageDigest digest = messageDigest();
        byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    static MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 tidak tersedia", ex);
        }
    }

    public static boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}