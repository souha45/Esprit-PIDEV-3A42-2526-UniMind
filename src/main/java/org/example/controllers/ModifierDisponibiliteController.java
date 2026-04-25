package org.example.controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.example.enums.TypeConsultation;
import org.example.entities.DisponibilitePsy;
import org.example.services.DisponibilitePsyService;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;

public class ModifierDisponibiliteController {

    // ── FXML ────────────────────────────────────────────────────────
    @FXML private DatePicker datePicker;

    // Spinners heure début
    @FXML private Spinner<Integer> spinnerHeureDebut;
    @FXML private Spinner<Integer> spinnerMinuteDebut;
    @FXML private Label            lblPreviewDebut;

    // Spinners heure fin
    @FXML private Spinner<Integer> spinnerHeureFin;
    @FXML private Spinner<Integer> spinnerMinuteFin;
    @FXML private Label            lblPreviewFin;

    // Type + lieu conditionnel
    @FXML private ComboBox<String> cbTypeConsult;
    @FXML private VBox             boxLieu;
    @FXML private TextField        txtLieu;
    @FXML private Button           btnOuvrirCarte;  // ← NOUVEAU

    // Boutons
    @FXML private Button btnFermer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    // ── Labels d'erreur inline ──────────────────────────────────────
    @FXML private Label errDate;
    @FXML private Label errHeureDebut;
    @FXML private Label errHeureFin;
    @FXML private Label errType;
    @FXML private Label errLieu;

    // ── Services / données ──────────────────────────────────────────
    private DisponibilitePsyService disponibiliteService;
    private DisponibilitePsy disponibiliteAModifier;
    private Stage modalStage;

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();

        // ── Spinners : valeurs initiales et format éditable (valeur -1 = vide)
        configurerSpinner(spinnerHeureDebut,   0, 23, -1);
        configurerSpinner(spinnerMinuteDebut,  0, 59, -1);
        configurerSpinner(spinnerHeureFin,     0, 23, -1);
        configurerSpinner(spinnerMinuteFin,    0, 59, -1);

        // ── Preview live des horaires ────────────────────────────────
        spinnerHeureDebut.valueProperty().addListener((o,ov,nv)  -> mettreAJourPreviewDebut());
        spinnerMinuteDebut.valueProperty().addListener((o,ov,nv) -> mettreAJourPreviewDebut());
        spinnerHeureFin.valueProperty().addListener((o,ov,nv)    -> mettreAJourPreviewFin());
        spinnerMinuteFin.valueProperty().addListener((o,ov,nv)   -> mettreAJourPreviewFin());

        mettreAJourPreviewDebut();
        mettreAJourPreviewFin();

        // ── Effacer erreurs à la saisie ──────────────────────────────
        datePicker.valueProperty().addListener((o,ov,nv) -> cacherErreur(errDate, datePicker));
        spinnerHeureDebut.valueProperty().addListener((o,ov,nv)  -> cacherErreurSimple(errHeureDebut));
        spinnerMinuteDebut.valueProperty().addListener((o,ov,nv) -> cacherErreurSimple(errHeureDebut));
        spinnerHeureFin.valueProperty().addListener((o,ov,nv)    -> cacherErreurSimple(errHeureFin));
        spinnerMinuteFin.valueProperty().addListener((o,ov,nv)   -> cacherErreurSimple(errHeureFin));
        cbTypeConsult.valueProperty().addListener((o,ov,nv)      -> {
            cacherErreurSimple(errType);
            gererAffichageLieu(nv);
        });
        txtLieu.textProperty().addListener((o,ov,nv) -> cacherErreurSimple(errLieu));

        // ── Actions boutons ──────────────────────────────────────────
        btnEnregistrer.setOnAction(e -> enregistrerModification());
        btnAnnuler.setOnAction(e     -> fermerModal());
        btnFermer.setOnAction(e      -> fermerModal());
        btnOuvrirCarte.setOnAction(e -> ouvrirCarte());  // ← NOUVEAU

        // ── Hover ────────────────────────────────────────────────────
        btnEnregistrer.setOnMouseEntered(e ->
                btnEnregistrer.setStyle(btnEnregistrer.getStyle().replace("#6366f1","#4f46e5")));
        btnEnregistrer.setOnMouseExited(e ->
                btnEnregistrer.setStyle(btnEnregistrer.getStyle().replace("#4f46e5","#6366f1")));

