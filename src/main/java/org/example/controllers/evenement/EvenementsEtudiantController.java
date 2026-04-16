package org.example.controllers.evenement;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.example.entities.Evenement;
import org.example.entities.Favori;
import org.example.enums.StatutEvenement;
import org.example.enums.TypeEvenement;
import org.example.services.EvenementService;
import org.example.services.FavoriService;
import org.example.services.ParticipationService;
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvenementsEtudiantController {

    @FXML
    private TilePane tileEvenements;

    @FXML
    private TextField txtRecherche;

    @FXML
    private ComboBox<TypeEvenement> comboType;

    @FXML
    private ComboBox<StatutEvenement> comboStatut;

    @FXML
    private DatePicker dateDu;

    @FXML
    private DatePicker dateAu;

    @FXML
    private Label lblTotal;

    private EvenementService evenementService;
    private FavoriService favoriService;
    private ParticipationService participationService;
    private List<Evenement> listeEvenements;
    private Set<Integer> favoriEvenementIds;
    private Set<Integer> participationEvenementIds;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        System.out.println("Initialisation de EvenementsEtudiantController");
        try {
            evenementService = new EvenementService();
            favoriService = new FavoriService();
            participationService = new ParticipationService();
            favoriEvenementIds = new HashSet<>();
            participationEvenementIds = new HashSet<>();
            System.out.println("EvenementService créé avec succès");

            initialiserFiltres();
            chargerFavorisEtudiant();
            chargerParticipationsEtudiant();
            chargerEvenements();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
            afficherErreur("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    @FXML
    private void appliquerFiltres(ActionEvent event) {
        if (listeEvenements == null) {
            return;
        }

        List<Evenement> resultats = filtrerEvenements(listeEvenements);
        if (resultats.isEmpty()) {
            afficherMessageAucunEvenement();
            lblTotal.setText("0 événements (filtrés)");
        } else {
            afficherCartesEvenements(resultats);
            lblTotal.setText(resultats.size() + " événements (filtrés)");
        }
    }

    @FXML
    private void reinitialiserFiltres(ActionEvent event) {
        txtRecherche.clear();
        comboType.setValue(null);
        comboStatut.setValue(null);
        dateDu.setValue(null);
        dateAu.setValue(null);

        if (listeEvenements == null) {
            return;
        }

        afficherCartesEvenements(listeEvenements);
        lblTotal.setText(listeEvenements.size() + " événements");
    }

    private List<Evenement> filtrerEvenements(List<Evenement> base) {
        String q = txtRecherche.getText() != null ? txtRecherche.getText().trim().toLowerCase() : "";
        TypeEvenement type = comboType.getValue();
        StatutEvenement statut = comboStatut.getValue();
        var du = dateDu.getValue();
        var au = dateAu.getValue();

        return base.stream().filter(e -> {
            if (e == null) {
                return false;
            }

            if (!q.isEmpty()) {
                String titre = e.getTitre() != null ? e.getTitre().toLowerCase() : "";
                String desc = e.getDescription() != null ? e.getDescription().toLowerCase() : "";
                String lieu = e.getLieu() != null ? e.getLieu().toLowerCase() : "";
                if (!titre.contains(q) && !desc.contains(q) && !lieu.contains(q)) {
                    return false;
                }
            }

            if (type != null && e.getType() != type) {
                return false;
            }

            if (statut != null && e.getStatut() != statut) {
                return false;
            }

            if (e.getDateDebut() != null) {
                var d = e.getDateDebut().toLocalDateTime().toLocalDate();
                if (du != null && d.isBefore(du)) {
                    return false;
                }
                if (au != null && d.isAfter(au)) {
                    return false;
                }
            }

            return true;
        }).toList();
    }

    private void initialiserFiltres() {
        comboType.getItems().setAll(TypeEvenement.values());
        comboStatut.getItems().setAll(StatutEvenement.values());

        comboType.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TypeEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        comboType.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TypeEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous" : item.name());
            }
        });

        comboStatut.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StatutEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        comboStatut.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StatutEvenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous" : item.name());
            }
        });
    }

    private void chargerFavorisEtudiant() {
        try {
            int etudiantId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (etudiantId <= 0) {
                return;
            }

            favoriEvenementIds.clear();
            for (Favori f : favoriService.afficher()) {
                if (f.getEtudiantId() == etudiantId) {
                    favoriEvenementIds.add(f.getEvenementId());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des favoris: " + e.getMessage());
        }
    }

    private void chargerParticipationsEtudiant() {
        try {
            int etudiantId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (etudiantId <= 0) {
                return;
            }

            participationEvenementIds.clear();
            for (org.example.entities.Participation p : participationService.afficher()) {
                if (p.getEtudiantId() == etudiantId) {
                    participationEvenementIds.add(p.getEvenementId());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des participations: " + e.getMessage());
        }
    }

    private void chargerEvenements() {
        System.out.println("Chargement des événements...");
        try {
            listeEvenements = evenementService.afficher();
            System.out.println("Nombre d'événements chargés: " + listeEvenements.size());

            if (listeEvenements.isEmpty()) {
                System.out.println("Aucun événement trouvé");
                afficherMessageAucunEvenement();
                lblTotal.setText("0 événements");
            } else {
                afficherCartesEvenements(listeEvenements);
                lblTotal.setText(listeEvenements.size() + " événements");
            }
            System.out.println("Événements affichés avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement des événements: " + e.getMessage());
            e.printStackTrace();
            afficherErreur("Impossible de charger les événements: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du chargement des événements: " + e.getMessage());
            e.printStackTrace();
            afficherErreur("Erreur inattendue: " + e.getMessage());
        }
    }

    private void afficherMessageAucunEvenement() {
        tileEvenements.getChildren().clear();
        Label lblMessage = new Label("🎪 Aucun événement disponible");
        lblMessage.setStyle("-fx-font-size: 20px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        tileEvenements.getChildren().add(lblMessage);
    }

    private void afficherCartesEvenements(List<Evenement> evenements) {
        System.out.println("Affichage des cartes d'événements...");
        tileEvenements.getChildren().clear();

        for (Evenement e : evenements) {
            try {
                System.out.println("Création de la carte pour l'événement: " + e.getTitre());
                VBox carteEvenement = creerCarteEvenement(e);
                tileEvenements.getChildren().add(carteEvenement);
                System.out.println("Carte ajoutée avec succès");
            } catch (Exception ex) {
                System.err.println("Erreur lors de la création de la carte pour l'événement " + e.getTitre() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        System.out.println("Nombre de cartes dans le TilePane: " + tileEvenements.getChildren().size());
    }

    private VBox creerCarteEvenement(Evenement evenement) {
        VBox carte = new VBox();
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;");
        carte.setPrefWidth(280);
        carte.setSpacing(10);

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setFitWidth(280);
        imageView.setPreserveRatio(false);

        if (evenement.getImage() != null && !evenement.getImage().trim().isEmpty()) {
            try {
                String encodedImageName = java.net.URLEncoder.encode(evenement.getImage().trim(), java.nio.charset.StandardCharsets.UTF_8);
                String fullImageUrl = "http://localhost/uploadsEvent/evenements/" + encodedImageName;
                Image image = new Image(fullImageUrl, true);
                imageView.setImage(image);
            } catch (Exception ex) {
                imageView.setImage(null);
            }
        }

        if (imageView.getImage() == null) {
            Label lblPasImage = new Label("🎪");
            lblPasImage.setStyle("-fx-font-size: 60px; -fx-alignment: center;");
            imageView.setImage(null);
        }

        // Conteneur pour l'image ou le placeholder
        VBox imageContainer = new VBox();
        imageContainer.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15 15 0 0;");
        imageContainer.setPrefHeight(150);
        imageContainer.setAlignment(Pos.CENTER);

        if (imageView.getImage() != null) {
            imageContainer.getChildren().add(imageView);
        } else {
            imageContainer.getChildren().add(new Label("🎪"));
            ((Label) imageContainer.getChildren().get(0)).setStyle("-fx-font-size: 60px;");
        }

        // Badge de statut
        Label lblStatut = new Label();
        lblStatut.setText(evenement.getStatut().toString());
        lblStatut.setStyle("-fx-background-color: " + getCouleurStatut(evenement.getStatut().toString()) + "; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 10; -fx-font-size: 11px;");

        // Titre
        Label lblTitre = new Label(evenement.getTitre());
        lblTitre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        lblTitre.setMaxWidth(260);

        // Date et lieu
        Label lblDate = new Label("📅 " + evenement.getDateDebut().toLocalDateTime().format(dateFormatter));
        lblDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label lblLieu = new Label("📍 " + evenement.getLieu());
        lblLieu.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        // Places libres
        int placesLibres = calculerPlacesLibres(evenement);
        Label lblPlacesLibres = new Label();
        if (placesLibres == -1) {
            lblPlacesLibres.setText("♾️ Illimité");
            lblPlacesLibres.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (placesLibres > 0) {
            lblPlacesLibres.setText("🎟️ " + placesLibres + " places libres");
            lblPlacesLibres.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            lblPlacesLibres.setText("❌ Complet");
            lblPlacesLibres.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }

        // Boutons style Symfony (détail + coeur + participer)
        Button btnVoir = new Button("Voir le détail");
        btnVoir.setStyle("-fx-background-color: transparent; -fx-text-fill: #6366f1; -fx-border-color: #6366f1; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold;");
        btnVoir.setOnAction(event -> voirEvenement(evenement));

        Button btnFavori = new Button();
        boolean isFavori = favoriEvenementIds != null && favoriEvenementIds.contains(evenement.getEvenementId());
        btnFavori.setText(isFavori ? "♥" : "♡");
        btnFavori.setStyle(isFavori
                ? "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;"
                : "-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-border-color: #ef4444; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;"
        );
        btnFavori.setOnAction(evt -> toggleFavori(evenement, btnFavori));

        // Contenu de la carte
        VBox contenu = new VBox(8);
        contenu.setPadding(new Insets(15));

        // Bouton Participer ou label Inscrit
        boolean estInscrit = participationEvenementIds != null && participationEvenementIds.contains(evenement.getEvenementId());
        if (estInscrit) {
            Label lblInscrit = new Label("✅ Inscrit");
            lblInscrit.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 14;");
            javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, btnVoir, lblInscrit, btnFavori);
            contenu.getChildren().addAll(lblStatut, lblTitre, lblDate, lblLieu, lblPlacesLibres, actions);
        } else {
            Button btnParticiper = new Button("🎯 Participer");
            btnParticiper.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;");
            btnParticiper.setOnAction(evt -> participer(evenement, btnParticiper));
            javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, btnVoir, btnParticiper, btnFavori);
            contenu.getChildren().addAll(lblStatut, lblTitre, lblDate, lblLieu, lblPlacesLibres, actions);
        }

        carte.getChildren().addAll(imageContainer, contenu);

        // Clic sur la carte pour voir les détails
        carte.setOnMouseClicked(event -> voirEvenement(evenement));

        return carte;
    }

    private void toggleFavori(Evenement evenement, Button btnFavori) {
        try {
            int etudiantId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (etudiantId <= 0) {
                return;
            }

            boolean deja = favoriEvenementIds != null && favoriEvenementIds.contains(evenement.getEvenementId());
            if (deja) {
                // Supprimer : retrouver l'id du favori pour cet event/etudiant
                int favoriId = trouverFavoriId(evenement.getEvenementId(), etudiantId);
                if (favoriId > 0) {
                    favoriService.supprimer(favoriId);
                }
                favoriEvenementIds.remove(evenement.getEvenementId());
                btnFavori.setText("♡");
                btnFavori.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-border-color: #ef4444; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;");
            } else {
                favoriService.ajouter(new Favori(evenement.getEvenementId(), etudiantId));
                favoriEvenementIds.add(evenement.getEvenementId());
                btnFavori.setText("♥");
                btnFavori.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-weight: bold;");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du toggle favori: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void participer(Evenement evenement, Button btnParticiper) {
        try {
            int etudiantId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (etudiantId <= 0) {
                return;
            }

            // Vérifier si déjà inscrit
            if (participationEvenementIds != null && participationEvenementIds.contains(evenement.getEvenementId())) {
                return;
            }

            // Créer la participation
            org.example.entities.Participation participation = new org.example.entities.Participation();
            participation.setEvenementId(evenement.getEvenementId());
            participation.setEtudiantId(etudiantId);
            participation.setStatut(org.example.enums.StatutParticipation.CONFIRME);
            participation.setDateInscription(new java.sql.Timestamp(System.currentTimeMillis()));

            participationService.ajouter(participation);
            participationEvenementIds.add(evenement.getEvenementId());

            // Recharger les cartes
            chargerEvenements();

        } catch (SQLException e) {
            System.err.println("Erreur lors de la participation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int trouverFavoriId(int evenementId, int etudiantId) throws SQLException {
        for (Favori f : favoriService.afficher()) {
            if (f.getEvenementId() == evenementId && f.getEtudiantId() == etudiantId) {
                return f.getId();
            }
        }
        return -1;
    }

    private String getCouleurStatut(String statut) {
        switch (statut.toUpperCase()) {
            case "OUVERT":
                return "#27ae60";
            case "FERME":
                return "#e74c3c";
            case "EN_ATTENTE":
                return "#f39c12";
            case "ANNULE":
                return "#95a5a6";
            default:
                return "#3498db";
        }
    }

    private int calculerPlacesLibres(Evenement evenement) throws RuntimeException {
        try {
            int capaciteMax = evenement.getCapaciteMax();
            int nombreInscrits = compterParticipations(evenement.getEvenementId());
            return capaciteMax > 0 ? capaciteMax - nombreInscrits : -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int compterParticipations(int evenementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM participation WHERE evenement_id = ?";
        try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void voirEvenement(Evenement evenement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/evenement/VoirEvenement.fxml"));
            Parent root = loader.load();
            VoirEvenementController controller = loader.getController();
            controller.setEvenement(evenement);
            controller.setPagePrecedente("/evenement/EvenementsEtudiant.fxml"); // Retour vers les événements

            NavigationContext.loadContentInCenter(root);
        } catch (IOException e) {
            afficherErreur("Impossible d'ouvrir l'écran de détails: " + e.getMessage());
        } catch (Exception e) {
            afficherErreur("Impossible d'afficher l'événement: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherEvenements(ActionEvent event) {
        appliquerFiltres(event);
    }

    private void afficherErreur(String message) {
        System.err.println("Erreur: " + message);
    }
}
