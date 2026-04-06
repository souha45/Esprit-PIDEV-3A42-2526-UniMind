package org.example.services;

import org.example.entities.Profil;
import org.example.entities.ResponsableEtudiant;
import org.example.entities.User;
import org.example.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResponsableService extends UserService {

    public ResponsableService() { super(); }

    // ── INSCRIPTION RESPONSABLE ───────────────────────────────────────
    @Override
    public void inscrire(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            System.out.println("✗ Email déjà utilisé.");
            return;
        }
        ResponsableEtudiant r = (ResponsableEtudiant) user;
        String hashedPassword = PasswordUtils.hasher(r.getPassword());
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at, poste, etablissement) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, r.getNom());
        ps.setString(2, r.getPrenom());
        ps.setString(3, r.getEmail());
        ps.setString(4, hashedPassword);
        ps.setString(5, r.getCin());
        ps.setString(6, "Responsable Etudiant");
        ps.setString(7, "en_attente");
        ps.setBoolean(8, false);
        ps.setBoolean(9, false);
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
        ps.setString(11, r.getPoste());
        ps.setString(12, r.getEtablissement());
        ps.executeUpdate();
        System.out.println("✓ Inscription soumise. En attente de validation par l'admin.");
    }

    @Override
    public void ajouter(User user) throws SQLException { inscrire(user); }

    // ── MODIFIER COMPTE ───────────────────────────────────────────────
    @Override
    public void modifier(User user) throws SQLException {
        ResponsableEtudiant r = (ResponsableEtudiant) user;
        String query = "UPDATE user SET nom=?, prenom=?, email=?, cin=?, poste=?, etablissement=? WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, r.getNom());
        ps.setString(2, r.getPrenom());
        ps.setString(3, r.getEmail());
        ps.setString(4, r.getCin());
        ps.setString(5, r.getPoste());
        ps.setString(6, r.getEtablissement());
        ps.setInt(7, r.getUserId());
        ps.executeUpdate();
        System.out.println("✓ Compte responsable modifié.");
    }

    // ── CHANGER MOT DE PASSE ──────────────────────────────────────────
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

    // ── MODIFIER PROFIL ───────────────────────────────────────────────
    public void modifierProfil(Profil profil) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM profil WHERE user_id=?";
        PreparedStatement checkPs = connection.prepareStatement(checkQuery);
        checkPs.setInt(1, profil.getUserId());
        ResultSet rs = checkPs.executeQuery();
        rs.next();
        boolean exists = rs.getInt(1) > 0;

        if (exists) {
            String query = "UPDATE profil SET photo=?, bio=?, tel=?, departement=?, fonction=?, etablissement=?, pseudo=?, updated_at=? WHERE user_id=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, profil.getPhoto());
            ps.setString(2, profil.getBio());
            ps.setString(3, profil.getTel());
            ps.setString(4, profil.getDepartement());
            ps.setString(5, profil.getFonction());
            ps.setString(6, profil.getEtablissement());
            ps.setString(7, profil.getPseudo());
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            ps.setInt(9, profil.getUserId());
            ps.executeUpdate();
        } else {
            String query = "INSERT INTO profil (user_id, photo, bio, tel, departement, fonction, etablissement, pseudo, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, profil.getUserId());
            ps.setString(2, profil.getPhoto());
            ps.setString(3, profil.getBio());
            ps.setString(4, profil.getTel());
            ps.setString(5, profil.getDepartement());
            ps.setString(6, profil.getFonction());
            ps.setString(7, profil.getEtablissement());
            ps.setString(8, profil.getPseudo());
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
        System.out.println("✓ Profil responsable mis à jour.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✓ Compte responsable supprimé.");
    }

    @Override
    public List<User> afficher() throws SQLException {
        List<User> liste = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role='Responsable Etudiant' ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) liste.add(mapUser(rs));
        return liste;
    }

    @Override
    public User mapUser(ResultSet rs) throws SQLException {
        ResponsableEtudiant r = new ResponsableEtudiant();
        r.setUserId(rs.getInt("user_id"));
        r.setNom(rs.getString("nom"));
        r.setPrenom(rs.getString("prenom"));
        r.setEmail(rs.getString("email"));
        r.setPassword(rs.getString("password"));
        r.setCin(rs.getString("cin"));
        r.setStatut(rs.getString("statut"));
        r.setActive(rs.getBoolean("is_active"));
        r.setVerified(rs.getBoolean("is_verified"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setPoste(rs.getString("poste"));
        r.setEtablissement(rs.getString("etablissement"));
        return r;
    }
}