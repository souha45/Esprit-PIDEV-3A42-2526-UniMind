package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.models.RendezVousDetail;
import org.example.models.User;
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

    // ── Table ───────────────────────────────────────────────────────
    @FXML private TableView<RendezVousDetail>             tableViewRendezVous;
    @FXML private TableColumn<RendezVousDetail, String>   colDateHeure;
    @FXML private TableColumn<RendezVousDetail, String>   colPsychologue;
    @FXML private TableColumn<RendezVousDetail, String>   colType;
    @FXML private TableColumn<RendezVousDetail, String>   colStatut;
    @FXML private TableColumn<RendezVousDetail, Void>     colActions;

    // ── Toolbar ─────────────────────────────────────────────────────
    @FXML private Button btnPrendreRdv;
    @FXML private Button btnRafraichir;
    @FXML private Label  lblStatut;

    // ── Données ─────────────────────────────────────────────────────
    private User                             utilisateur;
    private RendezVousService                rendezVousService;
    private ObservableList<RendezVousDetail> rendezVousList;
    private FilteredList<RendezVousDetail>   filteredList;
    private String                           filtreStatutActif = "tous";

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Date header
        LocalDate today = LocalDate.now();
        String jour = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(
                jour.substring(0, 1).toUpperCase() + jour.substring(1)
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
        btnRafraichir.setOnAction(e -> chargerRendezVous());

        // Hover btn principal
        btnPrendreRdv.setOnMouseEntered(e ->
                btnPrendreRdv.setStyle(btnPrendreRdv.getStyle().replace("#6366f1","#4f46e5")));
        btnPrendreRdv.setOnMouseExited(e ->
                btnPrendreRdv.setStyle(btnPrendreRdv.getStyle().replace("#4f46e5","#6366f1")));
    }

    // ── Filtres texte + combo ────────────────────────────────────────
    private void initialiserFiltres() {
        comboType.setItems(FXCollections.observableArrayList("Tous", "présentiel", "en ligne"));
        comboType.setValue("Tous");

        fieldRecherche.textProperty().addListener((o, ov, nv) -> appliquerFiltres());
        comboPsy.valueProperty().addListener((o, ov, nv)      -> appliquerFiltres());
        comboType.valueProperty().addListener((o, ov, nv)     -> appliquerFiltres());
    }

    private void remplirComboPsy(List<RendezVousDetail> liste) {
        ObservableList<String> psyNoms = FXCollections.observableArrayList("Tous");
        liste.stream()
                .map(r -> "Dr. " + r.getPsyPrenom() + " " + r.getPsyNom())
                .distinct()
                .forEach(psyNoms::add);
        comboPsy.setItems(psyNoms);
        comboPsy.setValue("Tous");
    }

    private void appliquerFiltres() {
        String recherche = fieldRecherche.getText() == null ? ""
                : fieldRecherche.getText().toLowerCase().trim();
        String psy  = comboPsy.getValue();
        String type = comboType.getValue();

        filteredList.setPredicate(rdv -> {
            // Filtre statut actif (boutons)
            boolean matchStatut = "tous".equals(filtreStatutActif)
                    || rdv.getStatutRDV().equalsIgnoreCase(filtreStatutActif);

            // Filtre recherche (psy ou date)
            String psyNom = "Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom();
            boolean matchRecherche = recherche.isEmpty()
                    || psyNom.toLowerCase().contains(recherche)
                    || rdv.getDateDispo().toString().contains(recherche);

            // Filtre combo psy
            boolean matchPsy = psy == null || "Tous".equals(psy)
                    || psyNom.equals(psy);

            // Filtre type
            boolean matchType = type == null || "Tous".equals(type)
                    || rdv.getTypeConsult().equalsIgnoreCase(type);

            return matchStatut && matchRecherche && matchPsy && matchType;
        });

        mettreAJourStatutLabel();
    }

    // ── Boutons filtre statut ────────────────────────────────────────
    private void configurerBoutonsStatut() {
        Button[] btns = {btnFiltreAll, btnFiltreDemande, btnFiltreConfirme,
                btnFiltreEnCours, btnFiltreTermine, btnFiltreAnnule, btnFiltreAbsent};
        String[] statuts = {"tous","demande","confirme","Encours","terminé","annulé","absent"};

        for (int i = 0; i < btns.length; i++) {
            final String s = statuts[i];
            final Button b = btns[i];
            b.setOnAction(e -> {
                filtreStatutActif = s;
                appliquerFiltres();
                surlignerBoutonActif(b, btns);
            });
        }
    }

    private void surlignerBoutonActif(Button actif, Button[] tous) {
        // Remettre les styles d'origine puis souligner l'actif
        String[][] styles = {
                {"-fx-background-color: #6366f1; -fx-text-fill: #ffffff;",
                        "-fx-background-color: #4f46e5; -fx-text-fill: #ffffff;"},
                {"-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04;",
                        "-fx-background-color: #fde68a; -fx-text-fill: #92400e;"},
                {"-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;",
                        "-fx-background-color: #bbf7d0; -fx-text-fill: #15803d;"},
                {"-fx-background-color: #dbeafe; -fx-text-fill: #2563eb;",
                        "-fx-background-color: #bfdbfe; -fx-text-fill: #1d4ed8;"},
                {"-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;",
                        "-fx-background-color: #e5e7eb; -fx-text-fill: #374151;"},
                {"-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;",
                        "-fx-background-color: #fecaca; -fx-text-fill: #b91c1c;"},
                {"-fx-background-color: #ffedd5; -fx-text-fill: #c2410c;",
                        "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412;"},
        };
        String commun = " -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 7 16; " +
                "-fx-background-radius: 20; -fx-cursor: hand;";

        for (int i = 0; i < tous.length; i++) {
            boolean isActif = tous[i] == actif;
            tous[i].setStyle(styles[i][isActif ? 1 : 0] + commun
                    + (isActif ? " -fx-border-color: rgba(0,0,0,0.15); -fx-border-width: 2; -fx-border-radius: 20;" : ""));
        }
    }

    // ── Colonnes ────────────────────────────────────────────────────
    private void configurerColonnes() {

        // Date & Heure (2 lignes)
        colDateHeure.setCellValueFactory(cell -> {
            RendezVousDetail r = cell.getValue();
            String date  = r.getDateDispo().toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String debut = r.getHeureDebut().toString().substring(0, 5);
            String fin   = r.getHeureFin().toString().substring(0, 5);
            return new SimpleStringProperty(date + "\n" + debut + " – " + fin);
        });
        colDateHeure.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: #374151; -fx-padding: 10 16; -fx-alignment: CENTER_LEFT;");
            }
        });

        // Psychologue
        colPsychologue.setCellValueFactory(cell -> {
            RendezVousDetail r = cell.getValue();
            return new SimpleStringProperty("Dr. " + r.getPsyPrenom() + " " + r.getPsyNom());
        });
        colPsychologue.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #3730a3; -fx-padding: 10 16;");
            }
        });

        // Type (badge coloré)
        colType.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTypeConsult()));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                boolean presentiel = "présentiel".equalsIgnoreCase(s);
                Label badge = new Label(presentiel ? "🏢  Présentiel" : "💻  En ligne");
                badge.setStyle(
                        "-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge); setText(null);
            }
        });

        // Statut (badge coloré)
        colStatut.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatutRDV()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                String[] sc = statutStyle(s);
                Label badge = new Label(sc[2]);
                badge.setStyle(
                        "-fx-background-color: " + sc[0] + "; -fx-text-fill: " + sc[1] + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge); setText(null);
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = new Button("🔍");
            private final Button btnAnnuler = new Button("✗");
            private final HBox   box        = new HBox(6, btnDetails, btnAnnuler);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                String styleView =
                        "-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; " +
                                "-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
                btnDetails.setStyle(styleView);
                btnDetails.setTooltip(new Tooltip("Voir les détails"));
                btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(styleView.replace("#ede9fe","#ddd6fe")));
                btnDetails.setOnMouseExited(e  -> btnDetails.setStyle(styleView));

                String styleDel =
                        "-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; " +
                                "-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
                btnAnnuler.setStyle(styleDel);
                btnAnnuler.setTooltip(new Tooltip("Annuler ce rendez-vous"));
                btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(styleDel.replace("#fee2e2","#fecaca")));
                btnAnnuler.setOnMouseExited(e  -> btnAnnuler.setStyle(styleDel));

                btnDetails.setOnAction(e ->
                        afficherDetailsRendezVous(getTableView().getItems().get(getIndex())));
                btnAnnuler.setOnAction(e ->
                        annulerRendezVous(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                RendezVousDetail rdv = getTableView().getItems().get(getIndex());
                String s = rdv.getStatutRDV().toLowerCase();
                // Masquer "Annuler" si déjà terminé / annulé / absent
                boolean peutAnnuler = !"terminé".equals(s) && !"annulé".equals(s) && !"absent".equals(s);
                btnAnnuler.setVisible(peutAnnuler);
                btnAnnuler.setManaged(peutAnnuler);

                setGraphic(box);
            }
        });
    }

    // ── Chargement ──────────────────────────────────────────────────
    private void chargerRendezVous() {
        if (utilisateur == null) {
            lblStatut.setText("Erreur : utilisateur non connecté");
            return;
        }
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
        long total    = liste.size();
        long aVenir   = liste.stream().filter(r ->
                "confirme".equalsIgnoreCase(r.getStatutRDV()) ||
                        "Encours".equalsIgnoreCase(r.getStatutRDV())).count();
        long attente  = liste.stream().filter(r ->
                "demande".equalsIgnoreCase(r.getStatutRDV())).count();
        long termines = liste.stream().filter(r ->
                "terminé".equalsIgnoreCase(r.getStatutRDV())).count();
        long annules  = liste.stream().filter(r ->
                "annulé".equalsIgnoreCase(r.getStatutRDV()) ||
                        "absent".equalsIgnoreCase(r.getStatutRDV())).count();

        lblStatTotal.setText(String.valueOf(total));
        lblStatAVenir.setText(String.valueOf(aVenir));
        lblStatAttente.setText(String.valueOf(attente));
        lblStatTermines.setText(String.valueOf(termines));
        lblStatAnnules.setText(String.valueOf(annules));
    }

    private void mettreAJourStatutLabel() {
        int nb = filteredList.size();
        lblStatut.setText(nb == 0 ? "Aucun rendez-vous trouvé."
                : nb + " rendez-vous affiché(s)");
    }

    // ── Actions (inchangées) ─────────────────────────────────────────
    private void afficherDetailsRendezVous(RendezVousDetail rdv) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du rendez-vous");
        alert.setHeaderText("Rendez-vous du " + rdv.getDateDispo());
        alert.setContentText(
                "📅 Date : "         + rdv.getDateDispo()                               + "\n" +
                        "⏰ Horaire : "       + rdv.getHeureDebut().toString().substring(0,5)
                        + " – " + rdv.getHeureFin().toString().substring(0,5) + "\n" +
                        "👨‍⚕️ Psychologue : Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom() + "\n" +
                        "💬 Type : "          + rdv.getTypeConsult()                             + "\n" +
                        "📌 Statut : "        + rdv.getStatutRDV()
        );
        alert.showAndWait();
    }

    private void annulerRendezVous(RendezVousDetail rdv) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation d'annulation");
        confirmation.setHeaderText("Annuler le rendez-vous");
        confirmation.setContentText("Voulez-vous vraiment annuler le rendez-vous du "
                + rdv.getDateDispo() + " avec Dr. " + rdv.getPsyPrenom() + " " + rdv.getPsyNom() + " ?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    rendezVousService.modifierStatutRendezVous(
                            rdv.getRendezVousId(),
                            utilisateur.getUserId(),
                            0,
                            "annulé"
                    );
                    chargerRendezVous();
                    afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous annulé avec succès !");
                } catch (SQLException e) {
                    afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible d'annuler : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void prendreRendezVous() {
        if (utilisateur == null) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrendreRendezVousModal.fxml"));
            Stage modalStage  = new Stage();
            Scene scene       = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
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
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire");
            e.printStackTrace();
        }
    }

    // ── Interface EtudiantPageController ────────────────────────────
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml("/RendezVousEtudiant.fxml");
        }
        chargerRendezVous();
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private String[] statutStyle(String statut) {
        return switch (statut.toLowerCase()) {
            case "confirme"  -> new String[]{"#dcfce7","#16a34a","✓ Confirmé"};
            case "encours"   -> new String[]{"#dbeafe","#2563eb","⏳ En cours"};
            case "demande"   -> new String[]{"#fef9c3","#ca8a04","🕐 Demande" };
            case "terminé"   -> new String[]{"#f3f4f6","#6b7280","✅ Terminé" };
            case "annulé"    -> new String[]{"#fee2e2","#dc2626","✗ Annulé"  };
            case "absent"    -> new String[]{"#ffedd5","#c2410c","⚠ Absent"  };
            default          -> new String[]{"#f3f4f6","#6b7280", statut      };
        };
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}