package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.example.entities.User;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class StatistiquesPsyController
        implements SidebarPsychologueController.PsyPageController {

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarPsychologueController sidebarPsychologueController;

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblDate;
    @FXML private Label lblStatut;

    // ── Filtres ─────────────────────────────────────────────────────
    @FXML private ComboBox<String> comboPeriode;
    @FXML private DatePicker       dateDebut;
    @FXML private DatePicker       dateFin;
    @FXML private ComboBox<String> comboTypeConsult;
    @FXML private ComboBox<String> comboGroupement;
    @FXML private Button           btnAppliquer;
    @FXML private Button           btnReset;

    // ── KPI Cards ───────────────────────────────────────────────────
    @FXML private Label kpiRdvTotal;
    @FXML private Label kpiRdvDetail;
    @FXML private Label kpiTermines;
    @FXML private Label kpiTerminesDetail;
    @FXML private Label kpiNoteMoyenne;
    @FXML private Label kpiNoteDetail;
    @FXML private Label kpiAnnules;
    @FXML private Label kpiAnnulesDetail;
    @FXML private Label kpiPatients;
    @FXML private Label kpiPatientsDetail;

    // ── Charts ──────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  chartEvolution;
    @FXML private CategoryAxis              axisX;
    @FXML private NumberAxis                axisY;
    @FXML private PieChart                  chartStatuts;
    @FXML private LineChart<String, Number> chartSatisfaction;
    @FXML private BarChart<String, Number>  chartTypes;

    // ── Données ─────────────────────────────────────────────────────
    private User utilisateur;

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Date header
        LocalDate today = LocalDate.now();
        String jour = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(jour.substring(0,1).toUpperCase() + jour.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

        // ── Combos ──────────────────────────────────────────────────
        comboPeriode.setItems(FXCollections.observableArrayList(
                "Cette semaine", "Ce mois", "3 derniers mois",
                "6 derniers mois", "Cette année", "Personnalisée"));
        comboPeriode.setValue("Ce mois");

        comboTypeConsult.setItems(FXCollections.observableArrayList(
                "Tous", "présentiel", "en ligne"));
        comboTypeConsult.setValue("Tous");

        comboGroupement.setItems(FXCollections.observableArrayList(
                "Semaine", "Mois", "Trimestre"));
        comboGroupement.setValue("Mois");

        // ── Listeners période ────────────────────────────────────────
        comboPeriode.valueProperty().addListener((o, ov, nv) -> {
            appliquerPeriodePredefinie(nv);
            if (!"Personnalisée".equals(nv)) chargerTout();
        });

        // ── Boutons ─────────────────────────────────────────────────
        btnAppliquer.setOnAction(e -> chargerTout());
        btnReset.setOnAction(e -> reinitialiserFiltres());

        btnAppliquer.setOnMouseEntered(e ->
                btnAppliquer.setStyle(btnAppliquer.getStyle().replace("#7c3aed","#6d28d9")));
        btnAppliquer.setOnMouseExited(e ->
                btnAppliquer.setStyle(btnAppliquer.getStyle().replace("#6d28d9","#7c3aed")));

        // ── Style charts ─────────────────────────────────────────────
        styleChart(chartEvolution);
        styleChart(chartTypes);
        styleChart(chartSatisfaction);

        // Période par défaut
        appliquerPeriodePredefinie("Ce mois");
    }

    // ── Interface ────────────────────────────────────────────────────
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarPsychologueController != null) {
            sidebarPsychologueController.setUtilisateur(user);
            sidebarPsychologueController.setActiveButtonByFxml("/StatistiquesPsy.fxml");
        }
        chargerTout();
    }

    // ── Réinitialiser filtres ────────────────────────────────────────
    private void reinitialiserFiltres() {
        comboPeriode.setValue("Ce mois");
        comboTypeConsult.setValue("Tous");
        comboGroupement.setValue("Mois");
        appliquerPeriodePredefinie("Ce mois");
        chargerTout();
    }

    // ── Périodes prédéfinies ─────────────────────────────────────────
    private void appliquerPeriodePredefinie(String periode) {
        LocalDate fin   = LocalDate.now();
        LocalDate debut;
        switch (periode) {
            case "Cette semaine"    -> debut = fin.with(java.time.DayOfWeek.MONDAY);
            case "Ce mois"          -> debut = fin.withDayOfMonth(1);
            case "3 derniers mois"  -> debut = fin.minusMonths(3).withDayOfMonth(1);
            case "6 derniers mois"  -> debut = fin.minusMonths(6).withDayOfMonth(1);
            case "Cette année"      -> debut = fin.withDayOfYear(1);
            default                 -> { return; } // Personnalisée → ne pas écraser
        }
        dateDebut.setValue(debut);
        dateFin.setValue(fin);
    }

    // ── Chargement principal ─────────────────────────────────────────
    private void chargerTout() {
        if (utilisateur == null) return;

        LocalDate debut = dateDebut.getValue();
        LocalDate fin   = dateFin.getValue();

        if (debut == null || fin == null || debut.isAfter(fin)) {
            lblStatut.setText("⚠ Période invalide — vérifiez les dates.");
            return;
        }

        String type = comboTypeConsult.getValue();
        String typeSQL = ("Tous".equals(type)) ? null : type;

        lblStatut.setText("Chargement des données…");

        chargerKPIs(debut, fin, typeSQL);
        chargerChartEvolution(debut, fin, typeSQL);
        chargerChartStatuts(debut, fin, typeSQL);
        chargerChartSatisfaction(debut, fin, typeSQL);
        chargerChartTypes(debut, fin);

        lblStatut.setText("Données mises à jour — "
                + debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " → " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    // ── 1. KPIs ─────────────────────────────────────────────────────
    private void chargerKPIs(LocalDate debut, LocalDate fin, String type) {
        int psyId = utilisateur.getUserId();
        try {
            Connection conn = getConn();

            String whereType = (type != null)
                    ? " AND d.type_consult = '" + type + "'"
                    : "";

            String base = "FROM rendez_vous rv " +
                    "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                    "WHERE rv.psy_id = " + psyId +
                    " AND d.date_dispo BETWEEN '" + debut + "' AND '" + fin + "'" + whereType;

            // Total RDV
            int total = queryInt(conn, "SELECT COUNT(*) " + base);
            kpiRdvTotal.setText(String.valueOf(total));
            kpiRdvDetail.setText(total + " rendez-vous sur la période");

            // Terminés
            int termines = queryInt(conn, "SELECT COUNT(*) " + base + " AND rv.statut = 'terminé'");
            kpiTermines.setText(String.valueOf(termines));
            int pctTermine = total > 0 ? (int)Math.round(termines * 100.0 / total) : 0;
            kpiTerminesDetail.setText(pctTermine + "% des RDV");

            // Annulés
            int annules = queryInt(conn,
                    "SELECT COUNT(*) " + base + " AND rv.statut IN ('annulé','absent')");
            kpiAnnules.setText(String.valueOf(annules));
            int pctAnnule = total > 0 ? (int)Math.round(annules * 100.0 / total) : 0;
            kpiAnnulesDetail.setText(pctAnnule + "% des RDV");

            // Patients distincts
            int patients = queryInt(conn,
                    "SELECT COUNT(DISTINCT rv.etudiant_id) " + base);
            kpiPatients.setText(String.valueOf(patients));
            kpiPatientsDetail.setText(patients + " patient" + (patients > 1 ? "s" : "") + " distincts");

            // Note moyenne (consultations)
            String sqlNote = "SELECT AVG(c.note_satisfaction) FROM consultation c " +
                    "JOIN rendez_vous rv ON c.rendez_vous_id = rv.rendez_vous_id " +
                    "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                    "WHERE c.psy_user_id = " + psyId +
                    " AND d.date_dispo BETWEEN '" + debut + "' AND '" + fin + "'" +
                    " AND c.note_satisfaction IS NOT NULL" + whereType;

            try (PreparedStatement ps = conn.prepareStatement(sqlNote);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getObject(1) != null) {
                    double moy = rs.getDouble(1);
                    kpiNoteMoyenne.setText(String.format("%.1f", moy) + " ★");
                    kpiNoteDetail.setText(buildEtoiles((int)Math.round(moy)));
                } else {
                    kpiNoteMoyenne.setText("—");
                    kpiNoteDetail.setText("Aucune évaluation");
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── 2. Évolution RDV (BarChart) ──────────────────────────────────
    private void chargerChartEvolution(LocalDate debut, LocalDate fin, String type) {
        chartEvolution.getData().clear();

        String groupement = comboGroupement.getValue();
        String formatSQL, formatJava;
        if ("Semaine".equals(groupement)) {
            formatSQL = "DATE_FORMAT(d.date_dispo, '%Y-%u')";
            formatJava = "Sem. %s";
        } else if ("Trimestre".equals(groupement)) {
            formatSQL = "CONCAT(YEAR(d.date_dispo), '-T', QUARTER(d.date_dispo))";
            formatJava = "%s";
        } else {
            formatSQL = "DATE_FORMAT(d.date_dispo, '%Y-%m')";
            formatJava = "%s";
        }

        String whereType = (type != null) ? " AND d.type_consult = '" + type + "'" : "";
        String sql = "SELECT " + formatSQL + " as periode, COUNT(*) as nb " +
                "FROM rendez_vous rv " +
                "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                "WHERE rv.psy_id = " + utilisateur.getUserId() +
                " AND d.date_dispo BETWEEN '" + debut + "' AND '" + fin + "'" + whereType +
                " GROUP BY periode ORDER BY periode ASC";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rendez-vous");

        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(
                        rs.getString("periode"),
                        rs.getInt("nb")));
            }
        } catch (Exception e) { e.printStackTrace(); }

        chartEvolution.getData().add(series);
        // Couleur bars
        series.getData().forEach(d ->
                d.getNode().setStyle("-fx-bar-fill: #7c3aed;"));
    }

    // ── 3. Répartition statuts (PieChart) ────────────────────────────
    private void chargerChartStatuts(LocalDate debut, LocalDate fin, String type) {
        chartStatuts.getData().clear();

        String whereType = (type != null) ? " AND d.type_consult = '" + type + "'" : "";
        String sql = "SELECT rv.statut, COUNT(*) as nb " +
                "FROM rendez_vous rv " +
                "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                "WHERE rv.psy_id = " + utilisateur.getUserId() +
                " AND d.date_dispo BETWEEN '" + debut + "' AND '" + fin + "'" + whereType +
                " GROUP BY rv.statut ORDER BY nb DESC";

        String[] couleurs = {"#7c3aed","#10b981","#f59e0b","#6b7280","#ef4444","#f97316"};
        int idx = 0;

        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String statut = rs.getString("statut");
                int    nb     = rs.getInt("nb");
                PieChart.Data slice = new PieChart.Data(
                        labelliserStatut(statut) + " (" + nb + ")", nb);
                chartStatuts.getData().add(slice);
                // Couleur
                final String couleur = couleurs[idx % couleurs.length];
                idx++;
                slice.nodeProperty().addListener((o, ov, nv) -> {
                    if (nv != null) nv.setStyle("-fx-pie-color: " + couleur + ";");
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── 4. Satisfaction dans le temps (LineChart) ────────────────────
    private void chargerChartSatisfaction(LocalDate debut, LocalDate fin, String type) {
        chartSatisfaction.getData().clear();

        String whereType = (type != null) ? " AND d.type_consult = '" + type + "'" : "";
        String sql = "SELECT DATE_FORMAT(d.date_dispo, '%Y-%m') as mois, " +
                "AVG(c.note_satisfaction) as moy " +
                "FROM consultation c " +
                "JOIN rendez_vous rv ON c.rendez_vous_id = rv.rendez_vous_id " +
                "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                "WHERE c.psy_user_id = " + utilisateur.getUserId() +
                " AND d.date_dispo BETWEEN '" + debut + "' AND '" + fin + "'" +
                " AND c.note_satisfaction IS NOT NULL" + whereType +
                " GROUP BY mois ORDER BY mois ASC";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Note moyenne");

        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double moy = rs.getDouble("moy");
                series.getData().add(new XYChart.Data<>(
                        rs.getString("mois"),
                        Math.round(moy * 10.0) / 10.0));
            }
        } catch (Exception e) { e.printStackTrace(); }

        chartSatisfaction.getData().add(series);
        series.getData().forEach(d ->
                d.getNode().setStyle("-fx-background-color: #f59e0b, white;"));
        if (!series.getData().isEmpty()) {
            series.getNode().lookup(".chart-series-line")
                    .setStyle("-fx-stroke: #f59e0b; -fx-stroke-width: 2.5;");
        }
    }

    // ── 5. Présentiel vs En ligne (BarChart) ─────────────────────────
    private void chargerChartTypes(LocalDate debut, LocalDate fin) {
        chartTypes.getData().clear();

        String sql = "SELECT d.type_consult, COUNT(*) as nb " +
                "FROM rendez_vous rv " +
                "JOIN disponibilite_psy d ON rv.dispo_id = d.dispo_id " +
                "WHERE rv.psy_id = " + utilisateur.getUserId() +
                " AND d.date_dispo BETWEEN '" + debut + "' AND '" + fin + "'" +
                " GROUP BY d.type_consult";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");

        String[] couleurs = {"#1d4ed8", "#5b21b6"};
        int idx = 0;

        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("type_consult");
                int    nb   = rs.getInt("nb");
                XYChart.Data<String, Number> data =
                        new XYChart.Data<>(
                                "présentiel".equalsIgnoreCase(type) ? "🏢 Présentiel" : "💻 En ligne",
                                nb);
                series.getData().add(data);
                final String col = couleurs[idx % 2];
                idx++;
                data.nodeProperty().addListener((o, ov, nv) -> {
                    if (nv != null) nv.setStyle("-fx-bar-fill: " + col + ";");
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        chartTypes.getData().add(series);
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private Connection getConn() {
        return MyDataBase_Unimind.getInstance().getConnection();
    }

    private int queryInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private String labelliserStatut(String s) {
        return switch (s.toLowerCase()) {
            case "confirme" -> "✓ Confirmé";
            case "encours"  -> "⏳ En cours";
            case "demande"  -> "🕐 Demande";
            case "terminé"  -> "✅ Terminé";
            case "annulé"   -> "✗ Annulé";
            case "absent"   -> "⚠ Absent";
            default         -> s;
        };
    }

    private String buildEtoiles(int note) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < note ? "★" : "☆");
        return sb.toString();
    }

    private void styleChart(XYChart<?,?> chart) {
        chart.setStyle("-fx-background-color: transparent;");
        chart.lookup(".chart-plot-background")
                .setStyle("-fx-background-color: #faf8ff;");
        chart.setLegendVisible(true);
    }
}