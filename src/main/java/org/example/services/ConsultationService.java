package org.example.services;

import org.example.entities.Consultation;
import org.example.entities.ConsultationDetail;
import org.example.entities.DisponibilitePsy;
import org.example.enums.StatutDisponibilite;
import org.example.enums.TypeConsultation;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService implements ICrud<Consultation>{

    Connection con;

    public ConsultationService() {
        // S'assurer que la connexion est bien initialisée
        con = MyDataBase_Unimind.getInstance().getConnection();

        // Vérifier que la connexion n'est pas null
        if (con == null) {
            System.out.println("❌ Erreur: La connexion à la base de données est null dans ConsultationService");
        } else {
            System.out.println("✓ ConsultationService: Connexion établie");
        }
    }
    @Override
    public void ajouter(Consultation consultation) throws SQLException {
        String sql = "INSERT INTO consultation (date_redaction, avis_psy, rendez_vous_id, psy_user_id, etudiant_user_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setTimestamp(1,consultation.getDateRedaction());
        pst.setString(2,"Consultation terminée le" + new Timestamp(System.currentTimeMillis()));
        pst.setInt(3,consultation.getRendezVousId());
        pst.setInt(4,consultation.getPsyUserId());
        pst.setInt(5,consultation.getEtudiantUserId());
        pst.executeUpdate();
        System.out.println("✓ Consultation ajoutée avec succès !");

    }

    @Override
    public void modifier(Consultation consultation) throws SQLException {
        // Vérifier que l'objet consultation n'est pas null
        if (consultation == null) {
            throw new SQLException("❌ La consultation ne peut pas être null.");
        }

        // Vérifier que l'ID est valide
        if (consultation.getConsultationId() <= 0) {
            throw new SQLException("❌ ID de consultation invalide.");
        }

        // Vérifier que la note est valide (si elle est fournie)
        if (consultation.getNoteSatisfaction() > 0) {
            if (consultation.getNoteSatisfaction() < 1 || consultation.getNoteSatisfaction() > 5) {
                throw new SQLException("❌ La note de satisfaction doit être comprise entre 1 et 5.");
            }
        }

        // Requête SQL avec mise à jour de l'avis, la note et la date de modification
        String sql = "UPDATE consultation SET avis_psy = ?, note_satisfaction = ?, date_modification = ? WHERE consultation_id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, consultation.getAvisPsy());
        pst.setShort(2, consultation.getNoteSatisfaction());
        pst.setTimestamp(3, new Timestamp(System.currentTimeMillis())); // updated_at = maintenant
        pst.setInt(4, consultation.getConsultationId());

        int rowsAffected = pst.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("\n✓ Consultation modifiée avec succès !");
            System.out.println("  - ID consultation : " + consultation.getConsultationId());
            System.out.println("  - Nouvel avis : " + (consultation.getAvisPsy() != null ? consultation.getAvisPsy() : "Non renseigné"));
            System.out.println("  - Nouvelle note : " + (consultation.getNoteSatisfaction() > 0 ? consultation.getNoteSatisfaction() + "/5" : "Non renseignée"));
            System.out.println("  - Date de modification : " + new Timestamp(System.currentTimeMillis()));
        } else {
            throw new SQLException(" Consultation non trouvée avec l'ID: " + consultation.getConsultationId());
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    @Override
    public List<Consultation> afficher() throws SQLException {
        List<Consultation> consultations = new ArrayList<>();

        String sql = "SELECT * FROM consultation";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()){
            Consultation consultation = new Consultation();
            consultation.setConsultationId(rs.getInt("consultation_id"));
            consultation.setAvisPsy(rs.getString("avis_psy"));
            consultation.setNoteSatisfaction(rs.getShort("note_satisfaction"));
            consultation.setRendezVousId(rs.getInt("rendez_vous_id"));
            consultation.setPsyUserId(rs.getInt("psy_user_id"));
            consultation.setEtudiantUserId(rs.getInt("etudiant_user_id"));
            consultation.setDateRedaction(rs.getTimestamp("date_redaction"));
            consultation.setDateModification(rs.getTimestamp("date_modification"));


            consultations.add(consultation);




        }

        return consultations;
    }


    //Méthode pour afficher la liste de consultation pour un étudiant
    public List<ConsultationDetail> getConsultationsDetailByEtudiant(int etudiantId) throws SQLException {
        List<ConsultationDetail> consultations = new ArrayList<>();

        String sql = "SELECT " +
                "  c.consultation_id, " +
                "  c.avis_psy, " +
                "  c.note_satisfaction, " +
                "  c.date_redaction, " +
                "  c.date_modification, " +
                "  u.nom as psy_nom, " +
                "  u.prenom as psy_prenom, " +
                "  u.email as psy_email, " +
                "  dp.date_dispo, " +
                "  dp.heure_debut, " +
                "  dp.heure_fin " +
                "FROM consultation c " +
                "INNER JOIN user u ON c.psy_user_id = u.user_id " +
                "INNER JOIN rendez_vous rdv ON c.rendez_vous_id = rdv.rendez_vous_id " +
                "INNER JOIN disponibilite_psy dp ON rdv.dispo_id = dp.dispo_id " +
                "WHERE c.etudiant_user_id = ? " +
                "ORDER BY c.date_redaction DESC";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, etudiantId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            ConsultationDetail detail = new ConsultationDetail(
                    rs.getInt("consultation_id"),
                    rs.getString("avis_psy"),
                    rs.getShort("note_satisfaction"),
                    rs.getTimestamp("date_redaction"),
                    rs.getTimestamp("date_modification"),
                    rs.getString("psy_nom"),
                    rs.getString("psy_prenom"),
                    rs.getString("psy_email"),
                    rs.getDate("date_dispo"),
                    rs.getTime("heure_debut"),
                    rs.getTime("heure_fin")
            );
            consultations.add(detail);
        }
        return consultations;
    }

    //Méthode pour afficher la liste de consultation pour un psychologue
    public List<ConsultationDetail> getConsultationsDetailByPsy(int psyId) throws SQLException {
        List<ConsultationDetail> consultations = new ArrayList<>();

        String sql = "SELECT " +
                "  c.consultation_id, " +
                "  c.avis_psy, " +
                "  c.note_satisfaction, " +
                "  c.date_redaction, " +
                "  c.date_modification, " +
                "  u.nom as etudiant_nom, " +
                "  u.prenom as etudiant_prenom, " +
                "  u.email as etudiant_email, " +
                "  dp.date_dispo, " +
                "  dp.heure_debut, " +
                "  dp.heure_fin " +
                "FROM consultation c " +
                "INNER JOIN user u ON c.etudiant_user_id = u.user_id " +
                "INNER JOIN rendez_vous rdv ON c.rendez_vous_id = rdv.rendez_vous_id " +
                "INNER JOIN disponibilite_psy dp ON rdv.dispo_id = dp.dispo_id " +
                "WHERE c.psy_user_id = ? " +
                "ORDER BY c.date_redaction DESC";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, psyId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            ConsultationDetail detail = new ConsultationDetail(
                    rs.getInt("consultation_id"),
                    rs.getString("avis_psy"),
                    rs.getShort("note_satisfaction"),
                    rs.getTimestamp("date_redaction"),
                    rs.getTimestamp("date_modification"),
                    rs.getString("etudiant_nom"),
                    rs.getString("etudiant_prenom"),
                    rs.getString("etudiant_email"),
                    rs.getDate("date_dispo"),
                    rs.getTime("heure_debut"),
                    rs.getTime("heure_fin")
            );
            consultations.add(detail);
        }

        return consultations;
    }
}
