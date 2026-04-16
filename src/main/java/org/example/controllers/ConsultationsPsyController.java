package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private ListView<ConsultationDetail> listViewConsultations;
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

        configurerListView();
        configurerFiltres();
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
        listViewConsultations.setItems(filteredList);

        lblStatut.setText(filteredList.size() + " consultation(s) affichée(s) sur " + consultationsList.size());
    }

    private void configurerListView() {
        listViewConsultations.setCellFactory(param -> new ListCell<ConsultationDetail>() {
            @Override
            protected void updateItem(ConsultationDetail consultation, boolean empty) {
                super.updateItem(consultation, empty);

                if (empty || consultation == null) {
                    setGraphic(null);
                    return;
                }

                // Conteneur principal
                VBox mainContainer = new VBox(8);
                mainContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                        "-fx-border-color: #e0e7ff; -fx-border-width: 1; -fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.06), 6, 0, 0, 2); " +
                        "-fx-padding: 16 20;");

                // Header avec patient et date
                HBox headerBox = new HBox(15);
                headerBox.setAlignment(Pos.CENTER_LEFT);

                // Patient
                VBox patientBox = new VBox(2);
                Label patientName = new Label(consultation.getEtudiantPrenom() + " " + consultation.getEtudiantNom());
                patientName.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");

                Label patientEmail = new Label(consultation.getEtudiantEmail());
                patientEmail.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");

                patientBox.getChildren().addAll(patientName, patientEmail);

                // Date et heure
                VBox dateBox = new VBox(2);
                Label dateLabel = new Label(consultation.getDateDispo().toString());
                dateLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

                Label timeLabel = new Label(consultation.getHeureDebut() + " - " + consultation.getHeureFin());
                timeLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #9ca3af;");

                dateBox.getChildren().addAll(dateLabel, timeLabel);

                headerBox.getChildren().addAll(patientBox, dateBox);

                // Note avec badge
                Label noteBadge = createNoteBadge(consultation.getNoteFormatted());

                // Avis
                Label avisLabel = new Label();
                String avis = consultation.getAvisFormatted();
                if (avis != null && !avis.trim().isEmpty()) {
                    String avisNettoye = avis.replaceFirst("Consultation terminée le\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s*", "");
                    if (avisNettoye.length() > 100) {
                        avisLabel.setText(avisNettoye.substring(0, 100) + "...");
                    } else {
                        avisLabel.setText(avisNettoye);
                    }
                } else {
                    avisLabel.setText("Aucun avis");
                }
                avisLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #4b5563; -fx-wrap-text: true;");
                avisLabel.setMaxWidth(500);

                // Boutons d'action
                HBox actionsBox = new HBox(10);
                actionsBox.setAlignment(Pos.CENTER_RIGHT);

                Button btnDetails = new Button("👁");
                btnDetails.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; " +
                        "-fx-padding: 8 12; -fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 6, 0, 0, 2);");
                btnDetails.setOnAction(e -> afficherDetailsConsultation(consultation));

                Button btnModifier = new Button("✏");
                btnModifier.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; " +
                        "-fx-padding: 8 12; -fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.3), 6, 0, 0, 2);");
                btnModifier.setOnAction(e -> modifierConsultation(consultation));

                actionsBox.getChildren().addAll(btnDetails, btnModifier);

                // Assemblage final
                mainContainer.getChildren().addAll(headerBox, noteBadge, avisLabel, actionsBox);
                setGraphic(mainContainer);
            }

            private Label createNoteBadge(String noteFormatted) {
                Label badge = new Label();

                if (noteFormatted == null || noteFormatted.contains("À NOTER")) {
                    badge.setText("⭐⭐⭐⭐⭐");
                    badge.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #9ca3af; " +
                            "-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; " +
                            "-fx-padding: 4 12; -fx-background-radius: 20;");
                } else {
                    try {
                        String noteStr = noteFormatted.replace("/5", "").trim();
                        int note = Integer.parseInt(noteStr);
                        String etoiles = getEtoiles(note);
                        badge.setText(etoiles);

                        String bgColor, textColor;
                        if (note <= 2) {
                            bgColor = "#fee2e2";
                            textColor = "#dc2626";
                        } else if (note <= 4) {
                            bgColor = "#fef3c7";
                            textColor = "#d97706";
                        } else {
                            bgColor = "#dcfce7";
                            textColor = "#16a34a";
                        }

                        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                    } catch (NumberFormatException e) {
                        badge.setText("⭐⭐⭐⭐⭐");
                        badge.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                    }
                }
                return badge;
            }

            private String getEtoiles(int note) {
                StringBuilder etoiles = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    etoiles.append(i < note ? "⭐" : "☆");
                }
                return etoiles.toString();
            }
        });
    }

    private void afficherDetailsConsultation(ConsultationDetail consultation) {
        Stage detailsStage = new Stage();
        detailsStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        detailsStage.initOwner(listViewConsultations.getScene().getWindow());
        detailsStage.setTitle("Détails de la consultation");
        detailsStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.15), 20, 0, 0, 0); " +
                "-fx-padding: 30; -fx-border-color: #e0e7ff; -fx-border-width: 1; -fx-border-radius: 16;");

        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 32px;");

        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Détails de la consultation");
        titleLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");

        Label dateLabel = new Label("Consultation du " + consultation.getDateDispo());
        dateLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: #6b7280;");

        titleBox.getChildren().addAll(titleLabel, dateLabel);
        headerBox.getChildren().addAll(iconLabel, titleBox);

        VBox contentBox = new VBox(15);

        // Section Patient
        VBox patientSection = createSection("👤 Patient",
                consultation.getEtudiantPrenom() + " " + consultation.getEtudiantNom(),
                consultation.getEtudiantEmail());

        // Section Rendez-vous
        VBox rdvSection = createSection("📅 Rendez-vous",
                consultation.getHeureDebut() + " - " + consultation.getHeureFin(),
                consultation.getDateDispo().toString());

        // Section Note
        String noteText = consultation.getNoteFormatted();
        String noteDisplay = noteText.contains("À NOTER") ? "⭐⭐⭐⭐⭐" : getEtoilesDisplay(noteText);
        VBox noteSection = createSection("⭐ Satisfaction", noteDisplay, noteText);

        // Section Avis
        String avis = consultation.getAvisFormatted();
        String avisNettoye = avis != null && !avis.trim().isEmpty() ?
                avis.replaceFirst("Consultation terminée le\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s*", "") : "Aucun avis";
        VBox avisSection = createSection("📝 Avis du psychologue", avisNettoye, null);

        // Section Date rédaction
        String dateRedaction = consultation.getDateRedaction() != null ?
                consultation.getDateRedaction().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Non spécifiée";
        VBox redactionSection = createSection("🕐 Date rédaction", dateRedaction, null);

        contentBox.getChildren().addAll(patientSection, rdvSection, noteSection, avisSection, redactionSection);

        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 6, 0, 0, 2);");
        closeButton.setOnAction(e -> detailsStage.close());

        mainContainer.getChildren().addAll(headerBox, contentBox, closeButton);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        detailsStage.setScene(scene);
        detailsStage.show();
    }

    private VBox createSection(String title, String content, String subtitle) {
        VBox section = new VBox(8);
        section.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-padding: 15;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

        Label contentLabel = new Label(content);
        contentLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: #1f2937; -fx-wrap-text: true;");

        section.getChildren().addAll(titleLabel, contentLabel);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
            section.getChildren().add(subtitleLabel);
        }

        return section;
    }

    private String getEtoilesDisplay(String noteFormatted) {
        try {
            String noteStr = noteFormatted.replace("/5", "").trim();
            int note = Integer.parseInt(noteStr);
            StringBuilder etoiles = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                etoiles.append(i < note ? "⭐" : "☆");
            }
            return etoiles.toString() + " (" + noteFormatted + ")";
        } catch (NumberFormatException e) {
            return "⭐⭐⭐⭐⭐";
        }
    }

    private void modifierConsultation(ConsultationDetail consultation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierConsultationModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(listViewConsultations.getScene().getWindow());
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