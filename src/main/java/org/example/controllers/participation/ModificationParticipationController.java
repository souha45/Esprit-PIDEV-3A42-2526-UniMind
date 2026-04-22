package org.example.controllers.participation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.example.entities.Participation;
import org.example.enums.Role;
import org.example.enums.StatutParticipation;
import org.example.services.ParticipationService;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ModificationParticipationController {

    @FXML
    private ComboBox<EvenementInfo> comboEvenement;
    @FXML
    private ComboBox<EtudiantInfo> comboEtudiant;
    @FXML
    private DatePicker dateInscription;

    @FXML
    private ComboBox<StatutParticipation> comboStatut;

    @FXML
    private Label lblErreurEvenementId;
    @FXML
    private Label lblErreurEtudiantId;
    @FXML
    private Label lblSucces;

    private final ParticipationService participationService = new ParticipationService();
    private Participation participationCourante;
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        // Vérifier si l'utilisateur est admin
        isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        comboStatut.setItems(FXCollections.observableArrayList(StatutParticipation.values()));

        chargerEvenements();
        chargerEtudiants();

        // Forcer la couleur du texte en noir pour tous les champs
        appliquerCouleurTexteNoir();
    }

    private void appliquerCouleurTexteNoir() {
        // Appliquer le style inline pour forcer le texte noir
        if (comboEvenement != null) comboEvenement.setStyle("-fx-text-fill: #000000;");
        if (comboEtudiant != null) comboEtudiant.setStyle("-fx-text-fill: #000000;");
        if (comboStatut != null) comboStatut.setStyle("-fx-text-fill: #000000;");
        if (dateInscription != null) dateInscription.setStyle("-fx-text-fill: #000000;");
    }

    public void setParticipation(Participation participation) {
        this.participationCourante = participation;
        remplirFormulaire();
    }

    private void remplirFormulaire() {
        if (participationCourante == null) {
            return;
        }

        // Sélectionner l'événement par son ID
        for (EvenementInfo evt : comboEvenement.getItems()) {
            if (evt.getEvenementId() == participationCourante.getEvenementId()) {
                comboEvenement.setValue(evt);
                break;
            }
        }

        // Sélectionner l'étudiant par son ID
        for (EtudiantInfo etu : comboEtudiant.getItems()) {
            if (etu.getEtudiantId() == participationCourante.getEtudiantId()) {
                comboEtudiant.setValue(etu);
                break;
            }
        }

        // Statut
        comboStatut.setValue(participationCourante.getStatut());

        // Date d'inscription
        if (participationCourante.getDateInscription() != null) {
            dateInscription.setValue(participationCourante.getDateInscription().toLocalDateTime().toLocalDate());
        }
    }

    @FXML
    private void enregistrerModification(ActionEvent event) {
        cacherErreurs();
        lblSucces.setVisible(false);

        boolean valide = validerFormulaire();
        if (!valide) {
            return;
        }

        try {
            Participation participation = construireParticipationModifiee();
            participationService.modifier(participation);

            lblSucces.setVisible(true);
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1.5),
                    ae -> {
                        lblSucces.setVisible(false);
                        retour(event);
                    }
            ));
            timeline.play();

        } catch (SQLException e) {
            afficherErreur("Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ADMIN);
        if (role == Role.ADMIN) {
            org.example.controllers.admin.AdminDashboardController.loadContent("/participation/GestionParticipation.fxml");
        } else {
            try {
                NavigationContext.loadContentInCenter("/participation/GestionParticipation.fxml");
            } catch (IOException e) {
                afficherErreur("Erreur lors de la navigation: " + e.getMessage());
            }
        }
    }

    private boolean validerFormulaire() {
        List<String> erreurs = new ArrayList<>();

        if (comboEvenement.getValue() == null) {
            lblErreurEvenementId.setVisible(true);
            erreurs.add("Événement : obligatoire");
        }

        if (comboEtudiant.getValue() == null) {
            lblErreurEtudiantId.setVisible(true);
            erreurs.add("Étudiant : obligatoire");
        }

        if (!erreurs.isEmpty()) {
            afficherErreur("Le formulaire contient des erreurs. Vérifiez les champs marqués en rouge.");
            return false;
        }

        return true;
    }

    private Participation construireParticipationModifiee() {
        Participation p = new Participation();
        p.setParticipationId(participationCourante.getParticipationId());

        // Événement
        if (comboEvenement.getValue() != null) {
            p.setEvenementId(comboEvenement.getValue().getEvenementId());
        } else {
            p.setEvenementId(participationCourante.getEvenementId());
        }

        // Étudiant
        if (comboEtudiant.getValue() != null) {
            p.setEtudiantId(comboEtudiant.getValue().getEtudiantId());
        } else {
            p.setEtudiantId(participationCourante.getEtudiantId());
        }

        // Statut
        p.setStatut(comboStatut.getValue());

        // Date d'inscription
        LocalDate date = dateInscription.getValue();
        if (date != null) {
            LocalDateTime dateTime = date.atStartOfDay();
            p.setDateInscription(Timestamp.valueOf(dateTime));
        } else {
            p.setDateInscription(participationCourante.getDateInscription());
        }

        // Conserver les autres champs
        p.setCreatedAt(participationCourante.getCreatedAt());
        p.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        p.setNoteSatisfaction(participationCourante.getNoteSatisfaction());
        p.setFeedbackCommentaire(participationCourante.getFeedbackCommentaire());
        p.setFeedbackAt(participationCourante.getFeedbackAt());
        p.setQrToken(participationCourante.getQrToken());
        p.setScannedAt(participationCourante.getScannedAt());
        p.setPresent(participationCourante.getPresent());

        return p;
    }

    private void cacherErreurs() {
        lblErreurEvenementId.setVisible(false);
        lblErreurEtudiantId.setVisible(false);
    }

    private void chargerEvenements() {
        try {
            String sql;
            if (isAdmin) {
                // Admin : voir tous les événements
                sql = "SELECT evenement_id, titre FROM evenement ORDER BY titre";
            } else {
                // Responsable : voir seulement ses événements
                sql = "SELECT evenement_id, titre FROM evenement WHERE organisateur_id = ? ORDER BY titre";
            }

            java.util.List<EvenementInfo> evenements = new ArrayList<>();

            try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                if (!isAdmin) {
                    int userId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
                    ps.setInt(1, userId);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int evenementId = rs.getInt("evenement_id");
                        String titre = rs.getString("titre");
                        evenements.add(new EvenementInfo(evenementId, titre));
                    }
                }
            }
            comboEvenement.setItems(FXCollections.observableArrayList(evenements));
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des événements: " + e.getMessage());
        }
    }

    private void chargerEtudiants() {
        try {
            String sql = "SELECT user_id, nom, prenom FROM user WHERE role = 'Etudiant' ORDER BY nom, prenom";
            java.util.List<EtudiantInfo> etudiants = new ArrayList<>();

            try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int etudiantId = rs.getInt("user_id");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    String fullName = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                    etudiants.add(new EtudiantInfo(etudiantId, fullName.trim()));
                }
            }
            comboEtudiant.setItems(FXCollections.observableArrayList(etudiants));
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des étudiants: " + e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Classe interne pour représenter un événement avec ID et titre
     */
    public static class EvenementInfo {
        private final int evenementId;
        private final String titre;

        public EvenementInfo(int evenementId, String titre) {
            this.evenementId = evenementId;
            this.titre = titre;
        }

        public int getEvenementId() {
            return evenementId;
        }

        public String getTitre() {
            return titre;
        }

        @Override
        public String toString() {
            return titre;
        }
    }

    /**
     * Classe interne pour représenter un étudiant avec ID et nom
     */
    public static class EtudiantInfo {
        private final int etudiantId;
        private final String fullName;

        public EtudiantInfo(int etudiantId, String fullName) {
            this.etudiantId = etudiantId;
            this.fullName = fullName;
        }

        public int getEtudiantId() {
            return etudiantId;
        }

        public String getFullName() {
            return fullName;
        }

        @Override
        public String toString() {
            return fullName;
        }
    }
}
