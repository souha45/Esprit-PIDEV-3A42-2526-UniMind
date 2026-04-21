package org.example.controllers.admin;

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

    // PROFIL ADMIN
    @FXML private Label pNomPrenom, pEmail, pRole, pStatut;
    @FXML private TextField pBio, pTel;
    @FXML private PasswordField pAncienMdp, pNouveauMdp, pConfirmMdp;
    @FXML private Label pErrTel, pErrMdp, pMessageProfil;
    @FXML private ImageView photoProfile;

    private AdminService adminService = new AdminService();
    private User adminConnecte;
    private ObservableList<User> tousLesUsers = FXCollections.observableArrayList();
    private Stage modalStage;
    private String currentPhotoPath = null;

    // INITIALISATION
    @FXML
    public void initialize() {
        configurerTable();
        configurerTableDemandes();
        chargerUtilisateurs();

        // Retirer les trois VBox du StackPane (elles ne seront plus dans l'arbre)
        contentArea.getChildren().removeAll(panneauGestion, panneauDemandes, panneauProfil);

        // Afficher le panneau Gestion par défaut
        afficherGestion();

        navRechercheField.textProperty().addListener((obs, o, n) -> filtrer(n));

        modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
    }

    public void setUser(User user) {
        this.adminConnecte = user;
        navNomAdmin.setText(user.getPrenom() + " " + user.getNom());
        pNomPrenom.setText(user.getPrenom() + " " + user.getNom());
        pEmail.setText(user.getEmail());
        pRole.setText(user.getRole().toString());
        pStatut.setText(user.getStatut());
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
        // Recharger les informations du profil (au cas où)
        if (adminConnecte != null) {
            pNomPrenom.setText(adminConnecte.getPrenom() + " " + adminConnecte.getNom());
            pEmail.setText(adminConnecte.getEmail());
            pRole.setText(adminConnecte.getRole().toString());
            pStatut.setText(adminConnecte.getStatut());
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
        Button[] allButtons = {btnGestionUsers, btnDemandes, btnProfil, btnStats, btnSeances};
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
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colActif.setCellValueFactory(new PropertyValueFactory<>("active"));

        colActif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); setStyle(""); }
                else {
                    setText(item ? "✓ Actif" : "✗ Inactif");
                    setStyle(item ? "-fx-text-fill: #16A34A;" : "-fx-text-fill: #DC2626;");
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier = createIconButton("✏", "#2563EB");
            private final Button btnSupprimer = createIconButton("🗑", "#DC2626");
            private final Button btnBloquer = createIconButton("🔒", "#D97706");
            private final Button btnDebloquer = createIconButton("🔓", "#059669");
            private final Button btnActiver = createIconButton("✅", "#0284C7");
            private final Button btnDesactiver = createIconButton("⛔", "#6B7280");
            private final HBox container = new HBox(5, btnModifier, btnSupprimer, btnBloquer, btnDebloquer, btnActiver, btnDesactiver);

            {
                container.setAlignment(Pos.CENTER);
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
            afficherMsgGestion("✓ " + users.size() + " utilisateur(s)", "#16A34A");
        } catch (SQLException e) {
            afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
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
        VBox modalContent = new VBox(15);
        modalContent.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);");
        modalContent.setMaxWidth(450);

        Label titre = new Label(user == null ? "➕ Ajouter un utilisateur" : "✏ Modifier " + user.getPrenom() + " " + user.getNom());
        titre.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #4C1D95;");

        TextField nomField = new TextField();
        nomField.setPromptText("Nom *");
        nomField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #E5E7EB; -fx-border-radius: 6;");

        TextField prenomField = new TextField();
        prenomField.setPromptText("Prénom *");
        prenomField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #E5E7EB; -fx-border-radius: 6;");

        TextField emailField = new TextField();
        emailField.setPromptText("Email *");
        emailField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #E5E7EB; -fx-border-radius: 6;");

        TextField cinField = new TextField();
        cinField.setPromptText("CIN (8 chiffres) *");
        cinField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #E5E7EB; -fx-border-radius: 6;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe *");
        passwordField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #E5E7EB; -fx-border-radius: 6;");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Etudiant", "Psychologue", "Responsable Etudiant"));
        roleCombo.setValue("Etudiant");
        roleCombo.setStyle("-fx-padding: 5; -fx-background-radius: 6; -fx-border-color: #E5E7EB; -fx-border-radius: 6;");

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-font-size: 12;");

        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            cinField.setText(user.getCin());
            roleCombo.setValue(user.getRole().name());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        }

        Button btnSave = new Button("💾 Sauvegarder");
        btnSave.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 10 20; -fx-background-radius: 6;");

        Button btnAnnuler = new Button("✕ Annuler");
        btnAnnuler.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 10 20; -fx-background-radius: 6;");

        HBox btnBox = new HBox(10, btnSave, btnAnnuler);
        btnBox.setAlignment(Pos.CENTER);

        modalContent.getChildren().addAll(titre, nomField, prenomField, emailField, cinField, roleCombo, passwordField, messageLabel, btnBox);

        Scene scene = new Scene(modalContent);
        scene.setFill(Color.TRANSPARENT);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        btnSave.setOnAction(e -> {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String cin = cinField.getText().trim();
            String role = roleCombo.getValue();
            String password = passwordField.getText();

            boolean valide = true;

            if (!ValidationUtils.isNomValide(nom)) { messageLabel.setText("Nom invalide"); valide = false; }
            else if (!ValidationUtils.isNomValide(prenom)) { messageLabel.setText("Prénom invalide"); valide = false; }
            else if (!ValidationUtils.isEmailValide(email)) { messageLabel.setText(ValidationUtils.messageEmail()); valide = false; }
            else if (!ValidationUtils.isCinValide(cin)) { messageLabel.setText(ValidationUtils.messageCin()); valide = false; }
            else if (user == null && !ValidationUtils.isPasswordValide(password)) { messageLabel.setText(ValidationUtils.messagePassword()); valide = false; }

            if (valide) {
                try {
                    if (user == null) {
                        User u = new User(nom, prenom, email, password, cin,
                                org.example.enums.Role.valueOf(role.replace(" ", "_").toUpperCase()), "actif");
                        u.setActive(true);
                        u.setVerified(true);
                        adminService.ajouter(u);
                        messageLabel.setStyle("-fx-text-fill: #16A34A;");
                        messageLabel.setText("✓ Utilisateur ajouté !");
                    } else {
                        user.setNom(nom);
                        user.setPrenom(prenom);
                        user.setEmail(email);
                        user.setCin(cin);
                        adminService.modifier(user);
                        messageLabel.setStyle("-fx-text-fill: #16A34A;");
                        messageLabel.setText("✓ Modifié avec succès !");
                    }
                    chargerUtilisateurs();
                    new Thread(() -> {
                        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                        javafx.application.Platform.runLater(stage::close);
                    }).start();
                } catch (SQLException ex) {
                    messageLabel.setStyle("-fx-text-fill: #DC2626;");
                    messageLabel.setText("Erreur : " + ex.getMessage());
                }
            }
        });

        btnAnnuler.setOnAction(e -> stage.close());
        stage.showAndWait();
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
        dColRole.setCellValueFactory(new PropertyValueFactory<>("role"));
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
