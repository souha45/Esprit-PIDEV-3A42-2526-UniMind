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
import java.util.stream.Collectors;

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
    @FXML private Label lblStatut;
    @FXML private TextField fieldRecherche;

    // Statistiques
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatNotees;
    @FXML private Label lblStatMoyenne;
    @FXML private Label lblStatCeMois;

    // ========== SIDEBAR ==========
    @FXML private SidebarPsyController sidebarPsyController;

    // ========== SERVICES ==========
    private ConsultationService consultationService;
    private ObservableList<ConsultationDetail> consultationsList;
    private ObservableList<ConsultationDetail> filteredList;
    private User utilisateur;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        consultationService = new ConsultationService();
        consultationsList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        configurerColonnes();
        configurerFiltres();
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

    private void configurerFiltres() {
        fieldRecherche.textProperty().addListener((obs, old, newVal) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        if (consultationsList == null) return;

        String recherche = fieldRecherche.getText().toLowerCase();

        List<ConsultationDetail> filtered = consultationsList.stream()
                .filter(c -> {
                    if (recherche != null && !recherche.isEmpty()) {
                        String patientInfo = (c.getEtudiantPrenom() + " " + c.getEtudiantNom() + " " + c.getEtudiantEmail()).toLowerCase();
                        return patientInfo.contains(recherche);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredList.clear();
        filteredList.addAll(filtered);
        tableViewConsultations.setItems(filteredList);

        lblStatut.setText(filteredList.size() + " consultation(s) affichée(s) sur " + consultationsList.size());
    }

    private void configurerColonnes() {
        // Colonne Patient
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

        // Colonne Note (avec étoiles)
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
                            String etoiles = getEtoiles(note);
                            setText(etoiles + " " + item);
                            if (note <= 2) {
                                setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                            } else if (note <= 4) {
                                setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                            }
                        } catch (NumberFormatException e) {
                            setText(item);
                            setStyle("");
                        }
                    } else {
                        setText("⭐ À noter");
                        setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                    }
                }
            }

            private String getEtoiles(int note) {
                StringBuilder etoiles = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    etoiles.append(i < note ? "★" : "☆");
                }
                return etoiles.toString();
            }
        });

        // Colonne Avis
        colAvis.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAvisFormatted()));

        // Colonne Avis avec style pour texte long
        colAvis.setCellFactory(column -> new TableCell<ConsultationDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (item.length() > 60) {
                        setText(item.substring(0, 60) + "...");
                    } else {
                        setText(item);
                    }
                    setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
                }
            }
        });

        ajouterBoutonsAction();
    }

    private void ajouterBoutonsAction() {
        colActions.setCellFactory(column -> new TableCell<ConsultationDetail, Void>() {
            private final Button btnModifier = new Button("✏️");
            private final Button btnDetails = new Button("🔍");
            private final HBox buttons = new HBox(8, btnDetails, btnModifier);

            {
                btnDetails.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
                btnModifier.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");

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
                        "📅 Date rédaction: " + consultation.getDateRedaction()
        );
        alert.showAndWait();
    }

    private void modifierConsultation(ConsultationDetail consultation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierConsultationModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(tableViewConsultations.getScene().getWindow());
            modalStage.setTitle("Modifier l'avis et la note");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            ModifierConsultationModalController controller = loader.getController();
            controller.setConsultation(consultation);
            controller.setUtilisateur(utilisateur);
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerConsultations();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification");
        }
    }

    private void chargerConsultations() {
        if (utilisateur == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté");
            return;
        }

        try {
            lblStatut.setText("Chargement en cours...");
            List<ConsultationDetail> consultations =
                    consultationService.getConsultationsDetailByPsy(utilisateur.getUserId());

            consultationsList.clear();
            consultationsList.addAll(consultations);

            // Mettre à jour les statistiques
            majStatistiques(consultations);

            appliquerFiltres();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void majStatistiques(List<ConsultationDetail> consultations) {
        int total = consultations.size();
        long notees = consultations.stream().filter(c -> c.getNoteSatisfaction() > 0).count();
        double moyenne = consultations.stream()
                .filter(c -> c.getNoteSatisfaction() > 0)
                .mapToInt(ConsultationDetail::getNoteSatisfaction)
                .average()
                .orElse(0);

        long ceMois = consultations.stream()
                .filter(c -> {
                    LocalDate dateRdv = c.getDateDispo().toLocalDate();
                    LocalDate now = LocalDate.now();
                    return dateRdv.getYear() == now.getYear() && dateRdv.getMonth() == now.getMonth();
                })
                .count();

        lblStatTotal.setText(String.valueOf(total));
        lblStatNotees.setText(String.valueOf(notees));
        lblStatMoyenne.setText(String.format("%.1f", moyenne));
        lblStatCeMois.setText(String.valueOf(ceMois));
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}