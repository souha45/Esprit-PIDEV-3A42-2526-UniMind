package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.utils.Session;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminLayoutController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnStats;
    @FXML private Button btnUsers;
    @FXML private Button btnSeances;
    @FXML private Button btnEvenements;
    @FXML private Button btnSponsors;
    @FXML private Button btnQuestionnaires;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load default page: Séances de méditation
        showSeances();
    }

    private void setActiveButton(Button active) {
        Button[] allButtons = {btnStats, btnUsers, btnSeances, btnEvenements, btnSponsors, btnQuestionnaires};
        for (Button btn : allButtons) {
            btn.getStyleClass().remove("nav-btn-active");
        }
        if (!active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur chargement page: " + fxmlPath);
        }
    }

    @FXML
    private void showStats() {
        setActiveButton(btnStats);
        loadPage("/org/example/views/Stats.fxml");
    }

    @FXML
    private void showUsers() {
        setActiveButton(btnUsers);
        // loadPage("/org/example/views/GestionUtilisateurs.fxml");
        System.out.println("Gestion Utilisateurs - page à implémenter");
    }

    @FXML
    public void showSeances() {
        setActiveButton(btnSeances);
        loadPage("/org/example/views/CategorieMeditation.fxml");
    }

    @FXML
    private void showEvenements() {
        setActiveButton(btnEvenements);
        // loadPage("/org/example/views/Evenements.fxml");
        System.out.println("Évènements - page à implémenter");
    }

    @FXML
    private void showSponsors() {
        setActiveButton(btnSponsors);
        // loadPage("/org/example/views/Sponsors.fxml");
        System.out.println("Sponsors - page à implémenter");
    }

    @FXML
    private void showQuestionnaires() {
        setActiveButton(btnQuestionnaires);
        // loadPage("/org/example/views/Questionnaires.fxml");
        System.out.println("Questionnaires - page à implémenter");
    }

    @FXML
    private void onLogout() {
        Session.getInstance().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
