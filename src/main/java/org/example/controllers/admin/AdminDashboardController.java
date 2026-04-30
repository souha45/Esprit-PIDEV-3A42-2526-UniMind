package org.example.controllers.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.entities.*;
import org.example.services.AdminService;
import org.example.utils.ValidationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class AdminDashboardController {

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
    @FXML private Button btnQuestionnaires;
    @FXML private Button btnQuestions;
    // ✅ Nouveau bouton
    @FXML private Button btnStatQuestionnaire;

    // CONTENU PRINCIPAL
    @FXML private StackPane contentArea;

    // PANNEAUX FIXES
    @FXML private VBox panneauGestion;
    @FXML private VBox panneauDemandes;
    @FXML private VBox panneauProfil;

    // GESTION USERS
    @FXML private TableView<User> tableUtilisateurs;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, Boolean> colActif;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private Label messageGestion;

    // DEMANDES
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
    // Dashboard
    @FXML private VBox panneauDashboard;
    @FXML private Button btnDashboard;
    @FXML private Label statTotalUsers, statActifs, statEnAttente, statBloques;
    @FXML private Canvas canvasRoles, canvasStatuts, canvasEvolution;
    @FXML private TableView<StatItem> tableStats;
    @FXML private TableColumn<StatItem, String> colCategorie;
    @FXML private TableColumn<StatItem, Long> colNombre;
    @FXML private TableColumn<StatItem, String> colPourcentage;
    @FXML private HBox roleLegend;
    private AdminService adminService = new AdminService();
    private User adminConnecte;
    private ObservableList<User> tousLesUsers = FXCollections.observableArrayList();
    private Stage modalStage;
    private String currentPhotoPath = null;
    // Ajoutez cette variable avec les autres déclarations
    private final String[] couleursStatuts = {"#10B981", "#F59E0B", "#EF4444", "#9CA3AF"};
    private final String[] couleursRoles = {"#7C3AED", "#3B82F6", "#10B981", "#F59E0B", "#EF4444"};

    // ══════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════

    @FXML
    public void initialize() {
        instance = this;

        configurerTable();
        configurerTableDemandes();

        // Configurer les colonnes de la table des stats
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("category"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("count"));
        colPourcentage.setCellValueFactory(new PropertyValueFactory<>("percentage"));

        chargerUtilisateurs();

        contentArea.getChildren().removeAll(panneauGestion, panneauDemandes, panneauProfil, panneauDashboard);
        afficherGestion();

        navRechercheField.textProperty().addListener((obs, o, n) -> appliquerFiltres());

        if (filtreRole != null) filtreRole.setOnAction(e -> appliquerFiltres());
        if (filtreStatut != null) filtreStatut.setOnAction(e -> appliquerFiltres());
        if (filtreActif != null) filtreActif.setOnAction(e -> appliquerFiltres());

        configurerClicsGraphiques();
        styliserElementsCliquables();

        modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
    }

    /**
     * Méthode statique pour charger du contenu dans le contentArea de l'AdminDashboard
     * Utilisable depuis les contrôleurs de gestion
     */
    // ==================== FILTRES ET TRI ====================
// Classe interne pour les stat
    public static class StatItem {
        private final String category;
        private final long count;
        private final String percentage;

        public StatItem(String category, long count, String percentage) {
            this.category = category;
            this.count = count;
            this.percentage = percentage;
        }

        public String getCategory() { return category; }
        public long getCount() { return count; }
        public String getPercentage() { return percentage; }
    }
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

    // ══════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════

    @FXML
    public void afficherGestion() {
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

    // ✅ NOUVEAU — Statistiques Questionnaires
    @FXML
    public void showStatQuestionnaire() {
        loadPage("/fxml/StatistiquesView.fxml");
        setActiveSidebarButton(btnStatQuestionnaire);
    }

    private void loadPage(String fxmlPath) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveSidebarButton(Button active) {
        Button[] allButtons = {
                btnGestionUsers, btnDemandes, btnProfil, btnStats,
                btnSeances, btnEvenements, btnParticipations, btnSponsors,
                btnFeedbacks, btnQuestionnaires, btnQuestions, btnStatQuestionnaire
        };
        for (Button btn : allButtons) {
            if (btn != null) btn.setStyle(sidebarBtnStyle(false));
        }
        if (active != null) active.setStyle(sidebarBtnStyle(true));
    }

    private String sidebarBtnStyle(boolean actif) {
        if (actif) {
            return "-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-pref-width: 210; -fx-pref-height: 42; -fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 13;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #E9D5FF; " +
                    "-fx-pref-width: 210; -fx-pref-height: 42; -fx-alignment: CENTER_LEFT; " +
                    "-fx-padding: 0 0 0 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 13;";
        }
    }

    // ══════════════════════════════════════════
    //  GESTION UTILISATEURS
    // ══════════════════════════════════════════

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
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 11; -fx-padding: 4 8; -fx-cursor: hand; -fx-background-radius: 4;");
        return btn;
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = adminService.afficher();
            tousLesUsers.setAll(users);
            tableUtilisateurs.setItems(tousLesUsers);
            configurerTri();

            if (labelNbResultats != null) {
                labelNbResultats.setText(users.size() + " / " + users.size() + " utilisateurs");
            }

            appliquerFiltres();
            mettreAJourDashboard(); // Ajout pour mettre à jour le dashboard

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
        roleCombo.setItems(FXCollections.observableArrayList(
                "Admin", "Etudiant", "Psychologue", "Responsable Etudiant"));
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
            boolean valide  = true;

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
                                org.example.enums.Role.valueOf(
                                        role.replace(" ", "_").toUpperCase()), "actif");
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
                "Supprimer " + user.getPrenom() + " " + user.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
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
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    private void debloquerUtilisateur(User user) {
        try {
            adminService.debloquer(user.getUserId());
            afficherMsgGestion("✓ Compte débloqué : " + user.getNom(), "#16A34A");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    private void activerUtilisateur(User user) {
        try {
            adminService.debloquer(user.getUserId());
            afficherMsgGestion("✓ Compte activé : " + user.getNom(), "#16A34A");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    private void desactiverUtilisateur(User user) {
        try {
            adminService.bloquer(user.getUserId());
            afficherMsgGestion("✓ Compte désactivé : " + user.getNom(), "#D97706");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    // ══════════════════════════════════════════
    //  DEMANDES EN ATTENTE
    // ══════════════════════════════════════════

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
                "Accepter " + sel.getPrenom() + " " + sel.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
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
                "Refuser " + sel.getPrenom() + " " + sel.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
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

    // ══════════════════════════════════════════
    //  PROFIL ADMIN
    // ══════════════════════════════════════════

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

                if (!java.nio.file.Files.exists(uploadDir))
                    java.nio.file.Files.createDirectories(uploadDir);

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
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE profil SET bio=?, tel=?, photo=?, updated_at=NOW() WHERE user_id=?");
                ps.setString(1, bio);
                ps.setString(2, tel);
                ps.setString(3, currentPhotoPath);
                ps.setInt(4, adminConnecte.getUserId());
                ps.executeUpdate();
            } else {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO profil (user_id, bio, tel, photo, updated_at) VALUES (?,?,?,?,NOW())");
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
        }
    }

    @FXML
    public void changerMotDePasse() {
        pErrMdp.setText(""); pMessageProfil.setText("");
        String ancien  = pAncienMdp.getText();
        String nouveau = pNouveauMdp.getText();
        String confirm = pConfirmMdp.getText();

        if (!ValidationUtils.isNonVide(ancien) || !ValidationUtils.isNonVide(nouveau))
        { pErrMdp.setText("Tous les champs sont obligatoires"); return; }
        if (!ValidationUtils.isPasswordValide(nouveau))
        { pErrMdp.setText(ValidationUtils.messagePassword()); return; }
        if (!ValidationUtils.isPasswordConfirme(nouveau, confirm))
        { pErrMdp.setText(ValidationUtils.messagePasswordConfirm()); return; }

        try {
            adminService.changerMotDePasse(adminConnecte.getUserId(), ancien, nouveau);
            pMessageProfil.setStyle("-fx-text-fill: #16A34A;");
            pMessageProfil.setText("✓ Mot de passe modifié !");
            pAncienMdp.clear(); pNouveauMdp.clear(); pConfirmMdp.clear();
        } catch (SQLException e) { pErrMdp.setText(e.getMessage()); }
    }

    // ══════════════════════════════════════════
    //  DÉCONNEXION
    // ══════════════════════════════════════════

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

    // ══════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════

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
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
    }

    private void supprimerProfilUser(int userId) {
        try {
            org.example.utils.MyDataBase_Unimind.getInstance().getConnection()
                    .prepareStatement("DELETE FROM profil WHERE user_id = " + userId)
                    .executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void afficherMsgGestion(String msg, String couleur) {
        messageGestion.setStyle("-fx-text-fill: " + couleur +
                "; -fx-font-size: 12; -fx-padding: 5 0 0 0;");
        messageGestion.setText(msg);
    }
    @FXML
    public void afficherDashboard() {
        panneauDashboard.setVisible(true);
        panneauDashboard.setManaged(true);
        contentArea.getChildren().setAll(panneauDashboard);
        setActiveSidebarButton(btnDashboard);
        mettreAJourDashboard();
    }

    private void mettreAJourDashboard() {
        if (tousLesUsers.isEmpty()) {
            System.out.println("⚠ Dashboard: Aucun utilisateur à afficher");
            return;
        }

        long total = tousLesUsers.size();
        long actifs = tousLesUsers.stream().filter(u -> "actif".equalsIgnoreCase(u.getStatut())).count();
        long attente = tousLesUsers.stream().filter(u -> "en_attente".equalsIgnoreCase(u.getStatut())).count();
        long bloques = tousLesUsers.stream().filter(u -> "bloqué".equalsIgnoreCase(u.getStatut())
                || "bloque".equalsIgnoreCase(u.getStatut())).count();

        statTotalUsers.setText(String.valueOf(total));
        statActifs.setText(String.valueOf(actifs));
        statEnAttente.setText(String.valueOf(attente));
        statBloques.setText(String.valueOf(bloques));

        dessinerGraphiqueRoles();
        dessinerGraphiqueStatuts();
        dessinerGraphiqueEvolution();
        chargerTableStats(); // Appel après les graphiques
    }

    private void dessinerGraphiqueRoles() {
        GraphicsContext gc = canvasRoles.getGraphicsContext2D();
        double w = canvasRoles.getWidth();
        double h = canvasRoles.getHeight();
        gc.clearRect(0, 0, w, h);

        // Stocker les données pour les clics
        dernierRoleData = tousLesUsers.stream().collect(Collectors.groupingBy(
                u -> u.getRole() != null ? u.getRole().toString().replace("_", " ") : "Inconnu",
                Collectors.counting()));

        String[] labels = dernierRoleData.keySet().toArray(new String[0]);
        long[] values = dernierRoleData.values().stream().mapToLong(Long::longValue).toArray();
        long total = Arrays.stream(values).sum();

        double cx = w / 2, cy = h / 2, r = Math.min(w, h) * 0.35;
        double startAngle = -90;

        for (int i = 0; i < values.length; i++) {
            final int index = i; // Variable finale pour le lambda
            double arc = 360.0 * values[i] / total;
            gc.setFill(Color.web(couleursRoles[i % couleursRoles.length]));
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, startAngle, arc, ArcType.ROUND);

            // Ajouter une étiquette avec pourcentage
            double midAngle = Math.toRadians(startAngle + arc / 2);
            double labelX = cx + (r + 25) * Math.cos(midAngle);
            double labelY = cy + (r + 25) * Math.sin(midAngle);
            gc.setFill(Color.web("#1E293B"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            double pct = values[i] * 100.0 / total;
            gc.fillText(String.format("%.1f%%", pct), labelX - 15, labelY);

            startAngle += arc;
        }

        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(cx - r * 0.55, cy - r * 0.55, r * 1.1, r * 1.1);

        gc.setFill(Color.web("#1E293B"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        gc.fillText(String.valueOf(total), cx - 12, cy + 6);
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText("utilisateurs", cx - 20, cy + 22);

        roleLegend.getChildren().clear();
        for (int i = 0; i < labels.length; i++) {
            final int index = i; // Variable finale pour le lambda
            HBox item = new HBox(6);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-cursor: hand;");
            item.setOnMouseClicked(e -> {
                if (filtreRole != null) {
                    filtreRole.setValue(labels[index]);
                    appliquerFiltres();
                    afficherGestion();
                    showMessageDialog("🎯 Filtre par rôle", "Affichage des utilisateurs avec le rôle: " + labels[index]);
                }
            });
            Circle color = new Circle(6, Color.web(couleursRoles[i % couleursRoles.length]));
            Label text = new Label(labels[i] + " (" + values[i] + ")");
            text.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            item.getChildren().addAll(color, text);
            roleLegend.getChildren().add(item);
        }
    }
    private void styliserElementsCliquables() {
        // Curseur main pour les cartes stats
        statTotalUsers.setStyle("-fx-cursor: hand; -fx-text-fill: #1E293B; -fx-font-size: 28px; -fx-font-weight: bold;");
        statActifs.setStyle("-fx-cursor: hand; -fx-text-fill: #059669; -fx-font-size: 28px; -fx-font-weight: bold;");
        statEnAttente.setStyle("-fx-cursor: hand; -fx-text-fill: #D97706; -fx-font-size: 28px; -fx-font-weight: bold;");
        statBloques.setStyle("-fx-cursor: hand; -fx-text-fill: #DC2626; -fx-font-size: 28px; -fx-font-weight: bold;");

        // Tooltips pour indiquer que c'est cliquable
        Tooltip.install(statTotalUsers, new Tooltip("Cliquez pour voir tous les utilisateurs"));
        Tooltip.install(statActifs, new Tooltip("Cliquez pour filtrer les utilisateurs actifs"));
        Tooltip.install(statEnAttente, new Tooltip("Cliquez pour voir les demandes en attente"));
        Tooltip.install(statBloques, new Tooltip("Cliquez pour voir les comptes bloqués"));
        Tooltip.install(canvasRoles, new Tooltip("Cliquez sur une portion pour filtrer par rôle"));
        Tooltip.install(canvasStatuts, new Tooltip("Cliquez sur une barre pour filtrer par statut"));
    }
    private void dessinerGraphiqueStatuts() {
        GraphicsContext gc = canvasStatuts.getGraphicsContext2D();
        double w = canvasStatuts.getWidth();
        double h = canvasStatuts.getHeight();
        gc.clearRect(0, 0, w, h);

        // Stocker les données pour les clics
        dernierStatutData = new LinkedHashMap<>();
        dernierStatutData.put("Actif", tousLesUsers.stream().filter(u -> "actif".equalsIgnoreCase(u.getStatut())).count());
        dernierStatutData.put("En attente", tousLesUsers.stream().filter(u -> "en_attente".equalsIgnoreCase(u.getStatut())).count());
        dernierStatutData.put("Bloqué", tousLesUsers.stream().filter(u -> "bloqué".equalsIgnoreCase(u.getStatut())).count());
        dernierStatutData.put("Inactif", tousLesUsers.stream().filter(u -> "inactif".equalsIgnoreCase(u.getStatut())).count());

        // Copier les données dans des tableaux finaux pour les lambda
        String[] noms = dernierStatutData.keySet().toArray(new String[0]);
        long[] vals = dernierStatutData.values().stream().mapToLong(Long::longValue).toArray();
        long max = Math.max(1, Arrays.stream(vals).max().orElse(1));
        long total = Arrays.stream(vals).sum();

        // Créer des copies finales pour les utiliser dans les lambdas
        final String[] finalNoms = noms;
        final long[] finalVals = vals;
        final long finalTotal = total;

        double padL = 50, padR = 20, padT = 20, padB = 40;
        double barW = (w - padL - padR) / (vals.length * 1.6);
        double chartH = h - padT - padB;

        // Grille
        gc.setStroke(Color.web("#E2E8F0"));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH * i / 4;
            gc.strokeLine(padL, y, w - padR, y);
            gc.setFill(Color.web("#94A3B8"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.fillText(String.valueOf((int)(max * (4 - i) / 4)), 4, y + 4);
        }

        // Barres avec effet cliquable
        for (int i = 0; i < vals.length; i++) {
            final int index = i; // Variable finale pour le lambda
            double barH = chartH * vals[i] / max;
            double bx = padL + i * (barW * 1.6) + barW * 0.3;
            double by = padT + chartH - barH;

            // Stocker les valeurs pour la zone cliquable
            String statutValue = switch (finalNoms[index]) {
                case "Actif" -> "actif";
                case "En attente" -> "en_attente";
                case "Bloqué" -> "bloqué";
                default -> "inactif";
            };
            final String finalStatutValue = statutValue;
            final String finalNom = finalNoms[index];

            // Zone cliquable
            Rectangle barZone = new Rectangle(bx, by, barW, barH);
            barZone.setFill(Color.TRANSPARENT);
            barZone.setOnMouseClicked(e -> {
                if (filtreStatut != null) {
                    filtreStatut.setValue(finalStatutValue);
                    appliquerFiltres();
                    afficherGestion();
                    showMessageDialog("📊 Filtre par statut", "Affichage des utilisateurs avec le statut: " + finalNom);
                }
            });

            gc.setFill(Color.web(couleursStatuts[i % couleursStatuts.length]));
            gc.fillRoundRect(bx, by, barW, barH, 6, 6);

            // Valeur et pourcentage
            double pct = vals[i] * 100.0 / finalTotal;
            gc.setFill(Color.web("#1E293B"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.fillText(vals[i] + " (" + String.format("%.1f", pct) + "%)", bx + barW / 2 - 20, by - 5);

            // Label
            gc.setFill(Color.web("#64748B"));
            gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText(finalNoms[index], bx + barW / 2 - 15, padT + chartH + 18);
        }
    }

    private void dessinerGraphiqueEvolution() {
        GraphicsContext gc = canvasEvolution.getGraphicsContext2D();
        double w = canvasEvolution.getWidth();
        double h = canvasEvolution.getHeight();
        gc.clearRect(0, 0, w, h);

        // Récupérer les inscriptions des 7 derniers jours
        List<Long> evolution = getInscriptionsParJour();

        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        double max = Math.max(1, evolution.stream().mapToLong(Long::longValue).max().orElse(1));

        double padL = 50, padR = 30, padT = 20, padB = 40;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;

        // Grille
        gc.setStroke(Color.web("#E2E8F0"));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH * i / 4;
            gc.strokeLine(padL, y, w - padR, y);
            gc.setFill(Color.web("#94A3B8"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.fillText(String.valueOf((int)(max * (4 - i) / 4)), 4, y + 4);
        }

        double[] xs = new double[7], ys = new double[7];
        for (int i = 0; i < 7; i++) {
            xs[i] = padL + i * chartW / 6;
            ys[i] = padT + chartH - (chartH * evolution.get(i) / max);
        }

        // Aire
        gc.beginPath();
        gc.moveTo(xs[0], padT + chartH);
        for (int i = 0; i < 7; i++) gc.lineTo(xs[i], ys[i]);
        gc.lineTo(xs[6], padT + chartH);
        gc.closePath();
        gc.setFill(Color.web("#EDE9FE", 0.5));
        gc.fill();

        // Courbe
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < 7; i++) {
            double cpx = (xs[i - 1] + xs[i]) / 2;
            gc.bezierCurveTo(cpx, ys[i - 1], cpx, ys[i], xs[i], ys[i]);
        }
        gc.setStroke(Color.web("#7C3AED"));
        gc.setLineWidth(2.5);
        gc.stroke();

        // Points
        for (int i = 0; i < 7; i++) {
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
            gc.setStroke(Color.web("#7C3AED"));
            gc.setLineWidth(2);
            gc.strokeOval(xs[i] - 5, ys[i] - 5, 10, 10);

            gc.setFill(Color.web("#64748B"));
            gc.setFont(Font.font("Segoe UI", 9));
            gc.fillText(jours[i], xs[i] - 8, padT + chartH + 18);
            gc.fillText(String.valueOf(evolution.get(i)), xs[i] - 5, ys[i] - 10);
        }
    }

    private List<Long> getInscriptionsParJour() {
        List<Long> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            java.sql.Date date = new java.sql.Date(cal.getTimeInMillis());
            long count = tousLesUsers.stream()
                    .filter(u -> u.getCreatedAt() != null &&
                            u.getCreatedAt().toLocalDateTime().toLocalDate()
                                    .equals(date.toLocalDate()))
                    .count();
            result.add(count);
        }
        return result;
    }

    private void chargerTableStats() {
        if (tousLesUsers == null || tousLesUsers.isEmpty()) {
            tableStats.setItems(FXCollections.observableArrayList());
            return;
        }

        long total = tousLesUsers.size();
        ObservableList<StatItem> stats = FXCollections.observableArrayList();

        // Par rôle
        Map<String, Long> parRole = tousLesUsers.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(
                        u -> {
                            String role = u.getRole().toString();
                            // Garder le nom complet sans transformation
                            return role;
                        },
                        Collectors.counting()));

        parRole.forEach((role, count) -> {
            double pct = total > 0 ? count * 100.0 / total : 0;
            stats.add(new StatItem(role, count, String.format("%.1f%%", pct)));
        });

        // Par statut
        Map<String, Long> parStatut = tousLesUsers.stream()
                .filter(u -> u.getStatut() != null)
                .collect(Collectors.groupingBy(
                        u -> u.getStatut(),
                        Collectors.counting()));

        parStatut.forEach((statut, count) -> {
            double pct = total > 0 ? count * 100.0 / total : 0;
            stats.add(new StatItem(statut, count, String.format("%.1f%%", pct)));
        });

        // Configuration DIRECTE des cellules - SOLUTION PRINCIPALE
        colCategorie.setCellValueFactory(cellData -> {
            StatItem item = cellData.getValue();
            return new SimpleStringProperty(item.getCategory());
        });

        colNombre.setCellValueFactory(cellData -> {
            StatItem item = cellData.getValue();
            return new SimpleLongProperty(item.getCount()).asObject();
        });

        colPourcentage.setCellValueFactory(cellData -> {
            StatItem item = cellData.getValue();
            return new SimpleStringProperty(item.getPercentage());
        });

        tableStats.setItems(stats);
        tableStats.refresh();

        System.out.println("✅ Statistiques chargées: " + stats.size() + " lignes");
        System.out.println("   Première ligne: " + (stats.isEmpty() ? "aucune" : stats.get(0).getCategory()));
    }
    // Ajout des variables pour stocker les données des graphiques
    private Map<String, Long> dernierRoleData;
    private Map<String, Long> dernierStatutData;

    // Méthodes pour gérer les clics sur les graphiques
    private void configurerClicsGraphiques() {
        // Rendre les canvas cliquables
        canvasRoles.setOnMouseClicked(e -> {
            double x = e.getX();
            double y = e.getY();
            String roleClique = getRoleFromClick(x, y);
            if (roleClique != null && !roleClique.equals("Tous les rôles")) {
                // Appliquer le filtre sur le rôle cliqué
                if (filtreRole != null) {
                    filtreRole.setValue(roleClique);
                    appliquerFiltres();
                    // Changer vers l'onglet Gestion Utilisateurs
                    afficherGestion();
                    // Message de confirmation
                    showMessageDialog("🔍 Filtre appliqué", "Affichage des utilisateurs avec le rôle: " + roleClique);
                }
            }
        });

        canvasStatuts.setOnMouseClicked(e -> {
            double x = e.getX();
            double y = e.getY();
            String statutClique = getStatutFromClick(x, y);
            if (statutClique != null && !statutClique.equals("Tous les statuts")) {
                // Appliquer le filtre sur le statut cliqué
                if (filtreStatut != null) {
                    filtreStatut.setValue(statutClique);
                    appliquerFiltres();
                    // Changer vers l'onglet Gestion Utilisateurs
                    afficherGestion();
                    showMessageDialog("🔍 Filtre appliqué", "Affichage des utilisateurs avec le statut: " + statutClique);
                }
            }
        });

        // Clic sur les cartes statistiques
        statTotalUsers.setOnMouseClicked(e -> {
            reinitialiserFiltres();
            afficherGestion();
            showMessageDialog("📊 Tous les utilisateurs", "Affichage de tous les " + statTotalUsers.getText() + " utilisateurs");
        });

        statActifs.setOnMouseClicked(e -> {
            if (filtreStatut != null) filtreStatut.setValue("actif");
            if (filtreActif != null) filtreActif.setValue("Actif");
            appliquerFiltres();
            afficherGestion();
            showMessageDialog("✅ Utilisateurs actifs", "Affichage des " + statActifs.getText() + " utilisateurs actifs");
        });

        statEnAttente.setOnMouseClicked(e -> {
            if (filtreStatut != null) filtreStatut.setValue("en_attente");
            appliquerFiltres();
            afficherGestion();
            showMessageDialog("⏳ Demandes en attente", "Affichage des " + statEnAttente.getText() + " demandes en attente");
        });

        statBloques.setOnMouseClicked(e -> {
            if (filtreStatut != null) filtreStatut.setValue("bloqué");
            appliquerFiltres();
            afficherGestion();
            showMessageDialog("🔒 Comptes bloqués", "Affichage des " + statBloques.getText() + " comptes bloqués");
        });

        // Clic sur les lignes du tableau des stats
        tableStats.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Double-clic
                StatItem selected = tableStats.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    String categorie = selected.getCategory();
                    if (categorie.startsWith("Rôle - ")) {
                        String role = categorie.substring(7);
                        if (filtreRole != null) {
                            filtreRole.setValue(role);
                            appliquerFiltres();
                            afficherGestion();
                            showMessageDialog("🎯 Filtre par rôle", "Affichage des utilisateurs avec le rôle: " + role);
                        }
                    } else if (categorie.startsWith("Statut - ")) {
                        String statut = categorie.substring(9);
                        if (filtreStatut != null) {
                            filtreStatut.setValue(statut);
                            appliquerFiltres();
                            afficherGestion();
                            showMessageDialog("📊 Filtre par statut", "Affichage des utilisateurs avec le statut: " + statut);
                        }
                    }
                }
            }
        });
    }

    private String getRoleFromClick(double x, double y) {
        if (dernierRoleData == null) return null;

        double w = canvasRoles.getWidth();
        double h = canvasRoles.getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double r = Math.min(w, h) * 0.35;

        // Vérifier si le clic est dans le donut
        double dx = x - cx;
        double dy = y - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > r * 0.55 && distance < r) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0) angle += 360;

            double startAngle = -90;
            List<String> roles = new ArrayList<>(dernierRoleData.keySet());
            List<Long> values = new ArrayList<>(dernierRoleData.values());
            long total = values.stream().mapToLong(Long::longValue).sum();

            for (int i = 0; i < values.size(); i++) {
                double arc = 360.0 * values.get(i) / total;
                if (angle >= startAngle && angle <= startAngle + arc) {
                    return roles.get(i);
                }
                startAngle += arc;
            }
        }
        return null;
    }

    private String getStatutFromClick(double x, double y) {
        if (dernierStatutData == null) return null;

        double w = canvasStatuts.getWidth();
        double h = canvasStatuts.getHeight();

        double padL = 50, padR = 20, padT = 20, padB = 40;
        double barW = (w - padL - padR) / (dernierStatutData.size() * 1.6);
        double chartH = h - padT - padB;

        List<String> statuts = new ArrayList<>(dernierStatutData.keySet());
        List<Long> values = new ArrayList<>(dernierStatutData.values());
        long max = Math.max(1, values.stream().mapToLong(Long::longValue).max().orElse(1));

        for (int i = 0; i < values.size(); i++) {
            double bx = padL + i * (barW * 1.6) + barW * 0.3;
            double barH = chartH * values.get(i) / max;

            if (x >= bx && x <= bx + barW && y >= padT + chartH - barH && y <= padT + chartH) {
                return statuts.get(i);
            }
        }
        return null;
    }

    private void showMessageDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
