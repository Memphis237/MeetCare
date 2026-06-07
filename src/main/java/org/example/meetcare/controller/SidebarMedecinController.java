package org.example.meetcare.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.dao.DoctorDocumentDao;
import org.example.meetcare.model.AccountStatus;
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
    private HBox invoicesItem;

    @FXML
    private HBox messagesItem;

    @FXML
    private HBox assistantItem;

    @FXML
    private HBox notificationsItem;

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
        roleLabel.setText(roleToLabel(user));
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
        applyRestrictedState(user.isDoctorPending());
        updateValidationBadge(user);
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
        setInactive(homeItem, searchItem, appointmentsItem, documentsItem, invoicesItem, messagesItem, assistantItem, notificationsItem, validationItem, settingsItem, logoutItem);

        HBox activeItem = switch (route == null ? "home" : route) {
            case "search" -> searchItem;
            case "appointments" -> appointmentsItem;
            case "documents" -> documentsItem;
            case "invoices" -> invoicesItem;
            case "messages" -> messagesItem;
            case "assistant" -> assistantItem;
            case "validation" -> validationItem;
            case "settings" -> settingsItem;
            default -> homeItem;
        };

        setActive(activeItem);
    }

    private void applyRestrictedState(boolean restricted) {
        double restrictedOpacity = restricted ? 0.45 : 1.0;
        for (HBox item : new HBox[]{homeItem, searchItem, appointmentsItem, documentsItem, invoicesItem, messagesItem, assistantItem, notificationsItem}) {
            if (item != null) {
                item.setOpacity(restrictedOpacity);
            }
        }
        if (validationItem != null) {
            validationItem.setOpacity(1.0);
        }
        if (settingsItem != null) {
            settingsItem.setOpacity(1.0);
        }
    }

    private void updateValidationBadge(Users user) {
        if (validationBadgeContainer == null || validationBadgeLabel == null || user == null) {
            return;
        }

        int count = doctorDocumentDao.countRejectedDocumentsForDoctorUserId(user.getIdUsers());
        boolean show = count > 0;
        validationBadgeContainer.setVisible(show);
        validationBadgeContainer.setManaged(show);
        validationBadgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    @FXML
    private void handleHome(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinHome(event);
    }

    @FXML
    private void handleSearchDoctor(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinPatients(event);
    }

    @FXML
    private void handleAppointments(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinAgenda(event);
    }

    @FXML
    private void handlePatients(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinPatients(event);
    }

    @FXML
    private void handleAvailability(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinAvailability(event);
    }

    @FXML
    private void handleDocuments(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinDocuments(event);
    }

    @FXML
    private void handleInvoices(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinInvoices(event);
    }

    @FXML
    private void handleMessages(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinMessages(event);
    }

    @FXML
    private void handleAssistant(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinAssistantMessages(event);
    }

    @FXML
    private void handleStats(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
        NavigationUtil.openMedecinStats(event);
    }

    @FXML
    private void handlePublicProfile(MouseEvent event) {
        if (blockIfPending()) {
            return;
        }
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

    private String roleToLabel(Users user) {
        String base = roleToLabel(user == null ? null : user.getRole());
        if (user != null && user.getRole() == Role.MEDECIN) {
            if (user.getAccountStatus() == AccountStatus.PENDING) {
                return base + " - En attente";
            }
            if (user.getAccountStatus() == AccountStatus.VERIFIED) {
                return base + " - Valide";
            }
            if (user.getAccountStatus() == AccountStatus.REJECTED) {
                return base + " - Refuse";
            }
        }
        return base;
    }

    private boolean blockIfPending() {
        Users current = AppSession.getCurrentUser();
        if (current != null && current.isDoctorPending()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Compte en attente");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez attendre que votre compte soit valide par l'administrateur.");
            alert.showAndWait();
            return true;
        }
        return false;
    }

    private void setInactive(HBox... items) {
        for (HBox item : items) {
            if (item == null) {
                continue;
            }
            item.setStyle("-fx-padding: 10 14 10 14; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    private void setActive(HBox item) {
        if (item == null) {
            return;
        }
        item.setStyle("-fx-padding: 10 14 10 14; -fx-background-color: #D6E9F8; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}
