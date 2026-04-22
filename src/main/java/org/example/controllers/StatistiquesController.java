package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.entities.Reponsequestionnaire;
import org.example.services.QuestionServices;
import org.example.services.QuestionnaireServices;
import org.example.services.ReponseQuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class StatistiquesController implements Initializable {

    // ─── FILTRE MOIS ───
    @FXML private ComboBox<String> cbMois;
    @FXML private Label lblPeriode;

    // ─── CARTES STAT ───
    @FXML private Label lblTotalQuestionnaires;
    @FXML private Label lblTotalQuestions;
    @FXML private Label lblTotalReponses;
    @FXML private Label lblBesoinPsy;

    // ─── GRAPHIQUES ───
    @FXML private PieChart pieType;
    @FXML private BarChart<String, Number> barNiveau;
    @FXML private BarChart<String, Number> barTopQuestionnaires;

    private final QuestionnaireServices        qService  = new QuestionnaireServices();
    private final QuestionServices             qqService = new QuestionServices();
    private final ReponseQuestionnaireServices rService  = new ReponseQuestionnaireServices();

    private List<Questionnaire>        tousQuestionnaires;
    private List<Question>             toutesQuestions;
    private List<Reponsequestionnaire> toutesReponses;

    // Mois en cours
    private int moisSelectionne  = LocalDate.now().getMonthValue();
    private int anneeSelectionnee = LocalDate.now().getYear();

    // Noms des mois en français
    private static final String[] MOIS_FR = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            tousQuestionnaires = qService.afficher();
            toutesQuestions    = qqService.afficher();
            toutesReponses     = rService.afficher();

            // ─── Remplir ComboBox mois ───
            ObservableList<String> moisItems = FXCollections.observableArrayList();
            moisItems.add("Tous les mois");
            for (int i = 0; i < 12; i++) {
                moisItems.add(MOIS_FR[i] + " " + anneeSelectionnee);
            }
            cbMois.setItems(moisItems);

            // Sélectionner le mois en cours
            cbMois.setValue(MOIS_FR[moisSelectionne - 1] + " " + anneeSelectionnee);

            // ─── Listener changement mois ───
            cbMois.valueProperty().addListener((obs, old, val) -> {
                if (val != null) rafraichir(val);
            });

            // ─── Charger les stats du mois en cours ───
            rafraichir(cbMois.getValue());

        } catch (SQLException e) {
            System.out.println("Erreur statistiques: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    //  RAFRAICHIR SELON MOIS SELECTIONNE
    // ══════════════════════════════════════════
    private void rafraichir(String moisStr) {

        List<Reponsequestionnaire> reponsesFiltrees;

        if (moisStr.equals("Tous les mois")) {
            reponsesFiltrees = toutesReponses;
            lblPeriode.setText("📅 Période : Toute la durée");
        } else {
            // Extraire le numéro du mois depuis le nom
            int mois = getMoisNumero(moisStr);
            int annee = anneeSelectionnee;

            // Filtrer les réponses par mois et année
            reponsesFiltrees = toutesReponses.stream()
                    .filter(r -> r.getCreatedAt() != null)
                    .filter(r -> {
                        LocalDate date = r.getCreatedAt().toLocalDateTime().toLocalDate();
                        return date.getMonthValue() == mois && date.getYear() == annee;
                    })
                    .collect(Collectors.toList());

            lblPeriode.setText("📅 Période : " + moisStr);
        }

        chargerCartes(tousQuestionnaires, toutesQuestions, reponsesFiltrees);
        chargerPieType(tousQuestionnaires);
        chargerBarNiveau(reponsesFiltrees);
        chargerBarTopQuestionnaires(reponsesFiltrees, tousQuestionnaires);
    }

    // ══════════════════════════════════════════
    //  HELPER — nom mois → numéro
    // ══════════════════════════════════════════
    private int getMoisNumero(String moisStr) {
        for (int i = 0; i < MOIS_FR.length; i++) {
            if (moisStr.startsWith(MOIS_FR[i])) return i + 1;
        }
        return LocalDate.now().getMonthValue();
    }

    // ══════════════════════════════════════════
    //  CARTES STAT
    // ══════════════════════════════════════════
    private void chargerCartes(List<Questionnaire> questionnaires,
                               List<Question> questions,
                               List<Reponsequestionnaire> reponses) {
        lblTotalQuestionnaires.setText(String.valueOf(questionnaires.size()));
        lblTotalQuestions.setText(String.valueOf(questions.size()));
        lblTotalReponses.setText(String.valueOf(reponses.size()));
        long besoinPsy = reponses.stream().filter(Reponsequestionnaire::isaBesoinPsy).count();
        lblBesoinPsy.setText(String.valueOf(besoinPsy));
    }

    // ══════════════════════════════════════════
    //  CAMEMBERT — RÉPARTITION PAR TYPE
    // ══════════════════════════════════════════
    private void chargerPieType(List<Questionnaire> questionnaires) {
        Map<String, Long> countByType = questionnaires.stream()
                .filter(q -> q.getType() != null)
                .collect(Collectors.groupingBy(q -> q.getType().name(), Collectors.counting()));

        List<PieChart.Data> pieData = new ArrayList<>();
        countByType.forEach((type, count) ->
                pieData.add(new PieChart.Data(type + " (" + count + ")", count)));

        pieType.setData(FXCollections.observableArrayList(pieData));
        pieType.setLegendVisible(true);
        pieType.setLabelsVisible(true);
    }

    // ══════════════════════════════════════════
    //  BARRES — RÉPONSES PAR NIVEAU
    // ══════════════════════════════════════════
    private void chargerBarNiveau(List<Reponsequestionnaire> reponses) {
        Map<String, Long> countByNiveau = reponses.stream()
                .filter(r -> r.getNiveau() != null)
                .collect(Collectors.groupingBy(Reponsequestionnaire::getNiveau, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Réponses");

        List<String> niveaux = List.of("legere", "modere", "severe");
        for (String niveau : niveaux) {
            long count = countByNiveau.getOrDefault(niveau, 0L);
            series.getData().add(new XYChart.Data<>(niveau.toUpperCase(), count));
        }

        barNiveau.getData().clear();
        barNiveau.getData().add(series);
        barNiveau.setLegendVisible(false);

        barNiveau.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                barNiveau.lookupAll(".data0.chart-bar")
                        .forEach(node -> node.setStyle("-fx-bar-fill: #22c55e;"));
                barNiveau.lookupAll(".data1.chart-bar")
                        .forEach(node -> node.setStyle("-fx-bar-fill: #f59e0b;"));
                barNiveau.lookupAll(".data2.chart-bar")
                        .forEach(node -> node.setStyle("-fx-bar-fill: #ef4444;"));
            }
        });
    }

    // ══════════════════════════════════════════
    //  BARRES — TOP QUESTIONNAIRES
    // ══════════════════════════════════════════
    private void chargerBarTopQuestionnaires(List<Reponsequestionnaire> reponses,
                                             List<Questionnaire> questionnaires) {
        Map<Integer, String> idToNom = questionnaires.stream()
                .collect(Collectors.toMap(
                        Questionnaire::getQuestionnaireId,
                        Questionnaire::getNom));

        Map<Integer, Long> countByQ = reponses.stream()
                .collect(Collectors.groupingBy(
                        Reponsequestionnaire::getQuestionnaireId,
                        Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Réponses");

        countByQ.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    String nom = idToNom.getOrDefault(entry.getKey(), "Q#" + entry.getKey());
                    if (nom.length() > 20) nom = nom.substring(0, 20) + "...";
                    series.getData().add(new XYChart.Data<>(nom, entry.getValue()));
                });

        barTopQuestionnaires.getData().clear();
        barTopQuestionnaires.getData().add(series);
        barTopQuestionnaires.setLegendVisible(false);
    }
}