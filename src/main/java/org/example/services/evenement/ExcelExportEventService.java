package org.example.services.evenement;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.entities.Evenement;
import org.example.services.EvenementService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ExcelExportEventService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private EvenementService evenementService;

    public ExcelExportEventService() {
        this.evenementService = new EvenementService();
    }

    /**
     * Exporte la liste des événements vers un fichier Excel
     *
     * @param evenements Liste des événements à exporter
     * @param filePath   Chemin du fichier Excel à créer
     * @throws IOException En cas d'erreur lors de l'écriture du fichier
     */
    public void exportEvenementsToExcel(List<Evenement> evenements, String filePath) throws IOException, SQLException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Événements");

            // Créer le style d'en-tête
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Créer la ligne d'en-tête (sans ID et Description)
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Titre",
                    "Type",
                    "Date Début",
                    "Date Fin",
                    "Lieu",
                    "Capacité Max",
                    "Inscrits",
                    "Places Restantes",
                    "Statut",
                    "Date Limite Inscription",
                    "Date Création",
                    "Organisateur"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowNum = 1;
            for (Evenement event : evenements) {
                Row row = sheet.createRow(rowNum++);

                int col = 0;
                row.createCell(col++).setCellValue(event.getTitre() != null ? event.getTitre() : "");
                row.createCell(col++).setCellValue(event.getType() != null ? event.getType().getDbValue() : "");
                row.createCell(col++).setCellValue(event.getDateDebut() != null ? DATE_FORMAT.format(event.getDateDebut()) : "");
                row.createCell(col++).setCellValue(event.getDateFin() != null ? DATE_FORMAT.format(event.getDateFin()) : "");
                row.createCell(col++).setCellValue(event.getLieu() != null ? event.getLieu() : "");
                row.createCell(col++).setCellValue(event.getCapaciteMax());
                row.createCell(col++).setCellValue(event.getNombreInscrits());
                row.createCell(col++).setCellValue(event.getPlacesRestantes());
                row.createCell(col++).setCellValue(event.getStatut() != null ? event.getStatut().getDbValue() : "");
                row.createCell(col++).setCellValue(event.getDateLimiteInscription() != null ? DATE_FORMAT.format(event.getDateLimiteInscription()) : "");
                row.createCell(col++).setCellValue(event.getDateCreation() != null ? DATE_FORMAT.format(event.getDateCreation()) : "");

                // Récupérer le nom de l'organisateur
                String nomOrganisateur = evenementService.getNomOrganisateur(event.getOrganisateurId());
                row.createCell(col++).setCellValue(nomOrganisateur != null ? nomOrganisateur : "Inconnu");
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
