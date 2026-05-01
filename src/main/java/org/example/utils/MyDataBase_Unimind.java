package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase_Unimind {
    final String USERNAME = "root";
    final String URL = "jdbc:mysql://localhost:3306/unimind_db?useUnicode=true&characterEncoding=UTF-8";
    final String PASSWORD = "";

    Connection connection;
    static MyDataBase_Unimind instance;

    //constructeur
    private MyDataBase_Unimind(){
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

            System.out.println("Connection established");
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }


    }

    public static MyDataBase_Unimind getInstance() {

        if(instance == null){
            instance = new MyDataBase_Unimind();
        }
        return instance;
    }

    public Connection getConnection() {

        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Connection re-established");
            }
        } catch (SQLException e) {
            System.err.println("Erreur de reconnexion: " + e.getMessage());
        }
        return connection;
    }
}