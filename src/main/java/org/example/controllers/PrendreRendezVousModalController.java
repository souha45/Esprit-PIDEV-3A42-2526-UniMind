package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.DisponibilitePsy;
import org.example.entities.Psychologue;
import org.example.entities.Etudiant;
import org.example.entities.RendezVous;
import org.example.services.*;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    @FXML private Button           btnDictation;
    @FXML private Button btnVueCalendrier;

    // ── Toast + Overlay ─────────────────────────────────────────────
    @FXML private StackPane toastContainer;
    @FXML private StackPane confirmOverlay;

    // ── Services ────────────────────────────────────────────────────
    private DisponibilitePsyService disponibiliteService;
    private RendezVousService       rendezVousService;
    private PsychologueService      psychologueService;
    private EtudiantService         etudiantService;
    private EmailGhofraneService emailService;
    private ReconnaissanceVocaleService reconnaissanceService;

    // ── Cache pour les noms des psychologues ────────────────────────
    private final Map<Integer, String> psyNameCache = new ConcurrentHashMap<>();

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
        etudiantService      = new EtudiantService();
        emailService         = new EmailGhofraneService();
        reconnaissanceService = new ReconnaissanceVocaleService();
        disponibilitesList   = FXCollections.observableArrayList();
        filteredList         = new FilteredList<>(disponibilitesList, p -> true);

        if (lblEmptyCreneaux != null) {
            lblEmptyCreneaux.setText("✨ Aucun créneau disponible pour ces critères");
            lblEmptyCreneaux.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-text-fill:#c4b5fd;");
        }

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

        // Configurer le bouton dictée
        btnDictation.setOnAction(e -> demarrerDictation());
        btnDictation.setOnMouseEntered(e ->
                btnDictation.setStyle(btnDictation.getStyle().replace("#6366f1","#4f46e5")));
        btnDictation.setOnMouseExited(e ->
                btnDictation.setStyle(btnDictation.getStyle().replace("#4f46e5","#6366f1")));

        btnVueCalendrier.setOnAction(e -> ouvrirCalendrierEtudiant());
        btnVueCalendrier.setOnMouseEntered(e ->
                btnVueCalendrier.setStyle(btnVueCalendrier.getStyle().replace("#8b5cf6","#7c3aed")));
        btnVueCalendrier.setOnMouseExited(e ->
                btnVueCalendrier.setStyle(btnVueCalendrier.getStyle().replace("#7c3aed","#8b5cf6")));
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTRES
    // ════════════════════════════════════════════════════════════════
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous", "présentiel", "en_ligne"));
        comboType.setValue("Tous");
        comboPsy.valueProperty().addListener((o, ov, nv)         -> appliquerFiltres());
        comboType.valueProperty().addListener((o, ov, nv)        -> appliquerFiltres());
        datePickerFiltre.valueProperty().addListener((o, ov, nv) -> appliquerFiltres());
    }

    private void remplirComboPsy(List<DisponibilitePsy> liste) {
        ObservableList<String> items = FXCollections.observableArrayList("Tous");
        liste.stream()
                .map(d -> getNomPsy(d.getUserId()))
                .distinct()
                .forEach(items::add);
        comboPsy.setItems(items);
        comboPsy.setValue("Tous");
    }

    private void appliquerFiltres() {
        String psy      = comboPsy.getValue();
        String type     = comboType.getValue();
        LocalDate dateMin = datePickerFiltre.getValue();

        filteredList.setPredicate(d -> {
            boolean mp = psy == null || "Tous".equals(psy) || getNomPsy(d.getUserId()).equals(psy);
            boolean mt = type == null || "Tous".equals(type) || d.getTypeConsult().toString().equalsIgnoreCase(type);
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
    // Remplacer l'ancienne méthode construireGrille() par :
    private void construireGrille() {
        reconstruireGrilleAvecSelection();
    }

    private VBox construireCarte(DisponibilitePsy dispo) {
        VBox carte = new VBox(6);
        carte.setPrefWidth(210);
        carte.setPrefHeight(118);
        carte.setUserData(dispo); // Stocker l'objet pour référence

        boolean selected = (disponibiliteSelectionnee != null &&
                disponibiliteSelectionnee.getDispoId() == dispo.getDispoId());

        carte.setStyle(selected ? STYLE_CARTE_SELECTED : STYLE_CARTE_IDLE);

        // ── HEADER ────────────────────────────────────────────────────
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
                "-fx-text-fill:" + (presentiel ? "#1d4ed8" : "#5b21b6") + ";" +
                "-fx-font-size:10px;-fx-padding:2 7;-fx-background-radius:12;");
        topRow.getChildren().addAll(lPsy, badge);

        // ── DATE ──────────────────────────────────────────────────────
        LocalDate ld = dispo.getDateDispo().toLocalDate();
        String jourAbrg = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        Label lDate = new Label(jourAbrg + " " + ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lDate.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (selected ? "#4c1d95" : "#1f2937") + ";");

        // ── HORAIRE ───────────────────────────────────────────────────
        String debut = dispo.getHeureDebut().toString().substring(0, 5);
        String fin = dispo.getHeureFin().toString().substring(0, 5);
        Label lHeure = new Label("🕐 " + debut + " – " + fin);
        lHeure.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (selected ? "#7c3aed" : "#6366f1") + ";");

        // ── LIEU ──────────────────────────────────────────────────────
        String lieu = dispo.getLieu();
        if (presentiel && lieu != null && !lieu.isEmpty()) {
            Label lLieu = new Label("📍 " + (lieu.length() > 22 ? lieu.substring(0, 22) + "…" : lieu));
            lLieu.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:#6b7280;");
            carte.getChildren().addAll(topRow, lDate, lHeure, lLieu);
        } else {
            carte.getChildren().addAll(topRow, lDate, lHeure);
        }

        // ── BADGE SÉLECTIONNÉ (un SEUL endroit) ───────────────────────
        if (selected) {
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);

            Label selectedBadge = new Label("✓  Sélectionné");
            selectedBadge.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 12; " +
                    "-fx-background-radius: 12;");
            carte.getChildren().addAll(spacer, selectedBadge);
        }

        // ── CLIC ──────────────────────────────────────────────────────
        carte.setOnMouseClicked(e -> {
            if (!selected) {
                // Désélectionner l'ancien
                disponibiliteSelectionnee = dispo;
                afficherBandeau(dispo);
                reconstruireGrilleAvecSelection();
            }
        });

        carte.setOnMouseEntered(e -> {
            if (!selected) {
                carte.setStyle(STYLE_CARTE_HOVER);
            }
        });
        carte.setOnMouseExited(e -> {
            if (!selected) {
                carte.setStyle(STYLE_CARTE_IDLE);
            }
        });

        return carte;
    }

    // ════════════════════════════════════════════════════════════════
    //  BANDEAU SÉLECTION
    // ════════════════════════════════════════════════════════════════
    private void afficherBandeau(DisponibilitePsy d) {
        if (d == null) return;

        String debut = d.getHeureDebut().toString().substring(0, 5);
        String fin = d.getHeureFin().toString().substring(0, 5);
        String date = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        // Capitaliser la première lettre
        date = date.substring(0, 1).toUpperCase() + date.substring(1);
        String nomPsy = getNomPsy(d.getUserId());

        lblSelection.setText(nomPsy + "  ·  " + date + "  ·  " + debut + " – " + fin + "  ·  " + d.getTypeConsult());
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
    //  RÉSERVATION
    // ════════════════════════════════════════════════════════════════
    private void effectuerReservation(DisponibilitePsy dispo, int etudiantId, String motif) {
        try {
            RendezVous rdv = new RendezVous(dispo.getDispoId(), etudiantId, dispo.getUserId(), motif);
            rendezVousService.ajouter(rdv);

            try {
                Psychologue psychologue = psychologueService.getPsychologueById(dispo.getUserId());
                Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);

                if (psychologue != null && psychologue.getEmail() != null && !psychologue.getEmail().isEmpty()) {
                    String nomEtudiant = (etudiant != null && etudiant.getNom() != null) ? etudiant.getNom() : "Étudiant";
                    String prenomEtudiant = (etudiant != null && etudiant.getPrenom() != null) ? etudiant.getPrenom() : "";
                    String emailEtudiant = (etudiant != null && etudiant.getEmail() != null) ? etudiant.getEmail() : "Non renseigné";

                    emailService.envoyerEmailNouveauRdvAuPsy(
                            psychologue.getEmail(), psychologue.getNom(), psychologue.getPrenom(),
                            nomEtudiant, prenomEtudiant, emailEtudiant,
                            dispo.getDateDispo().toString(),
                            dispo.getHeureDebut().toString().substring(0, 5),
                            dispo.getHeureFin().toString().substring(0, 5),
                            dispo.getTypeConsult().toString(),
                            dispo.getLieu(), motif
                    );
                    showToast("✓ Rendez-vous réservé ! Un email a été envoyé au psychologue.", ToastType.SUCCESS);
                } else {
                    showToast("✓ Rendez-vous réservé ! (Email non envoyé)", ToastType.WARNING);
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur email: " + e.getMessage());
                showToast("✓ Rendez-vous réservé ! (Email non envoyé)", ToastType.WARNING);
            }

            new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                    Duration.seconds(1.8), e -> fermerModal()
            )).play();

        } catch (SQLException e) {
            showToast("✗ Impossible de réserver : " + e.getMessage(), ToastType.ERROR);
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CONFIRMATION
    // ════════════════════════════════════════════════════════════════
    private void confirmerReservation() {
        if (disponibiliteSelectionnee == null) {
            showToast("⚠ Veuillez sélectionner un créneau disponible.", ToastType.WARNING);
            return;
        }

        String motifSaisi = txtMotif.getText().trim();
        String motifFinal = motifSaisi.isEmpty() ? "Consultation psychologique" : motifSaisi;
        DisponibilitePsy d = disponibiliteSelectionnee;

        String debut = d.getHeureDebut().toString().substring(0, 5);
        String fin = d.getHeureFin().toString().substring(0, 5);
        String nomPsy = getNomPsy(d.getUserId());
        String dateStr = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        dateStr = dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1);

        VBox card = new VBox(0);
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.28),24,0,0,6);");

        VBox cardHeader = new VBox(3);
        cardHeader.setStyle("-fx-background-color:linear-gradient(to bottom right,#4c1d95,#7c3aed);" +
                "-fx-padding:18 22 16 22;-fx-background-radius:16 16 0 0;");
        Label hTitle = new Label("📋  Confirmer la réservation");
        hTitle.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#ffffff;");
        Label hSub = new Label("Vérifiez les détails avant de confirmer");
        hSub.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.72);");
        cardHeader.getChildren().addAll(hTitle, hSub);

        VBox cardBody = new VBox(10);
        cardBody.setStyle("-fx-padding:18 22 14 22;");

        cardBody.getChildren().addAll(
                rowDetail("👨‍⚕️  Psychologue", nomPsy, "#4c1d95"),
                rowDetail("📅  Date", dateStr, "#374151"),
                rowDetail("🕐  Horaire", debut + " – " + fin, "#374151"),
                rowDetail("💬  Type", d.getTypeConsult().toString(), "#374151"),
                rowDetail("📝  Motif", motifFinal, "#374151")
        );

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

        confirmOverlay.getChildren().clear();
        confirmOverlay.getChildren().add(card);
        confirmOverlay.setVisible(true);
        confirmOverlay.setManaged(true);
        StackPane.setAlignment(card, Pos.CENTER);

        FadeTransition ft = new FadeTransition(Duration.millis(180), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        final String mf = motifFinal;
        final DisponibilitePsy dFinal = d;
        final int etudiantIdFinal = this.etudiantId;

        btnNon.setOnAction(e -> fermerOverlayConfirm(card));
        btnOui.setOnAction(e -> {
            fermerOverlayConfirm(card);
            effectuerReservation(dFinal, etudiantIdFinal, mf);
        });
        confirmOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == confirmOverlay) fermerOverlayConfirm(card);
        });
    }

    private HBox rowDetail(String label, String value, String color) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:6 12;-fx-background-color:#f5f3ff;-fx-background-radius:8;");
        Label lbl = new Label(label);
        lbl.setMinWidth(130);
        lbl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:#9ca3af;-fx-font-weight:bold;");
        Label val = new Label(value);
        val.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-text-fill:" + color + ";-fx-font-weight:bold;");
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
    //  TOAST
    // ════════════════════════════════════════════════════════════════
    private enum ToastType { SUCCESS, WARNING, ERROR }

    private void showToast(String message, ToastType type) {
        if (toastContainer == null) {
            Alert alert = new Alert(type == ToastType.ERROR ? Alert.AlertType.ERROR :
                    type == ToastType.WARNING ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
            alert.setContentText(message);
            alert.show();
            return;
        }

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
        if (psyNameCache.containsKey(userId)) {
            return psyNameCache.get(userId);
        }
        try {
            Psychologue psy = psychologueService.getPsychologueById(userId);
            if (psy != null) {
                String nom = "Dr. " + psy.getPrenom() + " " + psy.getNom().toUpperCase();
                psyNameCache.put(userId, nom);
                return nom;
            }
        } catch (Exception e) {
            System.err.println("Erreur récupération psy ID " + userId);
        }
        String fallback = "Psy #" + userId;
        psyNameCache.put(userId, fallback);
        return fallback;
    }

    private void fermerModal() {
        if (modalStage != null) modalStage.close();
    }

    // ════════════════════════════════════════════════════════════════
    //  DICTÉE VOCALE
    // ════════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════════
    //  DICTÉE VOCALE  — remplacer la méthode demarrerDictation()
    //  existante par celle-ci dans PrendreRendezVousModalController
    // ════════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════════
    //  Remplacer UNIQUEMENT la méthode demarrerDictation() existante
    //  dans PrendreRendezVousModalController par celle-ci
    // ════════════════════════════════════════════════════════════════
    private void demarrerDictation() {
        if (!reconnaissanceService.isReady()) {
            showToast("Service vocal non disponible. Vérifiez le modèle Vosk.", ToastType.ERROR);
            return;
        }

        Stage dictationStage = new Stage();
        dictationStage.setTitle("Dictée vocale");
        dictationStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dictationStage.initOwner(btnDictation.getScene().getWindow());
        dictationStage.setResizable(false);

        // ── Indicateur clignotant ────────────────────────────────────
        Label lblIndicateur = new Label("⚪");
        lblIndicateur.setStyle("-fx-font-size:30px;");

        Label lblEtat = new Label("Cliquez sur Démarrer puis parlez");
        lblEtat.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-text-fill:#6b7280;");
        lblEtat.setWrapText(true);
        lblEtat.setMaxWidth(400);

        // ── Zone résultat ────────────────────────────────────────────
        TextArea txtResultat = new TextArea();
        txtResultat.setPromptText("Le texte reconnu apparaîtra ici après l'arrêt...");
        txtResultat.setPrefHeight(100);
        txtResultat.setEditable(true);
        txtResultat.setWrapText(true);
        txtResultat.setStyle("-fx-background-color:#ffffff;-fx-border-color:#e0e7ff;" +
                "-fx-border-radius:10;-fx-background-radius:10;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:13px;");

        // ── Animation ────────────────────────────────────────────────
        javafx.animation.Timeline clignotement = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(500),
                        e -> lblIndicateur.setText(
                                lblIndicateur.getText().equals("🔴") ? "⚪" : "🔴"))
        );
        clignotement.setCycleCount(javafx.animation.Timeline.INDEFINITE);

        // ── Boutons ──────────────────────────────────────────────────
        Button btnStart = new Button("▶  Démarrer");
        btnStart.setStyle("-fx-background-color:#10b981;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:10 24;-fx-background-radius:8;-fx-cursor:hand;");

        Button btnStop = new Button("⏹  Arrêter");
        btnStop.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:10 24;-fx-background-radius:8;-fx-cursor:hand;");
        btnStop.setDisable(true);

        Button btnValider = new Button("✓  Utiliser ce texte");
        btnValider.setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:10 22;-fx-background-radius:8;-fx-cursor:hand;");
        btnValider.setDisable(true);

        Button btnFermerDictee = new Button("✕  Fermer");
        btnFermerDictee.setStyle("-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;" +
                "-fx-padding:10 20;-fx-background-radius:8;-fx-cursor:hand;");

        // ── Démarrer ─────────────────────────────────────────────────
        btnStart.setOnAction(e -> {
            txtResultat.clear();
            btnStart.setDisable(true);
            btnStop.setDisable(false);
            btnValider.setDisable(true);
            lblIndicateur.setText("🔴");
            lblEtat.setText("🎙 Écoute en cours... Parlez normalement en français");
            lblEtat.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-text-fill:#059669;");
            clignotement.play();

            // Démarrer la reconnaissance (non bloquant)
            reconnaissanceService.demarrerReconnaissance();
        });

        // ── Arrêter ──────────────────────────────────────────────────
        btnStop.setOnAction(e -> {
            btnStop.setDisable(true);
            clignotement.stop();
            lblIndicateur.setText("⚪");
            lblEtat.setText("⏳ Traitement en cours...");
            lblEtat.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-text-fill:#f59e0b;");

            // CRITIQUE : arrêter dans un thread séparé (jamais sur le thread UI)
            // sinon join(4000) gèle complètement l'interface
            Thread stopThread = new Thread(() -> {
                // Cet appel bloque jusqu'à 4s le temps de vider le buffer
                String texte = reconnaissanceService.arreterReconnaissance();

                // Retourner sur le thread UI pour mettre à jour l'affichage
                javafx.application.Platform.runLater(() -> {
                    btnStart.setDisable(false);

                    if (texte != null && !texte.isEmpty()) {
                        txtResultat.setText(texte);
                        btnValider.setDisable(false);
                        lblEtat.setText("✅ Texte reconnu ! Modifiez si nécessaire puis validez.");
                        lblEtat.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;" +
                                "-fx-text-fill:#059669;-fx-font-weight:bold;");
                    } else {
                        lblEtat.setText("❌ Aucun texte reconnu. Réessayez en parlant plus fort et distinctement.");
                        lblEtat.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-text-fill:#ef4444;");
                    }
                });
            }, "vosk-stop-thread");

            stopThread.setDaemon(true);
            stopThread.start();
        });

        // ── Valider ──────────────────────────────────────────────────
        btnValider.setOnAction(e -> {
            String texte = txtResultat.getText().trim();
            if (!texte.isEmpty()) {
                // Capitaliser la première lettre
                String texteFormate = Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
                String actuel = txtMotif.getText().trim();
                txtMotif.setText(actuel.isEmpty() ? texteFormate : actuel + " " + texteFormate);
                showToast("Texte ajouté au motif !", ToastType.SUCCESS);
            }
            dictationStage.close();
        });

        // ── Fermer ───────────────────────────────────────────────────
        btnFermerDictee.setOnAction(e -> {
            arreterSiEnCours(clignotement);
            dictationStage.close();
        });

        dictationStage.setOnCloseRequest(e -> arreterSiEnCours(clignotement));

        // ── Layout ───────────────────────────────────────────────────
        Label titre = new Label("🎤  Dictée vocale");
        titre.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:17px;" +
                "-fx-font-weight:bold;-fx-text-fill:#3730a3;");

        Label conseil = new Label("Parlez distinctement • Phrases courtes • En français");
        conseil.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:#9ca3af;");

        javafx.scene.layout.HBox boxBtns = new javafx.scene.layout.HBox(14, btnStart, btnStop);
        boxBtns.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.HBox boxActions = new javafx.scene.layout.HBox(14, btnValider, btnFermerDictee);
        boxActions.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(16);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setStyle("-fx-background-color:#f0f4ff;-fx-padding:28 32;");
        root.getChildren().addAll(titre, conseil, lblIndicateur, lblEtat,
                boxBtns, txtResultat, boxActions);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 480, 380);
        dictationStage.setScene(scene);
        dictationStage.showAndWait();
    }

    /** Arrête proprement si l'enregistrement est en cours */
    private void arreterSiEnCours(javafx.animation.Timeline clignotement) {
        if (reconnaissanceService.isRunning()) {
            clignotement.stop();
            Thread t = new Thread(() -> reconnaissanceService.arreterReconnaissance(), "vosk-cleanup");
            t.setDaemon(true);
            t.start();
        }
    }

    public void setEtudiantId(int id) {
        this.etudiantId = id;
        chargerDisponibilites();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }


    /**
     * Reconstruit la grille et met en surbrillance le créneau sélectionné
     */
    private void reconstruireGrilleAvecSelection() {
        gridCreneaux.getChildren().clear();

        if (filteredList.isEmpty()) {
            if (lblEmptyCreneaux != null) {
                lblEmptyCreneaux.setVisible(true);
                lblEmptyCreneaux.setManaged(true);
            }
            return;
        }

        if (lblEmptyCreneaux != null) {
            lblEmptyCreneaux.setVisible(false);
            lblEmptyCreneaux.setManaged(false);
        }

        for (DisponibilitePsy d : filteredList) {
            VBox carte = construireCarte(d);  // La carte gère elle-même son badge
            gridCreneaux.getChildren().add(carte);
        }

        // Mettre à jour le label du nombre de créneaux
        int nb = filteredList.size();
        if (lblNbCreneaux != null) {
            lblNbCreneaux.setText(nb + " créneau" + (nb > 1 ? "x" : "") + " disponible" + (nb > 1 ? "s" : ""));
        }
    }

    // ✅ La méthode qui ouvre le calendrier
    private void ouvrirCalendrierEtudiant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CalendrierDisponibiliteEtudiant.fxml"));
            Stage calendrierStage = new Stage();
            Scene scene = new Scene(loader.load());

            calendrierStage.initModality(Modality.WINDOW_MODAL);
            calendrierStage.initOwner(btnVueCalendrier.getScene().getWindow());
            calendrierStage.setTitle("Calendrier des disponibilités");
            calendrierStage.setScene(scene);
            calendrierStage.setResizable(false);

            CalendrierDisponibiliteEtudiantController controller = loader.getController();
            controller.setModalStage(calendrierStage);

            // ✅ Callback amélioré
            controller.setOnCreneauSelectionne(dispo -> {
                javafx.application.Platform.runLater(() -> {
                    // 1. Stocker le créneau sélectionné
                    disponibiliteSelectionnee = dispo;

                    // 2. Afficher le bandeau de sélection
                    afficherBandeau(dispo);

                    // 3. 🔥 FORCER le rafraîchissement de la grille avec la sélection
                    reconstruireGrilleAvecSelection();

                    // 4. Optionnel : fermer le calendrier
                    calendrierStage.close();

                    // 5. Afficher un toast de confirmation
                    showToast("✓ Créneau sélectionné : " + getNomPsy(dispo.getUserId()) + " - " +
                            dispo.getDateDispo().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            " " + dispo.getHeureDebut().toString().substring(0,5) + "h", ToastType.SUCCESS);
                });
            });

            calendrierStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showToast("❌ Impossible d'ouvrir le calendrier", ToastType.ERROR);
        }
    }


}