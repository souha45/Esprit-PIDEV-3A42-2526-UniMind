package org.example.controllers.evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.enums.Role;
import org.example.services.EvenementService;
import org.example.services.FavoriService;
import org.example.services.ParticipationService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class VoirEvenementController {

    private Evenement evenementCourant;
    private final EvenementService evenementService = new EvenementService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblType;
    @FXML
    private Label lblStatut;
    @FXML
    private Label lblCapacite;
    @FXML
    private Label lblDateDebut;
    @FXML
    private Label lblDateFin;
    @FXML
    private Label lblLieu;
    @FXML
    private TextArea txtDescription;
    @FXML
    private Label lblOrganisateur;
    @FXML
    private ImageView imgEvenement;
    @FXML
    private Label lblPasImage;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnParticiper;
    @FXML
    private Label lblDejaInscrit;
    @FXML
    private Button btnFavori;

    public void setEvenement(Evenement evenement) {
        this.evenementCourant = evenement;
        afficherDetails();
    }

    private void afficherDetails() {
        if (evenementCourant == null) {
            return;
        }

        lblTitre.setText(evenementCourant.getTitre() != null ? evenementCourant.getTitre() : "-");
        lblType.setText(evenementCourant.getType() != null ? evenementCourant.getType().toString() : "-");
        lblStatut.setText(evenementCourant.getStatut() != null ? evenementCourant.getStatut().toString() : "-");
        lblCapacite.setText(String.valueOf(evenementCourant.getCapaciteMax()));
        lblDateDebut.setText(evenementCourant.getDateDebut() != null ? evenementCourant.getDateDebut().toLocalDateTime().format(dateFormatter) : "-");
        lblDateFin.setText(evenementCourant.getDateFin() != null ? evenementCourant.getDateFin().toLocalDateTime().format(dateFormatter) : "-");
        lblLieu.setText(evenementCourant.getLieu() != null ? evenementCourant.getLieu() : "-");
        txtDescription.setText(evenementCourant.getDescription() != null ? evenementCourant.getDescription() : "");

        // Afficher le nom de l'organisateur au lieu de l'ID
        try {
            String nomOrganisateur = evenementService.getNomOrganisateur(evenementCourant.getOrganisateurId());
            lblOrganisateur.setText(nomOrganisateur);
        } catch (SQLException e) {
            lblOrganisateur.setText("Erreur lors de la récupération du nom");
        }

        // Charger et afficher l'image
        String imageName = evenementCourant.getImage();
        if (imageName != null && !imageName.trim().isEmpty()) {
            try {
                // Construire l'URL complète avec XAMPP : localhost/uploadsEvent/evenements/
                // Encoder le nom du fichier pour gérer les espaces et caractères spéciaux
                String encodedImageName = URLEncoder.encode(imageName.trim(), StandardCharsets.UTF_8);
                String fullImageUrl = "http://localhost/uploadsEvent/evenements/" + encodedImageName;
                Image image = new Image(fullImageUrl, true);
                imgEvenement.setImage(image);
                imgEvenement.setVisible(true);
                lblPasImage.setVisible(false);
            } catch (Exception e) {
                imgEvenement.setVisible(false);
                lblPasImage.setVisible(true);
                lblPasImage.setText("Erreur lors du chargement de l'image");
            }
        } else {
            imgEvenement.setVisible(false);
            lblPasImage.setVisible(true);
            lblPasImage.setText("Aucune image");
        }

        // Vérifier les permissions : cacher les boutons si l'utilisateur n'est pas l'organisateur (sauf admin)
        verifierPermissions();
    }

    private void verifierPermissions() {
        // Vérifier le rôle de l'utilisateur
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);
        int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
        boolean isAdmin = (role == Role.ADMIN);
        boolean estOrganisateur = (evenementCourant.getOrganisateurId() == currentUserId);
        boolean estEtudiant = (role == Role.ETUDIANT);

        // Pour les étudiants : afficher les boutons Participer et Favori
        if (estEtudiant) {
            // Vérifier si l'étudiant est déjà inscrit
            boolean dejaInscrit = estDejaInscrit(currentUserId);
            if (dejaInscrit) {
                btnParticiper.setVisible(false);
                lblDejaInscrit.setVisible(true);
            } else {
                btnParticiper.setVisible(true);
                lblDejaInscrit.setVisible(false);
            }
            btnFavori.setVisible(true);
            btnModifier.setVisible(false);
            btnSupprimer.setVisible(false);
            return;
        }

        // Pour les responsables : vérifier si l'événement leur appartient
        if (role == Role.RESPONSABLE_ETUDIANT) {
            btnParticiper.setVisible(false);
            lblDejaInscrit.setVisible(false);
            btnFavori.setVisible(false);
            btnModifier.setVisible(estOrganisateur);
            btnSupprimer.setVisible(estOrganisateur);
            return;
        }

        // Si admin, toujours afficher les boutons Modifier et Supprimer
        if (isAdmin) {
            btnParticiper.setVisible(false);
            lblDejaInscrit.setVisible(false);
            btnFavori.setVisible(false);
            btnModifier.setVisible(true);
            btnSupprimer.setVisible(true);
        }
    }

    private boolean estDejaInscrit(int etudiantId) {
        try {
            ParticipationService participationService = new ParticipationService();
            return participationService.verifierUnicite(evenementCourant.getEvenementId(), etudiantId);
        } catch (SQLException e) {
            return false;
        }
    }

    @FXML
    private void retour(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/evenement/GestionEvenement.fxml");
    }

    @FXML
    private void modifier(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evenement/ModificationEvenement.fxml"));
            Parent root = loader.load();

            ModificationEvenementController controller = loader.getController();
            controller.setEvenement(evenementCourant);

            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de modification: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de modifier l'événement: " + e.getMessage());
        }
    }

    @FXML
    private void supprimer(ActionEvent event) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer l'événement");
            confirmation.setContentText("Voulez-vous vraiment supprimer l'événement \"" + evenementCourant.getTitre() + "\" ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                evenementService.supprimer(evenementCourant.getEvenementId());
                afficherAlerte("Succès", "Événement supprimé avec succès");
                try {
                    retour(event);
                } catch (IOException e) {
                    afficherAlerte("Erreur", "Erreur lors de la navigation: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer l'événement: " + e.getMessage());
        }
    }

    @FXML
    private void participer(ActionEvent event) {
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (currentUserId == -1) {
                afficherAlerte("Erreur", "Utilisateur non connecté");
                return;
            }

            ParticipationService participationService = new ParticipationService();
            Participation participation = new Participation();
            participation.setEvenementId(evenementCourant.getEvenementId());
            participation.setEtudiantId(currentUserId);
            participation.setDateInscription(new java.sql.Timestamp(System.currentTimeMillis()));
            participation.setStatut(org.example.enums.StatutParticipation.CONFIRME);

            participationService.ajouter(participation);
            afficherAlerte("Succès", "Vous êtes inscrit à l'événement \"" + evenementCourant.getTitre() + "\"");
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de participer à l'événement: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterFavori(ActionEvent event) {
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (currentUserId == -1) {
                afficherAlerte("Erreur", "Utilisateur non connecté");
                return;
            }

            FavoriService favoriService = new FavoriService();

            // Vérifier si l'événement est déjà dans les favoris
            if (favoriService.verifierUnicite(evenementCourant.getEvenementId(), currentUserId)) {
                afficherAlerte("Information", "Cet événement est déjà dans vos favoris");
                return;
            }

            org.example.entities.Favori favori = new org.example.entities.Favori();
            favori.setEvenementId(evenementCourant.getEvenementId());
            favori.setEtudiantId(currentUserId);

            favoriService.ajouter(favori);
            afficherAlerte("Succès", "Événement ajouté aux favoris");
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible d'ajouter aux favoris: " + e.getMessage());
        }
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
