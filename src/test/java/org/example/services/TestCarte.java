package org.example.services;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class TestCarte extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("🔄 Chargement de la carte OpenStreetMap...");

        // 1. Créer la WebView (composant qui affiche une page web)
        WebView webView = new WebView();

        // 2. Récupérer le moteur de rendu
        WebEngine webEngine = webView.getEngine();

        // 3. Charger le fichier HTML local
        java.net.URL url = getClass().getResource("/carte.html");

        // 4. Vérifier si le fichier existe
        if (url == null) {
            System.err.println("❌ Erreur : Fichier carte.html non trouvé !");
            System.err.println("   Assurez-vous qu'il est dans : src/main/resources/carte.html");
            return;
        }

        System.out.println("✅ Fichier trouvé : " + url);

        // 5. Charger la page HTML dans la WebView
        webEngine.load(url.toExternalForm());

        // 6. Créer le conteneur et la scène
        VBox root = new VBox(webView);
        Scene scene = new Scene(root, 1000, 700);

        // 7. Configurer et afficher la fenêtre
        primaryStage.setTitle("🗺️ OpenStreetMap - Sélection d'un lieu");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("✅ Carte affichée avec succès !");
        System.out.println("   - Cliquez sur la carte pour sélectionner un lieu");
        System.out.println("   - Utilisez la barre de recherche pour trouver une adresse");
    }

    public static void main(String[] args) {
        // Lancer l'application JavaFX
        launch(args);
    }
}