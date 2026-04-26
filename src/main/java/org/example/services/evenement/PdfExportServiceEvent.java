package org.example.services.evenement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.example.entities.Evenement;
import org.example.entities.Participation;
import org.example.entities.Sponsor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class PdfExportServiceEvent {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final PDFont FONT_HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final float FONT_SIZE_NORMAL = 10f;
    private static final float FONT_SIZE_TITLE = 20f;
    private static final float LINE_HEIGHT = 14f;
    private static final float SECTION_GAP = 10f;
    private static final float LABEL_VALUE_GAP = 10f;
    private static final float TABLE_FONT_SIZE = 9f;
    private static final float TABLE_LINE_HEIGHT = 11f;
    private static final float TABLE_ROW_GAP = 2f;

    /**
     * Exporte un rapport d'événement en PDF
     */
    public void exportEvenementRapport(Evenement evenement, List<Participation> participations, List<Sponsor> sponsors, String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 50;
            float y = PDRectangle.A4.getHeight() - margin;
            float width = PDRectangle.A4.getWidth() - 2 * margin;

            float labelX = margin;
            float valueX = margin + 120;

            // Titre
            y = drawCenteredTitle(contentStream, "RAPPORT D'ÉVÉNEMENT", margin, y, width);

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // Informations de l'événement
            contentStream.setFont(FONT_HELVETICA_BOLD, 14);
            y = addLabelValue(contentStream, "Titre:", evenement.getTitre(), labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Type:", evenement.getType() != null ? evenement.getType().toString() : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Lieu:", evenement.getLieu(), labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Date début:", evenement.getDateDebut() != null ? DATE_FORMAT.format(evenement.getDateDebut()) : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Date fin:", evenement.getDateFin() != null ? DATE_FORMAT.format(evenement.getDateFin()) : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Capacité max:", String.valueOf(evenement.getCapaciteMax()), labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Inscrits:", String.valueOf(evenement.getNombreInscrits()), labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Statut:", evenement.getStatut() != null ? evenement.getStatut().toString() : "-", labelX, valueX, y, width - (valueX - labelX));
            y -= 10;

            // Description
            contentStream.setFont(FONT_HELVETICA_BOLD, 12);
            y = addText(contentStream, "Description:", margin, y, FONT_HELVETICA_BOLD);
            y -= LABEL_VALUE_GAP;
            contentStream.setFont(FONT_HELVETICA, 10);
            y = addWrappedText(contentStream, evenement.getDescription(), margin, y, width, FONT_HELVETICA, FONT_SIZE_NORMAL);
            y -= SECTION_GAP;

            // Participants
            if (participations != null && !participations.isEmpty()) {
                contentStream.setFont(FONT_HELVETICA_BOLD, 14);
                y = addText(contentStream, "PARTICIPANTS (" + participations.size() + ")", margin, y, FONT_HELVETICA_BOLD);
                y -= 10;

                contentStream.setFont(FONT_HELVETICA, 10);
                for (Participation p : participations) {
                    y = addText(contentStream, "- Étudiant ID: " + p.getEtudiantId() + " | Statut: " + p.getStatut() + " | Date inscription: " + 
                        (p.getDateInscription() != null ? DATE_ONLY_FORMAT.format(p.getDateInscription()) : "-"), margin, y, FONT_HELVETICA);
                    y -= 12;
                }
                y -= 10;
            }

            // Sponsors
            if (sponsors != null && !sponsors.isEmpty()) {
                contentStream.setFont(FONT_HELVETICA_BOLD, 14);
                y = addText(contentStream, "SPONSORS (" + sponsors.size() + ")", margin, y, FONT_HELVETICA_BOLD);
                y -= 10;

                contentStream.setFont(FONT_HELVETICA, 10);
                for (Sponsor s : sponsors) {
                    y = addText(contentStream, "- " + s.getNomSponsor() + " | Type: " + s.getTypeSponsor() + " | Email: " + s.getEmailContact(), margin, y, FONT_HELVETICA);
                    y -= 12;
                }
            }

            // Date de génération
            y = margin + 15;
            contentStream.setFont(FONT_HELVETICA, 8);
            y = addText(contentStream, "Généré le: " + DATE_FORMAT.format(new java.util.Date()), margin, y, FONT_HELVETICA);

            contentStream.close();
            document.save(outputPath);
        }
    }

    /**
     * Exporte une liste d'événements en PDF
     */
    public void exportEvenementsList(List<Evenement> evenements, String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 50;
            float y = PDRectangle.A4.getHeight() - margin;
            float width = PDRectangle.A4.getWidth() - 2 * margin;

            // Titre
            y = drawCenteredTitle(contentStream, "LISTE DES ÉVÉNEMENTS", margin, y, width);

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // En-têtes de tableau
            contentStream.setFont(FONT_HELVETICA_BOLD, 10);
            y = addText(contentStream, "Titre", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Type", margin + 220, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Date", margin + 340, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Statut", margin + 410, y, FONT_HELVETICA_BOLD);
            y -= 15;

            drawLine(contentStream, margin, y, margin + width, y);
            y -= 10;

            // Données
            contentStream.setFont(FONT_HELVETICA, TABLE_FONT_SIZE);
            float typeX = margin + 220;
            float dateX = margin + 340;
            float statutX = margin + 410;
            float titleMaxWidth = typeX - margin - 10;
            for (Evenement e : evenements) {
                List<String> titreLines = wrapLines(e.getTitre(), FONT_HELVETICA, TABLE_FONT_SIZE, titleMaxWidth);
                float rowHeight = Math.max(TABLE_LINE_HEIGHT, titreLines.size() * TABLE_LINE_HEIGHT) + TABLE_ROW_GAP;

                if (y - rowHeight < margin + 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = PDRectangle.A4.getHeight() - margin;
                    contentStream.setFont(FONT_HELVETICA, TABLE_FONT_SIZE);
                }

                float rowTopY = y;
                for (int i = 0; i < titreLines.size(); i++) {
                    addTextAtFontSize(contentStream, titreLines.get(i), margin, rowTopY - (i * TABLE_LINE_HEIGHT), FONT_HELVETICA, TABLE_FONT_SIZE);
                }

                addTextAtFontSize(contentStream, truncate(e.getType() != null ? e.getType().toString() : "-", 22), typeX, rowTopY, FONT_HELVETICA, TABLE_FONT_SIZE);
                addTextAtFontSize(contentStream, e.getDateDebut() != null ? DATE_ONLY_FORMAT.format(e.getDateDebut()) : "-", dateX, rowTopY, FONT_HELVETICA, TABLE_FONT_SIZE);
                addTextAtFontSize(contentStream, e.getStatut() != null ? e.getStatut().toString() : "-", statutX, rowTopY, FONT_HELVETICA, TABLE_FONT_SIZE);

                y -= rowHeight;
            }

            contentStream.close();
            document.save(outputPath);
        }
    }

    /**
     * Exporte une liste de participations en PDF
     */
    public void exportParticipationsList(List<Participation> participations, String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 50;
            float y = PDRectangle.A4.getHeight() - margin;
            float width = PDRectangle.A4.getWidth() - 2 * margin;

            // Titre
            y = drawCenteredTitle(contentStream, "LISTE DES PARTICIPATIONS", margin, y, width);

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // En-têtes de tableau
            contentStream.setFont(FONT_HELVETICA_BOLD, 10);
            y = addText(contentStream, "ID", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Événement ID", margin + 40, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Étudiant ID", margin + 120, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Statut", margin + 220, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Date inscription", margin + 300, y, FONT_HELVETICA_BOLD);
            y -= 15;

            drawLine(contentStream, margin, y, margin + width, y);
            y -= 10;

            // Données
            contentStream.setFont(FONT_HELVETICA, 9);
            for (Participation p : participations) {
                if (y < margin + 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = PDRectangle.A4.getHeight() - margin;
                }

                y = addText(contentStream, String.valueOf(p.getParticipationId()), margin, y, FONT_HELVETICA);
                y = addText(contentStream, String.valueOf(p.getEvenementId()), margin + 40, y, FONT_HELVETICA);
                y = addText(contentStream, String.valueOf(p.getEtudiantId()), margin + 120, y, FONT_HELVETICA);
                y = addText(contentStream, p.getStatut() != null ? p.getStatut().toString() : "-", margin + 220, y, FONT_HELVETICA);
                y = addText(contentStream, p.getDateInscription() != null ? DATE_ONLY_FORMAT.format(p.getDateInscription()) : "-", margin + 300, y, FONT_HELVETICA);
                y -= 12;
            }

            contentStream.close();
            document.save(outputPath);
        }
    }

    /**
     * Exporte une liste de sponsors en PDF
     */
    public void exportSponsorsList(List<Sponsor> sponsors, String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 50;
            float y = PDRectangle.A4.getHeight() - margin;
            float width = PDRectangle.A4.getWidth() - 2 * margin;

            // Titre
            y = drawCenteredTitle(contentStream, "LISTE DES SPONSORS", margin, y, width);

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // En-têtes de tableau
            contentStream.setFont(FONT_HELVETICA_BOLD, 10);
            y = addText(contentStream, "ID", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Nom", margin + 40, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Type", margin + 200, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Email", margin + 280, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Statut", margin + 380, y, FONT_HELVETICA_BOLD);
            y -= 15;

            drawLine(contentStream, margin, y, margin + width, y);
            y -= 10;

            // Données
            contentStream.setFont(FONT_HELVETICA, 9);
            for (Sponsor s : sponsors) {
                if (y < margin + 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = PDRectangle.A4.getHeight() - margin;
                }

                y = addText(contentStream, String.valueOf(s.getSponsorId()), margin, y, FONT_HELVETICA);
                y = addText(contentStream, truncate(s.getNomSponsor(), 25), margin + 40, y, FONT_HELVETICA);
                y = addText(contentStream, s.getTypeSponsor() != null ? s.getTypeSponsor().toString() : "-", margin + 200, y, FONT_HELVETICA);
                y = addText(contentStream, truncate(s.getEmailContact(), 20), margin + 280, y, FONT_HELVETICA);
                y = addText(contentStream, s.getStatut() != null ? s.getStatut().toString() : "-", margin + 380, y, FONT_HELVETICA);
                y -= 12;
            }

            contentStream.close();
            document.save(outputPath);
        }
    }

    /**
     * Exporte un rapport de sponsor en PDF
     */
    public void exportSponsorRapport(Sponsor sponsor, String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 50;
            float y = PDRectangle.A4.getHeight() - margin;
            float width = PDRectangle.A4.getWidth() - 2 * margin;

            float labelX = margin;
            float valueX = margin + 120;

            // Titre
            y = drawCenteredTitle(contentStream, "RAPPORT SPONSOR", margin, y, width);

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // Informations du sponsor
            contentStream.setFont(FONT_HELVETICA_BOLD, 14);
            y = addLabelValue(contentStream, "Nom:", sponsor.getNomSponsor(), labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Type:", sponsor.getTypeSponsor() != null ? sponsor.getTypeSponsor().toString() : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Email:", sponsor.getEmailContact(), labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Téléphone:", sponsor.getTelephone() != null ? sponsor.getTelephone() : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Site web:", sponsor.getSiteWeb() != null ? sponsor.getSiteWeb() : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Domaine d'activité:", sponsor.getDomaineActivite() != null ? sponsor.getDomaineActivite() : "-", labelX, valueX, y, width - (valueX - labelX));
            y = addLabelValue(contentStream, "Statut:", sponsor.getStatut() != null ? sponsor.getStatut().toString() : "-", labelX, valueX, y, width - (valueX - labelX));
            y -= 10;

            // Adresse
            contentStream.setFont(FONT_HELVETICA_BOLD, 12);
            y = addText(contentStream, "Adresse:", margin, y, FONT_HELVETICA_BOLD);
            y -= LABEL_VALUE_GAP;
            contentStream.setFont(FONT_HELVETICA, 10);
            y = addWrappedText(contentStream, sponsor.getAdresse(), margin, y, width, FONT_HELVETICA, FONT_SIZE_NORMAL);
            y -= SECTION_GAP;

            // Date de génération
            y = margin + 15;
            contentStream.setFont(FONT_HELVETICA, 8);
            y = addText(contentStream, "Généré le: " + DATE_FORMAT.format(new java.util.Date()), margin, y, FONT_HELVETICA);

            contentStream.close();
            document.save(outputPath);
        }
    }

    // Méthodes utilitaires

    private float addText(PDPageContentStream contentStream, String text, float x, float y, PDFont font) throws IOException {
        contentStream.setFont(font, FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text != null ? text : "-");
        contentStream.endText();
        return y;
    }

    private void addTextAtFontSize(PDPageContentStream contentStream, String text, float x, float y, PDFont font, float fontSize) throws IOException {
        contentStream.setFont(font, fontSize);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text != null ? text : "-");
        contentStream.endText();
    }

    private List<String> wrapLines(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();

        String safeText = (text == null || text.isBlank()) ? "-" : text.trim();
        String[] words = safeText.split("\\s+");

        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.length() > 0 ? line + " " + word : word;
            float width = font.getStringWidth(testLine) / 1000f * fontSize;

            if (width > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = line.length() > 0 ? line.append(" ").append(word) : line.append(word);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines;
    }

    private float addWrappedText(PDPageContentStream contentStream, String text, float x, float y, float maxWidth, PDFont font, float fontSize) throws IOException {
        if (text == null || text.isEmpty()) {
            return y;
        }

        contentStream.setFont(font, fontSize);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line.length() > 0 ? line + " " + word : word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;

            if (width > maxWidth && line.length() > 0) {
                contentStream.beginText();
                contentStream.newLineAtOffset(x, y);
                contentStream.showText(line.toString());
                contentStream.endText();
                y -= LINE_HEIGHT;
                line = new StringBuilder(word);
            } else {
                line = line.length() > 0 ? line.append(" ").append(word) : line.append(word);
            }
        }

        if (line.length() > 0) {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(line.toString());
            contentStream.endText();
            y -= LINE_HEIGHT;
        }

        return y;
    }

    private float addLabelValue(PDPageContentStream contentStream, String label, String value, float labelX, float valueX, float y, float valueMaxWidth) throws IOException {
        contentStream.setFont(FONT_HELVETICA_BOLD, FONT_SIZE_NORMAL);
        addText(contentStream, label, labelX, y, FONT_HELVETICA_BOLD);
        contentStream.setFont(FONT_HELVETICA, FONT_SIZE_NORMAL);
        float nextY = addWrappedText(contentStream, value != null ? value : "-", valueX, y, valueMaxWidth, FONT_HELVETICA, FONT_SIZE_NORMAL);
        if (nextY == y) {
            nextY = y - LINE_HEIGHT;
        }
        return nextY;
    }

    private float drawCenteredTitle(PDPageContentStream contentStream, String title, float margin, float y, float width) throws IOException {
        contentStream.setFont(FONT_HELVETICA_BOLD, FONT_SIZE_TITLE);
        float titleWidth = FONT_HELVETICA_BOLD.getStringWidth(title) / 1000f * FONT_SIZE_TITLE;
        float titleX = margin + Math.max(0, (width - titleWidth) / 2f);

        contentStream.beginText();
        contentStream.newLineAtOffset(titleX, y);
        contentStream.showText(title);
        contentStream.endText();

        return y - 30;
    }

    private void drawLine(PDPageContentStream contentStream, float x1, float y1, float x2, float y2) throws IOException {
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "-";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
