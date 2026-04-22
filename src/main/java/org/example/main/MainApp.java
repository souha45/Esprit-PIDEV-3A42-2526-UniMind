package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class MainApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/login.fxml")
        );

        // 🔥 Utiliser la taille de login.fxml (qui vient d'Islem)
        // Le login.fxml d'Islem fait 640x480 (défini dans son AnchorPane)
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());


        stage.setTitle("UniMind - Plateforme de santé mentale");
        stage.setResizable(true);  // ← On peut redimensionner
        stage.setScene(scene);
        stage.show();
    }
    public static void showAdminView() throws Exception {
        Parent view = FXMLLoader.load(MainApp.class.getResource("/fxml/AdminView.fxml"));
        primaryStage.getScene().setRoot(view);
        primaryStage.setTitle("UniMind — Interface Admin");
    }

    public static void showEtudiantView() throws Exception {
        Parent view = FXMLLoader.load(MainApp.class.getResource("/fxml/EtudiantView.fxml"));
        primaryStage.getScene().setRoot(view);
        primaryStage.setTitle("UniMind — Espace Étudiant");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
