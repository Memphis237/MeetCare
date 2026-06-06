package org.example.meetcare.model;

public enum AccountStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    SUSPENDED;

    public static AccountStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }

        try {
            return AccountStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PENDING;
        }
    }
}
