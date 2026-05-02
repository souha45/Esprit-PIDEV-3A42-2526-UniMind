package org.example.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import org.example.controllers.ResetPasswordController;
import org.example.services.RappelRendezVousService;
import org.example.services.evenement.EventStatusSchedulerService;
import org.example.utils.LocalCallbackServer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class MainApp extends Application {
    private static final Logger logger = Logger.getLogger(MainApp.class.getName());
    private static Stage primaryStage;
    private static RappelRendezVousService rappelService;  // 🔥 Changé en static
    private static EventStatusSchedulerService eventStatusScheduler;  // Scheduler pour les statuts d'événements

    @Override
    public void start(Stage stage) {
        try {
            // Démarrer le serveur d'activation
            LocalCallbackServer.startServer();
            logger.info("🚀 Démarrage du serveur d'activation...");

            // Démarrer le service de rappel de rendez-vous
            rappelService = new RappelRendezVousService();
            logger.info("🔔 Service de rappel de rendez-vous démarré...");

            // Démarrer le scheduler de mise à jour des statuts d'événements
            eventStatusScheduler = new EventStatusSchedulerService();
            eventStatusScheduler.start();
            logger.info("📅 Scheduler des statuts d'événements démarré...");

            primaryStage = stage;

            // Gestion des paramètres (token de réinitialisation)
            Parameters params = getParameters();
            if (params != null && params.getRaw() != null) {
                for (String arg : params.getRaw()) {
                    if (arg.contains("reset-password") && arg.contains("token=")) {
                        String token = extractTokenFromUrl(arg);
                        if (token != null && !token.isEmpty()) {
                            logger.info("🔐 Token reçu: " + token);
                            openResetPasswordWindow(token);
                        }
                    }
                }
            }

            // Charger le login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Scene scene = new Scene(loader.load());

            // Vérifier que les CSS existent avant de les ajouter
            String etudiantCss = getClass().getResource("/css/etudiant.css") != null
                    ? getClass().getResource("/css/etudiant.css").toExternalForm() : null;
            String adminCss = getClass().getResource("/css/admin.css") != null
                    ? getClass().getResource("/css/admin.css").toExternalForm() : null;

            if (etudiantCss != null) {
                scene.getStylesheets().add(etudiantCss);
            }
            if (adminCss != null) {
                scene.getStylesheets().add(adminCss);
            }

            stage.setTitle("UniMind - Plateforme de santé mentale");
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();

            // Arrêter les services à la fermeture
            stage.setOnCloseRequest(e -> {
                if (rappelService != null) {
                    rappelService.arreterScheduler();
                }
                if (eventStatusScheduler != null) {
                    eventStatusScheduler.stop();
                }
            });

        } catch (Exception e) {
            logger.severe("❌ Erreur au démarrage: " + e.getMessage());
            e.printStackTrace();
        }
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
            logger.warning("Erreur extraction token: " + e.getMessage());
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
                logger.severe("❌ Erreur reset password: " + e.getMessage());
            }
        });
    }

    // 🔥 CORRECTION : Ajouter throws Exception et vérifier primaryStage
    public static void showAdminView() throws Exception {
        if (primaryStage == null) {
            throw new Exception("PrimaryStage n'est pas initialisé");
        }
        Parent view = FXMLLoader.load(MainApp.class.getResource("/fxml/AdminView.fxml"));
        primaryStage.getScene().setRoot(view);
        primaryStage.setTitle("UniMind — Interface Admin");
    }

    // 🔥 CORRECTION : Ajouter throws Exception et vérifier primaryStage
    public static void showEtudiantView() throws Exception {
        if (primaryStage == null) {
            throw new Exception("PrimaryStage n'est pas initialisé");
        }
        Parent view = FXMLLoader.load(MainApp.class.getResource("/fxml/EtudiantView.fxml"));
        primaryStage.getScene().setRoot(view);
        primaryStage.setTitle("UniMind — Espace Étudiant");
    }

    @Override
    public void stop() {
        // Arrêter le serveur d'activation
        LocalCallbackServer.stopServer();
        // Arrêter le service de rappel
        if (rappelService != null) {
            rappelService.arreterScheduler();
        }
        // Arrêter le scheduler des statuts d'événements
        if (eventStatusScheduler != null) {
            eventStatusScheduler.stop();
        }
        logger.info("🛑 Services arrêtés");
    }

    public static void main(String[] args) {
        launch(args);
    }
}