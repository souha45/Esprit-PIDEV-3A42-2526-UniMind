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
import org.example.utils.MyDataBase_Unimind;

public class TraitementService implements ICrud<Traitement> {

    // Méthode pour vérifier l'unicité d'un traitement
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
            ps.setString(4, t.getCategorie().name());
            ps.setInt(5, t.getDureeJours());
            ps.setString(6, t.getDosage());
            ps.setDate(7, t.getDateDebut());
            ps.setDate(8, t.getDateFin());
            ps.setString(9, t.getStatut().name());
            ps.setString(10, t.getPriorite().name());
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
            ps.setString(4, t.getCategorie().name());
            ps.setInt(5, t.getDureeJours());
            ps.setString(6, t.getDosage());
            ps.setDate(7, t.getDateDebut());
            ps.setDate(8, t.getDateFin());
            ps.setString(9, t.getStatut().name());
            ps.setString(10, t.getPriorite().name());
            ps.setString(11, t.getObjectifTherapeutique());
            if (t.getPsychologueId() == null) ps.setNull(12, Types.INTEGER); else ps.setInt(12, t.getPsychologueId());
            ps.setInt(13, t.getEtudiantId());
            ps.setInt(14, t.getTraitementId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM traitement WHERE traitement_id=?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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

                // Conversion sécurisée des énumérations
                String categorieStr = rs.getString("categorie");
                if (categorieStr != null) {
                    try {
                        t.setCategorie(org.example.enums.CategorieTraitement.valueOf(categorieStr));
                    } catch (IllegalArgumentException e) {
                        t.setCategorie(org.example.enums.CategorieTraitement.COGNITIF); // Valeur par défaut
                    }
                }

                t.setDureeJours(rs.getInt("duree_jours"));
                t.setDosage(rs.getString("dosage"));
                t.setDateDebut(rs.getDate("date_debut"));
                t.setDateFin(rs.getDate("date_fin"));

                String statutStr = rs.getString("statut");
                if (statutStr != null) {
                    try {
                        t.setStatut(org.example.enums.StatutTraitement.valueOf(statutStr));
                    } catch (IllegalArgumentException e) {
                        t.setStatut(org.example.enums.StatutTraitement.EN_COURS); // Valeur par défaut
                    }
                }

                String prioriteStr = rs.getString("priorite");
                if (prioriteStr != null) {
                    try {
                        t.setPriorite(org.example.enums.PrioriteTraitement.valueOf(prioriteStr));
                    } catch (IllegalArgumentException e) {
                        t.setPriorite(PrioriteTraitement.MOYENNE); // Valeur par défaut
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
}
