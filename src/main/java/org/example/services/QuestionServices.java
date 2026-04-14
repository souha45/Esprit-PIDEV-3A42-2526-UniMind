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

    //  AJOUTER
    @Override
    public void ajouter(Question q) throws SQLException {

        // Vérifier que le questionnaire_id existe
        String checkSql = "SELECT COUNT(*) FROM questionnaire WHERE questionnaire_id = ?";
        PreparedStatement checkPst = con.prepareStatement(checkSql);
        checkPst.setInt(1, q.getQuestionnaireId());
        ResultSet checkRs = checkPst.executeQuery();
        if (checkRs.next() && checkRs.getInt(1) == 0) {
            throw new SQLException("Questionnaire introuvable avec l'ID=" + q.getQuestionnaireId());
        }

        // Vérifier que le texte n'est pas vide
        if (q.getTexte() == null || q.getTexte().trim().isEmpty()) {
            throw new SQLException("Le texte de la question ne peut pas être vide !");
        }

        String sql = "INSERT INTO question (texte, questionnaire_id, options_quest, " +
                "score_options, type_question, created_at) VALUES (?, ?, ?, ?, ?, NOW())";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, q.getTexte());
        pst.setInt(2, q.getQuestionnaireId());
        pst.setString(3, q.getOptionsQuest());
        pst.setString(4, q.getScoreOptions());
        pst.setString(5, q.getTypeQuestion());

        pst.executeUpdate();
        System.out.println("Question ajoutée avec succès !");
    }

    //  AFFICHER (toutes les questions)
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
            q.setTypeQuestion(rs.getString("type_question"));
            q.setQuestionnaireId(rs.getInt("questionnaire_id"));
            q.setCreatedAt(rs.getTimestamp("created_at"));
            q.setUpdatedAt(rs.getTimestamp("updated_at"));
            list.add(q);
        }

        return list;
    }

    //  AFFICHER PAR QUESTIONNAIRE

    public List<Question> afficherParQuestionnaire(int questionnaireId) throws SQLException {

        List<Question> list = new ArrayList<>();

        String sql = "SELECT * FROM question WHERE questionnaire_id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, questionnaireId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Question q = new Question();
            q.setQuestionId(rs.getInt("question_id"));
            q.setTexte(rs.getString("texte"));
            q.setOptionsQuest(rs.getString("options_quest"));
            q.setScoreOptions(rs.getString("score_options"));
            q.setTypeQuestion(rs.getString("type_question"));
            q.setQuestionnaireId(rs.getInt("questionnaire_id"));
            q.setCreatedAt(rs.getTimestamp("created_at"));
            q.setUpdatedAt(rs.getTimestamp("updated_at"));
            list.add(q);
        }

        return list;
    }

    //  MODIFIER
    @Override
    public void modifier(Question q) throws SQLException {

        //  Vérifier texte non vide
        if (q.getTexte() == null || q.getTexte().trim().isEmpty()) {
            throw new SQLException("Le texte de la question ne peut pas être vide !");
        }

        String sql = "UPDATE question SET texte=?, options_quest=?, score_options=?, " +
                "type_question=?, updated_at=NOW() WHERE question_id=?";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, q.getTexte());
        pst.setString(2, q.getOptionsQuest());
        pst.setString(3, q.getScoreOptions());
        pst.setString(4, q.getTypeQuestion());
        pst.setInt(5, q.getQuestionId());

        int rows = pst.executeUpdate();

        if (rows == 0) {
            throw new SQLException("Aucune question trouvée avec l'ID=" + q.getQuestionId());
        }

        System.out.println("Question modifiée avec succès !");
    }

    //  SUPPRIMER

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM question WHERE question_id=?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, id);

        int rows = pst.executeUpdate();

        if (rows == 0) {
            throw new SQLException("Aucune question trouvée avec l'ID=" + id);
        }

        System.out.println("Question supprimée avec succès ! ID=" + id);
    }
}