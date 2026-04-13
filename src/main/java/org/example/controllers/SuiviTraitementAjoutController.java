package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.ResourceBundle;

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

/**
 * Contrôleur pour l'ajout d'un suivi de traitement
 * Style cohérent avec le thème violet professionnel
 */
public class SuiviTraitementAjoutController implements Initializable {

    // COMPOSANTS FXML
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

    // SERVICES

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Traitement> traitementsList;
    private ObservableList<Etudiant> etudiantsList;

    // INITIALISATION

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();

        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Adapter le formulaire selon le rôle
            if (session.estPsychologue()) {
                initialiserFormulairePsychologue();
            } else {
                initialiserFormulaireEtudiant();
            }

            // Chargement des traitements
            chargerTraitements(session);

            // Configuration de la ComboBox Traitement
            configurerComboBoxTraitement();

            // Configuration du psychologue connecté
            txtPsychologue.setText(SessionManager.getInstance().getNomUtilisateur());
            txtPsychologue.setEditable(false);

            lblStatus.setText("✓ Prêt à ajouter un suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Charge les traitements depuis la base de données
     * @param session SessionManager pour les filtres
     */
    private void chargerTraitements(SessionManager session) throws SQLException {
        List<Traitement> traitements = traitementService.afficher();
        traitementsList = FXCollections.observableArrayList(traitements);

        // Filtrer les traitements selon l'utilisateur
        if (session.estEtudiant()) {
            traitementsList.setAll(traitements.stream()
                    .filter(t -> session.peutVoirTraitement(t.getEtudiantId(), t.getPsychologueId()))
                    .toList());
        }

        cmbTraitement.setItems(traitementsList);
    }

    /**
     * Configure l'affichage de la ComboBox des traitements
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
     * Initialise le formulaire pour le mode psychologue
     */
    private void initialiserFormulairePsychologue() {
        try {
            // Chargement des étudiants
            List<Etudiant> etudiants = etudiantService.afficher();
            etudiantsList = FXCollections.observableArrayList(etudiants);
            cmbEtudiant.setItems(etudiantsList);

            // Configuration de la ComboBox Étudiant
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

            // Rendre visibles les champs étudiant
            cmbEtudiant.setVisible(true);
            cmbEtudiant.setManaged(true);
            lblEtudiant.setVisible(true);
            lblEtudiant.setManaged(true);

            // Prompt pour les notes
            txtNotes.setPromptText("Observations professionnelles sur le suivi...");

        } catch (SQLException e) {
            afficherErreur("Erreur", "Impossible de charger les étudiants: " + e.getMessage());
        }
    }

    /**
     * Initialise le formulaire pour le mode étudiant
     */
    private void initialiserFormulaireEtudiant() {
        // Masquer les champs et labels pour l'étudiant
        cmbEtudiant.setVisible(false);
        cmbEtudiant.setManaged(false);
        lblEtudiant.setVisible(false);
        lblEtudiant.setManaged(false);
        lblPsychologue.setVisible(false);
        lblPsychologue.setManaged(false);

        // Prompt pour les notes
        txtNotes.setPromptText("Décrivez comment vous vous sentez et votre progression...");
    }

    // ACTIONS DES BOUTONS

    /**
     * Enregistre le nouveau suivi
     */
    @FXML
    private void handleEnregistrer() {
        try {
            if (validerFormulaire()) {
                ajouterSuivi();
                viderChamps();
                lblStatus.setText("✓ Suivi ajouté avec succès !");
                afficherSucces("Ajout réussi", "Le suivi a été ajouté avec succès.");
                fermerFenetre();
            }
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors de l'ajout: " + e.getMessage());
            afficherErreur("Erreur lors de l'ajout", e.getMessage());
        }
    }

    /**
     * Annule et ferme la fenêtre
     */
    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    /**
     * Vide tous les champs du formulaire
     */
    @FXML
    private void handleVider() {
        viderChamps();
        lblStatus.setText("✓ Formulaire vidé");
    }

    // MÉTHODES MÉTIER

    /**
     * Ajoute le suivi dans la base de données
     */
    private void ajouterSuivi() throws SQLException {
        SuiviTraitement suivi = new SuiviTraitement();
        SessionManager session = SessionManager.getInstance();

        // Timestamp actuel
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Initialisation des champs obligatoires
        suivi.setDateSaisie(now);
        suivi.setCreatedAt(now);
        suivi.setUpdatedAt(now);
        suivi.setObservationsPsy("");
        suivi.setDocumentName("");
        suivi.setDocumentMimeType("");
        suivi.setDocumentOriginalName("");
        suivi.setDocumentUpdatedAt(now);
        suivi.setDocumentSize(0);
        suivi.setRessenti(org.example.enums.RessentiSuivi.NEUTRE);

        // Définir qui a saisi le suivi
        suivi.setSaisiPar(session.getRoleSaisiPar());

        // Date du suivi
        if (dpDateSuivi.getValue() != null) {
            suivi.setDateSuivi(java.sql.Date.valueOf(dpDateSuivi.getValue()));
        }

        // Traitement sélectionné
        Traitement traitementSelectionne = cmbTraitement.getValue();
        if (traitementSelectionne != null) {
            suivi.setTraitementId(traitementSelectionne.getTraitementId());
        } else {
            afficherErreur("Traitement manquant", "Veuillez sélectionner un traitement");
            return;
        }

        // Vérification pour le mode psychologue
        if (session.estPsychologue() && cmbEtudiant.getValue() == null) {
            afficherErreur("Étudiant manquant", "Veuillez sélectionner un étudiant");
            return;
        }

        // Observations
        suivi.setObservations(txtNotes.getText());

        // Ajout du suivi
        suiviTraitementService.ajouter(suivi);
        System.out.println("Suivi ajouté pour le traitement: " + traitementSelectionne.getTitre());
    }

    /**
     * Valide les données du formulaire
     * @return true si les données sont valides
     */
    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();
        SessionManager session = SessionManager.getInstance();

        // Validation de la date
        if (dpDateSuivi.getValue() == null) {
            erreurs.append("• La date de suivi est obligatoire.\n");
        } else {
            // Vérification d'unicité
            try {
                Traitement traitement = cmbTraitement.getValue();
                if (traitement != null && suiviTraitementService.suiviExisteDeja(
                        java.sql.Date.valueOf(dpDateSuivi.getValue()),
                        traitement.getTraitementId())) {
                    erreurs.append("• Un suivi existe déjà pour cette date et ce traitement.\n");
                }
            } catch (SQLException e) {
                erreurs.append("• Erreur lors de la vérification de l'unicité.\n");
            }
        }

        // Validation du traitement
        if (cmbTraitement.getValue() == null) {
            erreurs.append("• Le traitement est obligatoire.\n");
        }

        // Validation de l'étudiant (uniquement pour psychologue)
        if (session.estPsychologue() && cmbEtudiant.getValue() == null) {
            erreurs.append("• L'étudiant est obligatoire.\n");
        }

        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }

        //  Validation de la longueur des notes
        if (txtNotes.getText() != null && txtNotes.getText().length() > 1000) {
            erreurs.append("• Les notes ne doivent pas dépasser 1000 caractères.\n");
        }

        //  Validation que la date de suivi n'est pas dans le futur
        if (dpDateSuivi.getValue() != null && dpDateSuivi.getValue().isAfter(java.time.LocalDate.now())) {
            erreurs.append("• La date de suivi ne peut pas être dans le futur.\n");
        }

        // AFFICHAGE DES ERREURS
        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }


        return true;
    }

    /**
     * Vide tous les champs du formulaire
     */
    private void viderChamps() {
        dpDateSuivi.setValue(null);
        cmbTraitement.setValue(null);
        cmbEtudiant.setValue(null);
        txtNotes.clear();
    }

    /**
     * Ferme la fenêtre actuelle
     */
    private void fermerFenetre() {
        Stage stage = (Stage) dpDateSuivi.getScene().getWindow();
        stage.close();
    }

    /**
     * Affiche une boîte de dialogue d'erreur
     */
    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de dialogue de succès
     */
    private void afficherSucces(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}