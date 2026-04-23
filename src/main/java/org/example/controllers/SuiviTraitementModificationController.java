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
import org.example.enums.SaisiPar;
import org.example.services.EtudiantTraitementService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.application.Platform;
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

    @FXML private DatePicker dpDateSuivi;
    @FXML private ComboBox<Traitement> cmbTraitement;
    @FXML private ComboBox<Etudiant> cmbEtudiant;
    @FXML private TextField txtPsychologue;
    @FXML private TextArea txtNotes;
    @FXML private Label lblStatus;
    @FXML private Label lblEtudiant;
    @FXML private Label lblPsychologue;

    @FXML private Label lblErreurDate;
    @FXML private Label lblErreurTraitement;
    @FXML private Label lblErreurEtudiant;
    @FXML private Label lblErreurNotes;

    private SuiviTraitementService suiviTraitementService;
    private TraitementService traitementService;
    private EtudiantTraitementService etudiantTraitementService;
    private ObservableList<Traitement> traitementsCompletList;
    private ObservableList<Etudiant> etudiantsList;
    private SuiviTraitement suiviSelectionne;
    private boolean isPsychologue = false;
    private boolean isDataLoaded = false;  // 🔥 Flag pour éviter la validation au chargement

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            suiviTraitementService = new SuiviTraitementService();
            traitementService = new TraitementService();
            etudiantTraitementService = new EtudiantTraitementService();

            chargerTraitements();
            chargerEtudiants();
            configurerComboBoxes();

            SessionManager session = SessionManager.getInstance();
            isPsychologue = session.estPsychologue();

            if (isPsychologue) {
                initialiserFormulairePsychologue();
            } else {
                initialiserFormulaireEtudiant();
            }

            txtPsychologue.setText(session.getNomUtilisateur());
            txtPsychologue.setEditable(false);

            ajouterListenersValidation();

            lblStatus.setText("✓ Prêt à modifier le suivi");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ajouterListenersValidation() {
        dpDateSuivi.valueProperty().addListener((obs, old, val) -> {
            if (isDataLoaded) validerDate();
        });
        cmbTraitement.valueProperty().addListener((obs, old, val) -> {
            if (isDataLoaded) validerTraitement();
        });
        txtNotes.textProperty().addListener((obs, old, val) -> {
            if (isDataLoaded) validerNotes();
        });

        if (cmbEtudiant != null) {
            cmbEtudiant.valueProperty().addListener((obs, old, val) -> {
                if (isDataLoaded) validerEtudiant();
            });
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
        if (isPsychologue && cmbEtudiant.getValue() == null) {
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
        validerDate();
        validerTraitement();
        validerNotes();

        if (isPsychologue && cmbEtudiant != null && cmbEtudiant.isVisible()) {
            validerEtudiant();
        }

        return !lblErreurDate.isVisible() && !lblErreurTraitement.isVisible() &&
                !lblErreurNotes.isVisible() && (!isPsychologue || !lblErreurEtudiant.isVisible());
    }

    public void setSuiviTraitement(SuiviTraitement suivi) {
        this.suiviSelectionne = suivi;

        if (suivi == null) {
            lblStatus.setText("✗ Erreur: Aucun suivi à modifier");
            return;
        }

        System.out.println("=== setSuiviTraitement ===");
        System.out.println("Traitement ID du suivi: " + suivi.getTraitementId());

        SessionManager session = SessionManager.getInstance();
        if (session.estEtudiant() && suivi.getSaisiPar() != SaisiPar.ETUDIANT) {
            lblStatus.setText("✗ Vous ne pouvez pas modifier le suivi du psychologue");
            dpDateSuivi.setDisable(true);
            cmbTraitement.setDisable(true);
            txtNotes.setDisable(true);
            return;
        }

        // Définir la date
        if (suivi.getDateSuivi() != null) {
            dpDateSuivi.setValue(suivi.getDateSuivi().toLocalDate());
        }

        txtNotes.setText(suivi.getObservations() != null ? suivi.getObservations() : "");

        // 🔥 CORRECTION: Charger les données et désactiver la validation temporairement
        Platform.runLater(() -> {
            // Désactiver la validation pendant le chargement
            isDataLoaded = false;

            // Configurer la liste des traitements selon le mode
            if (isPsychologue) {
                cmbTraitement.setItems(traitementsCompletList);
            } else {
                int etudiantId = session.getUtilisateurConnecteId();
                List<Traitement> traitementsEtudiant = traitementsCompletList.stream()
                        .filter(t -> t.getEtudiantId() == etudiantId)
                        .collect(Collectors.toList());
                cmbTraitement.setItems(FXCollections.observableArrayList(traitementsEtudiant));
            }

            // Chercher le traitement dans la liste actuelle
            Traitement traitementTrouve = null;
            for (Traitement t : cmbTraitement.getItems()) {
                if (t.getTraitementId() == suivi.getTraitementId()) {
                    traitementTrouve = t;
                    break;
                }
            }

            // Si le traitement n'est pas trouvé, le chercher dans la liste complète
            if (traitementTrouve == null && traitementsCompletList != null) {
                for (Traitement t : traitementsCompletList) {
                    if (t.getTraitementId() == suivi.getTraitementId()) {
                        traitementTrouve = t;
                        break;
                    }
                }

                // Ajouter le traitement manquant à la liste
                if (traitementTrouve != null) {
                    ObservableList<Traitement> newList = FXCollections.observableArrayList(cmbTraitement.getItems());
                    newList.add(traitementTrouve);
                    cmbTraitement.setItems(newList);
                    System.out.println("✅ Traitement manquant ajouté: " + traitementTrouve.getTitre());
                }
            }

            if (traitementTrouve != null) {
                cmbTraitement.setValue(traitementTrouve);
                System.out.println("✅ Traitement sélectionné: " + traitementTrouve.getTitre());

                if (isPsychologue && cmbEtudiant != null) {
                    for (Etudiant e : etudiantsList) {
                        if (e.getUserId() == traitementTrouve.getEtudiantId()) {
                            cmbEtudiant.setValue(e);
                            break;
                        }
                    }
                }
            } else {
                System.out.println("❌ Traitement ID " + suivi.getTraitementId() + " non trouvé");
            }

            // Réactiver la validation après le chargement
            isDataLoaded = true;

            // Valider (maintenant les champs sont remplis)
            validerDate();
            validerTraitement();
            validerNotes();
            if (isPsychologue) {
                validerEtudiant();
            }
        });

        lblStatus.setText("✓ Modification du suivi du " + (suivi.getDateSuivi() != null ? suivi.getDateSuivi().toString() : "date inconnue"));
    }

    private void chargerTraitements() throws SQLException {
        List<Traitement> traitements = traitementService.afficher();
        traitementsCompletList = FXCollections.observableArrayList(traitements);
        System.out.println("📋 " + traitementsCompletList.size() + " traitements chargés");
    }

    private void chargerEtudiants() throws SQLException {
        List<Etudiant> etudiants = etudiantTraitementService.afficher();
        etudiantsList = FXCollections.observableArrayList(etudiants);
        if (cmbEtudiant != null) {
            cmbEtudiant.setItems(etudiantsList);
        }
        System.out.println("📋 " + etudiantsList.size() + " étudiants chargés");
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
        cmbEtudiant.setVisible(true);
        cmbEtudiant.setManaged(true);
        lblEtudiant.setVisible(true);
        lblEtudiant.setManaged(true);
        txtPsychologue.setVisible(true);
        txtPsychologue.setManaged(true);
        lblPsychologue.setVisible(true);
        lblPsychologue.setManaged(true);

        cmbEtudiant.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isDataLoaded && newVal != null) {
                List<Traitement> traitementsFiltres = traitementsCompletList.stream()
                        .filter(t -> t.getEtudiantId() == newVal.getUserId())
                        .collect(Collectors.toList());
                cmbTraitement.setItems(FXCollections.observableArrayList(traitementsFiltres));
                cmbTraitement.setValue(null);
            } else if (isDataLoaded) {
                cmbTraitement.setItems(traitementsCompletList);
            }
        });

        txtNotes.setPromptText("Observations professionnelles sur le suivi...");
    }

    private void initialiserFormulaireEtudiant() {
        cmbEtudiant.setVisible(false);
        cmbEtudiant.setManaged(false);
        lblEtudiant.setVisible(false);
        lblEtudiant.setManaged(false);
        txtPsychologue.setVisible(false);
        txtPsychologue.setManaged(false);
        lblPsychologue.setVisible(false);
        lblPsychologue.setManaged(false);

        SessionManager session = SessionManager.getInstance();
        int etudiantId = session.getUtilisateurConnecteId();

        List<Traitement> traitementsEtudiant = traitementsCompletList.stream()
                .filter(t -> t.getEtudiantId() == etudiantId)
                .collect(Collectors.toList());

        cmbTraitement.setItems(FXCollections.observableArrayList(traitementsEtudiant));

        txtNotes.setPromptText("Décrivez votre progression, vos ressentis, vos difficultés...");
    }

    @FXML
    private void handleEnregistrer() {
        SessionManager session = SessionManager.getInstance();
        if (session.estEtudiant() && suiviSelectionne != null && suiviSelectionne.getSaisiPar() != SaisiPar.ETUDIANT) {
            afficherErreur("Accès refusé", "Vous ne pouvez pas modifier le suivi du psychologue.");
            return;
        }

        if (!isFormulaireValide()) {
            lblStatus.setText("✗ Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        try {
            modifierSuivi();
            lblStatus.setText("✓ Suivi modifié avec succès !");
            afficherSucces("Modification réussie", "Le suivi a été modifié avec succès.");
            fermerFenetre();
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors de la modification: " + e.getMessage());
            afficherErreur("Erreur", e.getMessage());
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

    private void modifierSuivi() throws SQLException {
        if (suiviSelectionne == null) {
            throw new SQLException("Aucun suivi à modifier");
        }

        SessionManager session = SessionManager.getInstance();

        Traitement traitementSelectionne = cmbTraitement.getValue();
        if (traitementSelectionne == null) {
            throw new SQLException("Veuillez sélectionner un traitement");
        }

        if (isPsychologue) {
            Etudiant etudiantSelectionne = cmbEtudiant.getValue();
            if (etudiantSelectionne == null) {
                throw new SQLException("Veuillez sélectionner un étudiant");
            }

            if (traitementSelectionne.getEtudiantId() != etudiantSelectionne.getUserId()) {
                throw new SQLException("Le traitement ne correspond pas à l'étudiant sélectionné");
            }
        }

        suiviSelectionne.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        suiviSelectionne.setSaisiPar(session.getRoleSaisiPar());

        if (dpDateSuivi.getValue() != null) {
            suiviSelectionne.setDateSuivi(java.sql.Date.valueOf(dpDateSuivi.getValue()));
        }

        suiviSelectionne.setTraitementId(traitementSelectionne.getTraitementId());
        suiviSelectionne.setObservations(txtNotes.getText());

        suiviTraitementService.modifier(suiviSelectionne);
        System.out.println("✅ Suivi modifié - Traitement ID: " + traitementSelectionne.getTraitementId());
    }

    private void fermerFenetre() {
        Stage stage = (Stage) dpDateSuivi.getScene().getWindow();
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