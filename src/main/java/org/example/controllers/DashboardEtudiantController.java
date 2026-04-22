package org.example.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.User;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.NavigationContext;
import org.example.services.EvenementService;
import org.example.services.ParticipationService;
import org.example.enums.StatutEvenement;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DashboardEtudiantController extends BaseDashboardController
        implements SidebarEtudiantController.EtudiantPageController {

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    // ── Content ScrollPane ───────────────────────────────────────────
    @FXML private ScrollPane contentScrollPane;

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblSoustitre;
    @FXML private Label lblDate;
    @FXML private Label lblHeure;

    // ── Stat cards ──────────────────────────────────────────────────
    @FXML private Label lblProchainsRdv;
    @FXML private Label lblProchainsRdvDetail;
    @FXML private Label lblTotalConsultations;
    @FXML private Label lblConsultDetail;
    @FXML private Label lblTraitementsActifs;
    @FXML private Label lblTraitDetail;
    @FXML private Label lblNoteMoyenne;
    @FXML private Label lblNoteDetail;
    @FXML private Label lblRdvAnnules;
    @FXML private Label lblAnnuleDetail;

    // ── Prochain RDV ────────────────────────────────────────────────
    @FXML private VBox   carteProchainRdv;
    @FXML private Label  lblRdvJour;
    @FXML private Label  lblRdvMois;
    @FXML private Label  lblRdvPsychologue;
    @FXML private Label  lblRdvHoraire;
    @FXML private Label  lblRdvType;
    @FXML private Label  lblRdvStatut;
    @FXML private Label  lblAucunRdv;
    @FXML private Button btnPrendreRdv;

    // ── Statuts RDV ─────────────────────────────────────────────────
    @FXML private VBox boxStatutsRdv;

    // ── Consultations ───────────────────────────────────────────────
    @FXML private VBox  listeConsultations;
    @FXML private Label lblBadgeConsult;
    @FXML private Label lblEmptyConsult;

    // ── Mon psy ─────────────────────────────────────────────────────
    @FXML private VBox  boxMonPsy;
    @FXML private Label lblEmptyPsy;

    // ── Événements & Participations (Module événements) ─────────────
    @FXML private VBox listeEvenementsDashboard;
    @FXML private Label lblNbEvenements;
    @FXML private Label lblEmptyEvenements;
    @FXML private VBox listeParticipationsDashboard;
    @FXML private Label lblNbParticipations;
    @FXML private Label lblEmptyParticipations;

    // ── Données ─────────────────────────────────────────────────────
    private User     utilisateur;
    private Timeline clockTimeline;

    // ── BDD ─────────────────────────────────────────────────────────
    private Connection getConn() {
        return MyDataBase_Unimind.getInstance().getConnection();
    }

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Initialiser le contexte de navigation (uniquement si pas déjà initialisé)
        // Sinon, lors d'un rechargement de dashboard dans le centre, on écrase le ScrollPane principal
        // et la navigation « coince ».
        if (NavigationContext.getContentScrollPane() == null) {
            NavigationContext.setContentScrollPane(contentScrollPane);
        }

        // Date en français
        LocalDate today = LocalDate.now();
        String jourSemaine = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(
                jourSemaine.substring(0, 1).toUpperCase() + jourSemaine.substring(1)
                        + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH))
        );

        // Horloge live
        updateHeure();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateHeure()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        btnPrendreRdv.setOnAction(e -> prendreRendezVous());

        // Hover bouton
        btnPrendreRdv.setOnMouseEntered(e ->
                btnPrendreRdv.setStyle(btnPrendreRdv.getStyle().replace("#6366f1", "#4f46e5")));
        btnPrendreRdv.setOnMouseExited(e ->
                btnPrendreRdv.setStyle(btnPrendreRdv.getStyle().replace("#4f46e5", "#6366f1")));
    }

    private void updateHeure() {
        lblHeure.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // ── Entry point ─────────────────────────────────────────────────
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarEtudiantController != null) sidebarEtudiantController.setUtilisateur(user);
        if (user == null) return;

        lblSoustitre.setText("Bonjour, " + user.getPrenom() + " " + user.getNom() + " 👋");
        chargerTout(user.getUserId());
    }

    @Override
    public void setUser(User user) {
        super.setUser(user);           // stocke dans utilisateurConnecte
        this.utilisateur = user;       // stocke localement aussi

        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
        }
        if (user == null) return;

        lblSoustitre.setText("Bonjour, Dr. " + user.getPrenom() + " " + user.getNom() + " 👋");
        chargerTout(user.getUserId());
    }

    private void chargerTout(int etudiantId) {
        chargerStatCartes(etudiantId);
        chargerProchainRdv(etudiantId);
        chargerRepartitionStatuts(etudiantId);
        chargerDernieresConsultations(etudiantId);
        chargerMonPsy(etudiantId);
        chargerEvenementsDashboard();
        chargerParticipationsDashboard(etudiantId);
    }

    // ── 1. Stat cards ───────────────────────────────────────────────
    private void chargerStatCartes(int etudiantId) {
        String debutSemaine = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
        String finSemaine   = LocalDate.now().with(java.time.DayOfWeek.SUNDAY).toString();

        try {
            Connection conn = getConn();

            // Prochains RDV cette semaine (confirmé / en cours / demande)
            int prochainsRdv = queryInt(conn,
                    "SELECT COUNT(*) FROM rendez_vous rv " +
                            "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                            "WHERE rv.etudiant_id = ? AND d.date_dispo BETWEEN ? AND ? " +
                            "AND rv.statut IN ('confirme','Encours','demande')",
                    etudiantId, debutSemaine, finSemaine);
            lblProchainsRdv.setText(String.valueOf(prochainsRdv));
            lblProchainsRdvDetail.setText(prochainsRdv <= 1 ? "rendez-vous cette semaine"
                    : "rendez-vous cette semaine");

            // Total consultations
            int totalConsult = queryInt(conn,
                    "SELECT COUNT(*) FROM consultation WHERE etudiant_user_id = ?", etudiantId);
            lblTotalConsultations.setText(String.valueOf(totalConsult));
            lblConsultDetail.setText(totalConsult <= 1 ? "séance effectuée" : "séances effectuées");

            // Traitements actifs
            int traitements = queryInt(conn,
                    "SELECT COUNT(*) FROM traitement WHERE etudiant_id = ? AND statut = 'actif'",
                    etudiantId);
            lblTraitementsActifs.setText(String.valueOf(traitements));
            lblTraitDetail.setText(traitements <= 1 ? "traitement actif" : "traitements actifs");

            // Note satisfaction moyenne (mes notes données)
            int nbNotes = queryInt(conn,
                    "SELECT COUNT(*) FROM consultation WHERE etudiant_user_id = ? AND note_satisfaction IS NOT NULL",
                    etudiantId);
            if (nbNotes > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT AVG(note_satisfaction) FROM consultation " +
                                "WHERE etudiant_user_id = ? AND note_satisfaction IS NOT NULL")) {
                    ps.setInt(1, etudiantId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        double moy = rs.getDouble(1);
                        lblNoteMoyenne.setText(String.format("%.1f", moy) + " ★");
                        lblNoteDetail.setText("note moyenne donnée");
                    }
                }
            } else {
                lblNoteMoyenne.setText("—");
                lblNoteDetail.setText("aucune évaluation");
            }

            // RDV annulés / absents
            int annules = queryInt(conn,
                    "SELECT COUNT(*) FROM rendez_vous WHERE etudiant_id = ? AND statut IN ('annulé','absent')",
                    etudiantId);
            lblRdvAnnules.setText(String.valueOf(annules));
            lblAnnuleDetail.setText(annules <= 1 ? "rendez-vous annulé" : "rendez-vous annulés");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── 2. Prochain RDV ─────────────────────────────────────────────
    private void chargerProchainRdv(int etudiantId) {
        String today = LocalDate.now().toString();
        String sql =
                "SELECT u.prenom, u.nom, d.date_dispo, d.heure_debut, d.heure_fin, " +
                        "       d.type_consult, rv.statut " +
                        "FROM rendez_vous rv " +
                        "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                        "JOIN user u ON rv.psy_id = u.user_id " +
                        "WHERE rv.etudiant_id = ? AND d.date_dispo >= ? " +
                        "AND rv.statut IN ('confirme','Encours','demande') " +
                        "ORDER BY d.date_dispo ASC, d.heure_debut ASC LIMIT 1";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ps.setString(2, today);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                LocalDate date  = rs.getDate("date_dispo").toLocalDate();
                String hDebut   = rs.getString("heure_debut").substring(0, 5);
                String hFin     = rs.getString("heure_fin").substring(0, 5);
                String prenom   = rs.getString("prenom");
                String nom      = rs.getString("nom");
                String type     = rs.getString("type_consult");
                String statut   = rs.getString("statut");

                lblRdvJour.setText(String.valueOf(date.getDayOfMonth()));
                lblRdvMois.setText(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase());
                lblRdvPsychologue.setText("Dr. " + prenom + " " + nom);
                lblRdvHoraire.setText(hDebut + " – " + hFin);

                // Badge type
                boolean presentiel = "présentiel".equalsIgnoreCase(type);
                lblRdvType.setText(presentiel ? "🏢 Présentiel" : "💻 En ligne");
                lblRdvType.setStyle(
                        "-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 3 10; -fx-background-radius: 20;");

                // Badge statut
                String[] sc = statutColors(statut);
                lblRdvStatut.setText(sc[2]);
                lblRdvStatut.setStyle(
                        "-fx-background-color: " + sc[0] + "; -fx-text-fill: " + sc[1] + "; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");

                carteProchainRdv.setVisible(true);
                carteProchainRdv.setManaged(true);
                lblAucunRdv.setVisible(false);
                lblAucunRdv.setManaged(false);

            } else {
                carteProchainRdv.setVisible(false);
                carteProchainRdv.setManaged(false);
                lblAucunRdv.setVisible(true);
                lblAucunRdv.setManaged(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── 3. Répartition statuts ──────────────────────────────────────
    private void chargerRepartitionStatuts(int etudiantId) {
        String[][] cfg = {
                {"demande",  "🟡 Demandes",  "#f59e0b"},
                {"confirme", "🟢 Confirmés", "#10b981"},
                {"Encours",  "🔵 En cours",  "#6366f1"},
                {"terminé",  "⚫ Terminés",  "#6b7280"},
                {"annulé",   "🔴 Annulés",   "#ef4444"},
                {"absent",   "🟠 Absents",   "#f97316"},
        };

        boxStatutsRdv.getChildren().clear();
        int total = 0;
        try {
            total = queryInt(getConn(),
                    "SELECT COUNT(*) FROM rendez_vous WHERE etudiant_id = ?", etudiantId);
        } catch (Exception e) { e.printStackTrace(); }

        for (String[] c : cfg) {
            int count = 0;
            try {
                count = queryInt(getConn(),
                        "SELECT COUNT(*) FROM rendez_vous WHERE etudiant_id = ? AND statut = ?",
                        etudiantId, c[0]);
            } catch (Exception e) { e.printStackTrace(); }

            int pct = (total > 0) ? (int) Math.round((count * 100.0) / total) : 0;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(c[1]);
            lbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #374151;");
            lbl.setMinWidth(120);

            StackPane barBg = new StackPane();
            barBg.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 4;");
            barBg.setMinHeight(8); barBg.setMaxHeight(8);
            HBox.setHgrow(barBg, Priority.ALWAYS);

            Region bar = new Region();
            bar.setMinHeight(8); bar.setMaxHeight(8);
            bar.setStyle("-fx-background-color: " + c[2] + "; -fx-background-radius: 4;");
            bar.prefWidthProperty().bind(barBg.widthProperty().multiply(pct / 100.0));
            StackPane.setAlignment(bar, Pos.CENTER_LEFT);
            barBg.getChildren().add(bar);

            Label lCount = new Label(count + " (" + pct + "%)");
            lCount.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
            lCount.setMinWidth(70);

            row.getChildren().addAll(lbl, barBg, lCount);
            boxStatutsRdv.getChildren().add(row);
        }
    }

    // ── 4. Dernières consultations ──────────────────────────────────
    private void chargerDernieresConsultations(int etudiantId) {
        String sql =
                "SELECT u.prenom, u.nom, d.date_dispo, d.heure_debut, d.heure_fin, " +
                        "       c.note_satisfaction, c.avis_psy " +
                        "FROM consultation c " +
                        "JOIN rendez_vous rv ON c.rendez_vous_id = rv.rendez_vous_id " +
                        "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                        "JOIN user u ON c.psy_user_id = u.user_id " +
                        "WHERE c.etudiant_user_id = ? " +
                        "ORDER BY c.date_redaction DESC LIMIT 3";

        listeConsultations.getChildren().clear();
        int count = 0;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                count++;
                String prenom   = rs.getString("prenom");
                String nom      = rs.getString("nom");
                LocalDate date  = rs.getDate("date_dispo").toLocalDate();
                String hDebut   = rs.getString("heure_debut").substring(0, 5);
                String hFin     = rs.getString("heure_fin").substring(0, 5);
                int note        = rs.getInt("note_satisfaction");
                boolean hasNote = !rs.wasNull();

                HBox card = new HBox(12);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 10; -fx-padding: 10 14;");

                // Icône
                Label ico = new Label("📝");
                ico.setStyle("-fx-font-size: 18px;");

                VBox info = new VBox(2);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label lNom = new Label("Dr. " + prenom + " " + nom);
                lNom.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                String dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + "  " + hDebut + " – " + hFin;
                Label lDate = new Label(dateStr);
                lDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
                info.getChildren().addAll(lNom, lDate);

                // Étoiles
                if (hasNote && note >= 1 && note <= 5) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 5; i++) sb.append(i < note ? "★" : "☆");
                    Label etoiles = new Label(sb.toString());
                    etoiles.setStyle("-fx-font-size: 14px; -fx-text-fill: #f59e0b;");
                    card.getChildren().addAll(ico, info, etoiles);
                } else {
                    card.getChildren().addAll(ico, info);
                }

                listeConsultations.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lblBadgeConsult.setText(count + " consultation" + (count > 1 ? "s" : ""));
        if (count == 0) {
            lblEmptyConsult.setVisible(true);
            lblEmptyConsult.setManaged(true);
        }
    }

    // ── 5. Mon psychologue ──────────────────────────────────────────
    private void chargerMonPsy(int etudiantId) {
        String sql =
                "SELECT u.prenom, u.nom, u.email, COUNT(*) AS nbRdv " +
                        "FROM rendez_vous rv " +
                        "JOIN user u ON rv.psy_id = u.user_id " +
                        "WHERE rv.etudiant_id = ? " +
                        "GROUP BY rv.psy_id, u.prenom, u.nom, u.email " +
                        "ORDER BY nbRdv DESC LIMIT 1";

        boxMonPsy.getChildren().clear();

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String prenom = rs.getString("prenom");
                String nom    = rs.getString("nom");
                String email  = rs.getString("email");
                int    nbRdv  = rs.getInt("nbRdv");

                // Initiales
                String initiales = String.valueOf(prenom.charAt(0)).toUpperCase()
                        + String.valueOf(nom.charAt(0)).toUpperCase();

                HBox card = new HBox(14);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 12; -fx-padding: 14 16;");

                // Avatar
                Label avatar = new Label(initiales);
                avatar.setStyle("-fx-background-color: #6366f1; -fx-text-fill: #ffffff; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-min-width: 46; -fx-min-height: 46; -fx-alignment: CENTER; " +
                        "-fx-background-radius: 50; -fx-padding: 10 12;");

                VBox info = new VBox(3);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label lNom = new Label("Dr. " + prenom + " " + nom);
                lNom.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
                Label lEmail = new Label(email);
                lEmail.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
                Label lNbRdv = new Label(nbRdv + " rendez-vous effectués");
                lNbRdv.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #6366f1;");
                info.getChildren().addAll(lNom, lEmail, lNbRdv);

                card.getChildren().addAll(avatar, info);
                boxMonPsy.getChildren().add(card);

            } else {
                lblEmptyPsy.setVisible(true);
                lblEmptyPsy.setManaged(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Événements à venir (Module événements) ───────────────────────
    private void chargerEvenementsDashboard() {
        try {
            EvenementService evenementService = new EvenementService();
            List<Evenement> evenements = evenementService.afficher();

            // Filtrer: statut A_VENIR ou EN_COURS, date debut >= aujourd'hui
            LocalDate today = LocalDate.now();
            List<Evenement> evenementsAVenir = evenements.stream()
                    .filter(e -> e.getStatut() == StatutEvenement.A_VENIR || e.getStatut() == StatutEvenement.EN_COURS)
                    .filter(e -> e.getDateDebut() != null && e.getDateDebut().toLocalDateTime().toLocalDate().isAfter(today.minusDays(1)))
                    .sorted((e1, e2) -> e1.getDateDebut().compareTo(e2.getDateDebut()))
                    .limit(3)
                    .toList();

            lblNbEvenements.setText(String.valueOf(evenementsAVenir.size()));

            if (evenementsAVenir.isEmpty()) {
                lblEmptyEvenements.setVisible(true);
                lblEmptyEvenements.setManaged(true);
            } else {
                listeEvenementsDashboard.getChildren().clear();
                for (Evenement e : evenementsAVenir) {
                    HBox card = new HBox(12);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 10; -fx-padding: 12 14;");

                    VBox info = new VBox(4);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    Label lTitre = new Label(e.getTitre() != null ? e.getTitre() : "Sans titre");
                    lTitre.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
                    lTitre.setWrapText(true);

                    String dateStr = e.getDateDebut() != null ? e.getDateDebut().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                    Label lDate = new Label(dateStr);
                    lDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");

                    Label lType = new Label(e.getType() != null ? e.getType().name() : "");
                    lType.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 12;");

                    info.getChildren().addAll(lTitre, lDate, lType);
                    card.getChildren().addAll(info);
                    listeEvenementsDashboard.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblEmptyEvenements.setVisible(true);
            lblEmptyEvenements.setManaged(true);
        }
    }

    // ── Mes Participations (Module événements) ────────────────────────
    private void chargerParticipationsDashboard(int etudiantId) {
        try {
            ParticipationService participationService = new ParticipationService();
            List<Participation> participations = participationService.afficher();

            // Filtrer: participations de cet étudiant, statut CONFIRME
            List<Participation> mesParticipations = participations.stream()
                    .filter(p -> p.getEtudiantId() == etudiantId)
                    .filter(p -> p.getStatut() == org.example.enums.StatutParticipation.CONFIRME)
                    .toList();

            lblNbParticipations.setText(String.valueOf(mesParticipations.size()));

            if (mesParticipations.isEmpty()) {
                lblEmptyParticipations.setVisible(true);
                lblEmptyParticipations.setManaged(true);
            } else {
                listeParticipationsDashboard.getChildren().clear();
                EvenementService evenementService = new EvenementService();
                LocalDate today = LocalDate.now();

                // Filtrer par date de l'événement et limiter à 3
                List<Participation> participationsFiltrees = mesParticipations.stream()
                        .filter(p -> {
                            try {
                                Evenement e = evenementService.findById(p.getEvenementId());
                                return e != null && e.getDateDebut() != null
                                        && e.getDateDebut().toLocalDateTime().toLocalDate().isAfter(today.minusDays(1));
                            } catch (SQLException ex) {
                                return false;
                            }
                        })
                        .sorted((p1, p2) -> {
                            try {
                                Evenement e1 = evenementService.findById(p1.getEvenementId());
                                Evenement e2 = evenementService.findById(p2.getEvenementId());
                                if (e1 != null && e2 != null && e1.getDateDebut() != null && e2.getDateDebut() != null) {
                                    return e1.getDateDebut().compareTo(e2.getDateDebut());
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                            return 0;
                        })
                        .limit(3)
                        .toList();

                for (Participation p : participationsFiltrees) {
                    try {
                        Evenement e = evenementService.findById(p.getEvenementId());
                        if (e != null) {
                            HBox card = new HBox(12);
                            card.setAlignment(Pos.CENTER_LEFT);
                            card.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10; -fx-padding: 12 14;");

                            VBox info = new VBox(4);
                            HBox.setHgrow(info, Priority.ALWAYS);

                            Label lTitre = new Label(e.getTitre() != null ? e.getTitre() : "Sans titre");
                            lTitre.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #166534;");
                            lTitre.setWrapText(true);

                            String dateStr = e.getDateDebut() != null ? e.getDateDebut().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                            Label lDate = new Label(dateStr);
                            lDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");

                            Label lStatut = new Label("Confirmé");
                            lStatut.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 12;");

                            info.getChildren().addAll(lTitre, lDate, lStatut);
                            card.getChildren().addAll(info);
                            listeParticipationsDashboard.getChildren().add(card);
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblEmptyParticipations.setVisible(true);
            lblEmptyParticipations.setManaged(true);
        }
    }

    // ── Modal prendre RDV ───────────────────────────────────────────
    private void prendreRendezVous() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrendreRendezVousModal.fxml"));
            Stage modalStage  = new Stage();
            Scene scene       = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnPrendreRdv.getScene().getWindow());
            modalStage.setTitle("Prendre un rendez-vous");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            PrendreRendezVousModalController controller = loader.getController();
            controller.setEtudiantId(utilisateur.getUserId());
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            // Rafraîchir après fermeture
            chargerTout(utilisateur.getUserId());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private String[] statutColors(String statut) {
        return switch (statut) {
            case "confirme" -> new String[]{"#dcfce7", "#16a34a", "✓ Confirmé"};
            case "Encours"  -> new String[]{"#dbeafe", "#2563eb", "⏳ En cours"};
            case "demande"  -> new String[]{"#fef9c3", "#ca8a04", "🕐 Demande" };
            case "terminé"  -> new String[]{"#f3f4f6", "#6b7280", "✅ Terminé" };
            case "annulé"   -> new String[]{"#fee2e2", "#dc2626", "✗ Annulé"  };
            case "absent"   -> new String[]{"#ffedd5", "#c2410c", "⚠ Absent"  };
            default         -> new String[]{"#f3f4f6", "#6b7280", statut       };
        };
    }

    private int queryInt(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) ps.setInt(i + 1, (Integer) params[i]);
                else ps.setString(i + 1, params[i].toString());
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
