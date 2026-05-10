package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import org.example.meetcare.utils.LogoutUtil;

public class SettingsPatientController {
    @FXML
    private CheckBox emailNotifToggle;

    @FXML
    private CheckBox smsNotifToggle;

    @FXML
    private void initialize() {
        if (emailNotifToggle != null) {
            emailNotifToggle.setSelected(true);
        }
        if (smsNotifToggle != null) {
            smsNotifToggle.setSelected(true);
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}
