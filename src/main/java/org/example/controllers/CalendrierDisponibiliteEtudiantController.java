package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.DisponibilitePsy;
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
import java.util.function.Consumer;

public class CalendrierDisponibiliteEtudiantController {

    @FXML private GridPane gridCalendrier;
    @FXML private GridPane gridEntetes;
    @FXML private Label    lblMoisAnnee;
    @FXML private Button   btnMoisPrec;
    @FXML private Button   btnMoisSuiv;
    @FXML private Button   btnRetour;
    @FXML private Label    lblSelection;

    private DisponibilitePsyService service;
    private Stage modalStage;
    private Consumer<DisponibilitePsy> onCreneauSelectionne;

    private YearMonth moisCourant = YearMonth.now();
    private Map<LocalDate, List<DisponibilitePsy>> creneauxParJour = new HashMap<>();
    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

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
            lblSelection.setText("Cliquez sur un créneau pour le sélectionner et prendre rendez-vous");
            lblSelection.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:11px; -fx-text-fill:#9ca3af; -fx-font-style:italic; -fx-padding:8 0 0 0;");
        }

        chargerCreneaux();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }

    public void setOnCreneauSelectionne(Consumer<DisponibilitePsy> callback) {
        this.onCreneauSelectionne = callback;
    }

    private void chargerCreneaux() {
        creneauxParJour.clear();
        try {
            // Récupérer UNIQUEMENT les créneaux disponibles (statut = "disponible")
            List<DisponibilitePsy> liste = service.afficherDisponibilitesDisponibles();
            for (DisponibilitePsy d : liste) {
                LocalDate jour = d.getDateDispo().toLocalDate();
                creneauxParJour.computeIfAbsent(jour, k -> new ArrayList<>()).add(d);
            }
            rafraichir();
        } catch (SQLException e) {
            System.err.println("[Calendrier Étudiant] Erreur chargement : " + e.getMessage());
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

    // ── CELLULE POUR ÉTUDIANT (simple clic, pas de glisser-déposer) ──
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
        cellule.getChildren().add(topRow);

        // Créneaux disponibles (UNIQUEMENT les DISPONIBLES)
        VBox creneauxContainer = new VBox(2);
        for (DisponibilitePsy d : creneaux) {
            if ("disponible".equalsIgnoreCase(d.getStatut().toString())) {
                Button btnCreneau = construireBoutonCreneau(d);
                creneauxContainer.getChildren().add(btnCreneau);
            }
        }

        if (creneauxContainer.getChildren().isEmpty() && !estPasse) {
            Label lblEmpty = new Label("Aucun\ncréneau");
            lblEmpty.setStyle("-fx-font-size:10px;-fx-text-fill:#c4b5fd;-fx-alignment:CENTER;");
            lblEmpty.setAlignment(Pos.CENTER);
            creneauxContainer.getChildren().add(lblEmpty);
        }

        cellule.getChildren().add(creneauxContainer);

        return cellule;
    }

    // ── BOUTON POUR UN CRÉNEAU (simple clic = sélection) ──
    private Button construireBoutonCreneau(DisponibilitePsy d) {
        String debut = d.getHeureDebut().toString().substring(0, 5);
        String fin = d.getHeureFin().toString().substring(0, 5);
        boolean presentiel = "présentiel".equalsIgnoreCase(d.getTypeConsult().toString());

        Button btn = new Button((presentiel ? "🏢" : "💻") + " " + debut + "–" + fin);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:#ede9fe; -fx-text-fill:#5b21b6;" +
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:4 6;-fx-background-radius:6;" +
                "-fx-cursor:hand;");

        // Tooltip avec détails
        String lieu = d.getLieu() != null && !d.getLieu().isEmpty() ? d.getLieu() : "En ligne";
        Tooltip.install(btn, new Tooltip(
                d.getTypeConsult() + "\n📍 " + lieu + "\n" + debut + " – " + fin
        ));

        // Clic = sélectionner et fermer
        btn.setOnAction(e -> {
            if (onCreneauSelectionne != null) {
                onCreneauSelectionne.accept(d);
            }
            fermer();
        });

        return btn;
    }

    private void fermer() {
        if (modalStage != null) modalStage.close();
        else if (gridCalendrier.getScene() != null)
            ((Stage) gridCalendrier.getScene().getWindow()).close();
    }
}