package org.example.entities;

import org.example.enums.Role;
import java.sql.Timestamp;

public class Admin extends User {


    // Constructeur par défaut
    public Admin() {
        super();
        this.role = Role.ADMIN;
    }

    // Constructeur pour création
    public Admin(String nom, String prenom, String email, String password, String cin,String statut) {
        super(nom, prenom, email, password, cin, Role.ADMIN, statut);

    }

    // Constructeur complet avec ID
    public Admin(int userId, String nom, String prenom, String email, String password, String cin, boolean isActive,
                 Timestamp createdAt, boolean isVerified, String statut) {
        super(nom, prenom, email, password, cin, Role.ADMIN, statut);
        this.userId = userId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.isVerified = isVerified;
    }



    @Override
    public String toString() {
        return "Admin{" +
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