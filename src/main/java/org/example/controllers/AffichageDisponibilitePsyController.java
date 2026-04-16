package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.models.DisponibilitePsy;
import org.example.models.User;
import org.example.services.DisponibilitePsyService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class AffichageDisponibilitePsyController
        implements SidebarPsyController.PsyPageController {

    // ── FXML ────────────────────────────────────────────────────────
    @FXML private TableView<DisponibilitePsy>              tableViewDisponibilites;
    @FXML private TableColumn<DisponibilitePsy, String>    colDate;
    @FXML private TableColumn<DisponibilitePsy, String>    colHoraires;
    @FXML private TableColumn<DisponibilitePsy, String>    colTypeConsult;
    @FXML private TableColumn<DisponibilitePsy, String>    colLieu;
    @FXML private TableColumn<DisponibilitePsy, String>    colStatut;
    @FXML private TableColumn<DisponibilitePsy, Void>      colAction;

    @FXML private Button    btnAjouter;
    @FXML private Button    btnRafraichir;
    @FXML private Label     lblStatut;
    @FXML private Label     lblDate;

    // Stat cards
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatDispo;
    @FXML private Label lblStatReserve;
    @FXML private Label lblStatAnnule;

    // Filtres
    @FXML private TextField  fieldRecherche;
    @FXML private ComboBox<String> comboFiltreStatut;
    @FXML private ComboBox<String> comboFiltreType;

    // ── Sidebar ─────────────────────────────────────────────────────
    @FXML private SidebarPsyController sidebarPsyController;

    // ── Données ─────────────────────────────────────────────────────
    private DisponibilitePsyService          disponibiliteService;
    private ObservableList<DisponibilitePsy> disponibilitesList;
    private FilteredList<DisponibilitePsy>   filteredList;
    private User                             utilisateur;

    // ── Design tokens ────────────────────────────────────────────────
    private static final String COL_HEADER =
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-text-fill: #6366f1;";

    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

        disponibiliteService = new DisponibilitePsyService();
        disponibilitesList   = FXCollections.observableArrayList();
        filteredList         = new FilteredList<>(disponibilitesList, p -> true);

        initialiserFiltres();
        configurerColonnes();
        styleTable();

        btnAjouter.setOnAction(e -> ouvrirFormulaireAjout());
        btnRafraichir.setOnAction(e -> chargerDisponibilites());

        // Hover btn ajouter
        btnAjouter.setOnMouseEntered(e ->
                btnAjouter.setStyle(btnAjouter.getStyle().replace("#6366f1", "#4f46e5")));
        btnAjouter.setOnMouseExited(e ->
                btnAjouter.setStyle(btnAjouter.getStyle().replace("#4f46e5", "#6366f1")));
    }

    // ── Filtres ─────────────────────────────────────────────────────
    private void initialiserFiltres() {
        comboFiltreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "disponible", "réservé", "annulé"));
        comboFiltreStatut.setValue("Tous");

        comboFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "présentiel", "en ligne"));
        comboFiltreType.setValue("Tous");

        // Listener sur chaque filtre → refiltre
        fieldRecherche.textProperty().addListener((o, ov, nv) -> appliquerFiltres());
        comboFiltreStatut.valueProperty().addListener((o, ov, nv) -> appliquerFiltres());
        comboFiltreType.valueProperty().addListener((o, ov, nv) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        String recherche = fieldRecherche.getText() == null ? "" :
                fieldRecherche.getText().toLowerCase().trim();
        String statut    = comboFiltreStatut.getValue();
        String type      = comboFiltreType.getValue();

        filteredList.setPredicate(d -> {
            // Filtre recherche (date ou lieu)
            boolean matchRecherche = recherche.isEmpty()
                    || d.getDateDispo().toString().contains(recherche)
                    || (d.getLieu() != null && d.getLieu().toLowerCase().contains(recherche));

            // Filtre statut
            boolean matchStatut = statut == null || statut.equals("Tous")
                    || d.getStatut().toString().equalsIgnoreCase(statut);

            // Filtre type
            boolean matchType = type == null || type.equals("Tous")
                    || d.getTypeConsult().toString().equalsIgnoreCase(type);

            return matchRecherche && matchStatut && matchType;
        });

        mettreAJourStatutLabel();
    }

    // ── Style table ─────────────────────────────────────────────────
    private void styleTable() {
        tableViewDisponibilites.setStyle(
                "-fx-background-color: transparent; -fx-border-width: 0; " +
                        "-fx-table-cell-border-color: #f3f4f6;");
        tableViewDisponibilites.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ── Colonnes ────────────────────────────────────────────────────
    private void configurerColonnes() {

        // ── Date (avec jour de la semaine) ──
        colDate.setCellValueFactory(cell -> {
            java.sql.Date d = cell.getValue().getDateDispo();
            LocalDate ld = d.toLocalDate();
            String jour = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            String date = ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return new SimpleStringProperty(jour + "\n" + date);
        });
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: #374151; -fx-padding: 10 16; -fx-alignment: CENTER_LEFT;");
            }
        });

        // ── Horaires ──
        colHoraires.setCellValueFactory(cell -> {
            String debut = cell.getValue().getHeureDebut().toString().substring(0, 5);
            String fin   = cell.getValue().getHeureFin().toString().substring(0, 5);
            return new SimpleStringProperty(debut + " – " + fin);
        });
        colHoraires.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label lbl = new Label(s);
                lbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; " +
                        "-fx-font-weight: bold; -fx-text-fill: #6366f1;");
                setGraphic(lbl);
                setText(null);
            }
        });

        // ── Type consultation (badge coloré) ──
        colTypeConsult.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTypeConsult().toString()));
        colTypeConsult.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                boolean presentiel = "présentiel".equalsIgnoreCase(s);
                Label badge = new Label(presentiel ? "🏢  Présentiel" : "💻  En ligne");
                badge.setStyle(
                        "-fx-background-color: " + (presentiel ? "#dbeafe" : "#ede9fe") + "; " +
                                "-fx-text-fill: "         + (presentiel ? "#1d4ed8" : "#6366f1") + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // ── Lieu ──
        colLieu.setCellValueFactory(cell -> {
            String lieu = cell.getValue().getLieu();
            return new SimpleStringProperty(lieu != null && !lieu.isEmpty() ? lieu : "—");
        });
        colLieu.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; " +
                        "-fx-text-fill: " + ("—".equals(s) ? "#c4b5fd" : "#374151") + "; " +
                        "-fx-padding: 10 16;");
            }
        });

        // ── Statut (badge coloré) ──
        colStatut.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatut().toString()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }

                String bg, fg, txt;
                switch (s.toLowerCase()) {
                    case "disponible" -> { bg = "#dcfce7"; fg = "#16a34a"; txt = "✅  Disponible"; }
                    case "réservé"    -> { bg = "#fff7ed"; fg = "#c2410c"; txt = "🔒  Réservé";    }
                    case "annulé"     -> { bg = "#fee2e2"; fg = "#dc2626"; txt = "❌  Annulé";     }
                    default           -> { bg = "#f3f4f6"; fg = "#6b7280"; txt = s;               }
                }
                Label badge = new Label(txt);
                badge.setStyle(
                        "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // ── Actions ──
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier  = new Button("✏");
            private final Button btnSupprimer = new Button("🗑");
            private final HBox   box          = new HBox(6, btnModifier, btnSupprimer);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                // Style modifier
                String styleEdit =
                        "-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; " +
                                "-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
                btnModifier.setStyle(styleEdit);
                btnModifier.setOnMouseEntered(e ->
                        btnModifier.setStyle(styleEdit.replace("#ede9fe", "#ddd6fe")));
                btnModifier.setOnMouseExited(e ->
                        btnModifier.setStyle(styleEdit));
                btnModifier.setTooltip(new Tooltip("Modifier cette disponibilité"));

                // Style supprimer
                String styleDel =
                        "-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; " +
                                "-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand;";
                btnSupprimer.setStyle(styleDel);
                btnSupprimer.setOnMouseEntered(e ->
                        btnSupprimer.setStyle(styleDel.replace("#fee2e2", "#fecaca")));
                btnSupprimer.setOnMouseExited(e ->
                        btnSupprimer.setStyle(styleDel));
                btnSupprimer.setTooltip(new Tooltip("Supprimer cette disponibilité"));

                btnModifier.setOnAction(e ->
                        modifierDisponibilite(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e ->
                        supprimerDisponibilite(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                DisponibilitePsy dispo = getTableView().getItems().get(getIndex());
                String statut = dispo.getStatut().toString().toLowerCase();

                // Masquer supprimer si réservé
                btnSupprimer.setVisible(!"réservé".equals(statut));
                btnSupprimer.setManaged(!"réservé".equals(statut));

                setGraphic(box);
            }
        });

        tableViewDisponibilites.setItems(filteredList);
    }

    // ── Interface PsyPageController ─────────────────────────────────
    @Override
    public void setUtilisateur(User user) {
        this.utilisateur = user;
        if (sidebarPsyController != null) {
            sidebarPsyController.setUtilisateur(user);
            sidebarPsyController.setActiveButtonByFxml("/AfficheDisponibilitesPsy.fxml");
        }
        chargerDisponibilites();
    }

    // ── Chargement ──────────────────────────────────────────────────
    private void chargerDisponibilites() {
        if (utilisateur == null) {
            lblStatut.setText("Erreur : utilisateur non connecté");
            return;
        }
        try {
            lblStatut.setText("Chargement…");
            List<DisponibilitePsy> liste =
                    disponibiliteService.afficherDisponibilitesPsy(utilisateur.getUserId());

            disponibilitesList.setAll(liste);

            // Stat cards
            long total    = liste.size();
            long dispo    = liste.stream().filter(d -> "disponible".equalsIgnoreCase(d.getStatut().toString())).count();
            long reserve  = liste.stream().filter(d -> "réservé".equalsIgnoreCase(d.getStatut().toString())).count();
            long annule   = liste.stream().filter(d -> "annulé".equalsIgnoreCase(d.getStatut().toString())).count();

            lblStatTotal.setText(String.valueOf(total));
            lblStatDispo.setText(String.valueOf(dispo));
            lblStatReserve.setText(String.valueOf(reserve));
            lblStatAnnule.setText(String.valueOf(annule));

            mettreAJourStatutLabel();

        } catch (SQLException e) {
            lblStatut.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mettreAJourStatutLabel() {
        int nb = filteredList.size();
        lblStatut.setText(nb == 0 ? "Aucune disponibilité trouvée."
                : nb + " disponibilité(s) affichée(s)");
    }

    // ── Modals ──────────────────────────────────────────────────────
    private void ouvrirFormulaireAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjoutDisponibiliteModal.fxml"));
            Stage modalStage  = new Stage();
            Scene scene       = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnAjouter.getScene().getWindow());
            modalStage.setTitle("Nouvelle disponibilité");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            AjoutDisponibiliteController controller = loader.getController();
            controller.setUserId(utilisateur.getUserId());
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerDisponibilites();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire d'ajout");
            e.printStackTrace();
        }
    }

    private void modifierDisponibilite(DisponibilitePsy dispo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierDisponibiliteModal.fxml"));
            Stage modalStage  = new Stage();
            Scene scene       = new Scene(loader.load());

            modalStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            modalStage.initOwner(btnAjouter.getScene().getWindow());
            modalStage.setTitle("Modifier la disponibilité");
            modalStage.setScene(scene);
            modalStage.setResizable(false);

            ModifierDisponibiliteController controller = loader.getController();
            controller.setDisponibiliteAModifier(dispo);
            controller.setModalStage(modalStage);

            modalStage.showAndWait();
            chargerDisponibilites();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification");
            e.printStackTrace();
        }
    }

    private void supprimerDisponibilite(DisponibilitePsy dispo) {
        afficherAlerteConfirmation(dispo);
    }

    private void afficherAlerteConfirmation(DisponibilitePsy dispo) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Unimind - Confirmation");
        confirmation.setHeaderText("Supprimer la disponibilité");
        confirmation.setContentText(buildContenuConfirmation(dispo));

        // Personnaliser les boutons
        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Non, annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnNon, btnOui);

        // Appliquer le style personnalisé
        DialogPane dialogPane = confirmation.getDialogPane();
        dialogPane.setStyle(getStyleAlerteConfirmation());

        confirmation.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                try {
                    disponibiliteService.supprimer(dispo.getDispoId());
                    afficherAlerteSuccesSuppression(dispo);
                    chargerDisponibilites();
                } catch (SQLException e) {
                    afficherAlerteErreur("Erreur SQL", "Une erreur est survenue lors de la suppression du créneau.", e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    afficherAlerteErreur("Erreur système", "Une erreur inattendue est survenue.", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // ---- Utilitaire ----──────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String buildContenuConfirmation(DisponibilitePsy dispo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Êtes-vous sûr de vouloir supprimer ce créneau ?\n\n");
        sb.append("Date : ").append(dispo.getDateDispo()).append("\n");
        sb.append("Horaire : ").append(dispo.getHeureDebut().toString().substring(0, 5))
          .append(" - ").append(dispo.getHeureFin().toString().substring(0, 5)).append("\n");
        sb.append("Type : ").append(dispo.getTypeConsult().toString());
        
        if (dispo.getLieu() != null && !dispo.getLieu().trim().isEmpty()) {
            sb.append("\nLieu : ").append(dispo.getLieu());
        }
        
        sb.append("\n\nCette action est irréversible.");
        return sb.toString();
    }

    private void afficherAlerteSuccesSuppression(DisponibilitePsy dispo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Unimind - Succès");
        alert.setHeaderText("Créneau supprimé avec succès !");

        String contenu = "Le créneau suivant a été supprimé :\n\n" +
                "Date : " + dispo.getDateDispo() + "\n" +
                "Horaire : " + dispo.getHeureDebut().toString().substring(0, 5) +
                " - " + dispo.getHeureFin().toString().substring(0, 5);
        alert.setContentText(contenu);

        // Personnaliser les boutons
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);

        // Appliquer le style personnalisé
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(getStyleAlerteSucces());

        alert.showAndWait();
    }

    private void afficherAlerteErreur(String titre, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Unimind - " + titre);
        alert.setHeaderText(header);
        alert.setContentText(message);

        // Personnaliser les boutons
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);

        // Appliquer le style personnalisé
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(getStyleAlerteErreur());

        alert.showAndWait();
    }

    private String getStyleAlerteConfirmation() {
        return "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-color: #fffbeb; " +
                "-fx-border-color: #fcd34d; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 20px;";
    }

    private String getStyleAlerteSucces() {
        return "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-color: #f0fdf4; " +
                "-fx-border-color: #86efac; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 20px;";
    }

    private String getStyleAlerteErreur() {
        return "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-background-color: #fef2f2; " +
                "-fx-border-color: #fca5a5; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 20px;";
    }
}