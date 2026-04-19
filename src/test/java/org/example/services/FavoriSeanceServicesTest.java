package org.example.services;

import org.example.entities.CategorieMeditation;
import org.example.entities.FavoriSeance;
import org.example.entities.SeanceMeditation;
import org.example.enums.NiveauMeditation;
import org.example.enums.TypeFichier;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FavoriSeanceServicesTest {

    static FavoriSeanceServices favoriService;
    static SeanceMeditationServices seanceService;
    static CategorieMeditationServices categorieService;
    private static int testCategorieId;
    private static int testSeanceId;
    private static final int TEST_USER_ID = 3; // utilisateur existant dans la base
    private static int testFavoriId;

    @BeforeAll
    static void setUp() throws SQLException {
        System.out.println("Setting up tests...");
        favoriService = new FavoriSeanceServices();
        seanceService = new SeanceMeditationServices();
        categorieService = new CategorieMeditationServices();

        // Créer une catégorie de test
        CategorieMeditation cat = new CategorieMeditation("CatTestFavori", "Description cat", "icon_fav.png");
        categorieService.ajouter(cat);
        List<CategorieMeditation> categories = categorieService.afficher();
        testCategorieId = categories.get(categories.size() - 1).getCategorieId();

        // Créer une séance de test
        SeanceMeditation seance = new SeanceMeditation(
                "Seance Favori Test",
                "Description séance test",
                "fichier.mp3",
                TypeFichier.audio,
                10,
                true,
                NiveauMeditation.debutant,
                testCategorieId
        );
        seanceService.ajouter(seance);
        List<SeanceMeditation> seances = seanceService.afficher();
        testSeanceId = seances.get(seances.size() - 1).getSeanceId();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        System.out.println("Cleaning up...");
        // Supprimer la séance de test
        if (testSeanceId > 0) {
            seanceService.supprimer(testSeanceId);
        }
        // Supprimer la catégorie de test
        if (testCategorieId > 0) {
            categorieService.supprimer(testCategorieId);
        }
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        FavoriSeance favori = new FavoriSeance(TEST_USER_ID, testSeanceId);
        favoriService.ajouter(favori);

        // Vérifier l'ajout
        List<FavoriSeance> favoris = favoriService.getFavorisByUser(TEST_USER_ID);
        assertFalse(favoris.isEmpty());
        FavoriSeance dernier = favoris.get(favoris.size() - 1);
        assertEquals(TEST_USER_ID, dernier.getUserId());
        assertEquals(testSeanceId, dernier.getSeanceId());
        assertNotNull(dernier.getCreatedAt());

        testFavoriId = dernier.getId();
    }

    @Test
    @Order(2)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");
        assertTrue(testFavoriId > 0, "Un ID de favori doit exister (exécutez ajouter() d'abord)");

        // Modifier le favori (changer user_id et seance_id – peu courant mais possible)
        FavoriSeance favori = new FavoriSeance();
        favori.setId(testFavoriId);
        favori.setUserId(TEST_USER_ID); // même utilisateur, on pourrait changer mais restons simples
        favori.setSeanceId(testSeanceId);

        favoriService.modifier(favori);

        // Vérifier que la modification ne casse rien (pas de changement visible car mêmes valeurs)
        List<FavoriSeance> favoris = favoriService.getFavorisByUser(TEST_USER_ID);
    }

    @Test
    @Order(3)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");
        List<FavoriSeance> liste = favoriService.afficher();
        assertNotNull(liste);
    }

    @Test
    @Order(4)
    void getFavorisByUser() throws SQLException {
        System.out.println("Testing getFavorisByUser method...");
        List<FavoriSeance> favorisUser = favoriService.getFavorisByUser(TEST_USER_ID);
        assertNotNull(favorisUser);
    }

    @Test
    @Order(5)
    void isFavori() throws SQLException {
        System.out.println("Testing isFavori method...");
        boolean estFavori = favoriService.isFavori(TEST_USER_ID, testSeanceId);
        assertTrue(estFavori, "La séance devrait être en favori");

        // Tester avec des IDs inexistants
        boolean faux = favoriService.isFavori(999, 999);
        assertFalse(faux);
    }

    @Test
    @Order(6)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");
        assertTrue(testFavoriId > 0, "Aucun ID à supprimer");

        favoriService.supprimer(testFavoriId);

        // Vérifier la suppression
        List<FavoriSeance> favoris = favoriService.getFavorisByUser(TEST_USER_ID);
    }
}