package org.example.controllers.evenement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.entities.Evenement;
import org.example.enums.Role;
import org.example.services.EvenementService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class GestionEvenementController {

    @FXML
    private TableView<EvenementService.EvenementAvecOrganisateurNom> tableEvenements;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colImage;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colTitre;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colType;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colDateDebut;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colLieu;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colOrganisateur;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colStatut;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, String> colPlacesLibres;

    @FXML
    private TableColumn<EvenementService.EvenementAvecOrganisateurNom, Void> colActions;

    @FXML
    private TextField txtRecherche;

    @FXML
    private Label lblTotal;

    @FXML
    private Button btnAjouter;

    private EvenementService evenementService;
    private ObservableList<EvenementService.EvenementAvecOrganisateurNom> listeEvenements;

    // Formatter pour les dates
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        evenementService = new EvenementService();
        listeEvenements = FXCollections.observableArrayList();

        // Mettre à jour automatiquement les images (une seule fois)
        try {
            evenementService.mettreAJourImagesAutomatiquement();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour automatique des images: " + e.getMessage());
        }

        // Cacher le bouton ajouter pour les étudiants
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);
        if (role == Role.ETUDIANT) {
            btnAjouter.setVisible(false);
        }

        // Configurer les colonnes
        configurerColonnes();

        // Charger les données
        chargerEvenements();
    }

    private void configurerColonnes() {
        // Configurer la colonne Image pour afficher des miniatures d'images
        colImage.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            private final javafx.scene.control.Label lblPasImage = new javafx.scene.control.Label("N/A");

            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
                lblPasImage.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(String imageName, boolean empty) {
                super.updateItem(imageName, empty);
                if (empty || imageName == null || imageName.trim().isEmpty()) {
                    setGraphic(lblPasImage);
                    imageView.setImage(null);
                } else {
                    try {
                        // Construire l'URL complète avec XAMPP
                        String encodedImageName = java.net.URLEncoder.encode(imageName.trim(), java.nio.charset.StandardCharsets.UTF_8);
                        String fullImageUrl = "http://localhost/uploadsEvent/evenements/" + encodedImageName;
                        javafx.scene.image.Image image = new javafx.scene.image.Image(fullImageUrl, true);
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(lblPasImage);
                    }
                }
            }
        });
        colImage.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvenement().getImage()));

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvenement().getType().toString()));
        colDateDebut.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEvenement().getDateDebut().toLocalDateTime().format(dateFormatter)));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colOrganisateur.setCellValueFactory(new PropertyValueFactory<>("organisateurNom"));
        colStatut.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvenement().getStatut().toString()));
        colPlacesLibres.setCellValueFactory(cellData -> {
            int placesLibres = cellData.getValue().getPlacesLibres();
            int capaciteMax = cellData.getValue().getEvenement().getCapaciteMax();
            if (placesLibres == -1) {
                return new javafx.beans.property.SimpleStringProperty("Illimité");
            } else {
                return new javafx.beans.property.SimpleStringProperty(placesLibres + " / " + capaciteMax);
            }
        });

        // Configurer la colonne Actions avec boutons Voir/Modifier/Supprimer
        colActions.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EvenementService.EvenementAvecOrganisateurNom evenementAvecNom = getTableView().getItems().get(getIndex());
                    Evenement evenement = evenementAvecNom.getEvenement();

                    // Vérifier si l'utilisateur est admin ou l'organisateur de l'événement
                    boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                            .map(role -> role == Role.ADMIN)
                            .orElse(false);
                    int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
                    boolean isOrganisateur = evenement.getOrganisateurId() == currentUserId;
                    boolean peutModifier = isAdmin || isOrganisateur;

                    // Créer les boutons selon les permissions
                    Button btnVoir = new Button("voir");
                    btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-min-width: 60px;");
                    btnVoir.setOnAction(event -> voirEvenement(getTableView().getItems().get(getIndex())));

                    HBox hbox = new HBox(5, btnVoir);

                    // Ajouter les boutons Modifier et Supprimer seulement si l'utilisateur a les permissions
                    if (peutModifier) {
                        Button btnModifier = new Button("modifier");
                        btnModifier.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-min-width: 60px;");
                        btnModifier.setOnAction(event -> modifierEvenement(getTableView().getItems().get(getIndex())));

                        Button btnSupprimer = new Button("supprimer");
                        btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-min-width: 80px;");
                        btnSupprimer.setOnAction(event -> supprimerEvenement(getTableView().getItems().get(getIndex())));

                        hbox.getChildren().addAll(btnModifier, btnSupprimer);
                    }

                    setGraphic(hbox);
                }
            }
        });
    }

    private void chargerEvenements() {
        try {
            listeEvenements.clear();
            // Charger les événements avec les noms des organisateurs
            for (Evenement e : evenementService.afficher()) {
                String nomOrganisateur = evenementService.getNomOrganisateur(e.getOrganisateurId());
                EvenementService.EvenementAvecOrganisateurNom evenementAvecNom = new EvenementService.EvenementAvecOrganisateurNom(e, nomOrganisateur);

                // Calculer les places libres
                int capaciteMax = e.getCapaciteMax();
                int nombreInscrits = compterParticipations(e.getEvenementId());
                int placesLibres = capaciteMax > 0 ? capaciteMax - nombreInscrits : -1; // -1 signifie illimité
                evenementAvecNom.setPlacesLibres(placesLibres);

                listeEvenements.add(evenementAvecNom);
            }
            tableEvenements.setItems(listeEvenements);
            lblTotal.setText(listeEvenements.size() + " événements");
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de charger les événements: " + e.getMessage());
        }
    }

    /**
     * Compte le nombre de participations pour un événement
     * @param evenementId L'ID de l'événement
     * @return Le nombre de participations
     */
    private int compterParticipations(int evenementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM participation WHERE evenement_id = ?";
        try (PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @FXML
    private void ajouterEvenement(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/evenement/AjoutEvenement.fxml");
    }

    private void voirEvenement(EvenementService.EvenementAvecOrganisateurNom evenementAvecNom) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evenement/VoirEvenement.fxml"));
            Parent root = loader.load();
            VoirEvenementController controller = loader.getController();
            controller.setEvenement(evenementAvecNom.getEvenement());
            
            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de détails: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible d'afficher l'événement: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherEvenements(ActionEvent actionEvent) {
        String recherche = txtRecherche.getText().trim().toLowerCase();

        if (recherche.isEmpty()) {
            chargerEvenements();
            return;
        }

        ObservableList<EvenementService.EvenementAvecOrganisateurNom> resultat = FXCollections.observableArrayList();

        for (EvenementService.EvenementAvecOrganisateurNom item : listeEvenements) {
            Evenement e = item.getEvenement();
            if (e.getTitre().toLowerCase().contains(recherche) ||
                e.getLieu().toLowerCase().contains(recherche) ||
                e.getType().toString().toLowerCase().contains(recherche) ||
                item.getOrganisateurNom().toLowerCase().contains(recherche)) {
                resultat.add(item);
            }
        }

        tableEvenements.setItems(resultat);
        lblTotal.setText(resultat.size() + " événements (filtrés)");
    }

    @FXML
    private void reinitialiserRecherche(ActionEvent actionEvent) {
        txtRecherche.clear();
        chargerEvenements();
    }

    private void modifierEvenement(EvenementService.EvenementAvecOrganisateurNom evenementAvecNom) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evenement/ModificationEvenement.fxml"));
            Parent root = loader.load();
            ModificationEvenementController controller = loader.getController();
            controller.setEvenement(evenementAvecNom.getEvenement());
            
            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de modification: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de modifier l'événement: " + e.getMessage());
        }
    }

    private void supprimerEvenement(EvenementService.EvenementAvecOrganisateurNom evenementAvecNom) {
        Evenement evenement = evenementAvecNom.getEvenement();
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer l'événement");
            confirmation.setContentText("Voulez-vous vraiment supprimer l'événement \"" + evenement.getTitre() + "\" ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                evenementService.supprimer(evenement.getEvenementId());
                chargerEvenements(); // Recharger la liste
                afficherAlerte("Succès", "Événement supprimé avec succès");
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer l'événement: " + e.getMessage());
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
