package org.example.utils;

import org.example.enums.SaisiPar;

/**
 * Gestionnaire de session pour gérer les rôles et permissions utilisateurs
 */
public class SessionManager {

    private static SessionManager instance;
    private int utilisateurConnecteId;
    private String roleUtilisateur; // "PSYCHOLOGUE" ou "ETUDIANT"
    private String nomUtilisateur;

    private SessionManager() {
        // Par défaut, psychologue connecté avec ID 1 (pour développement)
        this.utilisateurConnecteId = 1;
        this.roleUtilisateur = "PSYCHOLOGUE";
        this.nomUtilisateur = "psy psy";

        // Par défaut, étudiant connecté avec ID 3 (pour développement)
        //this.utilisateurConnecteId = 3;
        //this.roleUtilisateur = "ETUDIANT";
        //this.nomUtilisateur = "etudiant etudiant";
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Getters et setters
    public int getUtilisateurConnecteId() {
        return utilisateurConnecteId;
    }

    public void setUtilisateurConnecte(int id, String role, String nom) {
        this.utilisateurConnecteId = id;
        this.roleUtilisateur = role;
        this.nomUtilisateur = nom;
    }

    public String getRoleUtilisateur() {
        return roleUtilisateur;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    // Permissions pour les traitements
    public boolean peutCreerTraitement() {
        return "PSYCHOLOGUE".equals(roleUtilisateur);
    }

    public boolean peutModifierTraitement() {
        return "PSYCHOLOGUE".equals(roleUtilisateur);
    }

    public boolean peutSupprimerTraitement() {
        return "PSYCHOLOGUE".equals(roleUtilisateur);
    }

    public boolean peutVoirTousTraitements() {
        return "PSYCHOLOGUE".equals(roleUtilisateur);
    }

    public boolean peutVoirSesTraitements() {
        return true; // Tout le monde peut voir ses traitements
    }

    // Permissions pour les suivis
    public boolean peutCreerSuivi() {
        return true; // Psychologue et étudiant peuvent créer des suivis
    }

    public boolean peutModifierSuivi() {
        return true; // Psychologue et étudiant peuvent modifier des suivis
    }

    public boolean peutSupprimerSuivi() {
        return true; // Psychologue et étudiant peuvent supprimer des suivis
    }

    // Obtenir le rôle de saisie pour les suivis
    public SaisiPar getRoleSaisiPar() {
        if ("PSYCHOLOGUE".equals(roleUtilisateur)) {
            return SaisiPar.PSYCHOLOGUE;
        } else {
            return SaisiPar.ETUDIANT;
        }
    }

    // Filtrer les traitements selon l'utilisateur
    public boolean peutVoirTraitement(int traitementEtudiantId, int traitementPsychologueId) {
        if ("PSYCHOLOGUE".equals(roleUtilisateur)) {
            // Le psychologue voit uniquement ses propres traitements
            return traitementPsychologueId == utilisateurConnecteId;
        } else {
            // L'étudiant voit seulement ses traitements
            return traitementEtudiantId == utilisateurConnecteId;
        }
    }

    // Filtrer les suivis selon l'utilisateur
    public boolean peutVoirSuivi(int suiviTraitementId, int suiviEtudiantId, int suiviSaisiParId, int traitementPsychologueId) {
        if ("PSYCHOLOGUE".equals(roleUtilisateur)) {
            // Le psychologue voit :
            // 1. Ses propres suivis (saisiPar = psychologueId)
            // 2. Les suivis de ses étudiants (uniquement pour ses traitements)
            return (suiviSaisiParId == utilisateurConnecteId) ||
                    peutVoirTraitement(suiviEtudiantId, traitementPsychologueId);
        } else {
            // L'étudiant voit :
            // 1. Ses propres suivis (étudiantId = étudiantId)
            // 2. Les suivis du psychologue pour ses traitements (saisiPar = psychologue ET étudiantId = étudiantId)
            return (suiviEtudiantId == utilisateurConnecteId) ||
                    (suiviEtudiantId == utilisateurConnecteId && suiviSaisiParId != utilisateurConnecteId);
        }
    }

    // Méthode utilitaire pour vérifier si un suivi appartient à l'étudiant connecté
    public boolean estSuiviDeEtudiantConnecte(int suiviEtudiantId) {
        return "ETUDIANT".equals(roleUtilisateur) && suiviEtudiantId == utilisateurConnecteId;
    }

    // Méthode utilitaire pour vérifier si un suivi a été créé par le psychologue connecté
    public boolean estSuiviDuPsychologueConnecte(int suiviSaisiParId) {
        return "PSYCHOLOGUE".equals(roleUtilisateur) && suiviSaisiParId == utilisateurConnecteId;
    }

    // Méthodes pour simuler la connexion (pour développement)
    public void connecterCommePsychologue() {
        setUtilisateurConnecte(1, "PSYCHOLOGUE", "psy psy");
    }

    public void connecterCommeEtudiant() {
        setUtilisateurConnecte(3, "ETUDIANT", "etudiant etudiant");
    }

    public boolean estPsychologue() {
        return "PSYCHOLOGUE".equals(roleUtilisateur);
    }

    public boolean estEtudiant() {
        return "ETUDIANT".equals(roleUtilisateur);
    }
}
