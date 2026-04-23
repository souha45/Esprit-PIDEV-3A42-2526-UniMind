package org.example.services;

import org.example.entities.EvenementSponsor;
import org.example.entities.Sponsor;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeContribution;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvenementSponsorServiceTest {

    static EvenementSponsorService ess;
    static SponsorService sponsorService;
    static int idEvenementSponsorTest;
    static int idEvenementTest = 1; // Utiliser un événement existant
    static int idSponsorTest;       // Sponsor créé pour le test

    @BeforeAll
    static void setUp() throws SQLException {
        System.out.println("Setting up EvenementSponsorService tests...");
        ess = new EvenementSponsorService();
        sponsorService = new SponsorService();

        // Créer un sponsor pour le test
        Sponsor sponsorTest = new Sponsor(
            "Sponsor Test JUnit",
            org.example.enums.TypeSponsor.ENTREPRISE,
            "http://test-junit.com",
            "sponsor_test_junit@example.com",
            "123456789",
            "Adresse test",
            "Technologie",
            org.example.enums.StatutSponsor.CONFIRME,
            null // Pas d'image pour le test
        );

        sponsorService.ajouter(sponsorTest);
        idSponsorTest = sponsorTest.getSponsorId();
        System.out.println("Sponsor de test créé avec ID: " + idSponsorTest);
    }

    @AfterAll
    static void tearDown() throws SQLException {
        // Nettoyer le sponsor créé pour le test
        if (idSponsorTest > 0) {
            try {
                sponsorService.supprimer(idSponsorTest);
                System.out.println("Sponsor de test supprimé avec ID: " + idSponsorTest);
            } catch (SQLException e) {
                System.out.println("Erreur lors de la suppression du sponsor de test: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterEvenementSponsor() throws SQLException {
        System.out.println("Testing ajouter evenement sponsor...");
        
        EvenementSponsor es = new EvenementSponsor(
            new BigDecimal("1500.00"),
            TypeContribution.FINANCIER,
            "Sponsoring test JUnit",
            new Timestamp(System.currentTimeMillis()),
            StatutSponsor.CONFIRME,
            idEvenementTest,
            idSponsorTest
        );
        
        ess.ajouter(es);
        
        List<EvenementSponsor> evenementSponsors = ess.afficher();
        assertFalse(evenementSponsors.isEmpty());
        
        EvenementSponsor trouve = evenementSponsors.stream()
            .filter(esItem -> esItem.getEvenementId() == idEvenementTest && 
                             esItem.getSponsorId() == idSponsorTest &&
                             esItem.getTypeContribution() == TypeContribution.FINANCIER)
            .findFirst()
            .orElse(null);
        
        assertNotNull(trouve);
        assertEquals(new BigDecimal("1500.00"), trouve.getMontantContribution());
        assertEquals(TypeContribution.FINANCIER, trouve.getTypeContribution());
        assertEquals("Sponsoring test JUnit", trouve.getDescriptionContribution());
        assertEquals(StatutSponsor.CONFIRME, trouve.getStatut());
        
        idEvenementSponsorTest = trouve.getEvenementSponsorId();
        System.out.println("EvenementSponsor de test créé avec ID: " + idEvenementSponsorTest);
    }

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        System.out.println("Testing findById evenement sponsor...");
        
        EvenementSponsor es = ess.findById(idEvenementSponsorTest);
        assertNotNull(es);
        assertEquals(idEvenementSponsorTest, es.getEvenementSponsorId());
        assertEquals(idEvenementTest, es.getEvenementId());
        assertEquals(idSponsorTest, es.getSponsorId());
        assertEquals(TypeContribution.FINANCIER, es.getTypeContribution());
    }

    @Test
    @Order(3)
    void testModifierEvenementSponsor() throws SQLException {
        System.out.println("Testing modifier evenement sponsor...");
        
        EvenementSponsor es = ess.findById(idEvenementSponsorTest);
        assertNotNull(es);
        
        es.setMontantContribution(new BigDecimal("2000.00"));
        es.setTypeContribution(TypeContribution.MATERIEL);
        es.setDescriptionContribution("Contribution modifiée test JUnit");
        es.setStatut(StatutSponsor.EN_ATTENTE);
        
        ess.modifier(es);
        
        EvenementSponsor modifie = ess.findById(idEvenementSponsorTest);
        assertEquals(new BigDecimal("2000.00"), modifie.getMontantContribution());
        assertEquals(TypeContribution.MATERIEL, modifie.getTypeContribution());
        assertEquals("Contribution modifiée test JUnit", modifie.getDescriptionContribution());
        assertEquals(StatutSponsor.EN_ATTENTE, modifie.getStatut());
    }

    @Test
    @Order(4)
    void testAfficherEvenementSponsors() throws SQLException {
        System.out.println("Testing afficher evenement sponsors...");
        
        List<EvenementSponsor> evenementSponsors = ess.afficher();
        assertNotNull(evenementSponsors);
        assertFalse(evenementSponsors.isEmpty());
        
        boolean trouve = evenementSponsors.stream()
            .anyMatch(es -> es.getEvenementSponsorId() == idEvenementSponsorTest);
        assertTrue(trouve);
    }

    @Test
    @Order(5)
    void testSupprimerEvenementSponsor() throws SQLException {
        System.out.println("Testing supprimer evenement sponsor...");
        
        EvenementSponsor es = ess.findById(idEvenementSponsorTest);
        assertNotNull(es);
        
        ess.supprimer(idEvenementSponsorTest);
        
        EvenementSponsor supprime = ess.findById(idEvenementSponsorTest);
        assertNull(supprime);
        
        List<EvenementSponsor> evenementSponsors = ess.afficher();
        boolean trouve = evenementSponsors.stream()
            .anyMatch(esItem -> esItem.getEvenementSponsorId() == idEvenementSponsorTest);
        assertFalse(trouve);
    }
}
