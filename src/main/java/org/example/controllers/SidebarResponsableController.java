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
    @FXML private Button btnDashboard;
    @FXML private Button btnEvenements;
    @FXML private Button btnParticipations;
    @FXML private Button btnAttributionSponsors;
    @FXML private Button btnFeedbacks;

    private Object parentController;
    private Button activeButton;

    // Design tokens pour les styles de boutons
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

    public void setParentController(Object controller) {
        this.parentController = controller;
    }

    @FXML
    public void initialize() {
        afficherInfosSidebar();
        chargerPhotoSidebar();
        styliserBoutons();
        setActiveButton(btnDashboard);
    }

    private void setActiveButton(Button button) {
        Button[] boutons = {
                btnDashboard, btnEvenements, btnParticipations,
                btnAttributionSponsors, btnFeedbacks, btnProfil
        };
        for (Button btn : boutons) btn.setStyle(STYLE_IDLE);
        button.setStyle(STYLE_ACTIVE);
        activeButton = button;
    }

    private void styliserBoutons() {
        Button[] boutons = {
                btnDashboard, btnEvenements, btnParticipations,
                btnAttributionSponsors, btnFeedbacks, btnProfil
        };
        for (Button btn : boutons) {
            btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(STYLE_HOVER); });
            btn.setOnMouseExited(e -> { if (btn != activeButton) btn.setStyle(STYLE_IDLE); });
        }
        btnDeconnexion.setOnMouseEntered(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_HOVER));
        btnDeconnexion.setOnMouseExited(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_IDLE));
    }

    @FXML
    public void dashboard() {
        setActiveButton(btnDashboard);
        if (parentController instanceof DashboardResponsableController) {
            try {
                ((DashboardResponsableController) parentController).dashboard(new javafx.event.ActionEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private User getUtilisateur() {
        return SessionManager.getInstance().getCurrentUser();
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
        setActiveButton(btnEvenements);
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
        setActiveButton(btnParticipations);
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
        setActiveButton(btnAttributionSponsors);
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
        setActiveButton(btnFeedbacks);
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
        setActiveButton(btnProfil);
        User user = getUtilisateur();
        if (user == null) {
            System.err.println("❌ Erreur: impossible de récupérer l'utilisateur");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            javafx.scene.Parent content = loader.load();
            ProfilController ctrl = loader.getController();
            ctrl.setUser(user);
            org.example.utils.NavigationContext.loadContentInCenter(content);

            // Rafraîchir la photo après chargement
            chargerPhotoSidebar();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors de l'ouverture du profil: " + e.getMessage());
        }
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
