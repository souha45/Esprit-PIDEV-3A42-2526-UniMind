package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.*;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.PasswordUtils;
import org.example.utils.ValidationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDashboardController {

    @FXML private Label navNomUser;
    @FXML private ImageView navPhoto;
    @FXML private Button btnProfil;
    @FXML private Button btnDeconnexion;
    @FXML private Label sidebarRole;
    @FXML private VBox panneauProfil;

    // ── Labels infos ──────────────────────────────────────────────────
    @FXML private Label pNomPrenom, pEmail, pRole, pStatut;

    // ── Champs profil ─────────────────────────────────────────────────
    @FXML private TextArea pBio;        // ✅ TextArea (correspond au FXML)
    @FXML private TextField pTel, pPseudo;

    // ── Mot de passe ──────────────────────────────────────────────────
    @FXML private PasswordField pAncienMdp, pNouveauMdp, pConfirmMdp;

    // ── Labels erreurs ────────────────────────────────────────────────
    @FXML private Label pErrTel, pErrMdp, pMessageProfil;

    // ── Photo ─────────────────────────────────────────────────────────
    @FXML private ImageView photoProfile;

    private User utilisateur;
    private String currentPhotoPath = null;

    @FXML
    public void initialize() {
        if (btnProfil != null)
            btnProfil.setStyle(sidebarBtnStyle(true));
    }

    public void setUser(User user) {
        this.utilisateur = user;
        if (navNomUser != null)
            navNomUser.setText(user.getPrenom() + " " + user.getNom());
        if (sidebarRole != null)
            sidebarRole.setText(user.getRole().toString());
        chargerProfil();
    }

    @FXML
    public void afficherProfil() {
        if (panneauProfil != null) {
            panneauProfil.setVisible(true);
            panneauProfil.setManaged(true);
        }
        if (btnProfil != null)
            btnProfil.setStyle(sidebarBtnStyle(true));
        chargerProfil();
    }

    private void chargerProfil() {
        if (utilisateur == null) return;

        if (pNomPrenom != null) pNomPrenom.setText(utilisateur.getPrenom() + " " + utilisateur.getNom());
        if (pEmail != null) pEmail.setText(utilisateur.getEmail());
        if (pRole != null) pRole.setText(utilisateur.getRole().toString());
        if (pStatut != null) pStatut.setText(utilisateur.getStatut());

        try {
            Profil profil = getProfilByUserId(utilisateur.getUserId());
            if (profil != null) {
                if (pBio != null) pBio.setText(profil.getBio() != null ? profil.getBio() : "");
                if (pTel != null) pTel.setText(profil.getTel() != null ? profil.getTel() : "");
                if (pPseudo != null) pPseudo.setText(profil.getPseudo() != null ? profil.getPseudo() : "");

                if (profil.getPhoto() != null && !profil.getPhoto().isEmpty()) {
                    File file = new File(profil.getPhoto());
                    if (file.exists()) {
                        Image img = new Image(file.toURI().toString());
                        if (photoProfile != null) photoProfile.setImage(img);
                        if (navPhoto != null) navPhoto.setImage(img);
                        currentPhotoPath = profil.getPhoto();
                    } else {
                        viderPhoto();
                    }
                } else {
                    viderPhoto();
                }
            }
        } catch (SQLException e) {
            if (pMessageProfil != null) {
                pMessageProfil.setStyle("-fx-text-fill: #DC2626;");
                pMessageProfil.setText("Erreur chargement : " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private void viderPhoto() {
        if (photoProfile != null) photoProfile.setImage(null);
        if (navPhoto != null) navPhoto.setImage(null);
    }

    private Profil getProfilByUserId(int userId) throws SQLException {
        var conn = MyDataBase_Unimind.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM profil WHERE user_id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Profil p = new Profil();
            p.setProfilId(rs.getInt("profil_id"));
            p.setUserId(rs.getInt("user_id"));
            p.setBio(rs.getString("bio"));
            p.setTel(rs.getString("tel"));
            p.setPhoto(rs.getString("photo"));
            p.setPseudo(rs.getString("pseudo"));
            return p;
        }
        return null;
    }

    @FXML
    public void choisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File fichier = fc.showOpenDialog(photoProfile.getScene().getWindow());

        if (fichier != null) {
            try {
                String projectPath = System.getProperty("user.dir");
                Path uploadDir = Paths.get(projectPath, "uploads");

                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                String extension = ".jpg";
                int dotIndex = fichier.getName().lastIndexOf(".");
                if (dotIndex > 0) extension = fichier.getName().substring(dotIndex);

                String nomFichier = "photo_" + utilisateur.getUserId()
                        + "_" + System.currentTimeMillis() + extension;
                Path destination = uploadDir.resolve(nomFichier);

                Files.copy(fichier.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                currentPhotoPath = destination.toAbsolutePath().toString();

                Image img = new Image(fichier.toURI().toString());
                if (photoProfile != null) photoProfile.setImage(img);
                if (navPhoto != null) navPhoto.setImage(img);

                if (pMessageProfil != null) {
                    pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
                    pMessageProfil.setText("✓ Photo sélectionnée, cliquez sur Sauvegarder");
                }

            } catch (IOException e) {
                if (pMessageProfil != null) {
                    pMessageProfil.setStyle("-fx-text-fill: #DC2626;");
                    pMessageProfil.setText("Erreur copie photo : " + e.getMessage());
                }
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void sauvegarderProfil() {
        if (pErrTel != null) pErrTel.setText("");
        if (pMessageProfil != null) pMessageProfil.setText("");

        String tel = pTel != null ? pTel.getText().trim() : "";
        String bio = pBio != null ? pBio.getText().trim() : "";
        String pseudo = pPseudo != null ? pPseudo.getText().trim() : "";

        if (!tel.isEmpty() && !ValidationUtils.isTelephoneValide(tel)) {
            if (pErrTel != null) pErrTel.setText(ValidationUtils.messageTelephone());
            return;
        }

        try {
            var conn = MyDataBase_Unimind.getInstance().getConnection();

            PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM profil WHERE user_id = ?");
            check.setInt(1, utilisateur.getUserId());
            ResultSet rs = check.executeQuery();
            rs.next();
            boolean existe = rs.getInt(1) > 0;

            if (existe) {
                String sql = "UPDATE profil SET bio=?, tel=?, pseudo=?, photo=?, updated_at=NOW() WHERE user_id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, bio);
                ps.setString(2, tel);
                ps.setString(3, pseudo);
                ps.setString(4, currentPhotoPath);
                ps.setInt(5, utilisateur.getUserId());
                ps.executeUpdate();
            } else {
                String sql = "INSERT INTO profil (user_id, bio, tel, pseudo, photo, updated_at) VALUES (?,?,?,?,?,NOW())";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, utilisateur.getUserId());
                ps.setString(2, bio);
                ps.setString(3, tel);
                ps.setString(4, pseudo);
                ps.setString(5, currentPhotoPath);
                ps.executeUpdate();
            }

            if (pMessageProfil != null) {
                pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
                pMessageProfil.setText("✓ Profil mis à jour avec succès !");
            }

        } catch (SQLException e) {
            if (pMessageProfil != null) {
                pMessageProfil.setStyle("-fx-text-fill: #DC2626;");
                pMessageProfil.setText("Erreur : " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @FXML
    public void changerMotDePasse() {
        if (pErrMdp != null) pErrMdp.setText("");
        if (pMessageProfil != null) pMessageProfil.setText("");

        String ancien  = pAncienMdp != null ? pAncienMdp.getText() : "";
        String nouveau = pNouveauMdp != null ? pNouveauMdp.getText() : "";
        String confirm = pConfirmMdp != null ? pConfirmMdp.getText() : "";

        if (!ValidationUtils.isNonVide(ancien) || !ValidationUtils.isNonVide(nouveau)) {
            if (pErrMdp != null) pErrMdp.setText("Tous les champs sont obligatoires");
            return;
        }
        if (!ValidationUtils.isPasswordValide(nouveau)) {
            if (pErrMdp != null) pErrMdp.setText(ValidationUtils.messagePassword());
            return;
        }
        if (!nouveau.equals(confirm)) {
            if (pErrMdp != null) pErrMdp.setText(ValidationUtils.messagePasswordConfirm());
            return;
        }

        try {
            var conn = MyDataBase_Unimind.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT password FROM user WHERE user_id = ?");
            ps.setInt(1, utilisateur.getUserId());
            ResultSet rs = ps.executeQuery();

            if (rs.next() && PasswordUtils.verifier(ancien, rs.getString("password"))) {
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE user SET password = ? WHERE user_id = ?");
                update.setString(1, PasswordUtils.hasher(nouveau));
                update.setInt(2, utilisateur.getUserId());
                update.executeUpdate();

                if (pMessageProfil != null) {
                    pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
                    pMessageProfil.setText("✓ Mot de passe modifié avec succès !");
                }
                if (pAncienMdp != null) pAncienMdp.clear();
                if (pNouveauMdp != null) pNouveauMdp.clear();
                if (pConfirmMdp != null) pConfirmMdp.clear();
            } else {
                if (pErrMdp != null) pErrMdp.setText("Ancien mot de passe incorrect");
            }

        } catch (SQLException e) {
            if (pErrMdp != null) pErrMdp.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void seDeconnecter() {
        try {
            Stage stage = (Stage) navNomUser.getScene().getWindow();
            stage.close();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("UniMind - Connexion");
            loginStage.setScene(new Scene(loader.load(), 500, 420));
            loginStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sidebarBtnStyle(boolean actif) {
        if (actif)
            return "-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-pref-width: 210; -fx-pref-height: 42; -fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 13;";
        return "-fx-background-color: transparent; -fx-text-fill: #E9D5FF; " +
                "-fx-pref-width: 210; -fx-pref-height: 42; -fx-alignment: CENTER_LEFT; " +
                "-fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 13;";
    }
}