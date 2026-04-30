package org.example.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.enums.CategorieTraitement;
import org.example.enums.PrioriteTraitement;
import org.example.enums.StatutTraitement;

/**
 * Service d'IA pour la génération intelligente de traitements et l'analyse des suivis
 */
public class TraitementIAService {

    // ==================== CLASSE POUR VALIDATION ====================

    public static class ResultatValidation {
        private boolean valid;
        private String message;
        private String symptomePrincipal;

        public ResultatValidation(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public ResultatValidation(boolean valid, String message, String symptomePrincipal) {
            this.valid = valid;
            this.message = message;
            this.symptomePrincipal = symptomePrincipal;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getSymptomePrincipal() { return symptomePrincipal; }
    }

    // ==================== CLASSE POUR RÉSULTAT ANALYSE ====================

    public static class ResultatAnalyseSuivi {
        private final String tendance;
        private final double scoreProgression;
        private final List<String> observations;
        private final List<String> recommandations;
        private final String resume;
        private final String niveauRisque;

        public ResultatAnalyseSuivi(String tendance, double scoreProgression,
                                    List<String> observations, List<String> recommandations,
                                    String resume, String niveauRisque) {
            this.tendance = tendance;
            this.scoreProgression = scoreProgression;
            this.observations = observations;
            this.recommandations = recommandations;
            this.resume = resume;
            this.niveauRisque = niveauRisque;
        }

        public String getTendance() { return tendance; }
        public double getScoreProgression() { return scoreProgression; }
        public List<String> getObservations() { return observations; }
        public List<String> getRecommandations() { return recommandations; }
        public String getResume() { return resume; }
        public String getNiveauRisque() { return niveauRisque; }
    }

    // ==================== ATTRIBUTS ====================

    private Random random = new Random();

    // Base de connaissances des symptômes et traitements associés
    private Map<String, List<String>> symptomesTraitements = new HashMap<>();
    private Map<String, List<String>> categoriesSpecifiques = new HashMap<>();

    public TraitementIAService() {
        initialiserBaseConnaissances();
    }

    // ==================== BASE DE CONNAISSANCES ====================

    private void initialiserBaseConnaissances() {
        // Symptômes -> Types de traitements recommandés
        symptomesTraitements.put("anxiété", Arrays.asList(
                "Thérapie cognitivo-comportementale (TCC)",
                "Thérapie émotionnelle",
                "Méditation pleine conscience",
                "Relaxation",
                "Suivi psychologique"
        ));

        symptomesTraitements.put("dépression", Arrays.asList(
                "Thérapie cognitive",
                "Psychothérapie",
                "Suivi psychologique",
                "Thérapie comportementale",
                "Coaching"
        ));

        symptomesTraitements.put("stress", Arrays.asList(
                "Relaxation",
                "Méditation pleine conscience",
                "Thérapie émotionnelle",
                "Gestion du stress",
                "Suivi psychologique"
        ));

        symptomesTraitements.put("trouble du sommeil", Arrays.asList(
                "Relaxation",
                "Méditation pleine conscience",
                "Thérapie comportementale",
                "Hygiène du sommeil",
                "Suivi psychologique"
        ));

        symptomesTraitements.put("phobie", Arrays.asList(
                "Thérapie cognitivo-comportementale (TCC)",
                "Thérapie comportementale",
                "Désensibilisation systématique",
                "Psychothérapie",
                "Bilan psychologique"
        ));

        symptomesTraitements.put("burnout", Arrays.asList(
                "Thérapie émotionnelle",
                "Coaching",
                "Gestion du stress",
                "Relaxation",
                "Suivi psychologique"
        ));

        // Catégories spécifiques selon les besoins
        categoriesSpecifiques.put("anxiété", Arrays.asList("EMOTIONNEL", "COGNITIF", "RELAXATION"));
        categoriesSpecifiques.put("dépression", Arrays.asList("EMOTIONNEL", "COGNITIF", "COMPORTEMENTAL"));
        categoriesSpecifiques.put("stress", Arrays.asList("RELAXATION", "EMOTIONNEL", "COGNITIF"));
        categoriesSpecifiques.put("trouble du sommeil", Arrays.asList("RELAXATION", "EMOTIONNEL", "COGNITIF"));
        categoriesSpecifiques.put("phobie", Arrays.asList("COMPORTEMENTAL", "COGNITIF", "EMOTIONNEL"));
        categoriesSpecifiques.put("burnout", Arrays.asList("EMOTIONNEL", "RELAXATION", "COGNITIF"));
    }

    // ==================== VALIDATION DES SYMPTÔMES ====================

    public ResultatValidation validerSymptomes(String symptomes, String description) {
        if (symptomes == null || symptomes.trim().isEmpty()) {
            return new ResultatValidation(false, "Veuillez décrire les symptômes");
        }

        symptomes = symptomes.toLowerCase().trim();

        boolean symptomeValide = false;
        String symptomeTrouve = null;

        for (String symptomeConn : symptomesTraitements.keySet()) {
            if (symptomes.contains(symptomeConn)) {
                symptomeValide = true;
                symptomeTrouve = symptomeConn;
                break;
            }
        }

        boolean descriptionValide = false;
        if (description != null && !description.trim().isEmpty()) {
            String desc = description.toLowerCase();
            String[] motsClesPertinents = {
                    "anxiété", "anxieux", "peur", "angoisse", "stress", "dépression", "dépressif",
                    "triste", "fatigué", "épuisé", "burnout", "insomnie", "sommeil", "cauchemar",
                    "phobie", "panique", "crise", "nervosité", "tension", "inquiétude", "souci",
                    "moral", "humeur", "confiance", "estime", "concentration", "mémoire",
                    "appétit", "isolé", "seul", "social", "relation", "travail", "étude"
            };
            for (String mot : motsClesPertinents) {
                if (desc.contains(mot)) {
                    descriptionValide = true;
                    break;
                }
            }
        }

        if (symptomes.length() < 3) {
            return new ResultatValidation(false, "La description des symptômes est trop courte");
        }

        if (symptomes.matches("[^a-zA-Z\\sàâäéèêëïîôöùûüÿç]+") ||
                symptomes.replaceAll("[^a-zA-Z\\sàâäéèêëïîôöùûüÿç]", "").length() < 3) {
            return new ResultatValidation(false, "Les symptômes semblent incohérents.");
        }

        if (!symptomeValide && !descriptionValide) {
            return new ResultatValidation(false, "Les symptômes décrits ne semblent pas pertinents pour un suivi psychologique.");
        }

        String symptomePrincipal = symptomeTrouve != null ? symptomeTrouve : "stress";
        return new ResultatValidation(true, "Symptômes valides", symptomePrincipal);
    }

    // ==================== GÉNÉRATION DE TRAITEMENT ====================

    public Traitement genererTraitementPersonnalise(String symptomes, String description, int age, String niveauEtudes) {
        ResultatValidation validation = validerSymptomes(symptomes, description);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }

        Traitement traitement = new Traitement();
        String symptomePrincipal = validation.getSymptomePrincipal();

        traitement.setTitre(genererTitreTraitement(symptomePrincipal));
        traitement.setType(choisirTypeTraitement(symptomePrincipal));
        traitement.setCategorie(choisirCategorie(symptomePrincipal));
        traitement.setPriorite(evaluerPriorite(symptomes, description));
        traitement.setDureeJours(evaluerDuree(symptomePrincipal, niveauEtudes));
        traitement.setObjectifTherapeutique(genererObjectifs(symptomePrincipal));
        traitement.setDescription(genererDescription(symptomePrincipal, age));
        traitement.setStatut(StatutTraitement.EN_COURS);
        traitement.setDosage(genererDosage(symptomePrincipal));

        return traitement;
    }

