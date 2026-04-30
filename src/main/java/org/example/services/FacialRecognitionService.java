package org.example.services;

import org.example.config.Config;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class FacialRecognitionService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean verifyFace(BufferedImage webcamImage, int userId) {
        try {
            String base64Image = bufferedImageToBase64(webcamImage);

            JSONObject body = new JSONObject();
            body.put("image", base64Image);
            body.put("user_id", userId);

            String responseBody = postJson(Config.FLASK_API_URL + "/verify-face", body.toString());
            System.out.println("Réponse Flask: " + responseBody);

            if (responseBody == null) return false;

            JSONObject json = new JSONObject(responseBody);
            return json.optBoolean("match", false);

        } catch (Exception e) {
            System.err.println("Erreur verifyFace: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerFace(BufferedImage faceImage, int userId) {
        try {
            String base64Image = bufferedImageToBase64(faceImage);

            JSONObject body = new JSONObject();
            body.put("image", base64Image);
            body.put("user_id", userId);

            String responseBody = postJson(Config.FLASK_API_URL + "/register-face", body.toString());
            System.out.println("Réponse Flask register: " + responseBody);

            if (responseBody == null) return false;

            JSONObject json = new JSONObject(responseBody);
            return json.optBoolean("success", false);

        } catch (Exception e) {
            System.err.println("Erreur registerFace: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String postJson(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Flask [" + response.statusCode() + "] → " + url);
            return response.body();

        } catch (Exception e) {
            System.err.println("Erreur requête Flask: " + e.getMessage());
            return null;
        }
    }

    private String bufferedImageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}