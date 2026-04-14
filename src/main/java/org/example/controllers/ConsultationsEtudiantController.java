package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.example.models.ConsultationDetail;
import org.example.models.User;
import org.example.services.ConsultationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class ConsultationsEtudiantController implements SidebarEtudiantController.EtudiantPageController {

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblDate;

    // ── Stat cards ──────────────────────────────────────────────────
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatNotees;
    @FXML private Label lblStatMoyenne;
    @FXML private Label lblStatCeMois;

    // ── Filtres ─────────────────────────────────────────────────────
    @FXML private TextField fieldRecherche;

    // ── Table ───────────────────────────────────────────────────────
    @FXML private TableView<ConsultationDetail> tableViewConsultations;
    @FXML private TableColumn<ConsultationDetail, String> colDate;
    @FXML private TableColumn<ConsultationDetail, String> colPsychologue;
    @FXML private TableColumn<ConsultationDetail, String> colMotif;
    @FXML private TableColumn<ConsultationDetail, String> colNote;
    @FXML private TableColumn<ConsultationDetail, Void> colActions;

    // ── Toolbar ─────────────────────────────────────────────────────
    @FXML private Button btnRafraichir;
    @FXML private Label lblStatut;

    // ── Données ─────────────────────────────────────────────────────
    private User utilisateur;
    private ConsultationService consultationService;
    private ObservableList<ConsultationDetail> consultationsList;
    private FilteredList<ConsultationDetail> filteredList;

    @FXML
    public void initialize() {
        // Date header
        LocalDate today = LocalDate.now();
        String jour = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(
                jour.substring(0, 1).toUpperCase() + jour.substring(1)
                        + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

        consultationService = new ConsultationService();
        consultationsList = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(consultationsList, p -> true);

        configurerColonnes();
        configurerFiltres();

        tableViewConsultations.setItems(filteredList);
        tableViewConsultations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        btnRafraichir.setOnAction(e -> chargerConsultations());
    }

    private void configurerFiltres() {
        fieldRecherche.textProperty().addListener((o, ov, nv) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        String recherche = fieldRecherche.getText() == null ? ""
                : fieldRecherche.getText().toLowerCase().trim();

        filteredList.setPredicate(consultation -> {
            boolean matchRecherche = recherche.isEmpty()
                    || ("Dr. " + consultation.getPsyPrenom() + " " + consultation.getPsyNom()).toLowerCase().contains(recherche)
                    || consultation.getDateDispo().toString().contains(recherche);

            return matchRecherche;
        });

        int nb = filteredList.size();
        lblStatut.setText(nb == 0 ? "Aucune consultation trouvée."
                : nb + " consultation(s) affichée(s) sur " + consultationsList.size());
    }

    private void configurerColonnes() {

        // Date (formatée avec jour)
        colDate.setCellValueFactory(cell -> {
            ConsultationDetail c = cell.getValue();
            LocalDate date = c.getDateDispo().toLocalDate();
            String jourSemaine = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            return new SimpleStringProperty(
                    jourSemaine + "\n" + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                            c.getHeureDebut().toString().substring(0, 5) + " - " +
                            c.getHeureFin().toString().substring(0, 5));
        });
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: #374151; -fx-padding: 10 16; -fx-alignment: CENTER_LEFT;");
            }
        });

        // Psychologue
        colPsychologue.setCellValueFactory(cell -> {
            ConsultationDetail c = cell.getValue();
            return new SimpleStringProperty("Dr. " + c.getPsyPrenom() + " " + c.getPsyNom());
        });
        colPsychologue.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    return;
                }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #3730a3; -fx-padding: 10 16;");
            }
        });

        // Motif (avis du psychologue)
        colMotif.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getAvisFormatted()));
        colMotif.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    return;
                }
                if (s.length() > 60) {
                    setText(s.substring(0, 60) + "...");
                } else {
                    setText(s);
                }
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: #4b5563; -fx-padding: 10 16;");
            }
        });

        // Note (avec étoiles)
        colNote.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNoteFormatted()));
        colNote.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    return;
                }

                if (s.contains("/5") && !s.equals("À NOTER")) {
                    String noteStr = s.replace("/5", "");
                    try {
                        int note = Integer.parseInt(noteStr);
                        String etoiles = getEtoiles(note);
                        Label badge = new Label(etoiles + " " + s);
                        if (note <= 2) {
                            badge.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        } else if (note <= 4) {
                            badge.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        } else {
                            badge.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        }
                        setGraphic(badge);
                        setText(null);
                    } catch (NumberFormatException e) {
                        setText(s);
                        setStyle("");
                    }
                } else {
                    Label badge = new Label("⭐ À noter");
                    badge.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                    setGraphic(badge);
                    setText(null);
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

        // Actions (bouton Voir)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir = new Button("🔍");
            private final HBox box = new HBox(6, btnVoir);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                String styleView = "-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; " +
                        "-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
                btnVoir.setStyle(styleView);
                btnVoir.setTooltip(new Tooltip("Voir les détails"));
                btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(styleView.replace("#ede9fe", "#ddd6fe")));
                btnVoir.setOnMouseExited(e -> btnVoir.setStyle(styleView));

                btnVoir.setOnAction(e ->
                        afficherDetailsConsultation(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
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
                        "⏰ Horaire: " + consultation.getHeureDebut().toString().substring(0, 5) +
                        " - " + consultation.getHeureFin().toString().substring(0, 5) + "\n" +
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

            consultationsList.setAll(consultations);
            mettreAJourStatCards(consultations);
            appliquerFiltres();

        } catch (SQLException e) {
            lblStatut.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mettreAJourStatCards(List<ConsultationDetail> consultations) {
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
        lblStatMoyenne.setText(moyenne > 0 ? String.format("%.1f", moyenne) : "—");
        lblStatCeMois.setText(String.valueOf(ceMois));
    }

    // ── Interface EtudiantPageController ────────────────────────────
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml("/ConsultationsEtudiant.fxml");
        }
        chargerConsultations();
    }
}