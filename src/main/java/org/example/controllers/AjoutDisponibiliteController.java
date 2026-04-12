package org.example.controllers;

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
 * Contrôleur pour le modal d'ajout de disponibilité
 * Permet de choisir n'importe quelle heure et minute (00:00 à 23:59)
 */
public class AjoutDisponibiliteController {

    // ========== COMPOSANTS FXML ==========
    @FXML private DatePicker datePicker;                    // Sélecteur de date
    @FXML private TextField txtHeureDebut;                  // Heure début (0-23)
    @FXML private TextField txtMinuteDebut;                 // Minute début (0-59)
    @FXML private TextField txtHeureFin;                    // Heure fin (0-23)
    @FXML private TextField txtMinuteFin;                   // Minute fin (0-59)
    @FXML private ComboBox<String> cbTypeConsult;           // Type consultation
    @FXML private TextField txtLieu;                        // Lieu
    @FXML private Button btnFermer;                         // Bouton fermeture
    @FXML private Button btnAnnuler;                        // Bouton annuler
    @FXML private Button btnEnregistrer;                    // Bouton enregistrer

    // ========== SERVICES ==========
    private DisponibilitePsyService disponibiliteService;

    // ========== DONNÉES ==========
    private int userIdConnecte;
    private Stage modalStage;

    // ========== INITIALISATION ==========
    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();

        // Restreindre la saisie des champs (chiffres seulement)
        restreindreSaisieChiffres(txtHeureDebut, 2);
        restreindreSaisieChiffres(txtMinuteDebut, 2);
        restreindreSaisieChiffres(txtHeureFin, 2);
        restreindreSaisieChiffres(txtMinuteFin, 2);

        // Configurer les actions des boutons
        btnEnregistrer.setOnAction(event -> enregistrerDisponibilite());
        btnAnnuler.setOnAction(event -> fermerModal());
        btnFermer.setOnAction(event -> fermerModal());
    }

    /**
     * Restreint la saisie d'un TextField aux chiffres seulement
     * @param textField Le champ à restreindre
     * @param maxLength Longueur maximale
     */
    private void restreindreSaisieChiffres(TextField textField, int maxLength) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Ne garder que les chiffres
            if (!newVal.matches("\\d*")) {
                textField.setText(oldVal);
            }
            // Limiter la longueur
            if (newVal.length() > maxLength) {
                textField.setText(newVal.substring(0, maxLength));
            }
        });
    }

    /**
     * Valide et récupère une valeur entière depuis un TextField
     * @param textField Le champ à lire
     * @param min Valeur minimale
     * @param max Valeur maximale
     * @return La valeur entière, ou -1 si invalide
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
     * Enregistre la disponibilité
     */
    private void enregistrerDisponibilite() {
        // ========== 1. VÉRIFICATION DATE ==========
        if (datePicker.getValue() == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                    "❌ Veuillez sélectionner une date.");
            return;
        }

        LocalDate dateChoisie = datePicker.getValue();
        LocalDate dateActuelle = LocalDate.now();

        if (dateChoisie.isBefore(dateActuelle)) {
            afficherAlerte(Alert.AlertType.WARNING, "Date invalide",
                    "❌ La date ne peut pas être dans le passé.");
            return;
        }

        // ========== 2. RÉCUPÉRATION ET VALIDATION HEURE DÉBUT ==========
        int heureDebut = getValeurTextField(txtHeureDebut, 0, 23);
        if (heureDebut == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Heure invalide",
                    "❌ Heure de début invalide.\nVeuillez saisir un nombre entre 0 et 23.");
            return;
        }

        int minuteDebut = getValeurTextField(txtMinuteDebut, 0, 59);
        if (minuteDebut == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Minute invalide",
                    "❌ Minute de début invalide.\nVeuillez saisir un nombre entre 0 et 59.");
            return;
        }

        // ========== 3. RÉCUPÉRATION ET VALIDATION HEURE FIN ==========
        int heureFin = getValeurTextField(txtHeureFin, 0, 23);
        if (heureFin == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Heure invalide",
                    "❌ Heure de fin invalide.\nVeuillez saisir un nombre entre 0 et 23.");
            return;
        }

        int minuteFin = getValeurTextField(txtMinuteFin, 0, 59);
        if (minuteFin == -1) {
            afficherAlerte(Alert.AlertType.WARNING, "Minute invalide",
                    "❌ Minute de fin invalide.\nVeuillez saisir un nombre entre 0 et 59.");
            return;
        }

        // ========== 4. VÉRIFICATION DÉBUT < FIN ==========
        int debutTotalMinutes = heureDebut * 60 + minuteDebut;
        int finTotalMinutes = heureFin * 60 + minuteFin;

        if (debutTotalMinutes >= finTotalMinutes) {
            afficherAlerte(Alert.AlertType.WARNING, "Erreur horaire",
                    "❌ L'heure de début doit être avant l'heure de fin.\n\n" +
                            "Début: " + String.format("%02d:%02d", heureDebut, minuteDebut) + "\n" +
                            "Fin: " + String.format("%02d:%02d", heureFin, minuteFin));
            return;
        }

        // ========== 5. VÉRIFICATION TYPE ==========
        if (cbTypeConsult.getValue() == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                    "❌ Veuillez sélectionner un type de consultation.");
            return;
        }

        // ========== 6. VÉRIFICATION LIEU ==========
        String lieu = txtLieu.getText().trim();
        if (lieu.isEmpty()) {
            afficherAlerte(Alert.AlertType.WARNING, "Champ manquant",
                    "❌ Veuillez saisir un lieu.");
            return;
        }

        // ========== 7. ENREGISTREMENT ==========
        try {
            LocalDate date = datePicker.getValue();
            String heureDebutStr = String.format("%02d:%02d", heureDebut, minuteDebut);
            String heureFinStr = String.format("%02d:%02d", heureFin, minuteFin);
            String typeConsultStr = cbTypeConsult.getValue();

            // Conversion en Time pour la base de données
            Time heureDebutTime = Time.valueOf(String.format("%02d:%02d:00", heureDebut, minuteDebut));
            Time heureFinTime = Time.valueOf(String.format("%02d:%02d:00", heureFin, minuteFin));

            TypeConsultation typeConsult = "Présentiel".equals(typeConsultStr)
                    ? TypeConsultation.présentiel
                    : TypeConsultation.en_ligne;

            DisponibilitePsy disponibilite = new DisponibilitePsy(
                    userIdConnecte,
                    Date.valueOf(date),
                    heureDebutTime,
                    heureFinTime,
                    typeConsult,
                    lieu
            );

            disponibiliteService.ajouter(disponibilite);

            afficherAlerte(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Disponibilité ajoutée avec succès !\n\n" +
                            "📅 Date: " + date + "\n" +
                            "⏰ Horaire: " + heureDebutStr + " - " + heureFinStr + "\n" +
                            "📍 Type: " + typeConsultStr + "\n" +
                            "🏠 Lieu: " + lieu);

            fermerModal();

        } catch (SQLException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "❌ Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "❌ Erreur inattendue: " + e.getMessage());
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

    public void setUserId(int userId) {
        this.userIdConnecte = userId;
        System.out.println("Modal d'ajout - ID psychologue: " + userId);
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }
}