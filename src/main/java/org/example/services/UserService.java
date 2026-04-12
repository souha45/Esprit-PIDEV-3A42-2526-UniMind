package org.example.services;

import org.example.entities.User;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.PasswordUtils;

import java.sql.*;
import java.util.List;

public abstract class UserService implements ICrud<User> {

    protected Connection connection;

    public UserService() {
        this.connection = MyDataBase_Unimind.getInstance().getConnection();
    }

    // ── CONNEXION GENERALE ────────────────────────────────────────────
    public static User connexionGenerale(String email, String plainPassword,
                                         Connection connection,
                                         AdminService adminService,
                                         EtudiantService etudiantService,
                                         PsychologueService psychologueService,
                                         ResponsableService responsableService) throws SQLException {

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

        // ── equalsIgnoreCase pour gérer ADMIN/Admin/admin ─────────────
        String roleStr = rs.getString("role");
        User user = null;

        if (roleStr.equalsIgnoreCase("Admin")) {
            user = adminService.mapUser(rs);
        } else if (roleStr.equalsIgnoreCase("Etudiant")) {
            user = etudiantService.mapUser(rs);
        } else if (roleStr.equalsIgnoreCase("Psychologue")) {
            user = psychologueService.mapUser(rs);
        } else if (roleStr.equalsIgnoreCase("Responsable Etudiant")) {
            user = responsableService.mapUser(rs);
        } else {
            System.out.println("✗ Rôle non reconnu : " + roleStr);
        }

        if (user != null) {
            System.out.println("✓ Connexion réussie : " + user.getPrenom()
                    + " " + user.getNom() + " [" + user.getRole() + "]");
        }
        return user;
    }

    // ── DECONNEXION ───────────────────────────────────────────────────
    public void deconnexion(User user) {
        if (user != null) {
            System.out.println("✓ Déconnexion : " + user.getPrenom() + " " + user.getNom());
        }
    }

    // ── INSCRIPTION (abstraite) ───────────────────────────────────────
    public abstract void inscrire(User user) throws SQLException;

    // ── EMAIL UNIQUE ──────────────────────────────────────────────────
    protected boolean emailExiste(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // ── MAPPER (abstrait) ─────────────────────────────────────────────
    public abstract User mapUser(ResultSet rs) throws SQLException;

    @Override
    public List<User> afficher() throws SQLException {
        return null;
    }
}