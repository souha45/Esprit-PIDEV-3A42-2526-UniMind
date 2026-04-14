package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.models.DisponibilitePsy;
import org.example.models.RendezVous;
import org.example.services.DisponibilitePsyService;
import org.example.services.RendezVousService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PrendreRendezVousModalController {

    // ── FXML ────────────────────────────────────────────────────────
    // Filtres
    @FXML private ComboBox<String> comboPsy;
    @FXML private ComboBox<String> comboType;
    @FXML private DatePicker       datePickerFiltre;

    // Table
    @FXML private TableView<DisponibilitePsy>             tableViewDisponibilites;
    @FXML private TableColumn<DisponibilitePsy, String>   colPsychologue;
    @FXML private TableColumn<DisponibilitePsy, String>   colDate;
    @FXML private TableColumn<DisponibilitePsy, String>   colHeure;
    @FXML private TableColumn<DisponibilitePsy, String>   colType;
    @FXML private TableColumn<DisponibilitePsy, String>   colLieu;

    // Sélection
    @FXML private HBox   boxSelection;
    @FXML private Label  lblSelection;
    @FXML private Label  lblNbCreneaux;
    @FXML private Button btnDeselectionner;

    // Motif + actions
    @FXML private TextArea txtMotif;
    @FXML private Button   btnFermer;
    @FXML private Button   btnAnnuler;
    @FXML private Button   btnConfirmer;

    // ── Services ────────────────────────────────────────────────────
    private DisponibilitePsyService disponibiliteService;
    private RendezVousService       rendezVousService;

    // ── Données ─────────────────────────────────────────────────────
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private FilteredList<DisponibilitePsy>   filteredList;
    private DisponibilitePsy                 disponibiliteSelectionnee;
    private int                              etudiantId;
    private Stage                            modalStage;

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();
        rendezVousService    = new RendezVousService();
        disponibilitesList   = FXCollections.observableArrayList();
        filteredList         = new FilteredList<>(disponibilitesList, p -> true);

        initialiserFiltres();
        configurerColonnes();
        configurerSelection();

        tableViewDisponibilites.setItems(filteredList);
        tableViewDisponibilites.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        btnConfirmer.setOnAction(e -> confirmerReservation());
        btnAnnuler.setOnAction(e  -> fermerModal());
        btnFermer.setOnAction(e   -> fermerModal());
        btnDeselectionner.setOnAction(e -> deselectionner());

        // Hover bouton confirmer
        btnConfirmer.setOnMouseEntered(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#6366f1","#4f46e5")));
        btnConfirmer.setOnMouseExited(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#4f46e5","#6366f1")));

        // Hover bouton fermer header
        btnFermer.setOnMouseEntered(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("rgba(255,255,255,0.18)","rgba(255,255,255,0.30)")));
        btnFermer.setOnMouseExited(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("rgba(255,255,255,0.30)","rgba(255,255,255,0.18)")));
    }

    // ── Filtres ─────────────────────────────────────────────────────
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous", "présentiel", "en ligne"));
        comboType.setValue("Tous");

        // Listeners
        comboPsy.valueProperty().addListener((o, ov, nv)          -> appliquerFiltres());
        comboType.valueProperty().addListener((o, ov, nv)         -> appliquerFiltres());
        datePickerFiltre.valueProperty().addListener((o, ov, nv)  -> appliquerFiltres());
    }

    private void remplirComboPsy(List<DisponibilitePsy> liste) {
        // Note : la liste ne contient pas le nom du psy — on affiche l'ID en attendant
        // Si vous avez un service qui retourne le nom, remplacez ici
        ObservableList<String> items = FXCollections.observableArrayList("Tous");
        liste.stream()
                .map(d -> "Psy #" + d.getUserId())
                .distinct()
                .forEach(items::add);
        comboPsy.setItems(items);
        comboPsy.setValue("Tous");
    }

    private void appliquerFiltres() {
        String psy  = comboPsy.getValue();
        String type = comboType.getValue();
        LocalDate dateMin = datePickerFiltre.getValue();

        filteredList.setPredicate(d -> {
            // Filtre psy
            boolean matchPsy = psy == null || "Tous".equals(psy)
                    || ("Psy #" + d.getUserId()).equals(psy);

            // Filtre type
            boolean matchType = type == null || "Tous".equals(type)
                    || d.getTypeConsult().toString().equalsIgnoreCase(type);

            // Filtre date
            boolean matchDate = dateMin == null
                    || !d.getDateDispo().toLocalDate().isBefore(dateMin);

            return matchPsy && matchType && matchDate;
        });

        int nb = filteredList.size();
        lblNbCreneaux.setText(nb + " créneau" + (nb > 1 ? "x" : "") + " disponible" + (nb > 1 ? "s" : ""));
    }

    // ── Colonnes ────────────────────────────────────────────────────
    private void configurerColonnes() {

        // Psychologue
        colPsychologue.setCellValueFactory(cell ->
                new SimpleStringProperty("Psy #" + cell.getValue().getUserId()));
        colPsychologue.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #3730a3; -fx-padding: 10 14;");
            }
        });

        // Date (avec jour de la semaine)
        colDate.setCellValueFactory(cell -> {
            LocalDate ld = cell.getValue().getDateDispo().toLocalDate();
            String jour  = ld.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.SHORT, Locale.FRENCH);
            return new SimpleStringProperty(
                    jour + "\n" + ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: #374151; -fx-padding: 10 14; -fx-alignment: CENTER_LEFT;");
            }
        });

        // Horaires
        colHeure.setCellValueFactory(cell -> {
            String debut = cell.getValue().getHeureDebut().toString().substring(0, 5);
            String fin   = cell.getValue().getHeureFin().toString().substring(0, 5);
            return new SimpleStringProperty(debut + " – " + fin);
        });
        colHeure.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label lbl = new Label(s);
                lbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #6366f1;");
                setGraphic(lbl); setText(null);
            }
        });

        // Type (badge coloré)
        colType.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTypeConsult().toString()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                boolean presentiel = "présentiel".equalsIgnoreCase(s);
                Label badge = new Label(presentiel ? "🏢  Présentiel" : "💻  En ligne");
                badge.setStyle(
                        "-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge); setText(null);
            }
        });

        // Lieu
        colLieu.setCellValueFactory(cell -> {
            String lieu = cell.getValue().getLieu();
            return new SimpleStringProperty(
                    lieu != null && !lieu.isEmpty() ? lieu : "—");
        });
        colLieu.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: " + ("—".equals(s) ? "#c4b5fd" : "#374151") + "; " +
                        "-fx-padding: 10 14;");
            }
        });
    }

    // ── Sélection ───────────────────────────────────────────────────
    private void configurerSelection() {
        tableViewDisponibilites.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        disponibiliteSelectionnee = newSel;
                        afficherBandeauSelection(newSel);
                    }
                });
    }

    private void afficherBandeauSelection(DisponibilitePsy d) {
        String debut = d.getHeureDebut().toString().substring(0, 5);
        String fin   = d.getHeureFin().toString().substring(0, 5);
        String date  = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));

        lblSelection.setText("Psy #" + d.getUserId()
                + "  ·  " + date
                + "  ·  " + debut + " – " + fin
                + "  ·  " + d.getTypeConsult());
        boxSelection.setVisible(true);
        boxSelection.setManaged(true);
    }

    private void deselectionner() {
        disponibiliteSelectionnee = null;
        tableViewDisponibilites.getSelectionModel().clearSelection();
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);
        lblSelection.setText("");
    }

    // ── Chargement (appelé depuis setEtudiantId) ─────────────────────
    private void chargerDisponibilites() {
        try {
            List<DisponibilitePsy> disponibilites =
                    disponibiliteService.afficherDisponibilitesDisponibles();

            disponibilitesList.setAll(disponibilites);
            remplirComboPsy(disponibilites);
            appliquerFiltres();

        } catch (SQLException e) {
            lblNbCreneaux.setText("Erreur de chargement");
            e.printStackTrace();
        }
    }

    // ── Confirmation réservation (logique inchangée) ─────────────────
    private void confirmerReservation() {
        if (disponibiliteSelectionnee == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucune sélection",
                    "Veuillez sélectionner un créneau disponible.");
            return;
        }

        String motifSaisi = txtMotif.getText().trim();
        final String motifFinal = motifSaisi.isEmpty() ? "Consultation psychologique" : motifSaisi;
        final DisponibilitePsy dispoSelectionnee = disponibiliteSelectionnee;
        final int etudiantIdFinal = this.etudiantId;

        String debut = dispoSelectionnee.getHeureDebut().toString().substring(0, 5);
        String fin   = dispoSelectionnee.getHeureFin().toString().substring(0, 5);

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de réservation");
        confirmation.setHeaderText("Confirmer la réservation");
        confirmation.setContentText(
                "📅 Date : "         + dispoSelectionnee.getDateDispo()       + "\n" +
                        "⏰ Horaire : "       + debut + " – " + fin                    + "\n" +
                        "👨‍⚕️ Psychologue : Psy #" + dispoSelectionnee.getUserId()   + "\n" +
                        "💬 Type : "          + dispoSelectionnee.getTypeConsult()     + "\n" +
                        "📝 Motif : "         + motifFinal                             + "\n\n" +
                        "Confirmez-vous cette réservation ?"
        );

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                effectuerReservation(dispoSelectionnee, etudiantIdFinal, motifFinal);
            }
        });
    }

    private void effectuerReservation(DisponibilitePsy dispo, int etudiantId, String motif) {
        try {
            RendezVous rendezVous = new RendezVous(
                    dispo.getDispoId(),
                    etudiantId,
                    dispo.getUserId(),
                    motif
            );
            rendezVousService.ajouter(rendezVous);

            String debut = dispo.getHeureDebut().toString().substring(0, 5);
            String fin   = dispo.getHeureFin().toString().substring(0, 5);

            afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Rendez-vous réservé avec succès !\n\n" +
                            "📅 Date : "    + dispo.getDateDispo()  + "\n" +
                            "⏰ Horaire : " + debut + " – " + fin    + "\n" +
                            "📝 Motif : "   + motif);

            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de réserver le rendez-vous.\nErreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Utilitaires ─────────────────────────────────────────────────
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

    // ── Appelées depuis l'extérieur (inchangées) ─────────────────────
    public void setEtudiantId(int id) {
        this.etudiantId = id;
        chargerDisponibilites();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}