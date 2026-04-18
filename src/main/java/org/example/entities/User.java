package org.example.entities;

import org.example.enums.Role;
import java.sql.Timestamp;

public class User {
    // Attributs communs à tous les utilisateurs
    protected int userId;
    protected String nom;
    protected String prenom;
    protected String statut;
    protected String email;
    protected String password;
    protected String cin;
    protected Role role;
    protected boolean isActive;
    protected Timestamp createdAt;


    // Attributs de vérification (communs à tous)
    protected boolean isVerified;
    protected String verificationToken;
    protected Timestamp tokenExpiresAt;
    protected String resetToken;
    protected Timestamp resetTokenExpiresAt;

    // Constructeur par défaut
    public User() {
    }

    // Constructeur pour les attributs communs (sans ID)
    public User(String nom, String prenom, String email, String password, String cin, Role role, String statut) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.cin = cin;
        this.role = role;
        this.statut = statut;
        this.isActive = true;
        this.isVerified = false;
        this.createdAt = new Timestamp(System.currentTimeMillis());

    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public Timestamp getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Timestamp tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Timestamp getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(Timestamp resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", cin='" + cin + '\'' +
                ", role=" + role +
                '}';
    }
}