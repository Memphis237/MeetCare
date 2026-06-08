package org.example.meetcare.controller;

import org.example.meetcare.model.Users;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractChatControllerTest {
    private static final class TestChatController extends AbstractChatController {
    }

    @Test
    void buildDisplayNameShouldPreferFirstnameThenUsername() {
        TestChatController controller = new TestChatController();
        Users user = new Users();
        user.setPrenom("Alice");
        user.setUsername("admin");

        assertEquals("Alice admin", controller.buildDisplayName(user));
    }

    @Test
    void getWelcomeMessageShouldReuseDisplayName() {
        TestChatController controller = new TestChatController();
        Users user = new Users();
        user.setPrenom("Sophie");
        user.setUsername("durand");

        assertEquals("Bonjour Sophie durand, comment puis-je vous aider aujourd'hui ?",
                controller.getWelcomeMessage(user));
    }

    @Test
    void formatTimeShouldReturnEmptyStringWhenDateIsMissing() {
        TestChatController controller = new TestChatController();

        assertEquals("", controller.formatTime(null));
        assertEquals("14:35", controller.formatTime(LocalDateTime.of(2026, 6, 8, 14, 35)));
    }

    @Test
    void rootCauseMessageShouldUnwrapNestedCause() {
        TestChatController controller = new TestChatController();
        RuntimeException exception = new RuntimeException("outer", new IllegalStateException("inner"));

        assertEquals("inner", controller.rootCauseMessage(exception));
    }
}
