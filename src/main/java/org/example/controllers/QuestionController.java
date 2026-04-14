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

    @FXML private TextArea taTexte;
    @FXML private TextField tfOptions, tfScores, tfTypeQuestion;
    @FXML private ComboBox<String> cbQuestionnaire;
    @FXML private Label lblStatus;

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
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez une question dans le tableau", false); return; }
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
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez une question dans le tableau", false); return; }
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
        taTexte.clear(); tfOptions.clear(); tfScores.clear(); tfTypeQuestion.clear();
        cbQuestionnaire.setValue(null);
        lblStatus.setText("");
    }

    // ══════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════
    private boolean valider() {
        StringBuilder err = new StringBuilder();

        if (taTexte.getText().trim().isEmpty())
            err.append("• Le texte est obligatoire\n");
        else if (taTexte.getText().trim().length() < 5)
            err.append("• Le texte doit contenir au moins 5 caractères\n");

        if (cbQuestionnaire.getValue() == null)
            err.append("• Veuillez sélectionner un questionnaire\n");

        // Vérif cohérence options/scores
        String opts   = tfOptions.getText().trim();
        String scores = tfScores.getText().trim();
        if (!opts.isEmpty() && !scores.isEmpty()) {
            String[] optArr   = opts.split(",");
            String[] scoreArr = scores.split(",");
            if (optArr.length != scoreArr.length)
                err.append("• Le nombre d'options et de scores doit être identique\n");
            for (String s : scoreArr) {
                try { Double.parseDouble(s.trim()); }
                catch (NumberFormatException ex) {
                    err.append("• Les scores doivent être des nombres\n"); break;
                }
            }
        }

        if (err.length() > 0) { setStatus(err.toString(), false); return false; }
        return true;
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
                cbQuestionnaire.setValue(item); break;
            }
        }
        setStatus("📌 Question #" + selectedId + " sélectionnée", true);
    }

    private void loadData() {
        try { data.setAll(service.afficher()); }
        catch (SQLException e) { setStatus("❌ Erreur chargement : " + e.getMessage(), false); }
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
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (success ? "#22c55e" : "#ef4444") + ";");
    }
}