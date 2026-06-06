package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
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
    private HBox homeItem;

    @FXML
    private HBox searchItem;

    @FXML
    private HBox appointmentsItem;

    @FXML
    private HBox documentsItem;

    @FXML
    private HBox invoicesItem;

    @FXML
    private HBox messagesItem;

    @FXML
    private HBox assistantItem;

    @FXML
    private HBox notificationsItem;

    @FXML
    private HBox settingsItem;

    @FXML
    private HBox logoutItem;

    @FXML
    private void initialize() {
        refreshSidebar(null);
        Platform.runLater(() -> applyActiveRoute(AppSession.getSidebarRoute()));
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

    public void applyActiveRoute(String route) {
        setInactive(homeItem, searchItem, appointmentsItem, documentsItem, invoicesItem, messagesItem, assistantItem, notificationsItem, settingsItem, logoutItem);

        HBox activeItem = switch (route == null ? "home" : route) {
            case "search" -> searchItem;
            case "appointments" -> appointmentsItem;
            case "documents" -> documentsItem;
            case "invoices" -> invoicesItem;
            case "messages" -> messagesItem;
            case "assistant" -> assistantItem;
            case "settings" -> settingsItem;
            default -> homeItem;
        };

        setActive(activeItem);
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
        NavigationUtil.openPatientAssistantMessages(event);
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

    private void setInactive(HBox... items) {
        for (HBox item : items) {
            if (item == null) {
                continue;
            }
            item.setStyle("-fx-padding: 10 14 10 14; -fx-background-radius: 8; -fx-cursor: hand;");
            styleLabels(item, false);
        }
    }

    private void setActive(HBox item) {
        if (item == null) {
            return;
        }
        item.setStyle("-fx-padding: 10 14 10 14; -fx-background-color: #D6E9F8; -fx-background-radius: 8; -fx-cursor: hand;");
        styleLabels(item, true);
    }

    private void styleLabels(HBox item, boolean active) {
        item.getChildren().forEach(child -> {
            if (child instanceof Label label) {
                String text = label.getText() == null ? "" : label.getText();
                if (text.equals("Déconnexion")) {
                    label.setStyle("-fx-font-size: 13px; -fx-text-fill: red;");
                    return;
                }
                boolean isPrimaryText = !"".equals(text) && !"15".equals(text);
                if (active) {
                    label.setStyle(isPrimaryText
                            ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;"
                            : "-fx-font-size: 14px; -fx-text-fill: #2B7FD4;");
                } else {
                    label.setStyle(isPrimaryText
                            ? "-fx-font-size: 13px; -fx-text-fill: #4A6B8A;"
                            : "-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");
                }
            }
        });
    }
}
