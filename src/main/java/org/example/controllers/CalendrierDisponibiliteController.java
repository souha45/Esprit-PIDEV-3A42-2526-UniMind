package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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

    // ── Données ─────────────────────────────────────────────────────
    private DisponibilitePsyService service;
    private User utilisateur;
    private Stage calendrierStage;

    private YearMonth moisCourant = YearMonth.now();

    // Map jour → liste de créneaux (pour affichage rapide)
    private Map<LocalDate, List<DisponibilitePsy>> creneauxParJour = new HashMap<>();

    // Noms des jours (lundi en premier)
    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    // ────────────────────────────────────────────────────────────────
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

        // Hover navigation
        styleHoverBtn(btnMoisPrec);
        styleHoverBtn(btnMoisSuiv);
        styleHoverBtn(btnRetour);

        construireEntetes();
    }

    // ── Injection utilisateur depuis le parent ───────────────────────
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        chargerCreneaux();
        rafraichir();
    }

    public void setCalendrierStage(Stage stage) {
        this.calendrierStage = stage;
    }

    // ── Chargement des créneaux depuis la BDD ───────────────────────
    private void chargerCreneaux() {
        creneauxParJour.clear();
        if (utilisateur == null) return;
        try {
            List<DisponibilitePsy> liste =
                    service.afficherDisponibilitesPsy(utilisateur.getUserId());
            for (DisponibilitePsy d : liste) {
                LocalDate jour = d.getDateDispo().toLocalDate();
                creneauxParJour.computeIfAbsent(jour, k -> new ArrayList<>()).add(d);
            }
        } catch (SQLException e) {
            System.err.println("[Calendrier] Erreur chargement : " + e.getMessage());
        }
    }

    // ── Entêtes colonnes (Lun … Dim) ────────────────────────────────
    private void construireEntetes() {
        gridEntetes.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(JOURS[i]);
            boolean weekend = (i == 5 || i == 6);
            lbl.setStyle(
                    "-fx-font-family:'Segoe UI'; -fx-font-size:12px; -fx-font-weight:bold;" +
                            "-fx-text-fill:" + (weekend ? "#a5b4fc" : "#6366f1") + ";" +
                            "-fx-alignment:CENTER; -fx-padding:4 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(lbl, Priority.ALWAYS);
            gridEntetes.add(lbl, i, 0);
        }
    }

    // ── Construction de la grille pour le mois courant ──────────────
    private void rafraichir() {
        // Label mois / année
        String moisNom = moisCourant.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        moisNom = Character.toUpperCase(moisNom.charAt(0)) + moisNom.substring(1);
        lblMoisAnnee.setText(moisNom + " " + moisCourant.getYear());

        gridCalendrier.getChildren().clear();
        // Vider les contraintes de lignes existantes
        gridCalendrier.getRowConstraints().clear();

        LocalDate premier = moisCourant.atDay(1);
        // Lundi = 1, donc décalage (lundi = col 0)
        int decalage = premier.getDayOfWeek().getValue() - 1; // 0-6
        int nbJours  = moisCourant.lengthOfMonth();
        int nbLignes = (int) Math.ceil((decalage + nbJours) / 7.0);

        // Ajouter les RowConstraints pour que chaque ligne s'étende
        for (int r = 0; r < nbLignes; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(90);
            rc.setPrefHeight(110);
            gridCalendrier.getRowConstraints().add(rc);
        }

        LocalDate today = LocalDate.now();

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            int cellIndex  = decalage + jour - 1;
            int col        = cellIndex % 7;
            int row        = cellIndex / 7;

            List<DisponibilitePsy> creneaux =
                    creneauxParJour.getOrDefault(date, Collections.emptyList());

            VBox cellule = construireCellule(date, creneaux, today);
            gridCalendrier.add(cellule, col, row);
        }
    }

    // ── Construit une cellule jour ───────────────────────────────────
    private VBox construireCellule(LocalDate date,
                                   List<DisponibilitePsy> creneaux,
                                   LocalDate today) {
        boolean estAujourdhui = date.equals(today);
        boolean estPasse      = date.isBefore(today);
        boolean weekend       = (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY);

        // ── Fond de la cellule ───────────────────────────────────────
        String bgColor;
        if (estAujourdhui)       bgColor = "#eef2ff";
        else if (estPasse)       bgColor = "#f9fafb";
        else if (weekend)        bgColor = "#f5f3ff";
        else                     bgColor = "#ffffff";

        String bordure = estAujourdhui
                ? "-fx-border-color: #6366f1; -fx-border-width: 2;"
                : "-fx-border-color: #e0e7ff; -fx-border-width: 1;";

        VBox cellule = new VBox(3);
        cellule.setMaxWidth(Double.MAX_VALUE);
        cellule.setMaxHeight(Double.MAX_VALUE);
        cellule.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        bordure +
                        "-fx-background-radius:10; -fx-border-radius:10; -fx-padding:6 8;");

        // ── Numéro du jour ───────────────────────────────────────────
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblJour.setStyle(
                "-fx-font-family:'Segoe UI'; -fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-text-fill:" + (estAujourdhui ? "#6366f1"
                        : estPasse      ? "#d1d5db"
                        : weekend       ? "#8b5cf6"
                        : "#374151") + ";");

        if (estAujourdhui) {
            // Badge rond pour aujourd'hui
            StackPane badge = new StackPane(lblJour);
            badge.setStyle("-fx-background-color:#6366f1; -fx-background-radius:50;" +
                    "-fx-min-width:24; -fx-min-height:24; -fx-max-width:24; -fx-max-height:24;");
            lblJour.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px;" +
                    "-fx-font-weight:bold; -fx-text-fill:white;");
            topRow.getChildren().add(badge);
        } else {
            topRow.getChildren().add(lblJour);
        }

        // Icône "+" si futur et pas de créneaux (cliquable)
        if (!estPasse && creneaux.isEmpty()) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label plus = new Label("+");
            plus.setStyle("-fx-font-size:14px; -fx-text-fill:#c4b5fd; -fx-cursor:hand;");
            topRow.getChildren().addAll(spacer, plus);
        }

        cellule.getChildren().add(topRow);

        // ── Créneaux du jour ─────────────────────────────────────────
        for (DisponibilitePsy d : creneaux) {
            Label chip = construireChipCreneau(d);
            cellule.getChildren().add(chip);
        }

        // ── Clic sur cellule = ouvrir formulaire (si futur) ─────────
        if (!estPasse) {
            cellule.setOnMouseClicked(e -> ouvrirFormulaireAjout(date, null, null));
            cellule.setOnMouseEntered(ev -> {
                if (!estAujourdhui)
                    cellule.setStyle(cellule.getStyle()
                            .replace(bgColor, "#ede9fe")
                            .replace("#e0e7ff", "#a5b4fc"));
                cellule.setStyle(cellule.getStyle() + "-fx-cursor:hand;");
            });
            cellule.setOnMouseExited(ev ->
                    cellule.setStyle(
                            "-fx-background-color:" + bgColor + ";" +
                                    bordure +
                                    "-fx-background-radius:10; -fx-border-radius:10; -fx-padding:6 8; -fx-cursor:default;")
            );

            Tooltip.install(cellule, new Tooltip(
                    "Cliquez pour ajouter une disponibilité le "
                            + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }

        return cellule;
    }

    // ── Chip coloré pour un créneau ──────────────────────────────────
    private Label construireChipCreneau(DisponibilitePsy d) {
        String statut = d.getStatut().toString().toLowerCase();
        String debut  = d.getHeureDebut().toString().substring(0, 5);
        String fin    = d.getHeureFin().toString().substring(0, 5);

        String bg, fg;
        switch (statut) {
            case "disponible" -> { bg = "#dcfce7"; fg = "#16a34a"; }
            case "réservé"    -> { bg = "#fff7ed"; fg = "#c2410c"; }
            case "annulé"     -> { bg = "#fee2e2"; fg = "#dc2626"; }
            default           -> { bg = "#f3f4f6"; fg = "#6b7280"; }
        }

        Label chip = new Label(debut + "–" + fin);
        chip.setStyle(
                "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                        "-fx-font-family:'Segoe UI'; -fx-font-size:9px; -fx-font-weight:bold;" +
                        "-fx-padding:2 6; -fx-background-radius:20; -fx-cursor:hand;");
        chip.setMaxWidth(Double.MAX_VALUE);

        String type = d.getTypeConsult().toString();
        String lieu = (d.getLieu() != null && !d.getLieu().isEmpty()) ? d.getLieu() : "En ligne";
        Tooltip.install(chip, new Tooltip(
                debut + " – " + fin + "\n" + type + "\n" + lieu + "\nStatut : " + statut));

        // Clic sur chip = ouvrir formulaire pré-rempli avec les horaires du créneau
        LocalTime hDebut = d.getHeureDebut().toLocalTime();
        LocalTime hFin   = d.getHeureFin().toLocalTime();
        LocalDate jour   = d.getDateDispo().toLocalDate();
        chip.setOnMouseClicked(ev -> {
            ev.consume(); // ne pas propager au parent (cellule)
            ouvrirFormulaireAjout(jour, hDebut, hFin);
        });

        return chip;
    }

    // ── Ouvre le formulaire d'ajout pré-rempli ───────────────────────
    private void ouvrirFormulaireAjout(LocalDate date,
                                       LocalTime heureDebut,
                                       LocalTime heureFin) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/AjoutDisponibiliteModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene      = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(calendrierStage != null
                    ? calendrierStage
                    : gridCalendrier.getScene().getWindow());
            modalStage.setTitle("Nouvelle disponibilité");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            AjoutDisponibiliteController ctrl = loader.getController();
            ctrl.setUserId(utilisateur.getUserId());
            ctrl.setModalStage(modalStage);

            // ── Pré-remplissage ──────────────────────────────────────
            ctrl.setDatePreRemplie(date);
            if (heureDebut != null) ctrl.setHeuresPreRemplies(heureDebut, heureFin);

            modalStage.showAndWait();

            // Recharger les créneaux après ajout éventuel
            chargerCreneaux();
            rafraichir();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Fermer la fenêtre calendrier ─────────────────────────────────
    private void fermer() {
        if (calendrierStage != null) calendrierStage.close();
        else if (gridCalendrier.getScene() != null)
            ((Stage) gridCalendrier.getScene().getWindow()).close();
    }

    private void styleHoverBtn(Button btn) {
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle()
                .replace("rgba(255,255,255,0.15)", "rgba(255,255,255,0.30)")
                .replace("rgba(255,255,255,0.20)", "rgba(255,255,255,0.35)")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle()
                .replace("rgba(255,255,255,0.30)", "rgba(255,255,255,0.15)")
                .replace("rgba(255,255,255,0.35)", "rgba(255,255,255,0.20)")));
    }
}