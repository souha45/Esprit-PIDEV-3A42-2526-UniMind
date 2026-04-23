package org.example.entities;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.example.enums.RessentiSuivi;
import org.example.enums.SaisiPar;

public class SuiviTraitement {
    private int suivitraitementId;
    private Date dateSuivi;
    private boolean effectue;  // tinyint(4) -> boolean
    private Time heurePrevue;
    private Integer evaluation;  // Peut être NULL
    private boolean valide;  // tinyint(4) -> boolean
    private Timestamp dateSaisie;
    private Timestamp updatedAt;
    private int traitementId;
    private Time heureEffective;
    private String observations;
    private String observationsPsy;
    private RessentiSuivi ressenti;
    private SaisiPar saisiPar;
    private Timestamp createdAt;
    private String documentName;
    private Integer documentSize;
    private String documentMimeType;
    private String documentOriginalName;
    private Timestamp documentUpdatedAt;

    // Constructeur par défaut
    public SuiviTraitement() {
    }

    // Constructeur pour création (sans ID)
    public SuiviTraitement(Date dateSuivi, boolean effectue, Time heurePrevue,
                           boolean valide, int traitementId, String observations,
                           String observationsPsy, RessentiSuivi ressenti, SaisiPar saisiPar) {
        this.dateSuivi = dateSuivi;
        this.effectue = effectue;
        this.heurePrevue = heurePrevue;
        this.valide = valide;
        this.traitementId = traitementId;
        this.observations = observations;
        this.observationsPsy = observationsPsy;
        this.ressenti = ressenti;
        this.saisiPar = saisiPar;
        this.dateSaisie = new Timestamp(System.currentTimeMillis());
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur pour suivi effectué
    public SuiviTraitement(Date dateSuivi, boolean effectue, Time heurePrevue,
                           Time heureEffective, int traitementId, String observations,
                           String observationsPsy, RessentiSuivi ressenti, SaisiPar saisiPar) {
        this(dateSuivi, effectue, heurePrevue, true, traitementId, observations, observationsPsy, ressenti, saisiPar);
        this.heureEffective = heureEffective;
    }

    // Constructeur complet avec ID
    public SuiviTraitement(int suivitraitementId, Date dateSuivi, boolean effectue, Time heurePrevue,
                           Integer evaluation, boolean valide, Timestamp dateSaisie, Timestamp updatedAt,
                           int traitementId, Time heureEffective, String observations, String observationsPsy,
                           RessentiSuivi ressenti, SaisiPar saisiPar, Timestamp createdAt,
                           String documentName, Integer documentSize, String documentMimeType,
                           String documentOriginalName, Timestamp documentUpdatedAt) {
        this.suivitraitementId = suivitraitementId;
        this.dateSuivi = dateSuivi;
        this.effectue = effectue;
        this.heurePrevue = heurePrevue;
        this.evaluation = evaluation;
        this.valide = valide;
        this.dateSaisie = dateSaisie;
        this.updatedAt = updatedAt;
        this.traitementId = traitementId;
        this.heureEffective = heureEffective;
        this.observations = observations;
        this.observationsPsy = observationsPsy;
        this.ressenti = ressenti;
        this.saisiPar = saisiPar;
        this.createdAt = createdAt;
        this.documentName = documentName;
        this.documentSize = documentSize;
        this.documentMimeType = documentMimeType;
        this.documentOriginalName = documentOriginalName;
        this.documentUpdatedAt = documentUpdatedAt;
    }

    // Getters et Setters
    public int getSuivitraitementId() {
        return suivitraitementId;
    }

    public void setSuivitraitementId(int suivitraitementId) {
        this.suivitraitementId = suivitraitementId;
    }

    public Date getDateSuivi() {
        return dateSuivi;
    }

    public void setDateSuivi(Date dateSuivi) {
        this.dateSuivi = dateSuivi;
    }

    public boolean isEffectue() {
        return effectue;
    }

    public void setEffectue(boolean effectue) {
        this.effectue = effectue;
    }

    public Time getHeurePrevue() {
        return heurePrevue;
    }

    public void setHeurePrevue(Time heurePrevue) {
        this.heurePrevue = heurePrevue;
    }

    public Integer getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Integer evaluation) {
        this.evaluation = evaluation;
    }

    public boolean isValide() {
        return valide;
    }

    public void setValide(boolean valide) {
        this.valide = valide;
    }

    public Timestamp getDateSaisie() {
        return dateSaisie;
    }

    public void setDateSaisie(Timestamp dateSaisie) {
        this.dateSaisie = dateSaisie;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getTraitementId() {
        return traitementId;
    }

    public void setTraitementId(int traitementId) {
        this.traitementId = traitementId;
    }

    public Time getHeureEffective() {
        return heureEffective;
    }

    public void setHeureEffective(Time heureEffective) {
        this.heureEffective = heureEffective;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getObservationsPsy() {
        return observationsPsy;
    }

    public void setObservationsPsy(String observationsPsy) {
        this.observationsPsy = observationsPsy;
    }

    public RessentiSuivi getRessenti() {
        return ressenti;
    }

    public void setRessenti(RessentiSuivi ressenti) {
        this.ressenti = ressenti;
    }

    public SaisiPar getSaisiPar() {
        return saisiPar;
    }

    public void setSaisiPar(SaisiPar saisiPar) {
        this.saisiPar = saisiPar;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Integer getDocumentSize() {
        return documentSize;
    }

    public void setDocumentSize(Integer documentSize) {
        this.documentSize = documentSize;
    }

    public String getDocumentMimeType() {
        return documentMimeType;
    }

    public void setDocumentMimeType(String documentMimeType) {
        this.documentMimeType = documentMimeType;
    }

    public String getDocumentOriginalName() {
        return documentOriginalName;
    }

    public void setDocumentOriginalName(String documentOriginalName) {
        this.documentOriginalName = documentOriginalName;
    }

    public Timestamp getDocumentUpdatedAt() {
        return documentUpdatedAt;
    }

    public void setDocumentUpdatedAt(Timestamp documentUpdatedAt) {
        this.documentUpdatedAt = documentUpdatedAt;
    }

    // Méthodes utilitaires
    public String getRessentiLabel() {
        switch (ressenti) {
            case TRES_BIEN:
                return "Très bien";
            case BIEN:
                return "Bien";
            case NEUTRE:
                return "Neutre";
            case DIFFICILE:
                return "Difficile";
            case TRES_DIFFICILE:
                return "Très difficile";
            default:
                return ressenti.toString();
        }
    }

    public String getSaisiParLabel() {
        switch (saisiPar) {
            case ETUDIANT:
                return "Étudiant";
            case PSYCHOLOGUE:
                return "Psychologue";
            default:
                return saisiPar.toString();
        }
    }

    public void valider() {
        this.valide = true;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void invalider() {
        this.valide = false;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void ajouterDocument(String name, String originalName, String mimeType, int size) {
        this.documentName = name;
        this.documentOriginalName = originalName;
        this.documentMimeType = mimeType;
        this.documentSize = size;
        this.documentUpdatedAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean hasDocument() {
        return documentName != null && !documentName.isEmpty();
    }

    @Override
    public String toString() {
        return "SuiviTraitement{" +
                "suivitraitementId=" + suivitraitementId +
                ", dateSuivi=" + dateSuivi +
                ", effectue=" + effectue +
                ", valide=" + valide +
                ", ressenti=" + ressenti +
                ", saisiPar=" + saisiPar +
                ", traitementId=" + traitementId +
                '}';
    }
}