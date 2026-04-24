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
import org.example.entities.*;
import org.example.services.*;
import org.example.utils.Session;

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

    @FXML private SidebarEtudiantController sidebarEtudiantController;
    @FXML private FlowPane categoriesGrid;
    @FXML private TextField tfSearchCat;
    @FXML private VBox postsContainer;
    @FXML private Button btnLoadMore;
    @FXML private BorderPane mainLayout;

    // Citation widgets
    @FXML private Label lblCitation;
    @FXML private Label lblCitationAuteur;
    @FXML private Button btnRefreshCitation;

    // Services
    private final CategorieMeditationServices catService = new CategorieMeditationServices();
    private final SeanceMeditationServices seanceService = new SeanceMeditationServices();
    private final PostServices postService = new PostServices();
    private final CommentaireServices commentaireService = new CommentaireServices();
    private final EtudiantService etudiantService = new EtudiantService();

    private User currentUser;
    private List<CategorieMeditation> allCategories = new ArrayList<>();
    private List<Post> allPosts = new ArrayList<>();
    private int postsPage = 0;
    private static final int POSTS_PER_PAGE = 5;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Citations de secours en cas d'échec de l'API
    private static final List<String[]> FALLBACK_CITATIONS = List.of(
            new String[]{"La paix vient de l'intérieur. Ne la cherchez pas à l'extérieur.", "Bouddha"},
            new String[]{"Chaque jour est une nouvelle chance de changer votre vie.", "Anonyme"},
            new String[]{"Respirez. Vous êtes exactement là où vous devez être.", "Anonyme"},
            new String[]{"Le bonheur n'est pas quelque chose de prêt à l'emploi. Il vient de vos propres actions.", "Dalaï Lama"},
            new String[]{"Prenez soin de votre corps, c'est le seul endroit où vous devez vivre.", "Jim Rohn"},
            new String[]{"La méditation est une façon de nourrir et de fleurir l'esprit le plus profond en vous.", "Anonyme"},
            new String[]{"Vous n'avez pas à être parfait pour être incroyable.", "Anonyme"},
            new String[]{"Chaque moment est un nouveau départ.", "T.S. Eliot"},
            new String[]{"La force ne vient pas de la capacité physique. Elle vient d'une volonté indomptable.", "Gandhi"},
            new String[]{"Croyez en vous et tout devient possible.", "Anonyme"}
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // L'utilisateur sera injecté via setUtilisateur()
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

    // ══════════════════════════════════════════════════
    //  CITATION DU JOUR
    // ══════════════════════════════════════════════════

    /**
     * Charge la citation du jour.
     * Utilise un cache par date : si la citation a déjà été chargée aujourd'hui,
     * elle est réutilisée. Sinon, appel API ZenQuotes.
     */
    private void loadCitationDuJour() {
        lblCitation.setText("✦  Chargement de la citation...");
        lblCitationAuteur.setText("");
        btnRefreshCitation.setDisable(true);

        // Cache uniquement pour le chargement initial (même session, même jour)
        String cachedDate = CitationCache.getDate();
        String today = LocalDate.now().toString();

        if (today.equals(cachedDate) && CitationCache.getQuote() != null) {
            afficherCitation(CitationCache.getQuote(), CitationCache.getAuthor());
            btnRefreshCitation.setDisable(false);
            return;
        }

        // Premier chargement → /random aussi (plus dynamique)
        fetchAndDisplay(false);
    }

    // ── Nouvelle méthode pour le bouton refresh ──
    private void loadCitationRandom() {
        lblCitation.setText("✦  Chargement...");
        lblCitationAuteur.setText("");
        btnRefreshCitation.setDisable(true);
        fetchAndDisplay(true); // force random
    }

    // ── Méthode centrale qui appelle l'API ──
    private void fetchAndDisplay(boolean forceRandom) {
        CompletableFuture.supplyAsync(() -> fetchRandom())
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    btnRefreshCitation.setDisable(false);
                    if (result != null && result.length == 2
                            && result[0] != null && !result[0].isBlank()) {
                        CitationCache.set(LocalDate.now().toString(), result[0], result[1]);
                        afficherCitation(result[0], result[1]);
                    } else {
                        // Fallback local aléatoire à chaque appel
                        Random rand = new Random();
                        String[] fallback = FALLBACK_CITATIONS.get(
                                rand.nextInt(FALLBACK_CITATIONS.size()));
                        afficherCitation(fallback[0], fallback[1]);
                    }
                }));
    }

    /**
     * Appelle l'API ZenQuotes (https://zenquotes.io/api/today)
     * Retourne [quote, author] ou null si erreur.
     * Traduit côté texte via une liste de mappings si nécessaire.
     */
    private String[] fetchRandom() {
        // ZenQuotes /random — nouvelle citation à chaque appel ✅
        try {
            URL url = new URL("https://zenquotes.io/api/random");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "UnimindApp/1.0");

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();
                conn.disconnect();
                String quote = extractJsonField(json, "\"q\":\"", "\"");
                String author = extractJsonField(json, "\"a\":\"", "\"");
                if (quote != null && !quote.isBlank())
                    return new String[]{quote, author != null ? author : "Anonyme"};
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("ZenQuotes error: " + e.getMessage());
        }

        // Quotable.io comme secours
        try {
            URL url = new URL("https://api.quotable.io/random?tags=inspirational|happiness|mindfulness");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();
                conn.disconnect();
                String quote = extractJsonField(json, "\"content\":\"", "\"");
                String author = extractJsonField(json, "\"author\":\"", "\"");
                if (quote != null && !quote.isBlank())
                    return new String[]{quote, author != null ? author : "Anonyme"};
            }
            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Quotable error: " + e.getMessage());
        }

        return null;
    }

    /** Extrait un champ d'une chaîne JSON simple sans bibliothèque externe */
    private String extractJsonField(String json, String startMarker, String endMarker) {
        try {
            int start = json.indexOf(startMarker);
            if (start == -1) return null;
            start += startMarker.length();
            int end = json.indexOf(endMarker, start);
            if (end == -1) return null;
            String raw = json.substring(start, end);
            // Décoder les séquences d'échappement JSON basiques
            return raw.replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\n", " ")
                    .replace("\\r", "")
                    .replace("\\t", " ")
                    .replace("\\u2019", "'")
                    .replace("\\u201c", "«")
                    .replace("\\u201d", "»")
                    .replace("\\u2014", "—")
                    .replace("\\u2013", "–");
        } catch (Exception e) {
            return null;
        }
    }

    private void afficherCitation(String quote, String author) {
        lblCitation.setText("« " + quote + " »");
        lblCitationAuteur.setText("— " + (author != null && !author.isBlank() ? author : "Anonyme"));
    }

    @FXML
    private void onRefreshCitation() {
        // Vider le cache ET forcer un appel /random
        CitationCache.clear();
        loadCitationRandom(); // nouvelle méthode dédiée au refresh
    }

    /**
     * Cache simple en mémoire pour la citation du jour.
     * Évite des appels API répétés lors de la navigation dans l'app.
     */
    public static class CitationCache {
        private static String date;
        private static String quote;
        private static String author;

        static void set(String d, String q, String a) { date = d; quote = q; author = a; }
        static void clear() { date = null; quote = null; author = null; }
        static String getDate()   { return date; }
        static String getQuote()  { return quote; }
        static String getAuthor() { return author; }
    }

    // ══════════════════════════════════════════════════
    //  CATEGORIES
    // ══════════════════════════════════════════════════

    private void loadCategories() {
        try {
            allCategories = catService.afficher();
            renderCategories(allCategories);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderCategories(List<CategorieMeditation> categories) {
        categoriesGrid.getChildren().clear();
        for (CategorieMeditation cat : categories) {
            categoriesGrid.getChildren().add(buildCategoryCard(cat));
        }
        if (categories.isEmpty()) {
            Label empty = new Label("Aucune catégorie trouvée.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
            categoriesGrid.getChildren().add(empty);
        }
    }

    private VBox buildCategoryCard(CategorieMeditation cat) {
        VBox card = new VBox(10);
        card.getStyleClass().add("cat-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);

        // Icon
        StackPane iconContainer = new StackPane();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setPrefHeight(70);

        if (cat.getIconUrl() != null && !cat.getIconUrl().isBlank()) {
            try {
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
                javafx.scene.image.Image image = new javafx.scene.image.Image(
                        cat.getIconUrl(), 60, 60, true, true, true);
                image.errorProperty().addListener((obs, oldVal, hasError) -> {
                    if (hasError) {
                        Label fallback = new Label("🌸");
                        fallback.setStyle("-fx-font-size: 36px;");
                        iconContainer.getChildren().setAll(fallback);
                    }
                });
                imageView.setImage(image);
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
                iconContainer.getChildren().add(imageView);
            } catch (Exception e) {
                Label fallback = new Label("🌸");
                fallback.setStyle("-fx-font-size: 36px;");
                iconContainer.getChildren().add(fallback);
            }
        } else {
            Label emoji = new Label("🌸");
            emoji.setStyle("-fx-font-size: 36px;");
            iconContainer.getChildren().add(emoji);
        }

        Label nom = new Label(cat.getNom());
        nom.getStyleClass().add("cat-name");
        nom.setWrapText(true);

        String descText = cat.getDescription() != null && !cat.getDescription().isBlank()
                ? cat.getDescription() : "Aucune description";
        Label desc = new Label(descText.length() > 80 ? descText.substring(0, 80) + "..." : descText);
        desc.getStyleClass().add("cat-desc");
        desc.setWrapText(true);

        int nbSeances = 0, nbPosts = 0;
        try {
            nbSeances = (int) seanceService.afficher().stream()
                    .filter(s -> s.getCategorieId() == cat.getCategorieId() && s.isIsActive()).count();
            nbPosts = (int) postService.afficher().stream()
                    .filter(p -> p.getCategorieId() == cat.getCategorieId()).count();
        } catch (SQLException ignored) {}

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label seancesLbl = new Label("🎵 " + nbSeances + " séances");
        seancesLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6366f1; -fx-font-weight: bold;");
        Label postsLbl = new Label("💬 " + nbPosts + " posts");
        postsLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
        stats.getChildren().addAll(seancesLbl, postsLbl);

        Button btnExplorer = new Button("🔍  Explorer");
        btnExplorer.getStyleClass().add("btn-explorer");
        btnExplorer.setMaxWidth(Double.MAX_VALUE);
        btnExplorer.setOnAction(e -> openSeancesCategorie(cat));

        card.getChildren().addAll(iconContainer, nom, desc, stats, btnExplorer);
        return card;
    }

    @FXML
    private void onSearchCategorie() {
        String query = tfSearchCat.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            renderCategories(allCategories);
        } else {
            List<CategorieMeditation> filtered = allCategories.stream()
                    .filter(c -> (c.getNom() != null && c.getNom().toLowerCase().contains(query))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
            renderCategories(filtered);
        }
    }

    private void openSeancesCategorie(CategorieMeditation cat) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/views/EtudiantSeancesCategorie.fxml"));
            Node page = loader.load();
            EtudiantSeancesCategorieController ctrl = loader.getController();
            ctrl.setUtilisateur(currentUser);
            ctrl.initWithCategorie(cat);

            BorderPane root = (BorderPane) categoriesGrid.getScene().lookup("#mainLayout");
            if (root == null && categoriesGrid.getScene().getRoot() instanceof BorderPane bp) {
                root = bp;
            }
            if (root != null) root.setCenter(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════
    //  FORUM / POSTS
    // ══════════════════════════════════════════════════

    private void loadPosts() {
        try {
            allPosts = postService.afficher();
            allPosts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            postsPage = 0;
            postsContainer.getChildren().clear();
            renderNextPosts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderNextPosts() {
        int start = postsPage * POSTS_PER_PAGE;
        int end = Math.min(start + POSTS_PER_PAGE, allPosts.size());
        for (int i = start; i < end; i++) {
            postsContainer.getChildren().add(buildPostCard(allPosts.get(i)));
        }
        postsPage++;
        boolean hasMore = end < allPosts.size();
        btnLoadMore.setVisible(hasMore);
        btnLoadMore.setManaged(hasMore);
    }

    @FXML
    private void loadMorePosts() {
        renderNextPosts();
    }

    private VBox buildPostCard(Post post) {
        int currentUserId = (currentUser != null) ? currentUser.getUserId() : -1;
        boolean isMyPost = post.getUserId() == currentUserId;

        VBox card = new VBox(10);
        card.getStyleClass().add("post-card");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        String authorName = "Anonyme";
        if (!post.isIsAnonyme()) {
            try {
                User author = etudiantService.getUserById(post.getUserId());
                if (author != null) authorName = author.getPrenom() + " " + author.getNom();
            } catch (SQLException ignored) {}
        }

        Label avatar = new Label(post.isIsAnonyme() ? "🎭" : "👤");
        avatar.setStyle("-fx-font-size: 22px;");

        VBox authorInfo = new VBox(2);
        Label authorLbl = new Label(authorName);
        authorLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #374151;");

        String catName = allCategories.stream()
                .filter(c -> c.getCategorieId() == post.getCategorieId())
                .map(CategorieMeditation::getNom).findFirst().orElse("—");

        String dateStr = post.getUpdatedAt() != null
                ? (post.getUpdatedAt().equals(post.getCreatedAt()) ? "Créé le " : "Modifié le ")
                  + DATE_FORMAT.format(post.getUpdatedAt()) : "";
        Label metaLbl = new Label("🗂️ " + catName + "  •  " + dateStr);
        metaLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        authorInfo.getChildren().addAll(authorLbl, metaLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (isMyPost) {
            Button btnEdit = new Button("✏️");
            btnEdit.getStyleClass().addAll("btn-icon", "btn-edit");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setOnAction(e -> openEditPostDialog(post, card));

            Button btnDel = new Button("🗑️");
            btnDel.getStyleClass().addAll("btn-icon", "btn-delete");
            btnDel.setTooltip(new Tooltip("Supprimer"));
            btnDel.setOnAction(e -> deletePost(post, card));
            actions.getChildren().addAll(btnEdit, btnDel);
        }
        header.getChildren().addAll(avatar, authorInfo, spacer, actions);

        Label title = new Label(post.getTitre());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3730a3;");
        title.setWrapText(true);

        Label content = new Label(post.getContenu());
        content.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563;");
        content.setWrapText(true);

        // Comments section
        VBox commentsSection = new VBox(8);
        commentsSection.setStyle("-fx-padding: 10 0 0 0;");

        int[] nbComments = {0};
        try { nbComments[0] = commentaireService.getCommentairesByPost(post.getPostId()).size(); }
        catch (SQLException ignored) {}

        HBox commentHeader = new HBox(10);
        commentHeader.setAlignment(Pos.CENTER_LEFT);
        Label nbCommLbl = new Label("💬 " + nbComments[0] + " commentaire(s)");
        nbCommLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Button btnToggle = new Button("▼ Voir les commentaires");
        btnToggle.getStyleClass().add("btn-toggle-comments");
        Button btnAddComment = new Button("➕ Commenter");
        btnAddComment.getStyleClass().add("btn-comment");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        commentHeader.getChildren().addAll(nbCommLbl, spacer2, btnToggle, btnAddComment);

        VBox commentsBody = new VBox(8);
        commentsBody.setVisible(false);
        commentsBody.setManaged(false);

        btnToggle.setOnAction(e -> {
            boolean showing = commentsBody.isVisible();
            if (!showing) {
                loadComments(post, commentsBody, nbCommLbl);
                btnToggle.setText("▲ Masquer");
            } else {
                btnToggle.setText("▼ Voir les commentaires");
            }
            commentsBody.setVisible(!showing);
            commentsBody.setManaged(!showing);
        });

        btnAddComment.setOnAction(e ->
                openAddCommentForm(post, commentsBody, commentsSection, nbCommLbl, btnToggle));

        commentsSection.getChildren().addAll(commentHeader, commentsBody);
        card.getChildren().addAll(header, title, content, new Separator(), commentsSection);
        return card;
    }

    private void loadComments(Post post, VBox commentsBody, Label nbCommLbl) {
        commentsBody.getChildren().clear();
        try {
            List<Commentaire> comments = commentaireService.getCommentairesByPost(post.getPostId());
            nbCommLbl.setText("💬 " + comments.size() + " commentaire(s)");
            for (Commentaire c : comments) {
                commentsBody.getChildren().add(buildCommentCard(c, post, commentsBody, nbCommLbl));
            }
            if (comments.isEmpty()) {
                Label empty = new Label("Aucun commentaire pour l'instant.");
                empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af; -fx-padding: 8 0 0 16;");
                commentsBody.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox buildCommentCard(Commentaire c, Post post, VBox commentsBody, Label nbCommLbl) {
        int currentUserId = (currentUser != null) ? currentUser.getUserId() : -1;
        boolean isMyComment = c.getUserId() == currentUserId;

        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-padding: 8 8 8 16; -fx-background-color: #f8f7ff; "
                + "-fx-background-radius: 8; -fx-border-color: #e0e7ff; "
                + "-fx-border-width: 1; -fx-border-radius: 8;");

        Label avatar = new Label(c.isIsAnonyme() ? "🎭" : "👤");
        avatar.setStyle("-fx-font-size: 18px;");

        VBox body = new VBox(3);
        HBox.setHgrow(body, Priority.ALWAYS);

        String authorName = "Anonyme";
        if (!c.isIsAnonyme()) {
            try {
                User author = etudiantService.getUserById(c.getUserId());
                if (author != null) authorName = author.getPrenom() + " " + author.getNom();
            } catch (SQLException ignored) {}
        }

        HBox cHeader = new HBox(8);
        cHeader.setAlignment(Pos.CENTER_LEFT);
        Label authorLbl = new Label(authorName);
        authorLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #374151;");
        String dateStr = c.getUpdatedAt() != null
                ? (!c.getUpdatedAt().equals(c.getCreatedAt()) ? "modifié le " : "")
                  + DATE_FORMAT.format(c.getUpdatedAt()) : "";
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        if (isMyComment) {
            Button btnEdit = new Button("✏️");
            btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 12px;");
            btnEdit.setOnAction(e -> openEditCommentDialog(c, post, commentsBody, nbCommLbl));
            Button btnDel = new Button("🗑️");
            btnDel.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 12px;");
            btnDel.setOnAction(e -> deleteComment(c, post, commentsBody, nbCommLbl));
            cHeader.getChildren().addAll(authorLbl, dateLbl, sp, btnEdit, btnDel);
        } else {
            cHeader.getChildren().addAll(authorLbl, dateLbl);
        }

        Label contentLbl = new Label(c.getContenu());
        contentLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563;");
        contentLbl.setWrapText(true);
        body.getChildren().addAll(cHeader, contentLbl);
        row.getChildren().addAll(avatar, body);
        return row;
    }

    // ══════════════════════════════════════════════════
    //  DIALOGS POST / COMMENTAIRE
    // ══════════════════════════════════════════════════

    @FXML private void openNewPostDialog() { showPostDialog(null, null); }
    private void openEditPostDialog(Post post, VBox card) { showPostDialog(post, card); }

    private void showPostDialog(Post existing, VBox cardToReplace) {
        boolean isEdit = existing != null;
        ComboBox<String> cbCategorie = new ComboBox<>();
        Map<String, Integer> catMap = new LinkedHashMap<>();
        allCategories.forEach(c -> catMap.put(c.getNom(), c.getCategorieId()));
        cbCategorie.setItems(javafx.collections.FXCollections.observableArrayList(catMap.keySet()));
        cbCategorie.setPromptText("Choisir une catégorie...");
        cbCategorie.setMaxWidth(Double.MAX_VALUE);
        cbCategorie.getStyleClass().add("filter-combo");

        TextField tfTitre = new TextField();
        tfTitre.setPromptText("Titre de la discussion (min 4 caractères)");
        tfTitre.getStyleClass().add("form-input");

        TextArea taContenu = new TextArea();
        taContenu.setPromptText("Votre message... (min 4 caractères)");
        taContenu.setPrefRowCount(4);
        taContenu.setWrapText(true);
        taContenu.getStyleClass().add("form-textarea");

        CheckBox cbAnonyme = new CheckBox("Publier anonymement");
        cbAnonyme.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        Label errTitre = errLbl(), errContenu = errLbl(), errCat = errLbl();

        if (isEdit) {
            tfTitre.setText(existing.getTitre());
            taContenu.setText(existing.getContenu());
            cbAnonyme.setSelected(existing.isIsAnonyme());
            allCategories.stream().filter(c -> c.getCategorieId() == existing.getCategorieId())
                    .findFirst().ifPresent(c -> cbCategorie.setValue(c.getNom()));
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 8, 24));
        content.setPrefWidth(460);
        Label title = new Label(isEdit ? "✏️  Modifier le post" : "✏️  Nouveau post");
        title.getStyleClass().add("form-title");
        content.getChildren().addAll(title, new Separator(),
                fGroup("Catégorie *", cbCategorie, errCat),
                fGroup("Titre *", tfTitre, errTitre),
                fGroup("Contenu *", taContenu, errContenu),
                cbAnonyme);

        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");

        ButtonType btnPublier = new ButtonType(isEdit ? "✓ Enregistrer" : "📢 Publier",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnPublier, btnAnnuler);

        Button confirmBtn = (Button) dp.lookupButton(btnPublier);
        confirmBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");
        ((Button) dp.lookupButton(btnAnnuler)).setStyle("-fx-background-color: #f3f4f6; "
                + "-fx-text-fill: #6b7280; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean valid = true;
            if (cbCategorie.getValue() == null) { showErr(errCat, "⚠ Catégorie obligatoire."); valid = false; }
            if (tfTitre.getText().trim().length() < 4) { showErr(errTitre, "⚠ Titre min 4 caractères."); valid = false; }
            if (taContenu.getText().trim().length() < 4) { showErr(errContenu, "⚠ Contenu min 4 caractères."); valid = false; }
            if (!valid) ev.consume();
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnPublier) {
            try {
                Post post = isEdit ? existing : new Post();
                post.setTitre(tfTitre.getText().trim());
                post.setContenu(taContenu.getText().trim());
                post.setIsAnonyme(cbAnonyme.isSelected());
                post.setCategorieId(catMap.get(cbCategorie.getValue()));
                int uid = currentUser != null ? currentUser.getUserId()
                        : Session.getInstance().getCurrentUser().getUserId();
                post.setUserId(uid);
                if (isEdit) postService.modifier(post);
                else postService.ajouter(post);
                loadPosts();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void deletePost(Post post, VBox card) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le post");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment supprimer ce post ? Cette action est irréversible.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    postService.supprimer(post.getPostId());
                    postsContainer.getChildren().remove(card);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void openAddCommentForm(Post post, VBox commentsBody, VBox commentsSection,
                                    Label nbCommLbl, Button btnToggle) {
        TextArea taComment = new TextArea();
        taComment.setPromptText("Votre commentaire...");
        taComment.setPrefRowCount(3);
        taComment.setWrapText(true);
        taComment.getStyleClass().add("form-textarea");

        CheckBox cbAnonyme = new CheckBox("Publier anonymement");
        cbAnonyme.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        Label errComment = errLbl();

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 8, 24));
        content.setPrefWidth(420);
        Label title = new Label("💬  Ajouter un commentaire");
        title.getStyleClass().add("form-title");
        content.getChildren().addAll(title, new Separator(),
                fGroup("Commentaire *", taComment, errComment), cbAnonyme);

        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");

        ButtonType btnPublier = new ButtonType("📢 Publier", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnPublier, btnAnnuler);

        ((Button) dp.lookupButton(btnPublier)).setStyle("-fx-background-color: #6366f1; "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");
        ((Button) dp.lookupButton(btnAnnuler)).setStyle("-fx-background-color: #f3f4f6; "
                + "-fx-text-fill: #6b7280; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        ((Button) dp.lookupButton(btnPublier)).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (taComment.getText().trim().length() < 4) {
                showErr(errComment, "⚠ Commentaire min 4 caractères.");
                ev.consume();
            }
        });

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == btnPublier) {
                try {
                    Commentaire c = new Commentaire();
                    c.setContenu(taComment.getText().trim());
                    c.setIsAnonyme(cbAnonyme.isSelected());
                    c.setPostId(post.getPostId());
                    int uid = currentUser != null ? currentUser.getUserId()
                            : Session.getInstance().getCurrentUser().getUserId();
                    c.setUserId(uid);
                    commentaireService.ajouter(c);
                    commentsBody.setVisible(true);
                    commentsBody.setManaged(true);
                    btnToggle.setText("▲ Masquer");
                    loadComments(post, commentsBody, nbCommLbl);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void openEditCommentDialog(Commentaire c, Post post, VBox commentsBody, Label nbCommLbl) {
        TextArea taComment = new TextArea(c.getContenu());
        taComment.setPrefRowCount(3);
        taComment.setWrapText(true);
        taComment.getStyleClass().add("form-textarea");
        CheckBox cbAnonyme = new CheckBox("Publier anonymement");
        cbAnonyme.setSelected(c.isIsAnonyme());
        cbAnonyme.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        Label errComment = errLbl();

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 8, 24));
        content.setPrefWidth(420);
        Label title = new Label("✏️  Modifier le commentaire");
        title.getStyleClass().add("form-title");
        content.getChildren().addAll(title, new Separator(),
                fGroup("Commentaire *", taComment, errComment), cbAnonyme);

        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dp = dialog.getDialogPane();
        dp.setContent(content);
        dp.getStylesheets().add(getClass().getResource("/css/etudiant.css").toExternalForm());
        dp.setStyle("-fx-background-color: white;");

        ButtonType btnSave = new ButtonType("✓ Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✕ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dp.getButtonTypes().addAll(btnSave, btnCancel);

        ((Button) dp.lookupButton(btnSave)).setStyle("-fx-background-color: #6366f1; "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");
        ((Button) dp.lookupButton(btnCancel)).setStyle("-fx-background-color: #f3f4f6; "
                + "-fx-text-fill: #6b7280; -fx-background-radius: 8; -fx-padding: 9 20 9 20;");

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == btnSave) {
                try {
                    c.setContenu(taComment.getText().trim());
                    c.setIsAnonyme(cbAnonyme.isSelected());
                    commentaireService.modifier(c);
                    loadComments(post, commentsBody, nbCommLbl);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    private void deleteComment(Commentaire c, Post post, VBox commentsBody, Label nbCommLbl) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le commentaire");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce commentaire ?");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    commentaireService.supprimer(c.getCommentaireId());
                    loadComments(post, commentsBody, nbCommLbl);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    // ══════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════

    private VBox fGroup(String labelText, Node field, Label errLabel) {
        VBox g = new VBox(5);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        g.getChildren().addAll(lbl, field);
        if (errLabel != null) g.getChildren().add(errLabel);
        return g;
    }

    private Label errLbl() {
        Label l = new Label();
        l.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private void showErr(Label l, String msg) {
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }
}