package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.entities.Questionnaire;
import org.example.services.QuestionnaireServices;
import org.example.services.ReponseQuestionnaireServices;
import org.example.utils.LimiteQuestionnaire;
import org.example.utils.NavigationContext;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantQuestionnairesController implements Initializable {

    @FXML private ListView<Questionnaire> listQuestionnaire;
    @FXML private TextField tfSearch;
    @FXML private Label lblStatus;

    private final QuestionnaireServices service = new QuestionnaireServices();
    private final ReponseQuestionnaireServices reponseService = new ReponseQuestionnaireServices();
    private final ObservableList<Questionnaire> data = FXCollections.observableArrayList();
    private FilteredList<Questionnaire> filtered;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filtered = new FilteredList<>(data, p -> true);
        listQuestionnaire.setItems(filtered);
        listQuestionnaire.setCellFactory(lv -> new QuestionnaireCard());

        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, old, val) -> {
                String lower = val == null ? "" : val.toLowerCase().trim();
                filtered.setPredicate(q ->
                        lower.isEmpty()
                                || q.getNom().toLowerCase().contains(lower)
                                || q.getType().toString().toLowerCase().contains(lower)
                                || q.getCode().toLowerCase().contains(lower)
                );
            });
        }

        afficherPassagesRestants();
        loadData();
    }

    private void afficherPassagesRestants() {
        try {
            int userId = LimiteQuestionnaire.getInstance().getUserId();
            int passages = reponseService.countPassagesAujourdhui(userId);
            int restants = 2 - passages;

            if (restants <= 0) {
                setStatus("🚫 Vous avez atteint la limite de 2 questionnaires aujourd'hui. Revenez demain !", false);
            } else if (restants == 1) {
                setStatus("⚠️ Il vous reste 1 questionnaire à passer aujourd'hui.", true);
            } else {
                setStatus("✅ Vous pouvez passer " + restants + " questionnaires aujourd'hui.", true);
            }
        } catch (SQLException e) {
            System.out.println("Erreur vérification limite : " + e.getMessage());
        }
    }

    @FXML
    public void handleRepondre() {
        Questionnaire selected = listQuestionnaire.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠️ Veuillez sélectionner un questionnaire !", false);
            return;
        }

        try {
            int userId = LimiteQuestionnaire.getInstance().getUserId();
            if (!reponseService.peutPasser(userId)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Limite atteinte");
                alert.setHeaderText("🚫 Limite quotidienne atteinte");
                alert.setContentText(
                        "Vous avez déjà passé 2 questionnaires aujourd'hui.\n" +
                                "Revenez demain pour continuer !");
                alert.showAndWait();
                return;
            }
        } catch (SQLException e) {
            setStatus("❌ Erreur vérification limite : " + e.getMessage(), false);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/EtudiantRepondreView.fxml"));
            javafx.scene.Parent view = loader.load();

            EtudiantRepondreController controller = loader.getController();
            controller.setQuestionnaire(selected);

            // ── Navigation via NavigationContext ──
            NavigationContext.loadContentInCenter(view);

        } catch (Exception e) {
            setStatus("❌ Erreur navigation : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════
    //  CARTE QUESTIONNAIRE
    // ══════════════════════════════════════════

    private static class QuestionnaireCard extends ListCell<Questionnaire> {

        private final HBox      root      = new HBox(14);
        private final StackPane avatar    = new StackPane();
        private final Label     avLetter  = new Label();
        private final VBox      info      = new VBox(5);
        private final Label     nomLbl    = new Label();
        private final HBox      botRow    = new HBox(6);
        private final Label     typeBadge = new Label();
        private final Label     codeLbl   = new Label();
        private final Region    spacer    = new Region();
        private final VBox      rightBox  = new VBox(4);
        private final Label     nbreLbl   = new Label();
        private final Label     nbreText  = new Label("questions");

        QuestionnaireCard() {
            Circle circle = new Circle(22);
            avatar.getChildren().addAll(circle, avLetter);
            avatar.setMinSize(44, 44);
            avatar.setMaxSize(44, 44);
            avLetter.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

            nomLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            nomLbl.setWrapText(false);

            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-padding: 2 10 2 10; -fx-background-radius: 20;");
            codeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            Label dot = new Label("·");
            dot.setStyle("-fx-text-fill: #cbd5e1;");

            botRow.getChildren().addAll(typeBadge, dot, codeLbl);
            botRow.setAlignment(Pos.CENTER_LEFT);
            info.getChildren().addAll(nomLbl, botRow);
            HBox.setHgrow(info, Priority.ALWAYS);

            nbreLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #7c3aed; -fx-alignment: center;");
            nbreText.setStyle("-fx-font-size: 10px; -fx-text-fill: #a78bfa; -fx-alignment: center;");
            rightBox.getChildren().addAll(nbreLbl, nbreText);
            rightBox.setAlignment(Pos.CENTER);
            rightBox.setMinWidth(60);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(avatar, info, spacer, rightBox);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 16, 12, 16));
            root.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-border-color: #ede9fe; -fx-border-radius: 12; -fx-border-width: 1;");
            setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
        }

        @Override
        protected void updateItem(Questionnaire q, boolean empty) {
            super.updateItem(q, empty);
            if (empty || q == null) { setGraphic(null); return; }

            nomLbl.setText(q.getNom());
            codeLbl.setText(q.getCode());

            String type = q.getType() != null ? q.getType().toString() : "?";
            String[] colors = typeColors(type);

            avLetter.setText(type.substring(0, 1));
            avLetter.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + colors[1] + ";");
            avatar.getChildren().stream()
                    .filter(n -> n instanceof Circle)
                    .forEach(n -> ((Circle) n).setFill(Color.web(colors[0])));

            typeBadge.setText(type);
            typeBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-padding: 2 10 2 10; -fx-background-color: " + colors[2] +
                    "; -fx-text-fill: " + colors[3] + "; -fx-background-radius: 20;");

            nbreLbl.setText(String.valueOf(q.getNbreQuestions()));

            if (isSelected()) {
                root.setStyle("-fx-background-color: #f5f3ff; -fx-background-radius: 12; " +
                        "-fx-border-color: #7c3aed; -fx-border-radius: 12; -fx-border-width: 2;");
            } else {
                root.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-border-color: #ede9fe; -fx-border-radius: 12; -fx-border-width: 1;");
            }
            setGraphic(root);
        }

        private String[] typeColors(String type) {
            switch (type) {
                case "STRESS":     return new String[]{"#fef3c7", "#d97706", "#fde68a", "#92400e"};
                case "ANXIETE":    return new String[]{"#ede9fe", "#7c3aed", "#ddd6fe", "#4c1d95"};
                case "DEPRESSION": return new String[]{"#fee2e2", "#dc2626", "#fecaca", "#991b1b"};
                case "SOMMEIL":    return new String[]{"#e0f2fe", "#0284c7", "#bae6fd", "#075985"};
                case "BIEN_ETRE":  return new String[]{"#dcfce7", "#16a34a", "#bbf7d0", "#14532d"};
                default:           return new String[]{"#f1f5f9", "#475569", "#e2e8f0", "#334155"};
            }
        }
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
        if (lblStatus != null) {
            lblStatus.setText(msg);
            lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                    + (success ? "#22c55e" : "#ef4444") + ";");
        }
    }
}