package org.example.entities;

import org.example.enums.StatutDisponibilite;
import org.example.enums.TypeConsultation;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class DisponibilitePsy {

    private int dispoId;
    private Date dateDispo;
    private Time heureDebut;
    private Time heureFin;
    private TypeConsultation typeConsult;
    private String lieu;
    private StatutDisponibilite statut;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Clé étrangère vers Psychologue (table user)
    // L'objet Psychologue sera rempli par le DAO via jointure SQL si besoin
    private int userId;
    //private Psychologue psychologue;

    // Constructeur par défaut
    public DisponibilitePsy() {
    }

    // Constructeur sans ID (pour la création)
    public DisponibilitePsy(int userId, Date dateDispo, Time heureDebut, Time heureFin, TypeConsultation typeConsult, String lieu) {
        this.userId = userId;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeConsult = typeConsult;
        this.lieu = lieu;
        this.statut = StatutDisponibilite.disponible;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }


    // Ajoute ce constructeur dans ta classe DisponibilitePsy
    public DisponibilitePsy(int dispoId, int userId, Date dateDispo, Time heureDebut,
                            Time heureFin, TypeConsultation typeConsult, String lieu, StatutDisponibilite statut) {
        this.dispoId = dispoId;
        this.userId = userId;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeConsult = typeConsult;
        this.lieu = lieu;
        this.statut = statut;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters & Setters
    public int getDispoId() {
        return dispoId;
    }

    public void setDispoId(int dispoId) {
        this.dispoId = dispoId;
    }

    public Date getDateDispo() {
        return dateDispo;
    }

    public void setDateDispo(Date dateDispo) {
        this.dateDispo = dateDispo;
    }

    public Time getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(Time heureDebut) {
        this.heureDebut = heureDebut;
    }

    public Time getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(Time heureFin) {
        this.heureFin = heureFin;
    }

    public TypeConsultation getTypeConsult() {
        return typeConsult;
    }

    public void setTypeConsult(TypeConsultation typeConsult) {
        this.typeConsult = typeConsult;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public StatutDisponibilite getStatut() {
        return statut;
    }

    public void setStatut(StatutDisponibilite statut) {
        this.statut = statut;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "DisponibilitePsy{" +
                "dispoId=" + dispoId +
                ", dateDispo=" + dateDispo +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", typeConsult=" + typeConsult +
                ", lieu='" + lieu + '\'' +
                ", statut=" + statut +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                '}';
    }
}