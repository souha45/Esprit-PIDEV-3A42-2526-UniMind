package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.models.DisponibilitePsy;
import org.example.models.User;
import org.example.services.DisponibilitePsyService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AffichageDisponibilitePsyController
        implements SidebarPsyController.PsyPageController {

    // ========== COMPOSANTS FXML ==========
    @FXML private TableView<DisponibilitePsy> tableViewDisponibilites;
    @FXML private TableColumn<DisponibilitePsy, Integer> colId;
    @FXML private TableColumn<DisponibilitePsy, java.sql.Date> colDate;
    @FXML private TableColumn<DisponibilitePsy, java.sql.Time> colHeureDebut;
    @FXML private TableColumn<DisponibilitePsy, java.sql.Time> colHeureFin;
    @FXML private TableColumn<DisponibilitePsy, String> colTypeConsult;
    @FXML private TableColumn<DisponibilitePsy, String> colLieu;
    @FXML private TableColumn<DisponibilitePsy, String> colStatut;
    @FXML private TableColumn<DisponibilitePsy, Void> colAction;
    @FXML private Button btnAjouter;
    @FXML private Button btnRafraichir;
    @FXML private Label lblStatut;
    @FXML private Label lblDate;

    // ========== SIDEBAR ==========
    @FXML private SidebarPsyController sidebarPsyController;

    // ========== SERVICES ET DONNÉES ==========
    private DisponibilitePsyService disponibiliteService;
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private User utilisateur; // ← remplace userIdConnecte

    // ========== INITIALISATION ==========
    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        disponibiliteService = new DisponibilitePsyService();
        disponibilitesList = FXCollections.observableArrayList();
        configurerColonnes();
        btnAjouter.setOnAction(event -> ouvrirFormulaireAjout());
        btnRafraichir.setOnAction(event -> chargerDisponibilites());
    }

    // ========== INTERFACE PsyPageController ==========
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;

        // Configurer la sidebar
        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/AffichageDisponibilitePsy.fxml");
        }

        // Charger les données
        chargerDisponibilites();
    }

    // ========== COLONNES ==========
    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("dispoId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDispo"));
        colHeureDebut.setCellValueFactory(new PropertyValueFactory<>("heureDebut"));
        colHeureFin.setCellValueFactory(new PropertyValueFactory<>("heureFin"));
        colTypeConsult.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTypeConsult().toString()));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatut().toString()));

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "disponible" -> setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        case "réservé"    -> setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        case "annulé"     -> setStyle("-fx-text-fill: red;");
                        default           -> setStyle("");
                    }
                }
            }
        });

        ajouterBoutonsAction();
    }

    private void ajouterBoutonsAction() {
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button btnModifier  = new Button("✏️ Modifier");
            private final Button btnSupprimer = new Button("🗑️ Supprimer");
            private final HBox   buttons      = new HBox(5, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
                btnSupprimer.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

                btnModifier.setOnAction(event ->
                        modifierDisponibilite(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(event ->
                        supprimerDisponibilite(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    // ========== CHARGEMENT ==========
    private void chargerDisponibilites() {
        if (utilisateur == null) {
            lblStatut.setText("Erreur : utilisateur non connecté");
            return;
        }
        try {
            lblStatut.setText("Chargement en cours...");
            List<DisponibilitePsy> disponibilites =
                    disponibiliteService.afficherDisponibilitesPsy(utilisateur.getUserId());

            disponibilitesList.clear();
            disponibilitesList.addAll(disponibilites);
            tableViewDisponibilites.setItems(disponibilitesList);

            lblStatut.setText(disponibilitesList.isEmpty()
                    ? "Aucune disponibilité trouvée."
                    : disponibilitesList.size() + " disponibilité(s) trouvée(s)");

        } catch (SQLException e) {
            lblStatut.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== ACTIONS MODALS ==========
    private void ouvrirFormulaireAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutDisponibiliteModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnAjouter.getScene().getWindow());
            modalStage.setTitle("Ajouter une disponibilité");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            AjoutDisponibiliteController controller = loader.getController();
            controller.setUserId(utilisateur.getUserId()); // ← utilise l'objet User
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerDisponibilites();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire d'ajout");
            e.printStackTrace();
        }
    }

    private void modifierDisponibilite(DisponibilitePsy disponibilite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierDisponibiliteModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnAjouter.getScene().getWindow());
            modalStage.setTitle("Modifier une disponibilité");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            ModifierDisponibiliteController controller = loader.getController();
            controller.setDisponibiliteAModifier(disponibilite);
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerDisponibilites();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification");
            e.printStackTrace();
        }
    }

    private void supprimerDisponibilite(DisponibilitePsy disponibilite) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer la disponibilité");
        confirmation.setContentText("Voulez-vous vraiment supprimer la disponibilité du "
                + disponibilite.getDateDispo() + " ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    disponibiliteService.supprimer(disponibilite.getDispoId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Disponibilité supprimée !");
                    chargerDisponibilites();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer");
                    e.printStackTrace();
                }
            }
        });
    }

    // ========== UTILITAIRES ==========
    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}