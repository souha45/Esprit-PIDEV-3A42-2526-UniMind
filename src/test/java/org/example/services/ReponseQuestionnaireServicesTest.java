package org.example.services;

import org.example.entities.Reponsequestionnaire;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReponseQuestionnaireServicesTest {

    static ReponseQuestionnaireServices rs;

    // ⚠️ Mettre un questionnaire_id qui existe dans ta DB
    static final int QUESTIONNAIRE_ID_TEST = 1;

    @BeforeAll
    static void setUp() {
        rs = new ReponseQuestionnaireServices();
        System.out.println("=== START TEST ReponseQuestionnaire ===");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== END TEST ReponseQuestionnaire ===");
    }

    // ─────────────────────────────────────────
    //  TEST 1 : AJOUTER
    // ─────────────────────────────────────────
    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Test ajouter");

        Reponsequestionnaire r = new Reponsequestionnaire(
                15.5,
                "{\"q1\":\"reponse1\",\"q2\":\"reponse2\",\"q3\":\"reponse3\"}",  // ← virgule ici
                "Niveau légère",
                30,
                "legere",
                false,
                "Pas de commentaire",
                QUESTIONNAIRE_ID_TEST,
                null
        );

        rs.ajouter(r);

        List<Reponsequestionnaire> list = rs.afficher();

        // Liste non vide après ajout
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide après ajout");

        // Vérifier le dernier élément
        Reponsequestionnaire dernier = list.get(list.size() - 1);
        assertEquals(15.5, dernier.getScoreTotale(), "Le score doit être 15.5");
        assertEquals("legere", dernier.getNiveau(), "Le niveau doit être 'legere'");
        assertEquals(QUESTIONNAIRE_ID_TEST, dernier.getQuestionnaireId(), "L'ID questionnaire doit correspondre");
        assertFalse(dernier.isaBesoinPsy(), "aBesoinPsy doit être false");

        System.out.println("✓ Ajout OK : " + dernier);
    }

    // ─────────────────────────────────────────
    //  TEST 2 : AFFICHER
    // ─────────────────────────────────────────
    @Test
    @Order(2)
    void afficher() throws SQLException {
        System.out.println("Test afficher");

        List<Reponsequestionnaire> list = rs.afficher();

        // Non null
        assertNotNull(list, "La liste ne doit pas être null");

        // Au moins 1 réponse
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");

        // Vérifier que chaque réponse a les champs obligatoires
        for (Reponsequestionnaire r : list) {
            assertNotNull(r.getReponseQuest(), "reponseQuest ne doit pas être null");
            assertFalse(r.getReponseQuest().isEmpty(), "reponseQuest ne doit pas être vide");
            assertNotNull(r.getNiveau(), "niveau ne doit pas être null");
            assertTrue(r.getQuestionnaireId() > 0, "questionnaireId doit être positif");
        }

        System.out.println("✓ Affichage OK : " + list.size() + " réponse(s)");
        list.forEach(r -> System.out.println("   - " + r));
    }

    // ─────────────────────────────────────────
    //  TEST 3 : MODIFIER
    // ─────────────────────────────────────────
    @Test
    @Order(3)
    void modifier() throws SQLException {
        System.out.println("Test modifier");

        List<Reponsequestionnaire> list = rs.afficher();
        assertFalse(list.isEmpty(), "Il doit y avoir au moins une réponse à modifier");

        Reponsequestionnaire dernier = list.get(list.size() - 1);
        int id = dernier.getReponseQuestionnaireId();

        // Modifier les champs
        dernier.setScoreTotale(25.0);
        dernier.setNiveau("modere");
        dernier.setInterpretation("Niveau modéré après modification");
        dernier.setaBesoinPsy(true);
        dernier.setCommentaire("Commentaire modifié");
        dernier.setDureePassage(45);

        rs.modifier(dernier);

        // Vérifier en DB
        List<Reponsequestionnaire> newList = rs.afficher();
        Reponsequestionnaire modifie = newList.stream()
                .filter(r -> r.getReponseQuestionnaireId() == id)
                .findFirst()
                .orElse(null);

        assertNotNull(modifie, "La réponse modifiée doit exister");
        assertEquals(25.0, modifie.getScoreTotale(), "Le score doit être 25.0");
        assertEquals("modere", modifie.getNiveau(), "Le niveau doit être 'modere'");
        assertTrue(modifie.isaBesoinPsy(), "aBesoinPsy doit être true");

        System.out.println("✓ Modification OK : " + modifie);
    }

    // ─────────────────────────────────────────
    //  TEST 4 : SUPPRIMER
    // ─────────────────────────────────────────
    @Test
    @Order(4)
    void supprimer() throws SQLException {
        System.out.println("Test supprimer");

        List<Reponsequestionnaire> list = rs.afficher();
        assertFalse(list.isEmpty(), "Il doit y avoir au moins une réponse à supprimer");

        Reponsequestionnaire dernier = list.get(list.size() - 1);
        int id = dernier.getReponseQuestionnaireId();
        int tailleBefore = list.size();

        rs.supprimer(id);

        List<Reponsequestionnaire> newList = rs.afficher();

        // Taille diminuée de 1
        assertEquals(tailleBefore - 1, newList.size(),
                "La liste doit avoir un élément de moins après suppression");

        // L'élément ne doit plus exister
        boolean exists = newList.stream()
                .anyMatch(r -> r.getReponseQuestionnaireId() == id);
        assertFalse(exists, "La réponse supprimée ne doit plus exister");

        System.out.println("✓ Suppression OK : ID=" + id + " supprimée");
    }

    // ─────────────────────────────────────────
    //  TEST 5 : AFFICHER PAR USER
    // ─────────────────────────────────────────
    @Test
    @Order(5)
    void afficherParUser() throws SQLException {
        System.out.println("Test afficherParUser");

        // Ajouter une réponse avec userId=1 pour tester
        Reponsequestionnaire r = new Reponsequestionnaire(
                10.0,
                "{\"q1\":\"souvent\"}",  // ✅ JSON
                "Test interp",
                null,
                "legere",
                false,
                null,
                QUESTIONNAIRE_ID_TEST,
                1
        );
        rs.ajouter(r);

        List<Reponsequestionnaire> list = rs.afficherParUser(1);

        assertNotNull(list, "La liste ne doit pas être null");
        assertFalse(list.isEmpty(), "Il doit y avoir au moins une réponse pour userId=1");

        // Toutes les réponses doivent appartenir à userId=1
        for (Reponsequestionnaire rep : list) {
            assertEquals(1, rep.getUserId(), "Toutes les réponses doivent avoir userId=1");
        }

        // Nettoyer — supprimer la réponse ajoutée
        Reponsequestionnaire derniere = list.get(list.size() - 1);
        rs.supprimer(derniere.getReponseQuestionnaireId());

        System.out.println("✓ AfficherParUser OK : " + list.size() + " réponse(s) pour userId=1");
    }
}