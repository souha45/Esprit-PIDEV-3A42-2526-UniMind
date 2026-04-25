package org.example.controllers.participation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.entities.Participation;
import org.example.enums.Role;
import org.example.services.ParticipationService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.example.enums.StatutParticipation;
import org.example.services.EvenementService;
import org.example.services.evenement.PdfExportServiceEvent;

public class GestionParticipationController {

    @FXML
    private TableView<ParticipationService.ParticipationAvecNoms> tableParticipations;

    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colEvenement;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colEtudiant;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colDateInscription;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colDateEvenement;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colLieu;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colOrganisateur;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colStatut;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, String> colPlacesLibres;
    @FXML
    private TableColumn<ParticipationService.ParticipationAvecNoms, Void> colActions;

    @FXML
    private Label lblTotal;

    @FXML
    private TextField txtRecherche;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnExportPdf;

    // Filtres avancés
    @FXML
    private ComboBox<StatutParticipation> comboStatut;

    @FXML
    private ComboBox<String> comboEvenement;


    @FXML
    private DatePicker dateDu;

    @FXML
    private DatePicker dateAu;

    // Statistiques
    @FXML
    private Label lblStatutConfirme;

    @FXML
    private Label lblStatutEnAttente;

    @FXML
    private Label lblStatutAnnule;

    @FXML
    private Label lblTotalParticipations;

    @FXML
    private Pagination paginationParticipations;

    @FXML
    private Label lblPageInfo;

    private ParticipationService participationService;
    private EvenementService evenementService;
    private final PdfExportServiceEvent pdfExportService = new PdfExportServiceEvent();
    private ObservableList<ParticipationService.ParticipationAvecNoms> listeParticipations;
    private ObservableList<ParticipationService.ParticipationAvecNoms> listeFiltree;

    private static final int ITEMS_PER_PAGE = 10;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        participationService = new ParticipationService();
        evenementService = new EvenementService();
        listeParticipations = FXCollections.observableArrayList();
        listeFiltree = FXCollections.observableArrayList();

