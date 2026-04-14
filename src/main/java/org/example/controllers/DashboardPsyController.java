package org.example.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.models.User;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DashboardPsyController implements SidebarPsyController.PsyPageController {

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblDate;
    @FXML private Label lblHeure;
    @FXML private Label lblSoustitre;

    // ── Stat cards ──────────────────────────────────────────────────
    @FXML private Label lblRdvAujourdhui;
    @FXML private Label lblRdvAujDetaill;
    @FXML private Label lblPatientsActifs;
    @FXML private Label lblPatientsDetail;
    @FXML private Label lblRdvSemaine;
    @FXML private Label lblSemaineDetail;
    @FXML private Label lblTauxRemplissage;
    @FXML private Label lblTauxDetail;
    @FXML private Label lblTotalConsultations;
    @FXML private Label lblConsultDetail;

    // ── Prochains RDV ───────────────────────────────────────────────
    @FXML private VBox  listeProchainRdv;
    @FXML private Label lblBadgeRdvAuj;
    @FXML private Label lblEmptyRdv;

    // ── Statuts RDV ─────────────────────────────────────────────────
    @FXML private VBox boxStatutsRdv;

    // ── Disponibilités ──────────────────────────────────────────────
    @FXML private VBox  listeDisposVBox;
    @FXML private Label lblBadgeDispos;
    @FXML private Label lblEmptyDispos;

    // ── Satisfaction ────────────────────────────────────────────────
    @FXML private Label lblNoteMoyenne;
    @FXML private Label lblEtoiles;
    @FXML private Label lblNbEvaluations;
    @FXML private VBox  boxNotesDetail;

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarPsyController sidebarPsyController;

    private User utilisateur;
    private Timeline clockTimeline;

    // ── Connexion BDD ───────────────────────────────────────────────
    private Connection getConn() {
        return MyDataBase_Unimind.getInstance().getConnection();
    }

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        String jourSemaine = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(
                jourSemaine.substring(0, 1).toUpperCase() + jourSemaine.substring(1)
                        + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH))
        );

        updateHeure();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateHeure()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void updateHeure() {
        lblHeure.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarPsyController != null) sidebarPsyController.setUtilisateur(user);
        if (user == null) return;

        lblSoustitre.setText("Bonjour, Dr. " + user.getPrenom() + " " + user.getNom() + " 👋");
        chargerToutesLesStats(user.getUserId());
    }

    private void chargerToutesLesStats(int psyId) {
        chargerStatCartes(psyId);
        chargerProchainRdv(psyId);
        chargerRepartitionStatuts(psyId);
        chargerDisposAVenir(psyId);
        chargerSatisfaction(psyId);
    }

    // ── 1. Stat cards ───────────────────────────────────────────────
    private void chargerStatCartes(int psyId) {
        String today        = LocalDate.now().toString();
        String debutSemaine = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
        String finSemaine   = LocalDate.now().with(java.time.DayOfWeek.SUNDAY).toString();

        try {
            Connection conn = getConn();

            int rdvAuj = queryInt(conn,
                    "SELECT COUNT(*) FROM rendez_vous rv " +
                            "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                            "WHERE rv.psy_id = ? AND d.date_dispo = ? AND rv.statut IN ('confirme','Encours')",
                    psyId, today);
            lblRdvAujourdhui.setText(String.valueOf(rdvAuj));
            lblRdvAujDetaill.setText(rdvAuj <= 1 ? "rendez-vous planifié" : "rendez-vous planifiés");

            int patients = queryInt(conn,
                    "SELECT COUNT(DISTINCT rv.etudiant_id) FROM rendez_vous rv " +
                            "WHERE rv.psy_id = ? AND rv.statut NOT IN ('annulé','absent')",
                    psyId);
            lblPatientsActifs.setText(String.valueOf(patients));
            lblPatientsDetail.setText(patients <= 1 ? "étudiant suivi" : "étudiants suivis");

            int rdvSemaine = queryInt(conn,
                    "SELECT COUNT(*) FROM rendez_vous rv " +
                            "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                            "WHERE rv.psy_id = ? AND d.date_dispo BETWEEN ? AND ? " +
                            "AND rv.statut NOT IN ('annulé','absent')",
                    psyId, debutSemaine, finSemaine);
            lblRdvSemaine.setText(String.valueOf(rdvSemaine));
            lblSemaineDetail.setText("rendez-vous à venir");

            int totalDispos     = queryInt(conn,
                    "SELECT COUNT(*) FROM disponibilite_psy WHERE user_id = ?", psyId);
            int disposReservees = queryInt(conn,
                    "SELECT COUNT(*) FROM disponibilite_psy WHERE user_id = ? AND statut = 'réservé'", psyId);
            if (totalDispos > 0) {
                int taux = (int) Math.round((disposReservees * 100.0) / totalDispos);
                lblTauxRemplissage.setText(taux + "%");
                lblTauxDetail.setText("des créneaux réservés (" + disposReservees + "/" + totalDispos + ")");
            } else {
                lblTauxRemplissage.setText("—");
                lblTauxDetail.setText("aucun créneau créé");
            }

            int totalConsult = queryInt(conn,
                    "SELECT COUNT(*) FROM consultation WHERE psy_user_id = ?", psyId);
            lblTotalConsultations.setText(String.valueOf(totalConsult));
            lblConsultDetail.setText(totalConsult <= 1 ? "séance effectuée" : "séances effectuées");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── 2. Prochains RDV du jour ────────────────────────────────────
    private void chargerProchainRdv(int psyId) {
        String today = LocalDate.now().toString();
        String sql =
                "SELECT u.prenom, u.nom, d.heure_debut, d.heure_fin, rv.statut, d.type_consult " +
                        "FROM rendez_vous rv " +
                        "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                        "JOIN user u ON rv.etudiant_id = u.userId " +
                        "WHERE rv.psy_id = ? AND d.date_dispo = ? " +
                        "AND rv.statut IN ('confirme','Encours','demande') " +
                        "ORDER BY d.heure_debut ASC LIMIT 5";

        listeProchainRdv.getChildren().clear();
        int count = 0;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, psyId);
            ps.setString(2, today);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                listeProchainRdv.getChildren().add(buildRdvCard(
                        rs.getString("prenom"), rs.getString("nom"),
                        rs.getString("heure_debut").substring(0, 5),
                        rs.getString("heure_fin").substring(0, 5),
                        rs.getString("statut"), rs.getString("type_consult")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lblBadgeRdvAuj.setText(count + " aujourd'hui");
        if (count == 0) {
            lblEmptyRdv.setVisible(true);
            lblEmptyRdv.setManaged(true);
        }
    }

    private HBox buildRdvCard(String prenom, String nom, String hDebut, String hFin,
                              String statut, String type) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 10; -fx-padding: 10 14;");

        VBox heureBox = new VBox(1);
        heureBox.setAlignment(Pos.CENTER);
        heureBox.setMinWidth(52);
        Label lh1 = new Label(hDebut);
        lh1.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
        Label lh2 = new Label(hFin);
        lh2.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #a5b4fc;");
        heureBox.getChildren().addAll(lh1, lh2);

        Region sep = new Region();
        sep.setMinWidth(3); sep.setMaxWidth(3); sep.setMinHeight(36);
        sep.setStyle("-fx-background-color: #c4b5fd; -fx-background-radius: 2;");

        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        Label lNom = new Label(prenom + " " + nom);
        lNom.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        Label lType = new Label("📍 " + type);
        lType.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        infoBox.getChildren().addAll(lNom, lType);

        card.getChildren().addAll(heureBox, sep, infoBox, buildStatutBadge(statut));
        return card;
    }

    // ── 3. Répartition des statuts ──────────────────────────────────
    private void chargerRepartitionStatuts(int psyId) {
        String[][] statutsConfig = {
                {"demande",  "🟡 Demandes",  "#f59e0b"},
                {"confirme", "🟢 Confirmés", "#10b981"},
                {"Encours",  "🔵 En cours",  "#6366f1"},
                {"terminé",  "⚫ Terminés",  "#6b7280"},
                {"annulé",   "🔴 Annulés",   "#ef4444"},
                {"absent",   "🟠 Absents",   "#f97316"},
        };

        boxStatutsRdv.getChildren().clear();
        int total = 0;
        try { total = queryInt(getConn(),
                "SELECT COUNT(*) FROM rendez_vous WHERE psy_id = ?", psyId);
        } catch (Exception e) { e.printStackTrace(); }

        for (String[] cfg : statutsConfig) {
            int count = 0;
            try { count = queryInt(getConn(),
                    "SELECT COUNT(*) FROM rendez_vous WHERE psy_id = ? AND statut = ?",
                    psyId, cfg[0]);
            } catch (Exception e) { e.printStackTrace(); }

            int pct = (total > 0) ? (int) Math.round((count * 100.0) / total) : 0;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(cfg[1]);
            lbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #374151;");
            lbl.setMinWidth(120);

            StackPane barBg = new StackPane();
            barBg.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 4;");
            barBg.setMinHeight(8); barBg.setMaxHeight(8);
            HBox.setHgrow(barBg, Priority.ALWAYS);

            Region bar = new Region();
            bar.setMinHeight(8); bar.setMaxHeight(8);
            bar.setStyle("-fx-background-color: " + cfg[2] + "; -fx-background-radius: 4;");
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

    // ── 4. Disponibilités à venir (7 jours) ────────────────────────
    private void chargerDisposAVenir(int psyId) {
        String sql =
                "SELECT date_dispo, heure_debut, heure_fin, type_consult " +
                        "FROM disponibilite_psy " +
                        "WHERE user_id = ? AND statut = 'disponible' " +
                        "AND date_dispo BETWEEN ? AND ? " +
                        "ORDER BY date_dispo ASC, heure_debut ASC LIMIT 6";

        listeDisposVBox.getChildren().clear();
        int count = 0;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, psyId);
            ps.setString(2, LocalDate.now().toString());
            ps.setString(3, LocalDate.now().plusDays(7).toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                count++;
                LocalDate date = rs.getDate("date_dispo").toLocalDate();
                String hDebut  = rs.getString("heure_debut").substring(0, 5);
                String hFin    = rs.getString("heure_fin").substring(0, 5);
                String type    = rs.getString("type_consult");

                String dateFormat = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                        + " " + date.format(DateTimeFormatter.ofPattern("dd/MM"));

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 8; -fx-padding: 8 12;");

                Label lDate = new Label(dateFormat);
                lDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
                lDate.setMinWidth(66);

                Label lHeure = new Label(hDebut + " – " + hFin);
                lHeure.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #374151;");
                HBox.setHgrow(lHeure, Priority.ALWAYS);

                boolean presentiel = "présentiel".equalsIgnoreCase(type);
                Label lType = new Label(presentiel ? "🏢 Présentiel" : "💻 En ligne");
                lType.setStyle(
                        "-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");

                row.getChildren().addAll(lDate, lHeure, lType);
                listeDisposVBox.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lblBadgeDispos.setText(count + " créneaux libres");
        if (count == 0) {
            lblEmptyDispos.setVisible(true);
            lblEmptyDispos.setManaged(true);
        }
    }

    // ── 5. Satisfaction ─────────────────────────────────────────────
    private void chargerSatisfaction(int psyId) {
        int total = 0;
        double somme = 0;
        int[] distribution = new int[5];

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT note_satisfaction FROM consultation " +
                        "WHERE psy_user_id = ? AND note_satisfaction IS NOT NULL")) {
            ps.setInt(1, psyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int note = rs.getInt("note_satisfaction");
                if (note >= 1 && note <= 5) {
                    somme += note; total++;
                    distribution[note - 1]++;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (total == 0) {
            lblNoteMoyenne.setText("—");
            lblEtoiles.setText("☆☆☆☆☆");
            lblNbEvaluations.setText("Aucune évaluation");
            return;
        }

        double moyenne = somme / total;
        lblNoteMoyenne.setText(String.format("%.1f", moyenne));
        lblNbEvaluations.setText(total + (total == 1 ? " évaluation" : " évaluations"));

        int etoilesEntier = (int) Math.round(moyenne);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < etoilesEntier ? "★" : "☆");
        lblEtoiles.setText(sb.toString());

        boxNotesDetail.getChildren().clear();
        String[] couleurs = {"#ef4444", "#f97316", "#f59e0b", "#84cc16", "#10b981"};
        for (int i = 4; i >= 0; i--) {
            int nb  = distribution[i];
            int pct = (int) Math.round((nb * 100.0) / total);

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lStar = new Label((i + 1) + " ★");
            lStar.setStyle("-fx-font-size: 11px; -fx-text-fill: " + couleurs[i] + ";");
            lStar.setMinWidth(34);

            StackPane barBg = new StackPane();
            barBg.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 3;");
            barBg.setMinHeight(7); barBg.setMaxHeight(7);
            HBox.setHgrow(barBg, Priority.ALWAYS);

            Region bar = new Region();
            bar.setMinHeight(7); bar.setMaxHeight(7);
            bar.setStyle("-fx-background-color: " + couleurs[i] + "; -fx-background-radius: 3;");
            bar.prefWidthProperty().bind(barBg.widthProperty().multiply(pct / 100.0));
            StackPane.setAlignment(bar, Pos.CENTER_LEFT);
            barBg.getChildren().add(bar);

            Label lNb = new Label(String.valueOf(nb));
            lNb.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
            lNb.setMinWidth(22);

            row.getChildren().addAll(lStar, barBg, lNb);
            boxNotesDetail.getChildren().add(row);
        }
    }

    // ── Badge statut ────────────────────────────────────────────────
    private Label buildStatutBadge(String statut) {
        String bg, fg, txt;
        switch (statut) {
            case "confirme" -> { bg = "#dcfce7"; fg = "#16a34a"; txt = "✓ Confirmé"; }
            case "Encours"  -> { bg = "#dbeafe"; fg = "#2563eb"; txt = "⏳ En cours"; }
            case "demande"  -> { bg = "#fef9c3"; fg = "#ca8a04"; txt = "🕐 Demande";  }
            case "terminé"  -> { bg = "#f3f4f6"; fg = "#6b7280"; txt = "✅ Terminé"; }
            case "annulé"   -> { bg = "#fee2e2"; fg = "#dc2626"; txt = "✗ Annulé";   }
            case "absent"   -> { bg = "#ffedd5"; fg = "#c2410c"; txt = "⚠ Absent";   }
            default         -> { bg = "#f3f4f6"; fg = "#6b7280"; txt = statut;       }
        }
        Label badge = new Label(txt);
        badge.setStyle(
                "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-font-weight: bold; " +
                        "-fx-padding: 3 9; -fx-background-radius: 12;");
        return badge;
    }

    // ── Helper SQL ──────────────────────────────────────────────────
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