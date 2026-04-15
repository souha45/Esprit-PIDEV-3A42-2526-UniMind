package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.services.QuestionServices;
import org.example.services.QuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class QuestionController implements Initializable {

    // ─── LABELS ERREUR ───
    @FXML private Label errTexte, errQuestionnaire, errOptions, errScores, errTypeQuestion;

    // ─── FORM ───
    @FXML private TextArea taTexte;
    @FXML private TextField tfOptions, tfScores, tfTypeQuestion;
    @FXML private ComboBox<String> cbQuestionnaire;
    @FXML private Label lblStatus;

    // ─── TABLE ───
    @FXML private TableView<Question> tableQuestion;
    @FXML private TableColumn<Question, Integer> colId, colQId;
    @FXML private TableColumn<Question, String>  colTexte, colOptions, colScores, colType;

    private final QuestionServices service       = new QuestionServices();
    private final QuestionnaireServices qService = new QuestionnaireServices();
    private final ObservableList<Question> data  = FXCollections.observableArrayList();
    private int selectedId = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("questionId"));
        colTexte.setCellValueFactory(new PropertyValueFactory<>("texte"));
        colQId.setCellValueFactory(new PropertyValueFactory<>("questionnaireId"));
        colOptions.setCellValueFactory(new PropertyValueFactory<>("optionsQuest"));
        colScores.setCellValueFactory(new PropertyValueFactory<>("scoreOptions"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeQuestion"));

        tableQuestion.setItems(data);
        tableQuestion.setOnMouseClicked(e -> {
            Question selected = tableQuestion.getSelectionModel().getSelectedItem();
            if (selected != null) fillForm(selected);
        });

        loadQuestionnaires();
        loadData();
    }

    // ══════════════════════════════════════════
    //  HANDLERS
    // ══════════════════════════════════════════

    @FXML
    public void handleAjouter() {
        if (!valider()) return;
        try {
            service.ajouter(buildFromForm());
            setStatus("✅ Question ajoutée avec succès !", true);
            handleReset();
            loadData();
        } catch (SQLException e) {
            setStatus("❌ Erreur : " + e.getMessage(), false);
        }
    }

    @FXML
    public void handleModifier() {
        if (selectedId == -1) {
            setStatus("⚠️ Sélectionnez une question dans le tableau", false);
            return;
        }
        if (!valider()) return;
        try {
            Question q = buildFromForm();
            q.setQuestionId(selectedId);
            service.modifier(q);
            setStatus("✅ Question modifiée avec succès !", true);
            handleReset();
            loadData();
        } catch (SQLException e) {
            setStatus("❌ Erreur : " + e.getMessage(), false);
        }
    }

    @FXML
    public void handleSupprimer() {
        if (selectedId == -1) {
            setStatus("⚠️ Sélectionnez une question dans le tableau", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(selectedId);
                    setStatus("✅ Question supprimée !", true);
                    handleReset();
                    loadData();
                } catch (SQLException e) {
                    setStatus("❌ Erreur : " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    public void handleReset() {
        selectedId = -1;
        taTexte.clear();
        tfOptions.clear();
        tfScores.clear();
        tfTypeQuestion.clear();
        cbQuestionnaire.setValue(null);
        lblStatus.setText("");
        errTexte.setText("");
        errQuestionnaire.setText("");
        errOptions.setText("");
        errScores.setText("");
        errTypeQuestion.setText("");
    }

    // ══════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════

    private boolean valider() {
        errTexte.setText("");
        errQuestionnaire.setText("");
        errOptions.setText("");
        errScores.setText("");
        errTypeQuestion.setText("");
        lblStatus.setText("");

        boolean ok = true;

        // Validation texte
        if (taTexte.getText().trim().isEmpty()) {
            errTexte.setText("⚠ Le texte est obligatoire");
            ok = false;
        } else if (taTexte.getText().trim().length() < 5) {
            errTexte.setText("⚠ Minimum 5 caractères");
            ok = false;
        }

        // Validation questionnaire
        if (cbQuestionnaire.getValue() == null) {
            errQuestionnaire.setText("⚠ Veuillez sélectionner un questionnaire");
            ok = false;
        }

        // Validation options / scores
        String opts   = tfOptions.getText().trim();
        String scores = tfScores.getText().trim();

        if (!opts.isEmpty() && !scores.isEmpty()) {
            String[] optArr   = opts.split(",");
            String[] scoreArr = scores.split(",");
            if (optArr.length != scoreArr.length) {
                errOptions.setText("⚠ Nombre d'options et scores différent");
                ok = false;
            } else {
                for (String s : scoreArr) {
                    try {
                        Double.parseDouble(s.trim());
                    } catch (NumberFormatException ex) {
                        errScores.setText("⚠ Les scores doivent être des nombres");
                        ok = false;
                        break;
                    }
                }
            }
        } else if (!opts.isEmpty() && scores.isEmpty()) {
            errScores.setText("⚠ Veuillez saisir les scores correspondants");
            ok = false;
        } else if (opts.isEmpty() && !scores.isEmpty()) {
            errOptions.setText("⚠ Veuillez saisir les options correspondantes");
            ok = false;
        }

        // Validation type question
        if (tfTypeQuestion.getText().trim().isEmpty()) {
            errTypeQuestion.setText("⚠ Le type de question est obligatoire");
            ok = false;
        } else {
            String type = tfTypeQuestion.getText().trim().toUpperCase();
            if (!type.equals("QCM") && !type.equals("TEXTE")) {
                errTypeQuestion.setText("⚠ Type invalide : utilisez QCM ou TEXTE");
                ok = false;
            }
        }

        return ok;
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════

    private Question buildFromForm() {
        Question q = new Question();
        q.setTexte(taTexte.getText().trim());
        q.setOptionsQuest(tfOptions.getText().trim());
        q.setScoreOptions(tfScores.getText().trim());
        q.setTypeQuestion(tfTypeQuestion.getText().trim());
        String sel = cbQuestionnaire.getValue();
        if (sel != null) q.setQuestionnaireId(Integer.parseInt(sel.split(" - ")[0].trim()));
        return q;
    }

    private void fillForm(Question q) {
        selectedId = q.getQuestionId();
        taTexte.setText(q.getTexte());
        tfOptions.setText(q.getOptionsQuest() != null ? q.getOptionsQuest() : "");
        tfScores.setText(q.getScoreOptions() != null ? q.getScoreOptions() : "");
        tfTypeQuestion.setText(q.getTypeQuestion() != null ? q.getTypeQuestion() : "");
        for (String item : cbQuestionnaire.getItems()) {
            if (item.startsWith(q.getQuestionnaireId() + " - ")) {
                cbQuestionnaire.setValue(item);
                break;
            }
        }
        setStatus("📌 Question #" + selectedId + " sélectionnée", true);
    }

    private void loadData() {
        try {
            data.setAll(service.afficher());
        } catch (SQLException e) {
            setStatus("❌ Erreur chargement : " + e.getMessage(), false);
        }
    }

    private void loadQuestionnaires() {
        try {
            List<Questionnaire> list = qService.afficher();
            ObservableList<String> items = FXCollections.observableArrayList();
            for (Questionnaire q : list) items.add(q.getQuestionnaireId() + " - " + q.getNom());
            cbQuestionnaire.setItems(items);
        } catch (SQLException e) {
            setStatus("❌ Erreur chargement questionnaires", false);
        }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}