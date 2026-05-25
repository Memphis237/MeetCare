package org.example.meetcare.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class EnvLoader {
    private static boolean loaded;

    private EnvLoader() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        Path envPath = Paths.get(System.getProperty("user.dir"), ".env");
        if (!Files.isRegularFile(envPath)) {
            loaded = true;
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                parseLine(line);
            }
        } catch (IOException ex) {
            System.err.println("Impossible de lire le fichier .env.");
            ex.printStackTrace();
        } finally {
            loaded = true;
        }
    }

    private static void parseLine(String rawLine) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
            return;
        }

        int separatorIndex = line.indexOf('=');
        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();
        if (key.isEmpty() || System.getProperty(key) != null) {
            return;
        }

        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        System.setProperty(key, value);
    }
}
