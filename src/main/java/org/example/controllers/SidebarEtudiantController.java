package org.example.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.example.entities.User;
import org.example.services.VoiceAssistantService;
import org.example.services.VoiceAssistantService.NavigationCommand;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.NavigationContext;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.utils.NavigationContext;

public class SidebarEtudiantController {

    // ── FXML — zone utilisateur ────────────────────────────────────
    @FXML private ImageView sidebarPhoto;
    @FXML private Label     sidebarNomLabel;
    @FXML private Label     sidebarPrenomLabel;
    @FXML private Label     sidebarRoleLabel;
    @FXML private Label     lblInitiales;
    @FXML private Label     lblNomComplet;
    @FXML private Label     lblRole;

    // ── FXML — assistant vocal ─────────────────────────────────────
    @FXML private Button btnVoice;
    @FXML private Label  lblVoiceHint;
    @FXML private VBox   voiceFeedbackBox;
    @FXML private Label  lblVoiceStatus;
    @FXML private Label  lblVoiceTranscript;

    // ── FXML — boutons de navigation ──────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnMesRendezVous;
    @FXML private Button btnConsultations;
    @FXML private Button btnTraitements;
    @FXML private Button btnQuestionnaires;
    @FXML private Button btnMesReponses;
    @FXML private Button btnProfil;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnSeances;
    @FXML private Button btnFavoris;
    @FXML private Button btnEvenements;
    @FXML private Button btnMesParticipations;
    @FXML private Button btnFavorisEvenements;
    @FXML private StackPane contentArea;

    // ── Design tokens ──────────────────────────────────────────────
    private static final String STYLE_ACTIVE =
            "-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 16; -fx-background-radius: 10; " +
                    "-fx-border-color: #c4b5fd; -fx-border-width: 0 0 0 3; " +
                    "-fx-border-radius: 10; -fx-cursor: hand;";

    private static final String STYLE_IDLE =
            "-fx-background-color: transparent; -fx-text-fill: #6b7280; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 16; " +
                    "-fx-background-radius: 10; -fx-cursor: hand;";

    private static final String STYLE_HOVER =
            "-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 16; " +
                    "-fx-background-radius: 10; -fx-cursor: hand;";

    private static final String STYLE_LOGOUT_IDLE =
            "-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 16; " +
                    "-fx-background-radius: 10; -fx-cursor: hand;";

    private static final String STYLE_LOGOUT_HOVER =
            "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 16; " +
                    "-fx-background-radius: 10; -fx-cursor: hand;";

    // Styles bouton micro
    private static final String VOICE_IDLE =
            "-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; " +
                    "-fx-font-size: 16px; -fx-background-radius: 50%; " +
                    "-fx-min-width: 38px; -fx-min-height: 38px; " +
                    "-fx-max-width: 38px; -fx-max-height: 38px; -fx-cursor: hand; " +
                    "-fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 50%; -fx-border-width: 1.5;";

    private static final String VOICE_ACTIVE =
            "-fx-background-color: #ef4444; -fx-text-fill: white; " +
                    "-fx-font-size: 16px; -fx-background-radius: 50%; " +
                    "-fx-min-width: 38px; -fx-min-height: 38px; " +
                    "-fx-max-width: 38px; -fx-max-height: 38px; -fx-cursor: hand; " +
                    "-fx-border-color: #fca5a5; -fx-border-radius: 50%; -fx-border-width: 2;" +
                    "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.55), 10, 0, 0, 0);";

    // ── État interne ───────────────────────────────────────────────
    private User                   utilisateur;
    private BaseDashboardController parentController;
    private Connection             connection;
    private Button                 activeButton;
    private VoiceAssistantService  voiceService;
    private Timeline               pulseTimeline;    // animation pendant écoute

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Navigation classique
        btnDashboard    .setOnAction(e -> naviguer("/dashboard_etudiant.fxml",                  "Dashboard",        btnDashboard));
        btnMesRendezVous.setOnAction(e -> naviguer("/RendezVousEtudiant.fxml",                  "Mes Rendez-vous",  btnMesRendezVous));
        btnConsultations.setOnAction(e -> naviguer("/ConsultationsEtudiant.fxml",               "Consultations",    btnConsultations));
        btnTraitements  .setOnAction(e -> naviguer("/traitement-etudiant-view.fxml",            "Traitements",      btnTraitements));
        btnQuestionnaires.setOnAction(e -> naviguer("/fxml/EtudiantQuestionnairesView.fxml",    "Questionnaires",   btnQuestionnaires));
        btnMesReponses  .setOnAction(e -> naviguer("/fxml/EtudiantMesReponsesView.fxml",        "Mes Réponses",     btnMesReponses));
        btnSeances      .setOnAction(e -> naviguer("/org/example/views/EtudiantSeances.fxml",   "Séances",          btnSeances));
        btnFavoris      .setOnAction(e -> showMesFavoris());
        btnEvenements   .setOnAction(e -> showEvenements());
        btnMesParticipations.setOnAction(e -> showMesParticipations());
        btnFavorisEvenements.setOnAction(e -> showFavorisEvenements());
        btnProfil       .setOnAction(e -> ouvrirProfil());
        btnDeconnexion  .setOnAction(e -> seDeconnecter());

