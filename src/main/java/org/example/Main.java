package org.example;

import org.example.entities.Question;
import org.example.entities.Questionnaire;
import org.example.services.QuestionServices;
import org.example.services.QuestionnaireServices;
import org.example.utils.MyDataBase_Unimind;
import org.example.enums.TypeQuestionnaire;

import java.sql.SQLException;


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
        //test ta3 ajout

        // test ajout
        QuestionnaireServices qes = new QuestionnaireServices();

        try {
            System.out.println("\n** l'ajout **");
            //qes.ajouter(new Questionnaire("Q1", "Stress", "stress fait mal", TypeQuestionnaire.STRESS));
            //qes.ajouter(new Questionnaire("Q2", "depression", "deeepreesssiooonn", TypeQuestionnaire.DEPRESSION));
            // qes.ajouter(new Questionnaire("Q3", "sommeil", "noummmm", TypeQuestionnaire.SOMMEIL));
            //*qes.ajouter(new Questionnaire("Q4", "energieee", "power", TypeQuestionnaire.SOMMEIL));
            System.out.println("\n** l'affiche **");

            System.out.println(qes.afficher());
            //suppression
            System.out.println("\n** supprimer **");
            qes.supprimer(45);
            // affichage 2 ba3d supp
            System.out.println("\n ** apres suppression ** ");
            System.out.println(qes.afficher());

            //modification

            // Questionnaire q = new Questionnaire("Q20555", "Stress modifié", "new description", TypeQuestionnaire.STRESS);
            // Questionnaire q= new Questionnaire("Q33", "ok", "nermine", TypeQuestionnaire.SOMMEIL);
            // q.setQuestionnaireId(47);
            //q.setQuestionnaireId(45);


            //qes.modifier(q);

            //System.out.println("\n** apres modification **");
            //System.out.println(qes.afficher());

            //***** partie question

            // System.out.println("\n** QUESTION **");

            //QuestionServices qs = new QuestionServices();

// ajout
            //qs.ajouter(new Question("Comment vous sentez-vous ?",45,"A,B,C","1,2,3"));
            //qs.ajouter((new Question("cc cv ?",1,"oui,non","5,10")));
            //qs.ajouter(new Question("cc cv ?",45,"oui,non","1,0"));

// affichage
            // System.out.println("\n-- LISTE QUESTIONS --");
            // System.out.println(qs.afficher());

// modification

            //Question question = new Question("oooo llalallal", 45, "Pas du tout,Un peu", "0,2");

            // question.setQuestionId(87);

            // qs.modifier(question);
// affichage
            //System.out.println("\n-- APRES MODIFICATION --");
            //System.out.println(qs.afficher());
            //suppression
            //System.out.println("\n** supprimer **");
            //qs.supprimer(86);
            // affichage 2 ba3d supp
            // System.out.println("\n ** apres suppression ** ");
            //System.out.println(qs.afficher());


        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
