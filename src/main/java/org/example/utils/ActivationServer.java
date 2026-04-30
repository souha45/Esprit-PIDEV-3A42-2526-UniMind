package org.example.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.example.controllers.ActivationController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ActivationServer {
    private static HttpServer server;
    private static ActivationController activationController;
    private static int currentPort = 8080;

    public static void start() throws IOException {
        if (server == null) {
            // Trouver un port disponible
            currentPort = 8082;

            System.out.println("🔄 Création du serveur HTTP sur le port " + currentPort + "...");
            server = HttpServer.create(new InetSocketAddress(currentPort), 0);
            server.createContext("/activate", new ActivationHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("✅ Serveur d'activation démarré sur http://localhost:" + currentPort);
        }
    }

    private static int findAvailablePort(int startPort) {
        int port = startPort;
        while (port < startPort + 100) {
            try (ServerSocket socket = new ServerSocket(port)) {
                socket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                port++;
            }
        }
        return startPort;
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("Serveur d'activation arrêté");
        }
    }

    public static int getPort() {
        return currentPort;
    }

    public static void setActivationController(ActivationController controller) {
        activationController = controller;
    }

    private static class ActivationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);

            String userIdStr = params.get("userId");
            String token = params.get("token");

            String response;
            int statusCode;

            if (userIdStr != null && token != null && activationController != null) {
                try {
                    int userId = Integer.parseInt(userIdStr);
                    boolean success = activationController.activerCompte(userId, token);

                    if (success) {
                        response = getSuccessHtml();
                        statusCode = 200;
                    } else {
                        response = getErrorHtml("Token invalide ou compte déjà activé");
                        statusCode = 400;
                    }
                } catch (NumberFormatException e) {
                    response = getErrorHtml("ID utilisateur invalide");
                    statusCode = 400;
                }
            } else {
                response = getErrorHtml("Paramètres manquants ou serveur non configuré");
                statusCode = 400;
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        params.put(key, value);
                    }
                }
            }
            return params;
        }

        private String getSuccessHtml() {
            return "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Activation réussie - UniMind</title>\n" +
                    "    <style>\n" +
                    "        body{background:linear-gradient(135deg,#4F46E5,#7C3AED);font-family:Segoe UI,Arial,sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0}\n" +
                    "        .container{background:white;border-radius:20px;padding:40px;text-align:center;max-width:500px}\n" +
                    "        .icon{font-size:64px;margin-bottom:20px}\n" +
                    "        h1{color:#1E1B4B;margin-bottom:10px}\n" +
                    "        p{color:#4B5563;margin-bottom:30px}\n" +
                    "        button{background:linear-gradient(135deg,#4F46E5,#7C3AED);color:white;border:none;padding:12px 32px;border-radius:10px;font-size:16px;cursor:pointer}\n" +
                    "        button:hover{transform:scale(1.05)}\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"icon\">✅</div>\n" +
                    "        <h1>Activation réussie !</h1>\n" +
                    "        <p>Votre compte UniMind a été activé avec succès.<br>Vous pouvez maintenant vous connecter à l'application.</p>\n" +
                    "        <button onclick=\"window.close()\">Fermer cette fenêtre</button>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
        }

        private String getErrorHtml(String message) {
            return "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Erreur d'activation - UniMind</title>\n" +
                    "    <style>\n" +
                    "        body{background:linear-gradient(135deg,#DC2626,#991B1B);font-family:Segoe UI,Arial,sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0}\n" +
                    "        .container{background:white;border-radius:20px;padding:40px;text-align:center;max-width:500px}\n" +
                    "        .icon{font-size:64px;margin-bottom:20px}\n" +
                    "        h1{color:#991B1B;margin-bottom:10px}\n" +
                    "        p{color:#4B5563;margin-bottom:30px}\n" +
                    "        button{background:linear-gradient(135deg,#DC2626,#991B1B);color:white;border:none;padding:12px 32px;border-radius:10px;font-size:16px;cursor:pointer}\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"icon\">❌</div>\n" +
                    "        <h1>Erreur d'activation</h1>\n" +
                    "        <p>" + message + "</p>\n" +
                    "        <button onclick=\"window.close()\">Fermer</button>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }
}