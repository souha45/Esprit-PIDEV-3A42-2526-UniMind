package org.example.controllers.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    // ── NAVBAR ────────────────────────────────────────────────────────
    @FXML private TextField navRechercheField;
    @FXML private Label     navNomAdmin;
    @FXML private ImageView navPhotoAdmin;

    // ── SIDEBAR ───────────────────────────────────────────────────────
    @FXML private Button btnGestionUsers;
    @FXML private Button btnDemandes;
    @FXML private Button btnProfil;
    @FXML private Button btnDeconnexion;

    // ── PANNEAUX CONTENU ──────────────────────────────────────────────
    @FXML private VBox panneauGestion;
    @FXML private VBox panneauDemandes;
    @FXML private VBox panneauProfil;

    // ── TABLE UTILISATEURS ────────────────────────────────────────────
    @FXML private TableView<User>         tableUtilisateurs;
    @FXML private TableColumn<User, String>  colNom;
    @FXML private TableColumn<User, String>  colPrenom;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colStatut;
    @FXML private TableColumn<User, Boolean> colActif;
    @FXML private TableColumn<User, Void>    colActions;
    @FXML private Label messageGestion;

    // ── TABLE DEMANDES ────────────────────────────────────────────────
    @FXML private TableView<User>         tableDemandes;
    @FXML private TableColumn<User, String>  dColNom;
    @FXML private TableColumn<User, String>  dColPrenom;
    @FXML private TableColumn<User, String>  dColEmail;
    @FXML private TableColumn<User, String>  dColRole;
    @FXML private Label messageDemandes;

    // ── PROFIL ADMIN ──────────────────────────────────────────────────
    @FXML private Label         pNomPrenom, pEmail, pRole, pStatut;
    @FXML private TextField     pBio, pTel;
    @FXML private PasswordField pAncienMdp, pNouveauMdp, pConfirmMdp;
    @FXML private Label         pErrTel, pErrMdp, pMessageProfil;
    @FXML private ImageView     photoProfile;

    // ── État ──────────────────────────────────────────────────────────
    private AdminService            adminService   = new AdminService();
    private User                    adminConnecte;
    private ObservableList<User>    tousLesUsers   = FXCollections.observableArrayList();
    private String                  currentPhotoPath = null;

    // ══════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        configurerTable();
        configurerTableDemandes();
        chargerUtilisateurs();
        afficherPanneau("gestion");
        navRechercheField.textProperty().addListener((obs, o, n) -> filtrer(n));
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

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION SIDEBAR
    // ══════════════════════════════════════════════════════════════════

    @FXML public void afficherGestion()  { afficherPanneau("gestion");  chargerUtilisateurs(); }
    @FXML public void afficherDemandes() { afficherPanneau("demandes"); chargerDemandes(); }
    @FXML public void afficherProfil()   { afficherPanneau("profil"); }

    private void afficherPanneau(String nom) {
        panneauGestion.setVisible(false);  panneauGestion.setManaged(false);
        panneauDemandes.setVisible(false); panneauDemandes.setManaged(false);
        panneauProfil.setVisible(false);   panneauProfil.setManaged(false);

        // Reset tous les boutons
        String inactif = "-fx-background-color: transparent; -fx-text-fill: #C4B5FD;" +
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-pref-height: 46;" +
                "-fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 23; -fx-cursor: hand;" +
                "-fx-background-radius: 0;";
        String actif = "-fx-background-color: #4F46E5; -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold;" +
                "-fx-pref-height: 46; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 20;" +
                "-fx-cursor: hand; -fx-background-radius: 0;" +
                "-fx-border-color: transparent transparent transparent #A78BFA;" +
                "-fx-border-width: 0 0 0 3;";

        btnGestionUsers.setStyle(inactif);
        btnDemandes.setStyle(inactif);
        btnProfil.setStyle(inactif);

        switch (nom) {
            case "gestion"  -> { panneauGestion.setVisible(true);  panneauGestion.setManaged(true);  btnGestionUsers.setStyle(actif); }
            case "demandes" -> { panneauDemandes.setVisible(true); panneauDemandes.setManaged(true); btnDemandes.setStyle(actif); }
            case "profil"   -> { panneauProfil.setVisible(true);   panneauProfil.setManaged(true);   btnProfil.setStyle(actif); }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  TABLE UTILISATEURS — cellules redesignées
    // ══════════════════════════════════════════════════════════════════

    private void configurerTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // ── Colonne Rôle — badge coloré ───────────────────────────────
        // ── Colonne Rôle — badge coloré ───────────────────────────────
        colRole.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getRole() != null
                                ? cellData.getValue().getRole().toString()
                                : ""));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { setGraphic(null); return; }
                Label badge = new Label(item);
                String[] colors = roleColors(item);
                badge.setStyle(
                        "-fx-background-color: " + colors[0] + ";" +
                                "-fx-text-fill: " + colors[1] + ";" +
                                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                "-fx-font-family: 'Segoe UI';" +
                                "-fx-padding: 3 10; -fx-background-radius: 20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // ── Colonne Statut — badge coloré ─────────────────────────────
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String[] colors = statutColors(item);
                badge.setStyle(
                        "-fx-background-color: " + colors[0] + ";" +
                                "-fx-text-fill: " + colors[1] + ";" +
                                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                "-fx-font-family: 'Segoe UI';" +
                                "-fx-padding: 3 10; -fx-background-radius: 20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // ── Colonne Actif — indicateur visuel ─────────────────────────
        colActif.setCellValueFactory(new PropertyValueFactory<>("active"));
        colActif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER_LEFT);
                // Cercle indicateur
                Circle dot = new Circle(4);
                dot.setFill(Color.web(item ? "#10B981" : "#9CA3AF"));
                Label lbl = new Label(item ? "Actif" : "Inactif");
                lbl.setStyle("-fx-font-size: 12px; -fx-font-family: 'Segoe UI';" +
                        "-fx-text-fill: " + (item ? "#065F46" : "#6B7280") + ";");
                box.getChildren().addAll(dot, lbl);
                setGraphic(box);
                setText(null);
            }
        });

        // ── Colonne Actions — boutons icône compact ───────────────────
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit    = makeBtn("✏", "#4F46E5", "#EDE9FE");
            private final Button btnDel     = makeBtn("🗑", "#DC2626", "#FEE2E2");
            private final Button btnLock    = makeBtn("🔒", "#D97706", "#FEF3C7");
            private final Button btnUnlock  = makeBtn("🔓", "#059669", "#D1FAE5");
            private final Button btnOn      = makeBtn("✅", "#0284C7", "#DBEAFE");
            private final Button btnOff     = makeBtn("⛔", "#6B7280", "#F3F4F6");
            private final HBox  hbox        = new HBox(5, btnEdit, btnDel, btnLock, btnUnlock, btnOn, btnOff);

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(0, 4, 0, 4));
                btnEdit.setOnAction(e   -> ouvrirModalModification(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e    -> supprimerUtilisateur(getTableView().getItems().get(getIndex())));
                btnLock.setOnAction(e   -> bloquerUtilisateur(getTableView().getItems().get(getIndex())));
                btnUnlock.setOnAction(e -> debloquerUtilisateur(getTableView().getItems().get(getIndex())));
                btnOn.setOnAction(e     -> activerUtilisateur(getTableView().getItems().get(getIndex())));
                btnOff.setOnAction(e    -> desactiverUtilisateur(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        // Style global de la table
        tableUtilisateurs.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
            row.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
                if (!row.isEmpty())
                    row.setStyle("-fx-background-color: " + (isHovered ? "#F8F7FF" : "white") +
                            "; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
            });
            return row;
        });
    }

    /** Bouton icône compact avec couleur bg/fg */
    private Button makeBtn(String icon, String fgColor, String bgColor) {
        Button btn = new Button(icon);
        btn.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + fgColor + ";" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-background-radius: 6; -fx-padding: 4 8;" +
                        "-fx-min-width: 30; -fx-min-height: 28;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + fgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-background-radius: 6; -fx-padding: 4 8;" +
                        "-fx-min-width: 30; -fx-min-height: 28;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + fgColor + ";" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-background-radius: 6; -fx-padding: 4 8;" +
                        "-fx-min-width: 30; -fx-min-height: 28;"));
        return btn;
    }

    /** Couleurs pour les badges de rôle */
    private String[] roleColors(String role) {
        return switch (role.toUpperCase()) {
            case "ADMIN"                -> new String[]{"#EDE9FE", "#6D28D9"};
            case "ETUDIANT"             -> new String[]{"#DBEAFE", "#1D4ED8"};
            case "PSYCHOLOGUE"          -> new String[]{"#D1FAE5", "#065F46"};
            case "RESPONSABLE_ETUDIANT" -> new String[]{"#FFEDD5", "#C2410C"};
            default                     -> new String[]{"#F3F4F6", "#374151"};
        };
    }

    /** Couleurs pour les badges de statut */
    private String[] statutColors(String statut) {
        if (statut == null) return new String[]{"#F3F4F6", "#6B7280"};
        return switch (statut.toLowerCase()) {
            case "actif"        -> new String[]{"#D1FAE5", "#065F46"};
            case "en_attente"   -> new String[]{"#FEF9C3", "#854D0E"};
            case "bloqué",
                 "bloque"       -> new String[]{"#FEE2E2", "#991B1B"};
            case "inactif"      -> new String[]{"#F3F4F6", "#6B7280"};
            default             -> new String[]{"#F3F4F6", "#374151"};
        };
    }

    private void chargerUtilisateurs() {
        try {
            List<User> users = adminService.afficher();
            tousLesUsers.setAll(users);
            tableUtilisateurs.setItems(tousLesUsers);
            afficherMsgGestion("✓ " + users.size() + " utilisateur(s) chargé(s)", "#059669");
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
                        u.getEmail().toLowerCase().contains(r)));
    }

    // ══════════════════════════════════════════════════════════════════
    //  MODAL AJOUT / MODIFICATION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void ouvrirModalAjout() { ouvrirModal(null); }

    private void ouvrirModalModification(User user) { ouvrirModal(user); }

    private void ouvrirModal(User user) {
        VBox content = new VBox(16);
        content.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14;" +
                        "-fx-padding: 28; -fx-min-width: 480;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 24, 0, 0, 8);");

        // Titre
        Label titre = new Label(user == null ? "➕  Ajouter un utilisateur" :
                "✏  Modifier " + user.getPrenom() + " " + user.getNom());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;" +
                "-fx-text-fill: #1E1B4B; -fx-font-family: 'Segoe UI';");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #F3F4F6;");

        // Champs
        TextField nomField    = styledField("Nom *");
        TextField prenomField = styledField("Prénom *");
        TextField emailField  = styledField("Email *");
        TextField cinField    = styledField("CIN (8 chiffres) *");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe *");
        passwordField.setStyle(fieldStyle());

        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Admin", "Etudiant", "Psychologue", "Responsable Etudiant"));
        roleCombo.setValue("Etudiant");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle("-fx-background-radius: 10; -fx-border-color: #DDD6FE;" +
                "-fx-border-radius: 10; -fx-border-width: 1.5;" +
                "-fx-padding: 4; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");

        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        msgLabel.setWrapText(true);

        // Pré-remplir si modification
        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            cinField.setText(user.getCin());
            roleCombo.setValue(user.getRole().name());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        }

        // Grille 2 colonnes
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc, cc);
        grid.add(labeledField("Nom *",    nomField),    0, 0);
        grid.add(labeledField("Prénom *", prenomField), 1, 0);
        grid.add(labeledField("Email *",  emailField),  0, 1);
        grid.add(labeledField("CIN *",    cinField),    1, 1);
        grid.add(labeledField("Rôle *",   roleCombo),   0, 2);
        if (user == null)
            grid.add(labeledField("Mot de passe *", passwordField), 1, 2);

        // Boutons
        Button btnSave   = new Button("💾  Sauvegarder");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #4F46E5, #7C3AED);" +
                "-fx-text-fill: white; -fx-cursor: hand; -fx-padding: 10 24;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151;" +
                "-fx-cursor: hand; -fx-padding: 10 24; -fx-background-radius: 8;" +
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");

        HBox btnBox = new HBox(10, btnSave, btnAnnuler);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(titre, sep, grid, msgLabel, btnBox);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        btnSave.setOnAction(e -> {
            String nom      = nomField.getText().trim();
            String prenom   = prenomField.getText().trim();
            String email    = emailField.getText().trim();
            String cin      = cinField.getText().trim();
            String role     = roleCombo.getValue();
            String password = passwordField.getText();
            boolean ok = true;

            if (!ValidationUtils.isNomValide(nom)) {
                showModalErr(msgLabel, "Nom invalide"); ok = false;
            } else if (!ValidationUtils.isNomValide(prenom)) {
                showModalErr(msgLabel, "Prénom invalide"); ok = false;
            } else if (!ValidationUtils.isEmailValide(email)) {
                showModalErr(msgLabel, ValidationUtils.messageEmail()); ok = false;
            } else if (!ValidationUtils.isCinValide(cin)) {
                showModalErr(msgLabel, ValidationUtils.messageCin()); ok = false;
            } else if (user == null && !ValidationUtils.isPasswordValide(password)) {
                showModalErr(msgLabel, ValidationUtils.messagePassword()); ok = false;
            }

            if (ok) {
                try {
                    if (user == null) {
                        User u = new User(nom, prenom, email, password, cin,
                                org.example.enums.Role.valueOf(
                                        role.replace(" ", "_").toUpperCase()), "actif");
                        u.setActive(true);
                        u.setVerified(true);
                        adminService.ajouter(u);
                    } else {
                        user.setNom(nom); user.setPrenom(prenom);
                        user.setEmail(email); user.setCin(cin);
                        adminService.modifier(user);
                    }
                    showModalOk(msgLabel, user == null ? "✓ Utilisateur ajouté !" : "✓ Modifié avec succès !");
                    chargerUtilisateurs();
                    new Thread(() -> {
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                        javafx.application.Platform.runLater(stage::close);
                    }).start();
                } catch (SQLException ex) {
                    showModalErr(msgLabel, "Erreur : " + ex.getMessage());
                }
            }
        });

        btnAnnuler.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private String fieldStyle() {
        return "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;" +
                "-fx-padding: 10 12; -fx-background-radius: 10;" +
                "-fx-border-color: #DDD6FE; -fx-border-radius: 10;" +
                "-fx-border-width: 1.5; -fx-background-color: #FAFAFA;";
    }

    private VBox labeledField(String labelText, javafx.scene.Node field) {
        VBox box = new VBox(5);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: #374151; -fx-font-family: 'Segoe UI';");
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private void showModalErr(Label lbl, String msg) {
        lbl.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;" +
                "-fx-background-color: #FEF2F2; -fx-padding: 8 12;" +
                "-fx-background-radius: 8; -fx-font-family: 'Segoe UI';");
        lbl.setText("⚠  " + msg);
    }

    private void showModalOk(Label lbl, String msg) {
        lbl.setStyle("-fx-text-fill: #065F46; -fx-font-size: 12px;" +
                "-fx-background-color: #ECFDF5; -fx-padding: 8 12;" +
                "-fx-background-radius: 8; -fx-font-family: 'Segoe UI';");
        lbl.setText(msg);
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACTIONS CRUD
    // ══════════════════════════════════════════════════════════════════

    private void supprimerUtilisateur(User user) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer " + user.getPrenom() + " " + user.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
        c.setTitle("Confirmation suppression"); c.setHeaderText(null);
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                supprimerProfilUser(user.getUserId());
                adminService.supprimer(user.getUserId());
                afficherMsgGestion("✓ Utilisateur supprimé", "#059669");
                chargerUtilisateurs();
            } catch (SQLException e) {
                afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626");
            }
        }
    }

    private void bloquerUtilisateur(User user) {
        try { adminService.bloquer(user.getUserId());
            afficherMsgGestion("✓ Compte bloqué : " + user.getNom(), "#D97706");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    private void debloquerUtilisateur(User user) {
        try { adminService.debloquer(user.getUserId());
            afficherMsgGestion("✓ Compte débloqué : " + user.getNom(), "#059669");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    private void activerUtilisateur(User user) {
        try { adminService.debloquer(user.getUserId());
            afficherMsgGestion("✓ Compte activé : " + user.getNom(), "#059669");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    private void desactiverUtilisateur(User user) {
        try { adminService.bloquer(user.getUserId());
            afficherMsgGestion("✓ Compte désactivé : " + user.getNom(), "#D97706");
            chargerUtilisateurs();
        } catch (SQLException e) { afficherMsgGestion("Erreur : " + e.getMessage(), "#DC2626"); }
    }

    // ══════════════════════════════════════════════════════════════════
    //  TABLE DEMANDES
    // ══════════════════════════════════════════════════════════════════

    private void configurerTableDemandes() {
        dColNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        dColPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        dColEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        dColRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Badge rôle aussi dans la table demandes
        dColRole.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getRole() != null
                                ? cellData.getValue().getRole().toString()
                                : ""));
        dColRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { setGraphic(null); return; }
                Label badge = new Label(item);
                String[] colors = roleColors(item);
                badge.setStyle("-fx-background-color: " + colors[0] +
                        "; -fx-text-fill: " + colors[1] +
                        "; -fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-padding: 3 10; -fx-background-radius: 20;");
                setGraphic(badge); setText(null);
            }
        });

        tableDemandes.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isEmpty())
                    row.setStyle("-fx-background-color: " + (is ? "#F8F7FF" : "white") + ";");
            });
            return row;
        });
    }

    private void chargerDemandes() {
        try {
            List<User> demandes = adminService.afficherDemandesEnAttente();
            tableDemandes.setItems(FXCollections.observableArrayList(demandes));
            messageDemandes.setStyle("-fx-text-fill: #7C3AED; -fx-font-size: 12px;" +
                    "-fx-font-family: 'Segoe UI';");
            messageDemandes.setText("📋  " + demandes.size() + " demande(s) en attente de validation");
        } catch (SQLException e) {
            messageDemandes.setStyle("-fx-text-fill: #DC2626;");
            messageDemandes.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void accepterDemande() {
        User sel = tableDemandes.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Sélectionnez une demande !", ButtonType.OK).showAndWait(); return; }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Accepter " + sel.getPrenom() + " " + sel.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                adminService.accepterDemande(sel.getUserId());
                messageDemandes.setStyle("-fx-text-fill: #059669; -fx-font-size: 12px;");
                messageDemandes.setText("✓ Demande acceptée !");
                chargerDemandes(); chargerUtilisateurs();
            } catch (SQLException e) {
                messageDemandes.setStyle("-fx-text-fill: #DC2626;");
                messageDemandes.setText("Erreur : " + e.getMessage());
            }
        }
    }

    @FXML
    public void refuserDemande() {
        User sel = tableDemandes.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Sélectionnez une demande !", ButtonType.OK).showAndWait(); return; }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Refuser " + sel.getPrenom() + " " + sel.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                adminService.refuserDemande(sel.getUserId());
                messageDemandes.setStyle("-fx-text-fill: #D97706; -fx-font-size: 12px;");
                messageDemandes.setText("✓ Demande refusée.");
                chargerDemandes();
            } catch (SQLException e) {
                messageDemandes.setStyle("-fx-text-fill: #DC2626;");
                messageDemandes.setText("Erreur : " + e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PROFIL ADMIN
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void choisirPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File fichier = fc.showOpenDialog(photoProfile.getScene().getWindow());
        if (fichier == null) return;
        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(
                    System.getProperty("user.dir"), "uploads");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
            String nomFichier = fichier.getName();
            Files.copy(fichier.toPath(), uploadDir.resolve(nomFichier),
                    StandardCopyOption.REPLACE_EXISTING);
            currentPhotoPath = nomFichier;
            Image img = new Image(fichier.toURI().toString());
            photoProfile.setImage(img);
            navPhotoAdmin.setImage(img);
            afficherMsgProfil("✓ Photo sélectionnée — cliquez sur Sauvegarder", "#059669");
        } catch (IOException e) {
            afficherMsgProfil("Erreur photo : " + e.getMessage(), "#DC2626");
        }
    }

    @FXML
    public void sauvegarderProfil() {
        pErrTel.setText(""); pMessageProfil.setText("");
        String tel = pTel.getText().trim();
        String bio = pBio.getText().trim();
        if (!tel.isEmpty() && !ValidationUtils.isTelephoneValide(tel)) {
            pErrTel.setText("⚠  " + ValidationUtils.messageTelephone()); return;
        }
        try {
            var conn = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
            java.sql.PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM profil WHERE user_id = ?");
            check.setInt(1, adminConnecte.getUserId());
            java.sql.ResultSet rs = check.executeQuery(); rs.next();
            boolean existe = rs.getInt(1) > 0;
            if (existe) {
                conn.prepareStatement(
                        "UPDATE profil SET bio='" + bio + "', tel='" + tel + "'," +
                                " photo='" + currentPhotoPath + "', updated_at=NOW()" +
                                " WHERE user_id=" + adminConnecte.getUserId()).executeUpdate();
            } else {
                conn.prepareStatement(
                        "INSERT INTO profil (user_id, bio, tel, photo, updated_at)" +
                                " VALUES (" + adminConnecte.getUserId() + ",'" + bio + "','" + tel +
                                "','" + currentPhotoPath + "',NOW())").executeUpdate();
            }
            afficherMsgProfil("✓ Profil mis à jour avec succès !", "#059669");
        } catch (java.sql.SQLException e) {
            afficherMsgProfil("Erreur : " + e.getMessage(), "#DC2626");
        }
    }

    @FXML
    public void changerMotDePasse() {
        pErrMdp.setText(""); pMessageProfil.setText("");
        String ancien  = pAncienMdp.getText();
        String nouveau = pNouveauMdp.getText();
        String confirm = pConfirmMdp.getText();
        if (!ValidationUtils.isNonVide(ancien) || !ValidationUtils.isNonVide(nouveau)) {
            pErrMdp.setText("⚠  Tous les champs sont obligatoires"); return;
        }
        if (!ValidationUtils.isPasswordValide(nouveau)) {
            pErrMdp.setText("⚠  " + ValidationUtils.messagePassword()); return;
        }
        if (!ValidationUtils.isPasswordConfirme(nouveau, confirm)) {
            pErrMdp.setText("⚠  " + ValidationUtils.messagePasswordConfirm()); return;
        }
        try {
            adminService.changerMotDePasse(adminConnecte.getUserId(), ancien, nouveau);
            pAncienMdp.clear(); pNouveauMdp.clear(); pConfirmMdp.clear();
            afficherMsgProfil("✓ Mot de passe modifié avec succès !", "#059669");
        } catch (SQLException e) {
            pErrMdp.setText("⚠  " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  DÉCONNEXION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void seDeconnecter() {
        try {
            Stage stage = (Stage) navNomAdmin.getScene().getWindow();
            stage.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("UniMind — Connexion");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ══════════════════════════════════════════════════════════════════

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
                    File f = new File(System.getProperty("user.dir") + "/uploads/" + photoPath);
                    if (f.exists()) {
                        Image img = new Image(f.toURI().toString());
                        photoProfile.setImage(img);
                        navPhotoAdmin.setImage(img);
                        currentPhotoPath = photoPath;
                    }
                }
            }
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
                "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';");
        messageGestion.setText(msg);
    }

    private void afficherMsgProfil(String msg, String couleur) {
        pMessageProfil.setStyle("-fx-text-fill: " + couleur +
                "; -fx-font-size: 12px; -fx-font-family: 'Segoe UI';" +
                "-fx-background-color: " + (couleur.equals("#059669") ? "#ECFDF5" : "#FEF2F2") + ";" +
                "-fx-background-radius: 8; -fx-padding: 8 12;");
        pMessageProfil.setText(msg);
    }
}