package org.example.entities;

import org.example.enums.StatutParticipation;
import java.sql.Timestamp;

public class Participation {
    private int participationId;
    private Timestamp dateInscription;
    private StatutParticipation statut;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int evenementId;
    private int etudiantId;
    private Short noteSatisfaction;      // Peut être NULL
    private String feedbackCommentaire;   // Peut être NULL
    private Timestamp feedbackAt;         // Peut être NULL
    private String qrToken;               // Peut être NULL
    private Timestamp scannedAt;          // Peut être NULL
    private Boolean present;              // Peut être NULL (tinyint)

    // Constructeur par défaut
    public Participation() {
    }

    // Constructeur pour création (sans ID)
    public Participation(int evenementId, int etudiantId, StatutParticipation statut) {
        this.evenementId = evenementId;
        this.etudiantId = etudiantId;
        this.statut = statut;
        this.dateInscription = new Timestamp(System.currentTimeMillis());
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur pour création avec QR token
    public Participation(int evenementId, int etudiantId, StatutParticipation statut, String qrToken) {
        this(evenementId, etudiantId, statut);
        this.qrToken = qrToken;
    }

    // Constructeur complet avec ID
    public Participation(int participationId, Timestamp dateInscription, StatutParticipation statut,
                         Timestamp createdAt, Timestamp updatedAt, int evenementId, int etudiantId,
                         Short noteSatisfaction, String feedbackCommentaire, Timestamp feedbackAt,
                         String qrToken, Timestamp scannedAt, Boolean present) {
        this.participationId = participationId;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.evenementId = evenementId;
        this.etudiantId = etudiantId;
        this.noteSatisfaction = noteSatisfaction;
        this.feedbackCommentaire = feedbackCommentaire;
        this.feedbackAt = feedbackAt;
        this.qrToken = qrToken;
        this.scannedAt = scannedAt;
        this.present = present;
    }

    // Getters et Setters
    public int getParticipationId() {
        return participationId;
    }

    public void setParticipationId(int participationId) {
        this.participationId = participationId;
    }

    public Timestamp getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Timestamp dateInscription) {
        this.dateInscription = dateInscription;
    }

    public StatutParticipation getStatut() {
        return statut;
    }

    public void setStatut(StatutParticipation statut) {
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

    public int getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
    }

    public int getEtudiantId() {
        return etudiantId;
    }

    public void setEtudiantId(int etudiantId) {
        this.etudiantId = etudiantId;
    }

    public Short getNoteSatisfaction() {
        return noteSatisfaction;
    }

    public void setNoteSatisfaction(Short noteSatisfaction) {
        this.noteSatisfaction = noteSatisfaction;
    }

    public String getFeedbackCommentaire() {
        return feedbackCommentaire;
    }

    public void setFeedbackCommentaire(String feedbackCommentaire) {
        this.feedbackCommentaire = feedbackCommentaire;
    }

    public Timestamp getFeedbackAt() {
        return feedbackAt;
    }

    public void setFeedbackAt(Timestamp feedbackAt) {
        this.feedbackAt = feedbackAt;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public Timestamp getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Timestamp scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    // Méthodes utilitaires
    public void ajouterFeedback(short note, String commentaire) {
        this.noteSatisfaction = note;
        this.feedbackCommentaire = commentaire;
        this.feedbackAt = new Timestamp(System.currentTimeMillis());
    }

    public void confirmerPresence() {
        this.present = true;
        this.scannedAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean isPresent() {
        return present != null && present;
    }

    public boolean hasFeedback() {
        return noteSatisfaction != null;
    }

    @Override
    public String toString() {
        return "Participation{" +
                "participationId=" + participationId +
                ", evenementId=" + evenementId +
                ", etudiantId=" + etudiantId +
                ", statut=" + statut +
                ", present=" + present +
                ", noteSatisfaction=" + noteSatisfaction +
                '}';
    }
}