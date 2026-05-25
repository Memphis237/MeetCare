package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

public class DashboardController {
    @FXML private Label patientsCountLabel;
    @FXML private Label doctorsCountLabel;
    @FXML private Label confirmedCountLabel;
    @FXML private Label cancelledCountLabel;
    @FXML private Label todayCountLabel;
    @FXML private Label waitlistCountLabel;
    @FXML private VBox monthlyStatsContainer;
    @FXML private VBox activityContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("dashboard");
        bindSummary();
        renderMonthlyStats();
        renderActivity();
    }

    private void bindSummary() {
        AdminWorkspaceDao.DashboardSummary summary = adminWorkspaceDao.getDashboardSummary();
        patientsCountLabel.setText(String.valueOf(summary.totalPatients()));
        doctorsCountLabel.setText(String.valueOf(summary.totalDoctors()));
        confirmedCountLabel.setText(String.valueOf(summary.confirmedThisMonth()));
        cancelledCountLabel.setText(String.valueOf(summary.cancelledThisMonth()));
        todayCountLabel.setText(String.valueOf(summary.todayAppointments()));
        waitlistCountLabel.setText(String.valueOf(summary.waitlistCount()));
    }

    private void renderMonthlyStats() {
        monthlyStatsContainer.getChildren().clear();
        int max = adminWorkspaceDao.getMonthlyAppointmentStats().stream()
                .mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::total)
                .max()
                .orElse(1);

        for (AdminWorkspaceDao.MonthlyAppointmentStat stat : adminWorkspaceDao.getMonthlyAppointmentStats()) {
            VBox line = new VBox(8);
            Label monthLabel = new Label(stat.monthLabel());
            monthLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");

            HBox barRow = new HBox(0);
            barRow.setMinHeight(24);
            barRow.setPrefHeight(24);
            barRow.getStyleClass().add("admin-progress-track");

            Region confirmed = new Region();
            confirmed.getStyleClass().add("admin-progress-green");
            confirmed.setPrefWidth(Math.max(0, 840.0 * stat.confirmedCount() / max));

            Region cancelled = new Region();
            cancelled.getStyleClass().add("admin-progress-red");
            cancelled.setPrefWidth(Math.max(0, 840.0 * stat.cancelledCount() / max));

            barRow.getChildren().addAll(confirmed, cancelled);

            HBox info = new HBox(12);
            info.getChildren().addAll(
                    buildMutedLabel("Confirmés: " + stat.confirmedCount()),
                    buildMutedLabel("Annulés: " + stat.cancelledCount()),
                    buildMutedLabel(stat.total() + " RDV")
            );

            line.getChildren().addAll(monthLabel, barRow, info);
            monthlyStatsContainer.getChildren().add(line);
        }
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
            HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

            Label badge = new Label(activity.badge());
            badge.getStyleClass().addAll("admin-badge", "admin-badge-info");

            row.getChildren().addAll(textBox, badge);
            activityContainer.getChildren().add(row);
        }
    }

    private Label buildMutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-muted");
        return label;
    }
}
