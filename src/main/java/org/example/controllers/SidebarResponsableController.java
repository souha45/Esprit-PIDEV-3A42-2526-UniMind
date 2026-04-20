package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.utils.SessionManager;

public class SidebarResponsableController {

    @FXML private ImageView sidebarPhoto;
    @FXML private Label sidebarNomLabel;
    @FXML private Label sidebarPrenomLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Button btnProfil;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnEvenements;
    @FXML private Button btnParticipations;
    @FXML private Button btnAttributionSponsors;
    @FXML private Button btnFeedbacks;

    private Object parentController;

    public void setParentController(Object controller) {
        this.parentController = controller;
    }

    @FXML
    public void initialize() {
        afficherInfosSidebar();
        chargerPhotoSidebar();
    }

    private User getUtilisateur() {
        return SessionManager.getInstance().getCurrentUser().orElse(null);
    }

    private void afficherInfosSidebar() {
        User user = getUtilisateur();
        if (sidebarNomLabel != null && user != null) {
            sidebarNomLabel.setText(user.getNom());
            sidebarPrenomLabel.setText(user.getPrenom());
            sidebarRoleLabel.setText("Responsable");
        }
    }

    private void chargerPhotoSidebar() {
        if (sidebarPhoto == null) return;
        User user = getUtilisateur();
        if (user == null) return;

        try {
            sidebarPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png")));
        } catch (Exception e) {}
    }

    @FXML
    public void gestionEvenements() {
        if (parentController instanceof DashboardResponsableController) {
            try {
                ((DashboardResponsableController) parentController).gestionEvenements(new javafx.event.ActionEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void gestionParticipations() {
        if (parentController instanceof DashboardResponsableController) {
            try {
                ((DashboardResponsableController) parentController).gestionParticipations(new javafx.event.ActionEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void attributionSponsors() {
        if (parentController instanceof DashboardResponsableController) {
            try {
                ((DashboardResponsableController) parentController).attributionSponsors(new javafx.event.ActionEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void gestionFeedbacks() {
        if (parentController instanceof DashboardResponsableController) {
            try {
                ((DashboardResponsableController) parentController).gestionFeedbacks(new javafx.event.ActionEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void ouvrirProfil() {
        User user = getUtilisateur();
        if (user == null) {
            System.err.println("❌ Erreur: impossible de récupérer l'utilisateur");
            return;
        }

        // Profil.fxml sera ajouté après le merge avec le projet de l'ami
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Mon Profil");
        alert.setHeaderText("Profil Responsable");
        alert.setContentText("Le formulaire de profil sera ajouté après le merge.");
        alert.showAndWait();
    }

    @FXML
    public void seDeconnecter() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Connexion - Unimind");
            stage.setScene(new Scene(loader.load(), 1200, 800));
            stage.show();

            // Fermer la fenêtre actuelle
            Stage currentStage = (Stage) btnDeconnexion.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
