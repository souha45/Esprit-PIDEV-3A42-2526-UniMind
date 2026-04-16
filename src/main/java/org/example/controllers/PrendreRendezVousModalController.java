package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.models.DisponibilitePsy;
import org.example.models.Psychologue;
import org.example.models.RendezVous;
import org.example.services.DisponibilitePsyService;
import org.example.services.RendezVousService;
import org.example.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class PrendreRendezVousModalController {

    // FXML
    @FXML private ComboBox<String> comboPsy;
    @FXML private ComboBox<String> comboType;
    @FXML private DatePicker       datePickerFiltre;
    @FXML private TilePane         gridCreneaux;
    @FXML private HBox             boxSelection;
    @FXML private Label            lblSelection;
    @FXML private Label            lblNbCreneaux;
    @FXML private Button           btnDeselectionner;
    @FXML private TextArea         txtMotif;
    @FXML private Button           btnFermer;
    @FXML private Button           btnAnnuler;
    @FXML private Button           btnConfirmer;

    // Services
    private DisponibilitePsyService disponibiliteService;
    private RendezVousService       rendezVousService;
    private UserService             userService;

    // Données
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private FilteredList<DisponibilitePsy>   filteredList;
    private DisponibilitePsy                 disponibiliteSelectionnee;
    private int                              etudiantId;
    private Stage                            modalStage;

    // Méthode utilitaire pour récupérer le nom du psychologue
    private String getNomPsychologue(int userId) {
        if (userService == null) {
            return "Psy #" + userId;
        }
        try {
            Psychologue psy = userService.getPsychologueById(userId);
            if (psy != null) {
                return "Dr. " + psy.getPrenom() + " " + psy.getNom().toUpperCase();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du nom du psychologue: " + e.getMessage());
        }
        return "Psy #" + userId;
    }

    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();
        rendezVousService    = new RendezVousService();
        userService          = new UserService();
        disponibilitesList   = FXCollections.observableArrayList();
        filteredList         = new FilteredList<>(disponibilitesList, p -> true);

        initialiserFiltres();

        btnConfirmer.setOnAction(e -> confirmerReservation());
        btnAnnuler.setOnAction(e  -> fermerModal());
        btnFermer.setOnAction(e   -> fermerModal());
        btnDeselectionner.setOnAction(e -> deselectionner());

        // Hover bouton confirmer
        btnConfirmer.setOnMouseEntered(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#6366f1","#4f46e5")));
        btnConfirmer.setOnMouseExited(e ->
                btnConfirmer.setStyle(btnConfirmer.getStyle().replace("#4f46e5","#6366f1")));

        // Hover bouton fermer header
        btnFermer.setOnMouseEntered(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("rgba(255,255,255,0.18)","rgba(255,255,255,0.30)")));
        btnFermer.setOnMouseExited(e ->
                btnFermer.setStyle(btnFermer.getStyle().replace("rgba(255,255,255,0.30)","rgba(255,255,255,0.18)")));
    }

    // Filtres
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous", "présentiel", "en ligne"));
        comboType.setValue("Tous");

        comboPsy.valueProperty().addListener((o, ov, nv)          -> appliquerFiltres());
        comboType.valueProperty().addListener((o, ov, nv)         -> appliquerFiltres());
        datePickerFiltre.valueProperty().addListener((o, ov, nv)  -> appliquerFiltres());
    }

    private void remplirComboPsy(List<DisponibilitePsy> liste) {
        ObservableList<String> items = FXCollections.observableArrayList("Tous");
        liste.stream()
                .map(d -> getNomPsychologue(d.getUserId()))
                .distinct()
                .forEach(items::add);
        comboPsy.setItems(items);
        comboPsy.setValue("Tous");
    }

    private void appliquerFiltres() {
        String psy  = comboPsy.getValue();
        String type = comboType.getValue();
        LocalDate dateMin = datePickerFiltre.getValue();

        filteredList.setPredicate(d -> {
            boolean matchPsy = psy == null || "Tous".equals(psy)
                    || getNomPsychologue(d.getUserId()).equals(psy);

            boolean matchType = type == null || "Tous".equals(type)
                    || d.getTypeConsult().toString().equalsIgnoreCase(type);

            boolean matchDate = dateMin == null
                    || !d.getDateDispo().toLocalDate().isBefore(dateMin);

            return matchPsy && matchType && matchDate;
        });

        int nb = filteredList.size();
        lblNbCreneaux.setText(nb + " créneau" + (nb > 1 ? "x" : "") + " disponible" + (nb > 1 ? "s" : ""));

        creerCartesDisponibilites();
    }

    // Création des cartes
    private void creerCartesDisponibilites() {
        gridCreneaux.getChildren().clear();

        for (DisponibilitePsy dispo : filteredList) {
            VBox carte = creerCarteDisponibilite(dispo);
            gridCreneaux.getChildren().add(carte);
        }
    }

    private VBox creerCarteDisponibilite(DisponibilitePsy dispo) {
        VBox carte = new VBox(8);
        carte.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e7ff; " +
                "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; " +
                "-fx-padding: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.08), 6, 0, 0, 2);");
        carte.setPrefWidth(200);
        carte.setPrefHeight(100);

        // En-tête avec psychologue (nom réel)
        String nomPsy = getNomPsychologue(dispo.getUserId());
        Label psyLabel = new Label("Dr. " + nomPsy);
        psyLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: #3730a3;");

        // Date avec jour
        LocalDate ld = dispo.getDateDispo().toLocalDate();
        String jour = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        String date = ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Label dateLabel = new Label(jour + "\n" + date);
        dateLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                "-fx-text-fill: #374151;");

        // Horaires
        String debut = dispo.getHeureDebut().toString().substring(0, 5);
        String fin = dispo.getHeureFin().toString().substring(0, 5);
        Label horairesLabel = new Label(" " + debut + " - " + fin);
        horairesLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-text-fill: #6366f1;");

        // Type badge
        String type = dispo.getTypeConsult().toString();
        boolean presentiel = "présentiel".equalsIgnoreCase(type);
        Label typeBadge = new Label(presentiel ? " Présentiel" : " En ligne");
        typeBadge.setStyle("-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-padding: 3 8; -fx-background-radius: 12;");

        // Lieu
        String lieu = dispo.getLieu();
        String lieuText = (lieu != null && !lieu.isEmpty()) ? lieu : "Non spécifié";
        Label lieuLabel = new Label(" " + lieuText);
        lieuLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; " +
                "-fx-text-fill: #374151;");

        carte.getChildren().addAll(psyLabel, dateLabel, horairesLabel, typeBadge, lieuLabel);

        // Gestion du clic
        carte.setOnMouseClicked(e -> {
            if (disponibiliteSelectionnee != null && disponibiliteSelectionnee.equals(dispo)) {
                deselectionner();
            } else {
                disponibiliteSelectionnee = dispo;
                afficherBandeauSelection(dispo);
                mettreAJourStyleCartes();
            }
        });

        // Hover effects
        carte.setOnMouseEntered(ev -> {
            if (!dispo.equals(disponibiliteSelectionnee)) {
                carte.setStyle("-fx-background-color: #f8f7ff; -fx-border-color: #c7d2fe; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; " +
                        "-fx-padding: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.15), 8, 0, 0, 3);");
            }
        });
        carte.setOnMouseExited(ev -> {
            if (!dispo.equals(disponibiliteSelectionnee)) {
                carte.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e7ff; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; " +
                        "-fx-padding: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.08), 6, 0, 0, 2);");
            }
        });

        return carte;
    }

    private void mettreAJourStyleCartes() {
        for (int i = 0; i < gridCreneaux.getChildren().size(); i++) {
            VBox carte = (VBox) gridCreneaux.getChildren().get(i);
            DisponibilitePsy dispo = filteredList.get(i);

            if (dispo.equals(disponibiliteSelectionnee)) {
                carte.setStyle("-fx-background-color: #ede9fe; -fx-border-color: #6366f1; " +
                        "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; " +
                        "-fx-padding: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.25), 8, 0, 0, 3);");
            } else {
                carte.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e7ff; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; " +
                        "-fx-padding: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(99,102,241,0.08), 6, 0, 0, 2);");
            }
        }
    }

    // Sélection
    private void afficherBandeauSelection(DisponibilitePsy d) {
        String debut = d.getHeureDebut().toString().substring(0, 5);
        String fin   = d.getHeureFin().toString().substring(0, 5);
        String date  = d.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        String nomPsy = getNomPsychologue(d.getUserId());

        lblSelection.setText("Dr. " + nomPsy
                + "  ·  " + date
                + "  ·  " + debut + " - " + fin
                + "  ·  " + d.getTypeConsult());
        boxSelection.setVisible(true);
        boxSelection.setManaged(true);
    }

    private void deselectionner() {
        disponibiliteSelectionnee = null;
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);
        lblSelection.setText("");
        mettreAJourStyleCartes();
    }

    // Chargement
    private void chargerDisponibilites() {
        try {
            List<DisponibilitePsy> disponibilites =
                    disponibiliteService.afficherDisponibilitesDisponibles();

            disponibilitesList.setAll(disponibilites);
            remplirComboPsy(disponibilites);
            appliquerFiltres();

        } catch (SQLException e) {
            lblNbCreneaux.setText("Erreur de chargement");
            e.printStackTrace();
        }
    }

    // Confirmation réservation
    private void confirmerReservation() {
        if (disponibiliteSelectionnee == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucune sélection",
                    "Veuillez sélectionner un créneau disponible.");
            return;
        }

        String motifSaisi = txtMotif.getText().trim();
        final String motifFinal = motifSaisi.isEmpty() ? "Consultation psychologique" : motifSaisi;
        final DisponibilitePsy dispoSelectionnee = disponibiliteSelectionnee;
        final int etudiantIdFinal = this.etudiantId;

        String debut = dispoSelectionnee.getHeureDebut().toString().substring(0, 5);
        String fin   = dispoSelectionnee.getHeureFin().toString().substring(0, 5);
        String nomPsy = getNomPsychologue(dispoSelectionnee.getUserId());

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de réservation");
        confirmation.setHeaderText("Confirmer la réservation");
        confirmation.setContentText(
                " Date : "         + dispoSelectionnee.getDateDispo()       + "\n" +
                        " Horaire : "       + debut + " - " + fin                    + "\n" +
                        " Psychologue : Dr. " + nomPsy                             + "\n" +
                        " Type : "          + dispoSelectionnee.getTypeConsult()     + "\n" +
                        " Motif : "         + motifFinal                             + "\n\n" +
                        "Confirmez-vous cette réservation ?"
        );

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                effectuerReservation(dispoSelectionnee, etudiantIdFinal, motifFinal);
            }
        });
    }

    private void effectuerReservation(DisponibilitePsy dispo, int etudiantId, String motif) {
        try {
            RendezVous rendezVous = new RendezVous(
                    dispo.getDispoId(),
                    etudiantId,
                    dispo.getUserId(),
                    motif
            );
            rendezVousService.ajouter(rendezVous);

            String debut = dispo.getHeureDebut().toString().substring(0, 5);
            String fin   = dispo.getHeureFin().toString().substring(0, 5);

            afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                    " Rendez-vous réservé avec succès !\n\n" +
                            " Date : "    + dispo.getDateDispo()  + "\n" +
                            " Horaire : " + debut + " - " + fin    + "\n" +
                            " Motif : "   + motif);

            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    " Impossible de réserver le rendez-vous.\nErreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Utilitaires
    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void fermerModal() {
        if (modalStage != null) modalStage.close();
    }

    // Appelées depuis l'extérieur
    public void setEtudiantId(int id) {
        this.etudiantId = id;
        chargerDisponibilites();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}
