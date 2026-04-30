package org.example.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.config.Config;
import org.example.entities.User;
import org.example.services.*;
import org.example.utils.*;

// ── Imports AWT UNIQUEMENT pour Swing/Webcam — PAS Button ni TextField ──
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
// ────────────────────────────────────────────────────────────────────────
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.prefs.Preferences;

/**
 * LoginController — Toutes les erreurs corrigées :
 *
 *  1. btnOeilLogin          → javafx.scene.control.Button  (plus java.awt.Button)
 *  2. togglePasswordVisibility → 100% JavaFX (TextField/PasswordField dans HBox)
 *  3. passwordField.getParent() → HBox référencée via @FXML passwordBox
 *  4. Suppression imports   → java.awt.Button et java.awt.TextField retirés
 *  5. getCurrentPassword()  → lit le bon champ selon l'état de l'œil
 *  6. Se souvenir de moi    → Preferences Java (persistant entre sessions)
 *  7. SLF4J                 → ajouter slf4j-nop dans pom.xml (voir commentaire bas)
 */
public class LoginController {

    // ── Champs FXML — 100 % javafx.scene.control ─────────────────────
    @FXML private javafx.scene.control.TextField     emailField;
    @FXML private javafx.scene.control.PasswordField passwordField;
    @FXML private javafx.scene.control.Button        loginButton;
    @FXML private javafx.scene.control.Button        googleButton;
    @FXML private javafx.scene.control.Button        facialButton;
    @FXML private javafx.scene.control.Button        btnOeilLogin;  // ← JavaFX, pas AWT
    @FXML private Hyperlink                           forgotPasswordLink;
    @FXML private javafx.scene.control.Label         messageLabel;
    @FXML private StackPane                           captchaContainer;
    @FXML private javafx.scene.control.CheckBox       rememberMeCheck;
    /**
     * HBox déclarée dans le FXML autour du PasswordField + btnOeil :
     *   <HBox fx:id="passwordBox" ...>
     *     <PasswordField fx:id="passwordField" .../>
     *     <Button fx:id="btnOeilLogin" .../>
     *   </HBox>
     */
    @FXML private HBox passwordBox;

    // ── Services ──────────────────────────────────────────────────────
    private final CaptchaService           captchaService    = new CaptchaService();
    private final EmailService             emailService      = new EmailService();
    private final FacialRecognitionService facialService     = new FacialRecognitionService();
    private final GoogleAuthService        googleAuthService = new GoogleAuthService();

    // ── État interne ──────────────────────────────────────────────────
    private String  currentCaptchaResponse = "";
    private Webcam  webcam;
    private Stage   facialStage;
    private JFrame  webcamFrame;
    private boolean passwordVisible = false;

    // TextField JavaFX affiché à la place du PasswordField quand l'œil est actif
    private javafx.scene.control.TextField visiblePasswordField;

    // ── Préférences persistantes (Se souvenir de moi) ─────────────────
    private static final Preferences prefs      = Preferences.userNodeForPackage(LoginController.class);
    private static final String      PREF_EMAIL = "rem_email";
    private static final String      PREF_PWD   = "rem_pwd";
    private static final String      PREF_REM   = "rem_flag";

