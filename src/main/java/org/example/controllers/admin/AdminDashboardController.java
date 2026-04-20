package org.example.controllers.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private org.example.controllers.SidebarAdminController sidebarAdminController;

    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation
        NavigationContext.setContentScrollPane(contentScrollPane);

        // Initialiser le sidebar controller
        if (sidebarAdminController != null) {
            sidebarAdminController.setParentController(this);
        }

        // Charger par défaut la gestion des événements
        try {
            gestionEvenements(new ActionEvent());
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement initial: " + e.getMessage());
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
