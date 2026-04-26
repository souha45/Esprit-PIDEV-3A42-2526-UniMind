package org.example.entities;

import org.example.enums.TypeQuestionnaire;
import java.sql.Timestamp;

public class Questionnaire {
    private int questionnaireId;
    private String code;
    private String nom;
    private String nomTraduit; // ✅ champ traduction
    private String description;
    private TypeQuestionnaire type;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String interpretatLegere;
    private String interpretatModere;
    private String interpretatSevere;
    private int seuilLegere;
    private int seuilModere;
    private int seuilSevere;
    private int nbreQuestions;
    private Integer adminId;

    public Questionnaire() {}

    public Questionnaire(String code, String nom, String description, TypeQuestionnaire type,
                         String interpretatLegere, String interpretatModere, String interpretatSevere,
                         int seuilLegere, int seuilModere, int seuilSevere, int nbreQuestions, Integer adminId) {
        this.code = code;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.interpretatLegere = interpretatLegere;
        this.interpretatModere = interpretatModere;
        this.interpretatSevere = interpretatSevere;
        this.seuilLegere = seuilLegere;
        this.seuilModere = seuilModere;
        this.seuilSevere = seuilSevere;
        this.nbreQuestions = nbreQuestions;
        this.adminId = adminId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public Questionnaire(int questionnaireId, String code, String nom, String description, TypeQuestionnaire type,
                         Timestamp createdAt, Timestamp updatedAt, String interpretatLegere,
                         String interpretatModere, String interpretatSevere, int seuilLegere,
                         int seuilModere, int seuilSevere, int nbreQuestions, Integer adminId) {
        this.questionnaireId = questionnaireId;
        this.code = code;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.interpretatLegere = interpretatLegere;
        this.interpretatModere = interpretatModere;
        this.interpretatSevere = interpretatSevere;
        this.seuilLegere = seuilLegere;
        this.seuilModere = seuilModere;
        this.seuilSevere = seuilSevere;
        this.nbreQuestions = nbreQuestions;
        this.adminId = adminId;
    }

    public Questionnaire(String code, String nom, String description, TypeQuestionnaire type) {
        this.code = code;
        this.nom = nom;
        this.description = description;
        this.type = type;
    }

    public Questionnaire(String code, String nom, String description) {
        this.code = code;
        this.nom = nom;
        this.description = description;
    }

    // ✅ Getters/Setters nomTraduit
    public String getNomTraduit() {
        return nomTraduit != null ? nomTraduit : nom;
    }

    public void setNomTraduit(String nomTraduit) {
        this.nomTraduit = nomTraduit;
    }

    public int getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(int questionnaireId) { this.questionnaireId = questionnaireId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TypeQuestionnaire getType() { return type; }
    public void setType(TypeQuestionnaire type) { this.type = type; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getInterpretatLegere() { return interpretatLegere; }
    public void setInterpretatLegere(String interpretatLegere) { this.interpretatLegere = interpretatLegere; }
    public String getInterpretatModere() { return interpretatModere; }
    public void setInterpretatModere(String interpretatModere) { this.interpretatModere = interpretatModere; }
    public String getInterpretatSevere() { return interpretatSevere; }
    public void setInterpretatSevere(String interpretatSevere) { this.interpretatSevere = interpretatSevere; }
    public int getSeuilLegere() { return seuilLegere; }
    public void setSeuilLegere(int seuilLegere) { this.seuilLegere = seuilLegere; }
    public int getSeuilModere() { return seuilModere; }
    public void setSeuilModere(int seuilModere) { this.seuilModere = seuilModere; }
    public int getSeuilSevere() { return seuilSevere; }
    public void setSeuilSevere(int seuilSevere) { this.seuilSevere = seuilSevere; }
    public int getNbreQuestions() { return nbreQuestions; }
    public void setNbreQuestions(int nbreQuestions) { this.nbreQuestions = nbreQuestions; }
    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    public String interpreterScore(int score) {
        if (score <= seuilLegere) return interpretatLegere;
        else if (score <= seuilModere) return interpretatModere;
        else return interpretatSevere;
    }

    public String getNiveauScore(int score) {
        if (score <= seuilLegere) return "legere";
        else if (score <= seuilModere) return "modere";
        else return "severe";
    }

    @Override
    public String toString() {
        return "Questionnaire{" +
                "questionnaireId=" + questionnaireId +
                ", code='" + code + '\'' +
                ", nom='" + nom + '\'' +
                ", type=" + (type != null ? type.toString() : null) +
                '}';
    }
}