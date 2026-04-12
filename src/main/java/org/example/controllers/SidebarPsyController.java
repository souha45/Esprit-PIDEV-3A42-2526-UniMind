package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.models.User;

import java.io.IOException;

public class SidebarPsyController {

    @FXML private Label lblInitiales;
    @FXML private Label lblNomComplet;
    @FXML private Label lblRole;
    @FXML private Button btnDashboard;
    @FXML private Button btnDisponibilites;
    @FXML private Button btnRendezVous;
    @FXML private Button btnConsultations;
    @FXML private Button btnPatients;
    @FXML private Button btnDeconnexion;

    private User utilisateur;
    private Button activeButton;

    @FXML
    public void initialize() {
        btnDashboard.setOnAction(e -> naviguer("/DashboardPsy.fxml", "Dashboard", btnDashboard));
        btnDisponibilites.setOnAction(e -> naviguer("/AfficheDisponibilitesPsy.fxml", "Disponibilités", btnDisponibilites));
        btnRendezVous.setOnAction(e -> naviguer("/RendezVousPsy.fxml", "Rendez-vous", btnRendezVous));
        btnConsultations.setOnAction(e -> naviguer("/ConsultationsPsy.fxml", "Consultations", btnConsultations));
        btnPatients.setOnAction(e -> naviguer("/Patients.fxml", "Patients", btnPatients));
        btnDeconnexion.setOnAction(e -> deconnecter());

        styliserBoutons();
        setActiveButton(btnDashboard);
    }

    /**
     * Navigation centralisée — tout passe par ici.
     * La sidebar charge la scène et passe l'utilisateur au nouveau contrôleur.
     */
    private void naviguer(String fxmlPath, String titre, Button boutonActif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1200, 700);

            // Récupérer le contrôleur de la nouvelle page et lui passer l'utilisateur
            Object controller = loader.getController();
            if (controller instanceof PsyPageController) {
                ((PsyPageController) controller).setUtilisateur(utilisateur);
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
     * Chaque contrôleur de page implémente cette interface.
     * Cela permet à la sidebar de passer l'utilisateur sans connaître
     * le type exact du contrôleur.
     */
    public interface PsyPageController {
        void setUtilisateur(User user);
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user == null) return;

        lblNomComplet.setText("Dr. " + user.getPrenom() + " " + user.getNom());
        lblInitiales.setText(
                String.valueOf(user.getPrenom().charAt(0)).toUpperCase() +
                        String.valueOf(user.getNom().charAt(0)).toUpperCase()
        );
        lblRole.setText("Psychologue");
    }

    public void setActiveButtonByFxml(String fxmlPath) {
        switch (fxmlPath) {
            case "/DashboardPsy.fxml"              -> setActiveButton(btnDashboard);
            case "/AfficheDisponibilitesPsy.fxml" -> setActiveButton(btnDisponibilites);
            case "/RendezVousPsy.fxml"                -> setActiveButton(btnRendezVous);
            case "/ConsultationsPsy.fxml"             -> setActiveButton(btnConsultations);
            case "/Patients.fxml"                  -> setActiveButton(btnPatients);
        }
    }

    private void setActiveButton(Button button) {
        Button[] boutons = {btnDashboard, btnDisponibilites, btnRendezVous, btnConsultations, btnPatients};
        for (Button btn : boutons) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15;");
        }
        button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15; -fx-cursor: hand;");
        activeButton = button;
    }

    private void styliserBoutons() {
        Button[] boutons = {btnDashboard, btnDisponibilites, btnRendezVous, btnConsultations, btnPatients};
        for (Button btn : boutons) {
            btn.setOnMouseEntered(e -> {
                if (btn != activeButton)
                    btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15; -fx-cursor: hand;");
            });
            btn.setOnMouseExited(e -> {
                if (btn != activeButton)
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 15;");
            });
        }
    }

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