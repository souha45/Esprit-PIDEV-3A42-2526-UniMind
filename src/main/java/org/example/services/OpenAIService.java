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

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    // Charge la clé depuis plusieurs sources (ordre de priorité)
    private static final String API_KEY = chargerCle();

    private static String chargerCle() {
        // 1. Propriété système : -DGROQ_API_KEY=... dans pom.xml (priorité haute)
        String sysProp = System.getProperty("GROQ_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) {
            System.out.println("✅ Clé chargée depuis propriété système (pom.xml)");
            return sysProp;
        }

        // 2. Variable d'environnement OS
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            System.out.println("✅ Clé chargée depuis variable d'environnement");
            return envKey;
        }

        // 3. Fichier config.properties dans les ressources (non versionné)
        try (InputStream is = OpenAIService.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String key = props.getProperty("groq.api.key");
                if (key != null && !key.isBlank() && !key.equals("VOTRE_CLE_ICI")) {
                    System.out.println("✅ Clé chargée depuis config.properties");
                    return key;
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ config.properties non trouvé : " + e.getMessage());
        }

        System.out.println("❌ Aucune clé GROQ_API_KEY trouvée !");
        return null;
    }

    public static String analyserReponses(
            String nomQuestionnaire, String typeQuestionnaire,
            double score, String niveau, String interpretation) {
        return appellerGroq(nomQuestionnaire, typeQuestionnaire, score, niveau, interpretation);
    }

    public static String analyserReponsesDetaillees(
            String nomQuestionnaire, String typeQuestionnaire,
            double score, String niveau, String interpretation, String reponsesJson) {
        return appellerGroq(nomQuestionnaire, typeQuestionnaire, score, niveau, interpretation);
    }

    private static String appellerGroq(
            String nomQ, String typeQ, double score, String niveau, String interpretation) {

        if (API_KEY == null || API_KEY.isBlank()) {
            return "❌ Clé API non configurée.\n"
                    + "Dans pom.xml → plugin javafx → options :\n"
                    + "<option>-DGROQ_API_KEY=votre_cle</option>";
        }

        try {
            System.out.println("=== Appel Groq API === modèle: " + MODEL);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            String userMessage = "Un étudiant a complété le questionnaire \""
                    + nomQ + "\" de type " + typeQ
                    + " avec un score de " + String.format("%.0f", score)
                    + " (niveau: " + (niveau != null ? niveau : "non défini") + "). "
                    + "Interprétation: " + (interpretation != null ? interpretation : "non disponible") + ". "
                    + "Donne exactement 3 conseils pratiques et bienveillants en français "
                    + "pour aider cet étudiant. Commence par '1.' directement.";

            String body = "{"
                    + "\"model\": \"" + MODEL + "\","
                    + "\"messages\": ["
                    + "{\"role\": \"system\", \"content\": \"Tu es un assistant psychologique bienveillant spécialisé dans le bien-être des étudiants.\"},"
                    + "{\"role\": \"user\", \"content\": " + toJsonString(userMessage) + "}"
                    + "],"
                    + "\"max_tokens\": 500,"
                    + "\"temperature\": 0.7"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("Code HTTP : " + code);

            if (code == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) response.append(scanner.nextLine());
                scanner.close();

                String raw = response.toString();
                System.out.println("Réponse brute : " + raw.substring(0, Math.min(400, raw.length())));

                String contenu = extraireContenu(raw);
                System.out.println("Contenu extrait : " + contenu);
                return contenu;

            } else {
                String errBody = "";
                try {
                    Scanner sc = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
                    StringBuilder err = new StringBuilder();
                    while (sc.hasNextLine()) err.append(sc.nextLine());
                    sc.close();
                    errBody = err.toString();
                } catch (Exception ignored) {}

                System.out.println("❌ Erreur HTTP " + code + " : " + errBody);

                if (code == 401) return "❌ Clé API invalide ou expirée.\nRenouvelez-la sur https://console.groq.com";
                if (code == 429) return "⏳ Limite d'appels atteinte. Réessayez dans quelques secondes.";
                if (code == 400) return "❌ Requête invalide (400). Vérifiez le modèle ou la clé API.";
                return "❌ Erreur serveur (" + code + "). Vérifiez votre connexion.";
            }

        } catch (java.net.UnknownHostException e) {
            return "🔌 Pas de connexion internet. Vérifiez votre réseau.";
        } catch (java.net.SocketTimeoutException e) {
            return "⏳ Délai dépassé. Réessayez dans quelques instants.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur : " + e.getMessage();
        }
    }

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

            String contenu = result.toString().trim();
            return contenu.isBlank() ? null : contenu;

        } catch (Exception e) {
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