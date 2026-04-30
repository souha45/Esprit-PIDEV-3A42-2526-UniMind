package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.Etudiant;
import org.example.entities.Psychologue;
import org.example.entities.ResponsableEtudiant;
import org.example.entities.User;
import org.example.services.*;
import org.example.utils.ValidationUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfilController {

    @FXML private Label pNomPrenom, pEmail, pRole, pStatut;
    @FXML private TextField pBio, pTel, pPseudo;
    @FXML private PasswordField pAncienMdp, pNouveauMdp, pConfirmMdp;
    @FXML private Label pErrTel, pErrMdp, pMessageProfil;
    @FXML private ImageView photoProfile;
    @FXML private Button btnRetour;

    // Champs spécifiques selon le rôle
    @FXML private VBox sectionSpecifique;
    @FXML private Label labelSpecifique1, labelSpecifique2;
    @FXML private TextField fieldSpecifique1, fieldSpecifique2;

    private User utilisateurConnecte;
    private AdminService adminService = new AdminService();
    private EtudiantService etudiantService = new EtudiantService();
    private PsychologueService psychologueService = new PsychologueService();
    private ResponsableService responsableService = new ResponsableService();
    private Image defaultAvatar;
    private String currentPhotoPath = null;

    private Connection getConnection() {
        return adminService.getConnection();
    }

    private Image getDefaultAvatar() {
        if (defaultAvatar == null) {
            try {
                InputStream is = getClass().getResourceAsStream("/images/default_avatar.png");
                if (is == null) {
                    is = getClass().getResourceAsStream("/images/default_avatar.png");
                }
                if (is != null) {
                    defaultAvatar = new Image(is);
                }
            } catch (Exception e) {
                defaultAvatar = null;
            }
        }
        return defaultAvatar;
    }

    public void setUser(User user) {
        if (user == null) {
            System.err.println("Erreur: setUser() appelé avec user = null");
            return;
        }
        this.utilisateurConnecte = user;
        System.out.println("ProfilController.setUser() - User: " + user.getPrenom() + " " + user.getNom());

        afficherInfos();
        chargerChampsSpecifiques();
        chargerProfil();
        chargerPhoto();

        // ── Ajustement de la fenêtre après chargement des données ──
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) photoProfile.getScene().getWindow();
            stage.setWidth(780);
            stage.setHeight(700);
            stage.centerOnScreen();
        });

    }

    private void afficherInfos() {
        if (utilisateurConnecte == null) {
            System.err.println("afficherInfos: utilisateurConnecte est null");
            return;
        }
        pNomPrenom.setText(utilisateurConnecte.getPrenom() + " " + utilisateurConnecte.getNom());
        pEmail.setText(utilisateurConnecte.getEmail());
        pRole.setText(utilisateurConnecte.getRole().toString());
        pStatut.setText(utilisateurConnecte.getStatut());
    }

    private void chargerChampsSpecifiques() {
        if (utilisateurConnecte == null || sectionSpecifique == null) return;

        try {
            if (utilisateurConnecte instanceof Etudiant) {
                Etudiant e = (Etudiant) utilisateurConnecte;
                sectionSpecifique.setVisible(true);
                labelSpecifique1.setText("Identifiant étudiant :");
                labelSpecifique2.setText("Établissement :");
                fieldSpecifique1.setText(e.getIdentifiant());
                fieldSpecifique2.setText(e.getNomEtablissement());
            }
            else if (utilisateurConnecte instanceof Psychologue) {
                Psychologue p = (Psychologue) utilisateurConnecte;
                sectionSpecifique.setVisible(true);
                labelSpecifique1.setText("Spécialité :");
                labelSpecifique2.setText("Adresse :");
                fieldSpecifique1.setText(p.getSpecialite());
                fieldSpecifique2.setText(p.getAdresse());
            }
            else if (utilisateurConnecte instanceof ResponsableEtudiant) {
                ResponsableEtudiant r = (ResponsableEtudiant) utilisateurConnecte;
                sectionSpecifique.setVisible(true);
                labelSpecifique1.setText("Poste :");
                labelSpecifique2.setText("Établissement :");
                fieldSpecifique1.setText(r.getPoste());
                fieldSpecifique2.setText(r.getEtablissement());
            }
            else {
                sectionSpecifique.setVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerPhoto() {
        if (utilisateurConnecte == null) return;

        String photoPath = getPhotoChemin();
        if (photoPath != null && !photoPath.isEmpty()) {
            File file = new File(photoPath);
            if (file.exists()) {
                photoProfile.setImage(new Image(file.toURI().toString()));
                currentPhotoPath = photoPath;
                return;
            }
        }
        Image def = getDefaultAvatar();
        if (def != null) photoProfile.setImage(def);
    }

    private String getPhotoChemin() {
        if (utilisateurConnecte == null) return null;

        String query = "SELECT photo FROM profil WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, utilisateurConnecte.getUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("photo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void chargerProfil() {
        if (utilisateurConnecte == null) {
            System.err.println("chargerProfil: utilisateurConnecte est null");
            return;
        }

        String query = "SELECT bio, tel, pseudo FROM profil WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, utilisateurConnecte.getUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                pBio.setText(rs.getString("bio") != null ? rs.getString("bio") : "");
                pTel.setText(rs.getString("tel") != null ? rs.getString("tel") : "");
                pPseudo.setText(rs.getString("pseudo") != null ? rs.getString("pseudo") : "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void choisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.avif"));
        File fichier = fc.showOpenDialog(photoProfile.getScene().getWindow());

        if (fichier != null) {
            try {
                // Dossier uploads ABSOLU à côté du jar / répertoire de travail
                File uploadDir = new File(System.getProperty("user.dir"), "uploads");
                if (!uploadDir.exists()) uploadDir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + fichier.getName();
                File destination = new File(uploadDir, fileName);

                Files.copy(fichier.toPath(), destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                // On stocke le chemin ABSOLU
                currentPhotoPath = destination.getAbsolutePath();

                photoProfile.setImage(new Image(destination.toURI().toString()));
                pMessageProfil.setStyle("-fx-text-fill: green;");
                pMessageProfil.setText("✓ Photo sélectionnée, cliquez sur Sauvegarder");

            } catch (IOException e) {
                pMessageProfil.setStyle("-fx-text-fill: red;");
                pMessageProfil.setText("Erreur copie fichier : " + e.getMessage());
            }
        }
    }

    @FXML
    public void sauvegarderProfil() {
        if (utilisateurConnecte == null) {
            pMessageProfil.setStyle("-fx-text-fill: red;");
            pMessageProfil.setText("Erreur: utilisateur non connecté");
            return;
        }

        pErrTel.setText("");
        pMessageProfil.setText("");

        String tel = pTel.getText().trim();
        String bio = pBio.getText().trim();
        String pseudo = pPseudo.getText().trim();

        if (!tel.isEmpty() && !ValidationUtils.isTelephoneValide(tel)) {
            pErrTel.setText(ValidationUtils.messageTelephone());
            return;
        }

        try {
            String checkQuery = "SELECT COUNT(*) FROM profil WHERE user_id = ?";
            PreparedStatement check = getConnection().prepareStatement(checkQuery);
            check.setInt(1, utilisateurConnecte.getUserId());
            ResultSet rs = check.executeQuery();
            rs.next();
            boolean exists = rs.getInt(1) > 0;

            if (exists) {
                String update = "UPDATE profil SET bio=?, tel=?, pseudo=?, photo=?, updated_at=NOW() WHERE user_id=?";
                PreparedStatement ps = getConnection().prepareStatement(update);
                ps.setString(1, bio);
                ps.setString(2, tel);
                ps.setString(3, pseudo);
                ps.setString(4, currentPhotoPath);
                ps.setInt(5, utilisateurConnecte.getUserId());
                ps.executeUpdate();
            } else {
                String insert = "INSERT INTO profil (user_id, bio, tel, pseudo, photo, updated_at) VALUES (?,?,?,?,?,NOW())";
                PreparedStatement ps = getConnection().prepareStatement(insert);
                ps.setInt(1, utilisateurConnecte.getUserId());
                ps.setString(2, bio);
                ps.setString(3, tel);
                ps.setString(4, pseudo);
                ps.setString(5, currentPhotoPath);
                ps.executeUpdate();
            }

            // Sauvegarder les champs spécifiques selon le rôle
            sauvegarderChampsSpecifiques();

            pMessageProfil.setStyle("-fx-text-fill: green;");
            pMessageProfil.setText("✓ Profil mis à jour avec succès !");
        } catch (SQLException e) {
            pMessageProfil.setStyle("-fx-text-fill: red;");
            pMessageProfil.setText("Erreur : " + e.getMessage());
        }
        if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
            try {
                File photoFile = new File(currentPhotoPath);
                BufferedImage bufferedImage = ImageIO.read(photoFile);
                FacialRecognitionService facialService = new FacialRecognitionService();
                boolean registered = facialService.registerFace(bufferedImage, utilisateurConnecte.getUserId());
                if (registered) {
                    System.out.println("✅ Visage enregistré pour la reconnaissance faciale");
                } else {
                    System.out.println("⚠️ Impossible d'enregistrer le visage");
                }
            } catch (Exception e) {
                System.err.println("Erreur enregistrement visage: " + e.getMessage());
            }
        }
    }

    private void sauvegarderChampsSpecifiques() throws SQLException {
        if (utilisateurConnecte == null) return;

        if (utilisateurConnecte instanceof Etudiant && fieldSpecifique1 != null) {
            String update = "UPDATE user SET identifiant=?, nom_etablissement=? WHERE user_id=?";
            PreparedStatement ps = getConnection().prepareStatement(update);
            ps.setString(1, fieldSpecifique1.getText());
            ps.setString(2, fieldSpecifique2.getText());
            ps.setInt(3, utilisateurConnecte.getUserId());
            ps.executeUpdate();
        }
        else if (utilisateurConnecte instanceof Psychologue && fieldSpecifique1 != null) {
            String update = "UPDATE user SET specialite=?, adresse=? WHERE user_id=?";
            PreparedStatement ps = getConnection().prepareStatement(update);
            ps.setString(1, fieldSpecifique1.getText());
            ps.setString(2, fieldSpecifique2.getText());
            ps.setInt(3, utilisateurConnecte.getUserId());
            ps.executeUpdate();
        }
        else if (utilisateurConnecte instanceof ResponsableEtudiant && fieldSpecifique1 != null) {
            String update = "UPDATE user SET poste=?, etablissement=? WHERE user_id=?";
            PreparedStatement ps = getConnection().prepareStatement(update);
            ps.setString(1, fieldSpecifique1.getText());
            ps.setString(2, fieldSpecifique2.getText());
            ps.setInt(3, utilisateurConnecte.getUserId());
            ps.executeUpdate();
        }
    }

    @FXML
    public void changerMotDePasse() {
        if (utilisateurConnecte == null) {
            pErrMdp.setText("Erreur: utilisateur non connecté");
            return;
        }

        pErrMdp.setText("");
        pMessageProfil.setText("");

        String ancien = pAncienMdp.getText();
        String nouveau = pNouveauMdp.getText();
        String confirm = pConfirmMdp.getText();

        if (!ValidationUtils.isNonVide(ancien) || !ValidationUtils.isNonVide(nouveau)) {
            pErrMdp.setText("Tous les champs sont obligatoires");
            return;
        }
        if (!ValidationUtils.isPasswordValide(nouveau)) {
            pErrMdp.setText(ValidationUtils.messagePassword());
            return;
        }
        if (!nouveau.equals(confirm)) {
            pErrMdp.setText(ValidationUtils.messagePasswordConfirm());
            return;
        }

        try {
            String role = utilisateurConnecte.getRole().toString();
            switch (role) {
                case "ADMIN":
                    adminService.changerMotDePasse(utilisateurConnecte.getUserId(), ancien, nouveau);
                    break;
                case "ETUDIANT":
                    etudiantService.changerMotDePasse(utilisateurConnecte.getUserId(), ancien, nouveau);
                    break;
                case "PSYCHOLOGUE":
                    psychologueService.changerMotDePasse(utilisateurConnecte.getUserId(), ancien, nouveau);
                    break;
                default:
                    responsableService.changerMotDePasse(utilisateurConnecte.getUserId(), ancien, nouveau);
                    break;
            }
            pAncienMdp.clear();
            pNouveauMdp.clear();
            pConfirmMdp.clear();
            pMessageProfil.setStyle("-fx-text-fill: green;");
            pMessageProfil.setText("✓ Mot de passe modifié !");
        } catch (SQLException e) {
            pErrMdp.setText(e.getMessage());
        }
    }

    @FXML
    public void retour() {
        if (utilisateurConnecte == null) {
            System.err.println("retour: utilisateurConnecte est null");
            return;
        }

        try {
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.close();
            String role = utilisateurConnecte.getRole().toString();
            String fxmlPath;
            switch (role) {
                case "ADMIN":
                    fxmlPath = "/admin_dashboard.fxml";
                    break;
                case "ETUDIANT":
                    fxmlPath = "/dashboard_etudiant.fxml";
                    break;
                case "PSYCHOLOGUE":
                    fxmlPath = "/dashboard_psychologue.fxml";
                    break;
                default:
                    fxmlPath = "/dashboard_responsable.fxml";
                    break;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Stage newStage = new Stage();
            newStage.setTitle("UniMind");
            newStage.setScene(new Scene(loader.load()));
            Object controller = loader.getController();
            if (controller instanceof BaseDashboardController) {
                ((BaseDashboardController) controller).setUser(utilisateurConnecte);
            }
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void enregistrerVisage() {
        if (utilisateurConnecte == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une photo de visage");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png")
        );

        File faceFile = fileChooser.showOpenDialog(photoProfile.getScene().getWindow());
        if (faceFile != null) {
            try {
                BufferedImage faceImage = ImageIO.read(faceFile);
                FacialRecognitionService facialService = new FacialRecognitionService();
                boolean registered = facialService.registerFace(faceImage, utilisateurConnecte.getUserId());
                if (registered) {
                    pMessageProfil.setStyle("-fx-text-fill: green;");
                    pMessageProfil.setText("✓ Visage enregistré avec succès !");
                } else {
                    pMessageProfil.setStyle("-fx-text-fill: red;");
                    pMessageProfil.setText("✗ Aucun visage détecté dans l'image");
                }
            } catch (Exception e) {
                pMessageProfil.setStyle("-fx-text-fill: red;");
                pMessageProfil.setText("Erreur: " + e.getMessage());
            }
        }
    }
    @FXML
    public void seDeconnecter() {
        try {
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("UniMind - Connexion");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}