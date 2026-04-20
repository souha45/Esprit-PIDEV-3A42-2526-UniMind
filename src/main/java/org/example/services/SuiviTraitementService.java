package org.example.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.example.entities.SuiviTraitement;
import org.example.utils.MyDataBase_Unimind;

public class SuiviTraitementService implements ICrud<SuiviTraitement> {

    // Méthode pour vérifier l'unicité d'un suivi
    public boolean suiviExisteDeja(java.sql.Date dateSuivi, int traitementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM suivi_traitement WHERE dateSuivi = ? AND traitement_id = ?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, dateSuivi);
            ps.setInt(2, traitementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    @Override
    public void ajouter(SuiviTraitement s) throws SQLException {
        String sql = "INSERT INTO suivi_traitement (traitement_id, dateSuivi, dateSaisie, effectue, heurePrevue, heureEffective, observations, observationsPsy, evaluation, ressenti, saisiPar, valide, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getTraitementId());
            ps.setDate(2, s.getDateSuivi());
            ps.setTimestamp(3, s.getDateSaisie());
            ps.setBoolean(4, s.isEffectue());
            ps.setTime(5, s.getHeurePrevue());
            ps.setTime(6, s.getHeureEffective());
            ps.setString(7, s.getObservations());
            ps.setString(8, s.getObservationsPsy());
            if (s.getEvaluation() == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, s.getEvaluation());

            // CONVERSION AUTOMATIQUE : Majuscule → Minuscule pour la base
            ps.setString(10, s.getRessenti() != null ? s.getRessenti().name().toLowerCase() : null);

            // CONVERSION AUTOMATIQUE : Majuscule → Minuscule pour la base
            ps.setString(11, s.getSaisiPar() != null ? s.getSaisiPar().name().toLowerCase() : null);

            ps.setBoolean(12, s.isValide());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(SuiviTraitement s) throws SQLException {
        String sql = "UPDATE suivi_traitement SET traitement_id=?, dateSuivi=?, dateSaisie=?, effectue=?, heurePrevue=?, heureEffective=?, observations=?, observationsPsy=?, evaluation=?, ressenti=?, saisiPar=?, valide=?, updatedAt=NOW() WHERE suivitraitement_id=?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, s.getTraitementId());
            ps.setDate(2, s.getDateSuivi());
            ps.setTimestamp(3, s.getDateSaisie());
            ps.setBoolean(4, s.isEffectue());
            ps.setTime(5, s.getHeurePrevue());
            ps.setTime(6, s.getHeureEffective());
            ps.setString(7, s.getObservations());
            ps.setString(8, s.getObservationsPsy());
            if (s.getEvaluation() == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, s.getEvaluation());

            // CONVERSION AUTOMATIQUE : Majuscule → Minuscule pour la base
            ps.setString(10, s.getRessenti() != null ? s.getRessenti().name().toLowerCase() : null);

            // CONVERSION AUTOMATIQUE : Majuscule → Minuscule pour la base
            ps.setString(11, s.getSaisiPar() != null ? s.getSaisiPar().name().toLowerCase() : null);

            ps.setBoolean(12, s.isValide());
            ps.setInt(13, s.getSuivitraitementId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM suivi_traitement WHERE suivitraitement_id=?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime tous les suivis d'un traitement (utilisé par la suppression cascade)
     */
    public void supprimerParTraitementId(int traitementId) throws SQLException {
        String sql = "DELETE FROM suivi_traitement WHERE traitement_id=?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, traitementId);
            int deleted = ps.executeUpdate();
            System.out.println("✅ " + deleted + " suivi(s) supprimé(s) pour le traitement ID: " + traitementId);
        }
    }

    @Override
    public List<SuiviTraitement> afficher() throws SQLException {
        String sql = "SELECT * FROM suivi_traitement";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SuiviTraitement> result = new ArrayList<>();
            while (rs.next()) {
                SuiviTraitement s = extractSuiviFromResultSet(rs);
                result.add(s);
            }
            return result;
        }
    }

    /**
     * Récupère les suivis par traitement
     */
    public List<SuiviTraitement> getByTraitementId(int traitementId) throws SQLException {
        String sql = "SELECT * FROM suivi_traitement WHERE traitement_id=?";
        try (Connection cnx = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, traitementId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SuiviTraitement> result = new ArrayList<>();
                while (rs.next()) {
                    SuiviTraitement s = extractSuiviFromResultSet(rs);
                    result.add(s);
                }
                return result;
            }
        }
    }

    /**
     * Extrait un objet SuiviTraitement d'un ResultSet avec conversion automatique
     */
    private SuiviTraitement extractSuiviFromResultSet(ResultSet rs) throws SQLException {
        SuiviTraitement s = new SuiviTraitement();
        s.setSuivitraitementId(rs.getInt("suivitraitement_id"));
        s.setTraitementId(rs.getInt("traitement_id"));
        s.setDateSuivi(rs.getDate("dateSuivi"));
        s.setDateSaisie(rs.getTimestamp("dateSaisie"));
        s.setEffectue(rs.getBoolean("effectue"));
        s.setHeurePrevue(rs.getTime("heurePrevue"));
        s.setHeureEffective(rs.getTime("heureEffective"));
        s.setObservations(rs.getString("observations"));
        s.setObservationsPsy(rs.getString("observationsPsy"));
        s.setEvaluation(rs.getObject("evaluation") != null ? rs.getInt("evaluation") : null);
        s.setValide(rs.getBoolean("valide"));
        s.setCreatedAt(rs.getTimestamp("createdAt"));
        s.setUpdatedAt(rs.getTimestamp("updatedAt"));

        // LECTURE AUTOMATIQUE : Minuscule de la base → Majuscule pour JavaFX
        String ressentiStr = rs.getString("ressenti");
        if (ressentiStr != null) {
            try {
                s.setRessenti(org.example.enums.RessentiSuivi.valueOf(ressentiStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                s.setRessenti(org.example.enums.RessentiSuivi.NEUTRE);
            }
        } else {
            s.setRessenti(org.example.enums.RessentiSuivi.NEUTRE);
        }

        // LECTURE AUTOMATIQUE : Minuscule de la base → Majuscule pour JavaFX
        String saisiParStr = rs.getString("saisiPar");
        if (saisiParStr != null) {
            try {
                s.setSaisiPar(org.example.enums.SaisiPar.valueOf(saisiParStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                s.setSaisiPar(org.example.enums.SaisiPar.PSYCHOLOGUE);
            }
        } else {
            s.setSaisiPar(org.example.enums.SaisiPar.PSYCHOLOGUE);
        }

        return s;
    }
}