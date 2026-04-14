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
        System.out.println("Ajout d'une participation pour l'etudiant ID: " + p.getEtudiantId() + " a l'evenement ID: " + p.getEvenementId());
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
            System.out.println("Participation ajoutee avec succes");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setParticipationId(rs.getInt(1));
                    System.out.println("ID genere pour la participation: " + p.getParticipationId());
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

    /**
     * Vérifie si une participation avec le même evenementId et etudiantId existe déjà
     * @param evenementId L'ID de l'événement
     * @param etudiantId L'ID de l'étudiant
     * @return true si une participation existe déjà, false sinon
     */
    public boolean verifierUnicite(int evenementId, int etudiantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM participation WHERE evenement_id = ? AND etudiant_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ps.setInt(2, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Récupère les participations avec les noms des événements et étudiants
     * @return Liste d'objets ParticipationAvecNoms
     */
    public java.util.List<ParticipationAvecNoms> afficherAvecNoms() throws SQLException {
        String sql = "SELECT p.participation_id, p.date_inscription, p.statut, p.created_at, p.updated_at, " +
                     "p.evenement_id, p.etudiant_id, p.note_satisfaction, p.feedback_commentaire, p.feedback_at, " +
                     "p.qr_token, p.scanned_at, p.present, " +
                     "e.titre as evenement_titre, e.lieu as evenement_lieu, e.date_debut as evenement_date_debut, e.organisateur_id, " +
                     "u.prenom as etudiant_prenom, u.nom as etudiant_nom, " +
                     "org.prenom as organisateur_prenom, org.nom as organisateur_nom " +
                     "FROM participation p " +
                     "LEFT JOIN evenement e ON p.evenement_id = e.evenement_id " +
                     "LEFT JOIN user u ON p.etudiant_id = u.user_id " +
                     "LEFT JOIN user org ON e.organisateur_id = org.user_id";
        java.util.List<ParticipationAvecNoms> result = new java.util.ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ParticipationAvecNoms part = new ParticipationAvecNoms();
                part.setParticipationId(rs.getInt("participation_id"));
                part.setDateInscription(rs.getTimestamp("date_inscription"));
                part.setStatut(StatutParticipation.fromDb(rs.getString("statut")));
                part.setCreatedAt(rs.getTimestamp("created_at"));
                part.setUpdatedAt(rs.getTimestamp("updated_at"));
                part.setEvenementId(rs.getInt("evenement_id"));
                part.setEtudiantId(rs.getInt("etudiant_id"));
                part.setOrganisateurId(rs.getInt("organisateur_id"));

                Short noteSatisfaction = null;
                short note = rs.getShort("note_satisfaction");
                if (!rs.wasNull()) {
                    noteSatisfaction = note;
                }
                part.setNoteSatisfaction(noteSatisfaction);

                part.setFeedbackCommentaire(rs.getString("feedback_commentaire"));
                part.setFeedbackAt(rs.getTimestamp("feedback_at"));
                part.setQrToken(rs.getString("qr_token"));
                part.setScannedAt(rs.getTimestamp("scanned_at"));

                Boolean present = null;
                boolean presentVal = rs.getBoolean("present");
                if (!rs.wasNull()) {
                    present = presentVal;
                }
                part.setPresent(present);

                part.setEvenementTitre(rs.getString("evenement_titre"));
                part.setEvenementLieu(rs.getString("evenement_lieu"));
                part.setEvenementDateDebut(rs.getTimestamp("evenement_date_debut"));
                part.setEtudiantNom(rs.getString("etudiant_prenom") + " " + rs.getString("etudiant_nom"));

                String organisateurPrenom = rs.getString("organisateur_prenom");
                String organisateurNom = rs.getString("organisateur_nom");
                String organisateurNomComplet = (organisateurPrenom != null ? organisateurPrenom + " " : "") + (organisateurNom != null ? organisateurNom : "");
                part.setOrganisateurNom(organisateurNomComplet.trim());

                result.add(part);
            }
        }
        return result;
    }

    /**
     * Classe interne pour représenter une participation avec les noms
     */
    public static class ParticipationAvecNoms {
        private int participationId;
        private Timestamp dateInscription;
        private StatutParticipation statut;
        private Timestamp createdAt;
        private Timestamp updatedAt;
        private int evenementId;
        private int etudiantId;
        private int organisateurId;
        private Short noteSatisfaction;
        private String feedbackCommentaire;
        private Timestamp feedbackAt;
        private String qrToken;
        private Timestamp scannedAt;
        private Boolean present;
        private String evenementTitre;
        private String etudiantNom;
        private String organisateurNom;
        private String evenementLieu;
        private Timestamp evenementDateDebut;

        public int getParticipationId() { return participationId; }
        public void setParticipationId(int participationId) { this.participationId = participationId; }
        public Timestamp getDateInscription() { return dateInscription; }
        public void setDateInscription(Timestamp dateInscription) { this.dateInscription = dateInscription; }
        public StatutParticipation getStatut() { return statut; }
        public void setStatut(StatutParticipation statut) { this.statut = statut; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public Timestamp getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
        public int getEvenementId() { return evenementId; }
        public void setEvenementId(int evenementId) { this.evenementId = evenementId; }
        public int getEtudiantId() { return etudiantId; }
        public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }
        public int getOrganisateurId() { return organisateurId; }
        public void setOrganisateurId(int organisateurId) { this.organisateurId = organisateurId; }
        public Short getNoteSatisfaction() { return noteSatisfaction; }
        public void setNoteSatisfaction(Short noteSatisfaction) { this.noteSatisfaction = noteSatisfaction; }
        public String getFeedbackCommentaire() { return feedbackCommentaire; }
        public void setFeedbackCommentaire(String feedbackCommentaire) { this.feedbackCommentaire = feedbackCommentaire; }
        public Timestamp getFeedbackAt() { return feedbackAt; }
        public void setFeedbackAt(Timestamp feedbackAt) { this.feedbackAt = feedbackAt; }
        public String getQrToken() { return qrToken; }
        public void setQrToken(String qrToken) { this.qrToken = qrToken; }
        public Timestamp getScannedAt() { return scannedAt; }
        public void setScannedAt(Timestamp scannedAt) { this.scannedAt = scannedAt; }
        public Boolean getPresent() { return present; }
        public void setPresent(Boolean present) { this.present = present; }
        public boolean isPresent() { return present != null && present; }
        public String getEvenementTitre() { return evenementTitre; }
        public void setEvenementTitre(String evenementTitre) { this.evenementTitre = evenementTitre; }
        public String getEtudiantNom() { return etudiantNom; }
        public void setEtudiantNom(String etudiantNom) { this.etudiantNom = etudiantNom; }
        public String getOrganisateurNom() { return organisateurNom; }
        public void setOrganisateurNom(String organisateurNom) { this.organisateurNom = organisateurNom; }
        public String getEvenementLieu() { return evenementLieu; }
        public void setEvenementLieu(String evenementLieu) { this.evenementLieu = evenementLieu; }
        public Timestamp getEvenementDateDebut() { return evenementDateDebut; }
        public void setEvenementDateDebut(Timestamp evenementDateDebut) { this.evenementDateDebut = evenementDateDebut; }
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
