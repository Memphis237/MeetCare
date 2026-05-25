package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class SidebarPatientController {
    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private StackPane messagesBadgeContainer;

    @FXML
    private Label messagesBadgeLabel;

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

    public void setMessageBadgeCount(int count) {
        if (messagesBadgeContainer == null || messagesBadgeLabel == null) {
            return;
        }

        boolean hasUnread = count > 0;
        messagesBadgeContainer.setVisible(hasUnread);
        messagesBadgeContainer.setManaged(hasUnread);
        messagesBadgeLabel.setText(hasUnread ? (count > 99 ? "99+" : String.valueOf(count)) : "");
    }

    @FXML
    private void handleHome(MouseEvent event) {
        NavigationUtil.openPatientHome(event);
    }

    @FXML
    private void handleSearchDoctor(MouseEvent event) {
        NavigationUtil.openPatientSearch(event);
    }

    @FXML
    private void handleAppointments(MouseEvent event) {
        NavigationUtil.openPatientAppointments(event);
    }

    @FXML
    private void handleDocuments(MouseEvent event) {
        NavigationUtil.openPatientDocuments(event);
    }

    @FXML
    private void handleInvoices(MouseEvent event) {
        NavigationUtil.openPatientInvoices(event);
    }

    @FXML
    private void handleMessages(MouseEvent event) {
        NavigationUtil.openPatientMessages(event);
    }

    @FXML
    private void handleAssistant(MouseEvent event) {
        AppSession.setOpenAssistantConversation(true);
        NavigationUtil.openPatientMessages(event);
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.openPatientSettings(event);
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
            return "Patient";
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
            return "Patient";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Medecin";
            default -> "Patient";
        };
    }
}
