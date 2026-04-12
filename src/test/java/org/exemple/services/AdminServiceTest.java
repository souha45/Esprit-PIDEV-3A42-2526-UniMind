package org.exemple.services;

import org.example.entities.*;
import org.example.enums.Role;
import org.example.services.*;
import org.example.utils.MyDataBase_Unimind;
import org.example.utils.ValidationUtils;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminServiceTest {

    static AdminService adminService;
    static EtudiantService etudiantService;
    static PsychologueService psychologueService;
    static ResponsableService responsableService;

    static int idEtudiantCree    = -1;
    static int idPsyCree         = -1;
    static int idResponsableCree = -1;
    static int idAdminCree       = -1;


    // SETUP / TEARDOWN

    @BeforeAll
    static void setUp() {
        System.out.println("=== Démarrage des tests UniMind ===");
        adminService       = new AdminService();
        etudiantService    = new EtudiantService();
        psychologueService = new PsychologueService();
        responsableService = new ResponsableService();
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== Fin des tests - Nettoyage ===");
        try {
            // Supprimer les profils d'abord
            supprimerProfil(idEtudiantCree);
            supprimerProfil(idPsyCree);
            supprimerProfil(idResponsableCree);
            supprimerProfil(idAdminCree);

            // Puis supprimer les utilisateurs
            if (idEtudiantCree    > 0) adminService.supprimer(idEtudiantCree);
            if (idPsyCree         > 0) adminService.supprimer(idPsyCree);
            if (idResponsableCree > 0) adminService.supprimer(idResponsableCree);
            if (idAdminCree       > 0) adminService.supprimer(idAdminCree);

            System.out.println("✓ Nettoyage terminé");
        } catch (SQLException e) {
            System.out.println("⚠ Erreur nettoyage : " + e.getMessage());
        }
    }

    // Supprimer le profil lié à un user (pour éviter la FK constraint)
    static void supprimerProfil(int userId) {
        if (userId <= 0) return;
        try {
            Connection conn = MyDataBase_Unimind.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM profil WHERE user_id = ?"
            );
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Pas de profil pour cet utilisateur, pas grave
        }
    }

    // BLOC 1 : VALIDATION

    @Test @Order(1)
    void testEmailValide() {
        System.out.println("Test 1 : Validation email");
        assertTrue(ValidationUtils.isEmailValide("test@esprit.tn"));
        assertTrue(ValidationUtils.isEmailValide("user.name@domain.co"));
        assertFalse(ValidationUtils.isEmailValide("email_invalide"));
        assertFalse(ValidationUtils.isEmailValide("@esprit.tn"));
        assertFalse(ValidationUtils.isEmailValide("user@"));
        assertFalse(ValidationUtils.isEmailValide(""));
        assertFalse(ValidationUtils.isEmailValide(null));
    }

    @Test @Order(2)
    void testPasswordValide() {
        System.out.println("Test 2 : Validation mot de passe");
        assertTrue(ValidationUtils.isPasswordValide("Esprit123"));
        assertTrue(ValidationUtils.isPasswordValide("SecurePass1"));
        assertFalse(ValidationUtils.isPasswordValide("court1A"));
        assertFalse(ValidationUtils.isPasswordValide("sansChiffre!"));
        assertFalse(ValidationUtils.isPasswordValide("sansmajuscule1"));
        assertFalse(ValidationUtils.isPasswordValide("12345678"));
        assertFalse(ValidationUtils.isPasswordValide(null));
    }

    @Test @Order(3)
    void testCinValide() {
        System.out.println("Test 3 : Validation CIN");
        assertTrue(ValidationUtils.isCinValide("12345678"));
        assertFalse(ValidationUtils.isCinValide("1234"));
        assertFalse(ValidationUtils.isCinValide("123456789"));
        assertFalse(ValidationUtils.isCinValide("abcdefgh"));
        assertFalse(ValidationUtils.isCinValide(null));
    }

    @Test @Order(4)
    void testTelephoneValide() {
        System.out.println("Test 4 : Validation téléphone");
        assertTrue(ValidationUtils.isTelephoneValide("22345678"));
        assertTrue(ValidationUtils.isTelephoneValide("55123456"));
        assertFalse(ValidationUtils.isTelephoneValide("1234"));
        assertFalse(ValidationUtils.isTelephoneValide("abc12345"));
        assertFalse(ValidationUtils.isTelephoneValide(null));
    }

    @Test @Order(5)
    void testChampNonVide() {
        System.out.println("Test 5 : Validation champ non vide");
        assertTrue(ValidationUtils.isNonVide("valeur"));
        assertTrue(ValidationUtils.isNonVide("a"));
        assertFalse(ValidationUtils.isNonVide(""));
        assertFalse(ValidationUtils.isNonVide("   "));
        assertFalse(ValidationUtils.isNonVide(null));
    }

    // BLOC 2 : INSCRIPTION

    @Test @Order(6)
    void testInscrireEtudiant() throws SQLException {
        System.out.println("Test 6 : Inscription étudiant");

        Etudiant e = new Etudiant(
                "TestNom", "EtudiantTest",
                "etudiant_test" + System.currentTimeMillis() + "@esprit.tn",
                "Test1234!", "11111111",
                "ESP001", "Esprit School", "en_attente"
        );
        String email = e.getEmail();

        assertDoesNotThrow(() -> etudiantService.inscrire(e));

        List<User> users = adminService.afficher();
        User trouve = users.stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst().orElse(null);

        assertNotNull(trouve, "L'étudiant doit exister en base");
        assertEquals("en_attente", trouve.getStatut());
        assertFalse(trouve.isActive(), "Le compte ne doit pas être actif");

        idEtudiantCree = trouve.getUserId();
    }

    @Test @Order(7)
    void testInscrirePsychologue() throws SQLException {
        System.out.println("Test 7 : Inscription psychologue");

        Psychologue p = new Psychologue(
                "TestNom", "PsyTest",
                "psy_test" + System.currentTimeMillis() + "@esprit.tn",
                "Test1234!", "22222222",
                "Psychologie clinique", "Tunis", "22345678", "en_attente"
        );
        String email = p.getEmail();

        assertDoesNotThrow(() -> psychologueService.inscrire(p));

        List<User> users = adminService.afficher();
        User trouve = users.stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst().orElse(null);

        assertNotNull(trouve, "Le psychologue doit exister en base");
        assertEquals("en_attente", trouve.getStatut());
        assertFalse(trouve.isActive());

        idPsyCree = trouve.getUserId();
    }

    @Test @Order(8)
    void testInscrireResponsable() throws SQLException {
        System.out.println("Test 8 : Inscription responsable");

        ResponsableEtudiant r = new ResponsableEtudiant(
                "TestNom", "ResponsableTest",
                "resp_test" + System.currentTimeMillis() + "@esprit.tn",
                "Test1234!", "33333333",
                "Chef de département", "Esprit", "en_attente"
        );
        String email = r.getEmail();

        assertDoesNotThrow(() -> responsableService.inscrire(r));

        List<User> users = adminService.afficher();
        User trouve = users.stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst().orElse(null);

        assertNotNull(trouve, "Le responsable doit exister en base");
        assertEquals("en_attente", trouve.getStatut());
        assertFalse(trouve.isActive());

        idResponsableCree = trouve.getUserId();
    }

    @Test @Order(9)
    void testEmailDoublon() throws SQLException {
        System.out.println("Test 9 : Email doublon (unicité)");

        List<User> users = adminService.afficher();
        if (users.isEmpty()) return;

        String emailExistant = users.get(0).getEmail();

        User doublon = new User("Doublon", "Test",
                emailExistant, "Test1234!", "44444488",
                Role.ETUDIANT, "actif");

        assertThrows(SQLException.class, () -> adminService.ajouter(doublon));

        List<User> apres = adminService.afficher();
        long count = apres.stream()
                .filter(u -> emailExistant.equals(u.getEmail()))
                .count();
        assertEquals(1, count, "Un seul utilisateur doit avoir cet email");
    }

    // BLOC 3 : CONNEXION

    @Test @Order(10)
    void testConnexionAdmin() throws SQLException {
        System.out.println("Test 10 : Connexion admin");

        // Utiliser inscrire() qui force role="Admin" correctement
        String email = "adminn" + System.currentTimeMillis() + "@esprit.tn";
        String plainPassword = "AdminPass1";

        Admin admin = new Admin(
                "AdminTest", "Test", email, plainPassword, "55555588", "actif"
        );
        adminService.inscrire(admin); // inscrire() met role="Admin" et is_active=true

        // Récupérer l'ID
        List<User> users = adminService.afficher();
        User trouve = users.stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst().orElse(null);
        assertNotNull(trouve, "L'admin créé doit exister en base");
        idAdminCree = trouve.getUserId();

        //Tenter la connexion
        User connecte = UserService.connexionGenerale(
                email, plainPassword,
                MyDataBase_Unimind.getInstance().getConnection(),
                adminService, etudiantService, psychologueService, responsableService
        );

        assertNotNull(connecte, "La connexion doit réussir");
        assertEquals(Role.ADMIN, connecte.getRole());
        assertEquals(email, connecte.getEmail());
    }

    @Test @Order(11)
    void testConnexionMauvaisMotDePasse() throws SQLException {
        System.out.println("Test 11 : Connexion avec mauvais mot de passe");

        List<User> users = adminService.afficher();
        if (users.isEmpty()) return;

        String email = users.get(0).getEmail();

        User connecte = UserService.connexionGenerale(
                email, "MauvaisMotDePasse999",
                MyDataBase_Unimind.getInstance().getConnection(),
                adminService, etudiantService, psychologueService, responsableService
        );

        assertNull(connecte, "La connexion doit échouer avec un mauvais mot de passe");
    }

    @Test @Order(12)
    void testConnexionEmailInexistant() throws SQLException {
        System.out.println("Test 12 : Connexion avec email inexistant");

        User connecte = UserService.connexionGenerale(
                "email_inexistant_xyz_999@esprit.tn", "Test1234!",
                MyDataBase_Unimind.getInstance().getConnection(),
                adminService, etudiantService, psychologueService, responsableService
        );

        assertNull(connecte, "La connexion doit échouer avec un email inexistant");
    }

    // BLOC 4 : CRUD ADMIN

    @Test @Order(13)
    void testAjouterUtilisateur() throws SQLException {
        System.out.println("Test 13 : Ajouter utilisateur (admin)");

        String email = "ajout_test_" + System.currentTimeMillis() + "@esprit.tn";
        User user = new User("AjoutTest", "Admin",
                email, "Test1234!", "66666666", Role.ETUDIANT, "actif");
        user.setActive(true);
        user.setVerified(true);

        int taille = adminService.afficher().size();
        assertDoesNotThrow(() -> adminService.ajouter(user));

        List<User> apres = adminService.afficher();
        assertEquals(taille + 1, apres.size(), "La liste doit avoir un utilisateur de plus");

        User trouve = apres.stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst().orElse(null);
        assertNotNull(trouve);
        assertEquals("AjoutTest", trouve.getNom());

        // Nettoyage immédiat
        adminService.supprimer(trouve.getUserId());
    }

    @Test @Order(14)
    void testAfficherUtilisateurs() throws SQLException {
        System.out.println("Test 14 : Afficher tous les utilisateurs");

        List<User> users = adminService.afficher();
        assertNotNull(users, "La liste ne doit pas être null");
        assertFalse(users.isEmpty(), "La liste ne doit pas être vide");

        System.out.println("   → " + users.size() + " utilisateur(s) trouvé(s)");
    }

    @Test @Order(15)
    void testModifierUtilisateur() throws SQLException {
        System.out.println("Test 15 : Modifier un utilisateur");

        List<User> users = adminService.afficher();
        assertFalse(users.isEmpty());

        User lastUser = users.get(users.size() - 1);
        String ancienNom = lastUser.getNom();

        lastUser.setNom("NomModifie");
        lastUser.setPrenom("PrenomModifie");
        assertDoesNotThrow(() -> adminService.modifier(lastUser));

        List<User> apres = adminService.afficher();
        User modifie = apres.stream()
                .filter(u -> u.getUserId() == lastUser.getUserId())
                .findFirst().orElse(null);

        assertNotNull(modifie);
        assertEquals("NomModifie", modifie.getNom());
        assertNotEquals(ancienNom, modifie.getNom());

        // Remettre l'ancien nom
        modifie.setNom(ancienNom);
        adminService.modifier(modifie);
    }

    @Test @Order(16)
    void testBloquerEtDebloquerUtilisateur() throws SQLException {
        System.out.println("Test 16 : Bloquer et débloquer un utilisateur");

        if (idEtudiantCree == -1) return;

        // Bloquer
        assertDoesNotThrow(() -> adminService.bloquer(idEtudiantCree));
        List<User> apres = adminService.afficher();
        User bloque = apres.stream()
                .filter(u -> u.getUserId() == idEtudiantCree)
                .findFirst().orElse(null);
        assertNotNull(bloque);
        assertFalse(bloque.isActive(), "Le compte doit être bloqué");
        assertEquals("inactif", bloque.getStatut());

        // Débloquer
        assertDoesNotThrow(() -> adminService.debloquer(idEtudiantCree));
        List<User> apresDeblocage = adminService.afficher();
        User debloque = apresDeblocage.stream()
                .filter(u -> u.getUserId() == idEtudiantCree)
                .findFirst().orElse(null);
        assertNotNull(debloque);
        assertTrue(debloque.isActive(), "Le compte doit être débloqué");
        assertEquals("actif", debloque.getStatut());
    }

    @Test @Order(17)
    void testAccepterDemande() throws SQLException {
        System.out.println("Test 17 : Accepter une demande d'inscription");

        if (idPsyCree == -1) return;

        assertDoesNotThrow(() -> adminService.accepterDemande(idPsyCree));

        List<User> users = adminService.afficher();
        User accepte = users.stream()
                .filter(u -> u.getUserId() == idPsyCree)
                .findFirst().orElse(null);

        assertNotNull(accepte);
        assertEquals("actif", accepte.getStatut());
        assertTrue(accepte.isActive());
        assertTrue(accepte.isVerified());
    }

    @Test @Order(18)
    void testRefuserDemande() throws SQLException {
        System.out.println("Test 18 : Refuser une demande d'inscription");

        if (idResponsableCree == -1) return;

        assertDoesNotThrow(() -> adminService.refuserDemande(idResponsableCree));

        List<User> users = adminService.afficher();
        User refuse = users.stream()
                .filter(u -> u.getUserId() == idResponsableCree)
                .findFirst().orElse(null);

        assertNotNull(refuse);
        assertEquals("rejeté", refuse.getStatut());
        assertFalse(refuse.isActive());
    }

    @Test @Order(19)
    void testAfficherDemandesEnAttente() throws SQLException {
        System.out.println("Test 19 : Afficher les demandes en attente");

        List<User> demandes = adminService.afficherDemandesEnAttente();
        assertNotNull(demandes, "La liste des demandes ne doit pas être null");

        boolean toutesEnAttente = demandes.stream()
                .allMatch(u -> "en_attente".equals(u.getStatut()));
        assertTrue(toutesEnAttente, "Toutes les demandes doivent être en attente");

        System.out.println("   → " + demandes.size() + " demande(s) en attente");
    }

    @Test @Order(20)
    void testSupprimerUtilisateur() throws SQLException {
        System.out.println("Test 20 : Supprimer un utilisateur");

        String email = "temp_suppr_" + System.currentTimeMillis() + "@esprit.tn";
        User temp = new User("TempSuppr", "Test",
                email, "Test1234!", "77777777", Role.ETUDIANT, "actif");
        temp.setActive(true);
        temp.setVerified(true);

        adminService.ajouter(temp);

        List<User> avant = adminService.afficher();
        int idTemp = avant.stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst().map(User::getUserId).orElse(-1);
        assertTrue(idTemp > 0, "L'utilisateur temporaire doit exister");

        assertDoesNotThrow(() -> adminService.supprimer(idTemp));

        List<User> apres = adminService.afficher();
        boolean existeEncore = apres.stream().anyMatch(u -> u.getUserId() == idTemp);
        assertFalse(existeEncore, "L'utilisateur doit être supprimé");
    }


    // BLOC 5 : PROFIL ET MOT DE PASSE


    @Test @Order(21)
    void testModifierProfilEtudiant() throws SQLException {
        System.out.println("Test 21 : Modifier profil étudiant");

        if (idEtudiantCree == -1) return;

        Profil profil = new Profil();
        profil.setUserId(idEtudiantCree);
        profil.setBio("Bio de test pour étudiant");
        profil.setTel("22345678");
        profil.setNiveau("3ème année");
        profil.setFiliere("Informatique");
        profil.setPseudo("etudiant_test");

        assertDoesNotThrow(() -> etudiantService.modifierProfil(profil));
    }

    @Test @Order(22)
    void testChangerMotDePasseEtudiant() throws SQLException {
        System.out.println("Test 22 : Changer mot de passe étudiant");

        if (idEtudiantCree == -1) return;

        assertDoesNotThrow(() ->
                etudiantService.changerMotDePasse(idEtudiantCree, "Test1234!", "NouveauMdp1")
        );

        // Remettre l'ancien
        assertDoesNotThrow(() ->
                etudiantService.changerMotDePasse(idEtudiantCree, "NouveauMdp1", "Test1234!")
        );
    }

    @Test @Order(23)
    void testChangerMotDePasseAncienIncorrect() throws SQLException {
        System.out.println("Test 23 : Changer mot de passe avec ancien mdp incorrect");

        if (idEtudiantCree == -1) return;

        // Tentative avec mauvais ancien mdp — ne doit pas lever d'exception
        assertDoesNotThrow(() ->
                etudiantService.changerMotDePasse(idEtudiantCree, "MauvaisAncien!", "NouveauMdp1")
        );

        // Vérifier que l'ancien mot de passe fonctionne encore
        User etudiant = adminService.rechercherParId(idEtudiantCree);
        assertNotNull(etudiant);

        User connecte = UserService.connexionGenerale(
                etudiant.getEmail(), "Test1234!",
                MyDataBase_Unimind.getInstance().getConnection(),
                adminService, etudiantService, psychologueService, responsableService
        );
        assertNotNull(connecte, "L'ancien mot de passe doit toujours fonctionner");
    }
}