        // ← NOUVEAU : Hover pour le bouton carte
        btnOuvrirCarte.setOnMouseEntered(e ->
                btnOuvrirCarte.setStyle("-fx-background-color:#4f46e5; -fx-text-fill:white; " +
                        "-fx-font-size:13px; -fx-font-weight:bold; -fx-padding:8 15; " +
                        "-fx-background-radius:8; -fx-cursor:hand;"));
        btnOuvrirCarte.setOnMouseExited(e ->
                btnOuvrirCarte.setStyle("-fx-background-color:#6366f1; -fx-text-fill:white; " +
                        "-fx-font-size:13px; -fx-font-weight:bold; -fx-padding:8 15; " +
                        "-fx-background-radius:8; -fx-cursor:hand;"));
    }

    // ── OUVERTURE DE LA CARTE (NOUVEAU) ───────────────────────────────
    private void ouvrirCarte() {
        try {
            Stage carteStage = new Stage();
            carteStage.setTitle("Sélectionner un lieu sur la carte");
            carteStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            carteStage.initOwner(btnOuvrirCarte.getScene().getWindow());

            WebView   webView   = new WebView();
            WebEngine webEngine = webView.getEngine();

            java.net.URL url = getClass().getResource("/carte.html");
            if (url == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier carte.html non trouvé dans resources/");
                return;
            }

            // Créer le bridge Java ↔ JavaScript
            JavaBridge bridge = new JavaBridge(carteStage, txtLieu);

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            JSObject window = (JSObject) webEngine.executeScript("window");
                            window.setMember("javaApp", bridge);
                            System.out.println("[Carte] javaApp injecté avec succès");
                        } catch (Exception ex) {
                            System.err.println("[Carte] Erreur injection : " + ex.getMessage());
                        }
                    });
                }
            });

            webEngine.load(url.toExternalForm());
            VBox root = new VBox(webView);
            Scene scene = new Scene(root, 950, 700);
            carteStage.setScene(scene);
            carteStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la carte : " + e.getMessage());
        }
    }

    // ── PONT JAVA ↔ JAVASCRIPT (identique à AjoutDisponibiliteController) ──
    public static class JavaBridge {
        private final Stage     carteStage;
        private final TextField txtLieu;

        public JavaBridge(Stage carteStage, TextField txtLieu) {
            this.carteStage = carteStage;
            this.txtLieu    = txtLieu;
        }

        public void setSelectedAddress(String address) {
            javafx.application.Platform.runLater(() -> {
                if (txtLieu != null) {
                    txtLieu.setText(address);
                    System.out.println("[JavaBridge] Adresse mise à jour : " + address);
                }
                if (carteStage != null) {
                    carteStage.close();
                }
            });
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ── Configuration Spinner ────────────────────────────────────────
    private void configurerSpinner(Spinner<Integer> spinner, int min, int max, int init) {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, init);
        factory.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer v) {
                if (v == null || v == -1) return "--";
                return String.format("%02d", v);
            }
            @Override public Integer fromString(String s) {
                try {
                    String trimmed = s.trim();
                    if ("--".equals(trimmed) || trimmed.isEmpty()) return -1;
                    return Integer.parseInt(trimmed);
                }
                catch (NumberFormatException e) { return init; }
            }
        });
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
        spinner.getEditor().focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) spinner.increment(0);
        });
    }

    // ── Preview heure ────────────────────────────────────────────────
    private void mettreAJourPreviewDebut() {
        int heure = spinnerHeureDebut.getValue();
        int minute = spinnerMinuteDebut.getValue();
        if (heure == -1 || minute == -1) {
            lblPreviewDebut.setText("--:--");
        } else {
            lblPreviewDebut.setText(String.format("%02d:%02d", heure, minute));
        }
    }

    private void mettreAJourPreviewFin() {
        int heure = spinnerHeureFin.getValue();
        int minute = spinnerMinuteFin.getValue();
        if (heure == -1 || minute == -1) {
            lblPreviewFin.setText("--:--");
        } else {
            lblPreviewFin.setText(String.format("%02d:%02d", heure, minute));
        }
    }

    // ── Lieu conditionnel ────────────────────────────────────────────
    private void gererAffichageLieu(String typeChoisi) {
        boolean presentiel = "Présentiel".equals(typeChoisi);
        boxLieu.setVisible(presentiel);
        boxLieu.setManaged(presentiel);
        if (!presentiel) {
            txtLieu.clear();
            cacherErreurSimple(errLieu);
        }
    }

    // ── Erreurs inline ───────────────────────────────────────────────
    private void afficherErreur(Label errLabel, Control champ, String message) {
        errLabel.setText("⚠ " + message);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
        if (champ != null) {
            String baseStyle = champ.getStyle();
            if (!baseStyle.contains("#ef4444")) {
                champ.setStyle(baseStyle
                        .replace("#e0e7ff", "#fca5a5")
                        .replace("#f5f3ff", "#fff1f2"));
            }
        }
    }

    private void afficherErreurSimple(Label errLabel, String message) {
        errLabel.setText("⚠ " + message);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void cacherErreur(Label errLabel, Control champ) {
        errLabel.setVisible(false);
        errLabel.setManaged(false);
        if (champ != null) {
            champ.setStyle(champ.getStyle()
                    .replace("#fca5a5", "#e0e7ff")
                    .replace("#fff1f2", "#f5f3ff"));
        }
    }

    private void cacherErreurSimple(Label errLabel) {
        errLabel.setVisible(false);
        errLabel.setManaged(false);
    }

    private void reinitialiserErreurs() {
        cacherErreur(errDate, datePicker);
        cacherErreurSimple(errHeureDebut);
        cacherErreurSimple(errHeureFin);
        cacherErreurSimple(errType);
        cacherErreurSimple(errLieu);
    }

    // ── Chargement des données existantes ────────────────────────────
    public void setDisponibiliteAModifier(DisponibilitePsy disponibilite) {
        this.disponibiliteAModifier = disponibilite;

        if (disponibilite != null) {
            datePicker.setValue(disponibilite.getDateDispo().toLocalDate());

            String heureDebutStr = disponibilite.getHeureDebut().toString();
            String[] debutParts = heureDebutStr.split(":");
            spinnerHeureDebut.getValueFactory().setValue(Integer.parseInt(debutParts[0]));
            spinnerMinuteDebut.getValueFactory().setValue(Integer.parseInt(debutParts[1]));

            String heureFinStr = disponibilite.getHeureFin().toString();
            String[] finParts = heureFinStr.split(":");
            spinnerHeureFin.getValueFactory().setValue(Integer.parseInt(finParts[0]));
            spinnerMinuteFin.getValueFactory().setValue(Integer.parseInt(finParts[1]));

            String typeConsult = disponibilite.getTypeConsult().toString();
            if ("présentiel".equals(typeConsult)) {
                cbTypeConsult.setValue("Présentiel");
            } else {
                cbTypeConsult.setValue("En ligne");
            }

            txtLieu.setText(disponibilite.getLieu());
            gererAffichageLieu(cbTypeConsult.getValue());
            mettreAJourPreviewDebut();
            mettreAJourPreviewFin();
        }
    }

    // ── Enregistrement ───────────────────────────────────────────────
    private void enregistrerModification() {
        reinitialiserErreurs();
        boolean valide = true;

        if (datePicker.getValue() == null) {
            afficherErreur(errDate, datePicker, "Veuillez sélectionner une date.");
            valide = false;
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            afficherErreur(errDate, datePicker, "La date ne peut pas être dans le passé.");
            valide = false;
        }

        if (spinnerHeureDebut.getValue() == -1 || spinnerMinuteDebut.getValue() == -1) {
            afficherErreurSimple(errHeureDebut, "Veuillez sélectionner l'heure de début.");
            valide = false;
        }

        if (spinnerHeureFin.getValue() == -1 || spinnerMinuteFin.getValue() == -1) {
            afficherErreurSimple(errHeureFin, "Veuillez sélectionner l'heure de fin.");
            valide = false;
        }

        if (valide) {
            int debutMin = spinnerHeureDebut.getValue() * 60 + spinnerMinuteDebut.getValue();
            int finMin   = spinnerHeureFin.getValue()   * 60 + spinnerMinuteFin.getValue();
            if (debutMin >= finMin) {
                afficherErreurSimple(errHeureFin,
                        "L'heure de fin doit être après l'heure de début ("
                                + lblPreviewDebut.getText() + " - " + lblPreviewFin.getText() + ").");
                valide = false;
            }
        }

        if (cbTypeConsult.getValue() == null) {
            afficherErreurSimple(errType, "Veuillez sélectionner un type de consultation.");
            valide = false;
        }

        if ("Présentiel".equals(cbTypeConsult.getValue())) {
            if (txtLieu.getText().trim().isEmpty()) {
                afficherErreur(errLieu, txtLieu, "Veuillez saisir le lieu de consultation.");
                valide = false;
            }
        }

        if (!valide) return;

        try {
            LocalDate date = datePicker.getValue();
            int hd = spinnerHeureDebut.getValue(), md = spinnerMinuteDebut.getValue();
            int hf = spinnerHeureFin.getValue(),   mf = spinnerMinuteFin.getValue();

            Time heureDebutTime = Time.valueOf(String.format("%02d:%02d:00", hd, md));
            Time heureFinTime   = Time.valueOf(String.format("%02d:%02d:00", hf, mf));

            TypeConsultation typeConsult = "Présentiel".equals(cbTypeConsult.getValue())
                    ? TypeConsultation.présentiel
                    : TypeConsultation.en_ligne;

            String lieu = typeConsult == TypeConsultation.présentiel
                    ? txtLieu.getText().trim()
                    : null;

            disponibiliteAModifier.setDateDispo(Date.valueOf(date));
            disponibiliteAModifier.setHeureDebut(heureDebutTime);
            disponibiliteAModifier.setHeureFin(heureFinTime);
            disponibiliteAModifier.setTypeConsult(typeConsult);
            disponibiliteAModifier.setLieu(lieu);

            disponibiliteService.modifier(disponibiliteAModifier);
            afficherAlerteSucces(date, lieu);

        } catch (SQLException e) {
            afficherAlerteErreur("Erreur SQL", "Une erreur est survenue lors de la modification.", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            afficherAlerteErreur("Erreur système", "Une erreur inattendue est survenue.", e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Utilitaires ─────────────────────────────────────────────────
    private void fermerModal() {
        if (modalStage != null) modalStage.close();
    }

    public void setModalStage(Stage stage) {
        this.modalStage = stage;
    }

    private void afficherAlerteSucces(LocalDate date, String lieu) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Unimind - Succès");
        alert.setHeaderText("Disponibilité modifiée avec succès !");
        alert.setContentText(buildContenuSucces(date, lieu));
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.getDialogPane().setStyle(getStyleAlerteSucces());
        alert.showAndWait();
        fermerModal();
    }

    private void afficherAlerteErreur(String titre, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Unimind - " + titre);
        alert.setHeaderText(header);
        alert.setContentText(message);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.getDialogPane().setStyle(getStyleAlerteErreur());
        alert.showAndWait();
    }

    private String buildContenuSucces(LocalDate date, String lieu) {
        StringBuilder sb = new StringBuilder();
        sb.append("Votre créneau a été modifié :\n\n");
        sb.append("Date : ").append(date).append("\n");
        sb.append("Horaire : ").append(lblPreviewDebut.getText())
                .append(" - ").append(lblPreviewFin.getText()).append("\n");
        sb.append("Type : ").append(cbTypeConsult.getValue());
        if (lieu != null && !lieu.trim().isEmpty()) {
            sb.append("\nLieu : ").append(lieu);
        }
        return sb.toString();
    }

    private String getStyleAlerteSucces() {
        return "-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 14px; " +
                "-fx-background-color: #f0fdf4; -fx-border-color: #86efac; " +
                "-fx-border-width: 2px; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 20px;";
    }

    private String getStyleAlerteErreur() {
        return "-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 14px; " +
                "-fx-background-color: #fef2f2; -fx-border-color: #fca5a5; " +
                "-fx-border-width: 2px; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-padding: 20px;";
    }
}