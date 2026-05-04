package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.example.entities.*;
import org.example.services.*;
import org.example.enums.StatutEvenement;
import org.example.enums.StatutParticipation;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatsController implements Initializable {

    // ── KPI Meditation ──
    @FXML private Label lblTotalCategories;
    @FXML private Label lblTotalSeances;
    @FXML private Label lblSeancesActives;
    @FXML private Label lblSeancesVideo;

    // ── Charts Meditation ──
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private PieChart pieChart;

    // ── KPI Evenements ──
    @FXML private Label lblTotalEvenements;
    @FXML private Label lblEvenementsAVenir;
    @FXML private Label lblTotalParticipations;
    @FXML private Label lblParticipationsConfirmees;

    // ── Charts Evenements ──
    @FXML private BarChart<String, Number> barChartEvenements;
    @FXML private CategoryAxis xAxisEvenements;
    @FXML private PieChart pieChartParticipations;

    // ── KPI Questionnaires ──
    @FXML private Label lblTotalQuestionnaires;
    @FXML private Label lblTotalQuestions;
    @FXML private Label lblTotalReponses;
    @FXML private Label lblBesoinPsy;

    // ── Charts Questionnaires ──
    @FXML private PieChart pieChartQuestionnaires;
    @FXML private BarChart<String, Number> barChartNiveaux;
    @FXML private CategoryAxis xAxisNiveaux;
    @FXML private BarChart<String, Number> barChartTopQuestionnaires;
    @FXML private CategoryAxis xAxisTopQuestionnaires;

    // ── Filtre mois ──
    @FXML private ComboBox<String> cbMois;
    @FXML private Label lblPeriode;

    private static final String[] MOIS_FR = {
            "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre"
    };

    private final SeanceMeditationServices    seanceService       = new SeanceMeditationServices();
    private final CategorieMeditationServices categorieService    = new CategorieMeditationServices();
    private final EvenementService            evenementService    = new EvenementService();
    private final ParticipationService        participationService = new ParticipationService();
    private final QuestionnaireServices       qService            = new QuestionnaireServices();
    private final QuestionServices            qqService           = new QuestionServices();
    private final ReponseQuestionnaireServices rService           = new ReponseQuestionnaireServices();

    private List<Questionnaire>        tousQuestionnaires;
    private List<Question>             toutesQuestions;
    private List<Reponsequestionnaire> toutesReponses;
    private final int anneeSelectionnee = LocalDate.now().getYear();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // ── Meditation ──
            List<SeanceMeditation>    seances    = seanceService.afficher();
            List<CategorieMeditation> categories = categorieService.afficher();
            loadKPIs(seances, categories);
            loadLineChart(seances);
            loadPieChart(seances, categories);

            // ── Evenements ──
            List<Evenement>     evenements     = evenementService.afficher();
            List<Participation> participations = participationService.afficher();
            loadEventKPIs(evenements, participations);
            loadEventBarChart(evenements);
            loadParticipationPieChart(participations);

            // ── Questionnaires ──
            tousQuestionnaires = qService.afficher();
            toutesQuestions    = qqService.afficher();
            toutesReponses     = rService.afficher();
            initFiltresMois();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTRE MOIS QUESTIONNAIRES
    // ════════════════════════════════════════════════════════════════

    private void initFiltresMois() {
        if (cbMois == null) {
            rafraichirQuestionnaires("Tous les mois");
            return;
        }
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous les mois");
        for (String mois : MOIS_FR)
            items.add(mois + " " + anneeSelectionnee);

        cbMois.setItems(items);
        int moisCourant = LocalDate.now().getMonthValue();
        cbMois.setValue(MOIS_FR[moisCourant - 1] + " " + anneeSelectionnee);
        cbMois.valueProperty().addListener((obs, old, val) -> {
            if (val != null) rafraichirQuestionnaires(val);
        });
        rafraichirQuestionnaires(cbMois.getValue());
    }

    private void rafraichirQuestionnaires(String moisStr) {
        List<Reponsequestionnaire> filtrees;
        if ("Tous les mois".equals(moisStr)) {
            filtrees = toutesReponses;
            if (lblPeriode != null) lblPeriode.setText("Periode : Toute la duree");
        } else {
            int mois = getMoisNumero(moisStr);
            filtrees = toutesReponses.stream()
                    .filter(r -> r.getCreatedAt() != null)
                    .filter(r -> {
                        LocalDate d = r.getCreatedAt().toLocalDateTime().toLocalDate();
                        return d.getMonthValue() == mois && d.getYear() == anneeSelectionnee;
                    }).collect(Collectors.toList());
            if (lblPeriode != null) lblPeriode.setText("Periode : " + moisStr);
        }
        chargerKPIQuestionnaires(filtrees);
        chargerPieQuestionnaires(tousQuestionnaires);
        chargerBarNiveaux(filtrees);
        chargerBarTopQuestionnaires(filtrees);
    }

    private int getMoisNumero(String moisStr) {
        for (int i = 0; i < MOIS_FR.length; i++)
            if (moisStr.startsWith(MOIS_FR[i])) return i + 1;
        return LocalDate.now().getMonthValue();
    }

    // ════════════════════════════════════════════════════════════════
    //  KPI QUESTIONNAIRES
    // ════════════════════════════════════════════════════════════════

    private void chargerKPIQuestionnaires(List<Reponsequestionnaire> reponses) {
        if (lblTotalQuestionnaires != null)
            lblTotalQuestionnaires.setText(String.valueOf(tousQuestionnaires.size()));
        if (lblTotalQuestions != null)
            lblTotalQuestions.setText(String.valueOf(toutesQuestions.size()));
        if (lblTotalReponses != null)
            lblTotalReponses.setText(String.valueOf(reponses.size()));
        if (lblBesoinPsy != null) {
            long psy = reponses.stream().filter(Reponsequestionnaire::isaBesoinPsy).count();
            lblBesoinPsy.setText(String.valueOf(psy));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PIE CHART QUESTIONNAIRES PAR TYPE
    // ════════════════════════════════════════════════════════════════

    private void chargerPieQuestionnaires(List<Questionnaire> questionnaires) {
        if (pieChartQuestionnaires == null) return;
        Map<String, Long> countByType = questionnaires.stream()
                .filter(q -> q.getType() != null)
                .collect(Collectors.groupingBy(q -> q.getType().name(), Collectors.counting()));

        String[] colors = {"#6366f1","#10b981","#f59e0b","#ef4444","#3b82f6","#8b5cf6","#ec4899","#14b8a6"};
        List<PieChart.Data> dataList = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Long> e : countByType.entrySet()) {
            PieChart.Data slice = new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue());
            dataList.add(slice);
            final String color = colors[i % colors.length];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            i++;
        }
        if (dataList.isEmpty()) dataList.add(new PieChart.Data("Aucun", 1));
        pieChartQuestionnaires.setData(FXCollections.observableArrayList(dataList));
        pieChartQuestionnaires.setLabelsVisible(true);
        pieChartQuestionnaires.setLegendVisible(true);
    }

    // ════════════════════════════════════════════════════════════════
    //  BAR CHART NIVEAUX
    // ════════════════════════════════════════════════════════════════

    private void chargerBarNiveaux(List<Reponsequestionnaire> reponses) {
        if (barChartNiveaux == null) return;
        Map<String, Long> countByNiveau = reponses.stream()
                .filter(r -> r.getNiveau() != null)
                .collect(Collectors.groupingBy(Reponsequestionnaire::getNiveau, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Reponses");

        String[] niveaux       = {"legere",  "modere",  "severe"};
        String[] niveauxLabels = {"LEGERE",  "MODEREE", "SEVERE"};
        String[] niveauxColors = {"#22c55e", "#f59e0b", "#ef4444"};

        for (int i = 0; i < niveaux.length; i++) {
            long count = countByNiveau.getOrDefault(niveaux[i], 0L);
            series.getData().add(new XYChart.Data<>(niveauxLabels[i], count));
        }

        barChartNiveaux.getData().clear();
        barChartNiveaux.getData().add(series);
        barChartNiveaux.setLegendVisible(false);

        final String[] colors = niveauxColors;
        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                var node = series.getData().get(i).getNode();
                if (node != null) node.setStyle("-fx-bar-fill: " + colors[i] + ";");
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  BAR CHART TOP QUESTIONNAIRES
    // ════════════════════════════════════════════════════════════════

    private void chargerBarTopQuestionnaires(List<Reponsequestionnaire> reponses) {
        if (barChartTopQuestionnaires == null) return;
        Map<Integer, String> idToNom = tousQuestionnaires.stream()
                .collect(Collectors.toMap(
                        Questionnaire::getQuestionnaireId,
                        Questionnaire::getNom,
                        (a, b) -> a));

        Map<Integer, Long> countByQ = reponses.stream()
                .collect(Collectors.groupingBy(
                        Reponsequestionnaire::getQuestionnaireId,
                        Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Reponses");

        countByQ.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    String nom = idToNom.getOrDefault(entry.getKey(), "Q#" + entry.getKey());
                    if (nom.length() > 18) nom = nom.substring(0, 18) + "...";
                    series.getData().add(new XYChart.Data<>(nom, entry.getValue()));
                });

        barChartTopQuestionnaires.getData().clear();
        barChartTopQuestionnaires.getData().add(series);
        barChartTopQuestionnaires.setLegendVisible(false);

        Platform.runLater(() -> series.getData().forEach(d -> {
            if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #7c3aed;");
        }));
    }

    // ════════════════════════════════════════════════════════════════
    //  MEDITATION (code original)
    // ════════════════════════════════════════════════════════════════

    private void loadKPIs(List<SeanceMeditation> seances, List<CategorieMeditation> categories) {
        lblTotalCategories.setText(String.valueOf(categories.size()));
        lblTotalSeances.setText(String.valueOf(seances.size()));
        long actives = seances.stream().filter(SeanceMeditation::isIsActive).count();
        lblSeancesActives.setText(String.valueOf(actives));
        long videos = seances.stream()
                .filter(s -> s.getTypeFichier() != null && s.getTypeFichier().name().equals("video"))
                .count();
        lblSeancesVideo.setText(String.valueOf(videos));
    }

    private void loadLineChart(List<SeanceMeditation> seances) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
        List<String> last12Months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--)
            last12Months.add(now.minusMonths(i).format(formatter));

        Map<String, Long> countByMonth = seances.stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getCreatedAt().toLocalDateTime().toLocalDate().format(formatter),
                        Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Seances creees");
        for (String month : last12Months)
            series.getData().add(new XYChart.Data<>(month, countByMonth.getOrDefault(month, 0L)));

        xAxis.setCategories(FXCollections.observableArrayList(last12Months));
        lineChart.getData().add(series);
        lineChart.setCreateSymbols(true);
        Platform.runLater(this::styleLineChart);
    }

    private void styleLineChart() {
        lineChart.lookupAll(".chart-series-line").forEach(n ->
                n.setStyle("-fx-stroke: #6366f1; -fx-stroke-width: 2.5px;"));
        lineChart.lookupAll(".chart-line-symbol").forEach(n ->
                n.setStyle("-fx-background-color: #6366f1, white; -fx-background-radius: 5px; -fx-padding: 4px;"));
    }

    private void loadPieChart(List<SeanceMeditation> seances, List<CategorieMeditation> categories) {
        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(CategorieMeditation::getCategorieId, CategorieMeditation::getNom));
        Map<Integer, Long> countByCat = seances.stream()
                .collect(Collectors.groupingBy(SeanceMeditation::getCategorieId, Collectors.counting()));

        String[] colors = {"#6366f1","#8b5cf6","#ec4899","#f59e0b","#10b981","#3b82f6","#ef4444","#14b8a6"};
        List<PieChart.Data> pieData = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<Integer, Long> entry : countByCat.entrySet()) {
            String catName = catNames.getOrDefault(entry.getKey(), "Categorie " + entry.getKey());
            PieChart.Data slice = new PieChart.Data(catName + " (" + entry.getValue() + ")", entry.getValue());
            pieData.add(slice);
            final String color = colors[colorIdx % colors.length];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            colorIdx++;
        }
        if (pieData.isEmpty()) pieData.add(new PieChart.Data("Aucune seance", 1));
        pieChart.setData(FXCollections.observableArrayList(pieData));
        pieChart.getData().forEach(data ->
                Tooltip.install(data.getNode(),
                        new Tooltip(data.getName() + "\n" + (int) data.getPieValue() + " seance(s)")));
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENEMENTS (code original)
    // ════════════════════════════════════════════════════════════════

    private void loadEventKPIs(List<Evenement> evenements, List<Participation> participations) {
        lblTotalEvenements.setText(String.valueOf(evenements.size()));
        LocalDate today = LocalDate.now();
        long aVenir = evenements.stream()
                .filter(e -> e.getStatut() == StatutEvenement.A_VENIR || e.getStatut() == StatutEvenement.EN_COURS)
                .filter(e -> e.getDateDebut() != null &&
                        e.getDateDebut().toLocalDateTime().toLocalDate().isAfter(today.minusDays(1)))
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
        series.setName("Evenements");
        for (Map.Entry<String, Long> entry : countByType.entrySet())
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));

        xAxisEvenements.setCategories(FXCollections.observableArrayList(countByType.keySet()));
        barChartEvenements.getData().add(series);
        barChartEvenements.setAnimated(true);
        Platform.runLater(this::styleBarChart);
    }

    private void styleBarChart() {
        barChartEvenements.lookupAll(".chart-bar")
                .forEach(n -> n.setStyle("-fx-bar-fill: #6366f1;"));
    }

    private void loadParticipationPieChart(List<Participation> participations) {
        Map<StatutParticipation, Long> countByStatut = participations.stream()
                .collect(Collectors.groupingBy(Participation::getStatut, Collectors.counting()));

        String[] colors = {"#10b981", "#f59e0b", "#ef4444"};
        List<PieChart.Data> pieData = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<StatutParticipation, Long> entry : countByStatut.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey().getDbValue() + " (" + entry.getValue() + ")", entry.getValue());
            pieData.add(slice);
            final String color = colors[Math.min(colorIdx, colors.length - 1)];
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
            });
            colorIdx++;
        }
        if (pieData.isEmpty()) pieData.add(new PieChart.Data("Aucune participation", 1));
        pieChartParticipations.setData(FXCollections.observableArrayList(pieData));
        pieChartParticipations.getData().forEach(data ->
                Tooltip.install(data.getNode(),
                        new Tooltip(data.getName() + "\n" + (int) data.getPieValue() + " participation(s)")));
    }
}