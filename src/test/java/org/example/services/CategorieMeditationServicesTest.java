package org.example.services;

import org.example.entities.CategorieMeditation;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategorieMeditationServicesTest {

    static CategorieMeditationServices service;
    private static int testId; // ID de la catégorie créée pour les tests

    @BeforeAll
    //BeforeEach
    static void setUp() {
        System.out.println("Setting up tests...");
        service = new CategorieMeditationServices();
    }

    @AfterAll
    static void tearDown() {
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        CategorieMeditation cat = new CategorieMeditation("TestUnitaire", "Description test", "icone.png");
        service.ajouter(cat);

        // Vérifier que l'ajout a réussi
        List<CategorieMeditation> liste = service.afficher();
        assertFalse(liste.isEmpty());
        CategorieMeditation derniere = liste.get(liste.size() - 1);
        assertEquals("TestUnitaire", derniere.getNom());
        assertEquals("Description test", derniere.getDescription());
        assertEquals("icone.png", derniere.getIconUrl());

        // Stocker l'ID généré pour les tests suivants
        testId = derniere.getCategorieId();
    }

    @Test
    @Order(2)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");
        assertTrue(testId > 0, "Un ID de test doit exister (exécutez ajouter() d'abord)");

        CategorieMeditation cat = new CategorieMeditation();
        cat.setCategorieId(testId);
        cat.setNom("Nom modifié");
        cat.setDescription("Description modifiée");
        cat.setIconUrl("icone_modifiee.png");

        service.modifier(cat);
    }

    @Test
    @Order(3)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");
        List<CategorieMeditation> liste = service.afficher();
        assertNotNull(liste);
    }

    @Test
    @Order(4)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");
        assertTrue(testId > 0, "Aucun ID à supprimer");

        service.supprimer(testId);

        // Vérifier que la suppression a bien eu lieu
        List<CategorieMeditation> liste = service.afficher();
    }
}