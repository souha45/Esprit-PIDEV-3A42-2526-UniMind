package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.User;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SidebarPsychologueController {

    @FXML private ImageView sidebarPhoto;
    @FXML private Label sidebarNomLabel;
    @FXML private Label sidebarPrenomLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Button btnProfil;
    @FXML private Button btnDeconnexion;

    private BaseDashboardController parentController;
    private Connection connection;

    public void setParentController(BaseDashboardController controller) {
        this.parentController = controller;
        System.out.println("SidebarPsychologueController.setParentController() - Parent reçu");

        if (parentController != null) {
            this.connection = parentController.getConnection();
            afficherInfosSidebar();
            chargerPhotoSidebar();
        }
    }

    private User getUtilisateur() {
        return parentController != null ? parentController.getUtilisateurConnecte() : null;
    }

    private void afficherInfosSidebar() {
        User user = getUtilisateur();
        if (sidebarNomLabel != null && user != null) {
            sidebarNomLabel.setText(user.getNom());
            sidebarPrenomLabel.setText(user.getPrenom());
            sidebarRoleLabel.setText("Psychologue");
        }
    }

    private void chargerPhotoSidebar() {
        if (sidebarPhoto == null) return;
        User user = getUtilisateur();
        if (user == null) return;

        String photoPath = getPhotoChemin(user);
        if (photoPath != null && !photoPath.isEmpty()) {
            File file = new File(photoPath);
            if (file.exists()) {
                sidebarPhoto.setImage(new Image(file.toURI().toString()));
                return;
            }
        }
        try {
            sidebarPhoto.setImage(new Image(getClass().getResourceAsStream("/images/default_avatar.png")));
        } catch (Exception e) {}
    }

    private String getPhotoChemin(User user) {
        if (connection == null || user == null) return null;

        String query = "SELECT photo FROM profil WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, user.getUserId());
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("photo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @FXML
    public void ouvrirProfil() {
        User user = getUtilisateur();
        if (user == null) {
            System.err.println("❌ Erreur: impossible de récupérer l'utilisateur");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mon Profil");
            stage.setScene(new Scene(loader.load()));
            ProfilController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            chargerPhotoSidebar();
            if (parentController != null) {
                parentController.rafraichirPhotoNavbar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void seDeconnecter() {
        if (parentController != null) {
            parentController.seDeconnecter();
        }
    }
}