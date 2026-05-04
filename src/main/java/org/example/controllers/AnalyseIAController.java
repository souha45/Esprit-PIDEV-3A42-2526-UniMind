package org.example.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.example.entities.Questionnaire;
import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.entities.User;
import org.example.services.TraitementIAService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class AnalyseIAController implements Initializable {

    @FXML
    private Label lblTotalTraitements;
    @FXML
    private Label lblTotalSuivis;
    @FXML
    private Label lblTendanceGlobale;
    @FXML
    private Label lblTendanceText;
    @FXML
    private Label lblDateAnalyse;
    @FXML
    private VBox containerAnalyses;
    @FXML
    private Button btnFermer;
    @FXML
    private Button btnRafraichir;

    private TraitementIAService traitementIAService;
    private List<Traitement> traitements;
    private List<SuiviTraitement> suivis;
    private StringBuilder rapportComplet;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        traitementIAService = new TraitementIAService();
        rapportComplet = new StringBuilder();

        // Configuration des boutons
        btnFermer.setOnAction(e -> fermerFenetre());
        btnRafraichir.setOnAction(e -> rafraichirAnalyse());
    }

    public void setDonneesAnalyse(List<Traitement> traitements, List<SuiviTraitement> suivis) {
        this.traitements = traitements;
        this.suivis = suivis;

        // Mettre à jour la date
        lblDateAnalyse.setText("Analyse du " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Effectuer l'analyse
        effectuerAnalyse();
    }

    private void effectuerAnalyse() {
        // Vider le conteneur précédent
        containerAnalyses.getChildren().clear();

        // Réinitialiser le rapport
        rapportComplet = new StringBuilder();
        rapportComplet.append("📊 **RAPPORT D'ANALYSE IA**\n\n");
        rapportComplet.append("Analyse de ").append(traitements.size()).append(" traitement(s)\n");
        rapportComplet.append("Date: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");

        int totalSuivis = 0;
        int traitementsAvecAmelioration = 0;
        int traitementsAvecDetrioration = 0;

        // Analyser chaque traitement
        for (Traitement traitement : traitements) {
            List<SuiviTraitement> suivisDuTraitement = suivis.stream()
                    .filter(s -> s.getTraitementId() == traitement.getTraitementId())
                    .toList();

            totalSuivis += suivisDuTraitement.size();

            // Analyser avec l'IA
            TraitementIAService.ResultatAnalyseSuivi analyse = traitementIAService.analyserSuivisEtudiant(suivisDuTraitement, traitement);

            // Compter les tendances
            if (analyse.getTendance().equals("amélioration")) {
                traitementsAvecAmelioration++;
            } else if (analyse.getTendance().equals("détérioration")) {
                traitementsAvecDetrioration++;
            }

            // Créer la carte d'analyse individuelle
            VBox carteAnalyse = creerCarteAnalyse(traitement, analyse);
            containerAnalyses.getChildren().add(carteAnalyse);

            // Ajouter au rapport texte
            ajouterAuRapportTexte(traitement, analyse);
        }

        // Mettre à jour le résumé global
        mettreAJourResumeGlobal(traitements.size(), totalSuivis, traitementsAvecAmelioration, traitementsAvecDetrioration);

        // Ajouter le résumé global au rapport
        rapportComplet.append("--- **RÉSUMÉ GLOBAL** ---\n");
        rapportComplet.append("Total suivis analysés: ").append(totalSuivis).append("\n");
        rapportComplet.append("Traitements en amélioration: ").append(traitementsAvecAmelioration).append("\n");
        rapportComplet.append("Traitements en détérioration: ").append(traitementsAvecDetrioration).append("\n");

        if (traitementsAvecAmelioration > traitementsAvecDetrioration) {
            rapportComplet.append("📈 **Tendance générale positive**\n");
        } else if (traitementsAvecDetrioration > traitementsAvecAmelioration) {
            rapportComplet.append("📉 **Tendance générale préoccupante**\n");
        } else {
            rapportComplet.append("➡️ **Tendance générale stable**\n");
        }
    }

    private VBox creerCarteAnalyse(Traitement traitement, TraitementIAService.ResultatAnalyseSuivi analyse) {
        VBox carte = new VBox(16);
        carte.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        // Header de la carte
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label iconeTraitement = new Label("🔹");
        iconeTraitement.setStyle("-fx-font-size: 20px;");

        Label titreTraitement = new Label(traitement.getTitre());
        titreTraitement.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label tendanceLabel = new Label(getTendanceIcon(analyse.getTendance()) + " " + capitalize(analyse.getTendance()));
        tendanceLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; " +
                "-fx-text-fill: " + getTendanceColor(analyse.getTendance()) + ";");

        header.getChildren().addAll(iconeTraitement, titreTraitement);
        HBox.setHgrow(titreTraitement, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(tendanceLabel);

        // Contenu de la carte
        VBox contenu = new VBox(12);

        // Score de progression
        HBox scoreBox = new HBox(8);
        scoreBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label scoreLabel = new Label("Score de progression:");
        scoreLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label scoreValue = new Label(String.format("%.1f%%", analyse.getScoreProgression() * 100));
        scoreValue.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + getScoreColor(analyse.getScoreProgression()) + ";");
        scoreBox.getChildren().addAll(scoreLabel, scoreValue);

        // Niveau de risque
        Label risqueLabel = new Label("Niveau de risque: " + capitalize(analyse.getNiveauRisque()));
        risqueLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: " +
                getRisqueColor(analyse.getNiveauRisque()) + "; -fx-font-weight: 600;");

        // Résumé
        TextFlow resumeFlow = new TextFlow();
        Text resumeText = new Text(analyse.getResume());
        resumeText.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 2;");
        resumeFlow.getChildren().add(resumeText);

        contenu.getChildren().addAll(scoreBox, risqueLabel, resumeFlow);

        // Observations
        if (!analyse.getObservations().isEmpty()) {
            VBox observationsBox = creerSectionListe("📝 Observations", analyse.getObservations(), "#0ea5e9");
            contenu.getChildren().add(observationsBox);
        }

        // Recommandations
        if (!analyse.getRecommandations().isEmpty()) {
            VBox recommandationsBox = creerSectionListe("💡 Recommandations", analyse.getRecommandations(), "#10b981");
            contenu.getChildren().add(recommandationsBox);
        }

        carte.getChildren().addAll(header, new javafx.scene.control.Separator(), contenu);
        return carte;
    }

    private VBox creerSectionListe(String titre, List<String> items, String couleur) {
        VBox section = new VBox(8);

        Label titreLabel = new Label(titre);
        titreLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-font-weight: 600; " +
                "-fx-text-fill: " + couleur + ";");

        VBox listeItems = new VBox(4);
        for (String item : items) {
            Label itemLabel = new Label("• " + item);
            itemLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #64748b; " +
                    "-fx-wrap-text: true;");
            listeItems.getChildren().add(itemLabel);
        }

        section.getChildren().addAll(titreLabel, listeItems);
        return section;
    }

    private void mettreAJourResumeGlobal(int totalTraitements, int totalSuivis,
                                         int ameliorations, int deteriorations) {
        lblTotalTraitements.setText(String.valueOf(totalTraitements));
        lblTotalSuivis.setText(String.valueOf(totalSuivis));

        if (ameliorations > deteriorations) {
            lblTendanceGlobale.setText("📈");
            lblTendanceText.setText("Positive");
            lblTendanceText.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else if (deteriorations > ameliorations) {
            lblTendanceGlobale.setText("📉");
            lblTendanceText.setText("Préoccupante");
            lblTendanceText.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        } else {
            lblTendanceGlobale.setText("➡️");
            lblTendanceText.setText("Stable");
            lblTendanceText.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
        }
    }

    private void ajouterAuRapportTexte(Traitement traitement, TraitementIAService.ResultatAnalyseSuivi analyse) {
        rapportComplet.append("🔹 **").append(traitement.getTitre()).append("**\n");
        rapportComplet.append("   Tendance: ").append(analyse.getTendance()).append("\n");
        rapportComplet.append("   Niveau de risque: ").append(analyse.getNiveauRisque()).append("\n");
        rapportComplet.append("   ").append(analyse.getResume()).append("\n");

        if (!analyse.getObservations().isEmpty()) {
            rapportComplet.append("   Observations:\n");
            for (String obs : analyse.getObservations()) {
                rapportComplet.append("   • ").append(obs).append("\n");
            }
        }

        if (!analyse.getRecommandations().isEmpty()) {
            rapportComplet.append("   Recommandations:\n");
            for (String rec : analyse.getRecommandations()) {
                rapportComplet.append("   • ").append(rec).append("\n");
            }
        }
        rapportComplet.append("\n");
    }

    private void fermerFenetre() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }


    private void rafraichirAnalyse() {
        effectuerAnalyse();

        // Animation de rafraîchissement
        btnRafraichir.setText("🔄 Analyse en cours...");
        btnRafraichir.setDisable(true);

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000); // Simuler un traitement
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            btnRafraichir.setText("🔄 Rafraîchir l'analyse");
            btnRafraichir.setDisable(false);
        });
    }

    // Méthodes utilitaires
    private String getTendanceIcon(String tendance) {
        switch (tendance) {
            case "amélioration": return "📈";
            case "détérioration": return "📉";
            default: return "➡️";
        }
    }

    private String getTendanceColor(String tendance) {
        switch (tendance) {
            case "amélioration": return "#10b981";
            case "détérioration": return "#ef4444";
            default: return "#64748b";
        }
    }

    private String getScoreColor(double score) {
        if (score > 0.3) return "#10b981";
        if (score < -0.3) return "#ef4444";
        return "#64748b";
    }

    private String getRisqueColor(String risque) {
        switch (risque) {
            case "élevé": return "#ef4444";
            case "modéré": return "#f59e0b";
            case "faible": return "#3b82f6";
            default: return "#10b981";
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public void setDonnees(Questionnaire questionnaire, double score, String niveau, String interpretation, String reponsesJson, User user) {
    }
}
