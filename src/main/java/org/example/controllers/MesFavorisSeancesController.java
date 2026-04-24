package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.entities.*;
import org.example.enums.TypeFichier;
import org.example.services.*;
import org.example.utils.Session;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.media.*;
import java.io.File;

import javafx.application.Platform;
import javafx.scene.control.Slider;
import javafx.util.Duration;
import java.util.function.Function;

public class MesFavorisSeancesController implements SidebarEtudiantController.EtudiantPageController {

    @FXML private FlowPane favorisGrid;
    @FXML private SidebarEtudiantController sidebarEtudiantController;

    private final FavoriSeanceServices favoriService = new FavoriSeanceServices();
    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();

    private User currentUser;

    @Override
    public void setUtilisateur(User user) {
        this.currentUser = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml("/org/example/views/MesFavorisSeances.fxml");
        }
        loadFavoris();  // utilise currentUser au lieu de Session
    }

    @FXML
    public void initialize(URL url, ResourceBundle rb) {
    }

    private void loadFavoris() {
        favorisGrid.getChildren().clear();
        if (currentUser == null) return;
        int userId = currentUser.getUserId();
        if (userId < 0) return;

        try {
            List<FavoriSeance> favoris = favoriService.getFavorisByUser(userId);
            List<SeanceMeditation> allSeances = seanceService.afficher();

            List<SeanceMeditation> seancesFavorites = allSeances.stream()
                    .filter(s -> favoris.stream().anyMatch(f -> f.getSeanceId() == s.getSeanceId()))
                    .collect(Collectors.toList());

            if (seancesFavorites.isEmpty()) {
                VBox empty = new VBox(12);
                empty.setAlignment(Pos.CENTER);
                empty.setStyle("-fx-padding: 60;");
                Label icon = new Label("\uD83C\uDF38");
                icon.setStyle("-fx-font-size: 48px;");
                Label msg = new Label("Aucune séance favorite pour l'instant.");
                msg.setStyle("-fx-font-size: 15px; -fx-text-fill: #9ca3af;");
                Label hint = new Label("Explorez les catégories et ajoutez des séances à vos favoris.");
                hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #c4b5fd;");
                empty.getChildren().addAll(icon, msg, hint);
                favorisGrid.getChildren().add(empty);
            } else {
                for (SeanceMeditation s : seancesFavorites) {
                    FavoriSeance fav = favoris.stream()
                            .filter(f -> f.getSeanceId() == s.getSeanceId())
                            .findFirst().orElse(null);
                    favorisGrid.getChildren().add(buildFavoriCard(s, fav));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildFavoriCard(SeanceMeditation seance, FavoriSeance fav) {
        VBox card = new VBox(10);
        card.getStyleClass().add("seance-card");
        card.setPrefWidth(300);

        boolean isVideo = seance.getTypeFichier() == TypeFichier.video;
        Label typeBadge = new Label(isVideo ? "🎬 VIDEO" : "🎵 AUDIO");
        typeBadge.setStyle(isVideo
                ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-background-color: #fce7f3; -fx-text-fill: #9d174d; -fx-padding: 3 8; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
        titre.setWrapText(true);

        String descText = seance.getDescription() != null ? seance.getDescription() : "";
        Label desc = new Label(descText.length() > 80 ? descText.substring(0, 80) + "..." : descText);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        desc.setWrapText(true);

        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        Label duree = new Label("⏱ " + String.format("%d:%02d", min, sec));
        duree.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 3 8; -fx-background-radius: 8; -fx-font-size: 11px;");

        String niv = seance.getNiveau() != null ? seance.getNiveau().name() : "—";
        Label niveau = new Label("📊 " + niv);
        niveau.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-padding: 3 8; -fx-background-radius: 8; -fx-font-size: 11px;");

        HBox infoRow = new HBox(8, duree, niveau);

        // ✅ Bouton retirer des favoris
        Button btnFavori = new Button("❤️");
        btnFavori.setStyle("-fx-background-color: transparent; -fx-font-size: 22px; -fx-cursor: hand; -fx-padding: 0;");
        btnFavori.setTooltip(new Tooltip("Retirer des favoris"));
        btnFavori.setOnAction(e -> {
            if (fav != null) {
                try {
                    favoriService.supprimer(fav.getId());
                    loadFavoris();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });

        // ✅ Bouton démarrer
        Button btnDemarrer = new Button("▶️  Démarrer");
        btnDemarrer.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 8 16 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDemarrer.setOnAction(e -> ouvrirSeance(seance));

        HBox actions = new HBox(10, btnFavori, btnDemarrer);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(typeBadge, titre, desc, infoRow, new Separator(), actions);
        return card;
    }

    private void ouvrirSeance(SeanceMeditation seance) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("▶️  " + seance.getTitre());

        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        content.setPrefWidth(800);
        content.setStyle("-fx-background-color: white;");

        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");

        Label desc = new Label(seance.getDescription() != null ? seance.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        boolean isVideo = seance.getTypeFichier() == TypeFichier.video;

        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                badge("⏱ " + String.format("%d:%02d", min, sec), "#dbeafe", "#1d4ed8"),
                badge("📊 " + (seance.getNiveau() != null ? seance.getNiveau().name() : "—"), "#fef3c7", "#b45309"),
                badge(isVideo ? "🎬 VIDEO" : "🎵 AUDIO", "#fce7f3", "#9d174d")
        );

        // Conseils
        VBox conseils = new VBox(8);
        conseils.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10; -fx-padding: 14; " +
                "-fx-border-color: #86efac; -fx-border-width: 1; -fx-border-radius: 10;");
        Label conseilsTitle = new Label("💡 Conseils");
        conseilsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #15803d;");
        conseils.getChildren().addAll(conseilsTitle,
                conseilItem("🔇", "Environnement calme"),
                conseilItem("🪑", "Position confortable"),
                conseilItem("🌬️", "Respiration consciente"),
                conseilItem("🕊️", "Acceptation")
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

                    if (isVideo) {
                        MediaView mv = new MediaView(player);
                        mv.setFitWidth(480);
                        mv.setFitHeight(270);
                        mv.setPreserveRatio(true);
                        mediaBox.getChildren().add(mv);
                    } else {
                        Label audioIcon = new Label("🎵");
                        audioIcon.setStyle("-fx-font-size: 64px;");
                        Label audioLbl = new Label("Lecture en cours...");
                        audioLbl.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 14px;");
                        mediaBox.getChildren().addAll(audioIcon, audioLbl);
                    }

                    // ✅ APRÈS — une seule ligne
                    mediaBox.getChildren().add(buildMediaControls(player));

                    dialog.setOnCloseRequest(e -> player.stop());
                } catch (Exception ex) {
                    mediaBox.getChildren().add(new Label("⚠️ Impossible de charger le média."));
                }
            } else {
                mediaBox.getChildren().add(new Label("⚠️ Fichier introuvable."));
            }
        } else {
            mediaBox.getChildren().add(new Label("ℹ️ Aucun fichier média associé."));
        }

        HBox mainContent = new HBox(16);
        VBox left = new VBox(12, titre, desc, infoRow, new Separator(), mediaBox);
        HBox.setHgrow(left, Priority.ALWAYS);
        mainContent.getChildren().addAll(left, conseils);
        content.getChildren().add(mainContent);

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

    private VBox buildMediaControls(MediaPlayer player) {

        // ── Slider de progression ──────────────────────────────────────
        Slider progressBar = new Slider(0, 1, 0);
        progressBar.setPrefWidth(460);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle(
                "-fx-accent: #6366f1;" +                    // couleur du remplissage
                        "-fx-control-inner-background: #e0e7ff;"    // couleur du fond
        );

        // ── Labels de temps ───────────────────────────────────────────
        Label lblCurrent = new Label("0:00");
        lblCurrent.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-font-family: monospace;");
        Label lblTotal = new Label("0:00");
        lblTotal.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-font-family: monospace;");
        Label lblSep = new Label("/");
        lblSep.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

        // ── Boutons ───────────────────────────────────────────────────
        Button btnPlayPause = new Button("▶");
        Button btnStop      = new Button("⏹");
        Button btnMinus10   = new Button("−10s");
        Button btnPlus10    = new Button("+10s");

        String baseBtn = "-fx-background-radius: 8; -fx-padding: 7 14 7 14; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
        btnPlayPause.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; " + baseBtn);
        btnStop     .setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; " + baseBtn);
        btnMinus10  .setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; " + baseBtn);
        btnPlus10   .setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; " + baseBtn);

        btnPlayPause.setTooltip(new Tooltip("Lecture / Pause"));
        btnStop     .setTooltip(new Tooltip("Arrêter"));
        btnMinus10  .setTooltip(new Tooltip("Reculer de 10 secondes"));
        btnPlus10   .setTooltip(new Tooltip("Avancer de 10 secondes"));

        // ── Helpers ───────────────────────────────────────────────────
        java.util.function.Function<Double, String> fmtTime = secs -> {
            int s = secs.intValue();
            return String.format("%d:%02d", s / 60, s % 60);
        };

        // ── Mettre à jour la barre depuis le player ───────────────────
        final boolean[] userDragging = {false};

        player.currentTimeProperty().addListener((obs, oldT, newT) -> {
            if (!userDragging[0]) {
                Duration total = player.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toSeconds() > 0) {
                    double ratio = newT.toSeconds() / total.toSeconds();
                    progressBar.setValue(ratio);
                }
                lblCurrent.setText(fmtTime.apply(newT.toSeconds()));
            }
        });

        player.setOnReady(() -> {
            Duration total = player.getTotalDuration();
            if (total != null) {
                lblTotal.setText(fmtTime.apply(total.toSeconds()));
            }
        });

        player.setOnEndOfMedia(() -> {
            btnPlayPause.setText("▶");
            progressBar.setValue(0);
            player.seek(Duration.ZERO);
            player.stop();
        });

        // ── Seek par clic / drag sur la barre ─────────────────────────
        progressBar.setOnMousePressed(e -> {
            userDragging[0] = true;
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double seekSec = progressBar.getValue() * total.toSeconds();
                player.seek(Duration.seconds(seekSec));
                lblCurrent.setText(fmtTime.apply(seekSec));
            }
        });
        progressBar.setOnMouseDragged(e -> {
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double seekSec = progressBar.getValue() * total.toSeconds();
                player.seek(Duration.seconds(seekSec));
                lblCurrent.setText(fmtTime.apply(seekSec));
            }
        });
        progressBar.setOnMouseReleased(e -> userDragging[0] = false);

        // ── Actions boutons ───────────────────────────────────────────
        btnPlayPause.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                btnPlayPause.setText("▶");
            } else {
                player.play();
                btnPlayPause.setText("⏸");
            }
        });

        btnStop.setOnAction(e -> {
            player.stop();
            player.seek(Duration.ZERO);
            btnPlayPause.setText("▶");
            progressBar.setValue(0);
            lblCurrent.setText("0:00");
        });

        btnMinus10.setOnAction(e -> {
            Duration cur = player.getCurrentTime();
            Duration target = cur.subtract(Duration.seconds(10));
            player.seek(target.lessThan(Duration.ZERO) ? Duration.ZERO : target);
        });

        btnPlus10.setOnAction(e -> {
            Duration cur = player.getCurrentTime();
            Duration total = player.getTotalDuration();
            Duration target = cur.add(Duration.seconds(10));
            if (total != null && target.greaterThan(total)) target = total;
            player.seek(target);
        });

        // ── Assemblage ────────────────────────────────────────────────
        // Ligne 1 : barre de progression + temps
        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        timeRow.getChildren().addAll(progressBar, lblCurrent, lblSep, lblTotal);

        // Ligne 2 : boutons
        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.getChildren().addAll(btnPlayPause, btnStop, btnMinus10, btnPlus10);

        VBox controlsBox = new VBox(6, timeRow, btnRow);
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.setPadding(new Insets(6, 0, 0, 0));
        controlsBox.setMaxWidth(Double.MAX_VALUE);

        return controlsBox;
    }

    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-padding: 4 10 4 10; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private HBox conseilItem(String icon, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size: 14px;");
        Label lbl = new Label(text); lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        row.getChildren().addAll(ico, lbl);
        return row;
    }

    private Button styledBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        return b;
    }

    @FXML
    private void retour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/EtudiantSeances.fxml"));
            Node page = loader.load();
            // Récupérer le BorderPane parent (mainLayout) et remplacer le centre
            BorderPane mainLayout = (BorderPane) favorisGrid.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(page);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}