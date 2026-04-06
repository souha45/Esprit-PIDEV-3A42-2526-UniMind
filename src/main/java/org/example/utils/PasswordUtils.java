package org.example.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtils {

    // ── HASHER LE MOT DE PASSE ────────────────────────────────────────
    public static String hasher(String plainPassword) {
        return BCrypt.withDefaults().hashToString(13, plainPassword.toCharArray());
    }

    // ── VERIFIER LE MOT DE PASSE ──────────────────────────────────────
    public static boolean verifier(String plainPassword, String hashedPassword) {
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(
                    plainPassword.toCharArray(), hashedPassword
            );
            return result.verified;
        } catch (Exception e) {
            return false;
        }
    }
}