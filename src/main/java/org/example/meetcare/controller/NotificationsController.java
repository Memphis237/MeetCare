package org.example.meetcare.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.meetcare.dao.NotificationDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.NotificationEntry;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.NavigationUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationsController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationDao notificationDao = new NotificationDao();
    private final UsersDao usersDao = new UsersDao();

    @FXML
    private VBox notificationsContainer;

    @FXML
    private Label unreadCountLabel;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Button allFilterButton;

    @FXML
    private Button unreadFilterButton;

    @FXML
    private SidebarPatientController sidebarPatientController;

    @FXML
    private SidebarMedecinController sidebarMedecinController;

    private Timeline refreshTimeline;
    private boolean unreadOnly;

    @FXML
    private void initialize() {
        refreshView();
        startAutoRefresh();
        Platform.runLater(() -> refreshSidebars());
    }

    @FXML
    private void handleAllNotifications() {
        unreadOnly = false;
        refreshView();
    }

    @FXML
    private void handleUnreadNotifications() {
        unreadOnly = true;
        refreshView();
    }

    @FXML
    private void handleMarkAllAsRead() {
        Users currentUser = currentUser();
        if (currentUser == null) {
            return;
        }

        notificationDao.markAllAsRead(currentUser.getIdUsers());
        refreshView();
    }

    @FXML
    private void handleRefresh() {
        refreshView();
    }

    private void refreshView() {
        Users currentUser = currentUser();
        if (currentUser == null) {
            renderEmptyState("Session indisponible", "Connecte-toi pour voir tes notifications.");
            updateCounters(0, 0);
            return;
        }

        List<NotificationEntry> notifications = notificationDao.findNotificationsForUser(currentUser.getIdUsers(), unreadOnly);
        int unreadCount = notificationDao.countUnreadForUser(currentUser.getIdUsers());
        int totalCount = notificationDao.findNotificationsForUser(currentUser.getIdUsers(), false).size();

        updateCounters(unreadCount, totalCount);
        refreshSidebars();
        updateFilterStyles();

        notificationsContainer.getChildren().clear();
        if (notifications.isEmpty()) {
            renderEmptyState(unreadOnly ? "Aucune notification non lue" : "Aucune notification", unreadOnly
                    ? "Tu es a jour. Les nouvelles alertes s'afficheront ici."
                    : "Les notifications du systeme et de la messagerie apparaissent ici.");
            return;
        }

        for (NotificationEntry notification : notifications) {
            notificationsContainer.getChildren().add(createNotificationCard(notification));
        }
    }

    private HBox createNotificationCard(NotificationEntry notification) {
        boolean unread = !notification.isRead();
        HBox card = new HBox(14);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(unread
                ? "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #dbeafe; -fx-border-radius: 18; -fx-padding: 18; -fx-effect: dropshadow(gaussian, rgba(37,99,235,0.10), 14, 0, 0, 3); -fx-cursor: hand;"
                : "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #e5e7eb; -fx-border-radius: 18; -fx-padding: 18; -fx-cursor: hand;");
        card.setOnMouseClicked(event -> handleNotificationClick(notification));

        VBox marker = new VBox();
        marker.setMinWidth(10);
        marker.setPrefWidth(10);
        marker.setStyle(unread ? "-fx-background-color: #2563eb; -fx-background-radius: 999;" : "-fx-background-color: transparent;");

        VBox content = new VBox(8);
        content.setFillWidth(true);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safeText(notification.getTitle(), "Notification"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label type = new Label(normalizeType(notification.getType()));
        type.setStyle(typeStyle(notification.getType()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label time = new Label(formatDate(notification.getCreatedAt()));
        time.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        header.getChildren().addAll(title, type, spacer, time);

        Label message = new Label(safeText(notification.getMessage(), ""));
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label status = new Label(unread ? "Non lue" : "Lue");
        status.setStyle(unread
                ? "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-background-radius: 999; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 999; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label openHint = new Label(unread ? "Cliquer pour ouvrir et marquer comme lu" : "Cliquer pour naviguer");
        openHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        footer.getChildren().addAll(status, openHint);

        content.getChildren().addAll(header, message, footer);
        card.getChildren().addAll(marker, content);
        return card;
    }

    private void handleNotificationClick(NotificationEntry notification) {
        if (notification == null) {
            return;
        }

        Users currentUser = currentUser();
        if (currentUser == null) {
            return;
        }

        notificationDao.markAsRead(notification.getIdNotification(), currentUser.getIdUsers());
        refreshView();

        String type = safeText(notification.getType(), "").toUpperCase();
        if ("MESSAGE".equals(type)) {
            openMessages();
        } else if ("APPOINTMENT".equals(type)) {
            openAppointments();
        }
    }

    private void openMessages() {
        Users currentUser = currentUser();
        if (currentUser == null) {
            return;
        }

        if (currentUser.getRole() == Role.MEDECIN) {
            NavigationUtil.openMedecinMessages(notificationsContainer);
        } else {
            NavigationUtil.openPatientMessages(notificationsContainer);
        }
    }

    private void openAppointments() {
        Users currentUser = currentUser();
        if (currentUser == null) {
            return;
        }

        if (currentUser.getRole() == Role.MEDECIN) {
            NavigationUtil.openMedecinAgenda(notificationsContainer);
        } else {
            NavigationUtil.openPatientAppointments(notificationsContainer);
        }
    }

    private void renderEmptyState(String titleText, String subtitleText) {
        notificationsContainer.getChildren().clear();

        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #e5e7eb; -fx-border-radius: 18; -fx-padding: 28;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        empty.getChildren().addAll(title, subtitle);
        notificationsContainer.getChildren().add(empty);
    }

    private void updateCounters(int unreadCount, int totalCount) {
        if (unreadCountLabel != null) {
            unreadCountLabel.setText(String.valueOf(unreadCount));
        }
        if (totalCountLabel != null) {
            totalCountLabel.setText(String.valueOf(totalCount));
        }
    }

    private void updateFilterStyles() {
        if (allFilterButton != null) {
            allFilterButton.setStyle(filterButtonStyle(!unreadOnly));
        }
        if (unreadFilterButton != null) {
            unreadFilterButton.setStyle(filterButtonStyle(unreadOnly));
        }
    }

    private String filterButtonStyle(boolean active) {
        return active
                ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 10 16 10 16; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;"
                : "-fx-background-color: white; -fx-text-fill: #334155; -fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 10 16 10 16; -fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: #dbe5ef; -fx-cursor: hand;";
    }

    private String typeStyle(String type) {
        String value = safeText(type, "SYSTEM").toUpperCase();
        return switch (value) {
            case "MESSAGE" -> "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-background-radius: 999; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "APPOINTMENT" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-background-radius: 999; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "PAYMENT" -> "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 999; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            default -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 999; -fx-padding: 5 10 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
        };
    }

    private String normalizeType(String type) {
        String value = safeText(type, "SYSTEM").toUpperCase();
        return switch (value) {
            case "MESSAGE" -> "Message";
            case "APPOINTMENT" -> "Rendez-vous";
            case "PAYMENT" -> "Paiement";
            default -> "Systeme";
        };
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return DATE_FORMATTER.format(dateTime);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Users currentUser() {
        Users sessionUser = AppSession.getCurrentUser();
        if (sessionUser == null || sessionUser.getIdUsers() <= 0) {
            return null;
        }
        Users freshUser = usersDao.findById(sessionUser.getIdUsers()).orElse(sessionUser);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }

    private void refreshSidebars() {
        Users currentUser = AppSession.getCurrentUser();
        if (sidebarPatientController != null) {
            sidebarPatientController.refreshSidebar(currentUser);
        }
        if (sidebarMedecinController != null) {
            sidebarMedecinController.refreshSidebar(currentUser);
        }
    }

    private void startAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refreshView()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.playFromStart();

        if (notificationsContainer == null) {
            return;
        }

        notificationsContainer.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }

            newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> {
                if (newWindow instanceof Window) {
                    Window window = (Window) newWindow;
                    window.setOnHidden(event -> stopAutoRefresh());
                }
            });
        });
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}
