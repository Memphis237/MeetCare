package org.example.meetcare.utils;

import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.meetcare.controller.AppointmentBookingPatientController;
import org.example.meetcare.controller.AbstractMessagingController;
import org.example.meetcare.controller.DoctorPublicProfileController;

public class NavigationUtil {
    private static final double PATIENT_WIDTH = 1200;
    private static final double PATIENT_HEIGHT = 800;
    private static final double HOME_WIDTH = 1100;
    private static final double HOME_HEIGHT = 700;

    public static void navigateTo(Event event, String fxmlPath, double width, double height, String title) {
        try {
            AppSession.setSidebarRoute(routeFromNavigation(fxmlPath, title));
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
        AppSession.setSidebarRoute("home");
        navigateTo(event, "/view/homePatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Accueil");
    }

    public static void openPatientSearch(Event event) {
        AppSession.setSidebarRoute("search");
        navigateTo(event, "/view/searchDoctor.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Recherche");
    }

    public static void openPatientAppointments(Event event) {
        AppSession.setSidebarRoute("appointments");
        navigateTo(event, "/view/myAppointments.fxml", PATIENT_WIDTH, 720, "MeetCare - Mes rendez-vous");
    }

    public static void openPatientAppointments(Node source) {
        if (source == null) {
            return;
        }
        openPatientAppointments(new ActionEvent(source, null));
    }

    public static void openPatientAppointmentBooking(Event event) {
        AppSession.setSidebarRoute("appointments");
        navigateTo(event, "/view/appointmentBookingPatient.fxml", PATIENT_WIDTH, 760, "MeetCare - Prendre un rendez-vous");
    }

    public static void openPatientAppointmentBooking(Event event, int doctorId, String doctorName, String speciality, String address) {
        AppSession.setSidebarRoute("appointments");
        navigateToWithPostLoad(event, "/view/appointmentBookingPatient.fxml", PATIENT_WIDTH, 760, "MeetCare - Prendre un rendez-vous", controller -> {
            if (controller instanceof AppointmentBookingPatientController bookingController) {
                bookingController.setInitialDoctor(doctorId, doctorName, speciality, address, null, null);
                bookingController.applyInitialDoctorSelection();
            }
        });
    }

    public static void openPatientDocuments(Event event) {
        AppSession.setSidebarRoute("documents");
        navigateTo(event, "/view/documentsPatient.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Documents");
    }

    public static void openPatientInvoices(Event event) {
        AppSession.setSidebarRoute("invoices");
        navigateTo(event, "/view/invoicePatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Factures");
    }

    public static void openPatientMessages(Event event) {
        AppSession.setSidebarRoute("messages");
        navigateTo(event, "/view/messagePatient.fxml", 1280, 760, "MeetCare - Messages");
    }

    public static void openPatientMessages(Node source) {
        if (source == null) {
            return;
        }
        openPatientMessages(new ActionEvent(source, null));
    }

    public static void openPatientNotifications(Event event) {
        AppSession.setSidebarRoute("notifications");
        navigateTo(event, "/view/notificationsPatient.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Notifications");
    }

    public static void openPatientAssistantMessages(Event event) {
        AppSession.setSidebarRoute("assistant");
        navigateToWithPostLoad(event, "/view/messagePatient.fxml", 1280, 760, "MeetCare - Messages", controller -> {
            if (controller instanceof AbstractMessagingController messagingController) {
                messagingController.openAssistantConversationFromNavigation();
            }
        });
    }

    public static void openPatientSettings(Event event) {
        AppSession.setSidebarRoute("settings");
        navigateTo(event, "/view/settingsPatient.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }

    public static void openDoctorPublicProfile(Event event, int doctorId) {
        AppSession.setProfileReturnRoute(AppSession.getSidebarRoute());
        AppSession.setSidebarRoute("search");
        navigateToWithPostLoad(event, "/view/doctorPublicProfile.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Profil du medecin", controller -> {
            if (controller instanceof DoctorPublicProfileController profileController) {
                profileController.setDoctorId(doctorId);
            }
        });
    }

    public static void openAdminHome(Event event) {
        AppSession.setSidebarRoute("home");
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Dashboard");
    }

    public static void openAdminSearch(Event event) {
        AppSession.setSidebarRoute("search");
        navigateTo(event, "/view/usersAdmin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Utilisateurs");
    }

    public static void openAdminAppointments(Event event) {
        AppSession.setSidebarRoute("appointments");
        navigateTo(event, "/view/doctorsAdmin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - MÃ©decins");
    }

    public static void openAdminDocuments(Event event) {
        AppSession.setSidebarRoute("documents");
        navigateTo(event, "/view/appointmentsAdmin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Rendez-vous");
    }

    public static void openAdminInvoices(Event event) {
        AppSession.setSidebarRoute("invoices");
        navigateTo(event, "/view/dashboard.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Factures");
    }

    public static void openAdminStats(Event event) {
        AppSession.setSidebarRoute("stats");
        navigateTo(event, "/view/statsAdmin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Statistiques");
    }

    public static void openAdminValidation(Event event) {
        AppSession.setSidebarRoute("validation");
        navigateTo(event, "/view/validationAdmin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Validation");
    }

    public static void openAdminSettings(Event event) {
        AppSession.setSidebarRoute("settings");
        navigateTo(event, "/view/settingsAdmin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }

    public static void openMedecinHome(Event event) {
        AppSession.setSidebarRoute("home");
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Tableau de bord");
    }

    public static void openMedecinAgenda(Event event) {
        AppSession.setSidebarRoute("appointments");
        navigateTo(event, "/view/myAppointmentsMedecin.fxml", PATIENT_WIDTH, 720, "MeetCare - Agenda");
    }

    public static void openMedecinAgenda(Node source) {
        if (source == null) {
            return;
        }
        openMedecinAgenda(new ActionEvent(source, null));
    }

    public static void openMedecinValidation(Event event) {
        AppSession.setSidebarRoute("validation");
        navigateTo(event, "/view/validationMedecin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Patients");
    }

    public static void openMedecinPatients(Event event) {
        AppSession.setSidebarRoute("search");
        navigateTo(event, "/view/patientsMedecin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Recherche patients");
    }

    public static void openMedecinAvailability(Event event) {
        AppSession.setSidebarRoute("availability");
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Disponibilites");
    }

    public static void openMedecinDocuments(Event event) {
        AppSession.setSidebarRoute("documents");
        navigateTo(event, "/view/documentsMedecin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Documents");
    }

    public static void openMedecinInvoices(Event event) {
        AppSession.setSidebarRoute("invoices");
        navigateTo(event, "/view/invoiceMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Facturation");
    }

    public static void openMedecinMessages(Event event) {
        AppSession.setSidebarRoute("messages");
        navigateTo(event, "/view/messageMedecin.fxml", 1280, 760, "MeetCare - Messagerie");
    }

    public static void openMedecinMessages(Node source) {
        if (source == null) {
            return;
        }
        openMedecinMessages(new ActionEvent(source, null));
    }

    public static void openMedecinNotifications(Event event) {
        AppSession.setSidebarRoute("notifications");
        navigateTo(event, "/view/notificationsMedecin.fxml", PATIENT_WIDTH, PATIENT_HEIGHT, "MeetCare - Notifications");
    }

    public static void openMedecinAssistantMessages(Event event) {
        AppSession.setSidebarRoute("assistant");
        navigateToWithPostLoad(event, "/view/messageMedecin.fxml", 1280, 760, "MeetCare - Messagerie", controller -> {
            if (controller instanceof AbstractMessagingController messagingController) {
                messagingController.openAssistantConversationFromNavigation();
            }
        });
    }

    public static void openMedecinStats(Event event) {
        AppSession.setSidebarRoute("stats");
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Statistiques");
    }

    public static void openMedecinPublicProfile(Event event) {
        AppSession.setSidebarRoute("publicProfile");
        navigateTo(event, "/view/homeMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Profil public");
    }

    public static void openMedecinSettings(Event event) {
        AppSession.setSidebarRoute("settings");
        navigateTo(event, "/view/settingsMedecin.fxml", HOME_WIDTH, HOME_HEIGHT, "MeetCare - Parametres");
    }

    private static String routeFromNavigation(String fxmlPath, String title) {
        String titleValue = title == null ? "" : title.toLowerCase();
        String pathValue = fxmlPath == null ? "" : fxmlPath.toLowerCase();

        if (pathValue.contains("message")) {
            return "messages";
        }
        if (pathValue.contains("notifications")) {
            return "notifications";
        }
        if (pathValue.contains("settings")) {
            return "settings";
        }
        if (pathValue.contains("validation")) {
            return "validation";
        }
        if (pathValue.contains("appointment") || pathValue.contains("myappointments")) {
            return "appointments";
        }
        if (pathValue.contains("documents")) {
            return "documents";
        }
        if (pathValue.contains("invoice")) {
            return "invoices";
        }
        if (titleValue.contains("mÃ©decins") || titleValue.contains("medecins")) {
            return "appointments";
        }
        if (titleValue.contains("mes rendez-vous") || titleValue.contains("agenda")) {
            return "appointments";
        }
        if (titleValue.contains("utilisateurs") || titleValue.contains("recherche")) {
            return "search";
        }
        if (titleValue.contains("statistiques")) {
            return "stats";
        }
        if (titleValue.contains("profil public")) {
            return "publicProfile";
        }
        if (titleValue.contains("disponibilites")) {
            return "availability";
        }
        if (titleValue.contains("rendez-vous")) {
            return "documents";
        }
        if (titleValue.contains("accueil") || titleValue.contains("tableau de bord") || titleValue.contains("dashboard")) {
            return "home";
        }
        return "home";
    }

    private static void navigateToWithPostLoad(Event event, String fxmlPath, double width, double height, String title, java.util.function.Consumer<Object> afterLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (afterLoad != null) {
                afterLoad.accept(controller);
            }

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
}
