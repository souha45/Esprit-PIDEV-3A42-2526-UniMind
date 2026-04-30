package org.example.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.example.entities.Traitement;
import org.example.entities.User;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

public class OrdonnancePDFService {

    //  Palette de couleurs
    private static final DeviceRgb VIOLET_FONCE  = new DeviceRgb(79,  70,  229); // #4f46e5
    private static final DeviceRgb VIOLET_CLAIR  = new DeviceRgb(237, 233, 254); // #ede9fe
    private static final DeviceRgb VIOLET_MEDIUM = new DeviceRgb(99,  102, 241); // #6366f1
    private static final DeviceRgb GRIS_FONCE    = new DeviceRgb(31,  41,  55);  // #1f2937
    private static final DeviceRgb GRIS_MOYEN    = new DeviceRgb(107, 114, 128); // #6b7280
    private static final DeviceRgb GRIS_CLAIR    = new DeviceRgb(249, 250, 251); // #f9fafb
    private static final DeviceRgb BLANC         = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb VERT          = new DeviceRgb(16,  185, 129); // #10b981
    private static final DeviceRgb BORDURE       = new DeviceRgb(224, 231, 255); // #e0e7ff

    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    //  API publique

    /**
     * @param traitement  Le traitement à exporter
     * @param psychologue Le psychologue prescripteur
     * @param patient     L'étudiant concerné (pour afficher nom/prénom)
     */
    public byte[] genererOrdonnancePDF(Traitement traitement,
                                       User psychologue,
                                       User patient) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter   writer = new PdfWriter(out);
             PdfDocument pdf    = new PdfDocument(writer);
             Document    doc    = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 50, 36, 50);

            ajouterEntete(doc, psychologue);
            ajouterBandeau(doc, traitement);
            ajouterInfosPatient(doc, patient);
            ajouterDetailsTraitement(doc, traitement);
            ajouterInstructions(doc, traitement);
            ajouterSignature(doc, psychologue);
        }
        return out.toByteArray();
    }

    /** Surcharge sans objet patient (rétrocompatibilité) */
    public byte[] genererOrdonnancePDF(Traitement traitement,
                                       User psychologue) throws IOException {
        return genererOrdonnancePDF(traitement, psychologue, null);
    }

    public String genererNomFichier(Traitement traitement) {
        String titreNettoye = traitement.getTitre() != null
                ? traitement.getTitre().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_")
                : "traitement";
        return String.format("ordonnance_%s_%s.pdf",
                titreNettoye,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    //  Sections

    private void ajouterEntete(Document doc, User psy) {
        // Bandeau violet en-tête
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(VIOLET_FONCE)
                .setMarginBottom(0);

        // Colonne gauche — nom du praticien
        Cell gauche = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(18)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        gauche.add(new Paragraph("UNIMIND — Santé Mentale")
                .setFontColor(new DeviceRgb(196, 181, 253))
                .setFontSize(9)
                .setMarginBottom(4));
        gauche.add(new Paragraph("Dr. " + nomComplet(psy))
                .setFontColor(BLANC)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(2));
        gauche.add(new Paragraph("Psychologue clinicien")
                .setFontColor(new DeviceRgb(196, 181, 253))
                .setFontSize(10));
        header.addCell(gauche);

        // Colonne droite — coordonnées
        Cell droite = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(18)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT);
        droite.add(new Paragraph("ORDONNANCE MÉDICALE")
                .setFontColor(BLANC)
                .setFontSize(13)
                .setBold()
                .setMarginBottom(6));
        droite.add(new Paragraph("N° " + traitement(psy))
                .setFontColor(new DeviceRgb(196, 181, 253))
                .setFontSize(9)
                .setMarginBottom(2));
        if (psy != null && psy.getEmail() != null) {
            droite.add(new Paragraph(psy.getEmail())
                    .setFontColor(new DeviceRgb(196, 181, 253))
                    .setFontSize(9));
        }
        header.addCell(droite);

        doc.add(header);

        // Barre colorée fine sous l'entête
        doc.add(new Paragraph("")
                .setBackgroundColor(VERT)
                .setHeight(4)
                .setMarginBottom(20));
    }

    private void ajouterBandeau(Document doc, Traitement traitement) {
        // Pastille de statut
        String statut = traitement.getStatut() != null
                ? traitement.getStatut().name() : "EN_COURS";
        DeviceRgb couleurStatut = "TERMINE".equals(statut) ? VERT
                : "SUSPENDU".equals(statut) ? new DeviceRgb(245, 158, 11)
                : VIOLET_MEDIUM;
        String libelleStatut = "EN_COURS".equals(statut) ? "En cours"
                : "TERMINE".equals(statut) ? "Terminé" : "Suspendu";

        Table bandeau = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(VIOLET_CLAIR)
                .setBorder(new SolidBorder(BORDURE, 1))
                .setMarginBottom(20);

        Cell gauche = new Cell().setBorder(Border.NO_BORDER).setPadding(12);
        gauche.add(new Paragraph("Traitement : " + (traitement.getTitre() != null ? traitement.getTitre() : "Non spécifié"))
                .setFontColor(VIOLET_FONCE).setFontSize(14).setBold());
        bandeau.addCell(gauche);

        Cell droite = new Cell().setBorder(Border.NO_BORDER).setPadding(12)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT);
        droite.add(new Paragraph("Statut")
                .setFontColor(GRIS_MOYEN).setFontSize(8).setMarginBottom(3));
        droite.add(new Paragraph("● " + libelleStatut)
                .setFontColor(couleurStatut).setFontSize(11).setBold());
        bandeau.addCell(droite);

        doc.add(bandeau);
    }

    private void ajouterInfosPatient(Document doc, User patient) {
        doc.add(titreSousSection("👤  Informations Patient"));

        Table t = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20)
                .setBorder(new SolidBorder(BORDURE, 1));

        String nomPatient = patient != null
                ? (patient.getPrenom() + " " + patient.getNom()).trim()
                : "Non renseigné";
        String emailPatient = (patient != null && patient.getEmail() != null)
                ? patient.getEmail() : "—";

        ligneTableau(t, "Nom complet", nomPatient);
        ligneTableau(t, "Email", emailPatient);

        doc.add(t);
    }

    private void ajouterDetailsTraitement(Document doc, Traitement traitement) {
        doc.add(titreSousSection("💊  Détails du Traitement Prescrit"));

        Table t = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(16)
                .setBorder(new SolidBorder(BORDURE, 1));

        ligneTableau(t, "Catégorie",
                traitement.getCategorie() != null ? traitement.getCategorieLabel() : "—");
        ligneTableau(t, "Priorité",
                traitement.getPriorite() != null ? traitement.getPrioriteLabel() : "Normale");
        ligneTableau(t, "Date de début",
                traitement.getDateDebut() != null
                        ? traitement.getDateDebut().toLocalDate().format(DATE_FR) : "Immédiat");
        ligneTableau(t, "Durée",
                traitement.getDureeJours() + " jour(s)");
        ligneTableau(t, "Dosage",
                traitement.getDosage() != null ? traitement.getDosage() : "—");
        ligneTableau(t, "Type",
                traitement.getType() != null ? traitement.getType() : "—");

        doc.add(t);

        // Description
        if (nonVide(traitement.getDescription())) {
            doc.add(blocTexte("Description", traitement.getDescription()));
        }

        // Objectifs
        if (nonVide(traitement.getObjectifTherapeutique())) {
            doc.add(blocTexte("Objectifs thérapeutiques",
                    traitement.getObjectifTherapeutique()));
        }
    }

    private void ajouterInstructions(Document doc, Traitement traitement) {
        doc.add(titreSousSection("📋  Instructions et Recommandations"));

        String[] items = {
                "Suivre rigoureusement la posologie et le protocole prescrit",
                "Respecter les horaires et la fréquence des séances",
                "Ne pas interrompre le traitement sans avis du praticien",
                "Signaler tout changement d'état ou effet indésirable",
                "Tenir un journal de bord de suivi du traitement",
                "Contacter le cabinet en cas d'urgence ou de question"
        };

        for (String item : items) {
            doc.add(new Paragraph("▸  " + item)
                    .setFontColor(GRIS_FONCE)
                    .setFontSize(10)
                    .setMarginLeft(10)
                    .setMarginBottom(4));
        }

        doc.add(new Paragraph("").setMarginBottom(20));
    }

    private void ajouterSignature(Document doc, User psy) {
        // Ligne de séparation
        doc.add(new Paragraph("")
                .setBorderTop(new SolidBorder(BORDURE, 1))
                .setMarginBottom(20));

        Table sig = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));

        // Colonne gauche — date
        Cell dateCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
        dateCell.add(new Paragraph("Date d'émission")
                .setFontColor(GRIS_MOYEN).setFontSize(9).setMarginBottom(4));
        dateCell.add(new Paragraph(LocalDate.now().format(DATE_FR))
                .setFontColor(GRIS_FONCE).setFontSize(11).setBold());
        sig.addCell(dateCell);

        // Colonne droite — signature
        Cell sigCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT);
        sigCell.add(new Paragraph("Signature & Cachet")
                .setFontColor(GRIS_MOYEN).setFontSize(9).setMarginBottom(4));
        sigCell.add(new Paragraph("Dr. " + nomComplet(psy))
                .setFontColor(VIOLET_FONCE).setFontSize(12).setBold());
        sigCell.add(new Paragraph("Psychologue — Unimind")
                .setFontColor(GRIS_MOYEN).setFontSize(9));
        sig.addCell(sigCell);

        doc.add(sig);

        // Pied de page
        doc.add(new Paragraph(
                "Document confidentiel — Ordonnance émise par la plateforme Unimind · " +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(GRIS_MOYEN)
                .setFontSize(8)
                .setMarginTop(10)
                .setBorderTop(new SolidBorder(BORDURE, 1))
                .setPaddingTop(8));
    }

    //  Helpers

    private Paragraph titreSousSection(String texte) {
        return new Paragraph(texte)
                .setFontColor(VIOLET_FONCE)
                .setFontSize(12)
                .setBold()
                .setMarginBottom(8)
                .setBorderBottom(new SolidBorder(BORDURE, 1))
                .setPaddingBottom(4);
    }

    private void ligneTableau(Table table, String label, String valeur) {
        // Cellule label (fond violet clair)
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setFontColor(VIOLET_FONCE)
                        .setFontSize(10)
                        .setBold())
                .setBackgroundColor(VIOLET_CLAIR)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDURE, 1));

        // Cellule valeur (fond blanc cassé)
        Cell valeurCell = new Cell()
                .add(new Paragraph(valeur)
                        .setFontColor(GRIS_FONCE)
                        .setFontSize(10))
                .setBackgroundColor(GRIS_CLAIR)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDURE, 1));

        table.addCell(labelCell);
        table.addCell(valeurCell);
    }

    private Div blocTexte(String titre, String contenu) {
        Div div = new Div()
                .setBackgroundColor(GRIS_CLAIR)
                .setBorder(new SolidBorder(BORDURE, 1))
                .setPadding(12)
                .setMarginBottom(12);
        div.add(new Paragraph(titre)
                .setFontColor(VIOLET_FONCE)
                .setFontSize(10)
                .setBold()
                .setMarginBottom(6));
        div.add(new Paragraph(contenu)
                .setFontColor(GRIS_FONCE)
                .setFontSize(10)
                .setMarginBottom(0));
        return div;
    }

    private String nomComplet(User user) {
        if (user == null) return "Inconnu";
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom    = user.getNom()    != null ? user.getNom()    : "";
        return (prenom + " " + nom).trim();
    }

    private String traitement(User user) {
        return user != null ? String.valueOf(user.getUserId()) : "—";
    }

    private boolean nonVide(String s) {
        return s != null && !s.trim().isEmpty();
    }
}