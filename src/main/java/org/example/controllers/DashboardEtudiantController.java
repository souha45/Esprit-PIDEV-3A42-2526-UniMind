package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.models.User;

import java.io.IOException;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;

/**
 * Contrôleur pour le Dashboard Étudiant
 */
public class DashboardEtudiantController implements SidebarEtudiantController.EtudiantPageController {

    // ========== SIDEBAR (inclus) ==========
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    // ========== STATISTIQUES ==========
    @FXML private Label lblProchainsRdv;
    @FXML private Label lblTotalConsultations;
    @FXML private Label lblTraitementsActifs;
    @FXML private Label lblMessagesNonLus;

    // ========== PROCHAIN RDV ==========
    @FXML private VBox carteProchainRdv;
    @FXML private Label lblRdvDate;
    @FXML private Label lblRdvMois;
    @FXML private Label lblRdvPsychologue;
    @FXML private Label lblRdvHoraire;
    @FXML private Label lblRdvType;
    @FXML private Label lblAucunRdv;
    @FXML private Button btnPrendreRdv;

    // ========== TABLEAU RDV PASSÉS ==========
    @FXML private TableView<?> tableViewRdvsPasses;
    @FXML private TableColumn<?, ?> colPsychologue;
    @FXML private TableColumn<?, ?> colDate;
    @FXML private TableColumn<?, ?> colHeure;
    @FXML private TableColumn<?, ?> colType;
    @FXML private TableColumn<?, ?> colStatut;
    @FXML private TableColumn<?, ?> colAction;
    @FXML private Button btnVoirTout;

    // ========== DONNÉES ==========
    private User utilisateur;

    @FXML
    public void initialize() {
        // Configurer les actions via la sidebar


        btnPrendreRdv.setOnAction(event -> prendreRendezVous());
        btnVoirTout.setOnAction(event -> voirTout());
    }

    public void setUtilisateur(User user) {
        this.utilisateur = user;

        // Passer l'utilisateur à la sidebar
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
        }

        // Charger les statistiques
        chargerStatistiques();

        // Charger le prochain rendez-vous
        chargerProchainRendezVous();

        // Charger les rendez-vous passés
        chargerRdvsPasses();
    }

    private void chargerStatistiques() {
        // TODO: Remplacer par des vraies données de la base
        lblProchainsRdv.setText("1");
        lblTotalConsultations.setText("3");
        lblTraitementsActifs.setText("0");
        lblMessagesNonLus.setText("0");
    }

    private void chargerProchainRendezVous() {
        // TODO: Remplacer par une vraie requête SQL
        boolean aUnRdv = true;

        if (aUnRdv) {
            carteProchainRdv.setVisible(true);
            lblAucunRdv.setVisible(false);
            lblRdvDate.setText("23");
            lblRdvMois.setText("AVR");
            lblRdvPsychologue.setText("Consultation avec Dr. psy");
            lblRdvHoraire.setText("17:30");
            lblRdvType.setText("PRÉSENTIEL");
        } else {
            carteProchainRdv.setVisible(false);
            lblAucunRdv.setVisible(true);
        }
    }

    private void chargerRdvsPasses() {
        System.out.println("Chargement des rendez-vous passés...");
    }

    // ========== MÉTHODES DE NAVIGATION ==========

    private void afficherDashboard() {
        // Déjà sur le dashboard
        System.out.println("Dashboard affiché");
    }

    private void prendreRendezVous() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrendreRendezVousModal.fxml"));
            Stage modalStage = new Stage();
            Scene scene = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnPrendreRdv.getScene().getWindow());
            modalStage.setTitle("Prendre un rendez-vous");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            PrendreRendezVousModalController controller = loader.getController();
            controller.setEtudiantId(utilisateur.getUserId());
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerProchainRendezVous();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void voirTout() {
        System.out.println("Voir tous les rendez-vous");

    }
}