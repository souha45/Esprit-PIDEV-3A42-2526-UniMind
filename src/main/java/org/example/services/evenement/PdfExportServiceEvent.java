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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class PdfExportServiceEvent {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final PDFont FONT_HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

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

            // Titre
            contentStream.setFont(FONT_HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("RAPPORT D'ÉVÉNEMENT");
            contentStream.endText();
            y -= 30;

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // Informations de l'événement
            contentStream.setFont(FONT_HELVETICA_BOLD, 14);
            y = addText(contentStream, "Titre:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, evenement.getTitre(), margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Type:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, evenement.getType() != null ? evenement.getType().toString() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Lieu:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, evenement.getLieu(), margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Date début:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, evenement.getDateDebut() != null ? DATE_FORMAT.format(evenement.getDateDebut()) : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Date fin:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, evenement.getDateFin() != null ? DATE_FORMAT.format(evenement.getDateFin()) : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Capacité max:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, String.valueOf(evenement.getCapaciteMax()), margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Inscrits:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, String.valueOf(evenement.getNombreInscrits()), margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Statut:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, evenement.getStatut() != null ? evenement.getStatut().toString() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 20;

            // Description
            contentStream.setFont(FONT_HELVETICA_BOLD, 12);
            y = addText(contentStream, "Description:", margin, y, FONT_HELVETICA_BOLD);
            y -= 5;
            contentStream.setFont(FONT_HELVETICA, 10);
            y = addWrappedText(contentStream, evenement.getDescription(), margin, y, width, FONT_HELVETICA);
            y -= 20;

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
            y = margin + 30;
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
            contentStream.setFont(FONT_HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("LISTE DES ÉVÉNEMENTS");
            contentStream.endText();
            y -= 30;

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // En-têtes de tableau
            contentStream.setFont(FONT_HELVETICA_BOLD, 10);
            y = addText(contentStream, "ID", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Titre", margin + 40, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Type", margin + 200, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Date", margin + 280, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, "Statut", margin + 380, y, FONT_HELVETICA_BOLD);
            y -= 15;

            drawLine(contentStream, margin, y, margin + width, y);
            y -= 10;

            // Données
            contentStream.setFont(FONT_HELVETICA, 9);
            for (Evenement e : evenements) {
                if (y < margin + 50) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = PDRectangle.A4.getHeight() - margin;
                }

                y = addText(contentStream, String.valueOf(e.getEvenementId()), margin, y, FONT_HELVETICA);
                y = addText(contentStream, truncate(e.getTitre(), 25), margin + 40, y, FONT_HELVETICA);
                y = addText(contentStream, e.getType() != null ? e.getType().toString() : "-", margin + 200, y, FONT_HELVETICA);
                y = addText(contentStream, e.getDateDebut() != null ? DATE_ONLY_FORMAT.format(e.getDateDebut()) : "-", margin + 280, y, FONT_HELVETICA);
                y = addText(contentStream, e.getStatut() != null ? e.getStatut().toString() : "-", margin + 380, y, FONT_HELVETICA);
                y -= 12;
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
            contentStream.setFont(FONT_HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("LISTE DES PARTICIPATIONS");
            contentStream.endText();
            y -= 30;

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
            contentStream.setFont(FONT_HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("LISTE DES SPONSORS");
            contentStream.endText();
            y -= 30;

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

            // Titre
            contentStream.setFont(FONT_HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("RAPPORT SPONSOR");
            contentStream.endText();
            y -= 30;

            // Ligne de séparation
            drawLine(contentStream, margin, y, margin + width, y);
            y -= 20;

            // Informations du sponsor
            contentStream.setFont(FONT_HELVETICA_BOLD, 14);
            y = addText(contentStream, "Nom:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getNomSponsor(), margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Type:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getTypeSponsor() != null ? sponsor.getTypeSponsor().toString() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Email:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getEmailContact(), margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Téléphone:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getTelephone() != null ? sponsor.getTelephone() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Site web:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getSiteWeb() != null ? sponsor.getSiteWeb() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Domaine d'activité:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getDomaineActivite() != null ? sponsor.getDomaineActivite() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 10;

            y = addText(contentStream, "Statut:", margin, y, FONT_HELVETICA_BOLD);
            y = addText(contentStream, sponsor.getStatut() != null ? sponsor.getStatut().toString() : "-", margin + 80, y, FONT_HELVETICA);
            y -= 20;

            // Adresse
            contentStream.setFont(FONT_HELVETICA_BOLD, 12);
            y = addText(contentStream, "Adresse:", margin, y, FONT_HELVETICA_BOLD);
            y -= 5;
            contentStream.setFont(FONT_HELVETICA, 10);
            y = addWrappedText(contentStream, sponsor.getAdresse(), margin, y, width, FONT_HELVETICA);
            y -= 20;

            // Date de génération
            y = margin + 30;
            contentStream.setFont(FONT_HELVETICA, 8);
            y = addText(contentStream, "Généré le: " + DATE_FORMAT.format(new java.util.Date()), margin, y, FONT_HELVETICA);

            contentStream.close();
            document.save(outputPath);
        }
    }

    // Méthodes utilitaires

    private float addText(PDPageContentStream contentStream, String text, float x, float y, PDFont font) throws IOException {
        contentStream.setFont(font, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text != null ? text : "-");
        contentStream.endText();
        return y;
    }

    private float addWrappedText(PDPageContentStream contentStream, String text, float x, float y, float maxWidth, PDFont font) throws IOException {
        if (text == null || text.isEmpty()) {
            return y;
        }

        contentStream.setFont(font, 10);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line.length() > 0 ? line + " " + word : word;
            float width = font.getStringWidth(testLine) / 1000 * 12;

            if (width > maxWidth && line.length() > 0) {
                contentStream.beginText();
                contentStream.newLineAtOffset(x, y);
                contentStream.showText(line.toString());
                contentStream.endText();
                y -= 12;
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
            y -= 12;
        }

        return y;
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
