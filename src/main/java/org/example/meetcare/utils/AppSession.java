package org.example.meetcare.utils;

import org.example.meetcare.model.Users;

public final class AppSession {
    private static Users currentUser;
    private static boolean openAssistantConversation;
    private static String sidebarRoute;

    private AppSession() {
    }

    public static Users getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Users user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
        openAssistantConversation = false;
        sidebarRoute = null;
    }

    public static boolean isOpenAssistantConversation() {
        return openAssistantConversation;
    }

    public static void setOpenAssistantConversation(boolean openAssistantConversation) {
        AppSession.openAssistantConversation = openAssistantConversation;
    }

    public static String getSidebarRoute() {
        return sidebarRoute;
    }

    public static void setSidebarRoute(String sidebarRoute) {
        AppSession.sidebarRoute = sidebarRoute;
    }
}
