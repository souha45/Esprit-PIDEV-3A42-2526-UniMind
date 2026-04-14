package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.example.entities.Questionnaire;
import org.example.services.QuestionnaireServices;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantQuestionnairesController implements Initializable {

    @FXML private TableView<Questionnaire> tableQuestionnaire;
    @FXML private TableColumn<Questionnaire, Integer> colId, colNbre;
    @FXML private TableColumn<Questionnaire, String>  colCode, colNom, colType;
    @FXML private Label lblStatus;

    private final QuestionnaireServices service = new QuestionnaireServices();
    private final ObservableList<Questionnaire> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("questionnaireId"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colNbre.setCellValueFactory(new PropertyValueFactory<>("nbreQuestions"));
        tableQuestionnaire.setItems(data);
        loadData();
    }

    @FXML
    public void handleRepondre() {
        Questionnaire selected = tableQuestionnaire.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠️ Veuillez sélectionner un questionnaire !", false);
            return;
        }

        try {
            // ✅ Charger la vue de réponse
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/EtudiantRepondreView.fxml"));
            Pane view = loader.load();

            // ✅ Passer le questionnaire au controller
            EtudiantRepondreController controller = loader.getController();
            controller.setQuestionnaire(selected);

            // ✅ Naviguer vers la vue de réponse
            StackPane contentArea = (StackPane) tableQuestionnaire
                    .getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            setStatus("❌ Erreur navigation : " + e.getMessage(), false);
            e.printStackTrace();
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
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}