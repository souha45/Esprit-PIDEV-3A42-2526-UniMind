package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.models.User;

import java.io.IOException;

public class SidebarEtudiantController {

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

    // ── Design tokens ──────────────────────────────────────────────
    private static final String STYLE_ACTIVE =
            "-fx-background-color: #ede9fe; " +
                    "-fx-text-fill: #6366f1; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-border-color: #c4b5fd; " +
                    "-fx-border-width: 0 0 0 3; " +
                    "-fx-border-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_IDLE =
            "-fx-background-color: transparent; " +
                    "-fx-text-fill: #6b7280; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_HOVER =
            "-fx-background-color: #ede9fe; " +
                    "-fx-text-fill: #6366f1; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_LOGOUT_IDLE =
            "-fx-background-color: transparent; " +
                    "-fx-text-fill: #ef4444; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_LOGOUT_HOVER =
            "-fx-background-color: #fee2e2; " +
                    "-fx-text-fill: #dc2626; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";
    // ───────────────────────────────────────────────────────────────

    private User utilisateur;
    private Button activeButton;

    @FXML
    public void initialize() {
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

    private void naviguer(String fxmlPath, String titre, Button boutonActif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1200, 700);

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

    public interface EtudiantPageController {
        void setUtilisateur(User user);
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user == null) return;

        String prenom = user.getPrenom();
        String nom = user.getNom();
        lblNomComplet.setText(prenom + " " + nom);
        lblInitiales.setText(
                String.valueOf(prenom.charAt(0)).toUpperCase() +
                        String.valueOf(nom.charAt(0)).toUpperCase()
        );
        lblRole.setText("ÉTUDIANT");
    }

    public void setActiveButtonByFxml(String fxmlPath) {
        switch (fxmlPath) {
            case "/DashboardEtudiant.fxml"       -> setActiveButton(btnDashboard);
            case "/RendezVousEtudiant.fxml"      -> setActiveButton(btnMesRendezVous);
            case "/ConsultationsEtudiant.fxml"   -> setActiveButton(btnConsultations);
            case "/TraitementsEtudiant.fxml"     -> setActiveButton(btnTraitements);
            case "/SuiviTraitementsEtudiant.fxml"-> setActiveButton(btnSuiviTraitements);
            case "/QuestionnairesEtudiant.fxml"  -> setActiveButton(btnQuestionnaires);
            case "/MesReponsesEtudiant.fxml"     -> setActiveButton(btnMesReponses);
        }
    }

    private void setActiveButton(Button button) {
        Button[] boutons = {btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnSuiviTraitements, btnQuestionnaires, btnMesReponses};
        for (Button btn : boutons) {
            btn.setStyle(STYLE_IDLE);
        }
        button.setStyle(STYLE_ACTIVE);
        activeButton = button;
    }

    private void styliserBoutons() {
        Button[] boutons = {btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnSuiviTraitements, btnQuestionnaires, btnMesReponses};
        for (Button btn : boutons) {
            btn.setOnMouseEntered(e -> {
                if (btn != activeButton) btn.setStyle(STYLE_HOVER);
            });
            btn.setOnMouseExited(e -> {
                if (btn != activeButton) btn.setStyle(STYLE_IDLE);
            });
        }
        // Logout hover
        btnDeconnexion.setOnMouseEntered(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_HOVER));
        btnDeconnexion.setOnMouseExited(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_IDLE));
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