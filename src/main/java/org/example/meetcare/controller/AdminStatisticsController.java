package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

import java.util.List;

public class AdminStatisticsController {
    @FXML private Label totalPatientsLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label occupancyRateLabel;
    @FXML private VBox monthlyStatsContainer;
    @FXML private Label confirmedShareLabel;
    @FXML private Label cancelledShareLabel;
    @FXML private Region confirmedShareBar;
    @FXML private Region cancelledShareBar;
    @FXML private Label patientGrowthLabel;
    @FXML private Label doctorGrowthLabel;
    @FXML private Label appointmentsGrowthLabel;
    @FXML private Region patientGrowthBar;
    @FXML private Region doctorGrowthBar;
    @FXML private Region appointmentsGrowthBar;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("statistics");
        bindSummary();
        renderMonthlyStats();
        bindBreakdowns();
    }

    private void bindSummary() {
        AdminWorkspaceDao.DashboardSummary summary = adminWorkspaceDao.getDashboardSummary();
        List<AdminWorkspaceDao.MonthlyAppointmentStat> monthlyStats = adminWorkspaceDao.getMonthlyAppointmentStats();
        int totalAppointments = monthlyStats.stream().mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::total).sum();
        int totalConfirmed = monthlyStats.stream().mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::confirmedCount).sum();
        int totalCancelled = monthlyStats.stream().mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::cancelledCount).sum();
        int totalSlots = Math.max(totalConfirmed + totalCancelled, 1);
        int occupancyRate = (int) Math.round((totalConfirmed * 100.0) / totalSlots);

        totalPatientsLabel.setText(String.valueOf(summary.totalPatients()));
        totalDoctorsLabel.setText(String.valueOf(summary.totalDoctors()));
        totalAppointmentsLabel.setText(String.valueOf(totalAppointments));
        occupancyRateLabel.setText(occupancyRate + "%");
    }

    private void renderMonthlyStats() {
        monthlyStatsContainer.getChildren().clear();
        int max = adminWorkspaceDao.getMonthlyAppointmentStats().stream()
                .mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::total)
                .max()
                .orElse(1);
        for (AdminWorkspaceDao.MonthlyAppointmentStat stat : adminWorkspaceDao.getMonthlyAppointmentStats()) {
            VBox line = new VBox(8);
            Label month = new Label(stat.monthLabel());
            month.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");
            HBox bars = new HBox(0);
            bars.setMinHeight(24);
            bars.getStyleClass().add("admin-progress-track");

            Region confirmed = new Region();
            confirmed.getStyleClass().add("admin-progress-green");
            confirmed.setPrefWidth(Math.max(0, 820.0 * stat.confirmedCount() / max));
            Region cancelled = new Region();
            cancelled.getStyleClass().add("admin-progress-red");
            cancelled.setPrefWidth(Math.max(0, 820.0 * stat.cancelledCount() / max));
            bars.getChildren().addAll(confirmed, cancelled);

            HBox legend = new HBox(12,
                    buildMutedLabel("Confirmés: " + stat.confirmedCount()),
                    buildMutedLabel("Annulés: " + stat.cancelledCount()),
                    buildMutedLabel(stat.total() + " RDV"));
            line.getChildren().addAll(month, bars, legend);
            monthlyStatsContainer.getChildren().add(line);
        }
    }

    private void bindBreakdowns() {
        List<AdminWorkspaceDao.MonthlyAppointmentStat> monthlyStats = adminWorkspaceDao.getMonthlyAppointmentStats();
        int totalConfirmed = monthlyStats.stream().mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::confirmedCount).sum();
        int totalCancelled = monthlyStats.stream().mapToInt(AdminWorkspaceDao.MonthlyAppointmentStat::cancelledCount).sum();
        int total = Math.max(totalConfirmed + totalCancelled, 1);

        confirmedShareLabel.setText(totalConfirmed + " (" + Math.round(totalConfirmed * 100.0 / total) + "%)");
        cancelledShareLabel.setText(totalCancelled + " (" + Math.round(totalCancelled * 100.0 / total) + "%)");
        confirmedShareBar.setPrefWidth(380.0 * totalConfirmed / total);
        cancelledShareBar.setPrefWidth(380.0 * totalCancelled / total);

        AdminWorkspaceDao.DashboardSummary summary = adminWorkspaceDao.getDashboardSummary();
        patientGrowthLabel.setText("+" + summary.totalPatients());
        doctorGrowthLabel.setText("+" + summary.totalDoctors());
        appointmentsGrowthLabel.setText("+" + (summary.confirmedThisMonth() + summary.cancelledThisMonth()));
        patientGrowthBar.setPrefWidth(Math.min(380, summary.totalPatients() * 10.0));
        doctorGrowthBar.setPrefWidth(Math.min(380, summary.totalDoctors() * 10.0));
        appointmentsGrowthBar.setPrefWidth(Math.min(380, (summary.confirmedThisMonth() + summary.cancelledThisMonth()) * 10.0));
    }

    private Label buildMutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-muted");
        return label;
    }
}
