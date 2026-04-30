package org.example.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.services.EtudiantTraitementService;
import org.example.services.OrdonnancePDFService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TraitementController implements Initializable, SidebarPsychologueController.PsyPageController {

    @FXML
    private TableView<LigneGroupée> tableViewTraitements;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblCount;
    @FXML
    private Label lblDate;
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
    private Button btnAjouter;
    @FXML
    private TableColumn<LigneGroupée, Void> colActions;

    // Statistiques
    @FXML private Label statTotal;
    @FXML private Label statEnCours;
    @FXML private Label statTermine;
    @FXML private Label statSuspendu;
    @FXML private Label statPrioriteHaute;

    // Sidebar et Toast
    @FXML private SidebarPsychologueController sidebarPsyController;
    @FXML private StackPane toastContainer;

    // ==================== PAGINATION ====================
    @FXML private ComboBox<String> cmbItemsPerPage;
    @FXML private Button btnFirstPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Button btnLastPage;
    @FXML private Label lblPageInfo;
    @FXML private Label lblTotalPagesInfo;

    private List<LigneGroupée> toutesLesLignes;
    private int currentPage = 0;
    private int itemsPerPage = 10;

    private TraitementService traitementService;
    private EtudiantTraitementService etudiantTraitementService;
    private SuiviTraitementService suiviTraitementService;
    private ObservableList<LigneGroupée> lignesGroupéesList;
    private Map<Integer, Integer> cacheNbSuivis;
    private List<Traitement> tousLesTraitementsFiltres;
    private boolean isInitialized = false;
    private User utilisateur;
    private Map<Integer, String> cacheNomsEtudiants;

    // ==================== CLASSE INTERNE ====================

    public static class LigneGroupée {
        private final String nomEtudiant;
        private final Traitement traitement;
        private final int indexDansGroupe;
        private final int tailleGroupe;
        private final boolean estLigneSeparateur;
        private final int nbSuivis;

        public LigneGroupée(String nomEtudiant, Traitement traitement, int indexDansGroupe, int tailleGroupe, int nbSuivis) {
            this.nomEtudiant = nomEtudiant;
            this.traitement = traitement;
            this.indexDansGroupe = indexDansGroupe;
            this.tailleGroupe = tailleGroupe;
            this.nbSuivis = nbSuivis;
            this.estLigneSeparateur = false;
        }

        public LigneGroupée() {
            this.nomEtudiant = null;
            this.traitement = null;
            this.indexDansGroupe = -1;
            this.tailleGroupe = 0;
            this.nbSuivis = 0;
            this.estLigneSeparateur = true;
        }

        public String getNomEtudiant() { return nomEtudiant; }
        public Traitement getTraitement() { return traitement; }
        public int getIndexDansGroupe() { return indexDansGroupe; }
        public int getTailleGroupe() { return tailleGroupe; }
        public int getNbSuivis() { return nbSuivis; }
        public boolean isEstLigneSeparateur() { return estLigneSeparateur; }
        public boolean isPremiereLigneDuGroupe() { return indexDansGroupe == 0; }
        public Traitement getPremierTraitement() { return traitement; }

        public String getTexteAffichageEtudiant() {
            if (estLigneSeparateur) return "";
            if (isPremiereLigneDuGroupe()) {
                return nomEtudiant + "\n(" + tailleGroupe + " traitement" + (tailleGroupe > 1 ? "s" : "") + ")";
            }
            return "";
        }

        public String getIndicateurSuivis() {
            if (estLigneSeparateur) return "";
            if (nbSuivis == 0) {
                return "📭 Aucun suivi";
            } else {
                return "📋 " + nbSuivis + " suivi" + (nbSuivis > 1 ? "s" : "");
            }
        }

        public String getStyleIndicateurSuivis() {
            if (estLigneSeparateur) return "";
            if (nbSuivis == 0) {
                return "-fx-background-color: #f3f4f6; -fx-text-fill: #9ca3af; -fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 11px;";
            } else {
                return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            }
        }
    }

    // ==================== INITIALISATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lblDate != null) {
            lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        SessionManager session = SessionManager.getInstance();

        if (!session.estConnecte()) {
            if (lblStatus != null) {
                lblStatus.setText("Erreur: utilisateur non connecté");
            }
            return;
        }

        if (session.estEtudiant()) {
            Platform.runLater(() -> ouvrirInterfaceEtudiant());
            return;
        }

        try {
            traitementService = new TraitementService();
            etudiantTraitementService = new EtudiantTraitementService();
            suiviTraitementService = new SuiviTraitementService();
            cacheNbSuivis = new HashMap<>();
            tousLesTraitementsFiltres = new ArrayList<>();
            cacheNomsEtudiants = new HashMap<>();
            toutesLesLignes = new ArrayList<>();

            initialiserFiltres();
            initialiserTri();
            initialiserPagination();
            configurerColonnes();

            isInitialized = true;
            if (lblStatus != null) {
                lblStatus.setText("Interface Traitements - Mode psychologue");
            }

            prechargerNomsEtudiants();
            chargerDonneesAvecSession();

        } catch (Exception e) {
            if (lblStatus != null) {
                lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;

        SessionManager session = SessionManager.getInstance();
        if (user != null) {
            if (!session.estConnecte()) {
                session.initSession(user);
            } else if (session.getCurrentUser() == null || session.getCurrentUser().getUserId() != user.getUserId()) {
                session.updateSession(user);
            }
        }

        ensureServicesAndCachesInitialized();

        if (!isInitialized) {
            Platform.runLater(() -> {
                try {
                    if (!isInitialized && tableViewTraitements != null) {
                        ensureServicesAndCachesInitialized();
                        initialiserFiltres();
                        initialiserTri();
                        initialiserPagination();
                        configurerColonnes();
                        isInitialized = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/traitement-view.fxml");
        }
        chargerDonnees();
    }

    private void ensureServicesAndCachesInitialized() {
        if (traitementService == null) {
            traitementService = new TraitementService();
        }
        if (etudiantTraitementService == null) {
            etudiantTraitementService = new EtudiantTraitementService();
        }
        if (suiviTraitementService == null) {
            suiviTraitementService = new SuiviTraitementService();
        }
        if (cacheNbSuivis == null) {
            cacheNbSuivis = new HashMap<>();
        }
        if (tousLesTraitementsFiltres == null) {
            tousLesTraitementsFiltres = new ArrayList<>();
        }
        if (cacheNomsEtudiants == null) {
            cacheNomsEtudiants = new HashMap<>();
        }
        if (toutesLesLignes == null) {
            toutesLesLignes = new ArrayList<>();
        }
    }

    private void prechargerNomsEtudiants() {
        try {
            List<Etudiant> etudiants = etudiantTraitementService.afficher();
            for (Etudiant e : etudiants) {
                cacheNomsEtudiants.put(e.getUserId(), e.getNom() + " " + e.getPrenom());
            }
            System.out.println("✅ " + cacheNomsEtudiants.size() + " étudiants chargés en cache");
        } catch (SQLException e) {
            System.err.println("Erreur chargement cache étudiants: " + e.getMessage());
        }
    }

    private void ouvrirInterfaceEtudiant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-etudiant-view.fxml"));
            Parent root = loader.load();
            Scene currentScene = tableViewTraitements.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== INITIALISATION FILTRES ET TRI ====================

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
                "✅ Statut (En cours → Terminé)",
                "👤 Étudiant (A-Z)"
        ));
        cmbTri.setValue("📅 Date (plus récent)");
    }

    // ==================== PAGINATION ====================

    private void initialiserPagination() {
        cmbItemsPerPage.getItems().addAll("10", "20", "50", "100");
        cmbItemsPerPage.setValue("10");

        cmbItemsPerPage.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                itemsPerPage = Integer.parseInt(newVal);
                currentPage = 0;
                appliquerPagination();
            }
        });
    }

    private void appliquerPagination() {
        if (toutesLesLignes == null || toutesLesLignes.isEmpty()) {
            tableViewTraitements.setItems(FXCollections.observableArrayList());
            lblPageInfo.setText("Page 0 / 0");
            lblTotalPagesInfo.setText("0 ligne(s)");
            btnFirstPage.setDisable(true);
            btnPrevPage.setDisable(true);
            btnNextPage.setDisable(true);
            btnLastPage.setDisable(true);
            return;
        }

        int totalPages = (int) Math.ceil((double) toutesLesLignes.size() / itemsPerPage);

        if (totalPages == 0) totalPages = 1;

        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int fromIndex = currentPage * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, toutesLesLignes.size());

        List<LigneGroupée> pageLignes = toutesLesLignes.subList(fromIndex, toIndex);
        lignesGroupéesList = FXCollections.observableArrayList(pageLignes);
        tableViewTraitements.setItems(lignesGroupéesList);

        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages);
        lblTotalPagesInfo.setText(toutesLesLignes.size() + " ligne(s)");

        btnFirstPage.setDisable(currentPage == 0);
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
        btnLastPage.setDisable(currentPage >= totalPages - 1);

        long totalTraitements = toutesLesLignes.stream().filter(l -> l.getTraitement() != null).count();
        lblCount.setText(totalTraitements + " traitement(s)");
    }

    @FXML
    private void handleFirstPage() {
        currentPage = 0;
        appliquerPagination();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            appliquerPagination();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) toutesLesLignes.size() / itemsPerPage);
        if (currentPage < totalPages - 1) {
            currentPage++;
            appliquerPagination();
        }
    }

    @FXML
    private void handleLastPage() {
        int totalPages = (int) Math.ceil((double) toutesLesLignes.size() / itemsPerPage);
        if (totalPages > 0) {
            currentPage = totalPages - 1;
            appliquerPagination();
        }
    }

    // ==================== CHARGEMENT DES DONNÉES ====================

    private void chargerDonnees() {
        if (utilisateur == null) {
            chargerDonneesAvecSession();
            return;
        }

        try {
            if (lblStatus != null) lblStatus.setText("Chargement en cours...");
            List<Traitement> traitements = traitementService.afficher();
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();

            cacheNbSuivis.clear();
            for (SuiviTraitement suivi : tousLesSuivis) {
                int traitementId = suivi.getTraitementId();
                cacheNbSuivis.put(traitementId, cacheNbSuivis.getOrDefault(traitementId, 0) + 1);
            }

            int utilisateurId = utilisateur.getUserId();
            List<Traitement> traitementsFiltres = new ArrayList<>();

            for (Traitement traitement : traitements) {
                if (traitement.getPsychologueId() == utilisateurId) {
                    traitementsFiltres.add(traitement);
                }
            }

            tousLesTraitementsFiltres = new ArrayList<>(traitementsFiltres);
            traitementsFiltres = appliquerRechercheEtFiltres(traitementsFiltres);
            traitementsFiltres = appliquerTri(traitementsFiltres);
            mettreAJourStatistiques(tousLesTraitementsFiltres);

            toutesLesLignes = creerLignesGroupées(traitementsFiltres);
            currentPage = 0;
            appliquerPagination();

            long totalTraitements = toutesLesLignes.stream().filter(l -> l.getTraitement() != null).count();
            if (lblStatus != null) lblStatus.setText(totalTraitements + " traitement(s) affiché(s)");

            System.out.println("✅ Chargement terminé: " + totalTraitements + " traitements affichés");

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            if (lblStatus != null) lblStatus.setText("Erreur: " + e.getMessage());
            afficherToast("✗ Erreur de chargement: " + e.getMessage(), false);
        }
    }

    private void chargerDonneesAvecSession() {
        SessionManager session = SessionManager.getInstance();

        if (!session.estConnecte()) {
            if (lblStatus != null) lblStatus.setText("Erreur: utilisateur non connecté");
            return;
        }

        if (session.estEtudiant()) {
            Platform.runLater(() -> ouvrirInterfaceEtudiant());
            return;
        }

        try {
            if (lblStatus != null) lblStatus.setText("Chargement en cours...");
            List<Traitement> traitements = traitementService.afficher();
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();

            cacheNbSuivis.clear();
            for (SuiviTraitement suivi : tousLesSuivis) {
                int traitementId = suivi.getTraitementId();
                cacheNbSuivis.put(traitementId, cacheNbSuivis.getOrDefault(traitementId, 0) + 1);
            }

            int utilisateurId = session.getUtilisateurConnecteId();
            List<Traitement> traitementsFiltres = new ArrayList<>();

            for (Traitement traitement : traitements) {
                if (traitement.getPsychologueId() == utilisateurId) {
                    traitementsFiltres.add(traitement);
                }
            }

            tousLesTraitementsFiltres = new ArrayList<>(traitementsFiltres);
            traitementsFiltres = appliquerRechercheEtFiltres(traitementsFiltres);
            traitementsFiltres = appliquerTri(traitementsFiltres);
            mettreAJourStatistiques(tousLesTraitementsFiltres);

            toutesLesLignes = creerLignesGroupées(traitementsFiltres);
            currentPage = 0;
            appliquerPagination();

            long totalTraitements = toutesLesLignes.stream().filter(l -> l.getTraitement() != null).count();
            if (lblStatus != null) lblStatus.setText(totalTraitements + " traitement(s) affiché(s)");

            System.out.println("✅ Chargement terminé: " + totalTraitements + " traitements affichés");

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            if (lblStatus != null) lblStatus.setText("Erreur: " + e.getMessage());
            afficherToast("✗ Erreur de chargement: " + e.getMessage(), false);
        }
    }

    private List<Traitement> appliquerRechercheEtFiltres(List<Traitement> traitements) {
        if (txtRecherche == null || cmbFiltreStatut == null || cmbFiltrePriorite == null) {
            return traitements;
        }

        String recherche = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase().trim() : "";
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
                    if (statutFiltre != null && !"Tous les statuts".equals(statutFiltre)) {
                        if (t.getStatut() == null || !t.getStatut().name().equals(statutFiltre)) return false;
                    }
                    if (prioriteFiltre != null && !"Toutes les priorités".equals(prioriteFiltre)) {
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
            case "👤 Étudiant (A-Z)":
                result.sort((t1, t2) -> {
                    String nom1 = getNomEtudiant(t1.getEtudiantId());
                    String nom2 = getNomEtudiant(t2.getEtudiantId());
                    return nom1.compareToIgnoreCase(nom2);
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

    private List<LigneGroupée> creerLignesGroupées(List<Traitement> traitements) throws SQLException {
        List<LigneGroupée> lignes = new ArrayList<>();

        Map<Integer, List<Traitement>> traitementsParEtudiant = traitements.stream()
                .collect(Collectors.groupingBy(Traitement::getEtudiantId));

        boolean premierGroupe = true;

        for (Map.Entry<Integer, List<Traitement>> entry : traitementsParEtudiant.entrySet()) {
            Integer etudiantId = entry.getKey();
            List<Traitement> traitementsEtudiant = entry.getValue();
            String nomEtudiant = getNomEtudiant(etudiantId);
            int tailleGroupe = traitementsEtudiant.size();

            if (!premierGroupe) {
                lignes.add(new LigneGroupée());
            }
            premierGroupe = false;

            for (int i = 0; i < tailleGroupe; i++) {
                Traitement traitement = traitementsEtudiant.get(i);
                int nbSuivis = cacheNbSuivis.getOrDefault(traitement.getTraitementId(), 0);
                LigneGroupée ligne = new LigneGroupée(nomEtudiant, traitement, i, tailleGroupe, nbSuivis);
                lignes.add(ligne);
            }
        }
        return lignes;
    }

    private String getNomEtudiant(Integer etudiantId) {
        if (etudiantId == null || etudiantId == 0) return "Non assigné";

        if (cacheNomsEtudiants != null && cacheNomsEtudiants.containsKey(etudiantId)) {
            return cacheNomsEtudiants.get(etudiantId);
        }

        try {
            Etudiant etudiant = etudiantTraitementService.trouverParId(etudiantId);
            if (etudiant != null) {
                String nomComplet = etudiant.getNom() + " " + etudiant.getPrenom();
                if (cacheNomsEtudiants != null) {
                    cacheNomsEtudiants.put(etudiantId, nomComplet);
                }
                return nomComplet;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getNomEtudiant pour ID " + etudiantId + ": " + e.getMessage());
        }

        return "Étudiant #" + etudiantId;
    }

    // ==================== CONFIGURATION DES COLONNES ====================

    @SuppressWarnings("unchecked")
    private void configurerColonnes() {
        tableViewTraitements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<LigneGroupée, String> colEtudiant = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(0);
        TableColumn<LigneGroupée, String> colTitre = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(1);
        TableColumn<LigneGroupée, String> colType = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(2);
        TableColumn<LigneGroupée, String> colCategorie = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(3);
        TableColumn<LigneGroupée, String> colDuree = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(4);
        TableColumn<LigneGroupée, String> colStatut = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(5);
        TableColumn<LigneGroupée, String> colPriorite = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(6);
        TableColumn<LigneGroupée, String> colDateDebut = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(7);
        TableColumn<LigneGroupée, String> colObjectif = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(8);
        TableColumn<LigneGroupée, String> colSuivis = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(9);

        colEtudiant.setPrefWidth(180);
        colTitre.setPrefWidth(180);
        colType.setPrefWidth(120);
        colCategorie.setPrefWidth(100);
        colDuree.setPrefWidth(60);
        colStatut.setPrefWidth(90);
        colPriorite.setPrefWidth(90);
        colDateDebut.setPrefWidth(100);
        colObjectif.setPrefWidth(200);
        colSuivis.setPrefWidth(100);

        // ===== COLONNE ÉTUDIANT (avec VBox stylisé) =====
        colEtudiant.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            if (ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            return new javafx.beans.property.SimpleStringProperty(ligne.getTexteAffichageEtudiant());
        });

        colEtudiant.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                    setGraphic(null);
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                        setGraphic(null);
                    } else if (ligne.isPremiereLigneDuGroupe()) {
                        VBox vbox = new VBox();
                        vbox.setAlignment(Pos.CENTER);
                        vbox.setSpacing(4);

                        Label nomLabel = new Label(ligne.getNomEtudiant());
                        nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #4f46e5;");
                        nomLabel.setAlignment(Pos.CENTER);

                        Label countLabel = new Label("(" + ligne.getTailleGroupe() + " traitement" + (ligne.getTailleGroupe() > 1 ? "s" : "") + ")");
                        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
                        countLabel.setAlignment(Pos.CENTER);

                        vbox.getChildren().addAll(nomLabel, countLabel);
                        setGraphic(vbox);
                        setText(null);
                        setStyle("-fx-background-color: #f5f3ff; -fx-padding: 12 8;");
                        setAlignment(Pos.CENTER);
                    } else {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: #f5f3ff; -fx-padding: 12 8;");
                    }
                }
            }
        });

        // ===== COLONNE TITRE =====
        colTitre.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(t.getTitre());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colTitre.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE TYPE =====
        colType.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(t.getType());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colType.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE CATÉGORIE =====
        colCategorie.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(t.getCategorie().name());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colCategorie.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE DURÉE =====
        colDuree.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(String.valueOf(t.getDureeJours()));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colDuree.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else {
                        setText(item + "j");
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE STATUT (avec badge) =====
        colStatut.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(t.getStatut().name());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colStatut.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else if (item != null && !item.isEmpty()) {
                        Label badge = new Label(item.equals("EN_COURS") ? "En cours" : item.equals("TERMINE") ? "Terminé" : "Suspendu");
                        badge.getStyleClass().add("status-badge");
                        switch(item) {
                            case "EN_COURS": badge.getStyleClass().add("badge-EN_COURS"); break;
                            case "TERMINE": badge.getStyleClass().add("badge-TERMINE"); break;
                            case "SUSPENDU": badge.getStyleClass().add("badge-SUSPENDU"); break;
                        }
                        setGraphic(badge);
                        setText(null);
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-background-color: white; -fx-padding: 8px;");
                    } else {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE PRIORITÉ (avec badge) =====
        colPriorite.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(t.getPriorite().name());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colPriorite.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else if (item != null && !item.isEmpty()) {
                        Label badge = new Label(item.equals("HAUTE") ? "Haute" : item.equals("MOYENNE") ? "Moyenne" : "Basse");
                        badge.getStyleClass().add("status-badge");
                        switch(item) {
                            case "HAUTE": badge.getStyleClass().add("priority-HAUTE"); break;
                            case "MOYENNE": badge.getStyleClass().add("priority-MOYENNE"); break;
                            case "BASSE": badge.getStyleClass().add("priority-BASSE"); break;
                        }
                        setGraphic(badge);
                        setText(null);
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-background-color: white; -fx-padding: 8px;");
                    } else {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE DATE DÉBUT =====
        colDateDebut.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur() && t.getDateDebut() != null) {
                return new javafx.beans.property.SimpleStringProperty(t.getDateDebut().toString());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colDateDebut.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE OBJECTIF =====
        colObjectif.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                String obj = t.getObjectifTherapeutique();
                if (obj != null && obj.length() > 50) {
                    obj = obj.substring(0, 47) + "...";
                }
                return new javafx.beans.property.SimpleStringProperty(obj != null ? obj : "");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colObjectif.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // ===== COLONNE SUIVIS (avec badge) =====
        colSuivis.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            if (ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            return new javafx.beans.property.SimpleStringProperty(ligne.getIndicateurSuivis());
        });

        colSuivis.setCellFactory(param -> new TableCell<LigneGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else if (item != null && !item.isEmpty()) {
                        Label indicateur = new Label(item);
                        indicateur.setStyle(ligne.getStyleIndicateurSuivis());
                        setGraphic(indicateur);
                        setText(null);
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-background-color: white; -fx-padding: 8px;");
                    } else {
                        setText("");
                        setGraphic(null);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        configurerColonneActions();
    }

    private void configurerColonneActions() {
        SessionManager session = SessionManager.getInstance();

        colActions.setMinWidth(220);
        colActions.setPrefWidth(220);
        colActions.setMaxWidth(220);
        colActions.setResizable(false);

        colActions.setCellFactory(param -> new TableCell<LigneGroupée, Void>() {
            private final Button btnView = new Button("👁");
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final Button btnTranslate = new Button("🌐");
            private final HBox container = new HBox(6, btnView, btnEdit, btnDelete, btnTranslate);

            {
                container.setAlignment(Pos.CENTER);
                btnView.getStyleClass().addAll("table-action-button", "table-action-button-view");
                btnEdit.getStyleClass().addAll("table-action-button", "table-action-button-edit");
                btnDelete.getStyleClass().addAll("table-action-button", "table-action-button-delete");
                btnTranslate.getStyleClass().addAll("table-action-button", "table-action-button-translate");

                String compactStyle = "-fx-font-size: 11px; -fx-padding: 3 6;";
                btnView.setStyle(compactStyle);
                btnEdit.setStyle(compactStyle);
                btnDelete.setStyle(compactStyle);
                btnTranslate.setStyle(compactStyle);

                btnView.setTextOverrun(OverrunStyle.CLIP);
                btnEdit.setTextOverrun(OverrunStyle.CLIP);
                btnDelete.setTextOverrun(OverrunStyle.CLIP);
                btnTranslate.setTextOverrun(OverrunStyle.CLIP);

                btnView.setEllipsisString("");
                btnEdit.setEllipsisString("");
                btnDelete.setEllipsisString("");
                btnTranslate.setEllipsisString("");

                btnView.setMinWidth(48);
                btnEdit.setMinWidth(48);
                btnDelete.setMinWidth(48);
                btnTranslate.setMinWidth(48);

                btnView.setPrefWidth(48);
                btnEdit.setPrefWidth(48);
                btnDelete.setPrefWidth(48);
                btnTranslate.setPrefWidth(48);

                btnView.setMaxWidth(Double.MAX_VALUE);
                btnEdit.setMaxWidth(Double.MAX_VALUE);
                btnDelete.setMaxWidth(Double.MAX_VALUE);
                btnTranslate.setMaxWidth(Double.MAX_VALUE);

                btnView.setFocusTraversable(false);
                btnEdit.setFocusTraversable(false);
                btnDelete.setFocusTraversable(false);
                btnTranslate.setFocusTraversable(false);

                btnView.setOnAction(event -> {
                    LigneGroupée ligne = getTableRow() != null ? getTableRow().getItem() : null;
                    Traitement traitement = ligne != null ? ligne.getPremierTraitement() : null;
                    if (traitement != null) {
                        ouvrirPageAffichage(traitement);
                    }
                });

                btnEdit.setOnAction(event -> {
                    LigneGroupée ligne = getTableRow() != null ? getTableRow().getItem() : null;
                    Traitement traitement = ligne != null ? ligne.getPremierTraitement() : null;
                    if (traitement == null) return;
                    if (!session.peutModifierTraitement()) {
                        afficherToast("⚠️ Accès refusé : modification non autorisée", false);
                        return;
                    }
                    ouvrirPageModification(traitement);
                });

                btnDelete.setOnAction(event -> {
                    LigneGroupée ligne = getTableRow() != null ? getTableRow().getItem() : null;
                    Traitement traitement = ligne != null ? ligne.getPremierTraitement() : null;
                    if (traitement == null) return;
                    if (!session.peutSupprimerTraitement()) {
                        afficherToast("⚠️ Accès refusé : suppression non autorisée", false);
                        return;
                    }
                    supprimerTraitement(traitement);
                });

                btnTranslate.setOnAction(event -> {
                    LigneGroupée ligne = getTableRow() != null ? getTableRow().getItem() : null;
                    Traitement traitement = ligne != null ? ligne.getPremierTraitement() : null;
                    if (traitement != null) {
                        ouvrirPageTraductionPourTraitement(traitement);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    LigneGroupée ligne = getTableRow().getItem();
                    if (ligne.isEstLigneSeparateur()) {
                        setGraphic(null);
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else if (ligne.getTraitement() != null) {
                        boolean autoriseModif = session.peutModifierTraitement();
                        boolean autoriseSupp = session.peutSupprimerTraitement();
                        btnEdit.setDisable(!autoriseModif);
                        btnDelete.setDisable(!autoriseSupp);
                        setGraphic(container);
                        setStyle("-fx-background-color: white; -fx-padding: 8px;");
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    // ==================== ACTIONS ====================

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

    private void appliquerFiltres() {
        if (tousLesTraitementsFiltres == null || tousLesTraitementsFiltres.isEmpty()) {
            toutesLesLignes = new ArrayList<>();
            appliquerPagination();
            return;
        }

        try {
            List<Traitement> traitementsFiltres = new ArrayList<>(tousLesTraitementsFiltres);
            traitementsFiltres = appliquerRechercheEtFiltres(traitementsFiltres);
            traitementsFiltres = appliquerTri(traitementsFiltres);

            toutesLesLignes = creerLignesGroupées(traitementsFiltres);
            currentPage = 0;
            appliquerPagination();

            if (lblStatus != null) lblStatus.setText(toutesLesLignes.stream().filter(l -> l.getTraitement() != null).count() + " traitement(s) après filtrage");

        } catch (SQLException e) {
            afficherToast("✗ Erreur: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleExporterExcel() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter les traitements");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );
            fileChooser.setInitialFileName("traitements_" + java.time.LocalDate.now() + ".csv");

            File file = fileChooser.showSaveDialog(tableViewTraitements.getScene().getWindow());

            if (file != null) {
                exporterVersCSV(file);
                if (lblStatus != null) lblStatus.setText("✓ Export réussi: " + file.getName());
                afficherToast("✓ Export réussi: " + file.getName(), true);
            }
        } catch (Exception e) {
            afficherToast("✗ Erreur d'export: " + e.getMessage(), false);
        }
    }

    private void exporterVersCSV(File file) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            writer.write('\uFEFF');
            writer.write("Étudiant;Titre;Type;Catégorie;Durée;Statut;Priorité;Date Début;Objectif;Suivis\n");

            for (LigneGroupée ligne : toutesLesLignes) {
                if (ligne.getTraitement() != null) {
                    Traitement t = ligne.getTraitement();
                    writer.write(String.format("%s;%s;%s;%s;%d;%s;%s;%s;%s;%d\n",
                            ligne.getNomEtudiant(),
                            t.getTitre().replace(";", ","),
                            t.getType().replace(";", ","),
                            t.getCategorie().name(),
                            t.getDureeJours(),
                            t.getStatut().name(),
                            t.getPriorite().name(),
                            t.getDateDebut() != null ? t.getDateDebut().toString() : "",
                            t.getObjectifTherapeutique() != null ? t.getObjectifTherapeutique().replace(";", ",") : "",
                            ligne.getNbSuivis()
                    ));
                }
            }
        }
    }

    @FXML
    private void handleExporterPDF() {
        LigneGroupée selectedLigne = tableViewTraitements.getSelectionModel().getSelectedItem();

        if (selectedLigne == null || selectedLigne.getTraitement() == null) {
            afficherToast("⚠️ Veuillez sélectionner un traitement à exporter", false);
            return;
        }

        try {
            Traitement traitement = selectedLigne.getTraitement();

            if (utilisateur == null) {
                afficherToast("⚠️ Utilisateur non connecté", false);
                return;
            }

            OrdonnancePDFService pdfService = new OrdonnancePDFService();
            byte[] pdfBytes = pdfService.genererOrdonnancePDF(traitement, utilisateur);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer l'ordonnance PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf")
            );

            String nomFichier = pdfService.genererNomFichier(traitement);
            fileChooser.setInitialFileName(nomFichier);

            File file = fileChooser.showSaveDialog(tableViewTraitements.getScene().getWindow());

            if (file != null) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(pdfBytes);
                }

                afficherToast("✓ Ordonnance exportée: " + file.getName(), true);
                if (lblStatus != null) {
                    lblStatus.setText("✓ Ordonnance exportée: " + file.getName());
                }
            }

        } catch (Exception e) {
            afficherToast("✗ Erreur d'export PDF: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ==================== NAVIGATION ====================

    @FXML private void handleAjouter() { ouvrirPageAjout(); }

    @FXML private void handleRafraichir() {
        prechargerNomsEtudiants();
        if (utilisateur != null) {
            chargerDonnees();
        } else {
            chargerDonneesAvecSession();
        }
    }

    @FXML private void ouvrirSuiviTraitementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-view.fxml"));
            Parent root = loader.load();

            SuiviTraitementController controller = loader.getController();
            if (controller != null) {
                User userToPass = utilisateur;
                if (userToPass == null) {
                    SessionManager session = SessionManager.getInstance();
                    if (session.estConnecte()) {
                        userToPass = session.getCurrentUser();
                    }
                }
                if (userToPass != null) {
                    controller.setUtilisateur(userToPass);
                }
            }

            Scene currentScene = tableViewTraitements.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root);
            }
        } catch (Exception e) {
            afficherToast("✗ Erreur de navigation: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleStatistiques() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-statistiques-view.fxml"));
            Parent root = loader.load();

            TraitementStatistiquesController controller = loader.getController();
            if (controller != null) {
                User userToPass = utilisateur;
                if (userToPass == null) {
                    SessionManager session = SessionManager.getInstance();
                    if (session.estConnecte()) {
                        userToPass = session.getCurrentUser();
                    }
                }
                if (userToPass != null) {
                    controller.setUtilisateur(userToPass);
                }
            }

            Scene currentScene = tableViewTraitements.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root);
            }
        } catch (Exception e) {
            afficherToast("✗ Erreur: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private void ouvrirPageAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-ajout-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Traitement");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
            stage.setOnHiding(event -> {
                prechargerNomsEtudiants();
                if (utilisateur != null) {
                    chargerDonnees();
                } else {
                    chargerDonneesAvecSession();
                }
            });
        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
        }
    }

    private void ouvrirPageAffichage(Traitement traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-affichage-view.fxml"));
            Parent root = loader.load();
            TraitementAffichageController controller = loader.getController();
            controller.setTraitement(traitement);
            Stage stage = new Stage();
            stage.setTitle("Détails du Traitement");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
        }
    }

    private void ouvrirPageModification(Traitement traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-modification-view.fxml"));
            Parent root = loader.load();
            TraitementModificationController controller = loader.getController();
            controller.setTraitement(traitement);
            Stage stage = new Stage();
            stage.setTitle("Modifier un Traitement");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
            stage.setOnHiding(event -> {
                prechargerNomsEtudiants();
                if (utilisateur != null) {
                    chargerDonnees();
                } else {
                    chargerDonneesAvecSession();
                }
            });
        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
        }
    }

    private void supprimerTraitement(Traitement traitement) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le traitement");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer le traitement \"" + traitement.getTitre() + "\" ?\n\n⚠️ Tous les suivis associés seront également supprimés !");
        if (confirm.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            try {
                traitementService.supprimer(traitement.getTraitementId());
                prechargerNomsEtudiants();
                if (utilisateur != null) {
                    chargerDonnees();
                } else {
                    chargerDonneesAvecSession();
                }
                afficherToast("✓ Traitement et ses suivis supprimés avec succès", true);
            } catch (Exception e) {
                afficherToast("✗ Erreur de suppression: " + e.getMessage(), false);
            }
        }
    }

    private void ouvrirPageTraductionPourTraitement(Traitement traitement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-traduction-view.fxml"));
            Parent root = loader.load();

            TraitementTraductionController controller = loader.getController();
            if (controller != null) {
                controller.setTraitement(traitement);
                controller.setUtilisateur(utilisateur);
            }

            Stage stage = new Stage();
            stage.setTitle("Traduction de Traitement");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
            e.printStackTrace();
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
}