package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import org.example.entities.*;
import org.example.services.*;
import org.example.services.GeminiRecommandationService.Recommandation;
import org.example.utils.Session;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EtudiantSeancesController implements Initializable,
        SidebarEtudiantController.EtudiantPageController {

    @FXML private SidebarEtudiantController sidebarEtudiantController;
    @FXML private FlowPane categoriesGrid;
    @FXML private TextField tfSearchCat;
    @FXML private VBox postsContainer;
    @FXML private Button btnLoadMore;
    @FXML private BorderPane mainLayout;
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

    // Services
    private final CategorieMeditationServices catService     = new CategorieMeditationServices();
    private final SeanceMeditationServices    seanceService  = new SeanceMeditationServices();
    private final PostServices                postService    = new PostServices();
    private final CommentaireServices         commService    = new CommentaireServices();
    private final EtudiantService             etudiantService= new EtudiantService();
    private final ReactionService             reactionService= new ReactionService();
    private final ModerationService           moderationSvc  = new ModerationService();
    private final EmailService                emailService   = new EmailService();
    private final GeminiRecommandationService geminiService     = new GeminiRecommandationService();


    private User currentUser;
    private List<CategorieMeditation> allCategories = new ArrayList<>();
    private List<Post>                allPosts       = new ArrayList<>();
    private int  postsPage = 0;
    private static final int POSTS_PER_PAGE = 5;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Tailles maximales pour les médias
    private static final long MAX_IMAGE_SIZE = 5L  * 1024 * 1024;  // 5 MB
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;  // 50 MB
    private static final long MAX_FILE_SIZE  = 20L * 1024 * 1024;  // 20 MB

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
            new String[]{"Le bonheur vient de vos propres actions.", "Dalaï Lama"},
            new String[]{"Prenez soin de votre corps, c'est le seul endroit où vous devez vivre.", "Jim Rohn"},
            new String[]{"Vous n'avez pas à être parfait pour être incroyable.", "Anonyme"},
            new String[]{"Chaque moment est un nouveau départ.", "T.S. Eliot"},
            new String[]{"Croyez en vous et tout devient possible.", "Anonyme"}
    );

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

    // ══════════════════════════════════════════════════
    //  CITATION
    // ══════════════════════════════════════════════════

    private void loadCitationDuJour() {
        lblCitation.setText("✦  Chargement...");
        lblCitationAuteur.setText("");
        btnRefreshCitation.setDisable(true);
        String today = LocalDate.now().toString();
        if (today.equals(CitationCache.getDate()) && CitationCache.getQuote() != null) {
            afficherCitation(CitationCache.getQuote(), CitationCache.getAuthor());
            btnRefreshCitation.setDisable(false);
            return;
        }
        fetchAndDisplay();
    }

    private void fetchAndDisplay() {
        CompletableFuture.supplyAsync(this::fetchRandom)
                .whenComplete((result, err) -> Platform.runLater(() -> {
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
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "UnimindApp/1.0");
            if (conn.getResponseCode() == 200) {
                String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                conn.disconnect();
                String q = extractJson(json, "\"q\":\"", "\"");
                String a = extractJson(json, "\"a\":\"", "\"");
                if (q != null && !q.isBlank()) return new String[]{q, a != null ? a : "Anonyme"};
            }
        } catch (Exception e) { System.err.println("ZenQuotes: " + e.getMessage()); }
        return null;
    }

    private String extractJson(String json, String start, String end) {
        try {
            int s = json.indexOf(start); if (s == -1) return null;
            s += start.length();
            int e = json.indexOf(end, s); if (e == -1) return null;
            return json.substring(s, e).replace("\\\"","\"").replace("\\n"," ").replace("\\r","");
        } catch (Exception ex) { return null; }
    }

    private void afficherCitation(String q, String a) {
        lblCitation.setText("« " + q + " »");
        lblCitationAuteur.setText("— " + (a != null && !a.isBlank() ? a : "Anonyme"));
    }

    @FXML private void onRefreshCitation() { CitationCache.clear(); fetchAndDisplay(); }

    public static class CitationCache {
        private static String date, quote, author;
        public static void set(String d, String q, String a) { date=d; quote=q; author=a; }
        public static void clear() { date=null; quote=null; author=null; }
        public static String getDate() { return date; }
        public static String getQuote() { return quote; }
        public static String getAuthor() { return author; }
    }

    // ══════════════════════════════════════════════════
    //  CATEGORIES
    // ══════════════════════════════════════════════════

    private void loadCategories() {
        try { allCategories = catService.afficher(); renderCategories(allCategories); }
        catch (SQLException e) { e.printStackTrace(); }
    }

    private void renderCategories(List<CategorieMeditation> cats) {
        categoriesGrid.getChildren().clear();
        if (cats.isEmpty()) {
            Label l = new Label("Aucune catégorie."); l.setStyle("-fx-text-fill:#9ca3af;");
            categoriesGrid.getChildren().add(l); return;
        }
        cats.forEach(c -> categoriesGrid.getChildren().add(buildCategoryCard(c)));
    }

    private VBox buildCategoryCard(CategorieMeditation cat) {
        VBox card = new VBox(10); card.getStyleClass().add("cat-card");
        card.setPrefWidth(280); card.setMaxWidth(280);

        StackPane iconBox = new StackPane(); iconBox.setAlignment(Pos.CENTER); iconBox.setPrefHeight(70);
        if (cat.getIconUrl() != null && !cat.getIconUrl().isBlank()) {
            try {
                ImageView iv = new ImageView();
                Image img = new Image(cat.getIconUrl(), 60, 60, true, true, true);
                img.errorProperty().addListener((obs,ov,err) -> {
                    if (err) { Label l=new Label("🌸"); l.setStyle("-fx-font-size:36px;"); iconBox.getChildren().setAll(l); }
                });
                iv.setImage(img); iv.setFitWidth(60); iv.setFitHeight(60); iv.setPreserveRatio(true);
                iconBox.getChildren().add(iv);
            } catch (Exception e) { Label l=new Label("🌸"); l.setStyle("-fx-font-size:36px;"); iconBox.getChildren().add(l); }
        } else { Label l=new Label("🌸"); l.setStyle("-fx-font-size:36px;"); iconBox.getChildren().add(l); }

        Label nom = new Label(cat.getNom()); nom.getStyleClass().add("cat-name"); nom.setWrapText(true);
        String dt = cat.getDescription()!=null&&!cat.getDescription().isBlank() ? cat.getDescription() : "Aucune description";
        Label desc = new Label(dt.length()>80?dt.substring(0,80)+"...":dt);
        desc.getStyleClass().add("cat-desc"); desc.setWrapText(true);

        int ns=0,np=0;
        try {
            ns=(int)seanceService.afficher().stream().filter(s->s.getCategorieId()==cat.getCategorieId()&&s.isIsActive()).count();
            np=(int)postService.afficher().stream().filter(p->p.getCategorieId()==cat.getCategorieId()).count();
        } catch (SQLException ignored) {}

        HBox stats = new HBox(16); stats.setAlignment(Pos.CENTER_LEFT);
        Label sl = new Label("🎵 "+ns+" séances"); sl.setStyle("-fx-font-size:12px;-fx-text-fill:#6366f1;-fx-font-weight:bold;");
        Label pl = new Label("💬 "+np+" posts"); pl.setStyle("-fx-font-size:12px;-fx-text-fill:#10b981;-fx-font-weight:bold;");
        stats.getChildren().addAll(sl,pl);

        Button btnEx = new Button("🔍  Explorer"); btnEx.getStyleClass().add("btn-explorer");
        btnEx.setMaxWidth(Double.MAX_VALUE); btnEx.setOnAction(e->openSeancesCategorie(cat));

        card.getChildren().addAll(iconBox,nom,desc,stats,btnEx);
        return card;
    }

    @FXML private void onSearchCategorie() {
        String q = tfSearchCat.getText().trim().toLowerCase();
        renderCategories(q.isEmpty() ? allCategories : allCategories.stream()
                                                       .filter(c->(c.getNom()!=null&&c.getNom().toLowerCase().contains(q))
                                                                  ||(c.getDescription()!=null&&c.getDescription().toLowerCase().contains(q)))
                                                       .collect(Collectors.toList()));
    }

    private void openSeancesCategorie(CategorieMeditation cat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/EtudiantSeancesCategorie.fxml"));
            Node page = loader.load();
            EtudiantSeancesCategorieController ctrl = loader.getController();
            ctrl.setUtilisateur(currentUser); ctrl.initWithCategorie(cat);
            BorderPane root = (BorderPane) categoriesGrid.getScene().lookup("#mainLayout");
            if (root==null && categoriesGrid.getScene().getRoot() instanceof BorderPane bp) root=bp;
            if (root!=null) root.setCenter(page);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════
    //  POSTS
    // ══════════════════════════════════════════════════

    private void loadPosts() {
        try {
            allPosts = postService.afficher();
            allPosts.sort((a,b)->b.getCreatedAt().compareTo(a.getCreatedAt()));
            postsPage=0; postsContainer.getChildren().clear(); renderNextPosts();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void renderNextPosts() {
        int start=postsPage*POSTS_PER_PAGE, end=Math.min(start+POSTS_PER_PAGE,allPosts.size());
        for (int i=start;i<end;i++) postsContainer.getChildren().add(buildPostCard(allPosts.get(i)));
        postsPage++;
        boolean more = end<allPosts.size();
        btnLoadMore.setVisible(more); btnLoadMore.setManaged(more);
    }

    @FXML private void loadMorePosts() { renderNextPosts(); }

    private VBox buildPostCard(Post post) {
        int uid = currentUser!=null ? currentUser.getUserId() : -1;
        boolean isMyPost = post.getUserId()==uid;

        VBox card = new VBox(10); card.getStyleClass().add("post-card");

        // ── Header ──
        String authorName = "Anonyme";
        if (!post.isIsAnonyme()) {
            try { User a=etudiantService.getUserById(post.getUserId()); if(a!=null) authorName=a.getPrenom()+" "+a.getNom(); }
            catch (SQLException ignored) {}
        }
        Label avatar = new Label(post.isIsAnonyme()?"🎭":"👤"); avatar.setStyle("-fx-font-size:22px;");
        Label authorLbl = new Label(authorName); authorLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#374151;");
        String catName = allCategories.stream().filter(c->c.getCategorieId()==post.getCategorieId())
                .map(CategorieMeditation::getNom).findFirst().orElse("—");
        String dateStr = post.getUpdatedAt()!=null
                ? (post.getUpdatedAt().equals(post.getCreatedAt())?"Créé le ":"Modifié le ")+DATE_FMT.format(post.getUpdatedAt()):"";
        Label metaLbl = new Label("🗂 "+catName+"  •  "+dateStr); metaLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
        VBox authorInfo = new VBox(2, authorLbl, metaLbl);

        HBox actBtns = new HBox(6); actBtns.setAlignment(Pos.CENTER_RIGHT);
        if (isMyPost) {
            Button btnE = new Button("✏"); btnE.getStyleClass().addAll("btn-icon","btn-edit"); btnE.setTooltip(new Tooltip("Modifier")); btnE.setOnAction(e->openEditPostDialog(post,card));
            Button btnD = new Button("🗑"); btnD.getStyleClass().addAll("btn-icon","btn-delete"); btnD.setTooltip(new Tooltip("Supprimer")); btnD.setOnAction(e->deletePost(post,card));
            actBtns.getChildren().addAll(btnE,btnD);
        }
        Region sp = new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
        HBox header = new HBox(10,avatar,authorInfo,sp,actBtns); header.setAlignment(Pos.CENTER_LEFT);

        // ── Titre & Contenu ──
        Label title = new Label(post.getTitre()); title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#3730a3;"); title.setWrapText(true);

        // Rendu du texte formaté (gras/italique/souligné via marqueurs)
        javafx.scene.text.TextFlow contentLbl = buildFormattedLabel(post.getContenu());

        // ── Médias attachés ──
        VBox mediaBox = buildMediaPreview(post.getPostId());

        // ── Réactions ──
        HBox reactionsBar = buildReactionBar(post.getPostId(), true, -1);

        // ── Commentaires ──
        int[] nbComm = {0};
        try { nbComm[0]=commService.getCommentairesByPost(post.getPostId()).size(); } catch (SQLException ignored) {}
        Label nbCommLbl = new Label("💬 "+nbComm[0]+" commentaire(s)"); nbCommLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        Button btnToggle = new Button("▼ Voir les commentaires"); btnToggle.getStyleClass().add("btn-toggle-comments");
        Button btnAddComm = new Button("➕ Commenter"); btnAddComm.getStyleClass().add("btn-comment");
        Region sp2 = new Region(); HBox.setHgrow(sp2,Priority.ALWAYS);
        HBox commHeader = new HBox(10,nbCommLbl,sp2,btnToggle,btnAddComm); commHeader.setAlignment(Pos.CENTER_LEFT);

        VBox commBody = new VBox(8); commBody.setVisible(false); commBody.setManaged(false);
        btnToggle.setOnAction(e->{
            boolean show=commBody.isVisible();
            if(!show){loadComments(post,commBody,nbCommLbl); btnToggle.setText("▲ Masquer");}
            else btnToggle.setText("▼ Voir les commentaires");
            commBody.setVisible(!show); commBody.setManaged(!show);
        });
        btnAddComm.setOnAction(e->openAddCommentDialog(post,commBody,nbCommLbl,btnToggle));

        VBox commSection = new VBox(8,commHeader,commBody); commSection.setStyle("-fx-padding:10 0 0 0;");
        card.getChildren().addAll(header,title,contentLbl);
        if (!mediaBox.getChildren().isEmpty()) card.getChildren().add(mediaBox);
        card.getChildren().addAll(reactionsBar,new Separator(),commSection);
        return card;
    }

    // ══════════════════════════════════════════════════
    //  TEXTE FORMATÉ (Gras **text**, Italique _text_, Souligné __text__)
    // ══════════════════════════════════════════════════

    private javafx.scene.text.TextFlow buildFormattedLabel(String text) {
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow();
        flow.setStyle("-fx-line-spacing: 4;");
        flow.setPrefWidth(Double.MAX_VALUE);

        if (text == null || text.isBlank()) return flow;

        // Parser les marqueurs un par un
        int i = 0;
        while (i < text.length()) {

            // Gras : **texte**
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end != -1) {
                    javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(i + 2, end));
                    t.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-fill: #374151;");
                    flow.getChildren().add(t);
                    i = end + 2; continue;
                }
            }

            // Souligné : __texte__
            if (text.startsWith("__", i)) {
                int end = text.indexOf("__", i + 2);
                if (end != -1) {
                    javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(i + 2, end));
                    t.setStyle("-fx-underline: true; -fx-font-size: 13px; -fx-fill: #374151;");
                    flow.getChildren().add(t);
                    i = end + 2; continue;
                }
            }

            // Italique : _texte_
            if (text.charAt(i) == '_') {
                int end = text.indexOf('_', i + 1);
                if (end != -1) {
                    javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(i + 1, end));
                    t.setStyle("-fx-font-style: italic; -fx-font-size: 13px; -fx-fill: #374151;");
                    flow.getChildren().add(t);
                    i = end + 1; continue;
                }
            }

            // Code : `texte`
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end != -1) {
                    javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(i + 1, end));
                    t.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                            "-fx-fill: #6366f1; -fx-background-color: #f0f0f0;");
                    flow.getChildren().add(t);
                    i = end + 1; continue;
                }
            }

            // Texte normal : accumuler jusqu'au prochain marqueur
            int next = text.length();
            int b = text.indexOf("**", i);
            int u = text.indexOf("__", i);
            int it = text.indexOf('_', i);
            int co = text.indexOf('`', i);
            for (int n : new int[]{b, u, it, co}) if (n > i) next = Math.min(next, n);

            javafx.scene.text.Text t = new javafx.scene.text.Text(text.substring(i, next));
            t.setStyle("-fx-font-size: 13px; -fx-fill: #374151;");
            flow.getChildren().add(t);
            i = next;
        }
        return flow;
    }

    // ══════════════════════════════════════════════════
    //  MÉDIAS ATTACHÉS
    // ══════════════════════════════════════════════════

    private VBox buildMediaPreview(int postId) {
        VBox box = new VBox(8);
        try {
            String sql = "SELECT * FROM `post_media` WHERE `post_id` = ?";
            java.sql.Connection con =
                    org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, postId);
            java.sql.ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String type   = rs.getString("type");
                String chemin = rs.getString("chemin");
                String nom    = rs.getString("nom");
                long   taille = rs.getLong("taille");

                if ("IMAGE".equals(type)) {
                    try {
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
                        javafx.scene.image.Image img =
                                new javafx.scene.image.Image(
                                        new java.io.File(chemin).toURI().toString(),
                                        400, 250, true, true);
                        iv.setImage(img);
                        iv.setFitWidth(400); iv.setFitHeight(250);
                        iv.setPreserveRatio(true);
                        iv.setStyle("-fx-background-radius:10;");
                        box.getChildren().add(iv);
                    } catch (Exception ignored) {
                        box.getChildren().add(buildFileChip("🖼️", nom, taille));
                    }
                } else if ("VIDEO".equals(type)) {
                    box.getChildren().add(buildFileChip("🎬", nom, taille));
                } else {
                    box.getChildren().add(buildFileChip("📎", nom, taille));
                }
            }
            rs.close(); ps.close();
        } catch (java.sql.SQLException e) {
            System.err.println("Erreur chargement media: " + e.getMessage());
        }
        return box;
    }

    // ── Chip pour fichier non-image ──
    private HBox buildFileChip(String icon, String nom, long taille) {
        HBox chip = new HBox(8);
        chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color:#f5f3ff;-fx-background-radius:8;" +
                "-fx-border-color:#e0e7ff;-fx-border-width:1;-fx-border-radius:8;" +
                "-fx-padding:6 12 6 12;");
        Label ico  = new Label(icon); ico.setStyle("-fx-font-size:16px;");
        Label name = new Label(nom + "  (" + (taille/1024) + " Ko)");
        name.setStyle("-fx-font-size:12px;-fx-text-fill:#6366f1;-fx-font-weight:bold;");
        chip.getChildren().addAll(ico, name);
        return chip;
    }

    // ══════════════════════════════════════════════════
    //  BARRE DE RÉACTIONS
    // ══════════════════════════════════════════════════

    private HBox buildReactionBar(int entityId, boolean isPost, int commId) {
        HBox bar = new HBox(6); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-padding: 6 0 4 0;");

        int targetId = isPost ? entityId : commId;
        int uid = currentUser!=null ? currentUser.getUserId() : -1;

        String myReaction = null;
        Map<String,Integer> compteurs = new LinkedHashMap<>();
        try {
            if (isPost) { myReaction=reactionService.getReactionPost(entityId,uid); compteurs=reactionService.getCompteursPost(entityId); }
            else { myReaction=reactionService.getReactionCommentaire(commId,uid); compteurs=reactionService.getCompteursCommentaire(commId); }
        } catch (SQLException ignored) {}

        Map<String,Integer> finalCompteurs = compteurs;
        String finalMyReaction = myReaction;

        for (String reaction : ReactionService.REACTIONS) {
            int count = finalCompteurs.getOrDefault(reaction, 0);
            boolean isSelected = reaction.equals(finalMyReaction);

            Button btn = new Button(reaction + (count > 0 ? " " + count : ""));
            btn.setStyle(
                    "-fx-font-size: 13px; -fx-padding: 3 8 3 8; -fx-cursor: hand; " +
                            "-fx-background-radius: 20; -fx-border-radius: 20; " +
                            (isSelected
                                    ? "-fx-background-color: #ede9fe; -fx-border-color: #6366f1; -fx-border-width: 1.5;"
                                    : "-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-width: 1;")
            );

            btn.setOnAction(e -> {
                if (uid < 0) { showAlert("Connectez-vous pour réagir."); return; }
                try {
                    if (isPost) reactionService.toggleReactionPost(entityId, uid, reaction);
                    else        reactionService.toggleReactionCommentaire(commId, uid, reaction);
                    // Rafraîchir la barre de réactions
                    HBox newBar = buildReactionBar(entityId, isPost, commId);
                    VBox parent = (VBox) bar.getParent();
                    int idx = parent.getChildren().indexOf(bar);
                    if (idx >= 0) parent.getChildren().set(idx, newBar);
                } catch (SQLException ex) { ex.printStackTrace(); }
            });

            bar.getChildren().add(btn);
        }
        return bar;
    }

    // ══════════════════════════════════════════════════
    //  COMMENTAIRES
    // ══════════════════════════════════════════════════

    private void loadComments(Post post, VBox body, Label nbLbl) {
        body.getChildren().clear();
        try {
            List<Commentaire> comms = commService.getCommentairesByPost(post.getPostId());
            nbLbl.setText("💬 "+comms.size()+" commentaire(s)");
            if (comms.isEmpty()) {
                Label l=new Label("Aucun commentaire."); l.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;-fx-padding:8 0 0 16;");
                body.getChildren().add(l);
            } else comms.forEach(c->body.getChildren().add(buildCommentCard(c,post,body,nbLbl)));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox buildCommentCard(Commentaire c, Post post, VBox body, Label nbLbl) {
        int uid = currentUser!=null ? currentUser.getUserId() : -1;
        boolean isMe = c.getUserId()==uid;

        VBox card = new VBox(6);
        card.setStyle("-fx-padding:10 10 10 16;-fx-background-color:#f8f7ff;-fx-background-radius:8;-fx-border-color:#e0e7ff;-fx-border-width:1;-fx-border-radius:8;");

        String authorName="Anonyme";
        if(!c.isIsAnonyme()){
            try{User a=etudiantService.getUserById(c.getUserId()); if(a!=null) authorName=a.getPrenom()+" "+a.getNom();}
            catch(SQLException ignored){}
        }

        Label avatar=new Label(c.isIsAnonyme()?"🎭":"👤"); avatar.setStyle("-fx-font-size:18px;");
        Label authorLbl=new Label(authorName); authorLbl.setStyle("-fx-font-weight:bold;-fx-font-size:12px;-fx-text-fill:#374151;");
        String ds=c.getUpdatedAt()!=null?(!c.getUpdatedAt().equals(c.getCreatedAt())?"modifié le ":"")+DATE_FMT.format(c.getUpdatedAt()):"";
        Label dateLbl=new Label(ds); dateLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#9ca3af;");

        Region sp=new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
        HBox cHeader=new HBox(8,avatar,authorLbl,dateLbl,sp);

        if(isMe){
            Button bE=new Button("✏"); bE.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:12px;"); bE.setOnAction(e->openEditCommentDialog(c,post,body,nbLbl));
            Button bD=new Button("🗑"); bD.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-font-size:12px;"); bD.setOnAction(e->deleteComment(c,post,body,nbLbl));
            cHeader.getChildren().addAll(bE,bD);
        }
        cHeader.setAlignment(Pos.CENTER_LEFT);

        Label contentLbl=new Label(c.getContenu()); contentLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#4b5563;"); contentLbl.setWrapText(true);

        // Réactions du commentaire
        HBox reactionBar = buildReactionBar(post.getPostId(), false, c.getCommentaireId());

        card.getChildren().addAll(cHeader,contentLbl,reactionBar);
        return card;
    }

    // ══════════════════════════════════════════════════
    //  DIALOG POST (avec formatage + médias)
    // ══════════════════════════════════════════════════

    @FXML private void openNewPostDialog() { showPostDialog(null, null); }
    private void openEditPostDialog(Post post, VBox card) { showPostDialog(post, card); }

    private void showPostDialog(Post existing, VBox cardToReplace) {
        boolean isEdit = existing!=null;

        // ── Champs du formulaire ──
        ComboBox<String> cbCat = new ComboBox<>();
        Map<String,Integer> catMap = new LinkedHashMap<>();
        allCategories.forEach(c->catMap.put(c.getNom(),c.getCategorieId()));
        cbCat.setItems(javafx.collections.FXCollections.observableArrayList(catMap.keySet()));
        cbCat.setPromptText("Choisir une catégorie..."); cbCat.setMaxWidth(Double.MAX_VALUE); cbCat.getStyleClass().add("filter-combo");

        TextField tfTitre = new TextField(); tfTitre.setPromptText("Titre (min 4 caractères)"); tfTitre.getStyleClass().add("form-input");

        // ── Barre d'outils de formatage ──
        TextArea taContenu = new TextArea(); taContenu.setPromptText("Votre message..."); taContenu.setPrefRowCount(5); taContenu.setWrapText(true); taContenu.getStyleClass().add("form-textarea");

        HBox formatBar = buildFormatBar(taContenu);

        // ── Émojis rapides ──
        HBox emojiBar = buildEmojiBar(taContenu);

        // ── Fichier média ──
        Label lblMedia = new Label("Aucun fichier sélectionné"); lblMedia.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
        final File[] selectedFile = {null};
        Button btnChooseFile = new Button("📎 Joindre un fichier");
        btnChooseFile.setStyle("-fx-background-color:#ede9fe;-fx-text-fill:#6366f1;-fx-background-radius:8;-fx-padding:7 14 7 14;-fx-cursor:hand;-fx-font-size:12px;");
        btnChooseFile.setOnAction(e->{
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir un fichier");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.jpg","*.jpeg","*.png","*.gif","*.webp"),
                    new FileChooser.ExtensionFilter("Vidéos", "*.mp4","*.avi","*.mov"),
                    new FileChooser.ExtensionFilter("Fichiers", "*.pdf","*.doc","*.docx","*.txt","*.zip")
            );
            File f = fc.showOpenDialog(taContenu.getScene().getWindow());
            if (f!=null){
                long maxSize = f.getName().matches(".*\\.(mp4|avi|mov)") ? MAX_VIDEO_SIZE
                        : f.getName().matches(".*\\.(jpg|jpeg|png|gif|webp)") ? MAX_IMAGE_SIZE : MAX_FILE_SIZE;
                if (f.length()>maxSize){
                    showAlert("⚠️ Fichier trop volumineux. Max: "+(maxSize/1024/1024)+" MB");
                } else {
                    selectedFile[0]=f;
                    lblMedia.setText("📎 "+f.getName()+" ("+(f.length()/1024)+" Ko)");
                    lblMedia.setStyle("-fx-font-size:11px;-fx-text-fill:#6366f1;");
                }
            }
        });
        HBox mediaRow = new HBox(10,btnChooseFile,lblMedia); mediaRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox cbAnonyme = new CheckBox("Publier anonymement"); cbAnonyme.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
        Label errTitre=errLbl(), errContenu=errLbl(), errCat=errLbl(), errMod=errLbl();

        if(isEdit){
            tfTitre.setText(existing.getTitre());
            taContenu.setText(existing.getContenu());
            cbAnonyme.setSelected(existing.isIsAnonyme());
            allCategories.stream().filter(c->c.getCategorieId()==existing.getCategorieId())
                    .findFirst().ifPresent(c->cbCat.setValue(c.getNom()));
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(20,24,8,24)); content.setPrefWidth(520);
        Label dlgTitle = new Label(isEdit?"✏  Modifier le post":"✏  Nouveau post"); dlgTitle.getStyleClass().add("form-title");

        content.getChildren().addAll(dlgTitle,new Separator(),
                fGroup("Catégorie *",cbCat,errCat),
                fGroup("Titre *",tfTitre,errTitre),
                new Label("Contenu * — Mise en forme :") {{ setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6366f1;"); }},
                formatBar, emojiBar,
                taContenu, errContenu,
                fGroup("Pièce jointe (Image ≤5MB, Vidéo ≤50MB, Fichier ≤20MB)",mediaRow,null),
                cbAnonyme, errMod
        );

        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color:transparent;"); scroll.setPrefHeight(500);

        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dp = dialog.getDialogPane();
        dp.setContent(scroll);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color:white;");

        ButtonType btnPub = new ButtonType(isEdit?"✓ Enregistrer":"📢 Publier", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnn = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnPub,btnAnn);
        styleDialogButtons(dp,btnPub,btnAnn);

        ((Button)dp.lookupButton(btnPub)).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean valid=true;
            if(cbCat.getValue()==null){ showErr(errCat,"⚠ Catégorie obligatoire."); valid=false; }
            if(tfTitre.getText().trim().length()<4){ showErr(errTitre,"⚠ Titre min 4 caractères."); valid=false; }
            String contenuText = taContenu.getText().trim();
            if(contenuText.length()<4){ showErr(errContenu,"⚠ Contenu min 4 caractères."); valid=false; }
            if(!valid){ ev.consume(); return; }

            // ── MODÉRATION ──
            if (currentUser!=null) {
                ModerationService.ResultatModeration res = moderationSvc.verifierEtGerer(
                        contenuText + " " + tfTitre.getText().trim(), currentUser, contenuText);
                if (!res.estValide()) {
                    showErr(errMod, "🚫 Votre message contient des termes inappropriés. Publication refusée.");
                    ev.consume(); return;
                }
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if(result.isPresent() && result.get()==btnPub){
            try{
                Post post = isEdit ? existing : new Post();
                post.setTitre(tfTitre.getText().trim());
                post.setContenu(taContenu.getText().trim());
                post.setIsAnonyme(cbAnonyme.isSelected());
                post.setCategorieId(catMap.get(cbCat.getValue()));
                post.setUserId(currentUser!=null?currentUser.getUserId():Session.getInstance().getCurrentUser().getUserId());
                if (isEdit) postService.modifier(post);
                else        postService.ajouter(post); // ← maintenant post.getPostId() est correct

// ✅ Copier le fichier ET enregistrer en base
                if (selectedFile[0] != null) {
                    String cheminCopie = copierMedia(selectedFile[0], post.getPostId());
                    if (cheminCopie != null) {
                        enregistrerMediaEnBase(post.getPostId(), selectedFile[0], cheminCopie);
                    }
                }
                loadPosts();
            } catch(SQLException e){ e.printStackTrace(); }
        }
    }

    private void enregistrerMediaEnBase(int postId, File source, String chemin) {
        try {
            String nom  = source.getName();
            String ext  = nom.toLowerCase();
            String type = ext.matches(".*\\.(jpg|jpeg|png|gif|webp)") ? "IMAGE"
                    : ext.matches(".*\\.(mp4|avi|mov)")            ? "VIDEO"
                      : "FICHIER";

            String sql = "INSERT INTO `post_media` " +
                    "(`post_id`, `chemin`, `type`, `nom`, `taille`) " +
                    "VALUES (?, ?, ?, ?, ?)";
            java.sql.Connection con =
                    org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, postId);
            ps.setString(2, chemin);
            ps.setString(3, type);
            ps.setString(4, nom);
            ps.setLong(5, source.length());
            ps.executeUpdate();
            ps.close();

            System.out.println("Media enregistre en base: " + nom + " [" + type + "]");
        } catch (java.sql.SQLException e) {
            System.err.println("Erreur enregistrement media: " + e.getMessage());
        }
    }

    // ── Barre de formatage (Gras, Italique, Souligné) ──
    private HBox buildFormatBar(TextArea ta) {
        HBox bar = new HBox(6); bar.setAlignment(Pos.CENTER_LEFT);
        String btnStyle = "-fx-background-color:#f3f4f6;-fx-border-color:#e0e7ff;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:4 10 4 10;-fx-cursor:hand;-fx-font-size:12px;";

        Button btnB = new Button("B"); btnB.setStyle(btnStyle+"-fx-font-weight:bold;");
        btnB.setTooltip(new Tooltip("Gras — entoure la sélection de **...**"));
        btnB.setOnAction(e->insererFormatage(ta,"**","**"));

        Button btnI = new Button("I"); btnI.setStyle(btnStyle+"-fx-font-style:italic;");
        btnI.setTooltip(new Tooltip("Italique — entoure la sélection de _..._"));
        btnI.setOnAction(e->insererFormatage(ta,"_","_"));

        Button btnU = new Button("U"); btnU.setStyle(btnStyle+"-fx-underline:true;");
        btnU.setTooltip(new Tooltip("Souligné — entoure la sélection de __...__"));
        btnU.setOnAction(e->insererFormatage(ta,"__","__"));

        Button btnCode = new Button("< >"); btnCode.setStyle(btnStyle);
        btnCode.setTooltip(new Tooltip("Code — entoure de `...`"));
        btnCode.setOnAction(e->insererFormatage(ta,"`","`"));

        Label hint = new Label("Sélectionnez du texte puis cliquez pour formater");
        hint.setStyle("-fx-font-size:10px;-fx-text-fill:#9ca3af;");

        bar.getChildren().addAll(btnB,btnI,btnU,btnCode,hint);
        return bar;
    }

    private void insererFormatage(TextArea ta, String debut, String fin) {
        String selected = ta.getSelectedText();
        if (selected==null||selected.isEmpty()){
            int pos=ta.getCaretPosition();
            ta.insertText(pos,debut+"texte"+fin);
        } else {
            int start=ta.getSelection().getStart(), end=ta.getSelection().getEnd();
            ta.replaceText(start,end,debut+selected+fin);
        }
    }

    // ── Barre d'émojis rapides ──
    private HBox buildEmojiBar(TextArea ta) {
        HBox bar = new HBox(4); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-padding:2 0 2 0;");
        String[] emojis = {"😊","🙏","❤","🌸","✨","💪","👏","🤔","😂","🌟"};
        for (String emoji : emojis) {
            Button b = new Button(emoji);
            b.setStyle("-fx-background-color:transparent;-fx-font-size:16px;-fx-cursor:hand;-fx-padding:2 4 2 4;");
            b.setOnAction(e->ta.insertText(ta.getCaretPosition(),emoji));
            bar.getChildren().add(b);
        }
        return bar;
    }

    // ── Copier le média dans un dossier local ──
    private String copierMedia(File source, int postId) {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), "unimind_media", "posts");
            Files.createDirectories(dir);

            String nomFichier = source.getName();
            String ext = nomFichier.contains(".")
                    ? nomFichier.substring(nomFichier.lastIndexOf('.'))
                    : "";
            Path dest = dir.resolve("post_" + postId + "_" + System.currentTimeMillis() + ext);
            Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Media copie: " + dest.toAbsolutePath());
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("Erreur copie media: " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════
    //  DIALOG COMMENTAIRE (avec modération + email notif)
    // ══════════════════════════════════════════════════

    private void openAddCommentDialog(Post post, VBox body, Label nbLbl, Button btnToggle) {
        TextArea ta = new TextArea(); ta.setPromptText("Votre commentaire..."); ta.setPrefRowCount(3); ta.setWrapText(true); ta.getStyleClass().add("form-textarea");
        HBox emojiBar = buildEmojiBar(ta);
        CheckBox cbAnon = new CheckBox("Publier anonymement"); cbAnon.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
        Label errComm=errLbl(), errMod=errLbl();

        VBox content=new VBox(10); content.setPadding(new Insets(20,24,8,24)); content.setPrefWidth(440);
        Label dlgTitle=new Label("💬  Ajouter un commentaire"); dlgTitle.getStyleClass().add("form-title");
        content.getChildren().addAll(dlgTitle,new Separator(),
                new Label("Commentaire *"){{setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6366f1;");}},
                emojiBar, ta, errComm, cbAnon, errMod);

        Dialog<ButtonType> dialog=new Dialog<>();
        DialogPane dp=dialog.getDialogPane(); dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color:white;");
        ButtonType btnPub=new ButtonType("📢 Publier",ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnn=new ButtonType("✕ Annuler",ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnPub,btnAnn);
        styleDialogButtons(dp,btnPub,btnAnn);

        ((Button)dp.lookupButton(btnPub)).addEventFilter(javafx.event.ActionEvent.ACTION,ev->{
            if(ta.getText().trim().length()<4){showErr(errComm,"⚠ Min 4 caractères."); ev.consume(); return;}
            if(currentUser!=null){
                ModerationService.ResultatModeration res=moderationSvc.verifierEtGerer(ta.getText().trim(),currentUser,ta.getText().trim());
                if(!res.estValide()){showErr(errMod,"🚫 Termes inappropriés détectés. Publication refusée."); ev.consume();}
            }
        });

        dialog.showAndWait().ifPresent(btn->{
            if(btn==btnPub){
                try{
                    Commentaire c=new Commentaire();
                    c.setContenu(ta.getText().trim());
                    c.setIsAnonyme(cbAnon.isSelected());
                    c.setPostId(post.getPostId());
                    c.setUserId(currentUser!=null?currentUser.getUserId():Session.getInstance().getCurrentUser().getUserId());
                    commService.ajouter(c);

                    // ── NOTIFICATION EMAIL au propriétaire du post ──
                    envoyerNotificationReponse(post,c,cbAnon.isSelected());

                    body.setVisible(true); body.setManaged(true); btnToggle.setText("▲ Masquer");
                    loadComments(post,body,nbLbl);
                }catch(SQLException e){e.printStackTrace();}
            }
        });
    }

    /** Envoie un email au propriétaire du post si ce n'est pas lui qui commente */
    private void envoyerNotificationReponse(Post post, Commentaire comm, boolean commAuteurAnonyme) {
        if (currentUser==null) return;
        int postOwnerId = post.getUserId();
        if (postOwnerId==currentUser.getUserId()) return; // ne pas se notifier soi-même

        new Thread(()->{
            try{
                User postOwner = etudiantService.getUserById(postOwnerId);
                if(postOwner==null||postOwner.getEmail()==null) return;
                String prenomAuteur = commAuteurAnonyme ? "Anonyme" : currentUser.getPrenom();
                emailService.sendNotificationReponse(
                        postOwner.getEmail(),
                        postOwner.getPrenom(),
                        prenomAuteur,
                        commAuteurAnonyme,
                        post.getTitre(),
                        comm.getContenu()
                );
            } catch(SQLException e){ System.err.println("Notification email: "+e.getMessage()); }
        }).start();
    }

    private void openEditCommentDialog(Commentaire c, Post post, VBox body, Label nbLbl) {
        TextArea ta=new TextArea(c.getContenu()); ta.setPrefRowCount(3); ta.setWrapText(true); ta.getStyleClass().add("form-textarea");
        CheckBox cbAnon=new CheckBox("Publier anonymement"); cbAnon.setSelected(c.isIsAnonyme()); cbAnon.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
        Label errMod=errLbl();

        VBox content=new VBox(12); content.setPadding(new Insets(20,24,8,24)); content.setPrefWidth(420);
        Label dlgTitle=new Label("✏  Modifier le commentaire"); dlgTitle.getStyleClass().add("form-title");
        content.getChildren().addAll(dlgTitle,new Separator(),fGroup("Commentaire *",ta,null),cbAnon,errMod);

        Dialog<ButtonType> dialog=new Dialog<>();
        DialogPane dp=dialog.getDialogPane(); dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color:white;");
        ButtonType btnSave=new ButtonType("✓ Enregistrer",ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel=new ButtonType("✕ Annuler",ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnSave,btnCancel);
        styleDialogButtons(dp,btnSave,btnCancel);

        ((Button)dp.lookupButton(btnSave)).addEventFilter(javafx.event.ActionEvent.ACTION,ev->{
            if(currentUser!=null){
                ModerationService.ResultatModeration res=moderationSvc.verifierEtGerer(ta.getText().trim(),currentUser,ta.getText().trim());
                if(!res.estValide()){showErr(errMod,"🚫 Termes inappropriés. Modification refusée."); ev.consume();}
            }
        });

        dialog.showAndWait().ifPresent(btn->{
            if(btn==btnSave){
                try{ c.setContenu(ta.getText().trim()); c.setIsAnonyme(cbAnon.isSelected()); commService.modifier(c); loadComments(post,body,nbLbl);}
                catch(SQLException e){e.printStackTrace();}
            }
        });
    }

    private void deletePost(Post post, VBox card) {
        Alert a=new Alert(Alert.AlertType.CONFIRMATION); a.setHeaderText(null);
        a.setContentText("Supprimer ce post ? Cette action est irréversible.");
        a.showAndWait().ifPresent(btn->{ if(btn==ButtonType.OK){ try{ postService.supprimer(post.getPostId()); postsContainer.getChildren().remove(card); }catch(SQLException e){e.printStackTrace();} }});
    }

    private void deleteComment(Commentaire c, Post post, VBox body, Label nbLbl) {
        Alert a=new Alert(Alert.AlertType.CONFIRMATION); a.setHeaderText(null); a.setContentText("Supprimer ce commentaire ?");
        a.showAndWait().ifPresent(btn->{ if(btn==ButtonType.OK){ try{ commService.supprimer(c.getCommentaireId()); loadComments(post,body,nbLbl); }catch(SQLException e){e.printStackTrace();} }});
    }

    // ══════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════

    private void styleDialogButtons(DialogPane dp, ButtonType ok, ButtonType cancel) {
        ((Button)dp.lookupButton(ok)).setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:9 20 9 20;");
        ((Button)dp.lookupButton(cancel)).setStyle("-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;-fx-background-radius:8;-fx-padding:9 20 9 20;");
    }

    private VBox fGroup(String lbl, Node field, Label err) {
        VBox g=new VBox(5); Label l=new Label(lbl); l.getStyleClass().add("form-label");
        g.getChildren().addAll(l,field); if(err!=null) g.getChildren().add(err); return g;
    }

    private Label errLbl() { Label l=new Label(); l.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11px;"); l.setVisible(false); l.setManaged(false); return l; }
    private void showErr(Label l, String msg) { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING,msg,ButtonType.OK).showAndWait(); }
    private Label recBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-padding:4 10;-fx-background-radius:8;-fx-font-size:12px;-fx-font-weight:bold;");
        return l;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}