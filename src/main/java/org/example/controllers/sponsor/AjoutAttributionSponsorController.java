package org.example.controllers.sponsor;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.entities.Sponsor;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeContribution;
import org.example.services.EvenementSponsorService;
import org.example.services.SponsorService;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AjoutAttributionSponsorController {

    @FXML
    private ComboBox<EvenementInfo> comboEvenement;
    @FXML
    private ComboBox<SponsorInfo> comboSponsor;
    @FXML
    private TextField txtMontant;
    @FXML
    private ComboBox<TypeContribution> comboType;
    @FXML
    private TextArea txtDescription;
    @FXML
    private DatePicker dateContribution;
    @FXML
    private ComboBox<StatutSponsor> comboStatut;

    @FXML
    private Label lblErreurEvenement;
    @FXML
    private Label lblErreurSponsor;
    @FXML
    private Label lblErreurMontant;
    @FXML
    private Label lblSucces;

    private final EvenementSponsorService attributionService = new EvenementSponsorService();
    private final SponsorService sponsorService = new SponsorService();

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList(TypeContribution.values()));
        comboStatut.setItems(FXCollections.observableArrayList(StatutSponsor.values()));

        // Valeurs par défaut
        comboType.setValue(TypeContribution.FINANCIER);
        comboStatut.setValue(StatutSponsor.EN_ATTENTE);

        // Date par défaut : aujourd'hui
        dateContribution.setValue(LocalDate.now());

        chargerEvenements();
        chargerSponsors();

        // Forcer la couleur du texte en noir pour tous les champs
        appliquerCouleurTexteNoir();
    }

    private void appliquerCouleurTexteNoir() {
        if (txtMontant != null) txtMontant.setStyle("-fx-text-fill: #000000;");
        if (txtDescription != null) txtDescription.setStyle("-fx-text-fill: #000000;");
        if (comboEvenement != null) comboEvenement.setStyle("-fx-text-fill: #000000;");
        if (comboSponsor != null) comboSponsor.setStyle("-fx-text-fill: #000000;");
        if (comboType != null) comboType.setStyle("-fx-text-fill: #000000;");
        if (comboStatut != null) comboStatut.setStyle("-fx-text-fill: #000000;");
        if (dateContribution != null) dateContribution.setStyle("-fx-text-fill: #000000;");
    }

    private void chargerEvenements() {
        try {
            int userId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            String sql = "SELECT evenement_id, titre FROM evenement WHERE organisateur_id = ? ORDER BY titre";
            List<EvenementInfo> evenements = new ArrayList<>();

            try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
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

    private void chargerSponsors() {
        try {
            List<Sponsor> sponsors = sponsorService.afficher();
            List<SponsorInfo> sponsorInfos = new ArrayList<>();

            for (Sponsor sponsor : sponsors) {
                sponsorInfos.add(new SponsorInfo(sponsor.getSponsorId(), sponsor.getNomSponsor()));
            }

            comboSponsor.setItems(FXCollections.observableArrayList(sponsorInfos));
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des sponsors: " + e.getMessage());
        }
    }

    @FXML
    private void enregistrerAttribution(ActionEvent event) {
        cacherErreurs();
        lblSucces.setVisible(false);

        boolean valide = validerFormulaire();
        if (!valide) {
            return;
        }

        try {
            org.example.entities.EvenementSponsor attribution = creerAttribution();
            attributionService.ajouter(attribution);

            lblSucces.setVisible(true);
            lblSucces.setText("Attribution enregistrée avec succès !");

            // Revenir à la liste après un délai
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                try {
                    NavigationContext.loadContentInCenter("/sponsor/GestionAttributionSponsor.fxml");
                } catch (IOException ioException) {
                    afficherErreur("Erreur lors de la navigation : " + ioException.getMessage());
                }
            });
            pause.play();

        } catch (SQLException e) {
            afficherErreur("Erreur lors de l'enregistrement : " + e.getMessage());
        } catch (NumberFormatException e) {
            afficherErreur("Le montant doit être un nombre valide");
        }
    }

    private boolean validerFormulaire() {
        boolean valide = true;

        if (comboEvenement.getValue() == null) {
            lblErreurEvenement.setVisible(true);
            valide = false;
        }

        if (comboSponsor.getValue() == null) {
            lblErreurSponsor.setVisible(true);
            valide = false;
        }

        if (txtMontant.getText() == null || txtMontant.getText().trim().isEmpty()) {
            lblErreurMontant.setVisible(true);
            valide = false;
        } else {
            try {
                new BigDecimal(txtMontant.getText().trim());
            } catch (NumberFormatException e) {
                lblErreurMontant.setVisible(true);
                valide = false;
            }
        }

        return valide;
    }

    private org.example.entities.EvenementSponsor creerAttribution() {
        EvenementInfo evenement = comboEvenement.getValue();
        SponsorInfo sponsor = comboSponsor.getValue();
        BigDecimal montant = new BigDecimal(txtMontant.getText().trim());
        TypeContribution type = comboType.getValue();
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : null;
        LocalDate date = dateContribution.getValue();
        StatutSponsor statut = comboStatut.getValue();

        LocalDateTime dateTime = date.atStartOfDay();

        return new org.example.entities.EvenementSponsor(
                montant,
                type,
                description,
                Timestamp.valueOf(dateTime),
                statut,
                evenement.getEvenementId(),
                sponsor.getSponsorId()
        );
    }

    private void cacherErreurs() {
        lblErreurEvenement.setVisible(false);
        lblErreurSponsor.setVisible(false);
        lblErreurMontant.setVisible(false);
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void annuler(ActionEvent event) {
        try {
            NavigationContext.loadContentInCenter("/sponsor/GestionAttributionSponsor.fxml");
        } catch (IOException e) {
            afficherErreur("Erreur lors de la navigation : " + e.getMessage());
        }
    }

    // Classes internes pour les ComboBox
    public static class EvenementInfo {
        private int evenementId;
        private String titre;

        public EvenementInfo(int evenementId, String titre) {
            this.evenementId = evenementId;
            this.titre = titre;
        }

        public int getEvenementId() { return evenementId; }
        public String getTitre() { return titre; }

        @Override
        public String toString() {
            return titre;
        }
    }

    public static class SponsorInfo {
        private int sponsorId;
        private String nom;

        public SponsorInfo(int sponsorId, String nom) {
            this.sponsorId = sponsorId;
            this.nom = nom;
        }

        public int getSponsorId() { return sponsorId; }
        public String getNom() { return nom; }

        @Override
        public String toString() {
            return nom;
        }
    }
}
