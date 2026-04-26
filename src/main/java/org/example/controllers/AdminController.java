package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnQuestionnaire;
    @FXML private Button btnQuestion;
    @FXML private Button btnReponse;
    @FXML private Button btnStats;
    @FXML private Button btnGenerateurIA; // ✅ nouveau bouton

    private static final String ACTIVE   = "-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 18; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String INACTIVE = "-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-padding: 7 18; -fx-background-radius: 8; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showQuestionnaire();
    }

    @FXML public void showQuestionnaire() {
        loadView("/fxml/QuestionnaireView.fxml");
        setActive(btnQuestionnaire, btnQuestion, btnReponse, btnStats, btnGenerateurIA);
    }

    @FXML public void showQuestion() {
        loadView("/fxml/QuestionView.fxml");
        setActive(btnQuestion, btnQuestionnaire, btnReponse, btnStats, btnGenerateurIA);
    }

    @FXML public void showReponse() {
        loadView("/fxml/ReponseView.fxml");
        setActive(btnReponse, btnQuestionnaire, btnQuestion, btnStats, btnGenerateurIA);
    }

    @FXML public void showStats() {
        loadView("/fxml/StatistiquesView.fxml");
        setActive(btnStats, btnQuestionnaire, btnQuestion, btnReponse, btnGenerateurIA);
    }

    // ✅ Nouvelle méthode pour la page Générateur IA
    @FXML public void showGenerateurIA() {
        loadView("/fxml/GenerateurQuestionsIAView.fxml");
        setActive(btnGenerateurIA, btnQuestionnaire, btnQuestion, btnReponse, btnStats);
    }

    @FXML public void goToEtudiant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EtudiantView.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.scene.Scene scene = contentArea.getScene();
            scene.setRoot(view);
        } catch (Exception e) {
            System.out.println("Erreur navigation: " + e.getMessage());
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur chargement: " + fxmlPath + " → " + e.getMessage());
        }
    }

    private void setActive(Button active, Button... inactives) {
        active.setStyle(ACTIVE);
        for (Button b : inactives) b.setStyle(INACTIVE);
    }
}