package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class SidebarMedecinController {
    @FXML
    private void handleDashboard(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/homeMedecin.fxml", 1200, 760, "MeetCare - Espace medecin");
    }

    @FXML
    private void handleAgenda(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/agendaMedecin.fxml", 1200, 760, "MeetCare - Agenda medecin");
    }

    @FXML
    private void handlePatients(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/patientsMedecin.fxml", 1200, 760, "MeetCare - Patients");
    }

    @FXML
    private void handleDocuments(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/documentsMedecin.fxml", 1200, 760, "MeetCare - Documents medecin");
    }

    @FXML
    private void handleFacturation(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/invoiceMedecin.fxml", 1200, 750, "MeetCare - Facturation medecin");
    }

    @FXML
    private void handleMessages(ActionEvent event) {
        NavigationUtil.openMedecinMessages(event);
    }

    @FXML
    private void handleAssistantIA(ActionEvent event) {
        NavigationUtil.openMedecinAssistant(event);
    }

    @FXML
    private void handleStatistics(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/statistiquesMedecin.fxml", 1100, 700, "MeetCare - Statistiques medecin");
    }

    @FXML
    private void handleDisponibilites(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/disponibilitesMedecin.fxml", 1100, 700, "MeetCare - Disponibilites");
    }

    @FXML
    private void handleProfil(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/profilMedecin.fxml", 1100, 700, "MeetCare - Profil medecin");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/view/settingsMedecin.fxml", 1100, 700, "MeetCare - Parametres medecin");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}
