package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AdminNavigationState;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

import java.util.List;

public class SidebarAdminController {
    @FXML private VBox menuContainer;
    @FXML private Button dashboardButton;
    @FXML private Button usersButton;
    @FXML private Button doctorsButton;
    @FXML private Button appointmentsButton;
    @FXML private Button documentsButton;
    @FXML private Button invoicesButton;
    @FXML private Button statisticsButton;
    @FXML private Button waitlistButton;
    @FXML private Button notificationsButton;
    @FXML private Button activityButton;
    @FXML private Button settingsButton;

    @FXML
    private void initialize() {
        applyActiveState();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        open(event, "dashboard", "/view/dashboard.fxml", 1200, 700, "MeetCare - Admin");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        open(event, "users", "/view/adminUsers.fxml", 1200, 700, "MeetCare - Utilisateurs");
    }

    @FXML
    private void handleDoctors(ActionEvent event) {
        open(event, "doctors", "/view/adminDoctors.fxml", 1200, 700, "MeetCare - Medecins");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        open(event, "appointments", "/view/adminAppointments.fxml", 1200, 700, "MeetCare - Rendez-vous");
    }

    @FXML
    private void handleDocuments(ActionEvent event) {
        open(event, "documents", "/view/adminDocuments.fxml", 1200, 700, "MeetCare - Documents");
    }

    @FXML
    private void handleInvoices(ActionEvent event) {
        open(event, "invoices", "/view/adminInvoices.fxml", 1200, 700, "MeetCare - Factures");
    }

    @FXML
    private void handleStatistics(ActionEvent event) {
        open(event, "statistics", "/view/adminStatistics.fxml", 1200, 700, "MeetCare - Statistiques");
    }

    @FXML
    private void handleWaitlist(ActionEvent event) {
        open(event, "waitlist", "/view/adminWaitlist.fxml", 1200, 700, "MeetCare - Liste d'attente");
    }

    @FXML
    private void handleNotifications(ActionEvent event) {
        open(event, "notifications", "/view/adminNotifications.fxml", 1200, 700, "MeetCare - Notifications");
    }

    @FXML
    private void handleActivity(ActionEvent event) {
        open(event, "activity", "/view/adminActivity.fxml", 1200, 700, "MeetCare - Activite");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        open(event, "settings", "/view/adminSettings.fxml", 1200, 700, "MeetCare - Parametres");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    public void refreshSidebar(Users user) {
        applyActiveState();
    }

    private void open(ActionEvent event, String page, String path, double width, double height, String title) {
        AdminNavigationState.setCurrentPage(page);
        NavigationUtil.navigateTo(event, path, width, height, title);
    }

    private void applyActiveState() {
        String currentPage = AdminNavigationState.getCurrentPage();
        List<Button> buttons = List.of(
                dashboardButton, usersButton, doctorsButton, appointmentsButton, documentsButton,
                invoicesButton, statisticsButton, waitlistButton, notificationsButton, activityButton, settingsButton
        );

        for (Button button : buttons) {
            if (button != null) {
                button.getStyleClass().remove("admin-menu-button-active");
            }
        }

        Button activeButton = switch (currentPage) {
            case "users" -> usersButton;
            case "doctors" -> doctorsButton;
            case "appointments" -> appointmentsButton;
            case "documents" -> documentsButton;
            case "invoices" -> invoicesButton;
            case "statistics" -> statisticsButton;
            case "waitlist" -> waitlistButton;
            case "notifications" -> notificationsButton;
            case "activity" -> activityButton;
            case "settings" -> settingsButton;
            default -> dashboardButton;
        };

        if (activeButton != null) {
            activeButton.getStyleClass().add("admin-menu-button-active");
        }
    }
}
