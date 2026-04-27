package org.example.services;

import org.example.entities.RendezVousDetail;
import org.example.entities.User;
import org.example.entities.Etudiant;
import org.example.utils.MyDataBase_Unimind;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RappelRendezVousService {

    private EmailService emailService;
    private EtudiantService etudiantService;  // ← Utiliser EtudiantService
    private Connection con;
    private ScheduledExecutorService scheduler;

    public RappelRendezVousService() {
        emailService = new EmailService();
        etudiantService = new EtudiantService();  // ← CHANGÉ
        con = MyDataBase_Unimind.getInstance().getConnection();
        demarrerScheduler();
    }

    private void demarrerScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                verifierEtEnvoyerRappels();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la vérification des rappels: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 60, TimeUnit.MINUTES);

        System.out.println("✅ Service de rappel de rendez-vous démarré");
    }

    public void arreterScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("✅ Service de rappel arrêté");
        }
    }

    public void verifierEtEnvoyerRappels() {
        System.out.println("🔍 Vérification des rendez-vous à venir...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cibleMin = now.plusHours(23).plusMinutes(30);
        LocalDateTime cibleMax = now.plusHours(24).plusMinutes(30);

        LocalDate dateMin = cibleMin.toLocalDate();
        LocalDate dateMax = cibleMax.toLocalDate();
        LocalTime heureMin = cibleMin.toLocalTime();
        LocalTime heureMax = cibleMax.toLocalTime();

        String sql = "SELECT rdv.rendez_vous_id, rdv.etudiant_id, rdv.psy_id, " +
                "dp.date_dispo, dp.heure_debut, dp.heure_fin, dp.type_consult, " +
                "dp.lieu, rdv.motif " +
                "FROM rendez_vous rdv " +
                "JOIN disponibilite_psy dp ON rdv.dispo_id = dp.dispo_id " +
                "WHERE rdv.statut = 'confirme' " +
                "AND rdv.rappel_envoye = FALSE " +
                "AND dp.date_dispo BETWEEN ? AND ? " +
                "AND dp.heure_debut BETWEEN ? AND ?";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setDate(1, Date.valueOf(dateMin));
            pst.setDate(2, Date.valueOf(dateMax));
            pst.setTime(3, Time.valueOf(heureMin));
            pst.setTime(4, Time.valueOf(heureMax));

            ResultSet rs = pst.executeQuery();
            int count = 0;

            while (rs.next()) {
                int rdvId = rs.getInt("rendez_vous_id");
                int etudiantId = rs.getInt("etudiant_id");

                String date = rs.getDate("date_dispo").toString();
                String heureDebut = rs.getTime("heure_debut").toString().substring(0, 5);
                String heureFin = rs.getTime("heure_fin").toString().substring(0, 5);
                String typeConsult = rs.getString("type_consult");
                String lieu = rs.getString("lieu");
                String motif = rs.getString("motif");

                boolean sent = envoyerRappelEtudiant(etudiantId, date, heureDebut, heureFin, typeConsult, lieu, motif);

                if (sent) {
                    marquerRappelEnvoye(rdvId);
                    count++;
                }
            }

            if (count > 0) {
                System.out.println("✅ " + count + " rappel(s) de rendez-vous envoyé(s)");
            } else {
                System.out.println("📭 Aucun rendez-vous à rappeler pour les prochaines 24h");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean envoyerRappelEtudiant(int etudiantId, String date, String heureDebut, String heureFin,
                                          String typeConsult, String lieu, String motif) {
        // ← CHANGÉ : utiliser etudiantService.getEtudiantById()
        Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);

        if (etudiant != null && etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
            String sujet = "🔔 Rappel : Votre rendez-vous dans 24 heures - Unimind";
            String corps = buildEmailRappel(
                    etudiant.getNom(),
                    etudiant.getPrenom(),
                    date,
                    heureDebut,
                    heureFin,
                    typeConsult,
                    lieu,
                    motif
            );

            emailService.envoyerEmailRappel(etudiant.getEmail(), sujet, corps);
            System.out.println("✅ Email de rappel envoyé à " + etudiant.getEmail());
            return true;
        }
        return false;
    }

    private String buildEmailRappel(String nom, String prenom, String date,
                                    String heureDebut, String heureFin,
                                    String typeConsult, String lieu, String motif) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f0f4ff;padding:20px;}" +
                ".card{max-width:550px;margin:0 auto;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(99,102,241,0.15);}" +
                ".header{text-align:center;margin-bottom:24px;}" +
                ".logo{font-size:28px;font-weight:bold;color:#f59e0b;}" +
                ".title{font-size:20px;font-weight:bold;color:#d97706;margin-bottom:16px;}" +
                ".info{background:#fef3c7;padding:16px;border-radius:12px;margin:16px 0;}" +
                ".info-item{margin:8px 0;}.label{font-weight:bold;color:#f59e0b;}" +
                ".footer{text-align:center;font-size:12px;color:#9ca3af;margin-top:24px;}" +
                "</style></head><body>" +
                "<div class='card'>" +
                "<div class='header'><div class='logo'>🔔 Unimind</div></div>" +
                "<div class='title'>Rappel de rendez-vous</div>" +
                "<p>Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
                "<p>Ce message est pour vous rappeler que vous avez un rendez-vous dans <strong style='color:#f59e0b;'>24 heures</strong>.</p>" +
                "<div class='info'>" +
                "<div class='info-item'><span class='label'>📅 Date :</span> " + date + "</div>" +
                "<div class='info-item'><span class='label'>⏰ Horaire :</span> " + heureDebut + " - " + heureFin + "</div>" +
                "<div class='info-item'><span class='label'>💬 Type :</span> " + typeConsult + "</div>" +
                (lieu != null && !lieu.isEmpty() ? "<div class='info-item'><span class='label'>📍 Lieu :</span> " + lieu + "</div>" : "") +
                "<div class='info-item'><span class='label'>📝 Motif :</span> " + (motif != null ? motif : "Non spécifié") + "</div>" +
                "</div>" +
                "<p>Merci de ne pas manquer ce rendez-vous important.</p>" +
                "<div class='footer'>" +
                "<p>Cet email est envoyé automatiquement, merci de ne pas y répondre.</p>" +
                "<p>© 2025 Unimind - Santé Mentale</p>" +
                "</div>" +
                "</div></body></html>";
    }

    private void marquerRappelEnvoye(int rendezVousId) {
        String sql = "UPDATE rendez_vous SET rappel_envoye = TRUE WHERE rendez_vous_id = ?";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, rendezVousId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour du rappel: " + e.getMessage());
        }
    }
}