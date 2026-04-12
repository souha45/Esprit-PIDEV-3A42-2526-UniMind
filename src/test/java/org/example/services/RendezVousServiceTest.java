package org.example.services;

import org.example.models.RendezVous;
import org.example.models.RendezVousDetail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RendezVousServiceTest {

    static RendezVousService rdvs;

    @BeforeAll
//    @BeforeEach
    static void setUp() {
        System.out.println("Setting up tests...");
        rdvs =  new RendezVousService();
    }

    @AfterAll
//    @AfterEach
    static void tearDown() {
    }

    //Test l'ajout d'un rdv
    @Test
    void ajouter() throws SQLException {
        System.out.println("Testing ajouter method...");
        rdvs.ajouter(new RendezVous( 58,
                       3,
                     4,
                      "neeee mridh!!!"));
        assertFalse(rdvs.afficher().isEmpty());
        assertTrue(rdvs.afficher().get(rdvs.afficher().size() - 1).getMotif().equals("neeee mridh!!!")
                );
    }

    //Test Afficher la liste de rendezVous par étudiant connecté
    @Test
    void afficherRendezVousDetailsByEtudiant() throws SQLException{
        System.out.println("Testing afficherRendezVousDetailsByEtudiant method...");
        List<RendezVousDetail> listeRDV = rdvs.afficherRendezVousDetailsByEtudiant(3);
        System.out.println("La liste de rendezVous par Etudiant:"+ listeRDV);
    }

    //Test Afficher les détails d'un rdv
    @Test
    void afficherRendezVousById() throws SQLException{
        System.out.println("Testing afficherRendezVousById method...");
        RendezVousDetail rendezVous = rdvs.afficherRendezVousById(3, 17);
        System.out.println("Le RendezVous:"+ rendezVous);
    }

    //Méthodes pour annuler un rdv accepter ou refuser un rdv
    @Test
    void modifierStatutRendezVous() throws SQLException{
        System.out.println("Testing TraiterRdv method...");
       rdvs.modifierStatutRendezVous(17, 3, 4, "terminé");

    }


}
