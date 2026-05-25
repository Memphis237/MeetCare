package org.example.meetcare.utils;

public final class SelectedPatientStore {
    private static int patientId;
    private static String initialTab = "history";

    private SelectedPatientStore() {
    }

    public static void setSelection(int id, String tab) {
        patientId = id;
        initialTab = tab == null || tab.isBlank() ? "history" : tab;
    }

    public static int getPatientId() {
        return patientId;
    }

    public static String getInitialTab() {
        return initialTab;
    }
}
