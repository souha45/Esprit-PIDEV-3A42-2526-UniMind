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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

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

        sb.append("Tu es l'assistant virtuel d'Unimind, une application de santé mentale pour étudiants.\n\n");

        sb.append("🧠 **TON RÔLE UNIQUEMENT :**\n");
        sb.append("- Aider les étudiants avec des conseils de bien-être mental (stress, anxiété, sommeil, motivation, gestion des émotions)\n");
        sb.append("- Répondre aux questions sur l'application Unimind (fonctionnalités, prise de rendez-vous, consultations, etc.)\n");
        sb.append("- Proposer des techniques de relaxation, respiration, gestion du temps\n");
        sb.append("- Accompagner l'étudiant vers une meilleure santé mentale\n\n");

        sb.append("🚫 **CE QUE TU NE DOIS PAS FAIRE :**\n");
        sb.append("- Ne JAMAIS répondre à des questions hors contexte de santé mentale et bien-être\n");
        sb.append("- Ne JAMAIS donner des conseils sur des sujets non liés (maths, physique, programmation, cuisine, sport, technologie, etc.)\n");
        sb.append("- Ne JAMAIS poser de diagnostic médical\n");
        sb.append("- Si l'utilisateur pose une question hors sujet, répondre POLIMENT : \"Désolé, je suis spécialisé dans le bien-être mental et les fonctionnalités d'Unimind. Je ne peux pas répondre à cette question. Puis-je vous aider avec le stress, l'anxiété ou vos rendez-vous ?\"\n\n");

        sb.append("📋 **CONNAISSANCES SUR L'APPLICATION UNIMIND :**\n");
        sb.append("- Unimind permet de prendre rendez-vous avec des psychologues\n");
        sb.append("- Les consultations peuvent être en présentiel ou en ligne (visioconférence)\n");
        sb.append("- Les étudiants peuvent consulter leurs rendez-vous et motifs\n");
        sb.append("- Les psychologues gèrent leurs disponibilités (calendrier)\n");
        sb.append("- Un assistant vocal permet de dicter le motif de consultation\n");
        sb.append("- L'application propose des statistiques sur l'activité\n\n");

        sb.append("✅ **RÈGLES DE RÉPONSE :**\n");
        sb.append("- Réponds UNIQUEMENT en français\n");
        sb.append("- Sois concis : 2 à 4 phrases maximum (sauf si l'utilisateur demande plus de détails)\n");
        sb.append("- Utilise un ton chaleureux, bienveillant et rassurant\n");
        sb.append("- Propose des actions concrètes (exercices de respiration, conseils pratiques)\n\n");

        if (historique != null && !historique.isBlank() && historique.length() < 1000) {
            sb.append("**HISTORIQUE DE LA CONVERSATION :**\n");
            sb.append(historique).append("\n\n");
        }

        sb.append("**MESSAGE DE L'ÉTUDIANT :** ").append(message).append("\n\n");
        sb.append("**TA RÉPONSE (assistant Unimind) :** ");

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