package org.example.services;



import java.sql.Connection;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.util.ArrayList;

import java.util.List;



import org.example.entities.Etudiant;

import org.example.utils.MyDataBase_Unimind;



/**

 * Service pour la gestion des étudiants dans le module Traitements

 * Créé par VOUS - Ne modifie pas le service existant du collègue

 */

public class EtudiantTraitementService {



    private final Connection connection;



    public EtudiantTraitementService() throws SQLException {

        this.connection = MyDataBase_Unimind.getInstance().getConnection();

    }



    public List<Etudiant> afficher() throws SQLException {

        String sql = "SELECT user_id, nom, prenom, email FROM user WHERE role = 'Etudiant'";

        try (PreparedStatement ps = connection.prepareStatement(sql);

             ResultSet rs = ps.executeQuery()) {



            List<Etudiant> result = new ArrayList<>();

            while (rs.next()) {

                Etudiant etudiant = new Etudiant();

                etudiant.setUserId(rs.getInt("user_id"));

                etudiant.setNom(rs.getString("nom"));

                etudiant.setPrenom(rs.getString("prenom"));

                etudiant.setEmail(rs.getString("email"));

                result.add(etudiant);

            }

            return result;

        }

    }



    public Etudiant trouverParId(int id) throws SQLException {

        String sql = "SELECT user_id, nom, prenom, email FROM user WHERE user_id = ? AND role = 'Etudiant'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Etudiant etudiant = new Etudiant();

                    etudiant.setUserId(rs.getInt("user_id"));

                    etudiant.setNom(rs.getString("nom"));

                    etudiant.setPrenom(rs.getString("prenom"));

                    etudiant.setEmail(rs.getString("email"));

                    return etudiant;

                }

            }

        }

        return null;

    }

}