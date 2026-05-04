package org.example.services;

import org.example.entities.Question;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateurQuestionsIAService {

    // ✅ Groq API URL et modèle
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile"; // gratuit et rapide

    private static String chargerCle() {
        try (InputStream is = GenerateurQuestionsIAService.class
                .getResourceAsStream("/config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String key = props.getProperty("groq.api.key");
                if (key != null && !key.isBlank()) {
                    System.out.println("🔑 Clé Groq utilisée: " +
                            key.substring(0, Math.min(10, key.length())) + "...");
                    return key;
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ config.properties introuvable");
        }
        System.out.println("❌ Clé Groq non configurée");
        return null;
    }

    // 🎯 Méthode principale
    public static List<Question> genererQuestions(
            String typeQuestionnaire,
            String nomQuestionnaire,
            int nombreQuestions,
            int questionnaireId) {

        String apiKey = chargerCle();
        if (apiKey == null) return null;

        try {
            String prompt  = construirePrompt(typeQuestionnaire, nomQuestionnaire, nombreQuestions);
            String contenu = appellerGroq(prompt, apiKey);
            System.out.println("📦 Contenu extrait : " + contenu);
            if (contenu == null) return null;
            return parserQuestions(contenu, questionnaireId);
        } catch (Exception e) {
            System.out.println("❌ Erreur génération : " + e.getMessage());
            return null;
        }
    }

    // 🧠 Construction du prompt
    private static String construirePrompt(String type, String nom, int nombre) {
        return "Tu es un expert en psychologie clinique. Génère exactement " + nombre
                + " questions QCM en français pour un questionnaire de type " + type
                + " intitulé \"" + nom + "\".\n\n"
                + "Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ou après, "
                + "sans balises markdown.\n"
                + "Format EXACT à respecter :\n"
                + "[\n"
                + "  {\n"
                + "    \"texte\": \"Question ici ?\",\n"
                + "    \"options\": [\"Jamais\", \"Parfois\", \"Souvent\", \"Toujours\"],\n"
                + "    \"scores\": [0, 1, 2, 3]\n"
                + "  }\n"
                + "]\n"
                + "Génère exactement " + nombre + " objets dans le tableau.";
    }

    // 🤖 Appel API Groq (format OpenAI compatible)
    private static String appellerGroq(String prompt, String apiKey) {
        try {
            URL url = new URL(GROQ_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);

            // ✅ Format OpenAI compatible (Groq utilise le même format)
            String body = "{"
                    + "\"model\": \"" + MODEL + "\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"Tu es un expert en psychologie clinique. "
                    + "Réponds UNIQUEMENT en JSON valide sans markdown.\"},"
                    + "  {\"role\": \"user\", \"content\": " + toJsonString(prompt) + "}"
                    + "],"
                    + "\"max_tokens\": 2000,"
                    + "\"temperature\": 0.7"
                    + "}";

            System.out.println("📤 Envoi requête Groq...");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("📡 Code HTTP Groq : " + code);

            if (code == 200) {
                Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                StringBuilder res = new StringBuilder();
                while (sc.hasNextLine()) res.append(sc.nextLine());
                sc.close();
                String raw = res.toString();
                System.out.println("📥 Réponse brute : " +
                        raw.substring(0, Math.min(400, raw.length())));
                return extraireContenuGroq(raw);

            } else {
                Scanner sc = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
                StringBuilder err = new StringBuilder();
                while (sc.hasNextLine()) err.append(sc.nextLine());
                sc.close();
                System.out.println("❌ Erreur API Groq : " + err);

                if (code == 401) System.out.println("🔑 Clé API invalide ou expirée !");
                if (code == 429) System.out.println("⏳ Quota dépassé. Attendez quelques secondes.");
                if (code == 400) System.out.println("❌ Requête invalide. Vérifiez le format.");
                return null;
            }

        } catch (Exception e) {
            System.out.println("❌ Exception Groq API : " + e.getMessage());
            return null;
        }
    }

    // 📦 Extraction contenu Groq (format OpenAI)
    // Structure : {"choices":[{"message":{"content":"..."}}]}
    private static String extraireContenuGroq(String json) {
        try {
            // ✅ Chercher "content" dans choices[0].message
            int choicesIdx = json.indexOf("\"choices\"");
            if (choicesIdx == -1) {
                System.out.println("⚠ 'choices' non trouvé dans la réponse Groq");
                return null;
            }

            int contentIdx = json.indexOf("\"content\"", choicesIdx);
            if (contentIdx == -1) {
                System.out.println("⚠ 'content' non trouvé dans la réponse Groq");
                return null;
            }

            contentIdx += 9; // sauter "content"
            while (contentIdx < json.length() &&
                    (json.charAt(contentIdx) == ':' || json.charAt(contentIdx) == ' '))
                contentIdx++;

            if (contentIdx >= json.length() || json.charAt(contentIdx) != '"') return null;
            contentIdx++;

            StringBuilder result = new StringBuilder();
            while (contentIdx < json.length()) {
                char c = json.charAt(contentIdx);
                if (c == '\\' && contentIdx + 1 < json.length()) {
                    char next = json.charAt(contentIdx + 1);
                    switch (next) {
                        case '"':  result.append('"');  contentIdx += 2; continue;
                        case 'n':  result.append('\n'); contentIdx += 2; continue;
                        case 't':  result.append('\t'); contentIdx += 2; continue;
                        case '\\': result.append('\\'); contentIdx += 2; continue;
                        case 'r':  result.append('\r'); contentIdx += 2; continue;
                        default:   result.append(next); contentIdx += 2; continue;
                    }
                }
                if (c == '"') break;
                result.append(c);
                contentIdx++;
            }

            String contenu = result.toString().trim();

            // ✅ Supprimer les balises markdown si présentes
            if (contenu.startsWith("```")) {
                contenu = contenu
                        .replaceAll("^```[a-zA-Z]*\\n?", "")
                        .replaceAll("```$", "")
                        .trim();
            }

            System.out.println("✅ Contenu extrait OK (" + contenu.length() + " chars)");
            return contenu.isBlank() ? null : contenu;

        } catch (Exception e) {
            System.out.println("❌ Erreur extraction contenu Groq : " + e.getMessage());
            return null;
        }
    }

    // 🧾 Parser le tableau JSON de questions
    private static List<Question> parserQuestions(String jsonBrut, int questionnaireId) {
        List<Question> questions = new ArrayList<>();

        try {
            int arrayStart = jsonBrut.indexOf('[');
            int arrayEnd   = jsonBrut.lastIndexOf(']');
            if (arrayStart == -1 || arrayEnd == -1) {
                System.out.println("❌ Pas de tableau JSON trouvé dans : " +
                        jsonBrut.substring(0, Math.min(200, jsonBrut.length())));
                return questions;
            }

            String arrayContent = jsonBrut.substring(arrayStart + 1, arrayEnd);
            int depth    = 0;
            int objStart = -1;

            for (int i = 0; i < arrayContent.length(); i++) {
                char c = arrayContent.charAt(i);
                if (c == '{') {
                    if (depth == 0) objStart = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart != -1) {
                        String obj = arrayContent.substring(objStart, i + 1);
                        Question q = parserUneQuestion(obj, questionnaireId);
                        if (q != null) questions.add(q);
                        objStart = -1;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur parsing JSON : " + e.getMessage());
        }

        System.out.println("✅ Questions parsées : " + questions.size());
        return questions;
    }

    private static Question parserUneQuestion(String obj, int questionnaireId) {
        try {
            String texte         = extraireValeurString(obj, "texte");
            List<String>  options = extraireTableauStrings(obj, "options");
            List<Integer> scores  = extraireTableauIntegers(obj, "scores");

            if (texte == null || texte.isBlank()) {
                System.out.println("⚠ Texte vide, question ignorée");
                return null;
            }
            if (options.size() < 2) {
                System.out.println("⚠ Pas assez d'options (" + options.size() + "), ignorée");
                return null;
            }

            // ✅ Compléter les scores si manquants
            while (scores.size() < options.size()) scores.add(scores.size());

            String optStr = String.join("|", options);
            StringBuilder scStr = new StringBuilder();
            for (int j = 0; j < scores.size(); j++) {
                if (j > 0) scStr.append("|");
                scStr.append(scores.get(j));
            }

            return new Question(texte, optStr, scStr.toString(), "QCM", questionnaireId);

        } catch (Exception e) {
            System.out.println("⚠ Erreur parsing question : " + e.getMessage());
            return null;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String extraireValeurString(String obj, String champ) {
        String key = "\"" + champ + "\"";
        int idx = obj.indexOf(key);
        if (idx == -1) return null;
        idx += key.length();
        while (idx < obj.length() &&
                (obj.charAt(idx) == ':' || obj.charAt(idx) == ' ')) idx++;
        if (idx >= obj.length() || obj.charAt(idx) != '"') return null;
        idx++;

        StringBuilder result = new StringBuilder();
        while (idx < obj.length()) {
            char c = obj.charAt(idx);
            if (c == '\\' && idx + 1 < obj.length()) {
                result.append(obj.charAt(idx + 1));
                idx += 2;
                continue;
            }
            if (c == '"') break;
            result.append(c);
            idx++;
        }
        return result.toString().trim();
    }

    private static List<String> extraireTableauStrings(String obj, String champ) {
        List<String> list = new ArrayList<>();
        try {
            int champIdx = obj.indexOf("\"" + champ + "\"");
            if (champIdx == -1) return list;
            int start = obj.indexOf('[', champIdx);
            int end   = obj.indexOf(']', start);
            if (start == -1 || end == -1) return list;
            String content = obj.substring(start + 1, end);

            int i = 0;
            while (i < content.length()) {
                int q1 = content.indexOf('"', i);
                if (q1 == -1) break;
                int q2 = q1 + 1;
                while (q2 < content.length()) {
                    if (content.charAt(q2) == '"' &&
                            content.charAt(q2 - 1) != '\\') break;
                    q2++;
                }
                list.add(content.substring(q1 + 1, q2));
                i = q2 + 1;
            }
        } catch (Exception e) {
            System.out.println("⚠ extraireTableauStrings : " + e.getMessage());
        }
        return list;
    }

    private static List<Integer> extraireTableauIntegers(String obj, String champ) {
        List<Integer> list = new ArrayList<>();
        try {
            int champIdx = obj.indexOf("\"" + champ + "\"");
            if (champIdx == -1) return list;
            int start = obj.indexOf('[', champIdx);
            int end   = obj.indexOf(']', start);
            if (start == -1 || end == -1) return list;
            String content = obj.substring(start + 1, end);
            for (String s : content.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) list.add(Integer.parseInt(trimmed));
            }
        } catch (Exception e) {
            System.out.println("⚠ extraireTableauIntegers : " + e.getMessage());
        }
        return list;
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