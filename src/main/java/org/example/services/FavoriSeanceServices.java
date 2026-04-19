package org.example.services;

import org.example.entities.FavoriSeance;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriSeanceServices implements ICrud<FavoriSeance> {
    Connection con;

    public FavoriSeanceServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(FavoriSeance favoriSeance) throws SQLException {
        String sql = "INSERT INTO `favoriseance` (`user_id`, `seance_id`, `created_at`) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, favoriSeance.getUserId());
        preparedStatement.setInt(2, favoriSeance.getSeanceId());
        preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
        preparedStatement.executeUpdate();
        System.out.println("Ajout du favori (user " + favoriSeance.getUserId() + ", séance " + favoriSeance.getSeanceId() + ")");
    }

    @Override
    public void modifier(FavoriSeance favoriSeance) throws SQLException {
        String sql = "UPDATE `favoriseance` SET `user_id` = ?, `seance_id` = ? WHERE `id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, favoriSeance.getUserId());
        preparedStatement.setInt(2, favoriSeance.getSeanceId());
        preparedStatement.setInt(3, favoriSeance.getId());
        preparedStatement.executeUpdate();
        System.out.println("Modification du favori ID " + favoriSeance.getId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `favoriseance` WHERE `id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Suppression du favori ID " + id);
    }

    @Override
    public List<FavoriSeance> afficher() throws SQLException {
        List<FavoriSeance> favoris = new ArrayList<>();
        String sql = "SELECT * FROM `favoriseance`";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            FavoriSeance favori = new FavoriSeance();
            favori.setId(rs.getInt("id"));
            favori.setUserId(rs.getInt("user_id"));
            favori.setSeanceId(rs.getInt("seance_id"));
            favori.setCreatedAt(rs.getTimestamp("created_at"));
            favoris.add(favori);
        }
        return favoris;
    }

    // Méthode utilitaire : récupérer les favoris d'un utilisateur spécifique
    public List<FavoriSeance> getFavorisByUser(int userId) throws SQLException {
        List<FavoriSeance> favoris = new ArrayList<>();
        String sql = "SELECT * FROM `favoriseance` WHERE `user_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, userId);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            FavoriSeance favori = new FavoriSeance();
            favori.setId(rs.getInt("id"));
            favori.setUserId(rs.getInt("user_id"));
            favori.setSeanceId(rs.getInt("seance_id"));
            favori.setCreatedAt(rs.getTimestamp("created_at"));
            favoris.add(favori);
        }
        return favoris;
    }

    // Vérifier si une séance est déjà en favori pour un utilisateur
    public boolean isFavori(int userId, int seanceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `favoriseance` WHERE `user_id` = ? AND `seance_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, userId);
        preparedStatement.setInt(2, seanceId);
        ResultSet rs = preparedStatement.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public Connection getCon() {
        return con;
    }
}