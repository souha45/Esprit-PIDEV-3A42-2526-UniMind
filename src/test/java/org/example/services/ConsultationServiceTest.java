package org.example.services;

import org.example.models.Consultation;

import org.example.models.ConsultationDetail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

public class ConsultationServiceTest {

    static ConsultationService cs;

    @BeforeAll
//    @BeforeEach
    static void setUp() {
        System.out.println("Setting up tests...");
        cs =  new ConsultationService();
    }

    @AfterAll
//    @AfterEach
    static void tearDown() {
    }

    //Test modifier consultation
    @Test
    void modifier() throws SQLException {
        System.out.println("Testing modifier method...");
        Consultation lastConsultation = cs.afficher().get(cs.afficher().size() - 1);
        lastConsultation.setAvisPsy("kheyba khlass");
        lastConsultation.setNoteSatisfaction((short) 1);
        lastConsultation.setRendezVousId(17);
        lastConsultation.setEtudiantUserId(3);
        lastConsultation.setPsyUserId(4);


        cs.modifier(lastConsultation);

    }

    //Test afficher la liste de consultation pour un étudiant
    @Test
    void getConsultationsDetailByEtudiant() throws SQLException{
        System.out.println("Testing getConsultationsDetailByEtudiant method...");
        List<ConsultationDetail> listeConsultation = cs.getConsultationsDetailByEtudiant(3);
        System.out.println("La liste de Consultation par Etudiant:"+ listeConsultation);
    }

    //Test afficher la liste de consultation par un psy
    @Test
    void getConsultationsDetailByPsy() throws SQLException{
        System.out.println("Testing getConsultationsDetailByPsy method...");
        List<ConsultationDetail> listeConsultation = cs.getConsultationsDetailByPsy(4);
        System.out.println("La liste de Consultation par Psychologue:"+ listeConsultation);
    }
}
