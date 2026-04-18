package org.example.entities;

import org.example.enums.StatutSponsor;
import org.example.enums.TypeContribution;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class EvenementSponsor {
    private int evenementSponsorId;
    private BigDecimal montantContribution;
    private TypeContribution typeContribution;
    private String descriptionContribution;
    private Timestamp dateContribution;
    private StatutSponsor statut;  // CONFIRME, EN_ATTENTE, REFUSE, ANNULE
    private int evenementId;
    private int sponsorId;

    // Constructeur par défaut
    public EvenementSponsor() {
    }

    // Constructeur pour création (sans ID)
    public EvenementSponsor(BigDecimal montantContribution, TypeContribution typeContribution,
                            String descriptionContribution, Timestamp dateContribution,
                            StatutSponsor statut, int evenementId, int sponsorId) {
        this.montantContribution = montantContribution;
        this.typeContribution = typeContribution;
        this.descriptionContribution = descriptionContribution;
        this.dateContribution = dateContribution;
        this.statut = statut;
        this.evenementId = evenementId;
        this.sponsorId = sponsorId;
    }

    // Constructeur simplifié pour création rapide
    public EvenementSponsor(BigDecimal montantContribution, TypeContribution typeContribution,
                            int evenementId, int sponsorId) {
        this.montantContribution = montantContribution;
        this.typeContribution = typeContribution;
        this.descriptionContribution = null;
        this.dateContribution = new Timestamp(System.currentTimeMillis());
        this.statut = StatutSponsor.EN_ATTENTE;
        this.evenementId = evenementId;
        this.sponsorId = sponsorId;
    }

    // Constructeur complet avec ID
    public EvenementSponsor(int evenementSponsorId, BigDecimal montantContribution,
                            TypeContribution typeContribution, String descriptionContribution,
                            Timestamp dateContribution, StatutSponsor statut,
                            int evenementId, int sponsorId) {
        this.evenementSponsorId = evenementSponsorId;
        this.montantContribution = montantContribution;
        this.typeContribution = typeContribution;
        this.descriptionContribution = descriptionContribution;
        this.dateContribution = dateContribution;
        this.statut = statut;
        this.evenementId = evenementId;
        this.sponsorId = sponsorId;
    }

    // Getters et Setters
    public int getEvenementSponsorId() {
        return evenementSponsorId;
    }

    public void setEvenementSponsorId(int evenementSponsorId) {
        this.evenementSponsorId = evenementSponsorId;
    }

    public BigDecimal getMontantContribution() {
        return montantContribution;
    }

    public void setMontantContribution(BigDecimal montantContribution) {
        this.montantContribution = montantContribution;
    }

    public TypeContribution getTypeContribution() {
        return typeContribution;
    }

    public void setTypeContribution(TypeContribution typeContribution) {
        this.typeContribution = typeContribution;
    }

    public String getDescriptionContribution() {
        return descriptionContribution;
    }

    public void setDescriptionContribution(String descriptionContribution) {
        this.descriptionContribution = descriptionContribution;
    }

    public Timestamp getDateContribution() {
        return dateContribution;
    }

    public void setDateContribution(Timestamp dateContribution) {
        this.dateContribution = dateContribution;
    }

    public StatutSponsor getStatut() {
        return statut;
    }

    public void setStatut(StatutSponsor statut) {
        this.statut = statut;
    }

    public int getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
    }

    public int getSponsorId() {
        return sponsorId;
    }

    public void setSponsorId(int sponsorId) {
        this.sponsorId = sponsorId;
    }

    // Méthodes utilitaires
    public boolean isConfirmé() {
        return statut == StatutSponsor.CONFIRME;
    }

    public boolean isEnAttente() {
        return statut == StatutSponsor.EN_ATTENTE;
    }

    public boolean isRefusé() {
        return statut == StatutSponsor.REFUSE;
    }

    public boolean isAnnulé() {
        return statut == StatutSponsor.ANNULE;
    }

    public String getTypeContributionLabel() {
        switch (typeContribution) {
            case FINANCIER:
                return "Financier";
            case MATERIEL:
                return "Matériel";
            case LOGISTIQUE:
                return "Logistique";
            case COMMUNICATION:
                return "Communication";
            case AUTRE:
                return "Autre";
            default:
                return typeContribution.toString();
        }
    }

    public String getStatutLabel() {
        switch (statut) {
            case CONFIRME:
                return "Confirmé";
            case EN_ATTENTE:
                return "En attente";
            case REFUSE:
                return "Refusé";
            case ANNULE:
                return "Annulé";
            default:
                return statut.toString();
        }
    }

    @Override
    public String toString() {
        return "EvenementSponsor{" +
                "evenementSponsorId=" + evenementSponsorId +
                ", montantContribution=" + montantContribution +
                ", typeContribution=" + typeContribution +
                ", statut=" + statut +
                ", evenementId=" + evenementId +
                ", sponsorId=" + sponsorId +
                '}';
    }
}