package org.example.controllers;

import javafx.collections.FXCollections;
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

    @FXML private TextField nomField, prenomField, emailField, cinField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label messageGlobal;

    // Champs spécifiques Etudiant
    @FXML private VBox panneauEtudiant;
    @FXML private TextField identifiantField, etablissementField;

    // Champs spécifiques Psychologue
    @FXML private VBox panneauPsy;
    @FXML private TextField specialiteField, adresseField, telephoneField;

    // Champs spécifiques Responsable
    @FXML private VBox panneauResponsable;
    @FXML private TextField posteField, etablissementRespField;

    private EtudiantService etudiantService = new EtudiantService();
    private PsychologueService psychologueService = new PsychologueService();
    private ResponsableService responsableService = new ResponsableService();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(
                "Etudiant", "Psychologue", "Responsable Etudiant"
        ));
        roleCombo.setValue("Etudiant");
        afficherPanneauRole("Etudiant");

        roleCombo.setOnAction(e -> afficherPanneauRole(roleCombo.getValue()));
    }

    private void afficherPanneauRole(String role) {
        panneauEtudiant.setVisible(false);
        panneauEtudiant.setManaged(false);
        panneauPsy.setVisible(false);
        panneauPsy.setManaged(false);
        panneauResponsable.setVisible(false);
        panneauResponsable.setManaged(false);

        switch (role) {
            case "Etudiant" -> { panneauEtudiant.setVisible(true); panneauEtudiant.setManaged(true); }
            case "Psychologue" -> { panneauPsy.setVisible(true); panneauPsy.setManaged(true); }
            case "Responsable Etudiant" -> { panneauResponsable.setVisible(true); panneauResponsable.setManaged(true); }
        }
    }

    @FXML
    public void sInscrire() {
        messageGlobal.setText("");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String cin = cinField.getText().trim();
        String role = roleCombo.getValue();

        // ── VALIDATIONS COMMUNES ──────────────────────────────────────
        if (!ValidationUtils.isNonVide(nom) || !ValidationUtils.isNonVide(prenom)) {
            messageGlobal.setText("Nom et prénom obligatoires");
            return;
        }
        if (!ValidationUtils.isEmailValide(email)) {
            messageGlobal.setText(ValidationUtils.messageEmail());
            return;
        }
        if (!ValidationUtils.isPasswordValide(password)) {
            messageGlobal.setText(ValidationUtils.messagePassword());
            return;
        }
        if (!password.equals(confirm)) {
            messageGlobal.setText("Les mots de passe ne correspondent pas");
            return;
        }
        if (!ValidationUtils.isCinValide(cin)) {
            messageGlobal.setText(ValidationUtils.messageCin());
            return;
        }

        try {
            switch (role) {
                case "Etudiant" -> {
                    String identifiant = identifiantField.getText().trim();
                    String etablissement = etablissementField.getText().trim();
                    if (!ValidationUtils.isNonVide(identifiant) || !ValidationUtils.isNonVide(etablissement)) {
                        messageGlobal.setText("Identifiant et établissement obligatoires");
                        return;
                    }
                    Etudiant e = new Etudiant(nom, prenom, email, password, cin, identifiant, etablissement, "en_attente");
                    etudiantService.inscrire(e);
                }
                case "Psychologue" -> {
                    String specialite = specialiteField.getText().trim();
                    String adresse = adresseField.getText().trim();
                    String telephone = telephoneField.getText().trim();
                    if (!ValidationUtils.isTelephoneValide(telephone)) {
                        messageGlobal.setText(ValidationUtils.messageTelephone());
                        return;
                    }
                    Psychologue p = new Psychologue(nom, prenom, email, password, cin, specialite, adresse, telephone, "en_attente");
                    psychologueService.inscrire(p);
                }
                case "Responsable Etudiant" -> {
                    String poste = posteField.getText().trim();
                    String etablissement = etablissementRespField.getText().trim();
                    if (!ValidationUtils.isNonVide(poste) || !ValidationUtils.isNonVide(etablissement)) {
                        messageGlobal.setText("Poste et établissement obligatoires");
                        return;
                    }
                    ResponsableEtudiant r = new ResponsableEtudiant(nom, prenom, email, password, cin, poste, etablissement, "en_attente");
                    responsableService.inscrire(r);
                }
            }

            messageGlobal.setStyle("-fx-text-fill: green;");
            messageGlobal.setText("✓ Inscription soumise ! En attente de validation par l'admin.");

        } catch (SQLException e) {
            messageGlobal.setStyle("-fx-text-fill: red;");
            messageGlobal.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void allerConnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/login.fxml")
            );
            Stage stage = new Stage();
            stage.setTitle("UniMind - Connexion");
            stage.setScene(new Scene(loader.load(), 500, 400));
            stage.show();
            ((Stage) nomField.getScene().getWindow()).close();
        } catch (Exception e) {
            messageGlobal.setText("Erreur navigation");
        }
    }
}