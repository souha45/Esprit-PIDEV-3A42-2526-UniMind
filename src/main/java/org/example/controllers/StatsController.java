package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import org.example.entities.CategorieMeditation;
import org.example.entities.SeanceMeditation;
import org.example.services.CategorieMeditationServices;
import org.example.services.SeanceMeditationServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatsController implements Initializable {

    // KPI Labels
    @FXML private javafx.scene.control.Label lblTotalCategories;
    @FXML private javafx.scene.control.Label lblTotalSeances;
    @FXML private javafx.scene.control.Label lblSeancesActives;
    @FXML private javafx.scene.control.Label lblSeancesVideo;

    // Charts
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private PieChart pieChart;

    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();
    private final CategorieMeditationServices categorieService = new CategorieMeditationServices();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            List<SeanceMeditation> seances = seanceService.afficher();
            List<CategorieMeditation> categories = categorieService.afficher();

            loadKPIs(seances, categories);
            loadLineChart(seances);
            loadPieChart(seances, categories);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== KPIs ====================

    private void loadKPIs(List<SeanceMeditation> seances, List<CategorieMeditation> categories) {
        lblTotalCategories.setText(String.valueOf(categories.size()));
        lblTotalSeances.setText(String.valueOf(seances.size()));

        long actives = seances.stream().filter(SeanceMeditation::isIsActive).count();
        lblSeancesActives.setText(String.valueOf(actives));

        long videos = seances.stream()
                .filter(s -> s.getTypeFichier() != null
                        && s.getTypeFichier().name().equals("video"))
                .count();
        lblSeancesVideo.setText(String.valueOf(videos));
    }

    // ==================== LINE CHART ====================

    private void loadLineChart(List<SeanceMeditation> seances) {
        // Group séances by month (last 12 months)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        // Build ordered list of last 12 months
        List<String> last12Months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            last12Months.add(now.minusMonths(i).format(formatter));
        }

        // Count séances per month
        Map<String, Long> countByMonth = seances.stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(s -> {
                    LocalDate d = s.getCreatedAt().toLocalDateTime().toLocalDate();
                    return d.format(formatter);
                }, Collectors.counting()));

        // Build series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Séances créées");

        for (String month : last12Months) {
            long count = countByMonth.getOrDefault(month, 0L);
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(month, count);
            series.getData().add(dataPoint);
        }

        xAxis.setCategories(FXCollections.observableArrayList(last12Months));
        lineChart.getData().add(series);

        // Style the line after rendering
        lineChart.setCreateSymbols(true);
        lineChart.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                styleLineChart();
            }
        });

        javafx.application.Platform.runLater(this::styleLineChart);
    }

    private void styleLineChart() {
        // Style the line
        lineChart.lookupAll(".chart-series-line").forEach(node ->
                node.setStyle("-fx-stroke: #6366f1; -fx-stroke-width: 2.5px;")
        );
        // Style the data points
        lineChart.lookupAll(".chart-line-symbol").forEach(node ->
                node.setStyle("-fx-background-color: #6366f1, white; -fx-background-radius: 5px; -fx-padding: 4px;")
        );
    }

    // ==================== PIE CHART ====================

    private void loadPieChart(List<SeanceMeditation> seances, List<CategorieMeditation> categories) {
        // Map categorie_id -> nom
        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(
                        CategorieMeditation::getCategorieId,
                        CategorieMeditation::getNom
                ));

        // Count séances per category
        Map<Integer, Long> countByCat = seances.stream()
                .collect(Collectors.groupingBy(
                        SeanceMeditation::getCategorieId,
                        Collectors.counting()
                ));

        // Build pie slices
        List<PieChart.Data> pieData = new ArrayList<>();
        String[] colors = {
                "#6366f1", "#8b5cf6", "#ec4899", "#f59e0b",
                "#10b981", "#3b82f6", "#ef4444", "#14b8a6"
        };

        int colorIdx = 0;
        for (Map.Entry<Integer, Long> entry : countByCat.entrySet()) {
            String catName = catNames.getOrDefault(entry.getKey(), "Catégorie " + entry.getKey());
            PieChart.Data slice = new PieChart.Data(catName + " (" + entry.getValue() + ")", entry.getValue());
            pieData.add(slice);

            // Apply color after rendering
            final String color = colors[colorIdx % colors.length];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + color + ";");
                }
            });
            colorIdx++;
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("Aucune séance", 1));
        }

        pieChart.setData(FXCollections.observableArrayList(pieData));

        // Tooltips on hover
        pieChart.getData().forEach(data ->
                javafx.scene.control.Tooltip.install(data.getNode(),
                        new javafx.scene.control.Tooltip(
                                data.getName() + "\n" + (int) data.getPieValue() + " séance(s)"
                        ))
        );
    }
}