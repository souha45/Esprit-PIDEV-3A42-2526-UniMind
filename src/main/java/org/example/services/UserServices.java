package org.example.services;

import org.example.entities.User;
import org.example.enums.Role;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;

public class UserServices {
    Connection con;

    public UserServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    /**
     * Authentifie un utilisateur par email et mot de passe.
     * Retourne l'User si trouvé, null sinon.
     */
    public User login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE `email` = ? AND `password` = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, email);
        ps.setString(2, password); // En prod: comparer hash
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setCin(rs.getString("cin"));
            user.setRole(Role.valueOf(rs.getString("role")));
            user.setActive(rs.getBoolean("is_active"));
            user.setCreatedAt(rs.getTimestamp("created_at"));
            return user;
        }
        return null;
    }

    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE `user_id` = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setNom(rs.getString("nom"));
            user.setPrenom(rs.getString("prenom"));
            user.setEmail(rs.getString("email"));
            user.setRole(Role.valueOf(rs.getString("role")));
            return user;
        }
        return null;
    }
}