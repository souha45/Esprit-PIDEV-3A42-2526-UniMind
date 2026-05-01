package org.example.services;

import org.example.entities.User;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.List;
import java.util.Locale;

/**
 * Service de modération automatique du contenu.
 * - Détecte les mots toxiques/inappropriés
 * - Gère les infractions par utilisateur
 * - Bloque le compte à la 3ème infraction
 * - Envoie les emails d'avertissement/blocage
 */
public class ModerationService {

    private final Connection con;
    private final EmailForumService emailForumService = new EmailForumService();

    // ── Liste de mots interdits ──────────────────────────────
    // Complétez cette liste selon vos besoins
    private static final List<String> MOTS_INTERDITS = List.of(
            // Insultes générales
            "idiot", "imbécile", "crétin", "abruti", "connard", "con", "conne",
            "salaud", "salope", "pute", "putain", "merde", "bordel", "foutre",
            "enculé", "va te faire", "nique", "niquer", "bâtard", "batard",
            // Discriminations
            "raciste", "nazi", "fasciste",
            // Anglais
            "stupid", "idiot", "moron", "asshole", "bitch", "fuck", "shit",
            "damn", "crap", "bastard", "jerk",
            // Violence
            "tuer", "tue-toi", "suicide", "mort", "crève", "crever","kill"
    );

    public ModerationService() {
        this.con = MyDataBase_Unimind.getInstance().getConnection();
    }

    // ── Résultat d'une vérification de modération ────────────
    public record ResultatModeration(boolean estValide, String motDetecte) {
        public static ResultatModeration ok() {
            return new ResultatModeration(true, null);
        }
        public static ResultatModeration refus(String mot) {
            return new ResultatModeration(false, mot);
        }
    }

    /**
     * Vérifie le texte et gère l'infraction si nécessaire.
     * @return ResultatModeration (estValide=true si le contenu est propre)
     */
    public ResultatModeration verifierEtGerer(String texte, User user, String contexte) {
        String motDetecte = detecterMotInterdit(texte);
        if (motDetecte == null) return ResultatModeration.ok();

        // Enregistrer l'infraction
        int nbInfractions = enregistrerInfraction(user.getUserId(), motDetecte, contexte);

        // Gérer selon le nombre d'infractions
        if (nbInfractions >= 3) {
            // Bloquer le compte
            bloquerCompte(user.getUserId());
            // Email sévère en arrière-plan
            new Thread(() ->
                    emailForumService.sendEmailBlocage(user.getEmail(), user.getPrenom(), motDetecte)
            ).start();
        } else {
            // Email d'avertissement en arrière-plan
            new Thread(() ->
                    emailForumService.sendAvertissementModeration(
                            user.getEmail(), user.getPrenom(), motDetecte, nbInfractions)
            ).start();
        }

        return ResultatModeration.refus(motDetecte);
    }

    /**
     * Détecte le premier mot interdit trouvé dans le texte.
     * @return le mot détecté ou null si le texte est propre
     */
    public String detecterMotInterdit(String texte) {
        if (texte == null || texte.isBlank()) return null;
        String texteLower = texte.toLowerCase(Locale.FRENCH);

        for (String mot : MOTS_INTERDITS) {
            // Recherche en tant que mot entier (bordures de mots)
            if (texteLower.contains(mot.toLowerCase())) {
                return mot;
            }
        }
        return null;
    }

    /**
     * Enregistre une infraction et retourne le total d'infractions de l'utilisateur.
     */
    private int enregistrerInfraction(int userId, String mot, String contexte) {
        try {
            // Insérer l'infraction
            String sqlInsert = "INSERT INTO `infraction_moderation` " +
                    "(`user_id`, `mot_detecte`, `contexte`) VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sqlInsert);
            ps.setInt(1, userId);
            ps.setString(2, mot);
            ps.setString(3, contexte != null ? contexte.substring(0, Math.min(contexte.length(), 500)) : "");
            ps.executeUpdate();
            ps.close();

            // Compter le total des infractions
            String sqlCount = "SELECT COUNT(*) FROM `infraction_moderation` WHERE `user_id` = ?";
            PreparedStatement psCount = con.prepareStatement(sqlCount);
            psCount.setInt(1, userId);
            ResultSet rs = psCount.executeQuery();
            int count = rs.next() ? rs.getInt(1) : 1;
            rs.close();
            psCount.close();
            return count;
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Bloque le compte (is_active = 0).
     */
    private void bloquerCompte(int userId) {
        try {
            String sql = "UPDATE `user` SET `is_active` = 0 WHERE `user_id` = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
            ps.close();
            System.out.println("⛔ Compte bloqué — user_id: " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retourne le nombre d'infractions d'un utilisateur.
     */
    public int getNbInfractions(int userId) {
        try {
            String sql = "SELECT COUNT(*) FROM `infraction_moderation` WHERE `user_id` = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}