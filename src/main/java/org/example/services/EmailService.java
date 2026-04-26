package org.example.services;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import org.example.config.Config;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EmailService {
    private MailjetClient client;

    public EmailService() {
        ClientOptions options = ClientOptions.builder()
                .apiKey(Config.MAILJET_API_KEY)
                .apiSecretKey(Config.MAILJET_SECRET_KEY)
                .build();
        client = new MailjetClient(options);
    }

    public void sendActivationEmail(String toEmail, String toName, int userId, String token) throws Exception {
        String activationLink = "http://localhost:8080/activate?userId=" + userId + "&token=" + token;
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", Config.MAILJET_FROM_EMAIL)
                                        .put("Name", Config.MAILJET_FROM_NAME))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", toEmail)
                                                .put("Name", toName)))
                                .put(Emailv31.Message.SUBJECT, "Activez votre compte UniMind")
                                .put(Emailv31.Message.HTMLPART, buildActivationHtml(toName, activationLink))));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200)
            throw new Exception("Erreur Mailjet: " + response.getData());
        System.out.println("✅ Email d'activation envoyé à " + toEmail);
    }

    // ── Email de bienvenue envoyé à l'utilisateur après inscription ──────
    public void sendWelcomeEmail(String toEmail, String toName, String role) throws Exception {
        String roleLabel = switch (role) {
            case "Etudiant"             -> "Étudiant";
            case "Psychologue"          -> "Psychologue";
            case "Responsable Etudiant" -> "Responsable Étudiant";
            default                     -> role;
        };

        String html =
                "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                        "<body style='margin:0;padding:0;background:#F0EFFF;font-family:Segoe UI,Arial,sans-serif;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 20px;'>" +
                        "<table width='520' cellpadding='0' cellspacing='0' style='background:white;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.1);'>" +

                        // Header
                        "<tr><td style='background:linear-gradient(135deg,#4F46E5,#7C3AED);padding:36px 40px;text-align:center;'>" +
                        "<div style='font-size:42px;margin-bottom:10px;'>🧠</div>" +
                        "<h1 style='color:white;margin:0;font-size:26px;font-weight:700;'>UniMind</h1>" +
                        "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:14px;'>Plateforme de santé mentale étudiante</p>" +
                        "</td></tr>" +

                        // Body
                        "<tr><td style='padding:40px;'>" +
                        "<h2 style='color:#1E1B4B;margin:0 0 12px;font-size:20px;'>Bienvenue, " + toName + " ! 🎉</h2>" +
                        "<p style='color:#4B5563;line-height:1.7;margin:0 0 20px;'>Votre inscription en tant que <strong style='color:#4F46E5;'>" + roleLabel + "</strong> a bien été enregistrée.</p>" +
                        "<p style='color:#4B5563;line-height:1.7;margin:0 0 24px;'>Votre compte est actuellement <strong>en attente de validation</strong> par un administrateur. Vous recevrez un email dès que votre accès sera approuvé.</p>" +

                        // Info box
                        "<div style='background:#EFF6FF;border-left:4px solid #4F46E5;border-radius:8px;padding:16px 20px;margin-bottom:28px;'>" +
                        "<p style='margin:0;color:#1D4ED8;font-size:13px;line-height:1.6;'>" +
                        "ℹ️ <strong>Que se passe-t-il ensuite ?</strong><br>" +
                        "1. L'administrateur examine votre demande<br>" +
                        "2. Vous recevez un email de confirmation<br>" +
                        "3. Vous pouvez vous connecter à UniMind" +
                        "</p></div>" +

                        "<p style='color:#6B7280;font-size:13px;line-height:1.6;margin:0;'>Si vous avez des questions, n'hésitez pas à nous contacter à <a href='mailto:" + Config.MAILJET_FROM_EMAIL + "' style='color:#7C3AED;'>" + Config.MAILJET_FROM_EMAIL + "</a></p>" +
                        "</td></tr>" +

                        // Footer
                        "<tr><td style='background:#F8F7FF;padding:20px 40px;text-align:center;border-top:1px solid #E5E7EB;'>" +
                        "<p style='color:#9CA3AF;font-size:12px;margin:0;'>© 2025 UniMind · Plateforme de santé mentale</p>" +
                        "</td></tr>" +
                        "</table></td></tr></table></body></html>";

        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", Config.MAILJET_FROM_EMAIL)
                                        .put("Name", Config.MAILJET_FROM_NAME))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", toEmail)
                                                .put("Name", toName)))
                                .put(Emailv31.Message.SUBJECT, "✅ Inscription UniMind enregistrée !")
                                .put(Emailv31.Message.HTMLPART, html)));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200)
            throw new Exception("Mailjet erreur: " + response.getData());
        System.out.println("✅ Email de bienvenue envoyé à " + toEmail);
    }

    // ── Email admin redesigné ─────────────────────────────────────────────
    public void sendNewRegistrationToAdmin(String userName, String userEmail, String userRole) throws Exception {
        String roleLabel = switch (userRole) {
            case "Etudiant"             -> "🎓 Étudiant";
            case "Psychologue"          -> "💚 Psychologue";
            case "Responsable Etudiant" -> "👤 Responsable Étudiant";
            default                     -> userRole;
        };

        String roleBadgeColor = switch (userRole) {
            case "Etudiant"             -> "#4F46E5";
            case "Psychologue"          -> "#059669";
            case "Responsable Etudiant" -> "#EA580C";
            default                     -> "#6B7280";
        };

        String html =
                "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                        "<body style='margin:0;padding:0;background:#F0EFFF;font-family:Segoe UI,Arial,sans-serif;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 20px;'>" +
                        "<table width='540' cellpadding='0' cellspacing='0' style='background:white;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.12);'>" +

                        // Header
                        "<tr><td style='background:linear-gradient(135deg,#1E1B4B,#4F46E5);padding:32px 40px;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
                        "<td><div style='font-size:36px;'>🧠</div></td>" +
                        "<td style='padding-left:16px;'>" +
                        "<h1 style='color:white;margin:0;font-size:22px;font-weight:700;'>UniMind</h1>" +
                        "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:13px;'>Espace Administrateur</p>" +
                        "</td>" +
                        "<td align='right'><span style='background:rgba(255,255,255,0.15);color:white;padding:6px 14px;border-radius:20px;font-size:12px;font-weight:600;'>🔔 Nouvelle inscription</span></td>" +
                        "</tr></table>" +
                        "</td></tr>" +

                        // Alert banner
                        "<tr><td style='background:#FEF3C7;padding:14px 40px;border-bottom:1px solid #FDE68A;'>" +
                        "<p style='margin:0;color:#92400E;font-size:13px;font-weight:600;'>⚠️ Une nouvelle demande d'inscription attend votre validation.</p>" +
                        "</td></tr>" +

                        // Body
                        "<tr><td style='padding:36px 40px;'>" +
                        "<h2 style='color:#1E1B4B;margin:0 0 24px;font-size:18px;'>Informations du candidat</h2>" +

                        // Info card
                        "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F8F7FF;border-radius:12px;overflow:hidden;border:1px solid #E5E7EB;'>" +
                        "<tr><td style='padding:18px 24px;border-bottom:1px solid #E5E7EB;'>" +
                        "<span style='font-size:11px;color:#9CA3AF;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;'>Nom complet</span><br>" +
                        "<span style='font-size:16px;color:#1E1B4B;font-weight:700;margin-top:4px;display:block;'>" + userName + "</span>" +
                        "</td></tr>" +
                        "<tr><td style='padding:18px 24px;border-bottom:1px solid #E5E7EB;'>" +
                        "<span style='font-size:11px;color:#9CA3AF;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;'>Adresse email</span><br>" +
                        "<a href='mailto:" + userEmail + "' style='font-size:15px;color:#4F46E5;text-decoration:none;font-weight:600;margin-top:4px;display:block;'>" + userEmail + "</a>" +
                        "</td></tr>" +
                        "<tr><td style='padding:18px 24px;'>" +
                        "<span style='font-size:11px;color:#9CA3AF;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;'>Rôle demandé</span><br>" +
                        "<span style='display:inline-block;margin-top:6px;background:" + roleBadgeColor + ";color:white;padding:4px 14px;border-radius:20px;font-size:13px;font-weight:600;'>" + roleLabel + "</span>" +
                        "</td></tr>" +
                        "</table>" +

                        "<div style='margin:28px 0;text-align:center;'>" +
                        "<a href='http://localhost:8080' style='display:inline-block;background:linear-gradient(to right,#4F46E5,#7C3AED);color:white;padding:13px 32px;text-decoration:none;border-radius:10px;font-size:15px;font-weight:700;'>🔐 Accéder à l'espace admin</a>" +
                        "</div>" +

                        "<p style='color:#9CA3AF;font-size:12px;line-height:1.6;margin:0;text-align:center;'>Cet email a été généré automatiquement. Ne pas répondre directement.</p>" +
                        "</td></tr>" +

                        // Footer
                        "<tr><td style='background:#1E1B4B;padding:18px 40px;text-align:center;'>" +
                        "<p style='color:rgba(255,255,255,0.5);font-size:12px;margin:0;'>© 2025 UniMind · Tous droits réservés</p>" +
                        "</td></tr>" +
                        "</table></td></tr></table></body></html>";

        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", Config.MAILJET_FROM_EMAIL)
                                        .put("Name", Config.MAILJET_FROM_NAME))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", Config.ADMIN_EMAIL)
                                                .put("Name", "Administrateur UniMind")))
                                .put(Emailv31.Message.SUBJECT, "🔔 Nouvelle inscription UniMind — " + userName)
                                .put(Emailv31.Message.HTMLPART, html)));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200)
            throw new Exception("Mailjet erreur: " + response.getData());
        System.out.println("✅ Notification admin envoyée pour: " + userName);
    }

    public void sendResetPasswordEmail(String toEmail, String toName, String resetToken) throws Exception {
        String resetLink = "http://localhost:8080/reset-password?token=" + resetToken;

        String html =
                "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                        "<body style='margin:0;padding:0;background:#F0EFFF;font-family:Segoe UI,Arial,sans-serif;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 20px;'>" +
                        "<table width='500' cellpadding='0' cellspacing='0' style='background:white;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.1);'>" +
                        "<tr><td style='background:linear-gradient(135deg,#D97706,#F59E0B);padding:32px 40px;text-align:center;'>" +
                        "<div style='font-size:40px;margin-bottom:8px;'>🔐</div>" +
                        "<h1 style='color:white;margin:0;font-size:22px;'>Réinitialisation du mot de passe</h1>" +
                        "</td></tr>" +
                        "<tr><td style='padding:36px 40px;'>" +
                        "<p style='color:#1E1B4B;font-size:16px;font-weight:600;margin:0 0 12px;'>Bonjour " + toName + ",</p>" +
                        "<p style='color:#4B5563;line-height:1.7;margin:0 0 24px;'>Vous avez demandé à réinitialiser votre mot de passe UniMind. Cliquez sur le bouton ci-dessous pour en choisir un nouveau :</p>" +
                        "<div style='text-align:center;margin:28px 0;'>" +
                        "<a href='" + resetLink + "' style='display:inline-block;background:linear-gradient(to right,#D97706,#F59E0B);color:white;padding:13px 32px;text-decoration:none;border-radius:10px;font-size:15px;font-weight:700;'>Réinitialiser mon mot de passe</a>" +
                        "</div>" +
                        "<p style='color:#9CA3AF;font-size:12px;text-align:center;margin:0;'>Ce lien expire dans 24 heures. Si vous n'avez pas fait cette demande, ignorez cet email.</p>" +
                        "</td></tr>" +
                        "<tr><td style='background:#F8F7FF;padding:18px 40px;text-align:center;border-top:1px solid #E5E7EB;'>" +
                        "<p style='color:#9CA3AF;font-size:12px;margin:0;'>© 2025 UniMind · Plateforme de santé mentale</p>" +
                        "</td></tr>" +
                        "</table></td></tr></table></body></html>";

        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", Config.MAILJET_FROM_EMAIL)
                                        .put("Name", Config.MAILJET_FROM_NAME))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", toEmail)
                                                .put("Name", toName)))
                                .put(Emailv31.Message.SUBJECT, "🔐 Réinitialisation de votre mot de passe UniMind")
                                .put(Emailv31.Message.HTMLPART, html)));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200)
            throw new Exception("Mailjet erreur: " + response.getData());
        System.out.println("✅ Email reset envoyé à " + toEmail);
    }

    public void sendAccountApprovedEmail(String toEmail, String toName) throws Exception {
        String html =
                "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                        "<body style='margin:0;padding:0;background:#F0EFFF;font-family:Segoe UI,Arial,sans-serif;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 20px;'>" +
                        "<table width='500' cellpadding='0' cellspacing='0' style='background:white;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.1);'>" +
                        "<tr><td style='background:linear-gradient(135deg,#059669,#10B981);padding:32px 40px;text-align:center;'>" +
                        "<div style='font-size:40px;margin-bottom:8px;'>✅</div>" +
                        "<h1 style='color:white;margin:0;font-size:22px;'>Compte approuvé !</h1>" +
                        "</td></tr>" +
                        "<tr><td style='padding:36px 40px;text-align:center;'>" +
                        "<h2 style='color:#1E1B4B;margin:0 0 12px;'>Félicitations, " + toName + " !</h2>" +
                        "<p style='color:#4B5563;line-height:1.7;margin:0 0 28px;'>Votre compte UniMind a été <strong>approuvé</strong> par l'administrateur. Vous pouvez maintenant vous connecter et accéder à tous les services.</p>" +
                        "<a href='http://localhost:8080' style='display:inline-block;background:linear-gradient(to right,#059669,#10B981);color:white;padding:13px 32px;text-decoration:none;border-radius:10px;font-size:15px;font-weight:700;'>Se connecter à UniMind</a>" +
                        "</td></tr>" +
                        "<tr><td style='background:#F8F7FF;padding:18px 40px;text-align:center;border-top:1px solid #E5E7EB;'>" +
                        "<p style='color:#9CA3AF;font-size:12px;margin:0;'>© 2025 UniMind · Plateforme de santé mentale</p>" +
                        "</td></tr>" +
                        "</table></td></tr></table></body></html>";

        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", Config.MAILJET_FROM_EMAIL)
                                        .put("Name", Config.MAILJET_FROM_NAME))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", toEmail)
                                                .put("Name", toName)))
                                .put(Emailv31.Message.SUBJECT, "✅ Votre compte UniMind a été approuvé !")
                                .put(Emailv31.Message.HTMLPART, html)));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200)
            throw new Exception("Mailjet erreur: " + response.getData());
        System.out.println("✅ Email d'approbation envoyé à " + toEmail);
    }

    private String buildActivationHtml(String toName, String activationLink) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background:#F0EFFF;font-family:Segoe UI,Arial,sans-serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 20px;'>" +
                "<table width='500' cellpadding='0' cellspacing='0' style='background:white;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background:linear-gradient(135deg,#4F46E5,#7C3AED);padding:32px 40px;text-align:center;'>" +
                "<div style='font-size:40px;margin-bottom:8px;'>🧠</div>" +
                "<h1 style='color:white;margin:0;font-size:22px;'>Activez votre compte</h1>" +
                "</td></tr>" +
                "<tr><td style='padding:36px 40px;text-align:center;'>" +
                "<h2 style='color:#1E1B4B;margin:0 0 12px;'>Bienvenue, " + toName + " !</h2>" +
                "<p style='color:#4B5563;line-height:1.7;margin:0 0 28px;'>Cliquez sur le bouton ci-dessous pour activer votre compte UniMind.</p>" +
                "<a href='" + activationLink + "' style='display:inline-block;background:linear-gradient(to right,#4F46E5,#7C3AED);color:white;padding:13px 32px;text-decoration:none;border-radius:10px;font-size:15px;font-weight:700;'>Activer mon compte</a>" +
                "</td></tr>" +
                "<tr><td style='background:#F8F7FF;padding:18px 40px;text-align:center;border-top:1px solid #E5E7EB;'>" +
                "<p style='color:#9CA3AF;font-size:12px;margin:0;'>© 2025 UniMind</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }

    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void storeResetToken(int userId, String token, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE user SET reset_token = ? WHERE user_id = ?")) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("✅ Token stocké pour user_id: " + userId);
        }
    }

    public boolean verifyResetToken(String token, Connection conn) throws SQLException {
        return token != null && !token.isEmpty();
    }

    public boolean activateAccount(int userId, String token, Connection conn) throws SQLException {
        // Vérifier si le token correspond et n'est pas expiré
        String checkQuery = "SELECT user_id FROM user WHERE user_id = ? AND verification_token = ? AND is_verified = 0";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            checkStmt.setInt(1, userId);
            checkStmt.setString(2, token);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // Token invalide ou compte déjà vérifié
                }
            }
        }

        // Activer le compte et effacer le token
        String updateQuery = "UPDATE user SET is_verified = 1, verification_token = NULL WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
}