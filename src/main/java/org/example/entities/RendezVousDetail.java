package org.example.entities;

import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;

// Crée cette classe
public class RendezVousDetail {
    private int rendezVousId;
    private String statutRdv;
    private Timestamp createdAt;
    private Date dateDispo;
    private Time heureDebut;
    private Time heureFin;
    private String typeConsult;
    private String psyNom;
    private String psyPrenom;
    private String motif;


    // Constructeur
    public RendezVousDetail(int rendezVousId, String statutRdv, Timestamp createdAt,
                            Date dateDispo, Time heureDebut, Time heureFin,
                            String typeConsult, String psyNom, String psyPrenom) {
        this.rendezVousId = rendezVousId;
        this.statutRdv = statutRdv;
        this.createdAt = createdAt;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeConsult = typeConsult;
        this.psyNom = psyNom;
        this.psyPrenom = psyPrenom;

    }

    // Constructeur
    public RendezVousDetail(int rendezVousId, String statutRdv, Timestamp createdAt,
                            Date dateDispo, Time heureDebut, Time heureFin,
                            String typeConsult, String psyNom, String psyPrenom, String motif) {
        this.rendezVousId = rendezVousId;
        this.statutRdv = statutRdv;
        this.createdAt = createdAt;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeConsult = typeConsult;
        this.psyNom = psyNom;
        this.psyPrenom = psyPrenom;
        this.motif = motif;
    }

    public int getRendezVousId() {
        return rendezVousId;
    }

    public void setRendezVousId(int rendezVousId) {
        this.rendezVousId = rendezVousId;
    }

    public String getStatutRdv() {
        return statutRdv;
    }

    public void setStatutRdv(String statutRdv) {
        this.statutRdv = statutRdv;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp dateReservation) {
        this.createdAt = dateReservation;
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

    public String getTypeConsult() {
        return typeConsult;
    }

    public void setTypeConsult(String typeConsult) {
        this.typeConsult = typeConsult;
    }

    public String getPsyNom() {
        return psyNom;
    }

    public void setPsyNom(String psyNom) {
        this.psyNom = psyNom;
    }

    public String getPsyPrenom() {
        return psyPrenom;
    }

    public void setPsyPrenom(String psyPrenom) {
        this.psyPrenom = psyPrenom;
    }

    @Override
    public String toString() {
        return "RendezVousDetail{" +
                "rendezVousId=" + rendezVousId +
                ", statutRdv='" + statutRdv + '\'' +
                ", createdAt=" + createdAt +
                ", dateDispo=" + dateDispo +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", typeConsult='" + typeConsult + '\'' +
                ", psyNom='" + psyNom + '\'' +
                ", psyPrenom='" + psyPrenom + '\'' +
                ", motif='" + motif + '\'' +
                '}';
    }
}