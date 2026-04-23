package org.example.controllers.evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VoirEvenementController {

    private Evenement evenementCourant;
    private String pagePrecedente; // Pour savoir où retourner
    private boolean estFavori; // Pour suivre l'état favori
    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private final FavoriService favoriService = new FavoriService();
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
    @FXML
    private Button btnLaisserAvis;
    @FXML
    private Label lblAvisDonne;
    @FXML
    private VBox vboxAvis;
    @FXML
    private Label lblNoteMoyenne;
    @FXML
    private Label lblNbAvis;
    @FXML
    private Label lblAucunAvis;

    @FXML
    private void initialize() {
        lierManagedAuVisible(btnParticiper);
        lierManagedAuVisible(lblDejaInscrit);
        lierManagedAuVisible(btnFavori);
        lierManagedAuVisible(btnLaisserAvis);
        lierManagedAuVisible(lblAvisDonne);
        lierManagedAuVisible(btnModifier);
        lierManagedAuVisible(btnSupprimer);
    }

    private void lierManagedAuVisible(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        node.managedProperty().bind(node.visibleProperty());
    }

    public void setEvenement(Evenement evenement) {
        // Recharger l'événement depuis la base de données pour s'assurer que toutes les données sont complètes
        try {
            Evenement evenementReloaded = evenementService.findById(evenement.getEvenementId());
            if (evenementReloaded != null) {
                this.evenementCourant = evenementReloaded;
            } else {
                this.evenementCourant = evenement;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du rechargement de l'événement: " + e.getMessage());
            this.evenementCourant = evenement;
        }
        afficherDetails();
    }

    public void setPagePrecedente(String pagePrecedente) {
        this.pagePrecedente = pagePrecedente;
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

        // Vérifier si l'événement est déjà dans les favoris
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (currentUserId > 0) {
                estFavori = favoriService.verifierUnicite(evenementCourant.getEvenementId(), currentUserId);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des favoris: " + e.getMessage());
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

        // Mettre à jour le bouton favori selon l'état initial
        mettreAJourBoutonFavori();

        // Charger les avis
        chargerAvis();
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
            
            // Vérifier si l'événement est terminé
            boolean evenementTermine = evenementCourant.getDateFin() != null 
                    && evenementCourant.getDateFin().toLocalDateTime().isBefore(java.time.LocalDateTime.now());
            
            if (dejaInscrit) {
                btnParticiper.setVisible(false);
                lblDejaInscrit.setVisible(true);
                
                // Vérifier si l'étudiant a déjà laissé un avis
                Participation participationEtudiant = null;
                try {
                    for (Participation p : participationService.afficher()) {
                        if (p.getEvenementId() == evenementCourant.getEvenementId() && p.getEtudiantId() == currentUserId) {
                            participationEtudiant = p;
                            break;
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la récupération de la participation: " + e.getMessage());
                }
                
                // Afficher le bouton laisser avis ou le label avis donné
                if (evenementTermine) {
                    if (participationEtudiant != null && participationEtudiant.hasFeedback()) {
                        btnLaisserAvis.setVisible(false);
                        lblAvisDonne.setVisible(true);
                    } else {
                        btnLaisserAvis.setVisible(true);
                        lblAvisDonne.setVisible(false);
                    }
                } else {
                    btnLaisserAvis.setVisible(false);
                    lblAvisDonne.setVisible(false);
                }
            } else {
                btnParticiper.setVisible(true);
                lblDejaInscrit.setVisible(false);
                btnLaisserAvis.setVisible(false);
                lblAvisDonne.setVisible(false);
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
            btnLaisserAvis.setVisible(false);
            lblAvisDonne.setVisible(false);
            btnModifier.setVisible(estOrganisateur);
            btnSupprimer.setVisible(estOrganisateur);
            return;
        }

        // Si admin, toujours afficher les boutons Modifier et Supprimer
        if (isAdmin) {
            btnParticiper.setVisible(false);
            lblDejaInscrit.setVisible(false);
            btnFavori.setVisible(false);
            btnLaisserAvis.setVisible(false);
            lblAvisDonne.setVisible(false);
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
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);

        // Si une page précédente est définie, y retourner
        if (pagePrecedente != null && !pagePrecedente.isEmpty()) {
            NavigationContext.loadContentInCenter(pagePrecedente);
            return;
        }

        // Sinon, utiliser le comportement par défaut selon le rôle
        if (role == Role.ETUDIANT) {
            NavigationContext.loadContentInCenter("/participation/ParticipationsEtudiant.fxml");
        } else if (role == Role.RESPONSABLE_ETUDIANT) {
            NavigationContext.loadContentInCenter("/evenement/GestionEvenement.fxml");
        } else {
            // Admin utilise AdminDashboardController
            org.example.controllers.admin.AdminDashboardController.loadContent("/evenement/GestionEvenement.fxml");
        }
    }

    @FXML
    private void modifier(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evenement/ModificationEvenement.fxml"));
            Parent root = loader.load();

            ModificationEvenementController controller = loader.getController();
            controller.setEvenement(evenementCourant);

            NavigationContext.loadContentInCenter(root);
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

            // Vérifier la capacité maximale de l'événement
            if (!verifierCapaciteEvenement(evenementCourant.getEvenementId())) {
                afficherAlerte("Erreur", "Cet événement a atteint sa capacité maximale");
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

            // Rediriger vers la page des événements
            try {
                NavigationContext.loadContentInCenter("/evenement/EvenementsEtudiant.fxml");
            } catch (IOException ioException) {
                afficherAlerte("Erreur", "Erreur lors de la navigation: " + ioException.getMessage());
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de participer à l'événement: " + e.getMessage());
        }
    }

    /**
     * Vérifie si l'événement a encore de la place disponible
     * @param evenementId L'ID de l'événement
     * @return true si l'événement a encore de la place, false sinon
     */
    private boolean verifierCapaciteEvenement(int evenementId) throws SQLException {
        // Récupérer la capacité maximale de l'événement
        String sqlCapacite = "SELECT capacite_max FROM evenement WHERE evenement_id = ?";
        int capaciteMax = 0;
        try (PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sqlCapacite)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    capaciteMax = rs.getInt("capacite_max");
                }
            }
        }

        // Si capacite_max est 0 ou null, pas de limite
        if (capaciteMax <= 0) {
            return true;
        }

        // Compter le nombre actuel de participations pour cet événement
        String sqlCount = "SELECT COUNT(*) as nombre_participations FROM participation WHERE evenement_id = ?";
        int nombreParticipations = 0;
        try (PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sqlCount)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nombreParticipations = rs.getInt("nombre_participations");
                }
            }
        }

        // Vérifier si le nombre de participations est inférieur à la capacité maximale
        return nombreParticipations < capaciteMax;
    }

    @FXML
    private void ajouterFavori(ActionEvent event) {
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (currentUserId == -1) {
                afficherAlerte("Erreur", "Utilisateur non connecté");
                return;
            }

            if (estFavori) {
                // Supprimer des favoris
                supprimerFavori();
            } else {
                // Ajouter aux favoris
                org.example.entities.Favori favori = new org.example.entities.Favori();
                favori.setEvenementId(evenementCourant.getEvenementId());
                favori.setEtudiantId(currentUserId);
                favoriService.ajouter(favori);
                estFavori = true;
                afficherAlerte("Succès", "Événement ajouté aux favoris");
            }
            // Mettre à jour le bouton
            mettreAJourBoutonFavori();
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible d'ajouter aux favoris: " + e.getMessage());
        }
    }

    private void supprimerFavori() throws SQLException {
        int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
        if (currentUserId <= 0) {
            return;
        }

        // Trouver l'ID du favori
        int favoriId = -1;
        for (org.example.entities.Favori f : favoriService.afficher()) {
            if (f.getEvenementId() == evenementCourant.getEvenementId() && f.getEtudiantId() == currentUserId) {
                favoriId = f.getId();
                break;
            }
        }

        if (favoriId > 0) {
            favoriService.supprimer(favoriId);
            estFavori = false;
            afficherAlerte("Succès", "Événement retiré des favoris");
        }
    }

    private void mettreAJourBoutonFavori() {
        if (btnFavori == null) {
            return;
        }
        if (estFavori) {
            btnFavori.setText("♥ Retirer des favoris");
            btnFavori.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            btnFavori.setText("♡ Ajouter aux favoris");
            btnFavori.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-border-color: #ef4444; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;");
        }
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void chargerAvis() {
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (currentUserId <= 0) {
                return;
            }

            // Charger tous les avis pour cet événement
            List<Participation> toutesParticipations = new ArrayList<>();
            for (Participation p : participationService.afficher()) {
                if (p.getEvenementId() == evenementCourant.getEvenementId() && p.hasFeedback()) {
                    toutesParticipations.add(p);
                }
            }

            if (toutesParticipations.isEmpty()) {
                lblAucunAvis.setVisible(true);
                vboxAvis.getChildren().clear();
                vboxAvis.getChildren().add(lblAucunAvis);
                lblNoteMoyenne.setText("⭐ 0.0/5");
                lblNbAvis.setText("(0 avis)");
            } else {
                lblAucunAvis.setVisible(false);
                vboxAvis.getChildren().clear();
                
                for (Participation p : toutesParticipations) {
                    VBox avisCard = creerCarteAvis(p);
                    vboxAvis.getChildren().add(avisCard);
                }

                // Calculer la note moyenne
                double somme = 0;
                for (Participation p : toutesParticipations) {
                    somme += p.getNoteSatisfaction();
                }
                double moyenne = toutesParticipations.size() > 0 ? somme / toutesParticipations.size() : 0.0;
                lblNoteMoyenne.setText(String.format("⭐ %.1f/5", moyenne));
                lblNbAvis.setText("(" + toutesParticipations.size() + " avis)");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des avis: " + e.getMessage());
        }
    }

    private VBox creerCarteAvis(Participation participation) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        card.setSpacing(8);

        // Note en étoiles
        String etoiles = "⭐".repeat(participation.getNoteSatisfaction());
        Label lblNote = new Label(etoiles);
        lblNote.setStyle("-fx-font-size: 18px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");

        // Commentaire
        Label lblCommentaire = new Label(participation.getFeedbackCommentaire());
        lblCommentaire.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        lblCommentaire.setMaxWidth(650);

        // Date
        String dateStr = participation.getFeedbackAt() != null 
                ? participation.getFeedbackAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Date inconnue";
        Label lblDate = new Label(dateStr);
        lblDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        card.getChildren().addAll(lblNote, lblCommentaire, lblDate);
        return card;
    }

    @FXML
    private void laisserAvis(ActionEvent event) {
        try {
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (currentUserId <= 0) {
                afficherAlerte("Erreur", "Utilisateur non connecté");
                return;
            }

            // Vérifier si l'événement est terminé
            if (evenementCourant.getDateFin() != null 
                    && evenementCourant.getDateFin().toLocalDateTime().isAfter(java.time.LocalDateTime.now())) {
                afficherAlerte("Erreur", "Vous ne pouvez laisser un avis que lorsque l'événement est terminé");
                return;
            }

            // Récupérer la participation de l'étudiant
            final Participation[] participation = new Participation[1];
            for (Participation p : participationService.afficher()) {
                if (p.getEvenementId() == evenementCourant.getEvenementId() && p.getEtudiantId() == currentUserId) {
                    participation[0] = p;
                    break;
                }
            }

            if (participation[0] == null) {
                afficherAlerte("Erreur", "Vous devez être inscrit à cet événement pour laisser un avis");
                return;
            }

            // Vérifier si l'étudiant a déjà laissé un avis
            if (participation[0].hasFeedback()) {
                afficherAlerte("Erreur", "Vous avez déjà laissé un avis pour cet événement");
                return;
            }

            // Créer une boîte de dialogue pour le feedback
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Laisser un avis");

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            VBox content = new VBox(15);
            
            // Note
            Label lblNote = new Label("Note (1-5 étoiles):");
            ComboBox<Integer> comboNote = new ComboBox<>();
            for (int i = 1; i <= 5; i++) {
                comboNote.getItems().add(i);
            }
            comboNote.setValue(5);
            
            // Commentaire
            Label lblCommentaire = new Label("Commentaire:");
            TextArea txtCommentaire = new TextArea();
            txtCommentaire.setPrefRowCount(4);
            txtCommentaire.setPrefWidth(400);
            
            content.getChildren().addAll(lblNote, comboNote, lblCommentaire, txtCommentaire);
            dialogPane.setContent(content);

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    int note = comboNote.getValue();
                    String commentaire = txtCommentaire.getText().trim();

                    if (commentaire.isEmpty()) {
                        afficherAlerte("Erreur", "Veuillez laisser un commentaire");
                        return;
                    }

                    // Mettre à jour la participation
                    participation[0].ajouterFeedback((short) note, commentaire);
                    
                    try {
                        participationService.modifier(participation[0]);
                        afficherAlerte("Succès", "Votre avis a été enregistré avec succès !");
                        chargerAvis(); // Recharger les avis
                    } catch (SQLException e) {
                        afficherAlerte("Erreur", "Impossible d'enregistrer votre avis: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir le formulaire d'avis: " + e.getMessage());
        }
    }
}
