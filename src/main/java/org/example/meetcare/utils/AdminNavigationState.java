package org.example.meetcare.utils;

public final class AdminNavigationState {
    private static String currentPage = "dashboard";

    private AdminNavigationState() {
    }

    public static String getCurrentPage() {
        return currentPage;
    }

    public static void setCurrentPage(String page) {
        currentPage = page == null || page.isBlank() ? "dashboard" : page;
    }
}
