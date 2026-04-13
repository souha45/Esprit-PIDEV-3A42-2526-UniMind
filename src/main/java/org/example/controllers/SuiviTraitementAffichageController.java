package org.example.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.enums.SaisiPar;
import org.example.services.EtudiantService;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.stage.Stage;


public class SuiviTraitementAffichageController implements Initializable {


    @FXML
    private Label lblDateSuivi;
    @FXML
    private Label lblTraitement;
    @FXML
    private Label lblEtudiant;
    @FXML
    private Label lblPsychologue;
    @FXML
    private Label lblSaisiPar;
    @FXML
    private Label lblNotes;
    @FXML
    private Label lblStatus;

    // SERVICES

    private EtudiantService etudiantService;
    private TraitementService traitementService;
    private SuiviTraitementService suiviTraitementService;
    private SuiviTraitement suiviAffiche;

    // INITIALISATION

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            etudiantService = new EtudiantService();
            traitementService = new TraitementService();
            suiviTraitementService = new SuiviTraitementService();
            lblStatus.setText("✓ Prêt à afficher les détails");
        } catch (Exception e) {
            lblStatus.setText("✗ Erreur lors du chargement: " + e.getMessage());
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Initialise l'affichage avec les données du suivi
     * @param suivi Suivi de traitement à afficher
     */
    public void setSuiviTraitement(SuiviTraitement suivi) {
        this.suiviAffiche = suivi;

        if (suivi == null) {
            lblStatus.setText("✗ Erreur: Aucun suivi à afficher");
            return;
        }

        // DATE SUIVI
        if (suivi.getDateSuivi() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            lblDateSuivi.setText(suivi.getDateSuivi().toLocalDate().format(formatter));
        } else {
            lblDateSuivi.setText("Non spécifiée");
        }

        // TRAITEMENT
        chargerTraitement(suivi.getTraitementId());

        //  SAISI PAR
        if (suivi.getSaisiPar() != null) {
            if (suivi.getSaisiPar() == SaisiPar.PSYCHOLOGUE) {
                lblSaisiPar.setText("👨‍⚕️ Psychologue");
                lblSaisiPar.setStyle("-fx-text-fill: #4f46e5; -fx-font-weight: bold;");
            } else {
                lblSaisiPar.setText("👨‍🎓 Étudiant");
                lblSaisiPar.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
            }
        } else {
            lblSaisiPar.setText("Non spécifié");
        }

        // PSYCHOLOGUE
        SessionManager session = SessionManager.getInstance();
        lblPsychologue.setText(session.getNomUtilisateur());

        // NOTES / OBSERVATIONS
        String observations = suivi.getObservations();
        if (observations != null && !observations.isEmpty()) {
            lblNotes.setText(observations);
        } else {
            lblNotes.setText("Aucune observation");
        }

        lblStatus.setText("✓ Détails du suivi du " + lblDateSuivi.getText());
    }

    /**
     * Charge les informations du traitement et de l'étudiant associé
     * @param traitementId ID du traitement
     */
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
                    // Titre du traitement
                    lblTraitement.setText(traitementTrouve.getTitre());
                    lblTraitement.setStyle("-fx-font-weight: bold; -fx-text-fill: #4f46e5;");

                    // Chargement de l'étudiant associé
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
            System.err.println("Erreur lors du chargement du traitement: " + e.getMessage());
        }
    }

    /**
     * Charge le nom de l'étudiant à partir de son ID
     * @param etudiantId ID de l'étudiant
     */
    private void chargerEtudiant(int etudiantId) {
        try {
            if (etudiantId > 0) {
                List<Etudiant> etudiants = etudiantService.afficher();
                Etudiant etudiantTrouve = null;

                for (Etudiant etudiant : etudiants) {
                    if (etudiant.getUserId() == etudiantId) {
                        etudiantTrouve = etudiant;
                        break;
                    }
                }

                if (etudiantTrouve != null) {
                    lblEtudiant.setText(etudiantTrouve.getNom() + " " + etudiantTrouve.getPrenom());
                } else {
                    lblEtudiant.setText("Étudiant non trouvé");
                }
            } else {
                lblEtudiant.setText("Non spécifié");
            }
        } catch (Exception e) {
            lblEtudiant.setText("Erreur de chargement");
            System.err.println("Erreur lors du chargement de l'étudiant: " + e.getMessage());
        }
    }

    // ACTIONS DES BOUTONS

    /**
     * Ferme la fenêtre d'affichage
     */
    @FXML
    private void handleFermer() {
        fermerFenetre();
    }

    /**
     * Ferme la fenêtre actuelle
     */
    private void fermerFenetre() {
        Stage stage = (Stage) lblDateSuivi.getScene().getWindow();
        stage.close();
    }
}