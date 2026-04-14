package org.example.controllers.evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.services.EvenementService;
import org.example.services.ParticipationService;
import org.example.services.SponsorService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class AdminDashboardController {

    @FXML
    private Label lblUserName;

    @FXML
    private Label lblTotalEvenements;

    @FXML
    private Label lblTotalSponsors;

    @FXML
    private Label lblTotalParticipations;

    @FXML
    private Label lblEvenementsActifs;

    @FXML
    private Label lblEvenementsPasses;

    @FXML
    private ScrollPane contentScrollPane;

    private final EvenementService evenementService = new EvenementService();
    private final SponsorService sponsorService = new SponsorService();
    private final ParticipationService participationService = new ParticipationService();

    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation
        NavigationContext.setContentScrollPane(contentScrollPane);
        
        // Afficher le nom de l'utilisateur connecté
        SessionManager.getInstance().getCurrentUserFullName().ifPresent(lblUserName::setText);

        // Charger par défaut la gestion des événements
        try {
            gestionEvenements(new ActionEvent());
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement initial: " + e.getMessage());
        }
    }

    private void chargerStatistiques() {
        try {
            // Total événements
            int totalEvenements = evenementService.afficher().size();
            lblTotalEvenements.setText(String.valueOf(totalEvenements));

            // Total sponsors
            int totalSponsors = sponsorService.afficher().size();
            lblTotalSponsors.setText(String.valueOf(totalSponsors));

            // Total participations
            int totalParticipations = participationService.afficher().size();
            lblTotalParticipations.setText(String.valueOf(totalParticipations));

            // Événements actifs (statut en cours ou à venir)
            int evenementsActifs = (int) evenementService.afficher().stream()
                    .filter(e -> e.getStatut() == org.example.enums.StatutEvenement.EN_COURS || e.getStatut() == org.example.enums.StatutEvenement.A_VENIR)
                    .count();
            lblEvenementsActifs.setText(String.valueOf(evenementsActifs));

            // Événements passés (statut terminé)
            int evenementsPasses = (int) evenementService.afficher().stream()
                    .filter(e -> e.getStatut() == org.example.enums.StatutEvenement.TERMINE)
                    .count();
            lblEvenementsPasses.setText(String.valueOf(evenementsPasses));

        } catch (SQLException e) {
            System.err.println("Impossible de charger les statistiques: " + e.getMessage());
        }
    }

    @FXML
    private void vueDensemble(ActionEvent event) throws IOException {
        // Recharger les statistiques
        chargerStatistiques();
        // Recharger le contenu des statistiques dans le centre
        chargerContenuDansCentre("/evenement/AdminDashboardStats.fxml");
    }

    @FXML
    private void gestionEvenements(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/evenement/GestionEvenement.fxml");
    }

    @FXML
    private void gestionSponsors(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/sponsor/GestionSponsor.fxml");
    }

    @FXML
    private void gestionParticipations(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/participation/GestionParticipation.fxml");
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

        FXMLLoader loader = new FXMLLoader(resource);
        Parent content = loader.load();

        // Si c'est le fichier des statistiques, trouver les labels via lookup
        if (fxmlPath.equals("/evenement/AdminDashboardStats.fxml")) {
            lblTotalEvenements = (Label) content.lookup("#lblTotalEvenements");
            lblTotalSponsors = (Label) content.lookup("#lblTotalSponsors");
            lblTotalParticipations = (Label) content.lookup("#lblTotalParticipations");
            lblEvenementsActifs = (Label) content.lookup("#lblEvenementsActifs");
            lblEvenementsPasses = (Label) content.lookup("#lblEvenementsPasses");
            // Recharger les statistiques après l'injection
            chargerStatistiques();
        }

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
