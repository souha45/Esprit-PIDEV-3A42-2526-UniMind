package org.example.services.evenement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class EventAiGeneratorService {

    private final String geminiApiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EventAiGeneratorService() {
        this.geminiApiKey = loadApiKey();
        this.model = loadModel();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("event-config.properties")) {
            if (is != null) {
                props.load(is);
                String key = props.getProperty("gemini.api.key", "").trim();
                if (key.isEmpty() || key.equals("VOTRE_CLE_API_ICI")) {
                    throw new RuntimeException("GEMINI_API_KEY is not configured in event-config.properties");
                }
                return key;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load event-config.properties", e);
        }
        throw new RuntimeException("event-config.properties not found or GEMINI_API_KEY not configured");
    }

    private String loadModel() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("event-config.properties")) {
            if (is != null) {
                props.load(is);
                String model = props.getProperty("gemini.model", "gemini-2.5-flash").trim();
                if (model.isEmpty()) {
                    return "gemini-2.5-flash";
                }
                return model;
            }
        } catch (IOException e) {
            return "gemini-2.5-flash";
        }
        return "gemini-2.5-flash";
    }

    /**
     * Génère des suggestions d'événement via l'API Gemini
     * @param type Type d'événement
     * @param targetAudience Public cible
     * @param topic Sujet de l'événement
     * @param constraints Contraintes spécifiques
     * @param language Langue (par défaut 'fr')
     * @return Map contenant titles[], description, agenda[], checklist[]
     */
    public Map<String, Object> generate(String type, String targetAudience, String topic, String constraints, String language) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new RuntimeException("Missing field 'topic'");
        }

        String modelName = model;
        if (!modelName.startsWith("models/")) {
            modelName = "models/" + modelName;
        }

        // Construire le prompt
        Map<String, Object> user = new HashMap<>();
        user.put("language", language != null ? language : "fr");
        user.put("type", type != null ? type : "");
        user.put("target_audience", targetAudience != null ? targetAudience : "");
        user.put("topic", topic);
        user.put("constraints", constraints != null ? constraints : "");
        user.put("task", "Propose 5 titres, 1 description structurée, 6 points d'agenda, 8 éléments de checklist logistique, et un prompt court (en anglais) décrivant une belle image réaliste et moderne illustrant cet événement pour un générateur d'images.");
        user.put("output_schema", Map.of(
            "titles", List.of("..."),
            "description", "...",
            "agenda", List.of("..."),
            "checklist", List.of("..."),
            "image_prompt", "..."
        ));

        String prompt;
        try {
            prompt = "Tu es un assistant qui aide à rédiger des événements universitaires. " +
                "Réponds STRICTEMENT en JSON valide et rien d'autre. " +
                "Voici la demande (JSON):\n" + objectMapper.writeValueAsString(user);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize request", e);
        }

        // Construire la requête HTTP
        String url = "https://generativelanguage.googleapis.com/v1beta/" + modelName + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", prompt))
        )));
        requestBody.put("generationConfig", Map.of("temperature", 0.7));

        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String rawResponse = response.body();

            if (status < 200 || status >= 300) {
                throw new RuntimeException("Gemini request failed (" + status + "): " + rawResponse);
            }

            JsonNode payload = objectMapper.readTree(rawResponse);
            String content = payload.get("candidates").get(0)
                .get("content").get("parts").get(0)
                .get("text").asText();

            // Parser le JSON de la réponse
            String raw = content.trim();
            JsonNode data;
            try {
                data = objectMapper.readTree(raw);
            } catch (Exception e) {
                // Essayer d'extraire le JSON s'il est entouré de texte
                int start = raw.indexOf('{');
                int end = raw.lastIndexOf('}');
                if (start != -1 && end != -1 && end > start) {
                    String maybeJson = raw.substring(start, end + 1);
                    data = objectMapper.readTree(maybeJson);
                } else {
                    throw new RuntimeException("Invalid JSON returned by Gemini");
                }
            }

            // Extraire les données
            List<String> titles = new ArrayList<>();
            if (data.has("titles") && data.get("titles").isArray()) {
                for (JsonNode title : data.get("titles")) {
                    if (title.isTextual()) {
                        titles.add(title.asText());
                    }
                }
            }

            String description = data.has("description") && data.get("description").isTextual()
                ? data.get("description").asText()
                : "";

            List<String> agenda = new ArrayList<>();
            if (data.has("agenda") && data.get("agenda").isArray()) {
                for (JsonNode item : data.get("agenda")) {
                    if (item.isTextual()) {
                        agenda.add(item.asText());
                    }
                }
            }

            List<String> checklist = new ArrayList<>();
            if (data.has("checklist") && data.get("checklist").isArray()) {
                for (JsonNode item : data.get("checklist")) {
                    if (item.isTextual()) {
                        checklist.add(item.asText());
                    }
                }
            }

            String imagePrompt = data.has("image_prompt") && data.get("image_prompt").isTextual()
                ? data.get("image_prompt").asText()
                : "";

            String imageFileName = "";
            if (!imagePrompt.isEmpty()) {
                try {
                    String encodedPrompt = java.net.URLEncoder.encode(imagePrompt, java.nio.charset.StandardCharsets.UTF_8);
                    java.nio.file.Path destinationDir = java.nio.file.Paths.get("D:\\xampp\\htdocs\\uploadsEvent\\evenements\\");
                    
                    // S'assurer que le dossier existe
                    if (!java.nio.file.Files.exists(destinationDir)) {
                        java.nio.file.Files.createDirectories(destinationDir);
                    }
                    
                    long currentTime = System.currentTimeMillis();
                    
                    // --- Génération d'une seule image ---
                    String imageUrl = "https://image.pollinations.ai/prompt/" + encodedPrompt + "?width=800&height=600&nologo=true&seed=" + currentTime;
                    String fileName = "ai_event_" + currentTime + ".jpg";
                    java.nio.file.Path destination = destinationDir.resolve(fileName);
                    
                    HttpRequest imgRequest = HttpRequest.newBuilder().uri(URI.create(imageUrl)).GET().build();
                    HttpResponse<java.nio.file.Path> imgResponse = httpClient.send(imgRequest, HttpResponse.BodyHandlers.ofFile(destination));
                    if (imgResponse.statusCode() >= 200 && imgResponse.statusCode() < 300) {
                        imageFileName = fileName;
                    }
                    
                    System.out.println("1 Image IA générée avec succès.");
                } catch (Exception ex) {
                    System.err.println("Erreur lors de la génération de l'image IA: " + ex.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("titles", titles.stream().filter(s -> s != null && !s.trim().isEmpty()).limit(5).toList());
            result.put("description", description != null ? description.trim() : "");
            result.put("agenda", agenda.stream().filter(s -> s != null && !s.trim().isEmpty()).limit(10).toList());
            result.put("checklist", checklist.stream().filter(s -> s != null && !s.trim().isEmpty()).limit(15).toList());
            result.put("image_file", imageFileName);

            return result;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Version simplifiée avec seulement type et topic
     */
    public Map<String, Object> generate(String type, String topic) {
        return generate(type, "", topic, "", "fr");
    }
}
