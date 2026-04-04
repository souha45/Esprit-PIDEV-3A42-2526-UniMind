package org.example.entities;

import org.example.enums.Role;
import java.sql.Timestamp;

public class Psychologue extends User {
    // Attributs spécifiques aux psychologues
    private String specialite;// Psychologue clinicien, scolaire, etc.
    private String adresse;
    private String telephone;

    // Constructeur par défaut
    public Psychologue() {
        super();
        this.role = Role.PSYCHOLOGUE;
    }

    // Constructeur pour création
    public Psychologue(String nom, String prenom, String email, String password,
                       String telephone, String adresse, String cin,
                       String specialite, String statut) {
        super(nom, prenom, email, password, cin, Role.PSYCHOLOGUE, statut);
        this.specialite = specialite;
        this.adresse = adresse;
        this.telephone = telephone;
    }

    // Constructeur complet avec ID
    public Psychologue(int userId, String nom, String prenom, String email, String password,
                       String telephone, String adresse, String cin, boolean isActive,
                       Timestamp createdAt, boolean isVerified,
                       String specialite, String statut) {
        super(nom, prenom, email, password, cin, Role.PSYCHOLOGUE, statut);
        this.userId = userId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.isVerified = isVerified;
        this.specialite = specialite;
        this.adresse = adresse;
        this.telephone = telephone;

    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    @Override
    public String toString() {
        return "Psychologue{" +
                "specialite='" + specialite + '\'' +
                ", userId=" + userId +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", telephone='" + telephone + '\'' +
                ", adresse='" + adresse + '\'' +
                ", cin='" + cin + '\'' +
                ", role=" + role +
                '}';
    }
}
