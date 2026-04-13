package org.example.main;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class UnimindApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Unimind - Gestion des Traitements");

        // Ouvrir directement la vue des traitements
        ouvrirTraitementView(primaryStage);
    }

    private void ouvrirTraitementView(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-view.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("Unimind - Gestion des Traitements");
            primaryStage.setScene(new Scene(root, 1200, 800));
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setMaxWidth(1600);
            primaryStage.setMaxHeight(1200);

            // Appliquer un style moderne à la fenêtre
            primaryStage.getScene().getStylesheets().add(
                    getClass().getResource("/css/traitements-suivis.css").toExternalForm()
            );

            primaryStage.show();
        } catch (IOException e) {
            afficherErreur("Erreur lors de l'ouverture de l'interface Traitements", e);
        }
    }

    private void afficherErreur(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
