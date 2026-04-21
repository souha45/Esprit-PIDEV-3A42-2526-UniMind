package org.example.utils;

import org.example.entities.User;
import org.example.enums.SaisiPar;

/**
 * Gestionnaire de session pour gérer les rôles et permissions utilisateurs
 * Intégré avec le système d'authentification du collègue
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private String roleUtilisateur; // "PSYCHOLOGUE" ou "ETUDIANT"

    private SessionManager() {
        this.currentUser = null;
        this.roleUtilisateur = null;
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ==================== INTÉGRATION AVEC LE LOGIN ====================

    public void initSession(User user) {
        if (user != null) {
            this.currentUser = user;
            this.roleUtilisateur = user.getRole().name();
            System.out.println("✅ Session initialisée pour: " + user.getPrenom() + " " + user.getNom() + " (" + roleUtilisateur + ")");
        }
    }

    public void updateSession(User user) {
        if (user != null) {
            this.currentUser = user;
            this.roleUtilisateur = user.getRole().name();
            System.out.println("🔄 Session mise à jour: " + user.getPrenom() + " " + user.getNom());
        }
    }

    public void logout() {
        this.currentUser = null;
        this.roleUtilisateur = null;
        System.out.println("✅ Utilisateur déconnecté");
    }

    public boolean estConnecte() {
        return currentUser != null;
    }

    // ==================== GETTERS ====================

    public User getCurrentUser() {
        return currentUser;
    }

    public int getUtilisateurConnecteId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public String getRoleUtilisateur() {
        return roleUtilisateur;
    }

    public String getNomUtilisateur() {
        return currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "";
    }

    public String getEmailUtilisateur() {
        return currentUser != null ? currentUser.getEmail() : "";
    }

    // ==================== VÉRIFICATIONS DE RÔLE ====================

    public boolean estPsychologue() {
        return "PSYCHOLOGUE".equals(roleUtilisateur);
    }

    public boolean estEtudiant() {
        return "ETUDIANT".equals(roleUtilisateur);
    }

    // ==================== PERMISSIONS POUR LES TRAITEMENTS ====================

    public boolean peutCreerTraitement() {
        return estPsychologue();
    }

    public boolean peutModifierTraitement() {
        return estPsychologue();
    }

    public boolean peutSupprimerTraitement() {
        return estPsychologue();
    }

    public boolean peutVoirTraitement(int traitementEtudiantId, int traitementPsychologueId) {
        if (estPsychologue()) {
            return traitementPsychologueId == getUtilisateurConnecteId();
        } else if (estEtudiant()) {
            return traitementEtudiantId == getUtilisateurConnecteId();
        }
        return false;
    }

    // ==================== PERMISSIONS POUR LES SUIVIS ====================

    public boolean peutCreerSuivi() {
        return true;
    }

    public SaisiPar getRoleSaisiPar() {
        if (estPsychologue()) {
            return SaisiPar.PSYCHOLOGUE;
        } else {
            return SaisiPar.ETUDIANT;
        }
    }

    public boolean peutVoirSuivi(int suiviEtudiantId, int traitementPsychologueId) {
        if (estPsychologue()) {
            return traitementPsychologueId == getUtilisateurConnecteId();
        } else if (estEtudiant()) {
            return suiviEtudiantId == getUtilisateurConnecteId();
        }
        return false;
    }

    public boolean peutModifierSuivi(int suiviSaisiParId) {
        if (estEtudiant()) {
            return suiviSaisiParId == getUtilisateurConnecteId();
        }
        return estPsychologue();
    }

    public boolean peutSupprimerSuivi(int suiviSaisiParId) {
        return peutModifierSuivi(suiviSaisiParId);
    }
}