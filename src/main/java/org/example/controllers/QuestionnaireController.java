package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
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
    @FXML private TextArea  taDescription;
    @FXML private ComboBox<TypeQuestionnaire> cbType;
    @FXML private Label     lblStatus;

    // ─── SEARCH ───
    @FXML private TextField tfSearch;

    // ─── LIST ───
    @FXML private ListView<Questionnaire> listQuestionnaire;

    private final QuestionnaireServices   service = new QuestionnaireServices();
    private final ObservableList<Questionnaire> data     = FXCollections.observableArrayList();
    private FilteredList<Questionnaire>         filtered;
    private int selectedId = -1;

    // ══════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList(TypeQuestionnaire.values()));

        // FilteredList pour la recherche
        filtered = new FilteredList<>(data, p -> true);
        listQuestionnaire.setItems(filtered);
        listQuestionnaire.setCellFactory(lv -> new QuestionnaireCard());

        // Recherche dynamique
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, old, val) -> {
                String lower = val == null ? "" : val.toLowerCase().trim();
                filtered.setPredicate(q ->
                        lower.isEmpty()
                                || q.getNom().toLowerCase().contains(lower)
                                || q.getCode().toLowerCase().contains(lower)
                                || q.getType().name().toLowerCase().contains(lower)
                );
            });
        }

        // Sélection d'un item
        listQuestionnaire.setOnMouseClicked(e -> {
            Questionnaire selected = listQuestionnaire.getSelectionModel().getSelectedItem();
            if (selected != null) fillForm(selected);
        });

        loadData();
    }

    // ══════════════════════════════════════════
    //  INNER CLASS — CARTE PERSONNALISÉE
    // ══════════════════════════════════════════

    private static class QuestionnaireCard extends ListCell<Questionnaire> {

        private final HBox  root      = new HBox(12);
        private final StackPane avatar = new StackPane();
        private final Label  avLetter  = new Label();
        private final VBox   info      = new VBox(4);
        private final HBox   topRow    = new HBox(8);
        private final HBox   botRow    = new HBox(6);
        private final Label  nomLbl    = new Label();
        private final Label  codeLbl   = new Label();
        private final Label  typeBadge = new Label();
        private final Label  nbLbl     = new Label();
        private final HBox   seuilBox  = new HBox(4);
        private final Label  seuilL    = new Label();
        private final Label  seuilM    = new Label();
        private final Label  seuilS    = new Label();
        private final Region spacer    = new Region();

        QuestionnaireCard() {
            // Avatar cercle
            Circle circle = new Circle(19);
            avatar.getChildren().addAll(circle, avLetter);
            avatar.setMinSize(38, 38);
            avatar.setMaxSize(38, 38);
            avLetter.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

            // Nom
            nomLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            nomLbl.setMaxWidth(180);
            nomLbl.setEllipsisString("…");

            // Code
            codeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-family: monospace;");

            // Type badge
            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 20;");

            // Nb questions
            nbLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            // Seuil chips
            styleChip(seuilL, "#d1fae5", "#065f46");
            styleChip(seuilM, "#fef3c7", "#92400e");
            styleChip(seuilS, "#fee2e2", "#991b1b");
            seuilBox.getChildren().addAll(seuilL, seuilM, seuilS);
            seuilBox.setAlignment(Pos.CENTER_RIGHT);

            // Assemblage top row
            topRow.getChildren().addAll(nomLbl, codeLbl);
            topRow.setAlignment(Pos.CENTER_LEFT);

            // Séparateur
            Label dot = new Label("·");
            dot.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");

            botRow.getChildren().addAll(typeBadge, dot, nbLbl);
            botRow.setAlignment(Pos.CENTER_LEFT);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            info.getChildren().addAll(topRow, botRow);
            HBox.setHgrow(info, Priority.ALWAYS);

            root.getChildren().addAll(avatar, info, spacer, seuilBox);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 14, 10, 14));
            root.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                    + "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-border-width: 1;");

            setStyle("-fx-background-color: transparent; -fx-padding: 3 0 3 0;");
        }

        private void styleChip(Label lbl, String bg, String fg) {
            lbl.setStyle(String.format(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1 5 1 5; "
                            + "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 4;", bg, fg));
        }

        @Override
        protected void updateItem(Questionnaire q, boolean empty) {
            super.updateItem(q, empty);
            if (empty || q == null) {
                setGraphic(null);
                return;
            }

            // Nom + code
            nomLbl.setText(q.getNom());
            codeLbl.setText(q.getCode());
            nbLbl.setText(q.getNbreQuestions() + " question" + (q.getNbreQuestions() > 1 ? "s" : ""));

            // Seuils
            seuilL.setText("L: " + q.getSeuilLegere());
            seuilM.setText("M: " + q.getSeuilModere());
            seuilS.setText("S: " + q.getSeuilSevere());

            // Couleurs selon le type
            String[] colors = typeColors(q.getType());
            String avBg  = colors[0]; // bg avatar
            String avFg  = colors[1]; // fg avatar
            String bdBg  = colors[2]; // bg badge
            String bdFg  = colors[3]; // fg badge
            String label = colors[4]; // texte badge

            avLetter.setText(label.substring(0, 1));
            avLetter.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + avFg + ";");
            // Recolor circle via lookup trick
            avatar.setStyle("-fx-background-color: " + avBg + "; -fx-background-radius: 19;");
            avatar.getChildren().stream()
                    .filter(n -> n instanceof Circle)
                    .forEach(n -> ((Circle) n).setFill(Color.web(avBg)));

            typeBadge.setText(label);
            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; "
                    + "-fx-background-color: " + bdBg + "; -fx-text-fill: " + bdFg + "; -fx-background-radius: 20;");

            // Highlight si sélectionné
            if (isSelected()) {
                root.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; "
                        + "-fx-border-color: #3b82f6; -fx-border-radius: 10; -fx-border-width: 1.5;");
            } else {
                root.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                        + "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-border-width: 1;");
            }

            setGraphic(root);
        }

        /** Retourne [avBg, avFg, badgeBg, badgeFg, label] selon le type */
        private String[] typeColors(TypeQuestionnaire type) {
            if (type == null) return new String[]{"#f1f5f9","#64748b","#f1f5f9","#64748b","?"};
            return switch (type) {
                case STRESS     -> new String[]{"#fef2f2","#dc2626","#fee2e2","#991b1b","Stress"};
                case ANXIETE    -> new String[]{"#f5f3ff","#7c3aed","#ede9fe","#5b21b6","Anxiété"};
                case DEPRESSION -> new String[]{"#eff6ff","#2563eb","#dbeafe","#1d4ed8","Dépression"};
                case BIEN_ETRE  -> new String[]{"#f0fdf4","#16a34a","#dcfce7","#15803d","Bien-être"};
                case SOMMEIL    -> new String[]{"#fffbeb","#d97706","#fef3c7","#92400e","Sommeil"};
                default         -> new String[]{"#f8fafc","#475569","#f1f5f9","#334155", type.name()};
            };
        }
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
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez un questionnaire", false); return; }
        if (!valider()) return;
        try {
            Questionnaire q = buildFromForm();
            q.setQuestionnaireId(selectedId);
            service.modifier(q);
            setStatus("✅ Questionnaire modifié !", true);
            handleReset(); loadData();
        } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void handleSupprimer() {
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez un questionnaire", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer ce questionnaire ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(selectedId);
                    setStatus("✅ Questionnaire supprimé !", true);
                    handleReset(); loadData();
                } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
            }
        });
    }

    @FXML
    public void handleReset() {
        selectedId = -1;
        tfCode.clear(); tfNom.clear(); taDescription.clear();
        tfSeuilLeger.clear(); tfSeuilModere.clear(); tfSeuilSevere.clear();
        tfNbreQuestions.clear(); cbType.setValue(null); lblStatus.setText("");
        errCode.setText(""); errNom.setText(""); errType.setText("");
        errSeuilLeger.setText(""); errSeuilModere.setText("");
        errSeuilSevere.setText(""); errNbreQuestions.setText("");
        if (tfSearch != null) tfSearch.clear();
    }

    // ══════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════

    private boolean valider() {
        errCode.setText(""); errNom.setText(""); errType.setText("");
        errSeuilLeger.setText(""); errSeuilModere.setText("");
        errSeuilSevere.setText(""); errNbreQuestions.setText("");
        lblStatus.setText("");
        boolean ok = true;

        if (tfCode.getText().trim().isEmpty())           { errCode.setText("⚠ Obligatoire"); ok = false; }
        else if (tfCode.getText().trim().length() > 20)  { errCode.setText("⚠ Max 20 caractères"); ok = false; }
        if (tfNom.getText().trim().isEmpty())             { errNom.setText("⚠ Obligatoire"); ok = false; }
        if (cbType.getValue() == null)                    { errType.setText("⚠ Obligatoire"); ok = false; }
        if (!isPositiveInt(tfSeuilLeger.getText()))       { errSeuilLeger.setText("⚠ Entier ≥ 0 requis"); ok = false; }
        if (!isPositiveInt(tfSeuilModere.getText()))      { errSeuilModere.setText("⚠ Entier ≥ 0 requis"); ok = false; }
        if (!isPositiveInt(tfSeuilSevere.getText()))      { errSeuilSevere.setText("⚠ Entier ≥ 0 requis"); ok = false; }
        if (!isPositiveInt(tfNbreQuestions.getText()))    { errNbreQuestions.setText("⚠ Entier ≥ 0 requis"); ok = false; }

        if (ok) {
            int sl = Integer.parseInt(tfSeuilLeger.getText().trim());
            int sm = Integer.parseInt(tfSeuilModere.getText().trim());
            int ss = Integer.parseInt(tfSeuilSevere.getText().trim());
            if (!(sl < sm && sm < ss)) { errSeuilLeger.setText("⚠ Légère < Modéré < Sévère"); ok = false; }
        }
        return ok;
    }

    private boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s.trim()) >= 0; } catch (Exception e) { return false; }
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
        // Refresh la carte sélectionnée pour le highlight
        listQuestionnaire.refresh();
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
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (success ? "#22c55e" : "#ef4444") + ";");
    }
}