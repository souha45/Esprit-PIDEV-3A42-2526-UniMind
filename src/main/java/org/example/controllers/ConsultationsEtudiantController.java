package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.entities.ConsultationDetail;
import org.example.entities.User;
import org.example.services.ConsultationService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class ConsultationsEtudiantController
        implements SidebarEtudiantController.EtudiantPageController {

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
    @FXML private Button    btnRafraichir;
    @FXML private Label     lblStatut;

    // ── ListView ─────────────────────────────────────────────────────
    @FXML private ListView<ConsultationDetail> listViewConsultations;

    // ── Données ─────────────────────────────────────────────────────
    private User                                utilisateur;
    private ConsultationService                 consultationService;
    private ObservableList<ConsultationDetail>  consultationsList;
    private FilteredList<ConsultationDetail>    filteredList;

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        String jour = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(jour.substring(0,1).toUpperCase() + jour.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

        consultationService = new ConsultationService();
        consultationsList   = FXCollections.observableArrayList();
        filteredList        = new FilteredList<>(consultationsList, p -> true);

        configurerListView();

        listViewConsultations.setItems(filteredList);
        listViewConsultations.setFixedCellSize(-1); // hauteur auto

        fieldRecherche.textProperty().addListener((o, ov, nv) -> appliquerFiltres());
        btnRafraichir.setOnAction(e -> chargerConsultations());
    }

    // ── ListView custom cell ─────────────────────────────────────────
    private void configurerListView() {
        listViewConsultations.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ConsultationDetail c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                setGraphic(construireCarteConsultation(c));
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
            }
        });
        // Supprimer le focus ring
        listViewConsultations.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 12;");
    }

    private HBox construireCarteConsultation(ConsultationDetail c) {
        // ── Conteneur principal ──────────────────────────────────────
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 12; -fx-padding: 14 18;");

        // ── Bloc date (gauche) ───────────────────────────────────────
        LocalDate date   = c.getDateDispo().toLocalDate();
        String jourAbrg  = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        String moisAbrg  = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase();

        VBox dateBox = new VBox(0);
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setMinWidth(52);
        dateBox.setStyle("-fx-background-color: #6366f1; -fx-background-radius: 10; -fx-padding: 8 10;");
        Label lblJour = new Label(jourAbrg.toUpperCase());
        lblJour.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 9px; -fx-text-fill: rgba(255,255,255,0.80);");
        Label lblNumJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblNumJour.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label lblMois = new Label(moisAbrg);
        lblMois.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 9px; -fx-text-fill: rgba(255,255,255,0.80);");
        dateBox.getChildren().addAll(lblJour, lblNumJour, lblMois);

        // ── Bloc infos centre ────────────────────────────────────────
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Ligne 1 : psy + horaire
        HBox topLine = new HBox(10);
        topLine.setAlignment(Pos.CENTER_LEFT);
        Label lblPsy = new Label("Dr. " + c.getPsyPrenom() + " " + c.getPsyNom());
        lblPsy.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
        String hDebut = c.getHeureDebut().toString().substring(0, 5);
        String hFin   = c.getHeureFin().toString().substring(0, 5);
        Label lblHoraire = new Label("🕐 " + hDebut + " – " + hFin);
        lblHoraire.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
        topLine.getChildren().addAll(lblPsy, lblHoraire);

        // Ligne 2 : avis psy (nettoyé)
        String avisTexte = nettoyerAvis(c.getAvisPsy());
        Label lblAvis = new Label(avisTexte);
        lblAvis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #4b5563; -fx-wrap-text: true;");
        lblAvis.setWrapText(true);
        lblAvis.setMaxWidth(380);

        infoBox.getChildren().addAll(topLine, lblAvis);

        // ── Bloc droite : note + bouton ──────────────────────────────
        VBox rightBox = new VBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMinWidth(110);

        // Note étoiles
        int note = c.getNoteSatisfaction();
        Label lblNote;
        if (note > 0 && note <= 5) {
            String etoiles = buildEtoiles(note);
            lblNote = new Label(etoiles + "  " + note + "/5");
            String couleur = note <= 2 ? "#ef4444" : note <= 4 ? "#f59e0b" : "#10b981";
            lblNote.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                    "-fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
        } else {
            lblNote = new Label("☆☆☆☆☆  —");
            lblNote.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #c4b5fd;");
        }

        // Bouton voir détails
        Button btnVoir = new Button("🔍  Détails");
        btnVoir.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 6 14; -fx-background-radius: 20; -fx-cursor: hand;");
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(btnVoir.getStyle().replace("#ede9fe","#ddd6fe")));
        btnVoir.setOnMouseExited(e  -> btnVoir.setStyle(btnVoir.getStyle().replace("#ddd6fe","#ede9fe")));
        btnVoir.setOnAction(e -> ouvrirModalDetails(c));

        rightBox.getChildren().addAll(lblNote, btnVoir);

        card.getChildren().addAll(dateBox, infoBox, rightBox);
        return card;
    }

    // ── Modal détails moderne ────────────────────────────────────────
    private void ouvrirModalDetails(ConsultationDetail c) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.UNDECORATED);
        modal.setResizable(false);

        // ── Root ────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setPrefWidth(480);
        root.setStyle("-fx-background-color: #f0f4ff; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.25), 24, 0, 0, 6);");

        // ── Header gradient ──────────────────────────────────────────
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6); " +
                "-fx-padding: 22 24 18 24; -fx-background-radius: 16 16 0 0;");
        HBox hdrTop = new HBox();
        hdrTop.setAlignment(Pos.CENTER_LEFT);

        LocalDate date   = c.getDateDispo().toLocalDate();
        String dateStr   = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        dateStr = dateStr.substring(0,1).toUpperCase() + dateStr.substring(1)
                + " " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));

        VBox hdrInfo = new VBox(3);
        HBox.setHgrow(hdrInfo, Priority.ALWAYS);
        Label hdrTitre = new Label("📋  Détails de la consultation");
        hdrTitre.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label hdrDate = new Label(dateStr);
        hdrDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.78);");
        hdrInfo.getChildren().addAll(hdrTitre, hdrDate);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: #ffffff; " +
                "-fx-font-size: 13px; -fx-padding: 5 10; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-border-width: 0;");
        btnClose.setOnAction(e -> modal.close());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnClose.getStyle().replace("0.18","0.30")));
        btnClose.setOnMouseExited(e  -> btnClose.setStyle(btnClose.getStyle().replace("0.30","0.18")));

        hdrTop.getChildren().addAll(hdrInfo, btnClose);
        header.getChildren().add(hdrTop);

        // ── Body ────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setStyle("-fx-padding: 20 24 8 24;");

        String hDebut = c.getHeureDebut().toString().substring(0, 5);
        String hFin   = c.getHeureFin().toString().substring(0, 5);

        // Carte infos principales
        VBox carteInfos = new VBox(10);
        carteInfos.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                "-fx-padding: 16 18; -fx-border-color: #e0e7ff; -fx-border-width: 1; -fx-border-radius: 12;");

        carteInfos.getChildren().add(ligneInfo("👨‍⚕️  Psychologue", "Dr. " + c.getPsyPrenom() + " " + c.getPsyNom(), "#3730a3", true));
        carteInfos.getChildren().add(separateurLeger());
        carteInfos.getChildren().add(ligneInfo("⏰  Horaire", hDebut + " – " + hFin, "#374151", false));
        carteInfos.getChildren().add(separateurLeger());
        carteInfos.getChildren().add(ligneInfo("📅  Créé le", formaterDateRedaction(c.getDateRedaction()), "#374151", false));

        // Carte note
        int note = c.getNoteSatisfaction();
        VBox carteNote = new VBox(8);
        carteNote.setStyle("-fx-background-color: #fefce8; -fx-background-radius: 12; " +
                "-fx-padding: 14 18; -fx-border-color: #fde68a; -fx-border-width: 1; -fx-border-radius: 12;");
        Label titreNote = new Label("⭐  Votre satisfaction");
        titreNote.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ca8a04;");

        HBox noteRow = new HBox(10);
        noteRow.setAlignment(Pos.CENTER_LEFT);
        if (note > 0 && note <= 5) {
            Label etoilesLabel = new Label(buildEtoiles(note));
            etoilesLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #f59e0b;");
            String couleur = note <= 2 ? "#ef4444" : note <= 4 ? "#f59e0b" : "#10b981";
            Label noteChiffre = new Label(note + " / 5");
            noteChiffre.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
            noteRow.getChildren().addAll(etoilesLabel, noteChiffre);
        } else {
            Label pasNote = new Label("☆☆☆☆☆   Non noté");
            pasNote.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: #c4b5fd;");
            noteRow.getChildren().add(pasNote);
        }
        carteNote.getChildren().addAll(titreNote, noteRow);

        // Carte avis psy
        VBox carteAvis = new VBox(8);
        carteAvis.setStyle("-fx-background-color: #f5f3ff; -fx-background-radius: 12; " +
                "-fx-padding: 14 18; -fx-border-color: #e0e7ff; -fx-border-width: 1; -fx-border-radius: 12;");
        Label titreAvis = new Label("💬  Avis du psychologue");
        titreAvis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

        String avisTexte = nettoyerAvis(c.getAvisPsy());
        Label avisContent = new Label(avisTexte);
        avisContent.setWrapText(true);
        avisContent.setMaxWidth(420);
        avisContent.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #374151; -fx-line-spacing: 3;");
        carteAvis.getChildren().addAll(titreAvis, avisContent);

        body.getChildren().addAll(carteInfos, carteNote, carteAvis);

        // ── Footer ──────────────────────────────────────────────────
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 14 24 18 24;");
        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #6366f1; -fx-text-fill: #ffffff; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-padding: 10 28; -fx-background-radius: 10; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.30), 6, 0, 0, 2);");
        btnFermer.setOnAction(e -> modal.close());
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(btnFermer.getStyle().replace("#6366f1","#4f46e5")));
        btnFermer.setOnMouseExited(e  -> btnFermer.setStyle(btnFermer.getStyle().replace("#4f46e5","#6366f1")));
        footer.getChildren().add(btnFermer);

        root.getChildren().addAll(header, body, footer);

        Scene scene = new Scene(root);
        scene.setFill(null); // transparent pour les coins arrondis
        modal.setScene(scene);
        modal.showAndWait();
    }

    // ── Ligne info dans le modal ─────────────────────────────────────
    private HBox ligneInfo(String label, String valeur, String couleurVal, boolean gras) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setMinWidth(130);
        lbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        Label val = new Label(valeur);
        val.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                "-fx-text-fill: " + couleurVal + ";" +
                (gras ? " -fx-font-weight: bold;" : ""));
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private Region separateurLeger() {
        Region sep = new Region();
        sep.setMinHeight(1); sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #f3f4f6;");
        return sep;
    }

    // ── Nettoyage avis ───────────────────────────────────────────────
    /**
     * Supprime le préfixe automatique "Consultation terminée le..." ou
     * "Consultation crée le..." ajouté lors de la création automatique,
     * et retourne uniquement la partie utile de l'avis.
     * Si l'avis est vide ou null, retourne un message par défaut.
     */
    private String nettoyerAvis(String avis) {
        if (avis == null || avis.trim().isEmpty()) {
            return "Aucun avis rédigé par le psychologue.";
        }
        String a = avis.trim();

        // Supprimer les préfixes automatiques connus
        String[] prefixes = {
                "Consultation terminée le",
                "Consultation crée le",
                "Consultation créée le",
                "Consultation créé le"
        };
        for (String prefix : prefixes) {
            if (a.toLowerCase().startsWith(prefix.toLowerCase())) {
                // On cherche la fin de la date (après un espace et la timestamp)
                // Format : "Consultation terminée le 2026-04-05 16:36:24.911"
                // On retire tout jusqu'à la fin de la ligne de date
                int newline = a.indexOf('\n');
                if (newline > 0) {
                    a = a.substring(newline + 1).trim();
                } else {
                    // Toute la chaîne était le préfixe auto, pas d'avis réel
                    return "Aucun avis rédigé par le psychologue.";
                }
                break;
            }
        }

        return a.isEmpty() ? "Aucun avis rédigé par le psychologue." : a;
    }

    // ── Formatage date de rédaction ──────────────────────────────────
    private String formaterDateRedaction(Timestamp ts) {
        if (ts == null) return "—";
        return ts.toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm", Locale.FRENCH));
    }

    // ── Étoiles ──────────────────────────────────────────────────────
    private String buildEtoiles(int note) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < note ? "★" : "☆");
        return sb.toString();
    }

    // ── Filtres ──────────────────────────────────────────────────────
    private void appliquerFiltres() {
        String recherche = fieldRecherche.getText() == null ? ""
                : fieldRecherche.getText().toLowerCase().trim();

        filteredList.setPredicate(c -> {
            boolean matchPsy  = ("Dr. " + c.getPsyPrenom() + " " + c.getPsyNom()).toLowerCase().contains(recherche);
            boolean matchDate = c.getDateDispo().toString().contains(recherche);
            return recherche.isEmpty() || matchPsy || matchDate;
        });

        int nb = filteredList.size();
        lblStatut.setText(nb == 0 ? "Aucune consultation trouvée."
                : nb + " consultation(s) affichée(s) sur " + consultationsList.size());
    }

    // ── Chargement ───────────────────────────────────────────────────
    private void chargerConsultations() {
        if (utilisateur == null) { lblStatut.setText("Erreur : utilisateur non connecté"); return; }
        try {
            lblStatut.setText("Chargement…");
            List<ConsultationDetail> liste =
                    consultationService.getConsultationsDetailByEtudiant(utilisateur.getUserId());
            consultationsList.setAll(liste);
            mettreAJourStatCards(liste);
            appliquerFiltres();
        } catch (SQLException e) {
            lblStatut.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mettreAJourStatCards(List<ConsultationDetail> liste) {
        int total     = liste.size();
        long notees   = liste.stream().filter(c -> c.getNoteSatisfaction() > 0).count();
        double moy    = liste.stream().filter(c -> c.getNoteSatisfaction() > 0)
                .mapToInt(ConsultationDetail::getNoteSatisfaction)
                .average().orElse(0);
        LocalDate now = LocalDate.now();
        long ceMois   = liste.stream().filter(c ->
                c.getDateDispo().toLocalDate().getYear()  == now.getYear() &&
                        c.getDateDispo().toLocalDate().getMonth() == now.getMonth()).count();

        lblStatTotal.setText(String.valueOf(total));
        lblStatNotees.setText(String.valueOf(notees));
        lblStatMoyenne.setText(moy > 0 ? String.format("%.1f", moy) : "—");
        lblStatCeMois.setText(String.valueOf(ceMois));
    }

    // ── Interface ────────────────────────────────────────────────────
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