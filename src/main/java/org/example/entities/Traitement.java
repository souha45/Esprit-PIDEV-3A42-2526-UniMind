package org.example.entities;

import org.example.enums.CategorieTraitement;
import org.example.enums.PrioriteTraitement;
import org.example.enums.StatutTraitement;
import java.sql.Date;
import java.sql.Timestamp;

public class Traitement {
    private int traitementId;
    private String titre;
    private String description;
    private String type;
    private CategorieTraitement categorie;
    private int dureeJours;
    private String dosage;
    private Date dateDebut;
    private Date dateFin;
    private StatutTraitement statut;
    private PrioriteTraitement priorite;
    private String objectifTherapeutique;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer psychologueId;  // Peut être NULL
    private int etudiantId;

    // Constructeur par défaut
    public Traitement() {
    }

    // Constructeur pour création (sans ID)
    public Traitement(String titre, String description, String type, CategorieTraitement categorie,
                      int dureeJours, String dosage, Date dateDebut, Date dateFin,
                      StatutTraitement statut, PrioriteTraitement priorite,
                      String objectifTherapeutique, Integer psychologueId, int etudiantId) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
        this.dureeJours = dureeJours;
        this.dosage = dosage;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.priorite = priorite;
        this.objectifTherapeutique = objectifTherapeutique;
        this.psychologueId = psychologueId;
        this.etudiantId = etudiantId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur simplifié
    public Traitement(String titre, String description, String type, CategorieTraitement categorie,
                      int dureeJours, Date dateDebut, PrioriteTraitement priorite,
                      String objectifTherapeutique, int etudiantId) {
        this(titre, description, type, categorie, dureeJours, null, dateDebut, null,
                StatutTraitement.EN_COURS, priorite, objectifTherapeutique, null, etudiantId);
    }

    // Constructeur complet avec ID
    public Traitement(int traitementId, String titre, String description, String type,
                      CategorieTraitement categorie, int dureeJours, String dosage,
                      Date dateDebut, Date dateFin, StatutTraitement statut,
                      PrioriteTraitement priorite, String objectifTherapeutique,
                      Timestamp createdAt, Timestamp updatedAt, Integer psychologueId, int etudiantId) {
        this.traitementId = traitementId;
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
        this.dureeJours = dureeJours;
        this.dosage = dosage;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.priorite = priorite;
        this.objectifTherapeutique = objectifTherapeutique;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.psychologueId = psychologueId;
        this.etudiantId = etudiantId;
    }

    // Getters et Setters
    public int getTraitementId() {
        return traitementId;
    }

    public void setTraitementId(int traitementId) {
        this.traitementId = traitementId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CategorieTraitement getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieTraitement categorie) {
        this.categorie = categorie;
    }

    public int getDureeJours() {
        return dureeJours;
    }

    public void setDureeJours(int dureeJours) {
        this.dureeJours = dureeJours;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public StatutTraitement getStatut() {
        return statut;
    }

    public void setStatut(StatutTraitement statut) {
        this.statut = statut;
    }

    public PrioriteTraitement getPriorite() {
        return priorite;
    }

    public void setPriorite(PrioriteTraitement priorite) {
        this.priorite = priorite;
    }

    public String getObjectifTherapeutique() {
        return objectifTherapeutique;
    }

    public void setObjectifTherapeutique(String objectifTherapeutique) {
        this.objectifTherapeutique = objectifTherapeutique;
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

    public Integer getPsychologueId() {
        return psychologueId;
    }

    public void setPsychologueId(Integer psychologueId) {
        this.psychologueId = psychologueId;
    }

    public int getEtudiantId() {
        return etudiantId;
    }

    public void setEtudiantId(int etudiantId) {
        this.etudiantId = etudiantId;
    }

    // Méthodes utilitaires
    public boolean isEnCours() {
        return statut == StatutTraitement.EN_COURS;
    }

    public boolean isTermine() {
        return statut == StatutTraitement.TERMINE;
    }

    public boolean isSuspendu() {
        return statut == StatutTraitement.SUSPENDU;
    }

    public boolean isConfirme() {
        return priorite == PrioriteTraitement.CONFIRME;
    }

    public boolean isEnAttente() {
        return priorite == PrioriteTraitement.ATTENTE;
    }

    public boolean isAnnule() {
        return priorite == PrioriteTraitement.ANNULE;
    }

    public String getCategorieLabel() {
        switch (categorie) {
            case RELAXATION:
                return "Relaxation";
            case COGNITIF:
                return "Cognitif";
            case EMOTIONNEL:
                return "Émotionnel";
            case COMPORTEMENTAL:
                return "Comportemental";
            default:
                return categorie.toString();
        }
    }

    public String getStatutLabel() {
        switch (statut) {
            case EN_COURS:
                return "En cours";
            case TERMINE:
                return "Terminé";
            case SUSPENDU:
                return "Suspendu";
            default:
                return statut.toString();
        }
    }

    public String getPrioriteLabel() {
        switch (priorite) {
            case CONFIRME:
                return "Confirmé";
            case ATTENTE:
                return "En attente";
            case ANNULE:
                return "Annulé";
            default:
                return priorite.toString();
        }
    }

    public long getJoursRestants() {
        if (dateFin == null) return 0;
        long jours = (dateFin.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
        return Math.max(0, jours);
    }

    @Override
    public String toString() {
        return "Traitement{" +
                "traitementId=" + traitementId +
                ", titre='" + titre + '\'' +
                ", categorie=" + categorie +
                ", statut=" + statut +
                ", priorite=" + priorite +
                ", etudiantId=" + etudiantId +
                '}';
    }
}