package org.example.models;

import java.sql.Timestamp;

public class Favori {
    private int id;
    private Timestamp createdAt;
    private int evenementId;
    private int etudiantId;

    // Constructeur par défaut
    public Favori() {
    }

    // Constructeur pour création (sans ID)
    public Favori(int evenementId, int etudiantId) {
        this.evenementId = evenementId;
        this.etudiantId = etudiantId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public Favori(int id, Timestamp createdAt, int evenementId, int etudiantId) {
        this.id = id;
        this.createdAt = createdAt;
        this.evenementId = evenementId;
        this.etudiantId = etudiantId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
    }

    public int getEtudiantId() {
        return etudiantId;
    }

    public void setEtudiantId(int etudiantId) {
        this.etudiantId = etudiantId;
    }

    @Override
    public String toString() {
        return "Favori{" +
                "id=" + id +
                ", evenementId=" + evenementId +
                ", etudiantId=" + etudiantId +
                ", createdAt=" + createdAt +
                '}';
    }
}