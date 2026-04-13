package org.example.main;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.enums.PrioriteTraitement;
import org.example.services.SuiviTraitementService;
import org.example.services.TraitementService;
import org.example.utils.MyDataBase_Unimind;

public class Main {

    public static void main(String[] args) {
        // Test connexion
        MyDataBase_Unimind db = MyDataBase_Unimind.getInstance();
        if (db.getConnection() == null) {
            System.out.println(" Échec de la connexion à la base de données.");
            return;
        }
        System.out.println(" Connexion à la base de données réussie !");


        TraitementService traitementService = new TraitementService();
        SuiviTraitementService suiviService = new SuiviTraitementService();

        try {
            // CRUD TRAITEMENT
            System.out.println("\n * CRUD TRAITEMENT : ");

            // CREATE
            Traitement t1 = new Traitement(
                    "Thérapie CBT", "Thérapie cognitivo-comportementale", "CBT",
                    org.example.enums.CategorieTraitement.COGNITIF,
                    30,
                    null,
                    new Date(System.currentTimeMillis()), // dateDebut
                    null, // dateFin
                    org.example.enums.StatutTraitement.EN_COURS, // statut
                    PrioriteTraitement.MOYENNE, // priorite
                    "Réduire l'anxiété", // objectifTherapeutique
                    1, // psychologueId
                    2 // etudiantId
            );
            // createdAt est automatiquement initialisé dans le constructeur

            traitementService.ajouter(t1);
            System.out.println("CREATE Traitement: " + t1.getTitre());

            // READ ALL
            System.out.println("\nREAD ALL Traitements:");
            List<Traitement> traitements = traitementService.afficher();
            for (Traitement t : traitements) {
                System.out.println("ID: " + t.getTraitementId() +
                        ", Titre: " + t.getTitre() +
                        ", Type: " + t.getType() +
                        ", Catégorie: " + t.getCategorie() +
                        ", Durée: " + t.getDureeJours() + " jours" +
                        ", Statut: " + t.getStatut() +
                        ", Priorité: " + t.getPriorite() +
                        ", Début: " + t.getDateDebut() +
                        ", Objectif: " + t.getObjectifTherapeutique());
            }

            // UPDATE
            if (!traitements.isEmpty()) {
                Traitement toUpdate = traitements.get(0);
                toUpdate.setTitre(toUpdate.getTitre() + " (modifié)");
                toUpdate.setDescription("Description mise à jour");
                traitementService.modifier(toUpdate);
                System.out.println("\nUPDATE Traitement: " + toUpdate.getTitre());
            }

            // CRUD SUIVITRAITEMENT
            System.out.println("\n * CRUD SUIVITRAITEMENT : ");

            // Récupérer l'ID du dernier traitement pour le suivi
            int dernierTraitementId = traitements.get(traitements.size() - 1).getTraitementId();

            // CREATE
            SuiviTraitement s1 = new SuiviTraitement(
                    new Date(System.currentTimeMillis()), // dateSuivi
                    true, // effectue
                    null, // heurePrevue
                    false, // valide
                    dernierTraitementId, // traitementId
                    "Patient motivé.", // observations
                    "Bon progrès.", // observationsPsy
                    org.example.enums.RessentiSuivi.BIEN, // ressenti
                    org.example.enums.SaisiPar.PSYCHOLOGUE // saisiPar
            );
            s1.setEvaluation(7); // evaluation n'est pas dans le constructeur
            // dateSaisie et createdAt sont automatiquement initialisés dans le constructeur

            suiviService.ajouter(s1);
            System.out.println("CREATE SuiviTraitement: " + s1.getRessenti());

            // READ ALL
            System.out.println("\nREAD ALL SuiviTraitement:");
            List<SuiviTraitement> suivis = suiviService.afficher();
            for (SuiviTraitement s : suivis) {
                System.out.println("ID: " + s.getSuivitraitementId() +
                        ", TraitementID: " + s.getTraitementId() +
                        ", DateSuivi: " + s.getDateSuivi() +
                        ", DateSaisie: " + s.getDateSaisie() +
                        ", Effectué: " + s.isEffectue() +
                        ", HeurePrévue: " + s.getHeurePrevue() +
                        ", HeureEffective: " + s.getHeureEffective() +
                        ", Evaluation: " + s.getEvaluation() +
                        ", Ressenti: " + s.getRessenti() +
                        ", SaisiPar: " + s.getSaisiPar() +
                        ", Validé: " + s.isValide() +
                        ", Observations: " + s.getObservations());
            }

            // UPDATE
            if (!suivis.isEmpty()) {
                SuiviTraitement toUpdate = suivis.get(0);
                toUpdate.setEvaluation(8);
                toUpdate.setRessenti(org.example.enums.RessentiSuivi.TRES_BIEN);
                suiviService.modifier(toUpdate);
                System.out.println("\nUPDATE SuiviTraitement: " + toUpdate.getRessenti());
            }

            // DELETE SuiviTraitement
            if (!suivis.isEmpty()) {
                SuiviTraitement toDelete = suivis.get(0);
                suiviService.supprimer(toDelete.getSuivitraitementId());
                System.out.println("\nDELETE SuiviTraitement: " + toDelete.getRessenti());
            }

            System.out.println("\nTest CRUD terminé avec succès.");

        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}