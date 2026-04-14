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
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.services.EtudiantService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private SuiviTraitementService suiviTraitementService;
    private ObservableList<LigneGroupée> lignesGroupéesList;
    private Map<Integer, Integer> cacheNbSuivis;

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
        SessionManager session = SessionManager.getInstance();

        if (session.estEtudiant()) {
            Platform.runLater(() -> ouvrirInterfaceEtudiant());
            return;
        }

        try {
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();
            suiviTraitementService = new SuiviTraitementService();
            cacheNbSuivis = new HashMap<>();

            initialiserFiltres();
            configurerColonnes();
            chargerDonnees();

            lblStatus.setText("Interface Traitements - Mode psychologue");

        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ouvrirInterfaceEtudiant() {
        try {
            if (tableViewTraitements.getScene() == null) {
                Platform.runLater(() -> ouvrirInterfaceEtudiant());
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-etudiant-view.fxml"));
            Parent root = loader.load();
            Scene currentScene = tableViewTraitements.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            if (lblStatus != null) {
                lblStatus.setText("Erreur de redirection: " + e.getMessage());
            }
        }
    }

    // ==================== CHARGEMENT DES DONNÉES ====================

    private void chargerDonnees() throws SQLException {
        try {
            List<Traitement> traitements = traitementService.afficher();
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();

            cacheNbSuivis.clear();
            for (SuiviTraitement suivi : tousLesSuivis) {
                int traitementId = suivi.getTraitementId();
                cacheNbSuivis.put(traitementId, cacheNbSuivis.getOrDefault(traitementId, 0) + 1);
            }

            SessionManager session = SessionManager.getInstance();
            List<Traitement> traitementsFiltres = new ArrayList<>();

            for (Traitement traitement : traitements) {
                if (session.peutVoirTraitement(traitement.getEtudiantId(), traitement.getPsychologueId())) {
                    traitementsFiltres.add(traitement);
                }
            }

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

            List<LigneGroupée> lignes = creerLignesGroupées(traitementsTries);
            lignesGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewTraitements.setItems(lignesGroupéesList);

            long totalTraitements = lignes.stream().filter(l -> l.getTraitement() != null).count();
            lblCount.setText(totalTraitements + " traitement(s)");

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            afficherErreur("Erreur de chargement", e.getMessage());
        }
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
        try {
            if (etudiantId == null || etudiantId == 0) return "Non assigné";
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

        TableColumn<LigneGroupée, String> colSuivis = new TableColumn<>("SUIVIS");
        tableViewTraitements.getColumns().add(9, colSuivis);

        colEtudiant.setPrefWidth(180);
        colTitre.setPrefWidth(180);
        colType.setPrefWidth(120);
        colCategorie.setPrefWidth(120);
        colDuree.setPrefWidth(80);
        colStatut.setPrefWidth(100);
        colPriorite.setPrefWidth(90);
        colDateDebut.setPrefWidth(120);
        colObjectif.setPrefWidth(200);
        colSuivis.setPrefWidth(120);

        // Colonne Étudiant
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
                        setStyle("-fx-background-color: #f3f4f6; -fx-padding: 2px;");
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

        // Colonne Titre
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

        // Colonne Type
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

        // Colonne Catégorie
        colCategorie.setCellValueFactory(param -> {
            LigneGroupée ligne = param.getValue();
            Traitement t = ligne.getPremierTraitement();
            if (t != null && !ligne.isEstLigneSeparateur()) {
                return new javafx.beans.property.SimpleStringProperty(t.getCategorie().toString());
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

        // Colonne Durée
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
                        setText(item);
                        setStyle("-fx-background-color: white; -fx-padding: 10 8;");
                    }
                }
            }
        });

        // Colonne Statut
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
                        Label badge = new Label(item);
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

        // Colonne Priorité
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
                        Label badge = new Label(item);
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

        // Colonne Date Début
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

        // Colonne Objectif
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

        // Colonne Suivis
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

        colActions.setCellFactory(param -> new TableCell<LigneGroupée, Void>() {
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
                    LigneGroupée ligne = getTableView().getItems().get(getIndex());
                    Traitement traitement = ligne.getPremierTraitement();
                    if (traitement != null) {
                        ouvrirPageAffichage(traitement);
                    }
                });

                btnEdit.setOnAction(event -> {
                    LigneGroupée ligne = getTableView().getItems().get(getIndex());
                    Traitement traitement = ligne.getPremierTraitement();
                    if (traitement != null) {
                        ouvrirPageModification(traitement);
                    }
                });

                btnDelete.setOnAction(event -> {
                    LigneGroupée ligne = getTableView().getItems().get(getIndex());
                    Traitement traitement = ligne.getPremierTraitement();
                    if (traitement != null) {
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
                    if (ligne.isEstLigneSeparateur()) {
                        setGraphic(null);
                        setStyle("-fx-background-color: #e5e7eb; -fx-padding: 4px;");
                    } else if (ligne.getTraitement() != null) {
                        setGraphic(container);
                        setStyle("-fx-background-color: white; -fx-padding: 8px;");
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    // ==================== FILTRAGE ====================

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

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cmbFiltreStatut.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cmbFiltrePriorite.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
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
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        try {
            List<Traitement> tousLesTraitements = traitementService.afficher();

            SessionManager session = SessionManager.getInstance();
            List<Traitement> traitementsFiltres = new ArrayList<>();

            for (Traitement traitement : tousLesTraitements) {
                if (session.peutVoirTraitement(traitement.getEtudiantId(), traitement.getPsychologueId())) {
                    traitementsFiltres.add(traitement);
                }
            }

            traitementsFiltres = filtrerTraitements(traitementsFiltres);

            List<LigneGroupée> lignes = creerLignesGroupées(traitementsFiltres);
            lignesGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewTraitements.setItems(lignesGroupéesList);

            long totalTraitements = lignes.stream().filter(l -> l.getTraitement() != null).count();
            lblCount.setText(totalTraitements + " traitement(s)");

        } catch (SQLException e) {
            afficherErreur("Erreur", "Impossible d'appliquer les filtres: " + e.getMessage());
        }
    }

    private List<Traitement> filtrerTraitements(List<Traitement> traitements) {
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

    // ==================== NAVIGATION ====================

    @FXML
    private void handleAjouter() {
        ouvrirPageAjout();
    }

    @FXML
    private void handleRafraichir() {
        try {
            chargerDonnees();
            lblStatus.setText("Liste rafraîchie");
        } catch (Exception e) {
            afficherErreur("Erreur de rafraîchissement", e.getMessage());
        }
    }

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

    private void ouvrirPageAjout() {
        try {
            SessionManager session = SessionManager.getInstance();
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

    private void ouvrirPageModification(Traitement traitement) {
        try {
            SessionManager session = SessionManager.getInstance();
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

    private void supprimerTraitement(Traitement traitement) {
        try {
            SessionManager session = SessionManager.getInstance();
            if (!session.peutSupprimerTraitement()) {
                afficherErreur("Accès refusé", "Seul le psychologue peut supprimer des traitements.");
                return;
            }

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

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}