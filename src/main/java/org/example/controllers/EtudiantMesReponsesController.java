package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.entities.Reponsequestionnaire;
import org.example.services.ReponseQuestionnaireServices;
import org.example.utils.LimiteQuestionnaire;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class EtudiantMesReponsesController implements Initializable {

    @FXML private TableView<Reponsequestionnaire> tableReponse;
    @FXML private TableColumn<Reponsequestionnaire, Double>  colScore;
    @FXML private TableColumn<Reponsequestionnaire, Integer> colDuree;
    @FXML private TableColumn<Reponsequestionnaire, String>  colNiveau, colPsy, colDate;
    @FXML private Label lblStatus;

    // ✅ contentArea pour navigation
    private StackPane contentArea;

    private final ReponseQuestionnaireServices service = new ReponseQuestionnaireServices();
    private final ObservableList<Reponsequestionnaire> data = FXCollections.observableArrayList();

    private final PDType1Font FONT_BOLD    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ✅ Setter pour recevoir contentArea
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreTotale"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureePassage"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        colPsy.setCellValueFactory(new PropertyValueFactory<>("aBesoinPsy"));
        colPsy.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Reponsequestionnaire r = (Reponsequestionnaire) getTableRow().getItem();
                    setText(r.isaBesoinPsy() ? "Oui" : "Non");
                }
            }
        });

        tableReponse.setItems(data);
        loadData();
    }

    // ══════════════════════════════════════════
    //  EXPORT PDF
    // ══════════════════════════════════════════

    @FXML
    public void handleExporterPDF() {
        if (data.isEmpty()) {
            setStatus("Aucune donnee a exporter !", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le PDF");
        fileChooser.setInitialFileName("mes_reponses_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(tableReponse.getScene().getWindow());

        if (file == null) return;

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float margin    = 40;
                float pageWidth = PDRectangle.A4.getWidth();
                float yPos      = PDRectangle.A4.getHeight() - margin;

                cs.setFont(FONT_BOLD, 18);
                cs.setNonStrokingColor(0.49f, 0.23f, 0.93f);
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("UniMind - Mes Reponses");
                cs.endText();
                yPos -= 25;

                cs.setFont(FONT_REGULAR, 10);
                cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Exporte le : " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                cs.endText();
                yPos -= 30;

                cs.setStrokingColor(0.49f, 0.23f, 0.93f);
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, yPos);
                cs.lineTo(pageWidth - margin, yPos);
                cs.stroke();
                yPos -= 20;

                float[] colX     = {margin, 130, 230, 320, 400};
                String[] headers = {"Score", "Niveau", "Besoin Psy", "Duree", "Date"};

                cs.setFont(FONT_BOLD, 11);
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                for (int i = 0; i < headers.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(colX[i], yPos);
                    cs.showText(headers[i]);
                    cs.endText();
                }
                yPos -= 5;

                cs.setStrokingColor(0.8f, 0.8f, 0.8f);
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, yPos);
                cs.lineTo(pageWidth - margin, yPos);
                cs.stroke();
                yPos -= 15;

                cs.setFont(FONT_REGULAR, 10);
                boolean altRow = false;

                for (Reponsequestionnaire r : data) {
                    if (yPos < 60) break;

                    if (altRow) {
                        cs.setNonStrokingColor(0.97f, 0.95f, 1.0f);
                        cs.addRect(margin, yPos - 3, pageWidth - 2 * margin, 16);
                        cs.fill();
                    }
                    altRow = !altRow;

                    cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);

                    cs.beginText();
                    cs.newLineAtOffset(colX[0], yPos);
                    cs.showText(String.valueOf(r.getScoreTotale()));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(colX[1], yPos);
                    cs.showText(r.getNiveau() != null ? r.getNiveau() : "-");
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(colX[2], yPos);
                    cs.showText(r.isaBesoinPsy() ? "Oui" : "Non");
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(colX[3], yPos);
                    cs.showText(r.getDureePassage() != null ?
                            r.getDureePassage() + " min" : "-");
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(colX[4], yPos);
                    cs.showText(r.getCreatedAt() != null ?
                            r.getCreatedAt().toString().substring(0, 16) : "-");
                    cs.endText();

                    yPos -= 18;
                }

                yPos -= 10;
                cs.setStrokingColor(0.49f, 0.23f, 0.93f);
                cs.setLineWidth(1f);
                cs.moveTo(margin, yPos);
                cs.lineTo(pageWidth - margin, yPos);
                cs.stroke();
                yPos -= 15;

                double scoreMoyen = data.stream()
                        .mapToDouble(Reponsequestionnaire::getScoreTotale)
                        .average().orElse(0);

                cs.setFont(FONT_BOLD, 10);
                cs.setNonStrokingColor(0.49f, 0.23f, 0.93f);
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Total : " + data.size() + " reponse(s)   |   " +
                        "Score moyen : " + String.format("%.1f", scoreMoyen));
                cs.endText();
            }

            doc.save(file);
            setStatus("PDF exporte avec succes !", true);

        } catch (Exception e) {
            setStatus("Erreur export PDF : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════
    //  LOAD DATA
    // ══════════════════════════════════════════

    private void loadData() {
        try {
            int userId = LimiteQuestionnaire.getInstance().getUserId();
            List<Reponsequestionnaire> list = service.afficherParUser(userId);
            data.setAll(list);
        } catch (SQLException e) {
            setStatus("Erreur chargement : " + e.getMessage(), false);
        }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (success ? "#22c55e" : "#ef4444") + ";");
    }
}