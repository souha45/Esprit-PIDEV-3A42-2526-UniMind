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
            server.createContext("/do-reset-password", new DoResetPasswordHandler());
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
    static class DoResetPasswordHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String token = null;
            String password = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        token = URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
                    } else if (param.startsWith("password=")) {
                        password = URLDecoder.decode(param.substring(9), StandardCharsets.UTF_8);
                    }
                }
            }

            String response;
            if (token == null || password == null || password.length() < 8) {
                response = "Paramètres invalides";
            } else {
                try {
                    // Chercher l'utilisateur par reset_token et mettre à jour le mot de passe
                    String hashedPwd = org.example.utils.PasswordUtils.hasher(password);
                    try (var conn = MyDataBase_Unimind.getInstance().getConnection()) {
                        // Trouver le user_id via le token
                        var ps1 = conn.prepareStatement(
                                "SELECT user_id FROM user WHERE reset_token = ?");
                        ps1.setString(1, token);
                        var rs = ps1.executeQuery();
                        if (rs.next()) {
                            int userId = rs.getInt("user_id");
                            // Mettre à jour le mot de passe
                            var ps2 = conn.prepareStatement(
                                    "UPDATE user SET password = ?, reset_token = NULL WHERE user_id = ?");
                            ps2.setString(1, hashedPwd);
                            ps2.setInt(2, userId);
                            ps2.executeUpdate();
                            response = "OK";
                            System.out.println("✅ Mot de passe réinitialisé pour user_id: " + userId);
                        } else {
                            response = "Token invalide ou expiré";
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "Erreur serveur: " + e.getMessage();
                }
            }

            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }
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
                    var ps = conn.prepareStatement("UPDATE user SET is_verified=1, verification_token=NULL WHERE user_id=? AND verification_token=?");
                    ps.setInt(1, userId);
                    ps.setString(2, token);
                    if (ps.executeUpdate() > 0) {
                        sendResponse(exchange, getCaptchaRedirectHtml());
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
    private static String getCaptchaRedirectHtml() {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +

                // 🔥 REDIRECTION VERS CAPTCHA
                "<script>window.location.href='http://localhost:8080/captcha';</script>" +

                "</head><body>" +
                "<p>Redirection vers vérification...</p>" +
                "</body></html>";
    }
    static class ResetPasswordHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            System.out.println("ResetPasswordHandler - Query: " + query);

            if (query != null && query.contains("token=")) {
                String token = extractTokenFromQuery(query);
                System.out.println("Token extrait: " + token);

                String html = getOpenAppHtml(token);
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8); // ← bytes d'abord

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length); // ← longueur exacte
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                    os.flush(); // ← forcer l'envoi
                }
            } else {
                String errorHtml = getErrorHtml("Token manquant");
                byte[] bytes = errorHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                    os.flush();
                }
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
            return "<!DOCTYPE html>" +
                    "<html><head><meta charset='UTF-8'>" +
                    "<title>UniMind - Réinitialisation</title>" +
                    "<style>" +
                    "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                    "body { font-family: 'Segoe UI', Arial, sans-serif;" +
                    "background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%);" +
                    "min-height: 100vh; display: flex; justify-content: center;" +
                    "align-items: center; padding: 20px; }" +
                    ".card { background: white; border-radius: 20px; padding: 40px;" +
                    "max-width: 420px; width: 100%; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }" +
                    ".logo { text-align: center; font-size: 32px; margin-bottom: 8px; }" +
                    "h2 { text-align: center; color: #1E1B4B; font-size: 22px; margin-bottom: 8px; }" +
                    "p { text-align: center; color: #6B7280; font-size: 13px; margin-bottom: 24px; }" +
                    "label { display: block; font-size: 12px; font-weight: bold;" +
                    "color: #374151; margin-bottom: 6px; }" +
                    "input[type=password] { width: 100%; padding: 12px 14px;" +
                    "border: 1.5px solid #DDD6FE; border-radius: 10px; font-size: 14px;" +
                    "margin-bottom: 16px; outline: none; }" +
                    "input[type=password]:focus { border-color: #7C3AED; }" +
                    "button { width: 100%; padding: 13px;" +
                    "background: linear-gradient(to right, #D97706, #F59E0B);" +
                    "color: white; border: none; border-radius: 10px; font-size: 15px;" +
                    "font-weight: bold; cursor: pointer; }" +
                    "button:hover { opacity: 0.92; }" +
                    ".message { text-align: center; margin-top: 14px; font-size: 13px;" +
                    "padding: 10px; border-radius: 8px; display: none; }" +
                    ".error { background: #FEF2F2; color: #DC2626; border: 1px solid #FECACA; }" +
                    ".success { background: #ECFDF5; color: #059669; border: 1px solid #6EE7B7; }" +
                    "</style></head><body>" +
                    "<div class='card'>" +
                    "<div class='logo'>🔐</div>" +
                    "<h2>Nouveau mot de passe</h2>" +
                    "<p>Entrez votre nouveau mot de passe UniMind</p>" +
                    "<label>Nouveau mot de passe</label>" +
                    "<input type='password' id='pwd' placeholder='Minimum 8 caractères' />" +
                    "<label>Confirmer le mot de passe</label>" +
                    "<input type='password' id='pwd2' placeholder='Répétez le mot de passe' />" +
                    "<button onclick='submit()'>Réinitialiser mon mot de passe</button>" +
                    "<div class='message' id='msg'></div>" +
                    "</div>" +
                    "<script>" +
                    "function showMsg(text, type) {" +
                    "  var el = document.getElementById('msg');" +
                    "  el.textContent = text;" +
                    "  el.className = 'message ' + type;" +
                    "  el.style.display = 'block';" +
                    "}" +
                    "function submit() {" +
                    "  var pwd = document.getElementById('pwd').value;" +
                    "  var pwd2 = document.getElementById('pwd2').value;" +
                    "  if (!pwd || pwd.length < 8) {" +
                    "    showMsg('Le mot de passe doit contenir au moins 8 caractères.', 'error');" +
                    "    return;" +
                    "  }" +
                    "  if (pwd !== pwd2) {" +
                    "    showMsg('Les mots de passe ne correspondent pas.', 'error');" +
                    "    return;" +
                    "  }" +
                    "  fetch('http://localhost:8080/do-reset-password?token=" + token + "&password=' + encodeURIComponent(pwd), { method: 'POST' })" +
                    "  .then(function(r) { return r.text(); })" +
                    "  .then(function(text) {" +
                    "    if (text === 'OK') {" +
                    "      showMsg('Mot de passe modifié ! Vous pouvez fermer et vous connecter.', 'success');" +
                    "      document.querySelector('button').disabled = true;" +
                    "    } else {" +
                    "      showMsg('Erreur : ' + text, 'error');" +
                    "    }" +
                    "  })" +
                    "  .catch(function() {" +
                    "    showMsg('Connexion impossible. Assurez-vous que l application est ouverte.', 'error');" +
                    "  });" +
                    "}" +
                    "</script></body></html>";
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