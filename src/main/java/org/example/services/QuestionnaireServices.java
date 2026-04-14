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

    // ─────────────────────────────────────────
    //  AJOUTER
    // ─────────────────────────────────────────
    @Override
    public void ajouter(Questionnaire questionnaire) throws SQLException {

        String checkSql = "SELECT COUNT(*) FROM questionnaire WHERE code = ?";
        PreparedStatement checkPst = con.prepareStatement(checkSql);
        checkPst.setString(1, questionnaire.getCode());
        ResultSet rs = checkPst.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            throw new SQLException("Un questionnaire avec le code '" + questionnaire.getCode() + "' existe déjà !");
        }

        // APRÈS — valeurs depuis l'objet
        String sql = "INSERT INTO questionnaire (code, nom, description, type, created_at, " +
                "seuil_leger, seuil_modere, seuil_severe, nbre_questions) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, questionnaire.getCode());
        pst.setString(2, questionnaire.getNom());
        pst.setString(3, questionnaire.getDescription());
        pst.setString(4, questionnaire.getType().name());
        pst.setInt(5, questionnaire.getSeuilLegere());
        pst.setInt(6, questionnaire.getSeuilModere());
        pst.setInt(7, questionnaire.getSeuilSevere());
        pst.setInt(8, questionnaire.getNbreQuestions());

        pst.executeUpdate();
        System.out.println("Questionnaire ajouté avec succès !");
    }

    // ─────────────────────────────────────────
    //  AFFICHER
    // ─────────────────────────────────────────
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
            q.setCreatedAt(rs.getTimestamp("created_at"));
            q.setUpdatedAt(rs.getTimestamp("updated_at"));
            q.setSeuilLegere(rs.getInt("seuil_leger"));
            q.setSeuilModere(rs.getInt("seuil_modere"));
            q.setSeuilSevere(rs.getInt("seuil_severe"));
            q.setNbreQuestions(rs.getInt("nbre_questions"));
            q.setInterpretatLegere(rs.getString("interpretat_legere"));
            q.setInterpretatModere(rs.getString("interpretat_modere"));
            q.setInterpretatSevere(rs.getString("interpretat_severe"));

            // erreur
            String typeStr = rs.getString("type");
            try {
                q.setType(TypeQuestionnaire.valueOf(typeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println(" Type inconnu en DB : '" + typeStr + "' → ignoré");
                q.setType(null);
            }

            // adminId peut être NULL
            int adminId = rs.getInt("admin_id");
            q.setAdminId(rs.wasNull() ? null : adminId);

            questionnaires.add(q);
        }

        return questionnaires;
    }

    // ─────────────────────────────────────────
    //  MODIFIER
    // ─────────────────────────────────────────
    @Override
    public void modifier(Questionnaire questionnaire) throws SQLException {

        // bich intesti type null wala lee 9bal modification
        if (questionnaire.getType() == null) {
            throw new SQLException("Le type du questionnaire ne peut pas être null !");
        }

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

    // ─────────────────────────────────────────
    //  SUPPRIMER
    // ─────────────────────────────────────────
    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM questionnaire WHERE questionnaire_id=?";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, id);

        int rows = pst.executeUpdate();

        if (rows == 0) {
            throw new SQLException("Aucun questionnaire trouvé avec l'ID=" + id);
        }

        System.out.println("Questionnaire supprimé avec succès ! ID=" + id);
    }
}