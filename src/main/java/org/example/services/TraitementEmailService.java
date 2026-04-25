package org.example.services;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.example.entities.Etudiant;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.entities.User;

/**
 * Service d'email pour les traitements et suivis
 * Basé sur le service EmailSchedulerService de Symfony
 */
public class TraitementEmailService {

    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private boolean smtpAuth;
    private boolean smtpStarttls;
    private String fromEmail;
    private String fromName;

    public TraitementEmailService() {
        // Configuration par défaut - peut être chargée depuis un fichier de config
        this.smtpHost = "smtp.gmail.com"; // ou autre serveur SMTP
        this.smtpPort = 587;
        this.smtpAuth = true;
        this.smtpStarttls = true;
        this.fromEmail = "omaimagouider6@gmail.com"; // à configurer
        this.smtpUsername = "omaimagouider6@gmail.com"; // même email que l'expéditeur
        this.smtpPassword = "zchq ocxh cjzn ywnu"; // Remplacer par le mot de passe d'application Gmail de 16 caractères
        // Pour désactiver les emails pendant les tests, mettez cette valeur à null ou vide
        // this.smtpPassword = null; // Désactive les envois d'emails
        this.fromName = "Unimind - Plateforme de Suivi Thérapeutique";
    }

    /**
     * Configure les paramètres SMTP
     */
    public void configureSmtp(String host, int port, String username, String password) {
        this.smtpHost = host;
        this.smtpPort = port;
        this.smtpUsername = username;
        this.smtpPassword = password;
    }