        // Vérifier si l'utilisateur est un étudiant
        Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);
        boolean estEtudiant = (role == Role.ETUDIANT);

        // Cacher le bouton ajouter pour les étudiants
        if (estEtudiant) {
            btnAjouter.setVisible(false);
            colActions.setVisible(false);
            colEtudiant.setVisible(false);
            colDateInscription.setVisible(false);
            colPlacesLibres.setVisible(false);
        } else {
            // Pour les admins/responsables, cacher la colonne Date de l'événement
            colDateEvenement.setVisible(false);
        }

        // Initialiser les filtres
        initialiserFiltres();
        
        configurerColonnes();
        configurerColorationLignes();
        chargerParticipations();

        if (paginationParticipations != null) {
            paginationParticipations.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                if (newIdx != null) {
                    updateTableForPage(newIdx.intValue());
                }
            });
        }
    }

    private void configurerColorationLignes() {
        tableParticipations.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(ParticipationService.ParticipationAvecNoms participation, boolean empty) {
                super.updateItem(participation, empty);
                if (empty || participation == null) {
                    setStyle("");
                } else {
                    switch (participation.getStatut()) {
                        case EN_ATTENTE:
                        case ANNULE:
                            setStyle("-fx-background-color: #ffebee;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }

    private void initialiserFiltres() {
        // Initialiser le combo des statuts
        comboStatut.getItems().addAll(null, StatutParticipation.EN_ATTENTE, StatutParticipation.CONFIRME, StatutParticipation.ANNULE);
        comboStatut.setValue(null);

        // Initialiser le combo des événements
        try {
            comboEvenement.getItems().clear();
            comboEvenement.getItems().add("Tous les événements");
            for (var evenement : evenementService.afficher()) {
                comboEvenement.getItems().add(evenement.getTitre());
            }
            comboEvenement.setValue("Tous les événements");
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des événements: " + e.getMessage());
        }
    }

    private void configurerColonnes() {
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));
        colEtudiant.setCellValueFactory(new PropertyValueFactory<>("etudiantNom"));
        colDateInscription.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateInscription().toLocalDateTime().format(dateFormatter)));
        colDateEvenement.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getEvenementDateDebut() != null
                            ? cellData.getValue().getEvenementDateDebut().toLocalDateTime().format(dateFormatter)
                            : "-"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("evenementLieu"));
        colOrganisateur.setCellValueFactory(new PropertyValueFactory<>("organisateurNom"));
        colStatut.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut().toString()));
        colPlacesLibres.setCellValueFactory(cellData -> {
            int placesLibres = cellData.getValue().getPlacesLibres();
            int capaciteMax = cellData.getValue().getCapaciteMax();
            if (placesLibres == -1) {
                return new javafx.beans.property.SimpleStringProperty("Illimité");
            } else {
                return new javafx.beans.property.SimpleStringProperty(placesLibres + " / " + capaciteMax);
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ParticipationService.ParticipationAvecNoms participation = getTableView().getItems().get(getIndex());

                    // Vérifier si l'utilisateur est admin ou l'organisateur de l'événement
                    boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                            .map(role -> role == Role.ADMIN)
                            .orElse(false);
                    int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
                    boolean isOrganisateur = participation.getOrganisateurId() == currentUserId;
                    boolean peutModifier = isAdmin || isOrganisateur;

                    HBox hbox = new HBox(8);

                    // Ajouter les boutons Modifier et Supprimer seulement si l'utilisateur a les permissions
                    if (peutModifier) {
                        Button btnModifier = new Button("✏");
                        btnModifier.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
                        btnModifier.setOnAction(event -> modifierParticipation(getTableView().getItems().get(getIndex())));

                        Button btnSupprimer = new Button("🗑");
                        btnSupprimer.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
                        btnSupprimer.setOnAction(event -> supprimerParticipation(getTableView().getItems().get(getIndex())));

                        hbox.getChildren().addAll(btnModifier, btnSupprimer);
                    }

                    setGraphic(hbox);
                }
            }
        });
    }

    private void chargerParticipations() {
        try {
            listeParticipations.clear();
            Role role = SessionManager.getInstance().getCurrentUserRole().orElse(Role.ETUDIANT);
            int currentUserId = SessionManager.getInstance().getCurrentUserId().orElse(-1);

            if (role == Role.ETUDIANT) {
                // Filtrer par étudiant
                for (ParticipationService.ParticipationAvecNoms participation : participationService.afficherAvecNoms()) {
                    if (participation.getEtudiantId() == currentUserId) {
                        listeParticipations.add(participation);
                    }
                }
            } else {
                // Admin et responsable voient tout
                listeParticipations.addAll(participationService.afficherAvecNoms());
            }

            applyFilteredList(listeParticipations);
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de charger les participations: " + e.getMessage());
        }
    }

    private void applyFilteredList(List<ParticipationService.ParticipationAvecNoms> newList) {
        listeFiltree.setAll(newList);

        // Mettre à jour pagination
        if (paginationParticipations != null) {
            int pageCount = (int) Math.ceil((double) listeFiltree.size() / ITEMS_PER_PAGE);
            paginationParticipations.setPageCount(Math.max(pageCount, 1));
            paginationParticipations.setCurrentPageIndex(0);
            updateTableForPage(0);
        } else {
            tableParticipations.setItems(listeFiltree);
            if (lblPageInfo != null) lblPageInfo.setText("");
        }

        calculerStatistiques();
    }

    private void updateTableForPage(int pageIndex) {
        if (listeFiltree == null) {
            tableParticipations.setItems(FXCollections.observableArrayList());
            if (lblPageInfo != null) lblPageInfo.setText("");
            return;
        }

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, listeFiltree.size());

        ObservableList<ParticipationService.ParticipationAvecNoms> pageItems;
        if (fromIndex >= listeFiltree.size() || fromIndex < 0) {
            pageItems = FXCollections.observableArrayList();
        } else {
            pageItems = FXCollections.observableArrayList(listeFiltree.subList(fromIndex, toIndex));
        }

        tableParticipations.setItems(pageItems);

        if (lblPageInfo != null) {
            int total = listeFiltree.size();
            if (total == 0) {
                lblPageInfo.setText("Aucun résultat");
            } else {
                lblPageInfo.setText("Affichage " + (fromIndex + 1) + " - " + toIndex + " sur " + total);
            }
        }
    }

    private void calculerStatistiques() {
        int confirmes = 0;
        int enAttente = 0;
        int annulees = 0;

        for (ParticipationService.ParticipationAvecNoms participation : listeFiltree) {
            switch (participation.getStatut()) {
                case CONFIRME:
                    confirmes++;
                    break;
                case EN_ATTENTE:
                    enAttente++;
                    break;
                case ANNULE:
                    annulees++;
                    break;
            }
        }

        if (lblStatutConfirme != null) lblStatutConfirme.setText(confirmes + " confirmées");
        if (lblStatutEnAttente != null) lblStatutEnAttente.setText(enAttente + " en attente");
        if (lblStatutAnnule != null) lblStatutAnnule.setText(annulees + " annulées");
        if (lblTotalParticipations != null) lblTotalParticipations.setText(listeFiltree.size() + " participations");
        if (lblTotal != null) lblTotal.setText(listeFiltree.size() + " participations");
    }

    @FXML
    private void ajouterParticipation(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/participation/AjoutParticipation.fxml");
    }

    @FXML
    private void rechercherParticipations(ActionEvent event) {
        String recherche = txtRecherche.getText().trim().toLowerCase();

        if (recherche.isEmpty()) {
            chargerParticipations();
            return;
        }

        ObservableList<ParticipationService.ParticipationAvecNoms> resultats = FXCollections.observableArrayList();

        for (ParticipationService.ParticipationAvecNoms participation : listeParticipations) {
            if (participation.getEvenementTitre().toLowerCase().contains(recherche) ||
                participation.getEtudiantNom().toLowerCase().contains(recherche)) {
                resultats.add(participation);
            }
        }

        applyFilteredList(resultats);
    }

    @FXML
    private void appliquerFiltres(ActionEvent event) {
        List<ParticipationService.ParticipationAvecNoms> resultats = new ArrayList<>(listeParticipations);

        // Filtrer par statut
        StatutParticipation statut = comboStatut.getValue();
        if (statut != null) {
            resultats.removeIf(p -> p.getStatut() != statut);
        }

        // Filtrer par événement
        String evenement = comboEvenement.getValue();
        if (evenement != null && !evenement.equals("Tous les événements")) {
            resultats.removeIf(p -> !p.getEvenementTitre().equals(evenement));
        }


        // Filtrer par dates
        LocalDate du = dateDu.getValue();
        LocalDate au = dateAu.getValue();
        if (du != null || au != null) {
            resultats.removeIf(p -> {
                LocalDate dateInscription = p.getDateInscription().toLocalDateTime().toLocalDate();
                if (du != null && dateInscription.isBefore(du)) return true;
                if (au != null && dateInscription.isAfter(au)) return true;
                return false;
            });
        }

        applyFilteredList(resultats);
    }

    @FXML
    private void reinitialiserFiltres(ActionEvent event) {
        comboStatut.setValue(null);
        comboEvenement.setValue("Tous les événements");
        dateDu.setValue(null);
        dateAu.setValue(null);
        txtRecherche.clear();
        chargerParticipations();
    }

    @FXML
    private void reinitialiserRecherche(ActionEvent event) {
        txtRecherche.clear();
        chargerParticipations();
    }

    private void supprimerParticipation(ParticipationService.ParticipationAvecNoms participation) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer la participation");
            confirmation.setContentText("Voulez-vous vraiment supprimer cette participation (" + participation.getEvenementTitre() + " - " + participation.getEtudiantNom() + ") ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                participationService.supprimer(participation.getParticipationId());
                chargerParticipations();
                afficherAlerte("Succès", "Participation supprimée avec succès");
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer la participation: " + e.getMessage());
        }
    }

    private void modifierParticipation(ParticipationService.ParticipationAvecNoms participation) {
        try {
            // Récupérer l'objet Participation complet depuis le service
            Participation participationComplete = participationService.findById(participation.getParticipationId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/participation/ModificationParticipation.fxml"));
            Parent root = loader.load();
            ModificationParticipationController controller = loader.getController();
            controller.setParticipation(participationComplete);

            NavigationContext.loadContentInCenter(root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de modification: " + e.getMessage());
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de récupérer la participation: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de modifier la participation: " + e.getMessage());
        }
    }

    @FXML
    public void retourAccueil(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/participation/GestionParticipation.fxml");
    }

    @FXML
    private void exporterPdf() {
        try {
            // Créer un FileChooser pour choisir l'emplacement de sauvegarde
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Enregistrer la liste PDF");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
            );
            fileChooser.setInitialFileName("liste_participations.pdf");

            // Obtenir la fenêtre principale
            javafx.stage.Window window = btnExportPdf.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(window);

            if (file != null) {
                // Pour l'instant, générer une liste vide (à améliorer plus tard)
                List<org.example.entities.Participation> participations = new ArrayList<>();
                
                // Générer le PDF
                pdfExportService.exportParticipationsList(participations, file.getAbsolutePath());
                
                afficherAlerte("Succès", "La liste PDF a été générée avec succès !");
            }
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de générer le PDF: " + e.getMessage());
        }
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
