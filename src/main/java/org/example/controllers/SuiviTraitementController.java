package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.services.EtudiantService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;

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
import javafx.scene.layout.Region;
import javafx.stage.Stage;


public class SuiviTraitementController implements Initializable {

    //  CLASSE INTERNE POUR L'AFFICHAGE HIÉRARCHIQUE

    /**
     * Classe wrapper pour représenter une ligne groupée dans le tableau
     * Structure hiérarchique : Étudiant -> Traitement -> Suivis individuels
     */
    public static class LigneSuiviGroupée {
        private final String nomEtudiant;
        private final String nomTraitement;
        private final List<SuiviTraitement> suivis;
        private final boolean estEnteteEtudiant;
        private final boolean estEnteteTraitement;

        /**
         * Constructeur pour un en-tête d'étudiant
         * @param nomEtudiant Nom de l'étudiant
         * @param suivis Liste des suivis de l'étudiant
         */
        public LigneSuiviGroupée(String nomEtudiant, List<SuiviTraitement> suivis) {
            this.nomEtudiant = nomEtudiant;
            this.nomTraitement = null;
            this.suivis = suivis;
            this.estEnteteEtudiant = true;
            this.estEnteteTraitement = false;
        }

        /**
         * Constructeur pour un en-tête de traitement
         * @param nomEtudiant Nom de l'étudiant parent
         * @param nomTraitement Nom du traitement
         * @param suivis Liste des suivis du traitement
         */
        public LigneSuiviGroupée(String nomEtudiant, String nomTraitement, List<SuiviTraitement> suivis) {
            this.nomEtudiant = nomEtudiant;
            this.nomTraitement = nomTraitement;
            this.suivis = suivis;
            this.estEnteteEtudiant = false;
            this.estEnteteTraitement = true;
        }

        /**
         * Constructeur pour une ligne de suivi individuel
         * @param suivi Suivi à afficher
         */
        public LigneSuiviGroupée(SuiviTraitement suivi) {
            this.nomEtudiant = null;
            this.nomTraitement = null;
            this.suivis = List.of(suivi);
            this.estEnteteEtudiant = false;
            this.estEnteteTraitement = false;
        }

        // Getters
        public String getNomEtudiant() { return nomEtudiant; }
        public String getNomTraitement() { return nomTraitement; }
        public List<SuiviTraitement> getSuivis() { return suivis; }
        public boolean estEnteteEtudiant() { return estEnteteEtudiant; }
        public boolean estEnteteTraitement() { return estEnteteTraitement; }
        public boolean estEntete() { return estEnteteEtudiant || estEnteteTraitement; }

        /**
         * Récupère le premier suivi de la ligne
         * @return Le premier suivi ou null
         */
        public SuiviTraitement getPremierSuivi() {
            return suivis.isEmpty() ? null : suivis.get(0);
        }

        /**
         * Génère le texte d'affichage avec indentation et icônes
         * @return Texte formaté pour l'affichage
         */
        public String getAffichage() {
            if (estEnteteEtudiant) {
                return "📚 " + nomEtudiant + " (" + getTotalSuivisEtudiant() + " suivi(s))";
            } else if (estEnteteTraitement) {
                return "  └ 📋 " + nomTraitement + " (" + suivis.size() + " suivi(s))";
            } else {
                return "";
            }
        }

        /**
         * Calcule le nombre total de suivis pour l'étudiant
         * @return Nombre total de suivis
         */
        private int getTotalSuivisEtudiant() {
            return suivis.size();
        }
    }

    //  COMPOSANTS FXML

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
    private Button btnAppliquerFiltres;
    @FXML
    private Button btnReinitialiserFiltres;
    @FXML
    private Button btnAjouter;
    @FXML
    private TableColumn<LigneSuiviGroupée, Void> colActions;

    //  SERVICES

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<LigneSuiviGroupée> lignesSuivisGroupéesList;

    // INITIALISATION

