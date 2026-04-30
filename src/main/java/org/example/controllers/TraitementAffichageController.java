package org.example.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.example.entities.Etudiant;
import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.services.EtudiantTraitementService;
import org.example.services.OrdonnancePDFService;
import org.example.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class TraitementAffichageController implements Initializable {

    @FXML private Label lblTitre;
    @FXML private Label lblType;
    @FXML private Label lblCategorie;
    @FXML private Label lblStatut;
    @FXML private Label lblPriorite;
    @FXML private Label lblDureeJours;
    @FXML private Label lblDateDebut;
    @FXML private Label lblDateFin;
    @FXML private Label lblDosage;
    @FXML private Label lblEtudiant;
    @FXML private Label lblPsychologue;
    @FXML private Label lblObjectifTherapeutique;
    @FXML private Label lblDescription;
    @FXML private Label lblStatus;

    private EtudiantTraitementService etudiantTraitementService;
    private Traitement traitementAffiche;
    private Etudiant etudiantAffiche;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            etudiantTraitementService = new EtudiantTraitementService();
        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
        }
    }

    public void setTraitement(Traitement traitement) {
        this.traitementAffiche = traitement;

        if (traitement == null) {
            lblStatus.setText("✗ Erreur: Aucun traitement à afficher");
            return;
        }

        // Informations de base
        lblTitre.setText(traitement.getTitre());
        lblType.setText(traitement.getType());
        lblCategorie.setText(traitement.getCategorie().name());
        lblDureeJours.setText(traitement.getDureeJours() > 0 ? String.valueOf(traitement.getDureeJours()) : "Non spécifiée");

        // Statut avec badge stylisé
        String statut = traitement.getStatut().name();
        lblStatut.setText(statut.equals("EN_COURS") ? "En cours" : statut.equals("TERMINE") ? "Terminé" : "Suspendu");
        lblStatut.setStyle(getStatutStyle(statut));

        // Priorité avec badge stylisé
        String priorite = traitement.getPriorite().name();
        lblPriorite.setText(priorite.equals("HAUTE") ? "Haute" : priorite.equals("MOYENNE") ? "Moyenne" : "Basse");
        lblPriorite.setStyle(getPrioriteStyle(priorite));

        // Dates formatées
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblDateDebut.setText(traitement.getDateDebut() != null ? traitement.getDateDebut().toLocalDate().format(formatter) : "Non spécifiée");
        lblDateFin.setText(traitement.getDateFin() != null ? traitement.getDateFin().toLocalDate().format(formatter) : "Non spécifiée");

        // Dosage
        lblDosage.setText(traitement.getDosage() != null && !traitement.getDosage().isEmpty() ? traitement.getDosage() : "Non spécifié");

        // Objectif et Description
        lblObjectifTherapeutique.setText(traitement.getObjectifTherapeutique() != null && !traitement.getObjectifTherapeutique().isEmpty()
                ? traitement.getObjectifTherapeutique() : "Non spécifié");
        lblDescription.setText(traitement.getDescription() != null && !traitement.getDescription().isEmpty()
                ? traitement.getDescription() : "Non spécifiée");

        // Psychologue
        lblPsychologue.setText(SessionManager.getInstance().getNomUtilisateur());

        // Charger l'étudiant
        chargerEtudiant(traitement.getEtudiantId());

        lblStatus.setText("✓ Détails du traitement: " + traitement.getTitre());
    }

    private String getStatutStyle(String statut) {
        switch(statut) {
            case "EN_COURS": return "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca;";
            case "TERMINE": return "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            case "SUSPENDU": return "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412;";
            default: return "";
        }
    }

    private String getPrioriteStyle(String priorite) {
        switch(priorite) {
            case "HAUTE": return "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;";
            case "MOYENNE": return "-fx-background-color: #fef3c7; -fx-text-fill: #d97706;";
            case "BASSE": return "-fx-background-color: #e0e7ff; -fx-text-fill: #4f46e5;";
            default: return "";
        }
    }

    private void chargerEtudiant(int etudiantId) {
        try {
            if (etudiantId > 0) {
                var etudiants = etudiantTraitementService.afficher();
                Etudiant etudiantTrouve = null;
                for (Etudiant etudiant : etudiants) {
                    if (etudiant.getUserId() == etudiantId) {
                        etudiantTrouve = etudiant;
                        break;
                    }
                }
                this.etudiantAffiche = etudiantTrouve;
                lblEtudiant.setText(etudiantTrouve != null ? etudiantTrouve.getNom() + " " + etudiantTrouve.getPrenom() : "Étudiant non trouvé");
            } else {
                this.etudiantAffiche = null;
                lblEtudiant.setText("Non spécifié");
            }
        } catch (Exception e) {
            this.etudiantAffiche = null;
            lblEtudiant.setText("Erreur de chargement");
        }
    }

    @FXML private void handleFermer() { fermerFenetre(); }

    @FXML
    private void handleExporterPDF() {
        if (traitementAffiche == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun traitement");
            alert.setHeaderText(null);
            alert.setContentText("Aucun traitement à exporter");
            alert.showAndWait();
            return;
        }

        try {
            // Récupérer l'utilisateur connecté (psychologue)
            SessionManager session = SessionManager.getInstance();
            if (!session.estConnecte()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'authentification");
                alert.setHeaderText(null);
                alert.setContentText("Vous devez être connecté en tant que psychologue pour exporter une ordonnance");
                alert.showAndWait();
                return;
            }

            User utilisateur = session.getCurrentUser();

            // Créer le service PDF et générer l'ordonnance avec les informations du patient
            OrdonnancePDFService pdfService = new OrdonnancePDFService();
            byte[] pdfBytes = pdfService.genererOrdonnancePDF(traitementAffiche, utilisateur, etudiantAffiche);

            // Choix du fichier de sauvegarde
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer l'ordonnance PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf")
            );

            String nomFichier = pdfService.genererNomFichier(traitementAffiche);
            fileChooser.setInitialFileName(nomFichier);

            java.io.File file = fileChooser.showSaveDialog(lblTitre.getScene().getWindow());

            if (file != null) {
                // Sauvegarder le PDF
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(pdfBytes);
                }

                // Mettre à jour le statut
                lblStatus.setText("✓ Ordonnance exportée: " + file.getName());

                // Afficher un message de confirmation
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Export réussi");
                successAlert.setHeaderText(null);
                successAlert.setContentText("L'ordonnance a été exportée avec succès :\n" + file.getName());
                successAlert.showAndWait();
            }

        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Erreur d'export");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Une erreur est survenue lors de l'export de l'ordonnance :\n" + e.getMessage());
            errorAlert.showAndWait();
            e.printStackTrace();
        }
    }

    private void fermerFenetre() { ((javafx.stage.Stage) lblTitre.getScene().getWindow()).close(); }
}