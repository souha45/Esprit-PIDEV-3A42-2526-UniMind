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
import org.example.enums.SaisiPar;
import org.example.services.EtudiantService;
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
import javafx.stage.Stage;

public class SuiviTraitementController implements Initializable {

    // Classe wrapper pour représenter une ligne groupée
    public static class LigneSuiviGroupée {
        private final String nomEtudiant;
        private final String nomTraitement;
        private final List<SuiviTraitement> suivis;
        private final boolean estEnteteEtudiant;
        private final boolean estEnteteTraitement;

        public LigneSuiviGroupée(String nomEtudiant, List<SuiviTraitement> suivis) {
            this.nomEtudiant = nomEtudiant;
            this.nomTraitement = null;
            this.suivis = suivis;
            this.estEnteteEtudiant = true;
            this.estEnteteTraitement = false;
        }

        public LigneSuiviGroupée(String nomEtudiant, String nomTraitement, List<SuiviTraitement> suivis) {
            this.nomEtudiant = nomEtudiant;
            this.nomTraitement = nomTraitement;
            this.suivis = suivis;
            this.estEnteteEtudiant = false;
            this.estEnteteTraitement = true;
        }

        public LigneSuiviGroupée(SuiviTraitement suivi) {
            this.nomEtudiant = null;
            this.nomTraitement = null;
            this.suivis = List.of(suivi);
            this.estEnteteEtudiant = false;
            this.estEnteteTraitement = false;
        }

        public String getNomEtudiant() { return nomEtudiant; }
        public String getNomTraitement() { return nomTraitement; }
        public List<SuiviTraitement> getSuivis() { return suivis; }
        public boolean estEnteteEtudiant() { return estEnteteEtudiant; }
        public boolean estEnteteTraitement() { return estEnteteTraitement; }
        public boolean estEntete() { return estEnteteEtudiant || estEnteteTraitement; }
        public SuiviTraitement getPremierSuivi() {
            return suivis.isEmpty() ? null : suivis.get(0);
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
            return suivis.size();
        }
    }

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

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<LigneSuiviGroupée> lignesSuivisGroupéesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            initialiserFiltres();
            configurerColonnes();
            chargerDonnees();

