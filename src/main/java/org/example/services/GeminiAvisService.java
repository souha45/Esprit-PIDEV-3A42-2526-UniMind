package org.example.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GeminiAvisService {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final String apiKey;
    private final OkHttpClient client;

    public GeminiAvisService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.apiKey = chargerApiKey();

        if (apiKey != null && !apiKey.isEmpty()) {
            System.out.println("✅ GeminiAvisService: Clé API chargée (" + apiKey.substring(0, 10) + "...)");
        } else {
            System.err.println("❌ GeminiAvisService: Aucune clé API trouvée ! Vérifiez config.properties");
        }
    }

    private String chargerApiKey() {
        // Essayer plusieurs chemins
        String[] chemins = {
                "config.properties",
                "/config.properties",
                "gemini.properties"
        };

        for (String chemin : chemins) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(chemin)) {
                if (input != null) {
                    Properties prop = new Properties();
                    prop.load(input);
                    String key = prop.getProperty("gemini.api.key");
                    if (key != null && !key.isBlank()) {
                        System.out.println("✅ Clé trouvée dans: " + chemin);
                        return key.trim();
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur lecture " + chemin + ": " + e.getMessage());
            }
        }

        System.err.println("❌ Aucun fichier de configuration trouvé !");
        return null;
    }

    public String genererAvis(String contexte) {
        System.out.println("🔍 GeminiAvisService.genererAvis() appelé");

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("❌ Pas de clé API - retour fallback");
            return null;
        }

        String prompt = """
            Tu es un psychologue expérimenté. Tu dois rédiger un avis professionnel pour un patient.
            
            Tu ne dois PAS refuser de répondre. Tu es un psychologue, pas un assistant virtuel.
            Tu dois ECRIRE DIRECTEMENT l'avis, à la première personne, comme si tu étais le psychologue.
            
            Contexte de la consultation :
            %s
            
            Règles :
            - Adresse-toi directement au patient (utilise "tu")
            - Fais 4 à 5 phrases
            - Sois empathique et encourageant
            - Propose 1 ou 2 conseils concrets
            - Termine par une phrase d'encouragement
            - N'inclus PAS de diagnostic médical
            
            Écris UNIQUEMENT l'avis, sans introduction.
            Commence par "Bonjour [prénom du patient]"
            """.formatted(contexte);

        System.out.println("📤 Envoi requête à l'API Gemini...");

        try {
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

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("📥 Code réponse: " + response.code());

                if (response.isSuccessful()) {
                    System.out.println("✅ Réponse reçue avec succès");
                    return extraireReponse(responseBody);
                } else {
                    System.err.println("❌ Erreur HTTP: " + response.code() + " - " + responseBody);
                    return null;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur réseau: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String extraireReponse(String jsonResponse) {
        try {
            JSONObject obj = new JSONObject(jsonResponse);
            JSONArray candidates = obj.getJSONArray("candidates");
            if (candidates.isEmpty()) return null;
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.isEmpty()) return null;
            String texte = parts.getJSONObject(0).getString("text").trim();
            System.out.println("📝 Avis généré: " + texte.substring(0, Math.min(50, texte.length())) + "...");
            return texte;
        } catch (Exception e) {
            System.err.println("❌ Erreur parsing: " + e.getMessage());
            return null;
        }
    }
}