package org.example.controllers.sponsor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.example.entities.Sponsor;
import org.example.enums.Role;
import org.example.enums.TypeSponsor;
import org.example.enums.StatutSponsor;
import org.example.services.SponsorService;
import org.example.services.evenement.ExcelExportSponsorService;
import org.example.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class GestionSponsorController {

    @FXML
    private TableView<SponsorService.SponsorAvecInfos> tableSponsors;

    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colEvenement;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colLogo;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colNom;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colType;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colEmail;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colStatut;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, String> colDate;
    @FXML
    private TableColumn<SponsorService.SponsorAvecInfos, Void> colActions;

    @FXML
    private Label lblTotal;

    @FXML
    private TextField txtRecherche;

    @FXML
    private Button btnExportExcel;

    @FXML
    private ComboBox<TypeSponsor> comboType;

    @FXML
    private ComboBox<StatutSponsor> comboStatut;

    @FXML
    private DatePicker dateDu;

    @FXML
    private DatePicker dateAu;

    @FXML
    private Label lblStatutActif;

    @FXML
    private Label lblStatutInactif;

    @FXML
    private Label lblStatutRefuse;

    @FXML
    private Label lblStatutAnnule;

    @FXML
    private Label lblTotalSponsors;

    @FXML
    private Pagination paginationSponsors;

    @FXML
    private Label lblPageInfo;

    private SponsorService sponsorService;
    private ObservableList<SponsorService.SponsorAvecInfos> listeSponsors;
    private ObservableList<SponsorService.SponsorAvecInfos> listeFiltree;

    private static final int ITEMS_PER_PAGE = 10;

    @FXML
    public void initialize() {
        // Vérifier si l'utilisateur est admin
        boolean isAdmin = SessionManager.getInstance().getCurrentUserRole()
                .map(role -> role == Role.ADMIN)
                .orElse(false);

        if (!isAdmin) {
            afficherAlerte("Accès refusé", "L'accès à la gestion des sponsors est réservé à l'administrateur. Les responsables gèrent les attributions de sponsors aux événements via le menu 'Attributions'.");
            return;
        }

        sponsorService = new SponsorService();
        listeSponsors = FXCollections.observableArrayList();
        listeFiltree = FXCollections.observableArrayList();

        // Initialiser les filtres
        initialiserFiltres();

        configurerColonnes();
        configurerColorationLignes();
        chargerSponsors();

        if (paginationSponsors != null) {
            paginationSponsors.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                if (newIdx != null) {
                    updateTableForPage(newIdx.intValue());
                }
            });
        }
    }

    private void initialiserFiltres() {
        comboType.getItems().setAll(TypeSponsor.values());
        comboStatut.getItems().setAll(StatutSponsor.values());

        // Configurer les cell factories pour afficher les noms lisibles
        comboType.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TypeSponsor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        comboType.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TypeSponsor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous" : item.name());
            }
        });

        comboStatut.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StatutSponsor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        comboStatut.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(StatutSponsor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous" : item.name());
            }
        });
    }

    private void configurerColonnes() {
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("evenementTitre"));
        colLogo.setCellValueFactory(new PropertyValueFactory<>("logo"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomSponsor"));
        colType.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTypeSponsor().toString()));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("emailContact"));
        colStatut.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatut().toString()));
        colDate.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateContribution() != null ?
                                cellData.getValue().getDateContribution().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                                "N/A"));

        // Configurer la colonne Logo pour afficher des miniatures d'images
        colLogo.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            private final javafx.scene.control.Label lblPasLogo = new javafx.scene.control.Label("N/A");

            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
                lblPasLogo.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(String logoName, boolean empty) {
                super.updateItem(logoName, empty);
                if (empty || logoName == null || logoName.trim().isEmpty()) {
                    setGraphic(lblPasLogo);
                    imageView.setImage(null);
                } else {
                    try {
                        // Construire l'URL complète avec XAMPP
                        String encodedLogoName = java.net.URLEncoder.encode(logoName.trim(), java.nio.charset.StandardCharsets.UTF_8);
                        String fullLogoUrl = "http://localhost/uploadsEvent/sponsors/" + encodedLogoName;
                        javafx.scene.image.Image logo = new javafx.scene.image.Image(fullLogoUrl, true);
                        imageView.setImage(logo);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(lblPasLogo);
                    }
                }
            }
        });
        colLogo.setCellValueFactory(new PropertyValueFactory<>("logo"));

        colActions.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Créer le bouton Voir
                    Button btnVoir = new Button("👁");
                    btnVoir.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
                    btnVoir.setOnAction(event -> voirSponsor(getTableView().getItems().get(getIndex())));

                    Button btnModifier = new Button("✏");
                    btnModifier.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
                    btnModifier.setOnAction(event -> modifierSponsor(getTableView().getItems().get(getIndex())));

                    Button btnSupprimer = new Button("🗑");
                    btnSupprimer.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
                    btnSupprimer.setOnAction(event -> supprimerSponsor(getTableView().getItems().get(getIndex())));

                    HBox hbox = new HBox(8, btnVoir, btnModifier, btnSupprimer);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void configurerColorationLignes() {
        tableSponsors.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(SponsorService.SponsorAvecInfos sponsor, boolean empty) {
                super.updateItem(sponsor, empty);
                if (empty || sponsor == null) {
                    setStyle("");
                } else {
                    switch (sponsor.getStatut()) {
                        case EN_ATTENTE:
                            setStyle("-fx-background-color: #fff3e0;");
                            break;
                        case CONFIRME:
                            setStyle("-fx-background-color: #e8f5e9;");
                            break;
                        case REFUSE:
                            setStyle("-fx-background-color: #ffebee;");
                            break;
                        case ANNULE:
                            setStyle("-fx-background-color: #f5f5f5;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }

    private void chargerSponsors() {
        try {
            listeSponsors.clear();
            // Charger les sponsors simples
            java.util.List<org.example.entities.Sponsor> sponsors = sponsorService.afficher();

            // Créer les objets SponsorAvecInfos en ajoutant les informations de sponsoring
            for (org.example.entities.Sponsor sponsor : sponsors) {
                SponsorService.SponsorAvecInfos info = new SponsorService.SponsorAvecInfos();
                info.setSponsorId(sponsor.getSponsorId());
                info.setNomSponsor(sponsor.getNomSponsor());
                info.setTypeSponsor(sponsor.getTypeSponsor());
                info.setEmailContact(sponsor.getEmailContact());
                info.setLogo(sponsor.getLogo());
                info.setStatut(sponsor.getStatut());

                // Récupérer les informations de sponsoring (événement, date et organisateur)
                Object[] sponsoringInfos = sponsorService.getSponsoringInfos(sponsor.getSponsorId());
                info.setEvenementTitre((String) sponsoringInfos[0]);
                if (sponsoringInfos[1] != null) {
                    info.setDateContribution(java.sql.Timestamp.valueOf((String) sponsoringInfos[1]));
                }
                info.setOrganisateurId((Integer) sponsoringInfos[2]);

                listeSponsors.add(info);
            }

            applyFilteredList(listeSponsors, listeSponsors.size() + " sponsors");
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de charger les sponsors: " + e.getMessage());
        }
    }

    private void applyFilteredList(java.util.List<SponsorService.SponsorAvecInfos> newList, String totalLabel) {
        listeFiltree.setAll(newList);

        if (lblTotal != null) {
            lblTotal.setText(totalLabel);
        }

        // Mettre à jour pagination
        if (paginationSponsors != null) {
            int pageCount = (int) Math.ceil((double) listeFiltree.size() / ITEMS_PER_PAGE);
            paginationSponsors.setPageCount(Math.max(pageCount, 1));
            paginationSponsors.setCurrentPageIndex(0);
            updateTableForPage(0);
        } else {
            tableSponsors.setItems(listeFiltree);
            if (lblPageInfo != null) lblPageInfo.setText("");
        }

        // Calculer et afficher les statistiques
        calculerStatistiques();
    }

    private void updateTableForPage(int pageIndex) {
        if (listeFiltree == null) {
            tableSponsors.setItems(FXCollections.observableArrayList());
            if (lblPageInfo != null) lblPageInfo.setText("");
            return;
        }

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, listeFiltree.size());

        ObservableList<SponsorService.SponsorAvecInfos> pageItems;
        if (fromIndex >= listeFiltree.size() || fromIndex < 0) {
            pageItems = FXCollections.observableArrayList();
        } else {
            pageItems = FXCollections.observableArrayList(listeFiltree.subList(fromIndex, toIndex));
        }

        tableSponsors.setItems(pageItems);

        if (lblPageInfo != null) {
            int total = listeFiltree.size();
            if (total == 0) {
                lblPageInfo.setText("Aucun résultat");
            } else {
                lblPageInfo.setText("Affichage " + (fromIndex + 1) + " - " + toIndex + " sur " + total);
            }
        }
    }

    private void calculerStatistiques() {
        int enAttente = 0, confirmes = 0, refuses = 0, annules = 0;

        ObservableList<SponsorService.SponsorAvecInfos> base = (listeFiltree != null)
                ? listeFiltree
                : listeSponsors;

        for (SponsorService.SponsorAvecInfos sponsor : base) {
            switch (sponsor.getStatut()) {
                case EN_ATTENTE:
                    enAttente++;
                    break;
                case CONFIRME:
                    confirmes++;
                    break;
                case REFUSE:
                    refuses++;
                    break;
                case ANNULE:
                    annules++;
                    break;
            }
        }

        if (lblStatutActif != null) lblStatutActif.setText(confirmes + " confirmés");
        if (lblStatutInactif != null) lblStatutInactif.setText(enAttente + " en attente");
        if (lblStatutRefuse != null) lblStatutRefuse.setText(refuses + " refusés");
        if (lblStatutAnnule != null) lblStatutAnnule.setText(annules + " annulés");
        if (lblTotalSponsors != null) lblTotalSponsors.setText(base.size() + " sponsors");
    }

    @FXML
    private void ajouterSponsor(ActionEvent event) throws IOException {
        org.example.controllers.admin.AdminDashboardController.loadContent("/sponsor/AjoutSponsor.fxml");
    }

    @FXML
    private void rechercherSponsors(ActionEvent event) {
        String recherche = txtRecherche.getText().trim().toLowerCase();

        if (recherche.isEmpty()) {
            chargerSponsors();
            return;
        }

        ObservableList<SponsorService.SponsorAvecInfos> resultats = FXCollections.observableArrayList();

        for (SponsorService.SponsorAvecInfos sponsor : listeSponsors) {
            if (sponsor.getNomSponsor().toLowerCase().contains(recherche) ||
                sponsor.getEmailContact().toLowerCase().contains(recherche) ||
                (sponsor.getEvenementTitre() != null && sponsor.getEvenementTitre().toLowerCase().contains(recherche))) {
                resultats.add(sponsor);
            }
        }

        applyFilteredList(resultats, resultats.size() + " sponsors (filtrés)");
    }

    @FXML
    private void reinitialiserRecherche(ActionEvent event) {
        txtRecherche.clear();
        chargerSponsors();
    }

    @FXML
    public void appliquerFiltres(ActionEvent event) {
        if (listeSponsors == null || listeSponsors.isEmpty()) {
            return;
        }

        TypeSponsor type = comboType.getValue();
        StatutSponsor statut = comboStatut.getValue();
        var du = dateDu.getValue();
        var au = dateAu.getValue();

        ObservableList<SponsorService.SponsorAvecInfos> resultats = FXCollections.observableArrayList();

        for (SponsorService.SponsorAvecInfos sponsor : listeSponsors) {
            // Filtre par type
            if (type != null && sponsor.getTypeSponsor() != type) {
                continue;
            }

            // Filtre par statut
            if (statut != null && sponsor.getStatut() != statut) {
                continue;
            }

            // Filtre par date
            if (sponsor.getDateContribution() != null) {
                var d = sponsor.getDateContribution().toLocalDateTime().toLocalDate();
                if (du != null && d.isBefore(du)) {
                    continue;
                }
                if (au != null && d.isAfter(au)) {
                    continue;
                }
            }

            resultats.add(sponsor);
        }

        applyFilteredList(resultats, resultats.size() + " sponsors (filtrés)");
    }

    @FXML
    public void reinitialiserFiltres(ActionEvent event) {
        comboType.setValue(null);
        comboStatut.setValue(null);
        dateDu.setValue(null);
        dateAu.setValue(null);
        txtRecherche.clear();
        chargerSponsors();
    }

    private void voirSponsor(SponsorService.SponsorAvecInfos sponsor) {
        try {
            // Récupérer l'objet Sponsor complet depuis le service
            Sponsor sponsorComplete = sponsorService.findById(sponsor.getSponsorId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sponsor/VoirSponsor.fxml"));
            Parent root = loader.load();
            VoirSponsorController controller = loader.getController();
            controller.setSponsor(sponsorComplete);

            org.example.controllers.admin.AdminDashboardController.loadContent(root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de détails: " + e.getMessage());
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de récupérer le sponsor: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible d'afficher le sponsor: " + e.getMessage());
        }
    }

    private void modifierSponsor(SponsorService.SponsorAvecInfos sponsor) {
        try {
            // Récupérer l'objet Sponsor complet depuis le service
            Sponsor sponsorComplete = sponsorService.findById(sponsor.getSponsorId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sponsor/ModificationSponsor.fxml"));
            Parent root = loader.load();
            ModificationSponsorController controller = loader.getController();
            controller.setSponsor(sponsorComplete);

            org.example.controllers.admin.AdminDashboardController.loadContent(root);
        } catch (IOException e) {
            afficherAlerte("Erreur", "Impossible d'ouvrir l'écran de modification: " + e.getMessage());
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de récupérer le sponsor: " + e.getMessage());
        } catch (Exception e) {
            afficherAlerte("Erreur", "Impossible de modifier le sponsor: " + e.getMessage());
        }
    }

    private void supprimerSponsor(SponsorService.SponsorAvecInfos sponsor) {
        try {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le sponsor");
            confirmation.setContentText("Voulez-vous vraiment supprimer le sponsor \"" + sponsor.getNomSponsor() + "\" ?");

            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                sponsorService.supprimer(sponsor.getSponsorId());
                chargerSponsors();
                afficherAlerte("Succès", "Sponsor supprime avec succes");
            }
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de supprimer le sponsor: " + e.getMessage());
        }
    }

    @FXML
    public void retourAccueil(ActionEvent event) throws IOException {
        org.example.controllers.admin.AdminDashboardController.loadContent("/sponsor/GestionSponsor.fxml");
    }

    @FXML
    public void exporterExcel(ActionEvent event) {
        try {
            // Récupérer tous les sponsors
            List<Sponsor> sponsorsToExport = sponsorService.afficher();

            if (sponsorsToExport.isEmpty()) {
                afficherAlerte("Information", "Aucun sponsor à exporter.");
                return;
            }

            // Créer le service d'export
            ExcelExportSponsorService exportService = new ExcelExportSponsorService();

            // Générer le nom du fichier avec la date actuelle
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "sponsors_export_" + timestamp + ".xlsx";

            // Chemin du dossier de téléchargements de l'utilisateur
            String userHome = System.getProperty("user.home");
            String downloadPath = userHome + File.separator + "Downloads" + File.separator + fileName;

            // Exporter vers Excel
            exportService.exportSponsorsToExcel(sponsorsToExport, downloadPath);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText("Export réussi !");
            alert.setContentText("Fichier enregistré dans :\n" + downloadPath);
            alert.getDialogPane().setMinWidth(500);
            alert.showAndWait();

        } catch (IOException e) {
            afficherAlerte("Erreur", "Erreur lors de l'export Excel : " + e.getMessage());
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Erreur lors de l'export Excel : " + e.getMessage());
        }
    }

    private void afficherAlerte(String type, String message) {
        Alert alert = new Alert(type.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
