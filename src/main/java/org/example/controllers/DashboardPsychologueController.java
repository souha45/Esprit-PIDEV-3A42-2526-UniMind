package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.entities.Psychologue;
import org.example.entities.User;

public class DashboardPsychologueController extends BaseDashboardController {

    @FXML private Label specialiteLabel;
    @FXML private Label adresseLabel;
    @FXML private Label telephoneLabel;

    @FXML private VBox sidebarContainer;

    private SidebarPsychologueController sidebarController;

    @FXML
    public void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SidebarPsy.fxml"));
            VBox sidebar = loader.load();

            sidebarController = loader.getController();
            sidebarContainer.getChildren().add(sidebar);

            System.out.println("✅ Sidebar Psychologue chargé");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUser(User user) {
        super.setUser(user);

        if (sidebarController != null) {
            sidebarController.setParentController(this);
            System.out.println("✅ Sidebar Psychologue connecté");
        } else {
            System.err.println("❌ Sidebar Psychologue NULL");
        }

        if (user instanceof Psychologue p) {
            specialiteLabel.setText(p.getSpecialite());
            adresseLabel.setText(p.getAdresse());
            telephoneLabel.setText(p.getTelephone());
        }
    }
}