package org.example.services;

import org.example.entities.Sponsor;
import org.example.enums.StatutSponsor;
import org.example.enums.TypeSponsor;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SponsorServiceTest {

    static SponsorService ss;
    static int idSponsorTest;

    @BeforeAll
    static void setUp() {
        System.out.println("Setting up SponsorService tests...");
        ss = new SponsorService();
    }

    @Test
    @Order(1)
    void testAjouterSponsor() throws SQLException {
        System.out.println("Testing ajouter sponsor...");
        
        Sponsor s = new Sponsor(
            "Test JUnit Sponsor",
            TypeSponsor.ENTREPRISE,
            "https://test-sponsor.com",
            "contact@test-sponsor.com",
            "71234567",
            "123 Rue du Test, Tunis",
            "Technologie",
            StatutSponsor.EN_ATTENTE,
            "logo-test.png"
        );
        
        ss.ajouter(s);
        
        List<Sponsor> sponsors = ss.afficher();
        assertFalse(sponsors.isEmpty());
        
        Sponsor trouve = sponsors.stream()
            .filter(sp -> sp.getNomSponsor().equals("Test JUnit Sponsor"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(trouve);
        assertEquals(TypeSponsor.ENTREPRISE, trouve.getTypeSponsor());
        assertEquals("contact@test-sponsor.com", trouve.getEmailContact());
        assertEquals(StatutSponsor.EN_ATTENTE, trouve.getStatut());
        
        idSponsorTest = trouve.getSponsorId();
        System.out.println("Sponsor de test créé avec ID: " + idSponsorTest);
    }

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        System.out.println("Testing findById sponsor...");
        
        Sponsor s = ss.findById(idSponsorTest);
        assertNotNull(s);
        assertEquals(idSponsorTest, s.getSponsorId());
        assertEquals("Test JUnit Sponsor", s.getNomSponsor());
        assertEquals(TypeSponsor.ENTREPRISE, s.getTypeSponsor());
    }

    @Test
    @Order(3)
    void testModifierSponsor() throws SQLException {
        System.out.println("Testing modifier sponsor...");
        
        Sponsor s = ss.findById(idSponsorTest);
        assertNotNull(s);
        
        s.setNomSponsor("Test JUnit Sponsor Modifié");
        s.setTypeSponsor(TypeSponsor.ASSOCIATION);
        s.setStatut(StatutSponsor.EN_ATTENTE);
        
        ss.modifier(s);
        
        Sponsor modifie = ss.findById(idSponsorTest);
        assertEquals("Test JUnit Sponsor Modifié", modifie.getNomSponsor());
        assertEquals(TypeSponsor.ASSOCIATION, modifie.getTypeSponsor());
        assertEquals(StatutSponsor.EN_ATTENTE, modifie.getStatut());
    }

    @Test
    @Order(4)
    void testAfficherSponsors() throws SQLException {
        System.out.println("Testing afficher sponsors...");
        
        List<Sponsor> sponsors = ss.afficher();
        assertNotNull(sponsors);
        assertFalse(sponsors.isEmpty());
        
        boolean trouve = sponsors.stream()
            .anyMatch(sp -> sp.getSponsorId() == idSponsorTest);
        assertTrue(trouve);
    }

    @Test
    @Order(5)
    void testSupprimerSponsor() throws SQLException {
        System.out.println("Testing supprimer sponsor...");
        
        Sponsor s = ss.findById(idSponsorTest);
        assertNotNull(s);
        
        ss.supprimer(idSponsorTest);
        
        Sponsor supprime = ss.findById(idSponsorTest);
        assertNull(supprime);
        
        List<Sponsor> sponsors = ss.afficher();
        boolean trouve = sponsors.stream()
            .anyMatch(sp -> sp.getSponsorId() == idSponsorTest);
        assertFalse(trouve);
    }
}
