package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.entities.Reponsequestionnaire;
import org.example.services.QuestionServices;
import org.example.services.QuestionnaireServices;
import org.example.services.ReponseQuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StatistiquesController implements Initializable {

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            List<Questionnaire>        questionnaires = qService.afficher();
            List<Question>             questions      = qqService.afficher();
            List<Reponsequestionnaire> reponses       = rService.afficher();

            chargerCartes(questionnaires, questions, reponses);
            chargerPieType(questionnaires);
            chargerBarNiveau(reponses);
            chargerBarTopQuestionnaires(reponses, questionnaires);

        } catch (SQLException e) {
            System.out.println("Erreur statistiques: " + e.getMessage());
        }
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
        series.setName("Reponses");

        List<String> niveaux = List.of("legere", "modere", "severe");
        for (String niveau : niveaux) {
            long count = countByNiveau.getOrDefault(niveau, 0L);
            series.getData().add(new XYChart.Data<>(niveau.toUpperCase(), count));
        }

        barNiveau.getData().clear();
        barNiveau.getData().add(series);
        barNiveau.setLegendVisible(false);

        // Couleurs après rendu
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
        series.setName("Reponses");

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