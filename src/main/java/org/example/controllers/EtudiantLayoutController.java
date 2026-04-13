package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.utils.Session;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EtudiantLayoutController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private StackPane contentArea;
    @FXML private Button btnSeances;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (Session.getInstance().isLoggedIn()) {
            var user = Session.getInstance().getCurrentUser();
            lblUserName.setText(user.getPrenom() + " " + user.getNom());
        }
        showSeancesMeditation();
    }

    public StackPane getContentArea() { return contentArea; }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur chargement: " + fxmlPath);
        }
    }

    @FXML public void showSeancesMeditation() {
        loadPage("/org/example/views/EtudiantSeances.fxml");
    }

    @FXML public void showMesFavoris() {
        loadPage("/org/example/views/MesFavorisSeances.fxml");
    }

    @FXML private void showRendezVous()        { System.out.println("Mes Rendez-vous - à implémenter"); }
    @FXML private void showConsultations()     { System.out.println("Consultations - à implémenter"); }
    @FXML private void showTraitements()       { System.out.println("Traitements - à implémenter"); }
    @FXML private void showSuiviTraitements()  { System.out.println("Suivi - à implémenter"); }
    @FXML private void showQuestionnaires()    { System.out.println("Questionnaires - à implémenter"); }
    @FXML private void showMesReponses()       { System.out.println("Réponses - à implémenter"); }
    @FXML private void showEvenements()        { System.out.println("Événements - à implémenter"); }
    @FXML private void showMesEvenements()     { System.out.println("Mes Événements - à implémenter"); }
    @FXML private void showMesFavorisEvenements() { System.out.println("Mes Favoris - à implémenter"); }

    @FXML
    private void onLogout() {
        Session.getInstance().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/Login.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}