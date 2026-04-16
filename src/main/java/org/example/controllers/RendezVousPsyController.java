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
import java.util.stream.Collectors;

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
    @FXML private ComboBox<String> comboFiltreStatut;
    @FXML private ComboBox<String> comboFiltreType;
    @FXML private TextField fieldRecherche;

    // Statistiques
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatDemande;
    @FXML private Label lblStatConfirme;
    @FXML private Label lblStatEnCours;

    // ========== SIDEBAR ==========
    @FXML private SidebarPsyController sidebarPsyController;

    // ========== SERVICES ==========
    private RendezVousService rendezVousService;
    private ObservableList<RendezVousDetail> rendezVousList;
    private ObservableList<RendezVousDetail> filteredList;
    private User utilisateur;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        rendezVousService = new RendezVousService();
        rendezVousList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        configurerColonnes();
        configurerFiltres();
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

    private void configurerFiltres() {
        // Remplir les filtres
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "demande", "confirme", "en-cours", "terminé", "annulé", "absent"
        ));
        comboFiltreStatut.setValue("Tous");

        comboFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "présentiel", "en_ligne"
        ));
        comboFiltreType.setValue("Tous");

        // Listeners pour les filtres
        comboFiltreStatut.valueProperty().addListener((obs, old, newVal) -> appliquerFiltres());
        comboFiltreType.valueProperty().addListener((obs, old, newVal) -> appliquerFiltres());
        fieldRecherche.textProperty().addListener((obs, old, newVal) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        if (rendezVousList == null) return;

        String statutFiltre = comboFiltreStatut.getValue();
        String typeFiltre = comboFiltreType.getValue();
        String recherche = fieldRecherche.getText().toLowerCase();

        List<RendezVousDetail> filtered = rendezVousList.stream()
                .filter(rdv -> {
                    // Filtre par statut
                    if (statutFiltre != null && !statutFiltre.equals("Tous")) {
                        if (!rdv.getStatut().equalsIgnoreCase(statutFiltre)) return false;
                    }
                    // Filtre par type
                    if (typeFiltre != null && !typeFiltre.equals("Tous")) {
                        if (!rdv.getTypeConsult().equalsIgnoreCase(typeFiltre)) return false;
                    }
                    // Recherche par nom/prénom/email
                    if (recherche != null && !recherche.isEmpty()) {
                        String etudiantInfo = (rdv.getEtudiantPrenom() + " " + rdv.getEtudiantNom() + " " + rdv.getEtudiantEmail()).toLowerCase();
                        if (!etudiantInfo.contains(recherche)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredList.clear();
        filteredList.addAll(filtered);
        tableViewRendezVous.setItems(filteredList);

        int total = filteredList.size();
        lblStatut.setText(total + " rendez-vous affiché(s) sur " + rendezVousList.size());
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

        // Colonne Type (avec couleur)
        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTypeConsult().toUpperCase()));

        colType.setCellFactory(column -> new TableCell<RendezVousDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("présentiel")) {
                        setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;");
                    }
                }
            }
        });

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
                        case "demande" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        case "confirme" -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        case "en-cours" -> setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold;");
                        case "terminé" -> setStyle("-fx-text-fill: #6366f1; -fx-font-weight: bold;");
                        case "annulé" -> setStyle("-fx-text-fill: #ef4444;");
                        case "absent" -> setStyle("-fx-text-fill: #6b7280;");
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
                        Button btnConfirmer = createButton("✓ Confirmer", "#10b981");
                        Button btnRefuser = createButton("✗ Refuser", "#ef4444");
                        btnConfirmer.setOnAction(e -> changerStatut(rdv, "confirme"));
                        btnRefuser.setOnAction(e -> changerStatut(rdv, "annulé"));
                        buttons.getChildren().addAll(btnConfirmer, btnRefuser);
                        break;
                    case "confirme":
                        Button btnEnCours = createButton("▶ En cours", "#0ea5e9");
                        Button btnAnnuler = createButton("✗ Annuler", "#ef4444");
                        btnEnCours.setOnAction(e -> changerStatut(rdv, "en-cours"));
                        btnAnnuler.setOnAction(e -> changerStatut(rdv, "annulé"));
                        buttons.getChildren().addAll(btnEnCours, btnAnnuler);
                        break;
                    case "en-cours":
                        Button btnTerminer = createButton("✓ Terminer", "#6366f1");
                        Button btnAbsent = createButton("⏤ Absent", "#6b7280");
                        btnTerminer.setOnAction(e -> changerStatut(rdv, "terminé"));
                        btnAbsent.setOnAction(e -> changerStatut(rdv, "absent"));
                        buttons.getChildren().addAll(btnTerminer, btnAbsent);
                        break;
                    default:
                        Label info = new Label("✓ Traité");
                        info.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                        buttons.getChildren().add(info);
                }
                setGraphic(buttons);
            }
        });
    }

    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
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
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous " + getMessageStatut(nouveauStatut));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier le statut: " + e.getMessage());
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

            // Mettre à jour les statistiques
            majStatistiques(rendezVous);

            appliquerFiltres();

        } catch (SQLException e) {
            lblStatut.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void majStatistiques(List<RendezVousDetail> rendezVous) {
        int total = rendezVous.size();
        long demande = rendezVous.stream().filter(r -> r.getStatut().equalsIgnoreCase("demande")).count();
        long confirme = rendezVous.stream().filter(r -> r.getStatut().equalsIgnoreCase("confirme")).count();
        long enCours = rendezVous.stream().filter(r -> r.getStatut().equalsIgnoreCase("en-cours")).count();

        lblStatTotal.setText(String.valueOf(total));
        lblStatDemande.setText(String.valueOf(demande));
        lblStatConfirme.setText(String.valueOf(confirme));
        lblStatEnCours.setText(String.valueOf(enCours));
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}