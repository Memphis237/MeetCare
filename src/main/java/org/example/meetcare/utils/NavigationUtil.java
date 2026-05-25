package org.example.meetcare.utils;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationUtil {
    private static final double PATIENT_WIDTH = 1200;
    private static final double PATIENT_HEIGHT = 800;
    private static final double HOME_WIDTH = 1100;
    private static final double HOME_HEIGHT = 700;

    public static void navigateTo(Event event, String fxmlPath, double width, double height, String title) {
        try {
            Parent root = FXMLLoader.load(NavigationUtil.class.getResource(fxmlPath));
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(NavigationUtil.class.getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception ex) {
            System.err.println("Erreur lors de l'ouverture de la page " + fxmlPath + ":");
            ex.printStackTrace();
            throw new IllegalStateException("Impossible d'ouvrir la page demandee.", ex);
        }
    }

    public static void openPatientHome(Event event) {
        navigateTo(event, "/view/homePatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Accueil");
    }

    public static void openPatientSearch(Event event) {
        navigateTo(event, "/view/searchDoctor.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Recherche");
    }

    public static void openPatientAppointments(Event event) {
        navigateTo(event, "/view/myAppointments.fxml", PATIENT_WIDTH, 720, "MeetCare - Mes rendez-vous");
    }

    public static void openPatientDocuments(Event event) {
        navigateTo(event, "/view/documentsPatient.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Documents");
    }

    public static void openPatientInvoices(Event event) {
        navigateTo(event, "/view/invoicePatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Factures");
    }

    public static void openPatientMessages(Event event) {
        navigateTo(event, "/view/messagePatient.fxml", 1280, 760, "MeetCare - Messages");
    }

    public static void openPatientSettings(Event event) {
        navigateTo(event, "/view/settingsPatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }

    public static void openAdminHome(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Dashboard");
    }

    public static void openAdminSearch(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Recherche");
    }

    public static void openAdminAppointments(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Rendez-vous");
    }

    public static void openAdminDocuments(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Documents");
    }

    public static void openAdminInvoices(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Factures");
    }

    public static void openAdminStats(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Statistiques");
    }

    public static void openAdminValidation(Event event) {
        navigateTo(event, "/view/validationAdmin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Validation");
    }

    public static void openAdminSettings(Event event) {
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }

    public static void openMedecinHome(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Tableau de bord");
    }

    public static void openMedecinAgenda(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Agenda");
    }

    public static void openMedecinValidation(Event event) {
        navigateTo(event, "/view/validationMedecin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Patients");
    }

    public static void openMedecinPatients(Event event) {
        openMedecinValidation(event);
    }

    public static void openMedecinAvailability(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Disponibilites");
    }

    public static void openMedecinDocuments(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Documents");
    }

    public static void openMedecinInvoices(Event event) {
        navigateTo(event, "/view/invoiceMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Facturation");
    }

    public static void openMedecinMessages(Event event) {
        navigateTo(event, "/view/messageMedecin.fxml", 1280, 760, "MeetCare - Messagerie");
    }

    public static void openMedecinStats(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Statistiques");
    }

    public static void openMedecinPublicProfile(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Profil public");
    }

    public static void openMedecinSettings(Event event) {
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }
}
