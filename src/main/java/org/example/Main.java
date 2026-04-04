package org.example;

import org.example.utils.MyDataBase_Unimind;

public class Main {
    public static void main(String[] args) {
        // Établir la connexion à la base de données
        MyDataBase_Unimind db = MyDataBase_Unimind.getInstance();

        // Vérifier si la connexion est établie
        if (db.getConnection() != null) {
            System.out.println("✓ Connexion à la base de données réussie !");
        } else {
            System.out.println("✗ Échec de la connexion à la base de données.");
        }
    }
}