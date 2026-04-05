package org.example.services;

import org.example.entities.Consultation;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.sql.SQLException;
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

    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    @Override
    public List<Consultation> afficher() throws SQLException {
        return List.of();
    }
}
