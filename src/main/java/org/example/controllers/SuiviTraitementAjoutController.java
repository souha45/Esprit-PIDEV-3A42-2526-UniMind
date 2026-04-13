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

public class SuiviTraitementAjoutController implements Initializable {

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

            // Chargement des données
            chargerTraitements();
            chargerEtudiants();

            // Configuration des ComboBox
            configurerComboBoxTraitement();
            configurerComboBoxEtudiant();

            // Configuration selon le rôle
            if (session.estPsychologue()) {
                initialiserFormulairePsychologue();
            } else {
                initialiserFormulaireEtudiant();
            }

            // Configuration du psychologue connecté
            txtPsychologue.setText(session.getNomUtilisateur());
            txtPsychologue.setEditable(false);

            lblStatus.setText("✓ Prêt à ajouter un suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
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
     * Configure l'affichage de la ComboBox des étudiants
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
     * Initialise le formulaire pour le mode psychologue
     * CORRECTION : Ajout du filtre des traitements par étudiant
     */
    private void initialiserFormulairePsychologue() {
        // Rendre visibles les champs
        cmbEtudiant.setVisible(true);
        cmbEtudiant.setManaged(true);
        lblEtudiant.setVisible(true);
        lblEtudiant.setManaged(true);
        lblPsychologue.setVisible(true);
        lblPsychologue.setManaged(true);

        // Filtrer les traitements selon l'étudiant sélectionné
        cmbEtudiant.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Ne montrer que les traitements de l'étudiant sélectionné
                List<Traitement> traitementsFiltres = traitementsList.stream()
                        .filter(t -> t.getEtudiantId() == newVal.getUserId())
                        .collect(Collectors.toList());

                ObservableList<Traitement> filtres = FXCollections.observableArrayList(traitementsFiltres);
                cmbTraitement.setItems(filtres);
                cmbTraitement.setValue(null);

                if (traitementsFiltres.isEmpty()) {
                    lblStatus.setText("⚠ Aucun traitement pour cet étudiant");
                } else {
                    lblStatus.setText("✓ " + traitementsFiltres.size() + " traitement(s) disponible(s)");
                }
            } else {
                cmbTraitement.setItems(traitementsList);
            }
        });

        txtNotes.setPromptText("Observations professionnelles sur le suivi...");
    }

    /**
     * Initialise le formulaire pour le mode étudiant
     */
    private void initialiserFormulaireEtudiant() {
        // Masquer les champs étudiant
        cmbEtudiant.setVisible(false);
        cmbEtudiant.setManaged(false);
        lblEtudiant.setVisible(false);
        lblEtudiant.setManaged(false);
        lblPsychologue.setVisible(false);
        lblPsychologue.setManaged(false);

        // Filtrer les traitements pour l'étudiant connecté
        SessionManager session = SessionManager.getInstance();
        int etudiantId = session.getUtilisateurConnecteId();

        List<Traitement> traitementsEtudiant = traitementsList.stream()
                .filter(t -> t.getEtudiantId() == etudiantId)
                .collect(Collectors.toList());

        cmbTraitement.setItems(FXCollections.observableArrayList(traitementsEtudiant));

        txtNotes.setPromptText("Décrivez comment vous vous sentez et votre progression...");
    }

    // ACTIONS DES BOUTONS

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

    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    @FXML
    private void handleVider() {
        viderChamps();
        lblStatus.setText("✓ Formulaire vidé");
    }

    //  MÉTHODES MÉTIER

    /**
     * Ajoute le suivi dans la base de données
     * CORRECTION : Vérification de cohérence entre étudiant et traitement
     */
    private void ajouterSuivi() throws SQLException {
        SuiviTraitement suivi = new SuiviTraitement();
        SessionManager session = SessionManager.getInstance();
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

        // Qui a saisi le suivi
        suivi.setSaisiPar(session.getRoleSaisiPar());

        // Date du suivi
        if (dpDateSuivi.getValue() != null) {
            suivi.setDateSuivi(java.sql.Date.valueOf(dpDateSuivi.getValue()));
        }

        // Traitement sélectionné
        Traitement traitementSelectionne = cmbTraitement.getValue();
        if (traitementSelectionne == null) {
            afficherErreur("Traitement manquant", "Veuillez sélectionner un traitement");
            return;
        }

        //  VÉRIFICATION CRITIQUE DE COHÉRENCE
        int etudiantIdDuTraitement = traitementSelectionne.getEtudiantId();

        if (session.estPsychologue()) {
            // Le psychologue doit sélectionner un étudiant
            Etudiant etudiantSelectionne = cmbEtudiant.getValue();
            if (etudiantSelectionne == null) {
                afficherErreur("Étudiant manquant", "Veuillez sélectionner un étudiant");
                return;
            }

            // Vérifier que le traitement appartient bien à l'étudiant sélectionné
            if (etudiantIdDuTraitement != etudiantSelectionne.getUserId()) {
                afficherErreur("Incohérence des données",
                        "Le traitement sélectionné n'appartient pas à l'étudiant choisi.\n\n" +
                                "Étudiant du traitement: " + getNomEtudiant(etudiantIdDuTraitement) + "\n" +
                                "Étudiant sélectionné: " + etudiantSelectionne.getNom() + " " + etudiantSelectionne.getPrenom() + "\n\n" +
                                "Veuillez sélectionner un traitement valide pour cet étudiant.");
                return;
            }
        } else {
            // L'étudiant doit choisir un traitement qui lui appartient
            if (etudiantIdDuTraitement != session.getUtilisateurConnecteId()) {
                afficherErreur("Accès refusé", "Ce traitement ne vous appartient pas.");
                return;
            }
        }

        // Ajout du suivi
        suivi.setTraitementId(traitementSelectionne.getTraitementId());
        suivi.setObservations(txtNotes.getText());

        suiviTraitementService.ajouter(suivi);

        System.out.println("Suivi ajouté - Traitement: " + traitementSelectionne.getTitre()
                + " (ID: " + traitementSelectionne.getTraitementId() + ")"
                + " - Étudiant associé ID: " + etudiantIdDuTraitement);
    }

    /**
     * Récupère le nom d'un étudiant par son ID
     */
    private String getNomEtudiant(int etudiantId) {
        for (Etudiant e : etudiantsList) {
            if (e.getUserId() == etudiantId) {
                return e.getNom() + " " + e.getPrenom();
            }
        }
        return "ID " + etudiantId;
    }

    /**
     * Valide les données du formulaire
     */
    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();
        SessionManager session = SessionManager.getInstance();

        // Validation de la date
        if (dpDateSuivi.getValue() == null) {
            erreurs.append("• La date de suivi est obligatoire.\n");
        } else {
            // Vérification que la date n'est pas dans le futur
            if (dpDateSuivi.getValue().isAfter(java.time.LocalDate.now())) {
                erreurs.append("• La date de suivi ne peut pas être dans le futur.\n");
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

        // Validation de la longueur des notes
        if (txtNotes.getText() != null && txtNotes.getText().length() > 1000) {
            erreurs.append("• Les notes ne doivent pas dépasser 1000 caractères.\n");
        }

        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }

        return true;
    }

    /**
     * Vide tous les champs
     */
    private void viderChamps() {
        dpDateSuivi.setValue(null);
        cmbTraitement.setValue(null);
        cmbEtudiant.setValue(null);
        txtNotes.clear();

        // Restaurer la liste complète des traitements pour le psychologue
        SessionManager session = SessionManager.getInstance();
        if (session.estPsychologue()) {
            cmbTraitement.setItems(traitementsList);
        }
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