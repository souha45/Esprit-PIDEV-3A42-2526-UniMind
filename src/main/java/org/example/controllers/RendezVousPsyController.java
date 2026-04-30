package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.entities.RendezVousDetail;
import org.example.entities.User;
import org.example.entities.Etudiant;
import org.example.services.RendezVousService;
import org.example.services.EmailGhofraneService;
import org.example.services.EtudiantService;
import org.example.utils.MyDataBase_Unimind;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
    @FXML private TableColumn<RendezVousDetail, Void>   colActions;
    @FXML private Label       lblStatut;
    @FXML private Label       lblDate;
    @FXML private ComboBox<String> comboFiltreStatut;
    @FXML private ComboBox<String> comboFiltreType;
    @FXML private TextField   fieldRecherche;
    @FXML private TableColumn<RendezVousDetail, Void> colVisio;

    // Statistiques
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatDemande;
    @FXML private Label lblStatConfirme;
    @FXML private Label lblStatEnCours;

    // ── Toast container (injecté depuis le FXML)
    @FXML private StackPane toastContainer;

    // ========== SIDEBAR ==========
    @FXML private SidebarPsychologueController sidebarPsyController;

    // ========== SERVICES ==========
    private RendezVousService rendezVousService;
    private EmailGhofraneService emailService;
    private EtudiantService etudiantService;  // ← CHANGÉ : Utiliser EtudiantService
    private ObservableList<RendezVousDetail> rendezVousList;
    private ObservableList<RendezVousDetail> filteredList;
    private User utilisateur;

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        rendezVousService = new RendezVousService();
        emailService = new EmailGhofraneService();
        etudiantService = new EtudiantService();  // ← CHANGÉ
        rendezVousList    = FXCollections.observableArrayList();
        filteredList      = FXCollections.observableArrayList();

        configurerColonnes();
        configurerColonneVisio();
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

    // ══════════════════════════════════════════════════════════════
    //  FILTRES
    // ══════════════════════════════════════════════════════════════
    private void configurerFiltres() {
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "demande", "confirme", "en-cours", "terminé", "annulé", "absent"));
        comboFiltreStatut.setValue("Tous");

        comboFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "présentiel", "en_ligne"));
        comboFiltreType.setValue("Tous");

        comboFiltreStatut.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
        comboFiltreType.valueProperty().addListener((obs, o, n)   -> appliquerFiltres());
        fieldRecherche.textProperty().addListener((obs, o, n)     -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        if (rendezVousList == null) return;
        String statutFiltre = comboFiltreStatut.getValue();
        String typeFiltre   = comboFiltreType.getValue();
        String recherche    = fieldRecherche.getText().toLowerCase();

        List<RendezVousDetail> filtered = rendezVousList.stream()
                .filter(rdv -> {
                    if (statutFiltre != null && !statutFiltre.equals("Tous")
                            && !rdv.getStatut().equalsIgnoreCase(statutFiltre)) return false;
                    if (typeFiltre != null && !typeFiltre.equals("Tous")
                            && !rdv.getTypeConsult().equalsIgnoreCase(typeFiltre)) return false;
                    if (!recherche.isEmpty()) {
                        String info = (rdv.getEtudiantPrenom() + " " + rdv.getEtudiantNom()
                                + " " + rdv.getEtudiantEmail()).toLowerCase();
                        if (!info.contains(recherche)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredList.clear();
        filteredList.addAll(filtered);
        tableViewRendezVous.setItems(filteredList);
        lblStatut.setText(filteredList.size() + " rendez-vous affiché(s) sur " + rendezVousList.size());
    }

    // ══════════════════════════════════════════════════════════════
    //  COLONNES
    // ══════════════════════════════════════════════════════════════
    private void configurerColonnes() {
        colEtudiant.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEtudiantPrenom() + " " + c.getValue().getEtudiantNom()
                        + "\n" + c.getValue().getEtudiantEmail()));
        colEtudiant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;" +
                        "-fx-text-fill:#374151;-fx-padding:10 16;-fx-wrap-text:true;");
            }
        });

        colDateHeure.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateDispo().toString() + "\n"
                        + c.getValue().getHeureDebut() + " – " + c.getValue().getHeureFin()));

        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeConsult()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                boolean p = "présentiel".equalsIgnoreCase(item);
                Label badge = new Label(p ? "🏢 Présentiel" : "💻 En ligne");
                badge.setStyle("-fx-background-color:" + (p ? "#dbeafe" : "#ede9fe") + ";" +
                        "-fx-text-fill:" + (p ? "#1d4ed8" : "#6366f1") + ";" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:4 12;-fx-background-radius:20;");
                setGraphic(badge); setText(null);
            }
        });

        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String[] s = statutStyle(item);
                Label badge = new Label(s[2]);
                badge.setStyle("-fx-background-color:" + s[0] + ";-fx-text-fill:" + s[1] + ";" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:4 12;-fx-background-radius:20;");
                setGraphic(badge); setText(null);
            }
        });

        ajouterBoutonsAction();

