package org.example.services;

import org.example.entities.Reponsequestionnaire;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseQuestionnaireServices implements ICrud<Reponsequestionnaire> {

    Connection con;

    public ReponseQuestionnaireServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }


    //  AJOUTER

    @Override
    public void ajouter(Reponsequestionnaire r) throws SQLException {

        //  Vérifier que le questionnaire existe
        String checkSql = "SELECT COUNT(*) FROM questionnaire WHERE questionnaire_id = ?";
        PreparedStatement checkPst = con.prepareStatement(checkSql);
        checkPst.setInt(1, r.getQuestionnaireId());
        ResultSet checkRs = checkPst.executeQuery();
        if (checkRs.next() && checkRs.getInt(1) == 0) {
            throw new SQLException("Questionnaire introuvable avec l'ID=" + r.getQuestionnaireId());
        }

        // Vérifier que reponseQuest n'est pas vide
        if (r.getReponseQuest() == null || r.getReponseQuest().trim().isEmpty()) {
            throw new SQLException("Les réponses aux questions ne peuvent pas être vides !");
        }

        //  Vérifier que le niveau n'est pas vide
        if (r.getNiveau() == null || r.getNiveau().trim().isEmpty()) {
            throw new SQLException("Le niveau ne peut pas être vide !");
        }

        String sql = "INSERT INTO reponse_questionnaire " +
                "(score_totale, reponse_quest, interpretation, created_at, " +
                "duree_passage, niveau, a_besoin_psy, commentaire, questionnaire_id, user_id) " +
                "VALUES (?, ?, ?, NOW(), ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setDouble(1, r.getScoreTotale());
        pst.setString(2, r.getReponseQuest());
        pst.setString(3, r.getInterpretation());

        // duree_passage NULL possible
        if (r.getDureePassage() != null) {
            pst.setInt(4, r.getDureePassage());
        } else {
            pst.setNull(4, Types.INTEGER);
        }

        pst.setString(5, r.getNiveau());
        pst.setBoolean(6, r.isaBesoinPsy());

        // commentaire NULL possible
        if (r.getCommentaire() != null) {
            pst.setString(7, r.getCommentaire());
        } else {
            pst.setNull(7, Types.LONGVARCHAR);
        }

        pst.setInt(8, r.getQuestionnaireId());

        // user_id NULL possible
        if (r.getUserId() != null) {
            pst.setInt(9, r.getUserId());
        } else {
            pst.setNull(9, Types.INTEGER);
        }

        pst.executeUpdate();
        System.out.println("Réponse ajoutée avec succès !");
    }


    //  AFFICHER (toutes les réponses li Admin)

    @Override
    public List<Reponsequestionnaire> afficher() throws SQLException {

        List<Reponsequestionnaire> list = new ArrayList<>();

        String sql = "SELECT * FROM reponse_questionnaire";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Reponsequestionnaire r = new Reponsequestionnaire();

            r.setReponseQuestionnaireId(rs.getInt("reponse_questionnaire_id"));
            r.setScoreTotale(rs.getDouble("score_totale"));
            r.setReponseQuest(rs.getString("reponse_quest"));
            r.setInterpretation(rs.getString("interpretation"));
            r.setCreatedAt(rs.getTimestamp("created_at"));
            r.setUpdatedAt(rs.getTimestamp("updated_at"));
            r.setNiveau(rs.getString("niveau"));
            r.setaBesoinPsy(rs.getBoolean("a_besoin_psy"));
            r.setCommentaire(rs.getString("commentaire"));
            r.setQuestionnaireId(rs.getInt("questionnaire_id"));

            // duree_passage NULL possible
            int duree = rs.getInt("duree_passage");
            r.setDureePassage(rs.wasNull() ? null : duree);

            // user_id NULL possible
            int userId = rs.getInt("user_id");
            r.setUserId(rs.wasNull() ? null : userId);

            list.add(r);
        }

        return list;
    }


    //  AFFICHER PAR USER (Etudiant voit ses réponses)

    public List<Reponsequestionnaire> afficherParUser(int userId) throws SQLException {

        List<Reponsequestionnaire> list = new ArrayList<>();

        String sql = "SELECT * FROM reponse_questionnaire WHERE user_id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, userId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Reponsequestionnaire r = new Reponsequestionnaire();

            r.setReponseQuestionnaireId(rs.getInt("reponse_questionnaire_id"));
            r.setScoreTotale(rs.getDouble("score_totale"));
            r.setReponseQuest(rs.getString("reponse_quest"));
            r.setInterpretation(rs.getString("interpretation"));
            r.setCreatedAt(rs.getTimestamp("created_at"));
            r.setUpdatedAt(rs.getTimestamp("updated_at"));
            r.setNiveau(rs.getString("niveau"));
            r.setaBesoinPsy(rs.getBoolean("a_besoin_psy"));
            r.setCommentaire(rs.getString("commentaire"));
            r.setQuestionnaireId(rs.getInt("questionnaire_id"));
            r.setUserId(userId);

            int duree = rs.getInt("duree_passage");
            r.setDureePassage(rs.wasNull() ? null : duree);

            list.add(r);
        }

        return list;
    }


    //  AFFICHER PAR QUESTIONNAIRE (Admin)

    public List<Reponsequestionnaire> afficherParQuestionnaire(int questionnaireId) throws SQLException {

        List<Reponsequestionnaire> list = new ArrayList<>();

        String sql = "SELECT * FROM reponse_questionnaire WHERE questionnaire_id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, questionnaireId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Reponsequestionnaire r = new Reponsequestionnaire();

            r.setReponseQuestionnaireId(rs.getInt("reponse_questionnaire_id"));
            r.setScoreTotale(rs.getDouble("score_totale"));
            r.setReponseQuest(rs.getString("reponse_quest"));
            r.setInterpretation(rs.getString("interpretation"));
            r.setCreatedAt(rs.getTimestamp("created_at"));
            r.setUpdatedAt(rs.getTimestamp("updated_at"));
            r.setNiveau(rs.getString("niveau"));
            r.setaBesoinPsy(rs.getBoolean("a_besoin_psy"));
            r.setCommentaire(rs.getString("commentaire"));
            r.setQuestionnaireId(questionnaireId);

            int duree = rs.getInt("duree_passage");
            r.setDureePassage(rs.wasNull() ? null : duree);

            int userId = rs.getInt("user_id");
            r.setUserId(rs.wasNull() ? null : userId);

            list.add(r);
        }

        return list;
    }

    //  MODIFIER (Admin)

    @Override
    public void modifier(Reponsequestionnaire r) throws SQLException {

        // Vérifier niveau non vide
        if (r.getNiveau() == null || r.getNiveau().trim().isEmpty()) {
            throw new SQLException("Le niveau ne peut pas être vide !");
        }

        String sql = "UPDATE reponse_questionnaire SET " +
                "score_totale=?, interpretation=?, updated_at=NOW(), " +
                "duree_passage=?, niveau=?, a_besoin_psy=?, commentaire=? " +
                "WHERE reponse_questionnaire_id=?";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setDouble(1, r.getScoreTotale());
        pst.setString(2, r.getInterpretation());

        if (r.getDureePassage() != null) {
            pst.setInt(3, r.getDureePassage());
        } else {
            pst.setNull(3, Types.INTEGER);
        }

        pst.setString(4, r.getNiveau());
        pst.setBoolean(5, r.isaBesoinPsy());

        if (r.getCommentaire() != null) {
            pst.setString(6, r.getCommentaire());
        } else {
            pst.setNull(6, Types.LONGVARCHAR);
        }

        pst.setInt(7, r.getReponseQuestionnaireId());

        int rows = pst.executeUpdate();

        if (rows == 0) {
            throw new SQLException("Aucune réponse trouvée avec l'ID=" + r.getReponseQuestionnaireId());
        }

        System.out.println("Réponse modifiée avec succès !");
    }

    //  SUPPRIMER (Admin)

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM reponse_questionnaire WHERE reponse_questionnaire_id=?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, id);

        int rows = pst.executeUpdate();

        if (rows == 0) {
            throw new SQLException("Aucune réponse trouvée avec l'ID=" + id);
        }

        System.out.println("Réponse supprimée avec succès ! ID=" + id);
    }
}