    /**
     * Méthode d'initialisation appelée automatiquement après le chargement du FXML
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialisation des services
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Configuration de l'interface
            initialiserFiltres();
            configurerColonnes();
            chargerDonnees();

            lblStatus.setText("Interface Suivis Traitements prête");

        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //  CHARGEMENT DES DONNÉES

    private void chargerDonnees() {
        try {
            // Récupération de tous les suivis et traitements
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            List<Traitement> tousLesTraitements = traitementService.afficher();

            // Création d'une Map pour accéder rapidement aux traitements par ID
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            // Filtrage des suivis selon les permissions de l'utilisateur connecté
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            List<SuiviTraitement> suivisFiltres = new ArrayList<>();

            for (SuiviTraitement suivi : tousLesSuivis) {
                Traitement traitementAssocie = traitementsMap.get(suivi.getTraitementId());
                if (traitementAssocie == null) continue;

                int etudiantId = traitementAssocie.getEtudiantId();
                int psychologueId = traitementAssocie.getPsychologueId();

                boolean peutVoir = false;
                if (session.estPsychologue()) {
                    // Psychologue : voit ses propres suivis et ceux de ses étudiants
                    if (suivi.getSaisiPar() == org.example.enums.SaisiPar.PSYCHOLOGUE) {
                        peutVoir = (psychologueId == session.getUtilisateurConnecteId());
                    } else {
                        peutVoir = session.peutVoirTraitement(etudiantId, psychologueId);
                    }
                } else {
                    // Étudiant : voit ses propres suivis
                    if (suivi.getSaisiPar() == org.example.enums.SaisiPar.ETUDIANT) {
                        peutVoir = (etudiantId == session.getUtilisateurConnecteId());
                    } else {
                        peutVoir = (etudiantId == session.getUtilisateurConnecteId());
                    }
                }

                if (peutVoir) {
                    suivisFiltres.add(suivi);
                }
            }

            // Création des lignes hiérarchiques
            List<LigneSuiviGroupée> lignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);
            lignesSuivisGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewSuiviTraitements.setItems(lignesSuivisGroupéesList);

            // Mise à jour du compteur (uniquement les lignes de suivi, pas les en-têtes)
            long totalSuivis = lignes.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalSuivis + " suivi(s)");

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement des données: " + e.getMessage());
            afficherErreur("Erreur de chargement", "Erreur base de données: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    /**
     * Crée les lignes hiérarchiques pour l'affichage (Étudiant -> Traitement -> Suivis)
     * @param suivis Liste des suivis à organiser
     * @param traitementsMap Map des traitements par ID
     * @return Liste des lignes hiérarchiques
     */
    private List<LigneSuiviGroupée> creerLignesSuivisGroupées(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        List<LigneSuiviGroupée> lignes = new ArrayList<>();

        // Regroupement des suivis par étudiant
        Map<Integer, List<SuiviTraitement>> suivisParEtudiant = new HashMap<>();

        for (SuiviTraitement suivi : suivis) {
            Traitement traitement = traitementsMap.get(suivi.getTraitementId());
            if (traitement != null) {
                Integer etudiantId = traitement.getEtudiantId();
                suivisParEtudiant.computeIfAbsent(etudiantId, k -> new ArrayList<>()).add(suivi);
            }
        }

        // Pour chaque étudiant, créer la hiérarchie
        for (Map.Entry<Integer, List<SuiviTraitement>> entry : suivisParEtudiant.entrySet()) {
            Integer etudiantId = entry.getKey();
            List<SuiviTraitement> suivisEtudiant = entry.getValue();

            String nomEtudiant = getNomEtudiant(etudiantId);

            // Regroupement des suivis par traitement pour cet étudiant
            Map<Integer, List<SuiviTraitement>> suivisParTraitement = new HashMap<>();
            Map<Integer, String> nomsTraitements = new HashMap<>();

            for (SuiviTraitement suivi : suivisEtudiant) {
                Traitement traitement = traitementsMap.get(suivi.getTraitementId());
                if (traitement != null) {
                    Integer traitementId = suivi.getTraitementId();
                    suivisParTraitement.computeIfAbsent(traitementId, k -> new ArrayList<>()).add(suivi);
                    nomsTraitements.put(traitementId, traitement.getTitre());
                }
            }

            // 1. Ligne d'en-tête de l'étudiant
            LigneSuiviGroupée ligneEnteteEtudiant = new LigneSuiviGroupée(nomEtudiant, suivisEtudiant);
            lignes.add(ligneEnteteEtudiant);

            // Pour chaque traitement de cet étudiant
            for (Map.Entry<Integer, List<SuiviTraitement>> traitementEntry : suivisParTraitement.entrySet()) {
                Integer traitementId = traitementEntry.getKey();
                List<SuiviTraitement> suivisTraitement = traitementEntry.getValue();
                String nomTraitement = nomsTraitements.getOrDefault(traitementId, "Traitement #" + traitementId);

                // 2. Ligne d'en-tête du traitement
                LigneSuiviGroupée ligneEnteteTraitement = new LigneSuiviGroupée(nomEtudiant, nomTraitement, suivisTraitement);
                lignes.add(ligneEnteteTraitement);

                // 3. Lignes de suivis individuels
                for (SuiviTraitement suivi : suivisTraitement) {
                    lignes.add(new LigneSuiviGroupée(suivi));
                }
            }
        }
        return lignes;
    }

