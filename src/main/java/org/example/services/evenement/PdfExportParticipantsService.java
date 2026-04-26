package org.example.services.evenement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.services.ParticipationService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class PdfExportParticipantsService {

    private final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public void exportParticipants(String eventTitle, List<ParticipationService.ParticipantInfo> participants, File file) throws IOException {
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
                cs.showText("UniMind - Participants");
                cs.endText();
                y -= 22;

                cs.setFont(FONT_BOLD, 12);
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Événement : " + (eventTitle != null ? eventTitle : "-"));
                cs.endText();
                y -= 16;

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
                y -= 18;

                float[] colX = {margin, 250, 400};
                String[] headers = {"Nom", "Email", "Date Inscription"};

                cs.setFont(FONT_BOLD, 11);
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                for (int i = 0; i < headers.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(colX[i], y);
                    cs.showText(headers[i]);
                    cs.endText();
                }
                y -= 6;

                cs.setStrokingColor(0.8f, 0.8f, 0.8f);
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 14;

                cs.setFont(FONT_REGULAR, 10);
                boolean alt = false;

                for (ParticipationService.ParticipantInfo p : participants) {
                    if (y < 60) {
                        break;
                    }

                    if (alt) {
                        cs.setNonStrokingColor(0.97f, 0.95f, 1.0f);
                        cs.addRect(margin, y - 3, pageWidth - 2 * margin, 16);
                        cs.fill();
                    }
                    alt = !alt;

                    cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);

                    cs.beginText();
                    cs.newLineAtOffset(colX[0], y);
                    cs.showText((p.getPrenom() != null ? p.getPrenom() : "") + " " + (p.getNom() != null ? p.getNom() : ""));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(colX[1], y);
                    cs.showText(p.getEmail() != null ? p.getEmail() : "-");
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(colX[2], y);
                    cs.showText(p.getDateInscription() != null ? DATE_FORMAT.format(p.getDateInscription()) : "-");
                    cs.endText();

                    y -= 18;
                }

                y -= 10;
                cs.setFont(FONT_BOLD, 10);
                cs.setNonStrokingColor(0.49f, 0.23f, 0.93f);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Total : " + participants.size() + " participant(s)");
                cs.endText();
            }

            doc.save(file);
        }
    }
}