        styliserBoutons();
        setActiveButton(btnDashboard);
        initVoiceAssistant();
    }

    // ── Init assistant vocal ──────────────────────────────────────
    private void initVoiceAssistant() {
        voiceService = new VoiceAssistantService();

        // Callback : commande reconnue → naviguer
        voiceService.onCommandRecognized(cmd -> executeVoiceCommand(cmd));

        // Callback : statut (chargement, écoute, erreur...)
        voiceService.onStatusChanged(status -> {
            if (lblVoiceStatus != null) lblVoiceStatus.setText(status);
        });

        // Callback : transcript temps réel
        voiceService.onTranscriptChanged(text -> {
            if (lblVoiceTranscript != null) lblVoiceTranscript.setText("\"" + text + "\"");
        });

        // Hover du bouton micro
        if (btnVoice != null) {
            btnVoice.setOnMouseEntered(e -> {
                if (!voiceService.isListening())
                    btnVoice.setStyle(VOICE_IDLE.replace("0.18", "0.30"));
            });
            btnVoice.setOnMouseExited(e -> {
                if (!voiceService.isListening())
                    btnVoice.setStyle(VOICE_IDLE);
            });
        }
    }

    // ── Bouton micro : toggle ─────────────────────────────────────
    @FXML
    public void toggleVoiceAssistant() {
        if (voiceService.isListening()) {
            // Arrêter
            voiceService.stopListening();
            stopPulse();
            if (btnVoice != null) {
                btnVoice.setText("\uD83C\uDF99");
                btnVoice.setStyle(VOICE_IDLE);
            }
            if (lblVoiceHint  != null) lblVoiceHint.setText("vocal");
            if (voiceFeedbackBox != null) {
                voiceFeedbackBox.setVisible(false);
                voiceFeedbackBox.setManaged(false);
            }
        } else {
            // Démarrer
            voiceService.startListening();
            startPulse();
            if (btnVoice != null) {
                btnVoice.setText("⏹");
                btnVoice.setStyle(VOICE_ACTIVE);
            }
            if (lblVoiceHint  != null) lblVoiceHint.setText("arrêter");
            if (voiceFeedbackBox != null) {
                voiceFeedbackBox.setVisible(true);
                voiceFeedbackBox.setManaged(true);
            }
            if (lblVoiceStatus != null)
                lblVoiceStatus.setText("⏳ Chargement du modèle...");
            if (lblVoiceTranscript != null)
                lblVoiceTranscript.setText("");
        }
    }

    // ── Exécute la commande vocale reconnue ───────────────────────
    private void executeVoiceCommand(NavigationCommand cmd) {
        // Feedback visuel bref
        if (lblVoiceTranscript != null)
            lblVoiceTranscript.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#86efac;-fx-font-weight:bold;");

        // Remettre le style normal après 2s
        new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (lblVoiceTranscript != null)
                lblVoiceTranscript.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:white;-fx-font-weight:bold;");
        })).play();

        switch (cmd) {
            case DASHBOARD        -> { naviguer("/dashboard_etudiant.fxml",                  "Dashboard",        btnDashboard);     }
            case RENDEZ_VOUS      -> { naviguer("/RendezVousEtudiant.fxml",                  "Mes Rendez-vous",  btnMesRendezVous); }
            case CONSULTATIONS    -> { naviguer("/ConsultationsEtudiant.fxml",               "Consultations",    btnConsultations); }
            case TRAITEMENTS      -> { naviguer("/traitement-etudiant-view.fxml",            "Traitements",      btnTraitements);   }
            case QUESTIONNAIRES   -> { naviguer("/fxml/EtudiantQuestionnairesView.fxml",     "Questionnaires",   btnQuestionnaires);}
            case MES_REPONSES     -> { naviguer("/fxml/EtudiantMesReponsesView.fxml",        "Mes Réponses",     btnMesReponses);   }
            case SEANCES          -> { naviguer("/org/example/views/EtudiantSeances.fxml",   "Séances",          btnSeances);       }
            case FAVORIS_SEANCES  -> { showMesFavoris();        }
            case EVENEMENTS       -> { showEvenements();        }
            case MES_PARTICIPATIONS -> { showMesParticipations(); }
            case FAVORIS_EVENEMENTS -> { showFavorisEvenements(); }
            case PROFIL           -> { ouvrirProfil();          }
            case DECONNEXION      -> { seDeconnecter();         }
            case INCONNU          -> { /* rien */ }
        }
    }

    // ── Animation pulsation pendant écoute ───────────────────────
    private void startPulse() {
        if (pulseTimeline != null) pulseTimeline.stop();
        pulseTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    if (btnVoice != null) btnVoice.setOpacity(1.0);
                }),
                new KeyFrame(Duration.millis(600), e -> {
                    if (btnVoice != null) btnVoice.setOpacity(0.55);
                }),
                new KeyFrame(Duration.millis(1200))
        );
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);
        pulseTimeline.play();
    }

    private void stopPulse() {
        if (pulseTimeline != null) { pulseTimeline.stop(); pulseTimeline = null; }
        if (btnVoice != null) btnVoice.setOpacity(1.0);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION (inchangée)
    // ══════════════════════════════════════════════════════════════

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void showSeancesMeditation() {
        naviguer("/org/example/views/EtudiantSeances.fxml", "Séances de méditation", btnSeances);
    }
    @FXML public void showMesFavoris() {
        naviguer("/org/example/views/MesFavorisSeances.fxml", "Mes favoris", btnFavoris);
    }
    @FXML public void showEvenements() {
        naviguer("/evenement/EvenementsEtudiant.fxml", "Événements", btnEvenements);
    }
    @FXML public void showMesParticipations() {
        naviguer("/participation/ParticipationsEtudiant.fxml", "Mes Participations", btnMesParticipations);
    }
    @FXML public void showFavorisEvenements() {
        naviguer("/favori/FavorisEtudiant.fxml", "Favoris Événements", btnFavorisEvenements);
    }

    private void naviguer(String fxmlPath, String titre, Button boutonActif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EtudiantPageController)
                ((EtudiantPageController) controller).setUtilisateur(resolveUser());
            
            // Initialiser le SessionManager avec l'étudiant connecté pour les controllers événement/participation/favori
            User etudiant = resolveUser();
            if (etudiant != null && etudiant.getRole() != null) {
                org.example.utils.SessionManager.getInstance().initSession(etudiant);
                System.out.println("[SidebarEtudiant] Session initialisée pour: " + etudiant.getPrenom() + " " + etudiant.getNom() + " (ID: " + etudiant.getUserId() + ", Role: " + etudiant.getRole() + ")");
            }
            
            NavigationContext.loadContentInCenter(content);
            setActiveButton(boutonActif);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface EtudiantPageController {
        void setUtilisateur(User user);
    }

    // ══════════════════════════════════════════════════════════════
    //  SETTERS
    // ══════════════════════════════════════════════════════════════

    public void setParentController(BaseDashboardController controller) {
        this.parentController = controller;
        if (controller != null) {
            this.connection  = controller.getConnection();
            this.utilisateur = controller.getUtilisateurConnecte();
            rafraichirAffichage();
        }
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user == null) return;
        if (connection == null && parentController != null)
            this.connection = parentController.getConnection();
        rafraichirAffichage();
    }

    // ══════════════════════════════════════════════════════════════
    //  AFFICHAGE
    // ══════════════════════════════════════════════════════════════

    private void rafraichirAffichage() { afficherInfosSidebar(); chargerPhotoSidebar(); }

    private void afficherInfosSidebar() {
        User user = resolveUser();
        if (user == null) return;
        if (lblNomComplet  != null) lblNomComplet.setText(user.getPrenom() + " " + user.getNom());
        if (lblInitiales   != null) lblInitiales.setText(
                String.valueOf(user.getPrenom().charAt(0)).toUpperCase() +
                        String.valueOf(user.getNom().charAt(0)).toUpperCase());
        if (lblRole        != null) lblRole.setText("ÉTUDIANT");
        if (sidebarNomLabel    != null) sidebarNomLabel.setText(user.getNom());
        if (sidebarPrenomLabel != null) sidebarPrenomLabel.setText(user.getPrenom());
        if (sidebarRoleLabel   != null) sidebarRoleLabel.setText("Étudiant");
    }

    private void chargerPhotoSidebar() {
        if (sidebarPhoto == null) return;
        User user = resolveUser();
        if (user == null) return;
        String photoPath = getPhotoChemin(user);
        if (photoPath != null && !photoPath.isEmpty()) {
            File file = new File(photoPath);
            if (file.exists()) {
                try { sidebarPhoto.setImage(new Image(file.toURI().toString())); return; }
                catch (Exception e) { System.err.println("Photo load error: " + e.getMessage()); }
            }
        }
        try { sidebarPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png"))); }
        catch (Exception ignored) {}
    }

    private String getPhotoChemin(User user) {
        if (user == null) return null;
        try {
            Connection conn = MyDataBase_Unimind.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT photo FROM profil WHERE user_id = ?")) {
                ps.setInt(1, user.getUserId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("photo");
            }
        } catch (SQLException e) { System.err.println("getPhotoChemin: " + e.getMessage()); }
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    //  BOUTON ACTIF
    // ══════════════════════════════════════════════════════════════

    public void setActiveButtonByFxml(String fxmlPath) {
        switch (fxmlPath) {
            case "/dashboard_etudiant.fxml"                    -> setActiveButton(btnDashboard);
            case "/RendezVousEtudiant.fxml"                    -> setActiveButton(btnMesRendezVous);
            case "/ConsultationsEtudiant.fxml"                 -> setActiveButton(btnConsultations);
            case "/traitement-etudiant-view.fxml"              -> setActiveButton(btnTraitements);
            case "/fxml/EtudiantQuestionnairesView.fxml"       -> setActiveButton(btnQuestionnaires);
            case "/fxml/EtudiantMesReponsesView.fxml"          -> setActiveButton(btnMesReponses);
            case "/org/example/views/EtudiantSeances.fxml"     -> setActiveButton(btnSeances);
            case "/org/example/views/MesFavorisSeances.fxml"   -> setActiveButton(btnFavoris);
            case "/evenement/EvenementsEtudiant.fxml"          -> setActiveButton(btnEvenements);
            case "/participation/ParticipationsEtudiant.fxml"  -> setActiveButton(btnMesParticipations);
            case "/favori/FavorisEtudiant.fxml"                -> setActiveButton(btnFavorisEvenements);
        }
    }

    private void setActiveButton(Button button) {
        if (button == null) return;
        Button[] boutons = {
                btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnQuestionnaires, btnMesReponses, btnSeances, btnFavoris,
                btnEvenements, btnMesParticipations, btnFavorisEvenements
        };
        for (Button btn : boutons) if (btn != null) btn.setStyle(STYLE_IDLE);
        button.setStyle(STYLE_ACTIVE);
        activeButton = button;
    }

    private void styliserBoutons() {
        Button[] boutons = {
                btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnQuestionnaires, btnMesReponses, btnSeances, btnFavoris,
                btnEvenements, btnMesParticipations, btnFavorisEvenements
        };
        for (Button btn : boutons) {
            if (btn == null) continue;
            btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(STYLE_HOVER); });
            btn.setOnMouseExited (e -> { if (btn != activeButton) btn.setStyle(STYLE_IDLE);  });
        }
        if (btnDeconnexion != null) {
            btnDeconnexion.setOnMouseEntered(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_HOVER));
            btnDeconnexion.setOnMouseExited (e -> btnDeconnexion.setStyle(STYLE_LOGOUT_IDLE));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROFIL
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void ouvrirProfil() {
        User user = resolveUser();
        if (user == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(loader.load()));
            ProfilController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            chargerPhotoSidebar();
            if (parentController != null) parentController.rafraichirPhotoNavbar();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════
    //  DÉCONNEXION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void seDeconnecter() {
        // Arrêter l'assistant vocal proprement
        if (voiceService != null && voiceService.isListening()) {
            voiceService.stopListening();
        }
        NavigationContext.clear();
        EtudiantSeancesController.CitationCache.clear();
        if (parentController != null) {
            parentController.seDeconnecter();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("UniMind — Connexion");
                stage.show();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════

    private User resolveUser() {
        if (utilisateur != null) return utilisateur;
        if (parentController != null) return parentController.getUtilisateurConnecte();
        return null;
    }

    private Connection resolveConnection() {
        if (connection != null) return connection;
        if (parentController != null) return parentController.getConnection();
        return null;
    }
}