package org.example;

import org.example.entities.*;
import org.example.enums.NiveauMeditation;
import org.example.services.*;
import org.example.utils.MyDataBase_Unimind;

import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Établir la connexion à la base de données
        /*MyDataBase_Unimind db = MyDataBase_Unimind.getInstance();

        // Vérifier si la connexion est établie
        if (db.getConnection() != null) {
            System.out.println("✓ Connexion à la base de données réussie !");
        } else {
            System.out.println("✗ Échec de la connexion à la base de données.");
        }*/

        CategorieMeditationServices cat = new CategorieMeditationServices();
        try {
            //cat.ajouter(new CategorieMeditation("test13", "testt","test1"));
            //cat.supprimer(34);
            CategorieMeditation cat1 = new CategorieMeditation();
            cat1.setCategorieId(33);          // ID de la catégorie à modifier
            cat1.setNom("Nouveau nom");
            cat1.setDescription("Description mise à jour");
            cat1.setIconUrl("nouvelle_icone.png");
            //cat.modifier(cat1);
            System.out.println(cat.afficher());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ========== 2. Test SeanceMeditation ==========
        SeanceMeditationServices seanceService = new SeanceMeditationServices();
        try {
                // 2.1 Ajouter une séance
                /*seanceService.ajouter(new SeanceMeditation(
                                   "Méditation guidée", "Séance de relaxation profonde",
                                   "fichier_audio.mp3", org.example.enums.TypeFichier.audio,
                                   15, true, NiveauMeditation.débutant, 33));*/

                // 2.2 Modifier une séance (supposons ID = 1)
                SeanceMeditation seanceModif = new SeanceMeditation();
                seanceModif.setSeanceId(17);
                seanceModif.setTitre("Titre modifié");
                seanceModif.setDescription("Description modifiée");
                seanceModif.setFichier("nouveau_fichier.mp3");
                seanceModif.setTypeFichier(org.example.enums.TypeFichier.video);
                seanceModif.setDuree(20);
                seanceModif.setIsActive(false);
                seanceModif.setNiveau(NiveauMeditation.intermediaire);
                seanceModif.setCategorieId(33);
                //seanceService.modifier(seanceModif);

            // 2.4 Supprimer une séance
            //seanceService.supprimer(17);

                // 2.3 Afficher toutes les séances
                System.out.println("\n=== Liste des séances ===");
                System.out.println(seanceService.afficher());


    } catch (SQLException e) {
            System.out.println("Erreur SeanceMeditation : " + e.getMessage());
        }

        // ========== 3. Test FavoriSeance ==========
        FavoriSeanceServices favoriService = new FavoriSeanceServices();
        try {
            // Supposons un utilisateur
            int userId = 2;
            int seanceId = 16;

            // 3.1 Vérifier si déjà favori
            boolean estFavori = favoriService.isFavori(userId, seanceId);
            System.out.println("\n=== Favori (userId=" + userId + ", seanceId=" + seanceId + ") ===");
            System.out.println("Est favori ? " + estFavori);

            // 3.2 Ajouter un favori
            if (!estFavori) {
                FavoriSeance nouveauFavori = new FavoriSeance(userId, seanceId);
                //favoriService.ajouter(nouveauFavori);
                System.out.println("Ajout du favori");
            }

            // 3.3 Afficher les favoris de l'utilisateur
            List<FavoriSeance> favorisUser = favoriService.getFavorisByUser(userId);
            //System.out.println("Favoris de l'utilisateur " + userId + " : " + favorisUser);

            // 3.4 Supprimer un favori (par son ID)
            //favoriService.supprimer(5);
        } catch (SQLException e) {
            System.out.println("Erreur FavoriSeance : " + e.getMessage());
        }

        // ========== 4. Test Post ==========
        PostServices postService = new PostServices();
        try {
            // Récupérer une catégorie existante et un utilisateur (ID 2)
            List<CategorieMeditation> categories = cat.afficher();
            if (!categories.isEmpty()) {
                int catId = categories.get(0).getCategorieId();
                int userId = 2;

                // 4.1 Ajouter un post
                Post nouveauPost = new Post("Mon premier post", "Contenu intéressant", false, userId, catId);
                //postService.ajouter(nouveauPost);

                // 4.2 Afficher tous les posts
                System.out.println("\n=== Tous les posts ===");
                System.out.println(postService.afficher());

                // 4.3 Afficher les posts d'une catégorie
                System.out.println("Posts de la catégorie " + catId + " : ");
                System.out.println(postService.getPostsByCategorie(catId));

                // 4.4 Modifier un post
                Post postModif = new Post();
                postModif.setPostId(28);
                postModif.setTitre("Nouveau titre");
                postModif.setContenu("Contenu modifié");
                postModif.setIsAnonyme(true);
                postModif.setUserId(userId);
                postModif.setCategorieId(catId);
                //postService.modifier(postModif);

                // 4.5 Supprimer un post (ID = 28)
                //postService.supprimer(28);
            }
        } catch (SQLException e) {
            System.out.println("Erreur Post : " + e.getMessage());
        }

        // ========== 5. Test Commentaire ==========
        CommentaireServices commentaireService = new CommentaireServices();
        try {
            // Supposons un post existant (ID = 1) et un utilisateur (ID = 1)
            int postId = 26;
            int userId = 2;

            // 5.1 Ajouter un commentaire
            Commentaire nouveauCommentaire = new Commentaire("Très utile, merci !", false, userId, postId);
            //commentaireService.ajouter(nouveauCommentaire);

            // 5.2 Afficher les commentaires d'un post
            System.out.println("\n=== Commentaires du post " + postId + " ===");
            List<Commentaire> commentaires = commentaireService.getCommentairesByPost(postId);
            for (Commentaire c : commentaires) {
                System.out.println(c);
            }

            // 5.3 Modifier un commentaire (ID = 1)
            Commentaire comModif = new Commentaire();
            comModif.setCommentaireId(64);
            comModif.setContenu("Commentaire mis à jour");
            comModif.setIsAnonyme(true);
            comModif.setUserId(userId);
            comModif.setPostId(postId);
            //commentaireService.modifier(comModif);

            // 5.4 Supprimer un commentaire (ID = 5)
            //commentaireService.supprimer(64);
        } catch (SQLException e) {
            System.out.println("Erreur Commentaire : " + e.getMessage());
        }
    }
}