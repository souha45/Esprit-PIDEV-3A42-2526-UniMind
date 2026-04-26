package org.example.services;

import org.example.entities.Participation;
import org.example.entities.Evenement;
import org.example.enums.StatutParticipation;
import org.example.services.evenement.EventEmailService;
import org.example.utils.MyDataBase_Unimind;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ParticipationService implements ICrud<Participation> {

    private final Connection connection;
    private final EventEmailService emailService;
    private EvenementService evenementService;

    public ParticipationService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
        this.emailService = new EventEmailService();

        // Chargement des credentials email depuis event-config.properties
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("event-config.properties")) {
            if (is != null) {
                props.load(is);
                this.emailService.setUsername(props.getProperty("mail.username", ""));
                this.emailService.setPassword(props.getProperty("mail.password", ""));
            }
        } catch (IOException e) {
            System.err.println("Impossible de charger event-config.properties : " + e.getMessage());
        }
    }

    private EvenementService getEvenementService() {
        if (evenementService == null) {
            evenementService = new EvenementService();
        }
        return evenementService;
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

            // Envoyer l'email de confirmation d'inscription
            sendInscriptionEmail(p.getEtudiantId(), p.getEvenementId());
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
        // Récupérer les détails de la participation avant suppression pour l'email
        String selectSql = "SELECT etudiant_id, evenement_id FROM participation WHERE participation_id=?";
        int etudiantId = -1;
        int evenementId = -1;

        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    etudiantId = rs.getInt("etudiant_id");
                    evenementId = rs.getInt("evenement_id");
                }
            }
        }

        // Supprimer la participation
        String sql = "DELETE FROM participation WHERE participation_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        // Envoyer l'email de confirmation d'annulation
        if (etudiantId != -1 && evenementId != -1) {
            sendAnnulationEmail(etudiantId, evenementId);
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
                     "e.titre as evenement_titre, e.lieu as evenement_lieu, e.date_debut as evenement_date_debut, e.organisateur_id, e.capacite_max, " +
                     "(SELECT COUNT(*) FROM participation p2 WHERE p2.evenement_id = e.evenement_id) as nombre_inscrits, " +
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

                // Capacité maximale et places libres
                int capaciteMax = rs.getInt("capacite_max");
                int nombreInscrits = rs.getInt("nombre_inscrits");
                part.setCapaciteMax(capaciteMax);
                int placesLibres = capaciteMax > 0 ? capaciteMax - nombreInscrits : -1; // -1 signifie illimité
                part.setPlacesLibres(placesLibres);

                result.add(part);
            }
        }
        return result;
    }

    /**
     * Récupérer l'email d'un utilisateur par son ID
     */
    private String getUserEmail(int userId) throws SQLException {
        String sql = "SELECT email FROM user WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        return null;
    }

    /**
     * Récupérer le nom complet d'un utilisateur par son ID
     */
    private String getUserName(int userId) throws SQLException {
        String sql = "SELECT prenom, nom FROM user WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("prenom") + " " + rs.getString("nom");
                }
            }
        }
        return null;
    }

    /**
     * Récupérer tous les participants d'un événement
     */
    public java.util.List<ParticipantInfo> getParticipantsByEvenementId(int evenementId) throws SQLException {
        String sql = "SELECT p.etudiant_id, u.email, u.prenom, u.nom, p.date_inscription " +
                     "FROM participation p " +
                     "LEFT JOIN user u ON p.etudiant_id = u.user_id " +
                     "WHERE p.evenement_id = ?";
        java.util.List<ParticipantInfo> result = new java.util.ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParticipantInfo info = new ParticipantInfo();
                    info.setEtudiantId(rs.getInt("etudiant_id"));
                    info.setEmail(rs.getString("email"));
                    info.setPrenom(rs.getString("prenom"));
                    info.setNom(rs.getString("nom"));
                    info.setDateInscription(rs.getTimestamp("date_inscription"));
                    result.add(info);
                }
            }
        }
        return result;
    }

    /**
     * Classe interne pour les infos participant
     */
    public static class ParticipantInfo {
        private int etudiantId;
        private String email;
        private String prenom;
        private String nom;
        private java.sql.Timestamp dateInscription;

        public int getEtudiantId() { return etudiantId; }
        public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public java.sql.Timestamp getDateInscription() { return dateInscription; }
        public void setDateInscription(java.sql.Timestamp dateInscription) { this.dateInscription = dateInscription; }
    }

    /**
     * Récupérer les détails de l'événement pour l'email
     */
    private Evenement getEvenementDetails(int evenementId) throws SQLException {
        return getEvenementService().findById(evenementId);
    }

    /**
     * Envoyer l'email de confirmation d'inscription
     */
    private void sendInscriptionEmail(int etudiantId, int evenementId) {
        try {
            String email = getUserEmail(etudiantId);
            String participantName = getUserName(etudiantId);
            Evenement evenement = getEvenementDetails(evenementId);

            if (email != null && participantName != null && evenement != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String eventDate = evenement.getDateDebut() != null ? sdf.format(evenement.getDateDebut()) : "Non spécifié";
                String eventLocation = evenement.getLieu() != null ? evenement.getLieu() : "Non spécifié";

                emailService.sendInscriptionConfirmation(
                    email,
                    participantName,
                    evenement.getTitre(),
                    eventDate,
                    eventLocation
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'envoi de l'email d'inscription: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoyer l'email de confirmation d'annulation
     */
    private void sendAnnulationEmail(int etudiantId, int evenementId) {
        try {
            String email = getUserEmail(etudiantId);
            String participantName = getUserName(etudiantId);
            Evenement evenement = getEvenementDetails(evenementId);

            if (email != null && participantName != null && evenement != null) {
                emailService.sendAnnulationConfirmation(
                    email,
                    participantName,
                    evenement.getTitre()
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'envoi de l'email d'annulation: " + e.getMessage());
            e.printStackTrace();
        }
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
        private int capaciteMax;
        private int placesLibres;

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
        public int getCapaciteMax() { return capaciteMax; }
        public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }
        public int getPlacesLibres() { return placesLibres; }
        public void setPlacesLibres(int placesLibres) { this.placesLibres = placesLibres; }
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
