package org.example.controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.example.enums.TypeConsultation;
import org.example.entities.DisponibilitePsy;
import org.example.services.DisponibilitePsyService;
import netscape.javascript.JSObject;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class AjoutDisponibiliteController {

    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> spinnerHeureDebut;
    @FXML private Spinner<Integer> spinnerMinuteDebut;
    @FXML private Label            lblPreviewDebut;
    @FXML private Spinner<Integer> spinnerHeureFin;
    @FXML private Spinner<Integer> spinnerMinuteFin;
    @FXML private Label            lblPreviewFin;
    @FXML private ComboBox<String> cbTypeConsult;
    @FXML private VBox             boxLieu;
    @FXML private TextField        txtLieu;
    @FXML private Button           btnOuvrirCarte;
    @FXML private Button           btnFermer;
    @FXML private Button           btnAnnuler;
    @FXML private Button           btnEnregistrer;
    @FXML private Label errDate;
    @FXML private Label errHeureDebut;
    @FXML private Label errHeureFin;
    @FXML private Label errType;
    @FXML private Label errLieu;

    private DisponibilitePsyService disponibiliteService;
    private int   userIdConnecte;
    private Stage modalStage;

    @FXML
    public void initialize() {
        disponibiliteService = new DisponibilitePsyService();

        configurerSpinner(spinnerHeureDebut,  0, 23, -1);
        configurerSpinner(spinnerMinuteDebut, 0, 59, -1);
        configurerSpinner(spinnerHeureFin,    0, 23, -1);
        configurerSpinner(spinnerMinuteFin,   0, 59, -1);

        spinnerHeureDebut.valueProperty().addListener((o,ov,nv)  -> mettreAJourPreviewDebut());
        spinnerMinuteDebut.valueProperty().addListener((o,ov,nv) -> mettreAJourPreviewDebut());
        spinnerHeureFin.valueProperty().addListener((o,ov,nv)    -> mettreAJourPreviewFin());
        spinnerMinuteFin.valueProperty().addListener((o,ov,nv)   -> mettreAJourPreviewFin());

        mettreAJourPreviewDebut();
        mettreAJourPreviewFin();

        datePicker.valueProperty().addListener((o,ov,nv) -> cacherErreur(errDate, datePicker));
        spinnerHeureDebut.valueProperty().addListener((o,ov,nv)  -> cacherErreurSimple(errHeureDebut));
        spinnerMinuteDebut.valueProperty().addListener((o,ov,nv) -> cacherErreurSimple(errHeureDebut));
        spinnerHeureFin.valueProperty().addListener((o,ov,nv)    -> cacherErreurSimple(errHeureFin));
        spinnerMinuteFin.valueProperty().addListener((o,ov,nv)   -> cacherErreurSimple(errHeureFin));
        cbTypeConsult.valueProperty().addListener((o,ov,nv) -> {
            cacherErreurSimple(errType);
            gererAffichageLieu(nv);
        });
        txtLieu.textProperty().addListener((o,ov,nv) -> cacherErreurSimple(errLieu));

        btnEnregistrer.setOnAction(e -> enregistrerDisponibilite());
        btnAnnuler.setOnAction(e     -> fermerModal());
        btnFermer.setOnAction(e      -> fermerModal());
        btnOuvrirCarte.setOnAction(e -> ouvrirCarte());

        btnEnregistrer.setOnMouseEntered(e ->
                btnEnregistrer.setStyle(btnEnregistrer.getStyle().replace("#6366f1","#4f46e5")));
        btnEnregistrer.setOnMouseExited(e ->
                btnEnregistrer.setStyle(btnEnregistrer.getStyle().replace("#4f46e5","#6366f1")));
        btnOuvrirCarte.setOnMouseEntered(e ->
                btnOuvrirCarte.setStyle("-fx-background-color:#4f46e5;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:8 15;" +
                        "-fx-background-radius:8;-fx-cursor:hand;"));
        btnOuvrirCarte.setOnMouseExited(e ->
                btnOuvrirCarte.setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:8 15;" +
                        "-fx-background-radius:8;-fx-cursor:hand;"));
    }

    // ════════════════════════════════════════════════════════════════
    //  PRÉ-REMPLISSAGE depuis le calendrier
    // ════════════════════════════════════════════════════════════════

    /** Pré-remplit la date. Appeler après FXMLLoader.load(). */
    public void setDatePreRemplie(LocalDate date) {
        if (date != null) datePicker.setValue(date);
    }

    /** Pré-remplit heure début et heure fin. Appeler après FXMLLoader.load(). */
    public void setHeuresPreRemplies(LocalTime heureDebut, LocalTime heureFin) {
        if (heureDebut != null) {
            spinnerHeureDebut.getValueFactory().setValue(heureDebut.getHour());
            spinnerMinuteDebut.getValueFactory().setValue(heureDebut.getMinute());
        }
        if (heureFin != null) {
            spinnerHeureFin.getValueFactory().setValue(heureFin.getHour());
            spinnerMinuteFin.getValueFactory().setValue(heureFin.getMinute());
        }
        mettreAJourPreviewDebut();
        mettreAJourPreviewFin();
    }

    // ════════════════════════════════════════════════════════════════

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
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier carte.html non trouvé");
                return;
            }

            JavaBridge bridge = new JavaBridge(carteStage, txtLieu);
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            JSObject window = (JSObject) webEngine.executeScript("window");
                            window.setMember("javaApp", bridge);
                        } catch (Exception ex) {
                            System.err.println("[Carte] " + ex.getMessage());
                        }
                    });
                }
            });

            webEngine.load(url.toExternalForm());
            VBox root = new VBox(webView);
            carteStage.setScene(new Scene(root, 950, 700));
            carteStage.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la carte : " + e.getMessage());
        }
    }

    public static class JavaBridge {
        private final Stage     carteStage;
        private final TextField txtLieu;

        public JavaBridge(Stage s, TextField t) { carteStage = s; txtLieu = t; }

        public void setSelectedAddress(String address) {
            javafx.application.Platform.runLater(() -> {
                if (txtLieu    != null) txtLieu.setText(address);
                if (carteStage != null) carteStage.close();
            });
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String msg) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void configurerSpinner(Spinner<Integer> s, int min, int max, int init) {
        SpinnerValueFactory<Integer> f =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, init);
        f.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer v) {
                if (v == null || v == -1) return "--";
                return String.format("%02d", v);
            }
            @Override public Integer fromString(String str) {
                try {
                    String t = str.trim();
                    if ("--".equals(t) || t.isEmpty()) return -1;
                    return Integer.parseInt(t);
                } catch (NumberFormatException e) { return init; }
            }
        });
        s.setValueFactory(f);
        s.setEditable(true);
        s.getEditor().focusedProperty().addListener((o, ov, nv) -> { if (!nv) s.increment(0); });
    }

    private void mettreAJourPreviewDebut() {
        int h = spinnerHeureDebut.getValue(), m = spinnerMinuteDebut.getValue();
        lblPreviewDebut.setText((h==-1||m==-1) ? "--:--" : String.format("%02d:%02d",h,m));
    }

    private void mettreAJourPreviewFin() {
        int h = spinnerHeureFin.getValue(), m = spinnerMinuteFin.getValue();
        lblPreviewFin.setText((h==-1||m==-1) ? "--:--" : String.format("%02d:%02d",h,m));
    }

    private void gererAffichageLieu(String t) {
        boolean p = "Présentiel".equals(t);
        boxLieu.setVisible(p); boxLieu.setManaged(p);
        if (!p) { txtLieu.clear(); cacherErreurSimple(errLieu); }
    }

    private void afficherErreur(Label l, Control c, String msg) {
        l.setText("⚠ "+msg); l.setVisible(true); l.setManaged(true);
        if (c!=null) { String s=c.getStyle(); if(!s.contains("#ef4444"))
            c.setStyle(s.replace("#e0e7ff","#fca5a5").replace("#f5f3ff","#fff1f2")); }
    }
    private void afficherErreurSimple(Label l, String msg) {
        l.setText("⚠ "+msg); l.setVisible(true); l.setManaged(true);
    }
    private void cacherErreur(Label l, Control c) {
        l.setVisible(false); l.setManaged(false);
        if (c!=null) c.setStyle(c.getStyle().replace("#fca5a5","#e0e7ff").replace("#fff1f2","#f5f3ff"));
    }
    private void cacherErreurSimple(Label l) { l.setVisible(false); l.setManaged(false); }
    private void reinitialiserErreurs() {
        cacherErreur(errDate,datePicker); cacherErreurSimple(errHeureDebut);
        cacherErreurSimple(errHeureFin);  cacherErreurSimple(errType); cacherErreurSimple(errLieu);
    }

    private void enregistrerDisponibilite() {
        reinitialiserErreurs();
        boolean ok = true;

        if (datePicker.getValue()==null) {
            afficherErreur(errDate,datePicker,"Veuillez sélectionner une date."); ok=false;
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            afficherErreur(errDate,datePicker,"La date ne peut pas être dans le passé."); ok=false;
        }
        if (spinnerHeureDebut.getValue()==-1||spinnerMinuteDebut.getValue()==-1) {
            afficherErreurSimple(errHeureDebut,"Veuillez sélectionner l'heure de début."); ok=false;
        }
        if (spinnerHeureFin.getValue()==-1||spinnerMinuteFin.getValue()==-1) {
            afficherErreurSimple(errHeureFin,"Veuillez sélectionner l'heure de fin."); ok=false;
        }
        if (ok) {
            int d=spinnerHeureDebut.getValue()*60+spinnerMinuteDebut.getValue();
            int f=spinnerHeureFin.getValue()  *60+spinnerMinuteFin.getValue();
            if (d>=f) { afficherErreurSimple(errHeureFin,
                    "L'heure de fin doit être après l'heure de début ("
                            +lblPreviewDebut.getText()+" - "+lblPreviewFin.getText()+")."); ok=false; }
        }
        if (cbTypeConsult.getValue()==null) {
            afficherErreurSimple(errType,"Veuillez sélectionner un type de consultation."); ok=false;
        }
        if ("Présentiel".equals(cbTypeConsult.getValue())&&txtLieu.getText().trim().isEmpty()) {
            afficherErreur(errLieu,txtLieu,"Veuillez saisir le lieu de consultation."); ok=false;
        }
        if (!ok) return;

        try {
            int hd=spinnerHeureDebut.getValue(), md=spinnerMinuteDebut.getValue();
            int hf=spinnerHeureFin.getValue(),   mf=spinnerMinuteFin.getValue();
            TypeConsultation type = "Présentiel".equals(cbTypeConsult.getValue())
                    ? TypeConsultation.présentiel : TypeConsultation.en_ligne;
            String lieu = type==TypeConsultation.présentiel ? txtLieu.getText().trim() : null;

            disponibiliteService.ajouter(new DisponibilitePsy(
                    userIdConnecte, Date.valueOf(datePicker.getValue()),
                    Time.valueOf(String.format("%02d:%02d:00",hd,md)),
                    Time.valueOf(String.format("%02d:%02d:00",hf,mf)),
                    type, lieu));

            afficherAlerteSucces(datePicker.getValue(), lieu);
        } catch (SQLException e) {
            afficherAlerteErreur("Erreur SQL","Erreur lors de l'ajout.",e.getMessage());
        } catch (Exception e) {
            afficherAlerteErreur("Erreur système","Erreur inattendue.",e.getMessage());
        }
    }

    private void fermerModal() { if (modalStage!=null) modalStage.close(); }
    public void setUserId(int id)          { this.userIdConnecte = id; }
    public void setModalStage(Stage stage) { this.modalStage = stage; }

    private void afficherAlerteSucces(LocalDate date, String lieu) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Unimind - Succès"); a.setHeaderText("Disponibilité ajoutée avec succès !");
        a.setContentText(buildContenuSucces(date,lieu));
        a.getButtonTypes().setAll(new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        a.getDialogPane().setStyle(getStyleSucces());
        a.showAndWait(); fermerModal();
    }
    private void afficherAlerteErreur(String titre, String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Unimind - "+titre); a.setHeaderText(header); a.setContentText(msg);
        a.getButtonTypes().setAll(new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        a.getDialogPane().setStyle(getStyleErreur()); a.showAndWait();
    }
    private String buildContenuSucces(LocalDate date, String lieu) {
        return "Votre créneau a été enregistré :\n\nDate : " + date
                + "\nHoraire : " + lblPreviewDebut.getText() + " - " + lblPreviewFin.getText()
                + "\nType : " + cbTypeConsult.getValue()
                + (lieu!=null&&!lieu.isEmpty() ? "\nLieu : "+lieu : "");
    }
    private String getStyleSucces() {
        return "-fx-font-family:'Segoe UI';-fx-font-size:14px;-fx-background-color:#f0fdf4;" +
                "-fx-border-color:#86efac;-fx-border-width:2px;-fx-border-radius:12px;" +
                "-fx-background-radius:12px;-fx-padding:20px;";
    }
    private String getStyleErreur() {
        return "-fx-font-family:'Segoe UI';-fx-font-size:14px;-fx-background-color:#fef2f2;" +
                "-fx-border-color:#fca5a5;-fx-border-width:2px;-fx-border-radius:12px;" +
                "-fx-background-radius:12px;-fx-padding:20px;";
    }
}