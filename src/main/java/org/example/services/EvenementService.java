package org.example.services;

import org.example.entities.Evenement;
import org.example.enums.StatutEvenement;
import org.example.enums.TypeEvenement;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements ICrud<Evenement> {

    private final Connection connection;

    public EvenementService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {
        System.out.println("Ajout d'un nouvel événement: " + e.getTitre());
        String sql = "INSERT INTO evenement (titre, description, type, date_debut, date_fin, lieu, capacite_max, nombre_inscrits, statut, date_creation, date_limite_inscription, organisateur_id, updated_at, image, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType() != null ? e.getType().getDbValue() : null);
            ps.setTimestamp(4, e.getDateDebut());
            ps.setTimestamp(5, e.getDateFin());
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getCapaciteMax());
            ps.setInt(8, e.getNombreInscrits());
            ps.setString(9, e.getStatut() != null ? e.getStatut().getDbValue() : null);
            ps.setTimestamp(10, e.getDateCreation() != null ? e.getDateCreation() : new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(11, e.getDateLimiteInscription());
            ps.setInt(12, e.getOrganisateurId());
            ps.setTimestamp(13, e.getUpdatedAt());
            ps.setString(14, e.getImage());
            if (e.getLatitude() == null) {
                ps.setNull(15, Types.DOUBLE);
            } else {
                ps.setDouble(15, e.getLatitude());
            }
            if (e.getLongitude() == null) {
                ps.setNull(16, Types.DOUBLE);
            } else {
                ps.setDouble(16, e.getLongitude());
            }

            ps.executeUpdate();
            System.out.println("Événement ajouté avec succès");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setEvenementId(rs.getInt(1));
                    System.out.println("ID généré pour l'événement: " + e.getEvenementId());
                }
            }
        }
    }

    @Override
    public void modifier(Evenement e) throws SQLException {
        System.out.println("Modification de l'événement ID: " + e.getEvenementId());
        String sql = "UPDATE evenement SET titre=?, description=?, type=?, date_debut=?, date_fin=?, lieu=?, capacite_max=?, nombre_inscrits=?, statut=?, date_limite_inscription=?, organisateur_id=?, updated_at=?, image=?, latitude=?, longitude=? " +
                "WHERE evenement_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType() != null ? e.getType().getDbValue() : null);
            ps.setTimestamp(4, e.getDateDebut());
            ps.setTimestamp(5, e.getDateFin());
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getCapaciteMax());
            ps.setInt(8, e.getNombreInscrits());
            ps.setString(9, e.getStatut() != null ? e.getStatut().getDbValue() : null);
            ps.setTimestamp(10, e.getDateLimiteInscription());
            ps.setInt(11, e.getOrganisateurId());
            ps.setTimestamp(12, e.getUpdatedAt() != null ? e.getUpdatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setString(13, e.getImage());
            if (e.getLatitude() == null) {
                ps.setNull(14, Types.DOUBLE);
            } else {
                ps.setDouble(14, e.getLatitude());
            }
            if (e.getLongitude() == null) {
                ps.setNull(15, Types.DOUBLE);
            } else {
                ps.setDouble(15, e.getLongitude());
            }
            ps.setInt(16, e.getEvenementId());
            ps.executeUpdate();
            System.out.println("Événement ID " + e.getEvenementId() + " modifié avec succès");
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        System.out.println("Suppression de l'événement ID: " + id);
        String sql = "DELETE FROM evenement WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Événement ID " + id + " supprimé avec succès");
            } else {
                System.out.println("Aucun événement trouvé avec l'ID: " + id);
            }
        }
    }

    @Override
    public List<Evenement> afficher() throws SQLException {
        System.out.println("Récupération de tous les événements...");
        String sql = "SELECT evenement_id, titre, description, type, date_debut, date_fin, lieu, capacite_max, nombre_inscrits, statut, date_creation, date_limite_inscription, organisateur_id, updated_at, image, latitude, longitude FROM evenement";

        List<Evenement> result = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        System.out.println(result.size() + " événement(s) trouvé(s)");
        return result;
    }

    public Evenement findById(int id) throws SQLException {
        String sql = "SELECT evenement_id, titre, description, type, date_debut, date_fin, lieu, capacite_max, nombre_inscrits, statut, date_creation, date_limite_inscription, organisateur_id, updated_at, image, latitude, longitude FROM evenement WHERE evenement_id=?";
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

    private Evenement mapRow(ResultSet rs) throws SQLException {
        int evenementId = rs.getInt("evenement_id");
        String titre = rs.getString("titre");
        String description = rs.getString("description");
        TypeEvenement type = TypeEvenement.fromDb(rs.getString("type"));
        Timestamp dateDebut = rs.getTimestamp("date_debut");
        Timestamp dateFin = rs.getTimestamp("date_fin");
        String lieu = rs.getString("lieu");
        int capaciteMax = rs.getInt("capacite_max");
        int nombreInscrits = rs.getInt("nombre_inscrits");
        StatutEvenement statut = StatutEvenement.fromDb(rs.getString("statut"));
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        Timestamp dateLimiteInscription = rs.getTimestamp("date_limite_inscription");
        int organisateurId = rs.getInt("organisateur_id");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        String image = rs.getString("image");

        Double latitude = null;
        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) {
            latitude = lat;
        }
        Double longitude = null;
        double lon = rs.getDouble("longitude");
        if (!rs.wasNull()) {
            longitude = lon;
        }

        return new Evenement(
                evenementId,
                titre,
                description,
                type,
                dateDebut,
                dateFin,
                lieu,
                capaciteMax,
                nombreInscrits,
                statut,
                dateCreation,
                dateLimiteInscription,
                organisateurId,
                updatedAt,
                image,
                latitude,
                longitude
        );
    }

}
