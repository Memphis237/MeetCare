package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.example.meetcare.utils.LogoutUtil;

public class HomeMedecinController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, Dr. Martin");
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}
