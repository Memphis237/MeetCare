package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class HomePatientController {
    @FXML
    private Label patientNameLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private void initialize() {
        Users user = AppSession.getCurrentUser();
        if (user == null) {
            return;
        }

        String displayName = user.getUsername() == null || user.getUsername().isBlank()
                ? "Patient"
                : user.getUsername();
        patientNameLabel.setText(displayName);
        welcomeLabel.setText("Bienvenue, " + displayName);
        roleLabel.setText(roleToLabel(user.getRole()));
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - Paramètres");
    }

    private String roleToLabel(Role role) {
        if (role == null) {
            return "Patient";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Médecin";
            default -> "Patient";
        };
    }
}
