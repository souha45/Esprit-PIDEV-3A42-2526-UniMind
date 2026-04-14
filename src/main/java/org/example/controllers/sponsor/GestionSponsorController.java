package org.example.controllers.sponsor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.entities.Sponsor;
import org.example.enums.Role;
import org.example.services.SponsorService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class GestionSponsorController {

    @FXML
    private TableView<SponsorService.SponsorAvecInfos> tableSponsors;

    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colEvenement;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colLogo;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colNom;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colType;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colEmail;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colStatut;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colDate;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, Void> colActions;

    @FXML
    private Label lblTotal;

    @FXML
    private TextField txtRecherche;

    private SponsorService sponsorService;
    private ObservableList<SponsorService.SponsorAvecInfos> listeSponsors;

    @FXML
    public void initialize() {
        // Vérifier si l'utilisateur est admin
        boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        if (!isAdmin) {
            afficherAlerte("Accès refusé", "L'accès à la gestion des sponsors est réservé à l'administrateur. Les responsables gèrent les attributions de sponsors aux événements via le menu 'Attributions'.");
            naviguerVersRetour();
            return;
        }

        sponsorService = new SponsorService();
        listeSponsors = FXCollections.observableArrayList();

        configurerColonnes();
        chargerSponsors();
    }

    private void naviguerVersRetour() {
        try {
            NavigationContext.loadContentInCenter("/evenement/AdminDashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurerColonnes() {
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));
        colLogo.setCellValueFactory(new PropertyValueFactory<>("logo"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomSponsor"));
        colType.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTypeSponsor().toString()));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("emailContact"));
        colStatut.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut().toString()));
        colDate.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateContribution() != null ?
                                cellData.getValue().getDateContribution().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                                "N/A"));

        // Configurer la colonne Logo pour afficher des miniatures d'images
        colLogo.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            private final javafx.scene.control.Label lblPasLogo = new javafx.scene.control.Label("N/A");

            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
                lblPasLogo.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(String logoName, boolean empty) {
                super.updateItem(logoName, empty);
                if (empty || logoName == null || logoName.trim().isEmpty()) {
                    setGraphic(lblPasLogo);
                    imageView.setImage(null);
                } else {
                    try {
                        // Construire l'URL complète avec XAMPP
                        String encodedLogoName = java.net.URLEncoder.encode(logoName.trim(), java.nio.charset.StandardCharsets.UTF_8);
                        String fullLogoUrl = "http://localhost/uploadsEvent/sponsors/" + encodedLogoName;
                        javafx.scene.image.Image logo = new javafx.scene.image.Image(fullLogoUrl, true);
                        imageView.setImage(logo);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(lblPasLogo);
                    }
                }
            }
        });
        colLogo.setCellValueFactory(new PropertyValueFactory<>("logo"));

        colActions.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SponsorService.SponsorAvecInfos sponsor = getTableView().getItems().get(getIndex());

                    // Créer le bouton Voir
                    Button btnVoir = new Button("voir");
                    btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-min-width: 60px;");
                    btnVoir.setOnAction(event -> voirSponsor(getTableView().getItems().get(getIndex())));

                    Button btnModifier = new Button("modifier");
                    btnModifier.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-min-width: 60px;");
                    btnModifier.setOnAction(event -> modifierSponsor(getTableView().getItems().get(getIndex())));

                    Button btnSupprimer = new Button("supprimer");
                    btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-min-width: 80px;");
                    btnSupprimer.setOnAction(event -> supprimerSponsor(getTableView().getItems().get(getIndex())));

                    HBox hbox = new HBox(5, btnVoir, btnModifier, btnSupprimer);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void chargerSponsors() {
        try {
            listeSponsors.clear();
            // Charger les sponsors simples
            java.util.List<org.example.entities.Sponsor> sponsors = sponsorService.afficher();

            // Créer les objets SponsorAvecInfos en ajoutant les informations de sponsoring
            for (org.example.entities.Sponsor sponsor : sponsors) {
                SponsorService.SponsorAvecInfos info = new SponsorService.SponsorAvecInfos();
                info.setSponsorId(sponsor.getSponsorId());
                info.setNomSponsor(sponsor.getNomSponsor());
                info.setTypeSponsor(sponsor.getTypeSponsor());
                info.setEmailContact(sponsor.getEmailContact());
                info.setLogo(sponsor.getLogo());
                info.setStatut(sponsor.getStatut());

                // Récupérer les informations de sponsoring (événement, date et organisateur)
                Object[] sponsoringInfos = sponsorService.getSponsoringInfos(sponsor.getSponsorId());
                info.setEvenementTitre((String) sponsoringInfos[0]);
                if (sponsoringInfos[1] != null) {
                    info.setDateContribution(java.sql.Timestamp.valueOf((String) sponsoringInfos[1]));
                }
                info.setOrganisateurId((Integer) sponsoringInfos[2]);

                listeSponsors.add(info);
            }

            tableSponsors.setItems(listeSponsors);
            lblTotal.setText(listeSponsors.size() + " sponsors");
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de charger les sponsors: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterSponsor(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/sponsor/AjoutSponsor.fxml");
    }

    @FXML
    private void rechercherSponsors(ActionEvent event) {
        String recherche = txtRecherche.getText().trim().toLowerCase();

        if (recherche.isEmpty()) {
            chargerSponsors();
            return;
        }

        ObservableList<SponsorService.SponsorAvecInfos> resultats = FXCollections.observableArrayList();

        for (SponsorService.SponsorAvecInfos sponsor : listeSponsors) {
            if (sponsor.getNomSponsor().toLowerCase().contains(recherche) ||
                sponsor.getEmailContact().toLowerCase().contains(recherche) ||
                sponsor.getTypeSponsor().toString().toLowerCase().contains(recherche) ||
                (sponsor.getEvenementTitre() != null && sponsor.getEvenementTitre().toLowerCase().contains(recherche))) {
                resultats.add(sponsor);
            }
        }

        tableSponsors.setItems(resultats);
        lblTotal.setText(resultats.size() + " sponsors (filtrés)");
    }

    @FXML
    private void reinitialiserRecherche(ActionEvent event) {
        txtRecherche.clear();
        chargerSponsors();
    }

    private void voirSponsor(SponsorService.SponsorAvecInfos sponsor) {
        try {
            // Récupérer l'objet Sponsor complet depuis le service
            Sponsor sponsorComplete = sponsorService.findById(sponsor.getSponsorId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sponsor/VoirSponsor.fxml"));
            Parent root = loader.load();
            VoirSponsorController controller = loader.getController();
            controller.setSponsor(sponsorComplete);
            
            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de détails: " + e.getMessage());
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de récupérer le sponsor: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible d'afficher le sponsor: " + e.getMessage());
        }
    }

    private void modifierSponsor(SponsorService.SponsorAvecInfos sponsor) {
        try {
            // Récupérer l'objet Sponsor complet depuis le service
            Sponsor sponsorComplete = sponsorService.findById(sponsor.getSponsorId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sponsor/ModificationSponsor.fxml"));
            Parent root = loader.load();
            ModificationSponsorController controller = loader.getController();
            controller.setSponsor(sponsorComplete);
            
            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de modification: " + e.getMessage());
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de récupérer le sponsor: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de modifier le sponsor: " + e.getMessage());
        }
    }

    private void supprimerSponsor(SponsorService.SponsorAvecInfos sponsor) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le sponsor");
            confirmation.setContentText("Voulez-vous vraiment supprimer le sponsor \"" + sponsor.getNomSponsor() + "\" ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                sponsorService.supprimer(sponsor.getSponsorId());
                chargerSponsors();
                afficherAlerte("Succès", "Sponsor supprime avec succes");
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer le sponsor: " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil(ActionEvent event) throws IOException {
        // Recharger le dashboard Admin (qui affichera les statistiques par défaut)
        NavigationContext.loadContentInCenter("/evenement/AdminDashboard.fxml");
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

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
