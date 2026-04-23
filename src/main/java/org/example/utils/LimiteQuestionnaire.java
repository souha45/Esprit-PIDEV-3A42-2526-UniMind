package org.example.utils;

public class LimiteQuestionnaire {

    private static LimiteQuestionnaire instance;
    private int userId = 1; // temporaire jusqu'à intégration login

    private LimiteQuestionnaire() {}

    public static LimiteQuestionnaire getInstance() {
        if (instance == null) {
            instance = new LimiteQuestionnaire();
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