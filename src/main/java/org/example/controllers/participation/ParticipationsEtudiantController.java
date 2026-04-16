package org.example.controllers.participation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.services.ParticipationService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ParticipationsEtudiantController {

    @FXML
    private TilePane tileParticipations;

    @FXML
    private Label lblTotal;

    private ParticipationService participationService;
    private List<Participation> listeParticipations;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        System.out.println("Initialisation de ParticipationsEtudiantController");
        try {
            participationService = new ParticipationService();
            
            chargerParticipationsEtudiant();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerParticipationsEtudiant() {
        System.out.println("Chargement des participations étudiant...");
        try {
            int etudiantId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (etudiantId <= 0) {
                System.err.println("Aucun étudiant connecté");
                afficherMessageAucuneParticipation();
                return;
            }

            listeParticipations = new ArrayList<>();
            for (Participation p : participationService.afficher()) {
                if (p.getEtudiantId() == etudiantId) {
                    listeParticipations.add(p);
                }
            }
            
            System.out.println("Nombre de participations chargées: " + listeParticipations.size());

            if (listeParticipations.isEmpty()) {
                System.out.println("Aucune participation trouvée");
                afficherMessageAucuneParticipation();
                lblTotal.setText("0 participations");
            } else {
                afficherCartesParticipations(listeParticipations);
                lblTotal.setText(listeParticipations.size() + " participations");
            }
            System.out.println("Participations affichées avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement des participations: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du chargement des participations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void afficherMessageAucuneParticipation() {
        tileParticipations.getChildren().clear();
        Label lblMessage = new Label("✅ Aucune participation");
        lblMessage.setStyle("-fx-font-size: 20px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        tileParticipations.getChildren().add(lblMessage);
    }

    private void afficherCartesParticipations(List<Participation> participations) {
        System.out.println("Affichage des cartes de participations...");
        tileParticipations.getChildren().clear();

        for (Participation p : participations) {
            try {
                System.out.println("Création de la carte pour la participation ID: " + p.getParticipationId());
                VBox carteParticipation = creerCarteParticipation(p);
                tileParticipations.getChildren().add(carteParticipation);
                System.out.println("Carte ajoutée avec succès");
            } catch (Exception ex) {
                System.err.println("Erreur lors de la création de la carte pour la participation " + p.getParticipationId() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        System.out.println("Nombre de cartes dans le TilePane: " + tileParticipations.getChildren().size());
    }

    private VBox creerCarteParticipation(Participation participation) {
        try {
            // Charger l'événement associé
            Evenement evenement = chargerEvenementParId(participation.getEvenementId());
            if (evenement == null) {
                return null;
            }

            VBox carte = new VBox();
            carte.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;");
            carte.setPrefWidth(280);
            carte.setSpacing(10);

            // Image
            ImageView imageView = new ImageView();
            imageView.setFitHeight(150);
            imageView.setFitWidth(280);
            imageView.setPreserveRatio(false);

            if (evenement.getImage() != null && !evenement.getImage().trim().isEmpty()) {
                try {
                    String encodedImageName = URLEncoder.encode(evenement.getImage().trim(), StandardCharsets.UTF_8);
                    String fullImageUrl = "http://localhost/uploadsEvent/evenements/" + encodedImageName;
                    Image image = new Image(fullImageUrl, true);
                    imageView.setImage(image);
                } catch (Exception ex) {
                    imageView.setImage(null);
                }
            }

            // Conteneur pour l'image
            VBox imageContainer = new VBox();
            imageContainer.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15 15 0 0;");
            imageContainer.setPrefHeight(150);
            imageContainer.setAlignment(Pos.CENTER);

            if (imageView.getImage() != null) {
                imageContainer.getChildren().add(imageView);
            } else {
                imageContainer.getChildren().add(new Label("🎪"));
                ((Label) imageContainer.getChildren().get(0)).setStyle("-fx-font-size: 60px;");
            }

            // Badge de statut de participation
            Label lblStatutParticipation = new Label();
            lblStatutParticipation.setText(participation.getStatut().toString());
            lblStatutParticipation.setStyle("-fx-background-color: " + getCouleurStatutParticipation(participation.getStatut().toString()) + "; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 10; -fx-font-size: 11px;");

            // Vérifier si l'événement est terminé
            boolean evenementTermine = evenement.getDateFin() != null 
                    && evenement.getDateFin().toLocalDateTime().isBefore(java.time.LocalDateTime.now());

            // Badge événement terminé
            Label lblEvenementTermine = null;
            if (evenementTermine) {
                lblEvenementTermine = new Label("✓ Événement terminé");
                lblEvenementTermine.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 10; -fx-font-size: 11px;");
            }

            // Titre de l'événement
            Label lblTitre = new Label(evenement.getTitre());
            lblTitre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
            lblTitre.setMaxWidth(260);

            // Date et lieu
            Label lblDate = new Label("📅 " + (evenement.getDateDebut() != null 
                    ? evenement.getDateDebut().toLocalDateTime().format(dateFormatter) 
                    : "Date non définie"));
            lblDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

            Label lblLieu = new Label("📍 " + evenement.getLieu());
            lblLieu.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

            // Date d'inscription
            Label lblDateInscription = new Label("Inscrit le: " + participation.getDateInscription().toLocalDateTime().format(dateFormatter));
            lblDateInscription.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

            // Boutons
            Button btnVoir = new Button("Voir le détail");
            btnVoir.setStyle("-fx-background-color: transparent; -fx-text-fill: #6366f1; -fx-border-color: #6366f1; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold;");
            btnVoir.setOnAction(event -> voirEvenement(evenement));

            // Bouton laisser avis ou badge avis donné si l'événement est terminé
            Button btnLaisserAvis = null;
            Label lblAvisDonne = null;
            if (evenementTermine) {
                if (!participation.hasFeedback()) {
                    btnLaisserAvis = new Button("💬 Laisser un avis");
                    btnLaisserAvis.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold;");
                    btnLaisserAvis.setOnAction(event -> voirEvenement(evenement)); // Redirige vers le détail où le bouton laisser avis est disponible
                } else {
                    lblAvisDonne = new Label("✓ Avis donné");
                    lblAvisDonne.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold;");
                }
            }

            // Contenu de la carte
            VBox contenu = new VBox(8);
            contenu.setPadding(new Insets(15));
            
            javafx.scene.layout.HBox badges = new javafx.scene.layout.HBox(5);
            badges.getChildren().addAll(lblStatutParticipation);
            if (lblEvenementTermine != null) {
                badges.getChildren().add(lblEvenementTermine);
            }
            
            javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, btnVoir);
            if (btnLaisserAvis != null) {
                actions.getChildren().add(btnLaisserAvis);
            }
            if (lblAvisDonne != null) {
                actions.getChildren().add(lblAvisDonne);
            }
            
            contenu.getChildren().addAll(badges, lblTitre, lblDate, lblLieu, lblDateInscription, actions);

            carte.getChildren().addAll(imageContainer, contenu);

            // Clic sur la carte pour voir les détails
            carte.setOnMouseClicked(event -> voirEvenement(evenement));

            return carte;
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la carte: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Evenement chargerEvenementParId(int evenementId) {
        try {
            String sql = "SELECT * FROM evenement WHERE evenement_id = ?";
            try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                ps.setInt(1, evenementId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Evenement e = new Evenement();
                        e.setEvenementId(rs.getInt("evenement_id"));
                        e.setTitre(rs.getString("titre"));
                        e.setDescription(rs.getString("description"));
                        e.setDateDebut(rs.getTimestamp("date_debut"));
                        e.setDateFin(rs.getTimestamp("date_fin"));
                        e.setLieu(rs.getString("lieu"));
                        e.setImage(rs.getString("image"));
                        return e;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de l'événement: " + e.getMessage());
        }
        return null;
    }

    private String getCouleurStatutParticipation(String statut) {
        switch (statut.toUpperCase()) {
            case "CONFIRMEE":
                return "#27ae60";
            case "EN_ATTENTE":
                return "#f39c12";
            case "ANNULEE":
                return "#e74c3c";
            default:
                return "#3498db";
        }
    }

    private void voirEvenement(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evenement/VoirEvenement.fxml"));
            Parent root = loader.load();
            org.example.controllers.evenement.VoirEvenementController controller = loader.getController();
            controller.setEvenement(evenement);
            controller.setPagePrecedente("/participation/ParticipationsEtudiant.fxml"); // Retour vers les participations

            NavigationContext.loadContentInCenter(root);
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir l'écran de détails: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Impossible d'afficher l'événement: " + e.getMessage());
        }
    }
}
