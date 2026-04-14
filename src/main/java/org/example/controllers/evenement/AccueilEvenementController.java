package org.example.controllers.evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AccueilEvenementController {

    @FXML
    private void gererEvenements(ActionEvent event) throws IOException {
        naviguerVersEcran(event, "/evenement/GestionEvenement.fxml", "Gestion des Événements");
    }

    @FXML
    private void gererParticipations(ActionEvent event) throws IOException {
        naviguerVersEcran(event, "/participation/GestionParticipation.fxml", "Gestion des Participations");
    }

    @FXML
    private void gererSponsors(ActionEvent event) throws IOException {
        naviguerVersEcran(event, "/sponsor/GestionSponsor.fxml", "Gestion des Sponsors");
    }

    @FXML
    private void gererFavoris(ActionEvent event) throws IOException {
        naviguerVersEcran(event, "/favori/GestionFavori.fxml", "Gestion des Favoris");
    }

    @FXML
    private void quitter(ActionEvent event) {
        // Fermer l'application
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    /**
     * Méthode utilitaire pour naviguer entre les écrans
     */
    private void naviguerVersEcran(ActionEvent event, String fxmlPath, String titre) throws IOException {
        // Charger le nouvel écran
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        
        // Obtenir la scène actuelle et la stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
        // Changer la scène
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle(titre);
        stage.show();
    }
}
