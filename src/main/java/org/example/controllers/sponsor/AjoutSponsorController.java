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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.stage.FileChooser;

public class AjoutSponsorController {

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
            afficherAlerte("Accès refusé", "L'accès à l'ajout de sponsors est réservé à l'administrateur. Les responsables gèrent les attributions de sponsors aux événements.");
            naviguerVersRetour();
            return;
        }

        comboType.setItems(FXCollections.observableArrayList(TypeSponsor.values()));
        comboStatut.setItems(FXCollections.observableArrayList(StatutSponsor.values()));

        // Valeurs par défaut
        comboType.setValue(TypeSponsor.ENTREPRISE);
        comboStatut.setValue(StatutSponsor.EN_ATTENTE);

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

    @FXML
    private void enregistrerSponsor(ActionEvent event) {
        cacherErreurs();
        lblSucces.setVisible(false);

        boolean valide = validerFormulaire();
        if (!valide) {
            return;
        }

        try {
            Sponsor sponsor = creerSponsor();
            sponsorService.ajouter(sponsor);

            lblSucces.setVisible(true);
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1.5),
                    ae -> {
                        lblSucces.setVisible(false);
                        try {
                            NavigationContext.loadContentInCenter("/sponsor/GestionSponsor.fxml");
                        } catch (IOException e) {
                            afficherErreur("Erreur lors de la navigation : " + e.getMessage());
                        }
                    }
            ));
            timeline.play();

        } catch (SQLException e) {
            afficherErreur("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        try {
            NavigationContext.loadContentInCenter("/sponsor/GestionSponsor.fxml");
        } catch (IOException e) {
            afficherAlerte("Erreur", "Erreur lors de la navigation : " + e.getMessage());
        }
    }

    private boolean validerFormulaire() {
        List<String> erreurs = new ArrayList<>();

        // Valider nom
        if (txtNom.getText() == null || txtNom.getText().trim().isEmpty()) {
            lblErreurNom.setVisible(true);
            erreurs.add("Nom : obligatoire");
        }

        // Valider email
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
                erreurs.add("Telephone : doit contenir exactement 8 chiffres");
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
            afficherErreur("Le formulaire contient des erreurs. Vérifiez les champs marqués en rouge.");
            return false;
        }

        return true;
    }

    private boolean validerEmail(String email) {
        // Validation plus robuste de l'email
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        return pattern.matcher(email).matches();
    }

    private boolean validerTelephone(String telephone) {
        // Accepte uniquement des chiffres (avec ou sans espaces) et doit avoir exactement 8 chiffres
        String telephoneSansEspaces = telephone.replaceAll("\\s", "");
        Pattern pattern = Pattern.compile("^[0-9]{8}$");
        return pattern.matcher(telephoneSansEspaces).matches();
    }

    private boolean validerUrl(String url) {
        // Validation basique d'URL
        Pattern pattern = Pattern.compile("^(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
        return pattern.matcher(url).matches();
    }

    private Sponsor creerSponsor() {
        String nom = txtNom.getText().trim();
        String email = txtEmail.getText().trim();
        String telephone = txtTelephone.getText() != null ? txtTelephone.getText().trim() : null;
        String adresse = txtAdresse.getText() != null ? txtAdresse.getText().trim() : null;
        String domaine = txtDomaine.getText() != null ? txtDomaine.getText().trim() : null;
        String siteWeb = txtSiteWeb.getText() != null ? txtSiteWeb.getText().trim() : null;
        String logo = txtLogo.getText() != null ? txtLogo.getText().trim() : null;

        TypeSponsor type = comboType.getValue();
        StatutSponsor statut = comboStatut.getValue();

        return new Sponsor(nom, type, siteWeb, email, telephone, adresse, domaine, statut, logo);
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
