package org.example.services;

import org.example.entities.Question;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Service IA pour générer des questions QCM selon le type du questionnaire.
 * Utilise l'API Groq (llama-3.3-70b-versatile).
 */
public class GenerateurQuestionsIAService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    private static final String API_KEY = chargerCle();

    private static String chargerCle() {
        String sysProp = System.getProperty("GROQ_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;

        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey;

        try (InputStream is = GenerateurQuestionsIAService.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String key = props.getProperty("groq.api.key");
                if (key != null && !key.isBlank() && !key.equals("VOTRE_CLE_ICI")) return key;
            }
        } catch (IOException e) {
            System.out.println("⚠ config.properties introuvable");
        }
        return null;
    }

    /**
     * Génère une liste de questions QCM selon le type et le nombre demandé.
     *
     * @param typeQuestionnaire  ex: "DEPRESSION", "ANXIETE", "STRESS"
     * @param nomQuestionnaire   nom du questionnaire pour contextualiser
     * @param nombreQuestions    nombre de questions à générer
     * @param questionnaireId    ID du questionnaire pour lier les questions
     * @return liste de Question prêtes à être insérées en BDD
     */
    public static List<Question> genererQuestions(
            String typeQuestionnaire,
            String nomQuestionnaire,
            int nombreQuestions,
            int questionnaireId) {

        if (API_KEY == null || API_KEY.isBlank()) {
            System.out.println("❌ Clé API non configurée");
            return null;
        }

        try {
            String prompt = construirePrompt(typeQuestionnaire, nomQuestionnaire, nombreQuestions);
            String jsonBrut = appellerGroq(prompt);
            if (jsonBrut == null) return null;

            return parserQuestions(jsonBrut, questionnaireId);

        } catch (Exception e) {
            System.out.println("❌ Erreur génération : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTION DU PROMPT
    // ─────────────────────────────────────────────────────────────

    private static String construirePrompt(String type, String nom, int nombre) {
        return "Tu es un expert en psychologie clinique. Génère exactement " + nombre
                + " questions QCM en français pour un questionnaire de type " + type
                + " intitulé \"" + nom + "\".\n\n"
                + "RÈGLES STRICTES :\n"
                + "1. Chaque question doit avoir exactement 4 options de réponse\n"
                + "2. Les scores doivent être 0, 1, 2, 3 (du moins grave au plus grave)\n"
                + "3. Les questions doivent être cliniquement pertinentes pour évaluer " + type + "\n"
                + "4. Réponds UNIQUEMENT en JSON valide, sans texte avant ou après\n\n"
                + "FORMAT JSON OBLIGATOIRE :\n"
                + "[\n"
                + "  {\n"
                + "    \"texte\": \"Texte de la question ?\",\n"
                + "    \"options\": [\"Jamais\", \"Parfois\", \"Souvent\", \"Toujours\"],\n"
                + "    \"scores\": [0, 1, 2, 3]\n"
                + "  }\n"
                + "]\n\n"
                + "Génère maintenant " + nombre + " questions pour le type " + type + ".";
    }

    // ─────────────────────────────────────────────────────────────
    //  APPEL API GROQ
    // ─────────────────────────────────────────────────────────────

    private static String appellerGroq(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            String body = "{"
                    + "\"model\": \"" + MODEL + "\","
                    + "\"messages\": ["
                    + "{\"role\": \"system\", \"content\": \"Tu es un expert en psychologie. Tu réponds UNIQUEMENT en JSON valide, sans markdown, sans texte supplémentaire.\"},"
                    + "{\"role\": \"user\", \"content\": " + toJsonString(prompt) + "}"
                    + "],"
                    + "\"max_tokens\": 3000,"
                    + "\"temperature\": 0.6"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("Code HTTP Groq : " + code);

            if (code == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) response.append(scanner.nextLine());
                scanner.close();
                return extraireContenu(response.toString());
            } else {
                Scanner sc = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
                StringBuilder err = new StringBuilder();
                while (sc.hasNextLine()) err.append(sc.nextLine());
                sc.close();
                System.out.println("❌ Erreur Groq : " + err);
                return null;
            }

        } catch (Exception e) {
            System.out.println("❌ Exception appel Groq : " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  EXTRACTION DU CONTENU JSON DEPUIS LA RÉPONSE GROQ
    // ─────────────────────────────────────────────────────────────

    private static String extraireContenu(String json) {
        try {
            int idx = json.indexOf("\"content\"");
            if (idx == -1) return null;
            idx += 9;
            while (idx < json.length() && (json.charAt(idx) == ':' || json.charAt(idx) == ' ')) idx++;
            if (idx >= json.length() || json.charAt(idx) != '"') return null;
            idx++;

            StringBuilder result = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == '\\' && idx + 1 < json.length()) {
                    char next = json.charAt(idx + 1);
                    switch (next) {
                        case '"':  result.append('"');  idx += 2; continue;
                        case 'n':  result.append('\n'); idx += 2; continue;
                        case 't':  result.append('\t'); idx += 2; continue;
                        case '\\': result.append('\\'); idx += 2; continue;
                        case 'r':  result.append('\r'); idx += 2; continue;
                        default:   result.append(next); idx += 2; continue;
                    }
                }
                if (c == '"') break;
                result.append(c);
                idx++;
            }
            return result.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PARSING DU JSON → LIST<QUESTION>
    // ─────────────────────────────────────────────────────────────

    private static List<Question> parserQuestions(String jsonBrut, int questionnaireId) {
        List<Question> questions = new ArrayList<>();

        try {
            // Nettoyer le JSON (enlever éventuels backticks markdown)
            String json = jsonBrut.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            System.out.println("JSON reçu : " + json.substring(0, Math.min(300, json.length())));

            // Parser le tableau JSON manuellement
            // Format : [{"texte":"...","options":["...","...","...","..."],"scores":[0,1,2,3]}, ...]
            int i = 0;
            while (i < json.length()) {
                // Chercher le début d'un objet question
                int debutObj = json.indexOf("{", i);
                if (debutObj == -1) break;

                // Trouver la fin de cet objet
                int finObj = trouverFinObjet(json, debutObj);
                if (finObj == -1) break;

                String objJson = json.substring(debutObj, finObj + 1);

                // Extraire texte
                String texte = extraireChamp(objJson, "texte");

                // Extraire options
                List<String> options = extraireTableauStrings(objJson, "options");

                // Extraire scores
                List<Integer> scores = extraireTableauIntegers(objJson, "scores");

                if (texte != null && options.size() == 4 && scores.size() == 4) {
                    // Construire optionsQuest : "Option1|Option2|Option3|Option4"
                    String optionsStr = String.join("|", options);

                    // Construire scoreOptions : "0|1|2|3"
                    StringBuilder scoresStr = new StringBuilder();
                    for (int s = 0; s < scores.size(); s++) {
                        if (s > 0) scoresStr.append("|");
                        scoresStr.append(scores.get(s));
                    }

                    Question q = new Question(
                            texte,
                            optionsStr,
                            scoresStr.toString(),
                            "QCM",
                            questionnaireId
                    );
                    questions.add(q);
                    System.out.println("✅ Question parsée : " + texte.substring(0, Math.min(50, texte.length())));
                }

                i = finObj + 1;
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur parsing JSON : " + e.getMessage());
            e.printStackTrace();
        }

        return questions;
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS PARSING JSON MANUEL
    // ─────────────────────────────────────────────────────────────

    private static int trouverFinObjet(String json, int debut) {
        int depth = 0;
        boolean inString = false;
        for (int i = debut; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String extraireChamp(String obj, String champ) {
        String key = "\"" + champ + "\"";
        int idx = obj.indexOf(key);
        if (idx == -1) return null;
        idx += key.length();
        while (idx < obj.length() && (obj.charAt(idx) == ':' || obj.charAt(idx) == ' ')) idx++;
        if (idx >= obj.length() || obj.charAt(idx) != '"') return null;
        idx++;

        StringBuilder val = new StringBuilder();
        while (idx < obj.length()) {
            char c = obj.charAt(idx);
            if (c == '\\' && idx + 1 < obj.length()) {
                val.append(obj.charAt(idx + 1)); idx += 2; continue;
            }
            if (c == '"') break;
            val.append(c); idx++;
        }
        return val.toString().trim();
    }

    private static List<String> extraireTableauStrings(String obj, String champ) {
        List<String> result = new ArrayList<>();
        String key = "\"" + champ + "\"";
        int idx = obj.indexOf(key);
        if (idx == -1) return result;
        idx = obj.indexOf("[", idx);
        if (idx == -1) return result;
        int fin = obj.indexOf("]", idx);
        if (fin == -1) return result;

        String tableau = obj.substring(idx + 1, fin);
        int i = 0;
        while (i < tableau.length()) {
            int debut = tableau.indexOf("\"", i);
            if (debut == -1) break;
            debut++;
            StringBuilder val = new StringBuilder();
            while (debut < tableau.length()) {
                char c = tableau.charAt(debut);
                if (c == '\\' && debut + 1 < tableau.length()) {
                    val.append(tableau.charAt(debut + 1)); debut += 2; continue;
                }
                if (c == '"') break;
                val.append(c); debut++;
            }
            result.add(val.toString());
            i = debut + 1;
        }
        return result;
    }

    private static List<Integer> extraireTableauIntegers(String obj, String champ) {
        List<Integer> result = new ArrayList<>();
        String key = "\"" + champ + "\"";
        int idx = obj.indexOf(key);
        if (idx == -1) return result;
        idx = obj.indexOf("[", idx);
        if (idx == -1) return result;
        int fin = obj.indexOf("]", idx);
        if (fin == -1) return result;

        String tableau = obj.substring(idx + 1, fin);
        for (String part : tableau.split(",")) {
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private static String toJsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}