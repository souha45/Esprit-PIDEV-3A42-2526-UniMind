package org.example.services;

import org.example.entities.Traitement;
import org.example.enums.CategorieTraitement;
import org.example.enums.PrioriteTraitement;
import org.example.enums.StatutTraitement;
import org.junit.jupiter.api.*;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TraitementServiceTest {

    static TraitementService traitementService;
    static int lastTraitementId;

    @BeforeAll
    static void setUp() {
        System.out.println("Setting up TraitementService tests...");
        traitementService = new TraitementService();
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Cleaning up TraitementService tests...");
        // Nettoyer les données de test si nécessaire
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");

        // Créer un traitement de test
        Traitement traitement = new Traitement(
                "Test Thérapie CBT",
                "Description test thérapie cognitivo-comportementale",
                "CBT",
                CategorieTraitement.COGNITIF,
                30,
                null,
                new Date(System.currentTimeMillis()),
                null,
                StatutTraitement.EN_COURS,
                PrioriteTraitement.MOYENNE,
                "Objectif test : réduire l'anxiété",
                1,
                2
        );

        // Ajouter le traitement
        traitementService.ajouter(traitement);

        // Vérifier que le traitement a été ajouté
        List<Traitement> traitements = traitementService.afficher();
        assertFalse(traitements.isEmpty());

        // Récupérer le dernier traitement ajouté
        Traitement dernierTraitement = traitements.get(traitements.size() - 1);
        lastTraitementId = dernierTraitement.getTraitementId();

        // Vérifier les données
        assertEquals("Test Thérapie CBT", dernierTraitement.getTitre());
        assertEquals("CBT", dernierTraitement.getType());
        assertEquals(CategorieTraitement.COGNITIF, dernierTraitement.getCategorie());
        assertEquals(StatutTraitement.EN_COURS, dernierTraitement.getStatut());
        assertEquals("Objectif test : réduire l'anxiété", dernierTraitement.getObjectifTherapeutique());

        System.out.println("Traitement ajouté avec ID: " + lastTraitementId);
    }

    @Test
    @Order(2)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");

        List<Traitement> traitements = traitementService.afficher();

        assertNotNull(traitements);
        assertFalse(traitements.isEmpty());

        // Vérifier que notre traitement de test est dans la liste
        boolean found = traitements.stream()
                .anyMatch(t -> t.getTraitementId() == lastTraitementId);
        assertTrue(found);

        System.out.println("Nombre de traitements trouvés: " + traitements.size());
    }

    @Test
    @Order(3)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");

        // Récupérer le traitement à modifier
        List<Traitement> traitements = traitementService.afficher();
        Traitement toUpdate = traitements.stream()
                .filter(t -> t.getTraitementId() == lastTraitementId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Traitement de test non trouvé"));

        // Modifier les données
        toUpdate.setTitre("Test Thérapie CBT (modifié)");
        toUpdate.setDescription("Description modifiée pour le test");
        toUpdate.setStatut(StatutTraitement.TERMINE);
        toUpdate.setPriorite(PrioriteTraitement.MOYENNE);

        // Appliquer la modification
        traitementService.modifier(toUpdate);

        // Vérifier la modification
        List<Traitement> updatedTraitements = traitementService.afficher();
        Traitement updated = updatedTraitements.stream()
                .filter(t -> t.getTraitementId() == lastTraitementId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Traitement modifié non trouvé"));

        assertEquals("Test Thérapie CBT (modifié)", updated.getTitre());
        assertEquals("Description modifiée pour le test", updated.getDescription());
        assertEquals(StatutTraitement.TERMINE, updated.getStatut());
        assertEquals(PrioriteTraitement.MOYENNE, updated.getPriorite());

        System.out.println("Traitement modifié avec succès");
    }

    @Test
    @Order(4)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");

        // Vérifier que le traitement existe avant suppression
        List<Traitement> beforeDelete = traitementService.afficher();
        boolean existsBefore = beforeDelete.stream()
                .anyMatch(t -> t.getTraitementId() == lastTraitementId);
        assertTrue(existsBefore);

        // Supprimer le traitement
        traitementService.supprimer(lastTraitementId);

        // Vérifier que le traitement a été supprimé
        List<Traitement> afterDelete = traitementService.afficher();
        boolean existsAfter = afterDelete.stream()
                .anyMatch(t -> t.getTraitementId() == lastTraitementId);
        assertFalse(existsAfter);

        System.out.println("Traitement supprimé avec succès");
    }
}
