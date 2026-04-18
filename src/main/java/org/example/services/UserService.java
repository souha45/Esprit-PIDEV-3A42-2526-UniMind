package org.example.services;

import org.example.entities.User;
import org.example.enums.Role;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class UserService implements ICrud<User> {

    private final Connection connection;

    public UserService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    // CONNEXION GENERALE (version simplifiée pour ton projet)
    // Cette version sera remplacée par la version complète après le merge avec le projet de l'ami
    public static User connexionGenerale(String email, String plainPassword,
                                         Connection connection) throws SQLException {

        String query = "SELECT * FROM user WHERE email = ? AND is_active = 1";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("✗ Email introuvable ou compte inactif.");
            return null;
        }

        String hashedPassword = rs.getString("password");

        if (!PasswordUtils.verifier(plainPassword, hashedPassword)) {
            System.out.println("✗ Mot de passe incorrect.");
            return null;
        }

        // Créer un User à partir du ResultSet
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setCin(rs.getString("cin"));

        // Gérer la casse et les espaces pour le rôle
        String roleStr = rs.getString("role");
        if (roleStr != null) {
            String normalizedRole = roleStr.toUpperCase().replace(" ", "_");
            user.setRole(Role.valueOf(normalizedRole));
        }

        user.setActive(rs.getBoolean("is_active"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setCreatedAt(rs.getTimestamp("created_at"));

        System.out.println("✓ Connexion réussie : " + user.getPrenom()
                + " " + user.getNom() + " [" + user.getRole() + "]");

        return user;
    }

    // EMAIL UNIQUE (du projet de l'ami)
    public boolean emailExiste(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // MAPPER ABSTAIT (nécessaire pour connexionGenerale)
    public abstract User mapUser(ResultSet rs) throws SQLException;

    @Override
    public void ajouter(User user) throws SQLException {
        String sql = "INSERT INTO user (nom, prenom, email, password, cin, role, is_active, is_verified, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getCin());
            ps.setString(6, user.getRole().toString());
            ps.setBoolean(7, user.isActive());
            ps.setBoolean(8, user.isVerified());
            ps.setTimestamp(9, user.getCreatedAt());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void modifier(User user) throws SQLException {
        String sql = "UPDATE user SET nom=?, prenom=?, email=?, password=?, cin=?, role=?, is_active=?, is_verified=? WHERE user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getCin());
            ps.setString(6, user.getRole().toString());
            ps.setBoolean(7, user.isActive());
            ps.setBoolean(8, user.isVerified());
            ps.setInt(9, user.getUserId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE user_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> afficher() throws SQLException {
        String sql = "SELECT user_id, nom, prenom, email, password, cin, role, is_active, is_verified, created_at FROM user";
        List<User> result = new ArrayList<>();

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT user_id, nom, prenom, email, password, cin, role, is_active, is_verified, created_at FROM user WHERE user_id=?";
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
     * Compte le nombre total d'utilisateurs
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM user";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compte le nombre d'utilisateurs par rôle
     */
    public int countByRole(Role role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE role=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Trouve un utilisateur par email
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, nom, prenom, email, password, cin, role, is_active, is_verified, created_at FROM user WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Vérifie si un email existe déjà
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setCin(rs.getString("cin"));
        // Gérer la casse et les espaces pour le rôle
        String roleStr = rs.getString("role");
        if (roleStr != null) {
            // Remplacer les espaces par des underscores et mettre en majuscules
            String normalizedRole = roleStr.toUpperCase().replace(" ", "_");
            user.setRole(Role.valueOf(normalizedRole));
        }
        user.setActive(rs.getBoolean("is_active"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
