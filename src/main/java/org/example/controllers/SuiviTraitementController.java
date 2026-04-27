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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.enums.SaisiPar;
import org.example.services.EtudiantTraitementService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.animation.FadeTransition;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.collections.transformation.FilteredList;
import org.example.services.TraitementIAService;
import java.util.stream.Collectors;

public class SuiviTraitementController implements Initializable, SidebarPsychologueController.PsyPageController {

    // ==================== CLASSE INTERNE ====================

    public static class LigneSuiviGroupée {
        private final String nomEtudiant;
        private final String nomTraitement;
        private final List<SuiviTraitement> suivis;
        private final boolean estEnteteEtudiant;
        private final boolean estEnteteTraitement;
        private final SuiviTraitement suivi;

        public LigneSuiviGroupée(String nomEtudiant, List<SuiviTraitement> suivis) {
            this.nomEtudiant = nomEtudiant;
            this.nomTraitement = null;
            this.suivis = suivis;
            this.suivi = null;
            this.estEnteteEtudiant = true;
            this.estEnteteTraitement = false;
        }

        public LigneSuiviGroupée(String nomEtudiant, String nomTraitement, List<SuiviTraitement> suivis) {
            this.nomEtudiant = nomEtudiant;
            this.nomTraitement = nomTraitement;
            this.suivis = suivis;
            this.suivi = null;
            this.estEnteteEtudiant = false;
            this.estEnteteTraitement = true;
        }

        public LigneSuiviGroupée(SuiviTraitement suivi) {
            this.nomEtudiant = null;
            this.nomTraitement = null;
            this.suivis = null;
            this.suivi = suivi;
            this.estEnteteEtudiant = false;
            this.estEnteteTraitement = false;
        }

        public String getNomEtudiant() { return nomEtudiant; }
        public String getNomTraitement() { return nomTraitement; }
        public List<SuiviTraitement> getSuivis() { return suivis; }
        public SuiviTraitement getSuivi() { return suivi; }
        public boolean estEnteteEtudiant() { return estEnteteEtudiant; }
        public boolean estEnteteTraitement() { return estEnteteTraitement; }
        public boolean estEntete() { return estEnteteEtudiant || estEnteteTraitement; }

        public SuiviTraitement getPremierSuivi() {
            if (suivi != null) return suivi;
            if (suivis != null && !suivis.isEmpty()) return suivis.get(0);
            return null;
        }

        public String getAffichage() {
            if (estEnteteEtudiant) {
                return "📚 " + nomEtudiant + " (" + getTotalSuivisEtudiant() + " suivi(s))";
            } else if (estEnteteTraitement) {
                return "  └ 📋 " + nomTraitement + " (" + suivis.size() + " suivi(s))";
            } else {
                return "";
            }
        }

        private int getTotalSuivisEtudiant() {
            return suivis != null ? suivis.size() : 0;
        }
    }

    // ==================== COMPOSANTS FXML ====================

    @FXML
    private TableView<LigneSuiviGroupée> tableViewSuiviTraitements;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblCount;
    @FXML
    private Label lblDate;
    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cmbFiltrePeriode;
    @FXML
    private ComboBox<String> cmbFiltreSaisiPar;
    @FXML
    private ComboBox<String> cmbTri;
    @FXML
    private Button btnAppliquerFiltres;
    @FXML
    private Button btnReinitialiserFiltres;
    @FXML
    private Button btnAjouter;
    @FXML
    private TableColumn<LigneSuiviGroupée, Void> colActions;

    // ===== STATISTIQUES =====
    @FXML private Label statTotal;
    @FXML private Label statCeMois;
    @FXML private Label statCetteSemaine;
    @FXML private Label statAujourdhui;
    @FXML private Label statParEtudiant;

    // Sidebar et Toast
    @FXML private SidebarPsychologueController sidebarPsyController;
    @FXML private StackPane toastContainer;

    // ==================== PAGINATION ====================
    @FXML private ComboBox<Integer> cmbItemsPerPage;
    @FXML private Button btnFirstPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Button btnLastPage;
    @FXML private Label lblPageInfo;
    @FXML private Label lblTotalPagesInfo;

    private List<LigneSuiviGroupée> toutesLesLignes;
    private int currentPage = 0;
    private int itemsPerPage = 10;

