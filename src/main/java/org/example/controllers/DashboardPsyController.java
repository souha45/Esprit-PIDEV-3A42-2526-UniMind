package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.models.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardPsyController implements SidebarPsyController.PsyPageController {  // ← ajouter

    // ========== COMPOSANTS FXML ==========
    @FXML private Label lblDate;
    @FXML private VBox contenuDashboard;
    @FXML private VBox contenuPrincipal;

    // Statistiques
    @FXML private Label lblRdvAujourdhui;
    @FXML private Label lblPatientsActifs;
    @FXML private Label lblRdvSemaine;
    @FXML private Label lblTauxRemplissage;

    // ✅ Sidebar (inclus via fx:include) - JavaFX injecte automatiquement
    @FXML private SidebarPsyController sidebarPsyController;

    private User utilisateur;

    @FXML
    public void initialize() {
        // Date du jour
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Charger les statistiques (valeurs temporaires)
        chargerStatistiques();
    }

    /**
     * ✅ Cette méthode est appelée APRÈS que le FXML soit chargé
     * JavaFX injecte automatiquement le contrôleur de l'include
     * grâce au fx:id="sidebarPsy" sur le fx:include
     */

    public void setUtilisateur(User user) {
        this.utilisateur = user;

        // Passer l'utilisateur à la sidebar
        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
        }

        // Afficher le dashboard par défaut
        afficherDashboard();
    }

    private void chargerStatistiques() {
        // TODO: Remplacer par des vraies données de la base
        lblRdvAujourdhui.setText("4");
        lblPatientsActifs.setText("12");
        lblRdvSemaine.setText("18");
        lblTauxRemplissage.setText("75%");
    }

    private void afficherDashboard() {
        contenuPrincipal.setVisible(false);
        contenuPrincipal.setManaged(false);
        contenuDashboard.setVisible(true);
        contenuDashboard.setManaged(true);
    }


}