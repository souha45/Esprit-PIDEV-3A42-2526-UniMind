package org.example.entities;

import org.example.enums.StatutRendezVous;

import java.sql.Timestamp;

public class RendezVous {

    private int rendezVousId;
    private String motif;
    private StatutRendezVous statut;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Clés étrangères
    private int dispoId;
    private int etudiantId;
    private int psyId;

    // Objets liés — remplis par le DAO via jointure SQL si besoin
    //private DisponibilitePsy disponibilite;
    //private Etudiant etudiant;
    //private Psychologue psychologue;

    // Constructeur par défaut
    public RendezVous() {
    }

    // Constructeur sans ID (pour la création)
    public RendezVous(int dispoId, int etudiantId, int psyId, String motif) {
        this.dispoId = dispoId;
        this.etudiantId = etudiantId;
        this.psyId = psyId;
        this.motif = motif;
        this.statut = StatutRendezVous.demande;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    //Constructeur avec ID (pour la modification)
    public RendezVous(int rendezVousId, int dispoId, int etudiantId, int psyId, String motif) {
        this.rendezVousId = rendezVousId;
        this.dispoId = dispoId;
        this.etudiantId = etudiantId;
        this.psyId = psyId;
        this.motif = motif;
        this.statut = StatutRendezVous.demande;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters & Setters
    public int getRendezVousId() {
        return rendezVousId;
    }

    public void setRendezVousId(int rendezVousId) {
        this.rendezVousId = rendezVousId;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public StatutRendezVous getStatut() {
        return statut;
    }

    public void setStatut(StatutRendezVous statut) {
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

    public int getDispoId() {
        return dispoId;
    }

    public void setDispoId(int dispoId) {
        this.dispoId = dispoId;
    }

    public int getEtudiantId() {
        return etudiantId;
    }

    public void setEtudiantId(int etudiantId) {
        this.etudiantId = etudiantId;
    }

    public int getPsyId() {
        return psyId;
    }

    public void setPsyId(int psyId) {
        this.psyId = psyId;
    }

    @Override
    public String toString() {
        return "RendezVous{" +
                "rendezVousId=" + rendezVousId +
                ", motif='" + motif + '\'' +
                ", statut=" + statut +
                ", dispoId=" + dispoId +
                ", etudiantId=" + etudiantId +
                ", psyId=" + psyId +
                ", createdAt=" + createdAt +
                '}';
    }
}