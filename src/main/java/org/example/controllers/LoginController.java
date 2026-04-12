package org.example.controllers;

import javafx.fxml.FXML;//Permet d'utiliser l'annotation @FXML pour lier les composants
import javafx.fxml.FXMLLoader;//Charge les fichiers FXML (interface)
import javafx.scene.Scene;//Représente la fenêtre (contient tout le contenu)
import javafx.scene.control.*;//Tous les composants (Button, Label, TextField...)
import javafx.stage.Stage;//La fenêtre principale de l'application
import org.example.models.User;
import org.example.services.UserService;


import java.io.IOException;

public class LoginController {

    @FXML // Annotation qui dit à JavaFX que cette variable est liée à un élément du fichier FXML
    private TextField txtEmail;//Le nom txtEmail doit correspondre exactement au fx:id="txtEmail" du FXML

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Label lblErreur;

    //JavaFX remplit automatiquement ces variables après avoir chargé le FXML

    private UserService utilisateurService;

    @FXML //indique que cette méthode est appelée automatiquement après que tous les composants sont chargés
    public void initialize() {
        utilisateurService = new UserService();

        // Permettre de valider avec la touche Entrée
        txtPassword.setOnAction(event -> seConnecter());//setOnAction :définit ce qui se passe quand l'utilisateur appuie sur Entrée dans le champ mot de passe(appelle seConnecter())

        // Action du bouton
        btnLogin.setOnAction(event -> seConnecter());//setOnAction : définit ce qui se passe quand on clique sur le bouton (appelle seConnecter())
    }

    private void seConnecter() {
        try {
            // 1. Récupérer les valeurs
            String email = txtEmail.getText().trim();// récupère le texte tapé par l'utilisateur ->.trim() : supprime les espaces au début et à la fin
            String password = txtPassword.getText().trim();

            // 2. Vérifier les champs
            if (email.isEmpty()) {
                lblErreur.setText("Veuillez entrer votre email");
                return;
            }

            if (password.isEmpty()) {
                lblErreur.setText("Veuillez entrer votre mot de passe");
                return;//arrête l'exécution de la méthode (on ne continue pas)
            }

            // 3. Authentifier
            lblErreur.setText("Connexion en cours...");
            User user = utilisateurService.authentifier(email, password);//Appelle le service pour vérifier les identifiants ( Retourne un objet User si succès, null sinon)



            if (user == null) {
                lblErreur.setText("Email ou mot de passe incorrect");
                return;
            }

            // 4. Connexion réussie
            lblErreur.setText("");
            System.out.println("✅ Connexion réussie !");
            System.out.println("   - ID: " + user.getUserId());
            System.out.println("   - Nom: " + user.getNom() + " " + user.getPrenom());
            System.out.println("   - Rôle: " + user.getRole());

            // 5. Rediriger selon le rôle
            String role = user.getRole().toString().toLowerCase();

            if ("psychologue".equals(role)) {
                ouvrirDashboardPsy(user);
            } else if ("etudiant".equals(role)) {
                ouvrirDashboardEtudiant(user);
            } else {
                lblErreur.setText("Rôle non reconnu: " + role);
                return;
            }

            // 6. Fermer la fenêtre de login
            Stage stage = (Stage) btnLogin.getScene().getWindow();//btnLogin.getScene() : récupère la scène (le contenu de la fenêtre) /  getWindow() : récupère la fenêtre (Stage)  /(Stage) : cast (conversion de type) car la méthode retourne un objet Window
            stage.close(); // close() : ferme la fenêtre

        } catch (Exception e) {
            lblErreur.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ouvrirDashboardPsy(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardPsy.fxml")); //FXMLLoader : objet qui lit un fichier FXML  /   getClass().getResource() : cherche le fichier dans les ressources du projet /  /DashboardPsy.fxml : le chemin (le / signifie "à la racine des ressources")
            Scene scene = new Scene(loader.load(), 1000, 600);  //  loader.load() : charge le FXML et crée tous les composants   /  new Scene(...) : crée une nouvelle scène avec la largeur 1000 et hauteur 600

            // Passer l'utilisateur au contrôleur
            DashboardPsyController controller = loader.getController(); // loader.getController() : récupère le contrôleur associé au FXML chargé
            controller.setUtilisateur(user); //passe l'utilisateur connecté au dashboard (transmet l'ID, le nom, etc.)

            Stage stage = new Stage();// Crée une nouvelle fenêtre (Stage)
            stage.setTitle("Unimind - Dashboard Psychologue - " + user.getPrenom() + " " + user.getNom());//Définit le titre avec le nom de l'utilisateur
            stage.setScene(scene);//Place la scène dans la fenêtre
            stage.show();//affiche la fenêtre

        } catch (IOException e) {
            lblErreur.setText("Erreur lors du chargement du dashboard psy");
            e.printStackTrace();
        }
    }

    private void ouvrirDashboardEtudiant(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardEtudiant.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 700);

            DashboardEtudiantController controller = loader.getController();
            controller.setUtilisateur(user);

            Stage stage = new Stage();
            stage.setTitle("Unimind - Dashboard Étudiant - " + user.getPrenom() + " " + user.getNom());
            stage.setScene(scene);
            stage.show();

            // Fermer la fenêtre de login
            Stage loginStage = (Stage) btnLogin.getScene().getWindow();
            loginStage.close();

        } catch (IOException e) {
            lblErreur.setText("Erreur lors du chargement du dashboard étudiant");
            e.printStackTrace();
        }
    }
}