package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.entities.Reponsequestionnaire;
import org.example.services.QuestionServices;
import org.example.services.ReponseQuestionnaireServices;
import org.example.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class EtudiantRepondreController implements Initializable {

    @FXML private Label lblTitreQuestionnaire;
    @FXML private VBox questionsContainer;
    @FXML private Label lblStatus;

    private Questionnaire questionnaire;
    private List<Question> questions;
    private final Map<Integer, String> reponsesChoisies = new HashMap<>();
    private final Map<Integer, Double> scoresChoisis    = new HashMap<>();

    private final QuestionServices questionService        = new QuestionServices();
    private final ReponseQuestionnaireServices repService = new ReponseQuestionnaireServices();

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setQuestionnaire(Questionnaire q) {
        this.questionnaire = q;
        lblTitreQuestionnaire.setText("📋 " + q.getNom());
        chargerQuestions();
    }

    private void chargerQuestions() {
        try {
            questions = questionService.afficherParQuestionnaire(
                    questionnaire.getQuestionnaireId());
            questionsContainer.getChildren().clear();

            if (questions.isEmpty()) {
                Label lblVide = new Label(
                        "⚠️ Aucune question trouvée pour ce questionnaire.");
                lblVide.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px;");
                questionsContainer.getChildren().add(lblVide);
                return;
            }

            for (int i = 0; i < questions.size(); i++) {
                questionsContainer.getChildren().add(
                        buildQuestionBox(i + 1, questions.get(i)));
            }

        } catch (SQLException e) {
            setStatus("❌ Erreur chargement questions: " + e.getMessage(), false);
        }
    }

    private VBox buildQuestionBox(int numero, Question question) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #3b1f6e; -fx-background-radius: 10; " +
                "-fx-border-color: #6d28d9; -fx-border-radius: 10; " +
                "-fx-border-width: 1; -fx-padding: 14;");

        Label lblQuestion = new Label(numero + ". " + question.getTexte());
        lblQuestion.setWrapText(true);
        lblQuestion.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(lblQuestion);

        if (question.getOptionsQuest() != null && !question.getOptionsQuest().isEmpty()) {
            ToggleGroup group = new ToggleGroup();
            String[] options = question.getOptionsQuest()
                    .replaceAll("[\\[\\]\"]", "").split(",");
            String[] scores = question.getScoreOptions() != null
                    ? question.getScoreOptions().replaceAll("[\\[\\]\"\\s]", "").split(",")
                    : new String[0];

            for (int i = 0; i < options.length; i++) {
                String option = options[i].trim();
                final int idx = i;

                RadioButton rb = new RadioButton(option);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-text-fill: #c084fc; -fx-font-size: 13px;");

                rb.setOnAction(e -> {
                    reponsesChoisies.put(question.getQuestionId(), option);
                    if (idx < scores.length) {
                        try {
                            scoresChoisis.put(question.getQuestionId(),
                                    Double.parseDouble(scores[idx].trim()));
                        } catch (NumberFormatException ex) {
                            scoresChoisis.put(question.getQuestionId(), 0.0);
                        }
                    }
                });
                box.getChildren().add(rb);
            }
        } else {
            TextField tfReponse = new TextField();
            tfReponse.setPromptText("Votre réponse...");
            tfReponse.setStyle("-fx-background-color: #1a0a2e; -fx-text-fill: #e2e8f0; " +
                    "-fx-border-color: #6d28d9; -fx-border-radius: 6; " +
                    "-fx-background-radius: 6; -fx-padding: 7;");
            tfReponse.textProperty().addListener((obs, old, val) ->
                    reponsesChoisies.put(question.getQuestionId(), val));
            box.getChildren().add(tfReponse);
        }

        return box;
    }

    @FXML
    public void handleSoumettre() {
        if (questions == null || questions.isEmpty()) {
            setStatus("❌ Aucune question à répondre !", false);
            return;
        }

        for (Question q : questions) {
            if (!reponsesChoisies.containsKey(q.getQuestionId())) {
                setStatus("⚠️ Veuillez répondre à toutes les questions !", false);
                return;
            }
        }

        double scoreTotale = scoresChoisis.values().stream()
                .mapToDouble(Double::doubleValue).sum();

        StringBuilder jsonReponses = new StringBuilder("{");
        for (Question q : questions) {
            jsonReponses.append("\"q").append(q.getQuestionId())
                    .append("\":\"").append(reponsesChoisies.get(q.getQuestionId()))
                    .append("\",");
        }
        if (jsonReponses.length() > 1)
            jsonReponses.deleteCharAt(jsonReponses.length() - 1);
        jsonReponses.append("}");

        String niveau         = questionnaire.getNiveauScore((int) scoreTotale);
        String interpretation = questionnaire.interpreterScore((int) scoreTotale);

        // ✅ Récupérer le userId depuis SessionManager
        int userId = SessionManager.getInstance().getUserId();

        Reponsequestionnaire reponse = new Reponsequestionnaire(
                scoreTotale,
                jsonReponses.toString(),
                interpretation,
                null,
                niveau,
                scoreTotale >= questionnaire.getSeuilSevere(),
                null,
                questionnaire.getQuestionnaireId(),
                userId  // ✅ userId au lieu de null
        );

        try {
            repService.ajouter(reponse);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("✅ Résultat du Questionnaire");
            alert.setHeaderText("Questionnaire : " + questionnaire.getNom());
            alert.setContentText(
                    "🎯 Score Total : " + scoreTotale + "\n" +
                            "📊 Niveau      : " + niveau.toUpperCase() + "\n\n" +
                            "📝 Interprétation :\n" + interpretation + "\n\n" +
                            "Vous allez être redirigé vers vos réponses."
            );
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/EtudiantMesReponsesView.fxml"));
            Pane view = loader.load();
            StackPane contentArea = (StackPane) questionsContainer
                    .getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().equals("LIMITE_QUOTIDIENNE")) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Limite atteinte");
                alert.setHeaderText("🚫 Limite quotidienne atteinte");
                alert.setContentText("Vous avez déjà passé 2 questionnaires aujourd'hui.\nRevenez demain !");
                alert.showAndWait();
            } else {
                setStatus("❌ Erreur soumission : " + e.getMessage(), false);
            }
        }
    }

    @FXML
    public void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/EtudiantQuestionnairesView.fxml"));
            Pane view = loader.load();
            StackPane contentArea = (StackPane) questionsContainer
                    .getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.out.println("Erreur retour : " + e.getMessage());
        }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}