package org.example.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Contexte de navigation pour partager des références entre contrôleurs
 */
public class NavigationContext {
    private static ScrollPane contentScrollPane;
    private static VBox sidebar;
    
    public static void setContentScrollPane(ScrollPane scrollPane) {
        contentScrollPane = scrollPane;
    }
    
    public static ScrollPane getContentScrollPane() {
        return contentScrollPane;
    }
    
    public static void setSidebar(VBox vbox) {
        sidebar = vbox;
    }
    
    public static VBox getSidebar() {
        return sidebar;
    }
    
    /**
     * Charge un contenu FXML dans le ScrollPane central
     */
    public static void loadContentInCenter(String fxmlPath) throws IOException {
        if (contentScrollPane == null) {
            throw new IllegalStateException("ScrollPane non initialisé. Appelez setContentScrollPane d'abord.");
        }
        
        var resource = NavigationContext.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }
        
        Parent content = FXMLLoader.load(resource);
        contentScrollPane.setContent(content);
    }
    
    /**
     * Charge un contenu FXML dans le ScrollPane central avec un contrôleur pré-initialisé
     */
    public static void loadContentInCenter(String fxmlPath, Object controller) throws IOException {
        if (contentScrollPane == null) {
            throw new IllegalStateException("ScrollPane non initialisé. Appelez setContentScrollPane d'abord.");
        }
        
        var resource = NavigationContext.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }
        
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(controller);
        Parent content = loader.load();
        contentScrollPane.setContent(content);
    }
    
    /**
     * Charge un contenu Parent directement dans le ScrollPane central
     */
    public static void loadContentInCenter(Parent content) {
        if (contentScrollPane == null) {
            throw new IllegalStateException("ScrollPane non initialisé. Appelez setContentScrollPane d'abord.");
        }
        
        contentScrollPane.setContent(content);
    }
    
    public static void clear() {
        contentScrollPane = null;
        sidebar = null;
    }
}
