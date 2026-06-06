package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;
import org.example.meetcare.model.Users;

public class SidebarPatientController {
    @FXML
    private void handleHome(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/homePatient.fxml", 1100, 700, "MeetCare - Accueil");
    }

    @FXML
    private void handleSearchDoctor(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/searchDoctor.fxml", 1200, 800, "MeetCare - Recherche");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/myAppointments.fxml", 1200, 720, "MeetCare - Mes rendez-vous");
    }

    @FXML
    private void handleDocuments(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/documentsPatient.fxml", 1200, 800, "MeetCare - Documents");
    }

    @FXML
    private void handleInvoices(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/invoicePatient.fxml", 1100, 700, "MeetCare - Factures");
    }

    @FXML
    private void handleMessages(ActionEvent event) {
        NavigationUtil.openPatientMessages(event);
    }

    @FXML
    private void handleAssistant(ActionEvent event) {
        NavigationUtil.openPatientAssistant(event);
    }

    @FXML
    private void handleNotifications(ActionEvent event) {
        showUnavailableFeature("Notifications");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - Parametres");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    public void refreshSidebar(Users user) {
        // Sidebar currently does not display dynamic profile data on this branch.
    }

    public void setMessageBadgeCount(int count) {
        // Badge support is optional on this sidebar version.
    }

    private void showUnavailableFeature(String featureName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(featureName);
        alert.setContentText("Cette page sera bientot disponible.");
        alert.showAndWait();
    }
}
