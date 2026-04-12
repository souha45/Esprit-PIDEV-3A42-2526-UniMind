package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.example.models.RendezVousDetail;
import org.example.models.User;
import org.example.services.RendezVousService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RendezVousPsyController implements SidebarPsyController.PsyPageController {

    // ========== COMPOSANTS FXML ==========
    @FXML private TableView<RendezVousDetail> tableViewRendezVous;
    @FXML private TableColumn<RendezVousDetail, String> colEtudiant;
    @FXML private TableColumn<RendezVousDetail, String> colDateHeure;
    @FXML private TableColumn<RendezVousDetail, String> colType;
    @FXML private TableColumn<RendezVousDetail, String> colStatut;
    @FXML private TableColumn<RendezVousDetail, Void> colActions;
    @FXML private Button btnRafraichir;
    @FXML private Label lblStatut;
    @FXML private Label lblDate;

    // ========== SIDEBAR ==========
    @FXML private SidebarPsyController sidebarPsyController;

    // ========== SERVICES ==========
    private RendezVousService rendezVousService;
    private ObservableList<RendezVousDetail> rendezVousList;
    private User utilisateur;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        rendezVousService = new RendezVousService();
        rendezVousList = FXCollections.observableArrayList();

        configurerColonnes();
        btnRafraichir.setOnAction(event -> chargerRendezVous());
    }

    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;

        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/RendezVousPsy.fxml");
        }

        chargerRendezVous();
    }

    private void configurerColonnes() {
        // Colonne Étudiant (nom + prénom + email)
        colEtudiant.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getEtudiantPrenom() + " " +
                                cellData.getValue().getEtudiantNom() + "\n" +
                                cellData.getValue().getEtudiantEmail()
                ));

        // Colonne Date et Heure
        colDateHeure.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDateDispo().toString() + "\n" +
                                cellData.getValue().getHeureDebut() + " - " +
                                cellData.getValue().getHeureFin()
                ));

        // Colonne Type
        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTypeConsult().toUpperCase()));

        // Colonne Statut (avec couleur)
        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatut().toUpperCase()));

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
                        case "demande" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "confirme" -> setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                        case "en-cours" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        case "termine" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "annule" -> setStyle("-fx-text-fill: #e74c3c;");
                        case "absent" -> setStyle("-fx-text-fill: #95a5a6;");
                        default -> setStyle("");
                    }
                }
            }
        });

        ajouterBoutonsAction();
    }

    private void ajouterBoutonsAction() {
        colActions.setCellFactory(column -> new TableCell<RendezVousDetail, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                String statut = rdv.getStatut().toLowerCase();

                HBox buttons = new HBox(5);

                switch (statut) {
                    case "demande":
                        Button btnConfirmer = createButton("✓ Confirmer", "#2ecc71");
                        Button btnRefuser = createButton("✗ Refuser", "#e74c3c");
                        btnConfirmer.setOnAction(e -> changerStatut(rdv, "confirme"));
                        btnRefuser.setOnAction(e -> changerStatut(rdv, "annulé"));
                        buttons.getChildren().addAll(btnConfirmer, btnRefuser);
                        break;

                    case "confirme":
                        Button btnEnCours = createButton("▶ En cours", "#3498db");
                        Button btnAnnuler = createButton("✗ Annuler", "#e74c3c");
                        btnEnCours.setOnAction(e -> changerStatut(rdv, "en-cours"));
                        btnAnnuler.setOnAction(e -> changerStatut(rdv, "annulé"));
                        buttons.getChildren().addAll(btnEnCours, btnAnnuler);
                        break;

                    case "en-cours":
                        Button btnTerminer = createButton("✓ Terminer", "#27ae60");
                        Button btnAbsent = createButton("⏤ Absent", "#95a5a6");
                        btnTerminer.setOnAction(e -> changerStatut(rdv, "terminé"));
                        btnAbsent.setOnAction(e -> changerStatut(rdv, "absent"));
                        buttons.getChildren().addAll(btnTerminer, btnAbsent);
                        break;

                    case "termine":
                    case "annulé":
                    case "absent":
                        Label info = new Label("✓ Traité");
                        info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                        buttons.getChildren().add(info);
                        break;

                    default:
                        buttons.getChildren().add(new Label(""));
                }

                setGraphic(buttons);
            }
        });
    }

    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-cursor: hand;");
        return btn;
    }

    private void changerStatut(RendezVousDetail rdv, String nouveauStatut) {
        try {
            rendezVousService.modifierStatutRendezVous(
                    rdv.getRendezVousId(),
                    rdv.getEtudiantId(),
                    utilisateur.getUserId(),
                    nouveauStatut
            );

            chargerRendezVous();

            String message = "Rendez-vous " + getMessageStatut(nouveauStatut);
            showAlert(Alert.AlertType.INFORMATION, "Succès", message);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de modifier le statut: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getMessageStatut(String statut) {
        switch (statut) {
            case "confirme": return "confirmé !";
            case "annulé": return "annulé !";
            case "en-cours": return "passé en cours !";
            case "terminé": return "terminé !";
            case "absent": return "marqué absent !";
            default: return "modifié !";
        }
    }

    private void chargerRendezVous() {
        if (utilisateur == null) {
            lblStatut.setText("Erreur: utilisateur non connecté");
            return;
        }

        try {
            lblStatut.setText("Chargement en cours...");
            List<RendezVousDetail> rendezVous =
                    rendezVousService.afficherRendezVousDetailsByPsy(utilisateur.getUserId());

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

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}