    private String genererTitreTraitement(String symptome) {
        String[] prefixes = {"Prise en charge de ", "Accompagnement thérapeutique - ",
                "Programme de soin - ", "Thérapie ciblée - "};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        switch (symptome) {
            case "anxiété": return prefix + "l'anxiété et les troubles anxieux";
            case "dépression": return prefix + "la dépression et l'humeur";
            case "stress": return prefix + "la gestion du stress";
            case "trouble du sommeil": return prefix + "des troubles du sommeil";
            case "phobie": return prefix + "des phobies et peurs";
            case "burnout": return prefix + "du burnout et épuisement";
            default: return prefix + "du bien-être psychologique";
        }
    }

    private String choisirTypeTraitement(String symptome) {
        List<String> types = symptomesTraitements.get(symptome);
        return (types != null && !types.isEmpty()) ? types.get(random.nextInt(types.size())) : "Suivi psychologique";
    }

    private CategorieTraitement choisirCategorie(String symptome) {
        List<String> categories = categoriesSpecifiques.get(symptome);
        if (categories != null && !categories.isEmpty()) {
            String catStr = categories.get(random.nextInt(categories.size()));
            try {
                return CategorieTraitement.valueOf(catStr);
            } catch (IllegalArgumentException e) {
                return CategorieTraitement.EMOTIONNEL;
            }
        }
        return CategorieTraitement.EMOTIONNEL;
    }

