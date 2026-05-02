package org.example.services.evenement;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.services.EvenementService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ExcelExportParticipationService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private EvenementService evenementService;
    private Connection connection;

    public ExcelExportParticipationService() {
        this.evenementService = new EvenementService();
        this.connection = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
    }

    /**
     * Exporte la liste des participations vers un fichier Excel
     *
     * @param participations Liste des participations à exporter
     * @param filePath        Chemin du fichier Excel à créer
     * @throws IOException En cas d'erreur lors de l'écriture du fichier
     */
    public void exportParticipationsToExcel(List<Participation> participations, String filePath) throws IOException, SQLException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Participations");

            // Créer le style d'en-tête
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Créer la ligne d'en-tête (sans colonne Présent)
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Événement",
                    "Étudiant",
                    "Date Inscription",
                    "Statut",
                    "Note Satisfaction",
                    "Feedback Commentaire",
                    "Date Création"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowNum = 1;
            for (Participation participation : participations) {
                Row row = sheet.createRow(rowNum++);

                int col = 0;

                // Récupérer le nom de l'événement
                String nomEvenement = "Inconnu";
                try {
                    Evenement event = evenementService.findById(participation.getEvenementId());
                    if (event != null) {
                        nomEvenement = event.getTitre();
                    }
                } catch (SQLException e) {
                    // Garder "Inconnu" en cas d'erreur
                }
                row.createCell(col++).setCellValue(nomEvenement);

                // Récupérer le nom de l'étudiant directement depuis la base
                String nomEtudiant = getNomEtudiantById(participation.getEtudiantId());
                row.createCell(col++).setCellValue(nomEtudiant);

                row.createCell(col++).setCellValue(participation.getDateInscription() != null ? DATE_FORMAT.format(participation.getDateInscription()) : "");
                row.createCell(col++).setCellValue(participation.getStatut() != null ? participation.getStatut().getDbValue() : "");

                // Note de satisfaction (convertir en String pour éviter l'erreur de type)
                if (participation.getNoteSatisfaction() != null) {
                    row.createCell(col++).setCellValue(String.valueOf(participation.getNoteSatisfaction()));
                } else {
                    row.createCell(col++).setCellValue("");
                }

                row.createCell(col++).setCellValue(participation.getFeedbackCommentaire() != null ? participation.getFeedbackCommentaire() : "");
                row.createCell(col++).setCellValue(participation.getCreatedAt() != null ? DATE_FORMAT.format(participation.getCreatedAt()) : "");
            }

            // Ajuster la largeur des colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Écrire le fichier
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    /**
     * Récupère le nom de l'étudiant directement depuis la base (évite le problème enum Role)
     */
    private String getNomEtudiantById(int etudiantId) {
        String sql = "SELECT nom, prenom FROM user WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("prenom") + " " + rs.getString("nom");
                }
            }
        } catch (SQLException e) {
            // Retourner l'ID en cas d'erreur
            return "ID: " + etudiantId;
        }
        return "Inconnu";
    }

    /**
     * Crée le style pour les en-têtes du fichier Excel
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Fond bleu pour l'en-tête
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Bordures
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Police en gras
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // Alignement centré
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }
}
