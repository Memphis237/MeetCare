package org.example.meetcare.utils;

import org.example.meetcare.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DEFAULT_URL = "";
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_PASSWORD = "";

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection getConnection() {
        try {
            EnvLoader.load();
            if (connection == null || connection.isClosed()) {
                String url = getConfig("MEETCARE_DB_URL", "meetcare.db.url", DEFAULT_URL);
                String user = getConfig("MEETCARE_DB_USER", "meetcare.db.user", DEFAULT_USER);
                String password = getConfig("MEETCARE_DB_PASSWORD", "meetcare.db.password", DEFAULT_PASSWORD);

                if (isBlank(url) || isBlank(user)) {
                    throw new DatabaseException("La configuration de la base de donnees est incomplete.");
                }

                connection = DriverManager.getConnection(url, user, password);
            }
            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de se connecter a la base de donnees.", e);
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
            throw new DatabaseException("Impossible de fermer la connexion a la base de donnees.", e);
        } finally {
            connection = null;
        }
    }

    private static String getConfig(String envKey, String propertyKey, String defaultValue) {
        String envStylePropertyValue = System.getProperty(envKey);
        if (envStylePropertyValue != null && !envStylePropertyValue.isBlank()) {
            return envStylePropertyValue;
        }

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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
