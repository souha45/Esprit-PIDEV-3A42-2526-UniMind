package org.example.entities;

import org.example.enums.NiveauMeditation;
import org.example.enums.TypeFichier;
import java.sql.Timestamp;

public class SeanceMeditation {
    private int seanceId;
    private String titre;
    private String description;
    private String fichier;
    private TypeFichier typeFichier;  // AUDIO, VIDEO, PDF
    private int duree;                 // durée en minutes ou secondes
    private boolean isActive;          // tinyint(4) -> boolean
    private NiveauMeditation niveau;   // DEBUTANT, INTERMEDIAIRE, AVANCE
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int categorieId;

    // Constructeur par défaut
    public SeanceMeditation() {
    }

    // Constructeur pour création (sans ID)
    public SeanceMeditation(String titre, String description, String fichier,
                            TypeFichier typeFichier, int duree, boolean isActive,
                            NiveauMeditation niveau, int categorieId) {
        this.titre = titre;
        this.description = description;
        this.fichier = fichier;
        this.typeFichier = typeFichier;
        this.duree = duree;
        this.isActive = isActive;
        this.niveau = niveau;
        this.categorieId = categorieId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public SeanceMeditation(int seanceId, String titre, String description, String fichier,
                            TypeFichier typeFichier, int duree, boolean isActive, NiveauMeditation niveau,
                            Timestamp createdAt, Timestamp updatedAt, int categorieId) {
        this.seanceId = seanceId;
        this.titre = titre;
        this.description = description;
        this.fichier = fichier;
        this.typeFichier = typeFichier;
        this.duree = duree;
        this.isActive = isActive;
        this.niveau = niveau;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.categorieId = categorieId;
    }

    // Getters et Setters
    public int getSeanceId() {
        return seanceId;
    }

    public void setSeanceId(int seanceId) {
        this.seanceId = seanceId;
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

    public String getFichier() {
        return fichier;
    }

    public void setFichier(String fichier) {
        this.fichier = fichier;
    }

    public TypeFichier getTypeFichier() {
        return typeFichier;
    }

    public void setTypeFichier(TypeFichier typeFichier) {
        this.typeFichier = typeFichier;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public boolean isIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public NiveauMeditation getNiveau() {
        return niveau;
    }

    public void setNiveau(NiveauMeditation niveau) {
        this.niveau = niveau;
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

    public int getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(int categorieId) {
        this.categorieId = categorieId;
    }

    // Méthodes utilitaires
    public String getTypeFichierLabel() {
        switch (typeFichier) {
            case AUDIO:
                return "Audio";
            case VIDEO:
                return "Vidéo";
            case PDF:
                return "PDF";
            default:
                return typeFichier.toString();
        }
    }

    public String getNiveauLabel() {
        switch (niveau) {
            case DEBUTANT:
                return "Débutant";
            case INTERMEDIAIRE:
                return "Intermédiaire";
            case AVANCE:
                return "Avancé";
            default:
                return niveau.toString();
        }
    }

    @Override
    public String toString() {
        return "SeanceMeditation{" +
                "seanceId=" + seanceId +
                ", titre='" + titre + '\'' +
                ", typeFichier=" + typeFichier +
                ", duree=" + duree +
                ", isActive=" + isActive +
                ", niveau=" + niveau +
                ", categorieId=" + categorieId +
                '}';
    }
}