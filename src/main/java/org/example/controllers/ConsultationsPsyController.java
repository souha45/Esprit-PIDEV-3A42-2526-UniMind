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
import org.example.models.ConsultationDetail;
import org.example.models.User;
import org.example.services.ConsultationService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConsultationsPsyController implements SidebarPsyController.PsyPageController {

    // ========== COMPOSANTS FXML ==========
    @FXML private TableView<ConsultationDetail> tableViewConsultations;
    @FXML private TableColumn<ConsultationDetail, String> colPatient;
    @FXML private TableColumn<ConsultationDetail, String> colDateRdv;
    @FXML private TableColumn<ConsultationDetail, String> colHeureRdv;
    @FXML private TableColumn<ConsultationDetail, String> colNote;
    @FXML private TableColumn<ConsultationDetail, String> colAvis;
    @FXML private TableColumn<ConsultationDetail, Void> colActions;
    @FXML private Button btnRafraichir;
    @FXML private Label lblDate;
    @FXML private Label lblTotal;

    // ========== SIDEBAR ==========
    @FXML private SidebarPsyController sidebarPsyController;

    // ========== SERVICES ==========
    private ConsultationService consultationService;
    private ObservableList<ConsultationDetail> consultationsList;
    private User utilisateur;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        consultationService = new ConsultationService();
        consultationsList = FXCollections.observableArrayList();

        configurerColonnes();
        btnRafraichir.setOnAction(event -> chargerConsultations());
    }

    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;

        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/ConsultationsPsy.fxml");
        }

        chargerConsultations();
    }

    private void configurerColonnes() {
        // ✅ Colonne Patient (utilise les getters pour étudiant)
        colPatient.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getEtudiantPrenom() + " " +
                                cellData.getValue().getEtudiantNom() + "\n" +
                                cellData.getValue().getEtudiantEmail()
                ));

        // Colonne Date RDV
        colDateRdv.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateDispo().toString()));

        // Colonne Heure RDV
        colHeureRdv.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getHeureDebut() + " - " +
                                cellData.getValue().getHeureFin()
                ));

        // Colonne Note
        colNote.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNoteFormatted()));

        colNote.setCellFactory(column -> new TableCell<ConsultationDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("/5") && !item.equals("À NOTER")) {
                        String noteStr = item.replace("/5", "");
                        try {
                            int note = Integer.parseInt(noteStr);
                            if (note <= 2) {
                                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            } else if (note <= 4) {
                                setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                            }
                        } catch (NumberFormatException e) {
                            setStyle("");
                        }
                    } else {
                        setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                    }
                }
            }
        });

        // Colonne Avis
        colAvis.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAvisFormatted()));

        // Boutons d'action
        ajouterBoutonsAction();
    }

    private void ajouterBoutonsAction() {
        colActions.setCellFactory(column -> new TableCell<ConsultationDetail, Void>() {
            private final Button btnModifier = new Button("✏️");
            private final Button btnDetails = new Button("🔍");
            private final HBox buttons = new HBox(5, btnDetails, btnModifier);

            {
                btnDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 8; -fx-cursor: hand;");
                btnModifier.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 8; -fx-cursor: hand;");

                btnDetails.setOnAction(event -> {
                    ConsultationDetail consultation = getTableView().getItems().get(getIndex());
                    afficherDetailsConsultation(consultation);
                });

                btnModifier.setOnAction(event -> {
                    ConsultationDetail consultation = getTableView().getItems().get(getIndex());
                    modifierConsultation(consultation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void afficherDetailsConsultation(ConsultationDetail consultation) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la consultation");
        alert.setHeaderText("Consultation du " + consultation.getDateDispo());
        alert.setContentText(
                "👤 Patient: " + consultation.getEtudiantPrenom() + " " + consultation.getEtudiantNom() + "\n" +
                        "📧 Email: " + consultation.getEtudiantEmail() + "\n" +
                        "📅 Date RDV: " + consultation.getDateDispo() + "\n" +
                        "⏰ Horaire: " + consultation.getHeureDebut() + " - " + consultation.getHeureFin() + "\n" +
                        "⭐ Note: " + consultation.getNoteFormatted() + "\n" +
                        "📝 Avis: " + consultation.getAvisFormatted() + "\n" +
                        "📅 Date consultation: " + consultation.getDateRedaction()
        );
        alert.showAndWait();
    }

    /**
     * Ouvre le modal pour modifier l'avis et la note d'une consultation
     */
    /**
     * Ouvre le modal pour modifier l'avis et la note d'une consultation
     */
    private void modifierConsultation(ConsultationDetail consultation) {
        try {
            // Charger le FXML du modal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierConsultationModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            // Configurer le modal
            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(tableViewConsultations.getScene().getWindow());
            modalStage.setTitle("Modifier l'avis et la note");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            // Récupérer le contrôleur
            ModifierConsultationModalController controller = loader.getController();
            controller.setConsultation(consultation);
            controller.setUtilisateur(utilisateur);  // ✅ Passer l'utilisateur
            controller.setModalStage(modalStage);

            // Afficher et attendre
            modalStage.showAndWait();

            // Rafraîchir le tableau après modification
            chargerConsultations();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de modification");
        }
    }

    private void chargerConsultations() {
        if (utilisateur == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté");
            return;
        }

        try {
            List<ConsultationDetail> consultations =
                    consultationService.getConsultationsDetailByPsy(utilisateur.getUserId());

            consultationsList.clear();
            consultationsList.addAll(consultations);
            tableViewConsultations.setItems(consultationsList);

            lblTotal.setText(consultationsList.size() + " consultation(s)");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}