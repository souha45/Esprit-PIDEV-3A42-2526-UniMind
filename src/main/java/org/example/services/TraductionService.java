package org.example.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * TraductionService - Traduction via MyMemory API (gratuite, sans cle).
 *
 * Probleme corrige :
 *   L'API MyMemory retourne l'arabe en escape sequences JSON
 *   au lieu des vrais caracteres arabes.
 *
 *   Solution : methode decodeUnicode() qui convertit chaque
 *   sequence backslash-uXXXX en vrai caractere Unicode.
 */
public class TraductionService {

    // URL de l'API MyMemory
    private static final String API_URL =
            "https://api.mymemory.translated.net/get?q=%s&langpair=%s";

    // ════════════════════════════════════════════════════════════════
    //  METHODE PRINCIPALE
    // ════════════════════════════════════════════════════════════════

    /**
     * Traduit un texte d'une langue source vers une langue cible.
     *
     * @param texte      Texte a traduire
     * @param langSource Code langue source : "fr", "en", "ar"
     * @param langCible  Code langue cible  : "fr", "en", "ar"
     * @return           Traduction en vrais caracteres Unicode,
     *                   ou texte original en cas d'erreur
     */
    public static String traduire(String texte, String langSource, String langCible) {
        if (texte == null || texte.isBlank()) return texte;

        try {
            // 1. Encoder le texte pour l'URL
            String encoded  = URLEncoder.encode(texte, StandardCharsets.UTF_8);
            String langPair = langSource + "|" + langCible;
            String urlStr   = String.format(API_URL, encoded, langPair);

            // 2. Connexion HTTP
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // 3. Lire la reponse en UTF-8
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 4. Parser le JSON manuellement
            String json = response.toString();
            int start   = json.indexOf("\"translatedText\":\"") + 18;
            int end     = json.indexOf("\"", start);

            if (start > 17 && end > start) {
                String raw = json.substring(start, end);

                // 5. CORRECTION PRINCIPALE : decoder les sequences backslash-uXXXX
                // MyMemory retourne l'arabe sous forme de sequences escapees
                // decodeUnicode() les convertit en vrais caracteres arabes
                return decodeUnicode(raw);
            }

        } catch (Exception e) {
            System.out.println("Erreur traduction [" + langSource + " -> " + langCible + "]: "
                    + e.getMessage());
        }

        return texte; // retourner texte original si erreur
    }

    // ════════════════════════════════════════════════════════════════
    //  CORRECTION PRINCIPALE : decodage des sequences escapees
    // ════════════════════════════════════════════════════════════════

    /**
     * Convertit les sequences d'echappement Unicode en vrais caracteres.
     *
     * L'API MyMemory encode l'arabe sous forme de sequences JSON escapees.
     * Cette methode parcourt le texte caractere par caractere,
     * detecte les sequences backslash-u suivies de 4 chiffres hexadecimaux,
     * et les remplace par le vrai caractere Unicode correspondant.
     *
     * @param input Texte brut pouvant contenir des sequences escapees
     * @return      Texte avec les vrais caracteres Unicode
     */
    private static String decodeUnicode(String input) {
        if (input == null || input.isEmpty()) return "";

        // Optimisation : si pas de backslash-u dans le texte, retourner directement
        if (!input.contains("\\u") && !input.contains("\\U")) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < input.length()) {

            // Detecter le pattern : backslash + u + 4 chiffres hex
            if (i + 5 < input.length()
                    && input.charAt(i) == '\\'
                    && (input.charAt(i + 1) == 'u' || input.charAt(i + 1) == 'U')) {

                try {
                    // Lire les 4 chiffres hexadecimaux suivants
                    String hex = input.substring(i + 2, i + 6);
                    int codePoint = Integer.parseInt(hex, 16);

                    // Convertir le code en vrai caractere Unicode
                    result.append((char) codePoint);

                    // Avancer de 6 positions (backslash + u + 4 chiffres)
                    i += 6;

                } catch (NumberFormatException e) {
                    // Pas un vrai backslash-uXXXX → ajouter tel quel
                    result.append(input.charAt(i));
                    i++;
                }

            } else {
                // Caractere normal → ajouter directement
                result.append(input.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    // ════════════════════════════════════════════════════════════════
    //  METHODES PRATIQUES
    // ════════════════════════════════════════════════════════════════

    /**
     * Traduit un texte vers le Francais.
     */
    public static String versFrancais(String texte) {
        return traduire(texte, "en", "fr");
    }

    /**
     * Traduit un texte vers l'Anglais.
     */
    public static String versAnglais(String texte) {
        return traduire(texte, "fr", "en");
    }

    /**
     * Traduit un texte vers l'Arabe.
     * Retourne les vrais caracteres arabes grace au decodage Unicode.
     */
    public static String versArabe(String texte) {
        return traduire(texte, "fr", "ar");
    }
}