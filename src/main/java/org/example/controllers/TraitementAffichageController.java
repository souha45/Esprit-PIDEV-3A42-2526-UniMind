package org.example.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.example.entities.Etudiant;
import org.example.entities.Traitement;
import org.example.services.EtudiantService;
import org.example.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public class TraitementAffichageController implements Initializable {

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblType;
    @FXML
    private Label lblCategorie;
    @FXML
    private Label lblStatut;
    @FXML
    private Label lblPriorite;
    @FXML
    private Label lblDureeJours;
    @FXML
    private Label lblDateDebut;
    @FXML
    private Label lblDateFin;
    @FXML
    private Label lblDosage;
    @FXML
    private Label lblEtudiant;
    @FXML
    private Label lblPsychologue;
    @FXML
    private Label lblObjectifTherapeutique;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblStatus;

    private EtudiantService etudiantService;
    private Traitement traitementAffiche;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            etudiantService = new EtudiantService();
        } catch (Exception e) {
            lblStatus.setText("Erreur lors du chargement: " + e.getMessage());
        }
    }

    // Méthode pour initialiser avec le traitement à afficher
    public void setTraitement(Traitement traitement) {
        this.traitementAffiche = traitement;

        // Remplir les labels avec les données du traitement
        lblTitre.setText(traitement.getTitre());
        lblType.setText(traitement.getType());
        lblCategorie.setText(traitement.getCategorie().name());
        lblStatut.setText(traitement.getStatut().name());
        lblPriorite.setText(traitement.getPriorite().name());

        // Durée calculée si disponible
        if (traitement.getDureeJours() > 0) {
            lblDureeJours.setText(String.valueOf(traitement.getDureeJours()));
        } else {
            lblDureeJours.setText("Non spécifiée");
        }

        // Dates formatées
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (traitement.getDateDebut() != null) {
            lblDateDebut.setText(traitement.getDateDebut().toLocalDate().format(formatter));
        } else {
            lblDateDebut.setText("Non spécifiée");
        }

        if (traitement.getDateFin() != null) {
            lblDateFin.setText(traitement.getDateFin().toLocalDate().format(formatter));
        } else {
            lblDateFin.setText("Non spécifiée");
        }

        // Autres champs
        lblDosage.setText(traitement.getDosage() != null ? traitement.getDosage() : "Non spécifié");
        lblObjectifTherapeutique.setText(traitement.getObjectifTherapeutique() != null ? traitement.getObjectifTherapeutique() : "Non spécifié");
        lblDescription.setText(traitement.getDescription() != null ? traitement.getDescription() : "Non spécifiée");

        // Afficher le nom du psychologue connecté
        lblPsychologue.setText(SessionManager.getInstance().getNomUtilisateur());

        // Charger le nom de l'étudiant
        try {
            if (traitement.getEtudiantId() > 0) {
                // Utiliser la méthode afficher() pour récupérer tous les étudiants et trouver celui qui correspond
                var etudiants = etudiantService.afficher();
                Etudiant etudiantTrouve = null;
                for (Etudiant etudiant : etudiants) {
                    if (etudiant.getUserId() == traitement.getEtudiantId()) {
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

        lblStatus.setText("Détails du traitement: " + traitement.getTitre());
    }

    @FXML
    private void handleFermer() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        // Fermer la fenêtre actuelle
        javafx.stage.Stage stage = (javafx.stage.Stage) lblTitre.getScene().getWindow();
        stage.close();
    }
}
