package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnQuestionnaire;
    @FXML private Button btnQuestion;
    @FXML private Button btnReponse;

    private static final String ACTIVE   = "-fx-background-color: #7c8dfc; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 18; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE = "-fx-background-color: #1e2436; -fx-text-fill: #8892a4; -fx-font-weight: bold; -fx-padding: 7 18; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showQuestionnaire();
    }

    @FXML
    public void showQuestionnaire() {
        loadView("/fxml/QuestionnaireView.fxml");
        setActive(btnQuestionnaire, btnQuestion, btnReponse);
    }

    @FXML
    public void showQuestion() {
        loadView("/fxml/QuestionView.fxml");
        setActive(btnQuestion, btnQuestionnaire, btnReponse);
    }

    @FXML
    public void showReponse() {
        loadView("/fxml/ReponseView.fxml");
        setActive(btnReponse, btnQuestionnaire, btnQuestion);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Pane view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.out.println("Erreur chargement vue : " + fxmlPath + " → " + e.getMessage());
        }
    }

    private void setActive(Button active, Button... inactives) {
        active.setStyle(ACTIVE);
        for (Button b : inactives) b.setStyle(INACTIVE);
    }
}