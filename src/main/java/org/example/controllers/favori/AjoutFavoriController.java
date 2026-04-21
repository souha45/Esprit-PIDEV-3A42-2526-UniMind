package org.example.controllers.favori;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.entities.Favori;
import org.example.services.FavoriService;
import org.example.utils.NavigationContext;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AjoutFavoriController {

    @FXML
    private TextField txtEvenementId;
    @FXML
    private TextField txtEtudiantId;

    @FXML
    private Label lblErreurEvenementId;
    @FXML
    private Label lblErreurEtudiantId;
    @FXML
    private Label lblErreurUnicite;
    @FXML
    private Label lblSucces;

    private final FavoriService favoriService = new FavoriService();

    @FXML
    public void initialize() {
        // Valeur par défaut pour l'étudiant (peut être changé selon le contexte)
        txtEtudiantId.setText("1");
    }

    @FXML
    private void enregistrerFavori(ActionEvent event) {
        cacherErreurs();
        lblSucces.setVisible(false);

        boolean valide = validerFormulaire();
        if (!valide) {
            return;
        }

        try {
            Favori favori = creerFavori();
            favoriService.ajouter(favori);

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
            NavigationContext.loadContentInCenter("/favori/FavorisEtudiant.fxml");
        } catch (IOException e) {
            afficherErreur("Erreur lors de la navigation : " + e.getMessage());
        }
    }

    private boolean validerFormulaire() {
        List<String> erreurs = new ArrayList<>();

        // Valider evenementId
        if (txtEvenementId.getText() == null || txtEvenementId.getText().trim().isEmpty()) {
            lblErreurEvenementId.setVisible(true);
            erreurs.add("ID de l'événement : obligatoire");
        }

        // Valider etudiantId
        if (txtEtudiantId.getText() == null || txtEtudiantId.getText().trim().isEmpty()) {
            lblErreurEtudiantId.setVisible(true);
            erreurs.add("ID de l'étudiant : obligatoire");
        }

        // Valider unicité (evenementId + etudiantId)
        if (!txtEvenementId.getText().trim().isEmpty() && !txtEtudiantId.getText().trim().isEmpty()) {
            try {
                int evenementId = Integer.parseInt(txtEvenementId.getText().trim());
                int etudiantId = Integer.parseInt(txtEtudiantId.getText().trim());
                if (favoriService.verifierUnicite(evenementId, etudiantId)) {
                    lblErreurUnicite.setVisible(true);
                    erreurs.add("Unicité : ce favori existe déjà");
                }
            } catch (NumberFormatException e) {
                erreurs.add("Les IDs doivent être des nombres valides");
            } catch (SQLException e) {
                erreurs.add("Erreur lors de la vérification d'unicité : " + e.getMessage());
            }
        }

        if (!erreurs.isEmpty()) {
            afficherErreur("Le formulaire contient des erreurs. Vérifiez les champs marqués en rouge.");
            return false;
        }

        return true;
    }

    private Favori creerFavori() throws NumberFormatException {
        int evenementId = Integer.parseInt(txtEvenementId.getText().trim());
        int etudiantId = Integer.parseInt(txtEtudiantId.getText().trim());

        return new Favori(evenementId, etudiantId);
    }

    private void cacherErreurs() {
        lblErreurEvenementId.setVisible(false);
        lblErreurEtudiantId.setVisible(false);
        lblErreurUnicite.setVisible(false);
    }

    private void afficherErreur(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
