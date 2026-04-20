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
import org.example.enums.TypeEvenement;
import org.example.enums.StatutEvenement;
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

    @FXML
    private ComboBox<TypeEvenement> comboType;

    @FXML
    private ComboBox<StatutEvenement> comboStatut;

    @FXML
    private ComboBox<String> comboOrganisateur;

    @FXML
    private DatePicker dateDu;

    @FXML
    private DatePicker dateAu;

    @FXML
    private Label lblStatutAVenir;

    @FXML
    private Label lblStatutEnCours;

    @FXML
    private Label lblStatutTermine;

    @FXML
    private Label lblStatutAnnule;

    @FXML
    private Label lblPleins;

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

        // Initialiser les filtres
        initialiserFiltres();

        // Configurer les colonnes
        configurerColonnes();

        // Charger les données
        chargerEvenements();
    }

    private void initialiserFiltres() {
        comboType.getItems().setAll(TypeEvenement.values());
        comboStatut.getItems().setAll(StatutEvenement.values());

        // Charger la liste des organisateurs
        try {
            java.util.Set<String> organisateurs = new java.util.HashSet<>();
            for (Evenement e : evenementService.afficher()) {
                String nomOrganisateur = evenementService.getNomOrganisateur(e.getOrganisateurId());
                if (nomOrganisateur != null && !nomOrganisateur.trim().isEmpty()) {
                    organisateurs.add(nomOrganisateur);
                }
            }
            comboOrganisateur.getItems().setAll(organisateurs);
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des organisateurs: " + e.getMessage());
        }

        // Configurer les cell factories pour afficher les noms lisibles
        comboType.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TypeEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        comboType.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TypeEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous" : item.name());
            }
        });

        comboStatut.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StatutEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        comboStatut.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StatutEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous" : item.name());
            }
        });
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

        // Coloration des lignes selon le statut
        tableEvenements.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(EvenementService.EvenementAvecOrganisateurNom item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    Evenement e = item.getEvenement();
                    String style = "";
                    switch (e.getStatut()) {
                        case A_VENIR:
                            style = "-fx-background-color: #e8f5e9;"; // Vert pastel
                            break;
                        case EN_COURS:
                            style = "-fx-background-color: #fff9c4;"; // Jaune pastel
                            break;
                        case TERMINE:
                            style = "-fx-background-color: #f5f5f5;"; // Gris pastel
                            break;
                        case ANNULE:
                            style = "-fx-background-color: #ffebee;"; // Rouge pastel
                            break;
                    }

                    // Mettre en évidence les événements pleins
                    if (item.getPlacesLibres() == 0) {
                        style += " -fx-border-color: #e74c3c; -fx-border-width: 2px;";
                    }

                    setStyle(style);
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

            // Calculer et afficher les statistiques
            calculerStatistiques();
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de charger les événements: " + e.getMessage());
        }
    }

    private void calculerStatistiques() {
        int aVenir = 0, enCours = 0, termine = 0, annule = 0, pleins = 0;

        for (EvenementService.EvenementAvecOrganisateurNom item : listeEvenements) {
            Evenement e = item.getEvenement();

            // Compter par statut
            switch (e.getStatut()) {
                case A_VENIR:
                    aVenir++;
                    break;
                case EN_COURS:
                    enCours++;
                    break;
                case TERMINE:
                    termine++;
                    break;
                case ANNULE:
                    annule++;
                    break;
            }

            // Compter les événements pleins
            if (item.getPlacesLibres() == 0) {
                pleins++;
            }
        }

        lblStatutAVenir.setText(aVenir + " à venir");
        lblStatutEnCours.setText(enCours + " en cours");
        lblStatutTermine.setText(termine + " terminés");
        lblStatutAnnule.setText(annule + " annulés");
        lblPleins.setText(pleins + " événements pleins");
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
            controller.setPagePrecedente("/evenement/GestionEvenement.fxml"); // Retour vers la gestion des événements
            
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
                e.getLieu().toLowerCase().contains(recherche)) {
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

    @FXML
    public void appliquerFiltres(ActionEvent event) {
        if (listeEvenements == null || listeEvenements.isEmpty()) {
            return;
        }

        TypeEvenement type = comboType.getValue();
        StatutEvenement statut = comboStatut.getValue();
        String organisateur = comboOrganisateur.getValue();
        var du = dateDu.getValue();
        var au = dateAu.getValue();

        ObservableList<EvenementService.EvenementAvecOrganisateurNom> resultats = FXCollections.observableArrayList();

        for (EvenementService.EvenementAvecOrganisateurNom item : listeEvenements) {
            Evenement e = item.getEvenement();

            // Filtre par type
            if (type != null && e.getType() != type) {
                continue;
            }

            // Filtre par statut
            if (statut != null && e.getStatut() != statut) {
                continue;
            }

            // Filtre par organisateur
            if (organisateur != null && !organisateur.trim().isEmpty() && !organisateur.equals(item.getOrganisateurNom())) {
                continue;
            }

            // Filtre par date
            if (e.getDateDebut() != null) {
                var d = e.getDateDebut().toLocalDateTime().toLocalDate();
                if (du != null && d.isBefore(du)) {
                    continue;
                }
                if (au != null && d.isAfter(au)) {
                    continue;
                }
            }

            resultats.add(item);
        }

        tableEvenements.setItems(resultats);
        lblTotal.setText(resultats.size() + " événements (filtrés)");
    }

    @FXML
    public void reinitialiserFiltres(ActionEvent event) {
        comboType.setValue(null);
        comboStatut.setValue(null);
        comboOrganisateur.setValue(null);
        dateDu.setValue(null);
        dateAu.setValue(null);
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
                fxmlPath = "/admin_dashboard.fxml";
                break;
            case RESPONSABLE_ETUDIANT:
                fxmlPath = "/dashboard_responsable.fxml";
                break;
            case ETUDIANT:
                fxmlPath = "/dashboard_etudiant.fxml";
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
