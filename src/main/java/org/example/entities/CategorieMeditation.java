package org.example.entities;

import java.sql.Timestamp;

public class CategorieMeditation {
    private int categorieId;
    private String nom;
    private String description;
    private Timestamp dateCreation;
    private Timestamp updatedAt;
    private String iconUrl;

    // Constructeur par défaut
    public CategorieMeditation() {
    }

    // Constructeur pour création (sans ID)
    public CategorieMeditation(String nom, String description, String iconUrl) {
        this.nom = nom;
        this.description = description;
        this.iconUrl = iconUrl;
        this.dateCreation = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public CategorieMeditation(int categorieId, String nom, String description,
                               Timestamp dateCreation, Timestamp updatedAt, String iconUrl) {
        this.categorieId = categorieId;
        this.nom = nom;
        this.description = description;
        this.dateCreation = dateCreation;
        this.updatedAt = updatedAt;
        this.iconUrl = iconUrl;
    }

    // Getters et Setters
    public int getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(int categorieId) {
        this.categorieId = categorieId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    @Override
    public String toString() {
        return "CategorieMeditation{" +
                "categorieId=" + categorieId +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                '}';
    }
}