package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.entities.Questionnaire;
import org.example.enums.TypeQuestionnaire;
import org.example.services.QuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class QuestionnaireController implements Initializable {

    // ─── FORM ───
    @FXML private TextField tfCode, tfNom, tfSeuilLeger, tfSeuilModere, tfSeuilSevere, tfNbreQuestions;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<TypeQuestionnaire> cbType;
    @FXML private Label lblStatus;
    @FXML private Label errCode, errNom, errType;
    @FXML private Label errSeuilLeger, errSeuilModere, errSeuilSevere, errNbreQuestions;

    // ─── RECHERCHE + FILTRE + TRI ───
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private ComboBox<String> cbTri;
    @FXML private Label lblCompteur;

    // ─── LIST ───
    @FXML private ListView<Questionnaire> listQuestionnaire;

    private final QuestionnaireServices service = new QuestionnaireServices();
    private final ObservableList<Questionnaire> data = FXCollections.observableArrayList();
    private FilteredList<Questionnaire> filtered;
    private SortedList<Questionnaire> sorted;
    private int selectedId = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ─── FilteredList + SortedList ───
        filtered = new FilteredList<>(data, p -> true);
        sorted   = new SortedList<>(filtered);
        listQuestionnaire.setItems(sorted);
        listQuestionnaire.setCellFactory(lv -> new QuestionnaireCard());

        // ─── ComboBox form ───
        cbType.setItems(FXCollections.observableArrayList(TypeQuestionnaire.values()));

        // ─── ComboBox filtre type ───
        ObservableList<String> types = FXCollections.observableArrayList("Tous");
        for (TypeQuestionnaire t : TypeQuestionnaire.values()) types.add(t.name());
        cbFiltreType.setItems(types);
        cbFiltreType.setValue("Tous");

        // ─── ComboBox tri ───
        cbTri.setItems(FXCollections.observableArrayList(
                "Par défaut",
                "Nom A→Z",
                "Nom Z→A",
                "Nb Questions ↑",
                "Nb Questions ↓",
                "Seuil Légère ↑",
                "Seuil Légère ↓"
        ));
        cbTri.setValue("Par défaut");

        // ─── Listeners ───
        tfSearch.textProperty().addListener((obs, old, val) -> appliquerFiltre());
        cbFiltreType.valueProperty().addListener((obs, old, val) -> appliquerFiltre());
        cbTri.valueProperty().addListener((obs, old, val) -> appliquerTri());

        // ─── Clic liste ───
        listQuestionnaire.setOnMouseClicked(e -> {
            Questionnaire selected = listQuestionnaire.getSelectionModel().getSelectedItem();
            if (selected != null) fillForm(selected);
        });

        loadData();
    }

    // ══════════════════════════════════════════
    //  FILTRE — recherche + type
    // ══════════════════════════════════════════
    private void appliquerFiltre() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
        String type   = cbFiltreType.getValue();

        filtered.setPredicate(q -> {
            boolean matchSearch = search.isEmpty()
                    || q.getNom().toLowerCase().contains(search)
                    || q.getCode().toLowerCase().contains(search)
                    || (q.getType() != null && q.getType().name().toLowerCase().contains(search));

            boolean matchType = type == null || type.equals("Tous")
                    || (q.getType() != null && q.getType().name().equals(type));

            return matchSearch && matchType;
        });

        updateCompteur();
    }

    // ══════════════════════════════════════════
    //  TRI — appliqué SUR les résultats filtrés
    // ══════════════════════════════════════════
    private void appliquerTri() {
        String tri = cbTri.getValue();
        if (tri == null) return;

        Comparator<Questionnaire> comparator = switch (tri) {
            case "Nom A→Z"         -> Comparator.comparing(Questionnaire::getNom, String.CASE_INSENSITIVE_ORDER);
            case "Nom Z→A"         -> Comparator.comparing(Questionnaire::getNom, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Nb Questions ↑"  -> Comparator.comparingInt(Questionnaire::getNbreQuestions);
            case "Nb Questions ↓"  -> Comparator.comparingInt(Questionnaire::getNbreQuestions).reversed();
            case "Seuil Légère ↑"  -> Comparator.comparingInt(Questionnaire::getSeuilLegere);
            case "Seuil Légère ↓"  -> Comparator.comparingInt(Questionnaire::getSeuilLegere).reversed();
            default                -> null; // Par défaut = pas de tri
        };

        sorted.setComparator(comparator);
    }

    private void updateCompteur() {
        if (lblCompteur != null)
            lblCompteur.setText(filtered.size() + " / " + data.size() + " questionnaire(s)");
    }

    @FXML
    public void handleResetFiltre() {
        tfSearch.clear();
        cbFiltreType.setValue("Tous");
        cbTri.setValue("Par défaut");
        sorted.setComparator(null);
    }

    // ══════════════════════════════════════════
    //  CARTE QUESTIONNAIRE
    // ══════════════════════════════════════════
    private static class QuestionnaireCard extends ListCell<Questionnaire> {

        private final HBox      root      = new HBox(14);
        private final StackPane avatar    = new StackPane();
        private final Label     avLetter  = new Label();
        private final VBox      info      = new VBox(4);
        private final Label     nomLbl    = new Label();
        private final HBox      botRow    = new HBox(6);
        private final Label     typeBadge = new Label();
        private final Label     codeLbl   = new Label();
        private final Region    spacer    = new Region();
        private final VBox      rightBox  = new VBox(2);
        private final Label     nbreLbl   = new Label();
        private final Label     nbreText  = new Label("questions");

        QuestionnaireCard() {
            Circle circle = new Circle(20);
            avatar.getChildren().addAll(circle, avLetter);
            avatar.setMinSize(40, 40);
            avatar.setMaxSize(40, 40);
            avLetter.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            nomLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            nomLbl.setWrapText(false);

            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 20;");
            codeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            Label dot = new Label("·");
            dot.setStyle("-fx-text-fill: #cbd5e1;");
            botRow.getChildren().addAll(typeBadge, dot, codeLbl);
            botRow.setAlignment(Pos.CENTER_LEFT);

            info.getChildren().addAll(nomLbl, botRow);
            HBox.setHgrow(info, Priority.ALWAYS);

            nbreLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
            nbreText.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            rightBox.getChildren().addAll(nbreLbl, nbreText);
            rightBox.setAlignment(Pos.CENTER);
            rightBox.setMinWidth(55);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(avatar, info, spacer, rightBox);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 14, 10, 14));
            root.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-border-width: 1;");
            setStyle("-fx-background-color: transparent; -fx-padding: 3 0 3 0;");
        }

        @Override
        protected void updateItem(Questionnaire q, boolean empty) {
            super.updateItem(q, empty);
            if (empty || q == null) { setGraphic(null); return; }

            nomLbl.setText(q.getNom());
            codeLbl.setText(q.getCode());
            nbreLbl.setText(String.valueOf(q.getNbreQuestions()));

            String type = q.getType() != null ? q.getType().toString() : "?";
            String[] colors = typeColors(type);

            avLetter.setText(type.substring(0, 1));
            avLetter.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + colors[1] + ";");
            avatar.getChildren().stream()
                    .filter(n -> n instanceof Circle)
                    .forEach(n -> ((Circle) n).setFill(Color.web(colors[0])));

            typeBadge.setText(type);
            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; " +
                    "-fx-background-color: " + colors[2] + "; -fx-text-fill: " + colors[3] + "; -fx-background-radius: 20;");

            if (isSelected()) {
                root.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; " +
                        "-fx-border-color: #3b82f6; -fx-border-radius: 10; -fx-border-width: 1.5;");
            } else {
                root.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                        "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-border-width: 1;");
            }
            setGraphic(root);
        }

        private String[] typeColors(String type) {
            return switch (type) {
                case "STRESS"     -> new String[]{"#fef3c7","#d97706","#fde68a","#92400e"};
                case "ANXIETE"    -> new String[]{"#ede9fe","#7c3aed","#ddd6fe","#4c1d95"};
                case "DEPRESSION" -> new String[]{"#fee2e2","#dc2626","#fecaca","#991b1b"};
                case "SOMMEIL"    -> new String[]{"#e0f2fe","#0284c7","#bae6fd","#075985"};
                case "BIEN_ETRE"  -> new String[]{"#dcfce7","#16a34a","#bbf7d0","#14532d"};
                default           -> new String[]{"#f1f5f9","#475569","#e2e8f0","#334155"};
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
            setStatus("✅ Questionnaire ajouté !", true);
            handleReset(); loadData();
        } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void handleModifier() {
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez un questionnaire", false); return; }
        if (!valider()) return;
        try {
            Questionnaire q = buildFromForm();
            q.setQuestionnaireId(selectedId);
            service.modifier(q);
            setStatus("✅ Modifié !", true);
            handleReset(); loadData();
        } catch (SQLException e) { setStatus("❌ Erreur : " + e.getMessage(), false); }
    }

    @FXML
    public void handleSupprimer() {
        if (selectedId == -1) { setStatus("⚠️ Sélectionnez un questionnaire", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce questionnaire ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(selectedId);
                    setStatus("✅ Supprimé !", true);
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
    }

    // ══════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════
    private boolean valider() {
        errCode.setText(""); errNom.setText(""); errType.setText("");
        errSeuilLeger.setText(""); errSeuilModere.setText("");
        errSeuilSevere.setText(""); errNbreQuestions.setText("");
        boolean valid = true;

        if (tfCode.getText().trim().isEmpty())
        { errCode.setText("❌ Obligatoire"); valid = false; }
        else if (tfCode.getText().trim().length() > 20)
        { errCode.setText("❌ Max 20 caractères"); valid = false; }
        if (tfNom.getText().trim().isEmpty())
        { errNom.setText("❌ Obligatoire"); valid = false; }
        if (cbType.getValue() == null)
        { errType.setText("❌ Obligatoire"); valid = false; }
        if (!isPositiveInt(tfSeuilLeger.getText()))
        { errSeuilLeger.setText("❌ Entier positif requis"); valid = false; }
        if (!isPositiveInt(tfSeuilModere.getText()))
        { errSeuilModere.setText("❌ Entier positif requis"); valid = false; }
        if (!isPositiveInt(tfSeuilSevere.getText()))
        { errSeuilSevere.setText("❌ Entier positif requis"); valid = false; }
        if (!isPositiveInt(tfNbreQuestions.getText()))
        { errNbreQuestions.setText("❌ Entier positif requis"); valid = false; }

        if (isPositiveInt(tfSeuilLeger.getText()) &&
                isPositiveInt(tfSeuilModere.getText()) &&
                isPositiveInt(tfSeuilSevere.getText())) {
            int sl = Integer.parseInt(tfSeuilLeger.getText().trim());
            int sm = Integer.parseInt(tfSeuilModere.getText().trim());
            int ss = Integer.parseInt(tfSeuilSevere.getText().trim());
            if (!(sl < sm && sm < ss))
            { errSeuilSevere.setText("❌ Seuils croissants : L < M < S"); valid = false; }
        }
        return valid;
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
        setStatus("📌 Questionnaire sélectionné", true);
        listQuestionnaire.refresh();
    }

    private void loadData() {
        try {
            List<Questionnaire> list = service.afficher();
            data.setAll(list);
            updateCompteur();
        } catch (SQLException e) { setStatus("❌ Erreur chargement : " + e.getMessage(), false); }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}
