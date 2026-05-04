package org.example.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

public class OpenAIService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "llama-3.3-70b-versatile";

    private static String chargerCle() {
        try (InputStream is = OpenAIService.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                // ✅ Utilise la clé Groq
                String key = props.getProperty("groq.api.key");
                if (key != null && !key.isBlank()) {
                    System.out.println("✅ Clé Groq chargée : " +
                            key.substring(0, Math.min(10, key.length())) + "...");
                    return key;
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ config.properties non trouvé : " + e.getMessage());
        }
        System.out.println("❌ Aucune clé Groq trouvée !");
        return null;
    }

    public static String analyserReponses(
            String nomQuestionnaire, String typeQuestionnaire,
            double score, String niveau, String interpretation) {
        return appellerGroq(nomQuestionnaire, typeQuestionnaire,
                score, niveau, interpretation, null);
    }

    public static String analyserReponsesDetaillees(
            String nomQuestionnaire, String typeQuestionnaire,
            double score, String niveau, String interpretation, String reponsesJson) {
        return appellerGroq(nomQuestionnaire, typeQuestionnaire,
                score, niveau, interpretation, reponsesJson);
    }

    private static String appellerGroq(
            String nomQ, String typeQ,
            double score, String niveau,
            String interpretation, String reponsesJson) {

        String apiKey = chargerCle();
        if (apiKey == null || apiKey.isBlank()) {
            return "❌ Clé Groq non configurée.";
        }

        try {
            URL url = new URL(GROQ_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            // ✅ Message utilisateur enrichi
            StringBuilder userMessage = new StringBuilder();
            userMessage.append("Un étudiant a complété le questionnaire \"")
                    .append(nomQ).append("\" de type ").append(typeQ)
                    .append(" avec un score de ").append(String.format("%.0f", score))
                    .append(" (niveau: ").append(niveau != null ? niveau : "non défini").append("). ")
                    .append("Interprétation de base: ")
                    .append(interpretation != null ? interpretation : "non disponible").append(". ");

            if (reponsesJson != null && !reponsesJson.isBlank()) {
                userMessage.append("Réponses détaillées: ").append(reponsesJson).append(". ");
            }

            userMessage.append("En tant que psychologue bienveillant, donne exactement 3 conseils ")
                    .append("pratiques et personnalisés en français pour aider cet étudiant. ")
                    .append("Commence directement par '1.' sans introduction.");

            // ✅ Format OpenAI compatible (Groq)
            String body = "{"
                    + "\"model\": \"" + MODEL + "\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"Tu es un psychologue clinicien "
                    + "bienveillant et expert. Tu donnes des conseils pratiques, empathiques "
                    + "et adaptés au contexte psychologique de l'étudiant. "
                    + "Tu réponds toujours en français.\"},"
                    + "  {\"role\": \"user\", \"content\": "
                    + toJsonString(userMessage.toString()) + "}"
                    + "],"
                    + "\"max_tokens\": 600,"
                    + "\"temperature\": 0.7"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("📡 Code HTTP Groq : " + code);

            if (code == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) response.append(scanner.nextLine());
                scanner.close();

                String raw = response.toString();
                System.out.println("📥 Réponse Groq : " +
                        raw.substring(0, Math.min(300, raw.length())));
                return extraireContenuGroq(raw);

            } else {
                String errBody = "";
                try {
                    Scanner sc = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
                    StringBuilder err = new StringBuilder();
                    while (sc.hasNextLine()) err.append(sc.nextLine());
                    sc.close();
                    errBody = err.toString();
                } catch (Exception ignored) {}

                System.out.println("❌ Erreur Groq API : " + errBody);
                if (code == 401) return "❌ Clé Groq invalide.";
                if (code == 429) return "⏳ Limite d'appels atteinte. Réessayez.";
                return "❌ Erreur serveur (" + code + ").";
            }

        } catch (java.net.UnknownHostException e) {
            return "🔌 Pas de connexion internet.";
        } catch (java.net.SocketTimeoutException e) {
            return "⏳ Délai dépassé. Réessayez.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur : " + e.getMessage();
        }
    }

    // ✅ Extraction réponse format Groq/OpenAI
    // {"choices":[{"message":{"content":"..."}}]}
    private static String extraireContenuGroq(String json) {
        try {
            int choicesIdx = json.indexOf("\"choices\"");
            if (choicesIdx == -1) return null;

            int contentIdx = json.indexOf("\"content\"", choicesIdx);
            if (contentIdx == -1) return null;

            contentIdx += 9;
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
            return contenu.isBlank() ? null : contenu;

        } catch (Exception e) {
            System.out.println("❌ Erreur extraction Groq : " + e.getMessage());
            return null;
        }
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