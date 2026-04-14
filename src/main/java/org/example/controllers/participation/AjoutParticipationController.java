package org.example.controllers.participation;

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

public class AjoutParticipationController {

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
    private Label lblErreurUnicite;
    @FXML
    private Label lblSucces;

    private final ParticipationService participationService = new ParticipationService();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        // Vérifier si l'utilisateur est admin
        isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        comboStatut.setItems(FXCollections.observableArrayList(StatutParticipation.values()));
        comboStatut.setValue(StatutParticipation.EN_ATTENTE);

        chargerEvenements();
        chargerEtudiants();

        // Date par défaut : aujourd'hui
        dateInscription.setValue(LocalDate.now());

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

    @FXML
    private void enregistrerParticipation(ActionEvent event) {
        cacherErreurs();
        lblSucces.setVisible(false);

        boolean valide = validerFormulaire();
        if (!valide) {
            return;
        }

        try {
            Participation participation = creerParticipation();
            participationService.ajouter(participation);

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
            afficherErreur("Erreur lors de l'enregistrement : " + e.getMessage());
        } catch (NumberFormatException e) {
            afficherErreur("Les IDs doivent être des nombres valides");
        }
    }

    @FXML
    private void retour(ActionEvent event) {
        try {
            NavigationContext.loadContentInCenter("/participation/GestionParticipation.fxml");
        } catch (IOException e) {
            afficherErreur("Erreur lors de la navigation : " + e.getMessage());
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

        // Valider unicité (evenementId + etudiantId)
        if (comboEvenement.getValue() != null && comboEtudiant.getValue() != null) {
            try {
                int evenementId = comboEvenement.getValue().getEvenementId();
                int etudiantId = comboEtudiant.getValue().getEtudiantId();
                if (participationService.verifierUnicite(evenementId, etudiantId)) {
                    lblErreurUnicite.setVisible(true);
                    erreurs.add("Unicité : cette participation existe déjà");
                }
            } catch (SQLException e) {
                erreurs.add("Erreur lors de la vérification d'unicité : " + e.getMessage());
            }
        }

        if (!erreurs.isEmpty()) {
            afficherErreur("Veuillez corriger les erreurs suivantes :\n- " + String.join("\n- ", erreurs));
            return false;
        }

        return true;
    }

    private Participation creerParticipation() throws NumberFormatException {
        EvenementInfo evenement = comboEvenement.getValue();
        EtudiantInfo etudiant = comboEtudiant.getValue();
        int evenementId = evenement.getEvenementId();
        int etudiantId = etudiant.getEtudiantId();
        StatutParticipation statut = comboStatut.getValue();

        // Date d'inscription : utilise la date du DatePicker ou la date actuelle
        LocalDate date = dateInscription.getValue();
        LocalDateTime dateTime = date != null ? date.atStartOfDay() : LocalDateTime.now();
        Timestamp dateInscriptionTs = Timestamp.valueOf(dateTime);

        Participation participation = new Participation(evenementId, etudiantId, statut);
        participation.setDateInscription(dateInscriptionTs);

        return participation;
    }

    private void cacherErreurs() {
        lblErreurEvenementId.setVisible(false);
        lblErreurEtudiantId.setVisible(false);
        lblErreurUnicite.setVisible(false);
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
