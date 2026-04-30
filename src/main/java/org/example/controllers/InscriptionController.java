package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.example.entities.*;
import org.example.services.*;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.ValidationUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.Random;

public class InscriptionController {

    @FXML private VBox etapeRole;
    @FXML private VBox etapeFormulaire;
    @FXML private Label labelTitreRole;

    @FXML private TextField nomField, prenomField, emailField, cinField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private CheckBox conditionsCheck;

    @FXML private Label errNom, errPrenom, errEmail, errCin;
    @FXML private Label errPassword, errConfirm, errConditions;

    @FXML private VBox panneauEtudiant, panneauPsy, panneauResponsable;

    @FXML private TextField identifiantField, etablissementField;
    @FXML private Label errIdentifiant, errEtablissement;

    @FXML private TextField specialiteField, adresseField, telephoneField;
    @FXML private Label errSpecialite, errAdresse, errTelephone;

    @FXML private TextField posteField, etablissementRespField;
    @FXML private Label errPoste, errEtablissementResp;

    @FXML private Label messageGlobal;

    private String roleSelectionne = "";
    private EmailService emailService = new EmailService();

    @FXML
    public void initialize() {
        // Générer l'identifiant étudiant automatiquement
        if (identifiantField != null) {
            identifiantField.setEditable(false);
            identifiantField.setStyle("-fx-background-color: #F3F4F6; -fx-opacity: 0.7;");
            identifiantField.setText(generateStudentId());
        }

        // Map pour l'adresse du psychologue
        if (adresseField != null) {
            adresseField.setEditable(false);
            adresseField.setOnMouseClicked(e -> openMapForAddress());
        }
    }
//gener id etudient alea
    private String generateStudentId() {
        Random random = new Random();
        int year = Year.now().getValue();
        return "UNI" + year + String.format("%06d", random.nextInt(1000000));
    }

