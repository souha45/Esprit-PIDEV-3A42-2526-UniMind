package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import org.example.entities.*;
import org.example.enums.TypeFichier;
import org.example.services.*;
import org.example.utils.Session;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EtudiantSeancesCategorieController implements Initializable {

    @FXML private Label lblNomCategorie;
    @FXML private Label lblDescCategorie;
    @FXML private FlowPane seancesGrid;
    private User currentUser;

    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();
    private final FavoriSeanceServices favoriService = new FavoriSeanceServices();
    private CategorieMeditation categorie;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public void setUtilisateur(User user) {
        this.currentUser = user;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void initWithCategorie(CategorieMeditation cat) {
        this.categorie = cat;
        lblNomCategorie.setText(cat.getNom());
        String desc = cat.getDescription();
        lblDescCategorie.setText(desc != null && !desc.isBlank() ? "📝 " + desc : "");
        loadSeances();
    }

    private void loadSeances() {
        seancesGrid.getChildren().clear();
        try {
            List<SeanceMeditation> seances = seanceService.afficher().stream()
                    .filter(s -> s.getCategorieId() == categorie.getCategorieId() && s.isIsActive())
                    .collect(Collectors.toList());

            if (seances.isEmpty()) {
                Label empty = new Label("Aucune séance disponible pour cette catégorie.");
                empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
                seancesGrid.getChildren().add(empty);
            } else {
                for (SeanceMeditation s : seances) {
                    seancesGrid.getChildren().add(buildSeanceCard(s));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildSeanceCard(SeanceMeditation seance) {
        VBox card = new VBox(10);
        card.getStyleClass().add("seance-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        // Type badge + titre
        HBox typeRow = new HBox(8);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        boolean isVideo = seance.getTypeFichier() == TypeFichier.video;
        Label typeBadge = new Label(isVideo ? "🎬 VIDEO" : "🎵 AUDIO");
        typeBadge.setStyle(isVideo
                ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-background-color: #fce7f3; -fx-text-fill: #9d174d; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        typeRow.getChildren().add(typeBadge);

        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
        titre.setWrapText(true);

        // Description
        String descText = seance.getDescription() != null ? seance.getDescription() : "";
        Label desc = new Label(descText.length() > 80 ? descText.substring(0, 80) + "..." : descText);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        desc.setWrapText(true);

        // Durée + Niveau
        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        HBox infoRow = new HBox(10);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label duree = new Label("⏱ " + String.format("%d:%02d", min, sec));
        duree.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px;");

        String niv = seance.getNiveau() != null ? seance.getNiveau().name() : "—";
        String nivColor = switch (niv) {
            case "DEBUTANT" -> "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;";
            case "INTERMEDIAIRE" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;";
            default -> "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;";
        };
        Label niveau = new Label("📊 " + niv);
        niveau.setStyle(nivColor + " -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px;");

        infoRow.getChildren().addAll(duree, niveau);

        // Action buttons: Favori + Démarrer
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        // Favori button
        boolean[] isFav = {false};
        int currentUserId = (currentUser != null) ? currentUser.getUserId() : -1;

        try {
            if (currentUserId > 0) isFav[0] = favoriService.isFavori(currentUserId, seance.getSeanceId());
        } catch (SQLException ignored) {}

        Button btnFavori = new Button(isFav[0] ? "❤" : "♡");
        btnFavori.setStyle("-fx-background-color: transparent; -fx-font-size: 22px; -fx-cursor: hand; -fx-padding: 0;");
        btnFavori.setTooltip(new Tooltip(isFav[0] ? "Retirer des favoris" : "Ajouter aux favoris"));

        btnFavori.setOnAction(e -> {
            if (currentUserId < 0) { showInfo("Connectez-vous pour gérer vos favoris."); return; }
            try {
                if (isFav[0]) {
                    // Trouver l'id du favori et le supprimer
                    favoriService.getFavorisByUser(currentUserId).stream()
                            .filter(f -> f.getSeanceId() == seance.getSeanceId())
                            .findFirst().ifPresent(f -> {
                                try { favoriService.supprimer(f.getId()); } catch (SQLException ex) { ex.printStackTrace(); }
                            });
                    isFav[0] = false;
                    btnFavori.setText("♡");
                    btnFavori.setTooltip(new Tooltip("Ajouter aux favoris"));
                } else {
                    FavoriSeance fav = new FavoriSeance();
                    fav.setUserId(currentUserId);
                    fav.setSeanceId(seance.getSeanceId());
                    favoriService.ajouter(fav);
                    isFav[0] = true;
                    btnFavori.setText("❤");
                    btnFavori.setTooltip(new Tooltip("Retirer des favoris"));
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        // Démarrer button
        Button btnDemarrer = new Button("▶Démarrer");
        btnDemarrer.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 8 16 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDemarrer.setOnAction(e -> ouvrirSeance(seance));

        actions.getChildren().addAll(btnFavori, btnDemarrer);

        card.getChildren().addAll(typeRow, titre, desc, infoRow, new Separator(), actions);
        return card;
    }

    private void ouvrirSeance(SeanceMeditation seance) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("▶" + seance.getTitre());

        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        content.setPrefWidth(560);
        content.setStyle("-fx-background-color: white;");

        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");

        Label desc = new Label(seance.getDescription() != null ? seance.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                badge("⏱ " + String.format("%d:%02d", min, sec), "#dbeafe", "#1d4ed8"),
                badge("📊 " + (seance.getNiveau() != null ? seance.getNiveau().name() : "—"), "#fef3c7", "#b45309"),
                badge(seance.getTypeFichier() == TypeFichier.video ? "🎬 VIDEO" : "🎵 AUDIO", "#fce7f3", "#9d174d")
        );

        if (seance.getCreatedAt() != null) {
            Label dateLbl = new Label("📅 Créé le : " + DATE_FORMAT.format(seance.getCreatedAt()));
            dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
            content.getChildren().add(dateLbl);
        }

        // Conseils sidebar
        VBox conseils = new VBox(8);
        conseils.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10; -fx-padding: 14; -fx-border-color: #86efac; -fx-border-width: 1; -fx-border-radius: 10;");
        Label conseilsTitle = new Label("💡 Conseils");
        conseilsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #15803d;");
        conseils.getChildren().addAll(conseilsTitle,
                conseilItem("🔇", "Environnement calme"),
                conseilItem("♡", "Position confortable"),
                conseilItem("🌬", "Respiration consciente"),
                conseilItem("🕊", "Acceptation")
        );

        // Media
        VBox mediaBox = new VBox(10);
        mediaBox.setAlignment(Pos.CENTER);
        MediaPlayer[] playerRef = {null};

        if (seance.getFichier() != null && !seance.getFichier().isBlank()) {
            File file = new File(seance.getFichier());
            if (file.exists()) {
                try {
                    Media media = new Media(file.toURI().toString());
                    MediaPlayer player = new MediaPlayer(media);
                    playerRef[0] = player;

                    if (seance.getTypeFichier() == TypeFichier.video) {
                        MediaView mv = new MediaView(player);
                        mv.setFitWidth(500);
                        mv.setFitHeight(280);
                        mv.setPreserveRatio(true);
                        mediaBox.getChildren().add(mv);
                    } else {
                        Label audioIcon = new Label("🎵");
                        audioIcon.setStyle("-fx-font-size: 64px;");
                        Label audioLbl = new Label("Lecture en cours...");
                        audioLbl.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 14px;");
                        mediaBox.getChildren().addAll(audioIcon, audioLbl);
                    }

                    HBox controls = new HBox(10);
                    controls.setAlignment(Pos.CENTER);
                    Button btnPlay = styledBtn("▶ Lecture", "#6366f1");
                    Button btnPause = styledBtn("⏸ Pause", "#f59e0b");
                    Button btnStop = styledBtn("⏹ Arrêt", "#ef4444");
                    btnPlay.setOnAction(e -> player.play());
                    btnPause.setOnAction(e -> player.pause());
                    btnStop.setOnAction(e -> player.stop());
                    controls.getChildren().addAll(btnPlay, btnPause, btnStop);
                    mediaBox.getChildren().add(controls);

                    dialog.setOnCloseRequest(e -> player.stop());
                } catch (Exception ex) {
                    mediaBox.getChildren().add(new Label("⚠️ Impossible de charger le média."));
                }
            } else {
                mediaBox.getChildren().add(new Label("⚠️ Fichier introuvable."));
            }
        } else {
            mediaBox.getChildren().add(new Label("ℹ Aucun fichier média associé."));
        }

        HBox mainContent = new HBox(16);
        VBox left = new VBox(12, titre, desc, infoRow, new Separator(), mediaBox);
        HBox.setHgrow(left, Priority.ALWAYS);
        mainContent.getChildren().addAll(left, conseils);

        content.getChildren().addAll(mainContent);

        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");
        dp.getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dp.lookupButton(ButtonType.CLOSE)).setStyle(
                "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        dialog.showAndWait();
        if (playerRef[0] != null) playerRef[0].stop();
    }

    @FXML
    private void retourCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/EtudiantSeances.fxml"));
            Node page = loader.load();

            BorderPane mainLayout = (BorderPane) seancesGrid.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== HELPERS ====================

    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-padding: 4 10 4 10; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private HBox conseilItem(String icon, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 14px;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        row.getChildren().addAll(ico, lbl);
        return row;
    }

    private Button styledBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        return b;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}