//        // ✅ AJOUTER LA COLONNE VISIO À LA TABLE
//        // La colonne doit être ajoutée avant colActions dans l'ordre d'affichage
//        configurerColonneVisio();
//        tableViewRendezVous.getColumns().add(colVisio);
    }

    private String[] statutStyle(String statut) {
        return switch (statut.toLowerCase()) {
            case "demande"  -> new String[]{"#fef3c7", "#f59e0b", "🕐 Demande"};
            case "confirme" -> new String[]{"#ecfdf5", "#10b981", "✓ Confirmé"};
            case "en-cours" -> new String[]{"#e0f2fe", "#0ea5e9", "⏳ En cours"};
            case "terminé"  -> new String[]{"#eef2ff", "#6366f1", "✅ Terminé"};
            case "annulé"   -> new String[]{"#fee2e2", "#ef4444", "✗ Annulé"};
            case "absent"   -> new String[]{"#f3f4f6", "#6b7280", "⚠ Absent"};
            default         -> new String[]{"#f3f4f6", "#6b7280", statut};
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  BOUTONS ACTION
    // ══════════════════════════════════════════════════════════════
    private void ajouterBoutonsAction() {
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                String statut = rdv.getStatut().toLowerCase();
                HBox buttons = new HBox(5);
                buttons.setAlignment(Pos.CENTER_LEFT);

                switch (statut) {
                    case "demande" -> {
                        Button btnC = mkBtn("✓ Confirmer", "#10b981");
                        Button btnR = mkBtn("✗ Refuser",   "#ef4444");
                        Button btnD = mkBtn("👁 Détails",  "#6b7280");
                        btnC.setOnAction(e -> changerStatut(rdv, "confirme"));
                        btnR.setOnAction(e -> changerStatut(rdv, "annulé"));
                        btnD.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnC, btnR, btnD);
                    }
                    case "confirme" -> {
                        Button btnE = mkBtn("⏳ En cours", "#0ea5e9");
                        Button btnA = mkBtn("⚠ Absent",   "#f97316");
                        Button btnD = mkBtn("👁 Détails",  "#6b7280");
                        btnE.setOnAction(e -> changerStatut(rdv, "en-cours"));
                        btnA.setOnAction(e -> changerStatut(rdv, "absent"));
                        btnD.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnE, btnA, btnD);
                    }
                    case "en-cours" -> {
                        Button btnT = mkBtn("✅ Terminer", "#6366f1");
                        Button btnD = mkBtn("👁 Détails",  "#6b7280");
                        btnT.setOnAction(e -> changerStatut(rdv, "terminé"));
                        btnD.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        buttons.getChildren().addAll(btnT, btnD);
                    }
                    default -> {
                        Button btnD = mkBtn("👁 Détails", "#6b7280");
                        btnD.setOnAction(e -> afficherDetailsRendezVous(rdv));
                        Label done = new Label("Traité");
                        done.setStyle("-fx-text-fill:#9ca3af;-fx-font-style:italic;-fx-font-size:11px;");
                        buttons.getChildren().addAll(btnD, done);
                    }
                }
                setGraphic(buttons);
            }
        });
    }

    private Button mkBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-padding:5 10;-fx-background-radius:8;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════
    //  CHANGER STATUT — avec envoi d'email à l'étudiant
    // ══════════════════════════════════════════════════════════════
    private void changerStatut(RendezVousDetail rdv, String nouveauStatut) {
        try {
            rendezVousService.modifierStatutRendezVous(
                    rdv.getRendezVousId(),
                    rdv.getEtudiantId(),
                    utilisateur.getUserId(),
                    nouveauStatut);

            // ✅ Envoyer un email à l'étudiant si le statut est "confirme"
            if ("confirme".equals(nouveauStatut)) {
                envoyerEmailConfirmationEtudiant(rdv);
            }

            chargerRendezVous();
            showToast("✓  Rendez-vous " + getMessageStatut(nouveauStatut), true);
        } catch (SQLException e) {
            showToast("✗  Impossible de modifier le statut : " + e.getMessage(), false);
        }
    }

    /**
     * Envoie un email de confirmation à l'étudiant
     */
    private void envoyerEmailConfirmationEtudiant(RendezVousDetail rdv) {
        // Récupérer les informations complètes de l'étudiant
        Etudiant etudiant = etudiantService.getEtudiantById(rdv.getEtudiantId());

        if (etudiant != null && etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
            // Récupérer les informations du psychologue
            String psyNom = utilisateur.getNom();
            String psyPrenom = utilisateur.getPrenom();

            // Formater la date
            String date = rdv.getDateDispo().toString();
            String heureDebut = rdv.getHeureDebut().toString().substring(0, 5);
            String heureFin = rdv.getHeureFin().toString().substring(0, 5);

            emailService.envoyerEmailConfirmationEtudiant(
                    etudiant.getEmail(),           // Email étudiant
                    etudiant.getNom(),             // Nom étudiant
                    etudiant.getPrenom(),          // Prénom étudiant
                    date,                          // Date
                    heureDebut,                    // Heure début
                    heureFin,                      // Heure fin
                    rdv.getTypeConsult(),          // Type consultation
                    "",                            // Lieu (à récupérer si besoin)
                    rdv.getMotif() != null ? rdv.getMotif() : "Consultation psychologique",
                    psyNom,                        // Nom du psy
                    psyPrenom                      // Prénom du psy
            );
            System.out.println("✅ Email de confirmation envoyé à l'étudiant: " + etudiant.getEmail());
        } else {
            System.err.println("❌ Impossible d'envoyer l'email: étudiant non trouvé ou email invalide");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DÉTAILS — panel inline
    // ══════════════════════════════════════════════════════════════
    private void afficherDetailsRendezVous(RendezVousDetail rdv) {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color:#ffffff;-fx-background-radius:14;" +
                "-fx-border-color:#e0e7ff;-fx-border-width:1;-fx-border-radius:14;" +
                "-fx-padding:20 24;-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.15),16,0,0,4);" +
                "-fx-max-width:420;");
        panel.setMaxWidth(420);
        panel.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label("📋  Détails du rendez-vous");
        titre.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:15px;-fx-font-weight:bold;" +
                "-fx-text-fill:#3730a3;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color:transparent;-fx-text-fill:#9ca3af;" +
                "-fx-font-size:14px;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:0 4;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnClose.getStyle()
                .replace("#9ca3af", "#ef4444")));
        btnClose.setOnMouseExited(e  -> btnClose.setStyle(btnClose.getStyle()
                .replace("#ef4444", "#9ca3af")));
        header.getChildren().addAll(titre, spacer, btnClose);

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color:#e0e7ff;");

        VBox body = new VBox(8);
        body.getChildren().addAll(
                detailRow("👤  Patient",  rdv.getEtudiantPrenom() + " " + rdv.getEtudiantNom()),
                detailRow("📧  Email",    rdv.getEtudiantEmail()),
                detailRow("📅  Date",     rdv.getDateDispo().toString()),
                detailRow("🕐  Horaire",  rdv.getHeureDebut() + " – " + rdv.getHeureFin()),
                detailRow("💬  Type",     rdv.getTypeConsult()),
                detailRow("📌  Statut",   rdv.getStatut()),
                detailRow("📝  Motif",
                        (rdv.getMotif() != null && !rdv.getMotif().trim().isEmpty())
                                ? rdv.getMotif() : "Non spécifié")
        );

        panel.getChildren().addAll(header, sep, body);

        toastContainer.getChildren().add(panel);
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        StackPane.setAlignment(panel, Pos.CENTER);

        btnClose.setOnAction(e -> fermerOverlay(panel));
        toastContainer.setOnMouseClicked(e -> {
            if (e.getTarget() == toastContainer) fermerOverlay(panel);
        });

        FadeTransition ft = new FadeTransition(Duration.millis(180), panel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private HBox detailRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-text-fill:#374151;-fx-min-width:90;");
        Label val = new Label(value);
        val.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-text-fill:#6b7280;");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private void fermerOverlay(VBox panel) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), panel);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            toastContainer.getChildren().remove(panel);
            if (toastContainer.getChildren().isEmpty()) {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            }
        });
        ft.play();
    }

    // ══════════════════════════════════════════════════════════════
    //  TOAST NOTIFICATION
    // ══════════════════════════════════════════════════════════════
    private void showToast(String message, boolean success) {
        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color:" + (success ? "#10b981" : "#ef4444") + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-padding:12 22;-fx-background-radius:30;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),12,0,0,4);");

        toastContainer.getChildren().add(toast);
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.2));
        fadeOut.setOnFinished(e -> {
            toastContainer.getChildren().remove(toast);
            if (toastContainer.getChildren().isEmpty()) {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            }
        });

        fadeIn.play();
        fadeOut.play();
    }

    // ══════════════════════════════════════════════════════════════
    //  CHARGEMENT
    // ══════════════════════════════════════════════════════════════
    private void chargerRendezVous() {
        if (utilisateur == null) { lblStatut.setText("Erreur: utilisateur non connecté"); return; }
        try {
            lblStatut.setText("Chargement en cours...");
            List<RendezVousDetail> rdvs =
                    rendezVousService.afficherRendezVousDetailsByPsy(utilisateur.getUserId());
            rendezVousList.clear();
            rendezVousList.addAll(rdvs);
            majStatistiques(rdvs);
            appliquerFiltres();
        } catch (SQLException e) {
            lblStatut.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void majStatistiques(List<RendezVousDetail> rdvs) {
        lblStatTotal.setText(String.valueOf(rdvs.size()));
        lblStatDemande.setText(String.valueOf(
                rdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("demande")).count()));
        lblStatConfirme.setText(String.valueOf(
                rdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("confirme")).count()));
        lblStatEnCours.setText(String.valueOf(
                rdvs.stream().filter(r -> r.getStatut().equalsIgnoreCase("en-cours")).count()));
    }

    private String getMessageStatut(String statut) {
        return switch (statut) {
            case "confirme" -> "confirmé !";
            case "annulé"   -> "annulé !";
            case "en-cours" -> "passé en cours !";
            case "terminé"  -> "terminé !";
            case "absent"   -> "marqué absent !";
            default         -> "modifié !";
        };
    }

    // ══════════════════════════════════════════════════════════════
