package org.example.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.enums.SaisiPar;
import org.example.services.EtudiantTraitementService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SuiviTraitementAffichageController implements Initializable {

    @FXML private Label lblDateSuivi;
    @FXML private Label lblTraitement;
    @FXML private Label lblEtudiant;
    @FXML private Label lblPsychologue;
    @FXML private Label lblSaisiPar;
    @FXML private Label lblNotes;
    @FXML private Label lblStatus;

    private EtudiantTraitementService etudiantTraitementService;
    private TraitementService traitementService;
    private SuiviTraitementService suiviTraitementService;
    private SuiviTraitement suiviAffiche;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            etudiantTraitementService = new EtudiantTraitementService();
            traitementService = new TraitementService();
            suiviTraitementService = new SuiviTraitementService();
            lblStatus.setText("✓ Prêt à afficher les détails");
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
        }
    }

    public void setSuiviTraitement(SuiviTraitement suivi) {
        this.suiviAffiche = suivi;

        if (suivi == null) {
            lblStatus.setText("✗ Erreur: Aucun suivi à afficher");
            return;
        }

        // Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblDateSuivi.setText(suivi.getDateSuivi() != null ? suivi.getDateSuivi().toLocalDate().format(formatter) : "Non spécifiée");

        // Saisi par avec badge
        if (suivi.getSaisiPar() != null) {
            boolean isPsychologue = suivi.getSaisiPar() == SaisiPar.PSYCHOLOGUE;
            lblSaisiPar.setText(isPsychologue ? "Psychologue" : "Étudiant");
            lblSaisiPar.setStyle(isPsychologue
                    ? "-fx-background-color: #ede9fe; -fx-text-fill: #4f46e5;"
                    : "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
        } else {
            lblSaisiPar.setText("Non spécifié");
        }

        // Traitement
        chargerTraitement(suivi.getTraitementId());

        // Psychologue
        lblPsychologue.setText(SessionManager.getInstance().getNomUtilisateur());

        // Notes
        lblNotes.setText(suivi.getObservations() != null && !suivi.getObservations().isEmpty()
                ? suivi.getObservations() : "Aucune observation");

        lblStatus.setText("✓ Détails du suivi du " + lblDateSuivi.getText());
    }

    private void chargerTraitement(int traitementId) {
        try {
            if (traitementId > 0) {
                List<Traitement> traitements = traitementService.afficher();
                Traitement traitementTrouve = null;
                for (Traitement traitement : traitements) {
                    if (traitement.getTraitementId() == traitementId) {
                        traitementTrouve = traitement;
                        break;
                    }
                }
                if (traitementTrouve != null) {
                    lblTraitement.setText(traitementTrouve.getTitre());
                    chargerEtudiant(traitementTrouve.getEtudiantId());
                } else {
                    lblTraitement.setText("Traitement non trouvé");
                    lblEtudiant.setText("Non spécifié");
                }
            } else {
                lblTraitement.setText("Non spécifié");
                lblEtudiant.setText("Non spécifié");
            }
        } catch (Exception e) {
            lblTraitement.setText("Erreur de chargement");
            lblEtudiant.setText("Erreur de chargement");
        }
    }

    private void chargerEtudiant(int etudiantId) {
        try {
            if (etudiantId > 0) {
                List<Etudiant> etudiants = etudiantTraitementService.afficher();
                Etudiant etudiantTrouve = null;
                for (Etudiant etudiant : etudiants) {
                    if (etudiant.getUserId() == etudiantId) {
                        etudiantTrouve = etudiant;
                        break;
                    }
                }
                lblEtudiant.setText(etudiantTrouve != null ? etudiantTrouve.getNom() + " " + etudiantTrouve.getPrenom() : "Étudiant non trouvé");
            } else {
                lblEtudiant.setText("Non spécifié");
            }
        } catch (Exception e) {
            lblEtudiant.setText("Erreur de chargement");
        }
    }

    @FXML private void handleFermer() { fermerFenetre(); }
    private void fermerFenetre() { ((Stage) lblDateSuivi.getScene().getWindow()).close(); }
}