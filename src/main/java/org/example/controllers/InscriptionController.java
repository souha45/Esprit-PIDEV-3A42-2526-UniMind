package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.*;
import org.example.services.*;
import org.example.utils.ValidationUtils;

import java.sql.SQLException;

public class InscriptionController {

    // ── Étapes ────────────────────────────────────────────────────────
    @FXML private VBox etapeRole;
    @FXML private VBox etapeFormulaire;
    @FXML private Label labelTitreRole;

    // ── Champs communs ────────────────────────────────────────────────
    @FXML private TextField     nomField, prenomField, emailField, cinField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private CheckBox      conditionsCheck;

    // ── Labels d'erreur inline ────────────────────────────────────────
    @FXML private Label errNom, errPrenom, errEmail, errCin;
    @FXML private Label errPassword, errConfirm, errConditions;

    // ── Panneaux rôle ─────────────────────────────────────────────────
    @FXML private VBox panneauEtudiant, panneauPsy, panneauResponsable;

    // ── Champs Étudiant ───────────────────────────────────────────────
    @FXML private TextField identifiantField, etablissementField;
    @FXML private Label     errIdentifiant, errEtablissement;

    // ── Champs Psychologue ────────────────────────────────────────────
    @FXML private TextField specialiteField, adresseField, telephoneField;
    @FXML private Label     errSpecialite, errAdresse, errTelephone;

    // ── Champs Responsable ────────────────────────────────────────────
    @FXML private TextField posteField, etablissementRespField;
    @FXML private Label     errPoste, errEtablissementResp;

    // ── Message global (succès / erreur serveur) ──────────────────────
    @FXML private Label messageGlobal;

    // ── Rôle sélectionné ─────────────────────────────────────────────
    private String roleSelectionne = "";

    // ── Services ──────────────────────────────────────────────────────
    private final EtudiantService    etudiantService    = new EtudiantService();
    private final PsychologueService psychologueService = new PsychologueService();
    private final ResponsableService responsableService = new ResponsableService();

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION ÉTAPES
    // ══════════════════════════════════════════════════════════════════

    @FXML public void selectionnerEtudiant()    { ouvrirFormulaire("Etudiant"); }
    @FXML public void selectionnerPsy()         { ouvrirFormulaire("Psychologue"); }
    @FXML public void selectionnerResponsable() { ouvrirFormulaire("Responsable Etudiant"); }

    private void ouvrirFormulaire(String role) {
        roleSelectionne = role;

        // Titre dynamique
        switch (role) {
            case "Etudiant"              -> labelTitreRole.setText("Inscription - Étudiant");
            case "Psychologue"           -> labelTitreRole.setText("Inscription - Psychologue");
            case "Responsable Etudiant"  -> labelTitreRole.setText("Inscription - Responsable Étudiant");
        }

        // Afficher/cacher les panneaux rôle
        afficherPanneauRole(role);

        // Basculer étape
        etapeRole.setVisible(false);      etapeRole.setManaged(false);
        etapeFormulaire.setVisible(true); etapeFormulaire.setManaged(true);
    }

    @FXML public void retourChoixRole() {
        etapeFormulaire.setVisible(false); etapeFormulaire.setManaged(false);
        etapeRole.setVisible(true);        etapeRole.setManaged(true);
        clearAllErrors();
        hideGlobal();
    }

