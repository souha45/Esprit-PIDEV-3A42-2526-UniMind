package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.*;
import org.example.services.*;
import org.example.utils.ValidationUtils;

import java.sql.SQLException;

public class ProfilController {

    @FXML private Label nomPrenomLabel, roleLabel, emailLabel, statutLabel;
    @FXML private TextField bioField, telField;
    @FXML private PasswordField ancienMdpField, nouveauMdpField, confirmMdpField;
    @FXML private Label messageInfo;

    private User utilisateur;
    private EtudiantService etudiantService = new EtudiantService();
    private PsychologueService psychologueService = new PsychologueService();
    private ResponsableService responsableService = new ResponsableService();

    public void setUser(User user) {
        this.utilisateur = user;
        nomPrenomLabel.setText(user.getPrenom() + " " + user.getNom());
        roleLabel.setText(user.getRole().toString());
        emailLabel.setText(user.getEmail());
        statutLabel.setText(user.getStatut());
    }

    @FXML
    public void sauvegarderProfil() {
        String bio = bioField.getText().trim();
        String tel = telField.getText().trim();

        if (!ValidationUtils.isNonVide(tel) && tel.length() > 0) {
            if (!ValidationUtils.isTelephoneValide(tel)) {
                messageInfo.setStyle("-fx-text-fill: red;");
                messageInfo.setText(ValidationUtils.messageTelephone());
                return;
            }
        }

        try {
            Profil profil = new Profil();
            profil.setUserId(utilisateur.getUserId());
            profil.setBio(bio);
            profil.setTel(tel);

            if (utilisateur instanceof Etudiant) {
                etudiantService.modifierProfil(profil);
            } else if (utilisateur instanceof Psychologue) {
                psychologueService.modifierProfil(profil);
            } else if (utilisateur instanceof ResponsableEtudiant) {
                responsableService.modifierProfil(profil);
            }

            messageInfo.setStyle("-fx-text-fill: green;");
            messageInfo.setText("✓ Profil mis à jour");
        } catch (SQLException e) {
            messageInfo.setStyle("-fx-text-fill: red;");
            messageInfo.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void changerMotDePasse() {
        String ancien = ancienMdpField.getText();
        String nouveau = nouveauMdpField.getText();
        String confirm = confirmMdpField.getText();

        if (!ValidationUtils.isNonVide(ancien) || !ValidationUtils.isNonVide(nouveau)) {
            messageInfo.setStyle("-fx-text-fill: red;");
            messageInfo.setText("Tous les champs mot de passe sont obligatoires");
            return;
        }
        if (!ValidationUtils.isPasswordValide(nouveau)) {
            messageInfo.setStyle("-fx-text-fill: red;");
            messageInfo.setText(ValidationUtils.messagePassword());
            return;
        }
        if (!nouveau.equals(confirm)) {
            messageInfo.setStyle("-fx-text-fill: red;");
            messageInfo.setText("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            if (utilisateur instanceof Etudiant) {
                etudiantService.changerMotDePasse(utilisateur.getUserId(), ancien, nouveau);
            } else if (utilisateur instanceof Psychologue) {
                psychologueService.changerMotDePasse(utilisateur.getUserId(), ancien, nouveau);
            } else if (utilisateur instanceof ResponsableEtudiant) {
                responsableService.changerMotDePasse(utilisateur.getUserId(), ancien, nouveau);
            }
            messageInfo.setStyle("-fx-text-fill: green;");
            messageInfo.setText("✓ Mot de passe modifié");
            ancienMdpField.clear();
            nouveauMdpField.clear();
            confirmMdpField.clear();
        } catch (SQLException e) {
            messageInfo.setStyle("-fx-text-fill: red;");
            messageInfo.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void seDeconnecter() {
        try {
            Stage stage = (Stage) nomPrenomLabel.getScene().getWindow();
            stage.close();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/login.fxml")
            );
            Stage loginStage = new Stage();
            loginStage.setTitle("UniMind");
            loginStage.setScene(new Scene(loader.load(), 500, 400));
            loginStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}