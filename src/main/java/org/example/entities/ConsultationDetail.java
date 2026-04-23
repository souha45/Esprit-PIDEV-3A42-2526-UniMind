package org.example.entities;

import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;

/**
 * Classe pour les détails d'une consultation
 * Utilisable côté étudiant ET côté psychologue
 */
public class ConsultationDetail {

    // ========== CHAMPS COMMUNS ==========
    private int consultationId;
    private String avisPsy;
    private short noteSatisfaction;
    private Timestamp dateRedaction;
    private Timestamp dateModification;

    // Infos du rendez-vous
    private Date dateDispo;
    private Time heureDebut;
    private Time heureFin;

    // ========== POUR L'ÉTUDIANT (infos psychologue) ==========
    private String psyNom;
    private String psyPrenom;
    private String psyEmail;

    // ========== POUR LE PSYCHOLOGUE (infos étudiant) ==========
    private String etudiantNom;
    private String etudiantPrenom;
    private String etudiantEmail;
    private int etudiantId;
    private int rendezVousId;

    // ========== CONSTRUCTEURS ==========

    // Constructeur pour l'ÉTUDIANT (avec infos psychologue)
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

    // Constructeur pour le PSYCHOLOGUE (avec infos étudiant)
    public ConsultationDetail(int consultationId, String avisPsy, short noteSatisfaction,
                              Timestamp dateRedaction, Timestamp dateModification,
                              String etudiantNom, String etudiantPrenom, String etudiantEmail,
                              int etudiantId, int rendezVousId,
                              Date dateDispo, Time heureDebut, Time heureFin) {
        this.consultationId = consultationId;
        this.avisPsy = avisPsy;
        this.noteSatisfaction = noteSatisfaction;
        this.dateRedaction = dateRedaction;
        this.dateModification = dateModification;
        this.etudiantNom = etudiantNom;
        this.etudiantPrenom = etudiantPrenom;
        this.etudiantEmail = etudiantEmail;
        this.etudiantId = etudiantId;
        this.rendezVousId = rendezVousId;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    // ========== GETTERS ==========

    // Getters communs
    public int getConsultationId() { return consultationId; }
    public String getAvisPsy() { return avisPsy; }
    public short getNoteSatisfaction() { return noteSatisfaction; }
    public Timestamp getDateRedaction() { return dateRedaction; }
    public Timestamp getDateModification() { return dateModification; }
    public Date getDateDispo() { return dateDispo; }
    public Time getHeureDebut() { return heureDebut; }
    public Time getHeureFin() { return heureFin; }

    // Getters pour l'ÉTUDIANT (infos psychologue)
    public String getPsyNom() { return psyNom; }
    public String getPsyPrenom() { return psyPrenom; }
    public String getPsyEmail() { return psyEmail; }

    public String getPsychologueNomComplet() {
        if (psyPrenom == null || psyNom == null) return "";
        return psyPrenom + " " + psyNom;
    }

    // Getters pour le PSYCHOLOGUE (infos étudiant)
    public String getEtudiantNom() { return etudiantNom; }
    public String getEtudiantPrenom() { return etudiantPrenom; }
    public String getEtudiantEmail() { return etudiantEmail; }
    public int getEtudiantId() { return etudiantId; }
    public int getRendezVousId() { return rendezVousId; }

    public String getEtudiantNomComplet() {
        if (etudiantPrenom == null || etudiantNom == null) return "";
        return etudiantPrenom + " " + etudiantNom;
    }

    // ========== SETTERS ==========
    public void setConsultationId(int consultationId) { this.consultationId = consultationId; }
    public void setAvisPsy(String avisPsy) { this.avisPsy = avisPsy; }
    public void setNoteSatisfaction(short noteSatisfaction) { this.noteSatisfaction = noteSatisfaction; }
    public void setDateRedaction(Timestamp dateRedaction) { this.dateRedaction = dateRedaction; }
    public void setDateModification(Timestamp dateModification) { this.dateModification = dateModification; }
    public void setDateDispo(Date dateDispo) { this.dateDispo = dateDispo; }
    public void setHeureDebut(Time heureDebut) { this.heureDebut = heureDebut; }
    public void setHeureFin(Time heureFin) { this.heureFin = heureFin; }
    public void setPsyNom(String psyNom) { this.psyNom = psyNom; }
    public void setPsyPrenom(String psyPrenom) { this.psyPrenom = psyPrenom; }
    public void setPsyEmail(String psyEmail) { this.psyEmail = psyEmail; }
    public void setEtudiantNom(String etudiantNom) { this.etudiantNom = etudiantNom; }
    public void setEtudiantPrenom(String etudiantPrenom) { this.etudiantPrenom = etudiantPrenom; }
    public void setEtudiantEmail(String etudiantEmail) { this.etudiantEmail = etudiantEmail; }
    public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }
    public void setRendezVousId(int rendezVousId) { this.rendezVousId = rendezVousId; }

    // ========== MÉTHODES UTILITAIRES ==========

    public String getHeurePlage() {
        return heureDebut + " - " + heureFin;
    }

    public String getNoteFormatted() {
        return noteSatisfaction > 0 ? noteSatisfaction + "/5" : "À NOTER";
    }

    public String getAvisFormatted() {
        if (avisPsy == null || avisPsy.isEmpty()) {
            return "Aucun avis";
        }
        return avisPsy;
    }

    @Override
    public String toString() {
        return "ConsultationDetail{" +
                "consultationId=" + consultationId +
                ", dateDispo=" + dateDispo +
                ", heureDebut=" + heureDebut +
                ", note=" + noteSatisfaction +
                '}';
    }
}