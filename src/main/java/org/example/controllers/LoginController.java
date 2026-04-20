package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.enums.Role;
import org.example.services.UserService;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.SessionManager;
import org.example.utils.ValidationUtils;

import java.io.IOException;

public class LoginController {

    // ── Champs FXML ───────────────────────────────────────────────────
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private Button        btnOeilLogin;
    @FXML private HBox          passwordBox;

    // Labels d'erreur inline (sous chaque champ)
    @FXML private Label erreurEmail;
    @FXML private Label erreurPassword;

    // Message d'erreur global (identifiants incorrects / erreur serveur)
    @FXML private Label messageErreur;

    // ── État ──────────────────────────────────────────────────────────
    private boolean passwordShown = false;

    // ══════════════════════════════════════════════════════════════════
    //  STYLES — bordures normales / erreur
    // ══════════════════════════════════════════════════════════════════

    private static final String BORDER_NORMAL = "-fx-border-color: #DDD6FE;";
    private static final String BORDER_ERROR  = "-fx-border-color: #DC2626;";

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS — affichage des erreurs (null-safe)
    // ══════════════════════════════════════════════════════════════════

    private void showFieldError(Label label, String msg) {
        if (label == null) return;
        label.setText("⚠  " + msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideFieldError(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showGlobalError(String msg) {
        if (messageErreur == null) return;
        messageErreur.setText("⚠  " + msg);
        messageErreur.setVisible(true);
        messageErreur.setManaged(true);
    }

    private void hideGlobalError() {
        if (messageErreur == null) return;
        messageErreur.setText("");
        messageErreur.setVisible(false);
        messageErreur.setManaged(false);
    }

    private void markError(TextField field) {
        if (field == null) return;
        String s = field.getStyle().replace(BORDER_NORMAL, "").replace(BORDER_ERROR, "");
        field.setStyle(s + " " + BORDER_ERROR + " -fx-border-width: 1.5;");
    }

    private void markBoxError(HBox box) {
        if (box == null) return;
        String s = box.getStyle().replace(BORDER_NORMAL, "").replace(BORDER_ERROR, "");
        box.setStyle(s + " " + BORDER_ERROR + " -fx-border-width: 1.5;");
    }

    private void resetError(TextField field) {
        if (field == null) return;
        String s = field.getStyle()
                .replace(BORDER_ERROR, "")
                .replace("-fx-border-color: #DC2626;", "")
                .replace("-fx-border-width: 1.5;", "");
        field.setStyle(s + " " + BORDER_NORMAL + " -fx-border-width: 1.5;");
    }

    private void resetBoxError(HBox box) {
        if (box == null) return;
        String s = box.getStyle()
                .replace(BORDER_ERROR, "")
                .replace("-fx-border-color: #DC2626;", "")
                .replace("-fx-border-width: 1.5;", "");
        box.setStyle(s + " " + BORDER_NORMAL + " -fx-border-width: 1.5;");
    }

    private void clearAllErrors() {
        hideFieldError(erreurEmail);
        hideFieldError(erreurPassword);
        hideGlobalError();
        resetError(emailField);
        resetBoxError(passwordBox);
    }

    // ══════════════════════════════════════════════════════════════════
    //  TOGGLE — affichage mot de passe
    // ══════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════
    //  SE CONNECTER — validation complète
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void seConnecter() {
        clearAllErrors();

        String email    = emailField.getText().trim();
        String password = passwordShown
                ? passwordVisible.getText().trim()
                : passwordField.getText().trim();

        boolean valid = true;

        // ── Validation email ──────────────────────────────────────────
        if (!ValidationUtils.isNonVide(email)) {
            showFieldError(erreurEmail, "L'adresse email est obligatoire.");
            markError(emailField);
            valid = false;
        } else if (!ValidationUtils.isEmailValide(email)) {
            showFieldError(erreurEmail, ValidationUtils.messageEmail());
            markError(emailField);
            valid = false;
        }

        // ── Validation mot de passe ───────────────────────────────────
        if (!ValidationUtils.isNonVide(password)) {
            showFieldError(erreurPassword, "Le mot de passe est obligatoire.");
            markBoxError(passwordBox);
            valid = false;
        }

        if (!valid) return;

        // ── Tentative de connexion ────────────────────────────────────
        try {
            User user = UserService.connexionGenerale(
                    email, password,
                    MyDataBase_Unimind.getInstance().getConnection());

            if (user == null) {
                showGlobalError("Email ou mot de passe incorrect. Veuillez réessayer.");
                markError(emailField);
                markBoxError(passwordBox);
                return;
            }

            // Stocker l'utilisateur dans SessionManager
            SessionManager.getInstance().setCurrentUser(user);

            // ── Redirection selon le rôle ─────────────────────────────
            redirigerSelonRole(user.getRole());

        } catch (Exception e) {
            showGlobalError("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  REDIRECTION selon le rôle
    // ══════════════════════════════════════════════════════════════════

    private void redirigerSelonRole(Role role) throws IOException {
        Stage currentStage = (Stage) emailField.getScene().getWindow();
        String fxmlPath;
        String titre;

        switch (role) {
            case ADMIN -> {
                fxmlPath = "/admin_dashboard.fxml";
                titre = "UniMind — Administration";
            }
            case ETUDIANT -> {
                fxmlPath = "/dashboard_etudiant.fxml";
                titre = "UniMind — Espace Étudiant";
            }
            case PSYCHOLOGUE -> {
                // Tu n'as pas encore ce dashboard, affiche un message temporaire
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Rôle Psychologue");
                alert.setHeaderText("Dashboard Psychologue");
                alert.setContentText("Le dashboard psychologue sera ajouté après le merge.");
                alert.showAndWait();
                return;
            }
            case RESPONSABLE_ETUDIANT -> {
                fxmlPath = "/dashboard_responsable.fxml";
                titre = "UniMind — Espace Responsable";
            }
            default -> {
                showGlobalError("Rôle non reconnu.");
                return;
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.setScene(new Scene(loader.load(), 1200, 700));
        stage.show();
        currentStage.close();
    }

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void allerInscription() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Inscription");
        alert.setHeaderText("Inscription");
        alert.setContentText("Le formulaire d'inscription sera ajouté après le merge.");
        alert.showAndWait();
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
