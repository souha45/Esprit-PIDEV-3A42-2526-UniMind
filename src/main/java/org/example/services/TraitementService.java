package org.example.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.example.entities.Traitement;
import org.example.enums.PrioriteTraitement;
import org.example.enums.StatutTraitement;
import org.example.utils.MyDataBase_Unimind;

public class TraitementService implements ICrud<Traitement> {

    public boolean traitementExisteDeja(String titre, int etudiantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM traitement WHERE titre = ? AND etudiant_id = ?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, titre.trim());
            ps.setInt(2, etudiantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    @Override
    public void ajouter(Traitement t) throws SQLException {
        String sql = "INSERT INTO traitement (titre, description, type, categorie, duree_jours, dosage, date_debut, date_fin, statut, priorite, objectif_therapeutique, created_at, updated_at, psychologue_id, etudiant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?)";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getType());
            // Categorie : convertir en minuscule pour Symfony
            ps.setString(4, t.getCategorie().name().toLowerCase());
            ps.setInt(5, t.getDureeJours());
            ps.setString(6, t.getDosage());
            ps.setDate(7, t.getDateDebut());
            ps.setDate(8, t.getDateFin());
            // Statut : convertir en format Symfony ("en cours")
            ps.setString(9, convertirStatutPourSymfony(t.getStatut()));
            // Priorite : convertir en minuscule pour Symfony
            ps.setString(10, t.getPriorite().name().toLowerCase());
            ps.setString(11, t.getObjectifTherapeutique());
            if (t.getPsychologueId() == null) ps.setNull(12, Types.INTEGER); else ps.setInt(12, t.getPsychologueId());
            ps.setInt(13, t.getEtudiantId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Traitement t) throws SQLException {
        String sql = "UPDATE traitement SET titre=?, description=?, type=?, categorie=?, duree_jours=?, dosage=?, date_debut=?, date_fin=?, statut=?, priorite=?, objectif_therapeutique=?, updated_at=NOW(), psychologue_id=?, etudiant_id=? WHERE traitement_id=?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getTitre());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getType());
            // Categorie : convertir en minuscule pour Symfony
            ps.setString(4, t.getCategorie().name().toLowerCase());
            ps.setInt(5, t.getDureeJours());
            ps.setString(6, t.getDosage());
            ps.setDate(7, t.getDateDebut());
            ps.setDate(8, t.getDateFin());
            // Statut : convertir en format Symfony ("en cours")
            ps.setString(9, convertirStatutPourSymfony(t.getStatut()));
            // Priorite : convertir en minuscule pour Symfony
            ps.setString(10, t.getPriorite().name().toLowerCase());
            ps.setString(11, t.getObjectifTherapeutique());
            if (t.getPsychologueId() == null) ps.setNull(12, Types.INTEGER); else ps.setInt(12, t.getPsychologueId());
            ps.setInt(13, t.getEtudiantId());
            ps.setInt(14, t.getTraitementId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        Connection cnx = null;
        boolean autoCommit = false;

        try {
            cnx = MyDataBase_Unimind.getInstance().getConnection();
            autoCommit = cnx.getAutoCommit();
            cnx.setAutoCommit(false);

            // 1. Supprimer les suivis liés
            String deleteSuivisSql = "DELETE FROM suivi_traitement WHERE traitement_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(deleteSuivisSql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2. Supprimer le traitement
            String deleteTraitementSql = "DELETE FROM traitement WHERE traitement_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(deleteTraitementSql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            cnx.commit();

        } catch (SQLException e) {
            if (cnx != null) {
                try {
                    cnx.rollback();
                } catch (SQLException ex) {
                    System.err.println("Erreur rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (cnx != null) {
                cnx.setAutoCommit(autoCommit);
            }
        }
    }

    @Override
    public List<Traitement> afficher() throws SQLException {
        String sql = "SELECT * FROM traitement";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Traitement> result = new ArrayList<>();
            while (rs.next()) {
                Traitement t = new Traitement();
                t.setTraitementId(rs.getInt("traitement_id"));
                t.setTitre(rs.getString("titre"));
                t.setDescription(rs.getString("description"));
                t.setType(rs.getString("type"));

                // Categorie : lire la valeur (minuscule) et convertir en majuscule pour JavaFX
                String categorieStr = rs.getString("categorie");
                if (categorieStr != null) {
                    try {
                        t.setCategorie(org.example.enums.CategorieTraitement.valueOf(categorieStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        t.setCategorie(org.example.enums.CategorieTraitement.COGNITIF);
                    }
                }

                t.setDureeJours(rs.getInt("duree_jours"));
                t.setDosage(rs.getString("dosage"));
                t.setDateDebut(rs.getDate("date_debut"));
                t.setDateFin(rs.getDate("date_fin"));

                // Statut : lire la valeur Symfony et convertir en JavaFX
                String statutStr = rs.getString("statut");
                if (statutStr != null) {
                    t.setStatut(convertirStatutDeSymfony(statutStr));
                } else {
                    t.setStatut(StatutTraitement.EN_COURS);
                }

                // Priorite : lire la valeur (minuscule) et convertir en majuscule pour JavaFX
                String prioriteStr = rs.getString("priorite");
                if (prioriteStr != null) {
                    try {
                        t.setPriorite(PrioriteTraitement.valueOf(prioriteStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        t.setPriorite(PrioriteTraitement.MOYENNE);
                    }
                }

                t.setObjectifTherapeutique(rs.getString("objectif_therapeutique"));
                t.setCreatedAt(rs.getTimestamp("created_at"));
                t.setUpdatedAt(rs.getTimestamp("updated_at"));
                t.setPsychologueId(rs.getObject("psychologue_id") != null ? rs.getInt("psychologue_id") : null);
                t.setEtudiantId(rs.getInt("etudiant_id"));
                result.add(t);
            }
            return result;
        }
    }

    /**
     * Convertit un StatutTraitement JavaFX vers le format Symfony
     * EN_COURS → "en cours"
     * TERMINE → "termine"
     * SUSPENDU → "suspendu"
     */
    private String convertirStatutPourSymfony(StatutTraitement statut) {
        switch (statut) {
            case EN_COURS:
                return "en cours";
            case TERMINE:
                return "termine";
            case SUSPENDU:
                return "suspendu";
            default:
                return "en cours";
        }
    }

    /**
     * Convertit une chaîne Symfony vers StatutTraitement JavaFX
     * "en cours" → EN_COURS
     * "termine" → TERMINE
     * "suspendu" → SUSPENDU
     */
    private StatutTraitement convertirStatutDeSymfony(String valeur) {
        if (valeur == null) return StatutTraitement.EN_COURS;

        switch (valeur.toLowerCase()) {
            case "en cours":
                return StatutTraitement.EN_COURS;
            case "termine":
                return StatutTraitement.TERMINE;
            case "suspendu":
                return StatutTraitement.SUSPENDU;
            default:
                return StatutTraitement.EN_COURS;
        }
    }
}