package org.example.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.text.TextAlignment;
import org.example.entities.*;
import org.example.enums.TypeFichier;
import org.example.services.*;
import org.example.services.FreesoundService.AmbientSound;
import org.example.services.FreesoundService.SoundCategory;
import org.example.utils.NavigationContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.scene.control.Slider;
import javafx.util.Duration;

public class EtudiantSeancesCategorieController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────
    @FXML private Label   lblNomCategorie;
    @FXML private Label   lblDescCategorie;
    @FXML private FlowPane seancesGrid;
    @FXML private VBox    ambientPanelContainer;   // ← placeholder dans le FXML

    // ── Services ─────────────────────────────────────────────────────────
    private final SeanceMeditationServices seanceService   = new SeanceMeditationServices();
    private final FavoriSeanceServices     favoriService   = new FavoriSeanceServices();
    private final FreesoundService         freesoundService = new FreesoundService();

    // ── État ─────────────────────────────────────────────────────────────
    private User               currentUser;
    private CategorieMeditation categorie;

    // Sons ambiants
    private MediaPlayer ambientPlayer   = null;
    private Button      activeSoundBtn  = null;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ═════════════════════════════════════════════════════════════════════
    //  INIT
    // ═════════════════════════════════════════════════════════════════════

    public void setUtilisateur(User user) { this.currentUser = user; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void initWithCategorie(CategorieMeditation cat) {
        this.categorie = cat;
        lblNomCategorie.setText(cat.getNom());
        String desc = cat.getDescription();
        lblDescCategorie.setText(desc != null && !desc.isBlank() ? "📝 " + desc : "");
        loadSeances();

        // Injecter le panneau sons ambiants dans son placeholder
        if (ambientPanelContainer != null) {
            ambientPanelContainer.getChildren().setAll(buildAmbientPanel());
        }

        Platform.runLater(() -> {
            ScrollPane sp = trouverScrollPaneCentral();
            if (sp != null) NavigationContext.setContentScrollPane(sp);
        });
    }

    private ScrollPane trouverScrollPaneCentral() {
        Node node = seancesGrid;
        while (node != null) {
            if (node instanceof ScrollPane) return (ScrollPane) node;
            node = node.getParent();
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  SÉANCES
    // ═════════════════════════════════════════════════════════════════════

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
                for (SeanceMeditation s : seances)
                    seancesGrid.getChildren().add(buildSeanceCard(s));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox buildSeanceCard(SeanceMeditation seance) {
        VBox card = new VBox(10);
        card.getStyleClass().add("seance-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        boolean isVideo = seance.getTypeFichier() == TypeFichier.video;
        Label typeBadge = new Label(isVideo ? "🎬 VIDEO" : "🎵 AUDIO");
        typeBadge.setStyle(isVideo
                ? "-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-padding:3 8;-fx-background-radius:8;-fx-font-size:11px;-fx-font-weight:bold;"
                : "-fx-background-color:#fce7f3;-fx-text-fill:#9d174d;-fx-padding:3 8;-fx-background-radius:8;-fx-font-size:11px;-fx-font-weight:bold;");

        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#3730a3;");
        titre.setWrapText(true);

        String dt = seance.getDescription() != null ? seance.getDescription() : "";
        Label desc = new Label(dt.length() > 80 ? dt.substring(0, 80) + "..." : dt);
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        desc.setWrapText(true);

        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        Label duree = new Label("⏱ " + String.format("%d:%02d", min, sec));
        duree.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-padding:3 8;-fx-background-radius:8;-fx-font-size:11px;");

        String niv = seance.getNiveau() != null ? seance.getNiveau().name() : "—";
        String nc = switch (niv) {
            case "DEBUTANT"      -> "-fx-background-color:#dcfce7;-fx-text-fill:#15803d;";
            case "INTERMEDIAIRE" -> "-fx-background-color:#fef3c7;-fx-text-fill:#b45309;";
            default              -> "-fx-background-color:#fee2e2;-fx-text-fill:#b91c1c;";
        };
        Label niveau = new Label("📊 " + niv);
        niveau.setStyle(nc + "-fx-padding:3 8;-fx-background-radius:8;-fx-font-size:11px;");

        HBox infoRow = new HBox(10, duree, niveau);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        // ── Favori ──
        boolean[] isFav = {false};
        int uid = currentUser != null ? currentUser.getUserId() : -1;
        try { if (uid > 0) isFav[0] = favoriService.isFavori(uid, seance.getSeanceId()); }
        catch (SQLException ignored) {}

        Button btnFavori = new Button(isFav[0] ? "❤" : "♡");
        btnFavori.setStyle("-fx-background-color:transparent;-fx-font-size:22px;-fx-cursor:hand;-fx-padding:0;");
        btnFavori.setTooltip(new Tooltip(isFav[0] ? "Retirer des favoris" : "Ajouter aux favoris"));
        btnFavori.setOnAction(e -> {
            if (uid < 0) { showInfo("Connectez-vous pour gérer vos favoris."); return; }
            try {
                if (isFav[0]) {
                    favoriService.getFavorisByUser(uid).stream()
                            .filter(f -> f.getSeanceId() == seance.getSeanceId()).findFirst()
                            .ifPresent(f -> { try { favoriService.supprimer(f.getId()); } catch (SQLException ex) { ex.printStackTrace(); } });
                    isFav[0] = false; btnFavori.setText("♡");
                    btnFavori.setTooltip(new Tooltip("Ajouter aux favoris"));
                } else {
                    FavoriSeance fav = new FavoriSeance();
                    fav.setUserId(uid); fav.setSeanceId(seance.getSeanceId());
                    favoriService.ajouter(fav);
                    isFav[0] = true; btnFavori.setText("❤");
                    btnFavori.setTooltip(new Tooltip("Retirer des favoris"));
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        Button btnDemarrer = new Button("▶ Démarrer");
        btnDemarrer.setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:8;-fx-cursor:hand;");
        btnDemarrer.setOnAction(e -> ouvrirSeance(seance));

        HBox actions = new HBox(10, btnFavori, btnDemarrer);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(typeBadge, titre, desc, infoRow, new Separator(), actions);
        return card;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LECTEUR SÉANCE (dialogue)
    // ═════════════════════════════════════════════════════════════════════

    private void ouvrirSeance(SeanceMeditation seance) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("▶ " + seance.getTitre());

        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        content.setPrefWidth(800);
        content.setStyle("-fx-background-color:white;");

        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#3730a3;");
        Label desc = new Label(seance.getDescription() != null ? seance.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:13px;-fx-text-fill:#6b7280;");

        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                badge("⏱ " + String.format("%d:%02d", min, sec), "#dbeafe", "#1d4ed8"),
                badge("📊 " + (seance.getNiveau() != null ? seance.getNiveau().name() : "—"), "#fef3c7", "#b45309"),
                badge(seance.getTypeFichier() == TypeFichier.video ? "🎬 VIDEO" : "🎵 AUDIO", "#fce7f3", "#9d174d")
        );

        if (seance.getCreatedAt() != null) {
            Label dl = new Label("📅 Créé le : " + DATE_FORMAT.format(seance.getCreatedAt()));
            dl.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
            content.getChildren().add(dl);
        }

        VBox conseils = new VBox(8);
        conseils.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:10;-fx-padding:14;-fx-border-color:#86efac;-fx-border-width:1;-fx-border-radius:10;");
        Label ct = new Label("💡 Conseils");
        ct.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#15803d;");
        conseils.getChildren().addAll(ct,
                conseilItem("🔇","Environnement calme"),
                conseilItem("♡","Position confortable"),
                conseilItem("🌬","Respiration consciente"),
                conseilItem("🕊","Acceptation"));

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
                        mv.setFitWidth(500); mv.setFitHeight(280); mv.setPreserveRatio(true);
                        mediaBox.getChildren().add(mv);
                    } else {
                        Label ai = new Label("🎵"); ai.setStyle("-fx-font-size:64px;");
                        Label al = new Label("Lecture en cours..."); al.setStyle("-fx-text-fill:#6366f1;-fx-font-size:14px;");
                        mediaBox.getChildren().addAll(ai, al);
                    }
                    mediaBox.getChildren().add(buildMediaControls(player));
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
        content.getChildren().add(mainContent);

        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color:white;");
        dp.getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dp.lookupButton(ButtonType.CLOSE)).setStyle(
                "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:9 20;");

        dialog.showAndWait();
        if (playerRef[0] != null) playerRef[0].stop();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CONTRÔLES MÉDIA (barre de progression YouTube-like)
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildMediaControls(MediaPlayer player) {
        Slider progressBar = new Slider(0, 1, 0);
        progressBar.setPrefWidth(460);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent:#6366f1;-fx-control-inner-background:#e0e7ff;");

        Label lblCurrent = new Label("0:00");
        lblCurrent.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;-fx-font-family:monospace;");
        Label lblTotal = new Label("0:00");
        lblTotal.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;-fx-font-family:monospace;");
        Label lblSep = new Label("/");
        lblSep.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;");

        Button btnPP    = new Button("▶");
        Button btnStop  = new Button("⏹");
        Button btnM10   = new Button("−10s");
        Button btnP10   = new Button("+10s");

        String base = "-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;-fx-font-size:12px;-fx-font-weight:bold;";
        btnPP  .setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;" + base);
        btnStop.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" + base);
        btnM10 .setStyle("-fx-background-color:#e0e7ff;-fx-text-fill:#4338ca;" + base);
        btnP10 .setStyle("-fx-background-color:#e0e7ff;-fx-text-fill:#4338ca;" + base);

        java.util.function.Function<Double, String> fmt = s -> {
            int t = s.intValue(); return String.format("%d:%02d", t / 60, t % 60);
        };
        final boolean[] dragging = {false};

        player.currentTimeProperty().addListener((obs, o, n) -> {
            if (!dragging[0]) {
                Duration total = player.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toSeconds() > 0)
                    progressBar.setValue(n.toSeconds() / total.toSeconds());
                lblCurrent.setText(fmt.apply(n.toSeconds()));
            }
        });
        player.setOnReady(() -> {
            Duration total = player.getTotalDuration();
            if (total != null) lblTotal.setText(fmt.apply(total.toSeconds()));
        });
        player.setOnEndOfMedia(() -> { btnPP.setText("▶"); progressBar.setValue(0); player.seek(Duration.ZERO); player.stop(); });

        progressBar.setOnMousePressed(e -> {
            dragging[0] = true;
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double ss = progressBar.getValue() * total.toSeconds();
                player.seek(Duration.seconds(ss)); lblCurrent.setText(fmt.apply(ss));
            }
        });
        progressBar.setOnMouseDragged(e -> {
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double ss = progressBar.getValue() * total.toSeconds();
                player.seek(Duration.seconds(ss)); lblCurrent.setText(fmt.apply(ss));
            }
        });
        progressBar.setOnMouseReleased(e -> dragging[0] = false);

        btnPP.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) { player.pause(); btnPP.setText("▶"); }
            else { player.play(); btnPP.setText("⏸"); }
        });
        btnStop.setOnAction(e -> { player.stop(); player.seek(Duration.ZERO); btnPP.setText("▶"); progressBar.setValue(0); lblCurrent.setText("0:00"); });
        btnM10.setOnAction(e -> { Duration t = player.getCurrentTime().subtract(Duration.seconds(10)); player.seek(t.lessThan(Duration.ZERO) ? Duration.ZERO : t); });
        btnP10.setOnAction(e -> { Duration t = player.getCurrentTime().add(Duration.seconds(10)); Duration tot = player.getTotalDuration(); if (tot != null && t.greaterThan(tot)) t = tot; player.seek(t); });

        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        timeRow.getChildren().addAll(progressBar, lblCurrent, lblSep, lblTotal);

        HBox btnRow = new HBox(8, btnPP, btnStop, btnM10, btnP10);
        btnRow.setAlignment(Pos.CENTER);

        VBox box = new VBox(6, timeRow, btnRow);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 0, 0, 0));
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PANNEAU SONS AMBIANTS (Freesound)
    // ═════════════════════════════════════════════════════════════════════

    private VBox buildAmbientPanel() {

        VBox panel = new VBox(12);
        panel.setPrefWidth(260);
        panel.setMaxWidth(260);
        panel.setPadding(new Insets(16));
        panel.setStyle(
                "-fx-background-color:#f5f3ff;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:#ddd6fe;" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:14;"
        );

        Label title = new Label("🎧 Sons ambiants");
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#4338ca;");

        Label subtitle = new Label("Accompagnez votre méditation avec un son naturel");
        subtitle.setStyle("-fx-font-size:11px;-fx-text-fill:#6b7280;");
        subtitle.setWrapText(true);

        // ── Grille 2×4 de catégories ──────────────────────────────────
        GridPane catGrid = new GridPane();
        catGrid.setHgap(8); catGrid.setVgap(8);

        // ── Zone résultats ────────────────────────────────────────────
        VBox resultsBox = new VBox(6);
        resultsBox.setStyle(
                "-fx-background-color:white;-fx-background-radius:10;" +
                        "-fx-border-color:#e0e7ff;-fx-border-width:1;-fx-border-radius:10;-fx-padding:10;"
        );
        resultsBox.setVisible(false);
        resultsBox.setManaged(false);

        Label resultsTitle = new Label();
        resultsTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4338ca;");

        VBox soundsList = new VBox(4);
        resultsBox.getChildren().addAll(resultsTitle, soundsList);

        // ── Spinner ───────────────────────────────────────────────────
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(28, 28);
        spinner.setVisible(false);
        spinner.setManaged(false);

        // ── Arrêter tout ──────────────────────────────────────────────
        Button btnStop = new Button("⏹  Arrêter le son");
        btnStop.setMaxWidth(Double.MAX_VALUE);
        btnStop.setStyle(
                "-fx-background-color:#fee2e2;-fx-text-fill:#b91c1c;" +
                        "-fx-background-radius:8;-fx-padding:8 12;-fx-cursor:hand;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;"
        );
        btnStop.setVisible(false);
        btnStop.setManaged(false);
        btnStop.setOnAction(e -> stopAmbient(btnStop, soundsList));

        // ── Volume ────────────────────────────────────────────────────
        HBox volumeRow = new HBox(8);
        volumeRow.setAlignment(Pos.CENTER_LEFT);
        Label lblVol = new Label("🔊"); lblVol.setStyle("-fx-font-size:14px;");
        Slider volSlider = new Slider(0, 1, 0.7);
        volSlider.setPrefWidth(160);
        volSlider.setStyle("-fx-accent:#6366f1;");
        volSlider.setTooltip(new Tooltip("Volume"));
        volSlider.valueProperty().addListener((obs, o, n) -> { if (ambientPlayer != null) ambientPlayer.setVolume(n.doubleValue()); });
        volumeRow.getChildren().addAll(lblVol, volSlider);

        // ── Boutons catégories ────────────────────────────────────────
        List<SoundCategory> cats = FreesoundService.CATEGORIES;
        for (int i = 0; i < cats.size(); i++) {
            SoundCategory cat = cats.get(i);
            Button btn = new Button(cat.emoji() + "\n" + cat.label());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle(catBtnStyle(false));
            btn.setWrapText(true);
            btn.setTextAlignment(TextAlignment.CENTER);
            GridPane.setHgrow(btn, Priority.ALWAYS);

            btn.setOnAction(e -> {
                // Réinitialiser le style de tous les boutons catégorie
                catGrid.getChildren().forEach(n -> {
                    if (n instanceof Button) n.setStyle(catBtnStyle(false));
                });
                btn.setStyle(catBtnStyle(true));

                // Reset résultats + spinner
                resultsBox.setVisible(false); resultsBox.setManaged(false);
                spinner.setVisible(true);     spinner.setManaged(true);
                soundsList.getChildren().clear();

                Task<List<AmbientSound>> task = new Task<>() {
                    @Override protected List<AmbientSound> call() throws Exception {
                        return freesoundService.search(cat.query(), cat.emoji());
                    }
                };
                task.setOnSucceeded(ev -> Platform.runLater(() -> {
                    spinner.setVisible(false); spinner.setManaged(false);
                    List<AmbientSound> sounds = task.getValue();
                    soundsList.getChildren().clear();
                    resultsTitle.setText(cat.emoji() + "  " + cat.label());
                    if (sounds.isEmpty()) {
                        soundsList.getChildren().add(ambientLabel("Aucun son trouvé.", "#9ca3af"));
                    } else {
                        for (AmbientSound s : sounds)
                            soundsList.getChildren().add(buildSoundRow(s, volSlider, btnStop));
                    }
                    resultsBox.setVisible(true); resultsBox.setManaged(true);
                }));
                task.setOnFailed(ev -> Platform.runLater(() -> {
                    spinner.setVisible(false); spinner.setManaged(false);
                    soundsList.getChildren().clear();
                    soundsList.getChildren().add(
                            ambientLabel("⚠️ Erreur. Vérifiez votre clé API et votre connexion.", "#ef4444")
                    );
                    resultsBox.setVisible(true); resultsBox.setManaged(true);
                }));
                new Thread(task).start();
            });

            catGrid.add(btn, i % 2, i / 2);
        }

        panel.getChildren().addAll(title, subtitle, new Separator(), catGrid, spinner, resultsBox, btnStop, volumeRow);
        return panel;
    }

    // ── Ligne d'un son ────────────────────────────────────────────────────
    private HBox buildSoundRow(AmbientSound sound, Slider volSlider, Button btnStopAll) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#f5f3ff;-fx-background-radius:8;-fx-padding:6 8;");

        Label nameLbl = new Label(sound.emoji + " " + (sound.name.length() > 22 ? sound.name.substring(0,22) + "…" : sound.name));
        nameLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Button btnPlay = new Button("▶");
        btnPlay.setStyle(soundBtnStyle(false));
        btnPlay.setTooltip(new Tooltip(sound.name));

        btnPlay.setOnAction(e -> {
            // Si déjà en lecture → arrêter
            if (ambientPlayer != null && activeSoundBtn == btnPlay) {
                stopAmbient(btnStopAll, null); return;
            }
            // Arrêter le précédent
            if (ambientPlayer != null) {
                ambientPlayer.stop(); ambientPlayer.dispose(); ambientPlayer = null;
                if (activeSoundBtn != null) { activeSoundBtn.setText("▶"); activeSoundBtn.setStyle(soundBtnStyle(false)); }
            }
            // Démarrer
            try {
                Media media = new Media(sound.previewUrl);
                MediaPlayer player = new MediaPlayer(media);
                player.setVolume(volSlider.getValue());
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.play();
                ambientPlayer = player;
                activeSoundBtn = btnPlay;
                btnPlay.setText("⏹");
                btnPlay.setStyle(soundBtnStyle(true));
                btnStopAll.setVisible(true); btnStopAll.setManaged(true);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        row.getChildren().addAll(nameLbl, btnPlay);
        return row;
    }

    /** Arrête proprement le son ambiant en cours. */
    private void stopAmbient(Button btnStopAll, VBox soundsList) {
        if (ambientPlayer != null) { ambientPlayer.stop(); ambientPlayer.dispose(); ambientPlayer = null; }
        if (activeSoundBtn != null) { activeSoundBtn.setText("▶"); activeSoundBtn.setStyle(soundBtnStyle(false)); activeSoundBtn = null; }
        if (btnStopAll != null) { btnStopAll.setVisible(false); btnStopAll.setManaged(false); }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════════════════════════════════════

    @FXML
    private void retourCategories() {
        // Arrêter le son ambiant avant de partir
        if (ambientPlayer != null) { ambientPlayer.stop(); ambientPlayer.dispose(); ambientPlayer = null; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/EtudiantSeances.fxml"));
            Node page = loader.load();
            BorderPane mainLayout = (BorderPane) seancesGrid.getScene().lookup("#mainLayout");
            if (mainLayout != null) mainLayout.setCenter(page);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private String catBtnStyle(boolean active) {
        return active
                ? "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-background-radius:10;-fx-padding:8 6;-fx-cursor:hand;-fx-font-size:11px;-fx-font-weight:bold;-fx-alignment:center;"
                : "-fx-background-color:#ede9fe;-fx-text-fill:#4338ca;-fx-background-radius:10;-fx-padding:8 6;-fx-cursor:hand;-fx-font-size:11px;-fx-font-weight:bold;-fx-alignment:center;";
    }

    private String soundBtnStyle(boolean active) {
        return active
                ? "-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:4 10;-fx-cursor:hand;-fx-font-size:11px;"
                : "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:4 10;-fx-cursor:hand;-fx-font-size:11px;";
    }

    private Label ambientLabel(String text, String color) {
        Label l = new Label(text); l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;"); l.setWrapText(true); return l;
    }

    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-padding:4 10;-fx-background-radius:8;-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }

    private HBox conseilItem(String icon, String text) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:14px;");
        Label lbl = new Label(text); lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
        row.getChildren().addAll(ico, lbl);
        return row;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}