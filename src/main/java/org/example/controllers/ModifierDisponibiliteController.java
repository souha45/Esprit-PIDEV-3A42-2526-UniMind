package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.DisponibilitePsy;
import org.example.services.DisponibilitePsyService;
import org.example.enums.TypeConsultation;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;

/**
 * Contrôleur pour le modal de modification d'une disponibilité
 */
public class ModifierDisponibiliteController {

    // ========== COMPOSANTS FXML ==========
    @FXML private DatePicker datePicker;
    @FXML private TextField txtHeureDebut;
    @FXML private TextField txtMinuteDebut;
    @FXML private TextField txtHeureFin;
    @FXML private TextField txtMinuteFin;
    @FXML private ComboBox<String> cbTypeConsult;
    @FXML private TextField txtLieu;
    @FXML private Button btnFermer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    // ========== SERVICES ==========
    private DisponibilitePsyService disponibiliteService;

    // ========== DONNÉES ==========
    private DisponibilitePsy disponibiliteAModifier;
    private Stage modalStage;

    // ========== INITIALISATION ==========
    @FXML
    public void initialize() {
        // Initialiser le service
        disponibiliteService = new DisponibilitePsyService();

        // Remplir la ComboBox des types de consultation
        cbTypeConsult.setItems(FXCollections.observableArrayList(
                "Présentiel", "En ligne"
        ));

        // Restreindre la saisie des champs (chiffres seulement)
        restreindreSaisieChiffres(txtHeureDebut, 2);
        restreindreSaisieChiffres(txtMinuteDebut, 2);
        restreindreSaisieChiffres(txtHeureFin, 2);
        restreindreSaisieChiffres(txtMinuteFin, 2);

        // Configurer les actions des boutons
        btnEnregistrer.setOnAction(event -> enregistrerModification());
        btnAnnuler.setOnAction(event -> fermerModal());
        btnFermer.setOnAction(event -> fermerModal());
    }

    /**
     * Restreint la saisie d'un TextField aux chiffres seulement
     */
    private void restreindreSaisieChiffres(TextField textField, int maxLength) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                textField.setText(oldVal);
            }
            if (newVal.length() > maxLength) {
                textField.setText(newVal.substring(0, maxLength));
            }
        });
    }

    /**
     * Prépare le modal avec les données de la disponibilité à modifier
     */
    public void setDisponibiliteAModifier(DisponibilitePsy disponibilite) {
        this.disponibiliteAModifier = disponibilite;

        if (disponibilite != null) {
            // 1. Date
            datePicker.setValue(disponibilite.getDateDispo().toLocalDate());

            // 2. Heure début
            String heureDebutStr = disponibilite.getHeureDebut().toString();
            String[] debutParts = heureDebutStr.split(":");
            txtHeureDebut.setText(debutParts[0]);
            txtMinuteDebut.setText(debutParts[1]);

            // 3. Heure fin
            String heureFinStr = disponibilite.getHeureFin().toString();
            String[] finParts = heureFinStr.split(":");
            txtHeureFin.setText(finParts[0]);
            txtMinuteFin.setText(finParts[1]);

            // 4. Type de consultation
            String typeConsult = disponibilite.getTypeConsult().toString();
            if ("présentiel".equals(typeConsult)) {
                cbTypeConsult.setValue("Présentiel");
            } else {
                cbTypeConsult.setValue("En ligne");
            }

            // 5. Lieu
            txtLieu.setText(disponibilite.getLieu());
        }
    }

    /**
     * Valide et récupère une valeur entière depuis un TextField
     */
    private int getValeurTextField(TextField textField, int min, int max) {
        String texte = textField.getText().trim();
        if (texte.isEmpty()) {
            return -1;
        }
        try {
            int valeur = Integer.parseInt(texte);
            if (valeur >= min && valeur <= max) {
                return valeur;
            }
        } catch (NumberFormatException e) {
            // Ignorer
        }
        return -1;
    }

    /**
     * Enregistre les modifications
     */
    private void enregistrerModification() {
        // ========== 1. VÉRIFICATION DATE ==========
        if (datePicker.getValue() == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                    "Veuillez sélectionner une date.");
            return;
        }

        LocalDate dateChoisie = datePicker.getValue();
        LocalDate dateActuelle = LocalDate.now();

        if (dateChoisie.isBefore(dateActuelle)) {
            afficherAlerte(Alert.AlertType.WARNING, "Date invalide",
                    "La date ne peut pas être dans le passé.");
            return;
        }

        // ========== 2. HEURE DÉBUT ==========
        int heureDebut = getValeurTextField(txtHeureDebut, 0, 23);
        if (heureDebut == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Heure invalide",
                    "Heure de début invalide (0-23).");
            return;
        }

        int minuteDebut = getValeurTextField(txtMinuteDebut, 0, 59);
        if (minuteDebut == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Minute invalide",
                    "Minute de début invalide (0-59).");
            return;
        }

        // ========== 3. HEURE FIN ==========
        int heureFin = getValeurTextField(txtHeureFin, 0, 23);
        if (heureFin == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Heure invalide",
                    "Heure de fin invalide (0-23).");
            return;
        }

        int minuteFin = getValeurTextField(txtMinuteFin, 0, 59);
        if (minuteFin == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Minute invalide",
                    "Minute de fin invalide (0-59).");
            return;
        }

        // ========== 4. VÉRIFICATION DÉBUT < FIN ==========
        int debutTotal = heureDebut * 60 + minuteDebut;
        int finTotal = heureFin * 60 + minuteFin;

        if (debutTotal >= finTotal) {
            afficherAlerte(Alert.AlertType.WARNING, "Erreur horaire",
                    "L'heure de début doit être avant l'heure de fin.");
            return;
        }

        // ========== 5. TYPE ==========
        if (cbTypeConsult.getValue() == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                    "Veuillez sélectionner un type de consultation.");
            return;
        }

        // ========== 6. LIEU ==========
        String lieu = txtLieu.getText().trim();
        if (lieu.isEmpty()) {
            afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                    "Veuillez saisir un lieu.");
            return;
        }

        // ========== 7. MISE À JOUR ==========
        try {
            // Mettre à jour l'objet
            disponibiliteAModifier.setDateDispo(Date.valueOf(dateChoisie));
            disponibiliteAModifier.setHeureDebut(Time.valueOf(String.format("%02d:%02d:00", heureDebut, minuteDebut)));
            disponibiliteAModifier.setHeureFin(Time.valueOf(String.format("%02d:%02d:00", heureFin, minuteFin)));

            String typeConsultStr = cbTypeConsult.getValue();
            TypeConsultation typeConsult = "Présentiel".equals(typeConsultStr)
                    ? TypeConsultation.présentiel
                    : TypeConsultation.en_ligne;
            disponibiliteAModifier.setTypeConsult(typeConsult);
            disponibiliteAModifier.setLieu(lieu);

            // Sauvegarder en base
            disponibiliteService.modifier(disponibiliteAModifier);

            afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                    "Disponibilité modifiée avec succès !");

            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void fermerModal() {
        if (modalStage != null) {
            modalStage.close();
        }
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}