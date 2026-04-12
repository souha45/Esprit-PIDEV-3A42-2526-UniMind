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

/**
 * Contrôleur pour l'affichage des consultations de l'étudiant
 */
public class ConsultationsEtudiantController implements SidebarEtudiantController.EtudiantPageController {

    // ========== SIDEBAR (INCLUSE) ==========
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    // ========== COMPOSANTS PRINCIPAUX ==========
    @FXML private TableView<ConsultationDetail> tableViewConsultations;
    @FXML private TableColumn<ConsultationDetail, String> colDate;
    @FXML private TableColumn<ConsultationDetail, String> colPsychologue;
    @FXML private TableColumn<ConsultationDetail, String> colMotif;
    @FXML private TableColumn<ConsultationDetail, String> colNote;
    @FXML private TableColumn<ConsultationDetail, Void> colActions;
    @FXML private Button btnRafraichir;
    @FXML private Label lblStatut;
    @FXML private Label lblDate;

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

        // Configurer les callbacks de la sidebar

    }

    private void configurerColonnes() {
        // Colonne Date (avec heure)
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDateDispo().toString() + "\n" +
                                cellData.getValue().getHeureDebut() + " - " +
                                cellData.getValue().getHeureFin()
                ));

        // Colonne Psychologue
        colPsychologue.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        "Dr. " + cellData.getValue().getPsyPrenom() + " " +
                                cellData.getValue().getPsyNom() + "\n" +
                                "Psychologue"
                ));

        // Colonne Motif
        colMotif.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAvisFormatted()));

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
                    // Convertir "X/5" en étoiles
                    if (item.contains("/5") && !item.equals("Non renseignée")) {
                        String noteStr = item.replace("/5", "");
                        try {
                            int note = Integer.parseInt(noteStr);
                            String etoiles = getEtoiles(note);
                            setText(etoiles + " " + item);
                            if (note <= 2) {
                                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            } else if (note <= 4) {
                                setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                            }
                        } catch (NumberFormatException e) {
                            setText(item);
                            setStyle("");
                        }
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                    }
                }
            }

            private String getEtoiles(int note) {
                StringBuilder etoiles = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i < note) {
                        etoiles.append("★");
                    } else {
                        etoiles.append("☆");
                    }
                }
                return etoiles.toString();
            }
        });

        // Boutons d'action
        ajouterBoutonsAction();
    }

    private void ajouterBoutonsAction() {
        colActions.setCellFactory(column -> new TableCell<ConsultationDetail, Void>() {
            private final Button btnVoir = new Button("✅ Voir");
            private final HBox buttons = new HBox(5, btnVoir);

            {
                btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;");

                btnVoir.setOnAction(event -> {
                    ConsultationDetail consultation = getTableView().getItems().get(getIndex());
                    afficherDetailsConsultation(consultation);
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
                "👨‍⚕️ Psychologue: Dr. " + consultation.getPsyPrenom() + " " + consultation.getPsyNom() + "\n" +
                        "📅 Date: " + consultation.getDateDispo() + "\n" +
                        "⏰ Horaire: " + consultation.getHeureDebut() + " - " + consultation.getHeureFin() + "\n" +
                        "⭐ Note: " + consultation.getNoteFormatted() + "\n" +
                        "📝 Avis du psychologue: " + consultation.getAvisFormatted() + "\n" +
                        "📅 Date de rédaction: " + consultation.getDateRedaction()
        );
        alert.showAndWait();
    }

    private void chargerConsultations() {
        if (utilisateur == null) {
            lblStatut.setText("Erreur: utilisateur non connecté");
            return;
        }

        try {
            lblStatut.setText("Chargement en cours...");
            List<ConsultationDetail> consultations =
                    consultationService.getConsultationsDetailByEtudiant(utilisateur.getUserId());

            consultationsList.clear();
            consultationsList.addAll(consultations);
            tableViewConsultations.setItems(consultationsList);

            if (consultationsList.isEmpty()) {
                lblStatut.setText("Aucune consultation trouvée.");
            } else {
                lblStatut.setText(consultationsList.size() + " consultation(s) trouvée(s)");
            }

        } catch (SQLException e) {
            lblStatut.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== MÉTHODES DE NAVIGATION ==========

    private void rafraichir() {
        chargerConsultations();
    }



    // ========== MÉTHODES UTILITAIRES ==========

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

        chargerConsultations();
    }
}