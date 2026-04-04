package org.example.entities;

import java.sql.Timestamp;

public class Post {
    private int postId;
    private String titre;
    private String contenu;
    private boolean isAnonyme;    // tinyint(4) -> boolean
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int userId;
    private int categorieId;   // Peut être NULL

    // Constructeur par défaut
    public Post() {
    }

    // Constructeur pour création (sans ID)
    public Post(String titre, String contenu, boolean isAnonyme, int userId, int categorieId) {
        this.titre = titre;
        this.contenu = contenu;
        this.isAnonyme = isAnonyme;
        this.userId = userId;
        this.categorieId = categorieId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public Post(int postId, String titre, String contenu, boolean isAnonyme,
                Timestamp createdAt, Timestamp updatedAt, int userId, int categorieId) {
        this.postId = postId;
        this.titre = titre;
        this.contenu = contenu;
        this.isAnonyme = isAnonyme;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.categorieId = categorieId;
    }

    // Getters et Setters
    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public boolean isIsAnonyme() {
        return isAnonyme;
    }

    public void setIsAnonyme(boolean isAnonyme) {
        this.isAnonyme = isAnonyme;
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

    public int getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(int categorieId) {
        this.categorieId = categorieId;
    }

    @Override
    public String toString() {
        return "Post{" +
                "postId=" + postId +
                ", titre='" + titre + '\'' +
                ", isAnonyme=" + isAnonyme +
                ", userId=" + (isAnonyme ? "anonyme" : userId) +
                ", categorieId=" + categorieId +
                ", createdAt=" + createdAt +
                '}';
    }
}