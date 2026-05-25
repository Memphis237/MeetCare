package org.example.meetcare.controller;

import javafx.fxml.FXML;
import org.example.meetcare.model.Role;

public class MessageMedecinController extends AbstractMessagingController {
    @FXML
    private void initialize() {
        initializeMessaging();
    }

    @Override
    protected Role getTargetRole() {
        return Role.USER;
    }
}
