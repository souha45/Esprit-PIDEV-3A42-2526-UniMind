package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.example.utils.MyDataBase_Unimind;
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
import org.example.entities.RendezVousDetail;
import org.example.entities.User;
import org.example.services.RendezVousService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class RendezVousEtudiantController
        implements SidebarEtudiantController.EtudiantPageController {

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblDate;

    // ── Stat cards ──────────────────────────────────────────────────
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatAVenir;
    @FXML private Label lblStatAttente;
    @FXML private Label lblStatTermines;
    @FXML private Label lblStatAnnules;

    // ── Filtres ─────────────────────────────────────────────────────
    @FXML private TextField        fieldRecherche;
    @FXML private ComboBox<String> comboPsy;
    @FXML private ComboBox<String> comboType;

    // ── Boutons filtre statut ────────────────────────────────────────
    @FXML private Button btnFiltreAll;
    @FXML private Button btnFiltreDemande;
    @FXML private Button btnFiltreConfirme;
    @FXML private Button btnFiltreEnCours;
    @FXML private Button btnFiltreTermine;
    @FXML private Button btnFiltreAnnule;
    @FXML private Button btnFiltreAbsent;
    @FXML private Button btnAssistant;

    // ── Table ───────────────────────────────────────────────────────
    @FXML private TableView<RendezVousDetail>           tableViewRendezVous;
    @FXML private TableColumn<RendezVousDetail, String> colDateHeure;
    @FXML private TableColumn<RendezVousDetail, String> colPsychologue;
    @FXML private TableColumn<RendezVousDetail, String> colType;
    @FXML private TableColumn<RendezVousDetail, String> colStatut;
    @FXML private TableColumn<RendezVousDetail, Void>   colActions;
    @FXML private TableColumn<RendezVousDetail, Void> colRejoindre;

    // ── Toolbar ─────────────────────────────────────────────────────
    @FXML private Button    btnPrendreRdv;
    @FXML private Label     lblStatut;

    // ── Overlays ────────────────────────────────────────────────────
    @FXML private StackPane toastContainer;
    @FXML private StackPane overlayContainer;

    // ── Données ─────────────────────────────────────────────────────
    private User                             utilisateur;
    private RendezVousService                rendezVousService;
    private ObservableList<RendezVousDetail> rendezVousList;
    private FilteredList<RendezVousDetail>   filteredList;
    private String                           filtreStatutActif = "tous";

    // ════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        String jour = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(jour.substring(0,1).toUpperCase() + jour.substring(1)
                + " " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

        rendezVousService = new RendezVousService();
        rendezVousList    = FXCollections.observableArrayList();
        filteredList      = new FilteredList<>(rendezVousList, p -> true);

        initialiserFiltres();
        configurerColonnes();
        configurerBoutonsStatut();

        tableViewRendezVous.setItems(filteredList);
        tableViewRendezVous.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        btnPrendreRdv.setOnAction(e -> prendreRendezVous());
        btnPrendreRdv.setOnMouseEntered(e ->
                btnPrendreRdv.setStyle(btnPrendreRdv.getStyle().replace("#6366f1","#4f46e5")));
        btnPrendreRdv.setOnMouseExited(e ->
                btnPrendreRdv.setStyle(btnPrendreRdv.getStyle().replace("#4f46e5","#6366f1")));

        btnPrendreRdv.setOnAction(e -> prendreRendezVous());
// ✅ AJOUTER
        btnAssistant.setOnAction(e -> ouvrirAssistant());
        btnAssistant.setOnMouseEntered(e ->
                btnAssistant.setStyle(btnAssistant.getStyle().replace("#8b5cf6","#7c3aed")));
        btnAssistant.setOnMouseExited(e ->
                btnAssistant.setStyle(btnAssistant.getStyle().replace("#7c3aed","#8b5cf6")));
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTRES
    // ════════════════════════════════════════════════════════════════
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous","présentiel","en_ligne"));
        comboType.setValue("Tous");
        fieldRecherche.textProperty().addListener((o,ov,nv) -> appliquerFiltres());
        comboPsy.valueProperty().addListener((o,ov,nv)      -> appliquerFiltres());
        comboType.valueProperty().addListener((o,ov,nv)     -> appliquerFiltres());
    }

    private void remplirComboPsy(List<RendezVousDetail> liste) {
        ObservableList<String> psyNoms = FXCollections.observableArrayList("Tous");
        liste.stream()
                .map(r -> "Dr. " + r.getPsyPrenom() + " " + r.getPsyNom())
                .distinct().forEach(psyNoms::add);
        comboPsy.setItems(psyNoms);
        comboPsy.setValue("Tous");
    }

    private void appliquerFiltres() {
        String recherche = fieldRecherche.getText() == null ? ""
                : fieldRecherche.getText().toLowerCase().trim();
        String psy  = comboPsy.getValue();
        String type = comboType.getValue();

        filteredList.setPredicate(rdv -> {
            boolean matchStatut = "tous".equals(filtreStatutActif)
                    || rdv.getStatutRDV().equalsIgnoreCase(filtreStatutActif);
            String psyNom = "Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom();
            boolean matchRecherche = recherche.isEmpty()
                    || psyNom.toLowerCase().contains(recherche)
                    || rdv.getDateDispo().toString().contains(recherche);
            boolean matchPsy  = psy  == null || "Tous".equals(psy)  || psyNom.equals(psy);
            boolean matchType = type == null || "Tous".equals(type)
                    || rdv.getTypeConsult().equalsIgnoreCase(type);
            return matchStatut && matchRecherche && matchPsy && matchType;
        });
        mettreAJourStatutLabel();
    }

    // ════════════════════════════════════════════════════════════════
    //  BOUTONS FILTRE STATUT
    // ════════════════════════════════════════════════════════════════
    private void configurerBoutonsStatut() {
        Button[] btns    = {btnFiltreAll, btnFiltreDemande, btnFiltreConfirme,
                btnFiltreEnCours, btnFiltreTermine, btnFiltreAnnule, btnFiltreAbsent};
        String[] statuts = {"tous","demande","confirme","en-cours","terminé","annulé","absent"};
        for (int i = 0; i < btns.length; i++) {
            final String s = statuts[i];
            final Button b = btns[i];
            b.setOnAction(e -> { filtreStatutActif = s; appliquerFiltres(); surlignerBoutonActif(b, btns); });
        }
    }

    private void surlignerBoutonActif(Button actif, Button[] tous) {
        String[][] styles = {
                {"-fx-background-color:#6366f1;-fx-text-fill:#ffffff;","-fx-background-color:#4f46e5;-fx-text-fill:#ffffff;"},
                {"-fx-background-color:#fef9c3;-fx-text-fill:#ca8a04;","-fx-background-color:#fde68a;-fx-text-fill:#92400e;"},
                {"-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;","-fx-background-color:#bbf7d0;-fx-text-fill:#15803d;"},
                {"-fx-background-color:#dbeafe;-fx-text-fill:#2563eb;","-fx-background-color:#bfdbfe;-fx-text-fill:#1d4ed8;"},
                {"-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;","-fx-background-color:#e5e7eb;-fx-text-fill:#374151;"},
                {"-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;","-fx-background-color:#fecaca;-fx-text-fill:#b91c1c;"},
                {"-fx-background-color:#ffedd5;-fx-text-fill:#c2410c;","-fx-background-color:#fed7aa;-fx-text-fill:#9a3412;"},
        };
        String commun = "-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:7 16;-fx-background-radius:20;-fx-cursor:hand;";
        for (int i = 0; i < tous.length; i++) {
            boolean isActif = tous[i] == actif;
            tous[i].setStyle(styles[i][isActif ? 1 : 0] + commun
                    + (isActif ? "-fx-border-color:rgba(0,0,0,0.15);-fx-border-width:2;-fx-border-radius:20;" : ""));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  COLONNES
    // ════════════════════════════════════════════════════════════════
    private void configurerColonnes() {
        // 1. Colonne Date & Heure
        colDateHeure.setCellValueFactory(cell -> {
            RendezVousDetail r = cell.getValue();
            String date  = r.getDateDispo().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String debut = r.getHeureDebut().toString().substring(0,5);
            String fin   = r.getHeureFin().toString().substring(0,5);
            return new SimpleStringProperty(date + "\n" + debut + " – " + fin);
        });
        colDateHeure.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;" +
                        "-fx-text-fill:#374151;-fx-padding:10 16;-fx-alignment:CENTER_LEFT;");
            }
        });

        // 2. Colonne Psychologue
        colPsychologue.setCellValueFactory(cell ->
                new SimpleStringProperty("Dr. " + cell.getValue().getPsyPrenom()
                        + " " + cell.getValue().getPsyNom()));
        colPsychologue.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-font-family:'Segoe UI';-fx-font-size:13px;" +
                        "-fx-font-weight:bold;-fx-text-fill:#3730a3;-fx-padding:10 16;");
            }
        });

        // 3. Colonne Type
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTypeConsult()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                boolean p = "présentiel".equalsIgnoreCase(s);
                Label badge = new Label(p ? "🏢  Présentiel" : "💻  En ligne");
                badge.setStyle("-fx-background-color:" + (p ? "#dbeafe" : "#ede9fe") + ";" +
                        "-fx-text-fill:" + (p ? "#1d4ed8" : "#6366f1") + ";" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:4 12;-fx-background-radius:20;");
                setGraphic(badge); setText(null);
            }
        });

        // 4. Colonne Statut
        colStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatutRDV()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                String[] sc = statutStyle(s);
                Label badge = new Label(sc[2]);
                badge.setStyle("-fx-background-color:" + sc[0] + ";-fx-text-fill:" + sc[1] + ";" +
                        "-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:4 12;-fx-background-radius:20;");
                setGraphic(badge); setText(null);
            }
        });

        // ✅ 5. Colonne Actions (Détails + Annuler) - Original, à NE PAS MODIFIER
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = new Button("👁");
            private final Button btnAnnuler = new Button("✗");
            private final HBox   box        = new HBox(6, btnDetails, btnAnnuler);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                String sv = "-fx-background-color:#ede9fe;-fx-text-fill:#6366f1;" +
                        "-fx-font-size:13px;-fx-padding:6 10;-fx-background-radius:8;-fx-cursor:hand;";
                btnDetails.setStyle(sv);
                btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(sv.replace("#ede9fe","#ddd6fe")));
                btnDetails.setOnMouseExited(e  -> btnDetails.setStyle(sv));

                String sd = "-fx-background-color:#fee2e2;-fx-text-fill:#ef4444;" +
                        "-fx-font-size:13px;-fx-padding:6 10;-fx-background-radius:8;-fx-cursor:hand;";
                btnAnnuler.setStyle(sd);
                btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(sd.replace("#fee2e2","#fecaca")));
                btnAnnuler.setOnMouseExited(e  -> btnAnnuler.setStyle(sd));

                btnDetails.setOnAction(e ->
                        afficherDetailsRendezVous(getTableView().getItems().get(getIndex())));
                btnAnnuler.setOnAction(e ->
                        confirmerAnnulation(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                String s = rdv.getStatutRDV().toLowerCase();

                // ✅ Le bouton Annuler s'affiche UNIQUEMENT si :
                // - Le RDV n'est PAS terminé
                // - Le RDV n'est PAS annulé
                // - Le RDV n'est PAS absent
                // - Le RDV n'est PAS confirmé (NOUVEAU)
                boolean peutAnnuler = !"terminé".equals(s)
                        && !"annulé".equals(s)
                        && !"absent".equals(s)
                        && !"en-cours".equals(s)
                        && !"confirme".equals(s);  // ← NOUVEAU : cacher si confirmé
                btnAnnuler.setVisible(peutAnnuler);
                btnAnnuler.setManaged(peutAnnuler);
                setGraphic(box);
            }
        });

        // ✅ 6. NOUVEAU : Colonne Visio (Rejoindre)
        configurerColonneRejoindre();

        // ✅ 7. Ajouter la colonne Rejoindre à la table (avant ou après colActions)
        tableViewRendezVous.getColumns().add(colRejoindre);
    }

    /**
     * Configure la colonne pour rejoindre la visioconférence
     * Le bouton s'affiche UNIQUEMENT si :
     * - Le type de consultation est "en_ligne"
     * - Un lien de visio existe dans la BDD (lien_visio non null)
     */
    private void configurerColonneRejoindre() {
        colRejoindre.setCellFactory(col -> new TableCell<>() {
            private final Button btnRejoindre = new Button("🎥 Rejoindre");

            {
                btnRejoindre.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; " +
                        "-fx-font-size: 11px; -fx-padding: 5 10; " +
                        "-fx-background-radius: 15; -fx-cursor: hand; " +
                        "-fx-font-weight: bold;");

                btnRejoindre.setOnAction(e -> {
                    RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                    rejoindreVisioconference(rdv);
                });

                btnRejoindre.setOnMouseEntered(ev ->
                        btnRejoindre.setStyle(btnRejoindre.getStyle().replace("#10b981", "#059669")));
                btnRejoindre.setOnMouseExited(ev ->
                        btnRejoindre.setStyle(btnRejoindre.getStyle().replace("#059669", "#10b981")));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                String typeConsult = rdv.getTypeConsult().toLowerCase();

                // Récupérer le lien depuis la BDD si dispo
                String lienVisio = recupererLienVisio(rdv.getRendezVousId());
                boolean aUnLien = (lienVisio != null && !lienVisio.isEmpty());

                // DEBUG
                System.out.println("[Étudiant] RDV ID: " + rdv.getRendezVousId() +
                        ", Type: " + typeConsult +
                        ", Lien existant: " + aUnLien);

                // Afficher le bouton seulement si consultation en ligne ET lien existe
                boolean afficherRejoindre = "en_ligne".equals(typeConsult) && aUnLien;

                btnRejoindre.setVisible(afficherRejoindre);
                btnRejoindre.setManaged(afficherRejoindre);
                setGraphic(btnRejoindre);
            }
        });
    }

    /**
     * Récupère le lien de visioconférence depuis la base de données
     */
    private String recupererLienVisio(int rdvId) {
        String sql = "SELECT lien_visio FROM rendez_vous WHERE rendez_vous_id = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rdvId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("lien_visio");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Rejoint une consultation vidéo (ouvre le lien dans le navigateur)
     */
    private void rejoindreVisioconference(RendezVousDetail rdv) {
        String lienVisio = recupererLienVisio(rdv.getRendezVousId());

        if (lienVisio == null || lienVisio.isEmpty()) {
            showToast("❌ Le psychologue n'a pas encore démarré la consultation.", ToastType.ERROR);
            return;
        }

        // Ouvrir dans le navigateur par défaut
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(lienVisio));
            showToast("✓ Ouverture de la consultation vidéo...", ToastType.SUCCESS);
        } catch (IOException e) {
            e.printStackTrace();
            showToast("❌ Impossible d'ouvrir le navigateur", ToastType.ERROR);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MODAL DÉTAILS — card overlay moderne
    // ════════════════════════════════════════════════════════════════
    private void afficherDetailsRendezVous(RendezVousDetail rdv) {
        VBox card = new VBox(0);
        card.setMaxWidth(460);
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.28),24,0,0,6);");

        // Header gradient
        VBox hdr = new VBox(3);
        hdr.setStyle("-fx-background-color:linear-gradient(to bottom right,#3730a3,#6366f1);" +
                "-fx-padding:20 24 16 24;-fx-background-radius:16 16 0 0;");
        HBox hdrTop = new HBox();
        hdrTop.setAlignment(Pos.CENTER_LEFT);

        String dateStr = rdv.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        dateStr = dateStr.substring(0,1).toUpperCase() + dateStr.substring(1);

        VBox hdrInfo = new VBox(3);
        HBox.setHgrow(hdrInfo, Priority.ALWAYS);
        Label hTitle = new Label("📋  Détails du rendez-vous");
        hTitle.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:15px;" +
                "-fx-font-weight:bold;-fx-text-fill:#ffffff;");
        Label hDate = new Label(dateStr);
        hDate.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;" +
                "-fx-text-fill:rgba(255,255,255,0.75);");
        hdrInfo.getChildren().addAll(hTitle, hDate);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:#ffffff;" +
                "-fx-font-size:13px;-fx-padding:5 10;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-border-width:0;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(btnClose.getStyle().replace("0.15","0.28")));
        btnClose.setOnMouseExited(e  -> btnClose.setStyle(btnClose.getStyle().replace("0.28","0.15")));

        hdrTop.getChildren().addAll(hdrInfo, btnClose);
        hdr.getChildren().add(hdrTop);

        // Body
        VBox body = new VBox(10);
        body.setStyle("-fx-padding:18 24 12 24;");

        String debut = rdv.getHeureDebut().toString().substring(0,5);
        String fin   = rdv.getHeureFin().toString().substring(0,5);
        String[] sc  = statutStyle(rdv.getStatutRDV());

        body.getChildren().addAll(
                detailRow("👨‍⚕️  Psychologue", "Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom(), "#3730a3"),
                detailRow("🕐  Horaire",       debut + " – " + fin, "#374151"),
                detailRow("💬  Type",           rdv.getTypeConsult(), "#374151"),
                detailRowBadge("📌  Statut",    sc[2], sc[0], sc[1])
        );

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 24 18 24;" +
                "-fx-border-color:#f0f0ff;-fx-border-width:1 0 0 0;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:#6366f1;-fx-text-fill:#ffffff;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-padding:9 28;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.35),8,0,0,2);");
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(btnFermer.getStyle().replace("#6366f1","#4f46e5")));
        btnFermer.setOnMouseExited(e  -> btnFermer.setStyle(btnFermer.getStyle().replace("#4f46e5","#6366f1")));

        footer.getChildren().add(btnFermer);
        card.getChildren().addAll(hdr, body, footer);

        // Afficher overlay
        overlayContainer.getChildren().add(card);
        overlayContainer.setVisible(true);
        overlayContainer.setManaged(true);
        StackPane.setAlignment(card, Pos.CENTER);

        FadeTransition ft = new FadeTransition(Duration.millis(180), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        Runnable close = () -> fermerOverlay(card);
        btnClose.setOnAction(e  -> close.run());
        btnFermer.setOnAction(e -> close.run());
        overlayContainer.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayContainer) close.run();
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  MODAL CONFIRMATION ANNULATION
    // ════════════════════════════════════════════════════════════════
    private void confirmerAnnulation(RendezVousDetail rdv) {
        VBox card = new VBox(0);
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.22),24,0,0,6);");

        // Header rouge
        VBox hdr = new VBox(3);
        hdr.setStyle("-fx-background-color:linear-gradient(to bottom right,#b91c1c,#ef4444);" +
                "-fx-padding:18 22 16 22;-fx-background-radius:16 16 0 0;");
        Label hTitle = new Label("⚠  Annuler ce rendez-vous ?");
        hTitle.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:15px;" +
                "-fx-font-weight:bold;-fx-text-fill:#ffffff;");
        Label hSub = new Label("Cette action est irréversible");
        hSub.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;" +
                "-fx-text-fill:rgba(255,255,255,0.75);");
        hdr.getChildren().addAll(hTitle, hSub);

        // Body
        VBox body = new VBox(10);
        body.setStyle("-fx-padding:18 22 12 22;");

        String debut  = rdv.getHeureDebut().toString().substring(0,5);
        String fin    = rdv.getHeureFin().toString().substring(0,5);
        String dateF  = rdv.getDateDispo().toLocalDate()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));
        dateF = dateF.substring(0,1).toUpperCase() + dateF.substring(1);

        body.getChildren().addAll(
                detailRow("👨‍⚕️  Psychologue", "Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom(), "#374151"),
                detailRow("📅  Date",           dateF,                  "#374151"),
                detailRow("🕐  Horaire",         debut + " – " + fin,   "#374151")
        );

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 22 18 22;" +
                "-fx-border-color:#fef2f2;-fx-border-width:1 0 0 0;");

        Button btnNon = new Button("Non, garder");
        btnNon.setStyle("-fx-background-color:#f3f4f6;-fx-text-fill:#374151;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:9 18;-fx-background-radius:10;" +
                "-fx-border-color:#e5e7eb;-fx-border-width:1;-fx-border-radius:10;-fx-cursor:hand;");
        btnNon.setOnMouseEntered(e -> btnNon.setStyle(btnNon.getStyle().replace("#f3f4f6","#e5e7eb")));
        btnNon.setOnMouseExited(e  -> btnNon.setStyle(btnNon.getStyle().replace("#e5e7eb","#f3f4f6")));

        Button btnOui = new Button("✗  Oui, annuler");
        btnOui.setStyle("-fx-background-color:#ef4444;-fx-text-fill:#ffffff;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:9 18;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.35),8,0,0,2);");
        btnOui.setOnMouseEntered(e -> btnOui.setStyle(btnOui.getStyle().replace("#ef4444","#dc2626")));
        btnOui.setOnMouseExited(e  -> btnOui.setStyle(btnOui.getStyle().replace("#dc2626","#ef4444")));

        footer.getChildren().addAll(btnNon, btnOui);
        card.getChildren().addAll(hdr, body, footer);

        overlayContainer.getChildren().add(card);
        overlayContainer.setVisible(true);
        overlayContainer.setManaged(true);
        StackPane.setAlignment(card, Pos.CENTER);

        FadeTransition ft = new FadeTransition(Duration.millis(180), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        btnNon.setOnAction(e -> fermerOverlay(card));
        overlayContainer.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayContainer) fermerOverlay(card);
        });
        btnOui.setOnAction(e -> {
            fermerOverlay(card);
            effectuerAnnulation(rdv);
        });
    }

    private void effectuerAnnulation(RendezVousDetail rdv) {
        try {
            rendezVousService.modifierStatutRendezVous(
                    rdv.getRendezVousId(), utilisateur.getUserId(), 0, "annulé");
            chargerRendezVous();
            showToast("✓  Rendez-vous annulé avec succès.", ToastType.SUCCESS);
        } catch (SQLException e) {
            showToast("✗  Impossible d'annuler : " + e.getMessage(), ToastType.ERROR);
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FERMER OVERLAY
    // ════════════════════════════════════════════════════════════════
    private void fermerOverlay(VBox card) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), card);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            overlayContainer.getChildren().remove(card);
            if (overlayContainer.getChildren().isEmpty()) {
                overlayContainer.setVisible(false);
                overlayContainer.setManaged(false);
                overlayContainer.setOnMouseClicked(null);
            }
        });
        ft.play();
    }

    // ════════════════════════════════════════════════════════════════
    //  TOAST
    // ════════════════════════════════════════════════════════════════
    private enum ToastType { SUCCESS, WARNING, ERROR }

    private void showToast(String message, ToastType type) {
        String bg = switch (type) {
            case SUCCESS -> "#10b981";
            case WARNING -> "#f59e0b";
            case ERROR   -> "#ef4444";
        };
        Label pill = new Label(message);
        pill.setWrapText(true);
        pill.setMaxWidth(500);
        pill.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                "-fx-font-family:'Segoe UI';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-padding:12 22;-fx-background-radius:30;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),12,0,0,4);");

        toastContainer.getChildren().add(pill);
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        StackPane.setAlignment(pill, Pos.BOTTOM_CENTER);

        FadeTransition fi = new FadeTransition(Duration.millis(200), pill);
        fi.setFromValue(0); fi.setToValue(1);
        FadeTransition fo = new FadeTransition(Duration.millis(400), pill);
        fo.setDelay(Duration.seconds(2.4));
        fo.setFromValue(1); fo.setToValue(0);
        fo.setOnFinished(e -> {
            toastContainer.getChildren().remove(pill);
            if (toastContainer.getChildren().isEmpty()) {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            }
        });
        fi.play(); fo.play();
    }

    // ════════════════════════════════════════════════════════════════
    //  PRENDRE RDV
    // ════════════════════════════════════════════════════════════════
    private void prendreRendezVous() {
        if (utilisateur == null) {
            showToast("✗  Utilisateur non connecté.", ToastType.ERROR);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrendreRendezVousModal.fxml"));
            Stage modalStage  = new Stage();
            Scene scene       = new Scene(loader.load());
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.initOwner(btnPrendreRdv.getScene().getWindow());
            modalStage.setTitle("Prendre un rendez-vous");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            PrendreRendezVousModalController controller = loader.getController();
            controller.setEtudiantId(utilisateur.getUserId());
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerRendezVous();
        } catch (IOException e) {
            showToast("✗  Impossible d'ouvrir le formulaire.", ToastType.ERROR);
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CHARGEMENT
    // ════════════════════════════════════════════════════════════════
    private void chargerRendezVous() {
        if (utilisateur == null) { lblStatut.setText("Erreur : utilisateur non connecté"); return; }
        try {
            lblStatut.setText("Chargement…");
            List<RendezVousDetail> liste =
                    rendezVousService.afficherRendezVousDetailsByEtudiant(utilisateur.getUserId());
            rendezVousList.setAll(liste);
            remplirComboPsy(liste);
            mettreAJourStatCards(liste);
            appliquerFiltres();
        } catch (SQLException e) {
            lblStatut.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mettreAJourStatCards(List<RendezVousDetail> liste) {
        lblStatTotal.setText(String.valueOf(liste.size()));
        lblStatAVenir.setText(String.valueOf(liste.stream().filter(r ->
                "confirme".equalsIgnoreCase(r.getStatutRDV()) || "Encours".equalsIgnoreCase(r.getStatutRDV())).count()));
        lblStatAttente.setText(String.valueOf(liste.stream().filter(r ->
                "demande".equalsIgnoreCase(r.getStatutRDV())).count()));
        lblStatTermines.setText(String.valueOf(liste.stream().filter(r ->
                "terminé".equalsIgnoreCase(r.getStatutRDV())).count()));
        lblStatAnnules.setText(String.valueOf(liste.stream().filter(r ->
                "annulé".equalsIgnoreCase(r.getStatutRDV()) || "absent".equalsIgnoreCase(r.getStatutRDV())).count()));
    }

    private void mettreAJourStatutLabel() {
        int nb = filteredList.size();
        lblStatut.setText(nb == 0 ? "Aucun rendez-vous trouvé." : nb + " rendez-vous affiché(s)");
    }

    // ════════════════════════════════════════════════════════════════
    //  INTERFACE
    // ════════════════════════════════════════════════════════════════
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml("/RendezVousEtudiant.fxml");
        }
        chargerRendezVous();
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private HBox detailRow(String label, String value, String valueColor) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:6 12;-fx-background-color:#f8f7ff;-fx-background-radius:8;");
        Label lbl = new Label(label);
        lbl.setMinWidth(130);
        lbl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;" +
                "-fx-text-fill:#9ca3af;-fx-font-weight:bold;");
        Label val = new Label(value);
        val.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:12px;" +
                "-fx-text-fill:" + valueColor + ";-fx-font-weight:bold;");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private HBox detailRowBadge(String label, String badgeTxt, String bgColor, String fgColor) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:6 12;-fx-background-color:#f8f7ff;-fx-background-radius:8;");
        Label lbl = new Label(label);
        lbl.setMinWidth(130);
        lbl.setStyle("-fx-font-family:'Segoe UI';-fx-font-size:11px;" +
                "-fx-text-fill:#9ca3af;-fx-font-weight:bold;");
        Label badge = new Label(badgeTxt);
        badge.setStyle("-fx-background-color:" + bgColor + ";-fx-text-fill:" + fgColor + ";" +
                "-fx-font-family:'Segoe UI';-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-padding:3 12;-fx-background-radius:20;");
        row.getChildren().addAll(lbl, badge);
        return row;
    }

    private String[] statutStyle(String statut) {
        return switch (statut.toLowerCase()) {
            case "confirme" -> new String[]{"#dcfce7","#16a34a","✓ Confirmé"};
            case "encours"  -> new String[]{"#dbeafe","#2563eb","⏳ En cours"};
            case "demande"  -> new String[]{"#fef9c3","#ca8a04","🕐 Demande" };
            case "terminé"  -> new String[]{"#f3f4f6","#6b7280","✅ Terminé" };
            case "annulé"   -> new String[]{"#fee2e2","#dc2626","✗ Annulé"  };
            case "absent"   -> new String[]{"#ffedd5","#c2410c","⚠ Absent"  };
            default         -> new String[]{"#f3f4f6","#6b7280", statut      };
        };
    }

    // ════════════════════════════════════════════════════════════════
//  ASSISTANT VIRTUEL
// ════════════════════════════════════════════════════════════════
    private void ouvrirAssistant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AssistantModal.fxml"));
            Stage assistantStage = new Stage();
            Scene scene = new Scene(loader.load(), 400, 550);

            assistantStage.initModality(Modality.WINDOW_MODAL);
            assistantStage.initOwner(btnAssistant.getScene().getWindow());
            assistantStage.setTitle("Assistant Unimind");
            assistantStage.setScene(scene);
            assistantStage.setResizable(false);

            AssistantModalController controller = loader.getController();
            controller.setModalStage(assistantStage);

            assistantStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showToast("❌ Impossible d'ouvrir l'assistant", ToastType.ERROR);
        }
    }
}