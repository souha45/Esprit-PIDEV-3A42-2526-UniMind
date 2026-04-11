package org.example.services;


import org.example.entities.DisponibilitePsy;
import org.example.enums.StatutDisponibilite;
import org.example.enums.TypeConsultation;
import org.junit.jupiter.api.*;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DisponibilitePsyServiceTest {

    static DisponibilitePsyService ds;

    //s'exécute avant tous les tests
    @BeforeAll
    static void setUp() {
        System.out.println("Setting up tests...");
        ds = new DisponibilitePsyService(); //instance qui se connecte à la base de données
    }
    //s'exécute après tous les tests
    @AfterAll
    static void tearDown(){

    }
    //Test unitaire de l'ajout d'un dispo
    @Test// test unitaire
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        ds.ajouter(new DisponibilitePsy(
                4,                                          // userId
                Date.valueOf(LocalDate.of(2026, 9, 9)),    // dateDispo (2026-06-09)
                Time.valueOf(LocalTime.of(12, 0)),         // heureDebut (12:00)
                Time.valueOf(LocalTime.of(19, 0)),         // heureFin (17:00)
                TypeConsultation.en_ligne,               // typeConsult
                "ElHamma"// lieu
                 ));
        assertFalse(ds.afficher().isEmpty()); // c'est un test aprés l'ajout qui vérifier si la base est vide ou non il doit avoir la base non vide assertFalse(attend false) si il recoit true (c'est à dire la base est vide donc l'ajout n'établie pas)
        assertTrue(ds.afficher().get(ds.afficher().size() - 1).getLieu().equals("ElHamma")
               && ds.afficher().get(ds.afficher().size() - 1).getStatut().equals(StatutDisponibilite.disponible));  // Vérifier le contenu de la dernière dispo( Est-ce que la disponibilité qu'on vient d'ajouter a les BONNES valeurs ? On vérifie 2 choses dans cette dispo :Lieu = "ElHamma" et Statut = disponible)
    }
    //Test unitaire de la modification d'un dispo
    @Test
    void modifier() throws SQLException {
            System.out.println("Testing modifier method...");
            DisponibilitePsy lastDispo = ds.afficher().get(ds.afficher().size() - 1);
            lastDispo.setLieu("ElHamma");
            lastDispo.setHeureDebut(Time.valueOf(LocalTime.of(12, 0)));
            lastDispo.setHeureFin(Time.valueOf(LocalTime.of(19, 0)));
            lastDispo.setUserId(4);
            lastDispo.setStatut(StatutDisponibilite.disponible);
            lastDispo.setTypeConsult(TypeConsultation.en_ligne);
            lastDispo.setDateDispo(Date.valueOf(LocalDate.of(2026, 9, 9)));

            ds.modifier(lastDispo);

    }

    //Test unitaire de la suppression d'un dispo
    @Test
    void supprimer() throws SQLException{
        System.out.println("Testing supprimer method...");

        //VÉRIFIER le taille de liste avant suppression
        List<DisponibilitePsy> liste = ds.afficher();
        int tailleAvant = liste.size();
        System.out.println("tailleAvant: "+tailleAvant);

        ds.supprimer(54);

        //VÉRIFIER que la suppression a fonctionné
        List<DisponibilitePsy> listeApres = ds.afficher();
        int tailleApres = listeApres.size();
        System.out.println("tailleApres: "+tailleApres);

    }

    //Test unitaire de l'affichage du liste de dispo
    @Test
    void afficher() throws SQLException{
        System.out.println("Testing afficher method...");
        List<DisponibilitePsy> liste = ds.afficher();
        System.out.println("La liste de disponibilitePsy:"+ liste);
    }

    //Test de l'affichage de disponibilitePsy disponible uniquement
    @Test
    void afficherDisponibilitesDisponibles() throws SQLException{
        System.out.println("Testing afficherDisponibilitesDisponibles method...");
        List<DisponibilitePsy> liste = ds.afficherDisponibilitesDisponibles();
        System.out.println("La liste de disponibilitePsy disponible:"+ liste);
    }
    //Test de l'affichage de disponibilitePsy d'un psy
    @Test
    void afficherDisponibilitesPsy() throws SQLException{
        System.out.println("Testing afficherDisponibilitesPsy method...");
        List<DisponibilitePsy> liste = ds.afficherDisponibilitesPsy(4);
        System.out.println("La liste de disponibilitePsy d'un psychologue:"+ liste);
    }

    //Test l'affichage d'un seul disponibilite
    @Test
    void getOne() throws SQLException{
        System.out.println("Testing getOne method...");
        DisponibilitePsy dispo = ds.getOne(53);
        System.out.println("La disponibilitePsy:"+ dispo);


    }


}
