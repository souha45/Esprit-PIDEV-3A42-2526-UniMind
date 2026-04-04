package org.example.entities;

import org.example.enums.Role;
import java.sql.Timestamp;

public class ResponsableEtudiant extends User {
    // Attributs spécifiques aux responsables étudiants
    private String poste;           // Chef de département, Coordinateur, etc.
    private String etablissement;

    // Constructeur par défaut
    public ResponsableEtudiant() {
        super();
        this.role = Role.RESPONSABLE_ETUDIANT;
    }

    // Constructeur pour création
    public ResponsableEtudiant(String nom, String prenom, String email, String password, String cin,
                               String poste, String etablissement, String statut ) {
        super(nom, prenom, email, password, cin, Role.RESPONSABLE_ETUDIANT, statut);
        this.poste = poste;
        this.etablissement = etablissement;
    }

    // Constructeur complet avec ID
    public ResponsableEtudiant(int userId, String nom, String prenom, String email, String password, String cin, boolean isActive,
                               Timestamp createdAt, boolean isVerified, String etablissement,
                               String poste, String statut) {
        super(nom, prenom, email, password, cin, Role.RESPONSABLE_ETUDIANT, statut);
        this.userId = userId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.isVerified = isVerified;
        this.poste = poste;
        this.etablissement = etablissement;

    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }

    public String getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(String etablissement) {
        this.etablissement = etablissement;
    }

    @Override
    public String toString() {
        return "Responsable_Etudiant{" +
                "userId=" + userId +
                ", etablissement='" + etablissement + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", cin='" + cin + '\'' +
                ", role=" + role +
                ", poste='" + poste + '\'' +
                '}';
    }
}
