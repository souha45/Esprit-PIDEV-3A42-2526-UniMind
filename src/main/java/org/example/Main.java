package org.example;

import org.example.entities.DisponibilitePsy;
import org.example.enums.StatutDisponibilite;
import org.example.services.DisponibilitePsyService;
import org.example.enums.TypeConsultation;
import org.example.utils.MyDataBase_Unimind;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class Main {
    public static void main(String[] args) {
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

        //Test ajout dispo

            //DisponibilitePsy disponibilite = new DisponibilitePsy(
            //        5,                                          // userId
            //        Date.valueOf(LocalDate.of(2026, 6, 9)),    // dateDispo (2026-06-09)
             //       Time.valueOf(LocalTime.of(12, 0)),         // heureDebut (12:00)
            //        Time.valueOf(LocalTime.of(17, 0)),         // heureFin (17:00)
            //        TypeConsultation.Presentiel,               // typeConsult
             //       "France"// lieu
             //       );

            // Ajouter à la base
            //dpService.ajouter(disponibilite);


        //Test Modifier dispo
            DisponibilitePsy disponibiliteModifiee = new DisponibilitePsy(
                    3,                          // dispoId (l'ID à modifier)
                    5,                          // userId
                    Date.valueOf(LocalDate.of(2026, 6, 9)),    // dateDispo
                    Time.valueOf(LocalTime.of(12, 0)),         // heureDebut
                    Time.valueOf(LocalTime.of(17, 0)),         // heureFin
                    TypeConsultation.Presentiel,               // typeConsult
                    "France2",                                 // lieu
                    StatutDisponibilite.disponible             // statut
            );


                    dpService.modifier(disponibiliteModifiee);

        //Test supprimer dispo
                    //dpService.supprimer(4);

        //Test Affiche dispo
            System.out.println(dpService.afficher());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}