    // ══════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        try {
            LocalCallbackServer.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadGoogleRecaptcha();
        chargerIdentifiantsSauvegardes();
    }

    // ══════════════════════════════════════════════════════════════════
    //  SE SOUVENIR DE MOI
    // ══════════════════════════════════════════════════════════════════

    private void chargerIdentifiantsSauvegardes() {
        if (prefs.getBoolean(PREF_REM, false)) {
            emailField.setText(prefs.get(PREF_EMAIL, ""));
            passwordField.setText(prefs.get(PREF_PWD, ""));
            if (rememberMeCheck != null) rememberMeCheck.setSelected(true);
        }
    }

    private void sauvegarderOuEffacerIdentifiants(String email, String pwd) {
        if (rememberMeCheck != null && rememberMeCheck.isSelected()) {
            prefs.putBoolean(PREF_REM, true);
            prefs.put(PREF_EMAIL, email);
            prefs.put(PREF_PWD, pwd);
        } else {
            prefs.putBoolean(PREF_REM, false);
            prefs.remove(PREF_EMAIL);
            prefs.remove(PREF_PWD);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ŒIL — AFFICHER / MASQUER MOT DE PASSE  (100 % JavaFX)
    //  Correction des 5 erreurs liées à togglePasswordVisibility
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void togglePasswordVisibility() {

        if (!passwordVisible) {
            // ─── AFFICHER le mot de passe ─────────────────────────────
            String currentPwd = passwordField.getText();

            // Créer un TextField JavaFX qui imitera le PasswordField
            visiblePasswordField = new javafx.scene.control.TextField(currentPwd);
            visiblePasswordField.setStyle(
                    "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;" +
                            "-fx-padding: 12 14; -fx-background-radius: 10 0 0 10;" +
                            "-fx-border-color: transparent; -fx-background-color: transparent;"
            );
            // Faire grandir le TextField dans le HBox comme le PasswordField
            HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);

            // Synchroniser la frappe → le PasswordField (source de vérité cachée)
            visiblePasswordField.textProperty().addListener(
                    (obs, oldVal, newVal) -> passwordField.setText(newVal)
            );

            // Cacher le PasswordField et insérer le TextField à l'index 0 dans passwordBox
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordBox.getChildren().add(0, visiblePasswordField);

            if (btnOeilLogin != null) btnOeilLogin.setText("🙈");
            passwordVisible = true;

        } else {
            // ─── MASQUER le mot de passe ──────────────────────────────
            if (visiblePasswordField != null) {
                // Récupérer la valeur tapée avant de retirer le TextField
                passwordField.setText(visiblePasswordField.getText());
                passwordBox.getChildren().remove(visiblePasswordField);
                visiblePasswordField = null;
            }

            // Ré-afficher le PasswordField
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            if (btnOeilLogin != null) btnOeilLogin.setText("👁");
            passwordVisible = false;
        }
    }

    /** Retourne le mot de passe courant depuis le champ actif. */
    private String getCurrentPassword() {
        if (passwordVisible && visiblePasswordField != null) {
            return visiblePasswordField.getText();
        }
        return passwordField.getText();
    }

    // ══════════════════════════════════════════════════════════════════
    //  CAPTCHA Google reCAPTCHA v2
    // ══════════════════════════════════════════════════════════════════

    private void loadGoogleRecaptcha() {
        if (captchaContainer == null) return;
        captchaContainer.getChildren().clear();

        // S'assurer que le serveur est démarré
        try { LocalCallbackServer.startServer(); } catch (Exception e) { e.printStackTrace(); }

        WebView webView = new WebView();
        webView.setPrefHeight(120);
        webView.setPrefWidth(380);
        webView.setMinHeight(120);
        webView.setMaxHeight(120);


        // Charger depuis le serveur HTTP — pas loadContent()
        webView.getEngine().load("http://localhost:8080/captcha");

        // Polling toutes les 500ms sur le token stocké dans LocalCallbackServer
        javafx.animation.Timeline poll = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(500),
                        e -> {
                            String token = LocalCallbackServer.lastHCaptchaToken;
                            if (token != null && !token.isEmpty()
                                    && !token.equals(currentCaptchaResponse)) {
                                currentCaptchaResponse = token;
                                System.out.println("✓ Token récupéré par polling, longueur: "
                                        + token.length());
                            }
                        }
                )
        );
        poll.setCycleCount(javafx.animation.Animation.INDEFINITE);
        poll.play();

        captchaContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) poll.stop();
        });

        captchaContainer.getChildren().add(webView);
    }   // ══════════════════════════════════════════════════════════════════
    //  CONNEXION PRINCIPALE
    // ══════════════════════════════════════════════════════════════════


    @FXML
    public void connecter() {
        String email    = emailField.getText().trim();
        String password = getCurrentPassword();

        System.out.println("DEBUG → currentCaptchaResponse = '" + currentCaptchaResponse + "'");

//        if (currentCaptchaResponse.isEmpty()
//                || !captchaService.verifyCaptcha(currentCaptchaResponse)) {
//            setMessage("⚠ Veuillez valider le captcha.", true);
//            return;
//        }

        currentCaptchaResponse = "";

        if (!ValidationUtils.isNonVide(email) || !ValidationUtils.isNonVide(password)) {
            setMessage("⚠ Email et mot de passe requis.", true);
            return;
        }

        if (!ValidationUtils.isEmailValide(email)) {
            setMessage(ValidationUtils.messageEmail(), true);
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connexion...");

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                return authenticate(email, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();
            if (user != null) {
                if (!user.isActive() || !"actif".equalsIgnoreCase(user.getStatut())) {
                    setMessage("⚠ Votre compte n'est pas encore activé par l'administrateur.", true);
                } else if (!user.isVerified()) {
                    setMessage("⚠ Vérifiez votre email (lien d'activation envoyé).", true);
                } else {
                    sauvegarderOuEffacerIdentifiants(email, password);
                    setMessage("✓ Connexion réussie !", false);
                    try {
                        redirigerSelonRole(user);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                setMessage("✗ Email ou mot de passe incorrect.", true);
            }
            loginButton.setDisable(false);
            loginButton.setText("Se connecter  →");
        });

        loginTask.setOnFailed(event -> {
            setMessage("✗ Erreur : " + loginTask.getException().getMessage(), true);
            loginButton.setDisable(false);
            loginButton.setText("Se connecter  →");
        });

        new Thread(loginTask).start();
    }

    private User authenticate(String email, String password) {
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && PasswordUtils.verifier(password, rs.getString("password"))) {
                return mapUser(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setCin(rs.getString("cin"));
        user.setStatut(rs.getString("statut"));
        user.setActive(rs.getBoolean("is_active"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setCreatedAt(rs.getTimestamp("created_at"));

        // Normaliser "Responsable Etudiant" → "RESPONSABLE_ETUDIANT"
        String role = rs.getString("role")
                .toUpperCase()
                .trim()
                .replace(" ", "_");

        switch (role) {
            case "ADMIN"                  -> user.setRole(org.example.enums.Role.ADMIN);
            case "ETUDIANT"               -> user.setRole(org.example.enums.Role.ETUDIANT);
            case "PSYCHOLOGUE"            -> user.setRole(org.example.enums.Role.PSYCHOLOGUE);
            default                       -> user.setRole(org.example.enums.Role.RESPONSABLE_ETUDIANT);
        }
        return user;
    }

    private void redirigerSelonRole(User user) throws Exception {
        String fxml = switch (user.getRole()) {
            case ADMIN               -> "/admin_dashboard.fxml";
            case ETUDIANT            -> "/dashboard_etudiant.fxml";
            case PSYCHOLOGUE         -> "/dashboard_psychologue.fxml";
            default                  -> "/dashboard_responsable.fxml";
        };

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());

        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("UniMind - " + user.getRole());
        stage.setMaximized(true);

        Object controller = loader.getController();
        if (controller instanceof org.example.controllers.admin.AdminDashboardController) {
            ((org.example.controllers.admin.AdminDashboardController) controller).setUser(user);
        } else if (controller instanceof BaseDashboardController) {
            ((BaseDashboardController) controller).setUser(user);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  MOT DE PASSE OUBLIÉ
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void allerReinitialisationMdp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 500, 450));
            stage.setTitle("UniMind - Mot de passe oublié");
            stage.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de réinitialisation.");
        }
    }

    @FXML
    public void motDePasseOublie() {
        String email = emailField.getText().trim();
        if (!ValidationUtils.isEmailValide(email)) {
            showAlert(Alert.AlertType.WARNING, "Email requis",
                    "Entrez votre email pour recevoir le lien de réinitialisation.");
            return;
        }

        Task<Void> resetTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT user_id, nom, prenom FROM user WHERE email = ?")) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int    userId = rs.getInt("user_id");
                        String nom    = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        String token  = emailService.generateToken();
                        emailService.storeResetToken(userId, token, conn);
                        emailService.sendResetPasswordEmail(email, prenom + " " + nom, token);
                    }
                }
                return null;
            }
        };

        resetTask.setOnSucceeded(e ->
                showAlert(Alert.AlertType.INFORMATION, "Email envoyé",
                        "Un lien de réinitialisation a été envoyé à votre adresse email."));
        resetTask.setOnFailed(e ->
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        resetTask.getException().getMessage()));
        new Thread(resetTask).start();
    }

    // ══════════════════════════════════════════════════════════════════
    //  CONNEXION GOOGLE
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void loginWithGoogle() {
        try {
            String loginUrl = googleAuthService.getLoginUrl();

            Stage authStage = new Stage();
            authStage.initModality(Modality.APPLICATION_MODAL);
            authStage.setTitle("Connexion Google");
            authStage.setWidth(500);
            authStage.setHeight(600);

            WebView webView = new WebView();
            webView.getEngine().load(loginUrl);

            webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl != null && newUrl.startsWith("http://localhost:8080/callback")) {
                    String code = extractCodeFromUrl(newUrl);
                    if (code != null) {
                        authStage.close();
                        setMessage("Connexion Google en cours...", false);

                        Task<GoogleAuthService.GoogleUserInfo> task = new Task<>() {
                            @Override
                            protected GoogleAuthService.GoogleUserInfo call() {
                                return googleAuthService.exchangeCodeForUserInfo(code);
                            }
                        };

                        task.setOnSucceeded(ev -> {
                            GoogleAuthService.GoogleUserInfo info = task.getValue();
                            if (info != null && info.email != null && !info.email.isEmpty()) {
                                handleGoogleLogin(info);
                            } else {
                                setMessage("✗ Échec récupération infos Google.", true);
                            }
                        });
                        task.setOnFailed(ev ->
                                setMessage("✗ Erreur Google : " + task.getException().getMessage(), true));

                        new Thread(task).start();
                    }
                }
            });

            authStage.setScene(new Scene(webView, 500, 600));
            authStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de lancer la connexion Google : " + e.getMessage());
        }
    }

    private String extractCodeFromUrl(String url) {
        try {
            String[] parts = url.split("\\?");
            if (parts.length > 1) {
                for (String param : parts[1].split("&")) {
                    if (param.startsWith("code=")) return param.substring(5);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void handleGoogleLogin(GoogleAuthService.GoogleUserInfo userInfo) {
        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return findUserByEmail(userInfo.email);
            }
        };

        task.setOnSucceeded(ev -> {
            User existing = task.getValue();
            if (existing != null) {
                try {
                    redirigerSelonRole(existing);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'interface.");
                }
            } else {
                showGoogleSignupDialog(userInfo);
            }
        });
        task.setOnFailed(ev ->
                showAlert(Alert.AlertType.ERROR, "Erreur", task.getException().getMessage()));

        new Thread(task).start();
    }

    private User findUserByEmail(String email) {
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showGoogleSignupDialog(GoogleAuthService.GoogleUserInfo userInfo) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Nouveau compte");
            alert.setHeaderText("Bienvenue " +
                    (userInfo.givenName != null ? userInfo.givenName : "") + " !");
            alert.setContentText("Aucun compte associé à :\n" + userInfo.email +
                    "\n\nSouhaitez-vous créer un nouveau compte UniMind ?");

            ButtonType btnCreer   = new ButtonType("Créer mon compte", ButtonBar.ButtonData.YES);
            ButtonType btnAnnuler = new ButtonType("Annuler",          ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnCreer, btnAnnuler);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnCreer) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/inscription.fxml"));
                        Stage stage = new Stage();
                        stage.setScene(new Scene(loader.load()));

                        InscriptionController ctrl = loader.getController();
                        if (ctrl != null) {
                            ctrl.prefillWithGoogleInfo(
                                    userInfo.email, userInfo.givenName, userInfo.familyName);
                        }
                        stage.setTitle("Compléter mon inscription");
                        stage.show();
                        ((Stage) loginButton.getScene().getWindow()).close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'inscription.");
                    }
                }
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  RECONNAISSANCE FACIALE (Webcam sarxos + Flask DeepFace)
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void loginWithFacial() {
        String email = emailField.getText().trim();
        if (!ValidationUtils.isEmailValide(email)) {
            showAlert(Alert.AlertType.WARNING, "Email requis",
                    "Entrez votre email avant la reconnaissance faciale.");
            return;
        }

        facialStage = new Stage();
        facialStage.setTitle("Reconnaissance Faciale");
        facialStage.initModality(Modality.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout());

        webcam = Webcam.getDefault();
        if (webcam != null) {
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            WebcamPanel webcamPanel = new WebcamPanel(webcam);
            webcamPanel.setImageSizeDisplayed(true);
            panel.add(webcamPanel, BorderLayout.CENTER);
        } else {
            JLabel errorLabel = new JLabel("Aucune webcam détectée", JLabel.CENTER);
            panel.add(errorLabel, BorderLayout.CENTER);
        }

        JButton captureBtn = new JButton("📸  Capturer et Vérifier");
        captureBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        captureBtn.setBackground(new Color(5, 150, 105));
        captureBtn.setForeground(Color.WHITE);
        captureBtn.setFocusPainted(false);
        captureBtn.addActionListener(e -> captureAndVerify(email));
        panel.add(captureBtn, BorderLayout.SOUTH);

        webcamFrame = new JFrame("UniMind — Reconnaissance Faciale");
        webcamFrame.add(panel);
        webcamFrame.pack();
        webcamFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        webcamFrame.setLocationRelativeTo(null);
        webcamFrame.setVisible(true);

        facialStage.setOnHidden(ev -> {
            if (webcamFrame != null) webcamFrame.dispose();
            if (webcam != null && webcam.isOpen()) webcam.close();
        });

        // Scène JavaFX vide (l'UI réelle est dans webcamFrame Swing)
        facialStage.setScene(new Scene(new StackPane(), 1, 1));
        facialStage.show();
    }

    private void captureAndVerify(String email) {
        if (webcam == null || !webcam.isOpen()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Webcam non disponible.");
            return;
        }

        BufferedImage faceImage = webcam.getImage();
        webcam.close();

        Task<Boolean> verifyTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT user_id FROM user WHERE email = ?")) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return facialService.verifyFace(faceImage, rs.getInt("user_id"));
                    }
                }
                return false;
            }
        };

        verifyTask.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(verifyTask.getValue())) {
                if (facialStage != null) facialStage.close();
                if (webcamFrame != null) webcamFrame.dispose();
                connectWithEmail(email);
            } else {
                showAlert(Alert.AlertType.ERROR, "Échec", "Visage non reconnu. Veuillez réessayer.");
            }
        });
        verifyTask.setOnFailed(e ->
                showAlert(Alert.AlertType.ERROR, "Erreur", verifyTask.getException().getMessage()));

        new Thread(verifyTask).start();
    }

    private void connectWithEmail(String email) {
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapUser(rs);
                Platform.runLater(() -> {
                    try { redirigerSelonRole(user); }
                    catch (Exception ex) { ex.printStackTrace(); }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════

    @FXML
    public void allerInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/inscription.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 900, 700));
            stage.setTitle("UniMind - Inscription");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════════

    private void setMessage(String msg, boolean isError) {
        if (messageLabel == null) return;
        Platform.runLater(() -> {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError
                    ? "-fx-text-fill:#DC2626;-fx-font-size:12px;-fx-font-family:'Segoe UI';" +
                    "-fx-background-color:#FEF2F2;-fx-background-radius:8;" +
                    "-fx-padding:10 14;-fx-border-color:#FECACA;" +
                    "-fx-border-radius:8;-fx-border-width:1;"
                    : "-fx-text-fill:#059669;-fx-font-size:12px;-fx-font-family:'Segoe UI';" +
                    "-fx-background-color:#ECFDF5;-fx-background-radius:8;" +
                    "-fx-padding:10 14;-fx-border-color:#6EE7B7;" +
                    "-fx-border-radius:8;-fx-border-width:1;"
            );
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}

/*
 * ══════════════════════════════════════════════════════════════════════
 *  CORRECTION SLF4J — Ajouter dans pom.xml pour supprimer l'avertissement
 * ══════════════════════════════════════════════════════════════════════
 *
 *  <dependency>
 *      <groupId>org.slf4j</groupId>
 *      <artifactId>slf4j-nop</artifactId>
 *      <version>2.0.9</version>
 *  </dependency>
 *
 * ══════════════════════════════════════════════════════════════════════
 *  CORRECTION FacialRecognitionService — Remplacer les imports Apache HC5
 *  par java.net.http (déjà dans le JDK, pas besoin de dépendance externe) :
 *  Voir FacialRecognitionService_fixed.java
 * ══════════════════════════════════════════════════════════════════════
 */