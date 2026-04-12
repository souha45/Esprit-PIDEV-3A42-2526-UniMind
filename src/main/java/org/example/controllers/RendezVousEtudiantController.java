package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.models.RendezVousDetail;
import org.example.models.User;
import org.example.services.RendezVousService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class RendezVousEtudiantController implements SidebarEtudiantController.EtudiantPageController {

    // ========== SIDEBAR (INCLUSE) ==========
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    // ========== COMPOSANTS PRINCIPAUX ==========
    @FXML private TableView<RendezVousDetail> tableViewRendezVous;
    @FXML private TableColumn<RendezVousDetail, String> colDate;
    @FXML private TableColumn<RendezVousDetail, String> colHeure;
    @FXML private TableColumn<RendezVousDetail, String> colPsychologue;
    @FXML private TableColumn<RendezVousDetail, String> colType;
    @FXML private TableColumn<RendezVousDetail, String> colStatut;
    @FXML private TableColumn<RendezVousDetail, Void> colActions;
    @FXML private Button btnPrendreRdv;
    @FXML private Button btnRafraichir;
    @FXML private Label lblStatut;

    // ========== DONNÉES ==========
    private User utilisateur;
    private RendezVousService rendezVousService;
    private ObservableList<RendezVousDetail> rendezVousList;

    @FXML
    public void initialize() {
        rendezVousService = new RendezVousService();
        rendezVousList = FXCollections.observableArrayList();

        configurerColonnes();
        configurerActions();
    }

    private void configurerColonnes() {
        // Colonne Date
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateDispo().toString()));

        // Colonne Heure
        colHeure.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getHeureDebut().toString() + " - " +
                        cellData.getValue().getHeureFin().toString()));

        // Colonne Psychologue
        colPsychologue.setCellValueFactory(cellData ->
                new SimpleStringProperty("Dr. " + cellData.getValue().getPsyPrenom() + " " +
                        cellData.getValue().getPsyNom()));

        // Colonne Type
        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTypeConsult().toUpperCase()));

        // Colonne Statut
        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatutRDV().toUpperCase()));

        // Couleurs pour le statut
        colStatut.setCellFactory(column -> new TableCell<RendezVousDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toLowerCase()) {
                        case "demande":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        case "confirme":
                            setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                            break;
                        case "termine":
                            setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            break;
                        case "annule":
                            setStyle("-fx-text-fill: #e74c3c;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // Boutons d'action
        ajouterBoutonsAction();
    }

    private void ajouterBoutonsAction() {
        colActions.setCellFactory(column -> new TableCell<RendezVousDetail, Void>() {
            private final Button btnDetails = new Button("🔍 Détails");
            private final Button btnAnnuler = new Button("❌ Annuler");
            private final HBox buttons = new HBox(5, btnDetails, btnAnnuler);

            {
                btnDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                btnAnnuler.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");

                btnDetails.setOnAction(event -> {
                    RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                    afficherDetailsRendezVous(rdv);
                });

                btnAnnuler.setOnAction(event -> {
                    RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                    annulerRendezVous(rdv);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }

    private void afficherDetailsRendezVous(RendezVousDetail rdv) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du rendez-vous");
        alert.setHeaderText("Rendez-vous du " + rdv.getDateDispo());
        alert.setContentText(
                "📅 Date: " + rdv.getDateDispo() + "\n" +
                        "⏰ Horaire: " + rdv.getHeureDebut() + " - " + rdv.getHeureFin() + "\n" +
                        "👨‍⚕️ Psychologue: Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom() + "\n" +
                        "📝 Type: " + rdv.getTypeConsult() + "\n" +
                        "📌 Statut: " + rdv.getStatutRDV()
        );
        alert.showAndWait();
    }

    private void annulerRendezVous(RendezVousDetail rdv) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation d'annulation");
        confirmation.setHeaderText("Annuler le rendez-vous");
        confirmation.setContentText("Voulez-vous vraiment annuler le rendez-vous du " + rdv.getDateDispo() + " ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    rendezVousService.modifierStatutRendezVous(
                            rdv.getRendezVousId(),
                            utilisateur.getUserId(),
                            0,
                            "annulé"
                    );
                    chargerRendezVous();
                    afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous annulé avec succès !");
                } catch (SQLException e) {
                    afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible d'annuler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void chargerRendezVous() {
        if (utilisateur == null) {
            lblStatut.setText("Erreur: utilisateur non connecté");
            return;
        }

        try {
            lblStatut.setText("Chargement en cours...");
            List<RendezVousDetail> rendezVous = rendezVousService.afficherRendezVousDetailsByEtudiant(utilisateur.getUserId());

            rendezVousList.clear();
            rendezVousList.addAll(rendezVous);
            tableViewRendezVous.setItems(rendezVousList);

            if (rendezVousList.isEmpty()) {
                lblStatut.setText("Aucun rendez-vous trouvé.");
            } else {
                lblStatut.setText(rendezVousList.size() + " rendez-vous trouvé(s)");
            }
        } catch (SQLException e) {
            lblStatut.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurerActions() {
        btnPrendreRdv.setOnAction(event -> prendreRendezVous());
        btnRafraichir.setOnAction(event -> chargerRendezVous());
    }
    private void rafraichir() {
        chargerRendezVous();
    }

    private void prendreRendezVous() {
        try {
            if (utilisateur == null) {
                afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrendreRendezVousModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnPrendreRdv.getScene().getWindow());
            modalStage.setTitle("Prendre un rendez-vous");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            PrendreRendezVousModalController controller = loader.getController();
            controller.setEtudiantId(utilisateur.getUserId());
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerRendezVous();

        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de réservation");
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== MÉTHODE APPELÉE DEPUIS L'EXTÉRIEUR ==========

    public void setUtilisateur(User user) {
        this.utilisateur = user;

        // Mettre à jour la sidebar
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
        }

        chargerRendezVous();
    }
}