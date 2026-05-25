package org.example.meetcare.controller;

import javafx.event.Event;
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

public class SidebarMedecinController {
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
        NavigationUtil.openMedecinHome(event);
    }

    @FXML
    private void handleSearchDoctor(MouseEvent event) {
        NavigationUtil.openMedecinHome(event);
    }

    @FXML
    private void handleAppointments(MouseEvent event) {
        NavigationUtil.openMedecinAgenda(event);
    }

    @FXML
    private void handlePatients(MouseEvent event) {
        NavigationUtil.openMedecinPatients(event);
    }

    @FXML
    private void handleAvailability(MouseEvent event) {
        NavigationUtil.openMedecinAvailability(event);
    }

    @FXML
    private void handleDocuments(MouseEvent event) {
        NavigationUtil.openMedecinDocuments(event);
    }

    @FXML
    private void handleInvoices(MouseEvent event) {
        NavigationUtil.openMedecinInvoices(event);
    }

    @FXML
    private void handleMessages(MouseEvent event) {
        NavigationUtil.openMedecinMessages(event);
    }

    @FXML
    private void handleAssistant(MouseEvent event) {
        AppSession.setOpenAssistantConversation(true);
        NavigationUtil.openMedecinMessages(event);
    }

    @FXML
    private void handleStats(MouseEvent event) {
        NavigationUtil.openMedecinStats(event);
    }

    @FXML
    private void handlePublicProfile(MouseEvent event) {
        NavigationUtil.openMedecinPublicProfile(event);
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.openMedecinSettings(event);
    }

    @FXML
    private void handleValidation(MouseEvent event) {
        NavigationUtil.openMedecinValidation(event);
    }

    @FXML
    private void handleLogout(Event event) {
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
            return "Medecin";
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
            return "Medecin";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Medecin";
            default -> "Patient";
        };
    }
}
