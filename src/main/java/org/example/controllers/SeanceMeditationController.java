package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.CategorieMeditation;
import org.example.entities.SeanceMeditation;
import org.example.enums.NiveauMeditation;
import org.example.enums.TypeFichier;
import org.example.services.SeanceMeditationServices;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SeanceMeditationController implements Initializable {

    // Header
    @FXML private Label lblNomCategorie;
    @FXML private Label lblDescriptionCategorie;

    // Stats
    @FXML private Label lblNbSeances;
    @FXML private Label lblNbActives;
    @FXML private Label lblDerniereDate;

    // Toolbar
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterType;
    @FXML private ComboBox<String> cbFilterNiveau;
    @FXML private ComboBox<String> cbFilterStatut;

    // Table
    @FXML private TableView<SeanceMeditation> tableSeances;
    @FXML private TableColumn<SeanceMeditation, String> colTitre;
    @FXML private TableColumn<SeanceMeditation, String> colDescription;
    @FXML private TableColumn<SeanceMeditation, String> colType;
    @FXML private TableColumn<SeanceMeditation, String> colDuree;
    @FXML private TableColumn<SeanceMeditation, String> colNiveau;
    @FXML private TableColumn<SeanceMeditation, String> colStatut;
    @FXML private TableColumn<SeanceMeditation, String> colActions;

    // Services & State
    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();
    private CategorieMeditation categorieCourante;
    private ObservableList<SeanceMeditation> allSeances = FXCollections.observableArrayList();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilters();
        setupTable();
    }

    // ==================== INIT WITH CATEGORY ====================

    public void initWithCategorie(CategorieMeditation categorie) {
        this.categorieCourante = categorie;
        lblNomCategorie.setText(categorie.getNom());
        String desc = categorie.getDescription();
        lblDescriptionCategorie.setText(desc != null && !desc.isBlank() ? "📝 " + desc : "Aucune description disponible");
        loadData();
    }

    // ==================== FILTERS SETUP ====================

    private void setupFilters() {
        cbFilterType.setItems(FXCollections.observableArrayList("Tous", "video", "audio"));
        cbFilterType.getSelectionModel().selectFirst();

        cbFilterNiveau.setItems(FXCollections.observableArrayList("Tous", "debutant", "intermediaire", "avance"));
        cbFilterNiveau.getSelectionModel().selectFirst();

        cbFilterStatut.setItems(FXCollections.observableArrayList("Tous", "Actif", "Inactif"));
        cbFilterStatut.getSelectionModel().selectFirst();
    }

    // ==================== TABLE SETUP ====================

    private void setupTable() {
        // Titre
        colTitre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #3730a3;");
                }
            }
        });

        // Description
        colDescription.setCellValueFactory(data -> {
            String d = data.getValue().getDescription();
            if (d == null || d.isBlank()) return new SimpleStringProperty("—");
            return new SimpleStringProperty(d.length() > 50 ? d.substring(0, 50) + "..." : d);
        });

        // Type
        colType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTypeFichier() != null ? data.getValue().getTypeFichier().name() : "—"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item.equals("video") ? "🎬 video" : "🎵 audio");
                badge.setStyle(item.equals("VIDEO")
                        ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;"
                        : "-fx-background-color: #fce7f3; -fx-text-fill: #9d174d; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Durée
        colDuree.setCellValueFactory(data -> {
            int duree = data.getValue().getDuree();
            int min = duree / 60, sec = duree % 60;
            return new SimpleStringProperty(String.format("%d:%02d", min, sec));
        });

        // Niveau
        colNiveau.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNiveau() != null ? data.getValue().getNiveau().name() : "—"));
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String color, bg;
                switch (item) {
                    case "debutant" -> { color = "#15803d"; bg = "#dcfce7"; }
                    case "intermediaire" -> { color = "#b45309"; bg = "#fef3c7"; }
                    default -> { color = "#b91c1c"; bg = "#fee2e2"; }
                }
                Label badge = new Label(item);
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Statut
        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isIsActive() ? "Actif" : "Inactif"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item.equals("Actif") ? "✅ Actif" : "⛔ Inactif");
                badge.setStyle(item.equals("Actif")
                        ? "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;"
                        : "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final Button btnView   = new Button("▶");
            private final Button btnToggle = new Button("⚡");
            private final HBox box = new HBox(4, btnView, btnEdit, btnDelete, btnToggle);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().addAll("btn-icon", "btn-edit");
                btnDelete.getStyleClass().addAll("btn-icon", "btn-delete");
                btnView.getStyleClass().addAll("btn-icon", "btn-view");
                btnToggle.getStyleClass().addAll("btn-icon");
                btnToggle.setStyle("-fx-text-fill: #f59e0b;");

                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                btnView.setTooltip(new Tooltip("Consulter"));
                btnToggle.setTooltip(new Tooltip("Activer / Désactiver"));

                btnEdit.setOnAction(e -> openEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
                btnView.setOnAction(e -> consulterSeance(getTableView().getItems().get(getIndex())));
                btnToggle.setOnAction(e -> toggleStatut(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                SeanceMeditation s = getTableView().getItems().get(getIndex());
                btnToggle.setText(s.isIsActive() ? "🔴" : "🟢");
                btnToggle.setTooltip(new Tooltip(s.isIsActive() ? "Désactiver" : "Activer"));
                setGraphic(box);
            }
        });
    }

    // ==================== DATA ====================

    private void loadData() {
        try {
            List<SeanceMeditation> all = seanceService.afficher();
            List<SeanceMeditation> filtered = all.stream()
                    .filter(s -> s.getCategorieId() == categorieCourante.getCategorieId())
                    .collect(Collectors.toList());
            allSeances = FXCollections.observableArrayList(filtered);
            applyFilters();
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur chargement : " + e.getMessage());
        }
    }

    private void updateStats() {
        lblNbSeances.setText(String.valueOf(allSeances.size()));
        long actives = allSeances.stream().filter(SeanceMeditation::isIsActive).count();
        lblNbActives.setText(String.valueOf(actives));
        allSeances.stream()
                .filter(s -> s.getCreatedAt() != null)
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .ifPresentOrElse(
                        s -> lblDerniereDate.setText(DATE_FORMAT.format(s.getCreatedAt())),
                        () -> lblDerniereDate.setText("—")
                );
    }

    // ==================== SEARCH & FILTER ====================

    @FXML
    private void onSearch() { applyFilters(); }

    @FXML
    private void onFilter() { applyFilters(); }

    private void applyFilters() {
        String query = tfSearch.getText().trim().toLowerCase();
        String type = cbFilterType.getValue();
        String niveau = cbFilterNiveau.getValue();
        String statut = cbFilterStatut.getValue();

        List<SeanceMeditation> result = allSeances.stream().filter(s -> {
            boolean matchSearch = query.isEmpty()
                    || (s.getTitre() != null && s.getTitre().toLowerCase().contains(query))
                    || (s.getDescription() != null && s.getDescription().toLowerCase().contains(query));
            boolean matchType = type == null || type.equals("Tous")
                    || (s.getTypeFichier() != null && s.getTypeFichier().name().equals(type));
            boolean matchNiveau = niveau == null || niveau.equals("Tous")
                    || (s.getNiveau() != null && s.getNiveau().name().equals(niveau));
            boolean matchStatut = statut == null || statut.equals("Tous")
                    || (statut.equals("Actif") && s.isIsActive())
                    || (statut.equals("Inactif") && !s.isIsActive());
            return matchSearch && matchType && matchNiveau && matchStatut;
        }).collect(Collectors.toList());

        tableSeances.setItems(FXCollections.observableArrayList(result));
    }

    // ==================== FORM DIALOG ====================

    @FXML
    private void openAddForm() { showFormDialog(null); }

    private void openEditDialog(SeanceMeditation s) { showFormDialog(s); }

    private void showFormDialog(SeanceMeditation existing) {
        boolean isEdit = existing != null;

        // --- Fields ---
        TextField tfTitre = new TextField();
        tfTitre.setPromptText("Ex: Méditation du matin");
        tfTitre.getStyleClass().add("form-input");

        TextArea taDescription = new TextArea();
        taDescription.setPromptText("Décrivez cette séance...");
        taDescription.setPrefRowCount(2);
        taDescription.setWrapText(true);
        taDescription.getStyleClass().add("form-textarea");

        ComboBox<String> cbType = new ComboBox<>();
        cbType.setItems(FXCollections.observableArrayList("video", "audio"));
        cbType.setPromptText("Choisir le type...");
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.getStyleClass().add("filter-combo");

        TextField tfDuree = new TextField();
        tfDuree.setPromptText("Durée en secondes (max 3600)");
        tfDuree.getStyleClass().add("form-input");

        ComboBox<String> cbNiveau = new ComboBox<>();
        cbNiveau.setItems(FXCollections.observableArrayList("debutant", "intermediaire", "avance"));
        cbNiveau.setPromptText("Choisir le niveau...");
        cbNiveau.setMaxWidth(Double.MAX_VALUE);
        cbNiveau.getStyleClass().add("filter-combo");

        TextField tfCategorie = new TextField(categorieCourante.getNom());
        tfCategorie.setEditable(false);
        tfCategorie.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-border-color: #c7d2fe; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14 10 14;");

        // File chooser field
        HBox fileBox = new HBox(8);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        TextField tfFichier = new TextField();
        tfFichier.setPromptText("Aucun fichier sélectionné");
        tfFichier.setEditable(false);
        tfFichier.getStyleClass().add("form-input");
        HBox.setHgrow(tfFichier, Priority.ALWAYS);
        Button btnChooseFichier = new Button("📂 Choisir");
        btnChooseFichier.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 9 14 9 14; -fx-cursor: hand; -fx-font-weight: bold;");
        fileBox.getChildren().addAll(tfFichier, btnChooseFichier);

        final String[] selectedFilePath = {null};
        btnChooseFichier.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir un fichier média");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Fichiers médias", "*.mp3", "*.mp4"),
                    new FileChooser.ExtensionFilter("Audio MP3", "*.mp3"),
                    new FileChooser.ExtensionFilter("Vidéo MP4", "*.mp4")
            );
            File file = fc.showOpenDialog(tfTitre.getScene() != null ? tfTitre.getScene().getWindow() : null);
            if (file != null) {
                if (file.length() > 100L * 1024 * 1024) {
                    showAlert(Alert.AlertType.WARNING, "Fichier trop volumineux", "⚠️ Le fichier ne doit pas dépasser 100 MB.");
                } else {
                    selectedFilePath[0] = file.getAbsolutePath();
                    tfFichier.setText(file.getName());
                }
            }
        });

        CheckBox cbActif = new CheckBox("Séance active");
        cbActif.setSelected(true);
        cbActif.setStyle("-fx-text-fill: #374151; -fx-font-size: 13px;");

        // Error labels
        Label errTitre = errLabel();
        Label errDescription = errLabel();
        Label errType = errLabel();
        Label errDuree = errLabel();
        Label errNiveau = errLabel();
        Label errFichier = errLabel();

        // Pre-fill for edit
        if (isEdit) {
            tfTitre.setText(existing.getTitre());
            taDescription.setText(existing.getDescription() != null ? existing.getDescription() : "");
            if (existing.getTypeFichier() != null) cbType.setValue(existing.getTypeFichier().name());
            tfDuree.setText(String.valueOf(existing.getDuree()));
            if (existing.getNiveau() != null) cbNiveau.setValue(existing.getNiveau().name());
            if (existing.getFichier() != null) {
                tfFichier.setText(existing.getFichier());
                selectedFilePath[0] = existing.getFichier();
            }
            cbActif.setSelected(existing.isIsActive());
        }

        // --- Layout ---
        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 24, 8, 24));
        content.setPrefWidth(480);

        Label title = new Label(isEdit ? "✏  Modifier la séance" : "➕  Ajouter une séance");
        title.getStyleClass().add("form-title");
        Label subtitle = new Label("Catégorie : " + categorieCourante.getNom());
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6366f1; -fx-font-weight: bold;");
        Separator sep = new Separator();
        sep.getStyleClass().add("form-separator");

        // Scroll content
        VBox formBody = new VBox(8);

        formBody.getChildren().addAll(
                fieldGroup("Titre *", tfTitre, errTitre),
                fieldGroup("Description *", taDescription, errDescription),
                fieldGroupRow(
                        fieldGroup("Type de fichier *", cbType, errType),
                        fieldGroup("Niveau *", cbNiveau, errNiveau)
                ),
                fieldGroup("Durée (secondes) * — min 1, max 3600", tfDuree, errDuree),
                fieldGroup("Catégorie", tfCategorie, null),
                fieldGroup("Fichier (MP3 / MP4 — max 50MB) *", fileBox, errFichier),
                cbActif
        );

        ScrollPane scroll = new ScrollPane(formBody);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setPrefHeight(380);

        content.getChildren().addAll(title, subtitle, sep, scroll);

        // --- Dialog ---
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier une séance" : "Ajouter une séance");
        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");

        ButtonType btnConfirm = new ButtonType(isEdit ? "✓  Enregistrer" : "✓  Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel  = new ButtonType("✕  Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnConfirm, btnCancel);

        Button confirmBtn = (Button) dp.lookupButton(btnConfirm);
        confirmBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");
        ((Button) dp.lookupButton(btnCancel)).setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        // Validation
        confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean valid = true;

            // Reset
            clearErr(errTitre, tfTitre);
            clearErr(errDescription, taDescription);
            clearErr(errType, cbType);
            clearErr(errDuree, tfDuree);
            clearErr(errNiveau, cbNiveau);
            clearErr(errFichier, null);

            String titre = tfTitre.getText().trim();
            if (titre.isEmpty()) {
                showErr(errTitre, tfTitre, "⚠ Le titre est obligatoire."); valid = false;
            } else if (titre.length() < 4) {
                showErr(errTitre, tfTitre, "⚠ Le titre doit contenir au moins 4 caractères."); valid = false;
            }

            if (taDescription.getText().trim().isEmpty()) {
                showErr(errDescription, taDescription, "⚠ La description est obligatoire."); valid = false;
            }

            if (cbType.getValue() == null) {
                showErr(errType, cbType, "⚠ Le type est obligatoire."); valid = false;
            }

            String dureeStr = tfDuree.getText().trim();
            if (dureeStr.isEmpty()) {
                showErr(errDuree, tfDuree, "⚠ La durée est obligatoire."); valid = false;
            } else {
                try {
                    int duree = Integer.parseInt(dureeStr);
                    if (duree < 1 || duree > 3600) {
                        showErr(errDuree, tfDuree, "⚠ La durée doit être entre 1 et 3600 secondes."); valid = false;
                    }
                } catch (NumberFormatException e) {
                    showErr(errDuree, tfDuree, "⚠ Veuillez entrer un nombre valide."); valid = false;
                }
            }

            if (cbNiveau.getValue() == null) {
                showErr(errNiveau, cbNiveau, "⚠ Le niveau est obligatoire."); valid = false;
            }

            if (selectedFilePath[0] == null || selectedFilePath[0].isBlank()) {
                showErr(errFichier, null, "⚠ Veuillez choisir un fichier MP3 ou MP4."); valid = false;
            }

            if (!valid) event.consume();
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnConfirm) {
            SeanceMeditation s = isEdit ? existing : new SeanceMeditation();
            s.setTitre(tfTitre.getText().trim());
            s.setDescription(taDescription.getText().trim());
            s.setTypeFichier(TypeFichier.valueOf(cbType.getValue()));
            s.setDuree(Integer.parseInt(tfDuree.getText().trim()));
            s.setNiveau(NiveauMeditation.valueOf(cbNiveau.getValue()));
            s.setFichier(selectedFilePath[0]);
            s.setIsActive(cbActif.isSelected());
            s.setCategorieId(categorieCourante.getCategorieId());

            try {
                if (isEdit) {
                    seanceService.modifier(s);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅  Séance modifiée avec succès !");
                } else {
                    seanceService.ajouter(s);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅  Séance ajoutée avec succès !");
                }
                loadData();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌  Erreur base de données : " + e.getMessage());
            }
        }
    }

    // ==================== DELETE ====================

    private void confirmDelete(SeanceMeditation s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("🗑  Supprimer la séance");
        alert.setContentText("Voulez-vous vraiment supprimer la séance\n\"" + s.getTitre() + "\" ?\n\nCette action est irréversible.");
        styleAlert(alert);
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                seanceService.supprimer(s.getSeanceId());
                loadData();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅  Séance supprimée avec succès !");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌  " + e.getMessage());
            }
        }
    }

    // ==================== TOGGLE STATUT ====================

    private void toggleStatut(SeanceMeditation s) {
        String action = s.isIsActive() ? "désactiver" : "activer";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("⚡  Changer le statut");
        alert.setContentText("Voulez-vous vraiment " + action + " la séance\n\"" + s.getTitre() + "\" ?");
        styleAlert(alert);
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            s.setIsActive(!s.isIsActive());
            try {
                seanceService.modifier(s);
                loadData();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅  Séance " + (s.isIsActive() ? "activée" : "désactivée") + " avec succès !");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌  " + e.getMessage());
            }
        }
    }

    // ==================== CONSULTER (MEDIA PLAYER) ====================

    private void consulterSeance(SeanceMeditation s) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("▶️  " + s.getTitre());

        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        content.setPrefWidth(520);
        content.setStyle("-fx-background-color: white;");

        // Header
        Label titre = new Label(s.getTitre());
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");

        Label desc = new Label(s.getDescription() != null ? s.getDescription() : "Aucune description");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        // Info row
        HBox infoRow = new HBox(16);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        int min = s.getDuree() / 60, sec = s.getDuree() % 60;
        infoRow.getChildren().addAll(
                infoBadge("⏱ " + String.format("%d:%02d", min, sec), "#dbeafe", "#1d4ed8"),
                infoBadge("📊 " + (s.getNiveau() != null ? s.getNiveau().name() : "—"), "#fef3c7", "#b45309"),
                infoBadge(s.getTypeFichier() == TypeFichier.video ? "🎬 video" : "🎵 audio", "#fce7f3", "#9d174d"),
                infoBadge(s.isIsActive() ? "✅ Actif" : "⛔ Inactif",
                        s.isIsActive() ? "#dcfce7" : "#fee2e2",
                        s.isIsActive() ? "#15803d" : "#b91c1c")
        );

        if (s.getCreatedAt() != null) {
            Label dateLabel = new Label("📅 Créé le : " + DATE_FORMAT.format(s.getCreatedAt()));
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");
            content.getChildren().add(dateLabel);
        }

        Separator sep = new Separator();

        // Media Player
        VBox mediaBox = new VBox(8);
        mediaBox.setAlignment(Pos.CENTER);

        if (s.getFichier() != null && !s.getFichier().isBlank()) {
            try {
                File mediaFile = new File(s.getFichier());
                if (mediaFile.exists()) {
                    Media media = new Media(mediaFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);

                    if (s.getTypeFichier() == TypeFichier.video) {
                        MediaView mediaView = new MediaView(mediaPlayer);
                        mediaView.setFitWidth(460);
                        mediaView.setFitHeight(260);
                        mediaView.setPreserveRatio(true);
                        mediaBox.getChildren().add(mediaView);
                    } else {
                        Label audioIcon = new Label("🎵");
                        audioIcon.setStyle("-fx-font-size: 60px;");
                        Label audioLabel = new Label("Lecture audio en cours...");
                        audioLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6366f1;");
                        mediaBox.getChildren().addAll(audioIcon, audioLabel);
                    }

                    // Controls
                    HBox controls = new HBox(10);
                    controls.setAlignment(Pos.CENTER);
                    Button btnPlay = new Button("▶ Lecture");
                    Button btnPause = new Button("⏸ Pause");
                    Button btnStop = new Button("⏹ Arrêt");

                    String btnStyle = "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16 8 16; -fx-cursor: hand;";
                    btnPlay.setStyle(btnStyle);
                    btnPause.setStyle(btnStyle.replace("#6366f1", "#f59e0b"));
                    btnStop.setStyle(btnStyle.replace("#6366f1", "#ef4444"));

                    btnPlay.setOnAction(e -> mediaPlayer.play());
                    btnPause.setOnAction(e -> mediaPlayer.pause());
                    btnStop.setOnAction(e -> mediaPlayer.stop());

                    controls.getChildren().addAll(btnPlay, btnPause, btnStop);
                    mediaBox.getChildren().add(controls);

                    // Stop on close
                    dialog.setOnCloseRequest(e -> mediaPlayer.stop());
                } else {
                    mediaBox.getChildren().add(new Label("⚠️  Fichier introuvable : " + s.getFichier()));
                }
            } catch (Exception ex) {
                mediaBox.getChildren().add(new Label("⚠️  Impossible de charger le média."));
            }
        } else {
            mediaBox.getChildren().add(new Label("ℹ️  Aucun fichier média associé."));
        }

        content.getChildren().addAll(titre, desc, infoRow, sep, mediaBox);

        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");
        dp.getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dp.lookupButton(ButtonType.CLOSE)).setStyle(
                "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        dialog.showAndWait();
    }

    // ==================== RETOUR ====================

    @FXML
    private void retourCategories() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/org/example/views/CategorieMeditation.fxml"));
            javafx.scene.Node page = loader.load();

            javafx.scene.Node node = tableSeances.getScene().lookup("#contentArea");
            if (node instanceof StackPane) {
                ((StackPane) node).getChildren().setAll(page);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== HELPERS ====================

    private Label errLabel() {
        Label l = new Label();
        l.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private VBox fieldGroup(String labelText, javafx.scene.Node field, Label errLabel) {
        VBox g = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        g.getChildren().addAll(lbl, field);
        if (errLabel != null) g.getChildren().add(errLabel);
        return g;
    }

    private HBox fieldGroupRow(VBox left, VBox right) {
        HBox row = new HBox(10);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        row.getChildren().addAll(left, right);
        return row;
    }

    private void showErr(Label errLabel, javafx.scene.Node field, String msg) {
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
        if (field != null) field.setStyle(field.getStyle() + "; -fx-border-color: #ef4444;");
    }

    private void clearErr(Label errLabel, javafx.scene.Node field) {
        errLabel.setVisible(false);
        errLabel.setManaged(false);
        if (field != null) field.getStyleClass().remove("input-error");
    }

    private Label infoBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-padding: 4 10 4 10; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', sans-serif;");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());
    }
}