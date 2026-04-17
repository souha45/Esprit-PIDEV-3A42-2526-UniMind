package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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

    // ── Champs FXML ───────────────────────────────────────────────────
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private Button        btnOeilLogin;
    @FXML private HBox          passwordBox;

    // Labels d'erreur inline (sous chaque champ)
    @FXML private Label erreurEmail;      // sous emailField
    @FXML private Label erreurPassword;   // sous passwordBox

    // Message d'erreur global (identifiants incorrects / erreur serveur)
    @FXML private Label messageErreur;

    // ── État ──────────────────────────────────────────────────────────
    private boolean passwordShown = false;

    // ── Services ──────────────────────────────────────────────────────
    private final AdminService       adminService       = new AdminService();
    private final EtudiantService    etudiantService    = new EtudiantService();
    private final PsychologueService psychologueService = new PsychologueService();
    private final ResponsableService responsableService = new ResponsableService();

    // ══════════════════════════════════════════════════════════════════
    //  STYLES — bordures normales / erreur
    // ══════════════════════════════════════════════════════════════════

    private static final String BORDER_NORMAL = "-fx-border-color: #DDD6FE;";
    private static final String BORDER_ERROR  = "-fx-border-color: #DC2626;";

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS — affichage des erreurs (null-safe)
    // ══════════════════════════════════════════════════════════════════

    /** Affiche un message d'erreur rouge sous le champ concerné. */
    private void showFieldError(Label label, String msg) {
        if (label == null) return;
        label.setText("⚠  " + msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    /** Cache le message d'erreur d'un champ. */
    private void hideFieldError(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    /** Affiche le message d'erreur global (bannière rouge). */
    private void showGlobalError(String msg) {
        if (messageErreur == null) return;
        messageErreur.setText("⚠  " + msg);
        messageErreur.setVisible(true);
        messageErreur.setManaged(true);
    }

    /** Cache le message d'erreur global. */
    private void hideGlobalError() {
        if (messageErreur == null) return;
        messageErreur.setText("");
        messageErreur.setVisible(false);
        messageErreur.setManaged(false);
    }

    /** Applique une bordure rouge à un TextField. */
    private void markError(TextField field) {
        if (field == null) return;
        String s = field.getStyle().replace(BORDER_NORMAL, "").replace(BORDER_ERROR, "");
        field.setStyle(s + " " + BORDER_ERROR + " -fx-border-width: 1.5;");
    }

    /** Applique une bordure rouge au conteneur HBox du mot de passe. */
    private void markBoxError(HBox box) {
        if (box == null) return;
        String s = box.getStyle().replace(BORDER_NORMAL, "").replace(BORDER_ERROR, "");
        box.setStyle(s + " " + BORDER_ERROR + " -fx-border-width: 1.5;");
    }

    /** Remet la bordure normale à un TextField. */
    private void resetError(TextField field) {
        if (field == null) return;
        String s = field.getStyle()
                .replace(BORDER_ERROR, "")
                .replace("-fx-border-color: #DC2626;", "")
                .replace("-fx-border-width: 1.5;", "");
        field.setStyle(s + " " + BORDER_NORMAL + " -fx-border-width: 1.5;");
    }

    /** Remet la bordure normale au conteneur HBox. */
    private void resetBoxError(HBox box) {
        if (box == null) return;
        String s = box.getStyle()
                .replace(BORDER_ERROR, "")
                .replace("-fx-border-color: #DC2626;", "")
                .replace("-fx-border-width: 1.5;", "");
        box.setStyle(s + " " + BORDER_NORMAL + " -fx-border-width: 1.5;");
    }

    /** Efface toutes les erreurs et remet les styles normaux. */
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
        // 1. Efface toutes les erreurs précédentes
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

        // Si au moins un champ est invalide → on s'arrête ici
        if (!valid) return;

        // ── Tentative de connexion ────────────────────────────────────
        try {
            User user = UserService.connexionGenerale(
                    email, password,
                    MyDataBase_Unimind.getInstance().getConnection(),
                    adminService, etudiantService, psychologueService, responsableService);

            if (user == null) {
                // Identifiants incorrects → bannière globale + rouge sur les deux champs
                showGlobalError("Email ou mot de passe incorrect. Veuillez réessayer.");
                markError(emailField);
                markBoxError(passwordBox);
                return;
            }

            // ── Redirection selon le rôle ─────────────────────────────
            Stage currentStage = (Stage) emailField.getScene().getWindow();

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
            showGlobalError("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════

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
            showGlobalError("Erreur lors du chargement : " + e.getMessage());
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