package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.enums.TypeQuestionnaire;
import org.example.services.GenerateurQuestionsIAService;
import org.example.services.QuestionServices;
import org.example.services.QuestionnaireServices;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur de la page "Générer Questions avec IA".
 * Accessible depuis AdminController via un bouton dédié.
 */
public class GenerateurQuestionsIAController {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private ComboBox<Questionnaire> cbQuestionnaire;
    @FXML private Label lblType;
    @FXML private Label lblNbreQuestions;
    @FXML private Spinner<Integer> spinnerNbre;
    @FXML private Button btnGenerer;
    @FXML private Button btnSauvegarder;

    @FXML private VBox boxChargement;
    @FXML private VBox boxResultat;
    @FXML private Label lblStatus;
    @FXML private ListView<String> listQuestions;

    // ── État interne ───────────────────────────────────────────────
    private List<Question> questionsGenerees = null;

    // ════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Charger les questionnaires dans le ComboBox
        chargerQuestionnaires();

        // Spinner : nombre de questions à générer (1 à 20)
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5);
        spinnerNbre.setValueFactory(factory);

        // Quand on sélectionne un questionnaire, afficher son type et nbre questions
        cbQuestionnaire.setOnAction(e -> {
            Questionnaire q = cbQuestionnaire.getValue();
            if (q != null) {
                lblType.setText(q.getType() != null ? q.getType().name() : "—");
                lblNbreQuestions.setText(String.valueOf(q.getNbreQuestions()));
                spinnerNbre.getValueFactory().setValue(Math.min(q.getNbreQuestions(), 20));
            }
        });

        // État initial
        boxChargement.setVisible(false);
        boxChargement.setManaged(false);
        boxResultat.setVisible(false);
        boxResultat.setManaged(false);
        btnSauvegarder.setDisable(true);
        lblStatus.setText("");
    }

    // ════════════════════════════════════════════════════════════════
    //  CHARGEMENT DES QUESTIONNAIRES
    // ════════════════════════════════════════════════════════════════

    private void chargerQuestionnaires() {
        try {
            QuestionnaireServices service = new QuestionnaireServices();
            List<Questionnaire> liste = service.afficher();

            cbQuestionnaire.setItems(FXCollections.observableArrayList(liste));

            // Affichage personnalisé dans le ComboBox
            cbQuestionnaire.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Questionnaire q, boolean empty) {
                    super.updateItem(q, empty);
                    setText(empty || q == null ? null :
                            q.getCode() + " — " + q.getNom() +
                                    (q.getType() != null ? " (" + q.getType().name() + ")" : ""));
                }
            });
            cbQuestionnaire.setButtonCell(cbQuestionnaire.getCellFactory().call(null));

        } catch (SQLException e) {
            afficherErreur("Erreur chargement questionnaires : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  GÉNÉRATION IA
    // ════════════════════════════════════════════════════════════════

    @FXML
    public void handleGenerer() {
        Questionnaire q = cbQuestionnaire.getValue();
        if (q == null) {
            afficherErreur("⚠ Sélectionnez un questionnaire d'abord.");
            return;
        }
        if (q.getType() == null) {
            afficherErreur("⚠ Ce questionnaire n'a pas de type défini.");
            return;
        }

        int nbre = spinnerNbre.getValue();
        String type = q.getType().name();
        String nom  = q.getNom();

        // Afficher le spinner de chargement
        boxChargement.setVisible(true);
        boxChargement.setManaged(true);
        boxResultat.setVisible(false);
        boxResultat.setManaged(false);
        btnGenerer.setDisable(true);
        btnSauvegarder.setDisable(true);
        lblStatus.setText("⏳ Génération en cours...");
        lblStatus.setStyle("-fx-text-fill: #6b7280;");

        // Appel en arrière-plan
        new Thread(() -> {
            System.out.println("🤖 Génération IA : " + nbre + " questions pour " + type);

            List<Question> questions = GenerateurQuestionsIAService.genererQuestions(
                    type, nom, nbre, q.getQuestionnaireId());

            Platform.runLater(() -> {
                boxChargement.setVisible(false);
                boxChargement.setManaged(false);
                btnGenerer.setDisable(false);

                if (questions == null || questions.isEmpty()) {
                    afficherErreur("❌ Échec de la génération. Vérifiez votre clé API.");
                    return;
                }

                questionsGenerees = questions;

                // Afficher les questions dans la ListView
                listQuestions.getItems().clear();
                for (int i = 0; i < questions.size(); i++) {
                    Question qq = questions.get(i);
                    String affichage = (i + 1) + ". " + qq.getTexte()
                            + "\n   Options : " + qq.getOptionsQuest().replace("|", " | ")
                            + "\n   Scores  : " + qq.getScoreOptions().replace("|", " | ");
                    listQuestions.getItems().add(affichage);
                }

                boxResultat.setVisible(true);
                boxResultat.setManaged(true);
                btnSauvegarder.setDisable(false);
                lblStatus.setText("✅ " + questions.size() + " questions générées avec succès !");
                lblStatus.setStyle("-fx-text-fill: #10b981;");
            });

        }).start();
    }

    // ════════════════════════════════════════════════════════════════
    //  SAUVEGARDE EN BASE DE DONNÉES
    // ════════════════════════════════════════════════════════════════

    @FXML
    public void handleSauvegarder() {
        if (questionsGenerees == null || questionsGenerees.isEmpty()) return;

        btnSauvegarder.setDisable(true);
        lblStatus.setText("💾 Sauvegarde en cours...");
        lblStatus.setStyle("-fx-text-fill: #6b7280;");

        new Thread(() -> {
            try {
                QuestionServices questionService = new QuestionServices();
                int count = 0;
                for (Question q : questionsGenerees) {
                    questionService.ajouter(q);
                    count++;
                }
                final int total = count;
                Platform.runLater(() -> {
                    lblStatus.setText("✅ " + total + " questions sauvegardées en base de données !");
                    lblStatus.setStyle("-fx-text-fill: #10b981;");
                    btnSauvegarder.setDisable(true); // éviter double sauvegarde
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherErreur("❌ Erreur sauvegarde : " + e.getMessage());
                    btnSauvegarder.setDisable(false);
                });
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════════════
    //  RESET
    // ════════════════════════════════════════════════════════════════

    @FXML
    public void handleReset() {
        cbQuestionnaire.setValue(null);
        lblType.setText("—");
        lblNbreQuestions.setText("—");
        spinnerNbre.getValueFactory().setValue(5);
        listQuestions.getItems().clear();
        boxResultat.setVisible(false);
        boxResultat.setManaged(false);
        boxChargement.setVisible(false);
        boxChargement.setManaged(false);
        btnSauvegarder.setDisable(true);
        lblStatus.setText("");
        questionsGenerees = null;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPER
    // ════════════════════════════════════════════════════════════════

    private void afficherErreur(String msg) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: #ef4444;");
    }
}