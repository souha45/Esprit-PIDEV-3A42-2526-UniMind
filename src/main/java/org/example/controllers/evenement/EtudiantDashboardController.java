package org.example.controllers.evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;

public class EtudiantDashboardController {

    @FXML
    private Label lblUserName;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation
        NavigationContext.setContentScrollPane(contentScrollPane);
        
        // Afficher le nom de l'utilisateur connecté
        SessionManager.getInstance().getCurrentUserFullName().ifPresent(lblUserName::setText);

        // Charger par défaut les événements
        try {
            voirEvenements(new ActionEvent());
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement initial: " + e.getMessage());
        }
    }

    @FXML
    private void vueDensemble(ActionEvent event) {
        // Rediriger vers les événements
        try {
            voirEvenements(new ActionEvent());
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void voirEvenements(ActionEvent event) throws IOException {
        // Charger les événements dans le ScrollPane avec la nouvelle interface étudiante
        chargerContenuDansCentre("/evenement/EvenementsEtudiant.fxml");
    }

    @FXML
    private void voirParticipations(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/participation/GestionParticipation.fxml");
    }

    @FXML
    private void voirFavoris(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/favori/FavorisEtudiant.fxml");
    }

    @FXML
    private void logout(ActionEvent event) throws IOException {
        SessionManager.getInstance().logout();
        naviguerVersEcran(event, "/auth/Login.fxml", "Connexion");
    }

    private void chargerContenuDansCentre(String fxmlPath) throws IOException {
        var resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }
        Parent root = FXMLLoader.load(resource);
        contentScrollPane.setContent(root);
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
