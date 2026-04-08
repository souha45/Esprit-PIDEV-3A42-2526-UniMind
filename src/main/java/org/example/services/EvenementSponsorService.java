package org.example.services;

import org.example.entities.EvenementSponsor;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeContribution;
import org.example.utils.MyDataBase_Unimind;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementSponsorService implements ICrud<EvenementSponsor> {

    private final Connection connection;

    public EvenementSponsorService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(EvenementSponsor es) throws SQLException {
        System.out.println("Ajout d'un sponsoring pour l'événement ID: " + es.getEvenementId() + " par le sponsor ID: " + es.getSponsorId());
        String sql = "INSERT INTO evenement_sponsor (montant_contribution, type_contribution, description_contribution, date_contribution, statut, evenement_id, sponsor_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, es.getMontantContribution() != null ? es.getMontantContribution() : BigDecimal.ZERO);
            ps.setString(2, es.getTypeContribution() != null ? es.getTypeContribution().getDbValue() : null);
            ps.setString(3, es.getDescriptionContribution());
            ps.setTimestamp(4, es.getDateContribution() != null ? es.getDateContribution() : new Timestamp(System.currentTimeMillis()));
            ps.setString(5, es.getStatut() != null ? es.getStatut().getDbValue() : null);
            ps.setInt(6, es.getEvenementId());
            ps.setInt(7, es.getSponsorId());

            ps.executeUpdate();
            System.out.println("Sponsoring ajouté avec succès");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    es.setEvenementSponsorId(rs.getInt(1));
                    System.out.println("ID généré pour le sponsoring: " + es.getEvenementSponsorId());
                }
            }
        }
    }

    @Override
    public void modifier(EvenementSponsor es) throws SQLException {
        String sql = "UPDATE evenement_sponsor SET montant_contribution=?, type_contribution=?, description_contribution=?, date_contribution=?, statut=?, evenement_id=?, sponsor_id=? " +
                "WHERE evenementSponsor_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, es.getMontantContribution() != null ? es.getMontantContribution() : BigDecimal.ZERO);
            ps.setString(2, es.getTypeContribution() != null ? es.getTypeContribution().getDbValue() : null);
            ps.setString(3, es.getDescriptionContribution());
            ps.setTimestamp(4, es.getDateContribution());
            ps.setString(5, es.getStatut() != null ? es.getStatut().getDbValue() : null);
            ps.setInt(6, es.getEvenementId());
            ps.setInt(7, es.getSponsorId());
            ps.setInt(8, es.getEvenementSponsorId());

            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM evenement_sponsor WHERE evenementSponsor_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<EvenementSponsor> afficher() throws SQLException {
        String sql = "SELECT evenementSponsor_id, montant_contribution, type_contribution, description_contribution, date_contribution, statut, evenement_id, sponsor_id FROM evenement_sponsor";
        List<EvenementSponsor> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public EvenementSponsor findById(int id) throws SQLException {
        String sql = "SELECT evenementSponsor_id, montant_contribution, type_contribution, description_contribution, date_contribution, statut, evenement_id, sponsor_id FROM evenement_sponsor WHERE evenementSponsor_id=?";
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

    private EvenementSponsor mapRow(ResultSet rs) throws SQLException {
        int evenementSponsorId = rs.getInt("evenementSponsor_id");
        BigDecimal montant = rs.getBigDecimal("montant_contribution");
        TypeContribution typeContribution = TypeContribution.fromDb(rs.getString("type_contribution"));
        String description = rs.getString("description_contribution");
        Timestamp dateContribution = rs.getTimestamp("date_contribution");
        StatutSponsor statut = StatutSponsor.fromDb(rs.getString("statut"));
        int evenementId = rs.getInt("evenement_id");
        int sponsorId = rs.getInt("sponsor_id");

        return new EvenementSponsor(
                evenementSponsorId,
                montant,
                typeContribution,
                description,
                dateContribution,
                statut,
                evenementId,
                sponsorId
        );
    }
}
