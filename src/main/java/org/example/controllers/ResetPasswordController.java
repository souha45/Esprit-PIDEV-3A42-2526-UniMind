package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.PasswordUtils;
import org.example.utils.ValidationUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ResetPasswordController {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Label messageLabel;

    private String resetToken;
    private int userId;

    public void setResetToken(String token) {
        this.resetToken = token;
        verifyToken();
    }

    private void verifyToken() {
        String query = "SELECT user_id FROM user WHERE reset_token = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, resetToken);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
                messageLabel.setStyle("-fx-text-fill: #059669;");
                messageLabel.setText("✓ Compte vérifié. Entrez votre nouveau mot de passe.");
                resetButton.setDisable(false);
            } else {
                messageLabel.setStyle("-fx-text-fill: #DC2626;");
                messageLabel.setText("✗ Lien invalide ou expiré.");
                resetButton.setDisable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur: " + e.getMessage());
        }
    }
    private void verifyTokenDirect() {
        String query = "SELECT user_id FROM user WHERE reset_token = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, resetToken);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
                messageLabel.setStyle("-fx-text-fill: #059669;");
                messageLabel.setText("✓ Compte vérifié. Entrez votre nouveau mot de passe.");
                resetButton.setDisable(false);
            } else {
                messageLabel.setStyle("-fx-text-fill: #DC2626;");
                messageLabel.setText("✗ Lien invalide ou expiré.");
                resetButton.setDisable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur: " + e.getMessage());
        }
    }



    public void setUserEmail(String email) {
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM user WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
                messageLabel.setStyle("-fx-text-fill: #059669;");
                messageLabel.setText("✓ Email confirmé. Entrez votre nouveau mot de passe.");
                resetButton.setDisable(false);
            } else {
                messageLabel.setStyle("-fx-text-fill: #DC2626;");
                messageLabel.setText("✗ Aucun compte trouvé avec cet email.");
                resetButton.setDisable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void resetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!ValidationUtils.isNonVide(newPassword)) {
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            messageLabel.setText("Le mot de passe est obligatoire");
            return;
        }
        if (!ValidationUtils.isPasswordValide(newPassword)) {
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            messageLabel.setText(ValidationUtils.messagePassword());
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            messageLabel.setText(ValidationUtils.messagePasswordConfirm());
            return;
        }

        if (userId == 0) {
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            messageLabel.setText("Email non confirmé. Veuillez entrer votre email.");
            return;
        }

        resetButton.setDisable(true);
        resetButton.setText("Chargement...");

        String query = "UPDATE user SET password = ? WHERE user_id = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, PasswordUtils.hasher(newPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();

            messageLabel.setStyle("-fx-text-fill: #059669;");
            messageLabel.setText("✓ Mot de passe modifié ! Redirection...");

            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                        Stage stage = (Stage) resetButton.getScene().getWindow();
                        stage.setScene(new Scene(loader.load(), 900, 600));
                        stage.setTitle("UniMind - Connexion");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            messageLabel.setText("Erreur: " + e.getMessage());
            resetButton.setDisable(false);
            resetButton.setText("Réinitialiser");
        }
    }
}