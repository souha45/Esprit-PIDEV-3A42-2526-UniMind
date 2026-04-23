package org.example.main;

import org.example.entities.*;
import org.example.services.*;
import org.example.utils.MyDataBase_Unimind;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws SQLException {

        // ── CONNEXION DB ──────────────────────────────────────────────
        MyDataBase_Unimind db = MyDataBase_Unimind.getInstance();
        if (db.getConnection() != null) {
            System.out.println("✓ Connexion à la base de données réussie !\n");
        } else {
            System.out.println("✗ Échec de la connexion.");
            System.exit(1);
        }

        AdminService       adminService       = new AdminService();
        EtudiantService    etudiantService    = new EtudiantService();
        PsychologueService psychologueService = new PsychologueService();
        ResponsableService responsableService = new ResponsableService();

        // Utilisateur connecté (un seul à la fois)
        User utilisateurConnecte = null;

        while (true) {
            if (utilisateurConnecte == null) {
                // ── MENU NON CONNECTE ─────────────────────────────────
                System.out.println("\n" + "=".repeat(50));
                System.out.println("            UNIMIND - MENU PRINCIPAL");
                System.out.println("=".repeat(50));
                System.out.println("1.  S'inscrire");
                System.out.println("2.  Se connecter");
                System.out.println("0.  Quitter");
                System.out.print("\nVotre choix : ");

                int choix = lireInt();
                switch (choix) {
                    case 1:
                        menuInscription(etudiantService, psychologueService, responsableService);
                        break;
                    case 2:
                        utilisateurConnecte = menuConnexion(adminService, etudiantService,
                                psychologueService, responsableService, db);
                        if (utilisateurConnecte != null) {
                            System.out.println("\n✓ Bienvenue " + utilisateurConnecte.getPrenom()
                                    + " " + utilisateurConnecte.getNom() + " !");
                        }
                        break;
                    case 0:
                        System.out.println("\nAu revoir !");
                        scanner.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("\n❌ Choix invalide");
                }

            } else {
                // ── MENU SELON ROLE ───────────────────────────────────
                switch (utilisateurConnecte.getRole()) {
                    case ADMIN:
                        utilisateurConnecte = menuAdmin(adminService, utilisateurConnecte);
                        break;
                    case ETUDIANT:
                        utilisateurConnecte = menuUtilisateur(etudiantService, psychologueService,
                                responsableService, utilisateurConnecte);
                        break;
                    case PSYCHOLOGUE:
                        utilisateurConnecte = menuUtilisateur(etudiantService, psychologueService,
                                responsableService, utilisateurConnecte);
                        break;
                    case RESPONSABLE_ETUDIANT:
                        utilisateurConnecte = menuUtilisateur(etudiantService, psychologueService,
                                responsableService, utilisateurConnecte);
                        break;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MENU ADMIN
    // ═══════════════════════════════════════════════════════════════════
    private static User menuAdmin(AdminService service, User admin) throws SQLException {
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("     ADMIN : " + admin.getPrenom() + " " + admin.getNom());
            System.out.println("=".repeat(50));
            System.out.println("1.  Ajouter un utilisateur");
            System.out.println("2.  Modifier un utilisateur");
            System.out.println("3.  Supprimer un utilisateur");
            System.out.println("4.  Bloquer un utilisateur");
            System.out.println("5.  Débloquer un utilisateur");
            System.out.println("6.  Accepter une demande d'inscription");
            System.out.println("7.  Refuser une demande d'inscription");
            System.out.println("8.  Afficher les demandes en attente");
            System.out.println("9.  Afficher tous les utilisateurs");
            System.out.println("10. Modifier mon profil");
            System.out.println("7.  Se déconnecter");
            System.out.println("0.  Quitter");
            System.out.print("\nVotre choix : ");

            int choix = lireInt();
            switch (choix) {
                case 1: ajouterUtilisateur(service); break;
                case 2: modifierUtilisateur(service); break;
                case 3: supprimerUtilisateur(service); break;
                case 4: bloquerUtilisateur(service); break;
                case 5: debloquerUtilisateur(service); break;
                case 6: accepterDemande(service); break;
                case 7: refuserDemande(service); break;
                case 8: afficherDemandesAttente(service); break;
                case 9: afficherTousUtilisateurs(service); break;
                case 10:
                    System.out.println("\n⚠ Modification de profil admin non disponible dans ce menu.");
                    break;
                case 77:
                    service.deconnexion(admin);
                    return null; // déconnexion
                case 0:
                    System.out.println("\nAu revoir !");
                    System.exit(0);
                default:
                    System.out.println("❌ Choix invalide");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MENU UTILISATEUR (Etudiant / Psy / Responsable)
    // ═══════════════════════════════════════════════════════════════════
    private static User menuUtilisateur(EtudiantService etudiantService,
                                        PsychologueService psychologueService,
                                        ResponsableService responsableService,
                                        User user) throws SQLException {
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("     " + user.getRole() + " : " + user.getPrenom() + " " + user.getNom());
            System.out.println("=".repeat(50));
            System.out.println("1.  Modifier mon profil");
            System.out.println("2.  Changer mon mot de passe");
            System.out.println("3.  Se déconnecter");
            System.out.println("0.  Quitter");
            System.out.print("\nVotre choix : ");

            int choix = lireInt();
            switch (choix) {
                case 1:
                    menuProfil(user, etudiantService, psychologueService, responsableService);
                    break;
                case 2:
                    changerMotDePasse(user, etudiantService, psychologueService, responsableService);
                    break;
                case 3:
                    System.out.println("✓ Déconnexion réussie : " + user.getPrenom() + " " + user.getNom());
                    return null; // déconnexion
                case 0:
                    System.out.println("\nAu revoir !");
                    System.exit(0);
                default:
                    System.out.println("❌ Choix invalide");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONNEXION (avec détection du rôle)
    // ═══════════════════════════════════════════════════════════════════
    private static User menuConnexion(AdminService adminService, EtudiantService etudiantService,
                                      PsychologueService psychologueService, ResponsableService responsableService,
                                      MyDataBase_Unimind db) throws SQLException {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("              CONNEXION");
        System.out.println("-".repeat(40));
        System.out.print("Email : ");
        String email = scanner.nextLine();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine();

        User user = UserService.connexionGenerale(email, password, db.getConnection(),
                adminService, etudiantService, psychologueService, responsableService);

        if (user == null) {
            System.out.println("\n❌ Email ou mot de passe incorrect");
        }
        return user;
    }

    // ═══════════════════════════════════════════════════════════════════
    // MENU INSCRIPTION
    // ═══════════════════════════════════════════════════════════════════
    private static void menuInscription(EtudiantService etudiantService,
                                        PsychologueService psychologueService,
                                        ResponsableService responsableService) throws SQLException {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("    INSCRIPTION - Choisissez votre rôle");
        System.out.println("-".repeat(40));
        System.out.println("1. Étudiant");
        System.out.println("2. Psychologue");
        System.out.println("3. Responsable Étudiant");
        System.out.print("\nVotre choix : ");

        int choix = lireInt();
        switch (choix) {
            case 1: inscrireEtudiant(etudiantService); break;
            case 2: inscrirePsychologue(psychologueService); break;
            case 3: inscrireResponsable(responsableService); break;
            default: System.out.println("❌ Choix invalide");
        }
    }

    private static void inscrireEtudiant(EtudiantService service) throws SQLException {
        System.out.println("\n--- INSCRIPTION ÉTUDIANT ---");
        System.out.print("Nom : "); String nom = scanner.nextLine();
        System.out.print("Prénom : "); String prenom = scanner.nextLine();
        System.out.print("Email : "); String email = scanner.nextLine();
        System.out.print("Mot de passe : "); String password = scanner.nextLine();
        System.out.print("CIN : "); String cin = scanner.nextLine();
        System.out.print("Identifiant étudiant : "); String identifiant = scanner.nextLine();
        System.out.print("Nom de l'établissement : "); String etablissement = scanner.nextLine();

        Etudiant etudiant = new Etudiant(nom, prenom, email, password, cin, identifiant, etablissement, "en_attente");
        service.inscrire(etudiant);
        System.out.println("✓ Inscription enregistrée - En attente de validation par l'admin");
    }

    private static void inscrirePsychologue(PsychologueService service) throws SQLException {
        System.out.println("\n--- INSCRIPTION PSYCHOLOGUE ---");
        System.out.print("Nom : "); String nom = scanner.nextLine();
        System.out.print("Prénom : "); String prenom = scanner.nextLine();
        System.out.print("Email : "); String email = scanner.nextLine();
        System.out.print("Mot de passe : "); String password = scanner.nextLine();
        System.out.print("CIN : "); String cin = scanner.nextLine();
        System.out.print("Spécialité : "); String specialite = scanner.nextLine();
        System.out.print("Adresse : "); String adresse = scanner.nextLine();
        System.out.print("Téléphone : "); String telephone = scanner.nextLine();

        Psychologue psy = new Psychologue(nom, prenom, email, password, cin, specialite, adresse, telephone, "en_attente");
        service.inscrire(psy);
        System.out.println("✓ Inscription enregistrée - En attente de validation par l'admin");
    }

    private static void inscrireResponsable(ResponsableService service) throws SQLException {
        System.out.println("\n--- INSCRIPTION RESPONSABLE ÉTUDIANT ---");
        System.out.print("Nom : "); String nom = scanner.nextLine();
        System.out.print("Prénom : "); String prenom = scanner.nextLine();
        System.out.print("Email : "); String email = scanner.nextLine();
        System.out.print("Mot de passe : "); String password = scanner.nextLine();
        System.out.print("CIN : "); String cin = scanner.nextLine();
        System.out.print("Poste : "); String poste = scanner.nextLine();
        System.out.print("Établissement : "); String etablissement = scanner.nextLine();

        ResponsableEtudiant responsable = new ResponsableEtudiant(nom, prenom, email, password, cin, poste, etablissement, "en_attente");
        service.inscrire(responsable);
        System.out.println("✓ Inscription enregistrée - En attente de validation par l'admin");
    }

    // ═══════════════════════════════════════════════════════════════════
    // ACTIONS ADMIN
    // ═══════════════════════════════════════════════════════════════════
    private static void ajouterUtilisateur(AdminService service) throws SQLException {
        System.out.println("\n--- AJOUTER UN UTILISATEUR ---");
        System.out.print("Nom : "); String nom = scanner.nextLine();
        System.out.print("Prénom : "); String prenom = scanner.nextLine();
        System.out.print("Email : "); String email = scanner.nextLine();
        System.out.print("Mot de passe : "); String password = scanner.nextLine();
        System.out.print("CIN : "); String cin = scanner.nextLine();
        System.out.print("Rôle (ETUDIANT/PSYCHOLOGUE/RESPONSABLE_ETUDIANT/ADMIN) : ");
        String roleStr = scanner.nextLine();

        User user = new User(nom, prenom, email, password, cin,
                org.example.enums.Role.valueOf(roleStr), "actif");
        user.setActive(true);
        user.setVerified(true);
        service.ajouter(user);
    }

    private static void modifierUtilisateur(AdminService service) throws SQLException {
        System.out.print("\nID de l'utilisateur à modifier : ");
        int id = lireInt();

        List<User> users = service.afficher();
        User u = users.stream().filter(x -> x.getUserId() == id).findFirst().orElse(null);
        if (u == null) { System.out.println("❌ Introuvable"); return; }

        System.out.println("Laissez vide pour ne pas modifier");
        System.out.print("Nouveau nom (" + u.getNom() + ") : ");
        String nom = scanner.nextLine(); if (!nom.isEmpty()) u.setNom(nom);
        System.out.print("Nouveau prénom (" + u.getPrenom() + ") : ");
        String prenom = scanner.nextLine(); if (!prenom.isEmpty()) u.setPrenom(prenom);
        System.out.print("Nouvel email (" + u.getEmail() + ") : ");
        String email = scanner.nextLine(); if (!email.isEmpty()) u.setEmail(email);
        System.out.print("Nouveau CIN (" + u.getCin() + ") : ");
        String cin = scanner.nextLine(); if (!cin.isEmpty()) u.setCin(cin);

        service.modifier(u);
    }

    private static void supprimerUtilisateur(AdminService service) throws SQLException {
        System.out.print("\nID à supprimer : "); int id = lireInt();
        service.supprimer(id);
    }

    private static void bloquerUtilisateur(AdminService service) throws SQLException {
        System.out.print("\nID à bloquer : "); int id = lireInt();
        service.bloquer(id);
    }

    private static void debloquerUtilisateur(AdminService service) throws SQLException {
        System.out.print("\nID à débloquer : "); int id = lireInt();
        service.debloquer(id);
    }

    private static void accepterDemande(AdminService service) throws SQLException {
        afficherDemandesAttente(service);
        System.out.print("\nID à accepter : "); int id = lireInt();
        service.accepterDemande(id);
    }

    private static void refuserDemande(AdminService service) throws SQLException {
        afficherDemandesAttente(service);
        System.out.print("\nID à refuser : "); int id = lireInt();
        service.refuserDemande(id);
    }

    private static void afficherDemandesAttente(AdminService service) throws SQLException {
        System.out.println("\n--- DEMANDES EN ATTENTE ---");
        List<User> demandes = service.afficherDemandesEnAttente();
        if (demandes.isEmpty()) {
            System.out.println("  Aucune demande en attente");
        } else {
            for (User u : demandes) {
                System.out.println("  ID:" + u.getUserId() + " | " + u.getPrenom() + " " + u.getNom()
                        + " | " + u.getRole() + " | " + u.getStatut());
            }
        }
    }

    private static void afficherTousUtilisateurs(AdminService service) throws SQLException {
        System.out.println("\n--- TOUS LES UTILISATEURS ---");
        List<User> users = service.afficher();
        for (User u : users) {
            System.out.println("  ID:" + u.getUserId() + " | " + u.getPrenom() + " " + u.getNom()
                    + " | " + u.getRole() + " | " + u.getStatut() + " | Actif:" + u.isActive());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MODIFIER PROFIL
    // ═══════════════════════════════════════════════════════════════════
    private static void menuProfil(User user, EtudiantService etudiantService,
                                   PsychologueService psychologueService,
                                   ResponsableService responsableService) throws SQLException {
        System.out.println("\n--- MODIFICATION DU PROFIL ---");

        System.out.print("Bio : "); String bio = scanner.nextLine();
        System.out.print("Téléphone : "); String tel = scanner.nextLine();

        Profil profil = new Profil();
        profil.setUserId(user.getUserId());
        profil.setBio(bio);
        profil.setTel(tel);

        if (user instanceof Etudiant) {
            System.out.print("Niveau : "); String niveau = scanner.nextLine();
            System.out.print("Filière : "); String filiere = scanner.nextLine();
            System.out.print("Pseudo : "); String pseudo = scanner.nextLine();
            profil.setNiveau(niveau);
            profil.setFiliere(filiere);
            profil.setPseudo(pseudo);
            etudiantService.modifierProfil(profil);

        } else if (user instanceof Psychologue) {
            System.out.print("Spécialité : "); String specialite = scanner.nextLine();
            System.out.print("Expérience : "); String experience = scanner.nextLine();
            System.out.print("Qualification : "); String qualification = scanner.nextLine();
            System.out.print("Pseudo : "); String pseudo = scanner.nextLine();
            profil.setSpecialite(specialite);
            profil.setExperience(experience);
            profil.setQualification(qualification);
            profil.setPseudo(pseudo);
            psychologueService.modifierProfil(profil);

        } else if (user instanceof ResponsableEtudiant) {
            System.out.print("Département : "); String dept = scanner.nextLine();
            System.out.print("Fonction : "); String fonction = scanner.nextLine();
            System.out.print("Pseudo : "); String pseudo = scanner.nextLine();
            profil.setDepartement(dept);
            profil.setFonction(fonction);
            profil.setPseudo(pseudo);
            responsableService.modifierProfil(profil);
        }

        System.out.println("✓ Profil mis à jour avec succès.");
    }

    // ═══════════════════════════════════════════════════════════════════
    // CHANGER MOT DE PASSE
    // ═══════════════════════════════════════════════════════════════════
    private static void changerMotDePasse(User user, EtudiantService etudiantService,
                                          PsychologueService psychologueService,
                                          ResponsableService responsableService) throws SQLException {
        System.out.println("\n--- CHANGER MOT DE PASSE ---");
        System.out.print("Ancien mot de passe : "); String ancien = scanner.nextLine();
        System.out.print("Nouveau mot de passe : "); String nouveau = scanner.nextLine();
        System.out.print("Confirmer : "); String confirm = scanner.nextLine();

        if (!nouveau.equals(confirm)) {
            System.out.println("❌ Les mots de passe ne correspondent pas.");
            return;
        }

        if (user instanceof Etudiant) {
            etudiantService.changerMotDePasse(user.getUserId(), ancien, nouveau);
        } else if (user instanceof Psychologue) {
            psychologueService.changerMotDePasse(user.getUserId(), ancien, nouveau);
        } else if (user instanceof ResponsableEtudiant) {
            responsableService.changerMotDePasse(user.getUserId(), ancien, nouveau);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════
    private static int lireInt() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Nombre valide svp : ");
            }
        }
    }
    // il faut ajouter les tetst unitaire des services
    // interface graphique et tester les services (crud)avec eux sans console mch lezem design woow adi
    //controle de saisie (champs vides /format des champs(email/tel/pass)/unicité des entites (deux users avec la meme email non))
    //jamais utiliser id dans interface graphique pas afficher id (pas utiliser la saisie d'id )
    // dans jointure n'afficher pas l'id de l'entiter il faut afficher le nom
    //utiliser tableview /listeview (la seule contrainte est l'aspet visuelle )
    //fonctionner les crud avec notre interface
    //il faut mettre l'image dans le serveur et prendre l'url mte3ha w nhotouh fel bd
    //on peut utiliser les templtes dans javafx(on peut utiliser html et css aussi) (la modif fi west tableview 5ir)(dans sceane builder fama lele image)

}
