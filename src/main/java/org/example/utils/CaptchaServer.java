package org.example.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Serveur HTTP dédié au CAPTCHA sur le port 8081.
 * Séparé de LocalCallbackServer (port 8080) pour éviter les conflits.
 */
public class CaptchaServer {

    private static HttpServer server;
    private static boolean    isRunning = false;

    // Token reCAPTCHA reçu après validation par l'utilisateur
    public static volatile String lastCaptchaToken = "";

    // ── Démarrage ─────────────────────────────────────────────────────
    public static synchronized void start() {
        if (isRunning) return;

        try {
            server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/captcha",          new CaptchaPageHandler());
            server.createContext("/captcha-callback", new CaptchaCallbackHandler());
            server.setExecutor(null);
            server.start();
            isRunning = true;
            System.out.println("✓ CaptchaServer démarré sur http://localhost:8081");
        } catch (IOException e) {
            System.err.println("⚠ CaptchaServer: impossible de démarrer sur 8081 — " + e.getMessage());
        }
    }

    // ── Arrêt ─────────────────────────────────────────────────────────
    public static synchronized void stop() {
        if (server != null && isRunning) {
            server.stop(0);
            server    = null;
            isRunning = false;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE HTML — widget reCAPTCHA servi depuis localhost:8081
    // ══════════════════════════════════════════════════════════════════
    static class CaptchaPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
                    <style>
                      body {
                        margin: 0; padding: 0;
                        display: flex; justify-content: center; align-items: center;
                        min-height: 100px; background: #F3F0FF;
                      }
                    </style>
                </head>
                <body>
                    <div class="g-recaptcha"
                         data-sitekey="6Leh98csAAAAAIuHyrfJCSpwzgjsQGzjFmI-pVrm"
                         data-callback="onCaptchaSuccess">
                    </div>
                    <script>
                        function onCaptchaSuccess(response) {
                            // Envoyer le token au serveur local via une requête fetch
                            fetch('http://localhost:8081/captcha-callback?captcha=' + encodeURIComponent(response))
                              .catch(function() {
                                // Fallback : navigation directe
                                window.location.href = 'http://localhost:8081/captcha-callback?captcha=' + encodeURIComponent(response);
                              });
                        }
                    </script>
                </body>
                </html>
            """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  CALLBACK — stocke le token reCAPTCHA
    // ══════════════════════════════════════════════════════════════════
    static class CaptchaCallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("captcha=")) {
                // Extraire le token (peut être précédé d'autres params)
                for (String param : query.split("&")) {
                    if (param.startsWith("captcha=")) {
                        String token = URLDecoder.decode(
                                param.substring("captcha=".length()), StandardCharsets.UTF_8);
                        lastCaptchaToken = token;
                        System.out.println("✓ CAPTCHA token reçu (longueur: " + token.length() + ")");
                        break;
                    }
                }
            }

            // Réponse vide — le WebView restera sur cette page blanche
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] bytes = "<html><body></body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}