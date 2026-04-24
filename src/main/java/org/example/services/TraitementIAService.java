package org.example.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.example.entities.SuiviTraitement;
import org.example.entities.Traitement;
import org.example.enums.CategorieTraitement;
import org.example.enums.PrioriteTraitement;
import org.example.enums.RessentiSuivi;
import org.example.enums.StatutTraitement;

/**
 * Service d'IA pour la génération intelligente de traitements
 * Utilise des algorithmes et règles pour recommander des traitements personnalisés
 */
public class TraitementIAService {

    /**
     * Classe pour stocker le résultat de la validation des symptômes
     */
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

    private Random random = new Random();

    // Base de connaissances des symptômes et traitements associés
    private Map<String, List<String>> symptomesTraitements = new HashMap<>();
    private Map<String, List<String>> categoriesSpecifiques = new HashMap<>();

    public TraitementIAService() {
        initialiserBaseConnaissances();
    }

    /**
     * Initialise la base de connaissances de l'IA
     */
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
        categoriesSpecifiques.put("anxiété", Arrays.asList(
                "EMOTIONNEL", "COGNITIF", "RELAXATION"
        ));

        categoriesSpecifiques.put("dépression", Arrays.asList(
                "EMOTIONNEL", "COGNITIF", "COMPORTEMENTAL"
        ));

        categoriesSpecifiques.put("stress", Arrays.asList(
                "RELAXATION", "EMOTIONNEL", "COGNITIF"
        ));

        categoriesSpecifiques.put("trouble du sommeil", Arrays.asList(
                "RELAXATION", "EMOTIONNEL", "COGNITIF"
        ));

        categoriesSpecifiques.put("phobie", Arrays.asList(
                "COMPORTEMENTAL", "COGNITIF", "EMOTIONNEL"
        ));