    private PrioriteTraitement evaluerPriorite(String symptomes, String description) {
        symptomes = symptomes.toLowerCase();
        description = description.toLowerCase();
        String[] motsUrgence = {"urgent", "crise", "grave", "sévère", "difficile", "insupportable"};
        for (String mot : motsUrgence) {
            if (symptomes.contains(mot) || description.contains(mot)) {
                return PrioriteTraitement.HAUTE;
            }
        }
        if (symptomes.contains("dépression") || symptomes.contains("phobie")) return PrioriteTraitement.HAUTE;
        if (symptomes.contains("anxiété") || symptomes.contains("burnout")) return PrioriteTraitement.MOYENNE;
        return PrioriteTraitement.BASSE;
    }

    private int evaluerDuree(String symptome, String niveauEtudes) {
        int base;
        switch (symptome) {
            case "anxiété": base = 60; break;
            case "dépression": base = 90; break;
            case "phobie": base = 45; break;
            case "burnout": base = 60; break;
            default: base = 30;
        }
        if (niveauEtudes != null && (niveauEtudes.toLowerCase().contains("supérieur") ||
                niveauEtudes.toLowerCase().contains("université"))) {
            base += 15;
        }
        return base;
    }

    private String genererObjectifs(String symptome) {
        switch (symptome) {
            case "anxiété": return "Réduire les symptômes anxieux, développer des stratégies de coping et retrouver un état de calme.";
            case "dépression": return "Améliorer l'humeur, retrouver l'énergie et développer une vision positive de l'avenir.";
            case "stress": return "Apprendre à identifier et gérer les sources de stress, développer des techniques de relaxation.";
            case "trouble du sommeil": return "Améliorer la qualité du sommeil et établir une routine de coucher saine.";
            case "phobie": return "Réduire la peur face aux situations phobogènes et retrouver une vie sociale normale.";
            case "burnout": return "Récupérer de l'épuisement professionnel et retrouver un équilibre de vie.";
            default: return "Améliorer le bien-être psychologique général et renforcer la résilience émotionnelle.";
        }
    }

    private String genererDescription(String symptome, int age) {
        String desc;
        switch (symptome) {
            case "anxiété": desc = "Programme combinant relaxation, TCC et gestion du stress."; break;
            case "dépression": desc = "Approche thérapeutique visant à remonter l'humeur et développer la motivation."; break;
            case "stress": desc = "Programme incluant mindfulness, relaxation et restructuration cognitive."; break;
            case "trouble du sommeil": desc = "Traitement combinant hygiène du sommeil et relaxation."; break;
            case "phobie": desc = "Approche utilisant exposition progressive et restructuration cognitive."; break;
            case "burnout": desc = "Programme visant à restaurer l'énergie et développer des limites saines."; break;
            default: desc = "Traitement adapté avec une approche holistique.";
        }
        if (age < 25) desc += " Particulièrement adapté aux jeunes adultes.";
        else if (age > 40) desc += " Approche adaptée aux adultes avec expérience de vie.";
        return desc;
    }

