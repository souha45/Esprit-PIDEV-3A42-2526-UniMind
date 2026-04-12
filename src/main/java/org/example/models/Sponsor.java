package org.example.models;

import org.example.enums.StatutSponsor;
import org.example.enums.TypeSponsor;
import java.sql.Timestamp;

public class Sponsor {
    private int sponsorId;
    private String nomSponsor;
    private TypeSponsor typeSponsor;
    private String siteWeb;
    private String emailContact;
    private String telephone;
    private String adresse;
    private String domaineActivite;
    private StatutSponsor statut;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String logo;

    // Constructeur par défaut
    public Sponsor() {
    }

    // Constructeur pour création (sans ID)
    public Sponsor(String nomSponsor, TypeSponsor typeSponsor, String siteWeb,
                   String emailContact, String telephone, String adresse,
                   String domaineActivite, StatutSponsor statut, String logo) {
        this.nomSponsor = nomSponsor;
        this.typeSponsor = typeSponsor;
        this.siteWeb = siteWeb;
        this.emailContact = emailContact;
        this.telephone = telephone;
        this.adresse = adresse;
        this.domaineActivite = domaineActivite;
        this.statut = statut;
        this.logo = logo;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Constructeur complet avec ID
    public Sponsor(int sponsorId, String nomSponsor, TypeSponsor typeSponsor, String siteWeb,
                   String emailContact, String telephone, String adresse, String domaineActivite,
                   StatutSponsor statut, Timestamp createdAt, Timestamp updatedAt, String logo) {
        this.sponsorId = sponsorId;
        this.nomSponsor = nomSponsor;
        this.typeSponsor = typeSponsor;
        this.siteWeb = siteWeb;
        this.emailContact = emailContact;
        this.telephone = telephone;
        this.adresse = adresse;
        this.domaineActivite = domaineActivite;
        this.statut = statut;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.logo = logo;
    }

    // Getters et Setters
    public int getSponsorId() {
        return sponsorId;
    }

    public void setSponsorId(int sponsorId) {
        this.sponsorId = sponsorId;
    }

    public String getNomSponsor() {
        return nomSponsor;
    }

    public void setNomSponsor(String nomSponsor) {
        this.nomSponsor = nomSponsor;
    }

    public TypeSponsor getTypeSponsor() {
        return typeSponsor;
    }

    public void setTypeSponsor(TypeSponsor typeSponsor) {
        this.typeSponsor = typeSponsor;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getEmailContact() {
        return emailContact;
    }

    public void setEmailContact(String emailContact) {
        this.emailContact = emailContact;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getDomaineActivite() {
        return domaineActivite;
    }

    public void setDomaineActivite(String domaineActivite) {
        this.domaineActivite = domaineActivite;
    }

    public StatutSponsor getStatut() {
        return statut;
    }

    public void setStatut(StatutSponsor statut) {
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

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public String toString() {
        return "Sponsor{" +
                "sponsorId=" + sponsorId +
                ", nomSponsor='" + nomSponsor + '\'' +
                ", typeSponsor=" + typeSponsor +
                ", emailContact='" + emailContact + '\'' +
                ", statut=" + statut +
                '}';
    }
}