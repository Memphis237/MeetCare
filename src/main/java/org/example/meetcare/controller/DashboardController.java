package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.DoctorAdminEntry;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {
    private final UsersDao usersDao = new UsersDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    @FXML
    private VBox contentContainer;

    private Label usersCountLabel;
    private Label doctorsCountLabel;
    private Label appointmentsCountLabel;
    private Label verifiedRateLabel;

    private BarChart<Number, String> specialityBarChart;
    private PieChart statusPieChart;
    private LineChart<String, Number> appointmentsLineChart;

    private List<Users> loadedUsers = List.of();
    private List<DoctorAdminEntry> loadedDoctors = List.of();
    private List<Appointment> loadedAppointments = List.of();

    @FXML
    private void initialize() {
        buildLayout();
        refreshData();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshData();
    }

    private void buildLayout() {
        if (contentContainer == null) {
            return;
        }

        contentContainer.getChildren().clear();
        contentContainer.setSpacing(24);
        contentContainer.setPadding(new Insets(0));
        contentContainer.getChildren().addAll(
                buildHeader(),
                buildKpiRow(),
                buildChartsGrid(),
                buildBottomSection()
        );
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(6);
        Label title = new Label("Tableau de bord admin");
        title.getStyleClass().add("admin-page-title");

        Label subtitle = new Label("Pilotage global de l'activité médicale et administrative");
        subtitle.getStyleClass().add("admin-page-subtitle");
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label liveLabel = new Label("Données synchronisées en base");
        liveLabel.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-background-radius: 999; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-font-weight: bold;");

        header.getChildren().addAll(texts, spacer, liveLabel);
        return header;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                createKpiCard("Utilisateurs", "0", "admin-stat-card-completed", "admin-stat-title-completed", "admin-stat-value-completed", "👥"),
                createKpiCard("Médecins", "0", "admin-stat-card-confirmed", "admin-stat-title-confirmed", "admin-stat-value-confirmed", "🩺"),
                createKpiCard("Rendez-vous", "0", "admin-stat-card-urgent", "admin-stat-title-urgent", "admin-stat-value-urgent", "📅"),
                createKpiCard("Taux validés", "0%", "admin-stat-card-cancelled", "admin-stat-title-cancelled", "admin-stat-value-cancelled", "✓")
        );
        return row;
    }

    private VBox createKpiCard(String titleText,
                               String initialValue,
                               String cardStyle,
                               String titleStyle,
                               String valueStyle,
                               String iconText) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("admin-stat-card", cardStyle);
        card.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 22px;");

        Label title = new Label(titleText);
        title.getStyleClass().addAll("admin-stat-title", titleStyle);

        Label value = new Label(initialValue);
        value.getStyleClass().addAll("admin-stat-value", valueStyle);

        switch (titleText) {
            case "Utilisateurs" -> usersCountLabel = value;
            case "Médecins" -> doctorsCountLabel = value;
            case "Rendez-vous" -> appointmentsCountLabel = value;
            case "Taux validés" -> verifiedRateLabel = value;
            default -> {
            }
        }

        card.getChildren().addAll(icon, title, value);
        return card;
    }

    private HBox buildChartsGrid() {
        HBox grid = new HBox(18);
        grid.setAlignment(Pos.CENTER_LEFT);

        VBox barCard = chartCard("Médecins par spécialité", buildSpecialityBarChart());
        VBox pieCard = chartCard("Répartition des statuts", buildStatusPieChart());
        VBox lineCard = chartCard("Rendez-vous mensuels", buildAppointmentsLineChart());

        barCard.setPrefWidth(360);
        pieCard.setPrefWidth(340);
        lineCard.setPrefWidth(360);

        grid.getChildren().addAll(barCard, pieCard, lineCard);
        HBox.setHgrow(barCard, Priority.ALWAYS);
        HBox.setHgrow(pieCard, Priority.ALWAYS);
        HBox.setHgrow(lineCard, Priority.ALWAYS);
        return grid;
    }

    private VBox buildBottomSection() {
        VBox section = new VBox(14);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Indicateurs clés");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                smallInfoCard("Validés", String.valueOf(verifiedDoctorsCount()), "#dcfce7", "#15803d"),
                smallInfoCard("En attente", String.valueOf(pendingDoctorsCount()), "#fef3c7", "#b45309"),
                smallInfoCard("Rejets", String.valueOf(rejectedDoctorsCount()), "#fee2e2", "#b91c1c"),
                smallInfoCard("Consultations", String.valueOf(totalAppointments()), "#dbeafe", "#1d4ed8")
        );

        section.getChildren().addAll(header, stats);
        return section;
    }

    private VBox smallInfoCard(String label, String value, String bgColor, String textColor) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, white, " + bgColor + "); -fx-background-radius: 16; -fx-padding: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 16;");
        Label title = new Label(label);
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Label amount = new Label(value);
        amount.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        card.getChildren().addAll(title, amount);
        return card;
    }

    private VBox chartCard(String titleText, javafx.scene.Node chart) {
        VBox card = new VBox(14);
        card.getStyleClass().add("admin-chart-card");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        card.getChildren().addAll(title, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return card;
    }

    private BarChart<Number, String> buildSpecialityBarChart() {
        NumberAxis xAxis = new NumberAxis();
        CategoryAxis yAxis = new CategoryAxis();
        specialityBarChart = new BarChart<>(xAxis, yAxis);
        specialityBarChart.setLegendVisible(false);
        specialityBarChart.setCategoryGap(14);
        specialityBarChart.setAnimated(false);
        specialityBarChart.setPrefHeight(260);
        specialityBarChart.setPrefWidth(340);
        refreshSpecialityChart();
        return specialityBarChart;
    }

    private PieChart buildStatusPieChart() {
        statusPieChart = new PieChart();
        statusPieChart.setPrefHeight(260);
        statusPieChart.setPrefWidth(320);
        statusPieChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        statusPieChart.setAnimated(false);
        refreshStatusPieChart();
        return statusPieChart;
    }

    private LineChart<String, Number> buildAppointmentsLineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        appointmentsLineChart = new LineChart<>(xAxis, yAxis);
        appointmentsLineChart.setCreateSymbols(true);
        appointmentsLineChart.setAnimated(false);
        appointmentsLineChart.setLegendVisible(false);
        appointmentsLineChart.setPrefHeight(260);
        refreshAppointmentsChart();
        return appointmentsLineChart;
    }

    private void refreshData() {
        loadedUsers = usersDao.findAllUsers();
        loadedDoctors = doctorDao.findAllAdminEntries();
        loadedAppointments = appointmentDao.findAllAppointments();

        if (usersCountLabel != null) {
            usersCountLabel.setText(String.valueOf(loadedUsers.size()));
        }
        if (doctorsCountLabel != null) {
            doctorsCountLabel.setText(String.valueOf(loadedDoctors.size()));
        }
        if (appointmentsCountLabel != null) {
            appointmentsCountLabel.setText(String.valueOf(loadedAppointments.size()));
        }
        if (verifiedRateLabel != null) {
            verifiedRateLabel.setText(computeVerifiedRate(loadedUsers) + "%");
        }

        refreshCharts();
    }

    private void refreshCharts() {
        refreshSpecialityChart();
        refreshStatusPieChart();
        refreshAppointmentsChart();
    }

    private void refreshSpecialityChart() {
        if (specialityBarChart == null) {
            return;
        }

        specialityBarChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        Map<String, Long> counts = loadedDoctors.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        doctor -> safeText(doctor.getSpeciality(), "Non précisé"),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ));

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey())));

        specialityBarChart.getData().add(series);
    }

    private void refreshStatusPieChart() {
        if (statusPieChart == null) {
            return;
        }

        statusPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Validés", verifiedDoctorsCount()),
                new PieChart.Data("En attente", pendingDoctorsCount()),
                new PieChart.Data("Refusés", rejectedDoctorsCount()),
                new PieChart.Data("Suspendus", suspendedDoctorsCount())
        ));
    }

    private void refreshAppointmentsChart() {
        if (appointmentsLineChart == null) {
            return;
        }

        appointmentsLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<YearMonth, Long> grouped = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        appointment -> YearMonth.from(appointment.getDate()),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ));

        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .skip(Math.max(0, grouped.size() - 6L))
                .forEach(entry -> series.getData().add(new XYChart.Data<>(labelMonth(entry.getKey()), entry.getValue())));

        appointmentsLineChart.getData().add(series);
    }

    private String computeVerifiedRate(List<Users> users) {
        if (users == null || users.isEmpty()) {
            return "0";
        }
        long verifiedDoctors = users.stream()
                .filter(user -> user.getRole() == Role.MEDECIN)
                .filter(user -> user.getAccountStatus() == AccountStatus.VERIFIED)
                .count();
        long totalDoctors = users.stream().filter(user -> user.getRole() == Role.MEDECIN).count();
        if (totalDoctors == 0) {
            return "0";
        }
        return String.valueOf(Math.round((verifiedDoctors * 100.0) / totalDoctors));
    }

    private long verifiedDoctorsCount() {
        return loadedUsers.stream()
                .filter(user -> user.getRole() == Role.MEDECIN)
                .filter(user -> user.getAccountStatus() == AccountStatus.VERIFIED)
                .count();
    }

    private long pendingDoctorsCount() {
        return loadedUsers.stream()
                .filter(user -> user.getRole() == Role.MEDECIN)
                .filter(user -> user.getAccountStatus() == AccountStatus.PENDING)
                .count();
    }

    private long rejectedDoctorsCount() {
        return loadedUsers.stream()
                .filter(user -> user.getRole() == Role.MEDECIN)
                .filter(user -> user.getAccountStatus() == AccountStatus.REJECTED)
                .count();
    }

    private long suspendedDoctorsCount() {
        return loadedUsers.stream()
                .filter(user -> user.getRole() == Role.MEDECIN)
                .filter(user -> user.getAccountStatus() == AccountStatus.SUSPENDED)
                .count();
    }

    private long totalAppointments() {
        return loadedAppointments.size();
    }

    private String labelMonth(YearMonth yearMonth) {
        return yearMonth == null
                ? "-"
                : yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + yearMonth.getYear();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
