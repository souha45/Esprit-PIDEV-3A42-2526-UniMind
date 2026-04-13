package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.Etudiant;
import org.example.entities.Traitement;
import org.example.services.EtudiantService;
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
import javafx.stage.Stage;


public class TraitementController implements Initializable {


    @FXML
    private TableView<LigneGroupée> tableViewTraitements;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblCount;

    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cmbFiltreStatut;
    @FXML
    private ComboBox<String> cmbFiltrePriorite;
    @FXML
    private Button btnAppliquerFiltres;
    @FXML
    private Button btnReinitialiserFiltres;

    @FXML
    private Button btnAjouter;
    @FXML
    private TableColumn<LigneGroupée, Void> colActions;

    //CLASSE INTERNE POUR L'AFFICHAGE GROUPÉ

    /**
     * Classe wrapper pour représenter une ligne groupée (étudiant + traitements)
     * Permet d'afficher les traitements groupés par étudiant dans le tableau
     */
    public static class LigneGroupée {
        private final String nomEtudiant;
        private final List<Traitement> traitements;
        private final boolean estEntete;

        /**
         * Constructeur pour une ligne d'en-tête d'étudiant
         * @param nomEtudiant Nom de l'étudiant
         * @param traitements Liste des traitements de l'étudiant
         */
        public LigneGroupée(String nomEtudiant, List<Traitement> traitements) {
            this.nomEtudiant = nomEtudiant;
            this.traitements = traitements;
            this.estEntete = true;
        }

        /**
         * Constructeur pour une ligne de traitement individuel
         * @param traitement Traitement à afficher
         */
        public LigneGroupée(Traitement traitement) {
            this.nomEtudiant = null;
            this.traitements = List.of(traitement);
            this.estEntete = false;
        }

        // Getters
        public String getNomEtudiant() { return nomEtudiant; }
        public List<Traitement> getTraitements() { return traitements; }
        public boolean estEntete() { return estEntete; }

        /**
         * Récupère le premier traitement de la ligne
         * @return Le premier traitement ou null
         */
        public Traitement getPremierTraitement() {
            return traitements.isEmpty() ? null : traitements.get(0);
        }
    }

    // SERVICES

    private TraitementService traitementService;  // Service pour les opérations CRUD des traitements
    private EtudiantService etudiantService;      // Service pour récupérer les informations des étudiants
    private ObservableList<LigneGroupée> lignesGroupéesList;  // Liste observable des lignes groupées


