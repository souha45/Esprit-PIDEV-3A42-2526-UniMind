package org.example.models;

import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;

/**
 * Classe pour les détails d'un rendez-vous (utilisée pour l'affichage)
 * Utilisable côté étudiant ET côté psychologue
 */
public class RendezVousDetail {

    // ========== CHAMPS COMMUNS ==========
    private int rendezVousId;
    private String statutRdv;
    private Timestamp createdAt;
    private Date dateDispo;
    private Time heureDebut;
    private Time heureFin;
    private String typeConsult;
    private String motif;

    // ========== CHAMPS POUR LE PSYCHOLOGUE (infos étudiant) ==========
    private String etudiantNom;
    private String etudiantPrenom;
    private String etudiantEmail;
    private int etudiantId;
    private int dispoId;

    // ========== CHAMPS POUR L'ÉTUDIANT (infos psychologue) ==========
    private String psyNom;
    private String psyPrenom;
    private int psyId;

    // ========== CONSTRUCTEURS ==========

    // Constructeur pour l'ÉTUDIANT (sans motif)
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

    // Constructeur pour l'ÉTUDIANT (avec motif)
    public RendezVousDetail(int rendezVousId, String statutRdv, Timestamp createdAt,
                            Date dateDispo, Time heureDebut, Time heureFin,
                            String typeConsult, String psyNom, String psyPrenom, String motif) {
        this(rendezVousId, statutRdv, createdAt, dateDispo, heureDebut, heureFin,
                typeConsult, psyNom, psyPrenom);
        this.motif = motif;
    }

    // Constructeur pour le PSYCHOLOGUE (avec infos étudiant)
    public RendezVousDetail(int rendezVousId, String statutRdv, Timestamp createdAt,
                            Date dateDispo, Time heureDebut, Time heureFin,
                            String typeConsult, String etudiantNom, String etudiantPrenom,
                            String etudiantEmail, int etudiantId, int dispoId) {
        this.rendezVousId = rendezVousId;
        this.statutRdv = statutRdv;
        this.createdAt = createdAt;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeConsult = typeConsult;
        this.etudiantNom = etudiantNom;
        this.etudiantPrenom = etudiantPrenom;
        this.etudiantEmail = etudiantEmail;
        this.etudiantId = etudiantId;
        this.dispoId = dispoId;
    }

    // Constructeur complet (pour usage général)
    public RendezVousDetail(int rendezVousId, String statutRdv, Timestamp createdAt,
                            Date dateDispo, Time heureDebut, Time heureFin,
                            String typeConsult, String psyNom, String psyPrenom,
                            int psyId, String motif) {
        this.rendezVousId = rendezVousId;
        this.statutRdv = statutRdv;
        this.createdAt = createdAt;
        this.dateDispo = dateDispo;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.typeConsult = typeConsult;
        this.psyNom = psyNom;
        this.psyPrenom = psyPrenom;
        this.psyId = psyId;
        this.motif = motif;
    }

    // ========== GETTERS ==========

    // Getters communs
    public int getRendezVousId() { return rendezVousId; }
    public String getStatut() { return statutRdv; }
    public String getStatutRDV() { return statutRdv; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Date getDateDispo() { return dateDispo; }
    public Time getHeureDebut() { return heureDebut; }
    public Time getHeureFin() { return heureFin; }
    public String getTypeConsult() { return typeConsult; }
    public String getMotif() { return motif; }

    // Getters pour le PSYCHOLOGUE (infos étudiant)
    public String getEtudiantNom() { return etudiantNom; }
    public String getEtudiantPrenom() { return etudiantPrenom; }
    public String getEtudiantEmail() { return etudiantEmail; }
    public int getEtudiantId() { return etudiantId; }
    public int getDispoId() { return dispoId; }

    public String getEtudiantNomComplet() {
        if (etudiantPrenom == null || etudiantNom == null) return "";
        return etudiantPrenom + " " + etudiantNom;
    }

    // Getters pour l'ÉTUDIANT (infos psychologue)
    public String getPsyNom() { return psyNom; }
    public String getPsyPrenom() { return psyPrenom; }
    public int getPsyId() { return psyId; }

    public String getPsychologueNomComplet() {
        if (psyPrenom == null || psyNom == null) return "";
        return "Dr. " + psyPrenom + " " + psyNom;
    }

    // ========== SETTERS ==========
    public void setRendezVousId(int rendezVousId) { this.rendezVousId = rendezVousId; }
    public void setStatut(String statutRdv) { this.statutRdv = statutRdv; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setDateDispo(Date dateDispo) { this.dateDispo = dateDispo; }
    public void setHeureDebut(Time heureDebut) { this.heureDebut = heureDebut; }
    public void setHeureFin(Time heureFin) { this.heureFin = heureFin; }
    public void setTypeConsult(String typeConsult) { this.typeConsult = typeConsult; }
    public void setMotif(String motif) { this.motif = motif; }
    public void setPsyNom(String psyNom) { this.psyNom = psyNom; }
    public void setPsyPrenom(String psyPrenom) { this.psyPrenom = psyPrenom; }
    public void setPsyId(int psyId) { this.psyId = psyId; }
    public void setEtudiantNom(String etudiantNom) { this.etudiantNom = etudiantNom; }
    public void setEtudiantPrenom(String etudiantPrenom) { this.etudiantPrenom = etudiantPrenom; }
    public void setEtudiantEmail(String etudiantEmail) { this.etudiantEmail = etudiantEmail; }
    public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }
    public void setDispoId(int dispoId) { this.dispoId = dispoId; }

    @Override
    public String toString() {
        return "RendezVousDetail{" +
                "rendezVousId=" + rendezVousId +
                ", statutRdv='" + statutRdv + '\'' +
                ", dateDispo=" + dateDispo +
                ", heureDebut=" + heureDebut +
                ", typeConsult='" + typeConsult + '\'' +
                '}';
    }
}