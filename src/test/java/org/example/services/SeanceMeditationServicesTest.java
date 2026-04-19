package org.example.services;

import org.example.entities.CategorieMeditation;
import org.example.entities.SeanceMeditation;
import org.example.enums.NiveauMeditation;
import org.example.enums.TypeFichier;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeanceMeditationServicesTest {

    static SeanceMeditationServices service;
    static CategorieMeditationServices catService;
    private static int testCategorieId;
    private static int testSeanceId;

    @BeforeAll
    static void setUp() throws SQLException {
        System.out.println("Setting up tests...");
        service = new SeanceMeditationServices();
        catService = new CategorieMeditationServices();

        // Créer une catégorie de test (nécessaire car categorie_id ne peut pas être NULL)
        CategorieMeditation cat = new CategorieMeditation("CatTestSeance", "Description cat", "icon_cat.png");
        catService.ajouter(cat);
        List<CategorieMeditation> categories = catService.afficher();
        testCategorieId = categories.get(categories.size() - 1).getCategorieId();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        System.out.println("Cleaning up...");
        if (testCategorieId > 0) {
            catService.supprimer(testCategorieId);
        }
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        SeanceMeditation seance = new SeanceMeditation(
                "Seance Test",
                "Description test",
                "fichier.mp3",
                TypeFichier.audio,
                15,
                true,
                NiveauMeditation.debutant,
                testCategorieId
        );
        service.ajouter(seance);

        // Vérifier l'ajout
        List<SeanceMeditation> liste = service.afficher();
        assertFalse(liste.isEmpty());
        SeanceMeditation derniere = liste.get(liste.size() - 1);
        assertEquals("Seance Test", derniere.getTitre());
        assertEquals("Description test", derniere.getDescription());
        assertEquals(TypeFichier.audio, derniere.getTypeFichier());
        assertEquals(15, derniere.getDuree());
        assertTrue(derniere.isIsActive());
        assertEquals(NiveauMeditation.debutant, derniere.getNiveau());
        assertEquals(testCategorieId, derniere.getCategorieId());

        testSeanceId = derniere.getSeanceId();
    }

    @Test
    @Order(2)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");
        assertTrue(testSeanceId > 0, "Un ID de séance doit exister (exécutez ajouter() d'abord)");

        SeanceMeditation seance = new SeanceMeditation();
        seance.setSeanceId(testSeanceId);
        seance.setTitre("Titre modifié");
        seance.setDescription("Description modifiée");
        seance.setFichier("nouveau.mp4");
        seance.setTypeFichier(TypeFichier.video);
        seance.setDuree(30);
        seance.setIsActive(false);
        seance.setNiveau(NiveauMeditation.intermediaire);
        seance.setCategorieId(testCategorieId);

        service.modifier(seance);

    }

    @Test
    @Order(3)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");
        List<SeanceMeditation> liste = service.afficher();
        assertNotNull(liste);
    }

    @Test
    @Order(4)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");
        assertTrue(testSeanceId > 0, "Aucun ID à supprimer");

        service.supprimer(testSeanceId);

        // Vérifier la suppression
        List<SeanceMeditation> liste = service.afficher();
    }
}