    /**
     * Méthode d'initialisation appelée automatiquement après le chargement du FXML
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialisation des services
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Initialisation des composants d'interface
            initialiserFiltres();
            configurerColonnes();
            chargerDonnees();

            // Adaptation de l'interface selon le rôle de l'utilisateur connecté
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            if (session.estEtudiant()) {
                // En mode étudiant : masquer le bouton d'ajout
                btnAjouter.setVisible(false);
                btnAjouter.setManaged(false);
                lblStatus.setText("Interface Traitements (consultation uniquement) - Mode étudiant");
            } else {
                // En mode psychologue : tous les boutons sont visibles
                lblStatus.setText("Interface Traitements - Mode psychologue");
            }

        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    // CHARGEMENT DES DONNÉES

    private void chargerDonnees() throws SQLException {
        try {
            // Vérification et réinitialisation des services si nécessaire
            if (traitementService == null) {
                traitementService = new TraitementService();
            }
            if (etudiantService == null) {
                etudiantService = new EtudiantService();
            }

            // Récupération de tous les traitements
            List<Traitement> traitements = traitementService.afficher();

            // Filtrage des traitements selon les permissions de l'utilisateur connecté
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            List<Traitement> traitementsFiltres = new ArrayList<>();

            for (Traitement traitement : traitements) {
                if (session.peutVoirTraitement(traitement.getEtudiantId(), traitement.getPsychologueId())) {
                    traitementsFiltres.add(traitement);
                }
            }

            // Tri des traitements par nom d'étudiant pour un affichage organisé
            List<Traitement> traitementsTries = traitementsFiltres.stream()
                    .sorted((t1, t2) -> {
                        try {
                            String nom1 = getNomEtudiant(t1.getEtudiantId());
                            String nom2 = getNomEtudiant(t2.getEtudiantId());
                            return nom1.compareToIgnoreCase(nom2);
                        } catch (Exception e) {
                            return Integer.compare(t1.getEtudiantId(), t2.getEtudiantId());
                        }
                    })
                    .collect(Collectors.toList());

            // Création des lignes groupées (en-têtes étudiants + traitements individuels)
            List<LigneGroupée> lignes = creerLignesGroupées(traitementsTries);
            lignesGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewTraitements.setItems(lignesGroupéesList);

            // Mise à jour du compteur (uniquement les lignes de traitement, pas les en-têtes)
            long totalTraitements = lignes.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalTraitements + " traitement(s)");

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    /**
     * Crée les lignes groupées pour l'affichage hiérarchique (étudiant -> traitements)
     * @param traitements Liste des traitements à organiser
     * @return Liste des lignes groupées (en-têtes et traitements)
     */
    private List<LigneGroupée> creerLignesGroupées(List<Traitement> traitements) throws SQLException {
        List<LigneGroupée> lignes = new ArrayList<>();

        // Regroupement des traitements par ID d'étudiant
        Map<Integer, List<Traitement>> traitementsParEtudiant = traitements.stream()
                .collect(Collectors.groupingBy(Traitement::getEtudiantId));

        // Pour chaque étudiant, créer une ligne d'en-tête puis ses traitements
        for (Map.Entry<Integer, List<Traitement>> entry : traitementsParEtudiant.entrySet()) {
            Integer etudiantId = entry.getKey();
            List<Traitement> traitementsEtudiant = entry.getValue();

            try {
                // Récupération du nom de l'étudiant
                String nomEtudiant = getNomEtudiant(etudiantId);
                // Création de la ligne d'en-tête avec le nombre de traitements
                LigneGroupée ligneEntete = new LigneGroupée(nomEtudiant + " (" + traitementsEtudiant.size() + " traitement(s))", traitementsEtudiant);
                lignes.add(ligneEntete);

                // Ajout de chaque traitement individuel
                for (Traitement traitement : traitementsEtudiant) {
                    lignes.add(new LigneGroupée(traitement));
                }
            } catch (Exception e) {
                // En cas d'erreur, affichage avec un nom par défaut
                LigneGroupée ligneEntete = new LigneGroupée("Étudiant #" + etudiantId + " (" + traitementsEtudiant.size() + " traitement(s))", traitementsEtudiant);
                lignes.add(ligneEntete);
                for (Traitement traitement : traitementsEtudiant) {
                    lignes.add(new LigneGroupée(traitement));
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
        } catch (Exception e) {
            return "Erreur #" + etudiantId;
        }
    }


    /**
     * Configure les colonnes du tableau avec leurs largeurs et cell factories
     */
    @SuppressWarnings("unchecked")
    private void configurerColonnes() {
        // Politique de redimensionnement : la dernière colonne prend l'espace restant
        tableViewTraitements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Récupération des colonnes par leur index
        TableColumn<LigneGroupée, String> colEtudiant = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(0);
        TableColumn<LigneGroupée, String> colTitre = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(1);
        TableColumn<LigneGroupée, String> colType = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(2);
        TableColumn<LigneGroupée, String> colCategorie = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(3);
        TableColumn<LigneGroupée, String> colDuree = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(4);
        TableColumn<LigneGroupée, String> colStatut = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(5);
        TableColumn<LigneGroupée, String> colPriorite = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(6);
        TableColumn<LigneGroupée, String> colDateDebut = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(7);
        TableColumn<LigneGroupée, String> colObjectif = (TableColumn<LigneGroupée, String>) tableViewTraitements.getColumns().get(8);

        // Définition des largeurs préférées pour chaque colonne
        colEtudiant.setPrefWidth(200);
        colTitre.setPrefWidth(180);
        colType.setPrefWidth(120);
        colCategorie.setPrefWidth(120);
        colDuree.setPrefWidth(80);
        colStatut.setPrefWidth(100);
        colPriorite.setPrefWidth(90);
        colDateDebut.setPrefWidth(120);
        colObjectif.setPrefWidth(250);

        //COLONNE ÉTUDIANT
        // Affiche le nom de l'étudiant sur les lignes d'en-tête, vide pour les lignes de traitement
        colEtudiant.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            if (ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(ligne.getNomEtudiant());
            } else {
                return new javafx.beans.property.SimpleStringProperty("");
            }
        });

        // Style personnalisé pour la colonne Étudiant
        colEtudiant.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("");
                    setStyle("");
                } else if (getTableRow().getItem().estEntete()) {
                    // Style pour les lignes d'en-tête d'étudiant
                    setText(item);
                    setStyle("-fx-background-color: #ede9fe; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #4f46e5; -fx-padding: 12 8;");
                } else {
                    // Style pour les lignes de traitement
                    setText("");
                    setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                }
            }
        });

        //  COLONNE TITRE
        colTitre.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(t.getTitre());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // COLONNE TYPE
        colType.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(t.getType());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        //  COLONNE CATÉGORIE
        colCategorie.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(t.getCategorie().toString());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        //  COLONNE DURÉE
        colDuree.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(String.valueOf(t.getDureeJours()));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // COLONNE STATUT avec badge coloré
        colStatut.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(t.getStatut().name());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Création d'un badge pour le statut
        colStatut.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    // Application de la classe CSS selon la valeur du statut
                    switch(item) {
                        case "EN_COURS":
                            badge.getStyleClass().add("badge-EN_COURS");
                            break;
                        case "TERMINE":
                            badge.getStyleClass().add("badge-TERMINE");
                            break;
                        case "SUSPENDU":
                            badge.getStyleClass().add("badge-SUSPENDU");
                            break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        //  COLONNE PRIORITÉ avec badge coloré
        colPriorite.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(t.getPriorite().name());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Création d'un badge pour la priorité
        colPriorite.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    // Application de la classe CSS selon la valeur de la priorité
                    switch(item) {
                        case "HAUTE":
                            badge.getStyleClass().add("priority-HAUTE");
                            break;
                        case "MOYENNE":
                            badge.getStyleClass().add("priority-MOYENNE");
                            break;
                        case "BASSE":
                            badge.getStyleClass().add("priority-BASSE");
                            break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // COLONNE DATE DÉBUT
        colDateDebut.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete() && t.getDateDebut() != null) {
                return new javafx.beans.property.SimpleStringProperty(t.getDateDebut().toString());
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // COLONNE OBJECTIF (avec troncature si trop long)
        colObjectif.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.estEntete()) {
                String obj = t.getObjectifTherapeutique();
                // Troncature à 50 caractères avec ajout de "..."
                if (obj != null && obj.length() > 50) {
                    obj = obj.substring(0, 47) + "...";
                }
                return new javafx.beans.property.SimpleStringProperty(obj != null ? obj : "");
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

        colActions.setCellFactory(param -> new TableCell<>() {
            // Création des boutons d'action
            private final Button btnView = new Button("Afficher");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(8, btnView, btnEdit, btnDelete);

            {
                // Centrage des boutons dans la cellule
                container.setAlignment(Pos.CENTER);

                // Application des styles CSS
                btnView.getStyleClass().addAll("table-action-button", "table-action-button-view");
                btnEdit.getStyleClass().addAll("table-action-button", "table-action-button-edit");
                btnDelete.getStyleClass().addAll("table-action-button", "table-action-button-delete");

                // Largeur fixe pour uniformiser les boutons
                btnView.setPrefWidth(70);
                btnEdit.setPrefWidth(70);
                btnDelete.setPrefWidth(70);

                // Action du bouton Afficher
                btnView.setOnAction(event -> {
                    LigneGroupée ligne = getTableView().getItems().get(getIndex());
                    Traitement traitement = ligne.getPremierTraitement();
                    if (traitement != null && !ligne.estEntete()) {
                        ouvrirPageAffichage(traitement);
                    }
                });

                // Action du bouton Modifier
                btnEdit.setOnAction(event -> {
                    LigneGroupée ligne = getTableView().getItems().get(getIndex());
                    Traitement traitement = ligne.getPremierTraitement();
                    if (traitement != null && !ligne.estEntete()) {
                        ouvrirPageModification(traitement);
                    }
                });

                // Action du bouton Supprimer
                btnDelete.setOnAction(event -> {
                    LigneGroupée ligne = getTableView().getItems().get(getIndex());
                    Traitement traitement = ligne.getPremierTraitement();
                    if (traitement != null && !ligne.estEntete()) {
                        supprimerTraitement(traitement);
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
                    if (ligne.estEntete()) {
                        // Pas de boutons sur les lignes d'en-tête
                        setGraphic(null);
                    } else {
                        // Adaptation selon le rôle de l'utilisateur
                        if (session.estEtudiant()) {
                            // Étudiant : uniquement le bouton Afficher
                            setGraphic(btnView);
                        } else {
                            // Psychologue : tous les boutons
                            setGraphic(container);
                        }
                    }
                }
            }
        });
    }

    // FILTRAGE ET RECHERCHE

    /**
     * Initialise les composants de filtrage (ComboBox et listeners)
     */
    private void initialiserFiltres() {
        // Configuration du filtre par statut
        ObservableList<String> statuts = FXCollections.observableArrayList(
                "Tous les statuts", "EN_COURS", "TERMINE", "SUSPENDU"
        );
        cmbFiltreStatut.setItems(statuts);
        cmbFiltreStatut.setValue("Tous les statuts");

        // Configuration du filtre par priorité
        ObservableList<String> priorites = FXCollections.observableArrayList(
                "Toutes les priorités", "HAUTE", "MOYENNE", "BASSE"
        );
        cmbFiltrePriorite.setItems(priorites);
        cmbFiltrePriorite.setValue("Toutes les priorités");

        // Ajout des listeners pour appliquer les filtres en temps réel
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cmbFiltreStatut.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cmbFiltrePriorite.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
    }

    /**
     * Applique les filtres (recherche textuelle, statut, priorité)
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
        cmbFiltreStatut.setValue("Tous les statuts");
        cmbFiltrePriorite.setValue("Toutes les priorités");
        appliquerFiltres();
    }

    /**
     * Logique principale de filtrage des traitements
     */
    private void appliquerFiltres() {
        try {
            // Récupération de tous les traitements
            List<Traitement> tousLesTraitements = traitementService.afficher();

            // Filtrage selon les permissions de l'utilisateur
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            List<Traitement> traitementsFiltres = new ArrayList<>();

            for (Traitement traitement : tousLesTraitements) {
                if (session.peutVoirTraitement(traitement.getEtudiantId(), traitement.getPsychologueId())) {
                    traitementsFiltres.add(traitement);
                }
            }

            // Application des filtres supplémentaires
            traitementsFiltres = filtrerTraitements(traitementsFiltres);

            // Regroupement par étudiant pour l'affichage
            Map<String, List<Traitement>> traitementsParEtudiant = new HashMap<>();
            for (Traitement traitement : traitementsFiltres) {
                String nomEtudiant = getNomEtudiant(traitement.getEtudiantId());
                traitementsParEtudiant.computeIfAbsent(nomEtudiant, k -> new ArrayList<>()).add(traitement);
            }

            // Création des lignes groupées
            lignesGroupéesList = FXCollections.observableArrayList();
            for (Map.Entry<String, List<Traitement>> entry : traitementsParEtudiant.entrySet()) {
                lignesGroupéesList.add(new LigneGroupée(entry.getKey(), new ArrayList<>()));
                for (Traitement traitement : entry.getValue()) {
                    lignesGroupéesList.add(new LigneGroupée(traitement));
                }
            }

            // Mise à jour de l'affichage
            tableViewTraitements.setItems(lignesGroupéesList);
            long totalTraitements = lignesGroupéesList.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalTraitements + " traitement(s)");

        } catch (SQLException e) {
            afficherErreur("Erreur", "Impossible d'appliquer les filtres: " + e.getMessage());
        }
    }

    /**
     * Filtre la liste des traitements selon les critères saisis
     * @param traitements Liste des traitements à filtrer
     * @return Liste filtrée
     */
    private List<Traitement> filtrerTraitements(List<Traitement> traitements) {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String statutFiltre = cmbFiltreStatut.getValue();
        String prioriteFiltre = cmbFiltrePriorite.getValue();

        return traitements.stream()
                .filter(t -> {
                    // Filtre par recherche textuelle
                    if (!recherche.isEmpty()) {
                        boolean correspondRecherche =
                                (t.getTitre() != null && t.getTitre().toLowerCase().contains(recherche)) ||
                                        (t.getType() != null && t.getType().toLowerCase().contains(recherche)) ||
                                        (t.getObjectifTherapeutique() != null && t.getObjectifTherapeutique().toLowerCase().contains(recherche));
                        if (!correspondRecherche) return false;
                    }
                    // Filtre par statut
                    if (!"Tous les statuts".equals(statutFiltre)) {
                        if (t.getStatut() == null || !t.getStatut().name().equals(statutFiltre)) return false;
                    }
                    // Filtre par priorité
                    if (!"Toutes les priorités".equals(prioriteFiltre)) {
                        if (t.getPriorite() == null || !t.getPriorite().name().equals(prioriteFiltre)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ACTIONS PRINCIPALES

    /**
     * Ouvre la page d'ajout d'un nouveau traitement
     */
    @FXML
    private void handleAjouter() {
        ouvrirPageAjout();
    }

    /**
     * Rafraîchit la liste des traitements
     */
    @FXML
    private void handleRafraichir() {
        try {
            chargerDonnees();
            lblStatus.setText("Liste rafraîchie");
        } catch (Exception e) {
            afficherErreur("Erreur de rafraîchissement", e.getMessage());
        }
    }

    /**
     * Navigue vers la page de suivi des traitements
     */
    @FXML
    private void ouvrirSuiviTraitementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/suivi-traitement-view.fxml"));
            Parent root = loader.load();
            Scene currentScene = tableViewTraitements.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            afficherErreur("Erreur de navigation", "Impossible d'accéder à la page des suivis: " + e.getMessage());
        }
    }

    // MÉTHODES DE NAVIGATION

    /**
     * Ouvre la fenêtre d'ajout d'un traitement
     */
    private void ouvrirPageAjout() {
        try {
            // Vérification des permissions
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            if (!session.peutCreerTraitement()) {
                afficherErreur("Accès refusé", "Seul le psychologue peut créer des traitements.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-ajout-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Traitement");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
            lblStatus.setText("Page d'ajout ouverte");
        } catch (Exception e) {
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page d'ajout: " + e.getMessage());
        }
    }

    /**
     * Ouvre la fenêtre d'affichage des détails d'un traitement
     * @param traitement Traitement à afficher
     */
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
            lblStatus.setText("Page d'affichage ouverte pour: " + traitement.getTitre());
        } catch (Exception e) {
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page d'affichage: " + e.getMessage());
        }
    }

    /**
     * Ouvre la fenêtre de modification d'un traitement
     * @param traitement Traitement à modifier
     */
    private void ouvrirPageModification(Traitement traitement) {
        try {
            // Vérification des permissions
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            if (!session.peutModifierTraitement()) {
                afficherErreur("Accès refusé", "Seul le psychologue peut modifier des traitements.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-modification-view.fxml"));
            Parent root = loader.load();
            TraitementModificationController controller = loader.getController();
            controller.setTraitement(traitement);
            Stage stage = new Stage();
            stage.setTitle("Modifier un Traitement");
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
            lblStatus.setText("Page de modification ouverte pour: " + traitement.getTitre());
        } catch (Exception e) {
            afficherErreur("Erreur d'ouverture", "Impossible d'ouvrir la page de modification: " + e.getMessage());
        }
    }

    /**
     * Supprime un traitement après confirmation
     * @param traitement Traitement à supprimer
     */
    private void supprimerTraitement(Traitement traitement) {
        try {
            // Vérification des permissions
            org.example.utils.SessionManager session = org.example.utils.SessionManager.getInstance();
            if (!session.peutSupprimerTraitement()) {
                afficherErreur("Accès refusé", "Seul le psychologue peut supprimer des traitements.");
                return;
            }

            // Boîte de dialogue de confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le traitement");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer le traitement \"" + traitement.getTitre() + "\" ?");

            if (confirm.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
                traitementService.supprimer(traitement.getTraitementId());
                chargerDonnees();
                lblStatus.setText("Traitement supprimé: " + traitement.getTitre());
            }
        } catch (Exception e) {
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