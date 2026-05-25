package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class InvoicePatientController {
    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private void initialize() {
        Users user = refreshCurrentUser();
        if (avatarCircle != null && avatarInitialsLabel != null) {
            AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    private void handleOpenHome(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/homePatient.fxml", 1100, 700, "MeetCare - Accueil");
    }

    @FXML
    private void handleOpenSearchDoctor(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/searchDoctor.fxml", 1200, 800, "MeetCare - Recherche");
    }

    @FXML
    private void handleOpenAppointments(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/myAppointments.fxml", 1200, 720, "MeetCare - Mes rendez-vous");
    }

    @FXML
    private void handleOpenDocuments(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/documentsPatient.fxml", 1200, 800, "MeetCare - Documents");
    }

    @FXML
    private void handleOpenSettings(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - Parametres");
    }

    private Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = new UsersDao().findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }
}
