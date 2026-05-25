package org.example.meetcare.controller;

import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public class SidebarPatientController {

    private final NavigationUtil navigationUtil = new NavigationUtil();

    @FXML
    private void handleHome(MouseEvent event) {
        navigationUtil.navigateTo(event, "/view/homePatient.fxml", 1100, 700, "MeetCare - Accueil");
    }

    @FXML
    private void handleSearchDoctor(MouseEvent event) {
        navigationUtil.navigateTo(event, "/view/searchDoctor.fxml", 1200, 800, "MeetCare - Recherche");
    }

    @FXML
    private void handleAppointments(MouseEvent event) {
        navigationUtil.navigateTo(event, "/view/myAppointments.fxml", 1200, 720, "MeetCare - Mes rendez-vous");
    }

    @FXML
    private void handleDocuments(MouseEvent event) {
        navigationUtil.navigateTo(event, "/view/documentsPatient.fxml", 1200, 800, "MeetCare - Documents");
    }

    @FXML
    private void handleInvoices(MouseEvent event) {
        navigationUtil.navigateTo(event, "/view/invoicePatient.fxml", 1100, 700, "MeetCare - Factures");
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        navigationUtil.navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - Parametres");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}
