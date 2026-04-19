package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.enums.Role;
import org.example.services.UserService;
import org.example.utils.Session;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField tfEmail;
    @FXML private PasswordField tfPassword;
    @FXML private Label errLogin;

    private final UserService userService = new UserService();

    @FXML
    private void onLogin() {
        String email = tfEmail.getText().trim();
        String password = tfPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showErr("Veuillez remplir tous les champs.");
            return;
        }

        try {
            User user = userService.login(email, password);
            if (user == null) {
                showErr("Email ou mot de passe incorrect.");
            } else {
                Session.getInstance().setCurrentUser(user);
                navigateBasedOnRole(user);
            }
        } catch (SQLException e) {
            showErr("Erreur de connexion : " + e.getMessage());
        }
    }

    /** Accès rapide étudiant pour les tests */
    @FXML
    private void loginAsStudent() {
        User fakeStudent = new User();
        fakeStudent.setUserId(1);
        fakeStudent.setNom("Dupont");
        fakeStudent.setPrenom("Marie");
        fakeStudent.setEmail("marie@test.com");
        fakeStudent.setRole(Role.ETUDIANT);
        Session.getInstance().setCurrentUser(fakeStudent);
        navigateToStudent();
    }

    /** Accès rapide admin pour les tests */
    @FXML
    private void loginAsAdmin() {
        User fakeAdmin = new User();
        fakeAdmin.setUserId(99);
        fakeAdmin.setNom("Admin");
        fakeAdmin.setPrenom("Super");
        fakeAdmin.setEmail("admin@test.com");
        fakeAdmin.setRole(Role.ADMIN);
        Session.getInstance().setCurrentUser(fakeAdmin);
        navigateToAdmin();
    }

    private void navigateBasedOnRole(User user) {
        if (user.getRole() == Role.ADMIN) {
            navigateToAdmin();
        } else {
            navigateToStudent();
        }
    }

    private void navigateToAdmin() {
        loadScene("/org/example/views/AdminLayout.fxml", 1200, 750);
    }

    private void navigateToStudent() {
        loadScene("/SidebarEtudiant.fxml", 1200, 750);
    }

    private void loadScene(String fxml, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load(), w, h);
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showErr("Erreur chargement interface : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErr(String msg) {
        errLogin.setText("⚠ " + msg);
        errLogin.setVisible(true);
        errLogin.setManaged(true);
    }
}