package org.example.services;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Properties;

public class EmailService {

    private String apiKey;
    private String secretKey;
    private String fromEmail;
    private String fromName;

    public EmailService() {
        chargerConfiguration();
    }

    private void chargerConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("❌ Fichier config.properties non trouvé !");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            apiKey = prop.getProperty("mailjet.api.key");
            secretKey = prop.getProperty("mailjet.secret.key");
            fromEmail = prop.getProperty("mailjet.from.email");
            fromName = prop.getProperty("mailjet.from.name");
            System.out.println("✅ Configuration Mailjet chargée");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement config: " + e.getMessage());
        }
    }

    /**
     * Envoie un email au psychologue quand un étudiant prend un rendez-vous
     */
    public void envoyerEmailNouveauRdvAuPsy(String psyEmail, String psyNom, String psyPrenom,
                                            String etudiantNom, String etudiantPrenom, String etudiantEmail,
                                            String date, String heureDebut, String heureFin,
                                            String typeConsult, String lieu, String motif) {

        String sujet = "📅 Nouveau rendez-vous réservé - Unimind";

        String corps = buildEmailNouveauRdv(
                psyNom, psyPrenom, etudiantNom, etudiantPrenom, etudiantEmail,
                date, heureDebut, heureFin, typeConsult, lieu, motif
        );

        envoyerEmail(psyEmail, sujet, corps);
    }

    /**
     * Envoie un email de confirmation à l'étudiant
     */
    public void envoyerEmailConfirmationEtudiant(String etudiantEmail, String etudiantNom, String etudiantPrenom,
                                                 String date, String heureDebut, String heureFin,
                                                 String typeConsult, String lieu, String motif,
                                                 String psyNom, String psyPrenom) {

        String sujet = "✅ Votre rendez-vous a été confirmé - Unimind";

        String corps = buildEmailConfirmationEtudiant(
                etudiantNom, etudiantPrenom, date, heureDebut, heureFin,
                typeConsult, lieu, motif, psyNom, psyPrenom
        );

        envoyerEmail(etudiantEmail, sujet, corps);
    }

    private String buildEmailNouveauRdv(String psyNom, String psyPrenom,
                                        String etudiantNom, String etudiantPrenom, String etudiantEmail,
                                        String date, String heureDebut, String heureFin,
                                        String typeConsult, String lieu, String motif) {

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f0f4ff;padding:20px;}" +
                ".card{max-width:550px;margin:0 auto;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(99,102,241,0.15);}" +
                ".header{text-align:center;margin-bottom:24px;}" +
                ".logo{font-size:28px;font-weight:bold;color:#6366f1;}" +
                ".title{font-size:20px;font-weight:bold;color:#3730a3;margin-bottom:16px;}" +
                ".info{background:#f5f3ff;padding:16px;border-radius:12px;margin:16px 0;}" +
                ".info-item{margin:8px 0;}.label{font-weight:bold;color:#6366f1;}" +
                ".footer{text-align:center;font-size:12px;color:#9ca3af;margin-top:24px;}" +
                "</style></head><body>" +
                "<div class='card'><div class='header'><div class='logo'>🧠 Unimind</div></div>" +
                "<div class='title'>📅 Nouveau rendez-vous</div>" +
                "<p>Bonjour <strong>Dr. " + psyPrenom + " " + psyNom + "</strong>,</p>" +
                "<p>Un étudiant a réservé un créneau avec vous.</p>" +
                "<div class='info'>" +
                "<div class='info-item'><span class='label'>👤 Patient :</span> " + etudiantPrenom + " " + etudiantNom + "</div>" +
                "<div class='info-item'><span class='label'>📧 Email :</span> " + etudiantEmail + "</div>" +
                "<div class='info-item'><span class='label'>📅 Date :</span> " + date + "</div>" +
                "<div class='info-item'><span class='label'>⏰ Horaire :</span> " + heureDebut + " - " + heureFin + "</div>" +
                "<div class='info-item'><span class='label'>💬 Type :</span> " + typeConsult + "</div>" +
                (lieu != null && !lieu.isEmpty() ? "<div class='info-item'><span class='label'>📍 Lieu :</span> " + lieu + "</div>" : "") +
                "<div class='info-item'><span class='label'>📝 Motif :</span> " + (motif != null ? motif : "Non spécifié") + "</div>" +
                "</div><p>Connectez-vous à votre espace psychologue pour gérer ce rendez-vous.</p>" +
                "<div class='footer'><p>Cet email est envoyé automatiquement, merci de ne pas y répondre.</p>" +
                "<p>© 2025 Unimind - Santé Mentale</p></div></div></body></html>";
    }

    private String buildEmailConfirmationEtudiant(String etudiantNom, String etudiantPrenom,
                                                  String date, String heureDebut, String heureFin,
                                                  String typeConsult, String lieu, String motif,
                                                  String psyNom, String psyPrenom) {

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f0f4ff;padding:20px;}" +
                ".card{max-width:550px;margin:0 auto;background:white;border-radius:16px;padding:24px;}" +
                ".header{text-align:center;margin-bottom:24px;}" +
                ".logo{font-size:28px;font-weight:bold;color:#10b981;}" +
                ".title{font-size:20px;font-weight:bold;color:#065f46;margin-bottom:16px;}" +
                ".info{background:#ecfdf5;padding:16px;border-radius:12px;margin:16px 0;}" +
                ".info-item{margin:8px 0;}.label{font-weight:bold;color:#10b981;}" +
                ".footer{text-align:center;font-size:12px;color:#9ca3af;margin-top:24px;}" +
                "</style></head><body>" +
                "<div class='card'><div class='header'><div class='logo'>✅ Unimind</div></div>" +
                "<div class='title'>Rendez-vous confirmé</div>" +
                "<p>Bonjour <strong>" + etudiantPrenom + " " + etudiantNom + "</strong>,</p>" +
                "<p>Votre rendez-vous a été <strong style='color:#10b981;'>confirmé</strong>.</p>" +
                "<div class='info'>" +
                "<div class='info-item'><span class='label'>👨‍⚕️ Psychologue :</span> Dr. " + psyPrenom + " " + psyNom + "</div>" +
                "<div class='info-item'><span class='label'>📅 Date :</span> " + date + "</div>" +
                "<div class='info-item'><span class='label'>⏰ Horaire :</span> " + heureDebut + " - " + heureFin + "</div>" +
                "<div class='info-item'><span class='label'>💬 Type :</span> " + typeConsult + "</div>" +
                (lieu != null && !lieu.isEmpty() ? "<div class='info-item'><span class='label'>📍 Lieu :</span> " + lieu + "</div>" : "") +
                "<div class='info-item'><span class='label'>📝 Motif :</span> " + (motif != null ? motif : "Non spécifié") + "</div>" +
                "</div>" +
                "<p>Vous pouvez consulter vos rendez-vous dans votre espace étudiant.</p>" +
                "<div class='footer'><p>Cet email est envoyé automatiquement, merci de ne pas y répondre.</p>" +
                "<p>© 2025 Unimind - Santé Mentale</p></div></div></body></html>";
    }

    private void envoyerEmail(String destinataire, String sujet, String corpsHtml) {
        if (apiKey == null || secretKey == null) {
            System.err.println("❌ Mailjet non configuré");
            return;
        }

        try {
            ClientOptions options = ClientOptions.builder()
                    .apiKey(apiKey)
                    .apiSecretKey(secretKey)
                    .build();

            MailjetClient client = new MailjetClient(options);

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", fromEmail)
                                            .put("Name", fromName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", destinataire)
                                                    .put("Name", destinataire)))
                                    .put(Emailv31.Message.SUBJECT, sujet)
                                    .put(Emailv31.Message.HTMLPART, corpsHtml)));

            MailjetResponse response = client.post(request);

            if (response.getStatus() == 200) {
                System.out.println("✅ Email envoyé avec succès à " + destinataire);
            } else {
                System.err.println("❌ Erreur lors de l'envoi de l'email: " + response.getData());
            }

        } catch (MailjetException e) {
            System.err.println("❌ Exception Mailjet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}