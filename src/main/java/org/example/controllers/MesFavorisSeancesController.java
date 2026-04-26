package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.util.Duration;
import org.example.entities.*;
import org.example.enums.TypeFichier;
import org.example.services.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MesFavorisSeancesController implements Initializable,
        SidebarEtudiantController.EtudiantPageController {

    @FXML private FlowPane favorisGrid;
    @FXML private SidebarEtudiantController sidebarEtudiantController;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private Label lblCount;

    private final FavoriSeanceServices    favoriService  = new FavoriSeanceServices();
    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();

    private User currentUser;

    // ✅ Remplies UNE SEULE FOIS dans loadFavoris()
    private List<SeanceMeditation> allFavoris  = new ArrayList<>();
    private List<FavoriSeance>     favorisList = new ArrayList<>();

    // ══════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialiser les filtres dès le chargement FXML
        setupFilters();
    }

    @Override
    public void setUtilisateur(User user) {
        this.currentUser = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml(
                    "/org/example/views/MesFavorisSeances.fxml");
        }
        // Charger les données après avoir l'utilisateur
        loadFavoris();
    }

    // ══════════════════════════════════════════
    //  CHARGEMENT
    // ══════════════════════════════════════════

    private void loadFavoris() {
        allFavoris.clear();
        favorisList.clear();
        favorisGrid.getChildren().clear();
        if (currentUser == null) return;

        try {
            favorisList = favoriService.getFavorisByUser(currentUser.getUserId());
            List<SeanceMeditation> all = seanceService.afficher();
            allFavoris = all.stream()
                    .filter(s -> favorisList.stream()
                            .anyMatch(f -> f.getSeanceId() == s.getSeanceId()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Afficher tout après chargement
        applyFilters();
    }

    // ══════════════════════════════════════════
    //  FILTRES
    // ══════════════════════════════════════════

    private void setupFilters() {
        cbType.setItems(FXCollections.observableArrayList(
                "Tous les types", "VIDEO", "AUDIO"));
        cbType.getSelectionModel().selectFirst();

        cbNiveau.setItems(FXCollections.observableArrayList(
                "Tous les niveaux", "DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
        cbNiveau.getSelectionModel().selectFirst();
    }

    @FXML private void onSearch() { applyFilters(); }
    @FXML private void onFilter() { applyFilters(); }

    @FXML
    private void onReset() {
        // ✅ Vider les filtres → applyFilters() affichera tout
        tfSearch.clear();
        cbType.getSelectionModel().selectFirst();
        cbNiveau.getSelectionModel().selectFirst();
        applyFilters();
    }

    private void applyFilters() {
        String query  = tfSearch  != null ? tfSearch.getText().trim().toLowerCase() : "";
        String type   = cbType    != null ? cbType.getValue()   : null;
        String niveau = cbNiveau  != null ? cbNiveau.getValue() : null;

        boolean noFilter =
                query.isEmpty()
                        && (type   == null || type.equals("Tous les types"))
                        && (niveau == null || niveau.equals("Tous les niveaux"));

        if (noFilter) {
            // ✅ Aucun filtre → toutes les favorites
            renderFavoris(allFavoris);
            updateCompteur(allFavoris.size(), false);
            return;
        }

        List<SeanceMeditation> filtered = allFavoris.stream().filter(s -> {
            boolean matchSearch = query.isEmpty()
                    || (s.getTitre()       != null && s.getTitre().toLowerCase().contains(query))
                    || (s.getDescription() != null && s.getDescription().toLowerCase().contains(query));

            boolean matchType = type == null || type.equals("Tous les types")
                    || (s.getTypeFichier() != null
                    && s.getTypeFichier().name().equalsIgnoreCase(type));

            boolean matchNiveau = niveau == null || niveau.equals("Tous les niveaux")
                    || (s.getNiveau() != null
                    && s.getNiveau().name().equalsIgnoreCase(niveau));

            return matchSearch && matchType && matchNiveau;
        }).collect(Collectors.toList());

        renderFavoris(filtered);
        updateCompteur(filtered.size(), true);
    }

    private void updateCompteur(int count, boolean isFiltered) {
        if (lblCount == null) return;
        lblCount.setText(isFiltered
                ? "✦ " + count + " / " + allFavoris.size() + " séance(s)"
                : count > 0 ? count + " séance(s) favorite(s)" : "");
    }

    // ══════════════════════════════════════════
    //  RENDU
    // ══════════════════════════════════════════

    private void renderFavoris(List<SeanceMeditation> seances) {
        favorisGrid.getChildren().clear();

        if (seances.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));

            Label icon = new Label(allFavoris.isEmpty() ? "🤍" : "🔍");
            icon.setStyle("-fx-font-size:48px;");

            Label msg = new Label(allFavoris.isEmpty()
                    ? "Aucune séance favorite pour l'instant."
                    : "Aucune séance ne correspond à vos filtres.");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9ca3af;-fx-font-weight:bold;");

            Label hint = new Label(allFavoris.isEmpty()
                    ? "Explorez les catégories et ajoutez des séances à vos favoris."
                    : "Essayez de modifier vos critères ou cliquez sur Réinitialiser.");
            hint.setStyle("-fx-font-size:13px;-fx-text-fill:#c4b5fd;");
            hint.setWrapText(true);
            hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            empty.getChildren().addAll(icon, msg, hint);

            if (!allFavoris.isEmpty()) {
                Button btnReset = new Button("↺  Voir toutes mes séances favorites");
                btnReset.setStyle(
                        "-fx-background-color:#ede9fe;-fx-text-fill:#6366f1;" +
                                "-fx-font-weight:bold;-fx-font-size:13px;" +
                                "-fx-background-radius:10;-fx-padding:10 20 10 20;-fx-cursor:hand;");
                btnReset.setOnAction(e -> onReset());
                empty.getChildren().add(btnReset);
            }

            favorisGrid.getChildren().add(empty);
            return;
        }

        for (SeanceMeditation s : seances) {
            FavoriSeance fav = favorisList.stream()
                    .filter(f -> f.getSeanceId() == s.getSeanceId())
                    .findFirst().orElse(null);
            favorisGrid.getChildren().add(buildFavoriCard(s, fav));
        }
    }

    private VBox buildFavoriCard(SeanceMeditation seance, FavoriSeance fav) {
        VBox card = new VBox(10);
        card.getStyleClass().add("seance-card");
        card.setPrefWidth(300);

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
        String nivStyle = switch (niv) {
            case "DEBUTANT"      -> "-fx-background-color:#dcfce7;-fx-text-fill:#15803d;";
            case "INTERMEDIAIRE" -> "-fx-background-color:#fef3c7;-fx-text-fill:#b45309;";
            default              -> "-fx-background-color:#fee2e2;-fx-text-fill:#b91c1c;";
        };
        Label niveau = new Label("📊 " + niv);
        niveau.setStyle(nivStyle + "-fx-padding:3 8;-fx-background-radius:8;-fx-font-size:11px;");

        HBox infoRow = new HBox(8, duree, niveau);

        Button btnFavori = new Button("❤");
        btnFavori.setStyle("-fx-background-color:transparent;-fx-font-size:22px;-fx-cursor:hand;-fx-padding:0;");
        btnFavori.setTooltip(new Tooltip("Retirer des favoris"));
        btnFavori.setOnAction(e -> {
            if (fav != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Retirer des favoris");
                confirm.setHeaderText(null);
                confirm.setContentText("Retirer \"" + seance.getTitre() + "\" de vos favoris ?");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.OK) {
                        try {
                            favoriService.supprimer(fav.getId());
                            loadFavoris();
                        } catch (SQLException ex) { ex.printStackTrace(); }
                    }
                });
            }
        });

        Button btnDemarrer = new Button("▶  Démarrer");
        btnDemarrer.setStyle(
                "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-font-size:12px;" +
                        "-fx-font-weight:bold;-fx-padding:8 16 8 16;-fx-background-radius:8;-fx-cursor:hand;");
        btnDemarrer.setOnAction(e -> ouvrirSeance(seance));

        HBox actions = new HBox(10, btnFavori, btnDemarrer);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(typeBadge, titre, desc, infoRow, new Separator(), actions);
        return card;
    }

    // ══════════════════════════════════════════
    //  LECTEUR MÉDIA
    // ══════════════════════════════════════════

    private void ouvrirSeance(SeanceMeditation seance) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("▶  " + seance.getTitre());

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
        boolean isVideo = seance.getTypeFichier() == TypeFichier.video;
        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                badge("⏱ " + String.format("%d:%02d", min, sec), "#dbeafe", "#1d4ed8"),
                badge("📊 " + (seance.getNiveau() != null ? seance.getNiveau().name() : "—"), "#fef3c7", "#b45309"),
                badge(isVideo ? "🎬 VIDEO" : "🎵 AUDIO", "#fce7f3", "#9d174d"));

        VBox conseils = buildConseils();
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
                        mv.setFitWidth(480); mv.setFitHeight(270); mv.setPreserveRatio(true);
                        mediaBox.getChildren().add(mv);
                    } else {
                        Label ai = new Label("🎵"); ai.setStyle("-fx-font-size:64px;");
                        Label al = new Label("Lecture en cours...");
                        al.setStyle("-fx-text-fill:#6366f1;-fx-font-size:14px;");
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
        dp.setStyle("-fx-background-color:white;");
        dp.getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dp.lookupButton(ButtonType.CLOSE)).setStyle(
                "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:9 20 9 20;");

        dialog.showAndWait();
        if (playerRef[0] != null) playerRef[0].stop();
    }

    private VBox buildConseils() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:10;-fx-padding:14;" +
                "-fx-border-color:#86efac;-fx-border-width:1;-fx-border-radius:10;");
        Label t = new Label("💡 Conseils");
        t.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#15803d;");
        box.getChildren().addAll(t,
                conseilItem("🔇", "Environnement calme"),
                conseilItem("🪑", "Position confortable"),
                conseilItem("🌬", "Respiration consciente"),
                conseilItem("🕊", "Acceptation"));
        return box;
    }

    private VBox buildMediaControls(MediaPlayer player) {
        Slider bar = new Slider(0, 1, 0);
        bar.setPrefWidth(460); bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent:#6366f1;-fx-control-inner-background:#e0e7ff;");

        Label lblC = new Label("0:00"); lblC.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;-fx-font-family:monospace;");
        Label lblT = new Label("0:00"); lblT.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;-fx-font-family:monospace;");
        Label sep  = new Label("/");   sep.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;");

        Button btnPP  = new Button("▶");
        Button btnSt  = new Button("⏹");
        Button btnM10 = new Button("−10s");
        Button btnP10 = new Button("+10s");

        String b = "-fx-background-radius:8;-fx-padding:7 14 7 14;-fx-cursor:hand;-fx-font-size:12px;-fx-font-weight:bold;";
        btnPP .setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;" + b);
        btnSt .setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" + b);
        btnM10.setStyle("-fx-background-color:#e0e7ff;-fx-text-fill:#4338ca;" + b);
        btnP10.setStyle("-fx-background-color:#e0e7ff;-fx-text-fill:#4338ca;" + b);

        java.util.function.Function<Double, String> fmt = s -> {
            int si = s.intValue(); return String.format("%d:%02d", si / 60, si % 60);
        };
        final boolean[] drag = {false};

        player.currentTimeProperty().addListener((o, ov, nv) -> {
            if (!drag[0]) {
                Duration tot = player.getTotalDuration();
                if (tot != null && !tot.isUnknown() && tot.toSeconds() > 0)
                    bar.setValue(nv.toSeconds() / tot.toSeconds());
                lblC.setText(fmt.apply(nv.toSeconds()));
            }
        });
        player.setOnReady(() -> { Duration t2 = player.getTotalDuration(); if (t2 != null) lblT.setText(fmt.apply(t2.toSeconds())); });
        player.setOnEndOfMedia(() -> { btnPP.setText("▶"); bar.setValue(0); player.seek(Duration.ZERO); player.stop(); });

        bar.setOnMousePressed(e -> {
            drag[0] = true;
            Duration tot = player.getTotalDuration();
            if (tot != null && !tot.isUnknown()) { double s = bar.getValue() * tot.toSeconds(); player.seek(Duration.seconds(s)); lblC.setText(fmt.apply(s)); }
        });
        bar.setOnMouseDragged(e -> {
            Duration tot = player.getTotalDuration();
            if (tot != null && !tot.isUnknown()) { double s = bar.getValue() * tot.toSeconds(); player.seek(Duration.seconds(s)); lblC.setText(fmt.apply(s)); }
        });
        bar.setOnMouseReleased(e -> drag[0] = false);

        btnPP.setOnAction(e -> { if (player.getStatus() == MediaPlayer.Status.PLAYING) { player.pause(); btnPP.setText("▶"); } else { player.play(); btnPP.setText("⏸"); } });
        btnSt.setOnAction(e -> { player.stop(); player.seek(Duration.ZERO); btnPP.setText("▶"); bar.setValue(0); lblC.setText("0:00"); });
        btnM10.setOnAction(e -> { Duration c = player.getCurrentTime(); Duration t2 = c.subtract(Duration.seconds(10)); player.seek(t2.lessThan(Duration.ZERO) ? Duration.ZERO : t2); });
        btnP10.setOnAction(e -> { Duration c = player.getCurrentTime(); Duration tot = player.getTotalDuration(); Duration t2 = c.add(Duration.seconds(10)); if (tot != null && t2.greaterThan(tot)) t2 = tot; player.seek(t2); });

        HBox timeRow = new HBox(6); timeRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(bar, Priority.ALWAYS);
        timeRow.getChildren().addAll(bar, lblC, sep, lblT);
        HBox btnRow = new HBox(8, btnPP, btnSt, btnM10, btnP10); btnRow.setAlignment(Pos.CENTER);
        VBox box = new VBox(6, timeRow, btnRow); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(6, 0, 0, 0)); box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ══════════════════════════════════════════
    //  NAVIGATION + HELPERS
    // ══════════════════════════════════════════

    @FXML
    private void retour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/views/EtudiantSeances.fxml"));
            Node page = loader.load();
            BorderPane ml = (BorderPane) favorisGrid.getScene().lookup("#mainLayout");
            if (ml != null) ml.setCenter(page);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-padding:4 10 4 10;-fx-background-radius:8;-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }

    private HBox conseilItem(String icon, String text) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:14px;");
        Label lbl = new Label(text); lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
        row.getChildren().addAll(ico, lbl);
        return row;
    }
}