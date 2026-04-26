package org.example.services;

import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReactionService {

    private final Connection con;
    public static final String[] REACTIONS = {"👍", "❤", "😂", "😮", "😢", "😠"};

    public ReactionService() {
        this.con = MyDataBase_Unimind.getInstance().getConnection();
    }

    // ══ POSTS ══

    public void toggleReactionPost(int postId, int userId, String type) throws SQLException {
        String existing = getReactionPost(postId, userId);
        if (type.equals(existing)) {
            execUpdate("DELETE FROM reaction_post WHERE post_id=? AND user_id=?", postId, userId);
        } else if (existing != null) {
            String sql = "UPDATE reaction_post SET type=? WHERE post_id=? AND user_id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, type); ps.setInt(2, postId); ps.setInt(3, userId);
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO reaction_post(post_id,user_id,type) VALUES(?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, postId); ps.setInt(2, userId); ps.setString(3, type);
                ps.executeUpdate();
            }
        }
    }

    public String getReactionPost(int postId, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT type FROM reaction_post WHERE post_id=? AND user_id=?")) {
            ps.setInt(1, postId); ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("type") : null;
        }
    }

    public Map<String, Integer> getCompteursPost(int postId) throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String r : REACTIONS) m.put(r, 0);
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT type, COUNT(*) cnt FROM reaction_post WHERE post_id=? GROUP BY type")) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) m.put(rs.getString("type"), rs.getInt("cnt"));
        }
        return m;
    }

    // ══ COMMENTAIRES ══

    public void toggleReactionCommentaire(int commId, int userId, String type) throws SQLException {
        String existing = getReactionCommentaire(commId, userId);
        if (type.equals(existing)) {
            execUpdate("DELETE FROM reaction_commentaire WHERE commentaire_id=? AND user_id=?", commId, userId);
        } else if (existing != null) {
            String sql = "UPDATE reaction_commentaire SET type=? WHERE commentaire_id=? AND user_id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, type); ps.setInt(2, commId); ps.setInt(3, userId);
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO reaction_commentaire(commentaire_id,user_id,type) VALUES(?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, commId); ps.setInt(2, userId); ps.setString(3, type);
                ps.executeUpdate();
            }
        }
    }

    public String getReactionCommentaire(int commId, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT type FROM reaction_commentaire WHERE commentaire_id=? AND user_id=?")) {
            ps.setInt(1, commId); ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("type") : null;
        }
    }

    public Map<String, Integer> getCompteursCommentaire(int commId) throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String r : REACTIONS) m.put(r, 0);
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT type, COUNT(*) cnt FROM reaction_commentaire WHERE commentaire_id=? GROUP BY type")) {
            ps.setInt(1, commId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) m.put(rs.getString("type"), rs.getInt("cnt"));
        }
        return m;
    }

    private void execUpdate(String sql, int p1, int p2) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p1); ps.setInt(2, p2); ps.executeUpdate();
        }
    }
}