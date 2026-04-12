package org.example.services;

import org.example.entities.*;
import org.example.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminService extends UserService {

    public AdminService() { super(); }

    // ── INSCRIPTION ADMIN ─────────────────────────────────────────────
    @Override
    public void inscrire(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            throw new SQLException("Email déjà utilisé : " + user.getEmail());
        }
        if (cinExiste(user.getCin())) {
            throw new SQLException("CIN déjà utilisé : " + user.getCin());
        }
        String hashedPassword = PasswordUtils.hasher(user.getPassword());
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, hashedPassword);
        ps.setString(5, user.getCin());
        ps.setString(6, "Admin");
        ps.setString(7, "actif");
        ps.setBoolean(8, true);
        ps.setBoolean(9, true);
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
        System.out.println("✓ Admin créé avec succès.");
    }

    // ── AJOUTER UN UTILISATEUR ────────────────────────────────────────
    @Override
    public void ajouter(User user) throws SQLException {
        if (emailExiste(user.getEmail())) {
            throw new SQLException("Email déjà utilisé : " + user.getEmail());
        }
        if (cinExiste(user.getCin())) {
            throw new SQLException("CIN déjà utilisé : " + user.getCin());
        }
        String hashedPassword = PasswordUtils.hasher(user.getPassword());
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, hashedPassword);
        ps.setString(5, user.getCin());
        ps.setString(6, user.getRole().name());
        ps.setString(7, user.getStatut());
        ps.setBoolean(8, user.isActive());
        ps.setBoolean(9, user.isVerified());
        ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
        System.out.println("✓ Utilisateur ajouté avec succès.");
    }

    // ── MODIFIER UN UTILISATEUR ───────────────────────────────────────
    @Override
    public void modifier(User user) throws SQLException {
        // Vérifier unicité email (exclure l'utilisateur lui-même)
        String checkEmail = "SELECT COUNT(*) FROM user WHERE email = ? AND user_id != ?";
        PreparedStatement psEmail = connection.prepareStatement(checkEmail);
        psEmail.setString(1, user.getEmail());
        psEmail.setInt(2, user.getUserId());
        ResultSet rsEmail = psEmail.executeQuery();
        rsEmail.next();
        if (rsEmail.getInt(1) > 0) {
            throw new SQLException("Email déjà utilisé par un autre utilisateur !");
        }

        // Vérifier unicité CIN (exclure l'utilisateur lui-même)
        String checkCin = "SELECT COUNT(*) FROM user WHERE cin = ? AND user_id != ?";
        PreparedStatement psCin = connection.prepareStatement(checkCin);
        psCin.setString(1, user.getCin());
        psCin.setInt(2, user.getUserId());
        ResultSet rsCin = psCin.executeQuery();
        rsCin.next();
        if (rsCin.getInt(1) > 0) {
            throw new SQLException("CIN déjà utilisé par un autre utilisateur !");
        }

        String query = "UPDATE user SET nom=?, prenom=?, email=?, cin=?, role=?, statut=? WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getCin());
        ps.setString(5, user.getRole().name());
        ps.setString(6, user.getStatut());
        ps.setInt(7, user.getUserId());
        ps.executeUpdate();
        System.out.println("✓ Utilisateur modifié avec succès.");
    }

    // ── CHANGER MOT DE PASSE (admin) ──────────────────────────────────
    public void changerMotDePasse(int userId, String ancienMdp, String nouveauMdp) throws SQLException {
        String query = "SELECT password FROM user WHERE user_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String hash = rs.getString("password");
            if (!PasswordUtils.verifier(ancienMdp, hash)) {
                throw new SQLException("Ancien mot de passe incorrect !");
            }
            String update = "UPDATE user SET password = ? WHERE user_id = ?";
            PreparedStatement upd = connection.prepareStatement(update);
            upd.setString(1, PasswordUtils.hasher(nouveauMdp));
            upd.setInt(2, userId);
            upd.executeUpdate();
            System.out.println("✓ Mot de passe modifié avec succès.");
        }
    }

    // ── SUPPRIMER UN UTILISATEUR ──────────────────────────────────────
    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM user WHERE user_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✓ Utilisateur supprimé avec succès.");
    }

    // ── BLOQUER UN COMPTE ─────────────────────────────────────────────
    public void bloquer(int userId) throws SQLException {
        String query = "UPDATE user SET is_active=0, statut='inactif' WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ps.executeUpdate();
        System.out.println("✓ Compte bloqué avec succès.");
    }

    // ── DEBLOQUER UN COMPTE ───────────────────────────────────────────
    public void debloquer(int userId) throws SQLException {
        String query = "UPDATE user SET is_active=1, statut='actif' WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ps.executeUpdate();
        System.out.println("✓ Compte débloqué avec succès.");
    }

    // ── ACCEPTER DEMANDE ──────────────────────────────────────────────
    public void accepterDemande(int userId) throws SQLException {
        String query = "UPDATE user SET statut='actif', is_active=1, is_verified=1 WHERE user_id=? AND statut='en_attente'";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        int rows = ps.executeUpdate();
        if (rows > 0) System.out.println("✓ Demande acceptée - compte activé.");
        else System.out.println("✗ Aucune demande en attente pour cet utilisateur.");
    }

    // ── REFUSER DEMANDE ───────────────────────────────────────────────
    public void refuserDemande(int userId) throws SQLException {
        String query = "UPDATE user SET statut='rejeté', is_active=0 WHERE user_id=? AND statut='en_attente'";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        int rows = ps.executeUpdate();
        if (rows > 0) System.out.println("✓ Demande refusée.");
        else System.out.println("✗ Aucune demande en attente pour cet utilisateur.");
    }

    // ── AFFICHER DEMANDES EN ATTENTE ──────────────────────────────────
    public List<User> afficherDemandesEnAttente() throws SQLException {
        List<User> liste = new ArrayList<>();
        String query = "SELECT * FROM user WHERE statut='en_attente' ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) liste.add(mapUser(rs));
        System.out.println("✓ " + liste.size() + " demande(s) en attente.");
        return liste;
    }

    // ── AFFICHER TOUS ─────────────────────────────────────────────────
    @Override
    public List<User> afficher() throws SQLException {
        List<User> liste = new ArrayList<>();
        String query = "SELECT * FROM user ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) liste.add(mapUser(rs));
        return liste;
    }

    // ── RECHERCHER PAR ID ─────────────────────────────────────────────
    public User rechercherParId(int id) throws SQLException {
        String query = "SELECT * FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapUser(rs);
        System.out.println("✗ Utilisateur introuvable.");
        return null;
    }

    // ── MODIFIER PROFIL ───────────────────────────────────────────────
    public void modifierProfil(Profil profil) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM profil WHERE user_id = ?";
        PreparedStatement check = connection.prepareStatement(checkQuery);
        check.setInt(1, profil.getUserId());
        ResultSet rs = check.executeQuery();
        rs.next();
        boolean existe = rs.getInt(1) > 0;

        if (existe) {
            String update = "UPDATE profil SET bio=?, tel=?, updated_at=NOW() WHERE user_id=?";
            PreparedStatement ps = connection.prepareStatement(update);
            ps.setString(1, profil.getBio());
            ps.setString(2, profil.getTel());
            ps.setInt(3, profil.getUserId());
            ps.executeUpdate();
        } else {
            String insert = "INSERT INTO profil (user_id, bio, tel, updated_at) VALUES (?,?,?,NOW())";
            PreparedStatement ps = connection.prepareStatement(insert);
            ps.setInt(1, profil.getUserId());
            ps.setString(2, profil.getBio());
            ps.setString(3, profil.getTel());
            ps.executeUpdate();
        }
        System.out.println("✓ Profil admin mis à jour.");
    }

    // ── UNICITE CIN ───────────────────────────────────────────────────
    protected boolean cinExiste(String cin) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE cin = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, cin);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // ── MAPPER ────────────────────────────────────────────────────────
    @Override
    public User mapUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");

        if ("Etudiant".equalsIgnoreCase(role)) {
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

        } else if ("Psychologue".equalsIgnoreCase(role)) {
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

        } else if ("Responsable Etudiant".equalsIgnoreCase(role)) {
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

        } else {
            // ADMIN — capture "ADMIN", "Admin", "admin"
            Admin admin = new Admin();
            admin.setUserId(rs.getInt("user_id"));
            admin.setNom(rs.getString("nom"));
            admin.setPrenom(rs.getString("prenom"));
            admin.setEmail(rs.getString("email"));
            admin.setPassword(rs.getString("password"));
            admin.setCin(rs.getString("cin"));
            admin.setStatut(rs.getString("statut"));
            admin.setActive(rs.getBoolean("is_active"));
            admin.setVerified(rs.getBoolean("is_verified"));
            admin.setCreatedAt(rs.getTimestamp("created_at"));
            return admin;
        }
    }
}