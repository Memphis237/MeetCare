package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

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
}
