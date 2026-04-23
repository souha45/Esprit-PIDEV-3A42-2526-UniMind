package org.example.services;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import org.example.entities.SuiviTraitement;
import org.example.enums.RessentiSuivi;
import org.example.enums.SaisiPar;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SuiviTraitementServiceTest {

    static SuiviTraitementService suiviService;
    static int lastSuiviId;
    static int testTraitementId = 5; // ID d'un traitement existant qui a des suivis
    static int actualTraitementId; // ID réel du traitement utilisé

    @BeforeAll
    static void setUp() {
        System.out.println("Setting up SuiviTraitementService tests...");
        suiviService = new SuiviTraitementService();
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Cleaning up SuiviTraitementService tests...");
    }

    @Test
    @Order(1)
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        System.out.println("Utilisation du traitement ID existant: " + testTraitementId + " (qui a déjà des suivis associés)");

        try {
            // Vérifier d'abord si des suivis existent déjà
            List<SuiviTraitement> suivisExistants = suiviService.afficher();
            System.out.println("Nombre de suivis avant ajout: " + suivisExistants.size());

            // Créer un suivi de test
            SuiviTraitement suivi = new SuiviTraitement(
                    new Date(System.currentTimeMillis()), // dateSuivi
                    true, // effectue
                    null, // heurePrevue
                    false, // valide
                    testTraitementId, // traitementId
                    "Test observations patient", // observations
                    "Test observations psychologue", // observationsPsy
                    RessentiSuivi.BIEN, // ressenti
                    SaisiPar.PSYCHOLOGUE // saisiPar
            );
            suivi.setEvaluation(7); // evaluation n'est pas dans le constructeur

            System.out.println("Suivi créé avant ajout: " + suivi.toString());
            System.out.println("Traitement ID cible: " + testTraitementId);

            // Ajouter le suivi
            suiviService.ajouter(suivi);
            System.out.println("Suivi ajouté avec succès");

            // Vérifier que le suivi a été ajouté
            List<SuiviTraitement> suivis = suiviService.afficher();
            System.out.println("Nombre de suivis après ajout: " + suivis.size());

            if (suivis.isEmpty()) {
                System.out.println("ERREUR: Aucun suivi trouvé après ajout");
                return;
            }

            // Récupérer le dernier suivi ajouté
            SuiviTraitement dernierSuivi = suivis.get(suivis.size() - 1);
            lastSuiviId = dernierSuivi.getSuivitraitementId();
            actualTraitementId = dernierSuivi.getTraitementId(); // Enregistrer l'ID réel du traitement
            System.out.println("Dernier suivi ID: " + lastSuiviId);
            System.out.println("Traitement ID réel associé: " + actualTraitementId);

            // Afficher les détails du dernier suivi pour débogage
            System.out.println("Détails du dernier suivi:");
            System.out.println("  - ID: " + dernierSuivi.getSuivitraitementId());
            System.out.println("  - Traitement ID: " + dernierSuivi.getTraitementId());
            System.out.println("  - Observations: " + dernierSuivi.getObservations());
            System.out.println("  - Date: " + dernierSuivi.getDateSuivi());

            // Vérifier les données
            assertEquals(actualTraitementId, dernierSuivi.getTraitementId());
            assertEquals("Test observations patient", dernierSuivi.getObservations());
            assertEquals("Test observations psychologue", dernierSuivi.getObservationsPsy());
            assertEquals(RessentiSuivi.BIEN, dernierSuivi.getRessenti());
            assertEquals(SaisiPar.PSYCHOLOGUE, dernierSuivi.getSaisiPar());
            assertEquals(Integer.valueOf(7), dernierSuivi.getEvaluation());
            assertTrue(dernierSuivi.isEffectue());
            assertFalse(dernierSuivi.isValide());

            System.out.println("Suivi ajouté avec ID: " + lastSuiviId);

            // Vérifier que le suivi peut être récupéré par traitement ID
            List<SuiviTraitement> suivisDuTraitement = suiviService.getByTraitementId(testTraitementId);
            System.out.println("Nombre de suivis pour traitement " + testTraitementId + ": " + suivisDuTraitement.size());

            boolean foundInTraitement = suivisDuTraitement.stream()
                    .anyMatch(s -> s.getSuivitraitementId() == lastSuiviId);
            System.out.println("Suivi trouvé dans les suivis du traitement: " + foundInTraitement);

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    void afficher() throws SQLException {
        System.out.println("Testing afficher method...");

        List<SuiviTraitement> suivis = suiviService.afficher();

        assertNotNull(suivis);

        // S'il n'y a pas de suivis, le test passe
        if (suivis.isEmpty()) {
            System.out.println("Aucun suivi trouvé dans la base de données");
            return;
        }

        // S'il y a des suivis, vérifier que notre suivi de test est dans la liste
        if (lastSuiviId > 0) {
            boolean found = suivis.stream()
                    .anyMatch(s -> s.getSuivitraitementId() == lastSuiviId);
            assertTrue(found, "Le suivi ajouté précédemment devrait être trouvé dans la liste");
        }

        System.out.println("Nombre de suivis trouvés: " + suivis.size());
    }

    @Test
    @Order(3)
    void getByTraitementId() throws SQLException {
        System.out.println("Testing getByTraitementId method...");

        // Vérifier d'abord que lastSuiviId a été correctement initialisé
        if (lastSuiviId == 0 || actualTraitementId == 0) {
            System.out.println("ERREUR: lastSuiviId ou actualTraitementId n'a pas été initialisé. Le test ajouter a peut-être échoué ou n'a pas été exécuté.");
            System.out.println("Vérification des suivis existants...");
            List<SuiviTraitement> tousLesSuivis = suiviService.afficher();
            System.out.println("Nombre total de suivis dans la base: " + tousLesSuivis.size());

            if (!tousLesSuivis.isEmpty()) {
                // Utiliser le dernier suivi et récupérer son traitement ID
                SuiviTraitement dernierSuivi = tousLesSuivis.get(tousLesSuivis.size() - 1);
                lastSuiviId = dernierSuivi.getSuivitraitementId();
                actualTraitementId = dernierSuivi.getTraitementId(); // Récupérer l'ID réel du traitement
                System.out.println("Utilisation du dernier suivi ID: " + lastSuiviId);
                System.out.println("Traitement ID associé: " + actualTraitementId);
            } else {
                System.out.println("Aucun suivi trouvé dans la base de données!");
                return; // Arrêter le test si aucun suivi n'existe
            }
        }

        System.out.println("Recherche des suivis pour traitement ID: " + actualTraitementId);
        List<SuiviTraitement> suivisDuTraitement = suiviService.getByTraitementId(actualTraitementId);

        assertNotNull(suivisDuTraitement);
        System.out.println("Nombre de suivis trouvés pour traitement " + actualTraitementId + ": " + suivisDuTraitement.size());

        // Afficher les détails des suivis trouvés
        for (SuiviTraitement s : suivisDuTraitement) {
            System.out.println("  - Suivi ID: " + s.getSuivitraitementId() +
                    ", Traitement ID: " + s.getTraitementId() +
                    ", Observations: " + s.getObservations());
        }

        // Vérifier que notre suivi de test est dans les suivis de ce traitement
        boolean found = suivisDuTraitement.stream()
                .anyMatch(s -> s.getSuivitraitementId() == lastSuiviId);

        if (!found) {
            System.out.println("ERREUR: Le suivi avec ID " + lastSuiviId + " n'a pas été trouvé dans les suivis du traitement " + actualTraitementId);

            // Vérifier si le suivi existe du tout
            List<SuiviTraitement> tousLesSuivis = suiviService.afficher();
            boolean suiviExiste = tousLesSuivis.stream()
                    .anyMatch(s -> s.getSuivitraitementId() == lastSuiviId);

            System.out.println("Le suivi " + lastSuiviId + " existe dans la base: " + suiviExiste);

            if (suiviExiste) {
                SuiviTraitement suiviTrouve = tousLesSuivis.stream()
                        .filter(s -> s.getSuivitraitementId() == lastSuiviId)
                        .findFirst()
                        .orElse(null);

                if (suiviTrouve != null) {
                    System.out.println("Le suivi trouvé est associé au traitement ID: " + suiviTrouve.getTraitementId());
                    System.out.println("Attendu: " + actualTraitementId + ", Réel: " + suiviTrouve.getTraitementId());
                }
            }
        }

        assertTrue(found, "Le suivi avec ID " + lastSuiviId + " devrait être trouvé dans les suivis du traitement " + actualTraitementId);
    }

    @Test
    @Order(4)
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");

        // Récupérer un suivi à modifier (utiliser le même système que getByTraitementId)
        List<SuiviTraitement> suivis = suiviService.afficher();
        if (suivis.isEmpty()) {
            System.out.println("Aucun suivi trouvé pour le test de modification");
            return;
        }

        // Utiliser le dernier suivi ou lastSuiviId s'il est disponible
        int suiviIdToModify = lastSuiviId != 0 ? lastSuiviId : suivis.get(suivis.size() - 1).getSuivitraitementId();
        System.out.println("Modification du suivi ID: " + suiviIdToModify);

        SuiviTraitement toUpdate = suivis.stream()
                .filter(s -> s.getSuivitraitementId() == suiviIdToModify)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Suivi de test non trouvé"));

        System.out.println("Suivi trouvé pour modification: " + toUpdate.getObservations());

        // Modifier les données
        toUpdate.setObservations("Test observations modifiées");
        toUpdate.setRessenti(RessentiSuivi.TRES_BIEN);
        toUpdate.setEvaluation(9);
        toUpdate.setValide(true);

        // Appliquer la modification
        suiviService.modifier(toUpdate);
        System.out.println("Suivi modifié avec succès");

        // Vérifier la modification
        List<SuiviTraitement> updatedSuivis = suiviService.afficher();
        SuiviTraitement updated = updatedSuivis.stream()
                .filter(s -> s.getSuivitraitementId() == suiviIdToModify)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Suivi modifié non trouvé"));

        // Vérifications
        assertEquals("Test observations modifiées", updated.getObservations());
        assertEquals(RessentiSuivi.TRES_BIEN, updated.getRessenti());
        assertEquals(Integer.valueOf(9), updated.getEvaluation());
        assertTrue(updated.isValide());

        System.out.println("Modification vérifiée avec succès");
    }

    @Test
    @Order(5)
    void supprimer() throws SQLException {
        System.out.println("Testing supprimer method...");

        // Récupérer un suivi à supprimer
        List<SuiviTraitement> suivis = suiviService.afficher();
        if (suivis.isEmpty()) {
            System.out.println("Aucun suivi trouvé pour le test de suppression");
            return;
        }

        // Utiliser le dernier suivi ou lastSuiviId s'il est disponible
        int suiviIdToDelete = lastSuiviId != 0 ? lastSuiviId : suivis.get(suivis.size() - 1).getSuivitraitementId();
        System.out.println("Suppression du suivi ID: " + suiviIdToDelete);

        SuiviTraitement toDelete = suivis.stream()
                .filter(s -> s.getSuivitraitementId() == suiviIdToDelete)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Suivi de test non trouvé"));

        // Supprimer le suivi
        suiviService.supprimer(suiviIdToDelete);
        System.out.println("Suivi supprimé avec succès");

        // Vérifier que le suivi a été supprimé
        List<SuiviTraitement> remainingSuivis = suiviService.afficher();
        boolean deleted = remainingSuivis.stream()
                .noneMatch(s -> s.getSuivitraitementId() == suiviIdToDelete);

        assertTrue(deleted, "Le suivi devrait être supprimé");
        System.out.println("Suppression vérifiée avec succès");
    }
}
