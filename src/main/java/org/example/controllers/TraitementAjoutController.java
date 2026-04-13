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

public class TraitementAjoutController implements Initializable {


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

    //  SERVICES

    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Etudiant> etudiantsList;

    // INITIALISATION

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Vérification des permissions
        SessionManager session = SessionManager.getInstance();
        if (!session.peutCreerTraitement()) {
            afficherErreur("Accès refusé", "Seul le psychologue peut créer des traitements.");
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

            lblStatus.setText("✓ Prêt à ajouter un traitement");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    // ACTIONS DES BOUTONS

    /**
     * Enregistre le nouveau traitement
     */
    @FXML
    private void handleEnregistrer() {
        try {
            if (validerFormulaire()) {
                ajouterTraitement();
                viderChamps();
                lblStatus.setText("✓ Traitement ajouté avec succès !");
                afficherSucces("Ajout réussi", "Le traitement a été ajouté avec succès.");
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
     * Ajoute le traitement dans la base de données
     */
    private void ajouterTraitement() throws SQLException {
        Traitement traitement = new Traitement();

        // Champs obligatoires
        traitement.setTitre(txtTitre.getText().trim());
        traitement.setType(txtType.getText().trim());
        traitement.setCategorie(CategorieTraitement.valueOf(cmbCategorie.getValue()));
        traitement.setStatut(StatutTraitement.valueOf(cmbStatut.getValue()));
        traitement.setPriorite(PrioriteTraitement.valueOf(cmbPriorite.getValue()));

        // Dates
        if (dpDateDebut.getValue() != null) {
            traitement.setDateDebut(java.sql.Date.valueOf(dpDateDebut.getValue()));
        }
        if (dpDateFin.getValue() != null) {
            traitement.setDateFin(java.sql.Date.valueOf(dpDateFin.getValue()));
        }

        // Clés étrangères
        SessionManager session = SessionManager.getInstance();
        traitement.setPsychologueId(session.getUtilisateurConnecteId());

        Etudiant etudiantSelectionne = cmbEtudiant.getValue();
        if (etudiantSelectionne != null) {
            traitement.setEtudiantId(etudiantSelectionne.getUserId());
        } else {
            afficherErreur("Étudiant manquant", "Veuillez sélectionner un étudiant");
            return;
        }

        // Autres champs
        traitement.setDosage(txtDosage.getText());
        traitement.setObjectifTherapeutique(txtObjectifTherapeutique.getText());
        traitement.setDescription(txtDescription.getText());

        traitementService.ajouter(traitement);
        System.out.println("Traitement ajouté: " + traitement.getTitre());
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
        } else {
            // Validation d'unicité du titre
            try {
                Etudiant etudiant = cmbEtudiant.getValue();
                if (etudiant != null && traitementService.traitementExisteDeja(
                        txtTitre.getText().trim(), etudiant.getUserId())) {
                    erreurs.append("• Un traitement avec ce titre existe déjà pour cet étudiant.\n");
                }
            } catch (SQLException e) {
                erreurs.append("• Erreur lors de la vérification de l'unicité.\n");
            }
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
        // Validation de la longueur de la description
        if (txtDescription.getText() != null && txtDescription.getText().length() > 500) {
            erreurs.append("• La description ne doit pas dépasser 500 caractères.\n");
        }

        //  Validation de la longueur de l'objectif thérapeutique
        if (txtObjectifTherapeutique.getText() != null && txtObjectifTherapeutique.getText().length() > 255) {
            erreurs.append("• L'objectif thérapeutique ne doit pas dépasser 255 caractères.\n");
        }

        //  Validation du dosage
        if (txtDosage.getText() != null && !txtDosage.getText().trim().isEmpty()) {
            if (!txtDosage.getText().matches("^[a-zA-Z0-9\\s\\-]+$")) {
                erreurs.append("• Le dosage ne doit contenir que des lettres, chiffres, espaces ou tirets.\n");
            }
        }

        //  Validation des dates
        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null) {
            if (dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
                erreurs.append("• La date de fin ne peut pas être antérieure à la date de début.\n");
            }
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
        txtTitre.clear();
        txtType.clear();
        txtDosage.clear();
        txtObjectifTherapeutique.clear();
        txtDescription.clear();
        cmbCategorie.setValue(null);
        cmbStatut.setValue(null);
        cmbPriorite.setValue(null);
        cmbEtudiant.setValue(null);
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);

        // Réinitialiser le nom du psychologue (reprendre la session)
        SessionManager session = SessionManager.getInstance();
        txtNomPsychologue.setText(session.getNomUtilisateur());
    }

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