package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MediController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, Jean");
        }
    }
}
