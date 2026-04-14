package org.example.controllers.sponsor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.Sponsor;
import org.example.enums.Role;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeSponsor;
import org.example.services.SponsorService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.stage.FileChooser;

public class ModificationSponsorController {

    private Sponsor sponsorCourant;

    public void setSponsor(Sponsor sponsor) {
        this.sponsorCourant = sponsor;
        remplirFormulaire();
    }

    @FXML
    private TextField txtNom;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelephone;
    @FXML
    private TextArea txtAdresse;
    @FXML
    private TextField txtDomaine;
    @FXML
    private TextField txtSiteWeb;
    @FXML
    private TextField txtLogo;

    @FXML
    private ComboBox<TypeSponsor> comboType;
    @FXML
    private ComboBox<StatutSponsor> comboStatut;

    @FXML
    private Label lblErreurNom;
    @FXML
    private Label lblErreurEmail;
    @FXML
    private Label lblErreurTelephone;
    @FXML
    private Label lblErreurSiteWeb;

    @FXML
    private Label lblSucces;

    private final SponsorService sponsorService = new SponsorService();

    @FXML
    public void initialize() {
        // Vérifier si l'utilisateur est admin
        boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        if (!isAdmin) {
            afficherAlerte("Accès refusé", "L'accès à la modification de sponsors est réservé à l'administrateur. Les responsables gèrent les attributions de sponsors aux événements.");
            naviguerVersRetour();
            return;
        }

        comboType.setItems(FXCollections.observableArrayList(TypeSponsor.values()));
        comboStatut.setItems(FXCollections.observableArrayList(StatutSponsor.values()));

        // Forcer la couleur du texte en noir pour tous les champs
        appliquerCouleurTexteNoir();
    }

    private void appliquerCouleurTexteNoir() {
        // Appliquer le style inline pour forcer le texte noir
        if (txtNom != null) txtNom.setStyle("-fx-text-fill: #000000;");
        if (txtEmail != null) txtEmail.setStyle("-fx-text-fill: #000000;");
        if (txtTelephone != null) txtTelephone.setStyle("-fx-text-fill: #000000;");
        if (txtAdresse != null) txtAdresse.setStyle("-fx-text-fill: #000000;");
        if (txtDomaine != null) txtDomaine.setStyle("-fx-text-fill: #000000;");
        if (txtSiteWeb != null) txtSiteWeb.setStyle("-fx-text-fill: #000000;");
        if (txtLogo != null) txtLogo.setStyle("-fx-text-fill: #000000;");
        if (comboType != null) comboType.setStyle("-fx-text-fill: #000000;");
        if (comboStatut != null) comboStatut.setStyle("-fx-text-fill: #000000;");
    }

    private void naviguerVersRetour() {
        try {
            naviguerVersEcran(null, "/sponsor/GestionSponsor.fxml", "Gestion des Sponsors");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void remplirFormulaire() {
        if (sponsorCourant == null) {
            return;
        }

        txtNom.setText(sponsorCourant.getNomSponsor());
        txtEmail.setText(sponsorCourant.getEmailContact());
        txtTelephone.setText(sponsorCourant.getTelephone() != null ? sponsorCourant.getTelephone() : "");
        txtAdresse.setText(sponsorCourant.getAdresse() != null ? sponsorCourant.getAdresse() : "");
        txtDomaine.setText(sponsorCourant.getDomaineActivite() != null ? sponsorCourant.getDomaineActivite() : "");
        txtSiteWeb.setText(sponsorCourant.getSiteWeb() != null ? sponsorCourant.getSiteWeb() : "");
        txtLogo.setText(sponsorCourant.getLogo() != null ? sponsorCourant.getLogo() : "");

        comboType.setValue(sponsorCourant.getTypeSponsor());
        comboStatut.setValue(sponsorCourant.getStatut());
    }

    @FXML
    private void enregistrerModification(ActionEvent event) {
        cacherErreurs();
        lblSucces.setVisible(false);

        if (sponsorCourant == null) {
            afficherErreur("Aucun sponsor sélectionné");
            return;
        }

        boolean valide = validerFormulaire();
        if (!valide) {
            return;
        }

        try {
            Sponsor sponsorModifie = construireSponsorModifie();
            sponsorService.modifier(sponsorModifie);

            lblSucces.setVisible(true);
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1.5),
                    ae -> lblSucces.setVisible(false)
            ));
            timeline.play();

            try {
                NavigationContext.loadContentInCenter("/sponsor/GestionSponsor.fxml");
            } catch (IOException e) {
                afficherErreur("Erreur lors de la navigation : " + e.getMessage());
            }

        } catch (SQLException e) {
            afficherErreur("Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        try {
            NavigationContext.loadContentInCenter("/sponsor/GestionSponsor.fxml");
        } catch (IOException e) {
            afficherErreur("Erreur lors de la navigation : " + e.getMessage());
        }
    }

