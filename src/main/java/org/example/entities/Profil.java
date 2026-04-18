package org.example.entities;

import java.sql.Timestamp;

public class Profil {

    private int profilId;
    private int userId;          // clé étrangère vers user
    private String photo;
    private String bio;
    private String tel;
    private Timestamp dateNaissance;
    private String specialite;
    private String experience;
    private String qualification;
    private String niveau;
    private String filiere;
    private String departement;
    private String etablissement;
    private String fonction;
    private Timestamp updatedAt;
    private String pseudo;

    // Constructeur par défaut
    public Profil() {
    }

    // Constructeur sans ID (pour la création)
    public Profil(int userId, String photo, String bio, String tel,
                  Timestamp dateNaissance, String specialite, String experience,
                  String qualification, String niveau, String filiere,
                  String departement, String etablissement, String fonction,
                  String pseudo) {
        this.userId = userId;
        this.photo = photo;
        this.bio = bio;
        this.tel = tel;
        this.dateNaissance = dateNaissance;
        this.specialite = specialite;
        this.experience = experience;
        this.qualification = qualification;
        this.niveau = niveau;
        this.filiere = filiere;
        this.departement = departement;
        this.etablissement = etablissement;
        this.fonction = fonction;
        this.pseudo = pseudo;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters & Setters
    public int getProfilId() {
        return profilId;
    }

    public void setProfilId(int profilId) {
        this.profilId = profilId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public Timestamp getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(Timestamp dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public String getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(String etablissement) {
        this.etablissement = etablissement;
    }

    public String getFonction() {
        return fonction;
    }

    public void setFonction(String fonction) {
        this.fonction = fonction;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    @Override
    public String toString() {
        return "Profil{" +
                "profilId=" + profilId +
                ", userId=" + userId +
                ", photo='" + photo + '\'' +
                ", bio='" + bio + '\'' +
                ", tel='" + tel + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", specialite='" + specialite + '\'' +
                ", niveau='" + niveau + '\'' +
                ", filiere='" + filiere + '\'' +
                ", departement='" + departement + '\'' +
                ", etablissement='" + etablissement + '\'' +
                ", fonction='" + fonction + '\'' +
                ", pseudo='" + pseudo + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}