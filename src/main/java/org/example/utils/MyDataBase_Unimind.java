package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase_Unimind {
    final String USERNAME = "root";
    final String PASSWORD = "";
    final String URL = "jdbc:mysql://localhost:3306/unimind_db"
            + "?autoReconnect=true"
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&characterEncoding=UTF-8"
            + "&connectTimeout=5000"
            + "&socketTimeout=30000";

    Connection connection;
    static MyDataBase_Unimind instance;

    private MyDataBase_Unimind() {
        connect();
    }

    public static MyDataBase_Unimind getInstance() {
        if (instance == null) {
            instance = new MyDataBase_Unimind();
        }
        return instance;
    }

    // Crée une nouvelle connexion propre
    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("[DB] Connexion etablie.");
        } catch (SQLException e) {
            System.err.println("[DB] Erreur connexion : " + e.getMessage());
        }
    }

    // Vérifie si la connexion est vivante sans lancer d'exception
    private boolean isAlive() {
        try {
            return connection != null
                    && !connection.isClosed()
                    && connection.isValid(2);
        } catch (Exception e) {
            // isValid() peut elle-même lancer une exception si la connexion est morte
            return false;
        }
    }

    public Connection getConnection() {
        if (!isAlive()) {
            System.out.println("[DB] Connexion morte, reconnexion...");
            connect();
        }
        return connection;
    }
}