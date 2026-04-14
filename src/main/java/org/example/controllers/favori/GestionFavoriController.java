package org.example.controllers.favori;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.enums.Role;
import org.example.services.FavoriService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class GestionFavoriController {

    @FXML
    private TableView<FavoriService.FavoriAvecNoms> tableFavoris;

    @FXML
    private TableColumn<FavoriService.FavoriAvecNoms, String> colEvenement;

    @FXML
    private TableColumn<FavoriService.FavoriAvecNoms, String> colEtudiant;

    @FXML
    private TableColumn<FavoriService.FavoriAvecNoms, String> colDateAjout;

    @FXML
    private TableColumn<FavoriService.FavoriAvecNoms, Void> colActions;

    @FXML
    private Label lblTotal;

    @FXML
    private Button btnAjouter;

    private FavoriService favoriService;
    private ObservableList<FavoriService.FavoriAvecNoms> listeFavoris;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        favoriService = new FavoriService();
        listeFavoris = FXCollections.observableArrayList();

        // Vérifier si l'utilisateur est un étudiant
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);
        boolean estEtudiant = (role == Role.ETUDIANT);

        // Cacher le bouton ajouter et la colonne étudiant pour les étudiants
        if (estEtudiant) {
            btnAjouter.setVisible(false);
            colEtudiant.setVisible(false);
        }

        configurerColonnes();
        chargerFavoris();
    }

    private void configurerColonnes() {
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));
        colEtudiant.setCellValueFactory(new PropertyValueFactory<>("etudiantNom"));
        colDateAjout.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCreatedAt().toLocalDateTime().format(dateFormatter)));

        // Configurer la colonne Actions avec bouton Supprimer
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnSupprimer = new Button("supprimer");

            {
                btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-min-width: 80px;");
                btnSupprimer.setOnAction(event -> supprimerFavori(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnSupprimer);
                }
            }
        });
    }

    private void chargerFavoris() {
        try {
            listeFavoris.clear();
            Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);

            if (role == Role.ETUDIANT) {
                // Filtrer par étudiant
                for (FavoriService.FavoriAvecNoms favori : favoriService.afficherAvecNoms()) {
                    if (favori.getEtudiantId() == currentUserId) {
                        listeFavoris.add(favori);
                    }
                }
            } else {
                // Admin et responsable voient tout
                listeFavoris.addAll(favoriService.afficherAvecNoms());
            }

            tableFavoris.setItems(listeFavoris);
            lblTotal.setText(listeFavoris.size() + " favoris");
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de charger les favoris: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterFavori(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/favori/AjoutFavori.fxml");
    }

    private void supprimerFavori(FavoriService.FavoriAvecNoms favori) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le favori");
            confirmation.setContentText("Voulez-vous vraiment supprimer ce favori (" + favori.getEvenementTitre() + " - " + favori.getEtudiantNom() + ") ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                favoriService.supprimer(favori.getId());
                chargerFavoris();
                afficherAlerte("Succès", "Favori supprimé avec succès");
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer le favori: " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil(ActionEvent event) throws IOException {
        // Rediriger vers le dashboard selon le rôle de l'utilisateur connecté
        String fxmlPath;

        org.example.enums.Role role = org.example.utils.SessionManager.getInstance().getCurrentUserRole()
                .orElse(org.example.enums.Role.ETUDIANT);

        switch (role) {
            case ADMIN:
                fxmlPath = "/evenement/AdminDashboard.fxml";
                break;
            case RESPONSABLE_ETUDIANT:
                fxmlPath = "/evenement/ResponsableDashboard.fxml";
                break;
            case ETUDIANT:
                fxmlPath = "/evenement/EtudiantDashboard.fxml";
                break;
            default:
                fxmlPath = "/evenement/AccueilEvenement.fxml";
                break;
        }

        // Recharger le dashboard (qui affichera les statistiques par défaut)
        NavigationContext.loadContentInCenter(fxmlPath);
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
