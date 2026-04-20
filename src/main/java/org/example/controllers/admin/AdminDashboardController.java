package org.example.controllers.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private ScrollPane contentScrollPane;

    // Champs de la sidebar
    @FXML
    private ImageView sidebarPhoto;
    @FXML
    private Label sidebarNomLabel;
    @FXML
    private Label sidebarPrenomLabel;
    @FXML
    private Label sidebarRoleLabel;
    @FXML
    private Button btnProfil;
    @FXML
    private Button btnDeconnexion;
    @FXML
    private Button btnEvenements;
    @FXML
    private Button btnSponsors;
    @FXML
    private Button btnParticipations;
    @FXML
    private Button btnFeedbacks;

    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation
        NavigationContext.setContentScrollPane(contentScrollPane);

        // Initialiser la sidebar
        afficherInfosSidebar();
        chargerPhotoSidebar();

        // Charger par défaut la gestion des événements
        try {
            gestionEvenements(new ActionEvent());
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement initial: " + e.getMessage());
        }
    }

    // Méthodes de la sidebar
    private User getUtilisateur() {
        return SessionManager.getInstance().getCurrentUser().orElse(null);
    }

    private void afficherInfosSidebar() {
        User user = getUtilisateur();
        if (sidebarNomLabel != null && user != null) {
            sidebarNomLabel.setText(user.getNom());
            sidebarPrenomLabel.setText(user.getPrenom());
            sidebarRoleLabel.setText("Administrateur");
        }
    }

    private void chargerPhotoSidebar() {
        if (sidebarPhoto == null) return;
        try {
            sidebarPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png")));
        } catch (Exception e) {}
    }

    @FXML
    public void ouvrirProfil() {
        // Profil.fxml sera ajouté après le merge avec le projet de l'ami
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Mon Profil");
        alert.setHeaderText("Profil Administrateur");
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


    @FXML
    public void gestionEvenements(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/evenement/GestionEvenement.fxml");
    }

    @FXML
    public void gestionSponsors(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/sponsor/GestionSponsor.fxml");
    }

    @FXML
    public void gestionParticipations(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/participation/GestionParticipation.fxml"));
        Parent root = loader.load();
        contentScrollPane.setContent(root);
    }

    @FXML
    public void gestionFeedbacks(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/feedback/FeedbacksAdmin.fxml"));
        Parent root = loader.load();
        contentScrollPane.setContent(root);
    }

    @FXML
    private void logout(ActionEvent event) throws IOException {
        SessionManager.getInstance().logout();
        naviguerVersEcran(event, "/login.fxml", "Connexion");
    }

    private void chargerContenuDansCentre(String fxmlPath) throws IOException {
        var resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        Parent content = loader.load();
        contentScrollPane.setContent(content);
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
}
