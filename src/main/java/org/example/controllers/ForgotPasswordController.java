package org.example.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.services.EmailService;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.ValidationUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;

    private EmailService emailService = new EmailService();

    @FXML
    public void sendResetLink() {
        String email = emailField.getText().trim();

        if (!ValidationUtils.isEmailValide(email)) {
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            messageLabel.setText("Veuillez entrer une adresse email valide.");
            return;
        }

        Button sourceButton = (Button) emailField.getScene().lookup(".button");
        if (sourceButton != null) sourceButton.setDisable(true);

        Task<Void> resetTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT user_id, nom, prenom FROM user WHERE email = ?")) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int userId = rs.getInt("user_id");
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        String resetToken = emailService.generateToken();
                        try {
                            emailService.storeResetToken(userId, resetToken, conn);
                        } catch (Exception e) {
                            // Si la colonne n'existe pas, on utilise une approche alternative
                            String updateQuery = "UPDATE user SET reset_token = ? WHERE user_id = ?";
                            PreparedStatement updatePs = conn.prepareStatement(updateQuery);
                            updatePs.setString(1, resetToken);
                            updatePs.setInt(2, userId);
                            updatePs.executeUpdate();
                        }
                        emailService.sendResetPasswordEmail(email, prenom + " " + nom, resetToken);
                    } else {
                        throw new Exception("Aucun compte trouvé avec cet email");
                    }
                }
                return null;
            }
        };

        resetTask.setOnSucceeded(e -> {
            messageLabel.setStyle("-fx-text-fill: #059669;");
            messageLabel.setText("Un lien de réinitialisation a été envoyé à votre adresse email.");
            if (sourceButton != null) sourceButton.setDisable(false);
        });

        resetTask.setOnFailed(e -> {
            messageLabel.setStyle("-fx-text-fill: #DC2626;");
            String errorMsg = resetTask.getException().getMessage();
            if (errorMsg.contains("Aucun compte")) {
                messageLabel.setText("Aucun compte trouvé avec cet email.");
            } else {
                messageLabel.setText("Erreur: " + errorMsg);
            }
            if (sourceButton != null) sourceButton.setDisable(false);
        });

        new Thread(resetTask).start();
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 640, 480));
            stage.setTitle("UniMind - Connexion");
            stage.setResizable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}