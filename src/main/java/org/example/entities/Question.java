package org.example.entities;

import java.sql.Timestamp;

public class Question {
    private int questionId;
    private String texte;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String optionsQuest;
    private String scoreOptions;
    private String typeQuestion;
    private int questionnaireId;

    // Constructeur par défaut
    public Question() {
    }

    // Constructeur pour création (sans ID)
    public Question(String texte, String optionsQuest, String scoreOptions,
                    String typeQuestion, int questionnaireId) {
        this.texte = texte;
        this.optionsQuest = optionsQuest;
        this.scoreOptions = scoreOptions;
        this.typeQuestion = typeQuestion;
        this.questionnaireId = questionnaireId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public Question(int questionId, String texte, Timestamp createdAt, Timestamp updatedAt,
                    String optionsQuest, String scoreOptions, String typeQuestion, int questionnaireId) {
        this.questionId = questionId;
        this.texte = texte;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.optionsQuest = optionsQuest;
        this.scoreOptions = scoreOptions;
        this.typeQuestion = typeQuestion;
        this.questionnaireId = questionnaireId;
    }
    //constructeurrrr
    public Question(String texte, int questionnaireId, String optionsQuest, String scoreOptions) {
        this.texte = texte;
        this.questionnaireId = questionnaireId;
        this.optionsQuest = optionsQuest;
        this.scoreOptions = scoreOptions;
    }

    // Getters et Setters
    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
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

    public String getOptionsQuest() {
        return optionsQuest;
    }

    public void setOptionsQuest(String optionsQuest) {
        this.optionsQuest = optionsQuest;
    }

    public String getScoreOptions() {
        return scoreOptions;
    }

    public void setScoreOptions(String scoreOptions) {
        this.scoreOptions = scoreOptions;
    }

    public String getTypeQuestion() {
        return typeQuestion;
    }

    public void setTypeQuestion(String typeQuestion) {
        this.typeQuestion = typeQuestion;
    }

    public int getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(int questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    @Override
    public String toString() {
        return "Question{" +
                "questionId=" + questionId +
                ", texte='" + texte + '\'' +
                ", optionsQuest='" + optionsQuest + '\'' +
                ", scoreOptions='" + scoreOptions + '\'' +
                ", typeQuestion='" + typeQuestion + '\'' +
                ", questionnaireId=" + questionnaireId +
                '}';
    }
}