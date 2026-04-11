package org.example.services;

import org.example.entities.CategorieMeditation;
import org.example.entities.Post;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostServicesTest {

    static PostServices postService;
    static CategorieMeditationServices categorieService;
    private static int testCategorieId;
    private static final int TEST_USER_ID = 3; // utilisateur existant dans la base
    private static int testPostId;

    @BeforeAll
    static void setUp() throws SQLException {
        System.out.println("Setting up tests for PostServices...");
        postService = new PostServices();
        categorieService = new CategorieMeditationServices();

        // Créer une catégorie de test (obligatoire pour le post)
        CategorieMeditation cat = new CategorieMeditation("CatTestPost", "Description cat post", "icon_post.png");
        categorieService.ajouter(cat);
        List<CategorieMeditation> categories = categorieService.afficher();
        testCategorieId = categories.get(categories.size() - 1).getCategorieId();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        System.out.println("Cleaning up PostServices tests...");
        if (testCategorieId > 0) {
            categorieService.supprimer(testCategorieId);
        }
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        Post post = new Post("Titre test", "Contenu test", false, TEST_USER_ID, testCategorieId);
        postService.ajouter(post);

        // Vérifier l'ajout
        List<Post> liste = postService.afficher();
        assertFalse(liste.isEmpty());
        Post dernier = liste.get(liste.size() - 1);
        assertEquals("Titre test", dernier.getTitre());
        assertEquals("Contenu test", dernier.getContenu());
        assertFalse(dernier.isIsAnonyme());
        assertEquals(TEST_USER_ID, dernier.getUserId());
        assertEquals(testCategorieId, dernier.getCategorieId());

        testPostId = dernier.getPostId();
    }

    @Test
    @Order(2)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");
        assertTrue(testPostId > 0, "Un ID de post doit exister (exécutez ajouter() d'abord)");

        Post post = new Post();
        post.setPostId(testPostId);
        post.setTitre("Titre modifié");
        post.setContenu("Contenu modifié");
        post.setIsAnonyme(true);
        post.setUserId(TEST_USER_ID);
        post.setCategorieId(testCategorieId);

        postService.modifier(post);
    }

    @Test
    @Order(3)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");
        List<Post> liste = postService.afficher();
        assertNotNull(liste);
    }

    @Test
    @Order(4)
    void getPostsByCategorie() throws SQLException {
        System.out.println("Testing getPostsByCategorie method...");
        List<Post> posts = postService.getPostsByCategorie(testCategorieId);
        assertNotNull(posts);
    }

    @Test
    @Order(5)
    void getPostsByUser() throws SQLException {
        System.out.println("Testing getPostsByUser method...");
        List<Post> posts = postService.getPostsByUser(TEST_USER_ID);
        assertNotNull(posts);
    }

    @Test
    @Order(6)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");
        assertTrue(testPostId > 0, "Aucun ID à supprimer");

        postService.supprimer(testPostId);

        // Vérifier la suppression
        List<Post> liste = postService.afficher();
    }
}