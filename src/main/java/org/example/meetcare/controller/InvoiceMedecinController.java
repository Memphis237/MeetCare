package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
<<<<<<< ours
import javafx.scene.input.MouseEvent;
=======
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
>>>>>>> theirs
import org.example.meetcare.utils.LogoutUtil;

public class InvoiceMedecinController {
    @FXML
<<<<<<< ours
=======
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private void initialize() {
        Users user = AppSession.getCurrentUser();
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
    }

    @FXML
>>>>>>> theirs
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}
