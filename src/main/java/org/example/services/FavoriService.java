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
        System.out.println("Ajout d'un favori pour l'étudiant ID: " + f.getEtudiantId() + " à l'événement ID: " + f.getEvenementId());
        String sql = "INSERT INTO favori (created_at, evenement_id, etudiant_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, f.getCreatedAt() != null ? f.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, f.getEvenementId());
            ps.setInt(3, f.getEtudiantId());

            ps.executeUpdate();
            System.out.println("Favori ajouté avec succès");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    f.setId(rs.getInt(1));
                    System.out.println("ID généré pour le favori: " + f.getId());
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
            System.out.println("Favori ID " + f.getId() + " modifié avec succès");
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
                System.out.println("Favori ID " + id + " supprimé avec succès");
            } else {
                System.out.println("Aucun favori trouvé avec l'ID: " + id);
            }
        }
    }

    @Override
    public List<Favori> afficher() throws SQLException {
        System.out.println("Récupération de tous les favoris...");
        String sql = "SELECT id, created_at, evenement_id, etudiant_id FROM favori";
        List<Favori> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        System.out.println(result.size() + " favori(s) trouvé(s)");
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

    private Favori mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Timestamp createdAt = rs.getTimestamp("created_at");
        int evenementId = rs.getInt("evenement_id");
        int etudiantId = rs.getInt("etudiant_id");

        return new Favori(id, createdAt, evenementId, etudiantId);
    }
}
