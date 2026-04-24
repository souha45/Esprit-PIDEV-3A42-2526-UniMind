package org.example.services;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class ReconnaissanceVocaleService {

    private Model      model;
    private boolean    isModelLoaded = false;

    private volatile boolean isRecording   = false;
    private volatile boolean stopRequested = false;

    private TargetDataLine  microphone;
    private Thread          recordingThread;
    private StringBuilder   resultBuilder;

    // ────────────────────────────────────────────────────────────────
    //  CONSTRUCTEUR
    // ────────────────────────────────────────────────────────────────
    public ReconnaissanceVocaleService() {
        String[] chemins = {
                "src/main/resources/vosk-model/vosk-model-small-fr-0.22",
                "vosk-model/vosk-model-small-fr-0.22",
                "src/main/resources/vosk-model-small-fr-0.22"
        };

        String modelPath = null;
        for (String path : chemins) {
            File f = new File(path);
            if (f.exists() && f.isDirectory()) {
                modelPath = path;
                break;
            }
        }

        if (modelPath == null) {
            System.err.println("[Vosk] Modele introuvable !");
            return;
        }

        try {
            System.out.println("[Vosk] Chargement depuis : " + modelPath);
            model         = new Model(modelPath);
            isModelLoaded = true;
            System.out.println("[Vosk] Modele charge avec succes !");
        } catch (IOException e) {
            System.err.println("[Vosk] Erreur chargement : " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  DÉMARRAGE
    // ────────────────────────────────────────────────────────────────
    public void demarrerReconnaissance() {
        if (!isModelLoaded) { System.err.println("[Vosk] Modele non charge."); return; }
        if (isRecording)    { System.out.println("[Vosk] Deja en cours.");     return; }

        try {
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    16000f, 16, 1, 2, 16000f, false
            );

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[Vosk] Format audio non supporte.");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format, 32000);
            microphone.start();
            System.out.println("[Vosk] Microphone ouvert. Buffer=" + microphone.getBufferSize());

            stopRequested = false;
            isRecording   = true;
            resultBuilder = new StringBuilder();

            recordingThread = new Thread(() -> {
                Recognizer rec = null;
                try {
                    rec = new Recognizer(model, 16000.0f);
                } catch (Exception ex) {
                    System.err.println("[Vosk] Erreur Recognizer : " + ex.getMessage());
                    isRecording = false;
                    return;
                }

                // Buffer = ~0.25s d'audio à 16kHz 16-bit mono
                byte[] buffer = new byte[8192];
                System.out.println("[Vosk] Ecoute active...");

                try {
                    while (!stopRequested) {
                        int lu = microphone.read(buffer, 0, buffer.length);
                        if (lu <= 0) continue;

                        if (rec.acceptWaveForm(buffer, lu)) {
                            String texte = extraireTexte(rec.getResult(), "text");
                            if (!texte.isEmpty()) {
                                resultBuilder.append(texte).append(" ");
                                System.out.println("[Vosk] Phrase : " + texte);
                            }
                        } else {
                            String partiel = extraireTexte(rec.getPartialResult(), "partial");
                            if (!partiel.isEmpty())
                                System.out.println("[Vosk] Partiel : " + partiel);
                        }
                    }

                    // Vider le buffer restant
                    int dispo = microphone.available();
                    if (dispo > 0) {
                        byte[] reste = new byte[dispo];
                        int lu2 = microphone.read(reste, 0, reste.length);
                        if (lu2 > 0) rec.acceptWaveForm(reste, lu2);
                    }

                    // Résultat final
                    String texteFinal = extraireTexte(rec.getFinalResult(), "text");
                    if (!texteFinal.isEmpty()) {
                        resultBuilder.append(texteFinal).append(" ");
                        System.out.println("[Vosk] Final : " + texteFinal);
                    }

                } catch (Exception ex) {
                    System.err.println("[Vosk] Erreur thread : " + ex.getMessage());
                } finally {
                    rec.close();
                    isRecording = false;
                    System.out.println("[Vosk] Thread termine. Resultat='"
                            + resultBuilder.toString().trim() + "'");
                }
            }, "vosk-rec-thread");

            recordingThread.setDaemon(true);
            recordingThread.start();

        } catch (LineUnavailableException e) {
            System.err.println("[Vosk] Microphone indisponible : " + e.getMessage());
            isRecording = false;
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  ARRÊT — DOIT être appelé depuis un thread NON-UI
    // ────────────────────────────────────────────────────────────────
    public String arreterReconnaissance() {
        if (!isRecording && recordingThread == null)
            return resultBuilder != null ? resultBuilder.toString().trim() : "";

        System.out.println("[Vosk] Arret demande...");
        stopRequested = true;

        // Attendre la fin du thread (max 4s)
        if (recordingThread != null && recordingThread.isAlive()) {
            try { recordingThread.join(4000); }
            catch (InterruptedException ignored) {}
        }
        recordingThread = null;

        // Fermer le micro APRÈS la fin du thread
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        isRecording   = false;
        stopRequested = false;

        String result = resultBuilder != null ? resultBuilder.toString().trim() : "";
        System.out.println("[Vosk] Resultat : '" + result + "'");
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    //  EXTRACTION JSON  {"text":"..."} ou {"partial":"..."}
    // ────────────────────────────────────────────────────────────────
    private String extraireTexte(String json, String cle) {
        if (json == null || json.isEmpty()) return "";
        try {
            String tag = "\"" + cle + "\"";
            int idx = json.indexOf(tag);
            if (idx < 0) return "";
            // Chercher le premier " après la clé et les caractères :, espace
            int debut = json.indexOf("\"", idx + tag.length() + 1);
            if (debut < 0) return "";
            debut++;
            int fin = json.indexOf("\"", debut);
            if (fin < 0) return "";
            return json.substring(debut, fin).trim();
        } catch (Exception e) { return ""; }
    }

    public boolean isReady()   { return isModelLoaded; }
    public boolean isRunning() { return isRecording;   }

    public void fermer() {
        arreterReconnaissance();
        if (model != null) { model.close(); model = null; }
    }
}