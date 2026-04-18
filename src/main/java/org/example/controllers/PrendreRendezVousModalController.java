package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.DisponibilitePsy;
import org.example.entities.Psychologue;
import org.example.entities.RendezVous;
import org.example.services.DisponibilitePsyService;
import org.example.services.RendezVousService;
import org.example.services.UserService;

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

    // ── Services ────────────────────────────────────────────────────
    private DisponibilitePsyService disponibiliteService;
    private RendezVousService       rendezVousService;
    private UserService             userService;

    // ── Données ─────────────────────────────────────────────────────
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private FilteredList<DisponibilitePsy>   filteredList;
    private DisponibilitePsy                 disponibiliteSelectionnee;
    private int                              etudiantId;
    private Stage                            modalStage;

    // ── Styles cartes ────────────────────────────────────────────────
    private static final String STYLE_CARTE_IDLE =
            "-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; " +
                    "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; " +
                    "-fx-padding: 12 13; -fx-cursor: hand;";

    private static final String STYLE_CARTE_HOVER =
            "-fx-background-color: #faf5ff; -fx-border-color: #c4b5fd; " +
                    "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; " +
                    "-fx-padding: 12 13; -fx-cursor: hand;";

    private static final String STYLE_CARTE_SELECTED =
            "-fx-background-color: #ede9fe; -fx-border-color: #7c3aed; " +
                    "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; " +
                    "-fx-padding: 11 12; -fx-cursor: hand;";

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();
        rendezVousService    = new RendezVousService();
        userService          = new UserService();
        disponibilitesList   = FXCollections.observableArrayList();
        filteredList         = new FilteredList<>(disponibilitesList, p -> true);

        initialiserFiltres();

        btnConfirmer.setOnAction(e -> confirmerReservation());
        btnAnnuler.setOnAction(e   -> fermerModal());
        btnFermer.setOnAction(e    -> fermerModal());
        btnDeselectionner.setOnAction(e -> deselectionner());

        // Hover confirmer
        btnConfirmer.setOnMouseEntered(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#7c3aed","#6d28d9")));
        btnConfirmer.setOnMouseExited(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#6d28d9","#7c3aed")));
        // Hover fermer
        btnFermer.setOnMouseEntered(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("0.12","0.24")));
        btnFermer.setOnMouseExited(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("0.24","0.12")));
    }

    // ── Filtres ─────────────────────────────────────────────────────
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous", "présentiel", "en ligne"));
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
            boolean matchPsy  = psy  == null || "Tous".equals(psy)  || getNomPsy(d.getUserId()).equals(psy);
            boolean matchType = type == null || "Tous".equals(type) || d.getTypeConsult().toString().equalsIgnoreCase(type);
            boolean matchDate = dateMin == null || !d.getDateDispo().toLocalDate().isBefore(dateMin);
            return matchPsy && matchType && matchDate;
        });

        int nb = filteredList.size();
        lblNbCreneaux.setText(nb + " créneau" + (nb > 1 ? "x" : "") + " disponible" + (nb > 1 ? "s" : ""));
        construireGrille();
    }

    // ── Construction grille ─────────────────────────────────────────
    private void construireGrille() {
        gridCreneaux.getChildren().clear();

        if (filteredList.isEmpty()) {
            lblEmptyCreneaux.setVisible(true);
            lblEmptyCreneaux.setManaged(true);
            return;
        }
        lblEmptyCreneaux.setVisible(false);
        lblEmptyCreneaux.setManaged(false);

        for (DisponibilitePsy dispo : filteredList) {
            gridCreneaux.getChildren().add(construireCarte(dispo));
        }
    }

    private VBox construireCarte(DisponibilitePsy dispo) {
        VBox carte = new VBox(6);
        carte.setPrefWidth(210);
        carte.setPrefHeight(118);

        boolean selected = dispo.equals(disponibiliteSelectionnee);
        carte.setStyle(selected ? STYLE_CARTE_SELECTED : STYLE_CARTE_IDLE);

        // Ligne 1 : nom psy + badge type
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        String nomPsy = getNomPsy(dispo.getUserId());
        Label lPsy = new Label(nomPsy);
        lPsy.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + (selected ? "#4c1d95" : "#374151") + ";");
        HBox.setHgrow(lPsy, Priority.ALWAYS);
        lPsy.setMaxWidth(Double.MAX_VALUE);

        boolean presentiel = "présentiel".equalsIgnoreCase(dispo.getTypeConsult().toString());
        Label badge = new Label(presentiel ? "🏢" : "💻");
        badge.setStyle("-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#5b21b6") + "; " +
                "-fx-font-size: 10px; -fx-padding: 2 7; -fx-background-radius: 12;");
        topRow.getChildren().addAll(lPsy, badge);

        // Ligne 2 : date
        LocalDate ld    = dispo.getDateDispo().toLocalDate();
        String jourAbrg = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        String dateStr  = jourAbrg + " " + ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Label lDate = new Label(dateStr);
        lDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + (selected ? "#4c1d95" : "#1f2937") + ";");

        // Ligne 3 : horaire
        String debut = dispo.getHeureDebut().toString().substring(0, 5);
        String fin   = dispo.getHeureFin().toString().substring(0, 5);
        Label lHeure = new Label("🕐 " + debut + " – " + fin);
        lHeure.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + (selected ? "#7c3aed" : "#6366f1") + "; -fx-font-weight: bold;");

        // Ligne 4 : lieu (si présentiel)
        String lieu = dispo.getLieu();
        if (presentiel && lieu != null && !lieu.isEmpty()) {
            Label lLieu = new Label("📍 " + (lieu.length() > 22 ? lieu.substring(0, 22) + "…" : lieu));
            lLieu.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #6b7280;");
            carte.getChildren().addAll(topRow, lDate, lHeure, lLieu);
        } else {
            carte.getChildren().addAll(topRow, lDate, lHeure);
        }

        // Indicateur sélectionné en bas
        if (selected) {
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            Label selLabel = new Label("✓ Sélectionné");
            selLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; " +
                    "-fx-text-fill: #7c3aed; -fx-font-weight: bold;");
            carte.getChildren().addAll(spacer, selLabel);
        }

        // ── Interactions ─────────────────────────────────────────────
        carte.setOnMouseClicked(e -> {
            if (dispo.equals(disponibiliteSelectionnee)) {
                deselectionner();
            } else {
                disponibiliteSelectionnee = dispo;
                afficherBandeau(dispo);
                rafraichirStylesCartes();
            }
        });
        carte.setOnMouseEntered(e -> {
            if (!dispo.equals(disponibiliteSelectionnee))
                carte.setStyle(STYLE_CARTE_HOVER);
        });
        carte.setOnMouseExited(e -> {
            if (!dispo.equals(disponibiliteSelectionnee))
                carte.setStyle(STYLE_CARTE_IDLE);
        });

        return carte;
    }

    private void rafraichirStylesCartes() {
        // Reconstruire toute la grille pour appliquer les bons styles
        construireGrille();
    }

    // ── Bandeau sélection ────────────────────────────────────────────
    private void afficherBandeau(DisponibilitePsy d) {
        String debut  = d.getHeureDebut().toString().substring(0, 5);
        String fin    = d.getHeureFin().toString().substring(0, 5);
        String date   = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        String nomPsy = getNomPsy(d.getUserId());

        lblSelection.setText(nomPsy + "  ·  " + date + "  ·  " + debut + " – " + fin
                + "  ·  " + d.getTypeConsult());
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

    // ── Chargement ───────────────────────────────────────────────────
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

    // ── Confirmation (logique inchangée) ─────────────────────────────
    private void confirmerReservation() {
        if (disponibiliteSelectionnee == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucune sélection",
                    "Veuillez sélectionner un créneau disponible.");
            return;
        }

        String motifSaisi = txtMotif.getText().trim();
        final String motifFinal = motifSaisi.isEmpty() ? "Consultation psychologique" : motifSaisi;
        final DisponibilitePsy dispo = disponibiliteSelectionnee;
        final int etudiantIdFinal   = this.etudiantId;

        String debut  = dispo.getHeureDebut().toString().substring(0, 5);
        String fin    = dispo.getHeureFin().toString().substring(0, 5);
        String nomPsy = getNomPsy(dispo.getUserId());

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de réservation");
        confirmation.setHeaderText("Confirmer la réservation");
        confirmation.setContentText(
                "📅 Date : "         + dispo.getDateDispo()         + "\n" +
                        "⏰ Horaire : "       + debut + " – " + fin           + "\n" +
                        "👨‍⚕️ Psychologue : " + nomPsy                        + "\n" +
                        "💬 Type : "          + dispo.getTypeConsult()       + "\n" +
                        "📝 Motif : "         + motifFinal                   + "\n\n" +
                        "Confirmez-vous cette réservation ?"
        );

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK)
                effectuerReservation(dispo, etudiantIdFinal, motifFinal);
        });
    }

    private void effectuerReservation(DisponibilitePsy dispo, int etudiantId, String motif) {
        try {
            RendezVous rdv = new RendezVous(dispo.getDispoId(), etudiantId, dispo.getUserId(), motif);
            rendezVousService.ajouter(rdv);

            String debut = dispo.getHeureDebut().toString().substring(0, 5);
            String fin   = dispo.getHeureFin().toString().substring(0, 5);

            afficherAlerte(Alert.AlertType.INFORMATION, "Réservation confirmée",
                    "✅ Rendez-vous réservé avec succès !\n\n" +
                            "📅 Date : "    + dispo.getDateDispo() + "\n" +
                            "⏰ Horaire : " + debut + " – " + fin  + "\n" +
                            "📝 Motif : "   + motif);
            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de réserver : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private String getNomPsy(int userId) {
        if (userService == null) return "Psy #" + userId;
        try {
            Psychologue psy = userService.getPsychologueById(userId);
            if (psy != null)
                return "Dr. " + psy.getPrenom() + " " + psy.getNom().toUpperCase();
        } catch (Exception e) {
            System.err.println("Erreur récupération psy : " + e.getMessage());
        }
        return "Psy #" + userId;
    }

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

    public void setEtudiantId(int id) {
        this.etudiantId = id;
        chargerDisponibilites();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}