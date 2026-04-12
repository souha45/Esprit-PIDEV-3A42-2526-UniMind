package org.example.services;

import org.example.entities.Etudiant;
import org.example.entities.Profil;
import org.example.entities.User;
import org.example.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtudiantService extends UserService {

    public EtudiantService() { super(); }

    //  INSCRIPTION ETUDIANt
    @Override
    public void inscrire(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            System.out.println("✗ Email déjà utilisé.");
            return;
        }
        Etudiant e = (Etudiant) user;
        String hashedPassword = PasswordUtils.hasher(e.getPassword());
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at, identifiant, nom_etablissement) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getPrenom());
        ps.setString(3, e.getEmail());
        ps.setString(4, hashedPassword);
        ps.setString(5, e.getCin());
        ps.setString(6, "Etudiant");
        ps.setString(7, "en_attente");
        ps.setBoolean(8, false);
        ps.setBoolean(9, false);
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
        ps.setString(11, e.getIdentifiant());
        ps.setString(12, e.getNomEtablissement());
        ps.executeUpdate();
        System.out.println("✓ Inscription soumise. En attente de validation par l'admin.");
    }

    @Override
    public void ajouter(User user) throws SQLException { inscrire(user); }

    // MODIFIER COMPTE
    @Override
    public void modifier(User user) throws SQLException {
        Etudiant e = (Etudiant) user;
        String query = "UPDATE user SET nom=?, prenom=?, email=?, cin=?, identifiant=?, nom_etablissement=? WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, e.getNom());
        ps.setString(2, e.getPrenom());
        ps.setString(3, e.getEmail());
        ps.setString(4, e.getCin());
        ps.setString(5, e.getIdentifiant());
        ps.setString(6, e.getNomEtablissement());
        ps.setInt(7, e.getUserId());
        ps.executeUpdate();
        System.out.println("✓ Compte étudiant modifié.");
    }

    // CHANGER MOT DE PASSE
    public void changerMotDePasse(int userId, String ancienMdp, String nouveauMdp) throws SQLException {
        String query = "SELECT password FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String hashedActuel = rs.getString("password");
            if (PasswordUtils.verifier(ancienMdp, hashedActuel)) {
                String nouveauHash = PasswordUtils.hasher(nouveauMdp);
                String update = "UPDATE user SET password=? WHERE user_id=?";
                PreparedStatement ps2 = connection.prepareStatement(update);
                ps2.setString(1, nouveauHash);
                ps2.setInt(2, userId);
                ps2.executeUpdate();
                System.out.println("✓ Mot de passe modifié avec succès.");
            } else {
                System.out.println("✗ Ancien mot de passe incorrect.");
            }
        }
    }

    // MODIFIER PROFIL
    public void modifierProfil(Profil profil) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM profil WHERE user_id=?";
        PreparedStatement checkPs = connection.prepareStatement(checkQuery);
        checkPs.setInt(1, profil.getUserId());
        ResultSet rs = checkPs.executeQuery();
        rs.next();
        boolean exists = rs.getInt(1) > 0;

        if (exists) {
            String query = "UPDATE profil SET photo=?, bio=?, tel=?, date_naissance=?, niveau=?, filiere=?, pseudo=?, updated_at=? WHERE user_id=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, profil.getPhoto());
            ps.setString(2, profil.getBio());
            ps.setString(3, profil.getTel());
            ps.setTimestamp(4, profil.getDateNaissance());
            ps.setString(5, profil.getNiveau());
            ps.setString(6, profil.getFiliere());
            ps.setString(7, profil.getPseudo());
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            ps.setInt(9, profil.getUserId());
            ps.executeUpdate();
        } else {
            String query = "INSERT INTO profil (user_id, photo, bio, tel, date_naissance, niveau, filiere, pseudo, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, profil.getUserId());
            ps.setString(2, profil.getPhoto());
            ps.setString(3, profil.getBio());
            ps.setString(4, profil.getTel());
            ps.setTimestamp(5, profil.getDateNaissance());
            ps.setString(6, profil.getNiveau());
            ps.setString(7, profil.getFiliere());
            ps.setString(8, profil.getPseudo());
            ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
        System.out.println("✓ Profil étudiant mis à jour.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✓ Compte étudiant supprimé.");
    }

    @Override
    public List<User> afficher() throws SQLException {
        List<User> liste = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role='Etudiant' ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) liste.add(mapUser(rs));
        return liste;
    }

    @Override
    public User mapUser(ResultSet rs) throws SQLException {
        Etudiant e = new Etudiant();
        e.setUserId(rs.getInt("user_id"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        e.setEmail(rs.getString("email"));
        e.setPassword(rs.getString("password"));
        e.setCin(rs.getString("cin"));
        e.setStatut(rs.getString("statut"));
        e.setActive(rs.getBoolean("is_active"));
        e.setVerified(rs.getBoolean("is_verified"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        e.setIdentifiant(rs.getString("identifiant"));
        e.setNomEtablissement(rs.getString("nom_etablissement"));
        return e;
    }
}