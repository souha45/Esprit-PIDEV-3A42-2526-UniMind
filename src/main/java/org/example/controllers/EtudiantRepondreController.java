package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.entities.Reponsequestionnaire;
import org.example.services.EmailServiceQuestionnaire;
import org.example.services.OpenAIService;
import org.example.services.QuestionServices;
import org.example.services.ReponseQuestionnaireServices;
import org.example.services.TraductionService;
import org.example.utils.LimiteQuestionnaire;
import org.example.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class EtudiantRepondreController implements Initializable {

    @FXML private Label  lblTitreQuestionnaire;
    @FXML private VBox   questionsContainer;
    @FXML private Label  lblStatus;
    @FXML private Button btnFR;
    @FXML private Button btnEN;
    @FXML private Button btnAR;

    // ✅ contentArea pour navigation
    private StackPane contentArea;

    private Questionnaire questionnaire;
    private List<Question> questions;
    private List<Question> questionsOriginales = new ArrayList<>();

    private final Map<Integer, String> reponsesChoisies = new HashMap<>();
    private final Map<Integer, Double> scoresChoisis    = new HashMap<>();

    private final QuestionServices             questionService = new QuestionServices();
    private final ReponseQuestionnaireServices repService      = new ReponseQuestionnaireServices();

    private static final String LANG_ACTIVE =
            "-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-padding: 6 12; -fx-background-radius: 20; -fx-cursor: hand;";

    private static final String LANG_IDLE =
            "-fx-background-color: #f5f3ff; -fx-text-fill: #7c3aed; " +
                    "-fx-font-size: 12px; -fx-padding: 6 12; " +
                    "-fx-background-radius: 20; -fx-cursor: hand;";

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (btnFR != null) btnFR.setOnAction(e -> handleLangFR());
        if (btnEN != null) btnEN.setOnAction(e -> handleLangEN());
        if (btnAR != null) btnAR.setOnAction(e -> handleLangAR());
    }

    public void setQuestionnaire(Questionnaire q) {
        this.questionnaire = q;
        lblTitreQuestionnaire.setText("📋 " + q.getNom());
        chargerQuestions();
    }

    private boolean isArabic(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray())
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC) return true;
        return false;
    }

    private String[] splitOptions(String raw) {
        String cleaned = raw.replaceAll("[\\[\\]\"]", "").trim();
        return cleaned.contains("|") ? cleaned.split("\\|") : cleaned.split(",");
    }

    private String[] splitScores(String raw) {
        String cleaned = raw.replaceAll("[\\[\\]\"\\s]", "").trim();
        return cleaned.contains("|") ? cleaned.split("\\|") : cleaned.split(",");
    }

    private void chargerQuestions() {
        try {
            questions = questionService.afficherParQuestionnaire(
                    questionnaire.getQuestionnaireId());
            questionsOriginales = new ArrayList<>(questions);
            questionsContainer.getChildren().clear();

            if (questions.isEmpty()) {
                Label lblVide = new Label("Aucune question trouvee pour ce questionnaire.");
                lblVide.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px;");
                questionsContainer.getChildren().add(lblVide);
                return;
            }

            for (int i = 0; i < questions.size(); i++)
                questionsContainer.getChildren().add(buildQuestionBox(i + 1, questions.get(i)));

            if (btnFR != null) setLangActive(btnFR);

        } catch (SQLException e) {
            setStatus("Erreur chargement questions: " + e.getMessage(), false);
        }
    }

    @FXML
    public void handleLangFR() {
        setLangActive(btnFR);
        questions = new ArrayList<>(questionsOriginales);
        afficherQuestions();
        setStatus("Francais", true);
    }

    @FXML
    public void handleLangEN() {
        setLangActive(btnEN);
        setStatus("Traduction en anglais...", true);
        new Thread(() -> {
            List<Question> traduits = new ArrayList<>();
            for (Question q : questionsOriginales) {
                Question copie = copierQuestion(q);
                try {
                    copie.setTexte(TraductionService.versAnglais(q.getTexte()));
                    if (q.getOptionsQuest() != null && !q.getOptionsQuest().isBlank()) {
                        String[] opts = splitOptions(q.getOptionsQuest());
                        StringBuilder sb = new StringBuilder();
                        for (String opt : opts)
                            sb.append(TraductionService.versAnglais(opt.trim())).append("|");
                        copie.setOptionsQuest(sb.toString().replaceAll("\\|$", ""));
                    }
                } catch (Exception e) { copie = copierQuestion(q); }
                traduits.add(copie);
            }
            javafx.application.Platform.runLater(() -> {
                questions = traduits;
                afficherQuestions();
                setStatus("Traduit en Anglais", true);
            });
        }).start();
    }

    @FXML
    public void handleLangAR() {
        setLangActive(btnAR);
        setStatus("Traduction en arabe...", true);
        new Thread(() -> {
            List<Question> traduits = new ArrayList<>();
            for (Question q : questionsOriginales) {
                Question copie = copierQuestion(q);
                try {
                    copie.setTexte(TraductionService.versArabe(q.getTexte()));
                    if (q.getOptionsQuest() != null && !q.getOptionsQuest().isBlank()) {
                        String[] opts = splitOptions(q.getOptionsQuest());
                        StringBuilder sb = new StringBuilder();
                        for (String opt : opts)
                            sb.append(TraductionService.versArabe(opt.trim())).append("|");
                        copie.setOptionsQuest(sb.toString().replaceAll("\\|$", ""));
                    }
                } catch (Exception e) { copie = copierQuestion(q); }
                traduits.add(copie);
            }
            javafx.application.Platform.runLater(() -> {
                questions = traduits;
                afficherQuestions();
                setStatus("Traduit en Arabe", true);
            });
        }).start();
    }

    private void afficherQuestions() {
        questionsContainer.getChildren().clear();
        reponsesChoisies.clear();
        scoresChoisis.clear();
        for (int i = 0; i < questions.size(); i++)
            questionsContainer.getChildren().add(buildQuestionBox(i + 1, questions.get(i)));
    }

    private Question copierQuestion(Question q) {
        Question copie = new Question();
        copie.setQuestionId(q.getQuestionId());
        copie.setTexte(q.getTexte());
        copie.setOptionsQuest(q.getOptionsQuest());
        copie.setScoreOptions(q.getScoreOptions());
        copie.setTypeQuestion(q.getTypeQuestion());
        copie.setQuestionnaireId(q.getQuestionnaireId());
        return copie;
    }

    private void setLangActive(Button actif) {
        if (btnFR != null) btnFR.setStyle(LANG_IDLE);
        if (btnEN != null) btnEN.setStyle(LANG_IDLE);
        if (btnAR != null) btnAR.setStyle(LANG_IDLE);
        if (actif != null) actif.setStyle(LANG_ACTIVE);
    }

    private VBox buildQuestionBox(int numero, Question question) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #3b1f6e; -fx-background-radius: 10; " +
                "-fx-border-color: #6d28d9; -fx-border-radius: 10; " +
                "-fx-border-width: 1; -fx-padding: 14;");

        boolean arabe = isArabic(question.getTexte());
        box.setNodeOrientation(arabe ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        Label lblQuestion = new Label(numero + ". " + question.getTexte());
        lblQuestion.setWrapText(true);
        lblQuestion.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: bold;");
        if (arabe) lblQuestion.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        box.getChildren().add(lblQuestion);

        if (question.getOptionsQuest() != null && !question.getOptionsQuest().isEmpty()) {
            ToggleGroup group = new ToggleGroup();
            String[] options = splitOptions(question.getOptionsQuest());
            String[] scores  = question.getScoreOptions() != null && !question.getScoreOptions().isBlank()
                    ? splitScores(question.getScoreOptions()) : new String[0];

            for (int i = 0; i < options.length; i++) {
                String option = options[i].trim();
                if (option.isEmpty()) continue;
                final int idx = i;

                RadioButton rb = new RadioButton(option);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-text-fill: #c084fc; -fx-font-size: 13px;");
                if (arabe) rb.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

                String rep = reponsesChoisies.get(question.getQuestionId());
                if (rep != null && rep.equals(option)) rb.setSelected(true);

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
            tfReponse.setPromptText("Votre reponse...");
            tfReponse.setStyle("-fx-background-color: #1a0a2e; -fx-text-fill: #e2e8f0; " +
                    "-fx-border-color: #6d28d9; -fx-border-radius: 6; " +
                    "-fx-background-radius: 6; -fx-padding: 7;");
            if (arabe) tfReponse.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            String rep = reponsesChoisies.get(question.getQuestionId());
            if (rep != null) tfReponse.setText(rep);

            tfReponse.textProperty().addListener((obs, old, val) ->
                    reponsesChoisies.put(question.getQuestionId(), val));
            box.getChildren().add(tfReponse);
        }

        return box;
    }

    @FXML
    public void handleSoumettre() {
        if (questions == null || questions.isEmpty()) {
            setStatus("Aucune question a repondre !", false);
            return;
        }
        for (Question q : questions) {
            if (!reponsesChoisies.containsKey(q.getQuestionId())) {
                setStatus("Veuillez repondre a toutes les questions !", false);
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
        int    userId         = LimiteQuestionnaire.getInstance().getUserId();

        Reponsequestionnaire reponse = new Reponsequestionnaire(
                scoreTotale, jsonReponses.toString(), interpretation,
                null, niveau, scoreTotale >= questionnaire.getSeuilSevere(),
                null, questionnaire.getQuestionnaireId(), userId
        );

        try {
            repService.ajouter(reponse);

            // ✅ Email en arrière-plan
            String emailUser = null;
            try {
                emailUser = SessionManager.getInstance().getCurrentUser().getEmail();
            } catch (Exception ignored) {}

            if (emailUser != null && !emailUser.isBlank()) {
                final String nF = niveau;
                final double sF = scoreTotale;
                final String nQ = questionnaire.getNom();
                final String iF = interpretation;
                final String eF = emailUser;
                new Thread(() ->
                        EmailServiceQuestionnaire.envoyerResultat(eF, nQ, sF, nF, iF)
                ).start();
            }

            // ✅ Alert résultat
            String emoji = switch (niveau != null ? niveau.toLowerCase() : "") {
                case "legere", "leger" -> "🟢";
                case "modere"          -> "🟡";
                case "severe"          -> "🔴";
                default                -> "📊";
            };

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Resultat du Questionnaire");
            alert.setHeaderText("📋 " + questionnaire.getNom());
            alert.setContentText(
                    "🎯 Score : " + String.format("%.0f", scoreTotale) + "\n" +
                            emoji + " Niveau : " + (niveau != null ? niveau.toUpperCase() : "—") + "\n\n" +
                            "📝 Interpretation :\n" + (interpretation != null ? interpretation : "—") + "\n\n" +
                            "🤖 Analyse IA en cours de génération...\n" +
                            (emailUser != null ? "📧 Email envoyé à : " + emailUser : "")
            );
            alert.showAndWait();

            // ✅ Navigation vers Analyse IA
            naviguerVersAnalyseIA(scoreTotale, niveau, interpretation, jsonReponses.toString());

        } catch (Exception e) {
            if ("LIMITE_QUOTIDIENNE".equals(e.getMessage())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Limite atteinte");
                alert.setHeaderText("Limite quotidienne atteinte");
                alert.setContentText(
                        "Vous avez deja passe 2 questionnaires aujourd'hui.\nRevenez demain !");
                alert.showAndWait();
            } else {
                setStatus("Erreur soumission : " + e.getMessage(), false);
            }
        }
    }

    // ✅ Navigation vers la page Analyse IA
    private void naviguerVersAnalyseIA(
            double score, String niveau,
            String interpretation, String reponsesJson) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AnalyseIAView.fxml"));
            Parent view = loader.load();

            NermineIAAnalyseController ctrl = loader.getController();

            org.example.entities.User user = null;
            try {
                user = SessionManager.getInstance().getCurrentUser();
            } catch (Exception ignored) {}

            ctrl.setDonnees(questionnaire, score, niveau, interpretation, reponsesJson, user);

            // ✅ Utiliser contentArea directement
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            } else {
                // ✅ Chercher contentArea dans le parent
                javafx.scene.Node node = questionsContainer;
                while (node.getParent() != null) {
                    node = node.getParent();
                    if (node instanceof StackPane) {
                        ((StackPane) node).getChildren().setAll(view);
                        return;
                    }
                }
                // ✅ Fallback — Alert avec analyse IA
                afficherAnalyseIADansAlert(score, niveau, interpretation, reponsesJson);
            }

        } catch (Exception e) {
            System.err.println("Erreur navigation AnalyseIA : " + e.getMessage());
            // ✅ Fallback — afficher dans une Alert
            afficherAnalyseIADansAlert(score, niveau, interpretation, reponsesJson);
        }
    }

    // ✅ Fallback — affiche l'analyse IA dans une Alert si navigation impossible
    private void afficherAnalyseIADansAlert(
            double score, String niveau,
            String interpretation, String reponsesJson) {

        setStatus("🤖 Génération analyse IA...", true);

        String nomQ  = questionnaire != null ? questionnaire.getNom()  : "Questionnaire";
        String typeQ = questionnaire != null && questionnaire.getType() != null
                ? questionnaire.getType().toString() : "GENERAL";

        new Thread(() -> {
            String analyse = OpenAIService.analyserReponsesDetaillees(
                    nomQ, typeQ, score, niveau, interpretation, reponsesJson);

            javafx.application.Platform.runLater(() -> {
                if (analyse != null && !analyse.isBlank()) {
                    Alert alertIA = new Alert(Alert.AlertType.INFORMATION);
                    alertIA.setTitle("🤖 Analyse IA Personnalisée");
                    alertIA.setHeaderText("UniMind AI — Conseils personnalisés");

                    TextArea textArea = new TextArea(analyse);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setStyle(
                            "-fx-font-size: 13px;" +
                                    "-fx-font-family: 'Segoe UI';" +
                                    "-fx-background-color: #f5f3ff;");
                    textArea.setPrefSize(500, 300);

                    alertIA.getDialogPane().setContent(textArea);
                    alertIA.getDialogPane().setMinWidth(550);
                    alertIA.showAndWait();
                }

                // ✅ Navigation vers Mes Réponses après l'analyse
                if (contentArea != null) {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/fxml/EtudiantMesReponsesView.fxml"));
                        Parent view = loader.load();
                        EtudiantMesReponsesController ctrl = loader.getController();
                        ctrl.setContentArea(contentArea);
                        contentArea.getChildren().setAll(view);
                    } catch (Exception ex) {
                        System.out.println("Erreur navigation MesReponses : " + ex.getMessage());
                    }
                }
            });
        }).start();
    }

    @FXML
    public void handleRetour() {
        if (contentArea != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/EtudiantQuestionnairesView.fxml"));
                Parent view = loader.load();
                EtudiantQuestionnairesController ctrl = loader.getController();
                ctrl.setContentArea(contentArea);
                contentArea.getChildren().setAll(view);
            } catch (Exception e) {
                System.out.println("Erreur retour : " + e.getMessage());
            }
        }
    }

    private void setStatus(String msg, boolean success) {
        if (lblStatus == null) return;
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}