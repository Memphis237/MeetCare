package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class SidebarAdminController {
    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private void initialize() {
        refreshSidebar(null);
    }

    public void refreshSidebar(Users fallbackUser) {
        Users user = refreshCurrentUser(fallbackUser);
        if (user == null) {
            return;
        }

        patientNameLabel.setText(buildDisplayName(user));
        roleLabel.setText(roleToLabel(user.getRole()));
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
    }

    @FXML
    private void handleHome(MouseEvent event) {
        NavigationUtil.openAdminHome(event);
    }

    @FXML
    private void handleSearchDoctor(MouseEvent event) {
        NavigationUtil.openAdminSearch(event);
    }

    @FXML
    private void handleAppointments(MouseEvent event) {
        NavigationUtil.openAdminAppointments(event);
    }

    @FXML
    private void handleDocuments(MouseEvent event) {
        NavigationUtil.openAdminDocuments(event);
    }

    @FXML
    private void handleInvoices(MouseEvent event) {
        NavigationUtil.openAdminInvoices(event);
    }

    @FXML
    private void handleStats(MouseEvent event) {
        NavigationUtil.openAdminStats(event);
    }

    @FXML
    private void handleValidation(MouseEvent event) {
        NavigationUtil.openAdminValidation(event);
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.openAdminSettings(event);
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    private Users refreshCurrentUser(Users fallbackUser) {
        Users sessionUser = AppSession.getCurrentUser();
        Users user = sessionUser != null ? sessionUser : fallbackUser;
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = new UsersDao().findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Admin";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String roleToLabel(Role role) {
        if (role == null) {
            return "Admin";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Medecin";
            default -> "Patient";
        };
    }
}