    private void afficherPanneauRole(String role) {
        panneauEtudiant.setVisible(false);    panneauEtudiant.setManaged(false);
        panneauPsy.setVisible(false);          panneauPsy.setManaged(false);
        panneauResponsable.setVisible(false);  panneauResponsable.setManaged(false);
        switch (role) {
            case "Etudiant"             -> { panneauEtudiant.setVisible(true);   panneauEtudiant.setManaged(true); }
            case "Psychologue"          -> { panneauPsy.setVisible(true);         panneauPsy.setManaged(true); }
            case "Responsable Etudiant" -> { panneauResponsable.setVisible(true); panneauResponsable.setManaged(true); }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS — erreurs inline (null-safe)
    // ══════════════════════════════════════════════════════════════════

    private void showErr(Label l, String msg) {
        if (l == null) return;
        l.setText("⚠  " + msg);
        l.setVisible(true); l.setManaged(true);
    }

    private void hideErr(Label l) {
        if (l == null) return;
        l.setText(""); l.setVisible(false); l.setManaged(false);
    }

    private void markRed(TextField f) {
        if (f == null) return;
        f.setStyle(f.getStyle().replaceAll("-fx-border-color:[^;]+;", "")
                + " -fx-border-color: #DC2626; -fx-border-width: 1.5;");
    }

    private void markRedPwd(PasswordField f) {
        if (f == null) return;
        f.setStyle(f.getStyle().replaceAll("-fx-border-color:[^;]+;", "")
                + " -fx-border-color: #DC2626; -fx-border-width: 1.5;");
    }

    private void resetRed(TextField f) {
        if (f == null) return;
        f.setStyle(f.getStyle().replaceAll("-fx-border-color:[^;]+;", "")
                .replaceAll("-fx-border-width:[^;]+;", "")
                + " -fx-border-color: #DDD6FE; -fx-border-width: 1.5;");
    }

    private void resetRedPwd(PasswordField f) {
        if (f == null) return;
        f.setStyle(f.getStyle().replaceAll("-fx-border-color:[^;]+;", "")
                .replaceAll("-fx-border-width:[^;]+;", "")
                + " -fx-border-color: #DDD6FE; -fx-border-width: 1.5;");
    }

    private void clearAllErrors() {
        hideErr(errNom); hideErr(errPrenom); hideErr(errEmail); hideErr(errCin);
        hideErr(errPassword); hideErr(errConfirm); hideErr(errConditions);
        hideErr(errIdentifiant); hideErr(errEtablissement);
        hideErr(errSpecialite); hideErr(errAdresse); hideErr(errTelephone);
        hideErr(errPoste); hideErr(errEtablissementResp);
        resetRed(nomField); resetRed(prenomField);
        resetRed(emailField); resetRed(cinField);
        resetRedPwd(passwordField); resetRedPwd(confirmPasswordField);
        resetRed(identifiantField); resetRed(etablissementField);
        resetRed(specialiteField); resetRed(adresseField); resetRed(telephoneField);
        resetRed(posteField); resetRed(etablissementRespField);
    }

    private void showGlobalError(String msg) {
        if (messageGlobal == null) return;
        messageGlobal.setText("⚠  " + msg);
        messageGlobal.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;" +
                "-fx-font-family: 'Segoe UI'; -fx-background-color: #FEF2F2;" +
                "-fx-background-radius: 8; -fx-padding: 10 14;" +
                "-fx-border-color: #FECACA; -fx-border-radius: 8; -fx-border-width: 1;");
        messageGlobal.setVisible(true); messageGlobal.setManaged(true);
    }

    private void showGlobalSuccess(String msg) {
        if (messageGlobal == null) return;
        messageGlobal.setText("✓  " + msg);
        messageGlobal.setStyle("-fx-text-fill: #065F46; -fx-font-size: 12px;" +
                "-fx-font-family: 'Segoe UI'; -fx-background-color: #ECFDF5;" +
                "-fx-background-radius: 8; -fx-padding: 10 14;" +
                "-fx-border-color: #6EE7B7; -fx-border-radius: 8; -fx-border-width: 1;");
        messageGlobal.setVisible(true); messageGlobal.setManaged(true);
    }

    private void hideGlobal() {
        if (messageGlobal == null) return;
        messageGlobal.setText(""); messageGlobal.setVisible(false); messageGlobal.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════════
    //  S'INSCRIRE — validation complète avec unicité email & CIN
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void sInscrire() {
        clearAllErrors();
        hideGlobal();

        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim();
        String cin     = cinField.getText().trim();
        String pwd     = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        boolean conditions = conditionsCheck != null && conditionsCheck.isSelected();

        boolean valid = true;

        // ── Nom ───────────────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(nom)) {
            showErr(errNom, "Le nom est obligatoire."); markRed(nomField); valid = false;
        } else if (!ValidationUtils.isNomValide(nom)) {
            showErr(errNom, ValidationUtils.messageNom()); markRed(nomField); valid = false;
        }

        // ── Prénom ────────────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(prenom)) {
            showErr(errPrenom, "Le prénom est obligatoire."); markRed(prenomField); valid = false;
        } else if (!ValidationUtils.isNomValide(prenom)) {
            showErr(errPrenom, ValidationUtils.messageNom()); markRed(prenomField); valid = false;
        }

        // ── Email ─────────────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(email)) {
            showErr(errEmail, "L'email est obligatoire."); markRed(emailField); valid = false;
        } else if (!ValidationUtils.isEmailValide(email)) {
            showErr(errEmail, ValidationUtils.messageEmail()); markRed(emailField); valid = false;
        } else {
            // Vérification unicité email
            try {
                if (etudiantService.emailExiste(email)
                        || psychologueService.emailExiste(email)
                        || responsableService.emailExiste(email)) {
                    showErr(errEmail, "Cet email est déjà utilisé par un autre compte.");
                    markRed(emailField); valid = false;
                }
            } catch (Exception ignored) {}
        }

