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

    private Node originalDashboardContent;

    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation
        NavigationContext.setContentScrollPane(contentScrollPane);

        // Sauvegarder le contenu original du dashboard
        originalDashboardContent = contentScrollPane.getContent();

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
        // Restaurer le contenu original du dashboard
        if (originalDashboardContent != null) {
            contentScrollPane.setContent(originalDashboardContent);
            // Recharger les statistiques et charts
            chargerStatistiques();
            chargerCharts();
        }
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
            // Récupérer l'ID du responsable connecté
            int userId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (userId == -1) {
                lblTotalEvenements.setText("—");
                lblTotalParticipations.setText("—");
                lblTotalSponsors.setText("—");
                lblTotalFeedbacks.setText("—");
                return;
            }

            java.sql.Connection conn = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();

            // Total événements DU RESPONSABLE
            int totalEvenements = queryInt(conn, 
                "SELECT COUNT(*) FROM evenement WHERE organisateur_id = ?", userId);
            lblTotalEvenements.setText(String.valueOf(totalEvenements));

            // Total participations aux événements DU RESPONSABLE
            int totalParticipations = queryInt(conn, 
                "SELECT COUNT(*) FROM participation p " +
                "JOIN evenement e ON p.evenement_id = e.evenement_id " +
                "WHERE e.organisateur_id = ?", userId);
            lblTotalParticipations.setText(String.valueOf(totalParticipations));

            // Total sponsors attribués aux événements DU RESPONSABLE
            int totalSponsors = queryInt(conn, 
                "SELECT COUNT(DISTINCT s.sponsor_id) FROM sponsor s " +
                "JOIN evenement_sponsor es ON s.sponsor_id = es.sponsor_id " +
                "JOIN evenement e ON es.evenement_id = e.evenement_id " +
                "WHERE e.organisateur_id = ?", userId);
            lblTotalSponsors.setText(String.valueOf(totalSponsors));

            // Total feedbacks sur les événements DU RESPONSABLE
            int totalFeedbacks = queryInt(conn, 
                "SELECT COUNT(*) FROM participation p " +
                "JOIN evenement e ON p.evenement_id = e.evenement_id " +
                "WHERE e.organisateur_id = ? AND p.feedback_commentaire IS NOT NULL", userId);
            lblTotalFeedbacks.setText(String.valueOf(totalFeedbacks));

        } catch (Exception e) {
            e.printStackTrace();
            lblTotalEvenements.setText("—");
            lblTotalParticipations.setText("—");
            lblTotalSponsors.setText("—");
            lblTotalFeedbacks.setText("—");
        }
    }

    private int queryInt(java.sql.Connection conn, String sql, Integer userId) {
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void chargerCharts() {
        try {
            // Récupérer l'ID du responsable connecté
            int userId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (userId == -1) return;

            // Charger tous les événements puis filtrer par organisateur
            List<Evenement> allEvenements = evenementService.afficher();
            List<Evenement> evenements = allEvenements.stream()
                    .filter(e -> e.getOrganisateurId() == userId)
                    .collect(java.util.stream.Collectors.toList());
            
            // Récupérer les IDs des événements du responsable
            List<Integer> mesEvenementIds = evenements.stream()
                    .map(Evenement::getEvenementId)
                    .collect(java.util.stream.Collectors.toList());
            
            // Charger toutes les participations puis filtrer
            List<Participation> allParticipations = participationService.afficher();
            List<Participation> participations = allParticipations.stream()
                    .filter(p -> mesEvenementIds.contains(p.getEvenementId()))
                    .collect(java.util.stream.Collectors.toList());

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

        // Définir les couleurs pour chaque statut (ordre fixe)
        Map<StatutParticipation, String> statutColors = new HashMap<>();
        statutColors.put(StatutParticipation.CONFIRME, "#10b981");   // Vert
        statutColors.put(StatutParticipation.EN_ATTENTE, "#f59e0b"); // Orange
        statutColors.put(StatutParticipation.ANNULE, "#ef4444");     // Rouge

        // Ordre fixe des statuts pour le graphique
        List<StatutParticipation> statutOrder = Arrays.asList(
                StatutParticipation.CONFIRME,
                StatutParticipation.EN_ATTENTE,
                StatutParticipation.ANNULE
        );

        // Build pie slices dans l'ordre fixe
        List<PieChart.Data> pieData = new ArrayList<>();
        for (StatutParticipation statut : statutOrder) {
            Long count = countByStatut.getOrDefault(statut, 0L);
            if (count > 0) {
                String statutName = statut.getDbValue();
                PieChart.Data slice = new PieChart.Data(statutName + " (" + count + ")", count);
                pieData.add(slice);

                // Apply color spécifique au statut
                final String color = statutColors.get(statut);
                slice.nodeProperty().addListener((obs, old, node) -> {
                    if (node != null) {
                        node.setStyle("-fx-pie-color: " + color + ";");
                    }
                });
            }
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("Aucune participation", 1));
        }

        pieChartParticipations.setData(FXCollections.observableArrayList(pieData));

        // Corriger les couleurs de la légende
        corrigerCouleursLegende();

        // Tooltips on hover
        pieChartParticipations.getData().forEach(data ->
                javafx.scene.control.Tooltip.install(data.getNode(),
                        new javafx.scene.control.Tooltip(
                                data.getName() + "\n" + (int) data.getPieValue() + " participation(s)"
                        ))
        );
    }

    private void corrigerCouleursLegende() {
        // Attendre que le PieChart soit rendu
        pieChartParticipations.applyCss();
        pieChartParticipations.layout();

        // Map des couleurs par statut
        Map<String, String> legendColors = new HashMap<>();
        legendColors.put("confirme", "#10b981");   // Vert
        legendColors.put("attente", "#f59e0b");    // Orange
        legendColors.put("annule", "#ef4444");     // Rouge

        // Appliquer les couleurs aux items de la légende
        int index = 0;
        for (PieChart.Data data : pieChartParticipations.getData()) {
            String name = data.getName();
            String statut = name.split(" ")[0]; // Extraire le statut avant l'espace

            String color = legendColors.getOrDefault(statut, "#888888");

            // Appliquer la couleur au symbole de la légende
            final int idx = index;
            final String finalColor = color;

            javafx.application.Platform.runLater(() -> {
                try {
                    // Chercher le symbole de légende par index
                    Set<Node> legendItems = pieChartParticipations.lookupAll(".chart-legend-item");
                    if (legendItems.size() > idx) {
                        Node legendItem = (Node) legendItems.toArray()[idx];
                        Node symbol = legendItem.lookup(".chart-legend-item-symbol");
                        if (symbol != null) {
                            symbol.setStyle("-fx-background-color: " + finalColor + ";");
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs de rendu
                }
            });

            index++;
        }
    }
}