    private String genererDosage(String symptome) {
        switch (symptome) {
            case "anxiété": return "2 fois par semaine";
            case "dépression": return "1 fois par semaine";
            case "stress": return "3 fois par semaine";
            case "trouble du sommeil": return "2 fois par semaine";
            case "phobie": return "1 fois par semaine";
            case "burnout": return "2 fois par semaine";
            default: return "1 fois par semaine";
        }
    }

    // ==================== ANALYSE IA DES SUIVIS  ====================

    /**
     * Analyse les suivis d'un étudiant
     */
    public ResultatAnalyseSuivi analyserSuivisEtudiant(List<SuiviTraitement> suivis, Traitement traitement) {
        if (suivis == null || suivis.isEmpty()) {
            return new ResultatAnalyseSuivi(
                    "insuffisant", 0.0,
                    List.of("Aucun suivi disponible pour l'analyse"),
                    List.of("Commencer à enregistrer des suivis réguliers"),
                    "Pas assez de données pour analyser la progression",
                    "inconnu"
            );
        }

        // Trier les suivis par date
        List<SuiviTraitement> suivisTries = new ArrayList<>(suivis);
        suivisTries.sort((s1, s2) -> {
            if (s1.getDateSuivi() == null && s2.getDateSuivi() == null) return 0;
            if (s1.getDateSuivi() == null) return 1;
            if (s2.getDateSuivi() == null) return -1;
            return s1.getDateSuivi().compareTo(s2.getDateSuivi());
        });

        // Calculer le score de progression (basé UNIQUEMENT sur les observations)
        double scoreProgression = calculerScoreProgressionParObservations(suivisTries);
        String tendance = determinerTendance(scoreProgression);
        String niveauRisque = evaluerNiveauRisque(suivisTries, scoreProgression);

        List<String> observations = genererObservationsAnalyse(suivisTries, scoreProgression);
        List<String> recommandations = genererRecommandations(suivisTries, traitement, scoreProgression, niveauRisque);
        String resume = genererResumeAnalyse(tendance, scoreProgression, niveauRisque, suivis.size());

        return new ResultatAnalyseSuivi(tendance, scoreProgression, observations, recommandations, resume, niveauRisque);
    }

    /**
     * Calcule le score de progression en analysant uniquement les observations textuelles
     */
    private double calculerScoreProgressionParObservations(List<SuiviTraitement> suivis) {
        if (suivis.size() < 2) {
            return 0.0; // Pas assez de suivis pour comparer
        }

        double scoreTotal = 0.0;
        int comparaisonsValides = 0;

        for (int i = 1; i < suivis.size(); i++) {
            SuiviTraitement precedent = suivis.get(i - 1);
            SuiviTraitement actuel = suivis.get(i);

            String obsPrecedente = precedent.getObservations() != null ? precedent.getObservations().toLowerCase() : "";
            String obsActuelle = actuel.getObservations() != null ? actuel.getObservations().toLowerCase() : "";

            if (!obsPrecedente.isEmpty() || !obsActuelle.isEmpty()) {
                double score = comparerObservations(obsPrecedente, obsActuelle);
                scoreTotal += score;
                comparaisonsValides++;
            }
        }

        if (comparaisonsValides == 0) {
            return 0.0;
        }

        return appliquerLissage(scoreTotal / comparaisonsValides);
    }

