package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour interroger l'API Freesound.
 * Docs : https://freesound.org/docs/api/
 *
 * ⚠️  Remplacez YOUR_FREESOUND_API_KEY par votre clé obtenue sur
 *     https://freesound.org/apiv2/apply/
 */
public class FreesoundService {

    private static final String API_KEY  = "6QwHiVzlkKIkLu2dubrcbbagmXjA2t3uruylXDfS";
    private static final String BASE_URL = "https://freesound.org/apiv2";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ── DTO ──────────────────────────────────────────────────────────────
    public static class AmbientSound {
        public final int    id;
        public final String name;
        public final String previewUrl;   // mp3 preview — pas besoin de OAuth
        public final String emoji;

        public AmbientSound(int id, String name, String previewUrl, String emoji) {
            this.id = id; this.name = name; this.previewUrl = previewUrl; this.emoji = emoji;
        }
    }

    // ── Catégories prédéfinies ────────────────────────────────────────────
    public record SoundCategory(String label, String emoji, String query) {}

    public static final List<SoundCategory> CATEGORIES = List.of(
            new SoundCategory("Mer",       "🌊", "ocean waves relaxing"),
            new SoundCategory("Pluie",     "🌧️", "rain relaxing sleep"),
            new SoundCategory("Forêt",     "🌿", "forest birds nature"),
            new SoundCategory("Feu",       "🔥", "fireplace crackling"),
            new SoundCategory("Rivière",   "💧", "river stream water"),
            new SoundCategory("Vent",      "🍃", "wind gentle breeze"),
            new SoundCategory("Nuit",      "🦗", "night crickets insects"),
            new SoundCategory("Spa",       "🧘", "tibetan singing bowl meditation")
    );

    // ── Recherche ─────────────────────────────────────────────────────────
    /**
     * Recherche les 5 premiers sons correspondant à la requête.
     * Retourne uniquement les sons avec preview mp3.
     */
    public List<AmbientSound> search(String query, String emoji) throws Exception {
        String url = BASE_URL + "/search/text/"
                + "?query=" + java.net.URLEncoder.encode(query, "UTF-8")
                + "&fields=id,name,previews"
                + "&filter=duration:[5+TO+120]"   // entre 5s et 2min
                + "&page_size=5"
                + "&token=" + API_KEY;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200)
            throw new RuntimeException("Freesound API error " + resp.statusCode());

        JsonNode root    = mapper.readTree(resp.body());
        JsonNode results = root.get("results");

        List<AmbientSound> sounds = new ArrayList<>();
        if (results != null) {
            for (JsonNode node : results) {
                int    id      = node.get("id").asInt();
                String name    = node.get("name").asText();
                JsonNode prev  = node.get("previews");
                if (prev == null) continue;
                // On préfère hq-mp3, sinon lq-mp3
                String previewUrl = prev.has("preview-hq-mp3")
                        ? prev.get("preview-hq-mp3").asText()
                        : prev.has("preview-lq-mp3")
                          ? prev.get("preview-lq-mp3").asText()
                          : null;
                if (previewUrl == null) continue;
                sounds.add(new AmbientSound(id, name, previewUrl, emoji));
            }
        }
        return sounds;
    }
}