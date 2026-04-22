package org.example.controllers.feedback;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.enums.TypeEvenement;
import org.example.services.EvenementService;
import org.example.services.ParticipationService;
import org.example.utils.NavigationContext;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FeedbacksAdminController {

    @FXML
    private TilePane tileFeedbacks;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblNoteMoyenne;

    @FXML
    private Label lblTopEvenement;

    // Filtres
    @FXML
    private ComboBox<TypeEvenement> comboTypeEvenement;

    @FXML
    private ComboBox<String> comboResponsable;

    @FXML
    private DatePicker dateDebut;

    @FXML
    private DatePicker dateFin;

    @FXML
    private ComboBox<String> comboTri;

    private ParticipationService participationService;
    private EvenementService evenementService;
    private List<Participation> listeFeedbacks;
    private List<Participation> listeFiltree;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        System.out.println("Initialisation de FeedbacksAdminController");
        try {
            participationService = new ParticipationService();
            evenementService = new EvenementService();
            listeFeedbacks = new ArrayList<>();
            listeFiltree = new ArrayList<>();
            
            initialiserFiltres();
            chargerFeedbacks();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initialiserFiltres() {
        // Initialiser le combo des types d'événement
        comboTypeEvenement.getItems().clear();
        comboTypeEvenement.getItems().add(null);
        comboTypeEvenement.getItems().addAll(TypeEvenement.values());
        comboTypeEvenement.setValue(null);

        // Initialiser le combo des responsables
        comboResponsable.getItems().clear();
        comboResponsable.getItems().add(null);
        chargerResponsables();

        // Initialiser le combo de tri
        comboTri.getItems().addAll("Pertinence", "Date récente", "Date ancienne", "Note élevée", "Note faible", "Alphabétique");
        comboTri.setValue("Pertinence");
    }

    private void chargerResponsables() {
        try {
            // D'abord, essayer sans filtre de rôle pour voir si le problème vient de là
            String sql = "SELECT DISTINCT u.user_id, u.nom, u.prenom, u.role " +
                        "FROM user u " +
                        "INNER JOIN evenement e ON u.user_id = e.organisateur_id " +
                        "ORDER BY u.nom, u.prenom";
            System.out.println("SQL pour charger les responsables: " + sql);
            try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        String role = rs.getString("role");
                        String nomComplet = prenom + " " + nom;
                        comboResponsable.getItems().add(nomComplet);
                        count++;
                        System.out.println("Organisateur ajouté: " + nomComplet + " (rôle: " + role + ")");
                    }
                    System.out.println("Nombre d'organisateurs chargés: " + count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des responsables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerFeedbacks() {
        System.out.println("Chargement des feedbacks...");
        try {
            // Récupérer toutes les participations avec feedback
            listeFeedbacks.clear();
            for (Participation p : participationService.afficher()) {
                if (p.hasFeedback()) {
                    listeFeedbacks.add(p);
                }
            }

            listeFiltree.clear();
            listeFiltree.addAll(listeFeedbacks);
            
            System.out.println("Nombre de feedbacks chargés: " + listeFiltree.size());

            if (listeFiltree.isEmpty()) {
                afficherAucunFeedback();
                lblTotal.setText("0 avis");
                lblNoteMoyenne.setText("⭐ 0.0/5");
                lblTopEvenement.setText("-");
            } else {
                appliquerTri();
                afficherCartesFeedbacks(listeFiltree);
                calculerStatistiques();
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du chargement des feedbacks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void appliquerTri() {
        String tri = comboTri.getValue();
        if (tri == null) tri = "Pertinence";

        switch (tri) {
            case "Date récente":
                listeFiltree.sort((p1, p2) -> {
                    if (p1.getFeedbackAt() == null || p2.getFeedbackAt() == null) return 0;
                    return p2.getFeedbackAt().compareTo(p1.getFeedbackAt());
                });
                break;
            case "Date ancienne":
                listeFiltree.sort((p1, p2) -> {
                    if (p1.getFeedbackAt() == null || p2.getFeedbackAt() == null) return 0;
                    return p1.getFeedbackAt().compareTo(p2.getFeedbackAt());
                });
                break;
            case "Note élevée":
                listeFiltree.sort((p1, p2) -> Integer.compare(p2.getNoteSatisfaction(), p1.getNoteSatisfaction()));
                break;
            case "Note faible":
                listeFiltree.sort((p1, p2) -> Integer.compare(p1.getNoteSatisfaction(), p2.getNoteSatisfaction()));
                break;
            case "Alphabétique":
                listeFiltree.sort((p1, p2) -> {
                    String titre1 = chargerEvenementParId(p1.getEvenementId()) != null ? chargerEvenementParId(p1.getEvenementId()).getTitre() : "";
                    String titre2 = chargerEvenementParId(p2.getEvenementId()) != null ? chargerEvenementParId(p2.getEvenementId()).getTitre() : "";
                    return titre1.compareToIgnoreCase(titre2);
                });
                break;
            default: // Pertinence
                // Par défaut : événements à venir d'abord, puis par date de feedback
                listeFiltree.sort((p1, p2) -> {
                    Evenement e1 = chargerEvenementParId(p1.getEvenementId());
                    Evenement e2 = chargerEvenementParId(p2.getEvenementId());
                    if (e1 != null && e2 != null) {
                        boolean e1AVenir = e1.getDateDebut() != null && e1.getDateDebut().toLocalDateTime().isAfter(java.time.LocalDateTime.now());
                        boolean e2AVenir = e2.getDateDebut() != null && e2.getDateDebut().toLocalDateTime().isAfter(java.time.LocalDateTime.now());
                        if (e1AVenir && !e2AVenir) return -1;
                        if (!e1AVenir && e2AVenir) return 1;
                    }
                    if (p1.getFeedbackAt() == null || p2.getFeedbackAt() == null) return 0;
                    return p2.getFeedbackAt().compareTo(p1.getFeedbackAt());
                });
                break;
        }
    }

    private void calculerStatistiques() {
        lblTotal.setText(listeFiltree.size() + " avis");

        // Note moyenne
        if (!listeFiltree.isEmpty()) {
            double somme = listeFiltree.stream().mapToInt(Participation::getNoteSatisfaction).sum();
            double moyenne = somme / listeFiltree.size();
            lblNoteMoyenne.setText(String.format("⭐ %.1f/5", moyenne));
        } else {
            lblNoteMoyenne.setText("⭐ 0.0/5");
        }

        // Top événement (celui avec le plus de feedbacks)
        Map<String, Long> countByEvenement = listeFiltree.stream()
            .collect(Collectors.groupingBy(p -> {
                Evenement e = chargerEvenementParId(p.getEvenementId());
                return e != null ? e.getTitre() : "Inconnu";
            }, Collectors.counting()));
        
        Optional<Map.Entry<String, Long>> topEvenement = countByEvenement.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (topEvenement.isPresent()) {
            lblTopEvenement.setText(topEvenement.get().getKey() + " (" + topEvenement.get().getValue() + ")");
        } else {
            lblTopEvenement.setText("-");
        }
    }

    @FXML
    private void appliquerFiltres() {
        listeFiltree.clear();
        listeFiltree.addAll(listeFeedbacks);

        // Filtrer par type d'événement
        TypeEvenement type = comboTypeEvenement.getValue();
        if (type != null) {
            listeFiltree.removeIf(p -> {
                Evenement e = chargerEvenementParId(p.getEvenementId());
                return e == null || e.getType() != type;
            });
        }

        // Filtrer par responsable
        String responsableSelectionne = comboResponsable.getValue();
        if (responsableSelectionne != null) {
            listeFiltree.removeIf(p -> {
                Evenement e = chargerEvenementParId(p.getEvenementId());
                if (e == null) return true;
                String nomResponsable = chargerNomResponsable(e.getOrganisateurId());
                return !responsableSelectionne.equals(nomResponsable);
            });
        }

        // Filtrer par période (date de l'événement)
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut != null || fin != null) {
            listeFiltree.removeIf(p -> {
                Evenement e = chargerEvenementParId(p.getEvenementId());
                if (e == null || e.getDateDebut() == null) return true;
                LocalDate dateEvenement = e.getDateDebut().toLocalDateTime().toLocalDate();
                if (debut != null && dateEvenement.isBefore(debut)) return true;
                if (fin != null && dateEvenement.isAfter(fin)) return true;
                return false;
            });
        }

        if (listeFiltree.isEmpty()) {
            afficherAucunFeedback();
            lblTotal.setText("0 avis");
            lblNoteMoyenne.setText("⭐ 0.0/5");
            lblTopEvenement.setText("-");
        } else {
            appliquerTri();
            afficherCartesFeedbacks(listeFiltree);
            calculerStatistiques();
        }
    }

    @FXML
    private void reinitialiserFiltres() {
        comboTypeEvenement.setValue(null);
        comboResponsable.setValue(null);
        dateDebut.setValue(null);
        dateFin.setValue(null);
        comboTri.setValue("Pertinence");
        chargerFeedbacks();
    }

    @FXML
    private void retour() throws IOException {
        NavigationContext.loadContentInCenter("/admin_dashboard.fxml");
    }

    private void afficherAucunFeedback() {
        tileFeedbacks.getChildren().clear();
        Label lblMessage = new Label("✅ Aucun avis");
        lblMessage.setStyle("-fx-font-size: 20px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        tileFeedbacks.getChildren().add(lblMessage);
    }

    private void afficherCartesFeedbacks(List<Participation> feedbacks) {
        System.out.println("Affichage des cartes de feedbacks...");
        tileFeedbacks.getChildren().clear();

        for (Participation p : feedbacks) {
            try {
                System.out.println("Création de la carte pour le feedback ID: " + p.getParticipationId());
                VBox carteFeedback = creerCarteFeedback(p);
                tileFeedbacks.getChildren().add(carteFeedback);
                System.out.println("Carte ajoutée avec succès");
            } catch (Exception ex) {
                System.err.println("Erreur lors de la création de la carte pour le feedback " + p.getParticipationId() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private VBox creerCarteFeedback(Participation participation) {
        try {
            // Charger l'événement associé
            Evenement evenement = chargerEvenementParId(participation.getEvenementId());
            if (evenement == null) {
                return null;
            }

            VBox carte = new VBox();
            carte.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
            carte.setPrefWidth(350);
            carte.setSpacing(12);
            carte.setPadding(new Insets(15));

            // Titre de l'événement
            Label lblTitreEvenement = new Label(evenement.getTitre());
            lblTitreEvenement.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
            lblTitreEvenement.setMaxWidth(320);

            // Nom de l'étudiant
            String nomEtudiant = chargerNomEtudiant(participation.getEtudiantId());
            Label lblEtudiant = new Label("👤 " + nomEtudiant);
            lblEtudiant.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

            // Note en étoiles
            String etoiles = "⭐".repeat(participation.getNoteSatisfaction());
            Label lblNote = new Label(etoiles);
            lblNote.setStyle("-fx-font-size: 20px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");

            // Commentaire
            Label lblCommentaire = new Label(participation.getFeedbackCommentaire());
            lblCommentaire.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
            lblCommentaire.setMaxWidth(320);

            // Date du feedback
            Label lblDateFeedback = new Label("Avis donné le: " + 
                (participation.getFeedbackAt() != null 
                    ? participation.getFeedbackAt().toLocalDateTime().format(dateFormatter) 
                    : "Date inconnue"));
            lblDateFeedback.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

            carte.getChildren().addAll(lblTitreEvenement, lblEtudiant, lblNote, lblCommentaire, lblDateFeedback);

            return carte;
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la carte: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Evenement chargerEvenementParId(int evenementId) {
        try {
            String sql = "SELECT * FROM evenement WHERE evenement_id = ?";
            try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                ps.setInt(1, evenementId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Evenement e = new Evenement();
                        e.setEvenementId(rs.getInt("evenement_id"));
                        e.setTitre(rs.getString("titre"));
                        e.setDescription(rs.getString("description"));
                        e.setType(TypeEvenement.fromDb(rs.getString("type")));
                        e.setDateDebut(rs.getTimestamp("date_debut"));
                        e.setDateFin(rs.getTimestamp("date_fin"));
                        e.setLieu(rs.getString("lieu"));
                        e.setImage(rs.getString("image"));
                        e.setOrganisateurId(rs.getInt("organisateur_id"));
                        return e;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de l'événement: " + e.getMessage());
        }
        return null;
    }

    private String chargerNomEtudiant(int etudiantId) {
        try {
            String sql = "SELECT nom, prenom FROM user WHERE user_id = ?";
            try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                ps.setInt(1, etudiantId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement du nom de l'étudiant: " + e.getMessage());
        }
        return "Étudiant inconnu";
    }

    private String chargerNomResponsable(int responsableId) {
        try {
            String sql = "SELECT nom, prenom FROM user WHERE user_id = ?";
            try (java.sql.PreparedStatement ps = org.example.utils.MyDataBase_Unimind.getInstance().getConnection().prepareStatement(sql)) {
                ps.setInt(1, responsableId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String nom = rs.getString("nom");
                        String prenom = rs.getString("prenom");
                        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement du nom du responsable: " + e.getMessage());
        }
        return "Responsable inconnu";
    }
}
