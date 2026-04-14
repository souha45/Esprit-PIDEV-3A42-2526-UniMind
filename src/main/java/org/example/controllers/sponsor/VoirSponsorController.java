package org.example.controllers.sponsor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entities.Sponsor;
import org.example.enums.Role;
import org.example.services.SponsorService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class VoirSponsorController {

    private Sponsor sponsorCourant;
    private final SponsorService sponsorService = new SponsorService();

    @FXML
    private Label lblNom;
    @FXML
    private Label lblType;
    @FXML
    private Label lblStatut;
    @FXML
    private Label lblTelephone;
    @FXML
    private Label lblEmail;
    @FXML
    private Label lblSiteWeb;
    @FXML
    private Label lblDomaine;
    @FXML
    private TextArea txtAdresse;
    @FXML
    private ImageView imgLogo;
    @FXML
    private Label lblPasLogo;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    public void setSponsor(Sponsor sponsor) {
        this.sponsorCourant = sponsor;
        afficherDetails();
    }

    private void afficherDetails() {
        if (sponsorCourant == null) {
            return;
        }

        lblNom.setText(sponsorCourant.getNomSponsor() != null ? sponsorCourant.getNomSponsor() : "-");
        lblType.setText(sponsorCourant.getTypeSponsor() != null ? sponsorCourant.getTypeSponsor().toString() : "-");
        lblStatut.setText(sponsorCourant.getStatut() != null ? sponsorCourant.getStatut().toString() : "-");
        lblTelephone.setText(sponsorCourant.getTelephone() != null ? sponsorCourant.getTelephone() : "-");
        lblEmail.setText(sponsorCourant.getEmailContact() != null ? sponsorCourant.getEmailContact() : "-");
        lblSiteWeb.setText(sponsorCourant.getSiteWeb() != null ? sponsorCourant.getSiteWeb() : "-");
        lblDomaine.setText(sponsorCourant.getDomaineActivite() != null ? sponsorCourant.getDomaineActivite() : "-");
        txtAdresse.setText(sponsorCourant.getAdresse() != null ? sponsorCourant.getAdresse() : "");

        // Charger et afficher le logo
        String logoName = sponsorCourant.getLogo();
        if (logoName != null && !logoName.trim().isEmpty()) {
            try {
                // Construire l'URL complète avec XAMPP : localhost/uploadsEvent/sponsors/
                // Encoder le nom du fichier pour gérer les espaces et caractères spéciaux
                String encodedLogoName = URLEncoder.encode(logoName.trim(), StandardCharsets.UTF_8);
                String fullLogoUrl = "http://localhost/uploadsEvent/sponsors/" + encodedLogoName;
                Image logo = new Image(fullLogoUrl, true);
                imgLogo.setImage(logo);
                imgLogo.setVisible(true);
                lblPasLogo.setVisible(false);
            } catch (Exception e) {
                imgLogo.setVisible(false);
                lblPasLogo.setVisible(true);
                lblPasLogo.setText("Erreur lors du chargement du logo");
            }
        } else {
            imgLogo.setVisible(false);
            lblPasLogo.setVisible(true);
            lblPasLogo.setText("Aucun logo");
        }

        // Vérifier les permissions : seul l'admin peut modifier/supprimer les sponsors
        verifierPermissions();
    }

    private void verifierPermissions() {
        // Vérifier si l'utilisateur est admin
        boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        // Cacher les boutons si ce n'est pas l'admin
        btnModifier.setVisible(isAdmin);
        btnSupprimer.setVisible(isAdmin);
    }

    @FXML
    private void retour(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/sponsor/GestionSponsor.fxml");
    }

    @FXML
    private void modifier(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sponsor/ModificationSponsor.fxml"));
            Parent root = loader.load();

            ModificationSponsorController controller = loader.getController();
            controller.setSponsor(sponsorCourant);

            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de modification: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de modifier le sponsor: " + e.getMessage());
        }
    }

    @FXML
    private void supprimer(ActionEvent event) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le sponsor");
            confirmation.setContentText("Voulez-vous vraiment supprimer le sponsor \"" + sponsorCourant.getNomSponsor() + "\" ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                sponsorService.supprimer(sponsorCourant.getSponsorId());
                afficherAlerte("Succès", "Sponsor supprimé avec succès");
                try {
                    retour(event);
                } catch (IOException e) {
                    afficherAlerte("Erreur", "Erreur lors de la navigation: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer le sponsor: " + e.getMessage());
        }
    }

    private void naviguerVersEcran(ActionEvent event, String fxmlPath, String titre) throws IOException {
        var resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
        }
        Parent root = FXMLLoader.load(resource);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle(titre);
        stage.show();
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
