package org.example.services;

import org.example.entities.Evenement;
import org.example.enums.StatutEvenement;
import org.example.enums.TypeEvenement;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvenementServiceTest {

    static EvenementService es;
    static int idEvenementTest;

    @BeforeAll
    static void setUp() {
        System.out.println("Setting up EvenementService tests...");
        es = new EvenementService();
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Cleaning up after EvenementService tests...");
        // Nettoyage final si nécessaire
    }

    @Test
    @Order(1)
    void testAjouterEvenement() throws SQLException {
        System.out.println("Testing ajouter method...");
        
        Evenement e = new Evenement(
            "Test JUnit Event",
            "Description pour test unitaire",
            TypeEvenement.ATELIER,
            new Timestamp(System.currentTimeMillis() + 86400000), // demain
            new Timestamp(System.currentTimeMillis() + 172800000), // après-demain
            "Salle de test",
            50,
            0,
            StatutEvenement.A_VENIR,
            new Timestamp(System.currentTimeMillis() + 43200000), // limite inscription
            1, // organisateur ID
            null, // image
            36.8065, // latitude
            10.1815  // longitude
        );
        
        es.ajouter(e);
        
        // Vérifier que l'événement a été ajouté
        List<Evenement> evenements = es.afficher();
        assertFalse(evenements.isEmpty(), "La liste des événements ne devrait pas être vide");
        
        // Trouver l'événement ajouté
        Evenement trouve = evenements.stream()
            .filter(ev -> ev.getTitre().equals("Test JUnit Event"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(trouve, "L'événement ajouté devrait être trouvé");
        assertEquals("Description pour test unitaire", trouve.getDescription());
        assertEquals(TypeEvenement.ATELIER, trouve.getType());
        assertEquals("Salle de test", trouve.getLieu());
        assertEquals(50, trouve.getCapaciteMax());
        
        // Sauvegarder l'ID pour les tests suivants
        idEvenementTest = trouve.getEvenementId();
        System.out.println("Événement de test créé avec ID: " + idEvenementTest);
    }

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        System.out.println("Testing findById method...");
        
        Evenement e = es.findById(idEvenementTest);
        
        assertNotNull(e, "L'événement devrait être trouvé par ID");
        assertEquals(idEvenementTest, e.getEvenementId());
        assertEquals("Test JUnit Event", e.getTitre());
        assertEquals(TypeEvenement.ATELIER, e.getType());
    }

    @Test
    @Order(3)
    void testModifierEvenement() throws SQLException {
        System.out.println("Testing modifier method...");
        
        Evenement e = es.findById(idEvenementTest);
        assertNotNull(e, "L'événement à modifier devrait exister");
        
        // Modifier l'événement
        e.setTitre("Test JUnit Event Modifié");
        e.setDescription("Description modifiée pour test");
        e.setCapaciteMax(75);
        
        es.modifier(e);
        
        // Vérifier la modification
        Evenement modifie = es.findById(idEvenementTest);
        assertEquals("Test JUnit Event Modifié", modifie.getTitre());
        assertEquals("Description modifiée pour test", modifie.getDescription());
        assertEquals(75, modifie.getCapaciteMax());
    }

    @Test
    @Order(4)
    void testAfficherEvenements() throws SQLException {
        System.out.println("Testing afficher method...");
        
        List<Evenement> evenements = es.afficher();
        
        assertNotNull(evenements, "La liste des événements ne devrait pas être null");
        assertFalse(evenements.isEmpty(), "La liste des événements ne devrait pas être vide");
        
        // Vérifier que notre événement de test est dans la liste
        boolean trouve = evenements.stream()
            .anyMatch(e -> e.getEvenementId() == idEvenementTest);
        assertTrue(trouve, "L'événement de test devrait être dans la liste");
    }

    @Test
    @Order(5)
    void testSupprimerEvenement() throws SQLException {
        System.out.println("Testing supprimer method...");
        
        // Vérifier que l'événement existe avant suppression
        Evenement e = es.findById(idEvenementTest);
        assertNotNull(e, "L'événement à supprimer devrait exister");
        
        // Supprimer l'événement
        es.supprimer(idEvenementTest);
        
        // Vérifier que l'événement n'existe plus
        Evenement supprime = es.findById(idEvenementTest);
        assertNull(supprime, "L'événement supprimé ne devrait plus être trouvé");
        
        // Vérifier qu'il n'est plus dans la liste
        List<Evenement> evenements = es.afficher();
        boolean trouve = evenements.stream()
            .anyMatch(ev -> ev.getEvenementId() == idEvenementTest);
        assertFalse(trouve, "L'événement supprimé ne devrait plus être dans la liste");
    }
}
