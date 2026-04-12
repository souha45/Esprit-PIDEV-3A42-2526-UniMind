package org.example.models;

import java.sql.Timestamp;

public class FavoriSeance {
    private int id;
    private Timestamp createdAt;
    private int userId;
    private int seanceId;

    // Constructeur par défaut
    public FavoriSeance() {
    }

    // Constructeur pour création (sans ID)
    public FavoriSeance(int userId, int seanceId) {
        this.userId = userId;
        this.seanceId = seanceId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public FavoriSeance(int id, Timestamp createdAt, int userId, int seanceId) {
        this.id = id;
        this.createdAt = createdAt;
        this.userId = userId;
        this.seanceId = seanceId;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSeanceId() {
        return seanceId;
    }

    public void setSeanceId(int seanceId) {
        this.seanceId = seanceId;
    }

    @Override
    public String toString() {
        return "FavoriSeance{" +
                "id=" + id +
                ", userId=" + userId +
                ", seanceId=" + seanceId +
                ", createdAt=" + createdAt +
                '}';
    }
}