package org.example.services;

import org.example.entities.Favori;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FavoriServiceTest {

    static FavoriService fs;
    static int idFavoriTest;
    static int idEvenementTest = 1; // Utiliser un événement existant
    static int idEtudiantTest = 2;  // Utiliser un étudiant existant

    @BeforeAll
    static void setUp() {
        System.out.println("Setting up FavoriService tests...");
        fs = new FavoriService();
    }

    @Test
    @Order(1)
    void testAjouterFavori() throws SQLException {
        System.out.println("Testing ajouter favori...");
        
        Favori f = new Favori(idEvenementTest, idEtudiantTest);
        
        fs.ajouter(f);
        
        List<Favori> favoris = fs.afficher();
        assertFalse(favoris.isEmpty());
        
        Favori trouve = favoris.stream()
            .filter(fav -> fav.getEvenementId() == idEvenementTest && 
                         fav.getEtudiantId() == idEtudiantTest)
            .findFirst()
            .orElse(null);
        
        assertNotNull(trouve);
        assertEquals(idEvenementTest, trouve.getEvenementId());
        assertEquals(idEtudiantTest, trouve.getEtudiantId());
        assertNotNull(trouve.getCreatedAt());
        
        idFavoriTest = trouve.getId();
        System.out.println("Favori de test créé avec ID: " + idFavoriTest);
    }

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        System.out.println("Testing findById favori...");
        
        Favori f = fs.findById(idFavoriTest);
        assertNotNull(f);
        assertEquals(idFavoriTest, f.getId());
        assertEquals(idEvenementTest, f.getEvenementId());
        assertEquals(idEtudiantTest, f.getEtudiantId());
    }

    @Test
    @Order(3)
    void testModifierFavori() throws SQLException {
        System.out.println("Testing modifier favori...");
        
        Favori f = fs.findById(idFavoriTest);
        assertNotNull(f);
        
        // Pour un favori, on pourrait modifier l'association avec un autre événement
        int nouvelEvenementId = 2;
        f.setEvenementId(nouvelEvenementId);
        
        fs.modifier(f);
        
        Favori modifie = fs.findById(idFavoriTest);
        assertEquals(nouvelEvenementId, modifie.getEvenementId());
        assertEquals(idEtudiantTest, modifie.getEtudiantId());
    }

    @Test
    @Order(4)
    void testAfficherFavoris() throws SQLException {
        System.out.println("Testing afficher favoris...");
        
        List<Favori> favoris = fs.afficher();
        assertNotNull(favoris);
        assertFalse(favoris.isEmpty());
        
        boolean trouve = favoris.stream()
            .anyMatch(fav -> fav.getId() == idFavoriTest);
        assertTrue(trouve);
    }

    @Test
    @Order(5)
    void testSupprimerFavori() throws SQLException {
        System.out.println("Testing supprimer favori...");
        
        Favori f = fs.findById(idFavoriTest);
        assertNotNull(f);
        
        fs.supprimer(idFavoriTest);
        
        Favori supprime = fs.findById(idFavoriTest);
        assertNull(supprime);
        
        List<Favori> favoris = fs.afficher();
        boolean trouve = favoris.stream()
            .anyMatch(fav -> fav.getId() == idFavoriTest);
        assertFalse(trouve);
    }
}
