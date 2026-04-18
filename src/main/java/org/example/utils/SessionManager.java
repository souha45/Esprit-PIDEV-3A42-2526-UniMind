package org.example.utils;

public class SessionManager {

    private static SessionManager instance;
    private int userId = 1; // temporaire jusqu'à intégration login

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}