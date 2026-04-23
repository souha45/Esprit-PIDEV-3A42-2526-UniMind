package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.Etudiant;
import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.services.EtudiantTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class TraitementStatistiquesController implements Initializable {

    @FXML private Label lblDate;
    @FXML private Label lblStatus;
    @FXML private Label lblAnneeLabel;

    // Cartes
    @FXML private Label statTotal;
    @FXML private Label statEnCours;
    @FXML private Label statTermine;
    @FXML private Label statSuspendu;
    @FXML private Label statPrioriteHaute;

    // Champs FXML
    @FXML private PieChart pieChartStatut;
    @FXML private PieChart pieChartPriorite;
    @FXML private TableView<EtudiantStat> tableViewTopEtudiants;
    @FXML private TableColumn<EtudiantStat, String> colEtudiantNom;
    @FXML private TableColumn<EtudiantStat, Integer> colNbTraitements;
    @FXML private TableColumn<EtudiantStat, Integer> colEnCours;
    @FXML private TableColumn<EtudiantStat, Integer> colTermines;
    @FXML private TableColumn<EtudiantStat, Integer> colSuspendus;
    @FXML private VBox vboxBarCategorie;
    @FXML private VBox vboxBarEvolution;
    @FXML private VBox vboxEvolution;

    // Graphiques créés programmatiquement
    private BarChart<String, Number> barChartCategorie;
    private BarChart<String, Number> barChartEvolution;
    private CategoryAxis xAxisCategorie;
    private NumberAxis   yAxisCategorie;
    private CategoryAxis xAxisEvolution;
    private NumberAxis   yAxisEvolution;

    // Filtres
    @FXML private ComboBox<String>  cmbPeriode;
    @FXML private ComboBox<Integer> cmbAnnee;

    private TraitementService          traitementService;
    private EtudiantTraitementService  etudiantTraitementService;
    private List<Traitement>           tousLesTraitements;
    private List<Etudiant>             tousLesEtudiants;
    private User                       utilisateur;

    // ── Classe interne ────────────────────────────────────────────────────────
    public static class EtudiantStat {
        private final String nom;
        private int total, enCours, termine, suspendu;

        public EtudiantStat(String nom) { this.nom = nom; }

        public String getNom()    { return nom; }
        public int getTotal()     { return total; }
        public int getEnCours()   { return enCours; }
        public int getTermine()   { return termine; }
        public int getSuspendu()  { return suspendu; }

        public void incrementTotal()    { total++; }
        public void incrementEnCours()  { enCours++; }
        public void incrementTermine()  { termine++; }
        public void incrementSuspendu() { suspendu++; }
    }

    // ── initialize ────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lblDate != null)
            lblDate.setText(LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        initialiserPeriodes();
        configurerTableau();
        creerGraphiques();

        try {
            traitementService         = new TraitementService();
            etudiantTraitementService = new EtudiantTraitementService();
        } catch (Exception e) {
            if (lblStatus != null)
                lblStatus.setText("✗ Erreur d'initialisation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void creerGraphiques() {
        // ── BarChart Catégorie ──────────────────────────────
        xAxisCategorie = new CategoryAxis();
        xAxisCategorie.setLabel("Catégorie de traitement");
        xAxisCategorie.setTickMarkVisible(true);
        xAxisCategorie.setTickLabelsVisible(true);
        xAxisCategorie.setAnimated(false);
        xAxisCategorie.setAutoRanging(false);
        // ✅ Pré-remplir TOUTES les catégories dès la création
        xAxisCategorie.setCategories(FXCollections.observableArrayList(
                "Cognitif", "Comportemental", "Émotionnel", "Relaxation"));

        yAxisCategorie = new NumberAxis(0, 15, 1);
        yAxisCategorie.setLabel("Nombre de traitements");
        yAxisCategorie.setTickMarkVisible(true);
        yAxisCategorie.setTickLabelsVisible(true);
        yAxisCategorie.setMinorTickCount(0);
        yAxisCategorie.setAnimated(false);
        yAxisCategorie.setAutoRanging(false);

        barChartCategorie = new BarChart<>(xAxisCategorie, yAxisCategorie);
        barChartCategorie.setBarGap(8);
        barChartCategorie.setCategoryGap(40);
        barChartCategorie.setLegendVisible(false);
        barChartCategorie.setAnimated(false);
        barChartCategorie.setPrefHeight(320);
        barChartCategorie.setTitle("Traitements par Catégorie");
        VBox.setVgrow(barChartCategorie, javafx.scene.layout.Priority.ALWAYS);

        if (vboxBarCategorie != null) {
            vboxBarCategorie.getChildren().clear();
            vboxBarCategorie.getChildren().add(barChartCategorie);
        }

        // ── BarChart Évolution ──────────────────────────────
        xAxisEvolution = new CategoryAxis();
        xAxisEvolution.setLabel("Période");
        xAxisEvolution.setTickMarkVisible(true);
        xAxisEvolution.setTickLabelsVisible(true);
        xAxisEvolution.setTickLabelRotation(30);
        xAxisEvolution.setAnimated(false);
        xAxisEvolution.setAutoRanging(true);

        yAxisEvolution = new NumberAxis(0, 15, 1);
        yAxisEvolution.setLabel("Nombre de traitements");
        yAxisEvolution.setTickMarkVisible(true);
        yAxisEvolution.setTickLabelsVisible(true);
        yAxisEvolution.setMinorTickCount(0);
        yAxisEvolution.setAnimated(false);
        yAxisEvolution.setAutoRanging(false);

        barChartEvolution = new BarChart<>(xAxisEvolution, yAxisEvolution);
        barChartEvolution.setBarGap(8);
        barChartEvolution.setCategoryGap(40);
        barChartEvolution.setLegendVisible(false);
        barChartEvolution.setAnimated(false);
        barChartEvolution.setPrefHeight(320);
        barChartEvolution.setTitle("Évolution mensuelle des traitements");
        VBox.setVgrow(barChartEvolution, javafx.scene.layout.Priority.ALWAYS);

        if (vboxBarEvolution != null) {
            vboxBarEvolution.getChildren().clear();
            vboxBarEvolution.getChildren().add(barChartEvolution);
        }
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user != null) SessionManager.getInstance().initSession(user);
        chargerDonnees();
    }

    // ── Configuration initiale ────────────────────────────────────────────────
    private void initialiserPeriodes() {
        if (cmbPeriode != null) {
            cmbPeriode.setItems(FXCollections.observableArrayList(
                    "Cette année", "Ce mois", "Cette semaine", "Aujourd'hui"));
            cmbPeriode.setValue("Cette année");

            cmbPeriode.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                boolean estAnnee = "Cette année".equals(newVal);
                if (cmbAnnee != null)      cmbAnnee.setDisable(!estAnnee);
                if (lblAnneeLabel != null) lblAnneeLabel.setDisable(!estAnnee);

                boolean afficherEvolution =
                        !("Cette semaine".equals(newVal) || "Aujourd'hui".equals(newVal));
                if (vboxEvolution != null) {
                    vboxEvolution.setVisible(afficherEvolution);
                    vboxEvolution.setManaged(afficherEvolution);
                }
                if (tousLesTraitements != null) mettreAJourStatistiques();
            });
        }

        if (cmbAnnee != null) {
            int anneeActuelle = LocalDate.now().getYear();
            ObservableList<Integer> annees = FXCollections.observableArrayList();
            for (int i = 2020; i <= anneeActuelle + 1; i++) annees.add(i);
            cmbAnnee.setItems(annees);
            cmbAnnee.setValue(anneeActuelle);
            cmbAnnee.valueProperty().addListener((obs, o, n) -> {
                if (tousLesTraitements != null) mettreAJourStatistiques();
            });
        }
    }


    private void configurerTableau() {
        if (tableViewTopEtudiants != null)
            tableViewTopEtudiants.setColumnResizePolicy(
                    TableView.CONSTRAINED_RESIZE_POLICY);

        if (colEtudiantNom != null)
            colEtudiantNom.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getNom()));
        if (colNbTraitements != null)
            colNbTraitements.setCellValueFactory(c ->
                    new SimpleIntegerProperty(c.getValue().getTotal()).asObject());
        if (colEnCours != null)
            colEnCours.setCellValueFactory(c ->
                    new SimpleIntegerProperty(c.getValue().getEnCours()).asObject());
        if (colTermines != null)
            colTermines.setCellValueFactory(c ->
                    new SimpleIntegerProperty(c.getValue().getTermine()).asObject());
        if (colSuspendus != null)
            colSuspendus.setCellValueFactory(c ->
                    new SimpleIntegerProperty(c.getValue().getSuspendu()).asObject());
    }

    // ── Chargement ───────────────────────────────────────────────────────────
    private void chargerDonnees() {
        if (utilisateur == null) {
            SessionManager session = SessionManager.getInstance();
            if (session.estConnecte()) this.utilisateur = session.getCurrentUser();
            else {
                if (lblStatus != null) lblStatus.setText("✗ Utilisateur non connecté");
                return;
            }
        }
        try {
            if (lblStatus != null) lblStatus.setText("Chargement…");
            int id = utilisateur.getUserId();
            tousLesTraitements = traitementService.afficher().stream()
                    .filter(t -> t.getPsychologueId() == id)
                    .collect(Collectors.toList());
            tousLesEtudiants = etudiantTraitementService.afficher();
            mettreAJourStatistiques();
        } catch (SQLException e) {
            if (lblStatus != null)
                lblStatus.setText("✗ Erreur BD : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            if (lblStatus != null)
                lblStatus.setText("✗ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Mise à jour globale ───────────────────────────────────────────────────
    private void mettreAJourStatistiques() {
        if (tousLesTraitements == null || tousLesTraitements.isEmpty()) {
            reinitialiserAffichage();
            if (lblStatus != null) lblStatus.setText("⚠️ Aucun traitement trouvé");
            return;
        }

        List<Traitement> filtres = filtrerParPeriode(tousLesTraitements);

        if (filtres.isEmpty()) {
            reinitialiserAffichage();
            if (lblStatus != null)
                lblStatus.setText("⚠️ Aucun traitement pour cette période");
            return;
        }

        long total         = filtres.size();
        long enCours       = filtres.stream()
                .filter(t -> "EN_COURS".equals(t.getStatut().name())).count();
        long termine       = filtres.stream()
                .filter(t -> "TERMINE".equals(t.getStatut().name())).count();
        long suspendu      = filtres.stream()
                .filter(t -> "SUSPENDU".equals(t.getStatut().name())).count();
        long prioriteHaute = filtres.stream()
                .filter(t -> "HAUTE".equals(t.getPriorite().name())).count();

        if (statTotal != null)         statTotal.setText(String.valueOf(total));
        if (statEnCours != null)       statEnCours.setText(String.valueOf(enCours));
        if (statTermine != null)       statTermine.setText(String.valueOf(termine));
        if (statSuspendu != null)      statSuspendu.setText(String.valueOf(suspendu));
        if (statPrioriteHaute != null) statPrioriteHaute.setText(String.valueOf(prioriteHaute));

        mettreAJourPieChartStatut(filtres);
        mettreAJourPieChartPriorite(filtres);
        mettreAJourBarChartCategorie(filtres);
        mettreAJourBarChartEvolution(filtres);
        mettreAJourTopEtudiants(filtres);

        // Message statut
        LocalDate now         = LocalDate.now();
        String    periodeVal  = cmbPeriode != null ? cmbPeriode.getValue() : "";
        String    message     = "✓ " + total + " traitement(s)";
        switch (periodeVal != null ? periodeVal : "") {
            case "Cette année":
                int annee = (cmbAnnee != null && cmbAnnee.getValue() != null)
                        ? cmbAnnee.getValue() : now.getYear();
                message += " — " + annee; break;
            case "Ce mois":
                message += " — " + now.getMonth()
                        .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        + " " + now.getYear(); break;
            case "Cette semaine":
                LocalDate lun = now.with(DayOfWeek.MONDAY);
                message += " — semaine du "
                        + lun.format(DateTimeFormatter.ofPattern("dd/MM"))
                        + " au "
                        + lun.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM"));
                break;
            case "Aujourd'hui":
                message += " — "
                        + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); break;
        }
        if (lblStatus != null) lblStatus.setText(message);
    }

    private void reinitialiserAffichage() {
        if (statTotal != null)         statTotal.setText("0");
        if (statEnCours != null)       statEnCours.setText("0");
        if (statTermine != null)       statTermine.setText("0");
        if (statSuspendu != null)      statSuspendu.setText("0");
        if (statPrioriteHaute != null) statPrioriteHaute.setText("0");

        if (pieChartStatut   != null) { pieChartStatut.getData().clear();   pieChartStatut.setTitle("Aucune donnée"); }
        if (pieChartPriorite != null) { pieChartPriorite.getData().clear(); pieChartPriorite.setTitle("Aucune donnée"); }
        if (barChartCategorie != null){ barChartCategorie.getData().clear(); barChartCategorie.setTitle("Aucune donnée"); }
        if (barChartEvolution != null){ barChartEvolution.getData().clear(); barChartEvolution.setTitle("Aucune donnée"); }
        if (tableViewTopEtudiants != null) tableViewTopEtudiants.getItems().clear();
    }

    // ── Filtrage ──────────────────────────────────────────────────────────────
    private List<Traitement> filtrerParPeriode(List<Traitement> traitements) {
        if (cmbPeriode == null) return traitements;
        String    periode = cmbPeriode.getValue();
        if (periode == null) return traitements;
        LocalDate now     = LocalDate.now();

        return traitements.stream().filter(t -> {
            if (t.getDateDebut() == null) return false;
            LocalDate date = t.getDateDebut().toLocalDate();
            switch (periode) {
                case "Cette année":
                    int annee = (cmbAnnee != null && cmbAnnee.getValue() != null)
                            ? cmbAnnee.getValue() : now.getYear();
                    return date.getYear() == annee;
                case "Ce mois":
                    return date.getYear()  == now.getYear()
                            && date.getMonth() == now.getMonth();
                case "Cette semaine":
                    LocalDate lun = now.with(DayOfWeek.MONDAY);
                    return !date.isBefore(lun) && !date.isAfter(lun.plusDays(6));
                case "Aujourd'hui":
                    return date.equals(now);
                default: return true;
            }
        }).collect(Collectors.toList());
    }

    // ── Graphiques ────────────────────────────────────────────────────────────
    private void mettreAJourPieChartStatut(List<Traitement> t) {
        if (pieChartStatut == null) return;
        pieChartStatut.getData().clear();
        long ec = t.stream().filter(x -> "EN_COURS".equals(x.getStatut().name())).count();
        long te = t.stream().filter(x -> "TERMINE".equals(x.getStatut().name())).count();
        long su = t.stream().filter(x -> "SUSPENDU".equals(x.getStatut().name())).count();
        if (ec > 0) pieChartStatut.getData().add(new PieChart.Data("En cours ("  + ec + ")", ec));
        if (te > 0) pieChartStatut.getData().add(new PieChart.Data("Terminés ("  + te + ")", te));
        if (su > 0) pieChartStatut.getData().add(new PieChart.Data("Suspendus (" + su + ")", su));
        pieChartStatut.setTitle(pieChartStatut.getData().isEmpty()
                ? "Aucune donnée" : "Répartition par Statut");
    }

    private void mettreAJourPieChartPriorite(List<Traitement> t) {
        if (pieChartPriorite == null) return;
        pieChartPriorite.getData().clear();
        long ha = t.stream().filter(x -> "HAUTE".equals(x.getPriorite().name())).count();
        long mo = t.stream().filter(x -> "MOYENNE".equals(x.getPriorite().name())).count();
        long ba = t.stream().filter(x -> "BASSE".equals(x.getPriorite().name())).count();
        if (ha > 0) pieChartPriorite.getData().add(new PieChart.Data("Haute ("   + ha + ")", ha));
        if (mo > 0) pieChartPriorite.getData().add(new PieChart.Data("Moyenne (" + mo + ")", mo));
        if (ba > 0) pieChartPriorite.getData().add(new PieChart.Data("Basse ("   + ba + ")", ba));
        pieChartPriorite.setTitle(pieChartPriorite.getData().isEmpty()
                ? "Aucune donnée" : "Répartition par Priorité");
    }

    private void appliquerStyleAxe(CategoryAxis axe, String labelText) {
        axe.setLabel(labelText);
        axe.setTickMarkVisible(true);
        axe.setTickLabelsVisible(true);
        axe.setAnimated(false);
        // ✅ Style inline — ignore complètement le CSS
        axe.setStyle(
                "-fx-tick-label-fill: #1f2937;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-family: 'Segoe UI';"
        );
        // ✅ Forcer la couleur du label titre de l'axe
        axe.lookup(".axis-label");
    }

    private void appliquerStyleAxeY(NumberAxis axe, String labelText,
                                    double min, double max, double tick) {
        axe.setLabel(labelText);
        axe.setTickMarkVisible(true);
        axe.setTickLabelsVisible(true);
        axe.setMinorTickCount(0);
        axe.setAnimated(false);
        axe.setAutoRanging(false);
        axe.setLowerBound(min);
        axe.setUpperBound(max);
        axe.setTickUnit(tick);
        axe.setStyle(
                "-fx-tick-label-fill: #1f2937;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Segoe UI';"
        );
    }

    private BarChart<String, Number> creerBarChart(
            CategoryAxis xAxis, NumberAxis yAxis,
            String titre, List<String> categories,
            List<XYChart.Data<String, Number>> data) {

        // ✅ Définir les catégories AVANT de créer le BarChart
        xAxis.setCategories(FXCollections.observableArrayList(categories));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setBarGap(8);
        chart.setCategoryGap(40);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(320);
        chart.setTitle(titre);
        VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Traitements");
        data.forEach(d -> series.getData().add(d));
        chart.getData().add(series);

        // ✅ Après rendu, forcer couleurs sur tous les éléments texte
        Platform.runLater(() -> {
            // Couleur des tick labels axe X
            xAxis.lookupAll(".axis-tick-mark").forEach(n -> {});
            // Forcer via le nœud Text directement
            xAxis.lookupAll("Text").forEach(node -> {
                node.setStyle("-fx-fill: #1f2937; -fx-font-size: 12px;");
            });
            yAxis.lookupAll("Text").forEach(node -> {
                node.setStyle("-fx-fill: #1f2937; -fx-font-size: 11px;");
            });
            // Couleur titre des axes
            javafx.scene.Node xLabel = xAxis.lookup(".axis-label");
            if (xLabel != null)
                xLabel.setStyle("-fx-fill: #4f46e5; -fx-font-weight: bold; -fx-font-size: 12px;");
            javafx.scene.Node yLabel = yAxis.lookup(".axis-label");
            if (yLabel != null)
                yLabel.setStyle("-fx-fill: #4f46e5; -fx-font-weight: bold; -fx-font-size: 12px;");
            // Couleur des barres
            chart.lookupAll(".chart-bar").forEach(node ->
                    node.setStyle("-fx-bar-fill: #6366f1; -fx-background-radius: 4 4 0 0;"));
            // Fond du graphique
            javafx.scene.Node bg = chart.lookup(".chart-plot-background");
            if (bg != null) bg.setStyle("-fx-background-color: #f8f9ff;");
        });

        return chart;
    }

    private void mettreAJourBarChartCategorie(List<Traitement> traitements) {
        if (vboxBarCategorie == null) return;
        vboxBarCategorie.getChildren().clear();

        Map<String, Long> counts = (traitements == null || traitements.isEmpty())
                ? new HashMap<>()
                : traitements.stream().collect(
                Collectors.groupingBy(t -> t.getCategorie().name(), Collectors.counting()));

        Map<String, String> labelsFr = new LinkedHashMap<>();
        labelsFr.put("COGNITIF",       "Cognitif");
        labelsFr.put("COMPORTEMENTAL", "Comportemental");
        labelsFr.put("EMOTIONNEL",     "Émotionnel");
        labelsFr.put("RELAXATION",     "Relaxation");

        long maxVal = 1L;
        boolean hasData = false;
        List<XYChart.Data<String, Number>> dataList = new java.util.ArrayList<>();

        for (Map.Entry<String, String> e : labelsFr.entrySet()) {
            long c = counts.getOrDefault(e.getKey(), 0L);
            // ✅ Ajouter TOUTES les catégories (même à 0) pour afficher les labels X
            dataList.add(new XYChart.Data<>(e.getValue(), c));
            if (c > 0) { maxVal = Math.max(maxVal, c); hasData = true; }
        }

        List<String> categories = new java.util.ArrayList<>(labelsFr.values());

        CategoryAxis xAxis = new CategoryAxis();
        appliquerStyleAxe(xAxis, "Catégorie de traitement");
        xAxis.setAutoRanging(false);

        NumberAxis yAxis = new NumberAxis();
        appliquerStyleAxeY(yAxis, "Nombre de traitements", 0, maxVal + 2, 1);

        String titre = hasData ? "Traitements par Catégorie"
                : "Aucune donnée pour cette période";

        barChartCategorie = creerBarChart(xAxis, yAxis, titre, categories, dataList);
        xAxisCategorie = xAxis;
        yAxisCategorie = yAxis;
        vboxBarCategorie.getChildren().add(barChartCategorie);
    }

    private void mettreAJourBarChartEvolution(List<Traitement> traitements) {
        if (vboxBarEvolution == null) return;
        vboxBarEvolution.getChildren().clear();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        List<Map.Entry<YearMonth, Long>> entries = new java.util.ArrayList<>();
        if (traitements != null && !traitements.isEmpty()) {
            entries = traitements.stream()
                    .filter(t -> t.getDateDebut() != null)
                    .collect(Collectors.groupingBy(
                            t -> YearMonth.from(t.getDateDebut().toLocalDate()),
                            Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());
        }

        boolean hasData = !entries.isEmpty();
        long maxVal = entries.stream().mapToLong(Map.Entry::getValue).max().orElse(1L);

        List<String> periodes = entries.stream()
                .map(e -> e.getKey().atDay(1).format(fmt))
                .collect(Collectors.toList());

        // Si pas de données, mettre une période placeholder
        if (periodes.isEmpty()) periodes.add("Aucune donnée");

        List<XYChart.Data<String, Number>> dataList = new java.util.ArrayList<>();
        for (Map.Entry<YearMonth, Long> e : entries) {
            dataList.add(new XYChart.Data<>(e.getKey().atDay(1).format(fmt), e.getValue()));
        }

        CategoryAxis xAxis = new CategoryAxis();
        appliquerStyleAxe(xAxis, "Période");
        xAxis.setTickLabelRotation(30);
        xAxis.setAutoRanging(false);

        NumberAxis yAxis = new NumberAxis();
        appliquerStyleAxeY(yAxis, "Nombre de traitements", 0, maxVal + 2, 1);

        String titre = hasData ? "Évolution mensuelle des traitements"
                : "Aucune donnée pour cette période";

        barChartEvolution = creerBarChart(xAxis, yAxis, titre, periodes, dataList);
        xAxisEvolution = xAxis;
        yAxisEvolution = yAxis;
        vboxBarEvolution.getChildren().add(barChartEvolution);
    }

    private void mettreAJourTopEtudiants(List<Traitement> traitements) {
        if (tableViewTopEtudiants == null || tousLesEtudiants == null) return;
        Map<Integer, EtudiantStat> map = new HashMap<>();
        for (Etudiant e : tousLesEtudiants)
            map.put(e.getUserId(), new EtudiantStat(e.getPrenom() + " " + e.getNom()));

        for (Traitement t : traitements) {
            EtudiantStat s = map.get(t.getEtudiantId());
            if (s != null) {
                s.incrementTotal();
                switch (t.getStatut().name()) {
                    case "EN_COURS":  s.incrementEnCours();  break;
                    case "TERMINE":   s.incrementTermine();   break;
                    case "SUSPENDU":  s.incrementSuspendu();  break;
                }
            }
        }
        tableViewTopEtudiants.setItems(FXCollections.observableArrayList(
                map.values().stream()
                        .filter(s -> s.getTotal() > 0)
                        .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
                        .limit(5)
                        .collect(Collectors.toList())));
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML
    private void handleActualiser() {
        if (utilisateur != null) { chargerDonnees(); return; }
        SessionManager s = SessionManager.getInstance();
        if (s.estConnecte()) { this.utilisateur = s.getCurrentUser(); chargerDonnees(); }
        else if (lblStatus != null) lblStatus.setText("✗ Utilisateur non connecté");
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/traitement-view.fxml"));
            Parent root = loader.load();
            TraitementController ctrl = loader.getController();
            if (ctrl != null && utilisateur != null) ctrl.setUtilisateur(utilisateur);
            Scene scene = lblDate != null && lblDate.getScene() != null
                    ? lblDate.getScene()
                    : (lblStatus != null ? lblStatus.getScene() : null);
            if (scene != null) scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Erreur lors du retour : " + e.getMessage());
            alert.showAndWait();
        }
    }
}