package org.example.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Service d'envoi d'emails via Gmail SMTP.
 * Utilisé pour :
 *  - Avertissements de modération
 *  - Blocage après 3 infractions
 *  - Notification de réponse à un post
 *
 * Configuration Gmail requise :
 *  - Activer "Accès des applications moins sécurisées" OU
 *  - Utiliser un "Mot de passe d'application" (recommandé)
 *    depuis https://myaccount.google.com/apppasswords
 */
public class EmailForumService {

    private static final String FROM_EMAIL    = "souhakhenissi3@gmail.com";
    private static final String FROM_PASSWORD = "bfdmpqsjupyvictq";
    private static final String FROM_NAME     = "Unimind — Plateforme";

    // ─── Configuration SMTP Gmail ───────────────────────────
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
    }

    // ─── Méthode générique d'envoi ──────────────────────────
    public boolean sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé à " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            return false;
        }
    }

    // ─── Email d'avertissement modération (1ère et 2ème infraction) ──
    public void sendAvertissementModeration(String toEmail, String prenom,
                                            String motInterdit, int numeroInfraction) {
        String subject = "⚠️ Unimind — Avertissement de modération";
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px;
                        margin: 0 auto; background: #f8f9ff; padding: 20px;">
              <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6);
                          border-radius: 16px 16px 0 0; padding: 28px 30px; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 24px;">🧘 Unimind</h1>
                <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0;">Avertissement de modération</p>
              </div>

              <div style="background: white; border-radius: 0 0 16px 16px;
                          padding: 30px; border: 1px solid #e0e7ff;">
                <p style="font-size: 16px; color: #374151;">Bonjour <strong>%s</strong>,</p>

                <div style="background: #fef3c7; border-left: 4px solid #f59e0b;
                            padding: 16px; border-radius: 8px; margin: 20px 0;">
                  <p style="margin: 0; color: #92400e; font-weight: bold;">
                    ⚠️ Avertissement n°%d
                  </p>
                  <p style="margin: 8px 0 0; color: #b45309;">
                    Votre message contient un terme inapproprié qui a été détecté :
                    <strong>« %s »</strong>
                  </p>
                </div>

                <p style="color: #6b7280; font-size: 14px; line-height: 1.6;">
                  Votre publication a été <strong>refusée</strong>. Nous vous demandons
                  de respecter la charte de notre communauté bienveillante.
                </p>

                <div style="background: #fef2f2; border-radius: 8px; padding: 14px; margin: 16px 0;">
                  <p style="margin: 0; color: #991b1b; font-size: 13px;">
                    ⚠️ À la <strong>3ème infraction</strong>, votre compte sera automatiquement
                    <strong>suspendu</strong>.
                  </p>
                </div>

                <p style="color: #9ca3af; font-size: 12px; margin-top: 24px; text-align: center;">
                  L'équipe Unimind • <a href="mailto:unimind@gmail.com"
                  style="color: #6366f1;">unimind@gmail.com</a>
                </p>
              </div>
            </div>
            """.formatted(prenom, numeroInfraction, motInterdit);

        sendEmail(toEmail, subject, html);
    }

    // ─── Email de blocage (3ème infraction) ──────────────────
    public void sendEmailBlocage(String toEmail, String prenom, String motInterdit) {
        String subject = "🚫 Unimind — Compte suspendu";
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px;
                        margin: 0 auto; background: #f8f9ff; padding: 20px;">
              <div style="background: linear-gradient(135deg, #ef4444, #dc2626);
                          border-radius: 16px 16px 0 0; padding: 28px 30px; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 24px;">🧘 Unimind</h1>
                <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0;">Suspension de compte</p>
              </div>

              <div style="background: white; border-radius: 0 0 16px 16px;
                          padding: 30px; border: 1px solid #fecaca;">
                <p style="font-size: 16px; color: #374151;">Bonjour <strong>%s</strong>,</p>

                <div style="background: #fef2f2; border-left: 4px solid #ef4444;
                            padding: 16px; border-radius: 8px; margin: 20px 0;">
                  <p style="margin: 0; color: #991b1b; font-weight: bold;">
                    🚫 Votre compte a été suspendu
                  </p>
                  <p style="margin: 8px 0 0; color: #b91c1c;">
                    Suite à votre 3ème infraction (terme détecté : <strong>« %s »</strong>),
                    votre compte Unimind a été <strong>automatiquement désactivé</strong>.
                  </p>
                </div>

                <p style="color: #6b7280; font-size: 14px; line-height: 1.6;">
                  Vous ne pouvez plus vous connecter à la plateforme. Pour contester
                  cette décision, contactez notre équipe de modération.
                </p>

                <p style="color: #9ca3af; font-size: 12px; margin-top: 24px; text-align: center;">
                  L'équipe Unimind • <a href="mailto:unimind@gmail.com"
                  style="color: #6366f1;">unimind@gmail.com</a>
                </p>
              </div>
            </div>
            """.formatted(prenom, motInterdit);

        sendEmail(toEmail, subject, html);
    }

    // ─── Email de notification de réponse à un post ──────────
    public void sendNotificationReponse(String toEmail, String prenomDestinataire,
                                        String prenomAuteur, boolean auteurAnonyme,
                                        String titrePost, String contenuCommentaire) {
        String nomAuteur = auteurAnonyme ? "Un membre anonyme" : prenomAuteur;
        String subject = "💬 Unimind — Quelqu'un a répondu à votre post";
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px;
                        margin: 0 auto; background: #f8f9ff; padding: 20px;">
              <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6);
                          border-radius: 16px 16px 0 0; padding: 28px 30px; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 24px;">🧘 Unimind</h1>
                <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0;">Nouvelle réponse sur votre post</p>
              </div>

              <div style="background: white; border-radius: 0 0 16px 16px;
                          padding: 30px; border: 1px solid #e0e7ff;">
                <p style="font-size: 16px; color: #374151;">Bonjour <strong>%s</strong>,</p>

                <p style="color: #6b7280; font-size: 14px;">
                  <strong>%s</strong> a répondu à votre discussion :
                </p>

                <div style="background: #f5f3ff; border-left: 4px solid #6366f1;
                            padding: 14px; border-radius: 8px; margin: 16px 0;">
                  <p style="margin: 0 0 8px; color: #6366f1; font-weight: bold; font-size: 14px;">
                    📌 %s
                  </p>
                </div>

                <div style="background: #f8f7ff; border-radius: 10px; padding: 16px;
                            border: 1px solid #e0e7ff; margin: 16px 0;">
                  <p style="margin: 0 0 6px; color: #9ca3af; font-size: 11px;">
                    💬 Commentaire de %s :
                  </p>
                  <p style="margin: 0; color: #374151; font-size: 14px; line-height: 1.6;">
                    %s
                  </p>
                </div>

                <div style="text-align: center; margin-top: 24px;">
                  <p style="color: #9ca3af; font-size: 12px;">
                    Connectez-vous à Unimind pour voir la discussion complète et répondre.
                  </p>
                </div>

                <p style="color: #9ca3af; font-size: 12px; margin-top: 16px; text-align: center;">
                  L'équipe Unimind • <a href="mailto:unimind@gmail.com"
                  style="color: #6366f1;">unimind@gmail.com</a>
                </p>
              </div>
            </div>
            """.formatted(prenomDestinataire, nomAuteur, titrePost, nomAuteur, contenuCommentaire);

        sendEmail(toEmail, subject, html);
    }
}