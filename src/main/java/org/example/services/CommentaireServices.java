package org.example.services;

import org.example.entities.Commentaire;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireServices implements ICrud<Commentaire> {
    Connection con;

    public CommentaireServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(Commentaire commentaire) throws SQLException {
        String sql = "INSERT INTO `commentaire` (`contenu`, `is_anonyme`, `created_at`, `updated_at`, `user_id`, `post_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, commentaire.getContenu());
        preparedStatement.setBoolean(2, commentaire.isIsAnonyme());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        preparedStatement.setTimestamp(3, now); // created_at
        preparedStatement.setTimestamp(4, now); // updated_at
        preparedStatement.setInt(5, commentaire.getUserId());
        preparedStatement.setInt(6, commentaire.getPostId());
        preparedStatement.executeUpdate();
        System.out.println("Ajout du commentaire pour le post " + commentaire.getPostId());
    }

    @Override
    public void modifier(Commentaire commentaire) throws SQLException {
        String sql = "UPDATE `commentaire` SET `contenu` = ?, `is_anonyme` = ?, `updated_at` = ?, `user_id` = ?, `post_id` = ? " +
                "WHERE `commentaire_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, commentaire.getContenu());
        preparedStatement.setBoolean(2, commentaire.isIsAnonyme());
        preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
        preparedStatement.setInt(4, commentaire.getUserId());
        preparedStatement.setInt(5, commentaire.getPostId());
        preparedStatement.setInt(6, commentaire.getCommentaireId());
        preparedStatement.executeUpdate();
        System.out.println("Modification du commentaire ID " + commentaire.getCommentaireId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `commentaire` WHERE `commentaire_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Suppression du commentaire ID " + id);
    }

    @Override
    public List<Commentaire> afficher() throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT * FROM `commentaire`";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setCommentaireId(rs.getInt("commentaire_id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setIsAnonyme(rs.getBoolean("is_anonyme"));
            commentaire.setCreatedAt(rs.getTimestamp("created_at"));
            commentaire.setUpdatedAt(rs.getTimestamp("updated_at"));
            commentaire.setUserId(rs.getInt("user_id"));
            commentaire.setPostId(rs.getInt("post_id"));
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    // Méthode utilitaire : récupérer les commentaires d'un post spécifique
    public List<Commentaire> getCommentairesByPost(int postId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT * FROM `commentaire` WHERE `post_id` = ? ORDER BY `created_at` ASC";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, postId);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setCommentaireId(rs.getInt("commentaire_id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setIsAnonyme(rs.getBoolean("is_anonyme"));
            commentaire.setCreatedAt(rs.getTimestamp("created_at"));
            commentaire.setUpdatedAt(rs.getTimestamp("updated_at"));
            commentaire.setUserId(rs.getInt("user_id"));
            commentaire.setPostId(rs.getInt("post_id"));
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    // Méthode utilitaire : récupérer les commentaires d'un utilisateur spécifique
    public List<Commentaire> getCommentairesByUser(int userId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT * FROM `commentaire` WHERE `user_id` = ? ORDER BY `created_at` DESC";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, userId);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setCommentaireId(rs.getInt("commentaire_id"));
            commentaire.setContenu(rs.getString("contenu"));
            commentaire.setIsAnonyme(rs.getBoolean("is_anonyme"));
            commentaire.setCreatedAt(rs.getTimestamp("created_at"));
            commentaire.setUpdatedAt(rs.getTimestamp("updated_at"));
            commentaire.setUserId(rs.getInt("user_id"));
            commentaire.setPostId(rs.getInt("post_id"));
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    public Connection getCon() {
        return con;
    }
}