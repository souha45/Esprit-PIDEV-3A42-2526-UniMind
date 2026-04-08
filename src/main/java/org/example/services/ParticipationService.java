package org.example.services;

import org.example.entities.Participation;
import org.example.enums.StatutParticipation;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements ICrud<Participation> {

    private final Connection connection;

    public ParticipationService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(Participation p) throws SQLException {
        System.out.println("Ajout d'une participation pour l'étudiant ID: " + p.getEtudiantId() + " à l'événement ID: " + p.getEvenementId());
        String sql = "INSERT INTO participation (date_inscription, statut, created_at, updated_at, evenement_id, etudiant_id, note_satisfaction, feedback_commentaire, feedback_at, qr_token, scanned_at, present) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, p.getDateInscription() != null ? p.getDateInscription() : new Timestamp(System.currentTimeMillis()));
            ps.setString(2, p.getStatut() != null ? p.getStatut().getDbValue() : null);
            ps.setTimestamp(3, p.getCreatedAt() != null ? p.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(4, p.getUpdatedAt());
            ps.setInt(5, p.getEvenementId());
            ps.setInt(6, p.getEtudiantId());

            if (p.getNoteSatisfaction() == null) {
                ps.setNull(7, Types.SMALLINT);
            } else {
                ps.setShort(7, p.getNoteSatisfaction());
            }

            ps.setString(8, p.getFeedbackCommentaire());
            ps.setTimestamp(9, p.getFeedbackAt());
            ps.setString(10, p.getQrToken());
            ps.setTimestamp(11, p.getScannedAt());

            if (p.getPresent() == null) {
                ps.setNull(12, Types.TINYINT);
            } else {
                ps.setBoolean(12, p.getPresent());
            }

            ps.executeUpdate();
            System.out.println("Participation ajoutée avec succès");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setParticipationId(rs.getInt(1));
                    System.out.println("ID généré pour la participation: " + p.getParticipationId());
                }
            }
        }
    }

    @Override
    public void modifier(Participation p) throws SQLException {
        System.out.println("Modification de la participation ID: " + p.getParticipationId());
        String sql = "UPDATE participation SET date_inscription=?, statut=?, updated_at=?, evenement_id=?, etudiant_id=?, note_satisfaction=?, feedback_commentaire=?, feedback_at=?, qr_token=?, scanned_at=?, present=? " +
                "WHERE participation_id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, p.getDateInscription());
            ps.setString(2, p.getStatut() != null ? p.getStatut().getDbValue() : null);
            ps.setTimestamp(3, p.getUpdatedAt() != null ? p.getUpdatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setInt(4, p.getEvenementId());
            ps.setInt(5, p.getEtudiantId());

            if (p.getNoteSatisfaction() == null) {
                ps.setNull(6, Types.SMALLINT);
            } else {
                ps.setShort(6, p.getNoteSatisfaction());
            }

            ps.setString(7, p.getFeedbackCommentaire());
            ps.setTimestamp(8, p.getFeedbackAt());
            ps.setString(9, p.getQrToken());
            ps.setTimestamp(10, p.getScannedAt());

            if (p.getPresent() == null) {
                ps.setNull(11, Types.TINYINT);
            } else {
                ps.setBoolean(11, p.getPresent());
            }

            ps.setInt(12, p.getParticipationId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM participation WHERE participation_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Participation> afficher() throws SQLException {
        String sql = "SELECT participation_id, date_inscription, statut, created_at, updated_at, evenement_id, etudiant_id, note_satisfaction, feedback_commentaire, feedback_at, qr_token, scanned_at, present FROM participation";
        List<Participation> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public Participation findById(int id) throws SQLException {
        String sql = "SELECT participation_id, date_inscription, statut, created_at, updated_at, evenement_id, etudiant_id, note_satisfaction, feedback_commentaire, feedback_at, qr_token, scanned_at, present FROM participation WHERE participation_id=?";
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

    private Participation mapRow(ResultSet rs) throws SQLException {
        int participationId = rs.getInt("participation_id");
        Timestamp dateInscription = rs.getTimestamp("date_inscription");
        StatutParticipation statut = StatutParticipation.fromDb(rs.getString("statut"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        int evenementId = rs.getInt("evenement_id");
        int etudiantId = rs.getInt("etudiant_id");

        Short noteSatisfaction = null;
        short note = rs.getShort("note_satisfaction");
        if (!rs.wasNull()) {
            noteSatisfaction = note;
        }

        String feedbackCommentaire = rs.getString("feedback_commentaire");
        Timestamp feedbackAt = rs.getTimestamp("feedback_at");
        String qrToken = rs.getString("qr_token");
        Timestamp scannedAt = rs.getTimestamp("scanned_at");

        Boolean present = null;
        boolean presentVal = rs.getBoolean("present");
        if (!rs.wasNull()) {
            present = presentVal;
        }

        return new Participation(
                participationId,
                dateInscription,
                statut,
                createdAt,
                updatedAt,
                evenementId,
                etudiantId,
                noteSatisfaction,
                feedbackCommentaire,
                feedbackAt,
                qrToken,
                scannedAt,
                present
        );
    }

}
