package org.example.controllers;

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

public class DashboardResponsableController extends BaseDashboardController {

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private SidebarResponsableController sidebarResponsableController;

    @FXML
    private NavbarController navbarController;

    @FXML
    private Label lblTotalEvenements;

    @FXML
    private Label lblTotalParticipations;

    @FXML
    private Label lblTotalSponsors;

    @FXML
    private Label lblTotalFeedbacks;

    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation
        NavigationContext.setContentScrollPane(contentScrollPane);

        // Initialiser le navbar controller
        if (navbarController != null) {
            navbarController.setParentController(this);
        }

        // Initialiser le sidebar controller
        if (sidebarResponsableController != null) {
            sidebarResponsableController.setParentController(this);
        }

        // Charger les statistiques
        chargerStatistiques();
    }

    @Override
    protected void afficherInfosNavbar() {
        if (navNomLabel != null && utilisateurConnecte != null) {
            navNomLabel.setText("Bonjour, " + utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom());
        }
        chargerPhotoNavbar();
    }

    @FXML
    private void vueDensemble(ActionEvent event) throws IOException {
        // Cette méthode n'est plus utilisée mais gardée pour compatibilité
        try {
            gestionEvenements(new ActionEvent());
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void dashboard(ActionEvent event) {
        // Recharger le dashboard par défaut (les statistiques sont déjà dans le FXML)
        // On recharge juste les données
        chargerStatistiques();
    }

    @FXML
    public void gestionEvenements(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/evenement/GestionEvenement.fxml");
    }

    @FXML
    public void gestionParticipations(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/participation/GestionParticipation.fxml");
    }

    @FXML
    public void attributionSponsors(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/sponsor/GestionAttributionSponsor.fxml");
    }

    @FXML
    public void gestionFeedbacks(ActionEvent event) throws IOException {
        chargerContenuDansCentre("/feedback/FeedbacksAdmin.fxml");
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

    private void chargerStatistiques() {
        try {
            java.sql.Connection conn = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();

            // Total événements
            int totalEvenements = queryInt(conn, "SELECT COUNT(*) FROM evenement");
            lblTotalEvenements.setText(String.valueOf(totalEvenements));

            // Total participations
            int totalParticipations = queryInt(conn, "SELECT COUNT(*) FROM participation");
            lblTotalParticipations.setText(String.valueOf(totalParticipations));

            // Total sponsors
            int totalSponsors = queryInt(conn, "SELECT COUNT(*) FROM sponsor");
            lblTotalSponsors.setText(String.valueOf(totalSponsors));

            // Total feedbacks
            int totalFeedbacks = queryInt(conn, "SELECT COUNT(*) FROM feedback");
            lblTotalFeedbacks.setText(String.valueOf(totalFeedbacks));

        } catch (Exception e) {
            e.printStackTrace();
            lblTotalEvenements.setText("—");
            lblTotalParticipations.setText("—");
            lblTotalSponsors.setText("—");
            lblTotalFeedbacks.setText("—");
        }
    }

    private int queryInt(java.sql.Connection conn, String sql) {
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
