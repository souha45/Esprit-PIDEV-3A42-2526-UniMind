package org.example.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.example.entities.Traitement;
import org.example.entities.SuiviTraitement;

/**
 * Service de traduction pour les traitements et suivis
 * Basé sur le service TranslationService de Symfony
 */
public class TraitementTranslationService {

    private Map<String, String> supportedLanguages;
    private String defaultLanguage;

    public TraitementTranslationService() {
        this.supportedLanguages = new HashMap<>();
        this.supportedLanguages.put("fr", "Français");
        this.supportedLanguages.put("en", "English");
        this.supportedLanguages.put("es", "Español");
        this.supportedLanguages.put("de", "Deutsch");
        this.supportedLanguages.put("it", "Italiano");
        this.supportedLanguages.put("pt", "Português");
        this.supportedLanguages.put("ar", "العربية");
        this.supportedLanguages.put("zh", "中文");
        this.supportedLanguages.put("ja", "日本語");
        this.supportedLanguages.put("ru", "Русский");
        this.defaultLanguage = "fr";
    }

    /**
     * Obtient les langues supportées
     */
    public Map<String, String> getSupportedLanguages() {
        return new HashMap<>(supportedLanguages);
    }

    /**
     * Traduit un texte vers une langue cible
     */
    public TranslationResult translateText(String text, String targetLanguage, String sourceLanguage) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return new TranslationResult(true, text, text, sourceLanguage, null);
            }

            if (!isLanguageSupported(targetLanguage)) {
                return new TranslationResult(false, text, text, sourceLanguage,
                        "Langue cible non supportée. Langues disponibles: " + String.join(", ", supportedLanguages.keySet()));
            }

            String translatedText = translateWithGoogleTranslate(text, targetLanguage, sourceLanguage);

            if (translatedText != null) {
                System.out.println("Traduction réussie: " + text.substring(0, Math.min(100, text.length())) +
                        " -> " + translatedText.substring(0, Math.min(100, translatedText.length())));
                return new TranslationResult(true, translatedText, text, sourceLanguage, null);
            }

            // Fallback: retourner le texte original
            return new TranslationResult(true, text, text, sourceLanguage, null);

        } catch (Exception e) {
            System.err.println("Erreur de traduction: " + e.getMessage());
            return new TranslationResult(false, text, text, sourceLanguage,
                    "Erreur lors de la traduction: " + e.getMessage());
        }
    }

    /**
     * Traduit en utilisant l'API Google Translate
     */
    private String translateWithGoogleTranslate(String text, String targetLanguage, String sourceLanguage) {
        try {
            String sourceParam = "auto".equals(sourceLanguage) ? "auto" : sourceLanguage;
            String baseUrl = "https://translate.googleapis.com/translate_a/single";

            String params = String.format("client=gtx&sl=%s&tl=%s&dt=t&q=%s",
                    sourceParam,
                    targetLanguage,
                    URLEncoder.encode(text, StandardCharsets.UTF_8)
            );

            URL url = new URL(baseUrl + "?" + params);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Configuration de la connexion
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Parser la réponse JSON
                    return parseGoogleTranslateResponse(response.toString());
                }
            } else {
                System.err.println("Erreur HTTP: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Exception dans translateWithGoogleTranslate: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse la réponse de l'API Google Translate
     */
    private String parseGoogleTranslateResponse(String jsonResponse) {
        try {
            // La réponse est un tableau JSON complexe
            // Format: [[[["texte_traduit", "langue_source"], ...], ...], ...]

            if (jsonResponse.startsWith("[[")) {
                // Extraire le premier élément qui contient la traduction
                int firstQuote = jsonResponse.indexOf("\"");
                int secondQuote = jsonResponse.indexOf("\"", firstQuote + 1);

                if (firstQuote != -1 && secondQuote != -1) {
                    return jsonResponse.substring(firstQuote + 1, secondQuote);
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("Erreur parsing JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Traduit les données d'un traitement
     */
    public TraitementTranslationResult translateTraitement(Traitement traitement, String targetLanguage) {
        TraitementTranslationResult result = new TraitementTranslationResult();
        boolean hasError = false;

        // Champs à traduire
        String[] fieldsToTranslate = {"titre", "description", "objectifTherapeutique", "type", "dosage"};

        for (String field : fieldsToTranslate) {
            String fieldValue = getFieldValue(traitement, field);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                TranslationResult translationResult = translateText(fieldValue, targetLanguage, "auto");

                result.addTranslation(field, fieldValue, translationResult.getTranslatedText(), translationResult.getError());

                if (!translationResult.isSuccess()) {
                    hasError = true;
                }
            }
        }

        result.setTargetLanguage(targetLanguage);
        result.setSuccess(!hasError);

        return result;
    }

    /**
     * Traduit les observations d'un suivi
     */
    public SuiviTranslationResult translateSuiviObservations(SuiviTraitement suivi, String targetLanguage) {
        SuiviTranslationResult result = new SuiviTranslationResult();

        if (suivi.getObservations() != null && !suivi.getObservations().trim().isEmpty()) {
            TranslationResult translationResult = translateText(suivi.getObservations(), targetLanguage, "auto");

            result.setObservationsOriginal(suivi.getObservations());
            result.setObservationsTranslated(translationResult.getTranslatedText());
            result.setError(translationResult.getError());
            result.setSuccess(translationResult.isSuccess());
        } else {
            result.setSuccess(true);
        }

        return result;
    }

    /**
     * Obtient la valeur d'un champ par réflexion
     */
    private String getFieldValue(Traitement traitement, String fieldName) {
        try {
            switch (fieldName) {
                case "titre":
                    return traitement.getTitre();
                case "description":
                    return traitement.getDescription();
                case "objectifTherapeutique":
                    return traitement.getObjectifTherapeutique();
                case "type":
                    return traitement.getType();
                case "dosage":
                    return traitement.getDosage();
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Erreur getFieldValue pour " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si une langue est supportée
     */
    public boolean isLanguageSupported(String language) {
        return supportedLanguages.containsKey(language);
    }

    /**
     * Obtient le nom d'une langue
     */
    public String getLanguageName(String languageCode) {
        return supportedLanguages.getOrDefault(languageCode, languageCode);
    }

    /**
     * Classe résultat pour la traduction
     */
    public static class TranslationResult {
        private boolean success;
        private String translatedText;
        private String originalText;
        private String sourceLanguage;
        private String error;

        public TranslationResult(boolean success, String translatedText, String originalText,
                                 String sourceLanguage, String error) {
            this.success = success;
            this.translatedText = translatedText;
            this.originalText = originalText;
            this.sourceLanguage = sourceLanguage;
            this.error = error;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getTranslatedText() { return translatedText; }
        public String getOriginalText() { return originalText; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getError() { return error; }
    }

    /**
     * Classe résultat pour la traduction de traitement
     */
    public static class TraitementTranslationResult {
        private Map<String, TranslationData> translations;
        private String targetLanguage;
        private boolean success;

        public TraitementTranslationResult() {
            this.translations = new HashMap<>();
        }

        public void addTranslation(String field, String original, String translated, String error) {
            translations.put(field, new TranslationData(original, translated, error));
        }

        // Getters/Setters
        public Map<String, TranslationData> getTranslations() { return translations; }
        public String getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public TranslationData getTranslation(String field) {
            return translations.get(field);
        }

        public static class TranslationData {
            private String original;
            private String translated;
            private String error;

            public TranslationData(String original, String translated, String error) {
                this.original = original;
                this.translated = translated;
                this.error = error;
            }

            // Getters
            public String getOriginal() { return original; }
            public String getTranslated() { return translated; }
            public String getError() { return error; }
        }
    }

    /**
     * Classe résultat pour la traduction de suivi
     */
    public static class SuiviTranslationResult {
        private String observationsOriginal;
        private String observationsTranslated;
        private String error;
        private boolean success;

        // Getters/Setters
        public String getObservationsOriginal() { return observationsOriginal; }
        public void setObservationsOriginal(String observationsOriginal) { this.observationsOriginal = observationsOriginal; }
        public String getObservationsTranslated() { return observationsTranslated; }
        public void setObservationsTranslated(String observationsTranslated) { this.observationsTranslated = observationsTranslated; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}
