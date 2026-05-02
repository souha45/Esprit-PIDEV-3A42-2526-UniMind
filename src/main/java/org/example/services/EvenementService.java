package org.example.services;

import org.example.entities.Evenement;
import org.example.enums.StatutEvenement;
import org.example.enums.TypeEvenement;
import org.example.services.evenement.EventEmailService;
import org.example.utils.MyDataBase_Unimind;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EvenementService implements ICrud<Evenement> {

    private final Connection connection;
    private final EventEmailService emailService;
    private ParticipationService participationService;

    public EvenementService() {
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

    private ParticipationService getParticipationService() {
        if (participationService == null) {
            participationService = new ParticipationService();
        }
        return participationService;
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {
        System.out.println("Ajout d'un nouvel evenement: " + e.getTitre());
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
            System.out.println("Evenement ajoute avec succes");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setEvenementId(rs.getInt(1));
                    System.out.println("ID genere pour l'evenement: " + e.getEvenementId());
                }
            }
        }

        // Envoyer l'email de notification à tous les étudiants
        sendNewEventNotification(e);
    }

    @Override
    public void modifier(Evenement e) throws SQLException {
        System.out.println("Modification de l'evenement ID: " + e.getEvenementId());

        // Récupérer l'ancien événement avant modification
        Evenement oldEvent = findById(e.getEvenementId());
        StatutEvenement oldStatut = oldEvent != null ? oldEvent.getStatut() : null;

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
            System.out.println("Evenement ID " + e.getEvenementId() + " modifie avec succes");
        }

        // Envoyer les emails aux participants si le statut a changé
        if (oldStatut != null && e.getStatut() != null) {
            if (oldStatut != e.getStatut()) {
                // Le statut a changé
                if (e.getStatut() == StatutEvenement.ANNULE) {
                    // Événement annulé
                    sendEventCancellationEmails(e.getEvenementId(), e.getTitre());
                }
            } else if (e.getStatut() != StatutEvenement.ANNULE) {
                // Événement modifié (mais pas annulé)
                sendEventModificationEmails(e.getEvenementId(), e.getTitre(), e.getDateDebut(), e.getLieu(), oldEvent, e);
            }
        }
    }

    /**
     * Envoyer l'email de modification d'événement à tous les participants
     */
    private void sendEventModificationEmails(int evenementId, String eventTitle, Timestamp eventDate, String eventLocation, Evenement oldEvent, Evenement newEvent) {
        try {
            java.util.List<ParticipationService.ParticipantInfo> participants = getParticipationService().getParticipantsByEvenementId(evenementId);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String formattedDate = eventDate != null ? sdf.format(eventDate) : "Non spécifié";
            String formattedLocation = eventLocation != null ? eventLocation : "Non spécifié";

            // Détecter les changements
            String changes = detectChanges(oldEvent, newEvent);

            for (ParticipationService.ParticipantInfo participant : participants) {
                String participantName = participant.getPrenom() + " " + participant.getNom();
                emailService.sendEventModificationEmail(
                    participant.getEmail(),
                    participantName,
                    eventTitle,
                    formattedDate,
                    formattedLocation,
                    changes
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'envoi des emails de modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Détecter les changements entre l'ancien et le nouvel événement
     */
    private String detectChanges(Evenement oldEvent, Evenement newEvent) {
        StringBuilder changes = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // Comparer le titre
        if (!equals(oldEvent.getTitre(), newEvent.getTitre())) {
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Titre :</strong> de ").append(oldEvent.getTitre()).append(" à ").append(newEvent.getTitre());
            changes.append("</div>");
        }

        // Comparer la description
        if (!equals(oldEvent.getDescription(), newEvent.getDescription())) {
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Description :</strong> modifiée");
            changes.append("</div>");
        }

        // Comparer la date de début
        if (!equals(oldEvent.getDateDebut(), newEvent.getDateDebut())) {
            String oldDate = oldEvent.getDateDebut() != null ? sdf.format(oldEvent.getDateDebut()) : "Non spécifié";
            String newDate = newEvent.getDateDebut() != null ? sdf.format(newEvent.getDateDebut()) : "Non spécifié";
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Date de début :</strong> de ").append(oldDate).append(" à ").append(newDate);
            changes.append("</div>");
        }

        // Comparer la date de fin
        if (!equals(oldEvent.getDateFin(), newEvent.getDateFin())) {
            String oldDate = oldEvent.getDateFin() != null ? sdf.format(oldEvent.getDateFin()) : "Non spécifié";
            String newDate = newEvent.getDateFin() != null ? sdf.format(newEvent.getDateFin()) : "Non spécifié";
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Date de fin :</strong> de ").append(oldDate).append(" à ").append(newDate);
            changes.append("</div>");
        }

        // Comparer le lieu
        if (!equals(oldEvent.getLieu(), newEvent.getLieu())) {
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Lieu :</strong> de ").append(oldEvent.getLieu()).append(" à ").append(newEvent.getLieu());
            changes.append("</div>");
        }

        // Comparer la capacité
        if (oldEvent.getCapaciteMax() != newEvent.getCapaciteMax()) {
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Capacité :</strong> de ").append(oldEvent.getCapaciteMax()).append(" à ").append(newEvent.getCapaciteMax());
            changes.append("</div>");
        }

        // Comparer la date limite d'inscription
        if (!equals(oldEvent.getDateLimiteInscription(), newEvent.getDateLimiteInscription())) {
            String oldDate = oldEvent.getDateLimiteInscription() != null ? sdf.format(oldEvent.getDateLimiteInscription()) : "Non spécifié";
            String newDate = newEvent.getDateLimiteInscription() != null ? sdf.format(newEvent.getDateLimiteInscription()) : "Non spécifié";
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Date limite d'inscription :</strong> de ").append(oldDate).append(" à ").append(newDate);
            changes.append("</div>");
        }

        // Comparer le type
        if (!equals(oldEvent.getType(), newEvent.getType())) {
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Type :</strong> de ").append(oldEvent.getType()).append(" à ").append(newEvent.getType());
            changes.append("</div>");
        }

        // Comparer les coordonnées
        if (!equals(oldEvent.getLatitude(), newEvent.getLatitude()) || !equals(oldEvent.getLongitude(), newEvent.getLongitude())) {
            changes.append("<div class='changes-item'>");
            changes.append("<strong>Coordonnées :</strong> modifiées");
            changes.append("</div>");
        }

        if (changes.length() == 0) {
            changes.append("<div class='changes-item'>Aucun changement détecté</div>");
        }

        return changes.toString();
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Envoyer l'email d'annulation d'événement à tous les participants
     */
    private void sendEventCancellationEmails(int evenementId, String eventTitle) {
        try {
            java.util.List<ParticipationService.ParticipantInfo> participants = getParticipationService().getParticipantsByEvenementId(evenementId);

            for (ParticipationService.ParticipantInfo participant : participants) {
                String participantName = participant.getPrenom() + " " + participant.getNom();
                emailService.sendEventCancellationEmail(
                    participant.getEmail(),
                    participantName,
                    eventTitle
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'envoi des emails d'annulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoyer l'email de notification de nouvel événement à tous les étudiants
     */
    private void sendNewEventNotification(Evenement event) {
        try {
            // Récupérer tous les étudiants
            String sql = "SELECT user_id, email, prenom, nom FROM user WHERE role = 'Etudiant'";
            java.util.List<StudentInfo> students = new java.util.ArrayList<>();

            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudentInfo student = new StudentInfo();
                    student.setUserId(rs.getInt("user_id"));
                    student.setEmail(rs.getString("email"));
                    student.setPrenom(rs.getString("prenom"));
                    student.setNom(rs.getString("nom"));
                    students.add(student);
                }
            }

            // Formater les détails de l'événement
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String formattedDate = event.getDateDebut() != null ? sdf.format(event.getDateDebut()) : "Non spécifié";
            String formattedLocation = event.getLieu() != null ? event.getLieu() : "Non spécifié";
            String description = event.getDescription() != null ? event.getDescription() : "";

            // Envoyer l'email à chaque étudiant
            for (StudentInfo student : students) {
                String studentName = student.getPrenom() + " " + student.getNom();
                emailService.sendNewEventEmail(
                    student.getEmail(),
                    studentName,
                    event.getTitre(),
                    formattedDate,
                    formattedLocation,
                    description
                );
                System.out.println("Email de nouvel événement envoyé à: " + student.getEmail());
            }
            System.out.println("Emails de notification envoyés à " + students.size() + " étudiant(s)");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'envoi des emails de notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Classe interne pour stocker les informations d'un étudiant
     */
    private static class StudentInfo {
        private int userId;
        private String email;
        private String prenom;
        private String nom;

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        System.out.println("Suppression de l'evenement ID: " + id + " avec cascade delete");

        // Supprimer d'abord les participations liées à l'événement
        String sqlParticipations = "DELETE FROM participation WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlParticipations)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Participations liees a l'evenement ID " + id + " supprimees");
        }

        // Supprimer les favoris liés à l'événement
        String sqlFavoris = "DELETE FROM favori WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlFavoris)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Favoris lies a l'evenement ID " + id + " supprimes");
        }

        // Supprimer les attributions sponsors liées à l'événement
        String sqlAttributions = "DELETE FROM evenement_sponsor WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlAttributions)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Attributions sponsors liees a l'evenement ID " + id + " supprimees");
        }

        // Supprimer l'événement lui-même
        String sqlEvenement = "DELETE FROM evenement WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sqlEvenement)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Evenement ID " + id + " supprime avec succes (cascade delete effectue)");
            } else {
                System.out.println("Aucun evenement trouve avec l'ID: " + id);
            }
        }
    }

    @Override
    public List<Evenement> afficher() throws SQLException {
        System.out.println("Recuperation de tous les evenements...");
        String sql = "SELECT evenement_id, titre, description, type, date_debut, date_fin, lieu, capacite_max, nombre_inscrits, statut, date_creation, date_limite_inscription, organisateur_id, updated_at, image, latitude, longitude FROM evenement";

        List<Evenement> result = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        System.out.println(result.size() + " evenement(s) trouve(s)");
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

    /**
     * Vérifie si un événement avec le même titre et date de début existe déjà
     * @param titre Le titre de l'événement
     * @param dateDebut La date de début de l'événement
     * @param excludeId L'ID de l'événement à exclure de la vérification (pour modification), ou null pour l'ajout
     * @return true si un doublon existe, false sinon
     */
    public boolean verifierUnicite(String titre, Timestamp dateDebut, Integer excludeId) throws SQLException {
        String sql;
        if (excludeId != null) {
            sql = "SELECT COUNT(*) FROM evenement WHERE titre = ? AND date_debut = ? AND evenement_id != ?";
        } else {
            sql = "SELECT COUNT(*) FROM evenement WHERE titre = ? AND date_debut = ?";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setTimestamp(2, dateDebut);
            if (excludeId != null) {
                ps.setInt(3, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Récupère le nom de l'organisateur à partir de son ID
     * @param organisateurId L'ID de l'organisateur
     * @return Le nom complet de l'organisateur ou "Non assigné" si non trouvé
     */
    public String getNomOrganisateur(int organisateurId) throws SQLException {
        String sql = "SELECT nom, prenom FROM user WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, organisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    if (nom != null && prenom != null) {
                        return prenom + " " + nom;
                    } else if (nom != null) {
                        return nom;
                    } else if (prenom != null) {
                        return prenom;
                    }
                }
            }
        }
        return "Non assigné";
    }

    /**
     * Compte le nombre total d'événements
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Classe interne pour stocker un événement avec le nom de l'organisateur
     */
    public static class EvenementAvecOrganisateurNom {
        private final Evenement evenement;
        private final String organisateurNom;
        private int placesLibres;

        public EvenementAvecOrganisateurNom(Evenement evenement, String organisateurNom) {
            this.evenement = evenement;
            this.organisateurNom = organisateurNom;
            this.placesLibres = -1; // -1 signifie illimité ou non calculé
        }

        public Evenement getEvenement() {
            return evenement;
        }

        public String getOrganisateurNom() {
            return organisateurNom;
        }

        public int getPlacesLibres() {
            return placesLibres;
        }

        public void setPlacesLibres(int placesLibres) {
            this.placesLibres = placesLibres;
        }

        // Getters pour PropertyValueFactory
        public String getTitre() {
            return evenement.getTitre();
        }

        public String getLieu() {
            return evenement.getLieu();
        }
    }

    /**
     * Met à jour automatiquement les images des événements en faisant correspondre
     * les titres avec les noms de fichiers images
     */
    public void mettreAJourImagesAutomatiquement() throws SQLException {
        // Récupérer tous les événements
        List<Evenement> evenements = afficher();

        // Liste des images disponibles dans le dossier XAMPP
        java.io.File dossierImages = new java.io.File("D:\\xampp\\htdocs\\uploadsEvent\\evenements");
        java.io.File[] fichiersImages = dossierImages.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") ||
            name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".gif"));

        if (fichiersImages == null) {
            System.out.println("Aucune image trouvee dans le dossier XAMPP");
            return;
        }

        int misesAJour = 0;
        for (Evenement e : evenements) {
            String titreEvenement = e.getTitre().toLowerCase();
            String nomImageTrouvee = null;

            // Chercher une image qui correspond au titre de l'événement
            for (java.io.File fichierImage : fichiersImages) {
                String nomImage = fichierImage.getName().toLowerCase();

                // Correspondance exacte du titre dans le nom du fichier
                if (nomImage.contains(titreEvenement.replace(" ", "-")) ||
                    nomImage.contains(titreEvenement.replace(" ", "_")) ||
                    nomImage.contains(titreEvenement.replace(" ", " "))) {
                    nomImageTrouvee = fichierImage.getName();
                    break;
                }
            }

            // Si une image correspondante est trouvée et que l'événement n'a pas d'image
            if (nomImageTrouvee != null && (e.getImage() == null || e.getImage().trim().isEmpty())) {
                String sql = "UPDATE evenement SET image = ? WHERE evenement_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, nomImageTrouvee);
                    ps.setInt(2, e.getEvenementId());
                    ps.executeUpdate();
                    System.out.println("Image mise a jour pour : " + e.getTitre() + " -> " + nomImageTrouvee);
                    misesAJour++;
                }
            }
        }

        System.out.println("Mise a jour terminee. " + misesAJour + " evenements mis a jour.");
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
