package org.example.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.InputStream;
import java.util.Properties;

public class EmailServiceQuestionnaire {

    private static String EMAIL_FROM;
    private static String EMAIL_PASSWORD;

    static {
        try {
            Properties config = new Properties();
            InputStream is = EmailServiceQuestionnaire.class
                    .getResourceAsStream("/email.properties");
            if (is != null) {
                config.load(is);
                EMAIL_FROM     = config.getProperty("email.from");
                EMAIL_PASSWORD = config.getProperty("email.password");
                System.out.println("✅ Email config chargée : " + EMAIL_FROM);
            } else {
                System.out.println("❌ email.properties introuvable");
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur chargement email config : " + e.getMessage());
        }
    }

    public static void envoyerResultat(String emailDestinataire,
                                       String nomQuestionnaire,
                                       double score,
                                       String niveau,
                                       String interpretation) {
        if (EMAIL_FROM == null || EMAIL_PASSWORD == null) {
            System.out.println("❌ Config email manquante !");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(emailDestinataire));
            message.setSubject("✅ Résultat de votre questionnaire — " + nomQuestionnaire);

            String contenu = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white;
                                border-radius: 12px; padding: 30px;
                                border: 1px solid #e2e8f0;">
                        <h2 style="color: #7c3aed;">🧠 UniMind — Résultat du Questionnaire</h2>
                        <hr style="border-color: #e2e8f0;"/>
                        <p style="color: #64748b;">Bonjour,</p>
                        <p style="color: #64748b;">
                            Vous avez complété le questionnaire
                            <strong style="color: #1e293b;">%s</strong>.
                        </p>
                        <div style="background: #f5f3ff; border-radius: 10px; padding: 20px; margin: 20px 0;">
                            <p>🎯 Score : <strong style="color: #7c3aed; font-size: 20px;">%s</strong></p>
                            <p>📊 Niveau : <strong style="color: %s; font-size: 16px;">%s</strong></p>
                        </div>
                        <div style="background: #f8fafc; border-radius: 10px; padding: 16px;
                                    border-left: 4px solid #7c3aed;">
                            <strong>📝 Interprétation :</strong><br/>%s
                        </div>
                        <div style="background: #ecfdf5; border-radius: 10px; padding: 16px; margin-top: 16px;
                                    border-left: 4px solid #10b981;">
                            <strong>💡 Conseil :</strong><br/>%s
                        </div>
                        <hr style="border-color: #e2e8f0; margin-top: 20px;"/>
                        <p style="color: #94a3b8; font-size: 12px; text-align: center;">
                            Email envoyé automatiquement par UniMind.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                    nomQuestionnaire, score,
                    getCouleurNiveau(niveau),
                    niveau != null ? niveau.toUpperCase() : "INCONNU",
                    interpretation != null ? interpretation : "",
                    getConseil(niveau)
            );

            message.setContent(contenu, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé à : " + emailDestinataire);

        } catch (MessagingException e) {
            System.out.println("❌ Erreur envoi email : " + e.getMessage());
        }
    }

    private static String getCouleurNiveau(String niveau) {
        if (niveau == null) return "#64748b";
        return switch (niveau.toLowerCase()) {
            case "faible"           -> "#22c55e";
            case "modere", "modéré" -> "#f59e0b";
            case "eleve", "élevé"   -> "#ef4444";
            case "severe", "sévère" -> "#7c3aed";
            default                 -> "#64748b";
        };
    }

    private static String getConseil(String niveau) {
        if (niveau == null) return "Consultez votre responsable pour plus d'informations.";
        return switch (niveau.toLowerCase()) {
            case "faible"           -> "Votre niveau est satisfaisant. Continuez à prendre soin de vous ! 🌟";
            case "modere", "modéré" -> "Nous vous recommandons de consulter un professionnel. 💬";
            case "eleve", "élevé"   -> "Il est important de consulter un psychologue. 🏥";
            case "severe", "sévère" -> "Veuillez contacter immédiatement un professionnel. 🚨";
            default                 -> "Consultez votre responsable pour plus d'informations.";
        };
    }
}