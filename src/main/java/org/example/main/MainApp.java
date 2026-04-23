package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import org.example.services.RappelRendezVousService;

public class MainApp extends Application {
    private static Stage primaryStage;
    private RappelRendezVousService rappelService;  // ← AJOUTÉ

    @Override
    public void start(Stage stage) throws Exception {
        // Démarrer le service de rappel ✅ AJOUTÉ
        rappelService = new RappelRendezVousService();

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

        // ✅ AJOUTÉ : Arrêter le service à la fermeture de l'application
        stage.setOnCloseRequest(e -> {
            if (rappelService != null) {
                rappelService.arreterScheduler();
            }
        });
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

    // ✅ AJOUTÉ : Méthode pour arrêter proprement le service
    @Override
    public void stop() {
        if (rappelService != null) {
            rappelService.arreterScheduler();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}