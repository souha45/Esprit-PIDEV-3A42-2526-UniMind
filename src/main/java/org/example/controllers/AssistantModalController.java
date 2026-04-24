package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.services.GeminiService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AssistantModalController {

    // ── FXML ────────────────────────────────────────────────────────
    @FXML private VBox       chatContainer;
    @FXML private TextField  txtMessageChat;
    @FXML private Button     btnEnvoyerChat;
    @FXML private Button     btnReinitialiser;
    @FXML private Button     btnFermer;
    @FXML private ScrollPane scrollPaneChat;
    @FXML private HBox       boxTyping;
    @FXML private HBox       boxSuggestions;
    @FXML private Label      lblStatutConnexion;

    // Suggestions rapides
    @FXML private Button btnSugg1;
    @FXML private Button btnSugg2;
    @FXML private Button btnSugg3;

    // ── Données ─────────────────────────────────────────────────────
    private GeminiService  geminiService;
    private StringBuilder  historique;
    private Stage          modalStage;
    private boolean        enAttente = false;

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        geminiService = new GeminiService();
        historique    = new StringBuilder();

        // ── Envoi message ────────────────────────────────────────────
        btnEnvoyerChat.setOnAction(e -> envoyerMessage());
        txtMessageChat.setOnAction(e -> envoyerMessage());

        // ── Suggestions rapides ──────────────────────────────────────
        btnSugg1.setOnAction(e -> envoyerSuggestion("Je me sens stressé en ce moment"));
        btnSugg2.setOnAction(e -> envoyerSuggestion("J'ai des troubles du sommeil"));
        btnSugg3.setOnAction(e -> envoyerSuggestion("Donne-moi des conseils pour le bien-être"));

        // Hover suggestions
        styliserSuggestions(btnSugg1, btnSugg2, btnSugg3);

        // ── Hover bouton envoyer ─────────────────────────────────────
        btnEnvoyerChat.setOnMouseEntered(e ->
                btnEnvoyerChat.setStyle(btnEnvoyerChat.getStyle().replace("#7c3aed","#6d28d9")));
        btnEnvoyerChat.setOnMouseExited(e ->
                btnEnvoyerChat.setStyle(btnEnvoyerChat.getStyle().replace("#6d28d9","#7c3aed")));

        // ── Autres boutons ───────────────────────────────────────────
        btnReinitialiser.setOnAction(e -> reinitialiserChat());
        btnFermer.setOnAction(e -> fermer());
        btnFermer.setOnMouseEntered(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("0.15","0.28")));
        btnFermer.setOnMouseExited(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("0.28","0.15")));

        // ── Message de bienvenue ─────────────────────────────────────
        ajouterMessageAssistant(
                "👋 Bonjour ! Je suis votre assistant bien-être Unimind.\n\n" +
                        "Je suis là pour vous écouter, comprendre votre état et vous donner " +
                        "des conseils pratiques.\n\n" +
                        "💬 Vous pouvez me parler librement — tout reste confidentiel.");
    }

    // ── Envoi message ────────────────────────────────────────────────
    private void envoyerMessage() {
        if (enAttente) return;
        String message = txtMessageChat.getText().trim();
        if (message.isEmpty()) return;

        txtMessageChat.clear();
        cacherSuggestions();
        ajouterMessageUtilisateur(message);
        afficherTyping(true);
        bloquerSaisie(true);

        new Thread(() -> {
            try {
                String reponse = geminiService.envoyerMessage(message, historique.toString());
                historique.append("Étudiant : ").append(message).append("\n");
                historique.append("Assistant : ").append(reponse).append("\n\n");

                Platform.runLater(() -> {
                    afficherTyping(false);
                    ajouterMessageAssistant(reponse);
                    bloquerSaisie(false);
                    txtMessageChat.requestFocus();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    afficherTyping(false);
                    ajouterMessageAssistant(
                            "❌ Désolé, je rencontre une difficulté technique. " +
                                    "Veuillez réessayer dans un instant.");
                    bloquerSaisie(false);
                });
                ex.printStackTrace();
            }
        }).start();
    }

    private void envoyerSuggestion(String texte) {
        txtMessageChat.setText(texte);
        envoyerMessage();
    }

    // ── Bulles de messages ───────────────────────────────────────────

    /** Bulle utilisateur — droite, violet */
    private void ajouterMessageUtilisateur(String message) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(270);
        bubble.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 18 18 4 18; -fx-padding: 10 14;");

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(250);
        lblMsg.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #ffffff;");

        Label lblHeure = new Label(heureActuelle());
        lblHeure.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 9px; " +
                "-fx-text-fill: rgba(255,255,255,0.65);");
        lblHeure.setAlignment(Pos.CENTER_RIGHT);

        bubble.getChildren().addAll(lblMsg, lblHeure);
        wrapper.getChildren().add(bubble);
        chatContainer.getChildren().add(wrapper);
        defilerVersBas();
    }

    /** Bulle assistant — gauche, blanc avec bordure violette */
    private void ajouterMessageAssistant(String message) {
        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        // Avatar robot
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 16px; -fx-padding: 2 0 0 0;");

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(270);
        bubble.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18 18 18 4; " +
                "-fx-padding: 10 14; -fx-border-color: #ede9fe; " +
                "-fx-border-width: 1; -fx-border-radius: 18 18 18 4;");

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(245);
        lblMsg.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #374151;");

        Label lblHeure = new Label(heureActuelle());
        lblHeure.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 9px; -fx-text-fill: #c4b5fd;");

        bubble.getChildren().addAll(lblMsg, lblHeure);
        wrapper.getChildren().addAll(avatar, bubble);
        chatContainer.getChildren().add(wrapper);
        defilerVersBas();
    }

    // ── Indicateur "en train d'écrire" ──────────────────────────────
    private void afficherTyping(boolean visible) {
        enAttente = visible;
        boxTyping.setVisible(visible);
        boxTyping.setManaged(visible);
        if (visible) defilerVersBas();
    }

    // ── Suggestions rapides ──────────────────────────────────────────
    private void cacherSuggestions() {
        boxSuggestions.setVisible(false);
        boxSuggestions.setManaged(false);
    }

    private void styliserSuggestions(Button... btns) {
        String idle  = "-fx-background-color:#ede9fe;-fx-text-fill:#6366f1;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:10px;" +
                "-fx-padding:5 10;-fx-background-radius:20;-fx-cursor:hand;";
        String hover = "-fx-background-color:#ddd6fe;-fx-text-fill:#4f46e5;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:10px;" +
                "-fx-padding:5 10;-fx-background-radius:20;-fx-cursor:hand;";
        for (Button b : btns) {
            b.setOnMouseEntered(e -> b.setStyle(hover));
            b.setOnMouseExited(e  -> b.setStyle(idle));
        }
    }

    // ── Utilitaires ──────────────────────────────────────────────────
    private void bloquerSaisie(boolean bloquer) {
        txtMessageChat.setDisable(bloquer);
        btnEnvoyerChat.setDisable(bloquer);
    }

    private void defilerVersBas() {
        Platform.runLater(() -> scrollPaneChat.setVvalue(1.0));
    }

    private String heureActuelle() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void reinitialiserChat() {
        chatContainer.getChildren().clear();
        historique = new StringBuilder();

        // Remettre les suggestions
        boxSuggestions.setVisible(true);
        boxSuggestions.setManaged(true);

        ajouterMessageAssistant(
                "🔄 Conversation réinitialisée.\n\nComment puis-je vous aider aujourd'hui ?");
    }

    private void fermer() {
        if (modalStage != null) modalStage.close();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}