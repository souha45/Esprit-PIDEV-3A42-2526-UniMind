package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import org.example.entities.CategorieMeditation;
import org.example.entities.SeanceMeditation;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.services.CategorieMeditationServices;
import org.example.services.SeanceMeditationServices;
import org.example.services.EvenementService;
import org.example.services.ParticipationService;
import org.example.enums.StatutEvenement;
import org.example.enums.StatutParticipation;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatsController implements Initializable {

    // ── KPI Séances ──
    @FXML private Label lblTotalCategories;
    @FXML private Label lblTotalSeances;
    @FXML private Label lblTotalSeancesLabel;
    @FXML private Label lblSeancesActives;
    @FXML private Label lblSeancesVideo;
    @FXML private Label lblSeancesAudio;
    @FXML private Label lblDureeMoyenne;
    @FXML private Label lblFilterResult;

    // ── Filtres ──
    @FXML private ComboBox<String> cbFilterType;
    @FXML private ComboBox<String> cbFilterNiveau;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private ComboBox<String> cbFilterPeriode;
    @FXML private ComboBox<String> cbFilterCategorie;

    // ── Charts Séances ──
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private PieChart pieChart;
    @FXML private ComboBox<String> cbPeriodeLine;
    @FXML private ComboBox<String> cbPieSplit;
    @FXML private Label lblLineSubtitle;
    @FXML private Label lblPieSubtitle;

    // ── KPI Événements ──
    @FXML private Label lblTotalEvenements;
    @FXML private Label lblEvenementsAVenir;
    @FXML private Label lblTotalParticipations;
    @FXML private Label lblParticipationsConfirmees;

    // ── Charts Événements ──
    @FXML private BarChart<String, Number> barChartEvenements;
    @FXML private CategoryAxis xAxisEvenements;
    @FXML private PieChart pieChartParticipations;

    // ── Services ──
    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();
    private final CategorieMeditationServices categorieService = new CategorieMeditationServices();
    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

    // ── State ──
    private List<SeanceMeditation> allSeances = new ArrayList<>();
    private List<CategorieMeditation> allCategories = new ArrayList<>();
    private Map<Integer, String> catNamesMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilterCombos();
        setupChartCombos();

        try {
            allSeances = seanceService.afficher();
            allCategories = categorieService.afficher();
            catNamesMap = allCategories.stream()
                    .collect(Collectors.toMap(
                            CategorieMeditation::getCategorieId,
                            CategorieMeditation::getNom));

            // Remplir le filtre catégories dynamiquement
            List<String> catOptions = new ArrayList<>();
            catOptions.add("Toutes");
            allCategories.forEach(c -> catOptions.add(c.getNom()));
            cbFilterCategorie.setItems(FXCollections.observableArrayList(catOptions));
            cbFilterCategorie.getSelectionModel().selectFirst();

            // Chargement initial sans filtres
            applyFiltersAndRefresh();

            // Événements
            List<Evenement> evenements = evenementService.afficher();
            List<Participation> participations = participationService.afficher();
            loadEventKPIs(evenements, participations);
            loadEventBarChart(evenements);
            loadParticipationPieChart(participations);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════
    //  SETUP COMBOS
    // ══════════════════════════════════════════

    private void setupFilterCombos() {
        cbFilterType.setItems(FXCollections.observableArrayList(
                "Tous les types", "VIDEO", "AUDIO"));
        cbFilterType.getSelectionModel().selectFirst();

        cbFilterNiveau.setItems(FXCollections.observableArrayList(
                "Tous les niveaux", "DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
        cbFilterNiveau.getSelectionModel().selectFirst();

        cbFilterStatut.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Actives", "Inactives"));
        cbFilterStatut.getSelectionModel().selectFirst();

        cbFilterPeriode.setItems(FXCollections.observableArrayList(
                "Toute période",
                "Cette semaine",
                "Ce mois-ci",
                "3 derniers mois",
                "6 derniers mois",
                "Cette année"));
        cbFilterPeriode.getSelectionModel().selectFirst();
    }

    private void setupChartCombos() {
        cbPeriodeLine.setItems(FXCollections.observableArrayList(
                "12 derniers mois",
                "6 derniers mois",
                "3 derniers mois",
                "Cette année",
                "Semaine par semaine"));
        cbPeriodeLine.getSelectionModel().selectFirst();

        cbPieSplit.setItems(FXCollections.observableArrayList(
                "Par catégorie",
                "Par type (Vidéo/Audio)",
                "Par niveau",
                "Par statut"));
        cbPieSplit.getSelectionModel().selectFirst();
    }

    // ══════════════════════════════════════════
    //  ACTIONS FILTRES
    // ══════════════════════════════════════════

    @FXML
    private void onFilterChanged() {
        applyFiltersAndRefresh();
    }

    @FXML
    private void onResetFilters() {
        cbFilterType.getSelectionModel().selectFirst();
        cbFilterNiveau.getSelectionModel().selectFirst();
        cbFilterStatut.getSelectionModel().selectFirst();
        cbFilterPeriode.getSelectionModel().selectFirst();
        cbFilterCategorie.getSelectionModel().selectFirst();
        applyFiltersAndRefresh();
    }

    @FXML
    private void onPeriodLineChanged() {
        List<SeanceMeditation> filtered = getFilteredSeances();
        refreshLineChart(filtered);
    }

    @FXML
    private void onPieSplitChanged() {
        List<SeanceMeditation> filtered = getFilteredSeances();
        refreshPieChart(filtered);
    }

    // ══════════════════════════════════════════
    //  LOGIQUE FILTRE PRINCIPAL
    // ══════════════════════════════════════════

    private List<SeanceMeditation> getFilteredSeances() {
        String type    = cbFilterType.getValue();
        String niveau  = cbFilterNiveau.getValue();
        String statut  = cbFilterStatut.getValue();
        String periode = cbFilterPeriode.getValue();
        String cat     = cbFilterCategorie.getValue();

        LocalDate dateMin = getDateMinForPeriode(periode);

        return allSeances.stream().filter(s -> {

            // Filtre Type
            boolean matchType = type == null || type.equals("Tous les types")
                    || (s.getTypeFichier() != null && s.getTypeFichier().name().equalsIgnoreCase(type));

            // Filtre Niveau
            boolean matchNiveau = niveau == null || niveau.equals("Tous les niveaux")
                    || (s.getNiveau() != null && s.getNiveau().name().equalsIgnoreCase(niveau));

            // Filtre Statut
            boolean matchStatut = statut == null || statut.equals("Tous les statuts")
                    || (statut.equals("Actives") && s.isIsActive())
                    || (statut.equals("Inactives") && !s.isIsActive());

            // Filtre Période
            boolean matchPeriode = true;
            if (dateMin != null && s.getCreatedAt() != null) {
                LocalDate created = s.getCreatedAt().toLocalDateTime().toLocalDate();
                matchPeriode = !created.isBefore(dateMin);
            }

            // Filtre Catégorie
            boolean matchCat = cat == null || cat.equals("Toutes")
                    || catNamesMap.getOrDefault(s.getCategorieId(), "").equals(cat);

            return matchType && matchNiveau && matchStatut && matchPeriode && matchCat;

        }).collect(Collectors.toList());
    }

    private LocalDate getDateMinForPeriode(String periode) {
        if (periode == null) return null;
        LocalDate now = LocalDate.now();
        return switch (periode) {
            case "Cette semaine"     -> now.minusWeeks(1);
            case "Ce mois-ci"        -> now.withDayOfMonth(1);
            case "3 derniers mois"   -> now.minusMonths(3);
            case "6 derniers mois"   -> now.minusMonths(6);
            case "Cette année"       -> now.withDayOfYear(1);
            default                  -> null; // Toute période
        };
    }

    // ══════════════════════════════════════════
    //  REFRESH COMPLET
    // ══════════════════════════════════════════

    private void applyFiltersAndRefresh() {
        List<SeanceMeditation> filtered = getFilteredSeances();

        // Label résultat filtre
        boolean hasFilter = isFilterActive();
        if (hasFilter) {
            lblFilterResult.setText("✦ " + filtered.size() + " séance(s) filtrée(s)");
        } else {
            lblFilterResult.setText("");
        }

        refreshKPIs(filtered);
        refreshLineChart(filtered);
        refreshPieChart(filtered);
    }

    private boolean isFilterActive() {
        return !cbFilterType.getValue().equals("Tous les types")
                || !cbFilterNiveau.getValue().equals("Tous les niveaux")
                || !cbFilterStatut.getValue().equals("Tous les statuts")
                || !cbFilterPeriode.getValue().equals("Toute période")
                || (cbFilterCategorie.getValue() != null && !cbFilterCategorie.getValue().equals("Toutes"));
    }

    // ══════════════════════════════════════════
    //  KPIs
    // ══════════════════════════════════════════

    private void refreshKPIs(List<SeanceMeditation> seances) {
        lblTotalCategories.setText(String.valueOf(allCategories.size()));
        lblTotalSeances.setText(String.valueOf(seances.size()));

        // Label dynamique selon le filtre
        boolean hasFilter = isFilterActive();
        lblTotalSeancesLabel.setText(hasFilter ? "Séances filtrées" : "Séances totales");

        long actives = seances.stream().filter(SeanceMeditation::isIsActive).count();
        lblSeancesActives.setText(String.valueOf(actives));

        long videos = seances.stream()
                .filter(s -> s.getTypeFichier() != null && s.getTypeFichier().name().equalsIgnoreCase("VIDEO"))
                .count();
        lblSeancesVideo.setText(String.valueOf(videos));

        long audios = seances.stream()
                .filter(s -> s.getTypeFichier() != null && s.getTypeFichier().name().equalsIgnoreCase("AUDIO"))
                .count();
        lblSeancesAudio.setText(String.valueOf(audios));

        // Durée moyenne en minutes
        OptionalDouble moyenneSec = seances.stream()
                .mapToInt(SeanceMeditation::getDuree)
                .average();
        if (moyenneSec.isPresent()) {
            int moyMin = (int) (moyenneSec.getAsDouble() / 60);
            int moySec = (int) (moyenneSec.getAsDouble() % 60);
            lblDureeMoyenne.setText(String.format("%d:%02d min", moyMin, moySec));
        } else {
            lblDureeMoyenne.setText("— min");
        }
    }

    // ══════════════════════════════════════════
    //  LINE CHART
    // ══════════════════════════════════════════

    private void refreshLineChart(List<SeanceMeditation> seances) {
        lineChart.getData().clear();
        xAxis.getCategories().clear();

        String period = cbPeriodeLine.getValue();
        if (period == null) period = "12 derniers mois";

        List<String> labels = new ArrayList<>();
        Map<String, Long> countMap = new LinkedHashMap<>();

        LocalDate now = LocalDate.now();

        if (period.equals("Semaine par semaine")) {
            // 8 dernières semaines
            DateTimeFormatter wf = DateTimeFormatter.ofPattern("'S'ww/yyyy", Locale.FRENCH);
            for (int i = 7; i >= 0; i--) {
                String label = now.minusWeeks(i).format(wf);
                labels.add(label);
                countMap.put(label, 0L);
            }
            seances.stream()
                    .filter(s -> s.getCreatedAt() != null)
                    .forEach(s -> {
                        LocalDate d = s.getCreatedAt().toLocalDateTime().toLocalDate();
                        if (!d.isBefore(now.minusWeeks(8))) {
                            String key = d.format(wf);
                            countMap.computeIfPresent(key, (k, v) -> v + 1);
                        }
                    });
            lblLineSubtitle.setText("Séances créées — 8 dernières semaines");
        } else {
            // Par mois
            int nbMonths = switch (period) {
                case "6 derniers mois" -> 6;
                case "3 derniers mois" -> 3;
                case "Cette année"     -> now.getMonthValue();
                default                -> 12;
            };

            DateTimeFormatter mf = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);
            for (int i = nbMonths - 1; i >= 0; i--) {
                String label = now.minusMonths(i).format(mf);
                labels.add(label);
                countMap.put(label, 0L);
            }
            seances.stream()
                    .filter(s -> s.getCreatedAt() != null)
                    .forEach(s -> {
                        LocalDate d = s.getCreatedAt().toLocalDateTime().toLocalDate();
                        if (!d.isBefore(now.minusMonths(nbMonths))) {
                            String key = d.format(mf);
                            countMap.computeIfPresent(key, (k, v) -> v + 1);
                        }
                    });
            lblLineSubtitle.setText("Séances créées — " + period.toLowerCase());
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Séances");
        for (String label : labels) {
            series.getData().add(new XYChart.Data<>(label, countMap.getOrDefault(label, 0L)));
        }

        xAxis.setCategories(FXCollections.observableArrayList(labels));
        lineChart.getData().add(series);
        javafx.application.Platform.runLater(this::styleLineChart);
    }

    private void styleLineChart() {
        lineChart.lookupAll(".chart-series-line").forEach(n ->
                n.setStyle("-fx-stroke: #6366f1; -fx-stroke-width: 2.5px;"));
        lineChart.lookupAll(".chart-line-symbol").forEach(n ->
                n.setStyle("-fx-background-color: #6366f1, white; -fx-background-radius: 5px; -fx-padding: 4px;"));
    }

    // ══════════════════════════════════════════
    //  PIE CHART
    // ══════════════════════════════════════════

    private void refreshPieChart(List<SeanceMeditation> seances) {
        pieChart.getData().clear();

        String splitBy = cbPieSplit.getValue();
        if (splitBy == null) splitBy = "Par catégorie";

        Map<String, Long> dataMap;
        String[] colors = {"#6366f1","#8b5cf6","#ec4899","#f59e0b","#10b981","#3b82f6","#ef4444","#14b8a6"};

        switch (splitBy) {
            case "Par type (Vidéo/Audio)" -> {
                dataMap = seances.stream()
                        .filter(s -> s.getTypeFichier() != null)
                        .collect(Collectors.groupingBy(s -> s.getTypeFichier().name(), Collectors.counting()));
                lblPieSubtitle.setText("Répartition VIDEO / AUDIO");
            }
            case "Par niveau" -> {
                dataMap = seances.stream()
                        .filter(s -> s.getNiveau() != null)
                        .collect(Collectors.groupingBy(s -> s.getNiveau().name(), Collectors.counting()));
                lblPieSubtitle.setText("Répartition par niveau");
            }
            case "Par statut" -> {
                dataMap = seances.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.isIsActive() ? "Active ✅" : "Inactive ⛔",
                                Collectors.counting()));
                lblPieSubtitle.setText("Répartition Actives / Inactives");
            }
            default -> { // Par catégorie
                dataMap = seances.stream()
                        .collect(Collectors.groupingBy(
                                s -> catNamesMap.getOrDefault(s.getCategorieId(), "Autre"),
                                Collectors.counting()));
                lblPieSubtitle.setText("Répartition par catégorie");
            }
        }

        if (dataMap.isEmpty()) {
            pieChart.getData().add(new PieChart.Data("Aucune donnée", 1));
            return;
        }

        List<PieChart.Data> pieData = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Long> entry : dataMap.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieData.add(slice);
            final String color = colors[colorIdx % colors.length];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            colorIdx++;
        }

        pieChart.setData(FXCollections.observableArrayList(pieData));
        pieChart.getData().forEach(data ->
                Tooltip.install(data.getNode(),
                        new Tooltip(data.getName() + "\n" + (int) data.getPieValue() + " séance(s)")));
    }

    // ══════════════════════════════════════════
    //  MODULE ÉVÉNEMENTS (inchangé)
    // ══════════════════════════════════════════

    private void loadEventKPIs(List<Evenement> evenements, List<Participation> participations) {
        lblTotalEvenements.setText(String.valueOf(evenements.size()));
        LocalDate today = LocalDate.now();
        long aVenir = evenements.stream()
                .filter(e -> e.getStatut() == StatutEvenement.A_VENIR || e.getStatut() == StatutEvenement.EN_COURS)
                .filter(e -> e.getDateDebut() != null && !e.getDateDebut().toLocalDateTime().toLocalDate().isBefore(today))
                .count();
        lblEvenementsAVenir.setText(String.valueOf(aVenir));
        lblTotalParticipations.setText(String.valueOf(participations.size()));
        long confirmees = participations.stream()
                .filter(p -> p.getStatut() == StatutParticipation.CONFIRME).count();
        lblParticipationsConfirmees.setText(String.valueOf(confirmees));
    }

    private void loadEventBarChart(List<Evenement> evenements) {
        Map<String, Long> countByType = evenements.stream()
                .filter(e -> e.getType() != null)
                .collect(Collectors.groupingBy(e -> e.getType().name(), Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Événements");
        countByType.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
        xAxisEvenements.setCategories(FXCollections.observableArrayList(countByType.keySet()));
        barChartEvenements.getData().add(series);
        javafx.application.Platform.runLater(() ->
                barChartEvenements.lookupAll(".chart-bar").forEach(n ->
                        n.setStyle("-fx-bar-fill: #6366f1;")));
    }

    private void loadParticipationPieChart(List<Participation> participations) {
        String[] colors = {"#10b981", "#f59e0b", "#ef4444"};
        Map<StatutParticipation, Long> countByStatut = participations.stream()
                .collect(Collectors.groupingBy(Participation::getStatut, Collectors.counting()));
        List<PieChart.Data> pieData = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<StatutParticipation, Long> entry : countByStatut.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey().getDbValue() + " (" + entry.getValue() + ")", entry.getValue());
            pieData.add(slice);
            final String color = colors[Math.min(idx, colors.length - 1)];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            idx++;
        }
        if (pieData.isEmpty()) pieData.add(new PieChart.Data("Aucune participation", 1));
        pieChartParticipations.setData(FXCollections.observableArrayList(pieData));
        pieChartParticipations.getData().forEach(data ->
                Tooltip.install(data.getNode(),
                        new Tooltip(data.getName() + "\n" + (int) data.getPieValue() + " participation(s)")));
    }
}