    private boolean validerFormulaire() {
        List<String> erreurs = new ArrayList<>();

        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            lblErreurNom.setVisible(true);
            erreurs.add("Nom : obligatoire");
        }

        String email = txtEmail.getText();
        if (email == null || email.trim().isEmpty()) {
            lblErreurEmail.setVisible(true);
            erreurs.add("Email : obligatoire");
        } else if (!validerEmail(email.trim())) {
            lblErreurEmail.setVisible(true);
            erreurs.add("Email : format invalide");
        }

        // Valider téléphone (si renseigné)
        String telephone = txtTelephone.getText();
        if (telephone != null && !telephone.trim().isEmpty()) {
            if (!validerTelephone(telephone.trim())) {
                lblErreurTelephone.setVisible(true);
                erreurs.add("Telephone : doit contenir uniquement des chiffres");
            }
        }

        // Valider site web (si renseigné)
        String siteWeb = txtSiteWeb.getText();
        if (siteWeb != null && !siteWeb.trim().isEmpty()) {
            if (!validerUrl(siteWeb.trim())) {
                lblErreurSiteWeb.setVisible(true);
                erreurs.add("Site web : format URL invalide");
            }
        }

        if (!erreurs.isEmpty()) {
            afficherErreur("Veuillez corriger les erreurs suivantes :\n- " + String.join("\n- ", erreurs));
            return false;
        }

        return true;
    }

    private boolean validerEmail(String email) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        return pattern.matcher(email).matches();
    }

    private boolean validerTelephone(String telephone) {
        // Accepte uniquement des chiffres (avec ou sans espaces)
        String telephoneSansEspaces = telephone.replaceAll("\\s", "");
        Pattern pattern = Pattern.compile("^[0-9]+$");
        return pattern.matcher(telephoneSansEspaces).matches();
    }

    private boolean validerUrl(String url) {
        // Validation basique d'URL
        Pattern pattern = Pattern.compile("^(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
        return pattern.matcher(url).matches();
    }

    private Sponsor construireSponsorModifie() {
        Sponsor s = new Sponsor();
        s.setSponsorId(sponsorCourant.getSponsorId());

        s.setNomSponsor(txtNom.getText().trim());
        s.setEmailContact(txtEmail.getText().trim());
        s.setTelephone(txtTelephone.getText() != null ? txtTelephone.getText().trim() : null);
        s.setAdresse(txtAdresse.getText() != null ? txtAdresse.getText().trim() : null);
        s.setDomaineActivite(txtDomaine.getText() != null ? txtDomaine.getText().trim() : null);
        s.setSiteWeb(txtSiteWeb.getText() != null ? txtSiteWeb.getText().trim() : null);
        s.setLogo(txtLogo.getText() != null ? txtLogo.getText().trim() : null);

        s.setTypeSponsor(comboType.getValue());
        s.setStatut(comboStatut.getValue());

        s.setCreatedAt(sponsorCourant.getCreatedAt());
        s.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return s;
    }

    private void cacherErreurs() {
        lblErreurNom.setVisible(false);
        lblErreurEmail.setVisible(false);
        lblErreurTelephone.setVisible(false);
        lblErreurSiteWeb.setVisible(false);
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    @FXML
    private void parcourirLogo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un logo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog((Stage) ((Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Copier le logo dans le dossier XAMPP
                String dossierXAMPP = "D:\\xampp\\htdocs\\uploadsEvent\\sponsors\\";

                // Créer le dossier s'il n'existe pas
                File dossier = new File(dossierXAMPP);
                if (!dossier.exists()) {
                    dossier.mkdirs();
                }

                // Remplacer les espaces par des tirets dans le nom du fichier
                String originalName = selectedFile.getName();
                String safeName = originalName.replaceAll(" ", "-").replaceAll("'", "-");

                Path source = selectedFile.toPath();
                Path destination = Path.of(dossierXAMPP + safeName);

                // Copier le fichier
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

                // Afficher le nom du fichier dans le TextField
                txtLogo.setText(safeName);
            } catch (IOException e) {
                afficherErreur("Erreur lors de la copie du logo : " + e.getMessage());
            }
        }
    }
}