    /**
     * Envoie un email de notification lors de la création d'un traitement
     */
    public EmailResult envoyerNotificationNouveauTraitement(Traitement traitement) {
        try {
            EtudiantTraitementService etudiantService = new EtudiantTraitementService();
            Etudiant etudiant = etudiantService.trouverParId(traitement.getEtudiantId());

            if (etudiant == null) {
                return new EmailResult(false, "Étudiant non trouvé pour l'ID: " + traitement.getEtudiantId());
            }

            String subject = "Nouveau traitement assigné : " + traitement.getTitre();
            String body = creerEmailNouveauTraitement(traitement, etudiant);

            return sendEmail(etudiant.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi email création traitement: " + e.getMessage());
        }
    }

    /**
     * Envoie un email de notification lors de la fin d'un traitement
     */
    public EmailResult envoyerNotificationFinTraitement(Traitement traitement) {
        try {
            EtudiantTraitementService etudiantService = new EtudiantTraitementService();
            Etudiant etudiant = etudiantService.trouverParId(traitement.getEtudiantId());

            if (etudiant == null) {
                return new EmailResult(false, "Étudiant non trouvé pour l'ID: " + traitement.getEtudiantId());
            }

            String subject = "Traitement terminé : " + traitement.getTitre();
            String body = creerEmailFinTraitement(traitement, etudiant);

            return sendEmail(etudiant.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi email fin traitement: " + e.getMessage());
        }
    }

    /**
     * Envoie un email de rappel pour suivi manquant
     */
    public EmailResult envoyerNotificationSuiviManquant(Traitement traitement) {
        try {
            EtudiantTraitementService etudiantService = new EtudiantTraitementService();
            Etudiant etudiant = etudiantService.trouverParId(traitement.getEtudiantId());

            if (etudiant == null) {
                return new EmailResult(false, "Étudiant non trouvé pour l'ID: " + traitement.getEtudiantId());
            }

            String subject = "Rappel : Suivi de traitement requis";
            String body = creerEmailRappelSuivi(traitement, etudiant);

            return sendEmail(etudiant.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi email rappel suivi: " + e.getMessage());
        }
    }

    /**
     * Envoie un rapport hebdomadaire à un psychologue
     */
    public EmailResult envoyerRapportHebdomadaire(User psychologue) {
        try {
            String subject = "Rapport hebdomadaire de vos traitements";
            String body = creerRapportHebdomadaire(psychologue);

            return sendEmail(psychologue.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi rapport hebdomadaire: " + e.getMessage());
        }
    }

    /**
     * Envoie un email à l'étudiant quand un psychologue ajoute un traitement
     */
    public EmailResult envoyerNotificationNouveauTraitementEtudiant(Traitement traitement, User psychologue) {
        try {
            EtudiantTraitementService etudiantService = new EtudiantTraitementService();
            Etudiant etudiant = etudiantService.trouverParId(traitement.getEtudiantId());

            if (etudiant == null) {
                return new EmailResult(false, "Étudiant non trouvé pour l'ID: " + traitement.getEtudiantId());
            }

            String subject = "Nouveau traitement prescrit - " + traitement.getTitre();
            String body = creerEmailNouveauTraitementEtudiant(traitement, psychologue, etudiant);

            return sendEmail(etudiant.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi email nouveau traitement étudiant: " + e.getMessage());
        }
    }

    /**
     * Envoie un email à l'étudiant quand un psychologue modifie un traitement
     */
    public EmailResult envoyerNotificationModificationTraitementEtudiant(Traitement traitement, User psychologue) {
        try {
            EtudiantTraitementService etudiantService = new EtudiantTraitementService();
            Etudiant etudiant = etudiantService.trouverParId(traitement.getEtudiantId());

            if (etudiant == null) {
                return new EmailResult(false, "Étudiant non trouvé pour l'ID: " + traitement.getEtudiantId());
            }

            String subject = "Traitement modifié - " + traitement.getTitre();
            String body = creerEmailModificationTraitementEtudiant(traitement, psychologue, etudiant);

            return sendEmail(etudiant.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi email modification traitement étudiant: " + e.getMessage());
        }
    }

    /**
     * Envoie un email au psychologue quand un étudiant ajoute un suivi
     */
    public EmailResult envoyerNotificationNouveauSuiviPsychologue(SuiviTraitement suivi, Traitement traitement, User etudiant) {
        try {
            PsychologueService psychologueService = new PsychologueService();
            User psychologue = psychologueService.getPsychologueById(traitement.getPsychologueId());

            if (psychologue == null) {
                return new EmailResult(false, "Psychologue non trouvé pour l'ID: " + traitement.getPsychologueId());
            }

            String subject = "Nouveau suivi ajouté - " + traitement.getTitre();
            String body = creerEmailNouveauSuiviPsychologue(suivi, traitement, etudiant, psychologue);

            return sendEmail(psychologue.getEmail(), subject, body);

        } catch (Exception e) {
            return new EmailResult(false, "Erreur envoi email nouveau suivi psychologue: " + e.getMessage());
        }
    }

    /**
     * Vérifie et envoie les rappels de suivi automatiques
     * À exécuter quotidiennement
     */
    public RapportResult sendAutomaticReminders() {
        RapportResult result = new RapportResult();

        try {
            TraitementService traitementService = new TraitementService();
            List<Traitement> traitements = traitementService.afficher();

            for (Traitement traitement : traitements) {
                if (traitement.getStatut().name().equals("EN_COURS")) {
                    try {
                        SuiviTraitementService suiviService = new SuiviTraitementService();
                        List<SuiviTraitement> suivis = suiviService.getByTraitementId(traitement.getTraitementId());

                        boolean shouldSendReminder = false;
                        String raison = "";

                        if (suivis.isEmpty()) {
                            // Cas 1: Traitement sans suivi depuis plus de 5 jours
                            if (traitement.getDateDebut() != null &&
                                    traitement.getDateDebut().toLocalDate().isBefore(LocalDate.now().minusDays(5))) {
                                shouldSendReminder = true;
                                raison = "Traitement sans suivi depuis plus de 5 jours";
                            }
                        } else {
                            // Cas 2: Dernier suivi trop ancien
                            SuiviTraitement dernierSuivi = suivis.get(suivis.size() - 1);
                            if (dernierSuivi.getDateSuivi() != null &&
                                    dernierSuivi.getDateSuivi().toLocalDate().isBefore(LocalDate.now().minusDays(7))) {
                                shouldSendReminder = true;
                                raison = "Dernier suivi il y a plus de 7 jours";
                            }
                        }

                        if (shouldSendReminder) {
                            EmailResult emailResult = envoyerNotificationSuiviManquant(traitement);
                            if (emailResult.isSuccess()) {
                                result.incrementEmailsEnvoyes();
                                result.addDetail(traitement.getTraitementId(), "suivi_manquant", raison);
                            } else {
                                result.incrementErreurs();
                                result.addDetail(traitement.getTraitementId(), "erreur_email", emailResult.getErrorMessage());
                            }
                        }

                    } catch (Exception e) {
                        result.incrementErreurs();
                        System.err.println("Erreur traitement " + traitement.getTraitementId() + ": " + e.getMessage());
                    }
                }
            }

        } catch (SQLException e) {
            result.incrementErreurs();
            System.err.println("Erreur générale envoi rappels: " + e.getMessage());
        }

        return result;
    }

    /**
     * Envoie les rapports hebdomadaires à tous les psychologues
     * À exécuter chaque dimanche
     */
    public RapportResult sendWeeklyReports() {
        RapportResult result = new RapportResult();

        try {
            // Récupérer tous les psychologues (à implémenter selon votre structure)
            // Pour l'instant, simulation
            List<User> psychologues = getPsychologues();

            for (User psychologue : psychologues) {
                try {
                    EmailResult emailResult = envoyerRapportHebdomadaire(psychologue);
                    if (emailResult.isSuccess()) {
                        result.incrementRapportsEnvoyes();
                        result.addDetail(psychologue.getUserId(), "rapport_envoye", "Succès");
                    } else {
                        result.addDetail(psychologue.getUserId(), "rapport_echec", emailResult.getErrorMessage());
                    }
                } catch (Exception e) {
                    result.incrementErreurs();
                    System.err.println("Erreur rapport psychologue " + psychologue.getUserId() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            result.incrementErreurs();
            System.err.println("Erreur générale rapports hebdomadaires: " + e.getMessage());
        }

        return result;
    }

    /**
     * Envoie un email via SMTP
     */
    private EmailResult sendEmail(String toEmail, String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", smtpAuth ? "true" : "false");
            props.put("mail.smtp.starttls.enable", smtpStarttls ? "true" : "false");
            props.put("mail.smtp.ssl.trust", smtpHost);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);

            return new EmailResult(true, "Email envoyé avec succès");

        } catch (MessagingException e) {
            return new EmailResult(false, "Erreur envoi email: " + e.getMessage());
        } catch (Exception e) {
            return new EmailResult(false, "Erreur générale: " + e.getMessage());
        }
    }

    /**
     * Crée le contenu de l'email pour nouveau traitement
     */
    private String creerEmailNouveauTraitement(Traitement traitement, Etudiant etudiant) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#4f46e5;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".field{margin-bottom:15px;}");
        html.append(".label{font-weight:bold;color:#4f46e5;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>🎯 Nouveau Traitement Assigné</h2>");
        html.append("<p>Bonjour ").append(etudiant.getPrenom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Un nouveau traitement vous a été assigné par votre psychologue.</p>");

        html.append("<div class='field'>");
        html.append("<span class='label'>Titre du traitement :</span> ").append(traitement.getTitre());
        html.append("</div>");

        if (traitement.getDescription() != null) {
            html.append("<div class='field'>");
            html.append("<span class='label'>Description :</span><br>").append(traitement.getDescription());
            html.append("</div>");
        }

        html.append("<div class='field'>");
        html.append("<span class='label'>Catégorie :</span> ").append(traitement.getCategorie().name());
        html.append("</div>");

        html.append("<div class='field'>");
        html.append("<span class='label'>Priorité :</span> ").append(traitement.getPriorite().name());
        html.append("</div>");

        if (traitement.getObjectifTherapeutique() != null) {
            html.append("<div class='field'>");
            html.append("<span class='label'>Objectif thérapeutique :</span><br>").append(traitement.getObjectifTherapeutique());
            html.append("</div>");
        }

        if (traitement.getDateDebut() != null) {
            html.append("<div class='field'>");
            html.append("<span class='label'>Date de début :</span> ");
            html.append(traitement.getDateDebut().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            html.append("</div>");
        }

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Cet email a été envoyé automatiquement par la plateforme Unimind.</p>");
        html.append("<p>Connectez-vous à votre espace pour suivre votre traitement.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Crée le contenu de l'email pour fin de traitement
     */
    private String creerEmailFinTraitement(Traitement traitement, Etudiant etudiant) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#10b981;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>✅ Traitement Terminé</h2>");
        html.append("<p>Félicitations ").append(etudiant.getPrenom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Votre traitement a été marqué comme terminé avec succès.</p>");

        html.append("<div><strong>Traitement :</strong> ").append(traitement.getTitre()).append("</div>");

        if (traitement.getObjectifTherapeutique() != null) {
            html.append("<div><strong>Objectif atteint :</strong><br>").append(traitement.getObjectifTherapeutique()).append("</div>");
        }

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Cet email a été envoyé automatiquement par la plateforme Unimind.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Crée le contenu de l'email de rappel
     */
    private String creerEmailRappelSuivi(Traitement traitement, Etudiant etudiant) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#f59e0b;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>⏰ Rappel de Suivi</h2>");
        html.append("<p>Bonjour ").append(etudiant.getPrenom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Il est temps de faire un suivi pour votre traitement.</p>");

        html.append("<div><strong>Traitement :</strong> ").append(traitement.getTitre()).append("</div>");

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Connectez-vous à votre espace pour enregistrer votre suivi.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Crée le contenu du rapport hebdomadaire
     */
    private String creerRapportHebdomadaire(User psychologue) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#4f46e5;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>📊 Rapport Hebdomadaire</h2>");
        html.append("<p>Bonjour Dr ").append(psychologue.getNom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Voici votre rapport d'activité pour la semaine écoulée.</p>");

        // À implémenter avec les vraies statistiques
        html.append("<div><strong>Rapport généré le :</strong> ");
        html.append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        html.append("</div>");

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Ce rapport a été généré automatiquement par la plateforme Unimind.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Crée le contenu de l'email pour notification nouveau traitement à l'étudiant
     */
    private String creerEmailNouveauTraitementEtudiant(Traitement traitement, User psychologue, Etudiant etudiant) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#4f46e5;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".info-box{background:#e0e7ff;padding:15px;border-radius:6px;margin:10px 0;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>📋 Nouveau Traitement Prescrit</h2>");
        html.append("<p>Bonjour ").append(etudiant.getPrenom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Dr ").append(psychologue.getPrenom()).append(" ").append(psychologue.getNom());
        html.append(" vous a prescrit un nouveau traitement.</p>");

        html.append("<div class='info-box'>");
        html.append("<h3>").append(traitement.getTitre()).append("</h3>");
        html.append("<p><strong>Catégorie :</strong> ").append(traitement.getCategorie()).append("</p>");
        html.append("<p><strong>Type :</strong> ").append(traitement.getType()).append("</p>");
        html.append("<p><strong>Priorité :</strong> ").append(traitement.getPriorite()).append("</p>");
        html.append("<p><strong>Durée :</strong> ").append(traitement.getDureeJours()).append(" jour(s)</p>");
        if (traitement.getDosage() != null && !traitement.getDosage().isEmpty()) {
            html.append("<p><strong>Dosage :</strong> ").append(traitement.getDosage()).append("</p>");
        }
        html.append("</div>");

        if (traitement.getDescription() != null && !traitement.getDescription().isEmpty()) {
            html.append("<p><strong>Description :</strong></p>");
            html.append("<p>").append(traitement.getDescription()).append("</p>");
        }

        if (traitement.getObjectifTherapeutique() != null && !traitement.getObjectifTherapeutique().isEmpty()) {
            html.append("<p><strong>Objectifs thérapeutiques :</strong></p>");
            html.append("<p>").append(traitement.getObjectifTherapeutique()).append("</p>");
        }

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Connectez-vous à votre espace Unimind pour suivre votre traitement.</p>");
        html.append("<p>Ce message a été généré automatiquement.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Crée le contenu de l'email pour notification modification traitement à l'étudiant
     */
    private String creerEmailModificationTraitementEtudiant(Traitement traitement, User psychologue, Etudiant etudiant) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#f59e0b;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".info-box{background:#fef3c7;padding:15px;border-radius:6px;margin:10px 0;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>✏️ Traitement Modifié</h2>");
        html.append("<p>Bonjour ").append(etudiant.getPrenom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Dr ").append(psychologue.getPrenom()).append(" ").append(psychologue.getNom());
        html.append(" a modifié votre traitement.</p>");

        html.append("<div class='info-box'>");
        html.append("<h3>").append(traitement.getTitre()).append("</h3>");
        html.append("<p><strong>Catégorie :</strong> ").append(traitement.getCategorie()).append("</p>");
        html.append("<p><strong>Type :</strong> ").append(traitement.getType()).append("</p>");
        html.append("<p><strong>Statut :</strong> ").append(traitement.getStatut()).append("</p>");
        html.append("<p><strong>Priorité :</strong> ").append(traitement.getPriorite()).append("</p>");
        html.append("<p><strong>Durée :</strong> ").append(traitement.getDureeJours()).append(" jour(s)</p>");
        if (traitement.getDosage() != null && !traitement.getDosage().isEmpty()) {
            html.append("<p><strong>Dosage :</strong> ").append(traitement.getDosage()).append("</p>");
        }
        html.append("</div>");

        if (traitement.getDescription() != null && !traitement.getDescription().isEmpty()) {
            html.append("<p><strong>Description :</strong></p>");
            html.append("<p>").append(traitement.getDescription()).append("</p>");
        }

        if (traitement.getObjectifTherapeutique() != null && !traitement.getObjectifTherapeutique().isEmpty()) {
            html.append("<p><strong>Objectifs thérapeutiques :</strong></p>");
            html.append("<p>").append(traitement.getObjectifTherapeutique()).append("</p>");
        }

        html.append("<p><strong>Veuillez consulter les détails mis à jour dans votre espace Unimind.</strong></p>");
        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Connectez-vous à votre espace Unimind pour suivre votre traitement.</p>");
        html.append("<p>Ce message a été généré automatiquement.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Crée le contenu de l'email pour notification nouveau suivi au psychologue
     */
    private String creerEmailNouveauSuiviPsychologue(SuiviTraitement suivi, Traitement traitement, User etudiant, User psychologue) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;color:#333;}");
        html.append(".header{background:#10b981;color:white;padding:20px;border-radius:8px;margin-bottom:20px;}");
        html.append(".content{background:#f9fafb;padding:20px;border-radius:8px;}");
        html.append(".info-box{background:#d1fae5;padding:15px;border-radius:6px;margin:10px 0;}");
        html.append(".footer{margin-top:20px;font-size:12px;color:#666;}</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h2>📝 Nouveau Suivi Ajouté</h2>");
        html.append("<p>Bonjour Dr ").append(psychologue.getPrenom()).append(",</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Votre patient ").append(etudiant.getPrenom()).append(" ").append(etudiant.getNom());
        html.append(" a ajouté un nouveau suivi pour son traitement.</p>");

        html.append("<div class='info-box'>");
        html.append("<h3>Traitement : ").append(traitement.getTitre()).append("</h3>");
        html.append("<p><strong>Date du suivi :</strong> ");
        if (suivi.getDateSuivi() != null) {
            html.append(suivi.getDateSuivi().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            html.append("Date non spécifiée");
        }
        html.append("</p>");
        html.append("<p><strong>Saisi par :</strong> Étudiant</p>");
        html.append("</div>");

        if (suivi.getObservations() != null && !suivi.getObservations().isEmpty()) {
            html.append("<p><strong>Observations du patient :</strong></p>");
            html.append("<div style='background:#f3f4f6;padding:15px;border-radius:6px;font-style:italic;'>");
            html.append("<p>").append(suivi.getObservations()).append("</p>");
            html.append("</div>");
        }

        html.append("<p><strong>Actions recommandées :</strong></p>");
        html.append("<ul>");
        html.append("<li>Consulter les observations du patient</li>");
        html.append("<li>Évaluer l'évolution du traitement</li>");
        html.append("<li>Contacter le patient si nécessaire</li>");
        html.append("</ul>");

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>Connectez-vous à votre espace Unimind pour plus de détails.</p>");
        html.append("<p>Ce message a été généré automatiquement.</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Simulation de récupération des psychologues (à adapter selon votre structure)
     */
    private List<User> getPsychologues() {
        // À implémenter selon votre structure de données
        return List.of(); // liste vide pour l'instant
    }

    /**
     * Classe résultat pour l'envoi d'email
     */
    public static class EmailResult {
        private boolean success;
        private String errorMessage;

        public EmailResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Classe résultat pour les rapports automatisés
     */
    public static class RapportResult {
        private int emailsEnvoyes;
        private int erreurs;
        private int rapportsEnvoyes;
        private List<RapportDetail> details;

        public RapportResult() {
            this.emailsEnvoyes = 0;
            this.erreurs = 0;
            this.rapportsEnvoyes = 0;
            this.details = new java.util.ArrayList<>();
        }

        public void incrementEmailsEnvoyes() { emailsEnvoyes++; }
        public void incrementErreurs() { erreurs++; }
        public void incrementRapportsEnvoyes() { rapportsEnvoyes++; }
        public void addDetail(int id, String type, String raison) {
            details.add(new RapportDetail(id, type, raison));
        }

        // Getters
        public int getEmailsEnvoyes() { return emailsEnvoyes; }
        public int getErreurs() { return erreurs; }
        public int getRapportsEnvoyes() { return rapportsEnvoyes; }
        public List<RapportDetail> getDetails() { return details; }

        public static class RapportDetail {
            private int id;
            private String type;
            private String raison;

            public RapportDetail(int id, String type, String raison) {
                this.id = id;
                this.type = type;
                this.raison = raison;
            }

            // Getters
            public int getId() { return id; }
            public String getType() { return type; }
            public String getRaison() { return raison; }
        }
    }
}
