package org.example.services;

import org.example.entities.DisponibilitePsy;
import org.example.enums.StatutDisponibilite;
import org.example.enums.TypeConsultation;
import org.example.utils.MyDataBase_Unimind;
import java.sql.Timestamp;

import java.sql.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DisponibilitePsyService  implements ICrud<DisponibilitePsy>{

    Connection con;

    //constructeur
    public DisponibilitePsyService(){
        con = MyDataBase_Unimind.getInstance().getConnection();
    }



    @Override
    public void ajouter(DisponibilitePsy disponibilitePsy) throws SQLException {
        String sql = "INSERT INTO `disponibilite_psy`(`date_dispo`, `heure_debut`, `heure_fin`, `type_consult`, `lieu`, `statut`, `created_at`, `user_id`) VALUES ('"+disponibilitePsy.getDateDispo()+"','"+disponibilitePsy.getHeureDebut()+"','"+disponibilitePsy.getHeureFin()+"','"+disponibilitePsy.getTypeConsult()+"','"+disponibilitePsy.getLieu()+"','"+disponibilitePsy.getStatut()+"','"+disponibilitePsy.getCreatedAt()+"',"+disponibilitePsy.getUserId()+")";
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        System.out.println("DisponibilitePsy ajouté avec succés");

    }

    @Override
    public void modifier(DisponibilitePsy disponibilitePsy) throws SQLException {
        String sql = "UPDATE `disponibilite_psy` SET `date_dispo`=?,`heure_debut`=?,`heure_fin`=?,`type_consult`=?,`lieu`=?,`statut`=?,`updated_at`=?,`user_id`=? WHERE dispo_id =?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setDate(1, disponibilitePsy.getDateDispo());
        preparedStatement.setTime(2, disponibilitePsy.getHeureDebut());
        preparedStatement.setTime(3, disponibilitePsy.getHeureFin());
        preparedStatement.setString(4, disponibilitePsy.getTypeConsult().toString());
        preparedStatement.setString(5, disponibilitePsy.getLieu());
        preparedStatement.setString(6, disponibilitePsy.getStatut().toString());
        preparedStatement.setTimestamp(7, disponibilitePsy.getUpdatedAt());
        preparedStatement.setInt(8, disponibilitePsy.getUserId());
        preparedStatement.setInt(9, disponibilitePsy.getDispoId());

        preparedStatement.executeUpdate();
        System.out.println("DisponibilitePsy modifiée avec succès");


    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `disponibilite_psy` WHERE dispo_id=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();
        System.out.println("DisponibilitePsy supprimé avec succés");

    }

    @Override
    public List<DisponibilitePsy> afficher() throws SQLException {
        List<DisponibilitePsy> disponibilites = new ArrayList<>();

        String sql = "SELECT * FROM disponibilite_psy";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()){
            DisponibilitePsy disponibilitePsy = new DisponibilitePsy();
            disponibilitePsy.setDispoId(rs.getInt("dispo_id"));
            disponibilitePsy.setDateDispo(rs.getDate("date_dispo"));
            disponibilitePsy.setHeureDebut(rs.getTime("heure_debut"));
            disponibilitePsy.setHeureFin(rs.getTime("heure_fin"));
            disponibilitePsy.setTypeConsult(TypeConsultation.valueOf(rs.getString("type_consult")));
            disponibilitePsy.setLieu(rs.getString("lieu"));
            disponibilitePsy.setStatut(StatutDisponibilite.valueOf(rs.getString("statut")));
            disponibilitePsy.setCreatedAt(rs.getTimestamp("created_at"));
            disponibilitePsy.setUpdatedAt(rs.getTimestamp("updated_at"));
            disponibilitePsy.setUserId(rs.getInt("user_id"));
            disponibilites.add(disponibilitePsy);




        }

        return disponibilites;
    }
}
