package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.Consultation;
import org.example.entities.ConsultationDetail;
import org.example.entities.User;
import org.example.services.ConsultationService;
import org.example.services.GeminiService;

import java.sql.SQLException;

public class ModifierConsultationModalController {

    // ========== COMPOSANTS FXML ==========
    @FXML private TextArea  txtAvis;
    @FXML private HBox      etoilesContainer;
    @FXML private Label     lblNoteValue;
    @FXML private Button    btnFermer;
    @FXML private Button    btnAnnuler;
    @FXML private Button    btnEnregistrer;
    @FXML private Button    btnEffacer;
    @FXML private Button    btnGenererAvisIA;

    // Toast overlay injecté depuis le FXML
    @FXML private StackPane toastContainer;

    // ========== VARIABLES ==========
    private final Button[]  etoiles = new Button[5];
    private ConsultationService  consultationService;
    private ConsultationDetail   consultationDetail;  // ✅ CORRIGÉ : ConsultationDetail au lieu de RendezVousDetail
    private User                 utilisateur;
    private Stage                modalStage;
    private int                  noteActuelle = 0;
    private Runnable             onSucces;

    // ========== EMOJI FONT ==========
    private static final String EMOJI_FONT;
    static {
        String os = System.getProperty("os.name").toLowerCase();
        EMOJI_FONT = os.contains("win") ? "'Segoe UI Emoji'"
                : os.contains("mac") ? "'Apple Color Emoji'"
                : "'Noto Color Emoji'";
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        consultationService = new ConsultationService();
        creerEtoiles();
        btnEnregistrer.setOnAction(e -> enregistrerModification());
        btnAnnuler.setOnAction(e -> fermerModal());
        btnFermer.setOnAction(e -> fermerModal());
        if (btnEffacer != null) btnEffacer.setOnAction(e -> effacerNote());

        // ✅ Action du bouton IA
        if (btnGenererAvisIA != null) {
            btnGenererAvisIA.setOnAction(e -> genererAvisAvecIA());
            btnGenererAvisIA.setOnMouseEntered(ev ->
                    btnGenererAvisIA.setStyle(btnGenererAvisIA.getStyle().replace("#8b5cf6", "#7c3aed")));
            btnGenererAvisIA.setOnMouseExited(ev ->
                    btnGenererAvisIA.setStyle(btnGenererAvisIA.getStyle().replace("#7c3aed", "#8b5cf6")));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GÉNÉRATION AVIS IA
    // ══════════════════════════════════════════════════════════════
    private void genererAvisAvecIA() {
        if (consultationDetail == null) {
            showToast("❌ Aucune consultation sélectionnée", ToastType.ERROR);
            return;
        }

        String ancienTexte = txtAvis.getText();
        txtAvis.setText("🤖 Génération de l'avis en cours...");
        txtAvis.setDisable(true);
        btnGenererAvisIA.setDisable(true);

        StringBuilder contexte = new StringBuilder();

        // ✅ Utilisation des bonnes méthodes de ConsultationDetail
        contexte.append("Patient : ").append(consultationDetail.getEtudiantPrenom())
                .append(" ").append(consultationDetail.getEtudiantNom()).append("\n");
        contexte.append("Date de consultation : ").append(consultationDetail.getDateDispo().toLocalDate())
                .append("\n");
        contexte.append("Heure : ").append(consultationDetail.getHeureDebut().toString().substring(0, 5))
                .append(" - ").append(consultationDetail.getHeureFin().toString().substring(0, 5)).append("\n");

        String prompt = """
            Tu es un assistant pour psychologue. Rédige un avis professionnel et bienveillant.
            
            Contexte de la consultation :
            %s
            
            L'avis doit :
            - Faire 3-5 phrases
            - Être à la 2ème personne (tu)
            - Être encourageant et empathique
            - Proposer des pistes d'amélioration concrètes
            - Terminer par une phrase d'encouragement
            
            N'inclus PAS de diagnostic médical.
            """.formatted(contexte.toString());

        new Thread(() -> {
            try {
                GeminiService geminiService = new GeminiService();
                String avisGenere = geminiService.envoyerMessage(prompt, "");
                avisGenere = avisGenere.replaceAll("^\"+|\"+$", "").trim();
                final String avisFinal = avisGenere;

                javafx.application.Platform.runLater(() -> {
                    txtAvis.setText(avisFinal);
                    txtAvis.setDisable(false);
                    btnGenererAvisIA.setDisable(false);
                    showToast("✅ Avis généré avec succès ! Vous pouvez le modifier.", ToastType.SUCCESS);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    txtAvis.setText(ancienTexte);
                    txtAvis.setDisable(false);
                    btnGenererAvisIA.setDisable(false);
                    showToast("❌ Erreur : " + e.getMessage(), ToastType.ERROR);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTOILES
    // ══════════════════════════════════════════════════════════════
    private String styleEtoile(String couleur) {
        return "-fx-font-size:40px;-fx-font-family:" + EMOJI_FONT + ";" +
                "-fx-text-fill:" + couleur + ";-fx-background-color:transparent;" +
                "-fx-cursor:hand;-fx-padding:0;";
    }

    private void creerEtoiles() {
        for (int i = 0; i < 5; i++) {
            final int note  = i + 1;
            final int index = i;
            Button e = new Button("☆");
            e.setStyle(styleEtoile("#bdc3c7"));
            e.setPrefSize(60, 60);
            e.setOnAction(ev      -> definirNote(note));
            e.setOnMouseEntered(ev -> survolEtoiles(index));
            e.setOnMouseExited(ev  -> restaurerEtoiles());
            etoiles[i] = e;
            etoilesContainer.getChildren().add(e);
        }
    }

    private void definirNote(int note) {
        this.noteActuelle = note;
        mettreAJourAffichageEtoiles();
        updateNoteLabel();
    }

    private void effacerNote() {
        this.noteActuelle = 0;
        mettreAJourAffichageEtoiles();
        updateNoteLabel();
    }

    private void updateNoteLabel() {
        lblNoteValue.setText(noteActuelle > 0 ? noteActuelle + "/5" : "0/5");
    }

    private void mettreAJourAffichageEtoiles() {
        for (int i = 0; i < 5; i++) {
            boolean filled = i < noteActuelle;
            etoiles[i].setText(filled ? "★" : "☆");
            etoiles[i].setStyle(styleEtoile(filled ? "#f1c40f" : "#bdc3c7"));
        }
    }

    private void survolEtoiles(int index) {
        for (int i = 0; i < 5; i++) {
            boolean filled = i <= index;
            etoiles[i].setText(filled ? "★" : "☆");
            etoiles[i].setStyle(styleEtoile(filled ? "#f1c40f" : "#bdc3c7"));
        }
    }

    private void restaurerEtoiles() { mettreAJourAffichageEtoiles(); }

    // ══════════════════════════════════════════════════════════════
    //  ENREGISTREMENT
    // ══════════════════════════════════════════════════════════════
    private void enregistrerModification() {
        String avis = txtAvis.getText().trim();
        if (avis.isEmpty()) {
            showToast("⚠ Veuillez saisir un avis avant d'enregistrer.", ToastType.WARNING);
            return;
        }
        if (noteActuelle == 0) {
            showToast("⚠ Veuillez sélectionner une note (1 à 5 étoiles).", ToastType.WARNING);
            return;
        }
        try {
            Consultation entity = new Consultation();
            entity.setConsultationId(consultationDetail.getConsultationId());
            entity.setAvisPsy(avis);
            entity.setNoteSatisfaction((short) noteActuelle);
            entity.setRendezVousId(consultationDetail.getRendezVousId());
            entity.setPsyUserId(utilisateur.getUserId());
            entity.setEtudiantUserId(consultationDetail.getEtudiantId());
            entity.setDateModification(new java.sql.Timestamp(System.currentTimeMillis()));

            consultationService.modifier(entity);

            fermerModal();
            if (onSucces != null) onSucces.run();

        } catch (SQLException e) {
            showToast("✗ Erreur : " + e.getMessage(), ToastType.ERROR);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  TOAST
    // ══════════════════════════════════════════════════════════════
    private enum ToastType { SUCCESS, WARNING, ERROR }

    private void showToast(String message, ToastType type) {
        String bg = switch (type) {
            case SUCCESS -> "#10b981";
            case WARNING -> "#f59e0b";
            case ERROR   -> "#ef4444";
        };

        Label pill = new Label(message);
        pill.setWrapText(true);
        pill.setMaxWidth(460);
        pill.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-padding:12 22;-fx-background-radius:30;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.20),12,0,0,4);");

        toastContainer.getChildren().add(pill);
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        StackPane.setAlignment(pill, Pos.BOTTOM_CENTER);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), pill);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), pill);
        fadeOut.setDelay(Duration.seconds(type == ToastType.SUCCESS ? 1.2 : 2.2));
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            toastContainer.getChildren().remove(pill);
            if (toastContainer.getChildren().isEmpty()) {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            }
        });

        fadeIn.play();
        fadeOut.play();
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════
    private void fermerModal() {
        if (modalStage != null) modalStage.close();
    }

    public void setOnSucces(Runnable callback) {
        this.onSucces = callback;
    }

    // ✅ CORRIGÉ : Utilise ConsultationDetail
    public void setConsultation(ConsultationDetail consultation) {
        this.consultationDetail = consultation;

        String avisExistant = consultation.getAvisPsy();
        if (avisExistant != null && !avisExistant.isEmpty()
                && !avisExistant.equals("Aucun avis")
                && !avisExistant.equals("Non renseigné")) {
            txtAvis.setText(avisExistant);
        }

        short noteExistante = consultation.getNoteSatisfaction();
        this.noteActuelle   = noteExistante > 0 ? noteExistante : 0;
        mettreAJourAffichageEtoiles();
        lblNoteValue.setText(noteActuelle > 0 ? noteActuelle + "/5" : "0/5");
    }

    public void setUtilisateur(User user) { this.utilisateur = user; }
    public void setModalStage(Stage stage) { this.modalStage = stage; }
}