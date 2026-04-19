package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.example.entities.ConsultationDetail;
import org.example.entities.User;
import org.example.services.ConsultationService;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ConsultationsPsyController implements SidebarPsychologueController.PsyPageController {

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarPsychologueController sidebarPsyController;

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblDate;
    @FXML private Label lblStatut;

    // ── Stat cards ──────────────────────────────────────────────────
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatNotees;
    @FXML private Label lblStatMoyenne;
    @FXML private Label lblStatCeMois;

    // ── Toolbar ─────────────────────────────────────────────────────
    @FXML private TextField fieldRecherche;

    // ── ListView ────────────────────────────────────────────────────
    @FXML private ListView<ConsultationDetail> listViewConsultations;

    // ── Toast overlay ───────────────────────────────────────────────
    @FXML private StackPane toastContainer;

    // ── Données ─────────────────────────────────────────────────────
    private User                               utilisateur;
    private ConsultationService                consultationService;
    private ObservableList<ConsultationDetail> consultationsList;
    private ObservableList<ConsultationDetail> filteredList;

    // ════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        String jour = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(jour.substring(0, 1).toUpperCase() + jour.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

        consultationService = new ConsultationService();
        consultationsList   = FXCollections.observableArrayList();
        filteredList        = FXCollections.observableArrayList();

        configurerListView();
        fieldRecherche.textProperty().addListener((o, ov, nv) -> appliquerFiltres());
    }

    // ── Interface ────────────────────────────────────────────────────
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/ConsultationsPsy.fxml");
        }
        chargerConsultations();
    }

    // ════════════════════════════════════════════════════════════════
    //  LISTVIEW CUSTOM CELL
    // ════════════════════════════════════════════════════════════════
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
                setGraphic(construireCarte(c));
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
            }
        });
    }

    private VBox construireCarte(ConsultationDetail c) {
        VBox carte = new VBox(12);
        carte.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14; " +
                "-fx-border-color: #ede9fe; -fx-border-width: 1; -fx-border-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(109,40,217,0.07), 8, 0, 0, 2); " +
                "-fx-padding: 16 20;");

        // ── Ligne 1 : Patient + Date ──────────────────────────────────
        HBox ligne1 = new HBox(14);
        ligne1.setAlignment(Pos.CENTER_LEFT);

        String initiales = String.valueOf(c.getEtudiantPrenom().charAt(0)).toUpperCase()
                + String.valueOf(c.getEtudiantNom().charAt(0)).toUpperCase();
        Label avatar = new Label(initiales);
        avatar.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-min-width: 44; -fx-min-height: 44; -fx-alignment: CENTER; " +
                "-fx-background-radius: 50; -fx-padding: 8;");

        VBox patientBox = new VBox(3);
        HBox.setHgrow(patientBox, Priority.ALWAYS);
        Label nomPatient = new Label(c.getEtudiantPrenom() + " " + c.getEtudiantNom());
        nomPatient.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-text-fill: #4c1d95;");
        Label emailPatient = new Label(c.getEtudiantEmail());
        emailPatient.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        patientBox.getChildren().addAll(nomPatient, emailPatient);

        VBox dateBox = new VBox(3);
        dateBox.setAlignment(Pos.CENTER_RIGHT);
        LocalDate date  = c.getDateDispo().toLocalDate();
        String jourAbrg = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        Label lblDateRdv = new Label(jourAbrg + " " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblDateRdv.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-text-fill: #7c3aed;");
        String hDebut = c.getHeureDebut().toString().substring(0, 5);
        String hFin   = c.getHeureFin().toString().substring(0, 5);
        Label lblHoraire = new Label("🕐 " + hDebut + " – " + hFin);
        lblHoraire.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        dateBox.getChildren().addAll(lblDateRdv, lblHoraire);

        ligne1.getChildren().addAll(avatar, patientBox, dateBox);

        // ── Séparateur ────────────────────────────────────────────────
        Region sep = new Region();
        sep.setMinHeight(1); sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #f5f3ff;");

        // ── Ligne 2 : Note + Actions ──────────────────────────────────
        HBox ligne2 = new HBox(10);
        ligne2.setAlignment(Pos.CENTER_LEFT);

        int note = c.getNoteSatisfaction();
        Label etoilesLabel;
        if (note > 0 && note <= 5) {
            String etoiles = buildEtoiles(note);
            String couleur = note <= 2 ? "#ef4444" : note <= 4 ? "#d97706" : "#10b981";
            String bgColor = note <= 2 ? "#fee2e2" : note <= 4 ? "#fef3c7" : "#dcfce7";
            etoilesLabel = new Label(etoiles + "  " + note + " / 5");
            etoilesLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + couleur + "; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-padding: 4 14; -fx-background-radius: 20;");
        } else {
            etoilesLabel = new Label("☆☆☆☆☆  Non noté");
            etoilesLabel.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #c4b5fd; " +
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                    "-fx-padding: 4 14; -fx-background-radius: 20;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoir = new Button("👁  Détails");
        btnVoir.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 7 16; -fx-background-radius: 20; -fx-cursor: hand;");
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(btnVoir.getStyle().replace("#ede9fe", "#ddd6fe")));
        btnVoir.setOnMouseExited(e  -> btnVoir.setStyle(btnVoir.getStyle().replace("#ddd6fe", "#ede9fe")));
        btnVoir.setOnAction(e -> ouvrirModalDetails(c));

        Button btnModifier = new Button("✏  Modifier");
        btnModifier.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 7 16; -fx-background-radius: 20; -fx-cursor: hand;");
        btnModifier.setOnMouseEntered(e -> btnModifier.setStyle(btnModifier.getStyle().replace("#fef3c7", "#fde68a")));
        btnModifier.setOnMouseExited(e  -> btnModifier.setStyle(btnModifier.getStyle().replace("#fde68a", "#fef3c7")));
        btnModifier.setOnAction(e -> modifierConsultation(c));

        ligne2.getChildren().addAll(etoilesLabel, spacer, btnVoir, btnModifier);

        // ── Aperçu avis ───────────────────────────────────────────────
        String avisTexte = nettoyerAvis(c.getAvisPsy());
        if (!avisTexte.equals("Aucun avis rédigé.")) {
            String apercu = avisTexte.length() > 90 ? avisTexte.substring(0, 90) + "…" : avisTexte;
            Label lblAvis = new Label("💬 " + apercu);
            lblAvis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                    "-fx-text-fill: #6b7280; -fx-wrap-text: true;");
            lblAvis.setWrapText(true);
            carte.getChildren().addAll(ligne1, sep, ligne2, lblAvis);
        } else {
            carte.getChildren().addAll(ligne1, sep, ligne2);
        }

        return carte;
    }

    // ════════════════════════════════════════════════════════════════
    //  MODAL DÉTAILS
    // ════════════════════════════════════════════════════════════════
    private void ouvrirModalDetails(ConsultationDetail c) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.UNDECORATED);
        modal.setResizable(false);

        VBox root = new VBox(0);
        root.setPrefWidth(500);
        root.setStyle("-fx-background-color: #f8f7ff; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(109,40,217,0.30), 28, 0, 0, 8);");

        // Header
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #4c1d95, #7c3aed); " +
                "-fx-padding: 22 24 18 24; -fx-background-radius: 16 16 0 0;");
        HBox hdrTop = new HBox();
        hdrTop.setAlignment(Pos.CENTER_LEFT);

        LocalDate date = c.getDateDispo().toLocalDate();
        String dateStr = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        dateStr = dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1)
                + " " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));

        VBox hdrInfo = new VBox(3);
        HBox.setHgrow(hdrInfo, Priority.ALWAYS);
        Label hdrTitre = new Label("📋  Consultation — " + c.getEtudiantPrenom() + " " + c.getEtudiantNom());
        hdrTitre.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label hdrDate = new Label(dateStr);
        hdrDate.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.75);");
        hdrInfo.getChildren().addAll(hdrTitre, hdrDate);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: #ffffff; " +
                "-fx-font-size: 13px; -fx-padding: 5 10; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-border-width: 0;");
        btnClose.setOnAction(e -> modal.close());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnClose.getStyle().replace("0.15", "0.28")));
        btnClose.setOnMouseExited(e  -> btnClose.setStyle(btnClose.getStyle().replace("0.28", "0.15")));

        hdrTop.getChildren().addAll(hdrInfo, btnClose);
        header.getChildren().add(hdrTop);

        // Body
        VBox body = new VBox(12);
        body.setStyle("-fx-padding: 20 24 8 24;");

        String hDebut = c.getHeureDebut().toString().substring(0, 5);
        String hFin   = c.getHeureFin().toString().substring(0, 5);

        // Carte patient
        VBox cartePatient = new VBox(10);
        cartePatient.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                "-fx-padding: 14 18; -fx-border-color: #ede9fe; -fx-border-width: 1; -fx-border-radius: 12;");
        HBox patientRow = new HBox(12);
        patientRow.setAlignment(Pos.CENTER_LEFT);
        String initiales = String.valueOf(c.getEtudiantPrenom().charAt(0)).toUpperCase()
                + String.valueOf(c.getEtudiantNom().charAt(0)).toUpperCase();
        Label avt = new Label(initiales);
        avt.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-min-width: 42; -fx-min-height: 42; -fx-alignment: CENTER; " +
                "-fx-background-radius: 50; -fx-padding: 8;");
        VBox patInfo = new VBox(2);
        HBox.setHgrow(patInfo, Priority.ALWAYS);
        Label patNom = new Label(c.getEtudiantPrenom() + " " + c.getEtudiantNom());
        patNom.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4c1d95;");
        Label patEmail = new Label(c.getEtudiantEmail());
        patEmail.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        patInfo.getChildren().addAll(patNom, patEmail);
        patientRow.getChildren().addAll(avt, patInfo);
        cartePatient.getChildren().addAll(patientRow, separateurFin(),
                ligneDetail("⏰  Horaire", hDebut + " – " + hFin),
                ligneDetail("📅  Rédigé le", formaterDate(c.getDateRedaction())));

        // Carte note
        VBox carteNote = new VBox(8);
        carteNote.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 12; " +
                "-fx-padding: 14 18; -fx-border-color: #fde68a; -fx-border-width: 1; -fx-border-radius: 12;");
        Label titreNote = new Label("⭐  Satisfaction du patient");
        titreNote.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #92400e;");
        int note = c.getNoteSatisfaction();
        HBox noteRow = new HBox(12);
        noteRow.setAlignment(Pos.CENTER_LEFT);
        if (note > 0 && note <= 5) {
            Label etoilesLbl = new Label(buildEtoiles(note));
            etoilesLbl.setStyle("-fx-font-size: 24px; -fx-text-fill: #f59e0b;");
            String couleur = note <= 2 ? "#ef4444" : note <= 4 ? "#d97706" : "#10b981";
            Label noteChiffre = new Label(note + " / 5");
            noteChiffre.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 22px; " +
                    "-fx-font-weight: bold; -fx-text-fill: " + couleur + ";");
            noteRow.getChildren().addAll(etoilesLbl, noteChiffre);
        } else {
            Label pasNote = new Label("☆☆☆☆☆   Non noté par le patient");
            pasNote.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #c4b5fd;");
            noteRow.getChildren().add(pasNote);
        }
        carteNote.getChildren().addAll(titreNote, noteRow);

        // Carte avis
        VBox carteAvis = new VBox(8);
        carteAvis.setStyle("-fx-background-color: #f5f3ff; -fx-background-radius: 12; " +
                "-fx-padding: 14 18; -fx-border-color: #ddd6fe; -fx-border-width: 1; -fx-border-radius: 12;");
        Label titreAvis = new Label("💬  Mon avis sur cette consultation");
        titreAvis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");
        String avisTexte = nettoyerAvis(c.getAvisPsy());
        Label avisLbl = new Label(avisTexte);
        avisLbl.setWrapText(true);
        avisLbl.setMaxWidth(440);
        avisLbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                "-fx-text-fill: " + (avisTexte.equals("Aucun avis rédigé.") ? "#c4b5fd" : "#374151") + "; " +
                "-fx-line-spacing: 3;");
        carteAvis.getChildren().addAll(titreAvis, avisLbl);

        body.getChildren().addAll(cartePatient, carteNote, carteAvis);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 14 24 18 24;");

        Button btnModifier = new Button("✏  Modifier l'avis");
        btnModifier.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> { modal.close(); modifierConsultation(c); });

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: #ffffff; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-padding: 10 28; -fx-background-radius: 10; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.35), 8, 0, 0, 2);");
        btnFermer.setOnAction(e -> modal.close());
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(btnFermer.getStyle().replace("#7c3aed", "#6d28d9")));
        btnFermer.setOnMouseExited(e  -> btnFermer.setStyle(btnFermer.getStyle().replace("#6d28d9", "#7c3aed")));

        footer.getChildren().addAll(btnModifier, btnFermer);
        root.getChildren().addAll(header, body, footer);

        Scene scene = new Scene(root);
        scene.setFill(null);
        modal.setScene(scene);
        modal.showAndWait();
    }

    // ════════════════════════════════════════════════════════════════
    //  MODIFIER — ouvre le modal et affiche un toast après fermeture
    // ════════════════════════════════════════════════════════════════
    private void modifierConsultation(ConsultationDetail c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierConsultationModal.fxml"));
            Stage modalStage  = new Stage();
            Scene scene       = new Scene(loader.load());

            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(listViewConsultations.getScene().getWindow());
            modalStage.setTitle("Modifier l'avis et la note");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            ModifierConsultationModalController controller = loader.getController();
            controller.setConsultation(c);
            controller.setUtilisateur(utilisateur);
            controller.setModalStage(modalStage);

            // Callback appelé par le modal quand l'enregistrement réussit
            controller.setOnSucces(() -> {
                chargerConsultations();
                showToast("✓  Avis et note enregistrés avec succès !", true);
            });

            modalStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showToast("✗  Impossible d'ouvrir le formulaire de modification.", false);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  TOAST
    // ════════════════════════════════════════════════════════════════
    private void showToast(String message, boolean success) {
        Label pill = new Label(message);
        pill.setWrapText(true);
        pill.setMaxWidth(500);
        pill.setStyle(
                "-fx-background-color:" + (success ? "#10b981" : "#ef4444") + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-padding:12 22;-fx-background-radius:30;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.20),12,0,0,4);");

        toastContainer.getChildren().add(pill);
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        StackPane.setAlignment(pill, Pos.BOTTOM_CENTER);

        FadeTransition fadeIn  = new FadeTransition(Duration.millis(200), pill);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), pill);
        fadeOut.setDelay(Duration.seconds(2.5));
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            toastContainer.getChildren().remove(pill);
            if (toastContainer.getChildren().isEmpty()) {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            }
        });

        fadeIn.play();
        fadeOut.play();
    }

    // ════════════════════════════════════════════════════════════════
    //  CHARGEMENT
    // ════════════════════════════════════════════════════════════════
    private void chargerConsultations() {
        if (utilisateur == null) {
            showToast("✗  Utilisateur non connecté.", false);
            return;
        }
        try {
            lblStatut.setText("Chargement…");
            List<ConsultationDetail> liste =
                    consultationService.getConsultationsDetailByPsy(utilisateur.getUserId());
            consultationsList.setAll(liste);
            majStatistiques(liste);
            appliquerFiltres();
        } catch (SQLException e) {
            showToast("✗  Erreur de chargement : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private void appliquerFiltres() {
        if (consultationsList == null) return;
        String recherche = fieldRecherche.getText() == null ? ""
                : fieldRecherche.getText().toLowerCase().trim();
        List<ConsultationDetail> filtered = consultationsList.stream()
                .filter(c -> {
                    if (recherche.isEmpty()) return true;
                    String info = (c.getEtudiantPrenom() + " " + c.getEtudiantNom()
                            + " " + c.getEtudiantEmail()).toLowerCase();
                    return info.contains(recherche);
                })
                .collect(Collectors.toList());
        filteredList.setAll(filtered);
        listViewConsultations.setItems(filteredList);
        lblStatut.setText(filteredList.size() + " consultation(s) affichée(s) sur "
                + consultationsList.size());
    }

    private void majStatistiques(List<ConsultationDetail> liste) {
        int    total  = liste.size();
        long   notees = liste.stream().filter(c -> c.getNoteSatisfaction() > 0).count();
        double moy    = liste.stream().filter(c -> c.getNoteSatisfaction() > 0)
                .mapToInt(ConsultationDetail::getNoteSatisfaction).average().orElse(0);
        LocalDate now   = LocalDate.now();
        long   ceMois = liste.stream().filter(c ->
                c.getDateDispo().toLocalDate().getYear()  == now.getYear() &&
                        c.getDateDispo().toLocalDate().getMonth() == now.getMonth()).count();

        lblStatTotal.setText(String.valueOf(total));
        lblStatNotees.setText(String.valueOf(notees));
        lblStatMoyenne.setText(moy > 0 ? String.format("%.1f", moy) : "—");
        lblStatCeMois.setText(String.valueOf(ceMois));
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private String nettoyerAvis(String avis) {
        if (avis == null || avis.trim().isEmpty()) return "Aucun avis rédigé.";
        String a = avis.trim();
        for (String p : new String[]{"Consultation terminée le","Consultation crée le",
                "Consultation créée le","Consultation créé le"}) {
            if (a.toLowerCase().startsWith(p.toLowerCase())) {
                int nl = a.indexOf('\n');
                a = (nl > 0) ? a.substring(nl + 1).trim() : "";
                break;
            }
        }
        a = a.replaceFirst("(?i)Consultation (terminée|crée|créée|créé) le\\s*\\d{4}-\\d{2}-\\d{2}[\\s\\d:.]*", "").trim();
        return a.isEmpty() ? "Aucun avis rédigé." : a;
    }

    private String buildEtoiles(int note) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < note ? "★" : "☆");
        return sb.toString();
    }

    private String formaterDate(Timestamp ts) {
        if (ts == null) return "—";
        return ts.toLocalDateTime().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm", Locale.FRENCH));
    }

    private HBox ligneDetail(String label, String valeur) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setMinWidth(120);
        lbl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-text-fill:#9ca3af;");
        Label val = new Label(valeur);
        val.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-text-fill:#374151;");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private Region separateurFin() {
        Region r = new Region();
        r.setMinHeight(1); r.setMaxHeight(1);
        r.setStyle("-fx-background-color: #f5f3ff;");
        return r;
    }
}