    private void openMapForAddress() {
        Stage mapStage = new Stage();
        mapStage.initModality(Modality.APPLICATION_MODAL);
        mapStage.setTitle("Sélectionner une adresse");
        mapStage.setWidth(900);
        mapStage.setHeight(700);

        WebView webView = new WebView();
        webView.getEngine().setJavaScriptEnabled(true);

        // Version avec ressources locales et CDN de secours
        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Sélection d'adresse</title>
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #f5f5f5; }
                #map { height: 550px; width: 100%; background: #e0e0e0; }
                .info-panel {
                    padding: 15px 20px;
                    background: white;
                    border-top: 3px solid #4F46E5;
                    box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
                }
                .address-box {
                    margin: 12px 0;
                    padding: 12px;
                    background: #f8f9fa;
                    border-radius: 8px;
                    border: 1px solid #e0e0e0;
                    min-height: 70px;
                    word-break: break-word;
                    font-size: 13px;
                }
                .address-box.loading {
                    background: #fff3cd;
                    border-color: #ffc107;
                    color: #856404;
                }
                .address-box.success {
                    background: #d4edda;
                    border-color: #28a745;
                    color: #155724;
                }
                .address-box.error {
                    background: #f8d7da;
                    border-color: #dc3545;
                    color: #721c24;
                }
                button {
                    width: 100%;
                    padding: 12px;
                    background: #4F46E5;
                    color: white;
                    border: none;
                    border-radius: 8px;
                    cursor: pointer;
                    font-size: 15px;
                    font-weight: bold;
                }
                button:hover:not(:disabled) { background: #7C3AED; }
                button:disabled {
                    background: #adb5bd;
                    cursor: not-allowed;
                }
                .instruction { margin-bottom: 10px; font-size: 12px; color: #666; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <div class="info-panel">
                <strong>📍 Sélection de votre adresse professionnelle</strong>
                <div class="instruction">Cliquez sur la carte pour placer un marqueur</div>
                <div class="address-box" id="addressDisplay">
                    Aucune adresse sélectionnée
                </div>
                <button id="confirmBtn" disabled>
                    ✅ Confirmer et utiliser cette adresse
                </button>
            </div>
            
            <script>
                // Attendre que la page soit complètement chargée
                window.addEventListener('load', function() {
                    try {
                        // Initialisation de la carte
                        var map = L.map('map').setView([36.8065, 10.1815], 13);
                        
                        // Ajout des tuiles OpenStreetMap
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '© OpenStreetMap contributors',
                            maxZoom: 19
                        }).addTo(map);
                        
                        var marker = null;
                        var currentAddress = '';
                        var addressDisplay = document.getElementById('addressDisplay');
                        var confirmBtn = document.getElementById('confirmBtn');
                        
                        // Gestion du clic sur la carte
                        map.on('click', function(e) {
                            var lat = e.latlng.lat;
                            var lng = e.latlng.lng;
                            
                            // Ajouter ou déplacer le marqueur
                            if (marker) {
                                marker.setLatLng(e.latlng);
                            } else {
                                marker = L.marker(e.latlng).addTo(map);
                            }
                            
                            // Mettre à jour l'affichage
                            addressDisplay.innerHTML = '🔍 Recherche de l\\'adresse en cours...';
                            addressDisplay.className = 'address-box loading';
                            confirmBtn.disabled = true;
                            confirmBtn.textContent = '⏳ Chargement...';
                            
                            // Appel à l'API Nominatim
                            var url = 'https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&accept-language=fr';
                            
                            fetch(url, {
                                headers: {
                                    'User-Agent': 'UniMind Application/1.0'
                                }
                            })
                            .then(function(response) {
                                if (!response.ok) throw new Error('HTTP ' + response.status);
                                return response.json();
                            })
                            .then(function(data) {
                                if (data && data.display_name) {
                                    currentAddress = data.display_name;
                                    addressDisplay.innerHTML = '📍 ' + currentAddress;
                                    addressDisplay.className = 'address-box success';
                                    confirmBtn.disabled = false;
                                    confirmBtn.textContent = '✅ Confirmer cette adresse';
                                } else {
                                    throw new Error('Adresse non trouvée');
                                }
                            })
                            .catch(function(error) {
                                console.error('Erreur:', error);
                                currentAddress = '';
                                addressDisplay.innerHTML = '❌ ' + error.message + '. Veuillez réessayer.';
                                addressDisplay.className = 'address-box error';
                                confirmBtn.disabled = true;
                                confirmBtn.textContent = '❌ Erreur, réessayez';
                            });
                        });
                        
                        // Confirmation de l'adresse
                        confirmBtn.onclick = function() {
                            if (currentAddress) {
                                alert(currentAddress);
                            }
                        };
                        
                        // Ajuster la taille après chargement
                        setTimeout(function() {
                            map.invalidateSize();
                        }, 100);
                        
                    } catch(e) {
                        document.getElementById('addressDisplay').innerHTML = 'Erreur de chargement de la carte: ' + e.message;
                    }
                });
            </script>
        </body>
        </html>
    """;

        // Afficher un message de chargement
        webView.getEngine().loadContent(html);

        // Capturer l'adresse via alert
        webView.getEngine().setOnAlert(event -> {
            String address = event.getData();
            if (address != null && !address.isEmpty()) {
                Platform.runLater(() -> {
                    adresseField.setText(address);
                    mapStage.close();
                });
            }
        });

        // Afficher le chargement dans la console
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("✅ Carte chargée");
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("❌ Échec chargement");
            }
        });

        Scene scene = new Scene(webView, 900, 700);
        mapStage.setScene(scene);
        mapStage.show();
    }

    @FXML
    public void selectionnerEtudiant() {
        ouvrirFormulaire("Etudiant");
    }

    @FXML
    public void selectionnerPsy() {
        ouvrirFormulaire("Psychologue");
    }

    @FXML
    public void selectionnerResponsable() {
        ouvrirFormulaire("Responsable Etudiant");
    }

    private void ouvrirFormulaire(String role) {
        roleSelectionne = role;

        switch (role) {
            case "Etudiant" -> labelTitreRole.setText("Inscription - Étudiant");
            case "Psychologue" -> labelTitreRole.setText("Inscription - Psychologue");
            case "Responsable Etudiant" -> labelTitreRole.setText("Inscription - Responsable Étudiant");
        }

        afficherPanneauRole(role);
        etapeRole.setVisible(false);
        etapeRole.setManaged(false);
        etapeFormulaire.setVisible(true);
        etapeFormulaire.setManaged(true);
    }

    @FXML
    public void retourChoixRole() {
        etapeFormulaire.setVisible(false);
        etapeFormulaire.setManaged(false);
        etapeRole.setVisible(true);
        etapeRole.setManaged(true);
        clearAllErrors();
        hideGlobal();
    }

    private void afficherPanneauRole(String role) {
        panneauEtudiant.setVisible(false);
        panneauEtudiant.setManaged(false);
        panneauPsy.setVisible(false);
        panneauPsy.setManaged(false);
        panneauResponsable.setVisible(false);
        panneauResponsable.setManaged(false);

        switch (role) {
            case "Etudiant" -> {
                panneauEtudiant.setVisible(true);
                panneauEtudiant.setManaged(true);
            }
            case "Psychologue" -> {
                panneauPsy.setVisible(true);
                panneauPsy.setManaged(true);
            }
            case "Responsable Etudiant" -> {
                panneauResponsable.setVisible(true);
                panneauResponsable.setManaged(true);
            }
        }
    }

    private void showErr(Label l, String msg) {
        if (l == null) return;
        l.setText("⚠ " + msg);
        l.setVisible(true);
        l.setManaged(true);
    }

    private void hideErr(Label l) {
        if (l == null) return;
        l.setText("");
        l.setVisible(false);
        l.setManaged(false);
    }

    private void markRed(TextField f) {
        if (f == null) return;
        f.setStyle("-fx-border-color: #DC2626; -fx-border-width: 1.5;");
    }

    private void markRedPwd(PasswordField f) {
        if (f == null) return;
        f.setStyle("-fx-border-color: #DC2626; -fx-border-width: 1.5;");
    }

    private void resetRed(TextField f) {
        if (f == null) return;
        f.setStyle("-fx-border-color: #DDD6FE; -fx-border-width: 1.5;");
    }

    private void resetRedPwd(PasswordField f) {
        if (f == null) return;
        f.setStyle("-fx-border-color: #DDD6FE; -fx-border-width: 1.5;");
    }

    private void clearAllErrors() {
        hideErr(errNom); hideErr(errPrenom); hideErr(errEmail); hideErr(errCin);
        hideErr(errPassword); hideErr(errConfirm); hideErr(errConditions);
        hideErr(errIdentifiant); hideErr(errEtablissement);
        hideErr(errSpecialite); hideErr(errAdresse); hideErr(errTelephone);
        hideErr(errPoste); hideErr(errEtablissementResp);
        resetRed(nomField); resetRed(prenomField);
        resetRed(emailField); resetRed(cinField);
        resetRedPwd(passwordField); resetRedPwd(confirmPasswordField);
        resetRed(identifiantField); resetRed(etablissementField);
        resetRed(specialiteField); resetRed(adresseField); resetRed(telephoneField);
        resetRed(posteField); resetRed(etablissementRespField);
    }

    private void showGlobalError(String msg) {
        if (messageGlobal == null) return;
        messageGlobal.setText("⚠ " + msg);
        messageGlobal.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-background-color: #FEF2F2; -fx-background-radius: 8; -fx-padding: 10 14; -fx-border-color: #FECACA; -fx-border-radius: 8;");
        messageGlobal.setVisible(true);
        messageGlobal.setManaged(true);
    }

    private void showGlobalSuccess(String msg) {
        if (messageGlobal == null) return;
        messageGlobal.setText("✓ " + msg);
        messageGlobal.setStyle("-fx-text-fill: #065F46; -fx-font-size: 12px; -fx-background-color: #ECFDF5; -fx-background-radius: 8; -fx-padding: 10 14; -fx-border-color: #6EE7B7; -fx-border-radius: 8;");
        messageGlobal.setVisible(true);
        messageGlobal.setManaged(true);
    }

    private void hideGlobal() {
        if (messageGlobal == null) return;
        messageGlobal.setText("");
        messageGlobal.setVisible(false);
        messageGlobal.setManaged(false);
    }

    public void prefillWithGoogleInfo(String email, String firstName, String lastName) {
        if (emailField != null) {
            emailField.setText(email);
            emailField.setEditable(false);
            emailField.setStyle("-fx-background-color: #F3F4F6; -fx-opacity: 0.7;");
        }
        if (prenomField != null && firstName != null) {
            prenomField.setText(firstName);
        }
        if (nomField != null && lastName != null) {
            nomField.setText(lastName);
        }
    }

    @FXML
    public void sInscrire() {
        clearAllErrors();
        hideGlobal();

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String cin = cinField.getText().trim();
        String pwd = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        boolean conditions = conditionsCheck != null && conditionsCheck.isSelected();

        boolean valid = true;

        // Validation Nom
        if (!ValidationUtils.isNonVide(nom)) {
            showErr(errNom, "Le nom est obligatoire.");
            markRed(nomField);
            valid = false;
        } else if (!ValidationUtils.isNomValide(nom)) {
            showErr(errNom, ValidationUtils.messageNom());
            markRed(nomField);
            valid = false;
        }

        // Validation Prénom
        if (!ValidationUtils.isNonVide(prenom)) {
            showErr(errPrenom, "Le prénom est obligatoire.");
            markRed(prenomField);
            valid = false;
        } else if (!ValidationUtils.isNomValide(prenom)) {
            showErr(errPrenom, ValidationUtils.messageNom());
            markRed(prenomField);
            valid = false;
        }

        // Validation Email
        if (!ValidationUtils.isNonVide(email)) {
            showErr(errEmail, "L'email est obligatoire.");
            markRed(emailField);
            valid = false;
        } else if (!ValidationUtils.isEmailValide(email)) {
            showErr(errEmail, ValidationUtils.messageEmail());
            markRed(emailField);
            valid = false;
        } else {
            try {
                if (emailExists(email)) {
                    showErr(errEmail, "Cet email est déjà utilisé par un autre compte.");
                    markRed(emailField);
                    valid = false;
                }
            } catch (Exception ignored) {}
        }

        // Validation CIN
        if (!ValidationUtils.isNonVide(cin)) {
            showErr(errCin, "Le CIN est obligatoire.");
            markRed(cinField);
            valid = false;
        } else if (!ValidationUtils.isCinValide(cin)) {
            showErr(errCin, ValidationUtils.messageCin());
            markRed(cinField);
            valid = false;
        } else {
            try {
                if (cinExists(cin)) {
                    showErr(errCin, "Ce CIN est déjà associé à un compte existant.");
                    markRed(cinField);
                    valid = false;
                }
            } catch (Exception ignored) {}
        }

        // Validation Mot de passe
        if (!ValidationUtils.isNonVide(pwd)) {
            showErr(errPassword, "Le mot de passe est obligatoire.");
            markRedPwd(passwordField);
            valid = false;
        } else if (!ValidationUtils.isPasswordValide(pwd)) {
            showErr(errPassword, ValidationUtils.messagePassword());
            markRedPwd(passwordField);
            valid = false;
        }

        // Validation Confirmation
        if (!ValidationUtils.isNonVide(confirm)) {
            showErr(errConfirm, "La confirmation est obligatoire.");
            markRedPwd(confirmPasswordField);
            valid = false;
        } else if (!ValidationUtils.isPasswordConfirme(pwd, confirm)) {
            showErr(errConfirm, ValidationUtils.messagePasswordConfirm());
            markRedPwd(confirmPasswordField);
            valid = false;
        }

        // Validation Conditions
        if (!conditions) {
            showErr(errConditions, "Vous devez accepter les conditions d'utilisation.");
            valid = false;
        }

        // Validation champs spécifiques
        switch (roleSelectionne) {
            case "Etudiant" -> {
                String etablissement = etablissementField != null ? etablissementField.getText().trim() : "";
                if (!ValidationUtils.isNonVide(etablissement)) {
                    showErr(errEtablissement, "L'établissement est obligatoire.");
                    markRed(etablissementField);
                    valid = false;
                }
            }
            case "Psychologue" -> {
                String specialite = specialiteField != null ? specialiteField.getText().trim() : "";
                String adresse = adresseField != null ? adresseField.getText().trim() : "";
                String telephone = telephoneField != null ? telephoneField.getText().trim() : "";
                if (!ValidationUtils.isNonVide(specialite)) {
                    showErr(errSpecialite, "La spécialité est obligatoire.");
                    markRed(specialiteField);
                    valid = false;
                }
                if (!ValidationUtils.isNonVide(adresse)) {
                    showErr(errAdresse, "L'adresse est obligatoire.");
                    markRed(adresseField);
                    valid = false;
                }
                if (!ValidationUtils.isNonVide(telephone)) {
                    showErr(errTelephone, "Le téléphone est obligatoire.");
                    markRed(telephoneField);
                    valid = false;
                } else if (!ValidationUtils.isTelephoneValide(telephone)) {
                    showErr(errTelephone, ValidationUtils.messageTelephone());
                    markRed(telephoneField);
                    valid = false;
                }
            }
            case "Responsable Etudiant" -> {
                String poste = posteField != null ? posteField.getText().trim() : "";
                String etablissement = etablissementRespField != null ? etablissementRespField.getText().trim() : "";
                if (!ValidationUtils.isNonVide(poste)) {
                    showErr(errPoste, "Le poste est obligatoire.");
                    markRed(posteField);
                    valid = false;
                }
                if (!ValidationUtils.isNonVide(etablissement)) {
                    showErr(errEtablissementResp, "L'établissement est obligatoire.");
                    markRed(etablissementRespField);
                    valid = false;
                }
            }
        }

        if (!valid) return;

        // Enregistrement
        try {
            int userId = -1;
            String userName = "";

            switch (roleSelectionne) {
                case "Etudiant" -> {
                    String identifiant = identifiantField != null ? identifiantField.getText().trim() : generateStudentId();
                    String etablissement = etablissementField.getText().trim();
                    userId = registerStudent(nom, prenom, email, pwd, cin, identifiant, etablissement);
                    userName = nom + " " + prenom;
                }
                case "Psychologue" -> {
                    String specialite = specialiteField.getText().trim();
                    String adresse = adresseField.getText().trim();
                    String telephone = telephoneField.getText().trim();
                    userId = registerPsychologue(nom, prenom, email, pwd, cin, specialite, adresse, telephone);
                    userName = nom + " " + prenom;
                }
                case "Responsable Etudiant" -> {
                    String poste = posteField.getText().trim();
                    String etablissement = etablissementRespField.getText().trim();
                    userId = registerResponsable(nom, prenom, email, pwd, cin, poste, etablissement);
                    userName = nom + " " + prenom;
                }
            }

            if (userId > 0) {
                try (Connection conn = MyDataBase_Unimind.getInstance().getConnection()) {
                    // Générer un token d'activation
                    String activationToken = emailService.generateToken();

                    // Stocker le token dans verification_token (pas activation_token)
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE user SET verification_token = ?, is_verified = 0 WHERE user_id = ?");
                    ps.setString(1, activationToken);
                    ps.setInt(2, userId);
                    ps.executeUpdate();

                    // Envoyer email d'activation
                    emailService.sendActivationEmail(email, userName, userId, activationToken);

                    // Notifier l'admin
                    emailService.sendNewRegistrationToAdmin(userName, email, roleSelectionne);
                }
                showGlobalSuccess("Inscription réussie ! Vérifiez votre email pour activer votre compte.");

                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                pause.setOnFinished(e -> allerConnexion());
                pause.play();
            }
        } catch (SQLException e) {
            showGlobalError("Erreur lors de l'inscription : " + e.getMessage());
        } catch (Exception e) {
            showGlobalError("Erreur : " + e.getMessage());
        }
    }

    private boolean emailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean cinExists(String cin) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE cin = ?";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, cin);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private int registerStudent(String nom, String prenom, String email, String pwd,
                                String cin, String identifiant, String etablissement) throws SQLException {
        String hashedPassword = org.example.utils.PasswordUtils.hasher(pwd);
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at, identifiant, nom_etablissement) " +
                "VALUES (?, ?, ?, ?, ?, 'Etudiant', 'en_attente', 0, 0, NOW(), ?, ?)";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, hashedPassword);
            ps.setString(5, cin);
            ps.setString(6, identifiant);
            ps.setString(7, etablissement);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    private int registerPsychologue(String nom, String prenom, String email, String pwd,
                                    String cin, String specialite, String adresse, String telephone) throws SQLException {
        String hashedPassword = org.example.utils.PasswordUtils.hasher(pwd);
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at, specialite, adresse, telephone) " +
                "VALUES (?, ?, ?, ?, ?, 'Psychologue', 'en_attente', 0, 0, NOW(), ?, ?, ?)";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, hashedPassword);
            ps.setString(5, cin);
            ps.setString(6, specialite);
            ps.setString(7, adresse);
            ps.setString(8, telephone);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    private int registerResponsable(String nom, String prenom, String email, String pwd,
                                    String cin, String poste, String etablissement) throws SQLException {
        String hashedPassword = org.example.utils.PasswordUtils.hasher(pwd);
        String query = "INSERT INTO user (nom, prenom, email, password, cin, role, statut, is_active, is_verified, created_at, poste, etablissement) " +
                "VALUES (?, ?, ?, ?, ?, 'Responsable Etudiant', 'en_attente', 0, 0, NOW(), ?, ?)";
        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, hashedPassword);
            ps.setString(5, cin);
            ps.setString(6, poste);
            ps.setString(7, etablissement);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    @FXML
    public void allerConnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Stage stage = new Stage();
            stage.setTitle("UniMind — Connexion");
            stage.setScene(new Scene(loader.load(), 900, 600));
            stage.show();
            ((Stage) nomField.getScene().getWindow()).close();
        } catch (Exception e) {
            showGlobalError("Erreur de navigation : " + e.getMessage());
        }
    }
}