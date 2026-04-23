package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.example.main.MainApp;

import java.net.URL;
import java.util.ResourceBundle;

public class EtudiantController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnQuestionnaires;
    @FXML private Button btnMesReponses;

    private static final String ACTIVE   = "-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 18; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE = "-fx-background-color: #3b1f6e; -fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-padding: 7 18; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showQuestionnaires();
    }

    @FXML
    public void showQuestionnaires() {
        loadView("/fxml/EtudiantQuestionnairesView.fxml");
        setActive(btnQuestionnaires, btnMesReponses);
    }

    @FXML
    public void showMesReponses() {
        loadView("/fxml/EtudiantMesReponsesView.fxml");
        setActive(btnMesReponses, btnQuestionnaires);
    }

    @FXML
    public void goToAdmin() {
        try {
            MainApp.showAdminView();
        } catch (Exception e) {
            System.out.println("Erreur navigation admin: " + e.getMessage());
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Pane view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.out.println("Erreur chargement: " + fxmlPath + " → " + e.getMessage());
        }
    }

    private void setActive(Button active, Button inactive) {
        active.setStyle(ACTIVE);
        inactive.setStyle(INACTIVE);
    }
}