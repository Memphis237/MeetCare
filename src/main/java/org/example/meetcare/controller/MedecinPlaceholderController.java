package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class MedecinPlaceholderController {
    @FXML
    private void handleBackHome(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/homeMedecin.fxml", 1100, 700, "MeetCare - Espace medecin");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}
