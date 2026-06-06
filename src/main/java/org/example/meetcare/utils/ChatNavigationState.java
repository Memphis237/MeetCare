package org.example.meetcare.utils;

public final class ChatNavigationState {
    private static boolean assistantRequested;

    private ChatNavigationState() {
    }

    public static void requestAssistant() {
        assistantRequested = true;
    }

    public static void requestHumanMessages() {
        assistantRequested = false;
    }

    public static boolean consumeAssistantRequested() {
        boolean value = assistantRequested;
        assistantRequested = false;
        return value;
    }
}
