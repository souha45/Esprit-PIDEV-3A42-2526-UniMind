package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.entities.Reponsequestionnaire;
import org.example.services.EmailServiceQuestionnaire;
import org.example.services.QuestionServices;
import org.example.services.ReponseQuestionnaireServices;
import org.example.services.TraductionService;
import org.example.utils.LimiteQuestionnaire;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class EtudiantRepondreController implements Initializable {

    @FXML private Label lblTitreQuestionnaire;
    @FXML private VBox questionsContainer;
    @FXML private Label lblStatus;
    @FXML private Button btnFR;
    @FXML private Button btnEN;
    @FXML private Button btnAR;

    private Questionnaire questionnaire;
    private List<Question> questions;
    private List<Question> questionsOriginales = new ArrayList<>();

    private final Map<Integer, String> reponsesChoisies = new HashMap<>();
    private final Map<Integer, Double> scoresChoisis = new HashMap<>();

    private final QuestionServices questionService = new QuestionServices();
    private final ReponseQuestionnaireServices repService = new ReponseQuestionnaireServices();

    private static final String LANG_ACTIVE =
            "-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                    "-fx-font-size: 12px; -fx-font-weight: bold; " +
                    "-fx-padding: 6 12; -fx-background-radius: 20; -fx-cursor: hand;";

    private static final String LANG_IDLE =
            "-fx-background-color: #f5f3ff; -fx-text-fill: #7c3aed; " +
                    "-fx-font-size: 12px; -fx-padding: 6 12; " +
                    "-fx-background-radius: 20; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation des boutons de langue
        if (btnFR != null) btnFR.setOnAction(e -> handleLangFR());
        if (btnEN != null) btnEN.setOnAction(e -> handleLangEN());
        if (btnAR != null) btnAR.setOnAction(e -> handleLangAR());
    }

    public void setQuestionnaire(Questionnaire q) {
        this.questionnaire = q;
        lblTitreQuestionnaire.setText("📋 " + q.getNom());
        chargerQuestions();
    }

    private void chargerQuestions() {
        try {
            questions = questionService.afficherParQuestionnaire(
                    questionnaire.getQuestionnaireId());

            // Sauvegarder les originaux
            questionsOriginales = new ArrayList<>(questions);

            questionsContainer.getChildren().clear();

            if (questions.isEmpty()) {
                Label lblVide = new Label("⚠️ Aucune question trouvée pour ce questionnaire.");
                lblVide.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px;");
                questionsContainer.getChildren().add(lblVide);
                return;
            }

            for (int i = 0; i < questions.size(); i++) {
                questionsContainer.getChildren().add(buildQuestionBox(i + 1, questions.get(i)));
            }

            // Activer FR par défaut
            if (btnFR != null) setLangActive(btnFR);

        } catch (SQLException e) {
            setStatus("❌ Erreur chargement questions: " + e.getMessage(), false);
        }
    }

    // ══════════════════════════════════════════
    //  TRADUCTION
    // ══════════════════════════════════════════

    @FXML
    public void handleLangFR() {
        setLangActive(btnFR);
        // Restaurer questions originales
        questions = new ArrayList<>(questionsOriginales);
        afficherQuestions();
        setStatus("🇫🇷 Français", true);
    }

    @FXML
    public void handleLangEN() {
        setLangActive(btnEN);
        setStatus("⏳ Traduction en anglais...", true);

        new Thread(() -> {
            List<Question> traduits = new ArrayList<>();
            for (Question q : questionsOriginales) {
                Question copie = copierQuestion(q);
                try {
                    copie.setTexte(TraductionService.versAnglais(q.getTexte()));
                    // Traduire les options
                    if (q.getOptionsQuest() != null && !q.getOptionsQuest().isBlank()) {
                        String[] opts = q.getOptionsQuest()
                                .replaceAll("[\\[\\]\"]", "").split(",");
                        StringBuilder newOpts = new StringBuilder();
                        for (String opt : opts) {
                            newOpts.append(TraductionService.versAnglais(opt.trim()))
                                    .append(",");
                        }
                        copie.setOptionsQuest(newOpts.toString()
                                .replaceAll(",$", ""));
                    }
                } catch (Exception e) {
                    System.err.println("Erreur traduction EN: " + e.getMessage());
                    copie = copierQuestion(q); // Garder l'original en cas d'erreur
                }
                traduits.add(copie);
            }
            javafx.application.Platform.runLater(() -> {
                questions = traduits;
                afficherQuestions();
                setStatus("🇬🇧 Traduit en Anglais", true);
            });
        }).start();
    }

    @FXML
    public void handleLangAR() {
        setLangActive(btnAR);
        setStatus("⏳ Traduction en arabe...", true);

        new Thread(() -> {
            List<Question> traduits = new ArrayList<>();
            for (Question q : questionsOriginales) {
                Question copie = copierQuestion(q);
                try {
                    copie.setTexte(TraductionService.versArabe(q.getTexte()));
                    if (q.getOptionsQuest() != null && !q.getOptionsQuest().isBlank()) {
                        String[] opts = q.getOptionsQuest()
                                .replaceAll("[\\[\\]\"]", "").split(",");
                        StringBuilder newOpts = new StringBuilder();
                        for (String opt : opts) {
                            newOpts.append(TraductionService.versArabe(opt.trim()))
                                    .append(",");
                        }
                        copie.setOptionsQuest(newOpts.toString()
                                .replaceAll(",$", ""));
                    }
                } catch (Exception e) {
                    System.err.println("Erreur traduction AR: " + e.getMessage());
                    copie = copierQuestion(q);
                }
                traduits.add(copie);
            }
            javafx.application.Platform.runLater(() -> {
                questions = traduits;
                afficherQuestions();
                setStatus("🇸🇦 Traduit en Arabe", true);
            });
        }).start();
    }

    private void afficherQuestions() {
        questionsContainer.getChildren().clear();
        reponsesChoisies.clear();
        scoresChoisis.clear();
        for (int i = 0; i < questions.size(); i++) {
            questionsContainer.getChildren().add(
                    buildQuestionBox(i + 1, questions.get(i)));
        }
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

                // Vérifier si cette réponse était déjà choisie
                String reponseExistante = reponsesChoisies.get(question.getQuestionId());
                if (reponseExistante != null && reponseExistante.equals(option)) {
                    rb.setSelected(true);
                }

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

            // Restaurer la réponse existante
            String reponseExistante = reponsesChoisies.get(question.getQuestionId());
            if (reponseExistante != null) {
                tfReponse.setText(reponseExistante);
            }

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

        String niveau = questionnaire.getNiveauScore((int) scoreTotale);
        String interpretation = questionnaire.interpreterScore((int) scoreTotale);
        int userId = LimiteQuestionnaire.getInstance().getUserId();

        Reponsequestionnaire reponse = new Reponsequestionnaire(
                scoreTotale,
                jsonReponses.toString(),
                interpretation,
                null,
                niveau,
                scoreTotale >= questionnaire.getSeuilSevere(),
                null,
                questionnaire.getQuestionnaireId(),
                userId
        );

        try {
            repService.ajouter(reponse);

            // Envoi email + confirmation
            String emailUser = "";
            boolean emailEnvoye = false;
            try {
                emailUser = SessionManager.getInstance().getCurrentUser().getEmail();
                EmailServiceQuestionnaire.envoyerResultat(
                        emailUser,
                        questionnaire.getNom(),
                        scoreTotale,
                        niveau,
                        interpretation
                );
                emailEnvoye = true;
            } catch (Exception ex) {
                System.out.println("⚠️ Email non envoyé : " + ex.getMessage());
            }

            // Alert avec confirmation email
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("✅ Résultat du Questionnaire");
            alert.setHeaderText("Questionnaire : " + questionnaire.getNom());
            alert.setContentText(
                    "🎯 Score Total : " + scoreTotale + "\n" +
                            "📊 Niveau      : " + niveau.toUpperCase() + "\n\n" +
                            "📝 Interprétation :\n" + interpretation + "\n\n" +
                            (emailEnvoye
                                    ? "📧 Email de résultat envoyé à : " + emailUser + "\n\n"
                                    : "⚠️ Email non envoyé.\n\n") +
                            "Cliquez sur OK pour voir vos réponses."
            );

            // Navigation APRÈS fermeture de l'Alert
            alert.showAndWait().ifPresent(btn -> {
                try {
                    NavigationContext.loadContentInCenter("/fxml/EtudiantMesReponsesView.fxml");
                } catch (Exception ex) {
                    setStatus("❌ Erreur navigation : " + ex.getMessage(), false);
                }
            });

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
            NavigationContext.loadContentInCenter("/fxml/EtudiantQuestionnairesView.fxml");
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