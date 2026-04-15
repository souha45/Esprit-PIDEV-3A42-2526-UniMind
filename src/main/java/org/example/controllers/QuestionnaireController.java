package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.entities.Questionnaire;
import org.example.enums.TypeQuestionnaire;
import org.example.services.QuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class QuestionnaireController implements Initializable {

    @FXML private Label errCode, errNom, errType;
    @FXML private Label errSeuilLeger, errSeuilModere, errSeuilSevere, errNbreQuestions;

    // ─── FORM ───
    @FXML private TextField tfCode, tfNom, tfSeuilLeger, tfSeuilModere, tfSeuilSevere, tfNbreQuestions;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<TypeQuestionnaire> cbType;
    @FXML private Label lblStatus;

    // ─── TABLE ───
    @FXML private TableView<Questionnaire> tableQuestionnaire;
    @FXML private TableColumn<Questionnaire, Integer> colId, colNbre, colSL, colSM, colSS;
    @FXML private TableColumn<Questionnaire, String>  colCode, colNom, colType;

    private final QuestionnaireServices service = new QuestionnaireServices();
    private final ObservableList<Questionnaire> data = FXCollections.observableArrayList();
    private int selectedId = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList(TypeQuestionnaire.values()));

        colId.setCellValueFactory(new PropertyValueFactory<>("questionnaireId"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colNbre.setCellValueFactory(new PropertyValueFactory<>("nbreQuestions"));
        colSL.setCellValueFactory(new PropertyValueFactory<>("seuilLegere"));
        colSM.setCellValueFactory(new PropertyValueFactory<>("seuilModere"));
        colSS.setCellValueFactory(new PropertyValueFactory<>("seuilSevere"));

        tableQuestionnaire.setItems(data);

        tableQuestionnaire.setOnMouseClicked(e -> {
            Questionnaire selected = tableQuestionnaire.getSelectionModel().getSelectedItem();
            if (selected != null) fillForm(selected);
        });

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
            setStatus("✅ Questionnaire ajouté avec succès !", true);
            handleReset();
            loadData();
        } catch (SQLException e) {
            setStatus("❌ Erreur : " + e.getMessage(), false);
        }
    }

    @FXML
    public void handleModifier() {
        if (selectedId == -1) {
            setStatus("⚠️ Sélectionnez un questionnaire dans le tableau", false);
            return;
        }
        if (!valider()) return;
        try {
            Questionnaire q = buildFromForm();
            q.setQuestionnaireId(selectedId);
            service.modifier(q);
            setStatus("✅ Questionnaire modifié avec succès !", true);
            handleReset();
            loadData();
        } catch (SQLException e) {
            setStatus("❌ Erreur : " + e.getMessage(), false);
        }
    }

    @FXML
    public void handleSupprimer() {
        if (selectedId == -1) {
            setStatus("⚠️ Sélectionnez un questionnaire dans le tableau", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer ce questionnaire ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(selectedId);
                    setStatus("✅ Questionnaire supprimé !", true);
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
        tfCode.clear(); tfNom.clear(); taDescription.clear();
        tfSeuilLeger.clear(); tfSeuilModere.clear(); tfSeuilSevere.clear();
        tfNbreQuestions.clear();
        cbType.setValue(null);
        lblStatus.setText("");
        // Vider les messages d'erreur
        errCode.setText(""); errNom.setText(""); errType.setText("");
        errSeuilLeger.setText(""); errSeuilModere.setText("");
        errSeuilSevere.setText(""); errNbreQuestions.setText("");
    }

    // ══════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════

    private boolean valider() {
        // Réinitialiser tous les messages d'erreur
        errCode.setText(""); errNom.setText(""); errType.setText("");
        errSeuilLeger.setText(""); errSeuilModere.setText("");
        errSeuilSevere.setText(""); errNbreQuestions.setText("");
        lblStatus.setText("");

        boolean ok = true;

        if (tfCode.getText().trim().isEmpty()) {
            errCode.setText("⚠ Le code est obligatoire");
            ok = false;
        } else if (tfCode.getText().trim().length() > 20) {
            errCode.setText("⚠ Maximum 20 caractères");
            ok = false;
        }

        if (tfNom.getText().trim().isEmpty()) {
            errNom.setText("⚠ Le nom est obligatoire");
            ok = false;
        }

        if (cbType.getValue() == null) {
            errType.setText("⚠ Le type est obligatoire");
            ok = false;
        }

        if (!isPositiveInt(tfSeuilLeger.getText())) {
            errSeuilLeger.setText("⚠ Entier positif requis");
            ok = false;
        }
        if (!isPositiveInt(tfSeuilModere.getText())) {
            errSeuilModere.setText("⚠ Entier positif requis");
            ok = false;
        }
        if (!isPositiveInt(tfSeuilSevere.getText())) {
            errSeuilSevere.setText("⚠ Entier positif requis");
            ok = false;
        }
        if (!isPositiveInt(tfNbreQuestions.getText())) {
            errNbreQuestions.setText("⚠ Entier positif requis");
            ok = false;
        }

        // Vérifier seuils croissants
        if (ok && isPositiveInt(tfSeuilLeger.getText())
                && isPositiveInt(tfSeuilModere.getText())
                && isPositiveInt(tfSeuilSevere.getText())) {
            int sl = Integer.parseInt(tfSeuilLeger.getText().trim());
            int sm = Integer.parseInt(tfSeuilModere.getText().trim());
            int ss = Integer.parseInt(tfSeuilSevere.getText().trim());
            if (!(sl < sm && sm < ss)) {
                errSeuilLeger.setText("⚠ Légère < Modéré < Sévère");
                ok = false;
            }
        }

        return ok;
    }

    private boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s.trim()) >= 0; }
        catch (Exception e) { return false; }
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════

    private Questionnaire buildFromForm() {
        Questionnaire q = new Questionnaire();
        q.setCode(tfCode.getText().trim());
        q.setNom(tfNom.getText().trim());
        q.setDescription(taDescription.getText().trim());
        q.setType(cbType.getValue());
        q.setSeuilLegere(Integer.parseInt(tfSeuilLeger.getText().trim()));
        q.setSeuilModere(Integer.parseInt(tfSeuilModere.getText().trim()));
        q.setSeuilSevere(Integer.parseInt(tfSeuilSevere.getText().trim()));
        q.setNbreQuestions(Integer.parseInt(tfNbreQuestions.getText().trim()));
        return q;
    }

    private void fillForm(Questionnaire q) {
        selectedId = q.getQuestionnaireId();
        tfCode.setText(q.getCode());
        tfNom.setText(q.getNom());
        taDescription.setText(q.getDescription() != null ? q.getDescription() : "");
        cbType.setValue(q.getType());
        tfSeuilLeger.setText(String.valueOf(q.getSeuilLegere()));
        tfSeuilModere.setText(String.valueOf(q.getSeuilModere()));
        tfSeuilSevere.setText(String.valueOf(q.getSeuilSevere()));
        tfNbreQuestions.setText(String.valueOf(q.getNbreQuestions()));
        setStatus("📌 Questionnaire #" + selectedId + " sélectionné", true);
    }

    private void loadData() {
        try {
            List<Questionnaire> list = service.afficher();
            data.setAll(list);
        } catch (SQLException e) {
            setStatus("❌ Erreur chargement : " + e.getMessage(), false);
        }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}