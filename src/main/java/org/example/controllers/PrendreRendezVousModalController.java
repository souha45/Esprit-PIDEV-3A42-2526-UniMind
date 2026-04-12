package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.models.DisponibilitePsy;
import org.example.services.DisponibilitePsyService;
import org.example.services.RendezVousService;
import org.example.models.RendezVous;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class PrendreRendezVousModalController {

    // ========== COMPOSANTS FXML ==========
    @FXML private TableView<DisponibilitePsy> tableViewDisponibilites;
    @FXML private TableColumn<DisponibilitePsy, String> colPsychologue;
    @FXML private TableColumn<DisponibilitePsy, String> colDate;
    @FXML private TableColumn<DisponibilitePsy, String> colHeure;
    @FXML private TableColumn<DisponibilitePsy, String> colType;
    @FXML private TableColumn<DisponibilitePsy, String> colLieu;
    @FXML private TextArea txtMotif;
    @FXML private Label lblSelection;
    @FXML private Button btnFermer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnConfirmer;

    // ========== SERVICES ==========
    private DisponibilitePsyService disponibiliteService;
    private RendezVousService rendezVousService;

    // ========== DONNÉES ==========
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private DisponibilitePsy disponibiliteSelectionnee;
    private int etudiantId;
    private Stage modalStage;

    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();
        rendezVousService = new RendezVousService();
        disponibilitesList = FXCollections.observableArrayList();

        configurerColonnes();
        configurerSelection();

        btnConfirmer.setOnAction(event -> confirmerReservation());
        btnAnnuler.setOnAction(event -> fermerModal());
        btnFermer.setOnAction(event -> fermerModal());
    }

    private void configurerColonnes() {
        colPsychologue.setCellValueFactory(cellData ->
                new SimpleStringProperty("Psychologue ID: " + cellData.getValue().getUserId()));

        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateDispo().toString()));

        colHeure.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getHeureDebut().toString() + " - " +
                        cellData.getValue().getHeureFin().toString()));

        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTypeConsult().toString()));

        colLieu.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLieu()));
    }

    private void configurerSelection() {
        tableViewDisponibilites.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        disponibiliteSelectionnee = newSelection;
                        lblSelection.setText("✅ Créneau sélectionné : " +
                                newSelection.getDateDispo() + " de " +
                                newSelection.getHeureDebut() + " à " +
                                newSelection.getHeureFin());
                    } else {
                        disponibiliteSelectionnee = null;
                        lblSelection.setText("");
                    }
                }
        );
    }

    private void chargerDisponibilites() {
        try {
            lblSelection.setText("Chargement des disponibilités...");
            List<DisponibilitePsy> disponibilites = disponibiliteService.afficherDisponibilitesDisponibles();

            disponibilitesList.clear();
            disponibilitesList.addAll(disponibilites);
            tableViewDisponibilites.setItems(disponibilitesList);

            if (disponibilitesList.isEmpty()) {
                lblSelection.setText("Aucune disponibilité trouvée pour le moment.");
            } else {
                lblSelection.setText(disponibilitesList.size() + " créneaux disponibles");
            }
        } catch (SQLException e) {
            lblSelection.setText("Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Confirme la réservation du rendez-vous
     */
    private void confirmerReservation() {
        // 1. Vérifier qu'une disponibilité est sélectionnée
        if (disponibiliteSelectionnee == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucune sélection",
                    "Veuillez sélectionner un créneau disponible.");
            return;
        }

        // 2. Récupérer le motif et le stocker dans une variable FINALE
        String motifSaisi = txtMotif.getText().trim();
        final String motifFinal;
        if (motifSaisi.isEmpty()) {
            motifFinal = "Consultation psychologique";
        } else {
            motifFinal = motifSaisi;
        }

        // 3. Stocker les données dans des variables finales pour l'expression lambda
        final DisponibilitePsy dispoSelectionnee = disponibiliteSelectionnee;
        final int etudiantIdFinal = this.etudiantId;

        // 4. Confirmation avant réservation
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de réservation");
        confirmation.setHeaderText("Confirmer la réservation");
        confirmation.setContentText(
                "📅 Date: " + dispoSelectionnee.getDateDispo() + "\n" +
                        "⏰ Horaire: " + dispoSelectionnee.getHeureDebut() + " - " +
                        dispoSelectionnee.getHeureFin() + "\n" +
                        "👨‍⚕️ Psychologue ID: " + dispoSelectionnee.getUserId() + "\n" +
                        "📝 Motif: " + motifFinal + "\n\n" +
                        "Confirmez-vous cette réservation ?"
        );

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // ✅ Utilisation des variables finales dans la lambda
                effectuerReservation(dispoSelectionnee, etudiantIdFinal, motifFinal);
            }
        });
    }

    /**
     * Effectue la réservation dans la base de données
     */
    private void effectuerReservation(DisponibilitePsy dispo, int etudiantId, String motif) {
        try {
            // Créer l'objet RendezVous
            RendezVous rendezVous = new RendezVous(
                    dispo.getDispoId(),   // dispoId
                    etudiantId,            // etudiantId
                    dispo.getUserId(),     // psyId
                    motif                  // motif
            );

            // Appeler le service pour ajouter le rendez-vous
            rendezVousService.ajouter(rendezVous);

            // Succès
            afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Rendez-vous réservé avec succès !\n\n" +
                            "📅 Date: " + dispo.getDateDispo() + "\n" +
                            "⏰ Horaire: " + dispo.getHeureDebut() + " - " +
                            dispo.getHeureFin() + "\n" +
                            "📝 Motif: " + motif);

            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "❌ Impossible de réserver le rendez-vous.\n" +
                            "Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void fermerModal() {
        if (modalStage != null) {
            modalStage.close();
        }
    }

    // ========== MÉTHODES APPELÉES DEPUIS L'EXTÉRIEUR ==========

    public void setEtudiantId(int id) {
        this.etudiantId = id;
        chargerDisponibilites();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}