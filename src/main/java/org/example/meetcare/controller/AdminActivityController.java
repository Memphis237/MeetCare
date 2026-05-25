package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

public class AdminActivityController {
    @FXML private VBox activityContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("activity");
        renderActivity();
    }

    private void renderActivity() {
        activityContainer.getChildren().clear();
        for (AdminWorkspaceDao.ActivityRecord activity : adminWorkspaceDao.getRecentActivity()) {
            HBox row = new HBox(16);
            row.getStyleClass().add("admin-list-item");
            VBox textBox = new VBox(5);
            Label title = new Label(activity.title());
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #111827;");
            Label subtitle = new Label(activity.subtitle());
            subtitle.getStyleClass().add("admin-muted");
            Label date = new Label(activity.dateTime());
            date.getStyleClass().add("admin-muted");
            textBox.getChildren().addAll(title, subtitle, date);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Label badge = new Label(activity.badge());
            badge.getStyleClass().addAll("admin-badge", "admin-badge-info");
            row.getChildren().addAll(textBox, badge);
            activityContainer.getChildren().add(row);
        }
    }
}
