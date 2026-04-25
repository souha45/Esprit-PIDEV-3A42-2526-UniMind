package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.example.entities.*;
import org.example.enums.TypeFichier;
import org.example.services.*;
import org.example.services.GeminiRecommandationService.Recommandation;
import org.example.utils.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EtudiantSeancesController implements Initializable,
        SidebarEtudiantController.EtudiantPageController {

    // ── FXML injections ──────────────────────────────────────────────────
    @FXML private SidebarEtudiantController sidebarEtudiantController;
    @FXML private FlowPane categoriesGrid;
    @FXML private TextField tfSearchCat;
    @FXML private VBox postsContainer;
    @FXML private Button btnLoadMore;
    @FXML private BorderPane mainLayout;

    // Citation
    @FXML private Label lblCitation;
    @FXML private Label lblCitationAuteur;
    @FXML private Button btnRefreshCitation;

    // Recommandation émotionnelle
    @FXML private HBox  emojiRow;
    @FXML private Label lblEmotionSelectionnee;
    @FXML private TextArea taRessenti;
    @FXML private Button btnRecommander;
    @FXML private ProgressIndicator piChargement;
    @FXML private Label lblChargement;
    @FXML private VBox  recommandationsContainer;

    // ── Services ─────────────────────────────────────────────────────────
    private final CategorieMeditationServices catService   = new CategorieMeditationServices();
    private final SeanceMeditationServices    seanceService = new SeanceMeditationServices();
    private final PostServices                postService  = new PostServices();
    private final CommentaireServices         commentaireService = new CommentaireServices();
    private final EtudiantService             etudiantService   = new EtudiantService();
    private final GeminiRecommandationService geminiService     = new GeminiRecommandationService();

    // ── État ─────────────────────────────────────────────────────────────
    private User currentUser;
    private List<CategorieMeditation> allCategories = new ArrayList<>();
    private List<Post> allPosts = new ArrayList<>();
    private int postsPage = 0;
    private static final int POSTS_PER_PAGE = 5;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Émotion sélectionnée
    private String emotionSelectionnee = null;
    private VBox emojiActif = null;

    // Définition des émotions
    private static final String[][] EMOTIONS = {
            {"😊", "Joyeux / serein",        "#dcfce7", "#15803d"},
            {"😔", "Triste / mélancolique",   "#dbeafe", "#1d4ed8"},
            {"😫", "Stressé / submergé",      "#fce7f3", "#9d174d"},
            {"😤", "Énervé / frustré",        "#fee2e2", "#b91c1c"},
            {"😩", "Déprimé / fatigué",       "#fef3c7", "#b45309"},
            {"😐", "Neutre / calme",          "#f3f4f6", "#374151"},
            {"😰", "Anxieux / inquiet",       "#ede9fe", "#4338ca"},
    };

    private static final List<String[]> FALLBACK_CITATIONS = List.of(
            new String[]{"La paix vient de l'intérieur. Ne la cherchez pas à l'extérieur.", "Bouddha"},
            new String[]{"Chaque jour est une nouvelle chance de changer votre vie.", "Anonyme"},
            new String[]{"Respirez. Vous êtes exactement là où vous devez être.", "Anonyme"},
            new String[]{"Le bonheur n'est pas quelque chose de prêt à l'emploi. Il vient de vos propres actions.", "Dalaï Lama"},
            new String[]{"Prenez soin de votre corps, c'est le seul endroit où vous devez vivre.", "Jim Rohn"}
    );

    // ═════════════════════════════════════════════════════════════════════
    //  INIT
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buildEmojiRow();
    }

    @Override
    public void setUtilisateur(User user) {
        this.currentUser = user;
        if (sidebarEtudiantController != null) {
            sidebarEtudiantController.setUtilisateur(user);
            sidebarEtudiantController.setActiveButtonByFxml("/org/example/views/EtudiantSeances.fxml");
        }
        loadCategories();
        loadPosts();
        loadCitationDuJour();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ÉMOJIS — construction de la rangée
    // ═════════════════════════════════════════════════════════════════════

    private void buildEmojiRow() {
        emojiRow.getChildren().clear();
        for (String[] emotion : EMOTIONS) {
            String emoji   = emotion[0];
            String label   = emotion[1];
            String bgColor = emotion[2];
            String fgColor = emotion[3];

            VBox btn = new VBox(4);
            btn.setAlignment(Pos.CENTER);
            btn.setPrefWidth(78);
            btn.setPadding(new Insets(10, 8, 10, 8));
            btn.setStyle(emojiStyle(bgColor, fgColor, false));
            btn.setCursor(javafx.scene.Cursor.HAND);
            Tooltip.install(btn, new Tooltip(label));

            Label emojiLbl = new Label(emoji);
            emojiLbl.setStyle("-fx-font-size: 26px;");

            Label textLbl = new Label(label.split(" / ")[0]); // juste le 1er mot
            textLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + fgColor
                    + "; -fx-wrap-text: true; -fx-text-alignment: center;");
            textLbl.setWrapText(true);
            textLbl.setMaxWidth(70);

            btn.getChildren().addAll(emojiLbl, textLbl);

            btn.setOnMouseClicked(e -> selectEmotion(btn, label, bgColor, fgColor));
            btn.setOnMouseEntered(e -> {
                if (emojiActif != btn)
                    btn.setStyle(emojiStyle(bgColor, fgColor, true));
            });
            btn.setOnMouseExited(e -> {
                if (emojiActif != btn)
                    btn.setStyle(emojiStyle(bgColor, fgColor, false));
            });

            emojiRow.getChildren().add(btn);
        }
    }

    private void selectEmotion(VBox clicked, String label, String bg, String fg) {
        // Réinitialiser tous les boutons
        for (int i = 0; i < emojiRow.getChildren().size(); i++) {
            Node n = emojiRow.getChildren().get(i);
            if (n instanceof VBox v) {
                String[] e = EMOTIONS[i];
                v.setStyle(emojiStyle(e[2], e[3], false));
            }
        }
        // Activer celui cliqué
        clicked.setStyle(emojiStyleActive(bg, fg));
        emotionSelectionnee = label;

        lblEmotionSelectionnee.setText("✓  " + label + " sélectionné");
        lblEmotionSelectionnee.setVisible(true);
        lblEmotionSelectionnee.setManaged(true);

        // Réinitialiser les résultats précédents
        recommandationsContainer.setVisible(false);
        recommandationsContainer.setManaged(false);
        recommandationsContainer.getChildren().clear();
    }

    private String emojiStyle(String bg, String fg, boolean hover) {
        String border = hover ? fg : "transparent";
        return "-fx-background-color: " + bg + "; -fx-background-radius: 14;"
                + "-fx-border-color: " + border + "; -fx-border-width: 2;"
                + "-fx-border-radius: 14; -fx-cursor: hand;";
    }

    private String emojiStyleActive(String bg, String fg) {
        return "-fx-background-color: " + bg + "; -fx-background-radius: 14;"
                + "-fx-border-color: " + fg + "; -fx-border-width: 2.5;"
                + "-fx-border-radius: 14; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, " + fg + "55, 8, 0, 0, 2);";
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BOUTON RECOMMANDER
    // ─────────────────────────────────────────────────────────────────────
    @FXML
    private void onDemanderRecommandations() {
        if (emotionSelectionnee == null) {
            showInfo("Veuillez sélectionner une émotion avant de continuer.");
            return;
        }

        btnRecommander.setDisable(true);
        piChargement.setVisible(true);  piChargement.setManaged(true);
        lblChargement.setVisible(true); lblChargement.setManaged(true);
        recommandationsContainer.setVisible(false);
        recommandationsContainer.setManaged(false);
        recommandationsContainer.getChildren().clear();

        final String ressenti = taRessenti.getText().trim();

        CompletableFuture.supplyAsync(() -> {
            try {
                List<SeanceMeditation>    seances = seanceService.afficher();
                List<CategorieMeditation> cats    = catService.afficher();
                return geminiService.recommander(emotionSelectionnee, ressenti, seances, cats);
            } catch (Exception e) {
                System.err.println("Recommandation error: " + e.getMessage());
                return null;
            }
        }).whenComplete((result, error) -> Platform.runLater(() -> {
            btnRecommander.setDisable(false);
            piChargement.setVisible(false);  piChargement.setManaged(false);
            lblChargement.setVisible(false); lblChargement.setManaged(false);

            if (result != null && !result.isEmpty()) {
                afficherRecommandations(result);
            } else {
                Label errLbl = new Label(
                        "⚠️  Aucune séance disponible ou erreur de connexion. " +
                                "Vérifiez votre clé API Gemini et que des séances sont actives.");
                errLbl.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12px;");
                errLbl.setWrapText(true);
                recommandationsContainer.getChildren().add(errLbl);
                recommandationsContainer.setVisible(true);
                recommandationsContainer.setManaged(true);
            }
        }));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  AFFICHAGE DES 3 CARTES
    // ─────────────────────────────────────────────────────────────────────
    private void afficherRecommandations(List<GeminiRecommandationService.Recommandation> recs) {
        recommandationsContainer.getChildren().clear();

        // En-tête
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label sparkle = new Label("✨"); sparkle.setStyle("-fx-font-size:16px;");
        Label titreRec = new Label(
                "Recommandations pour « " + emotionSelectionnee + " »");
        titreRec.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#4338ca;");
        titreRec.setWrapText(true);
        header.getChildren().addAll(sparkle, titreRec);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#e0e7ff;");

        recommandationsContainer.getChildren().addAll(header, sep);

        // Rangée de cartes
        HBox cardsRow = new HBox(14);
        cardsRow.setAlignment(Pos.TOP_LEFT);
        for (int i = 0; i < recs.size(); i++)
            cardsRow.getChildren().add(buildRecommandationCard(recs.get(i), i + 1));

        recommandationsContainer.getChildren().add(cardsRow);
        recommandationsContainer.setVisible(true);
        recommandationsContainer.setManaged(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  CARTE D'UNE RECOMMANDATION
    // ─────────────────────────────────────────────────────────────────────
    private VBox buildRecommandationCard(
            GeminiRecommandationService.Recommandation rec, int numero) {

        SeanceMeditation seance = rec.seance;   // toujours non-null

        VBox card = new VBox(10);
        card.setPrefWidth(230);
        card.setMaxWidth(230);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:#e0e7ff;" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.08),10,0,0,3);"
        );

        // ── Numéro + type badge ──────────────────────────────────────
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label numBadge = new Label("#" + numero);
        numBadge.setStyle(
                "-fx-background-color:#6366f1;-fx-text-fill:white;" +
                        "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-padding:2 7;-fx-background-radius:20;");

        boolean isVideo = seance.getTypeFichier() == org.example.enums.TypeFichier.video;
        String typeBg   = isVideo ? "#dbeafe" : "#fce7f3";
        String typeFg   = isVideo ? "#1d4ed8" : "#9d174d";
        String typeIcon = isVideo ? "🎬" : "🎵";
        String typeNom  = isVideo ? "VIDEO" : "AUDIO";

        Label typeBadge = new Label(typeIcon + " " + typeNom);
        typeBadge.setStyle(
                "-fx-background-color:" + typeBg + ";-fx-text-fill:" + typeFg + ";" +
                        "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-padding:2 7;-fx-background-radius:20;");

        topRow.getChildren().addAll(numBadge, typeBadge);

        // ── Titre ────────────────────────────────────────────────────
        Label titre = new Label(seance.getTitre());
        titre.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#3730a3;");
        titre.setWrapText(true);

        // ── Description courte ───────────────────────────────────────
        String descText = seance.getDescription() != null ? seance.getDescription() : "";
        Label desc = new Label(descText.length() > 70 ? descText.substring(0, 70) + "…" : descText);
        desc.setStyle("-fx-font-size:11px;-fx-text-fill:#6b7280;");
        desc.setWrapText(true);

        // ── Méta (durée, niveau, catégorie) ─────────────────────────
        VBox metaBox = new VBox(4);
        metaBox.setStyle(
                "-fx-background-color:#f8f7ff;-fx-background-radius:8;-fx-padding:8;");

        int min = seance.getDuree() / 60, sec = seance.getDuree() % 60;
        metaBox.getChildren().addAll(
                metaLine("⏱", String.format("%d:%02d", min, sec)),
                metaLine("📊", seance.getNiveau() != null ? seance.getNiveau().name() : "—"),
                metaLine("🗂", rec.nomCategorie)
        );

        // ── Pourquoi ─────────────────────────────────────────────────
        Label pourquoi = new Label("💬 " + rec.pourquoi);
        pourquoi.setStyle(
                "-fx-font-size:11px;-fx-text-fill:#4338ca;-fx-font-style:italic;");
        pourquoi.setWrapText(true);

        // ── Bouton ▶ Démarrer ────────────────────────────────────────
        Button btnDemarrer = new Button("▶  Démarrer");
        btnDemarrer.setMaxWidth(Double.MAX_VALUE);
        btnDemarrer.setStyle(
                "-fx-background-color:#6366f1;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-font-size:12px;" +
                        "-fx-background-radius:10;-fx-padding:9 16;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.30),6,0,0,2);");

        // → Appelle exactement la même méthode que la page des séances
        btnDemarrer.setOnAction(e -> ouvrirSeanceDepuisRec(seance));

        card.getChildren().addAll(topRow, titre, desc, metaBox, pourquoi, btnDemarrer);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LECTEUR — copie exacte de EtudiantSeancesCategorieController.ouvrirSeance()
    //  (renommée pour éviter toute confusion)
    // ─────────────────────────────────────────────────────────────────────
    private void ouvrirSeanceDepuisRec(SeanceMeditation seance) {
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
                recBadge("⏱ " + String.format("%d:%02d", min, sec), "#dbeafe", "#1d4ed8"),
                recBadge("📊 " + (seance.getNiveau() != null ? seance.getNiveau().name() : "—"), "#fef3c7", "#b45309"),
                recBadge(seance.getTypeFichier() == org.example.enums.TypeFichier.video
                        ? "🎬 VIDEO" : "🎵 AUDIO", "#fce7f3", "#9d174d")
        );

        if (seance.getCreatedAt() != null) {
            Label dl = new Label("📅 Créé le : " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(seance.getCreatedAt()));
            dl.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
            content.getChildren().add(dl);
        }

        // Conseils sidebar
        VBox conseils = new VBox(8);
        conseils.setStyle(
                "-fx-background-color:#f0fdf4;-fx-background-radius:10;-fx-padding:14;" +
                        "-fx-border-color:#86efac;-fx-border-width:1;-fx-border-radius:10;");
        Label ct = new Label("💡 Conseils");
        ct.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#15803d;");
        conseils.getChildren().addAll(ct,
                conseilItemRec("🔇", "Environnement calme"),
                conseilItemRec("♡",  "Position confortable"),
                conseilItemRec("🌬", "Respiration consciente"),
                conseilItemRec("🕊", "Acceptation"));

        // Media
        VBox mediaBox = new VBox(10);
        mediaBox.setAlignment(Pos.CENTER);
        MediaPlayer[] playerRef = {null};

        if (seance.getFichier() != null && !seance.getFichier().isBlank()) {
            java.io.File file = new java.io.File(seance.getFichier());
            if (file.exists()) {
                try {
                    javafx.scene.media.Media media =
                            new javafx.scene.media.Media(file.toURI().toString());
                    MediaPlayer player = new MediaPlayer(media);
                    playerRef[0] = player;

                    if (seance.getTypeFichier() == org.example.enums.TypeFichier.video) {
                        javafx.scene.media.MediaView mv =
                                new javafx.scene.media.MediaView(player);
                        mv.setFitWidth(500); mv.setFitHeight(280); mv.setPreserveRatio(true);
                        mediaBox.getChildren().add(mv);
                    } else {
                        Label ai = new Label("🎵"); ai.setStyle("-fx-font-size:64px;");
                        Label al = new Label("Lecture en cours...");
                        al.setStyle("-fx-text-fill:#6366f1;-fx-font-size:14px;");
                        mediaBox.getChildren().addAll(ai, al);
                    }
                    mediaBox.getChildren().add(buildMediaControlsRec(player));
                    dialog.setOnCloseRequest(e -> player.stop());
                } catch (Exception ex) {
                    mediaBox.getChildren().add(new Label("⚠️ Impossible de charger le média."));
                }
            } else {
                mediaBox.getChildren().add(new Label("⚠️ Fichier introuvable : " + seance.getFichier()));
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
        try {
            dp.getStylesheets().add(
                    getClass().getResource("/css/etudiant.css").toExternalForm());
        } catch (Exception ignored) {}
        dp.setStyle("-fx-background-color:white;");
        dp.getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dp.lookupButton(ButtonType.CLOSE)).setStyle(
                "-fx-background-color:#6366f1;-fx-text-fill:white;" +
                        "-fx-background-radius:8;-fx-padding:9 20;");

        dialog.showAndWait();
        if (playerRef[0] != null) playerRef[0].stop();
    }

    //─────────────────────────────────────────────────────────────────────
    //  CONTRÔLES MÉDIA (identique à buildMediaControls, nommé _Rec)
    // ─────────────────────────────────────────────────────────────────────
    private VBox buildMediaControlsRec(MediaPlayer player) {
        javafx.scene.control.Slider progressBar = new javafx.scene.control.Slider(0, 1, 0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent:#6366f1;-fx-control-inner-background:#e0e7ff;");

        Label lblCur = new Label("0:00");
        lblCur.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;-fx-font-family:monospace;");
        Label lblTot = new Label("0:00");
        lblTot.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;-fx-font-family:monospace;");

        Button btnPP   = new Button("▶");
        Button btnStop = new Button("⏹");
        Button btnM10  = new Button("−10s");
        Button btnP10  = new Button("+10s");

        String base = "-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;" +
                "-fx-font-size:12px;-fx-font-weight:bold;";
        btnPP  .setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;" + base);
        btnStop.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" + base);
        btnM10 .setStyle("-fx-background-color:#e0e7ff;-fx-text-fill:#4338ca;" + base);
        btnP10 .setStyle("-fx-background-color:#e0e7ff;-fx-text-fill:#4338ca;" + base);

        java.util.function.Function<Double, String> fmt =
                s -> { int t = s.intValue(); return String.format("%d:%02d", t / 60, t % 60); };
        final boolean[] drag = {false};

        player.currentTimeProperty().addListener((obs, o, n) -> {
            if (!drag[0]) {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toSeconds() > 0)
                    progressBar.setValue(n.toSeconds() / total.toSeconds());
                lblCur.setText(fmt.apply(n.toSeconds()));
            }
        });
        player.setOnReady(() -> {
            javafx.util.Duration t = player.getTotalDuration();
            if (t != null) lblTot.setText(fmt.apply(t.toSeconds()));
        });
        player.setOnEndOfMedia(() -> {
            btnPP.setText("▶"); progressBar.setValue(0);
            player.seek(javafx.util.Duration.ZERO); player.stop();
        });

        progressBar.setOnMousePressed(e -> {
            drag[0] = true;
            javafx.util.Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double ss = progressBar.getValue() * total.toSeconds();
                player.seek(javafx.util.Duration.seconds(ss));
                lblCur.setText(fmt.apply(ss));
            }
        });
        progressBar.setOnMouseDragged(e -> {
            javafx.util.Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown()) {
                double ss = progressBar.getValue() * total.toSeconds();
                player.seek(javafx.util.Duration.seconds(ss));
                lblCur.setText(fmt.apply(ss));
            }
        });
        progressBar.setOnMouseReleased(e -> drag[0] = false);

        btnPP.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) { player.pause(); btnPP.setText("▶"); }
            else { player.play(); btnPP.setText("⏸"); }
        });
        btnStop.setOnAction(e -> {
            player.stop(); player.seek(javafx.util.Duration.ZERO);
            btnPP.setText("▶"); progressBar.setValue(0); lblCur.setText("0:00");
        });
        btnM10.setOnAction(e -> {
            javafx.util.Duration t = player.getCurrentTime().subtract(javafx.util.Duration.seconds(10));
            player.seek(t.lessThan(javafx.util.Duration.ZERO) ? javafx.util.Duration.ZERO : t);
        });
        btnP10.setOnAction(e -> {
            javafx.util.Duration t   = player.getCurrentTime().add(javafx.util.Duration.seconds(10));
            javafx.util.Duration tot = player.getTotalDuration();
            if (tot != null && t.greaterThan(tot)) t = tot;
            player.seek(t);
        });

        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        timeRow.getChildren().addAll(progressBar, lblCur, new Label("/"), lblTot);

        HBox btnRow = new HBox(8, btnPP, btnStop, btnM10, btnP10);
        btnRow.setAlignment(Pos.CENTER);

        VBox box = new VBox(6, timeRow, btnRow);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(6, 0, 0, 0));
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MICRO-HELPERS pour le lecteur
    // ─────────────────────────────────────────────────────────────────────
    private HBox metaLine(String icon, String text) {
        HBox row = new HBox(6); row.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon); ic.setStyle("-fx-font-size:12px;");
        Label tx = new Label(text); tx.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
        row.getChildren().addAll(ic, tx); return row;
    }

    /*private Label recBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-padding:4 10;-fx-background-radius:8;" +
                "-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }*/

    private HBox conseilItemRec(String icon, String text) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon); ic.setStyle("-fx-font-size:14px;");
        Label lb = new Label(text); lb.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;");
        row.getChildren().addAll(ic, lb); return row;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CITATION DU JOUR (inchangé)
    // ═════════════════════════════════════════════════════════════════════

    private void loadCitationDuJour() {
        lblCitation.setText("✦  Chargement de la citation...");
        lblCitationAuteur.setText("");
        btnRefreshCitation.setDisable(true);
        String today = LocalDate.now().toString();
        if (today.equals(CitationCache.getDate()) && CitationCache.getQuote() != null) {
            afficherCitation(CitationCache.getQuote(), CitationCache.getAuthor());
            btnRefreshCitation.setDisable(false);
            return;
        }
        fetchAndDisplay(false);
    }

    private void loadCitationRandom() {
        lblCitation.setText("✦  Chargement..."); lblCitationAuteur.setText("");
        btnRefreshCitation.setDisable(true);
        fetchAndDisplay(true);
    }

    private void fetchAndDisplay(boolean forceRandom) {
        CompletableFuture.supplyAsync(this::fetchRandom)
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    btnRefreshCitation.setDisable(false);
                    if (result != null && result.length == 2 && result[0] != null && !result[0].isBlank()) {
                        CitationCache.set(LocalDate.now().toString(), result[0], result[1]);
                        afficherCitation(result[0], result[1]);
                    } else {
                        String[] fb = FALLBACK_CITATIONS.get(new Random().nextInt(FALLBACK_CITATIONS.size()));
                        afficherCitation(fb[0], fb[1]);
                    }
                }));
    }

    private String[] fetchRandom() {
        try {
            URL url = new URL("https://zenquotes.io/api/random");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "UnimindApp/1.0");
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8); is.close(); conn.disconnect();
                String q = extractJsonField(json, "\"q\":\"", "\"");
                String a = extractJsonField(json, "\"a\":\"", "\"");
                if (q != null && !q.isBlank()) return new String[]{q, a != null ? a : "Anonyme"};
            }
            conn.disconnect();
        } catch (Exception e) { System.err.println("ZenQuotes: " + e.getMessage()); }
        return null;
    }

    private String extractJsonField(String json, String start, String end) {
        try {
            int s = json.indexOf(start); if (s == -1) return null;
            s += start.length(); int e = json.indexOf(end, s); if (e == -1) return null;
            return json.substring(s, e).replace("\\\"","\"").replace("\\n"," ").replace("\\u2019","'").replace("\\u2014","—");
        } catch (Exception e) { return null; }
    }

    private void afficherCitation(String quote, String author) {
        lblCitation.setText("« " + quote + " »");
        lblCitationAuteur.setText("— " + (author != null && !author.isBlank() ? author : "Anonyme"));
    }

    @FXML private void onRefreshCitation() { CitationCache.clear(); loadCitationRandom(); }

    public static class CitationCache {
        private static String date, quote, author;
        static void set(String d, String q, String a) { date = d; quote = q; author = a; }
        static void clear() { date = null; quote = null; author = null; }
        static String getDate()   { return date; }
        static String getQuote()  { return quote; }
        static String getAuthor() { return author; }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CATEGORIES (inchangé)
    // ═════════════════════════════════════════════════════════════════════

    private void loadCategories() {
        try { allCategories = catService.afficher(); renderCategories(allCategories); }
        catch (SQLException e) { e.printStackTrace(); }
    }

    private void renderCategories(List<CategorieMeditation> categories) {
        categoriesGrid.getChildren().clear();
        for (CategorieMeditation cat : categories)
            categoriesGrid.getChildren().add(buildCategoryCard(cat));
        if (categories.isEmpty()) {
            Label empty = new Label("Aucune catégorie trouvée.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
            categoriesGrid.getChildren().add(empty);
        }
    }

    private VBox buildCategoryCard(CategorieMeditation cat) {
        VBox card = new VBox(10);
        card.getStyleClass().add("cat-card");
        card.setPrefWidth(280); card.setMaxWidth(280);

        StackPane iconContainer = new StackPane();
        iconContainer.setAlignment(Pos.CENTER); iconContainer.setPrefHeight(70);
        if (cat.getIconUrl() != null && !cat.getIconUrl().isBlank()) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
                javafx.scene.image.Image img = new javafx.scene.image.Image(cat.getIconUrl(), 60, 60, true, true, true);
                img.errorProperty().addListener((obs, o, h) -> { if (h) { Label fb = new Label("🌸"); fb.setStyle("-fx-font-size:36px;"); iconContainer.getChildren().setAll(fb); } });
                iv.setImage(img); iv.setFitWidth(60); iv.setFitHeight(60); iv.setPreserveRatio(true);
                iconContainer.getChildren().add(iv);
            } catch (Exception e) { Label fb = new Label("🌸"); fb.setStyle("-fx-font-size:36px;"); iconContainer.getChildren().add(fb); }
        } else { Label e = new Label("🌸"); e.setStyle("-fx-font-size:36px;"); iconContainer.getChildren().add(e); }

        Label nom = new Label(cat.getNom()); nom.getStyleClass().add("cat-name"); nom.setWrapText(true);
        String dt = cat.getDescription() != null && !cat.getDescription().isBlank() ? cat.getDescription() : "Aucune description";
        Label desc = new Label(dt.length() > 80 ? dt.substring(0, 80) + "..." : dt);
        desc.getStyleClass().add("cat-desc"); desc.setWrapText(true);

        int nbSeances = 0;
        try { nbSeances = (int) seanceService.afficher().stream().filter(s -> s.getCategorieId() == cat.getCategorieId() && s.isIsActive()).count(); } catch (SQLException ignored) {}

        Label seancesLbl = new Label("🎵 " + nbSeances + " séances");
        seancesLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6366f1;-fx-font-weight:bold;");

        Button btnExplorer = new Button("🔍  Explorer");
        btnExplorer.getStyleClass().add("btn-explorer");
        btnExplorer.setMaxWidth(Double.MAX_VALUE);
        btnExplorer.setOnAction(e -> openSeancesCategorie(cat));

        card.getChildren().addAll(iconContainer, nom, desc, seancesLbl, btnExplorer);
        return card;
    }

    @FXML private void onSearchCategorie() {
        String q = tfSearchCat.getText().trim().toLowerCase();
        if (q.isEmpty()) { renderCategories(allCategories); return; }
        renderCategories(allCategories.stream()
                .filter(c -> (c.getNom() != null && c.getNom().toLowerCase().contains(q))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList()));
    }

    private void openSeancesCategorie(CategorieMeditation cat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/EtudiantSeancesCategorie.fxml"));
            Node page = loader.load();
            EtudiantSeancesCategorieController ctrl = loader.getController();
            ctrl.setUtilisateur(currentUser);
            ctrl.initWithCategorie(cat);
            BorderPane root = (BorderPane) categoriesGrid.getScene().lookup("#mainLayout");
            if (root == null && categoriesGrid.getScene().getRoot() instanceof BorderPane bp) root = bp;
            if (root != null) root.setCenter(page);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FORUM / POSTS (inchangé — copié tel quel)
    // ═════════════════════════════════════════════════════════════════════

    private void loadPosts() {
        try {
            allPosts = postService.afficher();
            allPosts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            postsPage = 0; postsContainer.getChildren().clear(); renderNextPosts();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void renderNextPosts() {
        int start = postsPage * POSTS_PER_PAGE, end = Math.min(start + POSTS_PER_PAGE, allPosts.size());
        for (int i = start; i < end; i++) postsContainer.getChildren().add(buildPostCard(allPosts.get(i)));
        postsPage++;
        boolean hasMore = end < allPosts.size();
        btnLoadMore.setVisible(hasMore); btnLoadMore.setManaged(hasMore);
    }

    @FXML private void loadMorePosts() { renderNextPosts(); }

    private VBox buildPostCard(Post post) {
        int uid = currentUser != null ? currentUser.getUserId() : -1;
        boolean isMine = post.getUserId() == uid;
        VBox card = new VBox(10); card.getStyleClass().add("post-card");

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        String authorName = "Anonyme";
        if (!post.isIsAnonyme()) { try { User a = etudiantService.getUserById(post.getUserId()); if (a != null) authorName = a.getPrenom() + " " + a.getNom(); } catch (SQLException ignored) {} }

        Label avatar = new Label(post.isIsAnonyme() ? "🎭" : "👤"); avatar.setStyle("-fx-font-size:22px;");
        VBox authorInfo = new VBox(2);
        Label authorLbl = new Label(authorName); authorLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#374151;");
        String catName = allCategories.stream().filter(c -> c.getCategorieId() == post.getCategorieId()).map(CategorieMeditation::getNom).findFirst().orElse("—");
        String dateStr = post.getUpdatedAt() != null ? (post.getUpdatedAt().equals(post.getCreatedAt()) ? "Créé le " : "Modifié le ") + DATE_FORMAT.format(post.getUpdatedAt()) : "";
        Label metaLbl = new Label("🗂️ " + catName + "  •  " + dateStr); metaLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
        authorInfo.getChildren().addAll(authorLbl, metaLbl);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(6); actions.setAlignment(Pos.CENTER_RIGHT);
        if (isMine) {
            Button btnEdit = new Button("✏️"); btnEdit.getStyleClass().addAll("btn-icon","btn-edit"); btnEdit.setOnAction(e -> openEditPostDialog(post, card));
            Button btnDel  = new Button("🗑️"); btnDel.getStyleClass().addAll("btn-icon","btn-delete"); btnDel.setOnAction(e -> deletePost(post, card));
            actions.getChildren().addAll(btnEdit, btnDel);
        }
        header.getChildren().addAll(avatar, authorInfo, spacer, actions);

        Label title = new Label(post.getTitre()); title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#3730a3;"); title.setWrapText(true);
        Label content = new Label(post.getContenu()); content.setStyle("-fx-font-size:13px;-fx-text-fill:#4b5563;"); content.setWrapText(true);

        VBox commentsSection = new VBox(8); commentsSection.setStyle("-fx-padding:10 0 0 0;");
        int[] nbC = {0}; try { nbC[0] = commentaireService.getCommentairesByPost(post.getPostId()).size(); } catch (SQLException ignored) {}

        HBox commentHeader = new HBox(10); commentHeader.setAlignment(Pos.CENTER_LEFT);
        Label nbCommLbl = new Label("💬 " + nbC[0] + " commentaire(s)"); nbCommLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        Button btnToggle = new Button("▼ Voir les commentaires"); btnToggle.getStyleClass().add("btn-toggle-comments");
        Button btnAddComment = new Button("➕ Commenter"); btnAddComment.getStyleClass().add("btn-comment");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        commentHeader.getChildren().addAll(nbCommLbl, sp2, btnToggle, btnAddComment);

        VBox commentsBody = new VBox(8); commentsBody.setVisible(false); commentsBody.setManaged(false);
        btnToggle.setOnAction(e -> {
            boolean showing = commentsBody.isVisible();
            if (!showing) { loadComments(post, commentsBody, nbCommLbl); btnToggle.setText("▲ Masquer"); }
            else { btnToggle.setText("▼ Voir les commentaires"); }
            commentsBody.setVisible(!showing); commentsBody.setManaged(!showing);
        });
        btnAddComment.setOnAction(e -> openAddCommentForm(post, commentsBody, commentsSection, nbCommLbl, btnToggle));

        commentsSection.getChildren().addAll(commentHeader, commentsBody);
        card.getChildren().addAll(header, title, content, new Separator(), commentsSection);
        return card;
    }

    private void loadComments(Post post, VBox commentsBody, Label nbCommLbl) {
        commentsBody.getChildren().clear();
        try {
            List<Commentaire> comments = commentaireService.getCommentairesByPost(post.getPostId());
            nbCommLbl.setText("💬 " + comments.size() + " commentaire(s)");
            for (Commentaire c : comments) commentsBody.getChildren().add(buildCommentCard(c, post, commentsBody, nbCommLbl));
            if (comments.isEmpty()) { Label e = new Label("Aucun commentaire."); e.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;-fx-padding:8 0 0 16;"); commentsBody.getChildren().add(e); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox buildCommentCard(Commentaire c, Post post, VBox commentsBody, Label nbCommLbl) {
        int uid = currentUser != null ? currentUser.getUserId() : -1;
        boolean isMine = c.getUserId() == uid;
        HBox row = new HBox(10); row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-padding:8 8 8 16;-fx-background-color:#f8f7ff;-fx-background-radius:8;-fx-border-color:#e0e7ff;-fx-border-width:1;-fx-border-radius:8;");
        Label avatar = new Label(c.isIsAnonyme() ? "🎭" : "👤"); avatar.setStyle("-fx-font-size:18px;");
        VBox body = new VBox(3); HBox.setHgrow(body, Priority.ALWAYS);
        String authorName = "Anonyme";
        if (!c.isIsAnonyme()) { try { User a = etudiantService.getUserById(c.getUserId()); if (a != null) authorName = a.getPrenom() + " " + a.getNom(); } catch (SQLException ignored) {} }
        HBox cH = new HBox(8); cH.setAlignment(Pos.CENTER_LEFT);
        Label aL = new Label(authorName); aL.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:#374151;");
        Label dL = new Label(c.getUpdatedAt() != null ? DATE_FORMAT.format(c.getUpdatedAt()) : ""); dL.setStyle("-fx-font-size:10px;-fx-text-fill:#9ca3af;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        if (isMine) {
            Button bE = new Button("✏️"); bE.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:12px;"); bE.setOnAction(e -> openEditCommentDialog(c, post, commentsBody, nbCommLbl));
            Button bD = new Button("🗑️"); bD.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:12px;"); bD.setOnAction(e -> deleteComment(c, post, commentsBody, nbCommLbl));
            cH.getChildren().addAll(aL, dL, sp, bE, bD);
        } else { cH.getChildren().addAll(aL, dL); }
        Label cL = new Label(c.getContenu()); cL.setStyle("-fx-font-size:13px;-fx-text-fill:#4b5563;"); cL.setWrapText(true);
        body.getChildren().addAll(cH, cL); row.getChildren().addAll(avatar, body);
        return row;
    }

    @FXML private void openNewPostDialog() { showPostDialog(null, null); }
    private void openEditPostDialog(Post post, VBox card) { showPostDialog(post, card); }

    private void showPostDialog(Post existing, VBox cardToReplace) {
        boolean isEdit = existing != null;
        ComboBox<String> cbCat = new ComboBox<>();
        Map<String, Integer> catMap = new LinkedHashMap<>();
        allCategories.forEach(c -> catMap.put(c.getNom(), c.getCategorieId()));
        cbCat.setItems(javafx.collections.FXCollections.observableArrayList(catMap.keySet()));
        cbCat.setPromptText("Choisir une catégorie..."); cbCat.setMaxWidth(Double.MAX_VALUE); cbCat.getStyleClass().add("filter-combo");
        TextField tfT = new TextField(); tfT.setPromptText("Titre (min 4 caractères)"); tfT.getStyleClass().add("form-input");
        TextArea taC = new TextArea(); taC.setPromptText("Votre message..."); taC.setPrefRowCount(4); taC.setWrapText(true); taC.getStyleClass().add("form-textarea");
        CheckBox cbA = new CheckBox("Publier anonymement"); cbA.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
        Label errT = errLbl(), errC = errLbl(), errCat = errLbl();
        if (isEdit) { tfT.setText(existing.getTitre()); taC.setText(existing.getContenu()); cbA.setSelected(existing.isIsAnonyme()); allCategories.stream().filter(c -> c.getCategorieId() == existing.getCategorieId()).findFirst().ifPresent(c -> cbCat.setValue(c.getNom())); }
        VBox content = new VBox(12); content.setPadding(new Insets(20,24,8,24)); content.setPrefWidth(460);
        Label title = new Label(isEdit ? "✏️  Modifier le post" : "✏️  Nouveau post"); title.getStyleClass().add("form-title");
        content.getChildren().addAll(title, new Separator(), fGroup("Catégorie *", cbCat, errCat), fGroup("Titre *", tfT, errT), fGroup("Contenu *", taC, errC), cbA);
        Dialog<ButtonType> dialog = new Dialog<>(); DialogPane dp = dialog.getDialogPane();
        dp.setContent(content); try { dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm()); } catch (Exception ignored) {}
        dp.setStyle("-fx-background-color:white;");
        ButtonType btnP = new ButtonType(isEdit ? "✓ Enregistrer" : "📢 Publier", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnA = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnP, btnA);
        ((Button) dp.lookupButton(btnP)).setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:9 20;");
        ((Button) dp.lookupButton(btnA)).setStyle("-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;-fx-background-radius:8;-fx-padding:9 20;");
        ((Button) dp.lookupButton(btnP)).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean valid = true;
            if (cbCat.getValue() == null) { showErr(errCat, "⚠ Catégorie obligatoire."); valid = false; }
            if (tfT.getText().trim().length() < 4) { showErr(errT, "⚠ Titre min 4 caractères."); valid = false; }
            if (taC.getText().trim().length() < 4) { showErr(errC, "⚠ Contenu min 4 caractères."); valid = false; }
            if (!valid) ev.consume();
        });
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == btnP) {
                try {
                    Post p = isEdit ? existing : new Post();
                    p.setTitre(tfT.getText().trim()); p.setContenu(taC.getText().trim()); p.setIsAnonyme(cbA.isSelected());
                    p.setCategorieId(catMap.get(cbCat.getValue()));
                    int uid = currentUser != null ? currentUser.getUserId() : Session.getInstance().getCurrentUser().getUserId();
                    p.setUserId(uid);
                    if (isEdit) postService.modifier(p); else postService.ajouter(p);
                    loadPosts();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void deletePost(Post post, VBox card) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle("Supprimer"); a.setHeaderText(null); a.setContentText("Supprimer ce post ?");
        a.showAndWait().ifPresent(btn -> { if (btn == ButtonType.OK) { try { postService.supprimer(post.getPostId()); postsContainer.getChildren().remove(card); } catch (SQLException e) { e.printStackTrace(); } } });
    }

    private void openAddCommentForm(Post post, VBox commentsBody, VBox commentsSection, Label nbCommLbl, Button btnToggle) {
        TextArea ta = new TextArea(); ta.setPromptText("Votre commentaire..."); ta.setPrefRowCount(3); ta.setWrapText(true); ta.getStyleClass().add("form-textarea");
        CheckBox cb = new CheckBox("Anonymement"); cb.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
        Label err = errLbl();
        VBox content = new VBox(12); content.setPadding(new Insets(20,24,8,24)); content.setPrefWidth(420);
        Label title = new Label("💬  Ajouter un commentaire"); title.getStyleClass().add("form-title");
        content.getChildren().addAll(title, new Separator(), fGroup("Commentaire *", ta, err), cb);
        Dialog<ButtonType> dialog = new Dialog<>(); DialogPane dp = dialog.getDialogPane();
        dp.setContent(content); try { dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm()); } catch (Exception ignored) {} dp.setStyle("-fx-background-color:white;");
        ButtonType btnP = new ButtonType("📢 Publier", ButtonBar.ButtonData.OK_DONE), btnA = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnP, btnA);
        ((Button) dp.lookupButton(btnP)).setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:9 20;");
        ((Button) dp.lookupButton(btnA)).setStyle("-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;-fx-background-radius:8;-fx-padding:9 20;");
        ((Button) dp.lookupButton(btnP)).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> { if (ta.getText().trim().length() < 4) { showErr(err, "⚠ Min 4 caractères."); ev.consume(); } });
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == btnP) {
                try {
                    Commentaire c = new Commentaire(); c.setContenu(ta.getText().trim()); c.setIsAnonyme(cb.isSelected()); c.setPostId(post.getPostId());
                    int uid = currentUser != null ? currentUser.getUserId() : Session.getInstance().getCurrentUser().getUserId(); c.setUserId(uid);
                    commentaireService.ajouter(c); commentsBody.setVisible(true); commentsBody.setManaged(true); btnToggle.setText("▲ Masquer"); loadComments(post, commentsBody, nbCommLbl);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void openEditCommentDialog(Commentaire c, Post post, VBox commentsBody, Label nbCommLbl) {
        TextArea ta = new TextArea(c.getContenu()); ta.setPrefRowCount(3); ta.setWrapText(true); ta.getStyleClass().add("form-textarea");
        CheckBox cb = new CheckBox("Anonymement"); cb.setSelected(c.isIsAnonyme()); cb.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
        Label err = errLbl();
        VBox content = new VBox(12); content.setPadding(new Insets(20,24,8,24)); content.setPrefWidth(420);
        Label title = new Label("✏️  Modifier"); title.getStyleClass().add("form-title");
        content.getChildren().addAll(title, new Separator(), fGroup("Commentaire *", ta, err), cb);
        Dialog<ButtonType> dialog = new Dialog<>(); DialogPane dp = dialog.getDialogPane();
        dp.setContent(content); try { dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm()); } catch (Exception ignored) {} dp.setStyle("-fx-background-color:white;");
        ButtonType btnS = new ButtonType("✓ Enregistrer", ButtonBar.ButtonData.OK_DONE), btnA = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnS, btnA);
        ((Button) dp.lookupButton(btnS)).setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:9 20;");
        ((Button) dp.lookupButton(btnA)).setStyle("-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;-fx-background-radius:8;-fx-padding:9 20;");
        dialog.showAndWait().ifPresent(btn -> { if (btn == btnS) { try { c.setContenu(ta.getText().trim()); c.setIsAnonyme(cb.isSelected()); commentaireService.modifier(c); loadComments(post, commentsBody, nbCommLbl); } catch (SQLException e) { e.printStackTrace(); } } });
    }

    private void deleteComment(Commentaire c, Post post, VBox commentsBody, Label nbCommLbl) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setHeaderText(null); a.setContentText("Supprimer ce commentaire ?");
        a.showAndWait().ifPresent(btn -> { if (btn == ButtonType.OK) { try { commentaireService.supprimer(c.getCommentaireId()); loadComments(post, commentsBody, nbCommLbl); } catch (SQLException e) { e.printStackTrace(); } } });
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private VBox fGroup(String labelText, Node field, Label errLabel) {
        VBox g = new VBox(5);
        Label lbl = new Label(labelText); lbl.getStyleClass().add("form-label");
        g.getChildren().addAll(lbl, field);
        if (errLabel != null) g.getChildren().add(errLabel);
        return g;
    }

    private Label errLbl() {
        Label l = new Label(); l.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11px;");
        l.setVisible(false); l.setManaged(false); return l;
    }

    private void showErr(Label l, String msg) { l.setText(msg); l.setVisible(true); l.setManaged(true); }

    private Label recBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-padding:4 10;-fx-background-radius:8;-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}