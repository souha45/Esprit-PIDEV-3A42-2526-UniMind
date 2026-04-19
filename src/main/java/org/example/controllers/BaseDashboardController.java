package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.services.*;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class BaseDashboardController {

    protected User utilisateurConnecte;

    protected AdminService adminService = new AdminService();
    protected EtudiantService etudiantService = new EtudiantService();
    protected PsychologueService psychologueService = new PsychologueService();
    protected ResponsableService responsableService = new ResponsableService();

    protected Label navNomLabel;
    protected ImageView navPhoto;

    private Image defaultAvatar;

    protected Connection getConnection() {
        return adminService.getConnection();
    }

    protected Image getDefaultAvatar() {
        if (defaultAvatar == null) {
            try {
                InputStream is = getClass().getResourceAsStream("/images/default_avatar.png");
                if (is != null) {
                    defaultAvatar = new Image(is);
                }
            } catch (Exception e) {
                defaultAvatar = null;
            }
        }
        return defaultAvatar;
    }

    public void setUser(User user) {
        this.utilisateurConnecte = user;
        System.out.println("BaseDashboardController.setUser() - User: " + (user != null ? user.getPrenom() + " " + user.getNom() : "null"));
        afficherInfosNavbar();
    }

    public User getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    protected void afficherInfosNavbar() {
        if (navNomLabel != null && utilisateurConnecte != null) {
            navNomLabel.setText(utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom());
        }
        chargerPhotoNavbar();
    }

    protected void chargerPhotoNavbar() {
        if (navPhoto == null || utilisateurConnecte == null) return;
        String photoPath = getPhotoChemin();
        if (photoPath != null && !photoPath.isEmpty()) {
            File file = new File(photoPath);
            if (file.exists()) {
                navPhoto.setImage(new Image(file.toURI().toString()));
                return;
            }
        }
        Image def = getDefaultAvatar();
        if (def != null) navPhoto.setImage(def);
    }

    protected String getPhotoChemin() {
        String query = "SELECT photo FROM profil WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, utilisateurConnecte.getUserId());
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("photo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void rafraichirPhotoNavbar() {
        chargerPhotoNavbar();
    }

    @FXML
    public void seDeconnecter() {
        try {
            // récupérer la fenêtre actuelle via n'importe quel node actif
            Stage stage = (Stage) javafx.stage.Window.getWindows().filtered(w -> w.isShowing()).get(0);

            stage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("UniMind - Connexion");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode utilitaire à ajouter dans BaseDashboardController
// (réutilisable par tous les controllers)
    protected Image chargerImageDepuisChemin(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) return null;

        // Essai absolu
        File f1 = new File(photoPath);
        if (f1.exists()) return new Image(f1.toURI().toString());

        // Essai relatif au user.dir
        File f2 = new File(System.getProperty("user.dir"), photoPath);
        if (f2.exists()) return new Image(f2.toURI().toString());

        // Essai nom seul dans uploads/
        File f3 = new File(System.getProperty("user.dir"),
                "uploads/" + new File(photoPath).getName());
        if (f3.exists()) return new Image(f3.toURI().toString());

        return null;
    }
}