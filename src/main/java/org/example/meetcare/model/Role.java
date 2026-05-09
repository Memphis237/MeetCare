package org.example.meetcare.model;

public enum Role {
    USER,
    ADMIN,
    MEDECIN;

    public static Role fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }

        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return USER;
        }
    }
}
