package org.example.services;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.example.config.Config;
import org.json.JSONObject;

public class CaptchaService {

    public boolean verifyCaptcha(String hcaptchaResponse) {
        if (hcaptchaResponse == null || hcaptchaResponse.isEmpty()) {
            return false;
        }

        // Token mathématique local → accepté directement
        if (hcaptchaResponse.equals("MATH_OK")) {
            return true;
        }

        try {
            String url    = "https://api.hcaptcha.com/siteverify";
            String params = "secret=" + Config.HCAPTCHA_SECRET_KEY
                    + "&response=" + hcaptchaResponse;

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(url);
                post.setEntity(new StringEntity(params));
                post.setHeader("Content-Type", "application/x-www-form-urlencoded");

                try (CloseableHttpResponse response = client.execute(post)) {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    JSONObject json     = new JSONObject(jsonResponse);
                    System.out.println("✓ hCaptcha API response: " + jsonResponse);
                    return json.optBoolean("success", false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}