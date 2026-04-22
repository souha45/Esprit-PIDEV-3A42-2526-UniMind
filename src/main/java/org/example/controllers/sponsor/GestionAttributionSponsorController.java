package org.example.controllers.sponsor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    private Label lblTotalAttributions;

    @FXML
    private Label lblMontantTotal;

    @FXML
    private Label lblStatutConfirme;

    @FXML
    private Label lblStatutEnAttente;

    @FXML
    private Label lblStatutRefuse;

    @FXML
    private Label lblStatutAnnule;

    @FXML
    private Label lblTopSponsor;

    @FXML
    private TextField txtRecherche;

    // Filtres avancés
    @FXML
    private ComboBox<StatutSponsor> comboStatut;

    @FXML
    private ComboBox<TypeContribution> comboType;

    @FXML
    private DatePicker dateDebut;

    @FXML
    private DatePicker dateFin;

    private final EvenementSponsorService attributionService = new EvenementSponsorService();
    private ObservableList<EvenementSponsorService.AttributionAvecInfos> listeAttributions;
    private ObservableList<EvenementSponsorService.AttributionAvecInfos> listeFiltree;

    @FXML
    public void initialize() {
        initialiserFiltres();
        configurerColonnes();
        configurerColorationLignes();
        chargerAttributions();
    }

    private void initialiserFiltres() {
        // Initialiser le combo des statuts
        comboStatut.getItems().clear();
        comboStatut.getItems().add(null);
        comboStatut.getItems().addAll(StatutSponsor.values());
        comboStatut.setValue(null);

        // Initialiser le combo des types de contribution
        comboType.getItems().clear();
        comboType.getItems().add(null);
        comboType.getItems().addAll(TypeContribution.values());
        comboType.setValue(null);
    }

    private void configurerColorationLignes() {
        tableAttributions.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(EvenementSponsorService.AttributionAvecInfos attribution, boolean empty) {
                super.updateItem(attribution, empty);
                if (empty || attribution == null) {
                    setStyle("");
                } else {
                    switch (attribution.getStatut()) {
                        case EN_ATTENTE:
                            setStyle("-fx-background-color: #fff3e0;");
                            break;
                        case CONFIRME:
                            setStyle("-fx-background-color: #e8f5e9;");
                            break;
                        case REFUSE:
                            setStyle("-fx-background-color: #ffebee;");
                            break;
                        case ANNULE:
                            setStyle("-fx-background-color: #f5f5f5;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
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

                    HBox hbox = new HBox(8);

                    // Ajouter les boutons Modifier et Supprimer seulement si l'utilisateur a les permissions
                    if (peutModifier) {
                        Button btnModifier = new Button("✏");
                        btnModifier.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
                        btnModifier.setOnAction(event -> modifierAttribution(getTableView().getItems().get(getIndex())));

                        Button btnSupprimer = new Button("🗑");
                        btnSupprimer.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
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

            listeFiltree = FXCollections.observableArrayList(listeAttributions);
            tableAttributions.setItems(listeFiltree);
            lblTotal.setText("Total : " + listeFiltree.size());
            calculerStatistiques();
        } catch (SQLException e) {
            afficherErreur("Erreur lors du chargement des attributions : " + e.getMessage());
        }
    }

    private void calculerStatistiques() {
        lblTotalAttributions.setText(listeFiltree.size() + " attributions");
        lblTotal.setText("Total : " + listeFiltree.size());

        // Montant total
        BigDecimal montantTotal = listeFiltree.stream()
            .map(EvenementSponsorService.AttributionAvecInfos::getMontantContribution)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblMontantTotal.setText(montantTotal.toString() + " DT");

        // Répartition par statut
        long confirmes = listeFiltree.stream().filter(a -> a.getStatut() == StatutSponsor.CONFIRME).count();
        long enAttente = listeFiltree.stream().filter(a -> a.getStatut() == StatutSponsor.EN_ATTENTE).count();
        long refuses = listeFiltree.stream().filter(a -> a.getStatut() == StatutSponsor.REFUSE).count();
        long annules = listeFiltree.stream().filter(a -> a.getStatut() == StatutSponsor.ANNULE).count();

        lblStatutConfirme.setText(confirmes + " confirmées");
        lblStatutEnAttente.setText(enAttente + " en attente");
        lblStatutRefuse.setText(refuses + " refusées");
        lblStatutAnnule.setText(annules + " annulées");

        // Top sponsor (celui avec le plus de contributions)
        Map<String, Long> countBySponsor = listeFiltree.stream()
            .collect(Collectors.groupingBy(EvenementSponsorService.AttributionAvecInfos::getSponsorNom, Collectors.counting()));
        
        Optional<Map.Entry<String, Long>> topSponsor = countBySponsor.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (topSponsor.isPresent()) {
            lblTopSponsor.setText(topSponsor.get().getKey() + " (" + topSponsor.get().getValue() + ")");
        } else {
            lblTopSponsor.setText("-");
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
                attribution.getSponsorNom().toLowerCase().contains(recherche)) {
                resultats.add(attribution);
            }
        }

        listeFiltree.clear();
        listeFiltree.addAll(resultats);
        tableAttributions.setItems(listeFiltree);
        lblTotal.setText("Total : " + resultats.size() + " (filtrés)");
        calculerStatistiques();
    }

    @FXML
    private void appliquerFiltres() {
        listeFiltree.clear();
        listeFiltree.addAll(listeAttributions);

        // Filtrer par statut
        StatutSponsor statut = comboStatut.getValue();
        if (statut != null) {
            listeFiltree.removeIf(a -> a.getStatut() != statut);
        }

        // Filtrer par type de contribution
        TypeContribution type = comboType.getValue();
        if (type != null) {
            listeFiltree.removeIf(a -> a.getTypeContribution() != type);
        }

        // Filtrer par période
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut != null || fin != null) {
            listeFiltree.removeIf(a -> {
                if (a.getDateContribution() == null) return true;
                LocalDate dateContribution = a.getDateContribution().toLocalDateTime().toLocalDate();
                if (debut != null && dateContribution.isBefore(debut)) return true;
                if (fin != null && dateContribution.isAfter(fin)) return true;
                return false;
            });
        }

        tableAttributions.setItems(listeFiltree);
        lblTotal.setText("Total : " + listeFiltree.size() + " (filtrés)");
        calculerStatistiques();
    }

    @FXML
    private void reinitialiserFiltres() {
        comboStatut.setValue(null);
        comboType.setValue(null);
        dateDebut.setValue(null);
        dateFin.setValue(null);
        txtRecherche.clear();
        chargerAttributions();
    }

    @FXML
    private void reinitialiserRecherche() {
        txtRecherche.clear();
        chargerAttributions();
    }

    @FXML
    private void retour(ActionEvent event) throws IOException {
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ADMIN);
        if (role == Role.ADMIN) {
            NavigationContext.loadContentInCenter("/admin_dashboard.fxml");
        } else {
            NavigationContext.loadContentInCenter("/dashboard_responsable.fxml");
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
