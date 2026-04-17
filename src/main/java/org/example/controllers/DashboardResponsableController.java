package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.entities.ResponsableEtudiant;
import org.example.entities.User;

public class DashboardResponsableController extends BaseDashboardController {

    @FXML private Label posteLabel;
    @FXML private Label etablissementLabel;

    @FXML private VBox sidebarContainer;

    private SidebarResponsableController sidebarController;

    @FXML
    public void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SidebarResponsable.fxml"));
            VBox sidebar = loader.load();

            sidebarController = loader.getController();
            sidebarContainer.getChildren().add(sidebar);

            System.out.println("✅ Sidebar Responsable chargé");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUser(User user) {
        super.setUser(user);

        if (sidebarController != null) {
            sidebarController.setParentController(this);
            System.out.println("✅ Sidebar Responsable connecté");
        } else {
            System.err.println("❌ Sidebar Responsable NULL");
        }

        if (user instanceof ResponsableEtudiant r) {
            posteLabel.setText(r.getPoste());
            etablissementLabel.setText(r.getEtablissement());
        }
    }
}