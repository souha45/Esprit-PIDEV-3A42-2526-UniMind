package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.models.Consultation;
import org.example.models.ConsultationDetail;
import org.example.models.User;
import org.example.services.ConsultationService;

import java.sql.SQLException;

public class ModifierConsultationModalController {

    @FXML private TextArea txtAvis;
    @FXML private HBox etoilesContainer;
    @FXML private Label lblNoteValue;
    @FXML private Button btnFermer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnEffacer;

    private ConsultationService consultationService;
    private ConsultationDetail consultationDetail;
    private User utilisateur;
    private Stage modalStage;
    private int noteActuelle = 0;

    private Button[] etoiles = new Button[5];

    // ========== DÉTECTION OS POUR POLICE EMOJI ==========
    private static final String EMOJI_FONT;
    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))         EMOJI_FONT = "'Segoe UI Emoji'";
        else if (os.contains("mac"))    EMOJI_FONT = "'Apple Color Emoji'";
        else                            EMOJI_FONT = "'Noto Color Emoji'";
    }

    @FXML
    public void initialize() {
        consultationService = new ConsultationService();
        creerEtoiles();
        btnEnregistrer.setOnAction(event -> enregistrerModification());
        btnAnnuler.setOnAction(event -> fermerModal());
        btnFermer.setOnAction(event -> fermerModal());
        if (btnEffacer != null) {
            btnEffacer.setOnAction(event -> effacerNote());
        }
    }

    // ========== STYLE HELPER ==========
    private String styleEtoile(String couleur) {
        return "-fx-font-size: 40px; " +
                "-fx-font-family: " + EMOJI_FONT + "; " +
                "-fx-text-fill: " + couleur + "; " +
                "-fx-background-color: transparent; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0;";
    }

    // ========== CRÉATION DES ÉTOILES ==========
    private void creerEtoiles() {
        for (int i = 0; i < 5; i++) {
            final int note = i + 1;
            Button etoile = new Button("☆");
            etoile.setStyle(styleEtoile("#bdc3c7"));
            etoile.setPrefSize(60, 60);

            etoile.setOnAction(event -> definirNote(note));

            final int index = i;
            etoile.setOnMouseEntered(e -> survolEtoiles(index));
            etoile.setOnMouseExited(e -> restaurerEtoiles());

            etoiles[i] = etoile;
            etoilesContainer.getChildren().add(etoile);
        }
    }

    private void definirNote(int note) {
        this.noteActuelle = note;
        mettreAJourAffichageEtoiles();
        lblNoteValue.setText(noteActuelle + "/5");
    }

    private void effacerNote() {
        this.noteActuelle = 0;
        mettreAJourAffichageEtoiles();
        lblNoteValue.setText("0/5");
    }

    private void mettreAJourAffichageEtoiles() {
        for (int i = 0; i < 5; i++) {
            if (i < noteActuelle) {
                etoiles[i].setText("★");
                etoiles[i].setStyle(styleEtoile("#f1c40f"));
            } else {
                etoiles[i].setText("☆");
                etoiles[i].setStyle(styleEtoile("#bdc3c7"));
            }
        }
    }

    private void survolEtoiles(int index) {
        for (int i = 0; i <= index; i++) {
            etoiles[i].setText("★");
            etoiles[i].setStyle(styleEtoile("#f1c40f"));
        }
        for (int i = index + 1; i < 5; i++) {
            etoiles[i].setText("☆");
            etoiles[i].setStyle(styleEtoile("#bdc3c7"));
        }
    }

    private void restaurerEtoiles() {
        mettreAJourAffichageEtoiles();
    }

    // ========== ENREGISTREMENT ==========
    private void enregistrerModification() {
        try {
            String avis = txtAvis.getText().trim();
            if (avis.isEmpty()) {
                afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                        "Veuillez saisir un avis.");
                return;
            }
            if (noteActuelle == 0) {
                afficherAlerte(Alert.AlertType.WARNING, "Note manquante",
                        "Veuillez sélectionner une note (1 à 5 étoiles).");
                return;
            }

            Consultation consultationEntity = new Consultation();
            consultationEntity.setConsultationId(consultationDetail.getConsultationId());
            consultationEntity.setAvisPsy(avis);
            consultationEntity.setNoteSatisfaction((short) noteActuelle);
            consultationEntity.setRendezVousId(consultationDetail.getRendezVousId());
            consultationEntity.setPsyUserId(utilisateur.getUserId());
            consultationEntity.setEtudiantUserId(consultationDetail.getEtudiantId());
            consultationEntity.setDateModification(new java.sql.Timestamp(System.currentTimeMillis()));

            consultationService.modifier(consultationEntity);

            afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                    "Avis et note enregistrés avec succès !\n\nNote: " + noteActuelle + "/5");
            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== UTILITAIRES ==========
    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void fermerModal() {
        if (modalStage != null) modalStage.close();
    }

    // ========== MÉTHODES EXTERNES ==========
    public void setConsultation(ConsultationDetail consultation) {
        this.consultationDetail = consultation;

        String avisExistant = consultation.getAvisPsy();
        if (avisExistant != null && !avisExistant.isEmpty()
                && !avisExistant.equals("Aucun avis")
                && !avisExistant.equals("Non renseigné")) {
            txtAvis.setText(avisExistant);
        }

        short noteExistante = consultation.getNoteSatisfaction();
        if (noteExistante > 0) {
            this.noteActuelle = noteExistante;
            mettreAJourAffichageEtoiles();
            lblNoteValue.setText(noteActuelle + "/5");
        } else {
            lblNoteValue.setText("0/5");
        }
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}