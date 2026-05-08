package org.example.meetcare.utils;

import org.example.meetcare.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/meetcare?serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    public static Connection getConnection() {
        String url      = getValue("db.url",      "DB_URL",      DEFAULT_URL);
        String user     = getValue("db.user",     "DB_USER",     DEFAULT_USER);
        String password = getValue("db.password", "DB_PASSWORD", DEFAULT_PASSWORD);

        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            // ✅ Log AVANT le return
               System.out.println("Connexion a la base de donnees reussie");
                 return connection;

        } catch (SQLException ex) {
            // ✅ Log AVANT le throw
            System.out.println("Erreur de connexion a la base de donnees: " + ex.getMessage());
            throw new DatabaseException("Impossible d'ouvrir une connexion a la base", ex);
        }
    }

    private static String getValue(String propKey, String envKey, String fallback) {
        String value = System.getProperty(propKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(envKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback;
    }
}