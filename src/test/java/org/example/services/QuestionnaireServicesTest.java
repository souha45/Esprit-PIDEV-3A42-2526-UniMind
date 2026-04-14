package org.example.services;

import org.example.entities.Questionnaire;
import org.example.enums.TypeQuestionnaire;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuestionnaireServicesTest {

    static QuestionnaireServices qs;

    @BeforeAll
    static void setUp() {
        qs = new QuestionnaireServices();
        System.out.println("=== START TEST ===");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== END TEST ===");
    }

    //  TEST 1 : AJOUTER

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Test ajouter");

        // Constructeur avec TypeQuestionnaire obligatoire
        Questionnaire q = new Questionnaire(
                "TEST01",
                "Nom Test",
                "Description Test",
                TypeQuestionnaire.STRESS
        );

        qs.ajouter(q);

        List<Questionnaire> list = qs.afficher();

        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");

        // Vérifier que le dernier ajouté est bien TEST01
        Questionnaire dernier = list.get(list.size() - 1);
        assertEquals("TEST01", dernier.getCode(), "Le code doit être TEST01");
        assertEquals("Nom Test", dernier.getNom(), "Le nom doit correspondre");

        System.out.println("✓ Ajout OK : " + dernier);
    }

    // ─────────────────────────────────────────
    //  TEST 2 : AFFICHER
    // ─────────────────────────────────────────
    @Test
    @Order(2)
    void afficher() throws SQLException {
        System.out.println("Test afficher");

        List<Questionnaire> list = qs.afficher();

        assertNotNull(list, "La liste ne doit pas être null");
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");

        System.out.println("✓ Affichage OK : " + list.size() + " questionnaire(s)");
        list.forEach(q -> System.out.println("   - " + q));
    }

    //  TEST 3 : MODIFIER
    @Test
    @Order(3)
    void modifier() throws SQLException {
        System.out.println("Test modifier");

        List<Questionnaire> list = qs.afficher();
        assertFalse(list.isEmpty(), "Il doit y avoir au moins un questionnaire");

        Questionnaire q = list.get(list.size() - 1);
        int id = q.getQuestionnaireId();

        // setType() obligatoire pour éviter NullPointerException
        q.setNom("Nom Modifié");
        q.setCode("TEST01-MOD");
        q.setDescription("Description modifiée");
        q.setType(TypeQuestionnaire.ANXIETE);

        qs.modifier(q);

        // Vérifier la modification en DB
        List<Questionnaire> newList = qs.afficher();
        Questionnaire modifie = newList.stream()
                .filter(x -> x.getQuestionnaireId() == id)
                .findFirst()
                .orElse(null);

        assertNotNull(modifie, "Le questionnaire modifié doit exister");
        assertEquals("Nom Modifié", modifie.getNom(), "Le nom doit être modifié");

        System.out.println("✓ Modification OK : " + modifie);
    }

    //  TEST 4 : SUPPRIMER

    @Test
    @Order(4)
    void supprimer() throws SQLException {
        System.out.println("Test supprimer");

        List<Questionnaire> list = qs.afficher();
        assertFalse(list.isEmpty(), "Il doit y avoir au moins un questionnaire à supprimer");

        Questionnaire q = list.get(list.size() - 1);
        int id = q.getQuestionnaireId();
        int tailleBefore = list.size();

        qs.supprimer(id);

        List<Questionnaire> newList = qs.afficher();

        // Taille diminuée de 1
        assertEquals(tailleBefore - 1, newList.size(), "La liste doit avoir un élément de moins");

        // L'élément supprimé ne doit plus exister
        boolean exists = newList.stream()
                .anyMatch(x -> x.getQuestionnaireId() == id);
        assertFalse(exists, "Le questionnaire supprimé ne doit plus exister");

        System.out.println("✓ Suppression OK : ID=" + id + " supprimé");
    }
}