package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.example.entities.RendezVousDetail;
import org.example.entities.User;
import org.example.services.RendezVousService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class RendezVousPsyController implements SidebarPsychologueController.PsyPageController {

    // ========== COMPOSANTS FXML ==========
    @FXML private TableView<RendezVousDetail> tableViewRendezVous;
    @FXML private TableColumn<RendezVousDetail, String> colEtudiant;
    @FXML private TableColumn<RendezVousDetail, String> colDateHeure;
    @FXML private TableColumn<RendezVousDetail, String> colType;
    @FXML private TableColumn<RendezVousDetail, String> colStatut;
    @FXML private TableColumn<RendezVousDetail, Void> colActions;
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
    @FXML private SidebarPsychologueController sidebarPsyController;

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
        // Colonne Étudiant (nom + prénom + email avec wrap-text)
        colEtudiant.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getEtudiantPrenom() + " " +
                                cellData.getValue().getEtudiantNom() + "\n" +
                                cellData.getValue().getEtudiantEmail()
                ));

        colEtudiant.setCellFactory(column -> new TableCell<RendezVousDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                            "-fx-text-fill: #374151; " +
                            "-fx-padding: 10 16; " +
                            "-fx-wrap-text: true;");
                }
            }
        });

        // Colonne Date et Heure
        colDateHeure.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDateDispo().toString() + "\n" +
                                cellData.getValue().getHeureDebut() + " - " +
                                cellData.getValue().getHeureFin()
                ));

        // Colonne Type (badge coloré comme dans les disponibilités)
        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTypeConsult().toString()));

        colType.setCellFactory(column -> new TableCell<RendezVousDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                boolean presentiel = "présentiel".equalsIgnoreCase(item);
                Label badge = new Label(presentiel ? "Présentiel" : "En ligne");
                badge.setStyle(
                        "-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Colonne Statut (badge coloré comme dans les disponibilités)
        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatut().toString()));

        colStatut.setCellFactory(column -> new TableCell<RendezVousDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                String bg, fg, txt;
                switch (item.toLowerCase()) {
                    case "demande" -> { bg = "#fef3c7"; fg = "#f59e0b"; txt = "Demande"; }
                    case "confirme" -> { bg = "#ecfdf5"; fg = "#10b981"; txt = "Confirmé"; }
                    case "en-cours" -> { bg = "#e0f2fe"; fg = "#0ea5e9"; txt = "En cours"; }
                    case "terminé" -> { bg = "#eef2ff"; fg = "#6366f1"; txt = "Terminé"; }
                    case "annulé" -> { bg = "#fee2e2"; fg = "#ef4444"; txt = "Annulé"; }
                    case "absent" -> { bg = "#f3f4f6"; fg = "#6b7280"; txt = "Absent"; }
                    default -> { bg = "#f3f4f6"; fg = "#6b7280"; txt = item; }
                }
                Label badge = new Label(txt);
                badge.setStyle(
                        "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge);
                setText(null);
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
                        Button btnConfirmer = createButton("Confirmer", "#10b981");
                        Button btnRefuser = createButton("Refuser", "#ef4444");
                        Button btnDetailsDemande = createButton("Détails", "#6b7280");
                        btnConfirmer.setOnAction(e -> changerStatut(rdv, "confirme"));
                        btnRefuser.setOnAction(e -> changerStatut(rdv, "annulé"));
                        btnDetailsDemande.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnConfirmer, btnRefuser, btnDetailsDemande);
                        break;
                    case "confirme":
                        Button btnEnCours = createButton("En cours", "#0ea5e9");
                        Button btnAbsent = createButton("Absent", "#6b7280");
                        Button btnDetailsConfirme = createButton("Détails", "#6b7280");
                        btnEnCours.setOnAction(e -> changerStatut(rdv, "en-cours"));
                        btnAbsent.setOnAction(e -> changerStatut(rdv, "absent"));
                        btnDetailsConfirme.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnEnCours, btnAbsent, btnDetailsConfirme);
                        break;
                    case "en-cours":
                        Button btnTerminer = createButton("Terminer", "#6366f1");
                        Button btnDetailsEnCours = createButton("Détails", "#6b7280");
                        btnTerminer.setOnAction(e -> changerStatut(rdv, "terminé"));
                        btnDetailsEnCours.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnTerminer, btnDetailsEnCours);
                        break;
                    default:
                        Button btnDetailsTraite = createButton("Détails", "#6b7280");
                        Label info = new Label("Traité");
                        info.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                        btnDetailsTraite.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnDetailsTraite, info);
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

    private void afficherDetailsRendezVous(RendezVousDetail rdv) {
        // Afficher les détails complets du rendez-vous avec le motif
        StringBuilder details = new StringBuilder();
        details.append("Patient: ").append(rdv.getEtudiantPrenom()).append(" ").append(rdv.getEtudiantNom()).append("\n");
        details.append("Email: ").append(rdv.getEtudiantEmail()).append("\n");
        details.append("Date: ").append(rdv.getDateDispo()).append("\n");
        details.append("Heure: ").append(rdv.getHeureDebut()).append(" - ").append(rdv.getHeureFin()).append("\n");
        details.append("Type: ").append(rdv.getTypeConsult()).append("\n");
        details.append("Statut: ").append(rdv.getStatut()).append("\n");
        
        // Ajouter le motif si disponible
        if (rdv.getMotif() != null && !rdv.getMotif().trim().isEmpty()) {
            details.append("Motif: ").append(rdv.getMotif());
        } else {
            details.append("Motif: Non spécifié");
        }
        
        showAlert(Alert.AlertType.INFORMATION, "Détails du rendez-vous", details.toString());
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
