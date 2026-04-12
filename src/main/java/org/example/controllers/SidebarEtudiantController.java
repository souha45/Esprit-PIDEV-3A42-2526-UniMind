package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.models.User;

import java.io.IOException;

/**
 * Contrôleur pour la sidebar de l'étudiant
 * Navigation centralisée - toutes les pages passent par ici
 */
public class SidebarEtudiantController {

    // ========== COMPOSANTS FXML ==========
    @FXML private Label lblInitiales;
    @FXML private Label lblNomComplet;
    @FXML private Label lblRole;
    @FXML private Button btnDashboard;
    @FXML private Button btnMesRendezVous;
    @FXML private Button btnConsultations;
    @FXML private Button btnTraitements;
    @FXML private Button btnSuiviTraitements;
    @FXML private Button btnQuestionnaires;
    @FXML private Button btnMesReponses;
    @FXML private Button btnDeconnexion;

    // ========== DONNÉES ==========
    private User utilisateur;
    private Button activeButton;

    @FXML
    public void initialize() {
        // Configuration des actions de navigation
        btnDashboard.setOnAction(e -> naviguer("/DashboardEtudiant.fxml", "Dashboard", btnDashboard));
        btnMesRendezVous.setOnAction(e -> naviguer("/RendezVousEtudiant.fxml", "Mes Rendez-vous", btnMesRendezVous));
        btnConsultations.setOnAction(e -> naviguer("/ConsultationsEtudiant.fxml", "Consultations", btnConsultations));
        btnTraitements.setOnAction(e -> naviguer("/TraitementsEtudiant.fxml", "Traitements", btnTraitements));
        btnSuiviTraitements.setOnAction(e -> naviguer("/SuiviTraitementsEtudiant.fxml", "Suivi Traitements", btnSuiviTraitements));
        btnQuestionnaires.setOnAction(e -> naviguer("/QuestionnairesEtudiant.fxml", "Questionnaires", btnQuestionnaires));
        btnMesReponses.setOnAction(e -> naviguer("/MesReponsesEtudiant.fxml", "Mes Réponses", btnMesReponses));
        btnDeconnexion.setOnAction(e -> deconnecter());

        styliserBoutons();
        setActiveButton(btnDashboard);
    }

    /**
     * Navigation centralisée — tout passe par ici.
     * La sidebar charge la scène et passe l'utilisateur au nouveau contrôleur.
     *
     * @param fxmlPath   Chemin du fichier FXML
     * @param titre      Titre de la fenêtre
     * @param boutonActif Bouton qui a été cliqué (pour le style)
     */
    private void naviguer(String fxmlPath, String titre, Button boutonActif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1200, 700);

            // Récupérer le contrôleur de la nouvelle page et lui passer l'utilisateur
            Object controller = loader.getController();
            if (controller instanceof EtudiantPageController) {
                ((EtudiantPageController) controller).setUtilisateur(utilisateur);
            }

            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Unimind - " + titre);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Interface que tous les contrôleurs de page doivent implémenter.
     * Permet à la sidebar de passer l'utilisateur sans connaître le type exact.
     */
    public interface EtudiantPageController {
        void setUtilisateur(User user);
    }

    /**
     * Définit l'utilisateur connecté et met à jour l'affichage
     */
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user == null) return;

        // Mettre à jour l'affichage
        String prenom = user.getPrenom();
        String nom = user.getNom();
        lblNomComplet.setText(prenom + " " + nom);

        // Générer les initiales (première lettre du prénom + première lettre du nom)
        String initiales = String.valueOf(prenom.charAt(0)).toUpperCase() +
                String.valueOf(nom.charAt(0)).toUpperCase();
        lblInitiales.setText(initiales);
        lblRole.setText("Étudiant");
    }

    /**
     * Définit le bouton actif selon le fichier FXML chargé
     */
    public void setActiveButtonByFxml(String fxmlPath) {
        switch (fxmlPath) {
            case "/DashboardEtudiant.fxml" -> setActiveButton(btnDashboard);
            case "/RendezVousEtudiant.fxml" -> setActiveButton(btnMesRendezVous);
            case "/ConsultationsEtudiant.fxml" -> setActiveButton(btnConsultations);
            case "/TraitementsEtudiant.fxml" -> setActiveButton(btnTraitements);
            case "/SuiviTraitementsEtudiant.fxml" -> setActiveButton(btnSuiviTraitements);
            case "/QuestionnairesEtudiant.fxml" -> setActiveButton(btnQuestionnaires);
            case "/MesReponsesEtudiant.fxml" -> setActiveButton(btnMesReponses);
        }
    }

    /**
     * Met en surbrillance le bouton actif
     */
    private void setActiveButton(Button button) {
        Button[] boutons = {btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnSuiviTraitements, btnQuestionnaires, btnMesReponses};
        for (Button btn : boutons) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15;");
        }
        button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15; -fx-cursor: hand;");
        activeButton = button;
    }

    /**
     * Stylise les boutons au survol
     */
    private void styliserBoutons() {
        Button[] boutons = {btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnSuiviTraitements, btnQuestionnaires, btnMesReponses};

        for (Button btn : boutons) {
            btn.setOnMouseEntered(e -> {
                if (btn != activeButton) {
                    btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15; -fx-cursor: hand;");
                }
            });
            btn.setOnMouseExited(e -> {
                if (btn != activeButton) {
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15;");
                }
            });
        }
    }

    /**
     * Déconnexion : retour à l'écran de login
     */
    private void deconnecter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load(), 500, 400);

            Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Unimind - Connexion");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}