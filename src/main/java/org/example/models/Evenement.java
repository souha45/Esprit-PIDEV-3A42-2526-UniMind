package org.example.models;

import org.example.enums.StatutEvenement;
import org.example.enums.TypeEvenement;
import java.sql.Timestamp;

public class Evenement {
    private int evenementId;
    private String titre;
    private String description;
    private TypeEvenement type;
    private Timestamp dateDebut;
    private Timestamp dateFin;
    private String lieu;
    private int capaciteMax;
    private int nombreInscrits;
    private StatutEvenement statut;
    private Timestamp dateCreation;
    private Timestamp dateLimiteInscription;
    private int organisateurId;
    private Timestamp updatedAt;
    private String image;
    private Double latitude;
    private Double longitude;

    // Constructeur par défaut
    public Evenement() {
    }

    // Constructeur pour création (sans ID)
    public Evenement(String titre, String description, TypeEvenement type,
                     Timestamp dateDebut, Timestamp dateFin, String lieu,
                     int capaciteMax, int nombreInscrits, StatutEvenement statut,
                     Timestamp dateLimiteInscription, int organisateurId,
                     String image, Double latitude, Double longitude) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.capaciteMax = capaciteMax;
        this.nombreInscrits = nombreInscrits;
        this.statut = statut;
        this.dateLimiteInscription = dateLimiteInscription;
        this.organisateurId = organisateurId;
        this.image = image;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dateCreation = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public Evenement(int evenementId, String titre, String description, TypeEvenement type,
                     Timestamp dateDebut, Timestamp dateFin, String lieu,
                     int capaciteMax, int nombreInscrits, StatutEvenement statut,
                     Timestamp dateCreation, Timestamp dateLimiteInscription,
                     int organisateurId, Timestamp updatedAt, String image,
                     Double latitude, Double longitude) {
        this.evenementId = evenementId;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.capaciteMax = capaciteMax;
        this.nombreInscrits = nombreInscrits;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateLimiteInscription = dateLimiteInscription;
        this.organisateurId = organisateurId;
        this.updatedAt = updatedAt;
        this.image = image;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters et Setters
    public int getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TypeEvenement getType() {
        return type;
    }

    public void setType(TypeEvenement type) {
        this.type = type;
    }

    public Timestamp getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Timestamp dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Timestamp getDateFin() {
        return dateFin;
    }

    public void setDateFin(Timestamp dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public int getNombreInscrits() {
        return nombreInscrits;
    }

    public void setNombreInscrits(int nombreInscrits) {
        this.nombreInscrits = nombreInscrits;
    }

    public StatutEvenement getStatut() {
        return statut;
    }

    public void setStatut(StatutEvenement statut) {
        this.statut = statut;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Timestamp getDateLimiteInscription() {
        return dateLimiteInscription;
    }

    public void setDateLimiteInscription(Timestamp dateLimiteInscription) {
        this.dateLimiteInscription = dateLimiteInscription;
    }

    public int getOrganisateurId() {
        return organisateurId;
    }

    public void setOrganisateurId(int organisateurId) {
        this.organisateurId = organisateurId;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // Méthodes utilitaires
    public boolean isComplet() {
        return nombreInscrits >= capaciteMax;
    }

    public int getPlacesRestantes() {
        return capaciteMax - nombreInscrits;
    }

    public boolean isInscriptionPossible() {
        if (statut == StatutEvenement.ANNULE || statut == StatutEvenement.TERMINE) {
            return false;
        }
        if (dateLimiteInscription != null && dateLimiteInscription.before(new Timestamp(System.currentTimeMillis()))) {
            return false;
        }
        return !isComplet();
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "evenementId=" + evenementId +
                ", titre='" + titre + '\'' +
                ", type=" + type +
                ", statut=" + statut +
                ", dateDebut=" + dateDebut +
                ", lieu='" + lieu + '\'' +
                ", placesRestantes=" + getPlacesRestantes() +
                '}';
    }
}