        categoriesSpecifiques.put("burnout", Arrays.asList(
                "EMOTIONNEL", "RELAXATION", "COGNITIF"
        ));
    }

    /**
     * Valide la pertinence des symptômes avant de générer un traitement
     */
    public ResultatValidation validerSymptomes(String symptomes, String description) {
        if (symptomes == null || symptomes.trim().isEmpty()) {
            return new ResultatValidation(false, "Veuillez décrire les symptômes");
        }

        symptomes = symptomes.toLowerCase().trim();

        // Vérifier si les symptômes contiennent des mots-clés pertinents
        boolean symptomeValide = false;
        String symptomeTrouve = null;

        for (String symptomeConn : symptomesTraitements.keySet()) {
            if (symptomes.contains(symptomeConn)) {
                symptomeValide = true;
                symptomeTrouve = symptomeConn;
                break;
            }
        }

        // Vérifier si la description contient des mots pertinents
        boolean descriptionValide = false;
        if (description != null && !description.trim().isEmpty()) {
            String desc = description.toLowerCase();
            // Mots-clés psychologiques pertinents
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

        // Vérifier si les symptômes sont trop courts ou incohérents
        if (symptomes.length() < 3) {
            return new ResultatValidation(false, "La description des symptômes est trop courte");
        }

        // Vérifier si les symptômes ne contiennent que des caractères aléatoires
        if (symptomes.matches("[^a-zA-Z\\sàâäéèêëïîôöùûüÿç]+") || symptomes.replaceAll("[^a-zA-Z\\sàâäéèêëïîôöùûüÿç]", "").length() < 3) {
            return new ResultatValidation(false, "Les symptômes semblent incohérents. Veuillez décrire clairement ce que vous ressentez.");
        }

        // Vérifier si les symptômes sont pertinents psychologiquement
        if (!symptomeValide && !descriptionValide) {
            return new ResultatValidation(false,
                    "Les symptômes décrits ne semblent pas pertinents pour un suivi psychologique."
            );
        }

        // Si valide, retourner le symptôme principal trouvé
        String symptomePrincipal = symptomeTrouve != null ? symptomeTrouve : "stress";
        return new ResultatValidation(true, "Symptômes valides", symptomePrincipal);
    }

    /**
     * Génère un traitement personnalisé basé sur les symptômes et informations
     */
    public Traitement genererTraitementPersonnalise(String symptomes, String description, int age, String niveauEtudes) {
        // Valider d'abord les symptômes
        ResultatValidation validation = validerSymptomes(symptomes, description);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }

        Traitement traitement = new Traitement();

        // Utiliser le symptôme principal validé
        String symptomePrincipal = validation.getSymptomePrincipal();

        // Génération du titre
        traitement.setTitre(genererTitreTraitement(symptomePrincipal, description));

        // Type de traitement recommandé
        traitement.setType(choisirTypeTraitement(symptomePrincipal));

        // Catégorie appropriée
        traitement.setCategorie(choisirCategorie(symptomePrincipal));

        // Priorité selon la sévérité
        traitement.setPriorite(evaluerPriorite(symptomes, description));

        // Durée recommandée
        traitement.setDureeJours(evaluerDuree(symptomePrincipal, niveauEtudes));

        // Objectifs thérapeutiques
        traitement.setObjectifTherapeutique(genererObjectifs(symptomePrincipal, description));

        // Description détaillée
        traitement.setDescription(genererDescription(symptomePrincipal, description, age));

        // Statut initial
        traitement.setStatut(StatutTraitement.EN_COURS);

        // Dosage/fréquence
        traitement.setDosage(genererDosage(symptomePrincipal));

        return traitement;
    }

    /**
     * Extrait le symptôme principal à partir de la description
     */
    private String extraireSymptomePrincipal(String symptomes) {
        symptomes = symptomes.toLowerCase();

        for (String symptome : symptomesTraitements.keySet()) {
            if (symptomes.contains(symptome)) {
                return symptome;
            }
        }

        // Si aucun symptôme spécifique n'est trouvé, retourne "stress" par défaut
        return "stress";
    }

    /**
     * Génère un titre personnalisé pour le traitement
     */
    private String genererTitreTraitement(String symptome, String description) {
        String[] prefixes = {
                "Prise en charge de ",
                "Accompagnement thérapeutique - ",
                "Programme de soin - ",
                "Suivi spécialisé - ",
                "Thérapie ciblée - "
        };

        String prefix = prefixes[random.nextInt(prefixes.length)];

        switch (symptome) {
            case "anxiété":
                return prefix + "l'anxiété et les troubles anxieux";
            case "dépression":
                return prefix + "la dépression et l'humeur";
            case "stress":
                return prefix + "la gestion du stress";
            case "trouble du sommeil":
                return prefix + "des troubles du sommeil";
            case "phobie":
                return prefix + "des phobies et peurs";
            case "burnout":
                return prefix + "du burnout et épuisement";
            default:
                return prefix + "du bien-être psychologique";
        }
    }

    /**
     * Choisit le type de traitement le plus approprié
     */
    private String choisirTypeTraitement(String symptome) {
        List<String> types = symptomesTraitements.get(symptome);
        if (types != null && !types.isEmpty()) {
            return types.get(random.nextInt(types.size()));
        }
        return "Suivi psychologique";
    }

    /**
     * Choisit la catégorie de traitement
     */
    private CategorieTraitement choisirCategorie(String symptome) {
        List<String> categories = categoriesSpecifiques.get(symptome);
        if (categories != null && !categories.isEmpty()) {
            String catStr = categories.get(random.nextInt(categories.size()));
            try {
                return CategorieTraitement.valueOf(catStr);
            } catch (IllegalArgumentException e) {
                // Si la catégorie n'existe pas, retourne une catégorie par défaut
                return CategorieTraitement.EMOTIONNEL;
            }
        }
        return CategorieTraitement.EMOTIONNEL;
    }

    /**
     * Évalue la priorité du traitement
     */
    private PrioriteTraitement evaluerPriorite(String symptomes, String description) {
        symptomes = symptomes.toLowerCase();
        description = description.toLowerCase();

        // Mots-clés indiquant une urgence
        String[] motsUrgence = {"urgent", "crise", "grave", "sévère", "difficile", "insupportable"};

        for (String mot : motsUrgence) {
            if (symptomes.contains(mot) || description.contains(mot)) {
                return PrioriteTraitement.HAUTE;
            }
        }

        // Symptômes spécifiques qui nécessitent une attention particulière
        if (symptomes.contains("dépression") || symptomes.contains("phobie")) {
            return PrioriteTraitement.HAUTE;
        }

        if (symptomes.contains("anxiété") || symptomes.contains("burnout")) {
            return PrioriteTraitement.MOYENNE;
        }

        return PrioriteTraitement.BASSE;
    }

    /**
     * Évalue la durée recommandée du traitement
     */
    private int evaluerDuree(String symptome, String niveauEtudes) {
        int base = 30; // 1 mois par défaut

        switch (symptome) {
            case "anxiété":
                base = 60; // 2 mois
                break;
            case "dépression":
                base = 90; // 3 mois
                break;
            case "phobie":
                base = 45; // 1.5 mois
                break;
            case "burnout":
                base = 60; // 2 mois
                break;
            case "trouble du sommeil":
                base = 30; // 1 mois
                break;
            default:
                base = 30;
        }

        // Ajustement selon le niveau d'études
        if (niveauEtudes != null) {
            if (niveauEtudes.toLowerCase().contains("supérieur") ||
                    niveauEtudes.toLowerCase().contains("université")) {
                base += 15; // +15 jours pour les étudiants supérieurs
            }
        }

        return base;
    }

    /**
     * Génère les objectifs thérapeutiques
     */
    private String genererObjectifs(String symptome, String description) {
        StringBuilder objectifs = new StringBuilder();

        switch (symptome) {
            case "anxiété":
                objectifs.append("Réduire les symptômes anxieux et améliorer la gestion du stress. ");
                objectifs.append("Développer des stratégies de coping efficaces. ");
                objectifs.append("Retrouver un état de calme et de sérénité.");
                break;
            case "dépression":
                objectifs.append("Améliorer l'humeur et retrouver l'énergie. ");
                objectifs.append("Développer une vision plus positive de soi et de l'avenir. ");
                objectifs.append("Reprendre progressivement les activités quotidiennes.");
                break;
            case "stress":
                objectifs.append("Apprendre à identifier et gérer les sources de stress. ");
                objectifs.append("Développer des techniques de relaxation. ");
                objectifs.append("Améliorer l'équilibre vie professionnelle/vie personnelle.");
                break;
            case "trouble du sommeil":
                objectifs.append("Améliorer la qualité du sommeil. ");
                objectifs.append("Établir une routine de coucher saine. ");
                objectifs.append("Réduire l'insomnie et les réveils nocturnes.");
                break;
            case "phobie":
                objectifs.append("Réduire la peur face aux situations phobogènes. ");
                objectifs.append("Développer des stratégies d'exposition progressive. ");
                objectifs.append("Retrouver une vie sociale et professionnelle normale.");
                break;
            case "burnout":
                objectifs.append("Récupérer de l'épuisement professionnel. ");
                objectifs.append("Développer des limites saines au travail. ");
                objectifs.append("Retrouver un équilibre et un sens à ses activités.");
                break;
            default:
                objectifs.append("Améliorer le bien-être psychologique général. ");
                objectifs.append("Développer des stratégies d'adaptation. ");
                objectifs.append("Renforcer la résilience émotionnelle.");
        }

        return objectifs.toString();
    }

    /**
     * Génère une description détaillée du traitement
     */
    private String genererDescription(String symptome, String description, int age) {
        StringBuilder desc = new StringBuilder();

        desc.append("Programme thérapeutique personnalisé pour ");

        switch (symptome) {
            case "anxiété":
                desc.append("la gestion des troubles anxieux. Ce traitement combine ");
                desc.append("des techniques de relaxation, de la thérapie cognitivo-comportementale ");
                desc.append("et des stratégies de gestion du stress adaptées à votre situation.");
                break;
            case "dépression":
                desc.append("le soutien dans la dépression. L'approche thérapeutique ");
                desc.append("vise à remonter l'humeur, développer la motivation et ");
                desc.append("créer des perspectives d'avenir positives.");
                break;
            case "stress":
                desc.append("la gestion du stress chronique. Le programme inclut ");
                desc.append("des techniques de mindfulness, de la relaxation et ");
                desc.append("des méthodes de restructuration cognitive.");
                break;
            case "trouble du sommeil":
                desc.append("l'amélioration de la qualité du sommeil. Le traitement ");
                desc.append("combine l'hygiène du sommeil, des techniques de relaxation ");
                desc.append("et la gestion des pensées qui perturbent le repos.");
                break;
            case "phobie":
                desc.append("le traitement des phobies spécifiques. L'approche ");
                desc.append("utilise des techniques d'exposition progressive et ");
                desc.append("de restructuration cognitive pour surmonter les peurs.");
                break;
            case "burnout":
                desc.append("la récupération du burnout. Le programme vise ");
                desc.append("à restaurer l'énergie, développer des limites saines ");
                desc.append("et retrouver un équilibre de vie durable.");
                break;
            default:
                desc.append("l'amélioration du bien-être psychologique. ");
                desc.append("Le traitement est adapté à vos besoins spécifiques ");
                desc.append("avec une approche holistique.");
        }

        // Adaptation selon l'âge
        if (age < 25) {
            desc.append(" Particulièrement adapté aux jeunes adultes et étudiants.");
        } else if (age > 40) {
            desc.append(" Approche adaptée aux adultes avec expérience de vie.");
        }

        return desc.toString();
    }

    /**
     * Génère le dosage/fréquence recommandé
     */
    private String genererDosage(String symptome) {
        switch (symptome) {
            case "anxiété":
                return "2 fois par semaine";
            case "dépression":
                return "1 fois par semaine";
            case "stress":
                return "3 fois par semaine";
            case "trouble du sommeil":
                return "2 fois par semaine";
            case "phobie":
                return "1 fois par semaine";
            case "burnout":
                return "2 fois par semaine";
            default:
                return "1 fois par semaine";
        }
    }

    /**
     * Analyse la description et retourne des suggestions d'amélioration
     */
    public List<String> analyserDescription(String description) {
        return Arrays.asList(
                "Considérez inclure plus de détails sur vos émotions",
                "Pensez à décrire les situations qui déclenchent vos symptômes",
                "Mentionnez la durée de vos symptômes",
                "Décrivez l'impact sur votre vie quotidienne"
        );
    }

    /**
     * Vérifie si le traitement généré est cohérent
     */
    public boolean validerTraitement(Traitement traitement) {
        if (traitement.getTitre() == null || traitement.getTitre().isEmpty()) {
            return false;
        }
        if (traitement.getType() == null || traitement.getType().isEmpty()) {
            return false;
        }
        if (traitement.getCategorie() == null) {
            return false;
        }
        if (traitement.getDureeJours() <= 0) {
            return false;
        }
        return true;
    }

    // =============================================
    // ANALYSE IA DES SUIVIS
    // =============================================

    /**
     * Résultat de l'analyse IA des suivis
     */
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

        // Getters
        public String getTendance() { return tendance; }
        public double getScoreProgression() { return scoreProgression; }
        public List<String> getObservations() { return observations; }
        public List<String> getRecommandations() { return recommandations; }
        public String getResume() { return resume; }
        public String getNiveauRisque() { return niveauRisque; }
    }

    /**
     * Analyse les suivis d'un étudiant pour un traitement donné
     */
    public ResultatAnalyseSuivi analyserSuivisEtudiant(List<SuiviTraitement> suivis, Traitement traitement) {
        if (suivis == null || suivis.isEmpty()) {
            return new ResultatAnalyseSuivi(
                    "insuffisant",
                    0.0,
                    List.of("Aucun suivi disponible pour l'analyse"),
                    List.of("Commencer à enregistrer des suivis réguliers"),
                    "Pas assez de données pour analyser la progression",
                    "inconnu"
            );
        }

        // Trier les suivis par date (créer une nouvelle liste modifiable)
        List<SuiviTraitement> suivisTries = new ArrayList<>(suivis);
        suivisTries.sort((s1, s2) -> {
            if (s1.getDateSuivi() == null && s2.getDateSuivi() == null) return 0;
            if (s1.getDateSuivi() == null) return 1;
            if (s2.getDateSuivi() == null) return -1;
            return s1.getDateSuivi().compareTo(s2.getDateSuivi());
        });

        // Analyse de la progression
        double scoreProgression = calculerScoreProgression(suivisTries);
        String tendance = determinerTendance(scoreProgression);
        String niveauRisque = evaluerNiveauRisque(suivisTries, scoreProgression);

        // Générer les observations
        List<String> observations = genererObservations(suivisTries, scoreProgression);

        // Générer les recommandations
        List<String> recommandations = genererRecommandations(suivisTries, traitement, scoreProgression, niveauRisque);

        // Générer le résumé
        String resume = genererResumeAnalyse(tendance, scoreProgression, niveauRisque, suivis.size());

        return new ResultatAnalyseSuivi(tendance, scoreProgression, observations, recommandations, resume, niveauRisque);
    }

    /**
     * Calcule le score de progression basé sur les suivis avec algorithme amélioré
     */
    private double calculerScoreProgression(List<SuiviTraitement> suivis) {
        if (suivis.size() == 1) {
            return 0.0; // Pas assez de données pour comparer
        }

        double scoreTotal = 0.0;
        int comparaisonsValides = 0;

        // Analyse progressive avec pondération
        for (int i = 1; i < suivis.size(); i++) {
            SuiviTraitement precedent = suivis.get(i - 1);
            SuiviTraitement actuel = suivis.get(i);

            double scorePaire = 0.0;
            int facteursPaire = 0;

            // 1. Comparaison du ressenti (poids: 40%)
            if (precedent.getRessenti() != null && actuel.getRessenti() != null) {
                double scoreRessenti = comparerRessenti(precedent.getRessenti(), actuel.getRessenti());
                scorePaire += scoreRessenti * 0.4;
                facteursPaire++;
            }

            // 2. Comparaison des observations (poids: 30%)
            String obsPrecedente = precedent.getObservations() != null ? precedent.getObservations().toLowerCase() : "";
            String obsActuelle = actuel.getObservations() != null ? actuel.getObservations().toLowerCase() : "";
            if (!obsPrecedente.isEmpty() || !obsActuelle.isEmpty()) {
                double scoreObservations = comparerObservationsAmelioree(obsPrecedente, obsActuelle);
                scorePaire += scoreObservations * 0.3;
                facteursPaire++;
            }

            // 3. Comparaison de l'évaluation (poids: 30%)
            if (precedent.getEvaluation() != null && actuel.getEvaluation() != null) {
                double scoreEvaluation = (actuel.getEvaluation() - precedent.getEvaluation()) / 10.0;
                // Limiter l'impact des variations extrêmes
                scoreEvaluation = Math.max(-1.0, Math.min(1.0, scoreEvaluation));
                scorePaire += scoreEvaluation * 0.3;
                facteursPaire++;
            }

            // Ajouter le score de la paire si on a des facteurs valides
            if (facteursPaire > 0) {
                scoreTotal += scorePaire;
                comparaisonsValides++;
            }
        }

        // Calcul du score moyen avec lissage
        if (comparaisonsValides == 0) {
            return 0.0;
        }

        double scoreMoyen = scoreTotal / comparaisonsValides;

        // Appliquer un lissage pour éviter les variations trop brutales
        return appliquerLissage(scoreMoyen);
    }

    /**
     * Applique un lissage au score pour plus de stabilité
     */
    private double appliquerLissage(double score) {
        // Seuils de lissage pour rendre l'analyse plus stable
        if (Math.abs(score) < 0.1) {
            return 0.0; // Considérer comme stable si variation très faible
        } else if (Math.abs(score) < 0.3) {
            return score * 0.7; // Réduire les variations faibles
        } else if (Math.abs(score) < 0.5) {
            return score * 0.85; // Léger lissage pour variations moyennes
        }
        return score; // Garder les variations fortes
    }

    /**
     * Comparaison améliorée des observations avec analyse sémantique
     */
    private double comparerObservationsAmelioree(String obsPrecedente, String obsActuelle) {
        if (obsPrecedente.isEmpty() && obsActuelle.isEmpty()) return 0.0;
        if (obsPrecedente.isEmpty() || obsActuelle.isEmpty()) return 0.1;

        // Mots-clés positifs d'amélioration
        String[] motsPositifs = {
                "mieux", "amélioré", "diminué", "réduit", "stable", "calme", "soulagé",
                "progress", "avancé", "évolué", "maitrisé", "contrôlé", "géré"
        };

        // Mots-clés négatifs de détérioration
        String[] motsNegatifs = {
                "pire", "augmenté", "empiré", "difficile", "stressant", "anxieux",
                "angoissant", "douloureux", "insupportable", "bloquant", "régressé"
        };

        // Compter les occurrences
        int scorePositif = compterOccurrences(obsActuelle, motsPositifs) - compterOccurrences(obsPrecedente, motsPositifs);
        int scoreNegatif = compterOccurrences(obsActuelle, motsNegatifs) - compterOccurrences(obsPrecedente, motsNegatifs);

        // Analyse de la longueur (plus de détails = plus d'engagement)
        int diffLongueur = obsActuelle.length() - obsPrecedente.length();
        double scoreLongueur = diffLongueur > 20 ? 0.1 : diffLongueur < -20 ? -0.1 : 0.0;

        // Score composite
        double scoreFinal = (scorePositif - scoreNegatif) * 0.2 + scoreLongueur;

        // Limiter le score
        return Math.max(-1.0, Math.min(1.0, scoreFinal));
    }

    /**
     * Compte les occurrences de mots-clés dans un texte
     */
    private int compterOccurrences(String texte, String[] motsCles) {
        int count = 0;
        for (String mot : motsCles) {
            if (texte.contains(mot)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compare deux niveaux de ressenti
     */
    private double comparerRessenti(RessentiSuivi ressentiPrecedent, RessentiSuivi ressentiActuel) {
        Map<RessentiSuivi, Integer> niveauxRessenti = Map.of(
                RessentiSuivi.TRES_DIFFICILE, 1,
                RessentiSuivi.DIFFICILE, 2,
                RessentiSuivi.NEUTRE, 3,
                RessentiSuivi.BIEN, 4,
                RessentiSuivi.TRES_BIEN, 5
        );

        int niveauPrecedent = niveauxRessenti.getOrDefault(ressentiPrecedent, 3);
        int niveauActuel = niveauxRessenti.getOrDefault(ressentiActuel, 3);

        return (niveauActuel - niveauPrecedent) / 4.0;
    }

    /**
     * Compare les descriptions d'observations
     */
    private double comparerObservations(String observationsPrecedentes, String observationsActuelles) {
        if (observationsPrecedentes == null || observationsActuelles == null) {
            return 0.0;
        }

        // Analyse simple basée sur la longueur et les mots-clés
        String[] motsClesAmelioration = {"mieux", "amélioré", "diminué", "réduit", "stable", "calme"};
        String[] motsClesDetrioration = {"pire", "augmenté", "empiré", "difficile", "stressant", "anxieux"};

        int scorePrecedent = compterMotsCles(observationsPrecedentes.toLowerCase(), motsClesDetrioration)
                - compterMotsCles(observationsPrecedentes.toLowerCase(), motsClesAmelioration);
        int scoreActuel = compterMotsCles(observationsActuelles.toLowerCase(), motsClesDetrioration)
                - compterMotsCles(observationsActuelles.toLowerCase(), motsClesAmelioration);

        return (scorePrecedent - scoreActuel) / 5.0;
    }

    /**
     * Compte les occurrences de mots-clés dans un texte
     */
    private int compterMotsCles(String texte, String[] motsCles) {
        int count = 0;
        for (String mot : motsCles) {
            if (texte.contains(mot)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Détermine la tendance basée sur le score de progression avec seuils améliorés
     */
    private String determinerTendance(double scoreProgression) {
        // Seuils plus stricts pour éviter les variations aléatoires
        if (scoreProgression > 0.4) {
            return "amélioration";
        } else if (scoreProgression < -0.4) {
            return "détérioration";
        } else {
            return "stable";
        }
    }

    /**
     * Évalue le niveau de risque avec analyse améliorée
     */
    private String evaluerNiveauRisque(List<SuiviTraitement> suivis, double scoreProgression) {
        // Considérer à la fois la progression et la régularité des suivis
        int nombreSuivis = suivis.size();

        if (scoreProgression < -0.6 && nombreSuivis >= 2) {
            return "élevé";
        } else if (scoreProgression < -0.3 && nombreSuivis >= 2) {
            return "modéré";
        } else if (scoreProgression < 0.3 || nombreSuivis < 2) {
            return "faible";
        } else {
            return "minimal";
        }
    }

    /**
     * Génère les observations basées sur l'analyse
     */
    private List<String> genererObservations(List<SuiviTraitement> suivis, double scoreProgression) {
        List<String> observations = new ArrayList<>();

        // Observation sur la tendance
        if (scoreProgression > 0.3) {
            observations.add("📈 Progression positive détectée dans les suivis");
        } else if (scoreProgression < -0.3) {
            observations.add("📉 Régression observée nécessitant une attention");
        } else {
            observations.add("➡️ État stable des symptômes");
        }

        // Observation sur la régularité
        if (suivis.size() >= 4) {
            observations.add("✅ Bon suivi régulier du traitement");
        } else if (suivis.size() >= 2) {
            observations.add("⚠️ Suivi irrégulier, pourrait être amélioré");
        } else {
            observations.add("❌ Suivi insuffisant pour une évaluation complète");
        }

        // Observation sur le ressenti si disponible
        Optional<SuiviTraitement> dernierSuivi = suivis.stream()
                .filter(s -> s.getRessenti() != null)
                .reduce((first, second) -> second);

        if (dernierSuivi.isPresent()) {
            RessentiSuivi ressenti = dernierSuivi.get().getRessenti();
            if (ressenti == RessentiSuivi.TRES_BIEN || ressenti == RessentiSuivi.BIEN) {
                observations.add("😊 Ressenti positif rapporté récemment");
            } else if (ressenti == RessentiSuivi.DIFFICILE || ressenti == RessentiSuivi.TRES_DIFFICILE) {
                observations.add("😟 Ressenti difficile nécessitant une attention");
            }
        }

        return observations;
    }

    /**
     * Génère les recommandations personnalisées
     */
    private List<String> genererRecommandations(List<SuiviTraitement> suivis, Traitement traitement,
                                                double scoreProgression, String niveauRisque) {
        List<String> recommandations = new ArrayList<>();

        // Recommandations basées sur la progression avec seuils améliorés
        if (scoreProgression > 0.4) {
            recommandations.add("🎯 Excellente progression - continuer le traitement actuel");
            recommandations.add("📊 Maintenir la fréquence des suivis actuels");
            recommandations.add("🌟 Partager les positifs avec le psychologue");
        } else if (scoreProgression < -0.4) {
            recommandations.add("🚨 Nécessite une consultation rapide pour ajustement");
            recommandations.add("🔄 Envisager une réévaluation du traitement");
            recommandations.add("📝 Augmenter temporairement la fréquence des suivis");
        } else {
            recommandations.add("🔍 Analyser les facteurs influençant la stabilité");
            recommandations.add("💬 Discuter des stratégies pour débloquer la progression");
            recommandations.add("📈 Explorer de nouvelles approches si nécessaire");
        }

        // Recommandations basées sur le niveau de risque
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
            case "minimal":
                recommandations.add("✅ Continuer le traitement comme prévu");
                break;
        }

        // Recommandations basées sur la régularité des suivis
        if (suivis.size() < 3) {
            recommandations.add("📝 Enregistrer des suivis plus fréquents (2-3 fois par semaine)");
        }

        // Recommandations basées sur la durée du traitement
        LocalDate dateDebut = traitement.getDateDebut().toLocalDate();
        long joursEcoules = ChronoUnit.DAYS.between(
                dateDebut,
                LocalDate.now()
        );

        if (joursEcoules > traitement.getDureeJours() * 0.8) {
            recommandations.add("🏁 Préparer la fin du traitement et évaluation finale");
        } else if (joursEcoules > traitement.getDureeJours() * 0.5) {
            recommandations.add("🔍 Évaluer la mi-parcours du traitement");
        }

        return recommandations;
    }

    /**
     * Génère un résumé de l'analyse
     */
    private String genererResumeAnalyse(String tendance, double scoreProgression,
                                        String niveauRisque, int nombreSuivis) {
        StringBuilder resume = new StringBuilder();

        resume.append("Analyse IA basée sur ").append(nombreSuivis).append(" suivi(s) : ");

        switch (tendance) {
            case "amélioration":
                resume.append("📈 Progression positive détectée (").append(String.format("%.1f", scoreProgression * 100)).append("% d'amélioration)");
                break;
            case "détérioration":
                resume.append("📉 Régression observée (").append(String.format("%.1f", Math.abs(scoreProgression) * 100)).append("% de détérioration)");
                break;
            case "stable":
                resume.append("➡️ État stable (").append(String.format("%.1f", Math.abs(scoreProgression) * 100)).append("% de variation)");
                break;
        }

        resume.append(". Niveau de risque : ").append(niveauRisque).append(".");

        return resume.toString();
    }
}
