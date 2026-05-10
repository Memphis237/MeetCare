package org.example.meetcare.utils;

import org.example.meetcare.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/meetcare?useSSL=false&serverTimezone=Europe/Paris&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(
                        getConfig("MEETCARE_DB_URL", "meetcare.db.url", DEFAULT_URL),
                        getConfig("MEETCARE_DB_USER", "meetcare.db.user", DEFAULT_USER),
                        getConfig("MEETCARE_DB_PASSWORD", "meetcare.db.password", DEFAULT_PASSWORD)
                );
            }
            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de se connecter à la base de données.", e);
        }
    }

    public static synchronized void closeConnection() {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de fermer la connexion à la base de données.", e);
        } finally {
            connection = null;
        }
    }

    private static String getConfig(String envKey, String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }
}
