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

    // Suivi à modifier
    private SuiviTraitement suiviSelectionne;

    // INITIALISATION
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Chargement des traitements
            chargerTraitements();

            // Configuration de la ComboBox Traitement
            configurerComboBoxTraitement();

            // Chargement des étudiants
            chargerEtudiants();

            // Configuration du psychologue connecté
            txtPsychologue.setText(SessionManager.getInstance().getNomUtilisateur());
            txtPsychologue.setEditable(false);

            // Adaptation selon le rôle
            adapterInterfaceSelonRole();

            lblStatus.setText("✓ Prêt à modifier le suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Charge les traitements depuis la base de données
     */
    private void chargerTraitements() throws SQLException {
        List<Traitement> traitements = traitementService.afficher();
        traitementsList = FXCollections.observableArrayList(traitements);
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
     * Charge les étudiants depuis la base de données
     */
    private void chargerEtudiants() throws SQLException {
        List<Etudiant> etudiants = etudiantService.afficher();
        etudiantsList = FXCollections.observableArrayList(etudiants);
        cmbEtudiant.setItems(etudiantsList);

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
     * Adapte l'interface selon le rôle de l'utilisateur
     */
    private void adapterInterfaceSelonRole() {
        SessionManager session = SessionManager.getInstance();

        if (session.estEtudiant()) {
            // Mode étudiant : masquer les champs étudiants
            cmbEtudiant.setVisible(false);
            cmbEtudiant.setManaged(false);
            lblEtudiant.setVisible(false);
            lblEtudiant.setManaged(false);
            lblPsychologue.setVisible(false);
            lblPsychologue.setManaged(false);
            txtNotes.setPromptText("Décrivez comment vous vous sentez et votre progression...");
        } else {
            // Mode psychologue : tous les champs visibles
            txtNotes.setPromptText("Observations professionnelles sur le suivi...");
        }
    }

    /**
     * Initialise le formulaire avec les données du suivi à modifier
     * @param suivi Suivi de traitement à modifier
     */
    public void setSuiviTraitement(SuiviTraitement suivi) {
        this.suiviSelectionne = suivi;

        if (suivi == null) {
            lblStatus.setText("✗ Erreur: Aucun suivi à modifier");
            return;
        }

        // Date du suivi
        if (suivi.getDateSuivi() != null) {
            dpDateSuivi.setValue(suivi.getDateSuivi().toLocalDate());
        }

        // Sélection du traitement
        for (Traitement traitement : traitementsList) {
            if (traitement.getTraitementId() == suivi.getTraitementId()) {
                cmbTraitement.setValue(traitement);
                break;
            }
        }

        // Sélection de l'étudiant associé (via le traitement)
        chargerEtudiantAssocie(suivi.getTraitementId());

        // Notes
        txtNotes.setText(suivi.getObservations() != null ? suivi.getObservations() : "");

        lblStatus.setText("✓ Modification du suivi du " + (suivi.getDateSuivi() != null ? suivi.getDateSuivi().toString() : "date inconnue"));
    }

    /**
     * Charge l'étudiant associé au traitement
     * @param traitementId ID du traitement
     */
    private void chargerEtudiantAssocie(int traitementId) {
        try {
            List<Traitement> traitements = traitementService.afficher();
            Traitement traitementTrouve = null;

            for (Traitement traitement : traitements) {
                if (traitement.getTraitementId() == traitementId) {
                    traitementTrouve = traitement;
                    break;
                }
            }

            if (traitementTrouve != null && traitementTrouve.getEtudiantId() > 0) {
                for (Etudiant etudiant : etudiantsList) {
                    if (etudiant.getUserId() == traitementTrouve.getEtudiantId()) {
                        cmbEtudiant.setValue(etudiant);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de l'étudiant: " + e.getMessage());
        }
    }

    // ACTIONS DES BOUTONS

    /**
     * Enregistre les modifications du suivi
     */
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

    /**
     * Annule et ferme la fenêtre
     */
    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    /**
     * Réinitialise le formulaire avec les valeurs originales
     */
    @FXML
    private void handleReinitialiser() {
        if (suiviSelectionne != null) {
            setSuiviTraitement(suiviSelectionne);
            lblStatus.setText("✓ Formulaire réinitialisé");
        }
    }

    // MÉTHODES MÉTIER

    /**
     * Modifie le suivi dans la base de données
     */
    private void modifierSuivi() throws SQLException {
        if (suiviSelectionne == null) {
            afficherErreur("Aucune sélection", "Aucun suivi à modifier");
            return;
        }

        // Vérification des permissions
        if (!verifierPermissions()) {
            return;
        }

        // Mise à jour des timestamps
        suiviSelectionne.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // Mise à jour de qui a saisi/modifié le suivi
        SessionManager session = SessionManager.getInstance();
        suiviSelectionne.setSaisiPar(session.getRoleSaisiPar());

        // Mise à jour de la date
        if (dpDateSuivi.getValue() != null) {
            suiviSelectionne.setDateSuivi(java.sql.Date.valueOf(dpDateSuivi.getValue()));
        }

        // Mise à jour du traitement
        Traitement traitementSelectionne = cmbTraitement.getValue();
        if (traitementSelectionne != null) {
            suiviSelectionne.setTraitementId(traitementSelectionne.getTraitementId());
        } else {
            afficherErreur("Traitement manquant", "Veuillez sélectionner un traitement");
            return;
        }

        // Vérification pour le mode psychologue
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.estPsychologue() && cmbEtudiant.getValue() == null) {
            afficherErreur("Étudiant manquant", "Veuillez sélectionner un étudiant");
            return;
        }

        // Mise à jour des notes
        suiviSelectionne.setObservations(txtNotes.getText());

        // Sauvegarde
        suiviTraitementService.modifier(suiviSelectionne);
        System.out.println("Suivi modifié: " + suiviSelectionne.getDateSuivi());
    }

    /**
     * Vérifie les permissions de modification
     * @return true si l'utilisateur a le droit de modifier
     */
    private boolean verifierPermissions() throws SQLException {
        SessionManager session = SessionManager.getInstance();

        // Récupération du traitement associé
        List<Traitement> traitements = traitementService.afficher();
        Traitement traitementAssocie = null;

        for (Traitement t : traitements) {
            if (t.getTraitementId() == suiviSelectionne.getTraitementId()) {
                traitementAssocie = t;
                break;
            }
        }

        if (traitementAssocie == null) {
            afficherErreur("Erreur", "Traitement associé non trouvé");
            return false;
        }

        // Vérification des permissions
        boolean peutModifier = false;
        int etudiantId = traitementAssocie.getEtudiantId();
        int psychologueId = traitementAssocie.getPsychologueId();

        if (session.estPsychologue()) {
            if (suiviSelectionne.getSaisiPar() == SaisiPar.PSYCHOLOGUE) {
                peutModifier = (psychologueId == session.getUtilisateurConnecteId());
            } else {
                peutModifier = session.peutVoirTraitement(etudiantId, psychologueId);
            }
        } else {
            if (suiviSelectionne.getSaisiPar() == SaisiPar.ETUDIANT) {
                peutModifier = (etudiantId == session.getUtilisateurConnecteId());
            }
        }

        if (!peutModifier) {
            afficherErreur("Accès refusé", "Vous n'avez pas les permissions pour modifier ce suivi");
            return false;
        }

        return true;
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
                    // Vérifier si c'est le même suivi qu'on modifie
                    if (suiviSelectionne.getDateSuivi() == null ||
                            !suiviSelectionne.getDateSuivi().equals(java.sql.Date.valueOf(dpDateSuivi.getValue())) ||
                            suiviSelectionne.getTraitementId() != traitement.getTraitementId()) {
                        erreurs.append("• Un suivi existe déjà pour cette date et ce traitement.\n");
                    }
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

        //  Validation de la longueur des notes
        if (txtNotes.getText() != null && txtNotes.getText().length() > 1000) {
            erreurs.append("• Les notes ne doivent pas dépasser 1000 caractères.\n");
        }

        // Validation que la date de suivi n'est pas dans le futur
        if (dpDateSuivi.getValue() != null && dpDateSuivi.getValue().isAfter(java.time.LocalDate.now())) {
            erreurs.append("• La date de suivi ne peut pas être dans le futur.\n");
        }


        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }

        return true;
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