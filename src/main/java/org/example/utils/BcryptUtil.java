package org.example.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BcryptUtil {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Vérifier un mot de passe (compatible PHP password_verify)
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    // Hasher un mot de passe (optionnel)
    public static String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    // Pour tester
    public static void main(String[] args) {
        // Tester avec un hash PHP
        String phpHash = "$2y$13$00gookoXhevVDTH1qbPzK.EV56BvBTn9HLmuPiyuIBH1GSXo4fr56";
        String password = "password123";

        boolean matches = verifyPassword(password, phpHash);
        System.out.println("Mot de passe: " + password);
        System.out.println("Hash PHP: " + phpHash);
        System.out.println("Correspond: " + matches);

        // Générer un nouveau hash
        String newHash = hashPassword("password123");
        System.out.println("Nouveau hash Java: " + newHash);
    }
}