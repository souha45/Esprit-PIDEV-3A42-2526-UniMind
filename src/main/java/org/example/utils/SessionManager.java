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
        // Initialisation vide - sera remplie après connexion
        this.currentUser = null;
        this.roleUtilisateur = null;
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ==================== INTÉGRATION AVEC LE LOGIN  ====================

    /**
     * Initialise la session avec l'utilisateur connecté
     * À appeler APRÈS la connexion (depuis les contrôleurs dashboard du collègue)
     * @param user Utilisateur connecté (entité User du collègue)
     */
    public void initSession(User user) {
        if (user != null) {
            this.currentUser = user;
            this.roleUtilisateur = user.getRole().name();
            System.out.println("✅ Session initialisée pour: " + user.getPrenom() + " " + user.getNom() + " (" + roleUtilisateur + ")");
        }
    }

    /**
     * Déconnecte l'utilisateur
     */
    public void logout() {
        this.currentUser = null;
        this.roleUtilisateur = null;
        System.out.println("✅ Utilisateur déconnecté");
    }

    /**
     * Vérifie si un utilisateur est connecté
     */
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

    /**
     * Seul le psychologue peut créer un traitement
     */
    public boolean peutCreerTraitement() {
        return estPsychologue();
    }

    /**
     * Seul le psychologue peut modifier un traitement
     */
    public boolean peutModifierTraitement() {
        return estPsychologue();
    }

    /**
     * Seul le psychologue peut supprimer un traitement
     */
    public boolean peutSupprimerTraitement() {
        return estPsychologue();
    }

    /**
     * Vérifie si l'utilisateur peut voir un traitement
     * @param traitementEtudiantId ID de l'étudiant lié au traitement
     * @param traitementPsychologueId ID du psychologue lié au traitement
     */
    public boolean peutVoirTraitement(int traitementEtudiantId, int traitementPsychologueId) {
        if (estPsychologue()) {
            // Le psychologue voit uniquement ses propres traitements
            return traitementPsychologueId == getUtilisateurConnecteId();
        } else if (estEtudiant()) {
            // L'étudiant voit seulement ses traitements
            return traitementEtudiantId == getUtilisateurConnecteId();
        }
        return false;
    }

    // ==================== PERMISSIONS POUR LES SUIVIS ====================

    /**
     * Psychologue et étudiant peuvent créer des suivis
     */
    public boolean peutCreerSuivi() {
        return true;
    }

    /**
     * Retourne le rôle de saisie pour un suivi (PSYCHOLOGUE ou ETUDIANT)
     */
    public SaisiPar getRoleSaisiPar() {
        if (estPsychologue()) {
            return SaisiPar.PSYCHOLOGUE;
        } else {
            return SaisiPar.ETUDIANT;
        }
    }

    /**
     * Vérifie si l'utilisateur peut voir un suivi
     * @param suiviEtudiantId ID de l'étudiant lié au traitement du suivi
     * @param traitementPsychologueId ID du psychologue lié au traitement
     */
    public boolean peutVoirSuivi(int suiviEtudiantId, int traitementPsychologueId) {
        if (estPsychologue()) {
            // Le psychologue voit les suivis de SES traitements
            return traitementPsychologueId == getUtilisateurConnecteId();
        } else if (estEtudiant()) {
            // L'étudiant voit les suivis de SES traitements
            return suiviEtudiantId == getUtilisateurConnecteId();
        }
        return false;
    }

    /**
     * Vérifie si l'utilisateur peut modifier/supprimer un suivi
     * @param suiviSaisiParId ID de la personne qui a saisi le suivi
     */
    public boolean peutModifierSuivi(int suiviSaisiParId) {
        if (estEtudiant()) {
            // L'étudiant ne peut modifier que ses propres suivis (saisiPar = ETUDIANT)
            return suiviSaisiParId == getUtilisateurConnecteId();
        }
        // Le psychologue peut modifier tous les suivis
        return estPsychologue();
    }

    /**
     * Vérifie si l'utilisateur peut supprimer un suivi
     */
    public boolean peutSupprimerSuivi(int suiviSaisiParId) {
        return peutModifierSuivi(suiviSaisiParId);
    }
}