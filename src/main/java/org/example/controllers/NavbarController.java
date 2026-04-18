package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.entities.User;
import org.example.utils.SessionManager;

public class NavbarController {

    @FXML protected Label navNomLabel;
    @FXML protected ImageView navPhoto;
    @FXML protected TextField navRechercheField;

    @FXML
    public void initialize() {
        afficherInfosNavbar();
        chargerPhotoNavbar();
    }

    private User getUtilisateur() {
        return SessionManager.getInstance().getCurrentUser().orElse(null);
    }

    private void afficherInfosNavbar() {
        User user = getUtilisateur();
        if (navNomLabel != null && user != null) {
            navNomLabel.setText(user.getPrenom() + " " + user.getNom());
        }
    }

    private void chargerPhotoNavbar() {
        if (navPhoto == null) return;
        try {
            navPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png")));
        } catch (Exception e) {}
    }

    @FXML
    public void onRecherche() {
        // Recherche à implémenter si besoin
    }
}
