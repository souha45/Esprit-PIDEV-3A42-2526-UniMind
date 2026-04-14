package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showAdminView();
    }

    public static void showAdminView() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/AdminView.fxml"));
        BorderPane root = loader.load();
        Scene scene = new Scene(root, 1200, 750);
        primaryStage.setTitle("UniMind — Interface Admin 👨‍💼");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void showEtudiantView() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/EtudiantView.fxml"));
        BorderPane root = loader.load();
        Scene scene = new Scene(root, 1200, 750);
        primaryStage.setTitle("UniMind — Interface Etudiant 👨‍🎓");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}