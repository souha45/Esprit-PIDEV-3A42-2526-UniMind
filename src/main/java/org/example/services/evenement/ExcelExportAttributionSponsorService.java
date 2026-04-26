package org.example.services.evenement;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entities.EvenementSponsor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ExcelExportAttributionSponsorService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private Connection connection;

    public ExcelExportAttributionSponsorService() {
        this.connection = org.example.utils.MyDataBase_Unimind.getInstance().getConnection();
    }

    /**
     * Exporte la liste des attributions de sponsors vers un fichier Excel
     *
     * @param attributions Liste des attributions à exporter
     * @param filePath     Chemin du fichier Excel à créer
     * @throws IOException En cas d'erreur lors de l'écriture du fichier
     */
    public void exportAttributionsToExcel(List<EvenementSponsor> attributions, String filePath) throws IOException, SQLException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attributions Sponsors");

            // Créer le style d'en-tête
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Créer la ligne d'en-tête
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Événement",
                    "Sponsor",
                    "Montant Contribution",
                    "Type Contribution",
                    "Description",
                    "Date Contribution",
                    "Statut"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowNum = 1;
            for (EvenementSponsor attribution : attributions) {
                Row row = sheet.createRow(rowNum++);

                int col = 0;

                // Récupérer le nom de l'événement
                String nomEvenement = getNomEvenementById(attribution.getEvenementId());
                row.createCell(col++).setCellValue(nomEvenement);

                // Récupérer le nom du sponsor
                String nomSponsor = getNomSponsorById(attribution.getSponsorId());
                row.createCell(col++).setCellValue(nomSponsor);

                // Montant de contribution
                if (attribution.getMontantContribution() != null) {
                    row.createCell(col++).setCellValue(attribution.getMontantContribution().doubleValue());
                } else {
                    row.createCell(col++).setCellValue(0);
                }

                row.createCell(col++).setCellValue(attribution.getTypeContribution() != null ? attribution.getTypeContributionLabel() : "");
                row.createCell(col++).setCellValue(attribution.getDescriptionContribution() != null ? attribution.getDescriptionContribution() : "");
                row.createCell(col++).setCellValue(attribution.getDateContribution() != null ? DATE_FORMAT.format(attribution.getDateContribution()) : "");
                row.createCell(col++).setCellValue(attribution.getStatut() != null ? attribution.getStatutLabel() : "");
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
     * Récupère le nom de l'événement directement depuis la base
     */
    private String getNomEvenementById(int evenementId) {
        String sql = "SELECT titre FROM evenement WHERE evenement_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("titre");
                }
            }
        } catch (SQLException e) {
            return "ID: " + evenementId;
        }
        return "Inconnu";
    }

    /**
     * Récupère le nom du sponsor directement depuis la base
     */
    private String getNomSponsorById(int sponsorId) {
        String sql = "SELECT nom_sponsor FROM sponsor WHERE sponsor_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, sponsorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nom_sponsor");
                }
            }
        } catch (SQLException e) {
            return "ID: " + sponsorId;
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