            lblStatus.setText("Interface Suivis Traitements prête");

        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerDonnees() {
        try {
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            List<Traitement> tousLesTraitements = traitementService.afficher();
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            SessionManager session = SessionManager.getInstance();
            List<SuiviTraitement> suivisFiltres = new ArrayList<>();

            for (SuiviTraitement suivi : tousLesSuivis) {
                Traitement traitementAssocie = traitementsMap.get(suivi.getTraitementId());
                if (traitementAssocie == null) continue;

                int etudiantId = traitementAssocie.getEtudiantId();

                boolean peutVoir = false;

                if (session.estPsychologue()) {
                    peutVoir = true;
                } else if (session.estEtudiant()) {
                    // L'étudiant voit ses propres suivis et ceux du psychologue pour ses traitements
                    peutVoir = (etudiantId == session.getUtilisateurConnecteId());
                } else {
                    peutVoir = true;
                }

                if (peutVoir) {
                    suivisFiltres.add(suivi);
                }
            }

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

    private List<LigneSuiviGroupée> creerLignesSuivisGroupées(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        List<LigneSuiviGroupée> lignes = new ArrayList<>();

        Map<Integer, List<SuiviTraitement>> suivisParEtudiant = new HashMap<>();

        for (SuiviTraitement suivi : suivis) {
            Traitement traitement = traitementsMap.get(suivi.getTraitementId());
            if (traitement != null) {
                suivisParEtudiant.computeIfAbsent(traitement.getEtudiantId(), k -> new ArrayList<>()).add(suivi);
            }
        }

        for (Map.Entry<Integer, List<SuiviTraitement>> entry : suivisParEtudiant.entrySet()) {
            Integer etudiantId = entry.getKey();
            List<SuiviTraitement> suivisEtudiant = entry.getValue();

            String nomEtudiant = getNomEtudiant(etudiantId);

            Map<Integer, List<SuiviTraitement>> suivisParTraitement = new HashMap<>();
            Map<Integer, String> nomsTraitements = new HashMap<>();

            for (SuiviTraitement suivi : suivisEtudiant) {
                Traitement traitement = traitementsMap.get(suivi.getTraitementId());
                if (traitement != null) {
                    suivisParTraitement.computeIfAbsent(suivi.getTraitementId(), k -> new ArrayList<>()).add(suivi);
                    nomsTraitements.put(suivi.getTraitementId(), traitement.getTitre());
                }
            }

            LigneSuiviGroupée ligneEnteteEtudiant = new LigneSuiviGroupée(nomEtudiant, suivisEtudiant);
            lignes.add(ligneEnteteEtudiant);

            for (Map.Entry<Integer, List<SuiviTraitement>> traitementEntry : suivisParTraitement.entrySet()) {
                Integer traitementId = traitementEntry.getKey();
                List<SuiviTraitement> suivisTraitement = traitementEntry.getValue();
                String nomTraitement = nomsTraitements.getOrDefault(traitementId, "Traitement #" + traitementId);

                LigneSuiviGroupée ligneEnteteTraitement = new LigneSuiviGroupée(nomEtudiant, nomTraitement, suivisTraitement);
                lignes.add(ligneEnteteTraitement);

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
            List<Etudiant> etudiants = etudiantService.afficher();
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
        TableColumn<LigneSuiviGroupée, String> colNotes = (TableColumn<LigneSuiviGroupée, String>) tableViewSuiviTraitements.getColumns().get(2);

        colEtudiant.setPrefWidth(400);
        colDateSuivi.setPrefWidth(150);
        colNotes.setPrefWidth(450);

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

        colDateSuivi.setCellValueFactory(param -> {
            LigneSuiviGroupée ligne = param.getValue();
            SuiviTraitement suivi = ligne.getPremierSuivi();
            if (suivi != null && !ligne.estEntete()) {
                return new javafx.beans.property.SimpleStringProperty(suivi.getDateSuivi() != null ? suivi.getDateSuivi().toString() : "");
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

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
                        // Vérifier si l'étudiant peut modifier CE suivi
                        if (session.estEtudiant()) {
                            // L'étudiant ne peut modifier que ses propres suivis
                            if (suivi.getSaisiPar() == SaisiPar.ETUDIANT) {
                                ouvrirPageModification(suivi);
                            } else {
                                afficherErreur("Accès refusé", "Vous ne pouvez pas modifier le suivi du psychologue.");
                            }
                        } else {
                            ouvrirPageModification(suivi);
                        }
                    }
                });

                btnDelete.setOnAction(event -> {
                    LigneSuiviGroupée ligne = getTableView().getItems().get(getIndex());
                    SuiviTraitement suivi = ligne.getPremierSuivi();
                    if (suivi != null && !ligne.estEntete()) {
                        // Vérifier si l'étudiant peut supprimer CE suivi
                        if (session.estEtudiant()) {
                            // L'étudiant ne peut supprimer que ses propres suivis
                            if (suivi.getSaisiPar() == SaisiPar.ETUDIANT) {
                                supprimerSuivi(suivi);
                            } else {
                                afficherErreur("Accès refusé", "Vous ne pouvez pas supprimer le suivi du psychologue.");
                            }
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
                    LigneSuiviGroupée ligne = getTableRow().getItem();
                    SuiviTraitement suivi = ligne.getPremierSuivi();

                    if (suivi != null) {
                        if (session.estEtudiant()) {
                            // Pour l'étudiant : afficher les boutons mais la vérification se fait dans l'action
                            setGraphic(container);
                        } else {
                            setGraphic(container);
                        }
                    } else {
                        setGraphic(container);
                    }
                }
            }
        });
    }

    // FILTRAGE
    private void initialiserFiltres() {
        ObservableList<String> periodes = FXCollections.observableArrayList(
                "Toutes les périodes", "Aujourd'hui", "Cette semaine", "Ce mois"
        );
        cmbFiltrePeriode.setItems(periodes);
        cmbFiltrePeriode.setValue("Toutes les périodes");

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        cmbFiltrePeriode.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
    }

    @FXML
    private void handleAppliquerFiltres() {
        appliquerFiltres();
    }

    @FXML
    private void handleReinitialiserFiltres() {
        txtRecherche.clear();
        cmbFiltrePeriode.setValue("Toutes les périodes");
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        try {
            List<SuiviTraitement> tousLesSuivis = suiviTraitementService.afficher();
            List<Traitement> tousLesTraitements = traitementService.afficher();
            Map<Integer, Traitement> traitementsMap = new HashMap<>();
            for (Traitement traitement : tousLesTraitements) {
                traitementsMap.put(traitement.getTraitementId(), traitement);
            }

            SessionManager session = SessionManager.getInstance();
            List<SuiviTraitement> suivisFiltres = new ArrayList<>();

            for (SuiviTraitement suivi : tousLesSuivis) {
                Traitement traitementAssocie = traitementsMap.get(suivi.getTraitementId());
                if (traitementAssocie == null) continue;

                int etudiantId = traitementAssocie.getEtudiantId();

                boolean peutVoir = false;
                if (session.estPsychologue()) {
                    peutVoir = true;
                } else if (session.estEtudiant()) {
                    peutVoir = (etudiantId == session.getUtilisateurConnecteId());
                } else {
                    peutVoir = true;
                }

                if (peutVoir) {
                    suivisFiltres.add(suivi);
                }
            }

            suivisFiltres = filtrerSuivis(suivisFiltres, traitementsMap);

            List<LigneSuiviGroupée> lignes = creerLignesSuivisGroupées(suivisFiltres, traitementsMap);
            lignesSuivisGroupéesList = FXCollections.observableArrayList(lignes);
            tableViewSuiviTraitements.setItems(lignesSuivisGroupéesList);

            long totalSuivis = lignes.stream().filter(l -> !l.estEntete()).count();
            lblCount.setText(totalSuivis + " suivi(s)");

        } catch (SQLException e) {
            afficherErreur("Erreur", "Impossible d'appliquer les filtres: " + e.getMessage());
        }
    }

    private List<SuiviTraitement> filtrerSuivis(List<SuiviTraitement> suivis, Map<Integer, Traitement> traitementsMap) {
        String recherche = txtRecherche.getText().toLowerCase().trim();
        String periodeFiltre = cmbFiltrePeriode.getValue();

        return suivis.stream()
                .filter(s -> {
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

                    if (!"Toutes les périodes".equals(periodeFiltre)) {
                        if (s.getDateSuivi() == null) return false;
                        if (!estDansPeriode(s.getDateSuivi().toLocalDate(), periodeFiltre)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
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

    // NAVIGATION
    @FXML
    private void handleAjouter() {
        ouvrirPageAjout();
    }

    @FXML
    private void handleRafraichir() {
        chargerDonnees();
        lblStatus.setText("Liste rafraîchie");
    }

    @FXML
    private void ouvrirTraitementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/traitement-view.fxml"));
            Parent root = loader.load();
            Scene currentScene = tableViewSuiviTraitements.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root);
            }
        } catch (Exception e) {
            afficherErreur("Erreur de navigation", "Impossible d'accéder à la page des traitements: " + e.getMessage());
        }
    }

    private void ouvrirPageAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/suivi-traitement-ajout-view.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/suivi-traitement-affichage-view.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/suivi-traitement-modification-view.fxml"));
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