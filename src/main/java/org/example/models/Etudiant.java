package org.example.models;

import org.example.enums.Role;

import java.sql.Timestamp;

public class Etudiant extends User {
    // Attributs spécifiques aux étudiants
    private String identifiant;      // Numéro étudiant
    private String nomEtablissement;

    // Constructeur par défaut
    public Etudiant() {
        super();
        this.role = Role.ETUDIANT;
    }

    // Constructeur avec tous les attributs (pour création)
    public Etudiant(String nom, String prenom, String email, String password, String cin,
                    String identifiant, String nomEtablissement, String statut) {
        super(nom, prenom, email, password, cin, Role.ETUDIANT, statut);
        this.identifiant = identifiant;
        this.nomEtablissement = nomEtablissement;
    }

    // Constructeur complet avec ID (pour récupération depuis DB)
    public Etudiant(int userId, String nom, String prenom, String email, String password, String cin, boolean isActive,
                    Timestamp createdAt, boolean isVerified,
                    String identifiant, String nomEtablissement, String statut) {
        super(nom, prenom, email, password, cin, Role.ETUDIANT, statut);
        this.userId = userId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.isVerified = isVerified;
        this.identifiant = identifiant;
        this.nomEtablissement = nomEtablissement;

    }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public String getNomEtablissement() {
        return nomEtablissement;
    }

    public void setNomEtablissement(String etablissement) {
        this.nomEtablissement = etablissement;
    }

    @Override
    public String toString() {
        return "Etudiant{" +
                "identifiant='" + identifiant + '\'' +
                "nom_etablissement='" + nomEtablissement + '\'' +
                ", userId=" + userId +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", cin='" + cin + '\'' +
                ", role=" + role +
                '}';
    }
}
