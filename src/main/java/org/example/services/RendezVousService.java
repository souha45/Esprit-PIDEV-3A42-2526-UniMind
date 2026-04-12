package org.example.services;

import org.example.models.Consultation;
import org.example.models.DisponibilitePsy;
import org.example.models.RendezVous;
import org.example.models.RendezVousDetail;
import org.example.enums.StatutDisponibilite;
import org.example.enums.StatutRendezVous;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RendezVousService implements ICrud<RendezVous>{

    Connection con;
    private DisponibilitePsyService disponibiliteService;

    public RendezVousService() {
        con = MyDataBase_Unimind.getInstance().getConnection();
        disponibiliteService = new DisponibilitePsyService();
    }
    @Override
    public void ajouter(RendezVous rendezVous) throws SQLException {
        // ÉTAPE 1 : Récupérer la disponibilité choisie
        DisponibilitePsy dispoChoisie = disponibiliteService.getOne(rendezVous.getDispoId());

        // ÉTAPE 2 : Vérifier que la disponibilité existe
        if (dispoChoisie == null) {
            throw new SQLException(" Cette disponibilité n'existe pas !");
        }

        // ÉTAPE 3 : Vérifier que la disponibilité est DISPONIBLE (pas déjà réservée)
        if (dispoChoisie.getStatut() != StatutDisponibilite.disponible) {
            throw new SQLException(" Cette disponibilité n'est plus disponible ! Statut actuel : " + dispoChoisie.getStatut());
        }

        String sql ="INSERT INTO `rendez_vous`(`motif`, `statut`, `created_at`, `dispo_id`, `etudiant_id`, `psy_id`) VALUES ('"+rendezVous.getMotif()+"','"+rendezVous.getStatut()+"','"+rendezVous.getCreatedAt()+"',"+rendezVous.getDispoId()+","+rendezVous.getEtudiantId()+","+rendezVous.getPsyId()+")";

        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        System.out.println("RendezVous créé avec succés");

        // ÉTAPE 5 : Mettre à jour le statut de la disponibilité à "RESERVE"
        String updateDispo = "UPDATE disponibilite_psy SET statut = ?, updated_at = ? WHERE dispo_id = ?";
        PreparedStatement pstDispo = con.prepareStatement(updateDispo);
        pstDispo.setString(1, StatutDisponibilite.réservé.toString());
        pstDispo.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        pstDispo.setInt(3, rendezVous.getDispoId());
        pstDispo.executeUpdate();
        System.out.println("DisponibilitePsy mis en réservé avec succés");



    }

    @Override
    public void modifier(RendezVous rendezVous) throws SQLException {

    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    /**
     * Affiche la liste des rendez-vous pour un psychologue
     */
    public List<RendezVousDetail> afficherRendezVousDetailsByPsy(int psyId) throws SQLException {
        List<RendezVousDetail> rendezVousDetails = new ArrayList<>();

        String sql = "SELECT " +
                "  rdv.rendez_vous_id, " +
                "  rdv.statut, " +
                "  rdv.created_at, " +
                "  dp.date_dispo, " +
                "  dp.heure_debut, " +
                "  dp.heure_fin, " +
                "  dp.type_consult, " +
                "  u.nom as etudiant_nom, " +
                "  u.prenom as etudiant_prenom, " +
                "  u.email as etudiant_email, " +
                "  u.user_id as etudiant_id, " +
                "  dp.dispo_id " +
                "FROM rendez_vous rdv " +
                "INNER JOIN disponibilite_psy dp ON rdv.dispo_id = dp.dispo_id " +
                "INNER JOIN user u ON rdv.etudiant_id = u.user_id " +
                "WHERE dp.user_id = ? " +
                "ORDER BY dp.date_dispo DESC, dp.heure_debut ASC";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, psyId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            RendezVousDetail detail = new RendezVousDetail(
                    rs.getInt("rendez_vous_id"),
                    rs.getString("statut"),
                    rs.getTimestamp("created_at"),
                    rs.getDate("date_dispo"),
                    rs.getTime("heure_debut"),
                    rs.getTime("heure_fin"),
                    rs.getString("type_consult"),
                    rs.getString("etudiant_nom"),
                    rs.getString("etudiant_prenom"),
                    rs.getString("etudiant_email"),
                    rs.getInt("etudiant_id"),
                    rs.getInt("dispo_id")
            );
            rendezVousDetails.add(detail);
        }

        return rendezVousDetails;
    }

    @Override
    public List<RendezVous> afficher() throws SQLException {
        List<RendezVous> rdvs = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()){
            RendezVous rendezVous = new RendezVous();
            rendezVous.setDispoId(rs.getInt("dispo_id"));
            rendezVous.setPsyId(rs.getInt("psy_id"));
            rendezVous.setEtudiantId(rs.getInt("etudiant_id"));
            rendezVous.setMotif(rs.getString("motif"));
            rendezVous.setStatut(StatutRendezVous.valueOf(rs.getString("statut")));
            rendezVous.setRendezVousId(rs.getInt("rendez_vous_id"));
            rdvs.add(rendezVous);




        }

        return rdvs;
    }


    //Afficher la liste de rendezVous par étudiant connecté
    public List<RendezVousDetail> afficherRendezVousDetailsByEtudiant(int etudiantId) throws SQLException {
        List<RendezVousDetail> rendezVousDetails = new ArrayList<>();

        String sql = "SELECT " +
                "  rdv.rendez_vous_id, " +
                "  rdv.statut as statut_rdv, " +
                "  rdv.created_at, " +
                "  dp.date_dispo, " +
                "  dp.heure_debut, " +
                "  dp.heure_fin, " +
                "  dp.type_consult, " +
                "  u.nom as psy_nom, " +
                "  u.prenom as psy_prenom " +
                "FROM rendez_vous rdv " +
                "INNER JOIN disponibilite_psy dp ON rdv.dispo_id = dp.dispo_id " +
                "INNER JOIN user u ON dp.user_id = u.user_id " +
                "WHERE rdv.etudiant_id = ? " +
                "ORDER BY dp.date_dispo DESC, dp.heure_debut ASC";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, etudiantId);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            RendezVousDetail detail = new RendezVousDetail(
                    rs.getInt("rendez_vous_id"),
                    rs.getString("statut_rdv"),
                    rs.getTimestamp("created_at"),
                    rs.getDate("date_dispo"),
                    rs.getTime("heure_debut"),
                    rs.getTime("heure_fin"),
                    rs.getString("type_consult"),
                    rs.getString("psy_nom"),
                    rs.getString("psy_prenom")
            );
            rendezVousDetails.add(detail);
        }

        return rendezVousDetails;
    }

    //Afficher les détails d'un rdv
    public RendezVousDetail afficherRendezVousById(int etudiantId, int rendezVousId) throws SQLException {

        String sql = "SELECT " +
                "  rdv.rendez_vous_id, " +
                "  rdv.statut as statut_rdv, " +
                "  rdv.created_at, " +
                "rdv.motif, " +
                "  dp.date_dispo, " +
                "  dp.heure_debut, " +
                "  dp.heure_fin, " +
                "  dp.type_consult, " +
                "  u.nom as psy_nom, " +
                "  u.prenom as psy_prenom " +
                "FROM rendez_vous rdv " +
                "INNER JOIN disponibilite_psy dp ON rdv.dispo_id = dp.dispo_id " +
                "INNER JOIN user u ON dp.user_id = u.user_id " +
                "WHERE rdv.etudiant_id = ?  AND rendez_vous_id = ?";

        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, etudiantId);
        pst.setInt(2, rendezVousId);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return new RendezVousDetail(
                    rs.getInt("rendez_vous_id"),
                    rs.getString("statut_rdv"),
                    rs.getTimestamp("created_at"),
                    rs.getDate("date_dispo"),
                    rs.getTime("heure_debut"),
                    rs.getTime("heure_fin"),
                    rs.getString("type_consult"),
                    rs.getString("psy_nom"),
                    rs.getString("psy_prenom"),
                    rs.getString("motif")
            );

        }

        throw new SQLException(" Rendez-vous non trouvé ou vous n'avez pas l'autorisation d'y accéder.");
    }

    //Méthodes pour annuler un rdv accepter ou refuser un rdv
    /**
     * Modifier le statut d'un rendez-vous (générique)
     *
     * @param rendezVousId  L'ID du rendez-vous
     * @param etudiantId    L'ID de l'étudiant
     * @param nouveauStatut Le nouveau statut (Demande, CONFIRME, ANNULE, TERMINE)
     * @return
     * @throws SQLException Si le rendez-vous n'existe pas ou modification non autorisée
     */
    public void modifierStatutRendezVous(int rendezVousId, int etudiantId,int psyUserId, String nouveauStatut) throws SQLException {
        // ÉTAPE 1 : Vérifier que le rendez-vous existe et appartient à l'étudiant
        String checkSql = "SELECT statut, dispo_id FROM rendez_vous WHERE rendez_vous_id = ? AND etudiant_id = ?";
        PreparedStatement checkPst = con.prepareStatement(checkSql);
        checkPst.setInt(1, rendezVousId);
        checkPst.setInt(2, etudiantId);
        ResultSet rs = checkPst.executeQuery();

        if (!rs.next()) {
            throw new SQLException("❌ Rendez-vous non trouvé ou vous n'avez pas l'autorisation.");
        }

        String statutActuel = rs.getString("statut");
        int disponibiliteId = rs.getInt("dispo_id");

        // ÉTAPE 2 : Vérifier les transitions autorisées
        boolean transitionAutorisee = false;

        switch (statutActuel) {
            case "demande":
                // Depuis EN_ATTENTE, on peut passer à ANNULE ou CONFIRME
                if ("annulé".equals(nouveauStatut) || "confirme".equals(nouveauStatut)) {
                    transitionAutorisee = true;
                }
                break;
            case "confirme":
                // Depuis CONFIRME, on peut passer à TERMINE ou ANNULE (selon les règles)
                if ("en-cours".equals(nouveauStatut) || "absent".equals(nouveauStatut) || "annulé".equals(nouveauStatut)) {
                    transitionAutorisee = true;
                }
                break;
            case "en-cours":
                // Depuis CONFIRME, on peut passer à TERMINE ou ANNULE (selon les règles)
                if ("terminé".equals(nouveauStatut) || "annulé".equals(nouveauStatut)) {
                    transitionAutorisee = true;
                }
                break;
            case "annulé":
            case "terminé":
                // Les rendez-vous annulés ou terminés ne peuvent plus être modifiés
                throw new SQLException("❌ Impossible de modifier un rendez-vous " + statutActuel.toLowerCase());
            default:
                throw new SQLException("❌ Statut actuel non reconnu : " + statutActuel);
        }

        if (!transitionAutorisee) {
            throw new SQLException("❌ Transition non autorisée : " + statutActuel + " → " + nouveauStatut);
        }

        // ÉTAPE 3 : Mettre à jour le statut
        String updateSql = "UPDATE rendez_vous SET statut = ?, updated_at = ? WHERE rendez_vous_id = ?";
        PreparedStatement updatePst = con.prepareStatement(updateSql);
        updatePst.setString(1, nouveauStatut);
        updatePst.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        updatePst.setInt(3, rendezVousId);
        updatePst.executeUpdate();

        // ÉTAPE 4 : Si le rendez-vous est annulé, remettre la disponibilité à DISPONIBLE
        if ("annulé".equals(nouveauStatut)) {
            String updateDispo = "UPDATE disponibilite_psy SET statut = ?, updated_at = ? WHERE dispo_id = ?";
            PreparedStatement dispoPst = con.prepareStatement(updateDispo);
            dispoPst.setString(1, "disponible");
            dispoPst.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            dispoPst.setInt(3, disponibiliteId);
            dispoPst.executeUpdate();
            System.out.println("✓ La disponibilité est maintenant libre");
        }

        // ÉTAPE 5 : Si le rendez-vous passe de "encours" à "termine", créer une consultation
        if ("en-cours".equals(statutActuel) && "terminé".equals(nouveauStatut)) {
            Consultation consultation = new Consultation(rendezVousId, psyUserId, etudiantId);
            consultation.setAvisPsy("Consultation terminée");

            ConsultationService consultationService = new ConsultationService();
            consultationService.ajouter(consultation);

            System.out.println("✓ Consultation créée automatiquement (ID: " + consultation.getConsultationId() + ")");
        }

        System.out.println("\n✓ Statut du rendez-vous modifié avec succès !");
        System.out.println("  - ID du rendez-vous : " + rendezVousId);
        System.out.println("  - Ancien statut : " + statutActuel);
        System.out.println("  - Nouveau statut : " + nouveauStatut);
    }
}
