package org.example.services;

import org.example.entities.Sponsor;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeSponsor;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SponsorService implements ICrud<Sponsor> {

    private final Connection connection;

    public SponsorService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(Sponsor s) throws SQLException {
        System.out.println("Ajout d'un nouveau sponsor: " + s.getNomSponsor());
        String sql = "INSERT INTO sponsor (nom_sponsor, type_sponsor, site_web, email_contact, telephone, adresse, domaine_activite, logo, statut, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getNomSponsor());
            ps.setString(2, s.getTypeSponsor() != null ? s.getTypeSponsor().getDbValue() : null);
            ps.setString(3, s.getSiteWeb());
            ps.setString(4, s.getEmailContact());
            ps.setString(5, s.getTelephone());
            ps.setString(6, s.getAdresse());
            ps.setString(7, s.getDomaineActivite());
            ps.setString(8, s.getLogo());
            ps.setString(9, s.getStatut() != null ? s.getStatut().getDbValue() : null);
            ps.setTimestamp(10, s.getCreatedAt() != null ? s.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(11, s.getUpdatedAt());

            ps.executeUpdate();
            System.out.println("Sponsor ajoute avec succes");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    s.setSponsorId(rs.getInt(1));
                    System.out.println("ID genere pour le sponsor: " + s.getSponsorId());
                }
            }
        }
    }

    @Override
    public void modifier(Sponsor s) throws SQLException {
        String sql = "UPDATE sponsor SET nom_sponsor=?, type_sponsor=?, site_web=?, email_contact=?, telephone=?, adresse=?, domaine_activite=?, logo=?, statut=?, updated_at=? " +
                "WHERE sponsor_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getNomSponsor());
            ps.setString(2, s.getTypeSponsor() != null ? s.getTypeSponsor().getDbValue() : null);
            ps.setString(3, s.getSiteWeb());
            ps.setString(4, s.getEmailContact());
            ps.setString(5, s.getTelephone());
            ps.setString(6, s.getAdresse());
            ps.setString(7, s.getDomaineActivite());
            ps.setString(8, s.getLogo());
            ps.setString(9, s.getStatut() != null ? s.getStatut().getDbValue() : null);
            ps.setTimestamp(10, s.getUpdatedAt() != null ? s.getUpdatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setInt(11, s.getSponsorId());

            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        System.out.println("Suppression du sponsor ID: " + id + " avec cascade delete");

        // Supprimer d'abord les attributions sponsors liées au sponsor
        String sqlAttributions = "DELETE FROM evenement_sponsor WHERE sponsor_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlAttributions)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Attributions liees au sponsor ID " + id + " supprimees");
        }

        // Supprimer le sponsor lui-même
        String sqlSponsor = "DELETE FROM sponsor WHERE sponsor_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlSponsor)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Sponsor ID " + id + " supprime avec succes (cascade delete effectue)");
            } else {
                System.out.println("Aucun sponsor trouve avec l'ID: " + id);
            }
        }
    }

    @Override
    public List<Sponsor> afficher() throws SQLException {
        String sql = "SELECT sponsor_id, nom_sponsor, type_sponsor, site_web, email_contact, telephone, adresse, domaine_activite, statut, created_at, updated_at, logo FROM sponsor";
        List<Sponsor> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public Sponsor findById(int id) throws SQLException {
        String sql = "SELECT sponsor_id, nom_sponsor, type_sponsor, site_web, email_contact, telephone, adresse, domaine_activite, statut, created_at, updated_at, logo FROM sponsor WHERE sponsor_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private Sponsor mapRow(ResultSet rs) throws SQLException {
        int sponsorId = rs.getInt("sponsor_id");
        String nomSponsor = rs.getString("nom_sponsor");
        TypeSponsor typeSponsor = TypeSponsor.fromDb(rs.getString("type_sponsor"));
        String siteWeb = rs.getString("site_web");
        String emailContact = rs.getString("email_contact");
        String telephone = rs.getString("telephone");
        String adresse = rs.getString("adresse");
        String domaineActivite = rs.getString("domaine_activite");
        StatutSponsor statut = StatutSponsor.fromDb(rs.getString("statut"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        String logo = rs.getString("logo");

        return new Sponsor(
                sponsorId,
                nomSponsor,
                typeSponsor,
                siteWeb,
                emailContact,
                telephone,
                adresse,
                domaineActivite,
                statut,
                createdAt,
                updatedAt,
                logo
        );
    }

    /**
     * Récupère les informations de sponsoring pour un sponsor (événement et date)
     * @param sponsorId L'ID du sponsor
     * @return Un tableau [evenementTitre, dateContribution, organisateurId] ou [null, null, -1] si pas de sponsoring
     */
    public Object[] getSponsoringInfos(int sponsorId) throws SQLException {
        String sql = "SELECT e.titre, es.date_contribution, e.organisateur_id " +
                     "FROM evenement_sponsor es " +
                     "JOIN evenement e ON es.evenement_id = e.evenement_id " +
                     "WHERE es.sponsor_id = ? " +
                     "ORDER BY es.date_contribution DESC " +
                     "LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, sponsorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String evenementTitre = rs.getString("titre");
                    java.sql.Timestamp dateContribution = rs.getTimestamp("date_contribution");
                    int organisateurId = rs.getInt("organisateur_id");
                    return new Object[]{evenementTitre, dateContribution != null ? dateContribution.toString() : null, organisateurId};
                }
            }
        }
        return new Object[]{null, null, -1};
    }

    /**
     * Classe interne pour représenter un sponsor avec les informations de sponsoring
     */
    public static class SponsorAvecInfos {
        private int sponsorId;
        private String nomSponsor;
        private TypeSponsor typeSponsor;
        private String emailContact;
        private String logo;
        private StatutSponsor statut;
        private java.sql.Timestamp dateContribution;
        private String evenementTitre;
        private int organisateurId;

        public int getSponsorId() { return sponsorId; }
        public void setSponsorId(int sponsorId) { this.sponsorId = sponsorId; }
        public String getNomSponsor() { return nomSponsor; }
        public void setNomSponsor(String nomSponsor) { this.nomSponsor = nomSponsor; }
        public TypeSponsor getTypeSponsor() { return typeSponsor; }
        public void setTypeSponsor(TypeSponsor typeSponsor) { this.typeSponsor = typeSponsor; }
        public String getEmailContact() { return emailContact; }
        public void setEmailContact(String emailContact) { this.emailContact = emailContact; }
        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
        public StatutSponsor getStatut() { return statut; }
        public void setStatut(StatutSponsor statut) { this.statut = statut; }
        public java.sql.Timestamp getDateContribution() { return dateContribution; }
        public void setDateContribution(java.sql.Timestamp dateContribution) { this.dateContribution = dateContribution; }
        public String getEvenementTitre() { return evenementTitre; }
        public void setEvenementTitre(String evenementTitre) { this.evenementTitre = evenementTitre; }
        public int getOrganisateurId() { return organisateurId; }
        public void setOrganisateurId(int organisateurId) { this.organisateurId = organisateurId; }
    }
}
