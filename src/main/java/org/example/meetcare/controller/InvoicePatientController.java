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

public class InvoicePatientController {
    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private void initialize() {
        Users user = refreshCurrentUser();
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
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
