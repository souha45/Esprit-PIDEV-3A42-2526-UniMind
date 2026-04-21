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
            if (hashedPassword == null || plainPassword == null) {
                return false;
            }

            // ── Convertir $2y$ (PHP) → $2a$ (Java) ───────────────────
            // Les deux sont identiques algorithmiquement
            String hashPourVerif = hashedPassword;
            if (hashedPassword.startsWith("$2y$")) {
                hashPourVerif = "$2a$" + hashedPassword.substring(4);
            }

            BCrypt.Result result = BCrypt.verifyer().verify(
                    plainPassword.toCharArray(),
                    hashPourVerif
            );
            return result.verified;

        } catch (Exception e) {
            System.out.println("Erreur BCrypt : " + e.getMessage());
            return false;
        }
    }
}
