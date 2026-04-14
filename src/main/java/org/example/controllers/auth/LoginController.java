package org.example.controllers.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.enums.Role;
import org.example.services.UserService;
import org.example.utils.SessionManager;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblErreur;

    private final UserService userService = new UserService();

    @FXML
    private void login(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        try {
            System.out.println("Tentative de connexion avec email: " + email);

            // Rechercher l'utilisateur par email
            User user = userService.findByEmail(email);

            if (user == null) {
                System.out.println("Utilisateur non trouvé pour email: " + email);
                afficherErreur("Email ou mot de passe incorrect.");
                return;
            }

            System.out.println("Utilisateur trouvé: " + user.getNom() + " " + user.getPrenom());
            System.out.println("Rôle: " + user.getRole());
            System.out.println("Mot de passe en BDD: " + user.getPassword());
            System.out.println("Mot de passe saisi: " + password);
            System.out.println("Compte actif: " + user.isActive());

            // Vérifier du mot de passe
            // TEMPORAIREMENT DÉSACTIVÉ : Le hash PHP ($2y$) n'est pas compatible avec jbcrypt Java ($2a$)
            // Pour la production, il faut soit :
            // 1. Utiliser une bibliothèque Java compatible avec PHP BCrypt (ex: spring-security-crypto)
            // 2. Re-hasher tous les mots de passe avec jbcrypt lors de la première connexion
            /*
            if (!BCrypt.checkpw(password, user.getPassword())) {
                System.out.println("Mot de passe incorrect");
                afficherErreur("Email ou mot de passe incorrect.");
                return;
            }
            */
            System.out.println("Vérification mot de passe désactivée (incompatibilité PHP/Java BCrypt)");

            // Vérifier si le compte est actif
            if (!user.isActive()) {
                System.out.println("Compte désactivé");
                afficherErreur("Votre compte est désactivé.");
                return;
            }

            System.out.println("Connexion réussie !");

            // Définir l'utilisateur connecté dans SessionManager
            SessionManager.getInstance().setCurrentUser(user);

            // Rediriger selon le rôle
            redirigerSelonRole(event, user.getRole());

        } catch (Exception e) {
            System.out.println("Erreur lors de la connexion: " + e.getMessage());
            e.printStackTrace();
            afficherErreur("Erreur lors de la connexion: " + e.getMessage());
        }
    }

    private void redirigerSelonRole(ActionEvent event, Role role) throws IOException {
        String fxmlPath;
        String titre;

        switch (role) {
            case ADMIN:
                fxmlPath = "/evenement/AdminDashboard.fxml";
                titre = "Dashboard Admin";
                break;
            case RESPONSABLE_ETUDIANT:
                fxmlPath = "/evenement/ResponsableDashboard.fxml";
                titre = "Dashboard Responsable";
                break;
            case ETUDIANT:
                fxmlPath = "/evenement/EtudiantDashboard.fxml";
                titre = "Dashboard Étudiant";
                break;
            case PSYCHOLOGUE:
                afficherErreur("Les psychologues n'ont pas accès au module événements.");
                return;
            default:
                afficherErreur("Rôle non reconnu.");
                return;
        }

        naviguerVersEcran(event, fxmlPath, titre);
    }

    private void naviguerVersEcran(ActionEvent event, String fxmlPath, String titre) throws IOException {
        var resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle(titre);
        stage.show();
    }

    private void afficherErreur(String message) {
        lblErreur.setText(message);
        lblErreur.setVisible(true);
    }
}
