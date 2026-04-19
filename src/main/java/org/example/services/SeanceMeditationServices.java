package org.example.services;

import org.example.entities.SeanceMeditation;
import org.example.enums.NiveauMeditation;
import org.example.enums.TypeFichier;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeanceMeditationServices implements ICrud<SeanceMeditation> {
    Connection con;

    public SeanceMeditationServices() {
        con = MyDataBase_Unimind.getInstance().getConnection();
    }

    @Override
    public void ajouter(SeanceMeditation seanceMeditation) throws SQLException {
        String sql = "INSERT INTO `seance_meditation` (`titre`, `description`, `fichier`, `type_fichier`, `duree`, `is_active`, `niveau`, `created_at`, `updated_at`, `categorie_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, seanceMeditation.getTitre());
        preparedStatement.setString(2, seanceMeditation.getDescription());
        preparedStatement.setString(3, seanceMeditation.getFichier());
        preparedStatement.setString(4, seanceMeditation.getTypeFichier().name()); // enum -> String
        preparedStatement.setInt(5, seanceMeditation.getDuree());
        preparedStatement.setBoolean(6, seanceMeditation.isIsActive());
        preparedStatement.setString(7, seanceMeditation.getNiveau().name()); // enum -> String
        Timestamp now = new Timestamp(System.currentTimeMillis());
        preparedStatement.setTimestamp(8, now); // created_at
        preparedStatement.setTimestamp(9, now); // updated_at
        preparedStatement.setInt(10, seanceMeditation.getCategorieId());
        preparedStatement.executeUpdate();
        System.out.println("Ajout de SeanceMeditation");
    }

    @Override
    public void modifier(SeanceMeditation seanceMeditation) throws SQLException {
        String sql = "UPDATE `seance_meditation` SET `titre` = ?, `description` = ?, `fichier` = ?, `type_fichier` = ?, `duree` = ?, `is_active` = ?, `niveau` = ?, `updated_at` = ?, `categorie_id` = ? " +
                "WHERE `seance_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, seanceMeditation.getTitre());
        preparedStatement.setString(2, seanceMeditation.getDescription());
        preparedStatement.setString(3, seanceMeditation.getFichier());
        preparedStatement.setString(4, seanceMeditation.getTypeFichier().name());
        preparedStatement.setInt(5, seanceMeditation.getDuree());
        preparedStatement.setBoolean(6, seanceMeditation.isIsActive());
        preparedStatement.setString(7, seanceMeditation.getNiveau().name());
        preparedStatement.setTimestamp(8, new Timestamp(System.currentTimeMillis())); // updated_at
        preparedStatement.setInt(9, seanceMeditation.getCategorieId());
        preparedStatement.setInt(10, seanceMeditation.getSeanceId());
        preparedStatement.executeUpdate();
        System.out.println("Modification de la séance ID " + seanceMeditation.getSeanceId());
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `seance_meditation` WHERE `seance_id` = ?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Suppression de la séance ID " + id);
    }

    @Override
    public List<SeanceMeditation> afficher() throws SQLException {
        List<SeanceMeditation> seances = new ArrayList<>();
        String sql = "SELECT * FROM `seance_meditation`";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            SeanceMeditation seance = new SeanceMeditation();
            seance.setSeanceId(rs.getInt("seance_id"));
            seance.setTitre(rs.getString("titre"));
            seance.setDescription(rs.getString("description"));
            seance.setFichier(rs.getString("fichier"));
            seance.setTypeFichier(TypeFichier.valueOf(rs.getString("type_fichier")));
            seance.setDuree(rs.getInt("duree"));
            seance.setIsActive(rs.getBoolean("is_active"));
            seance.setNiveau(NiveauMeditation.valueOf(rs.getString("niveau")));
            seance.setCreatedAt(rs.getTimestamp("created_at"));
            seance.setUpdatedAt(rs.getTimestamp("updated_at"));
            seance.setCategorieId(rs.getInt("categorie_id"));
            seances.add(seance);
        }
        return seances;
    }

    public Connection getCon() {
        return con;
    }
}