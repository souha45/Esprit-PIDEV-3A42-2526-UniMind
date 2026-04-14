package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Charger le fichier FXML de login
        Parent root = FXMLLoader.load(getClass().getResource("/auth/Login.fxml"));

        // Configurer la scène
        Scene scene = new Scene(root, 1200, 800);

        // Configurer et afficher la fenêtre
        primaryStage.setTitle("Connexion - Unimind");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
