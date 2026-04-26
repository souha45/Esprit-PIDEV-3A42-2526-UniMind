package org.example.services;

import javafx.application.Platform;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class VoiceAssistantService {

    // ── Résolution automatique du chemin du modèle ────────────────
    private String modelPath = resolveModelPath();

    /**
     * Cherche le dossier du modèle dans cet ordre :
     *  1. Classpath (src/main/resources/vosk-model-fr → dans le JAR/target)
     *  2. Répertoire courant / sous-dossiers courants
     *  3. Répertoire du JAR en cours d'exécution
     */
    private static String resolveModelPath() {
        String[] candidates = {
                "vosk-model-fr",
                "vosk-model-small-fr-0.22",
                "src/main/resources/vosk-model-fr",
                "src/main/resources/vosk-model-small-fr-0.22",
        };

        // 1. Classpath — fonctionne dans IntelliJ et avec Maven exec:java
        try {
            URL url = VoiceAssistantService.class.getResource("/vosk-model-fr");
            if (url != null) {
                String path = Paths.get(url.toURI()).toString();
                if (new File(path).isDirectory()) return path;
            }
        } catch (Exception ignored) {}

        // 2. Chercher relative au répertoire courant
        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.isDirectory()) return f.getAbsolutePath();
        }

        // 3. Chercher relative au JAR / classe en cours d'exécution
        try {
            File jarDir = new File(
                    VoiceAssistantService.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()).getParentFile();

            for (String candidate : candidates) {
                File f = new File(jarDir, candidate);
                if (f.isDirectory()) return f.getAbsolutePath();
            }
            // Remonter d'un niveau (cas Maven : target/classes → projet)
            File projectDir = jarDir.getParentFile();
            if (projectDir != null) {
                for (String candidate : candidates) {
                    File f = new File(projectDir, candidate);
                    if (f.isDirectory()) return f.getAbsolutePath();
                }
            }
        } catch (Exception ignored) {}

        // 4. Fallback — chemin tel quel (échouera avec un message clair)
        return "vosk-model-fr";
    }

    // ── Callbacks ────────────────────────────────────────────────
    private Consumer<NavigationCommand> onCommand;
    private Consumer<String>            onStatus;
    private Consumer<String>            onTranscript;

    // ── État ─────────────────────────────────────────────────────
    private volatile boolean listening = false;
    private Thread listeningThread;

    // ── Enum commandes ────────────────────────────────────────────
    public enum NavigationCommand {
        DASHBOARD, RENDEZ_VOUS, CONSULTATIONS, TRAITEMENTS,
        QUESTIONNAIRES, MES_REPONSES, SEANCES, FAVORIS_SEANCES,
        EVENEMENTS, MES_PARTICIPATIONS, FAVORIS_EVENEMENTS,
        PROFIL, DECONNEXION, INCONNU
    }

    private static final Map<String, NavigationCommand> KEYWORD_MAP = new HashMap<>();
    static {
        KEYWORD_MAP.put("dashboard",          NavigationCommand.DASHBOARD);
        KEYWORD_MAP.put("tableau de bord",    NavigationCommand.DASHBOARD);
        KEYWORD_MAP.put("accueil",            NavigationCommand.DASHBOARD);
        KEYWORD_MAP.put("page principale",    NavigationCommand.DASHBOARD);

        KEYWORD_MAP.put("rendez-vous",        NavigationCommand.RENDEZ_VOUS);
        KEYWORD_MAP.put("rendez vous",        NavigationCommand.RENDEZ_VOUS);
        KEYWORD_MAP.put("mes rendez",         NavigationCommand.RENDEZ_VOUS);

        KEYWORD_MAP.put("consultation",       NavigationCommand.CONSULTATIONS);
        KEYWORD_MAP.put("consultations",      NavigationCommand.CONSULTATIONS);

        KEYWORD_MAP.put("traitement",         NavigationCommand.TRAITEMENTS);
        KEYWORD_MAP.put("traitements",        NavigationCommand.TRAITEMENTS);
        KEYWORD_MAP.put("medicament",         NavigationCommand.TRAITEMENTS);

        KEYWORD_MAP.put("questionnaire",      NavigationCommand.QUESTIONNAIRES);
        KEYWORD_MAP.put("questionnaires",     NavigationCommand.QUESTIONNAIRES);

        KEYWORD_MAP.put("reponses",           NavigationCommand.MES_REPONSES);
        KEYWORD_MAP.put("mes reponses",       NavigationCommand.MES_REPONSES);
        KEYWORD_MAP.put("réponses",           NavigationCommand.MES_REPONSES);

        KEYWORD_MAP.put("seances",            NavigationCommand.SEANCES);
        KEYWORD_MAP.put("séances",            NavigationCommand.SEANCES);
        KEYWORD_MAP.put("meditation",         NavigationCommand.SEANCES);
        KEYWORD_MAP.put("méditation",         NavigationCommand.SEANCES);

        KEYWORD_MAP.put("favoris",            NavigationCommand.FAVORIS_SEANCES);
        KEYWORD_MAP.put("mes favoris",        NavigationCommand.FAVORIS_SEANCES);
        KEYWORD_MAP.put("favori",             NavigationCommand.FAVORIS_SEANCES);

        KEYWORD_MAP.put("evenement",          NavigationCommand.EVENEMENTS);
        KEYWORD_MAP.put("evenements",         NavigationCommand.EVENEMENTS);
        KEYWORD_MAP.put("événement",          NavigationCommand.EVENEMENTS);
        KEYWORD_MAP.put("événements",         NavigationCommand.EVENEMENTS);

        KEYWORD_MAP.put("participation",      NavigationCommand.MES_PARTICIPATIONS);
        KEYWORD_MAP.put("participations",     NavigationCommand.MES_PARTICIPATIONS);
        KEYWORD_MAP.put("mes participations", NavigationCommand.MES_PARTICIPATIONS);

        KEYWORD_MAP.put("favoris evenements", NavigationCommand.FAVORIS_EVENEMENTS);

        KEYWORD_MAP.put("profil",             NavigationCommand.PROFIL);
        KEYWORD_MAP.put("mon profil",         NavigationCommand.PROFIL);

        KEYWORD_MAP.put("deconnecter",        NavigationCommand.DECONNEXION);
        KEYWORD_MAP.put("déconnecter",        NavigationCommand.DECONNEXION);
        KEYWORD_MAP.put("deconnexion",        NavigationCommand.DECONNEXION);
        KEYWORD_MAP.put("déconnexion",        NavigationCommand.DECONNEXION);
        KEYWORD_MAP.put("quitter",            NavigationCommand.DECONNEXION);
        KEYWORD_MAP.put("sortir",             NavigationCommand.DECONNEXION);
    }

    // ── Setters ───────────────────────────────────────────────────
    public void onCommandRecognized(Consumer<NavigationCommand> cb) { this.onCommand   = cb; }
    public void onStatusChanged    (Consumer<String> cb)            { this.onStatus    = cb; }
    public void onTranscriptChanged(Consumer<String> cb)            { this.onTranscript = cb; }
    public void setModelPath(String path)                           { this.modelPath   = path; }
    public boolean isListening()                                    { return listening; }

    // ── Démarrage ─────────────────────────────────────────────────
    public void startListening() {
        if (listening) return;
        listening = true;
        listeningThread = new Thread(this::listenLoop, "VoiceAssistant-Thread");
        listeningThread.setDaemon(true);
        listeningThread.start();
    }

    // ── Arrêt ─────────────────────────────────────────────────────
    public void stopListening() {
        listening = false;
        if (listeningThread != null) listeningThread.interrupt();
        postStatus("🎤 Assistant vocal arrêté.");
    }

    // ── Boucle d'écoute ───────────────────────────────────────────
    private void listenLoop() {
        // Vérification préalable du dossier modèle
        File modelDir = new File(modelPath);
        if (!modelDir.isDirectory()) {
            postStatus("❌ Modèle introuvable.\nChemin essayé : " + modelDir.getAbsolutePath()
                    + "\n→ Décompressez vosk-model-small-fr-0.22 dans :\n"
                    + new File("src/main/resources/vosk-model-fr").getAbsolutePath());
            listening = false;
            return;
        }

        postStatus("⏳ Chargement du modèle : " + modelDir.getAbsolutePath());

        try (Model model = new Model(modelDir.getAbsolutePath())) {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                postStatus("❌ Microphone non supporté.");
                listening = false;
                return;
            }

            try (TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                 Recognizer rec   = new Recognizer(model, 16000)) {

                mic.open(format);
                mic.start();
                postStatus("🎤 En écoute... Parlez !");

                byte[] buffer = new byte[4096];

                while (listening && !Thread.currentThread().isInterrupted()) {
                    int bytesRead = mic.read(buffer, 0, buffer.length);
                    if (bytesRead <= 0) continue;

                    if (rec.acceptWaveForm(buffer, bytesRead)) {
                        String text = extractText(rec.getResult());
                        if (!text.isBlank()) {
                            postTranscript(text);
                            NavigationCommand cmd = parseCommand(text);
                            if (cmd != NavigationCommand.INCONNU) {
                                postStatus("✅ Commande : " + friendlyName(cmd));
                                if (onCommand != null)
                                    Platform.runLater(() -> onCommand.accept(cmd));
                            } else {
                                postStatus("❓ \"" + text + "\"");
                            }
                        }
                    } else {
                        String partial = extractText(rec.getPartialResult());
                        if (!partial.isBlank()) postTranscript("... " + partial);
                    }
                }
                mic.stop();
            }
        } catch (Exception e) {
            postStatus("❌ " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            System.err.println("[VoiceAssistant] " + e.getMessage());
        } finally {
            listening = false;
        }
    }

    // ── Parsing ────────────────────────────────────────────────────
    private NavigationCommand parseCommand(String text) {
        String lower = text.toLowerCase().trim();
        NavigationCommand best = NavigationCommand.INCONNU;
        int bestLen = 0;
        for (Map.Entry<String, NavigationCommand> entry : KEYWORD_MAP.entrySet()) {
            String kw = entry.getKey();
            if (lower.contains(kw) && kw.length() > bestLen) {
                best = entry.getValue(); bestLen = kw.length();
            }
        }
        return best;
    }

    private String extractText(String json) {
        try {
            for (String key : new String[]{"\"text\"", "\"partial\""}) {
                int idx = json.indexOf(key);
                if (idx == -1) continue;
                int s = json.indexOf('"', idx + key.length() + 1);
                if (s == -1) continue;
                int e = json.indexOf('"', s + 1);
                if (e == -1) continue;
                return json.substring(s + 1, e).trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String friendlyName(NavigationCommand cmd) {
        return switch (cmd) {
            case DASHBOARD          -> "Dashboard";
            case RENDEZ_VOUS        -> "Mes rendez-vous";
            case CONSULTATIONS      -> "Consultations";
            case TRAITEMENTS        -> "Traitements";
            case QUESTIONNAIRES     -> "Questionnaires";
            case MES_REPONSES       -> "Mes réponses";
            case SEANCES            -> "Séances méditation";
            case FAVORIS_SEANCES    -> "Mes favoris séances";
            case EVENEMENTS         -> "Événements";
            case MES_PARTICIPATIONS -> "Mes participations";
            case FAVORIS_EVENEMENTS -> "Favoris événements";
            case PROFIL             -> "Mon profil";
            case DECONNEXION        -> "Déconnexion";
            default                 -> "Inconnu";
        };
    }

    private void postStatus(String msg) {
        System.out.println("[VoiceAssistant] " + msg);
        if (onStatus != null) Platform.runLater(() -> onStatus.accept(msg));
    }
    private void postTranscript(String text) {
        if (onTranscript != null) Platform.runLater(() -> onTranscript.accept(text));
    }
}