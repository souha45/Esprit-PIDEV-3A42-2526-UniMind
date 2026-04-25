package org.example.config;

public class Config {
    // Mailjet Configuration
    public static final String MAILJET_API_KEY = "547907a4351f5571282b5dd370c7c916";
    public static final String MAILJET_SECRET_KEY = "b526ebe0c9c7618f3b97dece5d51d759";
    public static final String MAILJET_FROM_EMAIL = "louatiislem74@gmail.com";
    public static final String MAILJET_FROM_NAME = "UniMind";
    // Google OAuth
    public static final String GOOGLE_CLIENT_ID = "1013888225829-rrijv123rm3mhib9ivl6goi9hgag388f.apps.googleusercontent.com";
    public static final String GOOGLE_CLIENT_SECRET = "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";

    // reCAPTCHA// ── Serveur Flask (Reconnaissance Faciale) ─────────────────────────
    //    // Lancer : cd flask_face_server && python app.py
    public static final String FLASK_API_URL = "http://localhost:5001";
    // ── hCaptcha ──────────────────────────────────────────────────────────
    public static final String HCAPTCHA_SITE_KEY   = "c086af9a-5268-45d6-b594-7c08f1080242";
    public static final String HCAPTCHA_SECRET_KEY = "ES_f0c81756750a44918dc4259ac7031648";

    // Admin email
    public static final String ADMIN_EMAIL = "louatiislem74@gmail.com";
    public static final String BASE_URL = "http://localhost:8080";
}