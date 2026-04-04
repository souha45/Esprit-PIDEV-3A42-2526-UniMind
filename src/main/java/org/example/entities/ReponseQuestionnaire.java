package org.example.entities;

import java.sql.Timestamp;

public class ReponseQuestionnaire {
    private int reponseQuestionnaireId;
    private double scoreTotale;
    private String reponseQuest;      // Stocké en JSON dans la DB
    private String interpretation;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer dureePassage;     // Peut être NULL
    private String niveau;
    private boolean aBesoinPsy;       // tinyint(4) -> boolean
    private String commentaire;       // Peut être NULL
    private int questionnaireId;
    private int userId;           // Peut être NULL

    // Constructeur par défaut
    public ReponseQuestionnaire() {
    }

    // Constructeur pour création (sans ID)
    public ReponseQuestionnaire(double scoreTotale, String reponseQuest, String interpretation,
                                Integer dureePassage, String niveau, boolean aBesoinPsy,
                                String commentaire, int questionnaireId, int userId) {
        this.scoreTotale = scoreTotale;
        this.reponseQuest = reponseQuest;
        this.interpretation = interpretation;
        this.dureePassage = dureePassage;
        this.niveau = niveau;
        this.aBesoinPsy = aBesoinPsy;
        this.commentaire = commentaire;
        this.questionnaireId = questionnaireId;
        this.userId = userId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public ReponseQuestionnaire(int reponseQuestionnaireId, double scoreTotale, String reponseQuest,
                                String interpretation, Timestamp createdAt, Timestamp updatedAt,
                                Integer dureePassage, String niveau, boolean aBesoinPsy,
                                String commentaire, int questionnaireId, int userId) {
        this.reponseQuestionnaireId = reponseQuestionnaireId;
        this.scoreTotale = scoreTotale;
        this.reponseQuest = reponseQuest;
        this.interpretation = interpretation;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.dureePassage = dureePassage;
        this.niveau = niveau;
        this.aBesoinPsy = aBesoinPsy;
        this.commentaire = commentaire;
        this.questionnaireId = questionnaireId;
        this.userId = userId;
    }

    // Getters et Setters
    public int getReponseQuestionnaireId() {
        return reponseQuestionnaireId;
    }

    public void setReponseQuestionnaireId(int reponseQuestionnaireId) {
        this.reponseQuestionnaireId = reponseQuestionnaireId;
    }

    public double getScoreTotale() {
        return scoreTotale;
    }

    public void setScoreTotale(double scoreTotale) {
        this.scoreTotale = scoreTotale;
    }

    public String getReponseQuest() {
        return reponseQuest;
    }

    public void setReponseQuest(String reponseQuest) {
        this.reponseQuest = reponseQuest;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
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

    public Integer getDureePassage() {
        return dureePassage;
    }

    public void setDureePassage(Integer dureePassage) {
        this.dureePassage = dureePassage;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public boolean isaBesoinPsy() {
        return aBesoinPsy;
    }

    public void setaBesoinPsy(boolean aBesoinPsy) {
        this.aBesoinPsy = aBesoinPsy;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public int getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(int questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ReponseQuestionnaire{" +
                "reponseQuestionnaireId=" + reponseQuestionnaireId +
                ", scoreTotale=" + scoreTotale +
                ", niveau='" + niveau + '\'' +
                ", aBesoinPsy=" + aBesoinPsy +
                ", questionnaireId=" + questionnaireId +
                ", userId=" + userId +
                '}';
    }
}