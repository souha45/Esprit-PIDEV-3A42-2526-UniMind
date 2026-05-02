package org.example.services.evenement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.entities.Evenement;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class PdfExportEventDetailsService {

    private final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public void exportEventDetails(Evenement event, String organisateurNom, File file) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 40;
                float pageWidth = PDRectangle.A4.getWidth();
                float y = PDRectangle.A4.getHeight() - margin;

                cs.setFont(FONT_BOLD, 18);
                cs.setNonStrokingColor(0.49f, 0.23f, 0.93f);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("UniMind - Fiche Événement");
                cs.endText();
                y -= 28;

                cs.setFont(FONT_REGULAR, 10);
                cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Exporté le : " + DATE_FORMAT.format(new java.util.Date()));
                cs.endText();
                y -= 18;

                cs.setStrokingColor(0.49f, 0.23f, 0.93f);
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 22;

                y = writeField(cs, margin, y, "Titre", safe(event.getTitre()));
                y = writeField(cs, margin, y, "Type", event.getType() != null ? event.getType().toString() : "-");
                y = writeField(cs, margin, y, "Statut", event.getStatut() != null ? event.getStatut().toString() : "-");
                y = writeField(cs, margin, y, "Date début", event.getDateDebut() != null ? DATE_FORMAT.format(event.getDateDebut()) : "-");
                y = writeField(cs, margin, y, "Date fin", event.getDateFin() != null ? DATE_FORMAT.format(event.getDateFin()) : "-");
                y = writeField(cs, margin, y, "Lieu", safe(event.getLieu()));
                y = writeField(cs, margin, y, "Capacité max", String.valueOf(event.getCapaciteMax()));
                y = writeField(cs, margin, y, "Inscrits", String.valueOf(event.getNombreInscrits()));
                y = writeField(cs, margin, y, "Organisateur", safe(organisateurNom));

                cs.setFont(FONT_BOLD, 12);
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Description");
                cs.endText();
                y -= 16;

                cs.setFont(FONT_REGULAR, 10);
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                y = writeMultiline(cs, margin, y, safe(event.getDescription()), pageWidth - 2 * margin);
            }

            doc.save(file);
        }
    }

    private float writeField(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.setFont(FONT_BOLD, 11);
        cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(label + " : ");
        cs.endText();

        cs.setFont(FONT_REGULAR, 11);
        cs.beginText();
        cs.newLineAtOffset(x + 120, y);
        cs.showText(value);
        cs.endText();
        return y - 18;
    }

    private float writeMultiline(PDPageContentStream cs, float x, float y, String text, float width) throws IOException {
        if (text == null || text.isBlank()) {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText("-");
            cs.endText();
            return y - 14;
        }

        String[] words = text.replace("\r", " ").replace("\n", " ").split("\\s+");
        StringBuilder line = new StringBuilder();
        float fontSize = 10;

        for (String w : words) {
            String candidate = line.isEmpty() ? w : line + " " + w;
            float textWidth = FONT_REGULAR.getStringWidth(candidate) / 1000 * fontSize;
            if (textWidth > width) {
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(line.toString());
                cs.endText();
                y -= 14;
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty()) {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(line.toString());
            cs.endText();
            y -= 14;
        }

        return y;
    }

    private String safe(String v) {
        return v == null ? "-" : v;
    }
}
