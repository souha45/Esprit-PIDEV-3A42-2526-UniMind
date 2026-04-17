package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.example.entities.Etudiant;
import org.example.entities.User;

public class DashboardEtudiantController extends BaseDashboardController {

    @FXML private Label identifiantLabel;
    @FXML private Label etablissementLabel;
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    @FXML private VBox sidebarContainer;

    private SidebarEtudiantController sidebarController;

    @FXML
    public void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SidebarEtudiant.fxml"));
            VBox sidebar = loader.load();

            sidebarController = loader.getController();

            sidebarContainer.getChildren().add(sidebar);

            System.out.println("✅ Sidebar chargé avec son controller");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUser(User user) {
        super.setUser(user);

        if (sidebarController != null) {
            sidebarController.setParentController(this);
            System.out.println("✅ Sidebar connecté !");
        } else {
            System.err.println("❌ SidebarController NULL !");
        }

        if (user instanceof Etudiant) {
            Etudiant e = (Etudiant) user;
            identifiantLabel.setText(e.getIdentifiant());
            etablissementLabel.setText(e.getNomEtablissement());
        }
    }
}