//  VISIOCONFÉRENCE
// ══════════════════════════════════════════════════════════════

    /**
     * Configure la colonne Visioconférence
     * Le bouton s'affiche UNIQUEMENT si :
     * - Le statut du RDV est "confirme"
     * - Le type de consultation est "en_ligne"
     * - Le lien_visio n'a pas encore été généré (ou on régénère)
     */
    private void configurerColonneVisio() {
        colVisio.setCellFactory(col -> new TableCell<>() {
            private final Button btnVisio = new Button("📹 Démarrer");

            {
                btnVisio.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; " +
                        "-fx-font-size: 11px; -fx-padding: 5 10; " +
                        "-fx-background-radius: 15; -fx-cursor: hand; " +
                        "-fx-font-weight: bold;");

                btnVisio.setOnAction(e -> {
                    RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                    lancerVisioconference(rdv);
                });

                btnVisio.setOnMouseEntered(ev ->
                        btnVisio.setStyle(btnVisio.getStyle().replace("#8b5cf6", "#7c3aed")));
                btnVisio.setOnMouseExited(ev ->
                        btnVisio.setStyle(btnVisio.getStyle().replace("#7c3aed", "#8b5cf6")));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                String statut = rdv.getStatut().toLowerCase();
                String typeConsult = rdv.getTypeConsult().toLowerCase();

                // ✅ Afficher le bouton seulement si RDV confirmé ET consultation en ligne
                boolean afficherVisio = "confirme".equals(statut) && "en_ligne".equals(typeConsult);

                btnVisio.setVisible(afficherVisio);
                btnVisio.setManaged(afficherVisio);
                setGraphic(btnVisio);
            }
        });
    }

    /**
     * Lance une consultation vidéo pour un rendez-vous
     */
    private void lancerVisioconference(RendezVousDetail rdv) {
        try {
            // 1. Générer un lien unique pour la salle
            String lienVisio = genererLienVisio(rdv.getRendezVousId());

            // 2. Sauvegarder le lien dans la BDD
            sauvegarderLienVisio(rdv.getRendezVousId(), lienVisio);

            // 3. Ouvrir le lien dans le navigateur par défaut
            ouvrirLienDansNavigateur(lienVisio);

            // 4. Optionnel : mettre à jour le statut du RDV (facultatif)
            // mettreAJourStatutRDV(rdv.getRendezVousId(), "en-cours");

            showToast("✓ Consultation vidéo démarrée !", true);

        } catch (Exception e) {
            showToast("✗ Impossible de démarrer la visio : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    /**
     * Génère un lien unique pour la salle de visioconférence
     */
    private String genererLienVisio(int rendezVousId) {
        // Utiliser Daily.co (compatible avec tous les navigateurs)
        // Inscription gratuite sur daily.co pour obtenir un domaine
        String domaine = "https://unimind.daily.co"; // À remplacer par votre domaine Daily.co

        // Alternative gratuite sans inscription : Jitsi
        return "https://meet.jit.si/unimind_rencontre_" + rendezVousId + "_" + System.currentTimeMillis();
    }

    /**
     * Sauvegarde le lien de visioconférence dans la base de données
     */
    private void sauvegarderLienVisio(int rdvId, String lien) {
        String sql = "UPDATE rendez_vous SET lien_visio = ? WHERE rendez_vous_id = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lien);
            pstmt.setInt(2, rdvId);
            pstmt.executeUpdate();
            System.out.println("✓ Lien visio sauvegardé pour le RDV " + rdvId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ouvre le lien dans le navigateur par défaut du système
     */
    private void ouvrirLienDansNavigateur(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Impossible d'ouvrir le navigateur", false);
        }
    }
}