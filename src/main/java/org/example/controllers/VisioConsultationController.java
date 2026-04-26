package org.example.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class VisioConsultationController {

    @FXML private WebView webView;
    @FXML private Button btnFermer;
    @FXML private Label lblTitre;
    @FXML private Label lblStatut;
    @FXML private Label lblDuree;

    private WebEngine webEngine;
    private Stage stage;
    private Timeline chronometre;
    private int secondes = 0;
    private Runnable onFermerCallback;

    @FXML
    public void initialize() {
        webEngine = webView.getEngine();

        btnFermer.setOnAction(e -> fermer());

        // Démarrer le chronomètre
        demarrerChronometre();
    }

    /**
     * Charge la salle Jitsi
     */
    public void chargerSalle(String lienVisio, String titre, boolean estPsychologue) {
        lblTitre.setText(titre);
        webEngine.load(lienVisio);

        // Changer le statut
        lblStatut.setText("🟢 En ligne - En attente...");

        // Demander l'accès à la caméra/micro (avec vérification)
        webEngine.executeScript(
                "if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {" +
                "  navigator.mediaDevices.getUserMedia({ audio: true, video: true })" +
                "    .then(stream => { console.log('✅ Caméra et micro activés'); })" +
                "    .catch(err => { console.log('❌ Erreur: ' + err); });" +
                "} else {" +
                "  console.log('❌ navigator.mediaDevices.getUserMedia non supporté');" +
                "}"
        );
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnFermerCallback(Runnable callback) {
        this.onFermerCallback = callback;
    }

    private void demarrerChronometre() {
        chronometre = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondes++;
            long minutes = secondes / 60;
            long secs = secondes % 60;
            Platform.runLater(() -> lblDuree.setText(String.format("Durée: %02d:%02d", minutes, secs)));
        }));
        chronometre.setCycleCount(Timeline.INDEFINITE);
        chronometre.play();
    }

    private void fermer() {
        if (chronometre != null) {
            chronometre.stop();
        }

        if (onFermerCallback != null) {
            onFermerCallback.run();
        }

        if (stage != null) {
            stage.close();
        }
    }
}