package org.example.services.evenement;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EventEmailService {
    private final String host;
    private final String port;
    private String username;
    private String password;
    private final boolean authEnabled;

    // Configuration par défaut (Gmail SMTP)
    public EventEmailService() {
        this.host = "smtp.gmail.com";
        this.port = "587";
        this.username = ""; // À configurer avec votre email
        this.password = ""; // À configurer avec votre mot de passe (ou app password)
        this.authEnabled = true;
    }

    // Configuration personnalisée
    public EventEmailService(String host, String port, String username, String password, boolean authEnabled) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authEnabled = authEnabled;
    }

    /**
     * Envoyer un email simple
     */
    public boolean sendEmail(String to, String subject, String content) {
        return sendEmail(to, subject, content, false);
    }

    /**
     * Envoyer un email (texte ou HTML)
     */
    public boolean sendEmail(String to, String subject, String content, boolean isHtml) {
        if (username.isEmpty() || password.isEmpty()) {
            System.err.println("EventEmailService: username ou password non configuré. Configurez-les avant d'envoyer des emails.");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(authEnabled));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            if (isHtml) {
                message.setContent(content, "text/html; charset=UTF-8");
            } else {
                message.setText(content);
            }

            Transport.send(message);
            System.out.println("Email envoyé avec succès à: " + to);
            return true;
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Email de confirmation d'inscription
     */
    public boolean sendInscriptionConfirmation(String to, String participantName, String eventTitle, String eventDate, String eventLocation) {
        String subject = "Confirmation d'inscription : " + eventTitle;
        String content = buildInscriptionEmail(participantName, eventTitle, eventDate, eventLocation);
        return sendEmail(to, subject, content, true);
    }

    /**
     * Email de confirmation d'annulation d'inscription
     */
    public boolean sendAnnulationConfirmation(String to, String participantName, String eventTitle) {
        String subject = "Annulation d'inscription : " + eventTitle;
        String content = buildAnnulationEmail(participantName, eventTitle);
        return sendEmail(to, subject, content, true);
    }

    private String buildInscriptionEmail(String participantName, String eventTitle, String eventDate, String eventLocation) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 10px 10px; }" +
                ".event-details { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #667eea; }" +
                ".event-details strong { color: #667eea; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>UniMind</h2>" +
                "<p>Confirmation d'inscription</p>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour <strong>" + participantName + "</strong>,</p>" +
                "<p>Votre inscription à l'événement a été confirmée avec succès.</p>" +
                "<div class='event-details'>" +
                "<p><strong>Événement :</strong> " + eventTitle + "</p>" +
                "<p><strong>Date :</strong> " + eventDate + "</p>" +
                "<p><strong>Lieu :</strong> " + eventLocation + "</p>" +
                "</div>" +
                "<p>Nous espérons vous y voir !</p>" +
                "<p>Cordialement,<br>L'équipe UniMind</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildAnnulationEmail(String participantName, String eventTitle) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 10px 10px; }" +
                ".event-details { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #f5576c; }" +
                ".event-details strong { color: #f5576c; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>UniMind</h2>" +
                "<p>Annulation d'inscription</p>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour <strong>" + participantName + "</strong>,</p>" +
                "<p>Votre annulation d'inscription à l'événement a été prise en compte.</p>" +
                "<div class='event-details'>" +
                "<p><strong>Événement :</strong> " + eventTitle + "</p>" +
                "</div>" +
                "<p>Nous espérons vous revoir à un prochain événement !</p>" +
                "<p>Cordialement,<br>L'équipe UniMind</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Getters pour configuration
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
