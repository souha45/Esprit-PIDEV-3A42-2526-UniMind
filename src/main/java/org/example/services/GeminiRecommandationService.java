package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entities.CategorieMeditation;
import org.example.entities.SeanceMeditation;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Gemini — retourne UNIQUEMENT des séances existantes en BDD.
 *
 * Stratégie :
 *   1. On envoie à Gemini la liste complète des séances actives (id + titre + niveau + durée).
 *   2. Gemini choisit les 3 IDs les plus pertinents selon l'émotion.
 *   3. On résout ces IDs localement → objets SeanceMeditation garantis non-null.
 *   4. Fallback : si Gemini échoue ou renvoie des IDs inconnus, on fait un
 *      matching local par mots-clés (aucun appel réseau supplémentaire).
 *
 * ⚠️  Remplacez YOUR_GEMINI_API_KEY :  https://aistudio.google.com/app/apikey
 */
public class GeminiRecommandationService {

    private static final String API_KEY  = "AIzaSyDUVh0DKV5hwGvT8OLPJWxTPZtPEOuAoQ8";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── DTO ───────────────────────────────────────────────────────────────
    public static class Recommandation {
        /** Séance réelle en BDD — jamais null si le fallback fonctionne. */
        public SeanceMeditation seance;
        /** Nom de la catégorie (résolu localement). */
        public String nomCategorie;
        /** Explication courte générée par Gemini (ou par le fallback local). */
        public String pourquoi;
    }

    // ── Mapping émotion → mots-clés fallback ──────────────────────────────
    private static final Map<String, List<String>> EMOTION_KEYWORDS = Map.of(
            "Joyeux / serein",        List.of("énergi", "positiv", "visualis", "joie", "vitalit"),
            "Triste / mélancolique",  List.of("compassion", "gratitude", "acceptation", "douceur", "bienveillance"),
            "Stressé / submergé",     List.of("respir", "relaxation", "anti-stress", "calme", "détente"),
            "Énervé / frustré",       List.of("pleine conscience", "apaisement", "colère", "douceur", "nature"),
            "Déprimé / fatigué",      List.of("douceur", "repos", "récupér", "sommeil", "énergi"),
            "Neutre / calme",         List.of("découverte", "méditation", "bien-être", "équilibre"),
            "Anxieux / inquiet",      List.of("anxiét", "respir", "ancrage", "sécurit", "confiance")
    );

    // ── Point d'entrée principal ──────────────────────────────────────────
    /**
     * @param emotion    ex : "Stressé / submergé"
     * @param ressenti   texte libre optionnel
     * @param seances    toutes les séances actives en BDD
     * @param categories toutes les catégories
     * @return exactement 3 recommandations, chacune avec seance != null
     */
    public List<Recommandation> recommander(
            String emotion,
            String ressenti,
            List<SeanceMeditation> seances,
            List<CategorieMeditation> categories) {

        // Ne garder que les séances actives
        List<SeanceMeditation> actives = seances.stream()
                .filter(SeanceMeditation::isIsActive)
                .collect(Collectors.toList());

        if (actives.isEmpty()) return Collections.emptyList();

        // Construire un index id → séance pour résolution rapide
        Map<Integer, SeanceMeditation> index = new HashMap<>();
        actives.forEach(s -> index.put(s.getSeanceId(), s));

        // Construire un index categorieId → nom
        Map<Integer, String> catIndex = new HashMap<>();
        categories.forEach(c -> catIndex.put(c.getCategorieId(), c.getNom()));

        // ── Tentative Gemini ──────────────────────────────────────────
        List<Recommandation> result = tryGemini(emotion, ressenti, actives, index, catIndex);

        // ── Fallback local si Gemini échoue ou résultat insuffisant ──
        if (result.size() < 3) {
            List<Recommandation> fb = fallbackLocal(emotion, ressenti, actives, catIndex);
            // Ajouter sans doublon
            Set<Integer> dejaPris = result.stream()
                    .map(r -> r.seance.getSeanceId()).collect(Collectors.toSet());
            for (Recommandation r : fb) {
                if (!dejaPris.contains(r.seance.getSeanceId())) {
                    result.add(r);
                    dejaPris.add(r.seance.getSeanceId());
                }
                if (result.size() == 3) break;
            }
        }

        return result.subList(0, Math.min(3, result.size()));
    }

