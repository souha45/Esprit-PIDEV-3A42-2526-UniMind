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
            System.out.println("Sponsor ajouté avec succès");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    s.setSponsorId(rs.getInt(1));
                    System.out.println("ID généré pour le sponsor: " + s.getSponsorId());
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
        String sql = "DELETE FROM sponsor WHERE sponsor_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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
}
