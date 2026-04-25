package org.example.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LocalCallbackServer {
    private static CompletableFuture<String> authCodeFuture = new CompletableFuture<>();
    private static HttpServer server;
    private static boolean isRunning = false;

    public static synchronized void startServer() throws IOException {
        // Vérification renforcée
        if (isRunning) {
            System.out.println("ℹ️ Serveur déjà en cours d'exécution");
            return;
        }

        try {
            // Essayer de créer le serveur
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/callback", new CallbackHandler());
            server.createContext("/activate", new ActivationHandler());
            server.createContext("/reset-password", new ResetPasswordHandler());
            server.createContext("/captcha-callback", new CaptchaServer.CaptchaCallbackHandler());
            server.createContext("/captcha",          new HCaptchaPageHandler());
            server.createContext("/hcaptcha-callback", new HCaptchaCallbackHandler());
            server.setExecutor(null);
            server.start();
            isRunning = true;
            System.out.println("✅ Serveur OAuth démarré sur http://localhost:8080");
        } catch (IOException e) {
            if (e.getMessage().contains("Address already in use")) {
                // Le serveur tourne déjà, on marque isRunning=true et on continue
                System.out.println("ℹ️ Port 8080 déjà utilisé - le serveur tourne déjà");
                isRunning = true;
            } else {
                throw e;
            }
        }
    }

    public static synchronized void stopServer() {
        if (server != null && isRunning) {
            server.stop(0);
            server = null;
            isRunning = false;
            System.out.println("✅ Serveur OAuth arrêté");
        }
    }

    public static CompletableFuture<String> waitForAuthCode() {
        return authCodeFuture;
    }

    // ══════════════════════════════════════════════════════════════════
    //  HANDLERS (inchangés)
    // ══════════════════════════════════════════════════════════════════

    static class CallbackHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            if (params.containsKey("code")) {
                authCodeFuture.complete(params.get("code"));
                sendResponse(exchange, getSuccessHtml());
            } else if (params.containsKey("error")) {
                authCodeFuture.completeExceptionally(new Exception(params.get("error")));
                sendResponse(exchange, getErrorHtml(params.get("error")));
            }
        }
    }
    static class CaptchaCallbackHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("token=")) {
                String token = query.split("token=")[1];
                System.out.println("📝 Token CAPTCHA reçu par le serveur: " + token.substring(0, Math.min(20, token.length())) + "...");
            }

            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    static class ActivationHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            if (params.containsKey("userId") && params.containsKey("token")) {
                int userId = Integer.parseInt(params.get("userId"));
                String token = params.get("token");

                try (var conn = MyDataBase_Unimind.getInstance().getConnection()) {
                    var ps = conn.prepareStatement("UPDATE user SET is_verified=1, activation_token=NULL WHERE user_id=? AND activation_token=?");
                    ps.setInt(1, userId);
                    ps.setString(2, token);
                    if (ps.executeUpdate() > 0) {
                        sendResponse(exchange, getActivationSuccessHtml());
                    } else {
                        sendResponse(exchange, getActivationErrorHtml("Token invalide ou compte déjà activé"));
                    }
                } catch (Exception e) {
                    sendResponse(exchange, getActivationErrorHtml(e.getMessage()));
                }
            } else {
                sendResponse(exchange, getActivationErrorHtml("Paramètres manquants"));
            }
        }
    }

    static class ResetPasswordHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();

            System.out.println("ResetPasswordHandler - Query: " + query);

            if (query != null && query.contains("token=")) {
                String token = extractTokenFromQuery(query);
                System.out.println("Token extrait: " + token);

                String html = getOpenAppHtml(token);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html.getBytes());
                }
            } else {
                sendResponse(exchange, getErrorHtml("Token manquant"));
            }
        }

        private String extractTokenFromQuery(String query) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
            return null;
        }

        private String getOpenAppHtml(String token) {
            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>UniMind - Réinitialisation</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 20px;
                        padding: 40px;
                        max-width: 450px;
                        width: 100%;
                        text-align: center;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    }
                    .loader {
                        display: inline-block;
                        width: 50px;
                        height: 50px;
                        border: 3px solid #f3f3f3;
                        border-top: 3px solid #4F46E5;
                        border-radius: 50%;
                        animation: spin 1s linear infinite;
                        margin: 20px auto;
                    }
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                    h1 { color: #4F46E5; margin-bottom: 10px; }
                    p { color: #6B7280; }
                    .btn-retry {
                        background: #4F46E5;
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        border-radius: 8px;
                        cursor: pointer;
                        margin-top: 20px;
                    }
                    .token-info {
                        background: #F3F4F6;
                        padding: 10px;
                        border-radius: 8px;
                        font-family: monospace;
                        word-break: break-all;
                        margin: 15px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🔐 UniMind</h1>
                    <div class="loader"></div>
                    <p>Ouverture de l'application...</p>
                    <p style="font-size: 12px;">Si l'application ne s'ouvre pas automatiquement :</p>
                    <div class="token-info">
                        <strong>Code :</strong> <span id="token">%s</span>
                    </div>
                    <button class="btn-retry" onclick="retryOpen()">🔄 Réessayer d'ouvrir</button>
                    <button class="btn-retry" onclick="copyToken()" style="background: #059669; margin-left: 10px;">📋 Copier le code</button>
                </div>
                <script>
                    function openApp() {
                        const token = document.getElementById('token').innerText;
                        window.location.href = 'unimind://reset-password?token=' + encodeURIComponent(token);
                    }
                    function retryOpen() {
                        openApp();
                        setTimeout(() => {
                            document.querySelector('.loader').style.display = 'none';
                        }, 1000);
                    }
                    function copyToken() {
                        const token = document.getElementById('token').innerText;
                        navigator.clipboard.writeText(token).then(() => {
                            alert('Code copié ! Collez-le dans l\\'application UniMind.');
                        });
                    }
                    setTimeout(openApp, 1000);
                </script>
            </body>
            </html>
        """.formatted(token);
        }
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static String getSuccessHtml() {
        return "<html><head><style>" +
                "body{font-family:Segoe UI;display:flex;justify-content:center;align-items:center;height:100vh;background:linear-gradient(135deg,#667eea,#764ba2);margin:0}" +
                ".container{text-align:center;background:white;padding:40px;border-radius:20px}" +
                ".success{color:#059669;font-size:64px}" +
                "h2{color:#4F46E5}" +
                "</style></head>" +
                "<body><div class='container'><div class='success'>✓</div><h2>Authentification réussie !</h2><p>Vous pouvez fermer cette fenêtre.</p></div>" +
                "<script>setTimeout(()=>window.close(),2000);</script></body></html>";
    }

    private static String getErrorHtml(String error) {
        return "<html><head><style>" +
                "body{font-family:Segoe UI;display:flex;justify-content:center;align-items:center;height:100vh;background:linear-gradient(135deg,#667eea,#764ba2);margin:0}" +
                ".container{text-align:center;background:white;padding:40px;border-radius:20px}" +
                ".error{color:#DC2626;font-size:64px}" +
                "</style></head>" +
                "<body><div class='container'><div class='error'>✗</div><h2>Erreur: " + error + "</h2></div></body></html>";
    }

    private static String getActivationSuccessHtml() {
        return "<html><head><style>" +
                "body{font-family:Segoe UI;display:flex;justify-content:center;align-items:center;height:100vh;background:linear-gradient(135deg,#667eea,#764ba2);margin:0}" +
                ".container{text-align:center;background:white;padding:40px;border-radius:20px}" +
                ".success{color:#059669;font-size:64px}" +
                "</style></head>" +
                "<body><div class='container'><div class='success'>✓</div><h2>Compte activé !</h2><p>Votre compte a été activé avec succès.</p><p>Un administrateur va valider votre inscription.</p><p>Vous recevrez un email de confirmation.</p></div></body></html>";
    }

    private static String getActivationErrorHtml(String error) {
        return "<html><head><style>" +
                "body{font-family:Segoe UI;display:flex;justify-content:center;align-items:center;height:100vh;background:linear-gradient(135deg,#667eea,#764ba2);margin:0}" +
                ".container{text-align:center;background:white;padding:40px;border-radius:20px}" +
                ".error{color:#DC2626;font-size:64px}" +
                "</style></head>" +
                "<body><div class='container'><div class='error'>✗</div><h2>Erreur d'activation</h2><p>" + error + "</p></div></body></html>";
    }
    // ── Token hCaptcha reçu ───────────────────────────────────────────────
    public static volatile String lastHCaptchaToken = "";

    // ── Page hCaptcha servie depuis localhost:8080 ────────────────────────
    static class HCaptchaPageHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
                <style>
                  html, body {
                                margin: 0; padding: 4px;
                                display: flex; flex-direction: column;
                                justify-content: center; align-items: center;
                                min-height: 110px; background: #F3F0FF;
                                font-family: 'Segoe UI', sans-serif;
                              }
                  #status { margin-top: 8px; font-size: 12px; color: #6B7280; }
                  #status.ok { color: #059669; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="h-captcha"
                     data-sitekey="c086af9a-5268-45d6-b594-7c08f1080242"
                     data-callback="onCaptchaSuccess"
                     data-expired-callback="onExpired">
                </div>
                <div id="status">○ En attente de validation...</div>
                <script>
                    function onCaptchaSuccess(token) {
                        document.getElementById('status').textContent = '✅ Validation réussie !';
                        document.getElementById('status').className = 'ok';
                        fetch('http://localhost:8080/hcaptcha-callback?token=' + encodeURIComponent(token))
                          .catch(function() {});
                    }
                    function onExpired() {
                        document.getElementById('status').textContent = '⚠ Expiré, recochez';
                        document.getElementById('status').className = '';
                    }
                </script>
            </body>
            </html>
        """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    // ── Callback hCaptcha — stocke le token ──────────────────────────────
    static class HCaptchaCallbackHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        try {
                            lastHCaptchaToken = URLDecoder.decode(
                                    param.substring(6), StandardCharsets.UTF_8);
                            System.out.println("✓ hCaptcha token reçu, longueur: "
                                    + lastHCaptchaToken.length());
                        } catch (Exception e) { e.printStackTrace(); }
                        break;
                    }
                }
            }
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] bytes = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }
}