package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.DisponibilitePsy;
import org.example.entities.Psychologue;
import org.example.entities.RendezVous;
import org.example.services.DisponibilitePsyService;
import org.example.services.PsychologueService;
import org.example.services.RendezVousService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class PrendreRendezVousModalController {

    // ── FXML ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> comboPsy;
    @FXML private ComboBox<String> comboType;
    @FXML private DatePicker       datePickerFiltre;
    @FXML private TilePane         gridCreneaux;
    @FXML private Label            lblEmptyCreneaux;
    @FXML private HBox             boxSelection;
    @FXML private Label            lblSelection;
    @FXML private Label            lblNbCreneaux;
    @FXML private Button           btnDeselectionner;
    @FXML private TextArea         txtMotif;
    @FXML private Button           btnFermer;
    @FXML private Button           btnAnnuler;
    @FXML private Button           btnConfirmer;

    // ── Toast + Overlay overlay ─────────────────────────────────────
    @FXML private StackPane toastContainer;
    @FXML private StackPane confirmOverlay;

    // ── Services ────────────────────────────────────────────────────
    private DisponibilitePsyService disponibiliteService;
    private RendezVousService       rendezVousService;
    private PsychologueService      psychologueService;

    // ── Données ─────────────────────────────────────────────────────
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private FilteredList<DisponibilitePsy>   filteredList;
    private DisponibilitePsy                 disponibiliteSelectionnee;
    private int                              etudiantId;
    private Stage                            modalStage;

    // ── Styles cartes ────────────────────────────────────────────────
    private static final String STYLE_CARTE_IDLE =
            "-fx-background-color:#ffffff;-fx-border-color:#e5e7eb;" +
                    "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-padding:12 13;-fx-cursor:hand;";
    private static final String STYLE_CARTE_HOVER =
            "-fx-background-color:#faf5ff;-fx-border-color:#c4b5fd;" +
                    "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-padding:12 13;-fx-cursor:hand;";
    private static final String STYLE_CARTE_SELECTED =
            "-fx-background-color:#ede9fe;-fx-border-color:#7c3aed;" +
                    "-fx-border-width:2;-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-padding:11 12;-fx-cursor:hand;";

    // ════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();
        rendezVousService    = new RendezVousService();
        psychologueService   = new PsychologueService();
        disponibilitesList   = FXCollections.observableArrayList();
        filteredList         = new FilteredList<>(disponibilitesList, p -> true);

        initialiserFiltres();

        btnConfirmer.setOnAction(e -> confirmerReservation());
        btnAnnuler.setOnAction(e   -> fermerModal());
        btnFermer.setOnAction(e    -> fermerModal());
        btnDeselectionner.setOnAction(e -> deselectionner());

        btnConfirmer.setOnMouseEntered(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#7c3aed","#6d28d9")));
        btnConfirmer.setOnMouseExited(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#6d28d9","#7c3aed")));
        btnFermer.setOnMouseEntered(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("0.12","0.24")));
        btnFermer.setOnMouseExited(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("0.24","0.12")));
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTRES
    // ════════════════════════════════════════════════════════════════
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous","présentiel","en_ligne"));
        comboType.setValue("Tous");
        comboPsy.valueProperty().addListener((o,ov,nv)         -> appliquerFiltres());
        comboType.valueProperty().addListener((o,ov,nv)        -> appliquerFiltres());
        datePickerFiltre.valueProperty().addListener((o,ov,nv) -> appliquerFiltres());
    }

    private void remplirComboPsy(List<DisponibilitePsy> liste) {
        ObservableList<String> items = FXCollections.observableArrayList("Tous");
        liste.stream().map(d -> getNomPsy(d.getUserId())).distinct().forEach(items::add);
        comboPsy.setItems(items);
        comboPsy.setValue("Tous");
    }

    private void appliquerFiltres() {
        String psy      = comboPsy.getValue();
        String type     = comboType.getValue();
        LocalDate dateMin = datePickerFiltre.getValue();
        filteredList.setPredicate(d -> {
            boolean mp = psy   == null || "Tous".equals(psy)  || getNomPsy(d.getUserId()).equals(psy);
            boolean mt = type  == null || "Tous".equals(type) || d.getTypeConsult().toString().equalsIgnoreCase(type);
            boolean md = dateMin == null || !d.getDateDispo().toLocalDate().isBefore(dateMin);
            return mp && mt && md;
        });
        int nb = filteredList.size();
        lblNbCreneaux.setText(nb + " créneau" + (nb > 1 ? "x" : "") + " disponible" + (nb > 1 ? "s" : ""));
        construireGrille();
    }

    // ════════════════════════════════════════════════════════════════
    //  GRILLE
    // ════════════════════════════════════════════════════════════════
    private void construireGrille() {
        gridCreneaux.getChildren().clear();
        if (filteredList.isEmpty()) {
            lblEmptyCreneaux.setVisible(true);
            lblEmptyCreneaux.setManaged(true);
            return;
        }
        lblEmptyCreneaux.setVisible(false);
        lblEmptyCreneaux.setManaged(false);
        for (DisponibilitePsy d : filteredList)
            gridCreneaux.getChildren().add(construireCarte(d));
    }

    private VBox construireCarte(DisponibilitePsy dispo) {
        VBox carte = new VBox(6);
        carte.setPrefWidth(210);
        carte.setPrefHeight(118);
        boolean selected = dispo.equals(disponibiliteSelectionnee);
        carte.setStyle(selected ? STYLE_CARTE_SELECTED : STYLE_CARTE_IDLE);

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lPsy = new Label(getNomPsy(dispo.getUserId()));
        lPsy.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (selected ? "#4c1d95" : "#374151") + ";");
        HBox.setHgrow(lPsy, Priority.ALWAYS);
        lPsy.setMaxWidth(Double.MAX_VALUE);

        boolean presentiel = "présentiel".equalsIgnoreCase(dispo.getTypeConsult().toString());
        Label badge = new Label(presentiel ? "🏢" : "💻");
        badge.setStyle("-fx-background-color:" + (presentiel ? "#dbeafe" : "#ede9fe") + ";" +
                "-fx-text-fill:"       + (presentiel ? "#1d4ed8" : "#5b21b6") + ";" +
                "-fx-font-size:10px;-fx-padding:2 7;-fx-background-radius:12;");
        topRow.getChildren().addAll(lPsy, badge);

        LocalDate ld    = dispo.getDateDispo().toLocalDate();
        String jourAbrg = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        Label lDate = new Label(jourAbrg + " " + ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lDate.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (selected ? "#4c1d95" : "#1f2937") + ";");

        String debut = dispo.getHeureDebut().toString().substring(0, 5);
        String fin   = dispo.getHeureFin().toString().substring(0, 5);
        Label lHeure = new Label("🕐 " + debut + " – " + fin);
        lHeure.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (selected ? "#7c3aed" : "#6366f1") + ";");

        String lieu = dispo.getLieu();
        if (presentiel && lieu != null && !lieu.isEmpty()) {
            Label lLieu = new Label("📍 " + (lieu.length() > 22 ? lieu.substring(0,22)+"…" : lieu));
            lLieu.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:#6b7280;");
            carte.getChildren().addAll(topRow, lDate, lHeure, lLieu);
        } else {
            carte.getChildren().addAll(topRow, lDate, lHeure);
        }

        if (selected) {
            Region sp = new Region(); VBox.setVgrow(sp, Priority.ALWAYS);
            Label sl = new Label("✓ Sélectionné");
            sl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:10px;" +
                    "-fx-text-fill:#7c3aed;-fx-font-weight:bold;");
            carte.getChildren().addAll(sp, sl);
        }

        carte.setOnMouseClicked(e -> {
            if (dispo.equals(disponibiliteSelectionnee)) deselectionner();
            else { disponibiliteSelectionnee = dispo; afficherBandeau(dispo); rafraichirStylesCartes(); }
        });
        carte.setOnMouseEntered(e -> { if (!dispo.equals(disponibiliteSelectionnee)) carte.setStyle(STYLE_CARTE_HOVER); });
        carte.setOnMouseExited(e  -> { if (!dispo.equals(disponibiliteSelectionnee)) carte.setStyle(STYLE_CARTE_IDLE);  });

        return carte;
    }

    private void rafraichirStylesCartes() { construireGrille(); }

    // ════════════════════════════════════════════════════════════════
    //  BANDEAU SÉLECTION
    // ════════════════════════════════════════════════════════════════
    private void afficherBandeau(DisponibilitePsy d) {
        String debut  = d.getHeureDebut().toString().substring(0, 5);
        String fin    = d.getHeureFin().toString().substring(0, 5);
        String date   = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        lblSelection.setText(getNomPsy(d.getUserId()) + "  ·  " + date
                + "  ·  " + debut + " – " + fin + "  ·  " + d.getTypeConsult());
        boxSelection.setVisible(true);
        boxSelection.setManaged(true);
    }

    private void deselectionner() {
        disponibiliteSelectionnee = null;
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);
        lblSelection.setText("");
        construireGrille();
    }

    // ════════════════════════════════════════════════════════════════
    //  CHARGEMENT
    // ════════════════════════════════════════════════════════════════
    private void chargerDisponibilites() {
        try {
            List<DisponibilitePsy> liste = disponibiliteService.afficherDisponibilitesDisponibles();
            disponibilitesList.setAll(liste);
            remplirComboPsy(liste);
            appliquerFiltres();
        } catch (SQLException e) {
            lblNbCreneaux.setText("Erreur de chargement");
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CONFIRMATION — modal overlay moderne (remplace Alert.CONFIRMATION)
    // ════════════════════════════════════════════════════════════════
    private void confirmerReservation() {
        if (disponibiliteSelectionnee == null) {
            showToast("⚠  Veuillez sélectionner un créneau disponible.", ToastType.WARNING);
            return;
        }

        String motifSaisi  = txtMotif.getText().trim();
        String motifFinal  = motifSaisi.isEmpty() ? "Consultation psychologique" : motifSaisi;
        DisponibilitePsy d = disponibiliteSelectionnee;

        String debut   = d.getHeureDebut().toString().substring(0, 5);
        String fin     = d.getHeureFin().toString().substring(0, 5);
        String nomPsy  = getNomPsy(d.getUserId());
        String dateStr = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        dateStr = dateStr.substring(0,1).toUpperCase() + dateStr.substring(1);

        // ── Construire le card overlay ────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.28),24,0,0,6);");

        // Header
        VBox cardHeader = new VBox(3);
        cardHeader.setStyle("-fx-background-color:linear-gradient(to bottom right,#4c1d95,#7c3aed);" +
                "-fx-padding:18 22 16 22;-fx-background-radius:16 16 0 0;");
        Label hTitle = new Label("📋  Confirmer la réservation");
        hTitle.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#ffffff;");
        Label hSub = new Label("Vérifiez les détails avant de confirmer");
        hSub.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.72);");
        cardHeader.getChildren().addAll(hTitle, hSub);

        // Body
        VBox cardBody = new VBox(10);
        cardBody.setStyle("-fx-padding:18 22 14 22;");

        cardBody.getChildren().addAll(
                rowDetail("👨‍⚕️  Psychologue", nomPsy,   "#4c1d95"),
                rowDetail("📅  Date",           dateStr,  "#374151"),
                rowDetail("🕐  Horaire",         debut + " – " + fin, "#374151"),
                rowDetail("💬  Type",            d.getTypeConsult().toString(), "#374151"),
                rowDetail("📝  Motif",           motifFinal, "#374151")
        );

        // Footer
        HBox cardFooter = new HBox(10);
        cardFooter.setAlignment(Pos.CENTER_RIGHT);
        cardFooter.setStyle("-fx-padding:12 22 18 22;" +
                "-fx-border-color:#f3e8ff;-fx-border-width:1 0 0 0;");

        Button btnNon = new Button("✕  Annuler");
        btnNon.setStyle("-fx-background-color:#f5f3ff;-fx-text-fill:#7c3aed;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:9 20;-fx-background-radius:10;" +
                "-fx-border-color:#ddd6fe;-fx-border-width:1;-fx-border-radius:10;-fx-cursor:hand;");
        btnNon.setOnMouseEntered(e -> btnNon.setStyle(btnNon.getStyle().replace("#f5f3ff","#ede9fe")));
        btnNon.setOnMouseExited(e  -> btnNon.setStyle(btnNon.getStyle().replace("#ede9fe","#f5f3ff")));

        Button btnOui = new Button("✓  Confirmer");
        btnOui.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:#ffffff;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:9 22;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.35),8,0,0,2);");
        btnOui.setOnMouseEntered(e -> btnOui.setStyle(btnOui.getStyle().replace("#7c3aed","#6d28d9")));
        btnOui.setOnMouseExited(e  -> btnOui.setStyle(btnOui.getStyle().replace("#6d28d9","#7c3aed")));

        cardFooter.getChildren().addAll(btnNon, btnOui);
        card.getChildren().addAll(cardHeader, cardBody, cardFooter);

        // Afficher l'overlay
        confirmOverlay.getChildren().add(card);
        confirmOverlay.setVisible(true);
        confirmOverlay.setManaged(true);
        StackPane.setAlignment(card, Pos.CENTER);

        // Fade-in
        FadeTransition ft = new FadeTransition(Duration.millis(180), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Actions
        final String mf = motifFinal;
        btnNon.setOnAction(e -> fermerOverlayConfirm(card));
        confirmOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == confirmOverlay) fermerOverlayConfirm(card);
        });
        btnOui.setOnAction(e -> {
            fermerOverlayConfirm(card);
            effectuerReservation(d, etudiantId, mf);
        });
    }

    private HBox rowDetail(String label, String value, String valueColor) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:6 12;-fx-background-color:#f5f3ff;" +
                "-fx-background-radius:8;");
        Label lbl = new Label(label);
        lbl.setMinWidth(130);
        lbl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;" +
                "-fx-text-fill:#9ca3af;-fx-font-weight:bold;");
        Label val = new Label(value);
        val.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;" +
                "-fx-text-fill:" + valueColor + ";-fx-font-weight:bold;");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private void fermerOverlayConfirm(VBox card) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), card);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            confirmOverlay.getChildren().remove(card);
            if (confirmOverlay.getChildren().isEmpty()) {
                confirmOverlay.setVisible(false);
                confirmOverlay.setManaged(false);
            }
        });
        ft.play();
    }

    // ════════════════════════════════════════════════════════════════
    //  RÉSERVATION
    // ════════════════════════════════════════════════════════════════
    private void effectuerReservation(DisponibilitePsy dispo, int etudiantId, String motif) {
        try {
            RendezVous rdv = new RendezVous(dispo.getDispoId(), etudiantId, dispo.getUserId(), motif);
            rendezVousService.ajouter(rdv);
            showToast("✓  Rendez-vous réservé avec succès !", ToastType.SUCCESS);
            // Ferme le modal après 1,8 s
            new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                    Duration.seconds(1.8),
                    e -> fermerModal()
            )).play();
        } catch (SQLException e) {
            showToast("✗  Impossible de réserver : " + e.getMessage(), ToastType.ERROR);
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  TOAST
    // ════════════════════════════════════════════════════════════════
    private enum ToastType { SUCCESS, WARNING, ERROR }

    private void showToast(String message, ToastType type) {
        String bg = switch (type) {
            case SUCCESS -> "#10b981";
            case WARNING -> "#f59e0b";
            case ERROR   -> "#ef4444";
        };
        Label pill = new Label(message);
        pill.setWrapText(true);
        pill.setMaxWidth(480);
        pill.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-padding:12 22;-fx-background-radius:30;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.20),12,0,0,4);");

        toastContainer.getChildren().add(pill);
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        StackPane.setAlignment(pill, Pos.BOTTOM_CENTER);

        FadeTransition fi = new FadeTransition(Duration.millis(200), pill);
        fi.setFromValue(0); fi.setToValue(1);

        FadeTransition fo = new FadeTransition(Duration.millis(400), pill);
        fo.setDelay(Duration.seconds(type == ToastType.SUCCESS ? 1.4 : 2.4));
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> {
            toastContainer.getChildren().remove(pill);
            if (toastContainer.getChildren().isEmpty()) {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            }
        });
        fi.play(); fo.play();
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private String getNomPsy(int userId) {
        if (psychologueService == null) return "Psy #" + userId;
        try {
            Psychologue psy = psychologueService.getPsychologueById(userId);
            if (psy != null)
                return "Dr. " + psy.getPrenom() + " " + psy.getNom().toUpperCase();
        } catch (Exception e) {
            System.err.println("Erreur récupération psy : " + e.getMessage());
        }
        return "Psy #" + userId;
    }

    private void fermerModal() {
        if (modalStage != null) modalStage.close();
    }

    public void setEtudiantId(int id) {
        this.etudiantId = id;
        chargerDisponibilites();
    }

    public void setModalStage(Stage stage) { this.modalStage = stage; }
}