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

    // Classe pour afficher les attributions avec les noms
    public static class AttributionAvecInfos {
        private int evenementSponsorId;
        private int evenementId;
        private String evenementTitre;
        private int sponsorId;
        private String sponsorNom;
        private BigDecimal montantContribution;
        private TypeContribution typeContribution;
        private String descriptionContribution;
        private Timestamp dateContribution;
        private StatutSponsor statut;
        private int organisateurId;

        public AttributionAvecInfos() {}

        public AttributionAvecInfos(int evenementSponsorId, int evenementId, String evenementTitre,
                                   int sponsorId, String sponsorNom, BigDecimal montantContribution,
                                   TypeContribution typeContribution, String descriptionContribution,
                                   Timestamp dateContribution, StatutSponsor statut, int organisateurId) {
            this.evenementSponsorId = evenementSponsorId;
            this.evenementId = evenementId;
            this.evenementTitre = evenementTitre;
            this.sponsorId = sponsorId;
            this.sponsorNom = sponsorNom;
            this.montantContribution = montantContribution;
            this.typeContribution = typeContribution;
            this.descriptionContribution = descriptionContribution;
            this.dateContribution = dateContribution;
            this.statut = statut;
            this.organisateurId = organisateurId;
        }

        // Getters et Setters
        public int getEvenementSponsorId() { return evenementSponsorId; }
        public void setEvenementSponsorId(int evenementSponsorId) { this.evenementSponsorId = evenementSponsorId; }

        public int getEvenementId() { return evenementId; }
        public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

        public String getEvenementTitre() { return evenementTitre; }
        public void setEvenementTitre(String evenementTitre) { this.evenementTitre = evenementTitre; }

        public int getSponsorId() { return sponsorId; }
        public void setSponsorId(int sponsorId) { this.sponsorId = sponsorId; }

        public String getSponsorNom() { return sponsorNom; }
        public void setSponsorNom(String sponsorNom) { this.sponsorNom = sponsorNom; }

        public BigDecimal getMontantContribution() { return montantContribution; }
        public void setMontantContribution(BigDecimal montantContribution) { this.montantContribution = montantContribution; }

        public TypeContribution getTypeContribution() { return typeContribution; }
        public void setTypeContribution(TypeContribution typeContribution) { this.typeContribution = typeContribution; }

        public String getDescriptionContribution() { return descriptionContribution; }
        public void setDescriptionContribution(String descriptionContribution) { this.descriptionContribution = descriptionContribution; }

        public Timestamp getDateContribution() { return dateContribution; }
        public void setDateContribution(Timestamp dateContribution) { this.dateContribution = dateContribution; }

        public StatutSponsor getStatut() { return statut; }
        public void setStatut(StatutSponsor statut) { this.statut = statut; }

        public int getOrganisateurId() { return organisateurId; }
        public void setOrganisateurId(int organisateurId) { this.organisateurId = organisateurId; }
    }

    @Override
    public void ajouter(EvenementSponsor es) throws SQLException {
        System.out.println("Ajout d'un sponsoring pour l'evenement ID: " + es.getEvenementId() + " par le sponsor ID: " + es.getSponsorId());
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
            System.out.println("Sponsoring ajoute avec succes");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    es.setEvenementSponsorId(rs.getInt(1));
                    System.out.println("ID genere pour le sponsoring: " + es.getEvenementSponsorId());
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

    // Récupérer les attributions avec les noms pour un organisateur spécifique
    public List<AttributionAvecInfos> afficherAvecInfosPourOrganisateur(int organisateurId) throws SQLException {
        String sql = "SELECT es.evenementSponsor_id, es.evenement_id, e.titre AS evenement_titre, " +
                     "es.sponsor_id, s.nom_sponsor AS sponsor_nom, es.montant_contribution, " +
                     "es.type_contribution, es.description_contribution, es.date_contribution, es.statut, " +
                     "e.organisateur_id " +
                     "FROM evenement_sponsor es " +
                     "JOIN evenement e ON es.evenement_id = e.evenement_id " +
                     "JOIN sponsor s ON es.sponsor_id = s.sponsor_id " +
                     "WHERE e.organisateur_id = ? " +
                     "ORDER BY es.date_contribution DESC";

        List<AttributionAvecInfos> result = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, organisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int evenementSponsorId = rs.getInt("evenementSponsor_id");
                    int evenementId = rs.getInt("evenement_id");
                    String evenementTitre = rs.getString("evenement_titre");
                    int sponsorId = rs.getInt("sponsor_id");
                    String sponsorNom = rs.getString("sponsor_nom");
                    BigDecimal montant = rs.getBigDecimal("montant_contribution");
                    TypeContribution typeContribution = TypeContribution.fromDb(rs.getString("type_contribution"));
                    String description = rs.getString("description_contribution");
                    Timestamp dateContribution = rs.getTimestamp("date_contribution");
                    StatutSponsor statut = StatutSponsor.fromDb(rs.getString("statut"));
                    int orgId = rs.getInt("organisateur_id");

                    result.add(new AttributionAvecInfos(
                            evenementSponsorId, evenementId, evenementTitre,
                            sponsorId, sponsorNom, montant, typeContribution,
                            description, dateContribution, statut, orgId
                    ));
                }
            }
        }

        return result;
    }

    // Récupérer toutes les attributions avec les noms (pour l'admin)
    public List<AttributionAvecInfos> afficherAvecInfos() throws SQLException {
        String sql = "SELECT es.evenementSponsor_id, es.evenement_id, e.titre AS evenement_titre, " +
                     "es.sponsor_id, s.nom_sponsor AS sponsor_nom, es.montant_contribution, " +
                     "es.type_contribution, es.description_contribution, es.date_contribution, es.statut, " +
                     "e.organisateur_id " +
                     "FROM evenement_sponsor es " +
                     "JOIN evenement e ON es.evenement_id = e.evenement_id " +
                     "JOIN sponsor s ON es.sponsor_id = s.sponsor_id " +
                     "ORDER BY es.date_contribution DESC";

        List<AttributionAvecInfos> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int evenementSponsorId = rs.getInt("evenementSponsor_id");
                int evenementId = rs.getInt("evenement_id");
                String evenementTitre = rs.getString("evenement_titre");
                int sponsorId = rs.getInt("sponsor_id");
                String sponsorNom = rs.getString("sponsor_nom");
                BigDecimal montant = rs.getBigDecimal("montant_contribution");
                TypeContribution typeContribution = TypeContribution.fromDb(rs.getString("type_contribution"));
                String description = rs.getString("description_contribution");
                Timestamp dateContribution = rs.getTimestamp("date_contribution");
                StatutSponsor statut = StatutSponsor.fromDb(rs.getString("statut"));
                int orgId = rs.getInt("organisateur_id");

                result.add(new AttributionAvecInfos(
                        evenementSponsorId, evenementId, evenementTitre,
                        sponsorId, sponsorNom, montant, typeContribution,
                        description, dateContribution, statut, orgId
                ));
            }
        }

        return result;
    }
}
