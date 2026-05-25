package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

public class AdminNotificationsController {
    @FXML private ComboBox<String> filterComboBox;
    @FXML private VBox notificationsContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("notifications");
        filterComboBox.getItems().setAll("Toutes", "Non lues", "Lues");
        filterComboBox.setValue("Toutes");
        filterComboBox.setOnAction(event -> refreshNotifications());
        refreshNotifications();
    }

    private void refreshNotifications() {
        String filter = filterComboBox.getValue();
        var notifications = adminWorkspaceDao.getNotifications().stream()
                .filter(notification -> "Toutes".equals(filter)
                        || ("Non lues".equals(filter) && !notification.isRead())
                        || ("Lues".equals(filter) && notification.isRead()))
                .toList();
        notificationsContainer.getChildren().clear();
        for (AdminWorkspaceDao.NotificationRecord notification : notifications) {
            VBox card = new VBox();
            card.getStyleClass().add("admin-list-item");
            HBox row = new HBox(16);
            VBox textBox = new VBox(5);
            Label title = new Label(notification.title());
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #111827;");
            Label message = new Label(notification.message());
            message.setWrapText(true);
            message.getStyleClass().add("admin-muted");
            Label meta = new Label(notification.targetUser() + " • " + notification.dateTime());
            meta.getStyleClass().add("admin-muted");
            textBox.getChildren().addAll(title, message, meta);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Button toggle = new Button(notification.isRead() ? "Marquer non lue" : "Marquer lue");
            toggle.getStyleClass().add(notification.isRead() ? "admin-secondary-button" : "admin-primary-button");
            toggle.setOnAction(event -> {
                adminWorkspaceDao.markNotificationRead(notification.id(), !notification.isRead());
                refreshNotifications();
            });
            row.getChildren().addAll(textBox, toggle);
            card.getChildren().add(row);
            notificationsContainer.getChildren().add(card);
        }
    }
}
