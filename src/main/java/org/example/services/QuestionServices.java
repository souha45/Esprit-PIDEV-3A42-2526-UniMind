package org.example.services;

import org.example.entities.Question;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionServices implements ICrud<Question> {

    Connection con;

    public QuestionServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    // AJOUT
    @Override
    public void ajouter(Question q) throws SQLException {

        String sql = "INSERT INTO question (texte, questionnaire_id, options_quest, score_options, created_at) VALUES ('"
                + q.getTexte() + "','"
                + q.getQuestionnaireId() + "','"
                + q.getOptionsQuest() + "','"
                + q.getScoreOptions() + "', NOW())";

        Statement st = con.createStatement();
        st.executeUpdate(sql);

        System.out.println("Question ajoutée avec succès !");
    }

    // AFFICHAGE
    @Override
    public List<Question> afficher() throws SQLException {

        List<Question> list = new ArrayList<>();

        String sql = "SELECT * FROM question";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Question q = new Question();

            q.setQuestionId(rs.getInt("question_id"));
            q.setTexte(rs.getString("texte"));
            q.setOptionsQuest(rs.getString("options_quest"));
            q.setScoreOptions(rs.getString("score_options"));
            q.setQuestionnaireId(rs.getInt("questionnaire_id"));

            list.add(q);
        }

        return list;
    }

    // MODIFIER
    @Override
    public void modifier(Question q) throws SQLException {

        String sql = "UPDATE question SET texte=?, options_quest=?, score_options=? WHERE question_id=?";

        PreparedStatement pst = con.prepareStatement(sql);

        pst.setString(1, q.getTexte());
        pst.setString(2, q.getOptionsQuest());
        pst.setString(3, q.getScoreOptions());
        pst.setInt(4, q.getQuestionId());

        pst.executeUpdate();

        System.out.println("Question modifiée avec succès !");
    }

    // SUPPRIMER
    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM question WHERE question_id=?";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, id);

        pst.executeUpdate();

        System.out.println("Question supprimée avec succès !");
    }
}