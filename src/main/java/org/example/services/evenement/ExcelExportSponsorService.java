package org.example.services.evenement;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entities.Sponsor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ExcelExportSponsorService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /**
     * Exporte la liste des sponsors vers un fichier Excel
     *
     * @param sponsors Liste des sponsors à exporter
     * @param filePath Chemin du fichier Excel à créer
     * @throws IOException En cas d'erreur lors de l'écriture du fichier
     */
    public void exportSponsorsToExcel(List<Sponsor> sponsors, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sponsors");

            // Créer le style d'en-tête
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Créer la ligne d'en-tête (sans ID et Logo)
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Nom du Sponsor",
                    "Type",
                    "Email Contact",
                    "Téléphone",
                    "Site Web",
                    "Domaine d'Activité",
                    "Adresse",
                    "Statut",
                    "Date Création"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowNum = 1;
            for (Sponsor sponsor : sponsors) {
                Row row = sheet.createRow(rowNum++);

                int col = 0;
                row.createCell(col++).setCellValue(sponsor.getNomSponsor() != null ? sponsor.getNomSponsor() : "");
                row.createCell(col++).setCellValue(sponsor.getTypeSponsor() != null ? sponsor.getTypeSponsor().getDbValue() : "");
                row.createCell(col++).setCellValue(sponsor.getEmailContact() != null ? sponsor.getEmailContact() : "");
                row.createCell(col++).setCellValue(sponsor.getTelephone() != null ? sponsor.getTelephone() : "");
                row.createCell(col++).setCellValue(sponsor.getSiteWeb() != null ? sponsor.getSiteWeb() : "");
                row.createCell(col++).setCellValue(sponsor.getDomaineActivite() != null ? sponsor.getDomaineActivite() : "");
                row.createCell(col++).setCellValue(sponsor.getAdresse() != null ? sponsor.getAdresse() : "");
                row.createCell(col++).setCellValue(sponsor.getStatut() != null ? sponsor.getStatut().getDbValue() : "");
                row.createCell(col++).setCellValue(sponsor.getCreatedAt() != null ? DATE_FORMAT.format(sponsor.getCreatedAt()) : "");
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
