package org.example.services;

import org.example.entities.Questionnaire;
import org.example.enums.TypeQuestionnaire;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionnaireServices implements ICrud<Questionnaire> {

    Connection con;

    public QuestionnaireServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    //ajouter
    @Override
    public void ajouter(Questionnaire questionnaire) throws SQLException {

        String sql = "INSERT INTO questionnaire (code, nom, description, type, created_at, seuil_leger, seuil_modere, seuil_severe, nbre_questions) VALUES ('"
                + questionnaire.getCode() + "','"
                + questionnaire.getNom() + "','"
                + questionnaire.getDescription() + "','"
                + questionnaire.getType().name() + "', NOW(), 0, 0, 0, 0)";

        Statement statement = con.createStatement();
        statement.executeUpdate(sql);

        System.out.println("Questionnaire ajouté avec succès !");
    }
    //affichage
    @Override
    public List<Questionnaire> afficher() throws SQLException {

        List<Questionnaire> questionnaires = new ArrayList<>();

        String sql = "SELECT * FROM questionnaire";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            Questionnaire q = new Questionnaire();

            q.setQuestionnaireId(rs.getInt("questionnaire_id"));
            q.setCode(rs.getString("code"));
            q.setNom(rs.getString("nom"));
            q.setDescription(rs.getString("description"));

            // fix enum
            q.setType(TypeQuestionnaire.valueOf(rs.getString("type").toUpperCase()));

            questionnaires.add(q);
        }

        return questionnaires;
    }
    @Override
    public void modifier(Questionnaire questionnaire) throws SQLException {

        String sql = "UPDATE questionnaire SET code=?, nom=?, description=?, type=? WHERE questionnaire_id=?";

        PreparedStatement pst = con.prepareStatement(sql);

        pst.setString(1, questionnaire.getCode());
        pst.setString(2, questionnaire.getNom());
        pst.setString(3, questionnaire.getDescription());
        pst.setString(4, questionnaire.getType().name());
        pst.setInt(5, questionnaire.getQuestionnaireId());

        pst.executeUpdate();

        System.out.println("Questionnaire modifié avec succès !");
    }
    //supprissionnn
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM questionnaire WHERE questionnaire_id=?";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, id);

        pst.executeUpdate();

        System.out.println("Questionnaire supprimé avec succès !");


    }
}