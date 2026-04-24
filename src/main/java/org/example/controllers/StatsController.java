package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
    @FXML private Button btnExportCSV;

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
    private final SeanceMeditationServices seanceService    = new SeanceMeditationServices();
    private final CategorieMeditationServices categorieService = new CategorieMeditationServices();
    private final EvenementService evenementService          = new EvenementService();
    private final ParticipationService participationService  = new ParticipationService();

    // ── State ──
    private List<SeanceMeditation>   allSeances    = new ArrayList<>();
    private List<CategorieMeditation> allCategories = new ArrayList<>();
    private Map<Integer, String>     catNamesMap   = new HashMap<>();

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ══════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilterCombos();
        setupChartCombos();

        try {
            allSeances    = seanceService.afficher();
            allCategories = categorieService.afficher();
            catNamesMap   = allCategories.stream()
                    .collect(Collectors.toMap(
                            CategorieMeditation::getCategorieId,
                            CategorieMeditation::getNom));

            // Remplir filtre catégories dynamiquement
            List<String> catOptions = new ArrayList<>();
            catOptions.add("Toutes");
            allCategories.forEach(c -> catOptions.add(c.getNom()));
            cbFilterCategorie.setItems(FXCollections.observableArrayList(catOptions));
            cbFilterCategorie.getSelectionModel().selectFirst();

            applyFiltersAndRefresh();

            // Module Événements
            List<Evenement>     evenements     = evenementService.afficher();
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
                "Toute période", "Cette semaine", "Ce mois-ci",
                "3 derniers mois", "6 derniers mois", "Cette année"));
        cbFilterPeriode.getSelectionModel().selectFirst();
    }

    private void setupChartCombos() {
        cbPeriodeLine.setItems(FXCollections.observableArrayList(
                "12 derniers mois", "6 derniers mois", "3 derniers mois",
                "Cette année", "Semaine par semaine"));
        cbPeriodeLine.getSelectionModel().selectFirst();

        cbPieSplit.setItems(FXCollections.observableArrayList(
                "Par catégorie", "Par type (Vidéo/Audio)",
                "Par niveau", "Par statut"));
        cbPieSplit.getSelectionModel().selectFirst();
    }

    // ══════════════════════════════════════════
    //  ACTIONS FILTRES
    // ══════════════════════════════════════════

    @FXML private void onFilterChanged()   { applyFiltersAndRefresh(); }
    @FXML private void onPeriodLineChanged() {
        refreshLineChart(getFilteredSeances());
    }
    @FXML private void onPieSplitChanged() {
        refreshPieChart(getFilteredSeances());
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

    // ══════════════════════════════════════════
    //  1. EXPORT CSV
    // ══════════════════════════════════════════

    @FXML
    private void onExportCSV() {
        List<SeanceMeditation> filtered = getFilteredSeances();

        if (filtered.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Export CSV",
                    "Aucune séance à exporter avec les filtres actuels.");
            return;
        }

        // Ouvrir le sélecteur de fichier
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer les séances filtrées");
        fc.setInitialFileName("seances_unimind_"
                + LocalDate.now().toString().replace("-", "") + ".csv");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        File file = fc.showSaveDialog(btnExportCSV.getScene().getWindow());
        if (file == null) return; // annulé

        try (FileWriter writer = new FileWriter(file)) {

            // ── En-tête CSV ──
            writer.write("ID;Titre;Description;Type;Duree (sec);Duree (mm:ss);"
                    + "Niveau;Statut;Categorie;Date création\n");

            // ── Lignes ──
            for (SeanceMeditation s : filtered) {
                int min = s.getDuree() / 60, sec = s.getDuree() % 60;
                String ligne = String.join(";",
                        String.valueOf(s.getSeanceId()),
                        escapeCsv(s.getTitre()),
                        escapeCsv(s.getDescription()),
                        s.getTypeFichier() != null ? s.getTypeFichier().name() : "",
                        String.valueOf(s.getDuree()),
                        String.format("%d:%02d", min, sec),
                        s.getNiveau() != null ? s.getNiveau().name() : "",
                        s.isIsActive() ? "Active" : "Inactive",
                        escapeCsv(catNamesMap.getOrDefault(s.getCategorieId(), "—")),
                        s.getCreatedAt() != null ? DATE_FMT.format(s.getCreatedAt()) : ""
                );
                writer.write(ligne + "\n");
            }

            // ── Résumé en bas ──
            writer.write("\n");
            writer.write("Total exporté;" + filtered.size() + "\n");
            writer.write("Filtres appliqués;Type=" + cbFilterType.getValue()
                    + " | Niveau=" + cbFilterNiveau.getValue()
                    + " | Statut=" + cbFilterStatut.getValue()
                    + " | Période=" + cbFilterPeriode.getValue()
                    + " | Catégorie=" + cbFilterCategorie.getValue() + "\n");
            writer.write("Exporté le;" + DATE_FMT.format(new java.util.Date()) + "\n");

            showAlert(Alert.AlertType.INFORMATION,
                    "Export réussi ✅",
                    filtered.size() + " séance(s) exportée(s) dans :\n" + file.getAbsolutePath());

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur d'export",
                    "Impossible d'écrire le fichier : " + e.getMessage());
        }
    }

    /** Échappe une valeur CSV (guillemets si contient ; ou virgule) */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ══════════════════════════════════════════
    //  2. PIE CHART INTERACTIF — clic sur une part
    // ══════════════════════════════════════════

    /**
     * Attache les handlers de clic sur chaque part du PieChart.
     * Appelé après chaque refresh du PieChart.
     */
    private void attachPieClickHandlers(List<SeanceMeditation> filteredSeances) {
        String splitBy = cbPieSplit.getValue() != null ? cbPieSplit.getValue() : "Par catégorie";

        pieChart.getData().forEach(slice -> {
            // Extraire le label sans le "(nb)" à la fin
            String rawLabel = slice.getName();
            String label = rawLabel.contains("(")
                    ? rawLabel.substring(0, rawLabel.lastIndexOf("(")).trim()
                    : rawLabel;

            // Effet hover
            slice.getNode().setOnMouseEntered(e ->
                    slice.getNode().setStyle(
                            slice.getNode().getStyle() + "; -fx-opacity: 0.75; -fx-cursor: hand;"));
            slice.getNode().setOnMouseExited(e ->
                    slice.getNode().setStyle(
                            slice.getNode().getStyle().replace("; -fx-opacity: 0.75; -fx-cursor: hand;", "")));

            // Clic → ouvrir dialog
            final String splitByFinal = splitBy;
            slice.getNode().setOnMouseClicked(e ->
                    openSliceDetailDialog(label, splitByFinal, filteredSeances));
        });
    }

    /**
     * Ouvre un Dialog listant les séances correspondant à la part cliquée.
     */
    private void openSliceDetailDialog(String label, String splitBy,
                                       List<SeanceMeditation> filteredSeances) {
        // Filtrer les séances correspondant à ce segment
        List<SeanceMeditation> sliceSeances = filteredSeances.stream()
                .filter(s -> {
                    String key = switch (splitBy) {
                        case "Par type (Vidéo/Audio)" ->
                                s.getTypeFichier() != null ? s.getTypeFichier().name() : "—";
                        case "Par niveau" ->
                                s.getNiveau() != null ? s.getNiveau().name() : "—";
                        case "Par statut" ->
                                s.isIsActive() ? "Active ✅" : "Inactive ⛔";
                        default -> // Par catégorie
                                catNamesMap.getOrDefault(s.getCategorieId(), "Autre");
                    };
                    return key.equals(label);
                })
                .collect(Collectors.toList());

        // ── Construire le Dialog ──
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📋  " + label + " — " + sliceSeances.size() + " séance(s)");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 24, 8, 24));
        content.setPrefWidth(600);
        content.setMaxHeight(500);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("🥧  " + label);
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
        Label countBadge = new Label(sliceSeances.size() + " séance(s)");
        countBadge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; "
                + "-fx-padding: 4 12 4 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");
        header.getChildren().addAll(titleLbl, countBadge);

        Separator sep = new Separator();

        // Tableau des séances
        TableView<SeanceMeditation> table = new TableView<>();
        table.setPrefHeight(320);
        table.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(table, Priority.ALWAYS);

        // Colonnes
        TableColumn<SeanceMeditation, String> colTitre = new TableColumn<>("Titre");
        colTitre.setPrefWidth(180);
        colTitre.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getTitre()));
        colTitre.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #3730a3;");
            }
        });

        TableColumn<SeanceMeditation, String> colType = new TableColumn<>("Type");
        colType.setPrefWidth(75);
        colType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getTypeFichier() != null
                                ? d.getValue().getTypeFichier().name() : "—"));

        TableColumn<SeanceMeditation, String> colDuree = new TableColumn<>("Durée");
        colDuree.setPrefWidth(70);
        colDuree.setCellValueFactory(d -> {
            int min = d.getValue().getDuree() / 60, sec = d.getValue().getDuree() % 60;
            return new javafx.beans.property.SimpleStringProperty(
                    String.format("%d:%02d", min, sec));
        });

        TableColumn<SeanceMeditation, String> colNiveau = new TableColumn<>("Niveau");
        colNiveau.setPrefWidth(100);
        colNiveau.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getNiveau() != null
                                ? d.getValue().getNiveau().name() : "—"));

        TableColumn<SeanceMeditation, String> colStatut = new TableColumn<>("Statut");
        colStatut.setPrefWidth(80);
        colStatut.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().isIsActive() ? "✅ Active" : "⛔ Inactive"));

        TableColumn<SeanceMeditation, String> colCategorie = new TableColumn<>("Catégorie");
        colCategorie.setPrefWidth(150);
        colCategorie.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        catNamesMap.getOrDefault(d.getValue().getCategorieId(), "—")));

        table.getColumns().addAll(colTitre, colType, colDuree, colNiveau, colStatut, colCategorie);
        table.setItems(FXCollections.observableArrayList(sliceSeances));

        // Bouton export depuis ce dialog
        Button btnExport = new Button("📥  Exporter cette sélection CSV");
        btnExport.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 8 16 8 16; "
                + "-fx-background-radius: 8; -fx-cursor: hand;");
        btnExport.setOnAction(e -> exportSliceToCSV(label, sliceSeances));

        content.getChildren().addAll(header, sep, table, btnExport);

        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");
        dp.getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dp.lookupButton(ButtonType.CLOSE)).setStyle(
                "-fx-background-color: #6366f1; -fx-text-fill: white; "
                        + "-fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        dialog.showAndWait();
    }

    /** Export CSV depuis le dialog de détail d'un segment */
    private void exportSliceToCSV(String label, List<SeanceMeditation> seances) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter la sélection");
        fc.setInitialFileName("seances_" + label.replaceAll("[^a-zA-Z0-9]", "_") + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showSaveDialog(pieChart.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("ID;Titre;Type;Duree (mm:ss);Niveau;Statut;Categorie;Date création\n");
            for (SeanceMeditation s : seances) {
                int min = s.getDuree() / 60, sec = s.getDuree() % 60;
                writer.write(String.join(";",
                        String.valueOf(s.getSeanceId()),
                        escapeCsv(s.getTitre()),
                        s.getTypeFichier() != null ? s.getTypeFichier().name() : "",
                        String.format("%d:%02d", min, sec),
                        s.getNiveau() != null ? s.getNiveau().name() : "",
                        s.isIsActive() ? "Active" : "Inactive",
                        escapeCsv(catNamesMap.getOrDefault(s.getCategorieId(), "—")),
                        s.getCreatedAt() != null ? DATE_FMT.format(s.getCreatedAt()) : ""
                ) + "\n");
            }
            showAlert(Alert.AlertType.INFORMATION, "Export réussi ✅",
                    seances.size() + " séance(s) exportée(s).");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    //  LOGIQUE FILTRE
    // ══════════════════════════════════════════

    private List<SeanceMeditation> getFilteredSeances() {
        String type    = cbFilterType.getValue();
        String niveau  = cbFilterNiveau.getValue();
        String statut  = cbFilterStatut.getValue();
        String periode = cbFilterPeriode.getValue();
        String cat     = cbFilterCategorie.getValue();
        LocalDate dateMin = getDateMinForPeriode(periode);

        return allSeances.stream().filter(s -> {
            boolean matchType = type == null || type.equals("Tous les types")
                    || (s.getTypeFichier() != null && s.getTypeFichier().name().equalsIgnoreCase(type));
            boolean matchNiveau = niveau == null || niveau.equals("Tous les niveaux")
                    || (s.getNiveau() != null && s.getNiveau().name().equalsIgnoreCase(niveau));
            boolean matchStatut = statut == null || statut.equals("Tous les statuts")
                    || (statut.equals("Actives") && s.isIsActive())
                    || (statut.equals("Inactives") && !s.isIsActive());
            boolean matchPeriode = true;
            if (dateMin != null && s.getCreatedAt() != null) {
                LocalDate created = s.getCreatedAt().toLocalDateTime().toLocalDate();
                matchPeriode = !created.isBefore(dateMin);
            }
            boolean matchCat = cat == null || cat.equals("Toutes")
                    || catNamesMap.getOrDefault(s.getCategorieId(), "").equals(cat);
            return matchType && matchNiveau && matchStatut && matchPeriode && matchCat;
        }).collect(Collectors.toList());
    }

    private LocalDate getDateMinForPeriode(String periode) {
        if (periode == null) return null;
        LocalDate now = LocalDate.now();
        return switch (periode) {
            case "Cette semaine"   -> now.minusWeeks(1);
            case "Ce mois-ci"      -> now.withDayOfMonth(1);
            case "3 derniers mois" -> now.minusMonths(3);
            case "6 derniers mois" -> now.minusMonths(6);
            case "Cette année"     -> now.withDayOfYear(1);
            default                -> null;
        };
    }

    // ══════════════════════════════════════════
    //  REFRESH COMPLET
    // ══════════════════════════════════════════

    private void applyFiltersAndRefresh() {
        List<SeanceMeditation> filtered = getFilteredSeances();

        boolean hasFilter = isFilterActive();
        lblFilterResult.setText(hasFilter
                ? "✦ " + filtered.size() + " séance(s) filtrée(s)"
                : "");

        refreshKPIs(filtered);
        refreshLineChart(filtered);
        refreshPieChart(filtered);
    }

    private boolean isFilterActive() {
        return !cbFilterType.getValue().equals("Tous les types")
                || !cbFilterNiveau.getValue().equals("Tous les niveaux")
                || !cbFilterStatut.getValue().equals("Tous les statuts")
                || !cbFilterPeriode.getValue().equals("Toute période")
                || (cbFilterCategorie.getValue() != null
                && !cbFilterCategorie.getValue().equals("Toutes"));
    }

    // ══════════════════════════════════════════
    //  KPIs
    // ══════════════════════════════════════════

    private void refreshKPIs(List<SeanceMeditation> seances) {
        lblTotalCategories.setText(String.valueOf(allCategories.size()));
        lblTotalSeances.setText(String.valueOf(seances.size()));
        lblTotalSeancesLabel.setText(isFilterActive() ? "Séances filtrées" : "Séances totales");

        long actives = seances.stream().filter(SeanceMeditation::isIsActive).count();
        lblSeancesActives.setText(String.valueOf(actives));

        long videos = seances.stream()
                .filter(s -> s.getTypeFichier() != null
                        && s.getTypeFichier().name().equalsIgnoreCase("VIDEO"))
                .count();
        lblSeancesVideo.setText(String.valueOf(videos));

        long audios = seances.stream()
                .filter(s -> s.getTypeFichier() != null
                        && s.getTypeFichier().name().equalsIgnoreCase("AUDIO"))
                .count();
        lblSeancesAudio.setText(String.valueOf(audios));

        OptionalDouble moy = seances.stream().mapToInt(SeanceMeditation::getDuree).average();
        if (moy.isPresent()) {
            int mm = (int)(moy.getAsDouble() / 60), ss = (int)(moy.getAsDouble() % 60);
            lblDureeMoyenne.setText(String.format("%d:%02d min", mm, ss));
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

        String period = cbPeriodeLine.getValue() != null ? cbPeriodeLine.getValue() : "12 derniers mois";
        LocalDate now = LocalDate.now();
        List<String> labels = new ArrayList<>();
        Map<String, Long> countMap = new LinkedHashMap<>();

        if (period.equals("Semaine par semaine")) {
            DateTimeFormatter wf = DateTimeFormatter.ofPattern("'S'ww/yy", Locale.FRENCH);
            for (int i = 7; i >= 0; i--) {
                String lbl = now.minusWeeks(i).format(wf);
                labels.add(lbl); countMap.put(lbl, 0L);
            }
            seances.stream().filter(s -> s.getCreatedAt() != null).forEach(s -> {
                LocalDate d = s.getCreatedAt().toLocalDateTime().toLocalDate();
                if (!d.isBefore(now.minusWeeks(8))) {
                    String k = d.format(wf);
                    countMap.computeIfPresent(k, (key, v) -> v + 1);
                }
            });
            lblLineSubtitle.setText("Séances créées — 8 dernières semaines");
        } else {
            int nbMonths = switch (period) {
                case "6 derniers mois" -> 6;
                case "3 derniers mois" -> 3;
                case "Cette année"     -> now.getMonthValue();
                default                -> 12;
            };
            DateTimeFormatter mf = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);
            for (int i = nbMonths - 1; i >= 0; i--) {
                String lbl = now.minusMonths(i).format(mf);
                labels.add(lbl); countMap.put(lbl, 0L);
            }
            seances.stream().filter(s -> s.getCreatedAt() != null).forEach(s -> {
                LocalDate d = s.getCreatedAt().toLocalDateTime().toLocalDate();
                if (!d.isBefore(now.minusMonths(nbMonths))) {
                    String k = d.format(mf);
                    countMap.computeIfPresent(k, (key, v) -> v + 1);
                }
            });
            lblLineSubtitle.setText("Séances créées — " + period.toLowerCase());
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Séances");
        labels.forEach(lbl -> series.getData().add(
                new XYChart.Data<>(lbl, countMap.getOrDefault(lbl, 0L))));
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

        String splitBy = cbPieSplit.getValue() != null ? cbPieSplit.getValue() : "Par catégorie";
        String[] colors = {"#6366f1","#8b5cf6","#ec4899","#f59e0b",
                "#10b981","#3b82f6","#ef4444","#14b8a6"};

        Map<String, Long> dataMap;
        switch (splitBy) {
            case "Par type (Vidéo/Audio)" -> {
                dataMap = seances.stream().filter(s -> s.getTypeFichier() != null)
                        .collect(Collectors.groupingBy(s -> s.getTypeFichier().name(), Collectors.counting()));
                lblPieSubtitle.setText("Répartition VIDEO / AUDIO");
            }
            case "Par niveau" -> {
                dataMap = seances.stream().filter(s -> s.getNiveau() != null)
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
            default -> {
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

        int colorIdx = 0;
        for (Map.Entry<String, Long> entry : dataMap.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieChart.getData().add(slice);
            final String color = colors[colorIdx % colors.length];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            colorIdx++;
        }

        // Tooltips
        pieChart.getData().forEach(data ->
                Tooltip.install(data.getNode(),
                        new Tooltip(data.getName() + "\n"
                                + (int) data.getPieValue() + " séance(s)"
                                + "\n🖱 Cliquer pour voir le détail")));

        // ✅ Attacher les handlers de clic APRÈS que les nodes soient créés
        final List<SeanceMeditation> seancesCopy = new ArrayList<>(seances);
        javafx.application.Platform.runLater(() ->
                attachPieClickHandlers(seancesCopy));
    }

    // ══════════════════════════════════════════
    //  MODULE ÉVÉNEMENTS
    // ══════════════════════════════════════════

    private void loadEventKPIs(List<Evenement> evenements, List<Participation> participations) {
        lblTotalEvenements.setText(String.valueOf(evenements.size()));
        LocalDate today = LocalDate.now();
        long aVenir = evenements.stream()
                .filter(e -> e.getStatut() == StatutEvenement.A_VENIR
                        || e.getStatut() == StatutEvenement.EN_COURS)
                .filter(e -> e.getDateDebut() != null
                        && !e.getDateDebut().toLocalDateTime().toLocalDate().isBefore(today))
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
                barChartEvenements.lookupAll(".chart-bar")
                        .forEach(n -> n.setStyle("-fx-bar-fill: #6366f1;")));
    }

    private void loadParticipationPieChart(List<Participation> participations) {
        String[] colors = {"#10b981", "#f59e0b", "#ef4444"};
        Map<StatutParticipation, Long> countByStatut = participations.stream()
                .collect(Collectors.groupingBy(Participation::getStatut, Collectors.counting()));
        int idx = 0;
        for (Map.Entry<StatutParticipation, Long> entry : countByStatut.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey().getDbValue() + " (" + entry.getValue() + ")", entry.getValue());
            pieChartParticipations.getData().add(slice);
            final String color = colors[Math.min(idx, colors.length - 1)];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            idx++;
        }
        if (pieChartParticipations.getData().isEmpty())
            pieChartParticipations.getData().add(new PieChart.Data("Aucune participation", 1));
        pieChartParticipations.getData().forEach(data ->
                Tooltip.install(data.getNode(),
                        new Tooltip(data.getName() + "\n" + (int) data.getPieValue() + " participation(s)")));
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
        alert.showAndWait();
    }
}