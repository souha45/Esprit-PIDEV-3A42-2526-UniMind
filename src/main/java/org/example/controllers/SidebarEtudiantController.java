package org.example.controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.example.entities.User;
import org.example.utils.MyDataBase_Unimind;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SidebarEtudiantController {

    // ── FXML — zone utilisateur ────────────────────────────────────
    @FXML private ImageView sidebarPhoto;
    @FXML private Label     sidebarNomLabel;
    @FXML private Label     sidebarPrenomLabel;
    @FXML private Label     sidebarRoleLabel;

    // Labels initiales (style moderne)
    @FXML private Label lblInitiales;
    @FXML private Label lblNomComplet;
    @FXML private Label lblRole;

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
    //vérification

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur chargement: " + fxmlPath);
        }
    }

    @FXML
    public void showSeancesMeditation() {
        naviguer("/org/example/views/EtudiantSeances.fxml", "Séances de méditation", btnSeances);
    }

    @FXML
    public void showMesFavoris() {
        naviguer("/org/example/views/MesFavorisSeances.fxml", "Mes favoris", null);
    }

    @FXML
    public void showEvenements() {
        naviguer("/evenement/EvenementsEtudiant.fxml", "Événements", btnEvenements);
    }

    @FXML
    public void showMesParticipations() {
        naviguer("/participation/ParticipationsEtudiant.fxml", "Mes Participations", btnMesParticipations);
    }

    @FXML
    public void showFavorisEvenements() {
        naviguer("/favori/FavorisEtudiant.fxml", "Favoris Événements", btnFavorisEvenements);
    }


    // ── Design tokens ──────────────────────────────────────────────
    private static final String STYLE_ACTIVE =
            "-fx-background-color: #ede9fe; " +
                    "-fx-text-fill: #6366f1; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-border-color: #c4b5fd; " +
                    "-fx-border-width: 0 0 0 3; " +
                    "-fx-border-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_IDLE =
            "-fx-background-color: transparent; " +
                    "-fx-text-fill: #6b7280; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_HOVER =
            "-fx-background-color: #ede9fe; " +
                    "-fx-text-fill: #6366f1; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_LOGOUT_IDLE =
            "-fx-background-color: transparent; " +
                    "-fx-text-fill: #ef4444; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    private static final String STYLE_LOGOUT_HOVER =
            "-fx-background-color: #fee2e2; " +
                    "-fx-text-fill: #dc2626; " +
                    "-fx-font-family: 'Segoe UI'; " +
                    "-fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 11 16; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;";

    // ── État interne ───────────────────────────────────────────────
    private User               utilisateur;
    private BaseDashboardController parentController;
    private Connection         connection;
    private Button             activeButton;

    // ══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Navigation
        btnDashboard.setOnAction(e ->
                naviguer("/dashboard_etudiant.fxml",       "Dashboard",        btnDashboard));
        btnMesRendezVous.setOnAction(e ->
                naviguer("/RendezVousEtudiant.fxml",        "Mes Rendez-vous",  btnMesRendezVous));
        btnConsultations.setOnAction(e ->
                naviguer("/ConsultationsEtudiant.fxml",     "Consultations",    btnConsultations));
        btnTraitements.setOnAction(e ->
                naviguer("/traitement-etudiant-view.fxml",       "Traitements",      btnTraitements));
        btnQuestionnaires.setOnAction(e ->
                naviguer("/fxml/EtudiantQuestionnairesView.fxml", "Questionnaires", btnQuestionnaires));
        btnMesReponses.setOnAction(e ->
                naviguer("/fxml/EtudiantMesReponsesView.fxml", "Mes Réponses", btnMesReponses));
        btnProfil.setOnAction(e -> ouvrirProfil());
        btnDeconnexion.setOnAction(e -> seDeconnecter());
        btnSeances.setOnAction(e ->
                naviguer("/org/example/views/EtudiantSeances.fxml", "Séances de méditation", btnSeances));
        btnEvenements.setOnAction(e -> showEvenements());
        btnMesParticipations.setOnAction(e -> showMesParticipations());
        btnFavorisEvenements.setOnAction(e -> showFavorisEvenements());

        styliserBoutons();
        setActiveButton(btnDashboard);
    }

    // ══════════════════════════════════════════════════════════════
    //  SETTERS — deux modes d'alimentation possibles
    // ══════════════════════════════════════════════════════════════

    /**
     * Mode 1 — alimentation via le dashboard parent (BaseDashboardController).
     * Utilisé quand la sidebar est incluse dans un FXML géré par un BaseDashboardController.
     */
    public void setParentController(BaseDashboardController controller) {
        this.parentController = controller;
        if (controller != null) {
            this.connection  = controller.getConnection();
            this.utilisateur = controller.getUtilisateurConnecte();
            rafraichirAffichage();
        }
    }

    /**
     * Mode 2 — alimentation directe de l'utilisateur.
     * Utilisé lors de la navigation inter-pages (SidebarEtudiantController passe l'user lui-même).
     */
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (user == null) return;
        // Si on n'a pas encore de connexion, on en récupère une via le parent si dispo
        if (connection == null && parentController != null) {
            this.connection = parentController.getConnection();
        }
        rafraichirAffichage();
    }

    // ══════════════════════════════════════════════════════════════
    //  AFFICHAGE
    // ══════════════════════════════════════════════════════════════

    /** Met à jour tous les widgets de la zone utilisateur. */
    private void rafraichirAffichage() {
        afficherInfosSidebar();
        chargerPhotoSidebar();
    }

    private void afficherInfosSidebar() {
        User user = resolveUser();
        if (user == null) return;

        // ── Labels style "moderne" (initiales + nom complet + rôle) ──
        if (lblNomComplet != null) {
            lblNomComplet.setText(user.getPrenom() + " " + user.getNom());
        }
        if (lblInitiales != null) {
            lblInitiales.setText(
                    String.valueOf(user.getPrenom().charAt(0)).toUpperCase() +
                            String.valueOf(user.getNom().charAt(0)).toUpperCase());
        }
        if (lblRole != null) {
            lblRole.setText("ÉTUDIANT");
        }

        // ── Labels style "photo + texte" ──────────────────────────────
        if (sidebarNomLabel    != null) sidebarNomLabel.setText(user.getNom());
        if (sidebarPrenomLabel != null) sidebarPrenomLabel.setText(user.getPrenom());
        if (sidebarRoleLabel   != null) sidebarRoleLabel.setText("Étudiant");
    }

    private void chargerPhotoSidebar() {
        if (sidebarPhoto == null) return;
        User user = resolveUser();
        if (user == null) {
            System.out.println("❌ Sidebar - User is null");
            return;
        }

        System.out.println("=== SIDEBAR CHARGEMENT PHOTO ===");
        System.out.println("User ID: " + user.getUserId());
        System.out.println("User Nom: " + user.getNom());

        String photoPath = getPhotoChemin(user);
        System.out.println("Photo path from DB: " + photoPath);

        if (photoPath != null && !photoPath.isEmpty()) {
            File file = new File(photoPath);
            System.out.println("File exists: " + file.exists());
            System.out.println("File absolute path: " + file.getAbsolutePath());

            if (file.exists()) {
                try {
                    Image img = new Image(file.toURI().toString());
                    sidebarPhoto.setImage(img);
                    System.out.println("✅ Photo chargée avec succès");
                    return;
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement image: " + e.getMessage());
                }
            }
        }

        // Fallback
        try {
            sidebarPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png")));
            System.out.println("📷 Avatar par défaut chargé");
        } catch (Exception e) {
            System.err.println("❌ Impossible de charger l'avatar par défaut");
        }
    }
    private String getPhotoChemin(User user) {
        if (user == null) {
            System.out.println("❌ getPhotoChemin - User is null");
            return null;
        }

        System.out.println("🔍 Recherche photo pour user_id: " + user.getUserId());

        // 🔥 FORCER LA CONNEXION DIRECTE
        Connection conn = null;
        try {
            conn = MyDataBase_Unimind.getInstance().getConnection();
            System.out.println("🔗 Connexion directe obtenue: " + (conn != null ? "OK" : "NULL"));
        } catch (Exception e) {
            System.err.println("❌ Impossible d'obtenir la connexion: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        String query = "SELECT photo FROM profil WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, user.getUserId());
            var rs = ps.executeQuery();

            if (rs.next()) {
                String photo = rs.getString("photo");
                System.out.println("📸 Photo trouvée: " + photo);
                return photo;
            } else {
                System.out.println("⚠️ Aucun profil trouvé pour user_id: " + user.getUserId());
                // 🔥 Afficher tous les profils existants pour debug
                Statement stmt = conn.createStatement();
                ResultSet allRs = stmt.executeQuery("SELECT user_id, photo FROM profil");
                System.out.println("📋 Profils existants dans la BDD:");
                while (allRs.next()) {
                    System.out.println("   - user_id: " + allRs.getInt("user_id") + ", photo: " + allRs.getString("photo"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════

    private void naviguer(String fxmlPath, String titre, Button boutonActif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent content = loader.load();

            Object controller = loader.getController();

            if (controller instanceof EtudiantPageController) {
                ((EtudiantPageController) controller).setUtilisateur(resolveUser());
            }

            org.example.utils.NavigationContext.loadContentInCenter(content);

            setActiveButton(boutonActif);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Interface que chaque page-étudiant doit implémenter pour recevoir l'utilisateur. */
    public interface EtudiantPageController {
        void setUtilisateur(User user);
    }

    // ══════════════════════════════════════════════════════════════
    //  BOUTON ACTIF
    // ══════════════════════════════════════════════════════════════

    public void setActiveButtonByFxml(String fxmlPath) {
        switch (fxmlPath) {
            case "/dashboard_etudiant.fxml"      -> setActiveButton(btnDashboard);
            case "/RendezVousEtudiant.fxml"       -> setActiveButton(btnMesRendezVous);
            case "/ConsultationsEtudiant.fxml"    -> setActiveButton(btnConsultations);
            case "/traitement-etudiant-view.fxml"      -> setActiveButton(btnTraitements);
            case "/fxml/EtudiantQuestionnairesView.fxml" -> setActiveButton(btnQuestionnaires);
            case "/fxml/EtudiantMesReponsesView.fxml"    -> setActiveButton(btnMesReponses);
            case "/org/example/views/EtudiantSeances.fxml" -> setActiveButton(btnSeances);
            case "/org/example/views/MesFavorisSeances.fxml" -> setActiveButton(btnFavoris);
            case "/evenement/EvenementsEtudiant.fxml" -> setActiveButton(btnEvenements);
            case "/participation/ParticipationsEtudiant.fxml" -> setActiveButton(btnMesParticipations);
            case "/favori/FavorisEtudiant.fxml" -> setActiveButton(btnFavorisEvenements);
        }
    }

    private void setActiveButton(Button button) {
        Button[] boutons = {
                btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnQuestionnaires, btnMesReponses, btnSeances, btnFavoris,
                btnEvenements, btnMesParticipations, btnFavorisEvenements
        };
        for (Button btn : boutons) btn.setStyle(STYLE_IDLE);
        button.setStyle(STYLE_ACTIVE);
        activeButton = button;
    }

    private void styliserBoutons() {
        Button[] boutons = {
                btnDashboard, btnMesRendezVous, btnConsultations,
                btnTraitements, btnQuestionnaires, btnMesReponses, btnSeances,
                btnEvenements, btnMesParticipations, btnFavorisEvenements
        };
        for (Button btn : boutons) {
            btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(STYLE_HOVER); });
            btn.setOnMouseExited (e -> { if (btn != activeButton) btn.setStyle(STYLE_IDLE);  });
        }
        btnDeconnexion.setOnMouseEntered(e -> btnDeconnexion.setStyle(STYLE_LOGOUT_HOVER));
        btnDeconnexion.setOnMouseExited (e -> btnDeconnexion.setStyle(STYLE_LOGOUT_IDLE));
    }

    // ══════════════════════════════════════════════════════════════
    //  PROFIL
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void ouvrirProfil() {
        User user = resolveUser();
        if (user == null) {
            System.err.println("❌ SidebarEtudiant — utilisateur null, impossible d'ouvrir le profil");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(loader.load()));
            ProfilController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();

            // Rafraîchir la photo après fermeture
            chargerPhotoSidebar();
            if (parentController != null) parentController.rafraichirPhotoNavbar();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DÉCONNEXION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void seDeconnecter() {
        if (parentController != null) {
            // Délègue au parent qui gère la navigation vers login
            parentController.seDeconnecter();
        } else {
            // Déconnexion autonome (pas de parent)
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("UniMind — Connexion");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════

    /** Résout l'utilisateur : priorité au champ local, sinon on demande au parent. */
    private User resolveUser() {
        if (utilisateur != null) return utilisateur;
        if (parentController != null) return parentController.getUtilisateurConnecte();
        return null;
    }

    /** Résout la connexion : champ local en priorité, sinon celle du parent. */
    private Connection resolveConnection() {
        if (connection != null) return connection;
        if (parentController != null) return parentController.getConnection();
        return null;
    }
}
