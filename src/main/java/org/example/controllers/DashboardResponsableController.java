package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.chart.*;
import javafx.stage.Stage;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.services.EvenementService;
import org.example.services.ParticipationService;
import org.example.enums.StatutParticipation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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

    // ── Charts ──
    @FXML private BarChart<String, Number> barChartEvenements;
    @FXML private CategoryAxis xAxisEvenements;
    @FXML private PieChart pieChartParticipations;

    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

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

        // Charger les charts
        chargerCharts();
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
        chargerCharts();
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

    private void chargerCharts() {
        try {
            // Charger les événements et participations
            List<Evenement> evenements = evenementService.afficher();
            List<Participation> participations = participationService.afficher();

            // Charger le bar chart événements
            loadEventBarChart(evenements);

            // Charger le pie chart participations
            loadParticipationPieChart(participations);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadEventBarChart(List<Evenement> evenements) {
        // Count events by type
        Map<String, Long> countByType = evenements.stream()
                .filter(e -> e.getType() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getType().name(),
                        Collectors.counting()
                ));

        // Build series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Événements");

        for (Map.Entry<String, Long> entry : countByType.entrySet()) {
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(entry.getKey(), entry.getValue());
            series.getData().add(dataPoint);
        }

        xAxisEvenements.setCategories(FXCollections.observableArrayList(countByType.keySet()));
        barChartEvenements.getData().add(series);

        // Style the bars after rendering
        barChartEvenements.setAnimated(true);
        barChartEvenements.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                styleBarChart();
            }
        });

        javafx.application.Platform.runLater(this::styleBarChart);
    }

    private void styleBarChart() {
        // Style the bars
        barChartEvenements.lookupAll(".chart-bar").forEach(node ->
                node.setStyle("-fx-bar-fill: #6366f1;")
        );
    }

    private void loadParticipationPieChart(List<Participation> participations) {
        // Count participations by status
        Map<StatutParticipation, Long> countByStatut = participations.stream()
                .collect(Collectors.groupingBy(
                        Participation::getStatut,
                        Collectors.counting()
                ));

        // Build pie slices
        List<PieChart.Data> pieData = new ArrayList<>();
        String[] colors = {
                "#10b981", // CONFIRME - green
                "#f59e0b", // EN_ATTENTE - orange
                "#ef4444"  // ANNULE - red
        };

        int colorIdx = 0;
        for (Map.Entry<StatutParticipation, Long> entry : countByStatut.entrySet()) {
            String statutName = entry.getKey().getDbValue();
            PieChart.Data slice = new PieChart.Data(statutName + " (" + entry.getValue() + ")", entry.getValue());
            pieData.add(slice);

            // Apply color after rendering
            final String color = colors[Math.min(colorIdx, colors.length - 1)];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + color + ";");
                }
            });
            colorIdx++;
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("Aucune participation", 1));
        }

        pieChartParticipations.setData(FXCollections.observableArrayList(pieData));

        // Tooltips on hover
        pieChartParticipations.getData().forEach(data ->
                javafx.scene.control.Tooltip.install(data.getNode(),
                        new javafx.scene.control.Tooltip(
                                data.getName() + "\n" + (int) data.getPieValue() + " participation(s)"
                        ))
        );
    }
}
