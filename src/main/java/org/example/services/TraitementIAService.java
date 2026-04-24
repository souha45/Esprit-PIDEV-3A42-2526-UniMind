package org.example.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.example.entities.Traitement;
import org.example.enums.CategorieTraitement;
import org.example.enums.PrioriteTraitement;
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
}