        // ── CIN ───────────────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(cin)) {
            showErr(errCin, "Le CIN est obligatoire."); markRed(cinField); valid = false;
        } else if (!ValidationUtils.isCinValide(cin)) {
            showErr(errCin, ValidationUtils.messageCin()); markRed(cinField); valid = false;
        } else {
            // Vérification unicité CIN
            try {
                if (etudiantService.cinExiste(cin)
                        || psychologueService.cinExiste(cin)
                        || responsableService.cinExiste(cin)) {
                    showErr(errCin, "Ce CIN est déjà associé à un compte existant.");
                    markRed(cinField); valid = false;
                }
            } catch (Exception ignored) {}
        }

        // ── Mot de passe ──────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(pwd)) {
            showErr(errPassword, "Le mot de passe est obligatoire."); markRedPwd(passwordField); valid = false;
        } else if (!ValidationUtils.isPasswordValide(pwd)) {
            showErr(errPassword, ValidationUtils.messagePassword()); markRedPwd(passwordField); valid = false;
        }

        // ── Confirmation ──────────────────────────────────────────────
        if (!ValidationUtils.isNonVide(confirm)) {
            showErr(errConfirm, "La confirmation est obligatoire."); markRedPwd(confirmPasswordField); valid = false;
        } else if (!ValidationUtils.isPasswordConfirme(pwd, confirm)) {
            showErr(errConfirm, ValidationUtils.messagePasswordConfirm()); markRedPwd(confirmPasswordField); valid = false;
        }

        // ── Conditions ────────────────────────────────────────────────
        if (!conditions) {
            showErr(errConditions, "Vous devez accepter les conditions d'utilisation."); valid = false;
        }

        // ── Champs spécifiques au rôle ────────────────────────────────
        switch (roleSelectionne) {
            case "Etudiant" -> {
                String identifiant  = identifiantField  != null ? identifiantField.getText().trim()  : "";
                String etablissement = etablissementField != null ? etablissementField.getText().trim() : "";
                if (!ValidationUtils.isNonVide(identifiant)) {
                    showErr(errIdentifiant, "L'identifiant étudiant est obligatoire.");
                    markRed(identifiantField); valid = false;
                }
                if (!ValidationUtils.isNonVide(etablissement)) {
                    showErr(errEtablissement, "L'établissement est obligatoire.");
                    markRed(etablissementField); valid = false;
                }
            }
            case "Psychologue" -> {
                String specialite = specialiteField != null ? specialiteField.getText().trim() : "";
                String adresse    = adresseField    != null ? adresseField.getText().trim()    : "";
                String telephone  = telephoneField  != null ? telephoneField.getText().trim()  : "";
                if (!ValidationUtils.isNonVide(specialite)) {
                    showErr(errSpecialite, "La spécialité est obligatoire.");
                    markRed(specialiteField); valid = false;
                }
                if (!ValidationUtils.isNonVide(adresse)) {
                    showErr(errAdresse, "L'adresse est obligatoire.");
                    markRed(adresseField); valid = false;
                }
                if (!ValidationUtils.isNonVide(telephone)) {
                    showErr(errTelephone, "Le téléphone est obligatoire."); markRed(telephoneField); valid = false;
                } else if (!ValidationUtils.isTelephoneValide(telephone)) {
                    showErr(errTelephone, ValidationUtils.messageTelephone()); markRed(telephoneField); valid = false;
                }
            }
            case "Responsable Etudiant" -> {
                String poste         = posteField             != null ? posteField.getText().trim()             : "";
                String etablissement = etablissementRespField != null ? etablissementRespField.getText().trim() : "";
                if (!ValidationUtils.isNonVide(poste)) {
                    showErr(errPoste, "Le poste est obligatoire."); markRed(posteField); valid = false;
                }
                if (!ValidationUtils.isNonVide(etablissement)) {
                    showErr(errEtablissementResp, "L'établissement est obligatoire.");
                    markRed(etablissementRespField); valid = false;
                }
            }
        }

        if (!valid) return;

        // ── Enregistrement ────────────────────────────────────────────
        try {
            switch (roleSelectionne) {
                case "Etudiant" -> {
                    Etudiant e = new Etudiant(
                            nom, prenom, email, pwd, cin,
                            identifiantField.getText().trim(),
                            etablissementField.getText().trim(),
                            "en_attente");
                    etudiantService.inscrire(e);
                }
                case "Psychologue" -> {
                    Psychologue p = new Psychologue(
                            nom, prenom, email, pwd, cin,
                            specialiteField.getText().trim(),
                            adresseField.getText().trim(),
                            telephoneField.getText().trim(),
                            "en_attente");
                    psychologueService.inscrire(p);
                }
                case "Responsable Etudiant" -> {
                    ResponsableEtudiant r = new ResponsableEtudiant(
                            nom, prenom, email, pwd, cin,
                            posteField.getText().trim(),
                            etablissementRespField.getText().trim(),
                            "en_attente");
                    responsableService.inscrire(r);
                }
            }
            showGlobalSuccess("Inscription soumise ! En attente de validation par l'administrateur.");
        } catch (SQLException e) {
            showGlobalError("Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void allerConnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage stage = new Stage();
            stage.setTitle("UniMind — Connexion");
            stage.setScene(new Scene(loader.load(), 900, 600));
            stage.show();
            ((Stage) nomField.getScene().getWindow()).close();
        } catch (Exception e) {
            showGlobalError("Erreur de navigation : " + e.getMessage());
        }
    }
}