package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.DisponibilitePsy;
import org.example.entities.User;
import org.example.services.DisponibilitePsyService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CalendrierDisponibiliteController {

    @FXML private GridPane gridCalendrier;
    @FXML private GridPane gridEntetes;
    @FXML private Label    lblMoisAnnee;
    @FXML private Button   btnMoisPrec;
    @FXML private Button   btnMoisSuiv;
    @FXML private Button   btnRetour;
    @FXML private Label    lblSelection;

    private DisponibilitePsyService service;
    private User utilisateur;
    private Stage calendrierStage;

    private YearMonth moisCourant = YearMonth.now();
    private Map<LocalDate, List<DisponibilitePsy>> creneauxParJour = new HashMap<>();
    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    // Variables pour le glisser-déposer direct sur la cellule
    private LocalDate dateEnSelection;
    private int heureDebutSelection = -1;
    private int heureFinSelection = -1;
    private Label labelInfoActif;
    private double debutY = 0;

    @FXML
    public void initialize() {
        service = new DisponibilitePsyService();

        btnMoisPrec.setOnAction(e -> {
            moisCourant = moisCourant.minusMonths(1);
            rafraichir();
        });
        btnMoisSuiv.setOnAction(e -> {
            moisCourant = moisCourant.plusMonths(1);
            rafraichir();
        });
        btnRetour.setOnAction(e -> fermer());

        construireEntetes();

        if (lblSelection != null) {
            lblSelection.setText("Cliquez et glissez verticalement sur un jour pour sélectionner une plage horaire");
            lblSelection.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:11px; -fx-text-fill:#9ca3af; -fx-font-style:italic; -fx-padding:8 0 0 0;");
        }
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;
        chargerCreneaux();
        rafraichir();
    }

    public void setCalendrierStage(Stage stage) {
        this.calendrierStage = stage;
    }

    private void chargerCreneaux() {
        creneauxParJour.clear();
        if (utilisateur == null) return;
        try {
            List<DisponibilitePsy> liste = service.afficherDisponibilitesPsy(utilisateur.getUserId());
            for (DisponibilitePsy d : liste) {
                LocalDate jour = d.getDateDispo().toLocalDate();
                creneauxParJour.computeIfAbsent(jour, k -> new ArrayList<>()).add(d);
            }
        } catch (SQLException e) {
            System.err.println("[Calendrier] Erreur chargement : " + e.getMessage());
        }
    }

    private void construireEntetes() {
        gridEntetes.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(JOURS[i]);
            boolean weekend = (i == 5 || i == 6);
            lbl.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px; -fx-font-weight:bold;" +
                    "-fx-text-fill:" + (weekend ? "#a5b4fc" : "#6366f1") + ";" +
                    "-fx-alignment:CENTER; -fx-padding:4 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(lbl, Priority.ALWAYS);
            gridEntetes.add(lbl, i, 0);
        }
    }

    private void rafraichir() {
        String moisNom = moisCourant.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        moisNom = Character.toUpperCase(moisNom.charAt(0)) + moisNom.substring(1);
        lblMoisAnnee.setText(moisNom + " " + moisCourant.getYear());

        gridCalendrier.getChildren().clear();
        gridCalendrier.getRowConstraints().clear();

        LocalDate premier = moisCourant.atDay(1);
        int decalage = premier.getDayOfWeek().getValue() - 1;
        int nbJours = moisCourant.lengthOfMonth();
        int nbLignes = (int) Math.ceil((decalage + nbJours) / 7.0);

        for (int r = 0; r < nbLignes; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(100);
            rc.setPrefHeight(120);
            gridCalendrier.getRowConstraints().add(rc);
        }

        LocalDate today = LocalDate.now();

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            int cellIndex = decalage + jour - 1;
            int col = cellIndex % 7;
            int row = cellIndex / 7;

            List<DisponibilitePsy> creneaux = creneauxParJour.getOrDefault(date, Collections.emptyList());

            VBox cellule = construireCellule(date, creneaux, today);
            gridCalendrier.add(cellule, col, row);
        }
    }

    // ── CELLULE AVEC GLISSER-DÉPOSER DIRECT ──────────────────────────
    private VBox construireCellule(LocalDate date, List<DisponibilitePsy> creneaux, LocalDate today) {
        boolean estAujourdhui = date.equals(today);
        boolean estPasse = date.isBefore(today);
        boolean weekend = (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);

        String bgColor;
        if (estAujourdhui) bgColor = "#eef2ff";
        else if (estPasse) bgColor = "#f9fafb";
        else if (weekend) bgColor = "#f5f3ff";
        else bgColor = "#ffffff";

        String bordure = estAujourdhui ? "-fx-border-color: #6366f1; -fx-border-width: 2;" : "-fx-border-color: #e0e7ff; -fx-border-width: 1;";

        VBox cellule = new VBox(3);
        cellule.setMaxWidth(Double.MAX_VALUE);
        cellule.setMaxHeight(Double.MAX_VALUE);
        cellule.setStyle("-fx-background-color:" + bgColor + ";" + bordure +
                "-fx-background-radius:10; -fx-border-radius:10; -fx-padding:6 4;");

        // Numéro du jour
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setStyle("-fx-padding:0 0 4 2;");

        Label lblJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblJour.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px; -fx-font-weight:bold;" +
                "-fx-text-fill:" + (estAujourdhui ? "#6366f1" : estPasse ? "#d1d5db" : weekend ? "#8b5cf6" : "#374151") + ";");

        if (estAujourdhui) {
            StackPane badge = new StackPane(lblJour);
            badge.setStyle("-fx-background-color:#6366f1; -fx-background-radius:50; -fx-min-width:22; -fx-min-height:22;");
            lblJour.setStyle("-fx-text-fill:white; -fx-font-size:11px;");
            topRow.getChildren().add(badge);
        } else {
            topRow.getChildren().add(lblJour);
        }

        // Label d'info pour la sélection en cours
        Label lblInfo = new Label("");
        lblInfo.setStyle("-fx-font-size:9px; -fx-text-fill:#8b5cf6; -fx-padding:2; -fx-background-color:#ede9fe; -fx-background-radius:8;");
        lblInfo.setAlignment(Pos.CENTER);
        lblInfo.setMaxWidth(Double.MAX_VALUE);
        lblInfo.setVisible(false);

        cellule.getChildren().add(topRow);

        // Créneaux existants
        VBox creneauxContainer = new VBox(2);
        for (DisponibilitePsy d : creneaux) {
            Label chip = construireChipCreneau(d);
            creneauxContainer.getChildren().add(chip);
        }
        cellule.getChildren().add(creneauxContainer);
        cellule.getChildren().add(lblInfo);

        // Gestion du glisser-déposer sur la cellule entière (sauf si date passée)
        if (!estPasse) {
            cellule.setOnMousePressed(event -> {
                dateEnSelection = date;
                debutY = event.getY();
                double hauteur = cellule.getHeight();
                heureDebutSelection = 8 + (int)((debutY / hauteur) * 12);
                heureDebutSelection = Math.max(8, Math.min(19, heureDebutSelection));
                heureFinSelection = heureDebutSelection + 1;
                afficherInfoSelection(date, lblInfo);
                event.consume();
            });

            cellule.setOnMouseDragged(event -> {
                if (dateEnSelection != null && dateEnSelection.equals(date)) {
                    double y = event.getY();
                    double hauteur = cellule.getHeight();
                    int nouvelleHeure = 8 + (int)((y / hauteur) * 12);
                    nouvelleHeure = Math.max(8, Math.min(20, nouvelleHeure));

                    if (nouvelleHeure < heureDebutSelection) {
                        heureDebutSelection = nouvelleHeure;
                        heureFinSelection = heureDebutSelection + 1;
                    } else if (nouvelleHeure >= heureFinSelection) {
                        heureFinSelection = nouvelleHeure;
                        if (heureFinSelection > heureDebutSelection + 3) heureFinSelection = heureDebutSelection + 3;
                    }

                    heureDebutSelection = Math.max(8, Math.min(19, heureDebutSelection));
                    heureFinSelection = Math.max(heureDebutSelection + 1, Math.min(20, heureFinSelection));

                    afficherInfoSelection(date, lblInfo);
                    event.consume();
                }
            });

            cellule.setOnMouseReleased(event -> {
                if (dateEnSelection != null && heureDebutSelection != -1 && heureFinSelection != -1) {
                    ouvrirFormulaireAvecCreneau(date,
                            LocalTime.of(heureDebutSelection, 0),
                            LocalTime.of(heureFinSelection, 0));
                }
                reinitialiserSelection(lblInfo);
                event.consume();
            });

            // Tooltip informatif
            Tooltip.install(cellule, new Tooltip("Glissez verticalement pour sélectionner une plage horaire\n(déroulez de 8h à 20h)"));
        }

        return cellule;
    }

    private void afficherInfoSelection(LocalDate date, Label lblInfo) {
        if (lblInfo != null) {
            lblInfo.setText(String.format("📌 %02d:00 - %02d:00", heureDebutSelection, heureFinSelection));
            lblInfo.setVisible(true);
        }
        if (lblSelection != null) {
            lblSelection.setText(String.format("Sélection : %s de %02d:00 à %02d:00",
                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), heureDebutSelection, heureFinSelection));
        }
    }

    private void reinitialiserSelection(Label lblInfo) {
        if (lblInfo != null) {
            lblInfo.setVisible(false);
        }
        if (lblSelection != null) {
            lblSelection.setText("Cliquez et glissez verticalement sur un jour pour sélectionner une plage horaire");
        }
        dateEnSelection = null;
        heureDebutSelection = -1;
        heureFinSelection = -1;
    }

    private Label construireChipCreneau(DisponibilitePsy d) {
        String statut = d.getStatut().toString().toLowerCase();
        String debut = d.getHeureDebut().toString().substring(0, 5);
        String fin = d.getHeureFin().toString().substring(0, 5);

        String bg, fg;
        switch (statut) {
            case "disponible" -> { bg = "#dcfce7"; fg = "#16a34a"; }
            case "réservé"    -> { bg = "#fff7ed"; fg = "#c2410c"; }
            case "annulé"     -> { bg = "#fee2e2"; fg = "#dc2626"; }
            default           -> { bg = "#f3f4f6"; fg = "#6b7280"; }
        }

        Label chip = new Label(debut + "–" + fin);
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setAlignment(Pos.CENTER);
        chip.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                "-fx-font-family:'Segoe UI'; -fx-font-size:9px; -fx-font-weight:bold;" +
                "-fx-padding:2 4; -fx-background-radius:12; -fx-cursor:hand;");

        Tooltip.install(chip, new Tooltip(debut + " – " + fin + "\n" + d.getTypeConsult() + "\n" +
                (d.getLieu() != null ? d.getLieu() : "En ligne") + "\nStatut : " + statut));

        LocalTime hDebut = d.getHeureDebut().toLocalTime();
        LocalTime hFin = d.getHeureFin().toLocalTime();
        LocalDate jour = d.getDateDispo().toLocalDate();
        chip.setOnMouseClicked(ev -> {
            ev.consume();
            ouvrirFormulaireAvecCreneau(jour, hDebut, hFin);
        });

        return chip;
    }

    private void ouvrirFormulaireAvecCreneau(LocalDate date, LocalTime heureDebut, LocalTime heureFin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutDisponibiliteModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(calendrierStage != null ? calendrierStage : gridCalendrier.getScene().getWindow());
            modalStage.setTitle("Nouvelle disponibilité");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            AjoutDisponibiliteController controller = loader.getController();
            controller.setUserId(utilisateur.getUserId());
            controller.setModalStage(modalStage);
            controller.setDatePreRemplie(date);
            controller.setHeuresPreRemplies(heureDebut, heureFin);

            modalStage.showAndWait();

            chargerCreneaux();
            rafraichir();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir le formulaire");
            alert.showAndWait();
        }
    }

    private void fermer() {
        if (calendrierStage != null) calendrierStage.close();
        else if (gridCalendrier.getScene() != null)
            ((Stage) gridCalendrier.getScene().getWindow()).close();
    }
}