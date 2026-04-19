package org.example.services;

import org.example.entities.Post;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostServices implements ICrud<Post> {
    Connection con;

    public PostServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(Post post) throws SQLException {
        String sql = "INSERT INTO `post` (`titre`, `contenu`, `is_anonyme`, `created_at`, `updated_at`, `user_id`, `categorie_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, post.getTitre());
        preparedStatement.setString(2, post.getContenu());
        preparedStatement.setBoolean(3, post.isIsAnonyme());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        preparedStatement.setTimestamp(4, now); // created_at
        preparedStatement.setTimestamp(5, now); // updated_at
        preparedStatement.setInt(6, post.getUserId());
        preparedStatement.setInt(7, post.getCategorieId()); // jamais NULL
        preparedStatement.executeUpdate();
        System.out.println("Ajout du post : " + post.getTitre());
    }

    @Override
    public void modifier(Post post) throws SQLException {
        String sql = "UPDATE `post` SET `titre` = ?, `contenu` = ?, `is_anonyme` = ?, `updated_at` = ?, `user_id` = ?, `categorie_id` = ? " +
                "WHERE `post_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, post.getTitre());
        preparedStatement.setString(2, post.getContenu());
        preparedStatement.setBoolean(3, post.isIsAnonyme());
        preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        preparedStatement.setInt(5, post.getUserId());
        preparedStatement.setInt(6, post.getCategorieId()); // jamais NULL
        preparedStatement.setInt(7, post.getPostId());
        preparedStatement.executeUpdate();
        System.out.println("Modification du post ID " + post.getPostId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `post` WHERE `post_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Suppression du post ID " + id);
    }

    @Override
    public List<Post> afficher() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM `post`";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            Post post = new Post();
            post.setPostId(rs.getInt("post_id"));
            post.setTitre(rs.getString("titre"));
            post.setContenu(rs.getString("contenu"));
            post.setIsAnonyme(rs.getBoolean("is_anonyme"));
            post.setCreatedAt(rs.getTimestamp("created_at"));
            post.setUpdatedAt(rs.getTimestamp("updated_at"));
            post.setUserId(rs.getInt("user_id"));
            post.setCategorieId(rs.getInt("categorie_id")); // jamais NULL
            posts.add(post);
        }
        return posts;
    }

    // Méthode utilitaire : récupérer les posts d'une catégorie spécifique
    public List<Post> getPostsByCategorie(int categorieId) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM `post` WHERE `categorie_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, categorieId);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            Post post = new Post();
            post.setPostId(rs.getInt("post_id"));
            post.setTitre(rs.getString("titre"));
            post.setContenu(rs.getString("contenu"));
            post.setIsAnonyme(rs.getBoolean("is_anonyme"));
            post.setCreatedAt(rs.getTimestamp("created_at"));
            post.setUpdatedAt(rs.getTimestamp("updated_at"));
            post.setUserId(rs.getInt("user_id"));
            post.setCategorieId(rs.getInt("categorie_id"));
            posts.add(post);
        }
        return posts;
    }

    // Méthode utilitaire : récupérer les posts d'un utilisateur spécifique
    public List<Post> getPostsByUser(int userId) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM `post` WHERE `user_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, userId);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            Post post = new Post();
            post.setPostId(rs.getInt("post_id"));
            post.setTitre(rs.getString("titre"));
            post.setContenu(rs.getString("contenu"));
            post.setIsAnonyme(rs.getBoolean("is_anonyme"));
            post.setCreatedAt(rs.getTimestamp("created_at"));
            post.setUpdatedAt(rs.getTimestamp("updated_at"));
            post.setUserId(rs.getInt("user_id"));
            post.setCategorieId(rs.getInt("categorie_id"));
            posts.add(post);
        }
        return posts;
    }

    public Connection getCon() {
        return con;
    }
}