    // ── Appel Gemini ──────────────────────────────────────────────────────
    private List<Recommandation> tryGemini(
            String emotion, String ressenti,
            List<SeanceMeditation> actives,
            Map<Integer, SeanceMeditation> index,
            Map<Integer, String> catIndex) {

        try {
            String prompt = buildPrompt(emotion, ressenti, actives);
            String raw    = callGemini(prompt);
            return parseGeminiResponse(raw, index, catIndex);
        } catch (Exception e) {
            System.err.println("[Gemini] Erreur appel API : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Construction du prompt ────────────────────────────────────────────
    private String buildPrompt(String emotion, String ressenti,
                               List<SeanceMeditation> actives) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert en méditation. Un utilisateur se sent : **")
                .append(emotion).append("**.\n");

        if (ressenti != null && !ressenti.isBlank()) {
            sb.append("Son ressenti : \"").append(ressenti).append("\".\n");
            String r = ressenti.toLowerCase();
            if (r.contains("insomni") || r.contains("dormir") || r.contains("sommeil"))
                sb.append("L'utilisateur a des troubles du sommeil : inclure une séance liée au sommeil si disponible.\n");
        }

        sb.append("\nVoici TOUTES les séances disponibles (ID | Titre | Niveau | Durée min) :\n");
        for (SeanceMeditation s : actives) {
            sb.append("ID:").append(s.getSeanceId())
                    .append(" | ").append(s.getTitre())
                    .append(" | ").append(s.getNiveau() != null ? s.getNiveau().name() : "?")
                    .append(" | ").append(s.getDuree() / 60).append(" min\n");
        }

        sb.append("""

RÈGLE ABSOLUE : tu dois choisir EXACTEMENT 3 séances parmi la liste ci-dessus.
NE PAS inventer de nouvelles séances. Utiliser uniquement les IDs fournis.
Réponds UNIQUEMENT en JSON valide, sans markdown, sans backticks.
Format :
[
  {"seanceId": <id_exact>, "pourquoi": "explication courte en français"},
  {"seanceId": <id_exact>, "pourquoi": "..."},
  {"seanceId": <id_exact>, "pourquoi": "..."}
]
""");
        return sb.toString();
    }

    // ── Appel HTTP ────────────────────────────────────────────────────────
    private String callGemini(String prompt) throws Exception {
        String safe = prompt
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");

        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + safe + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.4,\"maxOutputTokens\":400}}";

        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();

        if (status != 200)
            throw new RuntimeException("Gemini API error " + status + ": " + response);

        JsonNode root = mapper.readTree(response);
        return root.at("/candidates/0/content/parts/0/text").asText();
    }

    // ── Parsing de la réponse Gemini ──────────────────────────────────────
    private List<Recommandation> parseGeminiResponse(
            String jsonText,
            Map<Integer, SeanceMeditation> index,
            Map<Integer, String> catIndex) {

        List<Recommandation> result = new ArrayList<>();
        try {
            String clean = jsonText.trim();
            if (clean.startsWith("```"))
                clean = clean.replaceAll("```[a-z]*\\n?", "").replaceAll("```", "").trim();

            JsonNode arr = mapper.readTree(clean);
            for (JsonNode node : arr) {
                int id = node.path("seanceId").asInt(-1);
                SeanceMeditation seance = index.get(id);
                if (seance == null) continue;   // ID inconnu → ignorer

                Recommandation r = new Recommandation();
                r.seance      = seance;
                r.pourquoi    = node.path("pourquoi").asText("Recommandé pour vous.");
                r.nomCategorie = catIndex.getOrDefault(seance.getCategorieId(), "Méditation");
                result.add(r);
                if (result.size() == 3) break;
            }
        } catch (Exception e) {
            System.err.println("[Gemini] Erreur parsing : " + e.getMessage());
        }
        return result;
    }

    // ── Fallback local par mots-clés ──────────────────────────────────────
    /**
     * Score chaque séance selon les mots-clés de l'émotion et le ressenti textuel.
     * Retourne les 3 meilleures (ou moins s'il n'y a pas 3 séances).
     */
    private List<Recommandation> fallbackLocal(
            String emotion, String ressenti,
            List<SeanceMeditation> actives,
            Map<Integer, String> catIndex) {

        List<String> keywords = new ArrayList<>(
                EMOTION_KEYWORDS.getOrDefault(emotion, List.of("méditation", "calme")));

        // Ajouter des mots du ressenti libre
        if (ressenti != null && !ressenti.isBlank()) {
            Arrays.stream(ressenti.toLowerCase().split("\\s+"))
                    .filter(w -> w.length() > 3)
                    .forEach(keywords::add);
        }

        // Scorer chaque séance
        Map<SeanceMeditation, Integer> scores = new LinkedHashMap<>();
        for (SeanceMeditation s : actives) {
            String text = ((s.getTitre() != null ? s.getTitre() : "") + " "
                    + (s.getDescription() != null ? s.getDescription() : "")).toLowerCase();
            int score = 0;
            for (String kw : keywords)
                if (text.contains(kw.toLowerCase())) score++;
            scores.put(s, score);
        }

        // Trier par score décroissant, prendre les 3 premiers
        return scores.entrySet().stream()
                .sorted(Map.Entry.<SeanceMeditation, Integer>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    Recommandation r = new Recommandation();
                    r.seance      = e.getKey();
                    r.nomCategorie = catIndex.getOrDefault(e.getKey().getCategorieId(), "Méditation");
                    r.pourquoi    = "Sélectionné pour vous aider avec : " + emotion.toLowerCase() + ".";
                    return r;
                })
                .collect(Collectors.toList());
    }
}