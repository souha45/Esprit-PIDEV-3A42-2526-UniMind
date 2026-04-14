package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SuiviTraitementAjoutController implements Initializable {

    @FXML
    private TextField txtDateSuivi;
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
    private Label lblDate;
    @FXML
    private Label lblTraitement;
    @FXML
    private Label lblEtudiant;
    @FXML
    private Label lblPsychologue;

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Traitement> traitementsList;
    private ObservableList<Etudiant> etudiantsList;
    private Traitement traitementPreSelectionne;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();

        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Date par défaut = aujourd'hui (pour tous les utilisateurs)
            txtDateSuivi.setText(LocalDate.now().format(formatter));
            // Le champ date est en lecture seule pour tout le monde
            txtDateSuivi.setEditable(false);

            chargerTraitements();
            chargerEtudiants();
            configurerComboBoxes();

            if (session.estPsychologue()) {
                initialiserFormulairePsychologue();
            } else {
                initialiserFormulaireEtudiant();
            }

            txtPsychologue.setText(session.getNomUtilisateur());
            txtPsychologue.setEditable(false);

            lblStatus.setText("✓ Prêt à ajouter un suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Pré-sélectionne un traitement pour l'étudiant
     */
    public void setTraitementPreSelectionne(Traitement traitement) {
        this.traitementPreSelectionne = traitement;
        if (cmbTraitement != null) {
            cmbTraitement.setValue(traitement);
            cmbTraitement.setDisable(true);
            lblStatus.setText("✓ Traitement pré-sélectionné: " + traitement.getTitre());
        }
    }

    private void chargerTraitements() throws SQLException {
        List<Traitement> traitements = traitementService.afficher();
        traitementsList = FXCollections.observableArrayList(traitements);
        cmbTraitement.setItems(traitementsList);
    }

    private void chargerEtudiants() throws SQLException {
        List<Etudiant> etudiants = etudiantService.afficher();
        etudiantsList = FXCollections.observableArrayList(etudiants);
        cmbEtudiant.setItems(etudiantsList);
    }

    private void configurerComboBoxes() {
        cmbTraitement.setCellFactory(param -> new javafx.scene.control.ListCell<Traitement>() {
            @Override
            protected void updateItem(Traitement traitement, boolean empty) {
                super.updateItem(traitement, empty);
                setText((empty || traitement == null) ? null : traitement.getTitre());
            }
        });

        cmbTraitement.setButtonCell(new javafx.scene.control.ListCell<Traitement>() {
            @Override
            protected void updateItem(Traitement traitement, boolean empty) {
                super.updateItem(traitement, empty);
                setText((empty || traitement == null) ? "Sélectionner un traitement" : traitement.getTitre());
            }
        });

        cmbEtudiant.setCellFactory(param -> new javafx.scene.control.ListCell<Etudiant>() {
            @Override
            protected void updateItem(Etudiant etudiant, boolean empty) {
                super.updateItem(etudiant, empty);
                setText((empty || etudiant == null) ? null : etudiant.getNom() + " " + etudiant.getPrenom());
            }
        });

        cmbEtudiant.setButtonCell(new javafx.scene.control.ListCell<Etudiant>() {
            @Override
            protected void updateItem(Etudiant etudiant, boolean empty) {
                super.updateItem(etudiant, empty);
                setText((empty || etudiant == null) ? "Sélectionner un étudiant" : etudiant.getNom() + " " + etudiant.getPrenom());
            }
        });
    }

    private void initialiserFormulairePsychologue() {
        // La date est visible (mais non modifiable)
        lblDate.setVisible(true);
        lblDate.setManaged(true);
        txtDateSuivi.setVisible(true);
        txtDateSuivi.setManaged(true);

        // Tout est visible
        cmbTraitement.setVisible(true);
        cmbTraitement.setManaged(true);
        lblTraitement.setVisible(true);
        lblTraitement.setManaged(true);

        cmbEtudiant.setVisible(true);
        cmbEtudiant.setManaged(true);
        lblEtudiant.setVisible(true);
        lblEtudiant.setManaged(true);
        txtPsychologue.setVisible(true);
        txtPsychologue.setManaged(true);
        lblPsychologue.setVisible(true);
        lblPsychologue.setManaged(true);

        // Filtre des traitements par étudiant
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

        txtNotes.setPromptText("Observations professionnelles sur le suivi...");
    }

    private void initialiserFormulaireEtudiant() {
        // Masquer la ligne date (déjà remplie automatiquement)
        lblDate.setVisible(false);
        lblDate.setManaged(false);
        txtDateSuivi.setVisible(false);
        txtDateSuivi.setManaged(false);

        // Masquer les champs étudiant et psychologue
        cmbEtudiant.setVisible(false);
        cmbEtudiant.setManaged(false);
        lblEtudiant.setVisible(false);
        lblEtudiant.setManaged(false);
        txtPsychologue.setVisible(false);
        txtPsychologue.setManaged(false);
        lblPsychologue.setVisible(false);
        lblPsychologue.setManaged(false);

        // Masquer le libellé Traitement (car déjà pré-sélectionné)
        lblTraitement.setVisible(false);
        lblTraitement.setManaged(false);

        // Le champ traitement est masqué aussi (déjà pré-sélectionné)
        cmbTraitement.setVisible(false);
        cmbTraitement.setManaged(false);

        txtNotes.setPromptText("Décrivez comment vous vous sentez et votre progression...");
    }

    @FXML
    private void handleEnregistrer() {
        try {
            if (validerFormulaire()) {
                ajouterSuivi();
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

    private void ajouterSuivi() throws SQLException {
        SuiviTraitement suivi = new SuiviTraitement();
        SessionManager session = SessionManager.getInstance();
        Timestamp now = new Timestamp(System.currentTimeMillis());

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
        suivi.setSaisiPar(session.getRoleSaisiPar());

        // Date du suivi (aujourd'hui)
        LocalDate dateSuivi = LocalDate.parse(txtDateSuivi.getText(), formatter);
        suivi.setDateSuivi(java.sql.Date.valueOf(dateSuivi));

        // Traitement
        Traitement traitementSelectionne;
        if (session.estEtudiant()) {
            traitementSelectionne = traitementPreSelectionne;
        } else {
            traitementSelectionne = cmbTraitement.getValue();
        }

        if (traitementSelectionne == null) {
            afficherErreur("Traitement manquant", "Veuillez sélectionner un traitement");
            return;
        }

        // Vérification pour l'étudiant
        if (session.estEtudiant()) {
            if (traitementSelectionne.getEtudiantId() != session.getUtilisateurConnecteId()) {
                afficherErreur("Accès refusé", "Ce traitement ne vous appartient pas.");
                return;
            }
        } else {
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

        suivi.setTraitementId(traitementSelectionne.getTraitementId());
        suivi.setObservations(txtNotes.getText());

        suiviTraitementService.ajouter(suivi);
        System.out.println("Suivi ajouté pour le traitement: " + traitementSelectionne.getTitre());
    }

    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();
        SessionManager session = SessionManager.getInstance();

        if (session.estEtudiant()) {
            if (traitementPreSelectionne == null) {
                erreurs.append("• Aucun traitement sélectionné.\n");
            }
        } else {
            if (cmbTraitement.getValue() == null) {
                erreurs.append("• Le traitement est obligatoire.\n");
            }
            if (cmbEtudiant.getValue() == null) {
                erreurs.append("• L'étudiant est obligatoire.\n");
            }
        }

        if (txtNotes.getText() == null || txtNotes.getText().trim().isEmpty()) {
            erreurs.append("• Les observations sont obligatoires.\n");
        }

        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }
        return true;
    }

    private void fermerFenetre() {
        Stage stage = (Stage) txtDateSuivi.getScene().getWindow();
        stage.close();
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherSucces(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}