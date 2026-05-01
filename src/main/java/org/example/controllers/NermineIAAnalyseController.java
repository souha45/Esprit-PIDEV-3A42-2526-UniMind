package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.Questionnaire;
import org.example.entities.User;
import org.example.services.OpenAIService;
import org.example.utils.NavigationContext;

public class NermineIAAnalyseController {

    // ── FXML ─────────────────────────────────────────────────────────
    @FXML private Label  lblTitreQuestionnaire;
    @FXML private Label  lblScore;
    @FXML private Label  lblNiveau;
    @FXML private Label  lblInterpretation;

    @FXML private VBox   boxChargement;
    @FXML private VBox   boxResultat;
    @FXML private Label  lblErreur;

    @FXML private TextArea taAnalyse;

    @FXML private Button btnRetour;
    @FXML private Button btnMesReponses;
    @FXML private Button btnCopier;

    // ── Données reçues ────────────────────────────────────────────────
    private Questionnaire questionnaire;
    private double        score;
    private String        niveau;
    private String        interpretation;
    private String        reponsesJson;
    private User          utilisateur;

    // ════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        boxChargement.setVisible(true);
        boxChargement.setManaged(true);
        boxResultat.setVisible(false);
        boxResultat.setManaged(false);
        lblErreur.setVisible(false);
        lblErreur.setManaged(false);

        taAnalyse.setEditable(false);
        taAnalyse.setWrapText(true);

        btnRetour.setOnAction(e -> handleRetour());
        btnMesReponses.setOnAction(e -> handleMesReponses());
        btnCopier.setOnAction(e -> handleCopier());

        styliserBouton(btnRetour,      "#6b7280", "#4b5563");
        styliserBouton(btnMesReponses, "#7c3aed", "#6d28d9");
        styliserBouton(btnCopier,      "#059669", "#047857");
    }

    // ════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ════════════════════════════════════════════════════════════════

    public void setDonnees(
            Questionnaire questionnaire,
            double        score,
            String        niveau,
            String        interpretation,
            String        reponsesJson,
            User          user) {

        this.questionnaire  = questionnaire;
        this.score          = score;
        this.niveau         = niveau;
        this.interpretation = interpretation;
        this.reponsesJson   = reponsesJson;
        this.utilisateur    = user;

        afficherInfosQuestionnaire();
        lancerAnalyseIA();
    }

    // ════════════════════════════════════════════════════════════════
    //  AFFICHAGE INFOS
    // ════════════════════════════════════════════════════════════════

    private void afficherInfosQuestionnaire() {
        if (questionnaire == null) return;

        lblTitreQuestionnaire.setText("📋 " + questionnaire.getNom());
        lblScore.setText("🎯 Score : " + String.format("%.0f", score));

        String niveauAffiche = switch (niveau != null ? niveau.toLowerCase() : "") {
            case "leger", "legere" -> "🟢 Légère";
            case "modere" -> "🟡 Modérée";
            case "severe" -> "🔴 Sévère";
            default       -> "📊 " + (niveau != null ? niveau : "—");
        };
        lblNiveau.setText(niveauAffiche);

        String couleurNiveau = switch (niveau != null ? niveau.toLowerCase() : "") {
            case "leger", "legere" -> "#10b981";
            case "modere" -> "#f59e0b";
            case "severe" -> "#ef4444";
            default       -> "#6b7280";
        };
        lblNiveau.setStyle(lblNiveau.getStyle() + " -fx-text-fill: " + couleurNiveau + ";");
        lblInterpretation.setText(interpretation != null ? interpretation : "");
    }

    // ════════════════════════════════════════════════════════════════
    //  ANALYSE IA EN ARRIÈRE-PLAN
    // ════════════════════════════════════════════════════════════════

    private void lancerAnalyseIA() {
        String nomQ  = questionnaire != null ? questionnaire.getNom() : "Questionnaire";
        String typeQ = questionnaire != null && questionnaire.getType() != null
                ? questionnaire.getType().toString() : "GENERAL";

        new Thread(() -> {
            String analyse;
            try {
                if (reponsesJson != null && !reponsesJson.isBlank()) {
                    analyse = OpenAIService.analyserReponsesDetaillees(
                            nomQ, typeQ, score, niveau, interpretation, reponsesJson);
                } else {
                    analyse = OpenAIService.analyserReponses(
                            nomQ, typeQ, score, niveau, interpretation);
                }
            } catch (Exception ex) {
                System.err.println("❌ Exception dans lancerAnalyseIA : " + ex.getMessage());
                analyse = "❌ Erreur inattendue : " + ex.getMessage();
            }

            final String resultatFinal = analyse;

            Platform.runLater(() -> {
                boxChargement.setVisible(false);
                boxChargement.setManaged(false);
                boxResultat.setVisible(true);
                boxResultat.setManaged(true);

                if (resultatFinal != null && !resultatFinal.isBlank()) {
                    taAnalyse.setText(resultatFinal);
                    lblErreur.setVisible(false);
                    lblErreur.setManaged(false);
                } else {
                    lblErreur.setText("L'analyse IA n'est pas disponible.\nVérifiez votre connexion.");
                    lblErreur.setVisible(true);
                    lblErreur.setManaged(true);
                    taAnalyse.setText("");
                }
            });

        }).start();
    }

    // ════════════════════════════════════════════════════════════════
    //  BOUTONS
    // ════════════════════════════════════════════════════════════════

    @FXML
    public void handleRetour() {
        try {
            NavigationContext.loadContentInCenter("/fxml/EtudiantQuestionnairesView.fxml");
        } catch (Exception e) {
            System.err.println("❌ Erreur retour : " + e.getMessage());
        }
    }

    @FXML
    public void handleMesReponses() {
        try {
            NavigationContext.loadContentInCenter("/fxml/EtudiantMesReponsesView.fxml");
        } catch (Exception e) {
            System.err.println("❌ Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    public void handleCopier() {
        String texte = taAnalyse.getText();
        if (texte != null && !texte.isBlank()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(texte);
            clipboard.setContent(content);

            String styleOriginal = btnCopier.getStyle();
            btnCopier.setText("Copié !");
            btnCopier.setStyle(styleOriginal.replace("#059669", "#10b981"));
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    btnCopier.setText("Copier");
                    btnCopier.setStyle(styleOriginal);
                });
            }).start();
        }
    }

    private void styliserBouton(Button btn, String couleur, String couleurHover) {
        String base = "-fx-background-color: " + couleur + "; -fx-text-fill: white; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; "
                + "-fx-padding: 10 22; -fx-background-radius: 10; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + couleurHover + "; -fx-text-fill: white; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; "
                + "-fx-padding: 10 22; -fx-background-radius: 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
    }
}