package org.example.services;

import org.example.entities.Profil;
import org.example.entities.Psychologue;
import org.example.entities.User;
import org.example.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PsychologueService extends UserService {

    public PsychologueService() { super(); }

    //  INSCRIPTION PSYCHOLOGUE
    @Override
    public void inscrire(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            System.out.println("✗ Email déjà utilisé.");
            return;
        }
        Psychologue p = (Psychologue) user;
        String hashedPassword = PasswordUtils.hasher(p.getPassword());
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at, specialite, adresse, telephone) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getPrenom());
        ps.setString(3, p.getEmail());
        ps.setString(4, hashedPassword);
        ps.setString(5, p.getCin());
        ps.setString(6, "Psychologue");
        ps.setString(7, "en_attente");
        ps.setBoolean(8, false);
        ps.setBoolean(9, false);
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
        ps.setString(11, p.getSpecialite());
        ps.setString(12, p.getAdresse());
        ps.setString(13, p.getTelephone());
        ps.executeUpdate();
        System.out.println("✓ Inscription soumise. En attente de validation par l'admin.");
    }

    @Override
    public void ajouter(User user) throws SQLException { inscrire(user); }

    //  MODIFIER COMPTE
    @Override
    public void modifier(User user) throws SQLException {
        Psychologue p = (Psychologue) user;
        String query = "UPDATE user SET nom=?, prenom=?, email=?, cin=?, specialite=?, adresse=?, telephone=? WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, p.getNom());
        ps.setString(2, p.getPrenom());
        ps.setString(3, p.getEmail());
        ps.setString(4, p.getCin());
        ps.setString(5, p.getSpecialite());
        ps.setString(6, p.getAdresse());
        ps.setString(7, p.getTelephone());
        ps.setInt(8, p.getUserId());
        ps.executeUpdate();
        System.out.println("✓ Compte psychologue modifié.");
    }

    // CHANGER MOT DE PASSE
    public void changerMotDePasse(int userId, String ancienMdp, String nouveauMdp) throws SQLException {
        String query = "SELECT password FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            if (PasswordUtils.verifier(ancienMdp, rs.getString("password"))) {
                String update = "UPDATE user SET password=? WHERE user_id=?";
                PreparedStatement ps2 = connection.prepareStatement(update);
                ps2.setString(1, PasswordUtils.hasher(nouveauMdp));
                ps2.setInt(2, userId);
                ps2.executeUpdate();
                System.out.println("✓ Mot de passe modifié avec succès.");
            } else {
                System.out.println("✗ Ancien mot de passe incorrect.");
            }
        }
    }

    //  MODIFIER PROFIL
    public void modifierProfil(Profil profil) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM profil WHERE user_id=?";
        PreparedStatement checkPs = connection.prepareStatement(checkQuery);
        checkPs.setInt(1, profil.getUserId());
        ResultSet rs = checkPs.executeQuery();
        rs.next();
        boolean exists = rs.getInt(1) > 0;

        if (exists) {
            String query = "UPDATE profil SET photo=?, bio=?, tel=?, specialite=?, experience=?, qualification=?, pseudo=?, updated_at=? WHERE user_id=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, profil.getPhoto());
            ps.setString(2, profil.getBio());
            ps.setString(3, profil.getTel());
            ps.setString(4, profil.getSpecialite());
            ps.setString(5, profil.getExperience());
            ps.setString(6, profil.getQualification());
            ps.setString(7, profil.getPseudo());
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            ps.setInt(9, profil.getUserId());
            ps.executeUpdate();
        } else {
            String query = "INSERT INTO profil (user_id, photo, bio, tel, specialite, experience, qualification, pseudo, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, profil.getUserId());
            ps.setString(2, profil.getPhoto());
            ps.setString(3, profil.getBio());
            ps.setString(4, profil.getTel());
            ps.setString(5, profil.getSpecialite());
            ps.setString(6, profil.getExperience());
            ps.setString(7, profil.getQualification());
            ps.setString(8, profil.getPseudo());
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
        System.out.println("✓ Profil psychologue mis à jour.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✓ Compte psychologue supprimé.");
    }

    @Override
    public List<User> afficher() throws SQLException {
        List<User> liste = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role='Psychologue' ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) liste.add(mapUser(rs));
        return liste;
    }

    @Override
    public User mapUser(ResultSet rs) throws SQLException {
        Psychologue p = new Psychologue();
        p.setUserId(rs.getInt("user_id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email"));
        p.setPassword(rs.getString("password"));
        p.setCin(rs.getString("cin"));
        p.setStatut(rs.getString("statut"));
        p.setActive(rs.getBoolean("is_active"));
        p.setVerified(rs.getBoolean("is_verified"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setSpecialite(rs.getString("specialite"));
        p.setAdresse(rs.getString("adresse"));
        p.setTelephone(rs.getString("telephone"));
        return p;
    }
    public boolean cinExiste(String cin) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE cin = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, cin);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }
}