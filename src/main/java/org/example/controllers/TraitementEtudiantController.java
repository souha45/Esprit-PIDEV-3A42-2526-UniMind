package org.example.controllers;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.enums.SaisiPar;
import org.example.services.OrdonnancePDFService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementEmailService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TraitementEtudiantController implements Initializable, SidebarEtudiantController.EtudiantPageController {

    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cmbFiltreStatut;
    @FXML
    private ComboBox<String> cmbFiltrePriorite;
    @FXML
    private ComboBox<String> cmbTri;
    @FXML
    private Button btnAppliquerFiltres;
    @FXML
    private Button btnReinitialiserFiltres;
    @FXML
    private VBox cardsContainer;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblAucunResultat;
    @FXML
    private Label lblDate;

    // Statistiques
    @FXML private Label statTotal;
    @FXML private Label statEnCours;
    @FXML private Label statTermine;
    @FXML private Label statSuspendu;
    @FXML private Label statPrioriteHaute;

    // Sidebar (injecté comme VBox, pas comme contrôleur)
    @FXML private SidebarEtudiantController sidebarEtudiantController;
    @FXML private StackPane toastContainer;

    private TraitementService traitementService;
    private SuiviTraitementService suiviTraitementService;
    private List<Traitement> tousLesTraitements;
    private List<SuiviTraitement> tousLesSuivis;
    private List<VBox> toutesLesCartes;
    private boolean isInitialized = false;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lblDate != null) {
            lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        try {
            traitementService = new TraitementService();
            suiviTraitementService = new SuiviTraitementService();
            toutesLesCartes = new ArrayList<>();

            initialiserFiltres();
            initialiserTri();

            isInitialized = true;
            if (lblStatus != null) {
                lblStatus.setText("✓ Bienvenue sur votre espace personnel");
            }

            // Charger les données via SessionManager
            //chargerDonneesAvecSession();

        } catch (Exception e) {
            if (lblStatus != null) {
                lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @Override
    public void setUtilisateur(User user) {
        this.currentUser = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml("/traitement-etudiant-view.fxml");
        }
        chargerDonnees();  // charge les données avec currentUser
    }

    private void chargerDonnees() {
        if (currentUser == null) {
            if (lblStatus != null) lblStatus.setText("✗ Utilisateur non connecté");
            return;
        }
        try {
            if (lblStatus != null) lblStatus.setText("Chargement en cours...");
            int etudiantId = currentUser.getUserId();

            List<Traitement> tousTraitements = traitementService.afficher().stream()
                    .filter(t -> t.getEtudiantId() == etudiantId)
                    .collect(Collectors.toList());

            tousLesSuivis = suiviTraitementService.afficher();

            mettreAJourStatistiques(tousTraitements);
            tousLesTraitements = tousTraitements;
            appliquerFiltres();

            if (lblStatus != null) {
                lblStatus.setText(tousLesTraitements.size() + " traitement(s) trouvé(s)");
            }

        } catch (SQLException e) {
            if (lblStatus != null) lblStatus.setText("✗ Erreur: " + e.getMessage());
            afficherToast("✗ Erreur de chargement: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }


    private void initialiserFiltres() {
        ObservableList<String> statuts = FXCollections.observableArrayList(
                "Tous les statuts", "EN_COURS", "TERMINE", "SUSPENDU"
        );
        cmbFiltreStatut.setItems(statuts);
        cmbFiltreStatut.setValue("Tous les statuts");

        ObservableList<String> priorites = FXCollections.observableArrayList(
                "Toutes les priorités", "HAUTE", "MOYENNE", "BASSE"
        );
        cmbFiltrePriorite.setItems(priorites);
        cmbFiltrePriorite.setValue("Toutes les priorités");

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
        cmbFiltreStatut.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
        cmbFiltrePriorite.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
        cmbTri.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
    }

    private void initialiserTri() {
        cmbTri.setItems(FXCollections.observableArrayList(
                "📅 Date (plus récent)",
                "📅 Date (plus ancien)",
                "🔤 Titre (A-Z)",
                "🔤 Titre (Z-A)",
                "📊 Priorité (Haute → Basse)",
                "📊 Priorité (Basse → Haute)",
                "✅ Statut (En cours → Terminé)"
        ));
        cmbTri.setValue("📅 Date (plus récent)");
    }

    private void chargerDonneesAvecSession() {
        SessionManager session = SessionManager.getInstance();

        if (!session.estConnecte()) {
            if (lblStatus != null) lblStatus.setText("✗ Erreur: utilisateur non connecté");
            return;
        }

        if (!session.estEtudiant()) {
            if (lblStatus != null) lblStatus.setText("✗ Accès réservé aux étudiants");
            return;
        }

        try {
            if (lblStatus != null) lblStatus.setText("Chargement en cours...");
            int etudiantId = session.getUtilisateurConnecteId();

            List<Traitement> tousTraitements = traitementService.afficher();
            tousLesTraitements = tousTraitements.stream()
                    .filter(t -> t.getEtudiantId() == etudiantId)
                    .collect(Collectors.toList());

            tousLesSuivis = suiviTraitementService.afficher();

            mettreAJourStatistiques(tousLesTraitements);
            appliquerFiltres();

            if (lblStatus != null) {
                lblStatus.setText(tousLesTraitements.size() + " traitement(s) trouvé(s)");
            }

        } catch (SQLException e) {
            if (lblStatus != null) lblStatus.setText("✗ Erreur: " + e.getMessage());
            afficherToast("✗ Erreur de chargement: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private void mettreAJourStatistiques(List<Traitement> traitements) {
        long total = traitements.size();
        long enCours = traitements.stream().filter(t -> t.getStatut().name().equals("EN_COURS")).count();
        long termine = traitements.stream().filter(t -> t.getStatut().name().equals("TERMINE")).count();
        long suspendu = traitements.stream().filter(t -> t.getStatut().name().equals("SUSPENDU")).count();
        long prioriteHaute = traitements.stream().filter(t -> t.getPriorite().name().equals("HAUTE")).count();

        if (statTotal != null) statTotal.setText(String.valueOf(total));
        if (statEnCours != null) statEnCours.setText(String.valueOf(enCours));
        if (statTermine != null) statTermine.setText(String.valueOf(termine));
        if (statSuspendu != null) statSuspendu.setText(String.valueOf(suspendu));
        if (statPrioriteHaute != null) statPrioriteHaute.setText(String.valueOf(prioriteHaute));
    }

    private List<Traitement> appliquerRechercheEtFiltres(List<Traitement> traitements) {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String statutFiltre = cmbFiltreStatut.getValue();
        String prioriteFiltre = cmbFiltrePriorite.getValue();

        return traitements.stream()
                .filter(t -> {
                    if (!recherche.isEmpty()) {
                        boolean correspondRecherche =
                                (t.getTitre() != null && t.getTitre().toLowerCase().contains(recherche)) ||
                                        (t.getType() != null && t.getType().toLowerCase().contains(recherche)) ||
                                        (t.getObjectifTherapeutique() != null && t.getObjectifTherapeutique().toLowerCase().contains(recherche));
                        if (!correspondRecherche) return false;
                    }
                    if (!"Tous les statuts".equals(statutFiltre)) {
                        if (t.getStatut() == null || !t.getStatut().name().equals(statutFiltre)) return false;
                    }
                    if (!"Toutes les priorités".equals(prioriteFiltre)) {
                        if (t.getPriorite() == null || !t.getPriorite().name().equals(prioriteFiltre)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Traitement> appliquerTri(List<Traitement> traitements) {
        String tri = cmbTri.getValue();
        if (tri == null) return traitements;

        List<Traitement> result = new ArrayList<>(traitements);

        switch (tri) {
            case "📅 Date (plus récent)":
                result.sort((t1, t2) -> {
                    if (t1.getDateDebut() == null) return 1;
                    if (t2.getDateDebut() == null) return -1;
                    return t2.getDateDebut().compareTo(t1.getDateDebut());
                });
                break;
            case "📅 Date (plus ancien)":
                result.sort((t1, t2) -> {
                    if (t1.getDateDebut() == null) return 1;
                    if (t2.getDateDebut() == null) return -1;
                    return t1.getDateDebut().compareTo(t2.getDateDebut());
                });
                break;
            case "🔤 Titre (A-Z)":
                result.sort(Comparator.comparing(Traitement::getTitre, String.CASE_INSENSITIVE_ORDER));
                break;
            case "🔤 Titre (Z-A)":
                result.sort((t1, t2) -> t2.getTitre().compareToIgnoreCase(t1.getTitre()));
                break;
            case "📊 Priorité (Haute → Basse)":
                result.sort((t1, t2) -> {
                    int p1 = getPrioriteOrdre(t1.getPriorite().name());
                    int p2 = getPrioriteOrdre(t2.getPriorite().name());
                    return Integer.compare(p1, p2);
                });
                break;
            case "📊 Priorité (Basse → Haute)":
                result.sort((t1, t2) -> {
                    int p1 = getPrioriteOrdre(t1.getPriorite().name());
                    int p2 = getPrioriteOrdre(t2.getPriorite().name());
                    return Integer.compare(p2, p1);
                });
                break;
            case "✅ Statut (En cours → Terminé)":
                result.sort((t1, t2) -> {
                    int s1 = getStatutOrdre(t1.getStatut().name());
                    int s2 = getStatutOrdre(t2.getStatut().name());
                    return Integer.compare(s1, s2);
                });
                break;
        }
        return result;
    }

    private int getPrioriteOrdre(String priorite) {
        switch (priorite) {
            case "HAUTE": return 1;
            case "MOYENNE": return 2;
            case "BASSE": return 3;
            default: return 4;
        }
    }

    private int getStatutOrdre(String statut) {
        switch (statut) {
            case "EN_COURS": return 1;
            case "TERMINE": return 2;
            case "SUSPENDU": return 3;
            default: return 4;
        }
    }

    private void appliquerFiltres() {
        if (tousLesTraitements == null) {
            return; // pas encore chargé
        }
        if (tousLesTraitements.isEmpty()) {
            cardsContainer.getChildren().clear();
            if (lblAucunResultat != null) {
                lblAucunResultat.setVisible(true);
                lblAucunResultat.setManaged(true);
                lblAucunResultat.setText("Aucun traitement trouvé.");
            }
            if (lblStatus != null) lblStatus.setText("0 traitement trouvé");
            return;
        }

        List<Traitement> traitementsFiltres = new ArrayList<>(tousLesTraitements);
        traitementsFiltres = appliquerRechercheEtFiltres(traitementsFiltres);
        traitementsFiltres = appliquerTri(traitementsFiltres);
        creerCartesTraitements(traitementsFiltres);

        long total = traitementsFiltres.size();
        if (lblStatus != null) lblStatus.setText(total + " traitement(s) trouvé(s)");
    }

    private void creerCartesTraitements(List<Traitement> traitements) {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        toutesLesCartes.clear();

        if (traitements.isEmpty()) {
            if (lblAucunResultat != null) {
                lblAucunResultat.setVisible(true);
                lblAucunResultat.setManaged(true);
                lblAucunResultat.setText("Aucun traitement ne correspond aux filtres.");
            }
            return;
        }

        // Il y a des traitements → cacher le message
        if (lblAucunResultat != null) {
            lblAucunResultat.setVisible(false);
            lblAucunResultat.setManaged(false);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Traitement traitement : traitements) {
            List<SuiviTraitement> suivis = tousLesSuivis.stream()
                    .filter(s -> s.getTraitementId() == traitement.getTraitementId())
                    .sorted((s1, s2) -> {
                        if (s1.getDateSuivi() == null || s2.getDateSuivi() == null) return 0;
                        return s2.getDateSuivi().compareTo(s1.getDateSuivi());
                    })
                    .collect(Collectors.toList());
            VBox carte = creerCarteTraitement(traitement, suivis, dateFormatter);
            toutesLesCartes.add(carte);
            cardsContainer.getChildren().add(carte);
        }
    }

    @FXML
    private void handleAppliquerFiltres() {
        appliquerFiltres();
    }

    @FXML
    private void handleReinitialiserFiltres() {
        txtRecherche.clear();
        cmbFiltreStatut.setValue("Tous les statuts");
        cmbFiltrePriorite.setValue("Toutes les priorités");
        cmbTri.setValue("📅 Date (plus récent)");
        appliquerFiltres();
    }

    private VBox creerCarteTraitement(Traitement traitement, List<SuiviTraitement> suivis, DateTimeFormatter dateFormatter) {
        VBox carte = new VBox();
        carte.setSpacing(12);
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        carte.setPadding(new Insets(16));

        // En-tête
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        Label statutBadge = new Label(traitement.getStatut().name());
        statutBadge.getStyleClass().add("status-badge");
        switch(traitement.getStatut().name()) {
            case "EN_COURS": statutBadge.getStyleClass().add("badge-EN_COURS"); break;
            case "TERMINE": statutBadge.getStyleClass().add("badge-TERMINE"); break;
            case "SUSPENDU": statutBadge.getStyleClass().add("badge-SUSPENDU"); break;
        }

        Label prioriteBadge = new Label(traitement.getPriorite().name());
        prioriteBadge.getStyleClass().add("status-badge");
        switch(traitement.getPriorite().name()) {
            case "HAUTE": prioriteBadge.getStyleClass().add("priority-HAUTE"); break;
            case "MOYENNE": prioriteBadge.getStyleClass().add("priority-MOYENNE"); break;
            case "BASSE": prioriteBadge.getStyleClass().add("priority-BASSE"); break;
        }

        // Indicateur de suivis
        HBox suiviIndicator = new HBox();
        suiviIndicator.setAlignment(Pos.CENTER);
        suiviIndicator.setSpacing(5);

        Label suiviIconLabel = new Label();
        Label suiviTextLabel = new Label();
        suiviTextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        if (suivis.isEmpty()) {
            suiviIconLabel.setText("📭");
            suiviTextLabel.setText("Aucun suivi");
            suiviTextLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: normal;");
            suiviIndicator.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 20; -fx-padding: 4 10;");
        } else {
            suiviIconLabel.setText("📋");
            suiviTextLabel.setText(suivis.size() + " suivi" + (suivis.size() > 1 ? "s" : ""));
            suiviTextLabel.setStyle("-fx-text-fill: #15803d;");
            suiviIndicator.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 20; -fx-padding: 4 10;");
        }

        suiviIndicator.getChildren().addAll(suiviIconLabel, suiviTextLabel);

        Label titreLabel = new Label(traitement.getTitre());
        titreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e1b4b;");
        HBox.setHgrow(titreLabel, Priority.ALWAYS);

        header.getChildren().addAll(statutBadge, prioriteBadge, titreLabel, suiviIndicator);

        // Détails
        VBox details = new VBox();
        details.setSpacing(8);
        details.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-padding: 12;");

        HBox typeCategorie = new HBox();
        typeCategorie.setSpacing(20);
        Label typeLabel = new Label("📋 Type: " + traitement.getType());
        typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        Label categorieLabel = new Label("🏷️ Catégorie: " + traitement.getCategorie());
        categorieLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        typeCategorie.getChildren().addAll(typeLabel, categorieLabel);

        HBox dates = new HBox();
        dates.setSpacing(20);
        String dateDebut = traitement.getDateDebut() != null ? traitement.getDateDebut().toLocalDate().format(dateFormatter) : "Non spécifiée";
        String dateFin = traitement.getDateFin() != null ? traitement.getDateFin().toLocalDate().format(dateFormatter) : "Non spécifiée";
        Label debutLabel = new Label("📅 Début: " + dateDebut);
        debutLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        Label finLabel = new Label("⏰ Fin prévue: " + dateFin);
        finLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        Label dureeLabel = new Label("⌛ Durée: " + traitement.getDureeJours() + " jours");
        dureeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");
        dates.getChildren().addAll(debutLabel, finLabel, dureeLabel);

        Label dosageLabel = new Label("💊 Dosage: " + (traitement.getDosage() != null && !traitement.getDosage().isEmpty() ? traitement.getDosage() : "Non spécifié"));
        dosageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");

        Text objectifText = new Text(traitement.getObjectifTherapeutique() != null && !traitement.getObjectifTherapeutique().isEmpty()
                ? traitement.getObjectifTherapeutique() : "Aucun objectif spécifié");
        objectifText.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        objectifText.setWrappingWidth(500);

        details.getChildren().addAll(typeCategorie, dates, dosageLabel, objectifText);

        // Section suivis
        VBox suivisSection = new VBox();
        suivisSection.setSpacing(10);

        if (!suivis.isEmpty()) {
            Label suivisTitle = new Label("📝 Historique des suivis (" + suivis.size() + ")");
            suivisTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4f46e5;");

            VBox suivisList = new VBox();
            suivisList.setSpacing(8);

            int maxSuivis = Math.min(3, suivis.size());
            for (int i = 0; i < maxSuivis; i++) {
                VBox suiviCard = creerCarteSuivi(suivis.get(i), dateFormatter);
                suivisList.getChildren().add(suiviCard);
            }

            if (suivis.size() > 3) {
                Label plusLabel = new Label("... et " + (suivis.size() - 3) + " autre(s) suivi(s)");
                plusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
                suivisList.getChildren().add(plusLabel);
            }

            suivisSection.getChildren().addAll(suivisTitle, suivisList);
        } else {
            Label aucunSuiviMsg = new Label("💡 Aucun suivi pour le moment. Cliquez sur le bouton ci-dessous pour ajouter votre premier suivi !");
            aucunSuiviMsg.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af; -fx-font-style: italic; -fx-padding: 10 0 5 0;");
            suivisSection.getChildren().add(aucunSuiviMsg);
        }

        // Boutons d'actions
        HBox actionsBox = new HBox();
        actionsBox.setSpacing(10);
        actionsBox.setAlignment(Pos.CENTER);

        Button btnAjouterSuivi = new Button("+ Ajouter un suivi");
        btnAjouterSuivi.getStyleClass().add("btn-primary-small");
        btnAjouterSuivi.setMaxWidth(Double.MAX_VALUE);
        btnAjouterSuivi.setOnAction(e -> ouvrirAjoutSuivi(traitement));

        Button btnExporterPDF = new Button("Exporter Ordonnance");
        btnExporterPDF.getStyleClass().add("btn-primary-small");
        btnExporterPDF.setOnAction(e -> handleExporterPDF(traitement));

        Button btnTraduire = new Button("Traduire");
        btnTraduire.getStyleClass().add("btn-secondary-small");
        btnTraduire.setOnAction(e -> ouvrirTraduction(traitement));

        actionsBox.getChildren().addAll(btnAjouterSuivi, btnExporterPDF, btnTraduire);

        carte.getChildren().addAll(header, details, suivisSection, actionsBox);

        return carte;
    }

    private VBox creerCarteSuivi(SuiviTraitement suivi, DateTimeFormatter dateFormatter) {
        VBox suiviCard = new VBox();
        suiviCard.setSpacing(8);
        suiviCard.setStyle("-fx-background-color: #fefce8; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #fde68a; -fx-border-radius: 8;");

        String dateSuivi = suivi.getDateSuivi() != null ? suivi.getDateSuivi().toLocalDate().format(dateFormatter) : "Date inconnue";
        Label dateLabel = new Label("📌 " + dateSuivi);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #92400e;");

        String saisiePar = suivi.getSaisiPar() == SaisiPar.PSYCHOLOGUE ? "👨‍⚕️ Psychologue" : "👨‍🎓 Vous";
        Label saisieLabel = new Label(saisiePar);
        saisieLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716c;");

        String observations = suivi.getObservations() != null && !suivi.getObservations().isEmpty()
                ? suivi.getObservations() : "Aucune observation";
        Label notesLabel = new Label(observations);
        notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        notesLabel.setWrapText(true);

        // Boutons Modifier/Supprimer
        HBox actionsBox = new HBox();
        actionsBox.setSpacing(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnModifier = new Button("Modifier");
        Button btnSupprimer = new Button("Supprimer");

        btnModifier.getStyleClass().addAll("table-action-button", "table-action-button-edit");
        btnSupprimer.getStyleClass().addAll("table-action-button", "table-action-button-delete");

        btnModifier.setPrefWidth(70);
        btnSupprimer.setPrefWidth(70);

        if (suivi.getSaisiPar() == SaisiPar.ETUDIANT) {
            btnModifier.setOnAction(e -> ouvrirModificationSuivi(suivi));
            btnSupprimer.setOnAction(e -> supprimerSuivi(suivi));
            actionsBox.getChildren().addAll(btnModifier, btnSupprimer);
        }

        suiviCard.getChildren().addAll(dateLabel, saisieLabel, notesLabel, actionsBox);

        return suiviCard;
    }

    private void ouvrirModificationSuivi(SuiviTraitement suivi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-modification-view.fxml"));
            Parent root = loader.load();

            SuiviTraitementModificationController controller = loader.getController();
            controller.setSuiviTraitement(suivi);

            Stage stage = new Stage();
            stage.setTitle("Modifier le suivi");
            stage.setScene(new Scene(root, 800, 650));
            stage.show();

            stage.setOnHiding(event -> chargerDonnees());

        } catch (Exception e) {
            e.printStackTrace();
            if (lblStatus != null) lblStatus.setText("✗ Erreur: Impossible d'ouvrir la page de modification");
            afficherToast("✗ Impossible d'ouvrir la page de modification", false);
        }
    }

    private void supprimerSuivi(SuiviTraitement suivi) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le suivi");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce suivi ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                suiviTraitementService.supprimer(suivi.getSuivitraitementId());
                chargerDonnees();
                afficherToast("✓ Suivi supprimé avec succès", true);
            } catch (SQLException e) {
                afficherToast("✗ Impossible de supprimer le suivi: " + e.getMessage(), false);
            }
        }
    }

    private void ouvrirAjoutSuivi(Traitement traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-ajout-view.fxml"));
            Parent root = loader.load();

            SuiviTraitementAjoutController controller = loader.getController();
            controller.setTraitementPreSelectionne(traitement);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un suivi - " + traitement.getTitre());
            stage.setScene(new Scene(root, 800, 650));
            stage.show();

            stage.setOnHiding(event -> {
                chargerDonnees();
                verifierEtNotifierNouveauSuivi(traitement);
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (lblStatus != null) lblStatus.setText("✗ Erreur: Impossible d'ouvrir la page d'ajout");
            afficherToast("✗ Impossible d'ouvrir la page d'ajout", false);
        }
    }

    // ==================== TOAST NOTIFICATION ====================

    private void afficherToast(String message, boolean success) {
        if (toastContainer == null) return;

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
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
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

    private void handleExporterPDF(Traitement traitement) {
        try {
            if (currentUser == null) {
                afficherToast("⚠️ Utilisateur non connecté", false);
                return;
            }

            // Récupérer le psychologue associé au traitement
            User psychologue = null;
            try {
                // Utiliser le service psychologue pour récupérer le psychologue par son ID
                org.example.services.PsychologueService psychologueService = new org.example.services.PsychologueService();
                psychologue = psychologueService.getPsychologueById(traitement.getPsychologueId());
            } catch (Exception e) {
                System.err.println("Impossible de récupérer le psychologue: " + e.getMessage());
            }

            // Créer le service PDF et générer l'ordonnance
            OrdonnancePDFService pdfService = new OrdonnancePDFService();
            byte[] pdfBytes = pdfService.genererOrdonnancePDF(traitement, psychologue, currentUser);

            // Choix du fichier de sauvegarde
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer l'ordonnance PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf")
            );

            String nomFichier = pdfService.genererNomFichier(traitement);
            fileChooser.setInitialFileName(nomFichier);

            File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());

            if (file != null) {
                // Sauvegarder le PDF
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(pdfBytes);
                }

                afficherToast("✓ Ordonnance exportée: " + file.getName(), true);
                if (lblStatus != null) {
                    lblStatus.setText("✓ Ordonnance exportée: " + file.getName());
                }
            }

        } catch (Exception e) {
            afficherToast(" Erreur d'export PDF: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private void ouvrirTraduction(Traitement traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-traduction-view.fxml"));
            Parent root = loader.load();

            TraitementTraductionController controller = loader.getController();
            controller.setTraitement(traitement);
            controller.setUtilisateur(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Traduction - " + traitement.getTitre());
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            if (lblStatus != null) lblStatus.setText("Erreur: Impossible d'ouvrir la page de traduction");
            afficherToast("Impossible d'ouvrir la page de traduction", false);
        }
    }

    /**
     * V\u00e9rifie si un nouveau suivi a \u00e9t\u00e9 ajout\u00e9 et envoie une notification au psychologue
     */
    private void verifierEtNotifierNouveauSuivi(Traitement traitement) {
        try {
            // R\u00e9cup\u00e9rer les suivis actuels pour ce traitement
            List<SuiviTraitement> suivisActuels = suiviTraitementService.afficher().stream()
                    .filter(s -> s.getTraitementId() == traitement.getTraitementId())
                    .sorted((s1, s2) -> {
                        if (s1.getDateSuivi() == null || s2.getDateSuivi() == null) return 0;
                        return s2.getDateSuivi().compareTo(s1.getDateSuivi());
                    })
                    .collect(Collectors.toList());

            if (!suivisActuels.isEmpty()) {
                SuiviTraitement dernierSuivi = suivisActuels.get(0);

                // V\u00e9rifier si le suivi a \u00e9t\u00e9 ajout\u00e9 aujourd'hui par l'\u00e9tudiant
                boolean estRecent = dernierSuivi.getDateSuivi() != null &&
                        dernierSuivi.getDateSuivi().toLocalDate().equals(LocalDate.now());
                boolean estEtudiant = dernierSuivi.getSaisiPar() == SaisiPar.ETUDIANT;

                if (estRecent && estEtudiant) {
                    // Envoyer la notification au psychologue
                    TraitementEmailService emailService = new TraitementEmailService();
                    var resultat = emailService.envoyerNotificationNouveauSuiviPsychologue(
                            dernierSuivi, traitement, currentUser);

                    if (resultat.isSuccess()) {
                        System.out.println(" Notification envoy\u00e9e au psychologue pour le nouveau suivi");
                    } else {
                        System.err.println(" Erreur envoi notification: " + resultat.getErrorMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la v\u00e9rification du nouveau suivi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}