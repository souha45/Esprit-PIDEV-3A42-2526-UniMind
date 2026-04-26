package org.example.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import org.example.controllers.ResetPasswordController;
import org.example.utils.LocalCallbackServer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MainApp extends Application {
    private static Stage primaryStage;
    private static boolean serverStarted = false;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        Parameters params = getParameters();
        if (params != null && params.getRaw() != null) {
            for (String arg : params.getRaw()) {
                if (arg.contains("reset-password") && arg.contains("token=")) {
                    String token = extractTokenFromUrl(arg);
                    if (token != null && !token.isEmpty()) {
                        System.out.println("🔐 Token reçu: " + token);
                        openResetPasswordWindow(token);
                    }
                }
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());

        stage.setTitle("UniMind - Plateforme de santé mentale");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    private String extractTokenFromUrl(String url) {
        try {
            if (url.contains("token=")) {
                String[] parts = url.split("token=");
                if (parts.length > 1) {
                    String token = parts[1];
                    if (token.contains("&")) {
                        token = token.split("&")[0];
                    }
                    return URLDecoder.decode(token, StandardCharsets.UTF_8.name());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void openResetPasswordWindow(String token) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/reset_password.fxml"));
                Stage resetStage = new Stage();
                resetStage.setScene(new Scene(loader.load()));
                resetStage.setTitle("UniMind - Réinitialisation du mot de passe");
                resetStage.setResizable(false);
                resetStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                resetStage.initOwner(primaryStage);

                ResetPasswordController controller = loader.getController();
                if (controller != null) {
                    controller.setResetToken(token);
                }

                resetStage.showAndWait();
            } catch (Exception e) {
                System.err.println("❌ Erreur reset password: " + e.getMessage());
                e.printStackTrace();
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

    @Override
    public void stop() throws Exception {
        // Arrêter le serveur proprement à la fermeture de l'application
        LocalCallbackServer.stopServer();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}