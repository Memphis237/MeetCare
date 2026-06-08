package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsAdminController {
    private final UsersDao usersDao = new UsersDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    @FXML
    private VBox contentContainer;

    private Label usersCountLabel;
    private Label doctorsCountLabel;
    private Label appointmentsCountLabel;
    private Label verifiedRateLabel;

    private Label validatedDoctorsLabel;
    private Label pendingDoctorsLabel;
    private Label rejectedDoctorsLabel;
    private Label totalConsultationsLabel;

    private LineChart<String, Number> monthlyAppointmentsLineChart;
    private PieChart statusPieChart;
    private BarChart<Number, String> specialityBarChart;
    private PieChart doctorStatusPieChart;

    private List<Users> loadedUsers = List.of();
    private List<DoctorAdminEntry> loadedDoctors = List.of();
    private List<Appointment> loadedAppointments = List.of();

    @FXML
    private void initialize() {
        buildLayout();
        refreshData();
    }

    @FXML
    private void handleRefresh(javafx.event.ActionEvent event) {
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
                buildFooterInsights());
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #ffffff, #f6f3ff);"
                        + "-fx-background-radius: 22;"
                        + "-fx-border-color: #e3dcf7;"
                        + "-fx-border-radius: 22;"
                        + "-fx-padding: 22 24 22 24;"
                        + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 14, 0, 0, 2);");

        VBox texts = new VBox(6);
        Label title = new Label("Statistiques admin");
        title.getStyleClass().add("admin-page-title");

        Label subtitle = new Label("Vue analytique large pour suivre la tendance generale de la plateforme");
        subtitle.getStyleClass().add("admin-page-subtitle");
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label liveLabel = new Label("Analyse long terme");
        liveLabel.setStyle(
                "-fx-background-color: #f5f3ff; -fx-text-fill: #6d28d9; -fx-background-radius: 999; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-font-weight: bold;");

        header.getChildren().addAll(texts, spacer, liveLabel);
        return header;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                createKpiCard("Utilisateurs", "0", "admin-stat-card-completed", "admin-stat-title-completed",
                        "admin-stat-value-completed", "👥"),
                createKpiCard("Médecins", "0", "admin-stat-card-confirmed", "admin-stat-title-confirmed",
                        "admin-stat-value-confirmed", "🩺"),
                createKpiCard("Rendez-vous", "0", "admin-stat-card-urgent", "admin-stat-title-urgent",
                        "admin-stat-value-urgent", "📅"),
                createKpiCard("Taux validés", "0%", "admin-stat-card-cancelled", "admin-stat-title-cancelled",
                        "admin-stat-value-cancelled", "✓"));
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

    private GridPane buildChartsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);

        VBox monthlyCard = chartCard("Rendez-vous mensuels", "Evolution sur les 12 derniers mois",
                buildMonthlyAppointmentsChart());
        VBox statusCard = chartCard("Statuts des rendez-vous", "Repartition globale des rendez-vous",
                buildStatusPieChart());
        VBox specialityCard = chartCard("Médecins par spécialité", "Top 6 des spécialités les plus representées",
                buildSpecialityBarChart());
        VBox doctorStatusCard = chartCard("Etat des médecins", "Validation des comptes professionnels",
                buildDoctorStatusPieChart());

        monthlyCard.setPrefHeight(340);
        statusCard.setPrefHeight(340);
        specialityCard.setPrefHeight(340);
        doctorStatusCard.setPrefHeight(340);

        GridPane.setHgrow(monthlyCard, Priority.ALWAYS);
        GridPane.setHgrow(statusCard, Priority.ALWAYS);
        GridPane.setHgrow(specialityCard, Priority.ALWAYS);
        GridPane.setHgrow(doctorStatusCard, Priority.ALWAYS);

        grid.add(monthlyCard, 0, 0);
        grid.add(statusCard, 1, 0);
        grid.add(specialityCard, 0, 1);
        grid.add(doctorStatusCard, 1, 1);

        return grid;
    }

    private VBox buildFooterInsights() {
        VBox section = new VBox(14);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Indicateurs complementaires");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                smallInfoCard("Validés", String.valueOf(verifiedDoctorsCount()), "#dcfce7", "#15803d"),
                smallInfoCard("En attente", String.valueOf(pendingDoctorsCount()), "#fef3c7", "#b45309"),
                smallInfoCard("Refusés", String.valueOf(rejectedDoctorsCount()), "#fee2e2", "#b91c1c"),
                smallInfoCard("Consultations", String.valueOf(totalAppointments()), "#dbeafe", "#1d4ed8"));

        section.getChildren().addAll(header, stats);
        return section;
    }

    private VBox smallInfoCard(String label, String value, String bgColor, String textColor) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, white, " + bgColor
                + "); -fx-background-radius: 16; -fx-padding: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 16;");
        Label title = new Label(label);
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Label amount = new Label(value);
        amount.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        switch (label) {
            case "Validés" -> validatedDoctorsLabel = amount;
            case "En attente" -> pendingDoctorsLabel = amount;
            case "Refusés" -> rejectedDoctorsLabel = amount;
            case "Consultations" -> totalConsultationsLabel = amount;
        }

        card.getChildren().addAll(title, amount);
        return card;
    }

    private VBox chartCard(String titleText, String subtitleText, Node chart) {
        VBox card = new VBox(12);
        card.getStyleClass().add("admin-chart-card");

        VBox texts = new VBox(3);
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        texts.getChildren().addAll(title, subtitle);

        card.getChildren().addAll(texts, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        return card;
    }

    private LineChart<String, Number> buildMonthlyAppointmentsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mois");
        yAxis.setLabel("Rendez-vous");
        monthlyAppointmentsLineChart = new LineChart<>(xAxis, yAxis);
        monthlyAppointmentsLineChart.setCreateSymbols(true);
        monthlyAppointmentsLineChart.setAnimated(true);
        monthlyAppointmentsLineChart.setLegendVisible(false);
        monthlyAppointmentsLineChart.setPrefHeight(260);
        refreshMonthlyAppointmentsChart();
        return monthlyAppointmentsLineChart;
    }

    private PieChart buildStatusPieChart() {
        statusPieChart = new PieChart();
        statusPieChart.setPrefHeight(260);
        statusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        statusPieChart.setAnimated(true);
        refreshStatusPieChart();
        return statusPieChart;
    }

    private BarChart<Number, String> buildSpecialityBarChart() {
        NumberAxis xAxis = new NumberAxis();
        CategoryAxis yAxis = new CategoryAxis();
        xAxis.setLabel("Nombre de médecins");
        yAxis.setLabel("Spécialité");
        specialityBarChart = new BarChart<>(xAxis, yAxis);
        specialityBarChart.setLegendVisible(false);
        specialityBarChart.setAnimated(true);
        specialityBarChart.setHorizontalGridLinesVisible(true);
        specialityBarChart.setVerticalGridLinesVisible(false);
        specialityBarChart.setCategoryGap(14);
        specialityBarChart.setBarGap(6);
        specialityBarChart.setPrefHeight(260);
        refreshSpecialityChart();
        return specialityBarChart;
    }

    private PieChart buildDoctorStatusPieChart() {
        doctorStatusPieChart = new PieChart();
        doctorStatusPieChart.setPrefHeight(260);
        doctorStatusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        doctorStatusPieChart.setAnimated(true);
        refreshDoctorStatusPieChart();
        return doctorStatusPieChart;
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

        if (validatedDoctorsLabel != null) {
            validatedDoctorsLabel.setText(String.valueOf(verifiedDoctorsCount()));
        }
        if (pendingDoctorsLabel != null) {
            pendingDoctorsLabel.setText(String.valueOf(pendingDoctorsCount()));
        }
        if (rejectedDoctorsLabel != null) {
            rejectedDoctorsLabel.setText(String.valueOf(rejectedDoctorsCount()));
        }
        if (totalConsultationsLabel != null) {
            totalConsultationsLabel.setText(String.valueOf(totalAppointments()));
        }

        refreshCharts();
    }

    private void refreshCharts() {
        refreshMonthlyAppointmentsChart();
        refreshStatusPieChart();
        refreshSpecialityChart();
        refreshDoctorStatusPieChart();
    }

    private void refreshMonthlyAppointmentsChart() {
        if (monthlyAppointmentsLineChart == null) {
            return;
        }

        monthlyAppointmentsLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<YearMonth, Long> grouped = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .collect(Collectors.groupingBy(
                        appointment -> YearMonth.from(appointment.getDate()),
                        LinkedHashMap::new,
                        Collectors.counting()));

        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .skip(Math.max(0, grouped.size() - 12))
                .forEach(entry -> series.getData()
                        .add(new XYChart.Data<>(labelMonth(entry.getKey()), entry.getValue())));

        monthlyAppointmentsLineChart.getData().add(series);
    }

    private void refreshStatusPieChart() {
        if (statusPieChart == null) {
            return;
        }

        long total = loadedAppointments.size();
        statusPieChart.setData(FXCollections.observableArrayList(
                createPieData("Confirmé", confirmedAppointmentsCount(), total),
                createPieData("En attente", pendingAppointmentsCount(), total),
                createPieData("Annulé", cancelledAppointmentsCount(), total)));
    }

    private void refreshDoctorStatusPieChart() {
        if (doctorStatusPieChart == null) {
            return;
        }

        doctorStatusPieChart.setData(FXCollections.observableArrayList(
                createPieData("Validés", verifiedDoctorsCount(), loadedUsers.stream().filter(user -> user.getRole() == Role.MEDECIN).count()),
                createPieData("En attente", pendingDoctorsCount(), loadedUsers.stream().filter(user -> user.getRole() == Role.MEDECIN).count()),
                createPieData("Refusés", rejectedDoctorsCount(), loadedUsers.stream().filter(user -> user.getRole() == Role.MEDECIN).count())));
    }

    private PieChart.Data createPieData(String name, long count, long total) {
        double percentage = (total == 0) ? 0 : (count * 100.0) / total;
        String label = String.format("%s : %d (%.1f%%)", name, count, percentage);
        return new PieChart.Data(label, count);
    }

    private void refreshSpecialityChart() {
        if (specialityBarChart == null) {
            return;
        }

        specialityBarChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        Map<String, Long> counts = loadedDoctors.stream()
                .collect(Collectors.groupingBy(
                        doctor -> safeText(doctor.getSpeciality(), "Non précisé"),
                        LinkedHashMap::new,
                        Collectors.counting()));

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey())));

        specialityBarChart.getData().add(series);
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

    private long confirmedAppointmentsCount() {
        return loadedAppointments.stream()
                .filter(appointment -> "CONFIRME".equalsIgnoreCase(appointment.getStatus()))
                .count();
    }

    private long pendingAppointmentsCount() {
        return loadedAppointments.stream()
                .filter(appointment -> {
                    String status = appointment.getStatus();
                    return status != null
                            && ("PENDING".equalsIgnoreCase(status) || "EN_ATTENTE".equalsIgnoreCase(status));
                })
                .count();
    }

    private long cancelledAppointmentsCount() {
        return loadedAppointments.stream()
                .filter(appointment -> {
                    String status = appointment.getStatus();
                    return status != null
                            && ("ANNULE".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status));
                })
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
