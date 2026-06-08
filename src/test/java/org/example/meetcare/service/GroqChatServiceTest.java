package org.example.meetcare.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroqChatServiceTest {
    @AfterEach
    void tearDown() {
        System.clearProperty("GROQ_MODEL");
        System.clearProperty("groq.model");
    }

    @Test
    void getModelNameShouldPreferSystemProperty() {
        System.setProperty("GROQ_MODEL", "test-model");

        GroqChatService service = new GroqChatService();
        assertEquals("test-model", service.getModelName());
    }
}
