package org.example.entities;

import java.sql.Timestamp;

public class Consultation {

    private int consultationId;
    private Timestamp dateRedaction;
    private Timestamp dateModification;
    private String avisPsy;
    private Short noteSatisfaction;

    // Clés étrangères
    private int rendezVousId;
    private int psyUserId;
    private int etudiantUserId;

    // Objets liés — remplis par le DAO via jointure SQL si besoin
    //private RendezVous rendezVous;
    //private Psychologue psychologue;
    //private Etudiant etudiant;

    // Constructeur par défaut
    public Consultation() {
    }

    // Constructeur sans ID (pour la création)
    public Consultation(int rendezVousId, int psyUserId, int etudiantUserId) {
        this.rendezVousId = rendezVousId;
        this.psyUserId = psyUserId;
        this.etudiantUserId = etudiantUserId;
        this.dateRedaction = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur avec ID (pour la modification)
    public Consultation(int consultationId, int rendezVousId, int psyUserId, int etudiantUserId, String avisPsy, short noteSatisfaction) {
        this.consultationId = consultationId;
        this.rendezVousId = rendezVousId;
        this.psyUserId = psyUserId;
        this.etudiantUserId = etudiantUserId;
        this.dateModification = new Timestamp(System.currentTimeMillis());
        this.avisPsy = avisPsy;
        this.noteSatisfaction = noteSatisfaction;
    }

    // Getters & Setters
    public int getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(int consultationId) {
        this.consultationId = consultationId;
    }

    public Timestamp getDateRedaction() {
        return dateRedaction;
    }

    public void setDateRedaction(Timestamp dateRedaction) {
        this.dateRedaction = dateRedaction;
    }

    public Timestamp getDateModification() {
        return dateModification;
    }

    public void setDateModification(Timestamp dateModification) {
        this.dateModification = dateModification;
    }

    public String getAvisPsy() {
        return avisPsy;
    }

    public void setAvisPsy(String avisPsy) {
        this.avisPsy = avisPsy;
    }

    public Short getNoteSatisfaction() {
        return noteSatisfaction;
    }

    public void setNoteSatisfaction(Short noteSatisfaction) {
        this.noteSatisfaction = noteSatisfaction;
    }

    public int getRendezVousId() {
        return rendezVousId;
    }

    public void setRendezVousId(int rendezVousId) {
        this.rendezVousId = rendezVousId;
    }

    public int getPsyUserId() {
        return psyUserId;
    }

    public void setPsyUserId(int psyUserId) {
        this.psyUserId = psyUserId;
    }

    public int getEtudiantUserId() {
        return etudiantUserId;
    }

    public void setEtudiantUserId(int etudiantUserId) {
        this.etudiantUserId = etudiantUserId;
    }


    @Override
    public String toString() {
        return "Consultation{" +
                "consultationId=" + consultationId +
                ", dateRedaction=" + dateRedaction +
                ", dateModification=" + dateModification +
                ", avisPsy='" + avisPsy + '\'' +
                ", noteSatisfaction=" + noteSatisfaction +
                ", rendezVousId=" + rendezVousId +
                ", psyUserId=" + psyUserId +
                ", etudiantUserId=" + etudiantUserId +
                '}';
    }
}