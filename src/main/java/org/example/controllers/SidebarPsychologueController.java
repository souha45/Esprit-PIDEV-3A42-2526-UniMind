package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.User;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SidebarPsychologueController {

    // ============================================================
    // Composants FXML (fusion des deux versions)
    // ============================================================
    @FXML private ImageView sidebarPhoto;
    @FXML private Label sidebarNomLabel;
    @FXML private Label sidebarPrenomLabel;
    @FXML private Label sidebarRoleLabel;

    @FXML private Label lblInitiales;
    @FXML private Label lblNomComplet;
    @FXML private Label lblRole;

    @FXML private Button btnProfil;
    @FXML private Button btnDashboard;
    @FXML private Button btnDisponibilites;
    @FXML private Button btnRendezVous;
    @FXML private Button btnConsultations;
    @FXML private Button btnPatients;
    @FXML private Button btnDeconnexion;

    // ============================================================
    // Styles (version moderne)
    // ============================================================
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

    // ============================================================
    // Données internes
    // ============================================================
    private BaseDashboardController parentController;
    private Connection connection;
    private User utilisateur;          // peut être setté directement
    private Button activeButton;

    // ============================================================
    // Initialisation
    // ============================================================
    @FXML
    public void initialize() {
        // Navigation
        btnDashboard.setOnAction(e -> naviguer("/DashboardPsy.fxml", "Dashboard", btnDashboard));
        btnDisponibilites.setOnAction(e -> naviguer("/AfficheDisponibilitesPsy.fxml", "Disponibilités", btnDisponibilites));
        btnRendezVous.setOnAction(e -> naviguer("/RendezVousPsy.fxml", "Rendez-vous", btnRendezVous));
        btnConsultations.setOnAction(e -> naviguer("/ConsultationsPsy.fxml", "Consultations", btnConsultations));
        btnPatients.setOnAction(e -> naviguer("/Patients.fxml", "Patients", btnPatients));
        btnProfil.setOnAction(e -> ouvrirProfil());
        btnDeconnexion.setOnAction(e -> seDeconnecter());

        styliserBoutons();
        setActiveButton(btnDashboard);
    }

    // ============================================================
    // Liaison avec le dashboard parent (première version)
    // ============================================================
    public void setParentController(BaseDashboardController controller) {
        this.parentController = controller;
        if (parentController != null) {
            this.connection = parentController.getConnection();
            // Récupérer l'utilisateur depuis le parent
            this.utilisateur = parentController.getUtilisateurConnecte();
            afficherInfosSidebar();
            chargerPhotoSidebar();
        }
    }

    // ============================================================
    // Setter direct de l'utilisateur (deuxième version)
    // ============================================================
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user == null) return;

        // Mettre à jour les labels modernes (initiaux)
        if (lblNomComplet != null) {
            lblNomComplet.setText("Dr. " + user.getPrenom() + " " + user.getNom());
        }
        if (lblInitiales != null) {
            lblInitiales.setText(
                    String.valueOf(user.getPrenom().charAt(0)).toUpperCase() +
                            String.valueOf(user.getNom().charAt(0)).toUpperCase()
            );
        }
        if (lblRole != null) lblRole.setText("PSYCHOLOGUE");

        // Mettre à jour la zone photo/nom (première version)
        afficherInfosSidebar();
        chargerPhotoSidebar();
    }

    // ============================================================
    // Affichage des infos (nom, prénom, rôle)
    // ============================================================
    private void afficherInfosSidebar() {
        User user = (utilisateur != null) ? utilisateur : (parentController != null ? parentController.getUtilisateurConnecte() : null);
        if (sidebarNomLabel != null && user != null) {
            sidebarNomLabel.setText(user.getNom());
            sidebarPrenomLabel.setText(user.getPrenom());
            sidebarRoleLabel.setText("Psychologue");
        }
    }

    // ============================================================
    // Chargement de la photo
    // ============================================================
    private void chargerPhotoSidebar() {
        if (sidebarPhoto == null) return;
        User user = (utilisateur != null) ? utilisateur : (parentController != null ? parentController.getUtilisateurConnecte() : null);
        if (user == null) return;

        String photoPath = getPhotoChemin(user);
        if (photoPath != null && !photoPath.isEmpty()) {
            File file = new File(photoPath);
            if (file.exists()) {
                sidebarPhoto.setImage(new Image(file.toURI().toString()));
                return;
            }
        }
        try {
            sidebarPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png")));
        } catch (Exception e) {}
    }

    private String getPhotoChemin(User user) {
        if (connection == null || user == null) return null;
        String query = "SELECT photo FROM profil WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, user.getUserId());
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("photo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ============================================================
    // Navigation avec passage de l'utilisateur
    // ============================================================
    private void naviguer(String fxmlPath, String titre, Button boutonActif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1200, 700);

            Object controller = loader.getController();
            if (controller instanceof PsyPageController) {
                ((PsyPageController) controller).setUtilisateur(utilisateur);
            }

            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Unimind - " + titre);
            stage.show();

            setActiveButton(boutonActif);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface PsyPageController {
        void setUtilisateur(User user);
    }

    // ============================================================
    // Gestion du bouton actif (styles)
    // ============================================================
    public void setActiveButtonByFxml(String fxmlPath) {
        switch (fxmlPath) {
            case "/DashboardPsy.fxml"              -> setActiveButton(btnDashboard);
            case "/AfficheDisponibilitesPsy.fxml"  -> setActiveButton(btnDisponibilites);
            case "/RendezVousPsy.fxml"             -> setActiveButton(btnRendezVous);
            case "/ConsultationsPsy.fxml"          -> setActiveButton(btnConsultations);
            case "/Patients.fxml"                  -> setActiveButton(btnPatients);
        }
    }

    private void setActiveButton(Button button) {
        Button[] boutons = {btnDashboard, btnDisponibilites, btnRendezVous, btnConsultations, btnPatients};
        for (Button btn : boutons) {
            btn.setStyle(STYLE_IDLE);
        }
        button.setStyle(STYLE_ACTIVE);
        activeButton = button;
    }

    private void styliserBoutons() {
        Button[] boutons = {btnDashboard, btnDisponibilites, btnRendezVous, btnConsultations, btnPatients};
        for (Button btn : boutons) {
            btn.setOnMouseEntered(e -> {
                if (btn != activeButton) btn.setStyle(STYLE_HOVER);
            });
            btn.setOnMouseExited(e -> {
                if (btn != activeButton) btn.setStyle(STYLE_IDLE);
            });
        }
        // Logout
        btnDeconnexion.setOnMouseEntered(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_HOVER));
        btnDeconnexion.setOnMouseExited(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_IDLE));
    }

    // ============================================================
    // Profil
    // ============================================================
    @FXML
    public void ouvrirProfil() {
        User user = (utilisateur != null) ? utilisateur : (parentController != null ? parentController.getUtilisateurConnecte() : null);
        if (user == null) {
            System.err.println("❌ Erreur: impossible de récupérer l'utilisateur");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(loader.load()));
            ProfilController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            chargerPhotoSidebar();
            if (parentController != null) {
                parentController.rafraichirPhotoNavbar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // Déconnexion
    // ============================================================
    @FXML
    public void seDeconnecter() {
        if (parentController != null) {
            parentController.seDeconnecter();
        } else {
            // Déconnexion autonome (si pas de parent)
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
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
}