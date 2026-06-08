package org.example.meetcare.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    @Test
    void hashLegacyShouldMatchSha256Reference() {
        assertEquals(
                "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
                PasswordUtil.hashLegacy("password"));
    }

    @Test
    void bcryptHashShouldBeRecognizedAndMatched() {
        String hash = PasswordUtil.hash("secret123");

        assertTrue(PasswordUtil.isBcryptHash(hash));
        assertTrue(PasswordUtil.matches("secret123", hash));
        assertFalse(PasswordUtil.matches("wrong", hash));
    }

    @Test
    void legacyHashShouldStillBeSupportedByMatches() {
        String hash = PasswordUtil.hashLegacy("legacy-pass");

        assertFalse(PasswordUtil.isBcryptHash(hash));
        assertTrue(PasswordUtil.matches("legacy-pass", hash));
        assertFalse(PasswordUtil.matches("wrong", hash));
    }
}
