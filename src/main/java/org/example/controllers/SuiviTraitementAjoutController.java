package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SuiviTraitementAjoutController implements Initializable {

    @FXML private DatePicker dpDateSuivi;
    @FXML private ComboBox<Traitement> cmbTraitement;
    @FXML private ComboBox<Etudiant> cmbEtudiant;
    @FXML private TextField txtPsychologue;
    @FXML private TextArea txtNotes;
    @FXML private Label lblStatus;
    @FXML private Label lblEtudiant;
    @FXML private Label lblPsychologue;
    @FXML private Label lblDate;
    @FXML private Label lblTraitementLabel;
    @FXML private VBox infoTraitementContainer;
    @FXML private Label lblInfoTraitement;

    // Labels d'erreur
    @FXML private Label lblErreurDate;
    @FXML private Label lblErreurTraitement;
    @FXML private Label lblErreurEtudiant;
    @FXML private Label lblErreurNotes;

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Traitement> traitementsList;
    private ObservableList<Etudiant> etudiantsList;
    private Traitement traitementPreSelectionne;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();

        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

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

            ajouterListenersValidation();

            lblStatus.setText("✓ Prêt à ajouter un suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ajouterListenersValidation() {
        if (dpDateSuivi != null && dpDateSuivi.isVisible()) {
            dpDateSuivi.valueProperty().addListener((obs, old, val) -> validerDate());
        }
        if (cmbTraitement != null && cmbTraitement.isVisible()) {
            cmbTraitement.valueProperty().addListener((obs, old, val) -> validerTraitement());
        }
        txtNotes.textProperty().addListener((obs, old, val) -> validerNotes());

        if (cmbEtudiant != null && cmbEtudiant.isVisible()) {
            cmbEtudiant.valueProperty().addListener((obs, old, val) -> validerEtudiant());
        }
    }

    private void validerDate() {
        if (dpDateSuivi.getValue() == null) {
            afficherErreur(lblErreurDate, dpDateSuivi, "La date de suivi est obligatoire");
        } else if (dpDateSuivi.getValue().isAfter(LocalDate.now())) {
            afficherErreur(lblErreurDate, dpDateSuivi, "La date ne peut pas être dans le futur");
        } else {
            cacherErreur(lblErreurDate, dpDateSuivi);
        }
    }

    private void validerTraitement() {
        if (cmbTraitement.getValue() == null) {
            afficherErreur(lblErreurTraitement, cmbTraitement, "Le traitement est obligatoire");
        } else {
            cacherErreur(lblErreurTraitement, cmbTraitement);
        }
    }

    private void validerEtudiant() {
        SessionManager session = SessionManager.getInstance();
        if (session.estPsychologue() && cmbEtudiant.getValue() == null) {
            afficherErreur(lblErreurEtudiant, cmbEtudiant, "L'étudiant est obligatoire");
        } else {
            cacherErreur(lblErreurEtudiant, cmbEtudiant);
        }
    }

    private void validerNotes() {
        String notes = txtNotes.getText();
        if (notes == null || notes.trim().isEmpty()) {
            afficherErreur(lblErreurNotes, txtNotes, "Les observations sont obligatoires");
        } else if (notes.length() > 1000) {
            afficherErreur(lblErreurNotes, txtNotes, "Les notes ne doivent pas dépasser 1000 caractères");
        } else {
            cacherErreur(lblErreurNotes, txtNotes);
        }
    }

    private void afficherErreur(Label label, javafx.scene.Node champ, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        if (champ != null) {
            champ.getStyleClass().add("form-field-error");
        }
    }

    private void cacherErreur(Label label, javafx.scene.Node champ) {
        label.setVisible(false);
        label.setManaged(false);
        if (champ != null) {
            champ.getStyleClass().remove("form-field-error");
        }
    }

    private boolean isFormulaireValide() {
        if (dpDateSuivi != null && dpDateSuivi.isVisible()) {
            validerDate();
        }
        if (cmbTraitement != null && cmbTraitement.isVisible()) {
            validerTraitement();
        }
        validerNotes();

        if (cmbEtudiant != null && cmbEtudiant.isVisible()) {
            validerEtudiant();
        }

        boolean dateOk = (dpDateSuivi == null || !dpDateSuivi.isVisible() || !lblErreurDate.isVisible());
        boolean traitementOk = (cmbTraitement == null || !cmbTraitement.isVisible() || !lblErreurTraitement.isVisible());
        boolean notesOk = !lblErreurNotes.isVisible();
        boolean etudiantOk = (cmbEtudiant == null || !cmbEtudiant.isVisible() || !lblErreurEtudiant.isVisible());

        return dateOk && traitementOk && notesOk && etudiantOk;
    }

    public void setTraitementPreSelectionne(Traitement traitement) {
        this.traitementPreSelectionne = traitement;
        if (cmbTraitement != null) {
            cmbTraitement.setValue(traitement);
        }
    }

    private void chargerTraitements() throws SQLException {
        List<Traitement> traitements = traitementService.afficher();
        traitementsList = FXCollections.observableArrayList(traitements);
        if (cmbTraitement != null) {
            cmbTraitement.setItems(traitementsList);
        }
    }

    private void chargerEtudiants() throws SQLException {
        List<Etudiant> etudiants = etudiantService.afficher();
        etudiantsList = FXCollections.observableArrayList(etudiants);
        if (cmbEtudiant != null) {
            cmbEtudiant.setItems(etudiantsList);
        }
    }

    private void configurerComboBoxes() {
        if (cmbTraitement != null) {
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
        }

        if (cmbEtudiant != null) {
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
    }

    private void initialiserFormulairePsychologue() {
        // Tout est visible pour le psychologue
        if (lblDate != null) {
            lblDate.setVisible(true);
            lblDate.setManaged(true);
        }
        if (dpDateSuivi != null) {
            dpDateSuivi.setVisible(true);
            dpDateSuivi.setManaged(true);
            dpDateSuivi.setValue(LocalDate.now());
        }
        if (lblTraitementLabel != null) {
            lblTraitementLabel.setVisible(true);
            lblTraitementLabel.setManaged(true);
        }
        if (cmbTraitement != null) {
            cmbTraitement.setVisible(true);
            cmbTraitement.setManaged(true);
            cmbTraitement.setDisable(false);
        }
        if (cmbEtudiant != null) {
            cmbEtudiant.setVisible(true);
            cmbEtudiant.setManaged(true);
        }
        if (lblEtudiant != null) {
            lblEtudiant.setVisible(true);
            lblEtudiant.setManaged(true);
        }
        if (txtPsychologue != null) {
            txtPsychologue.setVisible(true);
            txtPsychologue.setManaged(true);
        }
        if (lblPsychologue != null) {
            lblPsychologue.setVisible(true);
            lblPsychologue.setManaged(true);
        }
        if (infoTraitementContainer != null) {
            infoTraitementContainer.setVisible(false);
            infoTraitementContainer.setManaged(false);
        }

        txtNotes.setPromptText("Observations professionnelles sur le suivi...");

        if (cmbEtudiant != null) {
            cmbEtudiant.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && cmbTraitement != null) {
                    List<Traitement> traitementsFiltres = traitementsList.stream()
                            .filter(t -> t.getEtudiantId() == newVal.getUserId())
                            .collect(Collectors.toList());
                    cmbTraitement.setItems(FXCollections.observableArrayList(traitementsFiltres));
                    cmbTraitement.setValue(null);
                } else if (cmbTraitement != null) {
                    cmbTraitement.setItems(traitementsList);
                }
            });
        }
    }

    private void initialiserFormulaireEtudiant() {
        // Cacher la date (elle sera automatique)
        if (lblDate != null) {
            lblDate.setVisible(false);
            lblDate.setManaged(false);
        }
        if (dpDateSuivi != null) {
            dpDateSuivi.setVisible(false);
            dpDateSuivi.setManaged(false);
            dpDateSuivi.setValue(LocalDate.now());
        }

        // Cacher le label Traitement
        if (lblTraitementLabel != null) {
            lblTraitementLabel.setVisible(false);
            lblTraitementLabel.setManaged(false);
        }

        // Cacher la ComboBox Traitement
        if (cmbTraitement != null) {
            cmbTraitement.setVisible(false);
            cmbTraitement.setManaged(false);
        }

        // Cacher les champs étudiant et psychologue
        if (cmbEtudiant != null) {
            cmbEtudiant.setVisible(false);
            cmbEtudiant.setManaged(false);
        }
        if (lblEtudiant != null) {
            lblEtudiant.setVisible(false);
            lblEtudiant.setManaged(false);
        }
        if (txtPsychologue != null) {
            txtPsychologue.setVisible(false);
            txtPsychologue.setManaged(false);
        }
        if (lblPsychologue != null) {
            lblPsychologue.setVisible(false);
            lblPsychologue.setManaged(false);
        }

        // Afficher l'information du traitement sélectionné
        if (infoTraitementContainer != null && traitementPreSelectionne != null) {
            infoTraitementContainer.setVisible(true);
            infoTraitementContainer.setManaged(true);
            lblInfoTraitement.setText("Traitement: " + traitementPreSelectionne.getTitre());
        }

        txtNotes.setPromptText("Décrivez votre progression, vos ressentis, vos difficultés...");

        SessionManager session = SessionManager.getInstance();
        int etudiantId = session.getUtilisateurConnecteId();

        List<Traitement> traitementsEtudiant = traitementsList.stream()
                .filter(t -> t.getEtudiantId() == etudiantId)
                .collect(Collectors.toList());

        if (cmbTraitement != null) {
            cmbTraitement.setItems(FXCollections.observableArrayList(traitementsEtudiant));
            if (traitementPreSelectionne != null) {
                cmbTraitement.setValue(traitementPreSelectionne);
            }
        }
    }

    @FXML
    private void handleEnregistrer() {
        if (!isFormulaireValide()) {
            lblStatus.setText("✗ Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        try {
            ajouterSuivi();
            lblStatus.setText("✓ Suivi ajouté avec succès !");
            afficherSucces("Ajout réussi", "Votre suivi a été ajouté avec succès.");
            fermerFenetre();
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors de l'ajout: " + e.getMessage());
            afficherErreur("Erreur", e.getMessage());
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

        if (session.estEtudiant()) {
            suivi.setDateSuivi(java.sql.Date.valueOf(LocalDate.now()));
        } else if (dpDateSuivi != null && dpDateSuivi.getValue() != null) {
            suivi.setDateSuivi(java.sql.Date.valueOf(dpDateSuivi.getValue()));
        } else {
            suivi.setDateSuivi(java.sql.Date.valueOf(LocalDate.now()));
        }

        Traitement traitementSelectionne;
        if (session.estEtudiant()) {
            traitementSelectionne = traitementPreSelectionne;
            if (traitementSelectionne == null && cmbTraitement != null) {
                traitementSelectionne = cmbTraitement.getValue();
            }
        } else {
            traitementSelectionne = cmbTraitement != null ? cmbTraitement.getValue() : null;
        }

        if (traitementSelectionne == null) {
            throw new SQLException("Aucun traitement sélectionné");
        }

        suivi.setTraitementId(traitementSelectionne.getTraitementId());
        suivi.setObservations(txtNotes.getText());

        suiviTraitementService.ajouter(suivi);
        System.out.println("Suivi ajouté pour le traitement: " + traitementSelectionne.getTitre());
    }

    private void fermerFenetre() {
        Stage stage = (Stage) txtNotes.getScene().getWindow();
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