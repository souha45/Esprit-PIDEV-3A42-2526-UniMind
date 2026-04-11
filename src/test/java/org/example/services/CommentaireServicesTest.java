package org.example.services;

import org.example.entities.CategorieMeditation;
import org.example.entities.Commentaire;
import org.example.entities.Post;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentaireServicesTest {

    static CommentaireServices commentaireService;
    static PostServices postService;
    static CategorieMeditationServices categorieService;
    private static int testCategorieId;
    private static int testPostId;
    private static final int TEST_USER_ID = 3; // utilisateur existant dans la base
    private static int testCommentaireId;

    @BeforeAll
    static void setUp() throws SQLException {
        System.out.println("Setting up tests for CommentaireServices...");
        commentaireService = new CommentaireServices();
        postService = new PostServices();
        categorieService = new CategorieMeditationServices();

        // Créer une catégorie de test
        CategorieMeditation cat = new CategorieMeditation("CatTestCommentaire", "Description cat commentaire", "icon_comm.png");
        categorieService.ajouter(cat);
        List<CategorieMeditation> categories = categorieService.afficher();
        testCategorieId = categories.get(categories.size() - 1).getCategorieId();

        // Créer un post de test (obligatoire pour le commentaire)
        Post post = new Post("Post pour commentaire", "Contenu du post", false, TEST_USER_ID, testCategorieId);
        postService.ajouter(post);
        List<Post> posts = postService.afficher();
        testPostId = posts.get(posts.size() - 1).getPostId();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        System.out.println("Cleaning up CommentaireServices tests...");
        // Supprimer le post de test
        if (testPostId > 0) {
            postService.supprimer(testPostId);
        }
        if (testCategorieId > 0) {
            categorieService.supprimer(testCategorieId);
        }
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        Commentaire commentaire = new Commentaire("Super post, merci !", false, TEST_USER_ID, testPostId);
        commentaireService.ajouter(commentaire);

        // Vérifier l'ajout
        List<Commentaire> commentaires = commentaireService.getCommentairesByPost(testPostId);
        assertFalse(commentaires.isEmpty());
        Commentaire dernier = commentaires.get(commentaires.size() - 1);
        assertEquals("Super post, merci !", dernier.getContenu());
        assertFalse(dernier.isIsAnonyme());
        assertEquals(TEST_USER_ID, dernier.getUserId());
        assertEquals(testPostId, dernier.getPostId());

        testCommentaireId = dernier.getCommentaireId();
    }

    @Test
    @Order(2)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");
        assertTrue(testCommentaireId > 0, "Un ID de commentaire doit exister (exécutez ajouter() d'abord)");

        Commentaire commentaire = new Commentaire();
        commentaire.setCommentaireId(testCommentaireId);
        commentaire.setContenu("Commentaire modifié (anonyme)");
        commentaire.setIsAnonyme(true);
        commentaire.setUserId(TEST_USER_ID);
        commentaire.setPostId(testPostId);

        commentaireService.modifier(commentaire);
    }

    @Test
    @Order(3)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");
        List<Commentaire> liste = commentaireService.afficher();
        assertNotNull(liste);
    }

    @Test
    @Order(4)
    void getCommentairesByPost() throws SQLException {
        System.out.println("Testing getCommentairesByPost method...");
        List<Commentaire> commentaires = commentaireService.getCommentairesByPost(testPostId);
        assertNotNull(commentaires);
    }

    @Test
    @Order(5)
    void getCommentairesByUser() throws SQLException {
        System.out.println("Testing getCommentairesByUser method...");
        List<Commentaire> commentaires = commentaireService.getCommentairesByUser(TEST_USER_ID);
        assertNotNull(commentaires);
    }

    @Test
    @Order(6)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");
        assertTrue(testCommentaireId > 0, "Aucun ID à supprimer");

        commentaireService.supprimer(testCommentaireId);

    }
}