package org.example.controllers.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.entities.*;
import org.example.services.AdminService;
import org.example.utils.ValidationUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminDashboardController {

    // Instance statique pour permettre l'accès depuis d'autres contrôleurs
    private static AdminDashboardController instance;

    public static AdminDashboardController getInstance() {
        return instance;
    }

    // NAVBAR
    @FXML private TextField navRechercheField;
    @FXML private Label navNomAdmin;
    @FXML private ImageView navPhotoAdmin;

    // SIDEBAR
    @FXML private Button btnGestionUsers;
    @FXML private Button btnDemandes;
    @FXML private Button btnProfil;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnStats;
    @FXML private Button btnSeances;
    @FXML private Button btnEvenements;
    @FXML private Button btnParticipations;
    @FXML private Button btnSponsors;
    @FXML private Button btnFeedbacks;

    // CONTENU PRINCIPAL (StackPane)
    @FXML private StackPane contentArea;

    // PANNEAUX FIXES (à l'intérieur du StackPane)
    @FXML private VBox panneauGestion;
    @FXML private VBox panneauDemandes;
    @FXML private VBox panneauProfil;

    // GESTION USERS : TABLE
    @FXML private TableView<User> tableUtilisateurs;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, Boolean> colActif;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private Label messageGestion;

    // DEMANDES EN ATTENTE
    @FXML private TableView<User> tableDemandes;
    @FXML private TableColumn<User, String> dColNom;
    @FXML private TableColumn<User, String> dColPrenom;
    @FXML private TableColumn<User, String> dColEmail;
    @FXML private TableColumn<User, String> dColRole;
    @FXML private Label messageDemandes;
    @FXML private Label labelNbResultats;
    // Ajoutez ces déclarations avec les autres @FXML
    @FXML private ComboBox<String> filtreRole;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtreActif;
    @FXML private Button btnReinitialiserFiltres;
    // PROFIL ADMIN
    @FXML private Label pNomPrenom, pEmail, pRole, pStatut;
    @FXML private TextField pBio, pTel;
    @FXML private PasswordField pAncienMdp, pNouveauMdp, pConfirmMdp;
    @FXML private Label pErrTel, pErrMdp, pMessageProfil;
    @FXML private ImageView photoProfile;

    @FXML private Button btnQuestionnaires;
    @FXML private Button btnQuestions;

    private AdminService adminService = new AdminService();
    private User adminConnecte;
    private ObservableList<User> tousLesUsers = FXCollections.observableArrayList();
    private Stage modalStage;
    private String currentPhotoPath = null;

    // INITIALISATION
    @FXML
    public void initialize() {
        instance = this;

        // Ne pas appeler configurerFiltres() car les valeurs sont déjà dans le FXML
        configurerTable();
        configurerTableDemandes();
        chargerUtilisateurs();

        // Retirer les trois VBox du StackPane
        contentArea.getChildren().removeAll(panneauGestion, panneauDemandes, panneauProfil);

        // Afficher le panneau Gestion par défaut
        afficherGestion();

        // Ajouter les listeners pour les filtres
        navRechercheField.textProperty().addListener((obs, o, n) -> appliquerFiltres());

        if (filtreRole != null) filtreRole.setOnAction(e -> appliquerFiltres());
        if (filtreStatut != null) filtreStatut.setOnAction(e -> appliquerFiltres());
        if (filtreActif != null) filtreActif.setOnAction(e -> appliquerFiltres());

        modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
    }

    /**
     * Méthode statique pour charger du contenu dans le contentArea de l'AdminDashboard
     * Utilisable depuis les contrôleurs de gestion
     */
    // ==================== FILTRES ET TRI ====================

    private void configurerFiltres() {
        // Filtre Rôle
        filtreRole = new ComboBox<>();
        filtreRole.setItems(FXCollections.observableArrayList(
                "Tous les rôles", "Admin", "Etudiant", "Psychologue", "Responsable Etudiant"));
        filtreRole.setValue("Tous les rôles");  // Valeur par défaut
        filtreRole.setStyle(filtreComboStyle());
        filtreRole.setOnAction(e -> appliquerFiltres());

        // Filtre Statut
        filtreStatut = new ComboBox<>();
        ObservableList<String> statuts = FXCollections.observableArrayList(
                "Tous les statuts", "actif", "en_attente", "bloqué", "inactif");
        filtreStatut.setItems(statuts);
        filtreStatut.setValue("Tous les statuts");  // Valeur par défaut
        filtreStatut.setStyle(filtreComboStyle());
        filtreStatut.setOnAction(e -> appliquerFiltres());

        // Filtre Actif/Inactif
        filtreActif = new ComboBox<>();
        filtreActif.setItems(FXCollections.observableArrayList(
                "Tous", "Actif", "Inactif"));
        filtreActif.setValue("Tous");  // Valeur par défaut
        filtreActif.setStyle(filtreComboStyle());
        filtreActif.setOnAction(e -> appliquerFiltres());
    }

    private String filtreComboStyle() {
        return "-fx-background-radius: 8; -fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 8; -fx-border-width: 1.5;" +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;" +
                "-fx-pref-width: 180; -fx-pref-height: 36;" +
                "-fx-padding: 0 10; -fx-background-color: white;";
    }

    private void appliquerFiltres() {
        String recherche = navRechercheField != null ? navRechercheField.getText().toLowerCase() : "";

        // Récupération des valeurs avec gestion des nulls
        String role = (filtreRole != null && filtreRole.getValue() != null)
                ? filtreRole.getValue() : "Tous les rôles";
        String statut = (filtreStatut != null && filtreStatut.getValue() != null)
                ? filtreStatut.getValue() : "Tous les statuts";
        String actif = (filtreActif != null && filtreActif.getValue() != null)
                ? filtreActif.getValue() : "Tous";

        // Filtrer les utilisateurs
        ObservableList<User> filtres = tousLesUsers.filtered(u -> {
            // Filtre recherche textuelle
            boolean matchRecherche = recherche.isEmpty()
                    || (u.getNom() != null && u.getNom().toLowerCase().contains(recherche))
                    || (u.getPrenom() != null && u.getPrenom().toLowerCase().contains(recherche))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(recherche));

            // Filtre rôle
            String userRole = u.getRole() != null ? u.getRole().toString().replace("_", " ") : "";
            boolean matchRole = role.equals("Tous les rôles") || userRole.equalsIgnoreCase(role);

            // Filtre statut
            String userStatut = u.getStatut() != null ? u.getStatut() : "";
            boolean matchStatut = statut.equals("Tous les statuts") || userStatut.equalsIgnoreCase(statut);

            // Filtre actif/inactif
            boolean matchActif = actif.equals("Tous")
                    || (actif.equals("Actif") && u.isActive())
                    || (actif.equals("Inactif") && !u.isActive());

            return matchRecherche && matchRole && matchStatut && matchActif;
        });

        tableUtilisateurs.setItems(filtres);

        // Mettre à jour le compteur
        if (labelNbResultats != null) {
            labelNbResultats.setText(filtres.size() + " / " + tousLesUsers.size() + " utilisateurs");
        }

        // Message de résultat
        if (filtres.isEmpty()) {
            afficherMsgGestion("⚠ Aucun utilisateur ne correspond aux filtres", "#F59E0B");
        } else {
            afficherMsgGestion("✓ " + filtres.size() + " utilisateur(s) trouvé(s)", "#16A34A");
        }
    }

    // Filtres rapides par rôle
    @FXML public void filtrerAdmins() {
        if (filtreRole != null) filtreRole.setValue("Admin");
        if (filtreStatut != null) filtreStatut.setValue("Tous les statuts");
        appliquerFiltres();
    }

    @FXML public void filtrerEtudiants() {
        if (filtreRole != null) filtreRole.setValue("Etudiant");
        if (filtreStatut != null) filtreStatut.setValue("Tous les statuts");
        appliquerFiltres();
    }

    @FXML public void filtrerPsychologues() {
        if (filtreRole != null) filtreRole.setValue("Psychologue");
        if (filtreStatut != null) filtreStatut.setValue("Tous les statuts");
        appliquerFiltres();
    }

    @FXML public void filtrerResponsables() {
        if (filtreRole != null) filtreRole.setValue("Responsable Etudiant");
        if (filtreStatut != null) filtreStatut.setValue("Tous les statuts");
        appliquerFiltres();
    }

    @FXML public void filtrerBloques() {
        if (filtreRole != null) filtreRole.setValue("Tous les rôles");
        if (filtreStatut != null) filtreStatut.setValue("bloqué");
        appliquerFiltres();
    }

    @FXML public void filtrerEnAttente() {
        if (filtreRole != null) filtreRole.setValue("Tous les rôles");
        if (filtreStatut != null) filtreStatut.setValue("en_attente");
        appliquerFiltres();
    }

    @FXML public void filtrerActifs() {
        if (filtreActif != null) filtreActif.setValue("Actif");
        appliquerFiltres();
    }

    @FXML public void filtrerInactifs() {
        if (filtreActif != null) filtreActif.setValue("Inactif");
        appliquerFiltres();
    }

    @FXML
    public void reinitialiserFiltres() {
        if (filtreRole != null) filtreRole.setValue("Tous les rôles");
        if (filtreStatut != null) filtreStatut.setValue("Tous les statuts");
        if (filtreActif != null) filtreActif.setValue("Tous");
        if (navRechercheField != null) navRechercheField.clear();
        appliquerFiltres();
    }

    // Tri des colonnes
    private void configurerTri() {
        // Tri par Nom
        colNom.setComparator((String s1, String s2) -> {
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s1.compareToIgnoreCase(s2);
        });

        // Tri par Prénom
        colPrenom.setComparator((String s1, String s2) -> {
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s1.compareToIgnoreCase(s2);
        });

        // Tri par Email
        colEmail.setComparator((String s1, String s2) -> {
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s1.compareToIgnoreCase(s2);
        });

        // Tri par Rôle
        colRole.setComparator((String s1, String s2) -> {
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s1.compareToIgnoreCase(s2);
        });

        // Tri par Statut
        colStatut.setComparator((String s1, String s2) -> {
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s1.compareToIgnoreCase(s2);
        });
    }
    private void styliserComboBox() {
        if (filtreRole != null) {
            filtreRole.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 5 10; -fx-font-size: 13px;");
        }
        if (filtreStatut != null) {
            filtreStatut.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 5 10; -fx-font-size: 13px;");
        }
        if (filtreActif != null) {
            filtreActif.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 5 10; -fx-font-size: 13px;");
        }
    }
    public static void loadContent(String fxmlPath) {
        if (instance != null) {
            try {
                Node page = FXMLLoader.load(instance.getClass().getResource(fxmlPath));
                instance.contentArea.getChildren().setAll(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Surcharge pour charger directement un Node déjà chargé
     */
    public static void loadContent(Node node) {
        if (instance != null) {
            instance.contentArea.getChildren().setAll(node);
        }
    }

    public void setUser(User user) {
        this.adminConnecte = user;

        // Vérifier que les éléments existent avant de les utiliser
        if (navNomAdmin != null) {
            navNomAdmin.setText(user.getPrenom() + " " + user.getNom());
        }

        if (pNomPrenom != null) {
            pNomPrenom.setText(user.getPrenom() + " " + user.getNom());
        }

        if (pEmail != null) {
            pEmail.setText(user.getEmail());
        }

        if (pRole != null) {
            pRole.setText(user.getRole().toString());
        }

        if (pStatut != null) {
            pStatut.setText(user.getStatut());
        }

        chargerPhotoProfile(user);
    }

    // ==================== NAVIGATION (affichage des panneaux) ====================

    @FXML
    public void afficherGestion() {
        // S'assurer que la VBox est visible et gérée
        panneauGestion.setVisible(true);
        panneauGestion.setManaged(true);
        contentArea.getChildren().setAll(panneauGestion);
        setActiveSidebarButton(btnGestionUsers);
        chargerUtilisateurs();
    }

    @FXML
    public void afficherDemandes() {
        panneauDemandes.setVisible(true);
        panneauDemandes.setManaged(true);
        contentArea.getChildren().setAll(panneauDemandes);
        setActiveSidebarButton(btnDemandes);
        chargerDemandes();
    }

    @FXML
    public void afficherProfil() {
        panneauProfil.setVisible(true);
        panneauProfil.setManaged(true);
        contentArea.getChildren().setAll(panneauProfil);
        setActiveSidebarButton(btnProfil);

        // Recharger les informations du profil avec vérifications
        if (adminConnecte != null) {
            if (pNomPrenom != null) {
                pNomPrenom.setText(adminConnecte.getPrenom() + " " + adminConnecte.getNom());
            }
            if (pEmail != null) {
                pEmail.setText(adminConnecte.getEmail());
            }
            if (pRole != null) {
                pRole.setText(adminConnecte.getRole().toString());
            }
            if (pStatut != null) {
                pStatut.setText(adminConnecte.getStatut());
            }
        }
    }

    @FXML
    public void showSeances() {
        loadPage("/org/example/views/CategorieMeditation.fxml");
        setActiveSidebarButton(btnSeances);
    }

    @FXML
    public void showStats() {
        loadPage("/org/example/views/Stats.fxml");
        setActiveSidebarButton(btnStats);
    }

    @FXML
    public void showEvenements() {
        loadPage("/evenement/GestionEvenement.fxml");
        setActiveSidebarButton(btnEvenements);
    }

    @FXML
    public void showParticipations() {
        loadPage("/participation/GestionParticipation.fxml");
        setActiveSidebarButton(btnParticipations);
    }

    @FXML
    public void showSponsors() {
        loadPage("/sponsor/GestionSponsor.fxml");
        setActiveSidebarButton(btnSponsors);
    }

    @FXML
    public void showFeedbacks() {
        loadPage("/feedback/FeedbacksAdmin.fxml");
        setActiveSidebarButton(btnFeedbacks);
    }

    @FXML
    public void showQuestionnaires() {
        loadPage("/fxml/QuestionnaireView.fxml");
        setActiveSidebarButton(btnQuestionnaires);
    }

    @FXML
    public void showQuestions() {
        loadPage("/fxml/QuestionView.fxml");
        setActiveSidebarButton(btnQuestions);
    }

    private void loadPage(String fxmlPath) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gestion du style des boutons de la sidebar
    private void setActiveSidebarButton(Button active) {
        Button[] allButtons = {btnGestionUsers, btnDemandes, btnProfil, btnStats, btnSeances, btnEvenements, btnParticipations, btnSponsors, btnFeedbacks, btnQuestionnaires, btnQuestions};
        for (Button btn : allButtons) {
            btn.setStyle(sidebarBtnStyle(false));
        }
        active.setStyle(sidebarBtnStyle(true));
    }

    private String sidebarBtnStyle(boolean actif) {
        if (actif) {
            return "-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 210; -fx-pref-height: 42; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 13;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #E9D5FF; -fx-pref-width: 210; -fx-pref-height: 42; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 13;";
        }
    }

    // ==================== GESTION UTILISATEURS ====================

    private void configurerTable() {
        // Configuration correcte des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Colonne Rôle avec badge coloré
        colRole.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRole() != null
                        ? cellData.getValue().getRole().toString().replace("_", " ") : ""));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                String bg = getRoleBadgeColor(item);
                badge.setStyle(
                        "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-font-family: 'Segoe UI';" +
                                "-fx-padding: 5 12; -fx-background-radius: 20;"
                );
                setGraphic(badge);
                setText(null);
                setStyle("-fx-padding: 0 16;");
            }
        });

        // Colonne Statut avec badge
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String color = switch (item.toLowerCase()) {
                    case "actif" -> "#10B981";
                    case "en_attente" -> "#F59E0B";
                    case "bloqué", "bloque" -> "#EF4444";
                    default -> "#6B7280";
                };
                badge.setStyle(
                        "-fx-background-color: " + color + "20;" +
                                "-fx-text-fill: " + color + ";" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-font-family: 'Segoe UI';" +
                                "-fx-padding: 5 12; -fx-background-radius: 20;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        // Colonne Actif avec cercle + texte
        colActif.setCellValueFactory(new PropertyValueFactory<>("active"));
        colActif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER);
                Circle dot = new Circle(5);
                dot.setFill(item ? Color.web("#10B981") : Color.web("#94A3B8"));
                Label text = new Label(item ? "Actif" : "Inactif");
                text.setStyle("-fx-font-size: 12px; -fx-font-weight: 500;" +
                        "-fx-text-fill: " + (item ? "#065F46" : "#64748B") + ";" +
                        "-fx-font-family: 'Segoe UI';");
                box.getChildren().addAll(dot, text);
                setGraphic(box);
                setText(null);
            }
        });

        // Colonne Actions avec boutons modernes
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier = createArgonButton("✏", "#4F46E5");
            private final Button btnSupprimer = createArgonButton("🗑", "#EF4444");
            private final Button btnBloquer = createArgonButton("🔒", "#F59E0B");
            private final Button btnDebloquer = createArgonButton("🔓", "#10B981");
            private final Button btnActiver = createArgonButton("✅", "#3B82F6");
            private final Button btnDesactiver = createArgonButton("⛔", "#6B7280");
            private final HBox container = new HBox(8, btnModifier, btnSupprimer, btnBloquer, btnDebloquer, btnActiver, btnDesactiver);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(0, 8, 0, 8));
                btnModifier.setOnAction(e -> ouvrirModalModification(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> supprimerUtilisateur(getTableView().getItems().get(getIndex())));
                btnBloquer.setOnAction(e -> bloquerUtilisateur(getTableView().getItems().get(getIndex())));
                btnDebloquer.setOnAction(e -> debloquerUtilisateur(getTableView().getItems().get(getIndex())));
                btnActiver.setOnAction(e -> activerUtilisateur(getTableView().getItems().get(getIndex())));
                btnDesactiver.setOnAction(e -> desactiverUtilisateur(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        // Lignes avec survol
        tableUtilisateurs.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #F1F5F9 transparent; -fx-border-width: 1;");
            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (!row.isEmpty()) {
                    row.setStyle(isHover
                            ? "-fx-background-color: #F8FAFC; -fx-border-color: transparent transparent #F1F5F9 transparent; -fx-border-width: 1;"
                            : "-fx-background-color: white; -fx-border-color: transparent transparent #F1F5F9 transparent; -fx-border-width: 1;");
                }
            });
            return row;
        });
    }

    // Ajoutez ces méthodes utilitaires
    private String getRoleBadgeColor(String role) {
        return switch (role.toUpperCase().trim()) {
            case "ADMIN" -> "linear-gradient(to right, #F5365C, #F56036)";
            case "PSYCHOLOGUE" -> "linear-gradient(to right, #11CDEF, #1171EF)";
            case "RESPONSABLE ETUDIANT" -> "linear-gradient(to right, #FB6340, #FBB140)";
            default -> "linear-gradient(to right, #2DCE89, #2DCECC)";
        };
    }

    private Button createArgonButton(String icon, String color) {
        Button btn = new Button(icon);
        btn.setStyle(
                "-fx-background-color: " + color + "20; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 13px; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 6 12; " +
                        "-fx-min-width: 36; " +
                        "-fx-min-height: 32;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 6 12; " +
                        "-fx-min-width: 36; " +
                        "-fx-min-height: 32;" +
                        "-fx-effect: dropshadow(gaussian, " + color + "40, 8, 0, 0, 3);"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + "20; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-size: 13px; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 6 12; " +
                        "-fx-min-width: 36; " +
                        "-fx-min-height: 32;"
        ));
        return btn;
    }

    private Button createIconButton(String icon, String color) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 8; -fx-cursor: hand; -fx-background-radius: 4;");
        return btn;
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = adminService.afficher();
            tousLesUsers.setAll(users);
            tableUtilisateurs.setItems(tousLesUsers);

            // Configurer le tri après chargement
            configurerTri();

            // Mettre à jour le compteur
            if (labelNbResultats != null) {
                labelNbResultats.setText(users.size() + " / " + users.size() + " utilisateurs");
            }

            // Appliquer les filtres une seule fois
            appliquerFiltres();

        } catch (SQLException e) {
            e.printStackTrace();
            afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
            if (labelNbResultats != null) {
                labelNbResultats.setText("0 utilisateurs");
            }
        }
    }

    private void filtrer(String texte) {
        if (texte == null || texte.isEmpty()) {
            tableUtilisateurs.setItems(tousLesUsers);
            return;
        }
        String r = texte.toLowerCase();
        tableUtilisateurs.setItems(tousLesUsers.filtered(u ->
                u.getNom().toLowerCase().contains(r) ||
                        u.getPrenom().toLowerCase().contains(r) ||
                        u.getEmail().toLowerCase().contains(r)
        ));
    }

    @FXML
    public void ouvrirModalAjout() {
        ouvrirModal(null);
    }

    private void ouvrirModalModification(User user) {
        ouvrirModal(user);
    }
    private void ouvrirModal(User user) {
        // Dialog moderne style Argon
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Ajouter un utilisateur" : "Modifier l'utilisateur");
        dialog.setHeaderText(null);

        // Style du dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20;");
        dialogPane.getStylesheets().add(getClass().getResource("/css/modern.css") != null ?
                getClass().getResource("/css/modern.css").toExternalForm() : "");

        // Contenu
        VBox content = new VBox(16);
        content.setPadding(new Insets(10, 20, 10, 20));

        // Titre
        Label titleLabel = new Label(user == null ? "➕ Nouvel utilisateur" : "✏ Modification");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-family: 'Segoe UI';");

        // Champs avec style moderne
        TextField nomField = createModernTextField("Nom complet");
        TextField prenomField = createModernTextField("Prénom");
        TextField emailField = createModernTextField("Adresse email");
        TextField cinField = createModernTextField("CIN (8 chiffres)");
        PasswordField passwordField = createModernPasswordField("Mot de passe");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Etudiant", "Psychologue", "Responsable Etudiant"));
        roleCombo.setValue("Etudiant");
        roleCombo.setStyle(modernComboStyle());

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-font-size: 12px; -fx-font-family: 'Segoe UI';");

        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            cinField.setText(user.getCin());
            roleCombo.setValue(user.getRole().toString().replace("_", " "));
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        }

        // Organisation des champs en grille
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 10, 0));

        grid.add(createLabeledField("Nom complet", nomField), 0, 0);
        grid.add(createLabeledField("Prénom", prenomField), 1, 0);
        grid.add(createLabeledField("Email", emailField), 0, 1);
        grid.add(createLabeledField("CIN", cinField), 1, 1);
        grid.add(createLabeledField("Rôle", roleCombo), 0, 2);
        if (user == null) {
            grid.add(createLabeledField("Mot de passe", passwordField), 1, 2);
        }

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        content.getChildren().addAll(titleLabel, grid, messageLabel);

        // Boutons
        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        dialog.getDialogPane().setContent(content);

        // Styling des boutons
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #4F46E5, #7C3AED); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8;");
        Node cancelButton = dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");

        // Conversion du résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new User(nomField.getText().trim(), prenomField.getText().trim(),
                        emailField.getText().trim(), passwordField.getText(),
                        cinField.getText().trim(),
                        org.example.enums.Role.valueOf(roleCombo.getValue().replace(" ", "_").toUpperCase()),
                        "actif");
            }
            return null;
        });

        // Gestion de la sauvegarde
        dialog.showAndWait().ifPresent(result -> {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String cin = cinField.getText().trim();
            String role = roleCombo.getValue();
            String password = passwordField.getText();

            boolean valide = true;

            if (!ValidationUtils.isNomValide(nom)) { showMessage(messageLabel, "Nom invalide", "error"); valide = false; }
            else if (!ValidationUtils.isNomValide(prenom)) { showMessage(messageLabel, "Prénom invalide", "error"); valide = false; }
            else if (!ValidationUtils.isEmailValide(email)) { showMessage(messageLabel, ValidationUtils.messageEmail(), "error"); valide = false; }
            else if (!ValidationUtils.isCinValide(cin)) { showMessage(messageLabel, ValidationUtils.messageCin(), "error"); valide = false; }
            else if (user == null && !ValidationUtils.isPasswordValide(password)) { showMessage(messageLabel, ValidationUtils.messagePassword(), "error"); valide = false; }

            if (valide) {
                try {
                    if (user == null) {
                        User u = new User(nom, prenom, email, password, cin,
                                org.example.enums.Role.valueOf(role.replace(" ", "_").toUpperCase()), "actif");
                        u.setActive(true);
                        u.setVerified(true);
                        adminService.ajouter(u);
                        showMessage(messageLabel, "✓ Utilisateur ajouté avec succès !", "success");
                    } else {
                        user.setNom(nom);
                        user.setPrenom(prenom);
                        user.setEmail(email);
                        user.setCin(cin);
                        adminService.modifier(user);
                        showMessage(messageLabel, "✓ Utilisateur modifié avec succès !", "success");
                    }
                    chargerUtilisateurs();

                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            Stage stage = (Stage) messageLabel.getScene().getWindow();
                            if (stage != null) stage.close();
                        });
                    }).start();
                } catch (SQLException ex) {
                    showMessage(messageLabel, "Erreur : " + ex.getMessage(), "error");
                }
            }
        });
    }

    private TextField createModernTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-padding: 12 14; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-family: 'Segoe UI';"
        );
        field.focusedProperty().addListener((obs, old, newVal) -> {
            field.setStyle(newVal
                    ? "-fx-padding: 12 14; -fx-background-radius: 10; -fx-border-color: #4F46E5; -fx-border-radius: 10; -fx-border-width: 2; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';"
                    : "-fx-padding: 12 14; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        });
        return field;
    }

    private PasswordField createModernPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-padding: 12 14; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 1.5; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-family: 'Segoe UI';"
        );
        field.focusedProperty().addListener((obs, old, newVal) -> {
            field.setStyle(newVal
                    ? "-fx-padding: 12 14; -fx-background-radius: 10; -fx-border-color: #4F46E5; -fx-border-radius: 10; -fx-border-width: 2; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';"
                    : "-fx-padding: 12 14; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        });
        return field;
    }

    private String modernComboStyle() {
        return "-fx-padding: 8 12; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';";
    }

    private VBox createLabeledField(String labelText, javafx.scene.Node field) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-family: 'Segoe UI';");
        box.getChildren().addAll(label, field);
        return box;
    }

    private void showMessage(Label label, String message, String type) {
        if ("error".equals(type)) {
            label.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-family: 'Segoe UI'; -fx-padding: 8 12; -fx-background-color: #FEF2F2; -fx-background-radius: 8;");
            label.setText("⚠ " + message);
        } else {
            label.setStyle("-fx-text-fill: #065F46; -fx-font-size: 12px; -fx-font-family: 'Segoe UI'; -fx-padding: 8 12; -fx-background-color: #ECFDF5; -fx-background-radius: 8;");
            label.setText("✓ " + message);
        }
    }



    private void supprimerUtilisateur(User user) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer " + user.getPrenom() + " " + user.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        c.setTitle("Confirmation");
        c.setHeaderText(null);
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                supprimerProfilUser(user.getUserId());
                adminService.supprimer(user.getUserId());
                afficherMsgGestion("✓ Utilisateur supprimé", "#16A34A");
                chargerUtilisateurs();
            } catch (SQLException e) {
                afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
            }
        }
    }

    private void bloquerUtilisateur(User user) {
        try {
            adminService.bloquer(user.getUserId());
            afficherMsgGestion("✓ Compte bloqué : " + user.getNom(), "#D97706");
            chargerUtilisateurs();
        } catch (SQLException e) {
            afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
        }
    }

    private void debloquerUtilisateur(User user) {
        try {
            adminService.debloquer(user.getUserId());
            afficherMsgGestion("✓ Compte débloqué : " + user.getNom(), "#16A34A");
            chargerUtilisateurs();
        } catch (SQLException e) {
            afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
        }
    }

    private void activerUtilisateur(User user) {
        try {
            adminService.debloquer(user.getUserId());
            afficherMsgGestion("✓ Compte activé : " + user.getNom(), "#16A34A");
            chargerUtilisateurs();
        } catch (SQLException e) {
            afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
        }
    }

    private void desactiverUtilisateur(User user) {
        try {
            adminService.bloquer(user.getUserId());
            afficherMsgGestion("✓ Compte désactivé : " + user.getNom(), "#D97706");
            chargerUtilisateurs();
        } catch (SQLException e) {
            afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
        }
    }

    // ==================== DEMANDES EN ATTENTE ====================

    private void configurerTableDemandes() {
        dColNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        dColPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        dColEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        dColRole.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRole() != null
                        ? cellData.getValue().getRole().toString().replace("_", " ") : ""));
        dColRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String bg = getRoleBadgeColor(item);
                badge.setStyle(
                        "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-font-family: 'Segoe UI';" +
                                "-fx-padding: 5 12; -fx-background-radius: 20;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        tableDemandes.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #F1F5F9 transparent; -fx-border-width: 1;");
            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (!row.isEmpty()) {
                    row.setStyle(isHover
                            ? "-fx-background-color: #FFF7ED; -fx-border-color: transparent transparent #F1F5F9 transparent; -fx-border-width: 1;"
                            : "-fx-background-color: white; -fx-border-color: transparent transparent #F1F5F9 transparent; -fx-border-width: 1;");
                }
            });
            return row;
        });
    }

    private void chargerDemandes() {
        try {
            List<User> demandes = adminService.afficherDemandesEnAttente();
            tableDemandes.setItems(FXCollections.observableArrayList(demandes));
            messageDemandes.setText("✓ " + demandes.size() + " demande(s) en attente");
            messageDemandes.setStyle("-fx-text-fill: #7C3AED;");
        } catch (SQLException e) {
            messageDemandes.setText("Erreur : " + e.getMessage());
            messageDemandes.setStyle("-fx-text-fill: #DC2626;");
        }
    }

    @FXML
    public void accepterDemande() {
        User sel = tableDemandes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une demande !", ButtonType.OK).showAndWait();
            return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Accepter " + sel.getPrenom() + " " + sel.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                adminService.accepterDemande(sel.getUserId());
                messageDemandes.setStyle("-fx-text-fill: #16A34A;");
                messageDemandes.setText("✓ Demande acceptée !");
                chargerDemandes();
                chargerUtilisateurs();
            } catch (SQLException e) {
                messageDemandes.setStyle("-fx-text-fill: #DC2626;");
                messageDemandes.setText("Erreur : " + e.getMessage());
            }
        }
    }

    @FXML
    public void refuserDemande() {
        User sel = tableDemandes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une demande !", ButtonType.OK).showAndWait();
            return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Refuser " + sel.getPrenom() + " " + sel.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                adminService.refuserDemande(sel.getUserId());
                messageDemandes.setStyle("-fx-text-fill: #D97706;");
                messageDemandes.setText("✓ Demande refusée.");
                chargerDemandes();
            } catch (SQLException e) {
                messageDemandes.setStyle("-fx-text-fill: #DC2626;");
                messageDemandes.setText("Erreur : " + e.getMessage());
            }
        }
    }

    // ==================== PROFIL ADMIN ====================

    @FXML
    public void choisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File fichier = fc.showOpenDialog(photoProfile.getScene().getWindow());

        if (fichier != null) {
            try {
                String projectPath = System.getProperty("user.dir");
                java.nio.file.Path uploadDir = java.nio.file.Paths.get(projectPath, "uploads");

                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }

                String extension = ".jpg";
                int dotIndex = fichier.getName().lastIndexOf(".");
                if (dotIndex > 0) extension = fichier.getName().substring(dotIndex);

                String nomFichier = fichier.getName();

                java.nio.file.Path destination = uploadDir.resolve(nomFichier);
                java.nio.file.Files.copy(fichier.toPath(), destination,
                        StandardCopyOption.REPLACE_EXISTING);

                currentPhotoPath = nomFichier;
                Image img = new Image(fichier.toURI().toString());
                photoProfile.setImage(img);
                navPhotoAdmin.setImage(img);

                pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
                pMessageProfil.setText("✓ Photo sélectionnée, cliquez sur Sauvegarder");

            } catch (IOException e) {
                pMessageProfil.setStyle("-fx-text-fill: #DC2626;");
                pMessageProfil.setText("Erreur photo : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void sauvegarderProfil() {
        pErrTel.setText("");
        pMessageProfil.setText("");

        String tel = pTel.getText().trim();
        String bio = pBio.getText().trim();

        if (!tel.isEmpty() && !ValidationUtils.isTelephoneValide(tel)) {
            pErrTel.setText(ValidationUtils.messageTelephone());
            return;
        }

        try {
            var conn = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
            java.sql.PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM profil WHERE user_id = ?");
            check.setInt(1, adminConnecte.getUserId());
            java.sql.ResultSet rs = check.executeQuery();
            rs.next();
            boolean existe = rs.getInt(1) > 0;

            if (existe) {
                String sql = "UPDATE profil SET bio=?, tel=?, photo=?, updated_at=NOW() WHERE user_id=?";
                java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, bio);
                ps.setString(2, tel);
                ps.setString(3, currentPhotoPath);
                ps.setInt(4, adminConnecte.getUserId());
                ps.executeUpdate();
            } else {
                String sql = "INSERT INTO profil (user_id, bio, tel, photo, updated_at) VALUES (?,?,?,?,NOW())";
                java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, adminConnecte.getUserId());
                ps.setString(2, bio);
                ps.setString(3, tel);
                ps.setString(4, currentPhotoPath);
                ps.executeUpdate();
            }

            pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
            pMessageProfil.setText("✓ Profil mis à jour avec succès !");

        } catch (java.sql.SQLException e) {
            pMessageProfil.setStyle("-fx-text-fill: #DC2626;");
            pMessageProfil.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void changerMotDePasse() {
        pErrMdp.setText(""); pMessageProfil.setText("");
        String ancien = pAncienMdp.getText();
        String nouveau = pNouveauMdp.getText();
        String confirm = pConfirmMdp.getText();

        if (!ValidationUtils.isNonVide(ancien) || !ValidationUtils.isNonVide(nouveau)) {
            pErrMdp.setText("Tous les champs sont obligatoires");
            return;
        }
        if (!ValidationUtils.isPasswordValide(nouveau)) {
            pErrMdp.setText(ValidationUtils.messagePassword());
            return;
        }
        if (!ValidationUtils.isPasswordConfirme(nouveau, confirm)) {
            pErrMdp.setText(ValidationUtils.messagePasswordConfirm());
            return;
        }

        try {
            adminService.changerMotDePasse(adminConnecte.getUserId(), ancien, nouveau);
            pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
            pMessageProfil.setText("✓ Mot de passe modifié !");
            pAncienMdp.clear(); pNouveauMdp.clear(); pConfirmMdp.clear();
        } catch (SQLException e) {
            pErrMdp.setText(e.getMessage());
        }
    }

    // ==================== DÉCONNEXION ====================

    @FXML
    public void seDeconnecter() {
        try {
            Stage stage = (Stage) navNomAdmin.getScene().getWindow();
            stage.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("UniMind");
            loginStage.setScene(new Scene(loader.load(), 500, 420));
            loginStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==================== UTILITAIRES ====================

    private void chargerPhotoProfile(User user) {
        try {
            var conn = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT photo FROM profil WHERE user_id = ?");
            ps.setInt(1, user.getUserId());
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String photoPath = rs.getString("photo");
                if (photoPath != null && !photoPath.isEmpty()) {
                    String projectPath = System.getProperty("user.dir");
                    File f = new File(projectPath + "/uploads/" + photoPath);
                    if (f.exists()) {
                        Image img = new Image(f.toURI().toString());
                        photoProfile.setImage(img);
                        navPhotoAdmin.setImage(img);
                        currentPhotoPath = photoPath;
                        return;
                    }
                }
            }
            photoProfile.setImage(null);
            navPhotoAdmin.setImage(null);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    private void supprimerProfilUser(int userId) {
        try {
            org.example.utils.MyDataBase_Unimind.getInstance().getConnection()
                    .prepareStatement("DELETE FROM profil WHERE user_id = " + userId)
                    .executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void afficherMsgGestion(String msg, String couleur) {
        messageGestion.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 12; -fx-padding: 5 0 0 0;");
        messageGestion.setText(msg);
    }
}
