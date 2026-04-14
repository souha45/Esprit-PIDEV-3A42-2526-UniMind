package org.example.services;

import org.example.entities.Favori;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriService implements ICrud<Favori> {

    private final Connection connection;

    public FavoriService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(Favori f) throws SQLException {
        System.out.println("Ajout d'un favori pour l'etudiant ID: " + f.getEtudiantId() + " a l'evenement ID: " + f.getEvenementId());
        String sql = "INSERT INTO favori (created_at, evenement_id, etudiant_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, f.getCreatedAt() != null ? f.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, f.getEvenementId());
            ps.setInt(3, f.getEtudiantId());

            ps.executeUpdate();
            System.out.println("Favori ajoute avec succes");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    f.setId(rs.getInt(1));
                    System.out.println("ID genere pour le favori: " + f.getId());
                }
            }
        }
    }

    @Override
    public void modifier(Favori f) throws SQLException {
        System.out.println("Modification du favori ID: " + f.getId());
        String sql = "UPDATE favori SET created_at=?, evenement_id=?, etudiant_id=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, f.getCreatedAt());
            ps.setInt(2, f.getEvenementId());
            ps.setInt(3, f.getEtudiantId());
            ps.setInt(4, f.getId());
            ps.executeUpdate();
            System.out.println("Favori ID " + f.getId() + " modifie avec succes");
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        System.out.println("Suppression du favori ID: " + id);
        String sql = "DELETE FROM favori WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Favori ID " + id + " supprime avec succes");
            } else {
                System.out.println("Aucun favori trouve avec l'ID: " + id);
            }
        }
    }

    @Override
    public List<Favori> afficher() throws SQLException {
        System.out.println("Recuperation de tous les favoris...");
        String sql = "SELECT id, created_at, evenement_id, etudiant_id FROM favori";
        List<Favori> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        System.out.println(result.size() + " favori(s) trouve(s)");
        return result;
    }

    public Favori findById(int id) throws SQLException {
        String sql = "SELECT id, created_at, evenement_id, etudiant_id FROM favori WHERE id=?";
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
     * Vérifie si un favori avec le même evenementId et etudiantId existe déjà
     * @param evenementId L'ID de l'événement
     * @param etudiantId L'ID de l'étudiant
     * @return true si un favori existe déjà, false sinon
     */
    public boolean verifierUnicite(int evenementId, int etudiantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favori WHERE evenement_id = ? AND etudiant_id = ?";
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
     * Récupère les favoris avec les noms des événements et étudiants
     * @return Liste d'objets FavoriAvecNoms
     */
    public java.util.List<FavoriAvecNoms> afficherAvecNoms() throws SQLException {
        String sql = "SELECT f.id, f.created_at, f.evenement_id, f.etudiant_id, " +
                     "e.titre as evenement_titre, u.prenom as etudiant_prenom, u.nom as etudiant_nom " +
                     "FROM favori f " +
                     "LEFT JOIN evenement e ON f.evenement_id = e.evenement_id " +
                     "LEFT JOIN user u ON f.etudiant_id = u.user_id";
        java.util.List<FavoriAvecNoms> result = new java.util.ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                FavoriAvecNoms fav = new FavoriAvecNoms();
                fav.setId(rs.getInt("id"));
                fav.setCreatedAt(rs.getTimestamp("created_at"));
                fav.setEvenementId(rs.getInt("evenement_id"));
                fav.setEtudiantId(rs.getInt("etudiant_id"));
                fav.setEvenementTitre(rs.getString("evenement_titre"));
                fav.setEtudiantNom(rs.getString("etudiant_prenom") + " " + rs.getString("etudiant_nom"));
                result.add(fav);
            }
        }
        return result;
    }

    /**
     * Classe interne pour représenter un favori avec les noms
     */
    public static class FavoriAvecNoms {
        private int id;
        private Timestamp createdAt;
        private int evenementId;
        private int etudiantId;
        private String evenementTitre;
        private String etudiantNom;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public int getEvenementId() { return evenementId; }
        public void setEvenementId(int evenementId) { this.evenementId = evenementId; }
        public int getEtudiantId() { return etudiantId; }
        public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }
        public String getEvenementTitre() { return evenementTitre; }
        public void setEvenementTitre(String evenementTitre) { this.evenementTitre = evenementTitre; }
        public String getEtudiantNom() { return etudiantNom; }
        public void setEtudiantNom(String etudiantNom) { this.etudiantNom = etudiantNom; }
    }

    private Favori mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Timestamp createdAt = rs.getTimestamp("created_at");
        int evenementId = rs.getInt("evenement_id");
        int etudiantId = rs.getInt("etudiant_id");

        return new Favori(id, createdAt, evenementId, etudiantId);
    }
}
