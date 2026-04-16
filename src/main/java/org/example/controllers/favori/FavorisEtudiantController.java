package org.example.controllers.favori;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import org.example.utils.NavigationContext;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavorisEtudiantController {

    @FXML
    private TilePane tileFavoris;

    @FXML
    private Label lblTotal;

    @FXML
    private TextField txtRecherche;

    @FXML
    private ComboBox<TypeEvenement> comboType;

    @FXML
    private ComboBox<StatutEvenement> comboStatut;

    @FXML
    private ComboBox<String> comboTri;

    private FavoriService favoriService;
    private EvenementService evenementService;
    private List<Evenement> listeFavoris;
    private Set<Integer> favoriEvenementIds;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        System.out.println("Initialisation de FavorisEtudiantController");
        try {
            favoriService = new FavoriService();
            evenementService = new EvenementService();
            favoriEvenementIds = new HashSet<>();

            // Initialiser le ComboBox de tri
            comboTri.setItems(FXCollections.observableArrayList(
                "Date (plus proche)",
                "Date (plus lointain)"
            ));
            comboTri.setValue("Date (plus proche)");

            // Initialiser le ComboBox de type
            comboType.setItems(FXCollections.observableArrayList(TypeEvenement.values()));
            comboType.setValue(null);
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
                    setText(empty || item == null ? null : item.name());
                }
            });

            // Initialiser le ComboBox de statut
            comboStatut.setItems(FXCollections.observableArrayList(StatutEvenement.values()));
            comboStatut.setValue(null);
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
                    setText(empty || item == null ? null : item.name());
                }
            });

            chargerFavorisEtudiant();
            chargerEvenementsFavoris();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
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

    private void chargerEvenementsFavoris() {
        System.out.println("Chargement des événements favoris...");
        try {
            listeFavoris = new ArrayList<>();
            List<Evenement> tousEvenements = evenementService.afficher();

            // Filtrer pour ne garder que les favoris
            for (Evenement e : tousEvenements) {
                if (favoriEvenementIds.contains(e.getEvenementId())) {
                    listeFavoris.add(e);
                }
            }

            System.out.println("Nombre d'événements favoris chargés: " + listeFavoris.size());

            // Appliquer les filtres initiaux
            appliquerFiltres();
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement des favoris: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du chargement des favoris: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void appliquerFiltres() {
        List<Evenement> filtres = new ArrayList<>(listeFavoris);

        // Filtrer par recherche
        String recherche = txtRecherche.getText();
        if (recherche != null && !recherche.trim().isEmpty()) {
            String rechercheLower = recherche.toLowerCase();
            filtres.removeIf(e -> !e.getTitre().toLowerCase().contains(rechercheLower));
        }

        // Filtrer par type
        TypeEvenement type = comboType.getValue();
        if (type != null) {
            filtres.removeIf(e -> e.getType() != type);
        }

        // Filtrer par statut
        StatutEvenement statut = comboStatut.getValue();
        if (statut != null) {
            filtres.removeIf(e -> e.getStatut() != statut);
        }

        // Trier par date
        String tri = comboTri.getValue();
        if (tri != null) {
            if (tri.equals("Date (plus proche)")) {
                filtres.sort(Comparator.comparing(e -> e.getDateDebut().toLocalDateTime()));
            } else if (tri.equals("Date (plus lointain)")) {
                filtres.sort((e1, e2) -> e2.getDateDebut().toLocalDateTime().compareTo(e1.getDateDebut().toLocalDateTime()));
            }
        }

        // Afficher les résultats
        if (filtres.isEmpty()) {
            afficherMessageAucunFavori();
            lblTotal.setText("0 favoris");
        } else {
            afficherCartesFavoris(filtres);
            lblTotal.setText(filtres.size() + " favoris");
        }
    }

    @FXML
    private void reinitialiserFiltres(ActionEvent event) {
        txtRecherche.clear();
        comboType.setValue(null);
        comboStatut.setValue(null);
        comboTri.setValue("Date (plus proche)");
        appliquerFiltres();
    }

    private void afficherMessageAucunFavori() {
        tileFavoris.getChildren().clear();
        Label lblMessage = new Label("❤️ Aucun favori");
        lblMessage.setStyle("-fx-font-size: 20px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        tileFavoris.getChildren().add(lblMessage);
    }

    private void afficherCartesFavoris(List<Evenement> evenements) {
        System.out.println("Affichage des cartes de favoris...");
        tileFavoris.getChildren().clear();

        for (Evenement e : evenements) {
            try {
                System.out.println("Création de la carte pour le favori: " + e.getTitre());
                VBox carteFavori = creerCarteFavori(e);
                tileFavoris.getChildren().add(carteFavori);
                System.out.println("Carte ajoutée avec succès");
            } catch (Exception ex) {
                System.err.println("Erreur lors de la création de la carte pour le favori " + e.getTitre() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        System.out.println("Nombre de cartes dans le TilePane: " + tileFavoris.getChildren().size());
    }

    private VBox creerCarteFavori(Evenement evenement) {
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
                String encodedImageName = URLEncoder.encode(evenement.getImage().trim(), StandardCharsets.UTF_8);
                String fullImageUrl = "http://localhost/uploadsEvent/evenements/" + encodedImageName;
                Image image = new Image(fullImageUrl, true);
                imageView.setImage(image);
            } catch (Exception ex) {
                imageView.setImage(null);
            }
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

        // Boutons
        Button btnVoir = new Button("Voir le détail");
        btnVoir.setStyle("-fx-background-color: transparent; -fx-text-fill: #6366f1; -fx-border-color: #6366f1; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold;");
        btnVoir.setOnAction(event -> voirEvenement(evenement));

        Button btnSupprimerFavori = new Button("Retirer des favoris");
        btnSupprimerFavori.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold;");
        btnSupprimerFavori.setOnAction(evt -> supprimerFavori(evenement));

        // Contenu de la carte
        VBox contenu = new VBox(8);
        contenu.setPadding(new Insets(15));
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, btnVoir, btnSupprimerFavori);
        contenu.getChildren().addAll(lblStatut, lblTitre, lblDate, lblLieu, lblPlacesLibres, actions);

        carte.getChildren().addAll(imageContainer, contenu);

        // Clic sur la carte pour voir les détails
        carte.setOnMouseClicked(event -> voirEvenement(evenement));

        return carte;
    }

    private void supprimerFavori(Evenement evenement) {
        try {
            int etudiantId = SessionManager.getInstance().getCurrentUserId().orElse(-1);
            if (etudiantId <= 0) {
                return;
            }

            int favoriId = trouverFavoriId(evenement.getEvenementId(), etudiantId);
            if (favoriId > 0) {
                favoriService.supprimer(favoriId);
                favoriEvenementIds.remove(evenement.getEvenementId());

                // Retirer de la liste locale et réappliquer les filtres
                listeFavoris.removeIf(e -> e.getEvenementId() == evenement.getEvenementId());
                appliquerFiltres();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du favori: " + e.getMessage());
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
            org.example.controllers.evenement.VoirEvenementController controller = loader.getController();
            controller.setEvenement(evenement);
            controller.setPagePrecedente("/favori/FavorisEtudiant.fxml"); // Retour vers les favoris

            NavigationContext.loadContentInCenter(root);
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir l'écran de détails: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Impossible d'afficher l'événement: " + e.getMessage());
        }
    }
}