    /**
     * Récupère le nom complet d'un étudiant à partir de son ID
     * @param etudiantId ID de l'étudiant
     * @return Nom complet de l'étudiant ou message par défaut
     */
    private String getNomEtudiant(Integer etudiantId) {
        try {
            if (etudiantId == null || etudiantId == 0) {
                return "Non assigné";
            }
            List<Etudiant> etudiants = etudiantService.afficher();
            for (Etudiant etudiant : etudiants) {
                if (etudiant.getUserId() == etudiantId) {
                    return etudiant.getNom() + " " + etudiant.getPrenom();
                }
            }
            return "Étudiant #" + etudiantId;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du nom de l'étudiant: " + e.getMessage());
            return "Erreur #" + etudiantId;
        }
    }

    // ==================== CONFIGURATION DU TABLEAU ====================

    /**
     * Configure les colonnes du tableau avec leurs largeurs et cell factories
     */
    @SuppressWarnings("unchecked")
    private void configurerColonnes() {
        // Politique de redimensionnement : la dernière colonne prend l'espace restant
        tableViewSuiviTraitements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Vérification du nombre de colonnes
        if (tableViewSuiviTraitements.getColumns().size() < 4) {
            System.err.println("Erreur: Le tableau n'a pas assez de colonnes");
            return;
        }

        // Récupération des colonnes (4 colonnes au total)
        TableColumn<LigneSuiviGroupée, String> colEtudiant = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(0);
        TableColumn<LigneSuiviGroupée, String> colDateSuivi = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(1);
        TableColumn<LigneSuiviGroupée, String> colNotes = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(2);

        // Définition des largeurs préférées
        colEtudiant.setPrefWidth(400);  // Colonne hiérarchique la plus large
        colDateSuivi.setPrefWidth(150); // Date
        colNotes.setPrefWidth(450);     // Observations

        // ===== COLONNE ÉTUDIANT/TRAITEMENT (affichage hiérarchique) =====
        colEtudiant.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            return new javafx.beans.property.SimpleStringProperty(ligne.getAffichage());
        });

        // Style personnalisé avec indentation
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
                        // Style pour l'en-tête d'étudiant
                        setStyle("-fx-background-color: #ede9fe; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #4f46e5; -fx-padding: 12 8;");
                    } else if (ligne.estEnteteTraitement()) {
                        // Style pour l'en-tête de traitement (indenté)
                        setStyle("-fx-background-color: #f5f3ff; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6366f1; -fx-padding: 10 8 10 25;");
                    } else {
                        // Style pour les lignes de suivi (plus indentées)
                        setStyle("-fx-background-color: white; -fx-padding: 10 8 10 35; -fx-font-size: 12px;");
                    }
                }
            }
        });

        // COLONNE DATE SUIVI
        colDateSuivi.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            SuiviTraitement suivi = ligne.getPremierSuivi();
            if (suivi != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(suivi.getDateSuivi() != null ? suivi.getDateSuivi().toString() : "");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // COLONNE OBSERVATIONS (avec troncature)
        colNotes.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            SuiviTraitement suivi = ligne.getPremierSuivi();
            if (suivi != null && !ligne.estEntete()) {
                String notes = suivi.getObservations();
                // Troncature à 80 caractères
                if (notes != null && notes.length() > 80) {
                    notes = notes.substring(0, 77) + "...";
                }
                return new javafx.beans.property.SimpleStringProperty(notes != null ? notes : "");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Configuration de la colonne Actions
        configurerColonneActions();
    }

    /**
     * Configure la colonne Actions avec les boutons Afficher/Modifier/Supprimer
     */
    private void configurerColonneActions() {
        org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();

        colActions.setCellFactory(param -> new TableCell<LigneSuiviGroupée, Void>() {
            // Création des boutons d'action
            private final Button btnView = new Button("Afficher");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(8, btnView, btnEdit, btnDelete);

            {
                // Centrage des boutons
                container.setAlignment(Pos.CENTER);

                // Application des styles CSS
                btnView.getStyleClass().addAll("table-action-button", "table-action-button-view");
                btnEdit.getStyleClass().addAll("table-action-button", "table-action-button-edit");
                btnDelete.getStyleClass().addAll("table-action-button", "table-action-button-delete");

                // Largeur fixe pour uniformiser
                btnView.setPrefWidth(70);
                btnEdit.setPrefWidth(70);
                btnDelete.setPrefWidth(70);

                // Action du bouton Afficher
                btnView.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        ouvrirPageAffichage(suivi);
                    }
                });

                // Action du bouton Modifier
                btnEdit.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        ouvrirPageModification(suivi);
                    }
                });

                // Action du bouton Supprimer
                btnDelete.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        supprimerSuivi(suivi);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null || getTableRow().getItem().estEntete()) {
                    setGraphic(null);
                } else {
                    // Adaptation selon le rôle de l'utilisateur
                    if (session.estEtudiant()) {
                        setGraphic(btnView);  // Étudiant : uniquement Afficher
                    } else {
                        setGraphic(container); // Psychologue : tous les boutons
                    }
                }
            }
        });
    }

    //  FILTRAGE ET RECHERCHE

    /**
     * Initialise les composants de filtrage
     */
    private void initialiserFiltres() {
        // Configuration du filtre par période (suppression de "Les 30 derniers jours")
        ObservableList<String> periodes = FXCollections.observableArrayList(
                "Toutes les périodes", "Aujourd'hui", "Cette semaine", "Ce mois"
        );
        cmbFiltrePeriode.setItems(periodes);
        cmbFiltrePeriode.setValue("Toutes les périodes");

        // Listeners pour appliquer les filtres en temps réel
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cmbFiltrePeriode.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
    }

    /**
     * Applique les filtres (recherche textuelle et période)
     */
    @FXML
    private void handleAppliquerFiltres() {
        appliquerFiltres();
    }

    /**
     * Réinitialise tous les filtres
     */
    @FXML
    private void handleReinitialiserFiltres() {
        txtRecherche.clear();
        cmbFiltrePeriode.setValue("Toutes les périodes");
        appliquerFiltres();
    }

    /**
     * Logique principale de filtrage des suivis
     */
    private void appliquerFiltres() {
        try {
            // Récupération de toutes les données
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            List<Traitement> tousLesTraitements = traitementService.afficher();

            // Map pour accès rapide aux traitements
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            // Application des filtres
            List<SuiviTraitement> suivisFiltres = filtrerSuivis(tousLesSuivis, traitementsMap);

            // Recréation des lignes hiérarchiques
            List<LigneSuiviGroupée> lignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);
            lignesSuivisGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewSuiviTraitements.setItems(lignesSuivisGroupéesList);

            // Mise à jour du compteur
            long totalSuivis = lignes.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalSuivis + " suivi(s)");

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du filtrage: " + e.getMessage());
            afficherErreur("Erreur", "Impossible d'appliquer les filtres: " + e.getMessage());
        }
    }

    /**
     * Filtre la liste des suivis selon les critères saisis
     * Recherche sur : nom étudiant, titre traitement, observations
     * @param suivis Liste des suivis à filtrer
     * @param traitementsMap Map des traitements
     * @return Liste filtrée
     */
    private List<SuiviTraitement> filtrerSuivis(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String periodeFiltre = cmbFiltrePeriode.getValue();

        return suivis.stream()
                .filter(s -> {
                    // ===== FILTRE PAR RECHERCHE TEXTUELLE =====
                    if (!recherche.isEmpty()) {
                        Traitement traitement = traitementsMap.get(s.getTraitementId());
                        if (traitement != null) {
                            // Recherche par nom d'étudiant
                            String nomEtudiant = getNomEtudiant(traitement.getEtudiantId()).toLowerCase();
                            // Recherche par titre du traitement
                            String titreTraitement = traitement.getTitre().toLowerCase();
                            // Recherche par observations
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

                    // ===== FILTRE PAR PÉRIODE =====
                    if (!"Toutes les périodes".equals(periodeFiltre)) {
                        if (s.getDateSuivi() == null) return false;
                        if (!estDansPeriode(s.getDateSuivi().toLocalDate(), periodeFiltre)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si une date est dans la période sélectionnée
     * @param date Date à vérifier
     * @param periode Période sélectionnée
     * @return true si la date est dans la période
     */
    private boolean estDansPeriode(LocalDate date, String periode) {
        LocalDate aujourdHui = LocalDate.now();
        switch (periode) {
            case "Aujourd'hui":
                return date.equals(aujourdHui);
            case "Cette semaine": {
                // Calcul du lundi de la semaine en cours (1 = lundi, 7 = dimanche)
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

    //  ACTIONS PRINCIPALES

    /**
     * Ouvre la page d'ajout d'un nouveau suivi
     */
    @FXML
    private void handleAjouter() {
        ouvrirPageAjout();
    }

    /**
     * Rafraîchit la liste des suivis
     */
    @FXML
    private void handleRafraichir() {
        chargerDonnees();
        lblStatus.setText("Liste rafraîchie");
    }

    /**
     * Navigue vers la page des traitements
     */
    @FXML
    private void ouvrirTraitementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-view.fxml"));
            Parent root = loader.load();
            Scene currentScene = tableViewSuiviTraitements.getScene();
            currentScene.setRoot(root);
            lblStatus.setText("Navigation vers la page des traitements");
        } catch (Exception e) {
            System.err.println("Erreur de navigation: " + e.getMessage());
            afficherErreur("Erreur de navigation", "Impossible d'accéder à la page des traitements: " + e.getMessage());
        }
    }

    //  MÉTHODES DE NAVIGATION

    /**
     * Ouvre la fenêtre d'ajout d'un suivi
     */
    private void ouvrirPageAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-ajout-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Suivi de Traitement");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
            lblStatus.setText("Page d'ajout ouverte");
        } catch (Exception e) {
            System.err.println("Erreur d'ouverture de la page d'ajout: " + e.getMessage());
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page d'ajout: " + e.getMessage());
        }
    }

    /**
     * Ouvre la fenêtre d'affichage des détails d'un suivi
     * @param suivi Suivi à afficher
     */
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
            lblStatus.setText("Page d'affichage ouverte");
        } catch (Exception e) {
            System.err.println("Erreur d'ouverture de la page d'affichage: " + e.getMessage());
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page d'affichage: " + e.getMessage());
        }
    }

    /**
     * Ouvre la fenêtre de modification d'un suivi
     * @param suivi Suivi à modifier
     */
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
            lblStatus.setText("Page de modification ouverte");
        } catch (Exception e) {
            System.err.println("Erreur d'ouverture de la page de modification: " + e.getMessage());
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page de modification: " + e.getMessage());
        }
    }

    /**
     * Supprime un suivi après confirmation
     * @param suivi Suivi à supprimer
     */
    private void supprimerSuivi(SuiviTraitement suivi) {
        try {
            // Boîte de dialogue de confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le suivi");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce suivi ?");

            if (confirm.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
                suiviTraitementService.supprimer(suivi.getSuivitraitementId());
                chargerDonnees();
                lblStatus.setText("Suivi supprimé");
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de la suppression: " + e.getMessage());
            afficherErreur("Erreur de suppression", "Erreur base de données: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression: " + e.getMessage());
            afficherErreur("Erreur de suppression", e.getMessage());
        }
    }

    /**
     * Affiche une boîte de dialogue d'erreur
     * @param titre Titre de l'erreur
     * @param message Message d'erreur
     */
    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}