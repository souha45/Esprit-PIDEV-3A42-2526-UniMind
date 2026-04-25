package org.example.services;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.config.Config;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleAuthService {
    private static final String REDIRECT_URI = "http://localhost:8080/callback";

    public String getLoginUrl() {
        String scope = URLEncoder.encode("email profile openid", StandardCharsets.UTF_8);
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + Config.GOOGLE_CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&response_type=code" +
                "&scope=" + scope +
                "&access_type=offline";
    }

    public GoogleUserInfo exchangeCodeForUserInfo(String authorizationCode) {
        try {
            String accessToken = getAccessToken(authorizationCode);
            if (accessToken == null) {
                return null;
            }
            return getUserInfo(accessToken);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'échange du code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String getAccessToken(String authorizationCode) {
        String tokenUrl = "https://oauth2.googleapis.com/token";
        String body = "code=" + authorizationCode +
                "&client_id=" + Config.GOOGLE_CLIENT_ID +
                "&client_secret=" + Config.GOOGLE_CLIENT_SECRET +
                "&redirect_uri=" + REDIRECT_URI +
                "&grant_type=authorization_code";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(tokenUrl);
            post.setEntity(new StringEntity(body));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            try (CloseableHttpResponse response = client.execute(post)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(jsonResponse);

                if (json.has("access_token")) {
                    return json.getString("access_token");
                } else if (json.has("error")) {
                    System.err.println("Erreur Google: " + json.getString("error"));
                    if (json.has("error_description")) {
                        System.err.println("Description: " + json.getString("error_description"));
                    }
                    return null;
                }
            }
        } catch (IOException | org.apache.hc.core5.http.ParseException e) {
            System.err.println("Erreur réseau: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private GoogleUserInfo getUserInfo(String accessToken) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://www.googleapis.com/oauth2/v2/userinfo");
            get.setHeader("Authorization", "Bearer " + accessToken);

            try (CloseableHttpResponse response = client.execute(get)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(jsonResponse);

                if (json.has("error")) {
                    System.err.println("Erreur API Google: " + json.optString("error"));
                    return null;
                }

                return new GoogleUserInfo(
                        json.optString("id"),
                        json.optString("email"),
                        json.optString("name"),
                        json.optString("given_name"),
                        json.optString("family_name"),
                        json.optString("picture")
                );
            }
        } catch (IOException | org.apache.hc.core5.http.ParseException e) {
            System.err.println("Erreur lors de la récupération des infos utilisateur: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static class GoogleUserInfo {
        public String id;
        public String email;
        public String name;
        public String givenName;
        public String familyName;
        public String picture;

        public GoogleUserInfo(String id, String email, String name, String givenName, String familyName, String picture) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.givenName = givenName;
            this.familyName = familyName;
            this.picture = picture;
        }

        @Override
        public String toString() {
            return "GoogleUserInfo{" +
                    "email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}