package org.example.meetcare.utils;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationUtil {
    private static final double PATIENT_WIDTH = 1200;
    private static final double PATIENT_HEIGHT = 800;
    private static final double HOME_WIDTH = 1100;
    private static final double HOME_HEIGHT = 700;
    private static final double MESSAGE_WIDTH = 1180;
    private static final double MESSAGE_HEIGHT = 660;

    public static void navigateTo(Node source, String fxmlPath, double width, double height, String title) {
        navigateToNode(source, fxmlPath, width, height, title);
    }

    public static void navigateTo(Event event, String fxmlPath, double width, double height, String title) {
        navigateToNode((Node) event.getSource(), fxmlPath, width, height, title);
    }

    public static void navigateTo(ActionEvent event, String fxmlPath, double width, double height, String title) {
        navigateToNode((Node) event.getSource(), fxmlPath, width, height, title);
    }

    public static void navigateTo(MouseEvent event, String fxmlPath, double width, double height, String title) {
        navigateToNode((Node) event.getSource(), fxmlPath, width, height, title);
    }

    private static void navigateToNode(Node source, String fxmlPath, double width, double height, String title) {
        try {
            Parent root = FXMLLoader.load(NavigationUtil.class.getResource(fxmlPath));
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(NavigationUtil.class.getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
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
        navigateTo(event, "/view/messagePatient.fxml", MESSAGE_WIDTH, MESSAGE_HEIGHT, "MeetCare - Messages");
    }

    public static void openPatientSettings(Event event) {
        navigateTo(event, "/view/settingsPatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }

    public static void openMedecinMessages(Event event) {
        navigateTo(event, "/view/messageMedecin.fxml", MESSAGE_WIDTH, MESSAGE_HEIGHT, "MeetCare - Messagerie");
    }
}
