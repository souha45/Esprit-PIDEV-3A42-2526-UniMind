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
import org.example.services.EtudiantTraitementService;
import org.example.services.TraitementEmailService;
import org.example.services.TraitementIAService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<CategorieTraitement> cmbCategorie;
    @FXML private ToggleGroup statutGroup;
    @FXML private RadioButton rbEnCours, rbTermine, rbSuspendu;
    @FXML private ToggleGroup prioriteGroup;
    @FXML private RadioButton rbPrioriteBasse, rbPrioriteMoyenne, rbPrioriteHaute;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private Spinner<Integer> spinnerDosage;
    @FXML private ComboBox<String> cmbUniteDosage;
    @FXML private ComboBox<Etudiant> cmbEtudiant;
    @FXML private TextField txtObjectifTherapeutique;
    @FXML private TextArea txtDescription;
    @FXML private Label lblStatus;

    // Labels d'erreur
    @FXML private Label lblErreurTitre, lblErreurType, lblErreurCategorie, lblErreurStatut;
    @FXML private Label lblErreurPriorite, lblErreurDateDebut, lblErreurDateFin, lblErreurDosage;
    @FXML private Label lblErreurEtudiant, lblErreurObjectif, lblErreurDescription;

    // Champs IA
    @FXML private TextArea txtSymptomesIA;
    @FXML private TextArea txtDescriptionIA;
    @FXML private Spinner<Integer> spinnerAgeIA;
    @FXML private ComboBox<String> cmbNiveauEtudesIA;
    @FXML private Button btnGenererIA;
    @FXML private Button btnAnalyserIA;
    @FXML private Label lblStatusIA;

    private TraitementService traitementService;
    private EtudiantTraitementService etudiantTraitementService;
    private TraitementIAService iaService;
    private ObservableList<Etudiant> etudiantsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionManager session = SessionManager.getInstance();

        // Configuration du spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1);
        spinnerDosage.setValueFactory(valueFactory);
        spinnerDosage.setEditable(true);

        // Configuration des unités
        cmbUniteDosage.setItems(FXCollections.observableArrayList(
                "fois par jour", "fois par semaine", "fois par mois",
                "comprimé(s) par jour", "gélule(s) par jour", "séance(s) par semaine"));
        cmbUniteDosage.setValue("fois par semaine");

        // Configuration des ComboBox
        cmbType.setItems(FXCollections.observableArrayList(
                "Thérapie cognitive", "Thérapie comportementale", "Thérapie cognitivo-comportementale (TCC)",
                "Thérapie émotionnelle", "Coaching", "Méditation pleine conscience", "Relaxation",
                "Thérapie familiale", "Psychothérapie", "Suivi psychologique", "Bilan psychologique", "Autre"));
        cmbCategorie.setItems(FXCollections.observableArrayList(CategorieTraitement.values()));

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

        dpDateDebut.setValue(LocalDate.now());

        try {
            traitementService = new TraitementService();
            etudiantTraitementService = new EtudiantTraitementService();
            iaService = new TraitementIAService();

            List<Etudiant> etudiants = etudiantTraitementService.afficher();
            etudiantsList = FXCollections.observableArrayList(etudiants);
            cmbEtudiant.setItems(etudiantsList);
            configurerComboBoxEtudiant();

            // Configuration des champs IA
            configurerChampsIA();

            // Ajout des listeners pour validation en temps réel
            ajouterListenersValidation();

            lblStatus.setText("✓ Prêt à ajouter un traitement");
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur: " + e.getMessage());
        }
    }

    private void configurerComboBoxEtudiant() {
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

    private void ajouterListenersValidation() {
        txtTitre.textProperty().addListener((obs, old, val) -> validerTitre());
        cmbType.valueProperty().addListener((obs, old, val) -> validerType());
        cmbCategorie.valueProperty().addListener((obs, old, val) -> validerCategorie());
        statutGroup.selectedToggleProperty().addListener((obs, old, val) -> validerStatut());
        prioriteGroup.selectedToggleProperty().addListener((obs, old, val) -> validerPriorite());
        dpDateDebut.valueProperty().addListener((obs, old, val) -> validerDates());
        dpDateFin.valueProperty().addListener((obs, old, val) -> validerDates());
        spinnerDosage.valueProperty().addListener((obs, old, val) -> validerDosage());
        cmbUniteDosage.valueProperty().addListener((obs, old, val) -> validerDosage());
        cmbEtudiant.valueProperty().addListener((obs, old, val) -> validerEtudiant());
        txtObjectifTherapeutique.textProperty().addListener((obs, old, val) -> validerObjectif());
        txtDescription.textProperty().addListener((obs, old, val) -> validerDescription());
    }

    // Méthodes de validation individuelles
    private void validerTitre() {
        String titre = txtTitre.getText();
        if (titre == null || titre.trim().isEmpty()) {
            afficherErreur(lblErreurTitre, txtTitre, "Le titre est obligatoire");
        } else if (titre.trim().length() < 3) {
            afficherErreur(lblErreurTitre, txtTitre, "Le titre doit contenir au moins 3 caractères");
        } else if (titre.trim().length() > 200) {
            afficherErreur(lblErreurTitre, txtTitre, "Le titre ne doit pas dépasser 200 caractères");
        } else {
            cacherErreur(lblErreurTitre, txtTitre);
        }
    }

    private void validerType() {
        if (cmbType.getValue() == null) {
            afficherErreur(lblErreurType, cmbType, "Le type est obligatoire");
        } else {
            cacherErreur(lblErreurType, cmbType);
        }
    }

    private void validerCategorie() {
        if (cmbCategorie.getValue() == null) {
            afficherErreur(lblErreurCategorie, cmbCategorie, "La catégorie est obligatoire");
        } else {
            cacherErreur(lblErreurCategorie, cmbCategorie);
        }
    }

    private void validerStatut() {
        if (statutGroup.getSelectedToggle() == null) {
            afficherErreur(lblErreurStatut, null, "Le statut est obligatoire");
        } else {
            cacherErreur(lblErreurStatut, null);
        }
    }

    private void validerPriorite() {
        if (prioriteGroup.getSelectedToggle() == null) {
            afficherErreur(lblErreurPriorite, null, "La priorité est obligatoire");
        } else {
            cacherErreur(lblErreurPriorite, null);
        }
    }

    private void validerDates() {
        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null) {
            if (dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
                afficherErreur(lblErreurDateFin, dpDateFin, "La date de fin ne peut pas être antérieure à la date de début");
            } else {
                cacherErreur(lblErreurDateFin, dpDateFin);
            }
        }
        if (dpDateDebut.getValue() == null) {
            afficherErreur(lblErreurDateDebut, dpDateDebut, "La date de début est obligatoire");
        } else {
            cacherErreur(lblErreurDateDebut, dpDateDebut);
        }
    }

    private void validerDosage() {
        if (spinnerDosage.getValue() == null || spinnerDosage.getValue() < 0) {
            afficherErreur(lblErreurDosage, spinnerDosage, "La valeur du dosage doit être positive");
        } else if (cmbUniteDosage.getValue() == null) {
            afficherErreur(lblErreurDosage, cmbUniteDosage, "L'unité du dosage est obligatoire");
        } else {
            cacherErreur(lblErreurDosage, null);
        }
    }

    private void validerEtudiant() {
        if (cmbEtudiant.getValue() == null) {
            afficherErreur(lblErreurEtudiant, cmbEtudiant, "L'étudiant est obligatoire");
        } else {
            cacherErreur(lblErreurEtudiant, cmbEtudiant);
        }
    }

    private void validerObjectif() {
        String obj = txtObjectifTherapeutique.getText();
        if (obj != null && obj.length() > 255) {
            afficherErreur(lblErreurObjectif, txtObjectifTherapeutique, "L'objectif ne doit pas dépasser 255 caractères");
        } else {
            cacherErreur(lblErreurObjectif, txtObjectifTherapeutique);
        }
    }

    private void validerDescription() {
        String desc = txtDescription.getText();
        if (desc != null && desc.length() > 500) {
            afficherErreur(lblErreurDescription, txtDescription, "La description ne doit pas dépasser 500 caractères");
        } else {
            cacherErreur(lblErreurDescription, txtDescription);
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
        validerTitre();
        validerType();
        validerCategorie();
        validerStatut();
        validerPriorite();
        validerDates();
        validerDosage();
        validerEtudiant();
        validerObjectif();
        validerDescription();

        return !lblErreurTitre.isVisible() && !lblErreurType.isVisible() && !lblErreurCategorie.isVisible() &&
                !lblErreurStatut.isVisible() && !lblErreurPriorite.isVisible() && !lblErreurDateDebut.isVisible() &&
                !lblErreurDateFin.isVisible() && !lblErreurDosage.isVisible() && !lblErreurEtudiant.isVisible() &&
                !lblErreurObjectif.isVisible() && !lblErreurDescription.isVisible();
    }

    @FXML
    private void handleEnregistrer() {
        if (!isFormulaireValide()) {
            lblStatus.setText("✗ Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        try {
            ajouterTraitement();
            lblStatus.setText("✓ Traitement ajouté avec succès !");
            viderChamps();
            afficherSucces("Ajout réussi", "Le traitement a été ajouté avec succès.");
            fermerFenetre();
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur: " + e.getMessage());
            afficherErreur("Erreur", e.getMessage());
        }
    }

    private void ajouterTraitement() throws SQLException {
        Traitement traitement = new Traitement();
        traitement.setTitre(txtTitre.getText().trim());
        traitement.setType(cmbType.getValue());
        traitement.setCategorie(cmbCategorie.getValue());

        RadioButton selectedStatut = (RadioButton) statutGroup.getSelectedToggle();
        if (selectedStatut == rbEnCours) traitement.setStatut(StatutTraitement.EN_COURS);
        else if (selectedStatut == rbTermine) traitement.setStatut(StatutTraitement.TERMINE);
        else if (selectedStatut == rbSuspendu) traitement.setStatut(StatutTraitement.SUSPENDU);

        RadioButton selectedPriorite = (RadioButton) prioriteGroup.getSelectedToggle();
        if (selectedPriorite == rbPrioriteBasse) traitement.setPriorite(PrioriteTraitement.BASSE);
        else if (selectedPriorite == rbPrioriteMoyenne) traitement.setPriorite(PrioriteTraitement.MOYENNE);
        else if (selectedPriorite == rbPrioriteHaute) traitement.setPriorite(PrioriteTraitement.HAUTE);

        if (dpDateDebut.getValue() != null) traitement.setDateDebut(java.sql.Date.valueOf(dpDateDebut.getValue()));
        if (dpDateFin.getValue() != null) traitement.setDateFin(java.sql.Date.valueOf(dpDateFin.getValue()));

        int dosage = spinnerDosage.getValue();
        traitement.setDosage(dosage + " " + cmbUniteDosage.getValue());

        SessionManager session = SessionManager.getInstance();
        traitement.setPsychologueId(session.getUtilisateurConnecteId());
        traitement.setEtudiantId(cmbEtudiant.getValue().getUserId());
        traitement.setObjectifTherapeutique(txtObjectifTherapeutique.getText());
        traitement.setDescription(txtDescription.getText());

        traitementService.ajouter(traitement);

        // Envoyer une notification email à l'étudiant
        try {
            TraitementEmailService emailService = new TraitementEmailService();

            // Récupérer le psychologue connecté
            org.example.services.PsychologueService psychologueService = new org.example.services.PsychologueService();
            org.example.entities.User psychologue = psychologueService.getPsychologueById(session.getUtilisateurConnecteId());

            if (psychologue != null) {
                var resultat = emailService.envoyerNotificationNouveauTraitementEtudiant(traitement, psychologue);
                if (resultat.isSuccess()) {
                    System.out.println("✓ Notification envoyée à l'étudiant pour le nouveau traitement");
                } else {
                    System.err.println("✗ Erreur envoi notification étudiant: " + resultat.getErrorMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de notification: " + e.getMessage());
            // Ne pas bloquer l'ajout du traitement si l'email échoue
        }
    }

    @FXML private void handleAnnuler() { fermerFenetre(); }
    @FXML private void handleVider() { viderChamps(); }

    private void viderChamps() {
        txtTitre.clear();
        cmbType.setValue(null);
        cmbCategorie.setValue(null);
        rbEnCours.setSelected(true);
        rbPrioriteMoyenne.setSelected(true);
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(null);
        spinnerDosage.getValueFactory().setValue(1);
        cmbUniteDosage.setValue("fois par semaine");
        cmbEtudiant.setValue(null);
        txtObjectifTherapeutique.clear();
        txtDescription.clear();
        cacherErreur(lblErreurTitre, txtTitre);
        cacherErreur(lblErreurType, cmbType);
        cacherErreur(lblErreurCategorie, cmbCategorie);
        cacherErreur(lblErreurDateDebut, dpDateDebut);
        cacherErreur(lblErreurDateFin, dpDateFin);
        cacherErreur(lblErreurEtudiant, cmbEtudiant);
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

    // =============================================
    // MÉTHODES IA - GÉNÉRATION INTELLIGENTE
    // =============================================

    /**
     * Configure les champs spécifiques à l'IA
     */
    private void configurerChampsIA() {
        // Configuration du spinner pour l'âge
        SpinnerValueFactory<Integer> ageValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 50, 20);
        spinnerAgeIA.setValueFactory(ageValueFactory);
        spinnerAgeIA.setEditable(true);

        // Configuration du ComboBox pour le niveau d'études
        cmbNiveauEtudesIA.setItems(FXCollections.observableArrayList(
                "Secondaire",
                "Baccalauréat",
                "Licence",
                "Master",
                "Doctorat",
                "Formation professionnelle",
                "Autre"
        ));
        cmbNiveauEtudesIA.setValue("Licence");

        // Configuration des styles pour le statut IA
        lblStatusIA.getStyleClass().add("ia-status");
    }

    /**
     * Gère la génération de traitement par l'IA
     */
    @FXML
    private void handleGenererIA() {
        String symptomes = txtSymptomesIA.getText();
        String description = txtDescriptionIA.getText();
        Integer age = spinnerAgeIA.getValue();
        String niveauEtudes = cmbNiveauEtudesIA.getValue();

        // Validation des entrées IA
        if (symptomes == null || symptomes.trim().isEmpty()) {
            lblStatusIA.setText("❌ Veuillez décrire les symptômes");
            lblStatusIA.getStyleClass().add("error");
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            lblStatusIA.setText("❌ Veuillez donner une description détaillée");
            lblStatusIA.getStyleClass().add("error");
            return;
        }

        try {
            lblStatusIA.setText("🤖 Génération en cours...");
            lblStatusIA.getStyleClass().add("loading");

            // Génération du traitement par l'IA avec validation
            Traitement traitementGenere = iaService.genererTraitementPersonnalise(
                    symptomes, description, age, niveauEtudes);

            // Validation du traitement généré
            if (!iaService.validerTraitement(traitementGenere)) {
                lblStatusIA.setText("❌ Erreur lors de la génération du traitement");
                lblStatusIA.getStyleClass().add("error");
                return;
            }

            // Remplissage automatique des champs du formulaire
            remplirFormulaireAvecTraitement(traitementGenere);

            lblStatusIA.setText("✅ Traitement généré avec succès !");
            lblStatusIA.getStyleClass().removeAll("error", "loading");

            afficherSucces("Génération IA réussie",
                    "Le traitement a été généré intelligemment. Vous pouvez maintenant le modifier si nécessaire.");

        } catch (IllegalArgumentException e) {
            // Erreur de validation des symptômes
            lblStatusIA.setText("❌ " + e.getMessage());
            lblStatusIA.getStyleClass().removeAll("loading");
            lblStatusIA.getStyleClass().add("error");

        } catch (Exception e) {
            // Autres erreurs techniques
            lblStatusIA.setText("❌ Erreur technique");
            lblStatusIA.getStyleClass().removeAll("loading");
            lblStatusIA.getStyleClass().add("error");
            System.err.println("Erreur lors de la génération IA: " + e.getMessage());
            afficherErreur("Erreur technique",
                    "Une erreur est survenue lors de la génération du traitement. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleAnalyserIA() {
        String description = txtDescriptionIA.getText();

        if (description == null || description.trim().isEmpty()) {
            lblStatusIA.setText("❌ Veuillez entrer une description à analyser");
            lblStatusIA.getStyleClass().add("error");
            return;
        }

        try {
            lblStatusIA.setText("🔍 Analyse en cours...");
            lblStatusIA.getStyleClass().add("loading");

            // Analyse de la description
            var suggestions = iaService.analyserDescription(description);

            // Affichage des suggestions
            StringBuilder message = new StringBuilder("Suggestions d'amélioration:\n\n");
            for (int i = 0; i < suggestions.size(); i++) {
                message.append((i + 1)).append(". ").append(suggestions.get(i)).append("\n");
            }

            lblStatusIA.setText("✅ Analyse terminée");
            lblStatusIA.getStyleClass().removeAll("error", "loading");

            afficherSucces("Analyse IA", message.toString());

        } catch (Exception e) {
            lblStatusIA.setText("❌ Erreur: " + e.getMessage());
            lblStatusIA.getStyleClass().add("error");
            System.err.println("Erreur lors de l'analyse IA: " + e.getMessage());
        }
    }

    /**
     * Remplit le formulaire avec les données du traitement généré par l'IA
     */
    private void remplirFormulaireAvecTraitement(Traitement traitement) {
        // Titre
        txtTitre.setText(traitement.getTitre());

        // Type
        if (traitement.getType() != null) {
            cmbType.setValue(traitement.getType());
        }

        // Catégorie
        if (traitement.getCategorie() != null) {
            cmbCategorie.setValue(traitement.getCategorie());
        }

        // Priorité
        if (traitement.getPriorite() != null) {
            switch (traitement.getPriorite()) {
                case BASSE:
                    rbPrioriteBasse.setSelected(true);
                    break;
                case MOYENNE:
                    rbPrioriteMoyenne.setSelected(true);
                    break;
                case HAUTE:
                    rbPrioriteHaute.setSelected(true);
                    break;
            }
        }

        // Date de fin (calculée à partir de la durée)
        if (traitement.getDureeJours() > 0) {
            LocalDate dateFin = LocalDate.now().plusDays(traitement.getDureeJours());
            dpDateFin.setValue(dateFin);
        }

        // Dosage
        if (traitement.getDosage() != null && !traitement.getDosage().isEmpty()) {
            String[] dosageParts = traitement.getDosage().split(" ", 2);
            if (dosageParts.length >= 1) {
                try {
                    int dosageValue = Integer.parseInt(dosageParts[0]);
                    spinnerDosage.getValueFactory().setValue(dosageValue);
                } catch (NumberFormatException e) {
                    // Garder la valeur par défaut
                }
            }
            if (dosageParts.length >= 2) {
                cmbUniteDosage.setValue(dosageParts[1]);
            }
        }

        // Objectif thérapeutique
        if (traitement.getObjectifTherapeutique() != null) {
            txtObjectifTherapeutique.setText(traitement.getObjectifTherapeutique());
        }

        // Description
        if (traitement.getDescription() != null) {
            txtDescription.setText(traitement.getDescription());
        }

        // Validation automatique des champs remplis
        validerTitre();
        validerType();
        validerCategorie();
        validerPriorite();
        validerObjectif();
        validerDescription();
    }
}