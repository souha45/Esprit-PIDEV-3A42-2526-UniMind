package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class NavbarController {

    @FXML protected Label navNomLabel;
    @FXML protected ImageView navPhoto;
    @FXML protected TextField navRechercheField;

    private BaseDashboardController parentController;

    public void setParentController(BaseDashboardController controller) {
        this.parentController = controller;
        if (parentController != null) {
            parentController.navNomLabel = this.navNomLabel;
            parentController.navPhoto = this.navPhoto;
            parentController.afficherInfosNavbar();
        }
    }

    @FXML
    public void onRecherche() {
        // Recherche à implémenter si besoin
    }
}
