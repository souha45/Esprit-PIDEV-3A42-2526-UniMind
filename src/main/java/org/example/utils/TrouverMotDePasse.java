package org.example.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class TrouverMotDePasse {

    public static void main(String[] args) {

        // ── Hash stocké en base de données ────────────────────────────
        String hashBD = "$2y$13$xfeobzGo4Di3L2uvHKIv9OHkoWCsQFwq2XLF//sR2mAH3eBgIml/K";

        // ── Convertir $2y$ → $2a$ (PHP → Java) ───────────────────────
        String hashPourVerif = hashBD;
        if (hashBD.startsWith("$2y$")) {
            hashPourVerif = "$2a$" + hashBD.substring(4);
        }

        System.out.println("===========================================");
        System.out.println("  Recherche du mot de passe BCrypt");
        System.out.println("===========================================");
        System.out.println("Hash : " + hashBD);
        System.out.println();

        // ── Liste des mots de passe à tester ──────────────────────────
        String[] candidats = {
                // Mots de passe courants
                "Admin123!", "Admin123", "admin123",
                "Password1", "Password1!", "password123",
                "Unimind123!", "Unimind123", "unimind123",
                "Esprit123!", "Esprit123", "esprit123",
                "Test1234!", "Test1234", "test1234",
                "Azerty123!", "Azerty123", "azerty123",
                "Louati123!", "Louati123", "louati123",
                "Islem123!", "Islem123", "islem123",
                "Admin@123", "Admin@2024", "Admin@2025",
                "UniMind@123", "UniMind2024", "UniMind2025",
                "123456789", "12345678", "1234567890",
                "Motdepasse1", "Motdepasse1!",
                "Pidev2024!", "Pidev2025!", "Esprit2024!",
                // Ajoute tes propres candidats ici
                "MonMotDePasse1",
                "MonPassword1!",
        };

        boolean trouve = false;

        for (String candidat : candidats) {
            try {
                BCrypt.Result result = BCrypt.verifyer().verify(
                        candidat.toCharArray(),
                        hashPourVerif
                );

                if (result.verified) {
                    System.out.println("✅ MOT DE PASSE TROUVE : " + candidat);
                    trouve = true;
                    break;
                } else {
                    System.out.println("✗ " + candidat);
                }
            } catch (Exception e) {
                System.out.println("⚠ Erreur pour '" + candidat + "' : " + e.getMessage());
            }
        }

        if (!trouve) {
            System.out.println();
            System.out.println("❌ Mot de passe non trouvé dans la liste.");
            System.out.println();
            System.out.println("─── Solution : Réinitialiser le mot de passe ───");
            System.out.println("Lance cette commande SQL dans MySQL :");
            System.out.println();

            // Générer un nouveau hash pour "Admin123!"
            String nouveauMdp = "Admin123!";
            String nouveauHash = BCrypt.withDefaults().hashToString(13, nouveauMdp.toCharArray());
            System.out.println("UPDATE user");
            System.out.println("SET password = '" + nouveauHash + "'");
            System.out.println("WHERE email = 'louatiislem74@gmail.com';");
            System.out.println();
            System.out.println("Nouveau mot de passe : " + nouveauMdp);
        }

        System.out.println("===========================================");
    }
}