    /**
     * Compare deux observations textuelles pour détecter progression ou régression
     */
    private double comparerObservations(String obsPrecedente, String obsActuelle) {
        if (obsPrecedente.isEmpty() && obsActuelle.isEmpty()) return 0.0;
        if (obsPrecedente.isEmpty()) return 0.2;  // Premier suivi
        if (obsActuelle.isEmpty()) return -0.2;   // Suivi vide

        // Mots-clés positifs (progrès)
        String[] motsPositifs = {
                "mieux", "amélioré", "diminué", "réduit", "stable", "calme", "soulagé",
                "progrès", "avancé", "évolué", "maîtrisé", "contrôlé", "géré",
                "positif", "bien", "serein", "apaisé", "relaxé", "détendu"
        };

        // Mots-clés négatifs (régression)
        String[] motsNegatifs = {
                "pire", "augmenté", "empiré", "difficile", "stressant", "anxieux",
                "angoissant", "douloureux", "insupportable", "bloquant", "régressé",
                "négatif", "mal", "tendu", "inquiet", "fatigué", "épuisé"
        };

        int scorePositif = compterMotsCles(obsActuelle, motsPositifs) - compterMotsCles(obsPrecedente, motsPositifs);
        int scoreNegatif = compterMotsCles(obsActuelle, motsNegatifs) - compterMotsCles(obsPrecedente, motsNegatifs);

        // Analyse de la longueur (plus de détails = plus d'engagement)
        int diffLongueur = obsActuelle.length() - obsPrecedente.length();
        double scoreLongueur = diffLongueur > 30 ? 0.15 : diffLongueur < -30 ? -0.15 : 0.0;

        // Analyse de la ponctuation et émoticônes
        double scoreEmotions = analyserEmotions(obsActuelle) - analyserEmotions(obsPrecedente);

        // Score composite
        double scoreFinal = (scorePositif * 0.25) + (scoreNegatif * -0.25) + scoreLongueur + (scoreEmotions * 0.2);

        return Math.max(-1.0, Math.min(1.0, scoreFinal));
    }

    /**
     * Analyse les émoticônes et signes de ponctuation exprimant des émotions
     */
    private double analyserEmotions(String texte) {
        double score = 0.0;

        // Émoticônes positives
        if (texte.contains("😊") || texte.contains("🙂") || texte.contains("👍")) score += 0.3;
        if (texte.contains("❤️")) score += 0.2;

        // Émoticônes négatives
        if (texte.contains("😞") || texte.contains("😢") || texte.contains("😔")) score -= 0.3;
        if (texte.contains("😠") || texte.contains("😤")) score -= 0.2;

        // Ponctuation expressive
        if (texte.contains("!!!")) score += 0.1;
        if (texte.contains("???")) score -= 0.1;

        return Math.max(-0.5, Math.min(0.5, score));
    }

    private int compterMotsCles(String texte, String[] motsCles) {
        int count = 0;
        for (String mot : motsCles) {
            if (texte.contains(mot)) {
                count++;
            }
        }
        return count;
    }

    private double appliquerLissage(double score) {
        if (Math.abs(score) < 0.1) return 0.0;
        if (Math.abs(score) < 0.3) return score * 0.7;
        if (Math.abs(score) < 0.5) return score * 0.85;
        return score;
    }

    private String determinerTendance(double scoreProgression) {
        if (scoreProgression > 0.3) return "amélioration";
        if (scoreProgression < -0.3) return "détérioration";
        return "stable";
    }

    private String evaluerNiveauRisque(List<SuiviTraitement> suivis, double scoreProgression) {
        int nombreSuivis = suivis.size();

        // Vérifier les mots-clés alarmants dans le dernier suivi
        SuiviTraitement dernierSuivi = suivis.get(suivis.size() - 1);
        if (dernierSuivi.getObservations() != null) {
            String dernierObs = dernierSuivi.getObservations().toLowerCase();
            String[] motsAlarmants = {"crise", "urgence", "grave", "insupportable", "désespéré", "suicide"};
            for (String mot : motsAlarmants) {
                if (dernierObs.contains(mot)) {
                    return "élevé";
                }
            }
        }

        if (scoreProgression < -0.5 && nombreSuivis >= 2) return "élevé";
        if (scoreProgression < -0.2 && nombreSuivis >= 2) return "modéré";
        if (scoreProgression < 0.3 || nombreSuivis < 2) return "faible";
        return "minimal";
    }

    private List<String> genererObservationsAnalyse(List<SuiviTraitement> suivis, double scoreProgression) {
        List<String> observations = new ArrayList<>();

        // Tendance générale
        if (scoreProgression > 0.3) {
            observations.add("📈 Progression positive détectée dans les suivis");
        } else if (scoreProgression < -0.3) {
            observations.add("📉 Régression observée nécessitant une attention");
        } else {
            observations.add("➡️ État stable des symptômes");
        }

        // Régularité des suivis
        if (suivis.size() >= 4) {
            observations.add("✅ Bon suivi régulier du traitement");
        } else if (suivis.size() >= 2) {
            observations.add("⚠️ Suivi irrégulier, pourrait être amélioré");
        } else {
            observations.add("❌ Suivi insuffisant pour une évaluation complète");
        }

        // Analyse du dernier suivi
        SuiviTraitement dernier = suivis.get(suivis.size() - 1);
        if (dernier.getObservations() != null && !dernier.getObservations().isEmpty()) {
            String obs = dernier.getObservations().toLowerCase();
            if (obs.length() > 50) {
                observations.add("📝 Dernier suivi détaillé avec des informations substantielles");
            } else if (obs.length() < 20) {
                observations.add("⚠️ Dernier suivi trop concis, manque de détails");
            }
        }

        return observations;
    }

