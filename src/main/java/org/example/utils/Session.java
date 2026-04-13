package org.example.utils;

import org.example.entities.User;

/**
 * Singleton pour gérer l'utilisateur connecté dans toute l'application.
 * Utilisation: Session.getInstance().setCurrentUser(user);
 */
public class Session {
    private static Session instance;
    private User currentUser;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public void clear() { this.currentUser = null; }
    public boolean isLoggedIn() { return currentUser != null; }
}