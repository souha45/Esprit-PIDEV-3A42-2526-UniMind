package org.example.controllers.participation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.Participation;
import org.example.enums.Role;
import org.example.enums.StatutParticipation;
import org.example.services.ParticipationService;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.SessionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AjoutParticipationDialogController {

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
    private Label lblErreurCapacite;
    @FXML
    private Label lblSucces;

    private final ParticipationService participationService = new ParticipationService();
    private boolean isAdmin = false;
    private Stage dialogStage;
    private Consumer<Void> onSaveCallback;

    @FXML
    public void initialize() {
        isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        comboStatut.setItems(FXCollections.observableArrayList(StatutParticipation.values()));
        comboStatut.setValue(StatutParticipation.EN_ATTENTE);

        chargerEvenements();
        chargerEtudiants();

        dateInscription.setValue(LocalDate.now());

        appliquerCouleurTexteNoir();
    }

    private void appliquerCouleurTexteNoir() {
        if (comboEvenement != null) comboEvenement.setStyle("-fx-text-fill: #000000;");
        if (comboEtudiant != null) comboEtudiant.setStyle("-fx-text-fill: #000000;");
        if (comboStatut != null) comboStatut.setStyle("-fx-text-fill: #000000;");
        if (dateInscription != null) dateInscription.setStyle("-fx-text-fill: #000000;");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnSaveCallback(Consumer<Void> callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void enregistrer() {
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

            // Notifier le parent et fermer après un délai
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1.0),
                    ae -> {
                        if (onSaveCallback != null) {
                            onSaveCallback.accept(null);
                        }
                        fermerDialog();
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
    private void annuler() {
        fermerDialog();
    }

    private void fermerDialog() {
        if (dialogStage != null) {
            dialogStage.close();
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

        if (comboEvenement.getValue() != null && comboEtudiant.getValue() != null) {
            try {
                int evenementId = comboEvenement.getValue().getEvenementId();
                int etudiantId = comboEtudiant.getValue().getEtudiantId();
                if (participationService.verifierUnicite(evenementId, etudiantId)) {
                    lblErreurUnicite.setVisible(true);
                    erreurs.add("Unicité : cette participation existe déjà");
                }

                if (!verifierCapaciteEvenement(evenementId)) {
                    lblErreurCapacite.setVisible(true);
                    erreurs.add("Capacité : l'événement a atteint sa capacité maximale");
                }
            } catch (SQLException e) {
                erreurs.add("Erreur lors de la vérification : " + e.getMessage());
            }
        }

        if (!erreurs.isEmpty()) {
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

        LocalDate date = dateInscription.getValue();
        LocalDateTime dateTime = date != null ? LocalDateTime.of(date, LocalTime.now()) : LocalDateTime.now();
        Timestamp dateInscriptionTs = Timestamp.valueOf(dateTime);

        Participation participation = new Participation(evenementId, etudiantId, statut);
        participation.setDateInscription(dateInscriptionTs);

        return participation;
    }

    private void cacherErreurs() {
        lblErreurEvenementId.setVisible(false);
        lblErreurEtudiantId.setVisible(false);
        lblErreurUnicite.setVisible(false);
        lblErreurCapacite.setVisible(false);
    }

    private boolean verifierCapaciteEvenement(int evenementId) throws SQLException {
        String sqlCapacite = "SELECT capacite_max FROM evenement WHERE evenement_id = ?";
        int capaciteMax = 0;
        try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sqlCapacite)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    capaciteMax = rs.getInt("capacite_max");
                }
            }
        }

        if (capaciteMax <= 0) {
            return true;
        }

        String sqlCount = "SELECT COUNT(*) as nombre_participations FROM participation WHERE evenement_id = ?";
        int nombreParticipations = 0;
        try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sqlCount)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nombreParticipations = rs.getInt("nombre_participations");
                }
            }
        }

        return nombreParticipations < capaciteMax;
    }

    private void chargerEvenements() {
        try {
            String sql;
            if (isAdmin) {
                sql = "SELECT evenement_id, titre FROM evenement WHERE statut IN ('a_venir', 'en_cours') ORDER BY titre";
            } else {
                sql = "SELECT evenement_id, titre FROM evenement WHERE organisateur_id = ? AND statut IN ('a_venir', 'en_cours') ORDER BY titre";
            }

            List<EvenementInfo> evenements = new ArrayList<>();

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
            String sql = "SELECT user_id, CONCAT(prenom, ' ', nom) as full_name FROM user WHERE role = 'ETUDIANT' ORDER BY nom, prenom";
            List<EtudiantInfo> etudiants = new ArrayList<>();

            try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int etudiantId = rs.getInt("user_id");
                    String fullName = rs.getString("full_name");
                    etudiants.add(new EtudiantInfo(etudiantId, fullName));
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

    // Classes internes pour les ComboBox
    public static class EvenementInfo {
        private final int evenementId;
        private final String titre;

        public EvenementInfo(int evenementId, String titre) {
            this.evenementId = evenementId;
            this.titre = titre;
        }

        public int getEvenementId() { return evenementId; }
        public String getTitre() { return titre; }

        @Override
        public String toString() { return titre; }
    }

    public static class EtudiantInfo {
        private final int etudiantId;
        private final String fullName;

        public EtudiantInfo(int etudiantId, String fullName) {
            this.etudiantId = etudiantId;
            this.fullName = fullName;
        }

        public int getEtudiantId() { return etudiantId; }
        public String getFullName() { return fullName; }

        @Override
        public String toString() { return fullName; }
    }
}
