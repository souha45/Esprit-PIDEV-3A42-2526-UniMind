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
import java.sql.SQLException;
import java.util.UUID;

public class EmailService {
    private MailjetClient client;
    private boolean sandboxMode = false; // ← CHANGÉ À FALSE pour envoyer de vrais emails

    public EmailService() {
        ClientOptions options = ClientOptions.builder()
                .apiKey(Config.MAILJET_API_KEY)
                .apiSecretKey(Config.MAILJET_SECRET_KEY)
                .build();
        client = new MailjetClient(options);
    }

    public void sendActivationEmail(String toEmail, String toName, int userId, String token) throws Exception {
        String activationLink = Config.BASE_URL + "/activate?userId=" + userId + "&token=" + token;

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
                                .put(Emailv31.Message.HTMLPART,
                                        "<h2>Bienvenue sur UniMind, " + toName + "!</h2>" +
                                                "<p>Merci de vous être inscrit. Pour activer votre compte, cliquez sur le bouton ci-dessous :</p>" +
                                                "<a href='" + activationLink + "' style='background-color: #4F46E5; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Activer mon compte</a>" +
                                                "<p><br>Une fois votre compte activé, un administrateur devra valider votre inscription.</p>" +
                                                "<p>Cordialement,<br>L'équipe UniMind</p>")));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200) {
            System.err.println("Erreur Mailjet: " + response.getData());
            throw new Exception("Erreur d'envoi d'email: " + response.getData());
        }
        System.out.println("✅ Email d'activation envoyé à " + toEmail);
    }

    public void sendResetPasswordEmail(String toEmail, String toName, String resetToken) throws Exception {
        // Le lien pointe vers votre serveur local qui ouvrira l'application
        String resetLink = "http://localhost:8080/reset-password?token=" + resetToken;

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
                                .put(Emailv31.Message.SUBJECT, "Réinitialisation de votre mot de passe UniMind")
                                .put(Emailv31.Message.HTMLPART,
                                        "<!DOCTYPE html>" +
                                                "<html>" +
                                                "<head><meta charset='UTF-8'></head>" +
                                                "<body style='font-family: Arial, sans-serif; background-color: #f4f4f7; padding: 40px;'>" +
                                                "<div style='max-width: 500px; margin: 0 auto; background: white; border-radius: 16px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>" +
                                                "<div style='text-align: center;'>" +
                                                "<h1 style='color: #4F46E5; margin-bottom: 20px;'>🔐 UniMind</h1>" +
                                                "<h2 style='color: #1E1B4B;'>Bonjour " + toName + ",</h2>" +
                                                "<p style='color: #4B5563; line-height: 1.5;'>Vous avez demandé à réinitialiser votre mot de passe.</p>" +
                                                "<p style='color: #4B5563; line-height: 1.5;'>Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe :</p>" +
                                                "<a href='" + resetLink + "' style='display: inline-block; background-color: #D97706; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0;'>Réinitialiser mon mot de passe</a>" +
                                                "<p style='color: #9CA3AF; font-size: 12px;'>Ce lien expire dans 24 heures.</p>" +
                                                "<hr style='margin: 20px 0; border: none; border-top: 1px solid #E5E7EB;'>" +
                                                "<p style='color: #9CA3AF; font-size: 12px;'>L'équipe UniMind</p>" +
                                                "</div>" +
                                                "</div>" +
                                                "</body>" +
                                                "</html>")));

        MailjetResponse response = client.post(request);
        System.out.println("📧 Mailjet status: " + response.getStatus());
        System.out.println("📧 Mailjet data: " + response.getData());

        if (response.getStatus() != 200) {
            throw new Exception("Mailjet erreur " + response.getStatus() + " : " + response.getData());
        }
        System.out.println("✅ Email envoyé à " + toEmail);
    }

    public void sendNewRegistrationToAdmin(String userName, String userEmail, String userRole) throws Exception {
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", Config.MAILJET_FROM_EMAIL)
                                        .put("Name", Config.MAILJET_FROM_NAME))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", Config.ADMIN_EMAIL)
                                                .put("Name", "Administrateur")))
                                .put(Emailv31.Message.SUBJECT, "Nouvelle inscription en attente de validation")
                                .put(Emailv31.Message.HTMLPART,
                                        "<h2>Nouvelle inscription sur UniMind</h2>" +
                                                "<p>Un nouvel utilisateur s'est inscrit :</p>" +
                                                "<ul>" +
                                                "<li><strong>Nom :</strong> " + userName + "</li>" +
                                                "<li><strong>Email :</strong> " + userEmail + "</li>" +
                                                "<li><strong>Rôle :</strong> " + userRole + "</li>" +
                                                "</ul>" +
                                                "<p>Connectez-vous à votre espace admin pour approuver ou refuser cette demande.</p>" +
                                                "<p>Cordialement,<br>L'équipe UniMind</p>")));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200) {
            System.err.println("Erreur Mailjet: " + response.getData());
            throw new Exception("Erreur d'envoi d'email: " + response.getData());
        }
        System.out.println("✅ Notification admin envoyée pour: " + userName);
    }

    public void sendAccountApprovedEmail(String toEmail, String toName) throws Exception {
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
                                .put(Emailv31.Message.SUBJECT, "Votre compte UniMind a été approuvé")
                                .put(Emailv31.Message.HTMLPART,
                                        "<h2>Félicitations " + toName + " !</h2>" +
                                                "<p>Votre compte UniMind a été <strong>approuvé</strong> par l'administrateur.</p>" +
                                                "<p>Vous pouvez maintenant vous connecter et accéder à tous les services.</p>" +
                                                "<p>Cordialement,<br>L'équipe UniMind</p>")));

        MailjetResponse response = client.post(request);
        if (response.getStatus() != 200) {
            System.err.println("Erreur Mailjet: " + response.getData());
            throw new Exception("Erreur d'envoi d'email: " + response.getData());
        }
        System.out.println("✅ Email d'approbation envoyé à " + toEmail);
    }

    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void storeResetToken(int userId, String token, Connection conn) throws SQLException {
        String query = "UPDATE user SET reset_token = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("✅ Token stocké en BDD pour user_id: " + userId);
        }
    }

    public boolean verifyResetToken(String token, Connection conn) throws SQLException {
        return token != null && !token.isEmpty();
    }

    public boolean activateAccount(int userId, String token, Connection conn) throws SQLException {
        String query = "UPDATE user SET is_verified=1 WHERE user_id=?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, userId);
        return ps.executeUpdate() > 0;
    }
}