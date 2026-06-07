package org.example.meetcare.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.DoctorDocumentDao;
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
    private StackPane validationBadgeContainer;

    @FXML
    private Label validationBadgeLabel;

    @FXML
    private HBox homeItem;

    @FXML
    private HBox searchItem;

    @FXML
    private HBox appointmentsItem;

    @FXML
    private HBox documentsItem;

    @FXML
    private HBox statsItem;

    @FXML
    private HBox validationItem;

    @FXML
    private HBox settingsItem;

    @FXML
    private HBox logoutItem;

    private final DoctorDocumentDao doctorDocumentDao = new DoctorDocumentDao();

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
        updateValidationBadge();
    }

    public void applyActiveRoute(String route) {
        setInactive(homeItem, searchItem, appointmentsItem, documentsItem, statsItem, validationItem, settingsItem, logoutItem);

        HBox activeItem = switch (route == null ? "home" : route) {
            case "search" -> searchItem;
            case "appointments" -> appointmentsItem;
            case "documents" -> documentsItem;
            case "stats" -> statsItem;
            case "validation" -> validationItem;
            case "settings" -> settingsItem;
            default -> homeItem;
        };

        setActive(activeItem);
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

    private void updateValidationBadge() {
        if (validationBadgeContainer == null || validationBadgeLabel == null) {
            return;
        }

        int count = doctorDocumentDao.countPendingDoctorsForAdmin();
        boolean show = count > 0;
        validationBadgeContainer.setVisible(show);
        validationBadgeContainer.setManaged(show);
        validationBadgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
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
                if (text.equals("DÃ©connexion")) {
                    label.setStyle("-fx-font-size: 13px; -fx-text-fill: red;");
                    return;
                }
                if (text.isBlank() || "15".equals(text) || "99+".equals(text)) {
                    return;
                }
                label.setStyle(active
                        ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2B7FD4;"
                        : "-fx-font-size: 13px; -fx-text-fill: #4A6B8A;");
            }
        });
    }
}