    private List<String> genererRecommandations(List<SuiviTraitement> suivis, Traitement traitement,
                                                double scoreProgression, String niveauRisque) {
        List<String> recommandations = new ArrayList<>();

        // Recommandations basées sur la progression
        if (scoreProgression > 0.4) {
            recommandations.add("🎯 Excellente progression - continuer le traitement actuel");
            recommandations.add("📊 Maintenir la fréquence des suivis actuels");
            recommandations.add("🌟 Partager les réussites avec le psychologue");
        } else if (scoreProgression < -0.4) {
            recommandations.add("🚨 Nécessite une consultation rapide pour ajustement");
            recommandations.add("🔄 Envisager une réévaluation du traitement");
            recommandations.add("📝 Augmenter temporairement la fréquence des suivis");
        } else {
            recommandations.add("🔍 Analyser les facteurs influençant la stabilité");
            recommandations.add("💬 Discuter des stratégies pour débloquer la progression");
        }

        // Recommandations basées sur le risque
        switch (niveauRisque) {
            case "élevé":
                recommandations.add("⚡ Consultation urgente recommandée");
                recommandations.add("📞 Contacter le psychologue dans les 48h");
                break;
            case "modéré":
                recommandations.add("⏰ Consultation recommandée cette semaine");
                break;
            case "faible":
                recommandations.add("📅 Maintenir le suivi régulier");
                break;
            default:
                recommandations.add("✅ Continuer le traitement comme prévu");
        }

        // Recommandation sur la régularité
        if (suivis.size() < 3) {
            recommandations.add("📝 Enregistrer des suivis plus fréquents (2-3 fois par semaine)");
        }

        // Recommandation sur la durée du traitement
        if (traitement.getDateDebut() != null) {
            LocalDate dateDebut = traitement.getDateDebut().toLocalDate();
            long joursEcoules = ChronoUnit.DAYS.between(dateDebut, LocalDate.now());
            if (joursEcoules > traitement.getDureeJours() * 0.8) {
                recommandations.add("🏁 Préparer la fin du traitement et évaluation finale");
            } else if (joursEcoules > traitement.getDureeJours() * 0.5) {
                recommandations.add("🔍 Évaluer la mi-parcours du traitement");
            }
        }

        return recommandations;
    }

    private String genererResumeAnalyse(String tendance, double scoreProgression,
                                        String niveauRisque, int nombreSuivis) {
        StringBuilder resume = new StringBuilder();
        resume.append("Analyse IA basée sur ").append(nombreSuivis).append(" suivi(s) : ");

        switch (tendance) {
            case "amélioration":
                resume.append("📈 Progression positive détectée");
                break;
            case "détérioration":
                resume.append("📉 Régression observée");
                break;
            default:
                resume.append("➡️ État stable");
        }

        resume.append(". Niveau de risque : ").append(niveauRisque).append(".");
        return resume.toString();
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    public List<String> analyserDescription(String description) {
        return Arrays.asList(
                "Considérez inclure plus de détails sur vos émotions",
                "Pensez à décrire les situations qui déclenchent vos symptômes",
                "Mentionnez la durée de vos symptômes",
                "Décrivez l'impact sur votre vie quotidienne"
        );
    }

    public boolean validerTraitement(Traitement traitement) {
        if (traitement.getTitre() == null || traitement.getTitre().isEmpty()) return false;
        if (traitement.getType() == null || traitement.getType().isEmpty()) return false;
        if (traitement.getCategorie() == null) return false;
        if (traitement.getDureeJours() <= 0) return false;
        return true;
    }
}