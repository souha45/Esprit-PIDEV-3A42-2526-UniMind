package org.example.services;

import org.example.entities.CategorieMeditation;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieMeditationServices implements ICrud<CategorieMeditation> {
    Connection con;

    public CategorieMeditationServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(CategorieMeditation categorieMeditation) throws SQLException {
        String sql = "INSERT INTO `categorie_meditation` (`nom`, `description`, `date_creation`, `updated_at`, `icon_url`) " +
                "VALUES (?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, categorieMeditation.getNom());
        preparedStatement.setString(2, categorieMeditation.getDescription());
        // Set both timestamps to current time
        Timestamp now = new Timestamp(System.currentTimeMillis());
        preparedStatement.setTimestamp(3, now); // date_creation
        preparedStatement.setTimestamp(4, now); // updated_at
        preparedStatement.setString(5, categorieMeditation.getIconUrl());
        preparedStatement.executeUpdate();
        System.out.println("Ajout de CategorieMeditation");

    }

    @Override
    public void modifier(CategorieMeditation categorieMeditation) throws SQLException {
        String sql = "UPDATE `categorie_meditation` SET `nom` = ?, `description` = ?, `icon_url` = ?, `updated_at` = ? " +
                "WHERE `categorie_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, categorieMeditation.getNom());
        preparedStatement.setString(2, categorieMeditation.getDescription());
        preparedStatement.setString(3, categorieMeditation.getIconUrl());
        preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        preparedStatement.setInt(5, categorieMeditation.getCategorieId());
        preparedStatement.executeUpdate();
        System.out.println("Modification de la catégorie ID " + categorieMeditation.getCategorieId());

    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `categorie_meditation` WHERE `categorie_id` = ?";

        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Suppression du categorie_meditation");
    }

    @Override
    public List<CategorieMeditation> afficher() throws SQLException {
        List<CategorieMeditation> categories = new ArrayList<>();
        String sql = "SELECT * FROM `categorie_meditation`";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            CategorieMeditation cat = new CategorieMeditation();
            cat.setCategorieId(rs.getInt("categorie_id"));
            cat.setNom(rs.getString("nom"));
            cat.setDescription(rs.getString("description"));
            cat.setDateCreation(rs.getTimestamp("date_creation"));
            cat.setUpdatedAt(rs.getTimestamp("updated_at"));
            cat.setIconUrl(rs.getString("icon_url"));
            categories.add(cat);
            }
        return categories;
    }

    public Connection getCon() {
        return con;
    }
}
