package org.example.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GeminiService {

    // ✅ Utilise la MÊME version que FlutterFlow (gemini-2.5-flash)
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final String       apiKey;
    private final OkHttpClient client;

    public GeminiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.apiKey = chargerApiKey();

        // Afficher la clé pour déboguer (masquée partiellement)
        if (apiKey != null && !apiKey.isEmpty()) {
            System.out.println("✅ Clé API chargée: " + apiKey.substring(0, 10) + "...");
        } else {
            System.out.println("❌ Aucune clé API trouvée");
        }
    }

    private String chargerApiKey() {
        // Essayer de lire depuis config.properties
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String key = prop.getProperty("gemini.api.key");
                if (key != null && !key.isBlank()) {
                    return key.trim();
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lecture config: " + e.getMessage());
        }

        // Fallback: utiliser la clé qui fonctionne dans FlutterFlow
        // (pour les tests seulement)
        return "API travaillé avec succés";
    }

    public String envoyerMessage(String message, String historique) {
        if (apiKey == null || apiKey.isBlank()) {
            return "❌ Clé API non configurée";
        }

        try {
            // Construction du prompt avec historique
            String prompt = construirePrompt(message, historique);

            // Corps de la requête - IDENTIQUE au FlutterFlow
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            // Construction de l'URL avec la clé
            String url = API_URL + "?key=" + apiKey;

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            System.out.println("📤 Envoi requête à: " + url);
            System.out.println("📦 Body: " + requestBody.toString());

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("📥 Code réponse: " + response.code());
                System.out.println("📄 Réponse: " + responseBody);

                if (response.isSuccessful()) {
                    return extraireReponse(responseBody);
                } else if (response.code() == 429) {
                    return "⚠️ Quota journalier atteint. Réessayez demain ou utilisez une autre clé.";
                } else {
                    return "❌ Erreur " + response.code() + ": " + responseBody;
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur réseau: " + e.getMessage());
            return "❌ Problème de connexion: " + e.getMessage();
        }
    }

    private String construirePrompt(String message, String historique) {
        StringBuilder sb = new StringBuilder();

        sb.append("Tu es un assistant bienveillant pour étudiants.\n");
        sb.append("Règles: réponds en français, sois concis (2-4 phrases), donne des conseils pratiques.\n");
        sb.append("Ne fais JAMAIS de diagnostic médical.\n\n");

        if (historique != null && !historique.isBlank()) {
            sb.append("Historique:\n").append(historique).append("\n\n");
        }

        sb.append("Étudiant: ").append(message).append("\n");
        sb.append("Assistant: ");

        return sb.toString();
    }

    private String extraireReponse(String jsonResponse) {
        try {
            JSONObject obj = new JSONObject(jsonResponse);
            JSONArray candidates = obj.getJSONArray("candidates");

            if (candidates.isEmpty()) {
                return "Je n'ai pas pu générer de réponse.";
            }

            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");

            if (parts.isEmpty()) {
                return "Réponse vide.";
            }

            return parts.getJSONObject(0).getString("text").trim();

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing: " + e.getMessage());
            return "Erreur de traitement de la réponse.";
        }
    }
}