    /**
     * Initialise la pagination
     */
    private void initialiserPagination() {
        cmbItemsPerPage.setValue(10);
        cmbItemsPerPage.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                itemsPerPage = newVal;
                currentPage = 0;
                appliquerPagination();
            }
        });
    }

    /**
     * Applique la pagination sur les données
     */
    private void appliquerPagination() {
        if (toutesLesLignes == null || toutesLesLignes.isEmpty()) {
            tableViewSuiviTraitements.setItems(FXCollections.observableArrayList());
            lblPageInfo.setText("Page 0 / 0");
            lblTotalPagesInfo.setText("0 ligne(s)");
            btnFirstPage.setDisable(true);
            btnPrevPage.setDisable(true);
            btnNextPage.setDisable(true);
            btnLastPage.setDisable(true);
            return;
        }

        int totalPages = (int) Math.ceil((double) toutesLesLignes.size() / itemsPerPage);

        // Ajuster la page courante si elle dépasse
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int fromIndex = currentPage * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, toutesLesLignes.size());

        List<LigneSuiviGroupée> pageLignes = toutesLesLignes.subList(fromIndex, toIndex);
        lignesSuivisGroupéesList = FXCollections.observableArrayList(pageLignes);
        tableViewSuiviTraitements.setItems(lignesSuivisGroupéesList);

        // Mettre à jour les infos de pagination
        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages);
        lblTotalPagesInfo.setText(toutesLesLignes.size() + " ligne(s)");

        // Gérer l'état des boutons
        btnFirstPage.setDisable(currentPage == 0);
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
        btnLastPage.setDisable(currentPage >= totalPages - 1);

        // Mettre à jour le compteur
        long totalSuivis = toutesLesLignes.stream().filter(l -> !l.estEntete()).count();
        lblCount.setText(totalSuivis + " suivi(s)");
    }

    /**
     * Va à la première page
     */
    @FXML
    private void handleFirstPage() {
        currentPage = 0;
        appliquerPagination();
    }

    /**
     * Va à la page précédente
     */
    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            appliquerPagination();
        }
    }

    /**
     * Va à la page suivante
     */
    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) toutesLesLignes.size() / itemsPerPage);
        if (currentPage < totalPages - 1) {
            currentPage++;
            appliquerPagination();
        }
    }

    /**
     * Va à la dernière page
     */
    @FXML
    private void handleLastPage() {
        int totalPages = (int) Math.ceil((double) toutesLesLignes.size() / itemsPerPage);
        if (totalPages > 0) {
            currentPage = totalPages - 1;
            appliquerPagination();
        }
    }

    // ==================== SERVICES ====================

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantTraitementService etudiantTraitementService;
    private ObservableList<LigneSuiviGroupée> lignesSuivisGroupéesList;
    private List<SuiviTraitement> tousLesSuivisFiltres;
    private List<Traitement> tousLesTraitements;
    private boolean isInitialized = false;
    private User utilisateur;

    // ==================== INITIALISATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantTraitementService = new EtudiantTraitementService();
            tousLesSuivisFiltres = new ArrayList<>();
            toutesLesLignes = new ArrayList<>();

            initialiserFiltres();
            initialiserTri();
            initialiserPagination();
            configurerColonnes();

            isInitialized = true;
            lblStatus.setText("Interface Suivis Traitements prête");

        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/suivi-traitement-view.fxml");
        }
        chargerDonnees();
    }

    // ==================== INITIALISATION FILTRES ET TRI ====================

    private void initialiserFiltres() {
        ObservableList<String> periodes = FXCollections.observableArrayList(
                "Toutes les périodes", "Aujourd'hui", "Cette semaine", "Ce mois"
        );
        cmbFiltrePeriode.setItems(periodes);
        cmbFiltrePeriode.setValue("Toutes les périodes");

        ObservableList<String> saisiPar = FXCollections.observableArrayList(
                "Tous", "Psychologue", "Étudiant"
        );
        cmbFiltreSaisiPar.setItems(saisiPar);
        cmbFiltreSaisiPar.setValue("Tous");

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
        cmbFiltrePeriode.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
        cmbFiltreSaisiPar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
        cmbTri.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitialized) appliquerFiltres();
        });
    }

    private void initialiserTri() {
        cmbTri.setItems(FXCollections.observableArrayList(
                "📅 Date (plus récente)",
                "📅 Date (plus ancienne)",
                "👤 Étudiant (A-Z)",
                "👤 Étudiant (Z-A)",
                "📋 Traitement (A-Z)",
                "👨‍⚕️ Saisi par (Psychologue → Étudiant)"
        ));
        cmbTri.setValue("📅 Date (plus récente)");
    }

    // ==================== CHARGEMENT DES DONNÉES ====================

    private void chargerDonnees() {
        if (utilisateur == null) {
            lblStatus.setText("✗ Erreur: utilisateur non connecté");
            return;
        }

        try {
            lblStatus.setText("Chargement en cours...");
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            tousLesTraitements = traitementService.afficher();
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            int utilisateurId = utilisateur.getUserId();
            List<SuiviTraitement> suivisFiltres = new ArrayList<>();

            for (SuiviTraitement suivi : tousLesSuivis) {
                Traitement traitementAssocie = traitementsMap.get(suivi.getTraitementId());
                if (traitementAssocie == null) continue;

                int psychologueId = traitementAssocie.getPsychologueId();

                boolean peutVoir = false;

                if (utilisateur.getRole().equals("PSYCHOLOGUE")) {
                    peutVoir = (psychologueId == utilisateurId);
                } else if (utilisateur.getRole().equals("ETUDIANT")) {
                    int etudiantId = traitementAssocie.getEtudiantId();
                    peutVoir = (etudiantId == utilisateurId);
                } else {
                    peutVoir = true;
                }

                if (peutVoir) {
                    suivisFiltres.add(suivi);
                }
            }

            tousLesSuivisFiltres = new ArrayList<>(suivisFiltres);

            // Mettre à jour les statistiques
            mettreAJourStatistiques(tousLesSuivisFiltres, traitementsMap);

            // Appliquer filtres
            suivisFiltres = appliquerRechercheEtFiltres(suivisFiltres, traitementsMap);

            // Appliquer tri
            suivisFiltres = appliquerTri(suivisFiltres, traitementsMap);

            // MODIFICATION ICI : Stocker toutes les lignes pour la pagination
            toutesLesLignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);
            currentPage = 0;
            appliquerPagination();

            long totalSuivis = toutesLesLignes.stream().filter(l -> !l.estEntete()).count();
            lblStatus.setText(totalSuivis + " suivi(s) affiché(s)");

        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            lblStatus.setText("✗ Erreur: " + e.getMessage());
            afficherToast("✗ Erreur de chargement: " + e.getMessage(), false);
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            lblStatus.setText("✗ Erreur: " + e.getMessage());
            afficherToast("✗ Erreur de chargement: " + e.getMessage(), false);
        }
    }

    private void mettreAJourStatistiques(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        long total = suivis.size();
        long ceMois = suivis.stream().filter(s -> {
            LocalDate date = s.getDateSuivi().toLocalDate();
            return date.getMonth() == today.getMonth() && date.getYear() == today.getYear();
        }).count();

        long cetteSemaine = suivis.stream().filter(s -> {
            LocalDate date = s.getDateSuivi().toLocalDate();
            return !date.isBefore(startOfWeek) && !date.isAfter(today);
        }).count();

        long aujourdhui = suivis.stream().filter(s ->
                s.getDateSuivi().toLocalDate().equals(today)
        ).count();

        // Nombre d'étudiants différents ayant des suivis
        long parEtudiant = suivis.stream()
                .map(s -> {
                    Traitement t = traitementsMap.get(s.getTraitementId());
                    return t != null ? t.getEtudiantId() : null;
                })
                .filter(id -> id != null)
                .distinct()
                .count();

        statTotal.setText(String.valueOf(total));
        statCeMois.setText(String.valueOf(ceMois));
        statCetteSemaine.setText(String.valueOf(cetteSemaine));
        statAujourdhui.setText(String.valueOf(aujourdhui));
        statParEtudiant.setText(String.valueOf(parEtudiant));
    }

    private List<SuiviTraitement> appliquerRechercheEtFiltres(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String periodeFiltre = cmbFiltrePeriode.getValue();
        String saisiParFiltre = cmbFiltreSaisiPar.getValue();

        return suivis.stream()
                .filter(s -> {
                    // Filtre de recherche
                    if (!recherche.isEmpty()) {
                        Traitement traitement = traitementsMap.get(s.getTraitementId());
                        if (traitement != null) {
                            String nomEtudiant = getNomEtudiant(traitement.getEtudiantId()).toLowerCase();
                            String titreTraitement = traitement.getTitre().toLowerCase();
                            String observations = s.getObservations() != null ? s.getObservations().toLowerCase() : "";

                            boolean correspondRecherche =
                                    nomEtudiant.contains(recherche) ||
                                            titreTraitement.contains(recherche) ||
                                            observations.contains(recherche);

                            if (!correspondRecherche) return false;
                        } else {
                            return false;
                        }
                    }

                    // Filtre de période
                    if (!"Toutes les périodes".equals(periodeFiltre)) {
                        if (s.getDateSuivi() == null) return false;
                        if (!estDansPeriode(s.getDateSuivi().toLocalDate(), periodeFiltre)) return false;
                    }

                    // Filtre par "Saisi par"
                    if (!"Tous".equals(saisiParFiltre)) {
                        if ("Psychologue".equals(saisiParFiltre) && s.getSaisiPar() != SaisiPar.PSYCHOLOGUE) return false;
                        if ("Étudiant".equals(saisiParFiltre) && s.getSaisiPar() != SaisiPar.ETUDIANT) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<SuiviTraitement> appliquerTri(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        String tri = cmbTri.getValue();
        if (tri == null) return suivis;

        List<SuiviTraitement> result = new ArrayList<>(suivis);

        switch (tri) {
            case "📅 Date (plus récente)":
                result.sort((s1, s2) -> s2.getDateSuivi().compareTo(s1.getDateSuivi()));
                break;
            case "📅 Date (plus ancienne)":
                result.sort((s1, s2) -> s1.getDateSuivi().compareTo(s2.getDateSuivi()));
                break;
            case "👤 Étudiant (A-Z)":
                result.sort((s1, s2) -> {
                    Traitement t1 = traitementsMap.get(s1.getTraitementId());
                    Traitement t2 = traitementsMap.get(s2.getTraitementId());
                    if (t1 == null || t2 == null) return 0;
                    String nom1 = getNomEtudiant(t1.getEtudiantId());
                    String nom2 = getNomEtudiant(t2.getEtudiantId());
                    return nom1.compareToIgnoreCase(nom2);
                });
                break;
            case "👤 Étudiant (Z-A)":
                result.sort((s1, s2) -> {
                    Traitement t1 = traitementsMap.get(s1.getTraitementId());
                    Traitement t2 = traitementsMap.get(s2.getTraitementId());
                    if (t1 == null || t2 == null) return 0;
                    String nom1 = getNomEtudiant(t1.getEtudiantId());
                    String nom2 = getNomEtudiant(t2.getEtudiantId());
                    return nom2.compareToIgnoreCase(nom1);
                });
                break;
            case "📋 Traitement (A-Z)":
                result.sort((s1, s2) -> {
                    Traitement t1 = traitementsMap.get(s1.getTraitementId());
                    Traitement t2 = traitementsMap.get(s2.getTraitementId());
                    if (t1 == null || t2 == null) return 0;
                    return t1.getTitre().compareToIgnoreCase(t2.getTitre());
                });
                break;
            case "👨‍⚕️ Saisi par (Psychologue → Étudiant)":
                result.sort((s1, s2) -> {
                    int p1 = s1.getSaisiPar() == SaisiPar.PSYCHOLOGUE ? 1 : 2;
                    int p2 = s2.getSaisiPar() == SaisiPar.PSYCHOLOGUE ? 1 : 2;
                    return Integer.compare(p1, p2);
                });
                break;
        }
        return result;
    }

    private boolean estDansPeriode(LocalDate date, String periode) {
        LocalDate aujourdHui = LocalDate.now();
        switch (periode) {
            case "Aujourd'hui":
                return date.equals(aujourdHui);
            case "Cette semaine": {
                int jourSemaine = aujourdHui.getDayOfWeek().getValue();
                LocalDate debutSemaine = aujourdHui.minusDays(jourSemaine - 1);
                LocalDate finSemaine = debutSemaine.plusDays(6);
                return !date.isBefore(debutSemaine) && !date.isAfter(finSemaine);
            }
            case "Ce mois":
                return date.getMonth() == aujourdHui.getMonth() && date.getYear() == aujourdHui.getYear();
            default:
                return true;
        }
    }

    private List<LigneSuiviGroupée> creerLignesSuivisGroupées(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        List<LigneSuiviGroupée> lignes = new ArrayList<>();

        // Grouper les suivis par étudiant
        Map<Integer, List<SuiviTraitement>> suivisParEtudiant = new LinkedHashMap<>();

        for (SuiviTraitement suivi : suivis) {
            Traitement traitement = traitementsMap.get(suivi.getTraitementId());
            if (traitement != null) {
                int etudiantId = traitement.getEtudiantId();
                suivisParEtudiant.computeIfAbsent(etudiantId, k -> new ArrayList<>()).add(suivi);
            }
        }

        for (Map.Entry<Integer, List<SuiviTraitement>> entry : suivisParEtudiant.entrySet()) {
            Integer etudiantId = entry.getKey();
            List<SuiviTraitement> suivisEtudiant = entry.getValue();

            String nomEtudiant = getNomEtudiant(etudiantId);

            // Regrouper par traitement
            Map<Integer, List<SuiviTraitement>> suivisParTraitement = new LinkedHashMap<>();
            Map<Integer, String> nomsTraitements = new HashMap<>();

            for (SuiviTraitement suivi : suivisEtudiant) {
                Traitement traitement = traitementsMap.get(suivi.getTraitementId());
                if (traitement != null) {
                    int traitementId = suivi.getTraitementId();
                    suivisParTraitement.computeIfAbsent(traitementId, k -> new ArrayList<>()).add(suivi);
                    nomsTraitements.put(traitementId, traitement.getTitre());
                }
            }

            // En-tête étudiant
            LigneSuiviGroupée ligneEnteteEtudiant = new LigneSuiviGroupée(nomEtudiant, suivisEtudiant);
            lignes.add(ligneEnteteEtudiant);

            // Pour chaque traitement
            for (Map.Entry<Integer, List<SuiviTraitement>> traitementEntry : suivisParTraitement.entrySet()) {
                Integer traitementId = traitementEntry.getKey();
                List<SuiviTraitement> suivisTraitement = traitementEntry.getValue();
                String nomTraitement = nomsTraitements.getOrDefault(traitementId, "Traitement #" + traitementId);

                // En-tête traitement
                LigneSuiviGroupée ligneEnteteTraitement = new LigneSuiviGroupée(nomEtudiant, nomTraitement, suivisTraitement);
                lignes.add(ligneEnteteTraitement);

                // Suivis individuels
                for (SuiviTraitement suivi : suivisTraitement) {
                    lignes.add(new LigneSuiviGroupée(suivi));
                }
            }
        }
        return lignes;
    }

    private String getNomEtudiant(Integer etudiantId) {
        try {
            if (etudiantId == null || etudiantId == 0) return "Non assigné";
            List<Etudiant> etudiants = etudiantTraitementService.afficher();
            for (Etudiant etudiant : etudiants) {
                if (etudiant.getUserId() == etudiantId) {
                    return etudiant.getNom() + " " + etudiant.getPrenom();
                }
            }
            return "Étudiant #" + etudiantId;
        } catch (SQLException e) {
            return "Erreur #" + etudiantId;
        }
    }

    private void configurerColonnes() {
        tableViewSuiviTraitements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<LigneSuiviGroupée, String> colEtudiant = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(0);
        TableColumn<LigneSuiviGroupée, String> colDateSuivi = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(1);
        TableColumn<LigneSuiviGroupée, String> colSaisiPar = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(2);
        TableColumn<LigneSuiviGroupée, String> colNotes = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(3);

        colEtudiant.setPrefWidth(320);
        colDateSuivi.setPrefWidth(130);
        colSaisiPar.setPrefWidth(150);
        colNotes.setPrefWidth(380);

        colActions.setPrefWidth(180);
        colActions.setMinWidth(160);
        colActions.setResizable(false);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // ===== COLONNE ÉTUDIANT / TRAITEMENT =====
        colEtudiant.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getAffichage()));

        colEtudiant.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle(""); return;
                }
                LigneSuiviGroupée ligne = getTableRow().getItem();

                if (ligne.estEnteteEtudiant()) {
                    // En-tête étudiant — bandeau bleu clair et lumineux pour les psychologues
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 8; -fx-padding: 10 14;");

                    Label icone = new Label("👤");
                    icone.setStyle("-fx-font-size: 15px;");

                    Label nom = new Label(ligne.getNomEtudiant());
                    nom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 2, 0, 0, 1);");

                    Label compteur = new Label(ligne.getSuivis().size() + " suivi(s)");
                    compteur.setStyle("-fx-font-size: 11px; -fx-text-fill: #dbeafe; " +
                            "-fx-background-color: rgba(255,255,255,0.25); " +
                            "-fx-background-radius: 20; -fx-padding: 2 10;");

                    box.getChildren().addAll(icone, nom, compteur);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: #3b82f6; -fx-padding: 6 8;");

                } else if (ligne.estEnteteTraitement()) {
                    // En-tête traitement — légèrement indenté
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 6; " +
                            "-fx-padding: 8 12; -fx-border-color: #86efac; " +
                            "-fx-border-width: 0 0 0 3; -fx-border-radius: 0 6 6 0;");

                    Label icone = new Label("📋");
                    icone.setStyle("-fx-font-size: 13px;");

                    Label nom = new Label(ligne.getNomTraitement());
                    nom.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #166534;");

                    Label compteur = new Label(ligne.getSuivis().size() + " suivi(s)");
                    compteur.setStyle("-fx-font-size: 10px; -fx-text-fill: #15803d; " +
                            "-fx-background-color: #dcfce7; -fx-background-radius: 20; -fx-padding: 2 8;");

                    // Indentation
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    spacer.setPrefWidth(20);

                    box.getChildren().addAll(spacer, icone, nom, compteur);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: #f0fdf4; -fx-padding: 4 8;");

                } else {
                    // Ligne de données — indentation + point
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);

                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    spacer.setPrefWidth(40);

                    Label point = new Label("•");
                    point.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 14px;");

                    box.getChildren().addAll(spacer, point);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: white; -fx-padding: 0;");
                }
            }
        });

        // ===== COLONNE DATE SUIVI =====
        colDateSuivi.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            if (!ligne.estEntete() && ligne.getSuivi() != null && ligne.getSuivi().getDateSuivi() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        ligne.getSuivi().getDateSuivi().toLocalDate().format(dateFormatter));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colDateSuivi.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle(""); return;
                }
                LigneSuiviGroupée ligne = getTableRow().getItem();

                if (ligne.estEnteteEtudiant()) {
                    setText(null);
                    setStyle("-fx-background-color: #3b82f6;");
                } else if (ligne.estEnteteTraitement()) {
                    setText(null);
                    setStyle("-fx-background-color: #f0fdf4;");
                } else if (item != null && !item.isEmpty()) {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label icone = new Label("📅");
                    icone.setStyle("-fx-font-size: 11px;");
                    Label date = new Label(item);
                    date.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                    box.getChildren().addAll(icone, date);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: white; -fx-padding: 8 10;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: white;");
                }
            }
        });

        // ===== COLONNE SAISI PAR =====
        colSaisiPar.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            if (!ligne.estEntete() && ligne.getSuivi() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        ligne.getSuivi().getSaisiPar() == SaisiPar.PSYCHOLOGUE ? "PSYCHOLOGUE" : "ETUDIANT");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colSaisiPar.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle(""); return;
                }
                LigneSuiviGroupée ligne = getTableRow().getItem();

                if (ligne.estEnteteEtudiant()) {
                    setText(null);
                    setStyle("-fx-background-color: #3b82f6;");
                } else if (ligne.estEnteteTraitement()) {
                    setText(null);
                    setStyle("-fx-background-color: #f0fdf4;");
                } else if ("PSYCHOLOGUE".equals(item)) {
                    Label badge = new Label("👨‍⚕️  Psychologue");
                    badge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6d28d9; " +
                            "-fx-font-weight: bold; -fx-font-size: 11px; " +
                            "-fx-background-radius: 20; -fx-padding: 5 12;");
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-background-color: white; -fx-padding: 8px;");
                } else if ("ETUDIANT".equals(item)) {
                    Label badge = new Label("👨‍🎓  Étudiant");
                    badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; " +
                            "-fx-font-weight: bold; -fx-font-size: 11px; " +
                            "-fx-background-radius: 20; -fx-padding: 5 12;");
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-background-color: white; -fx-padding: 8px;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: white;");
                }
            }
        });

        // ===== COLONNE OBSERVATIONS =====
        colNotes.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            if (!ligne.estEntete() && ligne.getSuivi() != null) {
                String notes = ligne.getSuivi().getObservations();
                if (notes != null && notes.length() > 80) notes = notes.substring(0, 77) + "...";
                return new javafx.beans.property.SimpleStringProperty(notes != null ? notes : "Aucune observation");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colNotes.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle(""); return;
                }
                LigneSuiviGroupée ligne = getTableRow().getItem();

                if (ligne.estEnteteEtudiant()) {
                    setText(null);
                    setStyle("-fx-background-color: #3b82f6;");
                } else if (ligne.estEnteteTraitement()) {
                    setText(null);
                    setStyle("-fx-background-color: #f0fdf4;");
                } else {
                    boolean aucune = "Aucune observation".equals(item);
                    setText(item);
                    setStyle("-fx-background-color: white; -fx-padding: 10 12; -fx-font-size: 12px; " +
                            "-fx-text-fill: " + (aucune ? "#9ca3af" : "#374151") + "; " +
                            (aucune ? "-fx-font-style: italic;" : ""));
                }
            }
        });

        configurerColonneActions();
    }

    private void configurerColonneActions() {
        colActions.setCellFactory(param -> new TableCell<LigneSuiviGroupée, Void>() {

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null
                        || getTableRow().getItem().estEntete()) {
                    setGraphic(null);
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        LigneSuiviGroupée ligne = getTableRow().getItem();
                        if (ligne.estEnteteEtudiant()) setStyle("-fx-background-color: #3b82f6;");
                        else if (ligne.estEnteteTraitement()) setStyle("-fx-background-color: #f0fdf4;");
                        else setStyle("");
                    }
                    return;
                }

                LigneSuiviGroupée ligne = getTableRow().getItem();
                SuiviTraitement suivi = ligne.getSuivi();

                if (suivi == null) {
                    setGraphic(null);
                    return;
                }

                boolean estPsychologue = (utilisateur != null &&
                        "PSYCHOLOGUE".equals(utilisateur.getRole().name().trim()));
                boolean estEtudiant = (utilisateur != null &&
                        "ETUDIANT".equals(utilisateur.getRole().name().trim()));

                // ✅ Recréer les boutons à chaque appel pour éviter les conflits de cellules
                Button btnView   = new Button("👁️");
                Button btnEdit   = new Button("✏️");
                Button btnDelete = new Button("🗑️");

                btnView.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; " +
                        "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 12; -fx-cursor: hand;");
                btnEdit.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #a16207; " +
                        "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 12; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; " +
                        "-fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 10; -fx-cursor: hand;");

                btnView.setOnAction(event -> {
                    if (suivi != null) ouvrirPageAffichage(suivi);
                });

                btnEdit.setOnAction(event -> {
                    if (suivi != null) ouvrirPageModification(suivi);
                });

                btnDelete.setOnAction(event -> {
                    if (suivi != null) supprimerSuivi(suivi);
                });

                HBox container = new HBox(6);
                container.setAlignment(Pos.CENTER);

                // 👁️ Bouton Afficher toujours visible
                container.getChildren().add(btnView);

                if (estPsychologue) {
                    // ✅ Psy : Modifier/Supprimer UNIQUEMENT sur SES propres suivis
                    if (suivi.getSaisiPar() == SaisiPar.PSYCHOLOGUE) {
                        container.getChildren().addAll(btnEdit, btnDelete);
                    }
                    // ❌ Suivi ETUDIANT → bouton 👁️ uniquement

                } else if (estEtudiant) {
                    // ✅ Étudiant : Modifier/Supprimer UNIQUEMENT sur SES propres suivis
                    if (suivi.getSaisiPar() == SaisiPar.ETUDIANT) {
                        container.getChildren().addAll(btnEdit, btnDelete);
                    }
                    // ❌ Suivi PSYCHOLOGUE → bouton 👁️ uniquement

                }

                setGraphic(container);
                setStyle("-fx-background-color: white; -fx-padding: 6px;");
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
        cmbFiltrePeriode.setValue("Toutes les périodes");
        cmbFiltreSaisiPar.setValue("Tous");
        cmbTri.setValue("📅 Date (plus récente)");
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        if (tousLesSuivisFiltres == null || tousLesSuivisFiltres.isEmpty()) {
            toutesLesLignes = new ArrayList<>();
            appliquerPagination();
            return;
        }

        try {
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            List<SuiviTraitement> suivisFiltres = new ArrayList<>(tousLesSuivisFiltres);
            suivisFiltres = appliquerRechercheEtFiltres(suivisFiltres, traitementsMap);
            suivisFiltres = appliquerTri(suivisFiltres, traitementsMap);

            // MODIFICATION ICI : Stocker toutes les lignes pour la pagination
            toutesLesLignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);

            // Réinitialiser à la première page
            currentPage = 0;

            // Appliquer la pagination
            appliquerPagination();

            lblStatus.setText(toutesLesLignes.stream().filter(l -> !l.estEntete()).count() + " suivi(s) après filtrage");
        } catch (Exception e) {
            afficherToast("✗ Erreur: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleExporterExcel() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter les suivis");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );
            fileChooser.setInitialFileName("suivis_" + java.time.LocalDate.now() + ".csv");

            File file = fileChooser.showSaveDialog(tableViewSuiviTraitements.getScene().getWindow());

            if (file != null) {
                exporterVersCSV(file);
                lblStatus.setText("✓ Export réussi: " + file.getName());
                afficherToast("✓ Export réussi: " + file.getName(), true);
            }
        } catch (Exception e) {
            afficherToast("✗ Erreur d'export: " + e.getMessage(), false);
        }
    }

    private void exporterVersCSV(File file) throws IOException {
        // UTF-8 avec BOM pour Excel
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            // BOM UTF-8 pour Excel
            writer.write('\uFEFF');
            writer.write("Étudiant;Traitement;Date suivi;Saisi par;Observations\n");

            for (LigneSuiviGroupée ligne : lignesSuivisGroupéesList) {
                if (!ligne.estEntete() && ligne.getPremierSuivi() != null) {
                    SuiviTraitement s = ligne.getPremierSuivi();
                    Traitement t = null;
                    for (Traitement traitement : tousLesTraitements) {
                        if (traitement.getTraitementId() == s.getTraitementId()) {
                            t = traitement;
                            break;
                        }
                    }
                    String saisiPar = s.getSaisiPar() == SaisiPar.PSYCHOLOGUE ? "Psychologue" : "Étudiant";
                    // Récupérer le nom de l'étudiant depuis le traitement
                    String nomEtudiant = "";
                    if (t != null) {
                        nomEtudiant = getNomEtudiant(t.getEtudiantId());
                    }
                    writer.write(String.format("%s;%s;%s;%s;%s\n",
                            nomEtudiant,
                            t != null ? t.getTitre().replace(";", ",") : "",
                            s.getDateSuivi() != null ? s.getDateSuivi().toString() : "",
                            saisiPar,
                            s.getObservations() != null ? s.getObservations().replace(";", ",") : ""
                    ));
                }
            }
        }
    }

    // ==================== NAVIGATION ====================

    @FXML private void handleAjouter() { ouvrirPageAjout(); }
    @FXML private void handleRafraichir() { chargerDonnees(); }

    @FXML private void ouvrirTraitementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-view.fxml"));
            Parent root = loader.load();

            // Passer l'utilisateur au nouveau controller
            TraitementController controller = loader.getController();
            controller.setUtilisateur(utilisateur);

            Scene currentScene = tableViewSuiviTraitements.getScene();
            if (currentScene != null) currentScene.setRoot(root);
        } catch (Exception e) {
            afficherToast("✗ Erreur de navigation: " + e.getMessage(), false);
        }
    }

    private void ouvrirPageAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-ajout-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Suivi");
            stage.setScene(new Scene(root, 800, 650));
            stage.show();
            stage.setOnHiding(event -> chargerDonnees());
        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
        }
    }

    private void ouvrirPageAffichage(SuiviTraitement suivi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-affichage-view.fxml"));
            Parent root = loader.load();
            SuiviTraitementAffichageController controller = loader.getController();
            controller.setSuiviTraitement(suivi);
            Stage stage = new Stage();
            stage.setTitle("Détails du Suivi");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
        }
    }

    private void ouvrirPageModification(SuiviTraitement suivi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-modification-view.fxml"));
            Parent root = loader.load();
            SuiviTraitementModificationController controller = loader.getController();
            controller.setSuiviTraitement(suivi);
            Stage stage = new Stage();
            stage.setTitle("Modifier un Suivi");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
            stage.setOnHiding(event -> chargerDonnees());
        } catch (Exception e) {
            afficherToast("✗ Erreur d'ouverture: " + e.getMessage(), false);
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
                afficherToast("✗ Erreur de suppression: " + e.getMessage(), false);
            }
        }
    }

    /**
     * Ouvre la fenêtre d'analyse IA pour le psychologue
     */
    @FXML
    private void handleAnalyseIA() {
        try {
            if (utilisateur == null) {
                afficherToast("⚠️ Utilisateur non connecté", false);
                return;
            }

            // Récupérer tous les suivis et traitements
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            List<Traitement> tousLesTraitements = traitementService.afficher();

            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement t : tousLesTraitements) {
                traitementsMap.put(t.getTraitementId(), t);
            }

            int utilisateurId = utilisateur.getUserId();

            // Filtrer les traitements du psychologue connecté
            List<Traitement> traitementsPsychologue = new ArrayList<>();
            for (Traitement traitement : tousLesTraitements) {
                if (traitement.getPsychologueId() == utilisateurId) {
                    traitementsPsychologue.add(traitement);
                }
            }

            // Récupérer les IDs des traitements du psychologue
            List<Integer> traitementIds = traitementsPsychologue.stream()
                    .map(Traitement::getTraitementId)
                    .collect(Collectors.toList());

            // Filtrer les suivis correspondant aux traitements du psychologue
            List<SuiviTraitement> suivisPsychologue = tousLesSuivis.stream()
                    .filter(s -> traitementIds.contains(s.getTraitementId()))
                    .collect(Collectors.toList());

            if (traitementsPsychologue.isEmpty()) {
                afficherToast("⚠️ Aucun traitement trouvé pour l'analyse", false);
                return;
            }

            // Ouvrir la fenêtre d'analyse IA
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/analyse-ia-view.fxml"));
            Parent root = loader.load();

            AnalyseIAController controller = loader.getController();
            controller.setDonneesAnalyse(traitementsPsychologue, suivisPsychologue);

            Stage stage = new Stage();
            stage.setTitle("🤖 Analyse IA - Suivi des Traitements");
            stage.setScene(new Scene(root, 1000, 800));
            stage.setMaximized(true);
            stage.show();

            afficherToast("📊 Analyse IA lancée pour " + traitementsPsychologue.size() + " traitement(s)", true);

        } catch (Exception e) {
            e.printStackTrace();
            afficherToast("✗ Erreur lors de l'analyse IA: " + e.getMessage(), false);
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