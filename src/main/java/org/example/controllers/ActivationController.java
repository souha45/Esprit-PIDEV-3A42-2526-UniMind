package org.example.controllers;

import org.example.services.EmailService;
import org.example.utils.MyDataBase_Unimind;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ActivationController {

    private EmailService emailService = new EmailService();

    public boolean activerCompte(int userId, String token) {
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection()) {
            // Vérifier d'abord si la colonne verification_token existe
            String checkColumnQuery = "SHOW COLUMNS FROM user LIKE 'verification_token'";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkColumnQuery);
                 ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    System.err.println("❌ La colonne verification_token n'existe pas dans la table user");
                    return false;
                }
            }

            // Vérifier le token
            String sql = "SELECT user_id, email, nom, prenom FROM user " +
                    "WHERE user_id = ? AND verification_token = ? AND is_verified = 0";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, token);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");

                        // Activer le compte
                        String updateSql = "UPDATE user SET is_verified = 1, verification_token = NULL WHERE user_id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setInt(1, userId);
                            int updated = updatePs.executeUpdate();

                            if (updated > 0) {
                                System.out.println("✅ Compte activé pour: " + email);

                                // Envoyer email de confirmation (optionnel)
                                try {
                                    emailService.sendWelcomeEmail(email, nom + " " + prenom, "user");
                                } catch (Exception e) {
                                    System.err.println("⚠️ Erreur envoi email bienvenue: " + e.getMessage());
                                }

                                return true;
                            }
                        }
                    } else {
                        System.out.println("❌ Token invalide ou compte déjà activé pour userId: " + userId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Méthode utilitaire pour afficher une alerte JavaFX
    public void showActivationResult(boolean success, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle(success ? "Activation réussie" : "Erreur d'activation");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}