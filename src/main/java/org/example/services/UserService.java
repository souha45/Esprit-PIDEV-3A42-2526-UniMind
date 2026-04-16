package org.example.services;

import org.example.models.Etudiant;
import org.example.models.Psychologue;
import org.example.models.User;
import org.example.utils.BcryptUtil;
import org.example.utils.MyDataBase_Unimind;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    private Connection con;

    public UserService() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    public User authentifier(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ?";

        try {
            System.out.println("=== AUTHENTIFICATION ===");
            System.out.println("Email: " + email);

            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, email.trim());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                String role = rs.getString("role");

                System.out.println("Hash en base: " + hashedPassword);
                System.out.println("Mot de passe saisi: " + password);

                // Vérifier avec Spring Security (compatible PHP)
                boolean passwordMatch = BcryptUtil.verifyPassword(password, hashedPassword);

                System.out.println("Correspondance: " + passwordMatch);

                if (passwordMatch) {
                    int userId = rs.getInt("user_id");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    String cin = rs.getString("cin");
                    boolean isActive = rs.getBoolean("is_active");
                    boolean isVerified = rs.getBoolean("is_verified");

                    System.out.println("✅ Authentification réussie !");
                    System.out.println("   ID: " + userId);
                    System.out.println("   Nom: " + prenom + " " + nom);
                    System.out.println("   Rôle: " + role);

                    if ("psychologue".equalsIgnoreCase(role)) {
                        Psychologue psy = new Psychologue();
                        psy.setUserId(userId);
                        psy.setNom(nom);
                        psy.setPrenom(prenom);
                        psy.setEmail(email);
                        psy.setCin(cin);
                        psy.setActive(isActive);
                        psy.setVerified(isVerified);
                        return psy;
                    } else if ("etudiant".equalsIgnoreCase(role)) {
                        Etudiant etu = new Etudiant();
                        etu.setUserId(userId);
                        etu.setNom(nom);
                        etu.setPrenom(prenom);
                        etu.setEmail(email);
                        etu.setCin(cin);
                        etu.setActive(isActive);
                        etu.setVerified(isVerified);
                        return etu;
                    }
                } else {
                    System.out.println("❌ Mot de passe incorrect");
                    return null;
                }
            } else {
                System.out.println("❌ Email non trouvé: " + email);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public Psychologue getPsychologueById(int userId) {
        String sql = "SELECT * FROM user WHERE user_id = ? AND role = 'psychologue'";

        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                Psychologue psy = new Psychologue();
                psy.setUserId(rs.getInt("user_id"));
                psy.setNom(rs.getString("nom"));
                psy.setPrenom(rs.getString("prenom"));
                psy.setEmail(rs.getString("email"));
                psy.setCin(rs.getString("cin"));
                psy.setActive(rs.getBoolean("is_active"));
                psy.setVerified(rs.getBoolean("is_verified"));
                return psy;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du psychologue: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}