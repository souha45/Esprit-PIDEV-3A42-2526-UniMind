package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import org.example.entities.Etudiant;
import org.example.entities.Traitement;
import org.example.enums.CategorieTraitement;
import org.example.enums.PrioriteTraitement;
import org.example.enums.StatutTraitement;
import org.example.services.EtudiantService;
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


public class TraitementModificationController implements Initializable {


    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtType;
    @FXML
    private ComboBox<String> cmbCategorie;
    @FXML
    private ComboBox<String> cmbStatut;
    @FXML
    private ComboBox<String> cmbPriorite;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private TextField txtDosage;
    @FXML
    private TextField txtNomPsychologue;
    @FXML
    private ComboBox<Etudiant> cmbEtudiant;
    @FXML
    private TextField txtObjectifTherapeutique;
    @FXML
    private TextArea txtDescription;
    @FXML
    private Label lblStatus;

    // SERVICES

    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Etudiant> etudiantsList;

    // Traitement à modifier
    private Traitement traitementSelectionne;

    // INITIALISATION

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Vérification des permissions
        SessionManager session = SessionManager.getInstance();
        if (!session.peutModifierTraitement()) {
            afficherErreur("Accès refusé", "Seul le psychologue peut modifier les traitements.");
            fermerFenetre();
            return;
        }

        // Initialisation des ComboBox
        cmbCategorie.setItems(FXCollections.observableArrayList(
                "RELAXATION", "COGNITIF", "EMOTIONNEL", "COMPORTEMENTAL"
        ));
        cmbStatut.setItems(FXCollections.observableArrayList(
                "EN_COURS", "TERMINE", "SUSPENDU"
        ));
        cmbPriorite.setItems(FXCollections.observableArrayList(
                "BASSE", "MOYENNE", "HAUTE"
        ));

