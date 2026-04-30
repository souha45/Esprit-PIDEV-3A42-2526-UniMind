package org.example.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.enums.Role;
import org.example.services.TraitementTranslationService;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TraitementTraductionController implements Initializable {

    @FXML private Label lblStatus;
    @FXML private ComboBox<String> cmbLangueCible;
    @FXML private Button btnTraduire;
    @FXML private Button btnImprimer;
    @FXML private Button btnExporter;
    @FXML private Button btnRetour;

    @FXML private VBox containerResultats;

    @FXML private Label lblTitre;
    @FXML private Label lblType;
    @FXML private Label lblCategorie;
    @FXML private Label lblObjectif;

    private Traitement traitement;
    private TraitementTranslationService translationService;
    private User utilisateur;
    private ObservableList<String> languesDisponibles;

    private String titreTraduit;
    private String typeTraduit;
    private String categorieTraduite;
    private String objectifTraduit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        translationService = new TraitementTranslationService();

        languesDisponibles = FXCollections.observableArrayList(
                "Français (fr)",
                "English (en)",
                "Español (es)",
                "Deutsch (de)",
                "Italiano (it)",
                "Português (pt)",
                "Nederlands (nl)",
                "Русский (ru)",
                "العربية (ar)",
                "中文 (zh)"
        );

        cmbLangueCible.setItems(languesDisponibles);
        cmbLangueCible.setValue("English (en)");

        cmbLangueCible.setOnAction(event -> verifierLangue());
        btnTraduire.setOnAction(event -> handleTraduire());
        btnImprimer.setOnAction(event -> imprimerTraduction());
        btnExporter.setOnAction(event -> exporterTraduction());
        btnRetour.setOnAction(event -> handleRetour());
    }

    public void setTraitement(Traitement traitement) {
        this.traitement = traitement;
        afficherInformationsTraitement();
    }

    public void setUtilisateur(User utilisateur) {
        this.utilisateur = utilisateur;
    }

    private void afficherInformationsTraitement() {
        if (traitement == null) return;

        lblTitre.setText(traitement.getTitre() != null ? traitement.getTitre() : "");
        lblType.setText(traitement.getType() != null ? traitement.getType() : "");
        lblCategorie.setText(traitement.getCategorie() != null ? String.valueOf(traitement.getCategorie()) : "");
        lblObjectif.setText(traitement.getObjectifTherapeutique() != null ? traitement.getObjectifTherapeutique() : "");

        lblStatus.setText("Traitement chargé : " + traitement.getTitre());
    }

    private void verifierLangue() {
        String selected = cmbLangueCible.getValue();
        if (selected != null) {
            lblStatus.setText("✅ Prêt à traduire vers " + selected);
        }
    }

    private String getCodeLangue(String langueDisplay) {
        if (langueDisplay == null) return "en";
        if (langueDisplay.contains("Français")) return "fr";
        if (langueDisplay.contains("English")) return "en";
        if (langueDisplay.contains("Español")) return "es";
        if (langueDisplay.contains("Deutsch")) return "de";
        if (langueDisplay.contains("Italiano")) return "it";
        if (langueDisplay.contains("Português")) return "pt";
        if (langueDisplay.contains("Nederlands")) return "nl";
        if (langueDisplay.contains("Русский")) return "ru";
        if (langueDisplay.contains("العربية")) return "ar";
        if (langueDisplay.contains("中文")) return "zh";
        return "en";
    }

    @FXML
    private void handleTraduire() {
        if (traitement == null) {
            afficherMessage("Aucun traitement sélectionné", Alert.AlertType.ERROR);
            return;
        }

        final String selected = cmbLangueCible.getValue();
        if (selected == null) {
            afficherMessage("Veuillez sélectionner une langue", Alert.AlertType.WARNING);
            return;
        }

        final String langueCode = getCodeLangue(selected);
        // Toujours utiliser la détection automatique pour la langue source
        final String langueSourceCode = "auto";

        lblStatus.setText("🔄 Traduction automatique en cours vers " + selected + "...");
        btnTraduire.setDisable(true);

        new Thread(() -> {
            try {
                String titreTraduitTemp = traduireTexte(traitement.getTitre(), langueSourceCode, langueCode);
                String typeTraduitTemp = traduireTexte(traitement.getType(), langueSourceCode, langueCode);
                String categorieTraduiteTemp = traduireTexte(traitement.getCategorie().name(), langueSourceCode, langueCode);
                String objectifTraduitTemp = traduireTexte(traitement.getObjectifTherapeutique(), langueSourceCode, langueCode);

                Platform.runLater(() -> {
                    titreTraduit = titreTraduitTemp;
                    typeTraduit = typeTraduitTemp;
                    categorieTraduite = categorieTraduiteTemp;
                    objectifTraduit = objectifTraduitTemp;

                    afficherResultatsTraduction();
                    lblStatus.setText("✅ Traduction terminée avec succès vers " + selected);
                    btnTraduire.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherMessage("Erreur lors de la traduction : " + e.getMessage(), Alert.AlertType.ERROR);
                    lblStatus.setText("❌ Erreur de traduction");
                    btnTraduire.setDisable(false);
                });
            }
        }).start();
    }

    private String traduireTexte(String texte, String sourceLang, String targetLang) {
        if (texte == null || texte.trim().isEmpty()) {
            return "Non spécifié";
        }

        try {
            // Utiliser le service de traduction complet
            var result = translationService.translateText(texte, targetLang, sourceLang);
            if (result.isSuccess()) {
                return result.getTranslatedText();
            } else {
                System.err.println("Erreur traduction: " + result.getError());
                return texte;
            }
        } catch (Exception e) {
            System.err.println("Erreur traduction: " + e.getMessage());
            return texte;
        }
    }

    private void afficherResultatsTraduction() {
        containerResultats.getChildren().clear();

        afficherChampResultat("Titre", titreTraduit, traitement.getTitre());
        afficherChampResultat("Type", typeTraduit, traitement.getType());
        afficherChampResultat("Catégorie", categorieTraduite, traitement.getCategorie().name());
        afficherChampResultat("Objectif thérapeutique", objectifTraduit, traitement.getObjectifTherapeutique());
    }

    private void afficherChampResultat(String nomChamp, String traduit, String original) {
        VBox champBox = new VBox(12);
        champBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // En-tête du champ avec icône
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 16px;");
        Label nomLabel = new Label(nomChamp.toUpperCase());
        nomLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4f46e5; -fx-font-size: 16px;");
        headerBox.getChildren().addAll(iconLabel, nomLabel);

        // Conteneur principal pour original et traduction
        VBox contenuBox = new VBox(15);

        // Section Original
        VBox originalBox = new VBox(8);
        originalBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
        Label originalLabel = new Label("📝 TEXTE ORIGINAL");
        originalLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        Label originalText = new Label(original != null && !original.isEmpty() ? original : "Non spécifié");
        originalText.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e293b; -fx-line-spacing: 2; -fx-wrap-text: true;");
        originalText.setWrapText(true);
        originalText.setMinHeight(Label.USE_PREF_SIZE);
        originalBox.getChildren().addAll(originalLabel, originalText);

        // Flèche de traduction
        HBox flecheBox = new HBox();
        flecheBox.setAlignment(javafx.geometry.Pos.CENTER);
        Label flecheLabel = new Label("⬇️ TRADUCTION ⬇️");
        flecheLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6366f1; -fx-font-weight: bold; -fx-padding: 8;");
        flecheBox.getChildren().add(flecheLabel);

        // Section Traduit
        VBox traduitBox = new VBox(8);
        traduitBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f0fdf4, #dcfce7); -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #22c55e; -fx-border-width: 1;");
        Label traduitLabel = new Label("✅ TEXTE TRADUIT");
        traduitLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803d; -fx-font-weight: bold;");
        Label traduitText = new Label(traduit != null ? traduit : "En attente de traduction...");
        traduitText.setStyle("-fx-font-size: 14px; -fx-text-fill: #166534; -fx-font-weight: 500; -fx-line-spacing: 2; -fx-wrap-text: true;");
        traduitText.setWrapText(true);
        traduitText.setMinHeight(Label.USE_PREF_SIZE);
        traduitBox.getChildren().addAll(traduitLabel, traduitText);

        contenuBox.getChildren().addAll(originalBox, flecheBox, traduitBox);
        champBox.getChildren().addAll(headerBox, contenuBox);

        containerResultats.getChildren().add(champBox);
    }

    @FXML
    private void exporterTraduction() {
        if (traitement == null) {
            afficherMessage("Aucun traitement à exporter", Alert.AlertType.WARNING);
            return;
        }

        if (titreTraduit == null) {
            afficherMessage("Veuillez d'abord effectuer une traduction", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter la traduction en PDF");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );

        String selected = cmbLangueCible.getValue();
        String langueNom = selected != null ? selected.replace(" (", "_").replace(")", "") : "langue";
        String nomFichier = "traduction_traitement_" + langueNom + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        fileChooser.setInitialFileName(nomFichier);

        File file = fileChooser.showSaveDialog(btnExporter.getScene().getWindow());

        if (file != null) {
            try {
                genererPDF(file);
                afficherMessage("Traduction exportée en PDF avec succès : " + file.getName(),
                        Alert.AlertType.INFORMATION);
                lblStatus.setText("✅ Exporté vers " + file.getName());

            } catch (Exception e) {
                afficherMessage("Erreur lors de l'exportation PDF : " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    //  Pied de page sur la première page
    private void genererPDF(File file) throws Exception {
        String selected = cmbLangueCible.getValue();
        String langueNom = selected != null ? selected : "langue cible";
        String langueSourceNom = "Auto-détection";

        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(50, 50, 40, 50);  // Marge basse réduite

        PdfFont fontRegular = PdfFontFactory.createFont("Helvetica");
        PdfFont fontBold = PdfFontFactory.createFont("Helvetica-Bold");

        // En-tête
        Paragraph header = new Paragraph("Traduction de Traitement")
                .setFont(fontBold)
                .setFontSize(22)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(15);
        document.add(header);

        // Informations de traduction
        Paragraph info = new Paragraph()
                .add(new Paragraph("Langue source : " + langueSourceNom).setFont(fontRegular).setFontSize(11))
                .add(new Paragraph("Langue cible : " + langueNom).setFont(fontRegular).setFontSize(11))
                .add(new Paragraph("Date : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(fontRegular).setFontSize(11))
                .setMarginBottom(15);
        document.add(info);

        // Section Informations du traitement
        Paragraph sectionTitle = new Paragraph("INFORMATIONS DU TRAITEMENT")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(ColorConstants.BLUE)
                .setMarginTop(15)
                .setMarginBottom(8);
        document.add(sectionTitle);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        infoTable.setWidth(UnitValue.createPercentValue(100));

        addTableRow(infoTable, "Titre :", traitement.getTitre(), fontRegular, fontBold);
        addTableRow(infoTable, "Type :", traitement.getType(), fontRegular, fontBold);
        addTableRow(infoTable, "Catégorie :", traitement.getCategorie().name(), fontRegular, fontBold);
        addTableRow(infoTable, "Objectif :", traitement.getObjectifTherapeutique(), fontRegular, fontBold);

        document.add(infoTable);

        // Section Résultats de la traduction
        Paragraph resultsTitle = new Paragraph("RÉSULTATS DE LA TRADUCTION")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(ColorConstants.GREEN)
                .setMarginTop(20)
                .setMarginBottom(8);
        document.add(resultsTitle);

        addTranslationSection(document, "Titre", traitement.getTitre(), titreTraduit, fontRegular, fontBold);
        addTranslationSection(document, "Type", traitement.getType(), typeTraduit, fontRegular, fontBold);
        addTranslationSection(document, "Catégorie", traitement.getCategorie().name(), categorieTraduite, fontRegular, fontBold);
        addTranslationSection(document, "Objectif thérapeutique", traitement.getObjectifTherapeutique(), objectifTraduit, fontRegular, fontBold);

        //  Pied de page - Force à rester sur la première page
        Paragraph footer = new Paragraph("Document généré par UniMind - Système de traduction de traitements")
                .setFont(fontRegular)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setKeepTogether(true);  // Force le pied de page à rester groupé

        document.add(footer);
        document.close();
    }

    private void addTableRow(Table table, String label, String value, PdfFont fontRegular, PdfFont fontBold) {
        if (value == null || value.isEmpty()) value = "Non spécifié";

        Cell labelCell = new Cell().add(new Paragraph(label).setFont(fontBold).setFontSize(11))
                .setBorder(Border.NO_BORDER);
        Cell valueCell = new Cell().add(new Paragraph(value).setFont(fontRegular).setFontSize(11))
                .setBorder(Border.NO_BORDER);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTranslationSection(Document document, String title, String original, String translated,
                                       PdfFont fontRegular, PdfFont fontBold) {
        if (translated == null) translated = "En attente de traduction";
        if (original == null) original = "Non spécifié";

        Paragraph subTitle = new Paragraph(title)
                .setFont(fontBold)
                .setFontSize(12)
                .setMarginTop(10)
                .setMarginBottom(5);
        document.add(subTitle);

        Table translationTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        translationTable.setWidth(UnitValue.createPercentValue(100));

        Cell headerOriginal = new Cell().add(new Paragraph("TEXTE ORIGINAL").setFont(fontBold).setFontSize(9))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
        Cell headerTranslated = new Cell().add(new Paragraph("TRADUCTION").setFont(fontBold).setFontSize(9))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
        translationTable.addCell(headerOriginal);
        translationTable.addCell(headerTranslated);

        Cell originalCell = new Cell().add(new Paragraph(original).setFont(fontRegular).setFontSize(10))
                .setPadding(6);
        Cell translatedCell = new Cell().add(new Paragraph(translated).setFont(fontRegular).setFontSize(10))
                .setPadding(6)
                .setBackgroundColor(new DeviceRgb(200, 230, 201));

        translationTable.addCell(originalCell);
        translationTable.addCell(translatedCell);

        document.add(translationTable);
    }

    @FXML
    private void imprimerTraduction() {
        if (titreTraduit == null) {
            afficherMessage("Veuillez d'abord effectuer une traduction", Alert.AlertType.WARNING);
            return;
        }

        try {
            File tempFile = File.createTempFile("traduction_", ".pdf");
            genererPDF(tempFile);
            java.awt.Desktop.getDesktop().open(tempFile);
            lblStatus.setText("✅ Aperçu PDF ouvert");

        } catch (Exception e) {
            afficherMessage("Erreur lors de l'ouverture de l'aperçu : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleRetour() {
        //  Simplement fermer cette fenêtre — la fenêtre principale reste intacte
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.close();
    }

    private void afficherMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Traduction de Traitement");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}