package org.example;

import org.example.entities.Consultation;
import org.example.entities.ConsultationDetail;
import org.example.entities.DisponibilitePsy;
import org.example.entities.RendezVous;
import org.example.enums.StatutDisponibilite;
import org.example.services.DisponibilitePsyService;
import org.example.services.ConsultationService;
import org.example.enums.TypeConsultation;
import org.example.services.RendezVousService;
import org.example.utils.MyDataBase_Unimind;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {
        // Établir la connexion à la base de données
        MyDataBase_Unimind db = MyDataBase_Unimind.getInstance();

        // Vérifier si la connexion est établie
        if (db.getConnection() != null) {
            System.out.println("✓ Connexion à la base de données réussie !");
        } else {
            System.out.println("✗ Échec de la connexion à la base de données.");
        }

        DisponibilitePsyService dpService = new DisponibilitePsyService();

        try {
       //     *********************** TEST CRUD DISPONIBILITEDISPO *************************

        //Test ajout dispo

            DisponibilitePsy disponibilite = new DisponibilitePsy(
                    4,                                          // userId
                    Date.valueOf(LocalDate.of(2026, 9, 9)),    // dateDispo (2026-06-09)
                    Time.valueOf(LocalTime.of(12, 0)),         // heureDebut (12:00)
                    Time.valueOf(LocalTime.of(19, 0)),         // heureFin (17:00)
                    TypeConsultation.en_ligne,               // typeConsult
                   "sfax"// lieu
                    );

            // Ajouter à la base
            dpService.ajouter(disponibilite);


        //Test Modifier dispo
            DisponibilitePsy disponibiliteModifiee = new DisponibilitePsy(
                    3,                          // dispoId (l'ID à modifier)
                    5,                          // userId
                    Date.valueOf(LocalDate.of(2026, 6, 9)),    // dateDispo
                    Time.valueOf(LocalTime.of(12, 0)),         // heureDebut
                    Time.valueOf(LocalTime.of(17, 0)),         // heureFin
                    TypeConsultation.présentiel,               // typeConsult
                    "France2",                                 // lieu
                    StatutDisponibilite.disponible             // statut
            );


                    dpService.modifier(disponibiliteModifiee);

        //Test supprimer dispo
            // dpService.supprimer(4);

        //Test Affiche dispo
           // System.out.println(dpService.afficher());

        //Test Afficher disponibilité dispo uniquement
            System.out.println(dpService.afficherDisponibilitesDisponibles());

            //Test Afficher disponibilité d'un psy connecté uniquement
            System.out.println(dpService.afficherDisponibilitesPsy(5));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        RendezVousService rdService = new RendezVousService();
        //     *********************** TEST CRUD RENDEZVOUS *************************



        try {

            //Test ajout rendezVous
            //RendezVous rdvajout = new RendezVous(
            //       10,
            //       3,
            //      5,
            //      "je suis malade complètement malade"
            //);

            //rdService.ajouter(rdvajout);


            //Test Affiche RendezVous par étudiant
            System.out.println(rdService.afficherRendezVousDetailsByEtudiant(3));

            //Test Affiche rdv spécifique
            System.out.println(rdService.afficherRendezVousById(3, 14));


            //Test l'annulation d'un rdv et Test création automatique de consultation
            rdService.modifierStatutRendezVous(14, 3,4, "terminé");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        //     *********************** TEST CRUD CONSULTATION *************************

        //Tetst modification consultation
        ConsultationService cService = new ConsultationService();
        try{
            //Consultation consultationModifié = new Consultation(
            //        1,
             //       2,
             //       4,
              //      3,
              //      "problème bizarre",
               //     (short) 10
            //);


            //cService.modifier(consultationModifié);

            //Test afficher liste consultation by etudiant
           System.out.println(cService.getConsultationsDetailByPsy(4));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }
}