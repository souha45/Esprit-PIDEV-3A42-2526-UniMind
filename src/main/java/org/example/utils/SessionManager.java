package org.example.utils;

import org.example.entities.User;
import org.example.enums.Role;

import java.util.Optional;

/**
 * Gestionnaire de session pour l'application JavaFX
 * Gère l'utilisateur connecté et son rôle
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
        // Singleton pattern
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Définit l'utilisateur connecté
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Récupère l'utilisateur connecté
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Récupère le rôle de l'utilisateur connecté
     */
    public Optional<Role> getCurrentUserRole() {
        if (currentUser != null && currentUser.getRole() != null) {
            return Optional.of(currentUser.getRole());
        }
        return Optional.empty();
    }

    /**
     * Vérifie si un utilisateur est connecté
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Déconnecte l'utilisateur
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Vérifie si l'utilisateur connecté est admin
     */
    public boolean isAdmin() {
        return getCurrentUserRole().map(role -> role == Role.ADMIN).orElse(false);
    }

    /**
     * Vérifie si l'utilisateur connecté est responsable étudiant
     */
    public boolean isResponsableEtudiant() {
        return getCurrentUserRole().map(role -> role == Role.RESPONSABLE_ETUDIANT).orElse(false);
    }

    /**
     * Vérifie si l'utilisateur connecté est étudiant
     */
    public boolean isEtudiant() {
        return getCurrentUserRole().map(role -> role == Role.ETUDIANT).orElse(false);
    }

    /**
     * Vérifie si l'utilisateur connecté est psychologue
     */
    public boolean isPsychologue() {
        return getCurrentUserRole().map(role -> role == Role.PSYCHOLOGUE).orElse(false);
    }

    /**
     * Récupère l'ID de l'utilisateur connecté
     */
    public Optional<Integer> getCurrentUserId() {
        if (currentUser != null) {
            return Optional.of(currentUser.getUserId());
        }
        return Optional.empty();
    }

    /**
     * Récupère le nom complet de l'utilisateur connecté
     */
    public Optional<String> getCurrentUserFullName() {
        if (currentUser != null) {
            return Optional.of(currentUser.getPrenom() + " " + currentUser.getNom());
        }
        return Optional.empty();
    }
}
