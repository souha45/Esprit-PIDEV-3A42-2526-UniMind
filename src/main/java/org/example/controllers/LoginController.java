package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.controllers.admin.AdminDashboardController;
import org.example.controllers.DashboardEtudiantController;
import org.example.controllers.DashboardPsychologueController;
import org.example.controllers.DashboardResponsableController;
import org.example.entities.User;
import org.example.services.*;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.ValidationUtils;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageErreur;
    @FXML private TextField passwordVisible;
    @FXML private Button btnOeilLogin;

    private boolean passwordShown = false;

    private AdminService adminService = new AdminService();
    private EtudiantService etudiantService = new EtudiantService();
    private PsychologueService psychologueService = new PsychologueService();
    private ResponsableService responsableService = new ResponsableService();

    // ── Toggle affichage mot de passe ──────────────────────────────────
    @FXML
    public void togglePasswordLogin() {
        passwordShown = !passwordShown;
        if (passwordShown) {
            passwordVisible.setText(passwordField.getText());
            passwordVisible.setVisible(true);  passwordVisible.setManaged(true);
            passwordField.setVisible(false);   passwordField.setManaged(false);
            btnOeilLogin.setText("🙈");
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);    passwordField.setManaged(true);
            passwordVisible.setVisible(false); passwordVisible.setManaged(false);
            btnOeilLogin.setText("👁");
        }
    }

    @FXML
    public void seConnecter() {
        messageErreur.setText("");
        String email    = emailField.getText().trim();
        String password = passwordShown ? passwordVisible.getText() : passwordField.getText();

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

            // ── Redirection selon le rôle ─────────────────────────────
            switch (user.getRole()) {

                case ADMIN -> {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/admin_dashboard.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("UniMind — Administration");
                    stage.setScene(new Scene(loader.load(), 1200, 700));
                    AdminDashboardController ctrl = loader.getController();
                    ctrl.setUser(user);
                    stage.show();
                }

                case ETUDIANT -> {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/dashboard_etudiant.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("UniMind — Espace Étudiant");
                    stage.setScene(new Scene(loader.load(), 1000, 700));
                    DashboardEtudiantController ctrl = loader.getController();
                    ctrl.setUser(user);
                    stage.show();
                }

                case PSYCHOLOGUE -> {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/dashboard_psychologue.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("UniMind — Espace Psychologue");
                    stage.setScene(new Scene(loader.load(), 1000, 700));
                    DashboardPsychologueController ctrl = loader.getController();
                    ctrl.setUser(user);
                    stage.show();
                }

                case RESPONSABLE_ETUDIANT -> {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/dashboard_responsable.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("UniMind — Espace Responsable");
                    stage.setScene(new Scene(loader.load(), 1000, 700));
                    DashboardResponsableController ctrl = loader.getController();
                    ctrl.setUser(user);
                    stage.show();
                }
            }
            currentStage.close();

        } catch (Exception e) {
            messageErreur.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
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
    @FXML
    public void allerReinitialisationMdp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot de passe oublié");
        alert.setHeaderText("Réinitialisation du mot de passe");
        alert.setContentText("Un email de réinitialisation sera envoyé à l'adresse indiquée.");
        alert.showAndWait();
    }
}