package org.example.entities;

import java.sql.Timestamp;

public class Commentaire {
    private int commentaireId;
    private String contenu;
    private boolean isAnonyme;    // tinyint(4) -> boolean
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int userId;
    private int postId;

    // Constructeur par défaut
    public Commentaire() {
    }

    // Constructeur pour création (sans ID)
    public Commentaire(String contenu, boolean isAnonyme, int userId, int postId) {
        this.contenu = contenu;
        this.isAnonyme = isAnonyme;
        this.userId = userId;
        this.postId = postId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public Commentaire(int commentaireId, String contenu, boolean isAnonyme,
                       Timestamp createdAt, Timestamp updatedAt, int userId, int postId) {
        this.commentaireId = commentaireId;
        this.contenu = contenu;
        this.isAnonyme = isAnonyme;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.postId = postId;
    }

    // Getters et Setters
    public int getCommentaireId() {
        return commentaireId;
    }

    public void setCommentaireId(int commentaireId) {
        this.commentaireId = commentaireId;
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

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    /**
     * Retourne le nom de l'auteur (anonyme ou non)
     * @param nomAuteur Le nom de l'auteur (à passer depuis le service)
     * @return "Anonyme" si le commentaire est anonyme, sinon le nom
     */
    public String getNomAuteur(String nomAuteur) {
        return isAnonyme ? "Anonyme" : nomAuteur;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "commentaireId=" + commentaireId +
                ", contenu='" + (contenu != null && contenu.length() > 50 ? contenu.substring(0, 50) + "..." : contenu) + '\'' +
                ", isAnonyme=" + isAnonyme +
                ", userId=" + (isAnonyme ? "anonyme" : userId) +
                ", postId=" + postId +
                '}';
    }
}