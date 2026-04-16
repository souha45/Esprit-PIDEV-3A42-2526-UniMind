package org.example.controllers.feedback;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.services.ParticipationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FeedbacksAdminController {

    @FXML
    private TilePane tileFeedbacks;

    @FXML
    private Label lblTotal;

    private ParticipationService participationService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        System.out.println("Initialisation de FeedbacksAdminController");
        try {
            participationService = new ParticipationService();
            chargerFeedbacks();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerFeedbacks() {
        System.out.println("Chargement des feedbacks...");
        try {
            // Récupérer toutes les participations avec feedback
            List<Participation> feedbacks = new ArrayList<>();
            for (Participation p : participationService.afficher()) {
                if (p.hasFeedback()) {
                    feedbacks.add(p);
                }
            }

            System.out.println("Nombre de feedbacks chargés: " + feedbacks.size());

            if (feedbacks.isEmpty()) {
                afficherAucunFeedback();
                lblTotal.setText("0 avis");
            } else {
                afficherCartesFeedbacks(feedbacks);
                lblTotal.setText(feedbacks.size() + " avis");
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement des feedbacks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void afficherAucunFeedback() {
        tileFeedbacks.getChildren().clear();
        Label lblMessage = new Label("✅ Aucun avis");
        lblMessage.setStyle("-fx-font-size: 20px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        tileFeedbacks.getChildren().add(lblMessage);
    }

    private void afficherCartesFeedbacks(List<Participation> feedbacks) {
        System.out.println("Affichage des cartes de feedbacks...");
        tileFeedbacks.getChildren().clear();

        for (Participation p : feedbacks) {
            try {
                System.out.println("Création de la carte pour le feedback ID: " + p.getParticipationId());
                VBox carteFeedback = creerCarteFeedback(p);
                tileFeedbacks.getChildren().add(carteFeedback);
                System.out.println("Carte ajoutée avec succès");
            } catch (Exception ex) {
                System.err.println("Erreur lors de la création de la carte pour le feedback " + p.getParticipationId() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private VBox creerCarteFeedback(Participation participation) {
        try {
            // Charger l'événement associé
            Evenement evenement = chargerEvenementParId(participation.getEvenementId());
            if (evenement == null) {
                return null;
            }

            VBox carte = new VBox();
            carte.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
            carte.setPrefWidth(350);
            carte.setSpacing(12);
            carte.setPadding(new Insets(15));

            // Titre de l'événement
            Label lblTitreEvenement = new Label(evenement.getTitre());
            lblTitreEvenement.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
            lblTitreEvenement.setMaxWidth(320);

            // Nom de l'étudiant
            String nomEtudiant = chargerNomEtudiant(participation.getEtudiantId());
            Label lblEtudiant = new Label("👤 " + nomEtudiant);
            lblEtudiant.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

            // Note en étoiles
            String etoiles = "⭐".repeat(participation.getNoteSatisfaction());
            Label lblNote = new Label(etoiles);
            lblNote.setStyle("-fx-font-size: 20px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");

            // Commentaire
            Label lblCommentaire = new Label(participation.getFeedbackCommentaire());
            lblCommentaire.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
            lblCommentaire.setMaxWidth(320);

            // Date du feedback
            Label lblDateFeedback = new Label("Avis donné le: " + 
                (participation.getFeedbackAt() != null 
                    ? participation.getFeedbackAt().toLocalDateTime().format(dateFormatter) 
                    : "Date inconnue"));
            lblDateFeedback.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

            carte.getChildren().addAll(lblTitreEvenement, lblEtudiant, lblNote, lblCommentaire, lblDateFeedback);

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

    private String chargerNomEtudiant(int etudiantId) {
        try {
            String sql = "SELECT nom, prenom FROM user WHERE user_id = ?";
            try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                ps.setInt(1, etudiantId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement du nom de l'étudiant: " + e.getMessage());
        }
        return "Étudiant inconnu";
    }
}
