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
    @FXML private TextArea  taTexte;
    @FXML private TextField tfOptionInput;
    @FXML private TextField tfScoreInput;
    @FXML private ComboBox<String> cbQuestionnaire;
    @FXML private ComboBox<String> cbTypeQuestion;
    @FXML private Label lblStatus;

    // ─── LISTE OPTIONS DYNAMIQUE ───
    @FXML private ListView<String> lvOptions;
    @FXML private ListView<String> lvScores;
    private final ObservableList<String> optionsList = FXCollections.observableArrayList();
    private final ObservableList<String> scoresList  = FXCollections.observableArrayList();

    // ─── SEARCH ───
    @FXML private TextField tfSearch;

    // ─── LIST QUESTIONS ───
    @FXML private ListView<Question> listQuestion;

    // ─── PAGINATION ───
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label  lblPage;
    private static final int PAGE_SIZE = 5;
    private int currentPage = 0;
    private final ObservableList<Question> pageData = FXCollections.observableArrayList();

    private final QuestionServices      service  = new QuestionServices();
    private final QuestionnaireServices qService = new QuestionnaireServices();
    private final ObservableList<Question> data  = FXCollections.observableArrayList();
    private FilteredList<Question> filtered;
    private int selectedId = -1;

    // ══════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cbTypeQuestion.setItems(FXCollections.observableArrayList("QCM", "TEXTE", "LIKERT"));

        lvOptions.setItems(optionsList);
        lvScores.setItems(scoresList);

        filtered = new FilteredList<>(data, p -> true);

        // ✅ La ListView affiche pageData (page courante)
        listQuestion.setItems(pageData);
        listQuestion.setCellFactory(lv -> new QuestionCard());

        // ─── Recherche dynamique ───
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, old, val) -> {
                String lower = val == null ? "" : val.toLowerCase().trim();
                filtered.setPredicate(q ->
                        lower.isEmpty()
                                || q.getTexte().toLowerCase().contains(lower)
                                || (q.getTypeQuestion() != null && q.getTypeQuestion().toLowerCase().contains(lower))
                                || String.valueOf(q.getQuestionnaireId()).contains(lower)
                );
                // Retour à la page 1 après recherche
                currentPage = 0;
                updatePage();
            });
        }

        listQuestion.setOnMouseClicked(e -> {
            Question selected = listQuestion.getSelectionModel().getSelectedItem();
            if (selected != null) fillForm(selected);
        });

        loadQuestionnaires();
        loadData();
    }

    // ══════════════════════════════════════════
    //  PAGINATION
    // ══════════════════════════════════════════

    private void updatePage() {
        int total      = filtered.size();
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        // Sécurité page courante
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)           currentPage = 0;

        // Extraire les éléments de la page courante
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pageData.setAll(filtered.subList(from, to));

        // Mettre à jour le label et les boutons
        lblPage.setText("Page " + (currentPage + 1) + " / " + totalPages
                + "  (" + total + " question" + (total > 1 ? "s" : "") + ")");
        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    public void handlePrev() {
        if (currentPage > 0) {
            currentPage--;
            updatePage();
        }
    }

    @FXML
    public void handleNext() {
        int totalPages = (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePage();
        }
    }

    // ══════════════════════════════════════════
    //  AJOUT / SUPPRESSION OPTION
    // ══════════════════════════════════════════

    @FXML
    public void handleAjouterOption() {
        String opt   = tfOptionInput.getText().trim();
        String score = tfScoreInput.getText().trim();

        if (opt.isEmpty()) { errOptions.setText("⚠ Saisir une option"); return; }
        if (score.isEmpty()) { errScores.setText("⚠ Saisir un score"); return; }
        try { Double.parseDouble(score); }
        catch (NumberFormatException e) { errScores.setText("⚠ Score doit être un nombre"); return; }

        optionsList.add(opt);
        scoresList.add(score);
        tfOptionInput.clear();
        tfScoreInput.clear();
        errOptions.setText("");
        errScores.setText("");
    }

    @FXML
    public void handleSupprimerOption() {
        int idx = lvOptions.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            optionsList.remove(idx);
            scoresList.remove(idx);
        } else {
            errOptions.setText("⚠ Sélectionnez une option à supprimer");
        }
    }

    // ══════════════════════════════════════════
    //  INNER CLASS — CARTE PERSONNALISÉE
    // ══════════════════════════════════════════

    private static class QuestionCard extends ListCell<Question> {

        private final HBox      root      = new HBox(12);
        private final StackPane avatar    = new StackPane();
        private final Label     avLetter  = new Label();
        private final VBox      info      = new VBox(5);
        private final Label     texteLbl  = new Label();
        private final HBox      botRow    = new HBox(6);
        private final Label     typeBadge = new Label();
        private final Label     qidLbl    = new Label();
        private final Label     optLbl    = new Label();
        private final Region    spacer    = new Region();
        private final VBox      rightBox  = new VBox(4);
        private final Label     nbOpts    = new Label();
        private final Label     scoreLbl  = new Label();

        QuestionCard() {
            Circle circle = new Circle(19);
            avatar.getChildren().addAll(circle, avLetter);
            avatar.setMinSize(38, 38);
            avatar.setMaxSize(38, 38);
            avLetter.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

            texteLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            texteLbl.setMaxWidth(300);
            texteLbl.setWrapText(false);
            texteLbl.setEllipsisString("…");

            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 20;");
            qidLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            optLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            optLbl.setMaxWidth(250);
            optLbl.setEllipsisString("…");

            Label dot  = new Label("·"); dot.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");
            Label dot2 = new Label("·"); dot2.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;");

            botRow.getChildren().addAll(typeBadge, dot, qidLbl, dot2, optLbl);
            botRow.setAlignment(Pos.CENTER_LEFT);

            info.getChildren().addAll(texteLbl, botRow);
            HBox.setHgrow(info, Priority.ALWAYS);

            nbOpts.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-alignment: center-right;");
            scoreLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-family: monospace; -fx-alignment: center-right;");
            rightBox.getChildren().addAll(nbOpts, scoreLbl);
            rightBox.setAlignment(Pos.CENTER_RIGHT);
            rightBox.setMinWidth(80);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(avatar, info, spacer, rightBox);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 14, 10, 14));
            root.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                    + "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-border-width: 1;");
            setStyle("-fx-background-color: transparent; -fx-padding: 3 0 3 0;");
        }

        @Override
        protected void updateItem(Question q, boolean empty) {
            super.updateItem(q, empty);
            if (empty || q == null) { setGraphic(null); return; }

            String texte = q.getTexte();
            texteLbl.setText(texte != null && !texte.isBlank() ? texte : "(sans texte)");

            String type = q.getTypeQuestion() != null ? q.getTypeQuestion().toUpperCase().trim() : "?";
            String[] colors = typeColors(type);

            avLetter.setText(type.substring(0, 1));
            avLetter.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colors[1] + ";");
            avatar.getChildren().stream()
                    .filter(n -> n instanceof Circle)
                    .forEach(n -> ((Circle) n).setFill(Color.web(colors[0])));

            typeBadge.setText(type);
            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; "
                    + "-fx-background-color: " + colors[2] + "; -fx-text-fill: " + colors[3] + "; -fx-background-radius: 20;");

            qidLbl.setText("Questionnaire #" + q.getQuestionnaireId());

            String opts = q.getOptionsQuest();
            if (opts != null && !opts.isBlank()) {
                String raw = opts.trim().replaceAll("^\\[|\\]$", "");
                String[] arr = raw.split(",");
                optLbl.setText(arr.length + " option" + (arr.length > 1 ? "s" : ""));
                nbOpts.setText(arr.length + " opt.");
            } else {
                optLbl.setText("Aucune option");
                nbOpts.setText("–");
            }

            String sc = q.getScoreOptions();
            scoreLbl.setText(sc != null && !sc.isBlank() ? sc : "");

            if (isSelected()) {
                root.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; "
                        + "-fx-border-color: #3b82f6; -fx-border-radius: 10; -fx-border-width: 1.5;");
            } else {
                root.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                        + "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-border-width: 1;");
            }

            setGraphic(root);
        }

        private String[] typeColors(String type) {
            return switch (type) {
                case "QCM"    -> new String[]{"#eff6ff","#2563eb","#dbeafe","#1d4ed8"};
                case "TEXTE"  -> new String[]{"#f0fdf4","#16a34a","#dcfce7","#15803d"};
                case "LIKERT" -> new String[]{"#fdf4ff","#9333ea","#f3e8ff","#7e22ce"};
                default       -> new String[]{"#f8fafc","#475569","#f1f5f9","#334155"};
            };
        }
    }

    // ══════════════════════════════════════════
    //  HANDLERS CRUD
    // ══════════════════════════════════════════

    @FXML
    public void handleAjouter() {
        if (!valider()) return;
        try {
            service.ajouter(buildFromForm());
            setStatus("✅ Question ajoutée avec succès !", true);
            handleReset(); loadData();
        } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void handleModifier() {
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez une question", false); return; }
        if (!valider()) return;
        try {
            Question q = buildFromForm();
            q.setQuestionId(selectedId);
            service.modifier(q);
            setStatus("✅ Question modifiée avec succès !", true);
            handleReset(); loadData();
        } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void handleSupprimer() {
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez une question", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(selectedId);
                    setStatus("✅ Question supprimée !", true);
                    handleReset(); loadData();
                } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
            }
        });
    }

    @FXML
    public void handleReset() {
        selectedId = -1;
        taTexte.clear();
        tfOptionInput.clear();
        tfScoreInput.clear();
        optionsList.clear();
        scoresList.clear();
        cbTypeQuestion.setValue(null);
        cbQuestionnaire.setValue(null);
        lblStatus.setText("");
        errTexte.setText(""); errQuestionnaire.setText("");
        errOptions.setText(""); errScores.setText(""); errTypeQuestion.setText("");
        if (tfSearch != null) tfSearch.clear();
    }

    // ══════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════

    private boolean valider() {
        errTexte.setText(""); errQuestionnaire.setText("");
        errOptions.setText(""); errScores.setText(""); errTypeQuestion.setText("");
        lblStatus.setText("");
        boolean ok = true;

        if (taTexte.getText().trim().isEmpty())         { errTexte.setText("⚠ Obligatoire"); ok = false; }
        else if (taTexte.getText().trim().length() < 5) { errTexte.setText("⚠ Minimum 5 caractères"); ok = false; }
        if (cbQuestionnaire.getValue() == null)         { errQuestionnaire.setText("⚠ Sélectionnez un questionnaire"); ok = false; }
        if (cbTypeQuestion.getValue() == null)          { errTypeQuestion.setText("⚠ Obligatoire"); ok = false; }
        if (optionsList.size() != scoresList.size())    { errOptions.setText("⚠ Nombre d'options et scores différent"); ok = false; }

        return ok;
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════

    private Question buildFromForm() {
        Question q = new Question();
        q.setTexte(taTexte.getText().trim());

        StringBuilder opts = new StringBuilder("[");
        for (int i = 0; i < optionsList.size(); i++) {
            opts.append("\"").append(optionsList.get(i)).append("\"");
            if (i < optionsList.size() - 1) opts.append(",");
        }
        opts.append("]");

        StringBuilder scores = new StringBuilder("[");
        for (int i = 0; i < scoresList.size(); i++) {
            scores.append(scoresList.get(i));
            if (i < scoresList.size() - 1) scores.append(",");
        }
        scores.append("]");

        q.setOptionsQuest(opts.toString());
        q.setScoreOptions(scores.toString());
        q.setTypeQuestion(cbTypeQuestion.getValue());
        String sel = cbQuestionnaire.getValue();
        if (sel != null) q.setQuestionnaireId(Integer.parseInt(sel.split(" - ")[0].trim()));
        return q;
    }

    private void fillForm(Question q) {
        selectedId = q.getQuestionId();
        taTexte.setText(q.getTexte());

        optionsList.clear();
        scoresList.clear();

        if (q.getOptionsQuest() != null && !q.getOptionsQuest().isBlank()) {
            String raw = q.getOptionsQuest().trim().replaceAll("^\\[|\\]$", "");
            for (String opt : raw.split(",")) {
                String clean = opt.trim().replaceAll("^\"|\"$", "");
                if (!clean.isEmpty()) optionsList.add(clean);
            }
        }

        if (q.getScoreOptions() != null && !q.getScoreOptions().isBlank()) {
            String raw = q.getScoreOptions().trim().replaceAll("^\\[|\\]$", "");
            for (String sc : raw.split(",")) {
                String clean = sc.trim();
                if (!clean.isEmpty()) scoresList.add(clean);
            }
        }

        cbTypeQuestion.setValue(q.getTypeQuestion());
        for (String item : cbQuestionnaire.getItems()) {
            if (item.startsWith(q.getQuestionnaireId() + " - ")) {
                cbQuestionnaire.setValue(item); break;
            }
        }
        setStatus("📌 Question sélectionnée", true);
        listQuestion.refresh();
    }

    private void loadData() {
        try {
            data.setAll(service.afficher());
            currentPage = 0;
            updatePage();
        } catch (SQLException e) { setStatus("❌ Erreur chargement : " + e.getMessage(), false); }
    }

    private void loadQuestionnaires() {
        try {
            List<Questionnaire> list = qService.afficher();
            ObservableList<String> items = FXCollections.observableArrayList();
            for (Questionnaire q : list) items.add(q.getQuestionnaireId() + " - " + q.getNom());
            cbQuestionnaire.setItems(items);
        } catch (SQLException e) { setStatus("❌ Erreur chargement questionnaires", false); }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (success ? "#22c55e" : "#ef4444") + ";");
    }
}

