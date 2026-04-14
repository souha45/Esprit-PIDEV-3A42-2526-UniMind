package org.example.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class TraitementAjoutController implements Initializable {

    // ==================== COMPOSANTS FXML ====================

    @FXML
    private TextField txtTitre;
    @FXML
    private ComboBox<String> cmbType;
    @FXML
    private ComboBox<CategorieTraitement> cmbCategorie;
    @FXML
    private ToggleGroup statutGroup;
    @FXML
    private RadioButton rbEnCours;
    @FXML
    private RadioButton rbTermine;
    @FXML
    private RadioButton rbSuspendu;
    @FXML
    private ToggleGroup prioriteGroup;
    @FXML
    private RadioButton rbPrioriteBasse;
    @FXML
    private RadioButton rbPrioriteMoyenne;
    @FXML
    private RadioButton rbPrioriteHaute;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private Spinner<Integer> spinnerDosage;
    @FXML
    private ComboBox<String> cmbUniteDosage;
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

    // ==================== SERVICES ====================

    private TraitementService traitementService;
    private EtudiantService etudiantService;
    private ObservableList<Etudiant> etudiantsList;

    // ==================== INITIALISATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();

        if (!session.peutCreerTraitement()) {
            afficherErreur("Accès refusé", "Seul le psychologue peut créer des traitements.");
            fermerFenetre();
            return;
        }

        // Configuration du Spinner pour le dosage
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1);
        spinnerDosage.setValueFactory(valueFactory);
        spinnerDosage.setEditable(true);
        spinnerDosage.getStyleClass().add("form-spinner");

        // Configuration de la ComboBox des unités
        cmbUniteDosage.setItems(FXCollections.observableArrayList(
                "fois par jour",
                "fois par semaine",
                "fois par mois",
                "comprimé(s) par jour",
                "comprimé(s) par semaine",
                "gélule(s) par jour",
                "cuillère(s) par jour",
                "séance(s) par semaine",
                "séance(s) par mois"
        ));
        cmbUniteDosage.setValue("fois par semaine"); // Valeur par défaut

        // Configuration des ToggleGroups
        statutGroup = new ToggleGroup();
        rbEnCours.setToggleGroup(statutGroup);
        rbTermine.setToggleGroup(statutGroup);
        rbSuspendu.setToggleGroup(statutGroup);
        rbEnCours.setSelected(true);

        prioriteGroup = new ToggleGroup();
        rbPrioriteBasse.setToggleGroup(prioriteGroup);
        rbPrioriteMoyenne.setToggleGroup(prioriteGroup);
        rbPrioriteHaute.setToggleGroup(prioriteGroup);
        rbPrioriteMoyenne.setSelected(true);

        // Initialisation des ComboBox
        cmbType.setItems(FXCollections.observableArrayList(
                "Thérapie cognitive",
                "Thérapie comportementale",
                "Thérapie cognitivo-comportementale (TCC)",
                "Thérapie émotionnelle",
                "Coaching",
                "Méditation pleine conscience",
                "Relaxation",
                "Thérapie familiale",
                "Psychothérapie",
                "Suivi psychologique",
                "Bilan psychologique",
                "Autre"
        ));

        cmbCategorie.setItems(FXCollections.observableArrayList(CategorieTraitement.values()));

        try {
            traitementService = new TraitementService();
            etudiantService = new EtudiantService();

            // Charger les étudiants
            List<Etudiant> etudiants = etudiantService.afficher();
            etudiantsList = FXCollections.observableArrayList(etudiants);
            cmbEtudiant.setItems(etudiantsList);

            // Configuration de l'affichage des étudiants
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

            // Date par défaut = aujourd'hui
            dpDateDebut.setValue(LocalDate.now());

            // Psychologue connecté
            txtNomPsychologue.setText(session.getNomUtilisateur());
            txtNomPsychologue.setEditable(false);

            lblStatus.setText("✓ Prêt à ajouter un traitement");

        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    // ==================== ACTIONS DES BOUTONS ====================

    @FXML
    private void handleEnregistrer() {
        try {
            if (validerFormulaire()) {
                ajouterTraitement();
                viderChamps();
                lblStatus.setText("✓ Traitement ajouté avec succès !");
                afficherSucces("Ajout réussi", "Le traitement a été ajouté avec succès.");
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

    // ==================== MÉTHODES MÉTIER ====================

    private void ajouterTraitement() throws SQLException {
        Traitement traitement = new Traitement();

        // Champs obligatoires
        traitement.setTitre(txtTitre.getText().trim());
        traitement.setType(cmbType.getValue());
        traitement.setCategorie(cmbCategorie.getValue());

        // Statut
        RadioButton selectedStatut = (RadioButton) statutGroup.getSelectedToggle();
        if (selectedStatut == rbEnCours) {
            traitement.setStatut(StatutTraitement.EN_COURS);
        } else if (selectedStatut == rbTermine) {
            traitement.setStatut(StatutTraitement.TERMINE);
        } else if (selectedStatut == rbSuspendu) {
            traitement.setStatut(StatutTraitement.SUSPENDU);
        }

        // Priorité
        RadioButton selectedPriorite = (RadioButton) prioriteGroup.getSelectedToggle();
        if (selectedPriorite == rbPrioriteBasse) {
            traitement.setPriorite(PrioriteTraitement.BASSE);
        } else if (selectedPriorite == rbPrioriteMoyenne) {
            traitement.setPriorite(PrioriteTraitement.MOYENNE);
        } else if (selectedPriorite == rbPrioriteHaute) {
            traitement.setPriorite(PrioriteTraitement.HAUTE);
        }

        // Dates
        if (dpDateDebut.getValue() != null) {
            traitement.setDateDebut(java.sql.Date.valueOf(dpDateDebut.getValue()));
        }
        if (dpDateFin.getValue() != null) {
            traitement.setDateFin(java.sql.Date.valueOf(dpDateFin.getValue()));
        }

        // Dosage (Spinner + Unité)
        int dosage = spinnerDosage.getValue();
        String unite = cmbUniteDosage.getValue();
        traitement.setDosage(dosage + " " + unite);

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
        traitement.setObjectifTherapeutique(txtObjectifTherapeutique.getText());
        traitement.setDescription(txtDescription.getText());

        traitementService.ajouter(traitement);
        System.out.println("Traitement ajouté: " + traitement.getTitre() + " - Dosage: " + traitement.getDosage());
    }

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
        if (cmbType.getValue() == null || cmbType.getValue().isEmpty()) {
            erreurs.append("• Le type est obligatoire.\n");
        }

        // Validation de la catégorie
        if (cmbCategorie.getValue() == null) {
            erreurs.append("• La catégorie est obligatoire.\n");
        }

        // Validation du statut
        if (statutGroup.getSelectedToggle() == null) {
            erreurs.append("• Le statut est obligatoire.\n");
        }

        // Validation de la priorité
        if (prioriteGroup.getSelectedToggle() == null) {
            erreurs.append("• La priorité est obligatoire.\n");
        }

        // Validation de l'étudiant
        if (cmbEtudiant.getValue() == null) {
            erreurs.append("• L'étudiant est obligatoire.\n");
        }

        // Validation du dosage
        if (spinnerDosage.getValue() == null || spinnerDosage.getValue() < 0) {
            erreurs.append("• La valeur du dosage doit être un nombre positif.\n");
        }
        if (cmbUniteDosage.getValue() == null || cmbUniteDosage.getValue().isEmpty()) {
            erreurs.append("• L'unité du dosage est obligatoire.\n");
        }

        // Validation des dates
        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null) {
            if (dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
                erreurs.append("• La date de fin ne peut pas être antérieure à la date de début.\n");
            }
        }

        // Validation de la description
        if (txtDescription.getText() != null && txtDescription.getText().length() > 500) {
            erreurs.append("• La description ne doit pas dépasser 500 caractères.\n");
        }

        // Validation de l'objectif
        if (txtObjectifTherapeutique.getText() != null && txtObjectifTherapeutique.getText().length() > 255) {
            erreurs.append("• L'objectif thérapeutique ne doit pas dépasser 255 caractères.\n");
        }

        if (erreurs.length() > 0) {
            afficherErreur("Erreur de validation", erreurs.toString());
            return false;
        }

        return true;
    }

    private void viderChamps() {
        txtTitre.clear();
        cmbType.setValue(null);
        cmbCategorie.setValue(null);
        rbEnCours.setSelected(true);
        rbPrioriteMoyenne.setSelected(true);
        txtObjectifTherapeutique.clear();
        txtDescription.clear();
        cmbEtudiant.setValue(null);
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(null);
        spinnerDosage.getValueFactory().setValue(1);
        cmbUniteDosage.setValue("fois par semaine");

        SessionManager session = SessionManager.getInstance();
        txtNomPsychologue.setText(session.getNomUtilisateur());
    }

    private void fermerFenetre() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
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