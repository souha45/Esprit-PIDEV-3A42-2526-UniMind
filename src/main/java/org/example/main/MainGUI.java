package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le fichier FXML de l'affichage des disponibilités
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        Scene scene = new Scene(loader.load(), 950, 550);

        // Titre de la fenêtre
        primaryStage.setTitle("Unimind - Gestion des disponibilités (Psychologue)");

        // Appliquer un style CSS optionnel (si tu veux personnaliser)
        // scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // Afficher un message dans la console pour confirmer
        System.out.println("Application démarrée avec succès !");
    }

    public static void main(String[] args) {
        // Lancer l'application JavaFX
        launch(args);
    }
}