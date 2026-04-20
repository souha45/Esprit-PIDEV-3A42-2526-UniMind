package org.example.controllers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.example.enums.SaisiPar;
import org.example.services.EtudiantTraitementService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SuiviTraitementController implements Initializable {

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

    // ==================== SERVICES ====================

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantTraitementService etudiantTraitementService;
    private ObservableList<LigneSuiviGroupée> lignesSuivisGroupéesList;
    private List<SuiviTraitement> tousLesSuivisFiltres;
    private List<Traitement> tousLesTraitements;
    private boolean isInitialized = false;

    // ==================== INITIALISATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantTraitementService = new EtudiantTraitementService();
            tousLesSuivisFiltres = new ArrayList<>();

            initialiserFiltres();
            initialiserTri();
            configurerColonnes();
            chargerDonnees();

            isInitialized = true;
            lblStatus.setText("Interface Suivis Traitements prête");

        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
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
        try {
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            tousLesTraitements = traitementService.afficher();
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            SessionManager session = SessionManager.getInstance();
            List<SuiviTraitement> suivisFiltres = new ArrayList<>();

            for (SuiviTraitement suivi : tousLesSuivis) {
                Traitement traitementAssocie = traitementsMap.get(suivi.getTraitementId());
                if (traitementAssocie == null) continue;

                int psychologueId = traitementAssocie.getPsychologueId();

                boolean peutVoir = false;

                if (session.estPsychologue()) {
                    // CORRECTION : Le psychologue ne voit que les suivis de SES traitements
                    peutVoir = (psychologueId == session.getUtilisateurConnecteId());
                } else if (session.estEtudiant()) {
                    int etudiantId = traitementAssocie.getEtudiantId();
                    peutVoir = (etudiantId == session.getUtilisateurConnecteId());
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

            // Créer les lignes groupées
            List<LigneSuiviGroupée> lignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);
            lignesSuivisGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewSuiviTraitements.setItems(lignesSuivisGroupéesList);

            long totalSuivis = lignes.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalSuivis + " suivi(s)");

        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            afficherErreur("Erreur de chargement", "Erreur base de données: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            afficherErreur("Erreur de chargement", e.getMessage());
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

    // ==================== CONFIGURATION DES COLONNES ====================

    private void configurerColonnes() {
        tableViewSuiviTraitements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<LigneSuiviGroupée, String> colEtudiant = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(0);
        TableColumn<LigneSuiviGroupée, String> colDateSuivi = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(1);
        TableColumn<LigneSuiviGroupée, String> colSaisiPar = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(2);
        TableColumn<LigneSuiviGroupée, String> colNotes = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(3);

        colEtudiant.setPrefWidth(350);
        colDateSuivi.setPrefWidth(120);
        colSaisiPar.setPrefWidth(120);
        colNotes.setPrefWidth(400);

        // Colonne Étudiant / Traitement
        colEtudiant.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            return new javafx.beans.property.SimpleStringProperty(ligne.getAffichage());
        });

        colEtudiant.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneSuiviGroupée ligne = getTableRow().getItem();
                    setText(item);
                    if (ligne.estEnteteEtudiant()) {
                        setStyle("-fx-background-color: #ede9fe; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #4f46e5; -fx-padding: 12 8;");
                    } else if (ligne.estEnteteTraitement()) {
                        setStyle("-fx-background-color: #f5f3ff; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6366f1; -fx-padding: 10 8 10 25;");
                    } else {
                        setStyle("-fx-background-color: white; -fx-padding: 10 8 10 35; -fx-font-size: 12px;");
                    }
                }
            }
        });

        // Colonne Date Suivi
        colDateSuivi.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            SuiviTraitement suivi = ligne.getPremierSuivi();
            if (suivi != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(suivi.getDateSuivi() != null ? suivi.getDateSuivi().toString() : "");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Colonne Saisi Par
        colSaisiPar.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            SuiviTraitement suivi = ligne.getPremierSuivi();
            if (suivi != null && !ligne.estEntete()) {
                String texte = suivi.getSaisiPar() == SaisiPar.PSYCHOLOGUE ? "👨‍⚕️ Psychologue" : "👨‍🎓 Étudiant";
                return new javafx.beans.property.SimpleStringProperty(texte);
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colSaisiPar.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneSuiviGroupée ligne = getTableRow().getItem();
                    if (ligne.estEntete()) {
                        setText("");
                        setStyle("");
                    } else if (item != null && !item.isEmpty()) {
                        setText(item);
                        if (item.contains("Psychologue")) {
                            setStyle("-fx-text-fill: #4f46e5; -fx-font-weight: bold; -fx-padding: 10 8;");
                        } else {
                            setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 10 8;");
                        }
                    } else {
                        setText("");
                    }
                }
            }
        });

        // Colonne Observations
        colNotes.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            SuiviTraitement suivi = ligne.getPremierSuivi();
            if (suivi != null && !ligne.estEntete()) {
                String notes = suivi.getObservations();
                if (notes != null && notes.length() > 80) {
                    notes = notes.substring(0, 77) + "...";
                }
                return new javafx.beans.property.SimpleStringProperty(notes != null ? notes : "");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        colNotes.setCellFactory(param -> new TableCell<LigneSuiviGroupée, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else {
                    LigneSuiviGroupée ligne = getTableRow().getItem();
                    if (ligne.estEntete()) {
                        setText("");
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        configurerColonneActions();
    }

    private void configurerColonneActions() {
        SessionManager session = SessionManager.getInstance();

        colActions.setCellFactory(param -> new TableCell<LigneSuiviGroupée, Void>() {
            private final Button btnView = new Button("Afficher");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(8, btnView, btnEdit, btnDelete);

            {
                container.setAlignment(Pos.CENTER);
                btnView.getStyleClass().addAll("table-action-button", "table-action-button-view");
                btnEdit.getStyleClass().addAll("table-action-button", "table-action-button-edit");
                btnDelete.getStyleClass().addAll("table-action-button", "table-action-button-delete");
                btnView.setPrefWidth(70);
                btnEdit.setPrefWidth(70);
                btnDelete.setPrefWidth(70);

                btnView.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        ouvrirPageAffichage(suivi);
                    }
                });

                btnEdit.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        if (session.estEtudiant() && suivi.getSaisiPar() != SaisiPar.ETUDIANT) {
                            afficherErreur("Accès refusé", "Vous ne pouvez pas modifier le suivi du psychologue.");
                        } else {
                            ouvrirPageModification(suivi);
                        }
                    }
                });

                btnDelete.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        if (session.estEtudiant() && suivi.getSaisiPar() != SaisiPar.ETUDIANT) {
                            afficherErreur("Accès refusé", "Vous ne pouvez pas supprimer le suivi du psychologue.");
                        } else {
                            supprimerSuivi(suivi);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null || getTableRow().getItem().estEntete()) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
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
        cmbFiltrePeriode.setValue("Toutes les périodes");
        cmbFiltreSaisiPar.setValue("Tous");
        cmbTri.setValue("📅 Date (plus récente)");
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        if (tousLesSuivisFiltres == null || tousLesSuivisFiltres.isEmpty()) {
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

            List<LigneSuiviGroupée> lignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);
            lignesSuivisGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewSuiviTraitements.setItems(lignesSuivisGroupéesList);

            long totalSuivis = lignes.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalSuivis + " suivi(s)");

        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible d'appliquer les filtres: " + e.getMessage());
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
            }
        } catch (Exception e) {
            afficherErreur("Erreur d'export", e.getMessage());
        }
    }

    private void exporterVersCSV(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
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
                    writer.write(String.format("%s;%s;%s;%s;%s\n",
                            ligne.getNomEtudiant(),
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
            Scene currentScene = tableViewSuiviTraitements.getScene();
            if (currentScene != null) currentScene.setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur de navigation", "Impossible d'accéder à la page des traitements: " + e.getMessage());
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
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page d'ajout: " + e.getMessage());
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
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page d'affichage: " + e.getMessage());
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
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page de modification: " + e.getMessage());
        }
    }

    private void supprimerSuivi(SuiviTraitement suivi) {
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le suivi");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce suivi ?");
            if (confirm.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
                suiviTraitementService.supprimer(suivi.getSuivitraitementId());
                chargerDonnees();
                lblStatus.setText("Suivi supprimé avec succès");
            }
        } catch (SQLException e) {
            afficherErreur("Erreur de suppression", "Erreur base de données: " + e.getMessage());
        } catch (Exception e) {
            afficherErreur("Erreur de suppression", e.getMessage());
        }
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}