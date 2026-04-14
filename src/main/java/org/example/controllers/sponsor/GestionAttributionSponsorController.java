package org.example.controllers.sponsor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.enums.Role;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeContribution;
import org.example.services.EvenementSponsorService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;

public class GestionAttributionSponsorController {

    @FXML
    private TableView<EvenementSponsorService.AttributionAvecInfos> tableAttributions;

    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, String> colEvenement;
    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, String> colSponsor;
    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, BigDecimal> colMontant;
    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, TypeContribution> colType;
    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, StatutSponsor> colStatut;
    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, java.sql.Timestamp> colDate;
    @FXML
    private TableColumn<EvenementSponsorService.AttributionAvecInfos, Void> colActions;

    @FXML
    private Label lblTotal;

    @FXML
    private TextField txtRecherche;

    private final EvenementSponsorService attributionService = new EvenementSponsorService();
    private ObservableList<EvenementSponsorService.AttributionAvecInfos> listeAttributions;

    @FXML
    public void initialize() {
        configurerColonnes();
        chargerAttributions();
    }

    private void configurerColonnes() {
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));
        colSponsor.setCellValueFactory(new PropertyValueFactory<>("sponsorNom"));

        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantContribution"));
        colMontant.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal montant, boolean empty) {
                super.updateItem(montant, empty);
                if (empty || montant == null) {
                    setText("");
                } else {
                    setText(montant.toString() + " DT");
                }
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("typeContribution"));
        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(TypeContribution type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText("");
                } else {
                    setText(type.name());
                }
            }
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(StatutSponsor statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText("");
                } else {
                    setText(statut.name());
                    if (statut == StatutSponsor.CONFIRME) {
                        setTextFill(javafx.scene.paint.Color.GREEN);
                    } else if (statut == StatutSponsor.REFUSE) {
                        setTextFill(javafx.scene.paint.Color.RED);
                    } else if (statut == StatutSponsor.EN_ATTENTE) {
                        setTextFill(javafx.scene.paint.Color.ORANGE);
                    }
                }
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateContribution"));
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(java.sql.Timestamp date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    setText(sdf.format(date));
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EvenementSponsorService.AttributionAvecInfos attribution = getTableView().getItems().get(getIndex());

                    // Vérifier si l'utilisateur est admin ou l'organisateur de l'événement
                    boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                            .map(role -> role == Role.ADMIN)
                            .orElse(false);
                    int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
                    boolean isOrganisateur = attribution.getOrganisateurId() == currentUserId;
                    boolean peutModifier = isAdmin || isOrganisateur;

                    HBox hbox = new HBox(5);

                    // Ajouter les boutons Modifier et Supprimer seulement si l'utilisateur a les permissions
                    if (peutModifier) {
                        Button btnModifier = new Button("modifier");
                        btnModifier.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-min-width: 60px;");
                        btnModifier.setOnAction(event -> modifierAttribution(getTableView().getItems().get(getIndex())));

                        Button btnSupprimer = new Button("supprimer");
                        btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-min-width: 80px;");
                        btnSupprimer.setOnAction(event -> supprimerAttribution(getTableView().getItems().get(getIndex())));

                        hbox.getChildren().addAll(btnModifier, btnSupprimer);
                    }

                    setGraphic(hbox);
                }
            }
        });
    }

    private void chargerAttributions() {
        try {
            // Vérifier si l'utilisateur est admin
            boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                    .map(role -> role == Role.ADMIN)
                    .orElse(false);

            if (isAdmin) {
                // Admin voit toutes les attributions
                listeAttributions = FXCollections.observableArrayList(
                        attributionService.afficherAvecInfos()
                );
            } else {
                // Responsable voit seulement ses attributions
                int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
                listeAttributions = FXCollections.observableArrayList(
                        attributionService.afficherAvecInfosPourOrganisateur(currentUserId)
                );
            }

            tableAttributions.setItems(listeAttributions);
            lblTotal.setText("Total : " + listeAttributions.size());
        } catch (SQLException e) {
            afficherErreur("Erreur lors du chargement des attributions : " + e.getMessage());
        }
    }

    @FXML
    private void ajouterAttribution() {
        try {
            NavigationContext.loadContentInCenter("/sponsor/AjoutAttributionSponsor.fxml");
        } catch (IOException e) {
            afficherErreur("Erreur lors de l'ouverture du formulaire : " + e.getMessage());
        }
    }

    @FXML
    private void rechercherAttributions() {
        String recherche = txtRecherche.getText().trim().toLowerCase();

        if (recherche.isEmpty()) {
            chargerAttributions();
            return;
        }

        ObservableList<EvenementSponsorService.AttributionAvecInfos> resultats = FXCollections.observableArrayList();

        for (EvenementSponsorService.AttributionAvecInfos attribution : listeAttributions) {
            if (attribution.getEvenementTitre().toLowerCase().contains(recherche) ||
                attribution.getSponsorNom().toLowerCase().contains(recherche) ||
                attribution.getTypeContribution().toString().toLowerCase().contains(recherche) ||
                attribution.getStatut().toString().toLowerCase().contains(recherche)) {
                resultats.add(attribution);
            }
        }

        tableAttributions.setItems(resultats);
        lblTotal.setText("Total : " + resultats.size() + " (filtrés)");
    }

    @FXML
    private void reinitialiserRecherche() {
        txtRecherche.clear();
        chargerAttributions();
    }

    @FXML
    private void retour(ActionEvent event) {
        try {
            NavigationContext.loadContentInCenter("/sponsor/GestionAttributionSponsor.fxml");
        } catch (IOException e) {
            afficherErreur("Erreur lors de la navigation : " + e.getMessage());
        }
    }

    private void modifierAttribution(EvenementSponsorService.AttributionAvecInfos attribution) {
        try {
            // Récupérer l'objet EvenementSponsor complet depuis le service
            org.example.entities.EvenementSponsor attributionComplete = attributionService.findById(attribution.getEvenementSponsorId());

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/sponsor/ModificationAttributionSponsor.fxml"));
            javafx.scene.Parent root = loader.load();
            ModificationAttributionSponsorController controller = loader.getController();
            controller.setAttribution(attributionComplete);

            NavigationContext.loadContentInCenter((javafx.scene.Parent) root);
        } catch (IOException e) {
            afficherErreur("Erreur lors de l'ouverture du formulaire : " + e.getMessage());
        } catch (SQLException e) {
            afficherErreur("Erreur lors de la récupération de l'attribution : " + e.getMessage());
        }
    }

    private void supprimerAttribution(EvenementSponsorService.AttributionAvecInfos attribution) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette attribution ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                attributionService.supprimer(attribution.getEvenementSponsorId());
                afficherSucces("Attribution supprimée avec succès");
                chargerAttributions();
            } catch (SQLException e) {
                afficherErreur("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherSucces(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
