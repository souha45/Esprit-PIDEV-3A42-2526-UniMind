package org.example.main;

import org.example.utils.MyDataBase_Unimind;
import org.example.services.*;
import org.example.entities.*;
import org.example.enums.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * Main class for testing CRUD operations
 */
public class Main {
    public static void main(String[] args) {
        // Établir la connexion à la base de données
        MyDataBase_Unimind db = MyDataBase_Unimind.getInstance();


        // Vérifier si la connexion est établie
        if (db.getConnection() != null) {
            System.out.println("Connexion à la base de données réussie !");

            try {
                System.out.println("\n--- TEST CRUD MODULE GESTION EVENEMENTS (COMPLET) ---");

                EvenementService evenementService = new EvenementService();
                SponsorService sponsorService = new SponsorService();
                EvenementSponsorService evenementSponsorService = new EvenementSponsorService();
                ParticipationService participationService = new ParticipationService();
                FavoriService favoriService = new FavoriService();

                // 1) EVENEMENT - Create
                System.out.println("\n[1] EVENEMENT - Creation");
                String uniqueEventName = "Test CRUD " + System.currentTimeMillis();
                Evenement evenement = new Evenement(
                        uniqueEventName, "Evenement de test",
                        TypeEvenement.ATELIER,
                        Timestamp.valueOf("2026-07-15 09:00:00"),
                        Timestamp.valueOf("2026-07-15 17:00:00"),
                        "Salle A", 30, 0, StatutEvenement.A_VENIR,
                        Timestamp.valueOf("2026-07-10 23:59:59"),
                        1,
                        null, null, null
                );
                evenementService.ajouter(evenement);
                int evenementId = evenement.getEvenementId();
                System.out.println("Evenement cree. ID=" + evenementId);

                // EVENEMENT - Read
                System.out.println("[1] EVENEMENT - Lecture");
                Evenement evenementLu = evenementService.findById(evenementId);
                System.out.println("Evenement lu: " + (evenementLu != null ? evenementLu.getTitre() : "NON TROUVE"));

                // EVENEMENT - Update
                System.out.println("[1] EVENEMENT - Modification");
                if (evenementLu != null) {
                    String uniqueTitle = "Test CRUD - MODIFIE " + System.currentTimeMillis();
                    evenementLu.setTitre(uniqueTitle);
                    evenementLu.setStatut(StatutEvenement.EN_COURS);
                    evenementService.modifier(evenementLu);
                    System.out.println("Evenement modifie");
                }

                // 2) SPONSOR - Create
                System.out.println("\n[2] SPONSOR - Creation");
                String uniqueSponsorName = "Sponsor CRUD " + System.currentTimeMillis();
                Sponsor sponsor = new Sponsor(
                        uniqueSponsorName,
                        TypeSponsor.ENTREPRISE,
                        "https://example.com",
                        "crud@example.com",
                        "00000000",
                        "Adresse test",
                        "Domaine test",
                        StatutSponsor.CONFIRME,
                        null
                );
                sponsorService.ajouter(sponsor);
                int sponsorId = sponsor.getSponsorId();
                System.out.println("Sponsor cree. ID=" + sponsorId);

                // SPONSOR - Read
                System.out.println("[2] SPONSOR - Lecture");
                Sponsor sponsorLu = sponsorService.findById(sponsorId);
                System.out.println("Sponsor lu: " + (sponsorLu != null ? sponsorLu.getNomSponsor() : "NON TROUVE"));

                // SPONSOR - Update
                System.out.println("[2] SPONSOR - Modification");
                if (sponsorLu != null) {
                    String uniqueName = "Sponsor CRUD - MODIFIE " + System.currentTimeMillis();
                    sponsorLu.setNomSponsor(uniqueName);
                    sponsorService.modifier(sponsorLu);
                    System.out.println("Sponsor modifie");
                }

                // 3) EVENEMENT_SPONSOR - Create
                System.out.println("\n[3] EVENEMENT_SPONSOR - Creation");
                EvenementSponsor es = new EvenementSponsor(
                        new BigDecimal("1000.00"),
                        TypeContribution.FINANCIER,
                        "Contribution test",
                        new Timestamp(System.currentTimeMillis()),
                        StatutSponsor.CONFIRME,
                        evenementId,
                        sponsorId
                );
                evenementSponsorService.ajouter(es);
                int evenementSponsorId = es.getEvenementSponsorId();
                System.out.println("EvenementSponsor cree. ID=" + evenementSponsorId);

                // EVENEMENT_SPONSOR - Read
                System.out.println("[3] EVENEMENT_SPONSOR - Lecture");
                EvenementSponsor esLu = evenementSponsorService.findById(evenementSponsorId);
                System.out.println("EvenementSponsor lu: " + (esLu != null ? "ID=" + esLu.getEvenementSponsorId() : "NON TROUVE"));

                // EVENEMENT_SPONSOR - Update
                System.out.println("[3] EVENEMENT_SPONSOR - Modification");
                if (esLu != null) {
                    esLu.setDescriptionContribution("Contribution test - MODIFIE");
                    evenementSponsorService.modifier(esLu);
                    System.out.println("EvenementSponsor modifie");
                }

                // 4) PARTICIPATION - Create
                System.out.println("\n[4] PARTICIPATION - Creation");

                // Récupérer un étudiant qui existe vraiment dans la base
                int etudiantId = 0;

                try {
                    java.sql.Connection conn = db.getConnection();
                    java.sql.Statement stmt = conn.createStatement();
                    java.sql.ResultSet rs = stmt.executeQuery("SELECT user_id FROM `user` WHERE role = 'etudiant' LIMIT 1");
                    if (rs.next()) {
                        etudiantId = rs.getInt("user_id");
                        System.out.println("Etudiant trouve dans la base: " + etudiantId);
                    } else {
                        System.out.println("Aucun etudiant trouve, utilisation de l'ID 200 par defaut");
                        etudiantId = 200;
                    }
                    rs.close();
                    stmt.close();
                } catch (Exception e) {
                    System.out.println("Erreur lors de la recherche d'etudiant: " + e.getMessage());
                    etudiantId = 200;
                }

                Participation participation = new Participation(
                        evenementId,
                        etudiantId,
                        StatutParticipation.CONFIRME
                );
                participationService.ajouter(participation);
                int participationId = participation.getParticipationId();
                System.out.println("Participation creee. ID=" + participationId);

                // PARTICIPATION - Read
                System.out.println("[4] PARTICIPATION - Lecture");
                Participation participationLu = participationService.findById(participationId);
                System.out.println("Participation lue: " + (participationLu != null ? "ID=" + participationLu.getParticipationId() : "NON TROUVE"));

                // PARTICIPATION - Update
                System.out.println("[4] PARTICIPATION - Modification");
                if (participationLu != null) {
                    participationLu.setStatut(StatutParticipation.ANNULE);
                    participationService.modifier(participationLu);
                    System.out.println("Participation modifiee");
                }

                // 5) FAVORI - Create
                System.out.println("\n[5] FAVORI - Creation");
                System.out.println("Utilisation du meme etudiant ID: " + etudiantId + " pour le favori");
                Favori favori = new Favori(
                        0,
                        new Timestamp(System.currentTimeMillis()),
                        evenementId,
                        etudiantId  // Utilise le même étudiant trouvé précédemment
                );
                favoriService.ajouter(favori);
                int favoriId = favori.getId();
                System.out.println("Favori cree. ID=" + favoriId);

                // FAVORI - Read
                System.out.println("[5] FAVORI - Lecture");
                Favori favoriLu = favoriService.findById(favoriId);
                System.out.println("Favori lu: " + (favoriLu != null ? "ID=" + favoriLu.getId() : "NON TROUVE"));

                // Nettoyage (Delete) - ordre important (liens/fk)
                System.out.println("\n[6] NETTOYAGE - Suppressions");
                favoriService.supprimer(favoriId);
                System.out.println("Favori supprime");

                participationService.supprimer(participationId);
                System.out.println("Participation supprimee");

                evenementSponsorService.supprimer(evenementSponsorId);
                System.out.println("EvenementSponsor supprime");

                sponsorService.supprimer(sponsorId);
                System.out.println("Sponsor supprime");

                evenementService.supprimer(evenementId);
                System.out.println("Evenement supprime");

                System.out.println("\n--- FIN TEST CRUD COMPLET ---");
            } catch (SQLException e) {
                System.err.println("Erreur SQL: " + e.getMessage());
            }
        } else {
            System.out.println("Échec de la connexion à la base de données.");
        }
    }
}
