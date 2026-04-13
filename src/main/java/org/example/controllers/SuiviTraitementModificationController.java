package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
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
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SuiviTraitementModificationController implements Initializable {

    @FXML
    private DatePicker dpDateSuivi;
    @FXML
    private ComboBox<Traitement> cmbTraitement;
    @FXML
    private ComboBox<Etudiant> cmbEtudiant;
    @FXML
    private TextField txtPsychologue;
    @FXML
    private TextArea txtNotes;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblEtudiant;
    @FXML
    private Label lblPsychologue;

    //  SERVICES

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Traitement> traitementsList;
    private ObservableList<Etudiant> etudiantsList;
    private SuiviTraitement suiviSelectionne;

    // INITIALISATION

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Chargement des données
            chargerTraitements();
            chargerEtudiants();

            // Configuration des ComboBox
            configurerComboBoxTraitement();
            configurerComboBoxEtudiant();

            // Configuration selon le rôle
            SessionManager session = SessionManager.getInstance();
            if (session.estPsychologue()) {
                initialiserFormulairePsychologue();
            } else {
                initialiserFormulaireEtudiant();
            }

            // Configuration du psychologue
            txtPsychologue.setText(session.getNomUtilisateur());
            txtPsychologue.setEditable(false);

            lblStatus.setText("✓ Prêt à modifier le suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Charge tous les traitements
     */
    private void chargerTraitements() throws SQLException {
        List<Traitement> traitements = traitementService.afficher();
        traitementsList = FXCollections.observableArrayList(traitements);
        cmbTraitement.setItems(traitementsList);
    }

    /**
     * Charge tous les étudiants
     */
    private void chargerEtudiants() throws SQLException {
        List<Etudiant> etudiants = etudiantService.afficher();
        etudiantsList = FXCollections.observableArrayList(etudiants);
        cmbEtudiant.setItems(etudiantsList);
    }

    /**
     * Configure l'affichage des traitements
     */
    private void configurerComboBoxTraitement() {
        cmbTraitement.setCellFactory(param -> new javafx.scene.control.ListCell<Traitement>() {
            @Override
            protected void updateItem(Traitement traitement, boolean empty) {
                super.updateItem(traitement, empty);
                if (empty || traitement == null) {
                    setText(null);
                } else {
                    setText(traitement.getTitre());
                }
            }
        });

        cmbTraitement.setButtonCell(new javafx.scene.control.ListCell<Traitement>() {
            @Override
            protected void updateItem(Traitement traitement, boolean empty) {
                super.updateItem(traitement, empty);
                if (empty || traitement == null) {
                    setText("Sélectionner un traitement");
                } else {
                    setText(traitement.getTitre());
                }
            }
        });
    }

    /**
     * Configure l'affichage des étudiants
     */
    private void configurerComboBoxEtudiant() {
        cmbEtudiant.setCellFactory(param -> new javafx.scene.control.ListCell<Etudiant>() {
            @Override
            protected void updateItem(Etudiant etudiant, boolean empty) {
                super.updateItem(etudiant, empty);
                if (empty || etudiant == null) {
                    setText(null);
                } else {
                    setText(etudiant.getNom() + " " + etudiant.getPrenom());
                }
            }
        });

        cmbEtudiant.setButtonCell(new javafx.scene.control.ListCell<Etudiant>() {
            @Override
            protected void updateItem(Etudiant etudiant, boolean empty) {
                super.updateItem(etudiant, empty);
                if (empty || etudiant == null) {
                    setText("Sélectionner un étudiant");
                } else {
                    setText(etudiant.getNom() + " " + etudiant.getPrenom());
                }
            }
        });
    }

    /**
     * Initialise le formulaire pour le psychologue avec filtrage
     */
    private void initialiserFormulairePsychologue() {
        // Filtrer les traitements selon l'étudiant sélectionné
        cmbEtudiant.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<Traitement> traitementsFiltres = traitementsList.stream()
                        .filter(t -> t.getEtudiantId() == newVal.getUserId())
                        .collect(Collectors.toList());
                cmbTraitement.setItems(FXCollections.observableArrayList(traitementsFiltres));
                cmbTraitement.setValue(null);
            } else {
                cmbTraitement.setItems(traitementsList);
            }
        });
    }

    /**
     * Initialise le formulaire pour l'étudiant
     */
    private void initialiserFormulaireEtudiant() {
        cmbEtudiant.setVisible(false);
        cmbEtudiant.setManaged(false);
        lblEtudiant.setVisible(false);
        lblEtudiant.setManaged(false);
        lblPsychologue.setVisible(false);
        lblPsychologue.setManaged(false);
    }

    /**
     * Initialise le formulaire avec le suivi à modifier
     */
    public void setSuiviTraitement(SuiviTraitement suivi) {
        this.suiviSelectionne = suivi;

        if (suivi == null) {
            lblStatus.setText("✗ Erreur: Aucun suivi à modifier");
            return;
        }

        // Date
        if (suivi.getDateSuivi() != null) {
            dpDateSuivi.setValue(suivi.getDateSuivi().toLocalDate());
        }

        // Traitement
        for (Traitement traitement : traitementsList) {
            if (traitement.getTraitementId() == suivi.getTraitementId()) {
                cmbTraitement.setValue(traitement);

                // Sélectionner automatiquement l'étudiant associé
                SessionManager session = SessionManager.getInstance();
                if (session.estPsychologue()) {
                    for (Etudiant etudiant : etudiantsList) {
                        if (etudiant.getUserId() == traitement.getEtudiantId()) {
                            cmbEtudiant.setValue(etudiant);
                            break;
                        }
                    }
                }
                break;
            }
        }

        // Notes
        txtNotes.setText(suivi.getObservations() != null ? suivi.getObservations() : "");

        lblStatus.setText("✓ Modification du suivi du " + (suivi.getDateSuivi() != null ? suivi.getDateSuivi().toString() : "date inconnue"));
    }

    //  ACTIONS

    @FXML
    private void handleEnregistrer() {
        try {
            if (validerFormulaire()) {
                modifierSuivi();
                lblStatus.setText("✓ Suivi modifié avec succès !");
                afficherSucces("Modification réussie", "Le suivi a été modifié avec succès.");
                fermerFenetre();
            }
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors de la modification: " + e.getMessage());
            afficherErreur("Erreur lors de la modification", e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    @FXML
    private void handleReinitialiser() {
        if (suiviSelectionne != null) {
            setSuiviTraitement(suiviSelectionne);
            lblStatus.setText("✓ Formulaire réinitialisé");
        }
    }

    // MÉTHODES MÉTIER
    /**
     * Modifie le suivi avec vérification de cohérence
     */
    private void modifierSuivi() throws SQLException {
        if (suiviSelectionne == null) {
            afficherErreur("Aucune sélection", "Aucun suivi à modifier");
            return;
        }

        SessionManager session = SessionManager.getInstance();

        // Récupérer le traitement sélectionné
        Traitement traitementSelectionne = cmbTraitement.getValue();
        if (traitementSelectionne == null) {
            afficherErreur("Traitement manquant", "Veuillez sélectionner un traitement");
            return;
        }

        // Vérification de cohérence pour le psychologue
        if (session.estPsychologue()) {
            Etudiant etudiantSelectionne = cmbEtudiant.getValue();
            if (etudiantSelectionne == null) {
                afficherErreur("Étudiant manquant", "Veuillez sélectionner un étudiant");
                return;
            }

            if (traitementSelectionne.getEtudiantId() != etudiantSelectionne.getUserId()) {
                afficherErreur("Incohérence", "Le traitement ne correspond pas à l'étudiant sélectionné.");
                return;
            }
        }

        // Mise à jour
        suiviSelectionne.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        suiviSelectionne.setSaisiPar(session.getRoleSaisiPar());

        if (dpDateSuivi.getValue() != null) {
            suiviSelectionne.setDateSuivi(java.sql.Date.valueOf(dpDateSuivi.getValue()));
        }

        suiviSelectionne.setTraitementId(traitementSelectionne.getTraitementId());
        suiviSelectionne.setObservations(txtNotes.getText());

        suiviTraitementService.modifier(suiviSelectionne);
        System.out.println("Suivi modifié - Traitement ID: " + traitementSelectionne.getTraitementId());
    }

    /**
     * Valide le formulaire
     */
    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();
        SessionManager session = SessionManager.getInstance();

        if (dpDateSuivi.getValue() == null) {
            erreurs.append("• La date de suivi est obligatoire.\n");
        }
        if (cmbTraitement.getValue() == null) {
            erreurs.append("• Le traitement est obligatoire.\n");
        }
        if (session.estPsychologue() && cmbEtudiant.getValue() == null) {
            erreurs.append("• L'étudiant est obligatoire.\n");
        }

        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }
        return true;
    }

    /**
     * Ferme la fenêtre
     */
    private void fermerFenetre() {
        Stage stage = (Stage) dpDateSuivi.getScene().getWindow();
        stage.close();
    }

    /**
     * Affiche une erreur
     */
    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche un succès
     */
    private void afficherSucces(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}