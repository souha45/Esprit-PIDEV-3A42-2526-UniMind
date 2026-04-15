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
    }

    @Override
    public void modifier(Evenement e) throws SQLException {
        System.out.println("Modification de l'evenement ID: " + e.getEvenementId());
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
