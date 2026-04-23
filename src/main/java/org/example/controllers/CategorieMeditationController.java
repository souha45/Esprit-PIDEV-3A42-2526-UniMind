package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.entities.CategorieMeditation;
import org.example.services.CategorieMeditationServices;
import org.example.services.SeanceMeditationServices;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategorieMeditationController implements Initializable {

    // Stats
    @FXML private Label lblNbCategories;
    @FXML private Label lblNbSeances;
    @FXML private Label lblDerniereDate;

    // Toolbar
    @FXML private TextField tfSearch;

    // Table
    @FXML private TableView<CategorieMeditation> tableCategories;
    @FXML private TableColumn<CategorieMeditation, String> colIcon;
    @FXML private TableColumn<CategorieMeditation, String> colNom;
    @FXML private TableColumn<CategorieMeditation, String> colDescription;
    @FXML private TableColumn<CategorieMeditation, String> colDate;
    @FXML private TableColumn<CategorieMeditation, String> colActions;

    // Services
    private final CategorieMeditationServices categorieService = new CategorieMeditationServices();
    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();

    // State
    private ObservableList<CategorieMeditation> allCategories = FXCollections.observableArrayList();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadData();
    }

    // ==================== TABLE SETUP ====================

    private void setupTable() {
        // Icon column
        colIcon.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label placeholder = new Label("\uD83C\uDF3C");

            {
                imageView.setFitWidth(36);
                imageView.setFitHeight(36);
                imageView.setPreserveRatio(true);
                placeholder.setStyle("-fx-font-size: 22px;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CategorieMeditation cat = (CategorieMeditation) getTableRow().getItem();
                    String iconUrl = cat.getIconUrl();
                    if (iconUrl != null && !iconUrl.isBlank()) {
                        try {
                            imageView.setImage(new Image(iconUrl, 36, 36, true, true, true));
                            setGraphic(imageView);
                        } catch (Exception e) {
                            setGraphic(placeholder);
                        }
                    } else {
                        setGraphic(placeholder);
                    }
                }
            }
        });

        // Nom column
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #3730a3;");
                }
            }
        });

        // Description column
        colDescription.setCellValueFactory(data -> {
            String desc = data.getValue().getDescription();
            if (desc == null || desc.isBlank()) return new SimpleStringProperty("—");
            return new SimpleStringProperty(desc.length() > 60 ? desc.substring(0, 60) + "..." : desc);
        });

        // Date column
        colDate.setCellValueFactory(data -> {
            if (data.getValue().getDateCreation() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(DATE_FORMAT.format(data.getValue().getDateCreation()));
        });

        // Actions column
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final Button btnView = new Button("👁");
            private final HBox box = new HBox(6, btnEdit, btnDelete, btnView);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().addAll("btn-icon", "btn-edit");
                btnDelete.getStyleClass().addAll("btn-icon", "btn-delete");
                btnView.getStyleClass().addAll("btn-icon", "btn-view");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                btnView.setTooltip(new Tooltip("Voir les séances"));

                btnEdit.setOnAction(e -> openEditDialog(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
                btnView.setOnAction(e -> viewSeances(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ==================== DATA LOADING ====================

    private void loadData() {
        try {
            List<CategorieMeditation> categories = categorieService.afficher();
            allCategories = FXCollections.observableArrayList(categories);
            tableCategories.setItems(allCategories);
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement : " + e.getMessage());
        }
    }

    private void updateStats() {
        lblNbCategories.setText(String.valueOf(allCategories.size()));
        try {
            lblNbSeances.setText(String.valueOf(seanceService.afficher().size()));
        } catch (SQLException e) {
            lblNbSeances.setText("—");
        }
        allCategories.stream()
                .filter(c -> c.getDateCreation() != null)
                .max((a, b) -> a.getDateCreation().compareTo(b.getDateCreation()))
                .ifPresentOrElse(
                        c -> lblDerniereDate.setText(DATE_FORMAT.format(c.getDateCreation())),
                        () -> lblDerniereDate.setText("—")
                );
    }

    // ==================== SEARCH ====================

    @FXML
    private void onSearch() {
        String query = tfSearch.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            tableCategories.setItems(allCategories);
        } else {
            List<CategorieMeditation> filtered = allCategories.stream()
                    .filter(c -> {
                        boolean matchNom = c.getNom() != null && c.getNom().toLowerCase().contains(query);
                        boolean matchDesc = c.getDescription() != null && c.getDescription().toLowerCase().contains(query);
                        return matchNom || matchDesc;
                    })
                    .collect(Collectors.toList());
            tableCategories.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    // ==================== MODAL FORM ====================

    @FXML
    private void openAddForm() {
        showFormDialog(null);
    }

    private void openEditDialog(CategorieMeditation cat) {
        showFormDialog(cat);
    }

    private void showFormDialog(CategorieMeditation existing) {
        boolean isEdit = (existing != null);

        // --- Fields ---
        TextField tfNom = new TextField();
        tfNom.setPromptText("Ex: Méditation Zen");
        tfNom.getStyleClass().add("form-input");

        TextArea taDescription = new TextArea();
        taDescription.setPromptText("Décrivez cette catégorie...");
        taDescription.setPrefRowCount(3);
        taDescription.setWrapText(true);
        taDescription.getStyleClass().add("form-textarea");

        TextField tfIconUrl = new TextField();
        tfIconUrl.setPromptText("https://exemple.com/icon.png");
        tfIconUrl.getStyleClass().add("form-input");

        Label errNom = new Label();
        errNom.getStyleClass().add("error-label");
        errNom.setVisible(false);
        errNom.setManaged(false);

        // Pre-fill if editing
        if (isEdit) {
            tfNom.setText(existing.getNom());
            taDescription.setText(existing.getDescription() != null ? existing.getDescription() : "");
            tfIconUrl.setText(existing.getIconUrl() != null ? existing.getIconUrl() : "");
        }

        // --- Layout ---
        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 8, 24));
        content.setPrefWidth(420);

        // Title
        Label title = new Label(isEdit ? "✏ Modifier la catégorie" : "➕  Ajouter une catégorie");
        title.getStyleClass().add("form-title");

        Label subtitle = new Label(isEdit ? "Modifiez les informations de la catégorie" : "Remplissez les informations de la nouvelle catégorie");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

        Separator sep = new Separator();
        sep.getStyleClass().add("form-separator");
        sep.setPadding(new Insets(4, 0, 4, 0));

        // Nom group
        VBox nomGroup = new VBox(5);
        Label lblNom = new Label("Nom de la catégorie *");
        lblNom.getStyleClass().add("form-label");
        nomGroup.getChildren().addAll(lblNom, tfNom, errNom);

        // Description group
        VBox descGroup = new VBox(5);
        Label lblDesc = new Label("Description (optionnel)");
        lblDesc.getStyleClass().add("form-label");
        descGroup.getChildren().addAll(lblDesc, taDescription);

        // Icon group
        VBox iconGroup = new VBox(5);
        Label lblIcon = new Label("URL de l'icône (optionnel)");
        lblIcon.getStyleClass().add("form-label");
        iconGroup.getChildren().addAll(lblIcon, tfIconUrl);

        content.getChildren().addAll(title, subtitle, sep, nomGroup, descGroup, iconGroup);

        // --- Dialog ---
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier une catégorie" : "Ajouter une catégorie");

        // Style the dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getStylesheets().add(getClass().getResource("/css/admin.css").toExternalForm());
        dialogPane.getStyleClass().add("form-dialog-pane");
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 16;");

        // Buttons
        ButtonType btnConfirm = new ButtonType(isEdit ? "✓  Enregistrer" : "✓  Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✕  Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(btnConfirm, btnCancel);

        // Style confirm button
        Button confirmBtn = (Button) dialogPane.lookupButton(btnConfirm);
        confirmBtn.getStyleClass().add("btn-primary");
        confirmBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        Button cancelBtn = (Button) dialogPane.lookupButton(btnCancel);
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        // Validation on confirm
        confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            errNom.setVisible(false);
            errNom.setManaged(false);
            tfNom.getStyleClass().remove("input-error");

            String nom = tfNom.getText().trim();
            if (nom.isEmpty()) {
                errNom.setText("⚠ Le nom est obligatoire.");
                errNom.setVisible(true);
                errNom.setManaged(true);
                tfNom.getStyleClass().add("input-error");
                event.consume();
            } else if (nom.length() < 4) {
                errNom.setText("⚠ Le nom doit contenir au moins 4 caractères.");
                errNom.setVisible(true);
                errNom.setManaged(true);
                tfNom.getStyleClass().add("input-error");
                event.consume();
            }
            // ---------- NOUVEAU : VÉRIFICATION D'UNICITÉ ----------
            else {
                boolean nomExisteDeja = allCategories.stream().anyMatch(c -> {
                    if (isEdit && c.getCategorieId() == existing.getCategorieId()) {
                        return false; // on s'autorise son propre nom
                    }
                    return c.getNom() != null && c.getNom().equalsIgnoreCase(nom);
                });
                if (nomExisteDeja) {
                    errNom.setText("⚠ Ce nom de catégorie existe déjà.");
                    errNom.setVisible(true);
                    errNom.setManaged(true);
                    tfNom.getStyleClass().add("input-error");
                    event.consume();  // empêche la fermeture du dialogue
                }
            }
            // -----------------------------------------------------
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == btnConfirm) {
            CategorieMeditation cat = isEdit ? existing : new CategorieMeditation();
            cat.setNom(tfNom.getText().trim());
            cat.setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
            cat.setIconUrl(tfIconUrl.getText().trim().isEmpty() ? null : tfIconUrl.getText().trim());

            try {
                if (isEdit) {
                    categorieService.modifier(cat);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅  Catégorie modifiée avec succès !");
                } else {
                    categorieService.ajouter(cat);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅  Catégorie ajoutée avec succès !");
                }
                loadData();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌  Erreur base de données : " + e.getMessage());
            }
        }
    }

    // ==================== DELETE ====================

    private void confirmDelete(CategorieMeditation cat) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("🗑  Supprimer la catégorie");
        alert.setContentText("Voulez-vous vraiment supprimer la catégorie\n\"" + cat.getNom() + "\" ?\n\nCette action est irréversible.");
        styleAlert(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categorieService.supprimer(cat.getCategorieId());
                loadData();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅  Catégorie supprimée avec succès !");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌  Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    // ==================== VIEW SEANCES ====================

    private void viewSeances(CategorieMeditation cat) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/org/example/views/SeanceMeditation.fxml"));
            javafx.scene.Node page = loader.load();

            // Passer la catégorie au controller des séances
            SeanceMeditationController controller = loader.getController();
            controller.initWithCategorie(cat);

            // Naviguer vers la page dans le contentArea
            javafx.scene.layout.StackPane contentArea =
                    (javafx.scene.layout.StackPane) tableCategories.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(page);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les séances : " + e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-font-family: 'Segoe UI', sans-serif;"
        );
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/admin.css").toExternalForm()
        );
    }
}