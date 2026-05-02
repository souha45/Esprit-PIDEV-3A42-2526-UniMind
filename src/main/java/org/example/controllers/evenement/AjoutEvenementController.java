package org.example.controllers.evenement;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.example.entities.Evenement;
import org.example.enums.Role;
import org.example.enums.StatutEvenement;
import org.example.enums.TypeEvenement;
import org.example.services.EvenementService;
import org.example.services.evenement.EventAiGeneratorService;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AjoutEvenementController {

    @FXML
    private TextField txtTitre;
    @FXML
    private TextArea txtDescription;
    @FXML
    private ComboBox<TypeEvenement> comboType;
    @FXML
    private ComboBox<StatutEvenement> comboStatut;
    @FXML
    private DatePicker dateDebut;
    @FXML
    private DatePicker dateFin;
    @FXML
    private DatePicker dateLimiteInscription;
    @FXML
    private TextField txtHeureDebut;
    @FXML
    private TextField txtHeureFin;
    @FXML
    private TextField txtHeureLimite;
    @FXML
    private TextField txtLieu;
    @FXML
    private TextField txtLatitude;
    @FXML
    private TextField txtLongitude;
    @FXML
    private TextField txtCapacite;
    @FXML
    private VBox vboxOrganisateur;
    @FXML
    private ComboBox<OrganisateurInfo> comboOrganisateur;
    @FXML
    private TextField txtImage;

    @FXML
    private Label lblErreurTitre;
    @FXML
    private Label lblErreurUnicite;
    @FXML
    private Label lblErreurDateDebut;
    @FXML
    private Label lblErreurDateDebutPasse;
    @FXML
    private Label lblErreurDateFin;
    @FXML
    private Label lblErreurDates;
    @FXML
    private Label lblErreurDateLimiteIncoherence;
    @FXML
    private Label lblErreurDateLimite;
    @FXML
    private Label lblErreurHeureDebut;
    @FXML
    private Label lblErreurHeureFin;
    @FXML
    private Label lblErreurHeureLimite;
    @FXML
    private Label lblErreurLieu;
    @FXML
    private Label lblErreurCapacite;
    @FXML
    private Label lblSucces;

    // Champs IA
    @FXML
    private TextField txtTopicAi;
    @FXML
    private CheckBox chkGenererDescription;
    @FXML
    private ComboBox<String> comboTitresIA;
    @FXML
    private VBox vboxResultatsIA;
    @FXML
    private Label lblErreurIA;
    @FXML
    private ProgressIndicator progressIA;

    @FXML private VBox vboxImagePreview;
    @FXML private javafx.scene.image.ImageView imgPreview1;
    private String currentAiImage;

    private final EvenementService evenementService = new EvenementService();
    private final EventAiGeneratorService aiGeneratorService = new EventAiGeneratorService();
    private boolean isAdmin = false;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Vérifier si l'utilisateur est admin
        isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        // Initialiser les ComboBox avec les valeurs des enums
        comboType.setItems(FXCollections.observableArrayList(TypeEvenement.values()));
        comboStatut.setItems(FXCollections.observableArrayList(StatutEvenement.values()));

        // Charger les organisateurs depuis la BDD (seulement pour l'admin)
        if (isAdmin) {
            chargerOrganisateurs();
        } else {
            // Cacher le champ organisateur pour les responsables
            vboxOrganisateur.setVisible(false);
            vboxOrganisateur.setManaged(false);
        }

        // Valeurs par défaut
        comboStatut.setValue(StatutEvenement.A_VENIR);
        comboType.setValue(TypeEvenement.ATELIER);

        // Valeurs par défaut pour les heures (cohérent avec un calendrier d'événement)
        if (txtHeureDebut != null) {
            txtHeureDebut.setText("09:00");
        }
        if (txtHeureFin != null) {
            txtHeureFin.setText("17:00");
        }
        if (txtHeureLimite != null) {
            txtHeureLimite.setText("");
        }

        // Forcer la couleur du texte en noir pour tous les champs
        appliquerCouleurTexteNoir();
    }

    private void appliquerCouleurTexteNoir() {
        // Appliquer le style inline pour forcer le texte noir
        if (txtTitre != null) txtTitre.setStyle("-fx-text-fill: #000000;");
        if (txtDescription != null) txtDescription.setStyle("-fx-text-fill: #000000;");
        if (txtHeureDebut != null) txtHeureDebut.setStyle("-fx-text-fill: #000000;");
        if (txtHeureFin != null) txtHeureFin.setStyle("-fx-text-fill: #000000;");
        if (txtHeureLimite != null) txtHeureLimite.setStyle("-fx-text-fill: #000000;");
        if (txtLieu != null) txtLieu.setStyle("-fx-text-fill: #000000;");
        if (txtCapacite != null) txtCapacite.setStyle("-fx-text-fill: #000000;");
        if (txtImage != null) txtImage.setStyle("-fx-text-fill: #000000;");
        if (dateDebut != null) dateDebut.setStyle("-fx-text-fill: #000000;");
        if (dateFin != null) dateFin.setStyle("-fx-text-fill: #000000;");
        if (dateLimiteInscription != null) dateLimiteInscription.setStyle("-fx-text-fill: #000000;");
        if (comboType != null) comboType.setStyle("-fx-text-fill: #000000;");
        if (comboStatut != null) comboStatut.setStyle("-fx-text-fill: #000000;");
        if (comboOrganisateur != null) comboOrganisateur.setStyle("-fx-text-fill: #000000;");
    }

    private void chargerOrganisateurs() {
        try {
            String sql = "SELECT user_id, nom, prenom FROM user WHERE role = 'Responsable Etudiant' ORDER BY nom, prenom";
            java.util.List<OrganisateurInfo> organisateurs = new ArrayList<>();

            try (PreparedStatement ps = MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    String fullName = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                    organisateurs.add(new OrganisateurInfo(userId, fullName.trim()));
                }
            }

            comboOrganisateur.setItems(FXCollections.observableArrayList(organisateurs));

            // Sélectionner le premier organisateur par défaut
            if (!organisateurs.isEmpty()) {
                comboOrganisateur.setValue(organisateurs.get(0));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des organisateurs: " + e.getMessage());
        }
    }

    @FXML
    private void enregistrerEvenement(ActionEvent event) {
        // Cacher les messages d'erreur
        cacherErreurs();
        lblSucces.setVisible(false);

        // Valider les champs
        boolean valide = validerFormulaire();

        if (!valide) {
            return;
        }

        try {
            // Créer l'événement
            Evenement evenement = creerEvenement();

            // Enregistrer dans la base
            evenementService.ajouter(evenement);

            // Afficher le message de succès
            lblSucces.setVisible(true);

            // Rediriger vers la page de gestion des événements après un court délai
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1),
                    ae -> {
                        try {
                            NavigationContext.loadContentInCenter("/evenement/GestionEvenement.fxml");
                        } catch (IOException e) {
                            System.err.println("Erreur lors de la navigation: " + e.getMessage());
                        }
                    }
            ));
            timeline.play();

        } catch (SQLException e) {
            afficherErreur("Erreur lors de l'enregistrement : " + e.getMessage());
        } catch (NumberFormatException e) {
            afficherErreur("La capacité doit être un nombre valide");
        }
    }

    @FXML
    private void reinitialiserFormulaire() {
        txtTitre.clear();
        txtDescription.clear();
        txtLieu.clear();
        txtLatitude.clear();
        txtLongitude.clear();
        txtCapacite.clear();
        txtImage.clear();
        if (isAdmin && comboOrganisateur != null && !comboOrganisateur.getItems().isEmpty()) {
            comboOrganisateur.setValue(comboOrganisateur.getItems().get(0));
        }
        dateDebut.setValue(null);
        dateFin.setValue(null);
        dateLimiteInscription.setValue(null);
        if (txtHeureDebut != null) {
            txtHeureDebut.setText("09:00");
        }
        if (txtHeureFin != null) {
            txtHeureFin.setText("17:00");
        }
        if (txtHeureLimite != null) {
            txtHeureLimite.setText("");
        }
        comboType.setValue(TypeEvenement.ATELIER);
        comboStatut.setValue(StatutEvenement.A_VENIR);
        cacherErreurs();
        lblSucces.setVisible(false);
    }

    @FXML
    private void retour(ActionEvent event) throws IOException {
        NavigationContext.loadContentInCenter("/evenement/GestionEvenement.fxml");
    }

    private boolean validerFormulaire() {
        List<String> erreurs = new ArrayList<>();

        // Valider titre
        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            lblErreurTitre.setVisible(true);
            erreurs.add("Titre : obligatoire");
        }

        // Valider unicité (titre + date de début)
        LocalDate dateDebutUnicite = dateDebut.getValue();
        LocalTime heureDebutUnicite = parseHeure(txtHeureDebut != null ? txtHeureDebut.getText() : null);
        if (dateDebutUnicite != null && heureDebutUnicite != null) {
            try {
                Timestamp dateDebutTimestamp = Timestamp.valueOf(LocalDateTime.of(dateDebutUnicite, heureDebutUnicite));
                if (evenementService.verifierUnicite(txtTitre.getText().trim(), dateDebutTimestamp, null)) {
                    lblErreurUnicite.setVisible(true);
                    erreurs.add("Unicité : un événement avec ce titre et cette date existe déjà");
                }
            } catch (SQLException e) {
                erreurs.add("Erreur lors de la vérification d'unicité : " + e.getMessage());
            }
        }

        // Valider lieu
        if (txtLieu.getText() == null || txtLieu.getText().trim().isEmpty()) {
            lblErreurLieu.setVisible(true);
            erreurs.add("Lieu : obligatoire");
        }

        // Valider capacité
        try {
            int capacite = Integer.parseInt(txtCapacite.getText());
            if (capacite <= 0) {
                lblErreurCapacite.setVisible(true);
                erreurs.add("Capacité maximale : doit être > 0");
            }
        } catch (NumberFormatException e) {
            lblErreurCapacite.setVisible(true);
            erreurs.add("Capacité maximale : doit être un nombre");
        }

        // Valider organisateur (seulement pour l'admin)
        if (isAdmin) {
            if (comboOrganisateur.getValue() == null) {
                erreurs.add("Organisateur : obligatoire");
            }
        }

        // Valider dates + heures
        LocalDate dateDebutValue = dateDebut.getValue();
        LocalDate dateFinValue = dateFin.getValue();

        if (dateDebutValue == null) {
            lblErreurDateDebut.setVisible(true);
            erreurs.add("Date de début : obligatoire");
        }
        if (dateFinValue == null) {
            lblErreurDateFin.setVisible(true);
            erreurs.add("Date de fin : obligatoire");
        }

        LocalTime heureDebut = parseHeure(txtHeureDebut != null ? txtHeureDebut.getText() : null);
        if (heureDebut == null) {
            if (lblErreurHeureDebut != null) {
                lblErreurHeureDebut.setVisible(true);
            }
            erreurs.add("Heure de début : format HH:mm");
        }

        // Vérifier que la date de début n'est pas dans le passé
        if (dateDebutValue != null && heureDebut != null) {
            LocalDateTime debut = LocalDateTime.of(dateDebutValue, heureDebut);
            if (debut.isBefore(LocalDateTime.now())) {
                if (lblErreurDateDebutPasse != null) {
                    lblErreurDateDebutPasse.setVisible(true);
                }
                erreurs.add("Date de début : ne peut pas être dans le passé");
            }
        }

        LocalTime heureFin = parseHeure(txtHeureFin != null ? txtHeureFin.getText() : null);
        if (heureFin == null) {
            if (lblErreurHeureFin != null) {
                lblErreurHeureFin.setVisible(true);
            }
            erreurs.add("Heure de fin : format HH:mm");
        }

        if (dateDebutValue != null && dateFinValue != null && heureDebut != null && heureFin != null) {
            LocalDateTime debut = LocalDateTime.of(dateDebutValue, heureDebut);
            LocalDateTime fin = LocalDateTime.of(dateFinValue, heureFin);
            if (!fin.isAfter(debut)) {
                lblErreurDates.setVisible(true);
                erreurs.add("Dates : fin doit être après début");
            }
        }

        // Valider date limite d'inscription (optionnelle)
        LocalDate dateLimiteValue = dateLimiteInscription.getValue();
        String heureLimiteStr = txtHeureLimite != null ? txtHeureLimite.getText() : null;

        // Si l'utilisateur saisit une heure limite sans date limite (ou inversement), on considère invalide
        boolean limiteDateRenseignee = dateLimiteValue != null;
        boolean limiteHeureRenseignee = heureLimiteStr != null && !heureLimiteStr.trim().isEmpty();
        LocalTime heureLimite = null;

        if (limiteHeureRenseignee) {
            heureLimite = parseHeure(heureLimiteStr);
            if (heureLimite == null) {
                if (lblErreurHeureLimite != null) {
                    lblErreurHeureLimite.setVisible(true);
                }
                erreurs.add("Heure limite : format HH:mm");
            }
        }

        if (limiteDateRenseignee ^ limiteHeureRenseignee) {
            // xor => un seul des deux est renseigné
            if (lblErreurDateLimiteIncoherence != null) {
                lblErreurDateLimiteIncoherence.setVisible(true);
            }
            erreurs.add("Date limite : renseigne la date ET l'heure, ou laisse les deux vides");
        }

        if (dateLimiteValue != null && dateDebutValue != null && heureLimite != null && heureDebut != null) {
            LocalDateTime limite = LocalDateTime.of(dateLimiteValue, heureLimite);
            LocalDateTime debut = LocalDateTime.of(dateDebutValue, heureDebut);
            if (!limite.isBefore(debut)) {
                lblErreurDateLimite.setVisible(true);
                erreurs.add("Date limite : doit être avant la date/heure de début");
            }
        }

        if (!erreurs.isEmpty()) {
            String message = "Erreurs détectées :\n" + String.join("\n", erreurs);
            afficherErreur(message);
            return false;
        }

        return true;
    }

    private Evenement creerEvenement() throws NumberFormatException {
        String titre = txtTitre.getText().trim();
        String description = txtDescription.getText().trim();
        TypeEvenement type = comboType.getValue();
        StatutEvenement statut = comboStatut.getValue();
        String lieu = txtLieu.getText().trim();
        int capacite = Integer.parseInt(txtCapacite.getText());

        // Récupérer l'ID de l'organisateur
        int organisateurId;
        if (isAdmin) {
            // Pour l'admin, utiliser l'organisateur sélectionné
            OrganisateurInfo organisateur = comboOrganisateur.getValue();
            if (organisateur == null) {
                throw new NumberFormatException("Veuillez sélectionner un organisateur");
            }
            organisateurId = organisateur.getUserId();
        } else {
            // Pour le responsable, utiliser l'ID de l'utilisateur connecté
            organisateurId = SessionManager.getInstance().getCurrentUserId()
                    .orElseThrow(() -> new NumberFormatException("Utilisateur non connecté"));
        }

        // Convertir les dates + heures en Timestamp
        LocalDate dateDebutValue = dateDebut.getValue();
        LocalDate dateFinValue = dateFin.getValue();
        LocalDate dateLimiteValue = dateLimiteInscription.getValue();

        LocalTime heureDebut = parseHeure(txtHeureDebut != null ? txtHeureDebut.getText() : null);
        LocalTime heureFin = parseHeure(txtHeureFin != null ? txtHeureFin.getText() : null);
        LocalTime heureLimite = null;
        String heureLimiteStr = txtHeureLimite != null ? txtHeureLimite.getText() : null;
        if (heureLimiteStr != null && !heureLimiteStr.trim().isEmpty()) {
            heureLimite = parseHeure(heureLimiteStr);
        }

        Timestamp dateDebutTimestamp = Timestamp.valueOf(LocalDateTime.of(dateDebutValue, heureDebut));
        Timestamp dateFinTimestamp = Timestamp.valueOf(LocalDateTime.of(dateFinValue, heureFin));
        Timestamp dateLimiteTimestamp = (dateLimiteValue != null && heureLimite != null) ?
                Timestamp.valueOf(LocalDateTime.of(dateLimiteValue, heureLimite)) : null;

        // Créer l'événement
        String image = txtImage.getText() != null && !txtImage.getText().trim().isEmpty() ? txtImage.getText().trim() : null;
        
        // Récupérer les coordonnées si disponibles
        Double latitude = null;
        Double longitude = null;
        System.out.println("=== DEBUG creerEvenement ===");
        System.out.println("txtLatitude field: " + (txtLatitude != null ? txtLatitude.getText() : "FIELD IS NULL"));
        System.out.println("txtLongitude field: " + (txtLongitude != null ? txtLongitude.getText() : "FIELD IS NULL"));
        if (txtLatitude != null && txtLatitude.getText() != null && !txtLatitude.getText().trim().isEmpty()) {
            try {
                latitude = Double.parseDouble(txtLatitude.getText().trim());
                System.out.println("Latitude parsée: " + latitude);
            } catch (NumberFormatException e) {
                System.out.println("ERREUR parsing latitude: " + txtLatitude.getText());
                latitude = null;
            }
        }
        if (txtLongitude != null && txtLongitude.getText() != null && !txtLongitude.getText().trim().isEmpty()) {
            try {
                longitude = Double.parseDouble(txtLongitude.getText().trim());
                System.out.println("Longitude parsée: " + longitude);
            } catch (NumberFormatException e) {
                System.out.println("ERREUR parsing longitude: " + txtLongitude.getText());
                longitude = null;
            }
        }
        System.out.println("Latitude finale: " + latitude + ", Longitude finale: " + longitude);
        System.out.println("Lieu: " + lieu);
        
        return new Evenement(
                titre,
                description.isEmpty() ? null : description,
                type,
                dateDebutTimestamp,
                dateFinTimestamp,
                lieu,
                capacite,
                0, // nombreInscrits initialisé à 0
                statut,
                dateLimiteTimestamp,
                organisateurId,
                image, // image
                latitude,
                longitude
        );
    }

    @FXML
    private void parcourirImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog((Stage) ((Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Copier l'image dans le dossier XAMPP
                String dossierXAMPP = "D:\\xampp\\htdocs\\uploadsEvent\\evenements\\";

                // Remplacer les espaces par des tirets dans le nom du fichier
                String originalName = selectedFile.getName();
                String safeName = originalName.replaceAll(" ", "-").replaceAll("'", "-");

                Path source = selectedFile.toPath();
                Path destination = Path.of(dossierXAMPP + safeName);

                // Copier le fichier
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

                // Afficher le nom du fichier dans le TextField
                txtImage.setText(safeName);

                afficherAlerte("Succès", "Image copiée dans le dossier XAMPP : " + safeName);
            } catch (IOException e) {
                afficherAlerte("Erreur", "Impossible de copier l'image : " + e.getMessage());
            }
        }
    }

    @FXML
    private void ouvrirCarteLieu(ActionEvent event) {
        // Créer une nouvelle fenêtre pour la carte
        Stage mapStage = new Stage();
        mapStage.setTitle("Sélectionner le lieu sur la carte");
        mapStage.setWidth(900);
        mapStage.setHeight(600);

        // Créer un WebView pour afficher la carte
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Charger le fichier HTML de la carte
        String mapHtmlPath = getClass().getResource("/evenement/EventOpenStreetMap.html").toExternalForm();
        webEngine.load(mapHtmlPath);

        // Créer le bridge Java-JavaScript
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("Page HTML chargée avec succès");
                
                // Attendre un peu que le DOM soit complètement chargé
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(500); // Attendre 500ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    // Injecter le bridge JavaScript
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    EventMapJavaBridge bridge = new EventMapJavaBridge(mapStage);
                    window.setMember("javaBridge", bridge);
                    System.out.println("Bridge Java injecté: " + bridge);
                    
                    // Vérifier que le bridge est accessible
                    try {
                        Object test = webEngine.executeScript("typeof window.javaBridge");
                        System.out.println("Type de javaBridge: " + test);
                        
                        Object testMethod = webEngine.executeScript("typeof window.javaBridge.onLocationSelected");
                        System.out.println("Type de onLocationSelected: " + testMethod);
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la vérification du bridge: " + e.getMessage());
                    }
                });
            }
        });

        // Quand le WebView est redimensionné, Leaflet doit recalculer la taille
        webView.widthProperty().addListener((obsW, oldW, newW) -> {
            javafx.application.Platform.runLater(() -> {
                try {
                    webEngine.executeScript("if(typeof fixSize==='function')fixSize();");
                } catch (Exception ignored) {}
            });
        });
        webView.heightProperty().addListener((obsH, oldH, newH) -> {
            javafx.application.Platform.runLater(() -> {
                try {
                    webEngine.executeScript("if(typeof fixSize==='function')fixSize();");
                } catch (Exception ignored) {}
            });
        });

        // Créer la scène et afficher la fenêtre
        javafx.scene.Scene scene = new javafx.scene.Scene(webView);
        mapStage.setScene(scene);
        mapStage.show();
    }

    /**
     * Bridge Java pour communiquer avec JavaScript
     * Doit être public pour être accessible depuis JavaScript
     */
    public class EventMapJavaBridge {
        private final Stage mapStage;
        
        public EventMapJavaBridge(Stage mapStage) {
            this.mapStage = mapStage;
        }
        
        /**
         * Méthode appelée depuis JavaScript
         * Doit être publique
         */
        public void onLocationSelected(String address, double lat, double lng) {
            System.out.println("=== Bridge Java appelé ===");
            System.out.println("Adresse: " + address);
            System.out.println("Latitude: " + lat);
            System.out.println("Longitude: " + lng);
            System.out.println("txtLieu est null: " + (txtLieu == null));
            System.out.println("txtLatitude est null: " + (txtLatitude == null));
            System.out.println("txtLongitude est null: " + (txtLongitude == null));
            
            // Mettre à jour les champs du formulaire
            javafx.application.Platform.runLater(() -> {
                System.out.println("Dans Platform.runLater");
                System.out.println("txtLieu avant: " + (txtLieu != null ? txtLieu.getText() : "null"));
                
                if (txtLieu != null) {
                    txtLieu.setText(address);
                    System.out.println("txtLieu après: " + txtLieu.getText());
                }
                if (txtLatitude != null) {
                    txtLatitude.setText(String.valueOf(lat));
                }
                if (txtLongitude != null) {
                    txtLongitude.setText(String.valueOf(lng));
                }
                
                System.out.println("Champs mis à jour avec succès");
                
                // Fermer la fenêtre de la carte
                if (mapStage != null) {
                    mapStage.close();
                    System.out.println("Fenêtre de carte fermée");
                }
            });
        }
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void cacherErreurs() {
        lblErreurTitre.setVisible(false);
        lblErreurUnicite.setVisible(false);
        lblErreurDateDebut.setVisible(false);
        if (lblErreurDateDebutPasse != null) {
            lblErreurDateDebutPasse.setVisible(false);
        }
        lblErreurDateFin.setVisible(false);
        lblErreurDates.setVisible(false);
        if (lblErreurDateLimiteIncoherence != null) {
            lblErreurDateLimiteIncoherence.setVisible(false);
        }
        lblErreurDateLimite.setVisible(false);
        if (lblErreurHeureDebut != null) {
            lblErreurHeureDebut.setVisible(false);
        }
        if (lblErreurHeureFin != null) {
            lblErreurHeureFin.setVisible(false);
        }
        if (lblErreurHeureLimite != null) {
            lblErreurHeureLimite.setVisible(false);
        }
        lblErreurLieu.setVisible(false);
        lblErreurCapacite.setVisible(false);
    }

    private LocalTime parseHeure(String heure) {
        if (heure == null) {
            return null;
        }
        String h = heure.trim();
        if (h.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(h, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Générer des suggestions avec l'IA
     */
    @FXML
    private void genererAvecIA() {
        String topic = txtTopicAi.getText().trim();
        if (topic.isEmpty()) {
            lblErreurIA.setText("Veuillez entrer un sujet");
            lblErreurIA.setVisible(true);
            return;
        }

        lblErreurIA.setVisible(false);
        progressIA.setVisible(true);
        vboxResultatsIA.setVisible(false);

        // Exécuter dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                String type = comboType.getValue() != null ? comboType.getValue().getDbValue() : "";
                Map<String, Object> result = aiGeneratorService.generate(type, "", topic, "", "fr");

                List<String> titles = (List<String>) result.get("titles");
                String description = (String) result.get("description");
                String imageFile = (String) result.get("image_file");

                // Mettre à jour l'UI sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    progressIA.setVisible(false);
                    comboTitresIA.setItems(FXCollections.observableArrayList(titles));
                    vboxResultatsIA.setVisible(true);

                    // Si la case est cochée, appliquer aussi la description
                    if (chkGenererDescription.isSelected() && description != null && !description.isEmpty()) {
                        txtDescription.setText(description);
                    }
                    
                    // Afficher l'image générée
                    if (imageFile != null && !imageFile.isEmpty()) {
                        currentAiImage = imageFile;
                        
                        try {
                            String path = "file:///D:/xampp/htdocs/uploadsEvent/evenements/" + imageFile;
                            if (imgPreview1 != null) {
                                imgPreview1.setImage(new javafx.scene.image.Image(path));
                            }
                            if (vboxImagePreview != null) {
                                vboxImagePreview.setVisible(true);
                                vboxImagePreview.setManaged(true);
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur chargement image IA: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressIA.setVisible(false);
                    lblErreurIA.setText("Erreur: " + e.getMessage());
                    lblErreurIA.setVisible(true);
                });
            }
        }).start();
    }

    /**
     * Appliquer le titre sélectionné depuis l'IA
     */
    @FXML
    private void appliquerTitreIA() {
        String selectedTitle = comboTitresIA.getValue();
        if (selectedTitle != null && !selectedTitle.isEmpty()) {
            txtTitre.setText(selectedTitle);
        }
    }

    @FXML
    private void choisirImage1() {
        if (currentAiImage != null && txtImage != null) {
            txtImage.setText(currentAiImage);
            afficherAlerte("Succès", "Image IA sélectionnée !");
        }
    }

    /**
     * Classe interne pour représenter un organisateur avec ID et nom
     */
    public static class OrganisateurInfo {
        private final int userId;
        private final String fullName;

        public OrganisateurInfo(int userId, String fullName) {
            this.userId = userId;
            this.fullName = fullName;
        }

        public int getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        @Override
        public String toString() {
            return fullName;
        }
    }
}
