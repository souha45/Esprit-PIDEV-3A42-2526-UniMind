package org.example.services;

import org.example.entities.Participation;
import org.example.enums.StatutParticipation;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParticipationServiceTest {

    static ParticipationService ps;
    static int idParticipationTest;
    static int idEvenementTest = 1; // Utiliser un événement existant
    static int idEtudiantTest = 2;  // Utiliser un étudiant existant

    @BeforeAll
    static void setUp() {
        System.out.println("Setting up ParticipationService tests...");
        ps = new ParticipationService();
    }

    @Test
    @Order(1)
    void testAjouterParticipation() throws SQLException {
        System.out.println("Testing ajouter participation...");
        
        Participation p = new Participation(
            idEvenementTest,
            idEtudiantTest,
            StatutParticipation.EN_ATTENTE
        );
        
        ps.ajouter(p);
        
        List<Participation> participations = ps.afficher();
        assertFalse(participations.isEmpty());
        
        // Trouver la participation ajoutée
        Participation trouve = participations.stream()
            .filter(part -> part.getEvenementId() == idEvenementTest && 
                          part.getEtudiantId() == idEtudiantTest &&
                          part.getStatut() == StatutParticipation.EN_ATTENTE)
            .findFirst()
            .orElse(null);
        
        assertNotNull(trouve);
        idParticipationTest = trouve.getParticipationId();
        System.out.println("Participation de test créée avec ID: " + idParticipationTest);
    }

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        System.out.println("Testing findById participation...");
        
        Participation p = ps.findById(idParticipationTest);
        assertNotNull(p);
        assertEquals(idParticipationTest, p.getParticipationId());
        assertEquals(idEvenementTest, p.getEvenementId());
        assertEquals(idEtudiantTest, p.getEtudiantId());
        assertEquals(StatutParticipation.EN_ATTENTE, p.getStatut());
    }

    @Test
    @Order(3)
    void testModifierParticipation() throws SQLException {
        System.out.println("Testing modifier participation...");
        
        Participation p = ps.findById(idParticipationTest);
        assertNotNull(p);
        
        // Modifier la participation
        p.setStatut(StatutParticipation.CONFIRME);
        p.ajouterFeedback((short) 5, "Excellent événement !");
        
        ps.modifier(p);
        
        Participation modifie = ps.findById(idParticipationTest);
        assertEquals(StatutParticipation.CONFIRME, modifie.getStatut());
        assertEquals(Short.valueOf((short) 5), modifie.getNoteSatisfaction());
        assertEquals("Excellent événement !", modifie.getFeedbackCommentaire());
        assertNotNull(modifie.getFeedbackAt());
    }

    @Test
    @Order(4)
    void testAfficherParticipations() throws SQLException {
        System.out.println("Testing afficher participations...");
        
        List<Participation> participations = ps.afficher();
        assertNotNull(participations);
        assertFalse(participations.isEmpty());
        
        boolean trouve = participations.stream()
            .anyMatch(p -> p.getParticipationId() == idParticipationTest);
        assertTrue(trouve);
    }

    @Test
    @Order(5)
    void testSupprimerParticipation() throws SQLException {
        System.out.println("Testing supprimer participation...");
        
        Participation p = ps.findById(idParticipationTest);
        assertNotNull(p);
        
        ps.supprimer(idParticipationTest);
        
        Participation supprime = ps.findById(idParticipationTest);
        assertNull(supprime);
        
        List<Participation> participations = ps.afficher();
        boolean trouve = participations.stream()
            .anyMatch(part -> part.getParticipationId() == idParticipationTest);
        assertFalse(trouve);
    }
}