        try {
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Chargement des étudiants
            List<Etudiant> etudiants = etudiantService.afficher();
            etudiantsList = FXCollections.observableArrayList(etudiants);
            cmbEtudiant.setItems(etudiantsList);

            // Configuration de l'affichage de la ComboBox Étudiant
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
                        setText(null);
                    } else {
                        setText(etudiant.getNom() + " " + etudiant.getPrenom());
                    }
                }
            });

            // Configuration du psychologue connecté
            txtNomPsychologue.setText(session.getNomUtilisateur());
            txtNomPsychologue.setEditable(false);

            lblStatus.setText("✓ Prêt à modifier le traitement");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Initialise le formulaire avec les données du traitement à modifier
     * @param traitement Traitement à modifier
     */
    public void setTraitement(Traitement traitement) {
        this.traitementSelectionne = traitement;

        if (traitement == null) {
            lblStatus.setText("✗ Erreur: Aucun traitement à modifier");
            return;
        }

        // Remplissage du formulaire
        txtTitre.setText(traitement.getTitre());
        txtType.setText(traitement.getType());
        cmbCategorie.setValue(traitement.getCategorie().name());
        cmbStatut.setValue(traitement.getStatut().name());
        cmbPriorite.setValue(traitement.getPriorite().name());
        txtDosage.setText(traitement.getDosage());

        if (traitement.getDateDebut() != null) {
            dpDateDebut.setValue(traitement.getDateDebut().toLocalDate());
        }
        if (traitement.getDateFin() != null) {
            dpDateFin.setValue(traitement.getDateFin().toLocalDate());
        }

        txtObjectifTherapeutique.setText(traitement.getObjectifTherapeutique());
        txtDescription.setText(traitement.getDescription());

        // Sélection de l'étudiant correspondant
        if (etudiantsList != null) {
            for (Etudiant etudiant : etudiantsList) {
                if (etudiant.getUserId() == traitement.getEtudiantId()) {
                    cmbEtudiant.setValue(etudiant);
                    break;
                }
            }
        }

        lblStatus.setText("✓ Modification du traitement: " + traitement.getTitre());
    }

    //  ACTIONS DES BOUTONS

    /**
     * Enregistre les modifications du traitement
     */
    @FXML
    private void handleEnregistrer() {
        try {
            if (validerFormulaire()) {
                modifierTraitement();
                lblStatus.setText("✓ Traitement modifié avec succès !");
                afficherSucces("Modification réussie", "Le traitement a été modifié avec succès.");
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
        if (traitementSelectionne != null) {
            setTraitement(traitementSelectionne);
            lblStatus.setText("✓ Formulaire réinitialisé");
        }
    }

    //  MÉTHODES MÉTIER

    /**
     * Modifie le traitement dans la base de données
     */
    private void modifierTraitement() throws SQLException {
        if (traitementSelectionne == null) {
            afficherErreur("Aucune sélection", "Aucun traitement à modifier");
            return;
        }

        // Mise à jour des champs
        traitementSelectionne.setTitre(txtTitre.getText().trim());
        traitementSelectionne.setType(txtType.getText().trim());
        traitementSelectionne.setCategorie(CategorieTraitement.valueOf(cmbCategorie.getValue()));
        traitementSelectionne.setStatut(StatutTraitement.valueOf(cmbStatut.getValue()));
        traitementSelectionne.setPriorite(PrioriteTraitement.valueOf(cmbPriorite.getValue()));

        // Mise à jour des dates
        if (dpDateDebut.getValue() != null) {
            traitementSelectionne.setDateDebut(java.sql.Date.valueOf(dpDateDebut.getValue()));
        } else {
            traitementSelectionne.setDateDebut(null);
        }

        if (dpDateFin.getValue() != null) {
            traitementSelectionne.setDateFin(java.sql.Date.valueOf(dpDateFin.getValue()));
        } else {
            traitementSelectionne.setDateFin(null);
        }

        // Mise à jour des autres champs
        traitementSelectionne.setDosage(txtDosage.getText());
        traitementSelectionne.setObjectifTherapeutique(txtObjectifTherapeutique.getText());
        traitementSelectionne.setDescription(txtDescription.getText());

        // Mise à jour de l'étudiant
        Etudiant etudiantSelectionne = cmbEtudiant.getValue();
        if (etudiantSelectionne != null) {
            traitementSelectionne.setEtudiantId(etudiantSelectionne.getUserId());
        } else {
            afficherErreur("Étudiant manquant", "Veuillez sélectionner un étudiant");
            return;
        }

        // Sauvegarde des modifications
        traitementService.modifier(traitementSelectionne);
        System.out.println("Traitement modifié: " + traitementSelectionne.getTitre());
    }

    /**
     * Valide les données du formulaire
     * @return true si les données sont valides
     */
    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();

        // Validation du titre
        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            erreurs.append("• Le titre est obligatoire.\n");
        } else if (txtTitre.getText().trim().length() < 3) {
            erreurs.append("• Le titre doit contenir au moins 3 caractères.\n");
        } else if (txtTitre.getText().trim().length() > 200) {
            erreurs.append("• Le titre ne doit pas dépasser 200 caractères.\n");
        }

        // Validation du type
        if (txtType.getText() == null || txtType.getText().trim().isEmpty()) {
            erreurs.append("• Le type est obligatoire.\n");
        } else if (txtType.getText().trim().length() < 2) {
            erreurs.append("• Le type doit contenir au moins 2 caractères.\n");
        } else if (txtType.getText().trim().length() > 100) {
            erreurs.append("• Le type ne doit pas dépasser 100 caractères.\n");
        }

        // Validation des ComboBox
        if (cmbCategorie.getValue() == null) {
            erreurs.append("• La catégorie est obligatoire.\n");
        }
        if (cmbStatut.getValue() == null) {
            erreurs.append("• Le statut est obligatoire.\n");
        }
        if (cmbPriorite.getValue() == null) {
            erreurs.append("• La priorité est obligatoire.\n");
        }
        if (cmbEtudiant.getValue() == null) {
            erreurs.append("• L'étudiant est obligatoire.\n");
        }

        // Validation des dates
        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null) {
            if (dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
                erreurs.append("• La date de fin ne peut pas être antérieure à la date de début.\n");
            }
        }

        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }

        return true;
    }

    //  MÉTHODES UTILITAIRES

    /**
     * Ferme la fenêtre actuelle
     */
    private void fermerFenetre() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
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