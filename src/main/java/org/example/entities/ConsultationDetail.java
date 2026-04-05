package org.example.entities;

import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;

public class ConsultationDetail {
    private int consultationId;
    private String avisPsy;
    private short noteSatisfaction;
    private Timestamp dateRedaction;
    private Timestamp dateModification;

    // Infos du psychologue
    private String psyNom;
    private String psyPrenom;
    private String psyEmail;

    // Infos du rendez-vous
    private Date dateDispo;
    private Time heureDebut;
    private Time heureFin;

    // Constructeur
    public ConsultationDetail(int consultationId, String avisPsy, short noteSatisfaction,
                              Timestamp dateRedaction, Timestamp dateModification,
                              String psyNom, String psyPrenom, String psyEmail,
                              Date dateDispo, Time heureDebut, Time heureFin) {
        this.consultationId = consultationId;
        this.avisPsy = avisPsy;
        this.noteSatisfaction = noteSatisfaction;
        this.dateRedaction = dateRedaction;
        this.dateModification = dateModification;
        this.psyNom = psyNom;
        this.psyPrenom = psyPrenom;
        this.psyEmail = psyEmail;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    // Getters
    public int getConsultationId() {
        return consultationId;
    }

    public String getAvisPsy() {
        return avisPsy;
    }

    public short getNoteSatisfaction() {
        return noteSatisfaction;
    }

    public Timestamp getDateRedaction() {
        return dateRedaction;
    }

    public Timestamp getDateModification() {
        return dateModification;
    }

    public String getPsyNom() {
        return psyNom;
    }

    public String getPsyPrenom() {
        return psyPrenom;
    }

    public String getPsyEmail() {
        return psyEmail;
    }

    public String getPsychologueNomComplet() {
        return psyPrenom + " " + psyNom;
    }

    public Date getDateDispo() {
        return dateDispo;
    }

    public Time getHeureDebut() {
        return heureDebut;
    }

    public Time getHeureFin() {
        return heureFin;
    }

    public String getHeurePlage() {
        return heureDebut + " - " + heureFin;
    }

    public String getNoteFormatted() {
        return noteSatisfaction > 0 ? noteSatisfaction + "/5" : "Non renseignée";
    }

    public String getAvisFormatted() {
        return avisPsy != null ? avisPsy : "Avis non renseigné";
    }

    @Override
    public String toString() {
        return "ConsultationDetail{" +
                "consultationId=" + consultationId +
                ", dateDispo=" + dateDispo +
                ", heureDebut=" + heureDebut +
                ", heureFin=" + heureFin +
                ", psychologue='" + getPsychologueNomComplet() + '\'' +
                ", note=" + noteSatisfaction +
                ", avis='" + (avisPsy != null && avisPsy.length() > 30 ? avisPsy.substring(0, 30) + "..." : avisPsy) + '\'' +
                '}';
    }
}