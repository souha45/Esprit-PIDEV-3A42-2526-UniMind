package org.example.services.evenement;

import org.example.enums.StatutEvenement;
import org.example.utils.MyDataBase_Unimind;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service planifié qui met à jour automatiquement les statuts des événements
 * selon leur date de début et de fin.
 *
 * Règles:
 * - A_VENIR → EN_COURS quand date_debut <= maintenant <= date_fin
 * - A_VENIR/EN_COURS → TERMINE quand maintenant > date_fin
 */
public class EventStatusSchedulerService {

    private final ScheduledExecutorService scheduler;
    private static final long INTERVAL_SECONDS = 60; // Vérifier toutes les 60 secondes

    public EventStatusSchedulerService() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EventStatusScheduler");
            t.setDaemon(true); // Le thread s'arrête quand l'app se ferme
            return t;
        });
    }

    /**
     * Démarre le scheduler
     */
    public void start() {
        System.out.println("[EventStatusScheduler] Démarrage du scheduler...");

        // Exécuter immédiatement une première fois
        updateEventStatuses();

        // Puis planifier l'exécution périodique
        scheduler.scheduleAtFixedRate(
                this::updateEventStatuses,
                INTERVAL_SECONDS,
                INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        System.out.println("[EventStatusScheduler] Scheduler démarré (intervalle: " + INTERVAL_SECONDS + "s)");
    }

    /**
     * Arrête le scheduler
     */
    public void stop() {
        System.out.println("[EventStatusScheduler] Arrêt du scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Met à jour les statuts des événements dans la base de données
     */
    private void updateEventStatuses() {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            // 1. Mettre à jour EN_COURS (événements dont la date de début est passée mais pas encore fini)
            int updatedToEnCours = updateToEnCours(now);

            // 2. Mettre à jour TERMINE (événements dont la date de fin est passée)
            int updatedToTermine = updateToTermine(now);

            if (updatedToEnCours > 0 || updatedToTermine > 0) {
                System.out.println("[EventStatusScheduler] Statuts mis à jour: " +
                        updatedToEnCours + " événement(s) EN_COURS, " +
                        updatedToTermine + " événement(s) TERMINE");
            }

        } catch (SQLException e) {
            System.err.println("[EventStatusScheduler] Erreur lors de la mise à jour des statuts: " + e.getMessage());
        }
    }

    /**
     * Met à jour le statut EN_COURS pour les événements qui ont commencé mais pas encore terminé
     */
    private int updateToEnCours(Timestamp now) throws SQLException {
        String sql = "UPDATE evenement SET statut = ? " +
                "WHERE statut IN ('a_venir', 'actif') " +
                "AND date_debut <= ? " +
                "AND date_fin >= ?";

        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, StatutEvenement.EN_COURS.getDbValue());
            ps.setTimestamp(2, now);
            ps.setTimestamp(3, now);

            return ps.executeUpdate();
        }
    }

    /**
     * Met à jour le statut TERMINE pour les événements dont la date de fin est passée
     */
    private int updateToTermine(Timestamp now) throws SQLException {
        String sql = "UPDATE evenement SET statut = ? " +
                "WHERE statut IN ('a_venir', 'actif', 'en_cours') " +
                "AND date_fin < ?";

        try (Connection conn = MyDataBase_Unimind.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, StatutEvenement.TERMINE.getDbValue());
            ps.setTimestamp(2, now);

            return ps.executeUpdate();
        }
    }
}
