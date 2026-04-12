package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.controllers.admin.AdminDashboardController;
import org.example.entities.User;
import org.example.services.*;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.ValidationUtils;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageErreur;

    private AdminService adminService = new AdminService();
    private EtudiantService etudiantService = new EtudiantService();
    private PsychologueService psychologueService = new PsychologueService();
    private ResponsableService responsableService = new ResponsableService();

    @FXML
    public void seConnecter() {
        messageErreur.setText("");
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Validations ───────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(email)) {
            messageErreur.setText("Email obligatoire"); return;
        }
        if (!ValidationUtils.isEmailValide(email)) {
            messageErreur.setText(ValidationUtils.messageEmail()); return;
        }
        if (!ValidationUtils.isNonVide(password)) {
            messageErreur.setText("Mot de passe obligatoire"); return;
        }

        try {
            User user = UserService.connexionGenerale(email, password,
                    MyDataBase_Unimind.getInstance().getConnection(),
                    adminService, etudiantService, psychologueService, responsableService);

            if (user == null) {
                messageErreur.setText("Email ou mot de passe incorrect");
                return;
            }

            Stage currentStage = (Stage) emailField.getScene().getWindow();

            switch (user.getRole()) {
                case ADMIN -> {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/admin_dashboard.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("UniMind — Admin");
                    stage.setScene(new Scene(loader.load(), 1100, 700));
                    AdminDashboardController ctrl = loader.getController();
                    ctrl.setUser(user);
                    stage.show();
                }
                case ETUDIANT, PSYCHOLOGUE, RESPONSABLE_ETUDIANT -> {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/user_dashboard.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("UniMind — " + user.getPrenom() + " " + user.getNom());
                    stage.setScene(new Scene(loader.load(), 900, 650));
                    UserDashboardController ctrl = loader.getController();
                    ctrl.setUser(user);
                    stage.show();
                }
            }
            currentStage.close();

        } catch (Exception e) {
            messageErreur.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void allerInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/inscription.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Inscription — UniMind");
            stage.setScene(new Scene(loader.load(), 540, 600));
            stage.show();
            ((Stage) emailField.getScene().getWindow()).close();
        } catch (Exception e) {
            messageErreur.setText("Erreur : " + e.getMessage());
        }
    }
}