package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
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
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DashboardController {
    private final UsersDao usersDao = new UsersDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    @FXML
    private VBox contentContainer;

    private Label appointmentsTodayLabel;
    private Label appointmentsWeekLabel;
    private Label newUsersWeekLabel;
    private Label verifiedDoctorsLabel;

    private Label urgentAppointmentsLabel;
    private Label teleconsultationLabel;
    private Label pendingDoctorsLabel;
    private Label activeDoctorsLabel;

    private LineChart<String, Number> dailyAppointmentsLineChart;
    private BarChart<String, Number> newUsersBarChart;
    private PieChart weeklyStatusPieChart;
    private BarChart<String, Number> urgencyBarChart;

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
                buildFooterInsights());
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("admin-dashboard-hero");

        VBox texts = new VBox(6);
        Label title = new Label("Dashboard admin");
        title.getStyleClass().add("admin-page-title");

        Label subtitle = new Label("Vue rapide sur les 7 derniers jours pour suivre la dynamique de l'app.");
        subtitle.getStyleClass().add("admin-page-subtitle");
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label liveLabel = new Label("Focus semaine");
        liveLabel.getStyleClass().add("admin-dashboard-chip");

        header.getChildren().addAll(texts, spacer, liveLabel);
        return header;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                createKpiCard("RDV aujourd'hui", "0", "dashboard-kpi-blue", "dashboard-kpi-value-blue", "◦"),
                createKpiCard("RDV 7 jours", "0", "dashboard-kpi-teal", "dashboard-kpi-value-teal", "↗"),
                createKpiCard("Nouveaux comptes", "0", "dashboard-kpi-amber", "dashboard-kpi-value-amber", "+"),
                createKpiCard("Médecins validés", "0", "dashboard-kpi-green", "dashboard-kpi-value-green", "✓"));
        return row;
    }

    private VBox createKpiCard(String titleText,
            String initialValue,
            String cardStyle,
            String valueStyle,
            String iconText) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("admin-stat-card", "dashboard-kpi-card", cardStyle);
        card.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.getStyleClass().add("dashboard-kpi-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("admin-stat-title");

        Label value = new Label(initialValue);
        value.getStyleClass().addAll("admin-stat-value", valueStyle);

        switch (titleText) {
            case "RDV aujourd'hui" -> appointmentsTodayLabel = value;
            case "RDV 7 jours" -> appointmentsWeekLabel = value;
            case "Nouveaux comptes" -> newUsersWeekLabel = value;
            case "Médecins validés" -> verifiedDoctorsLabel = value;
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

        VBox appointmentsCard = chartCard("Rendez-vous quotidiens", "Evolution jour par jour sur la semaine en cours",
                buildDailyAppointmentsChart());
        VBox usersCard = chartCard("Nouveaux comptes", "Inscriptions sur les 7 derniers jours",
                buildNewUsersChart());
        VBox statusCard = chartCard("Statut de la semaine", "Repartition des rendez-vous sur 7 jours",
                buildWeeklyStatusPieChart());
        VBox urgencyCard = chartCard("Niveau d'urgence", "Volume des rendez-vous selon leur urgence",
                buildUrgencyChart());

        appointmentsCard.setPrefHeight(340);
        usersCard.setPrefHeight(340);
        statusCard.setPrefHeight(340);
        urgencyCard.setPrefHeight(340);

        GridPane.setHgrow(appointmentsCard, Priority.ALWAYS);
        GridPane.setHgrow(usersCard, Priority.ALWAYS);
        GridPane.setHgrow(statusCard, Priority.ALWAYS);
        GridPane.setHgrow(urgencyCard, Priority.ALWAYS);

        grid.add(appointmentsCard, 0, 0);
        grid.add(usersCard, 1, 0);
        grid.add(statusCard, 0, 1);
        grid.add(urgencyCard, 1, 1);

        return grid;
    }

    private VBox buildFooterInsights() {
        VBox section = new VBox(14);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Signaux rapides");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
                smallInfoCard("Urgents", String.valueOf(urgentAppointmentsCount()), "#ecfeff", "#0f766e"),
                smallInfoCard("Téléconsultations", String.valueOf(teleconsultationsCount()), "#eff6ff", "#1d4ed8"),
                smallInfoCard("Médecins en attente", String.valueOf(pendingDoctorsCount()), "#fef3c7", "#b45309"),
                smallInfoCard("Actifs", String.valueOf(activeDoctorsCount()), "#dcfce7", "#15803d"));

        section.getChildren().addAll(header, stats);
        return section;
    }

    private VBox smallInfoCard(String label, String value, String bgColor, String textColor) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, white, " + bgColor
                + "); -fx-background-radius: 16; -fx-padding: 18; -fx-border-color: #dbe5ef; -fx-border-radius: 16;");
        Label title = new Label(label);
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Label amount = new Label(value);
        amount.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        card.getChildren().addAll(title, amount);
        return card;
    }

    private VBox chartCard(String titleText, String subtitleText, Node chart) {
        VBox card = new VBox(12);
        card.getStyleClass().add("admin-dashboard-chart-card");

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

    private LineChart<String, Number> buildDailyAppointmentsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jours");
        yAxis.setLabel("Rendez-vous");
        dailyAppointmentsLineChart = new LineChart<>(xAxis, yAxis);
        dailyAppointmentsLineChart.setCreateSymbols(true);
        dailyAppointmentsLineChart.setAnimated(true);
        dailyAppointmentsLineChart.setLegendVisible(false);
        dailyAppointmentsLineChart.setPrefHeight(260);
        refreshDailyAppointmentsChart();
        return dailyAppointmentsLineChart;
    }

    private BarChart<String, Number> buildNewUsersChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jours");
        yAxis.setLabel("Comptes");
        newUsersBarChart = new BarChart<>(xAxis, yAxis);
        newUsersBarChart.setLegendVisible(false);
        newUsersBarChart.setAnimated(true);
        newUsersBarChart.setCategoryGap(14);
        newUsersBarChart.setBarGap(6);
        newUsersBarChart.setPrefHeight(260);
        refreshNewUsersChart();
        return newUsersBarChart;
    }

    private PieChart buildWeeklyStatusPieChart() {
        weeklyStatusPieChart = new PieChart();
        weeklyStatusPieChart.setPrefHeight(260);
        weeklyStatusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        weeklyStatusPieChart.setAnimated(true);
        refreshWeeklyStatusChart();
        return weeklyStatusPieChart;
    }

    private BarChart<String, Number> buildUrgencyChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Niveau");
        yAxis.setLabel("Rendez-vous");
        urgencyBarChart = new BarChart<>(xAxis, yAxis);
        urgencyBarChart.setLegendVisible(false);
        urgencyBarChart.setAnimated(true);
        urgencyBarChart.setCategoryGap(16);
        urgencyBarChart.setBarGap(8);
        urgencyBarChart.setPrefHeight(260);
        refreshUrgencyChart();
        return urgencyBarChart;
    }

    private void refreshData() {
        loadedUsers = usersDao.findAllUsers();
        loadedDoctors = doctorDao.findAllAdminEntries();
        loadedAppointments = appointmentDao.findAllAppointments();

        if (appointmentsTodayLabel != null) {
            appointmentsTodayLabel.setText(String.valueOf(appointmentsTodayCount()));
        }
        if (appointmentsWeekLabel != null) {
            appointmentsWeekLabel.setText(String.valueOf(appointmentsLastSevenDaysCount()));
        }
        if (newUsersWeekLabel != null) {
            newUsersWeekLabel.setText(String.valueOf(newUsersLastSevenDaysCount()));
        }
        if (verifiedDoctorsLabel != null) {
            verifiedDoctorsLabel.setText(String.valueOf(verifiedDoctorsCount()));
        }

        if (urgentAppointmentsLabel != null) {
            urgentAppointmentsLabel.setText(String.valueOf(urgentAppointmentsCount()));
        }
        if (teleconsultationLabel != null) {
            teleconsultationLabel.setText(String.valueOf(teleconsultationsCount()));
        }
        if (pendingDoctorsLabel != null) {
            pendingDoctorsLabel.setText(String.valueOf(pendingDoctorsCount()));
        }
        if (activeDoctorsLabel != null) {
            activeDoctorsLabel.setText(String.valueOf(activeDoctorsCount()));
        }

        refreshCharts();
    }

    private void refreshCharts() {
        refreshDailyAppointmentsChart();
        refreshNewUsersChart();
        refreshWeeklyStatusChart();
        refreshUrgencyChart();
    }

    private void refreshDailyAppointmentsChart() {
        if (dailyAppointmentsLineChart == null) {
            return;
        }

        dailyAppointmentsLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<LocalDate, Long> grouped = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        Appointment::getDate,
                        LinkedHashMap::new,
                        Collectors.counting()));

        for (LocalDate day : lastSevenDays()) {
            series.getData().add(new XYChart.Data<>(dayLabel(day), grouped.getOrDefault(day, 0L)));
        }

        dailyAppointmentsLineChart.getData().add(series);
    }

    private void refreshNewUsersChart() {
        if (newUsersBarChart == null) {
            return;
        }

        newUsersBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<LocalDate, Long> grouped = loadedUsers.stream()
                .filter(user -> user.getDate() != null)
                .map(user -> user.getDate().toLocalDate())
                .filter(date -> !date.isBefore(recentStartDate()))
                .filter(date -> !date.isAfter(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        date -> date,
                        LinkedHashMap::new,
                        Collectors.counting()));

        for (LocalDate day : lastSevenDays()) {
            series.getData().add(new XYChart.Data<>(dayLabel(day), grouped.getOrDefault(day, 0L)));
        }

        newUsersBarChart.getData().add(series);
    }

    private void refreshWeeklyStatusChart() {
        if (weeklyStatusPieChart == null) {
            return;
        }

        long confirmed = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .filter(appointment -> "CONFIRME".equalsIgnoreCase(appointment.getStatus()))
                .count();
        long pending = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .filter(appointment -> {
                    String status = appointment.getStatus();
                    return status != null && ("PENDING".equalsIgnoreCase(status) || "EN_ATTENTE".equalsIgnoreCase(status));
                })
                .count();
        long cancelled = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .filter(appointment -> {
                    String status = appointment.getStatus();
                    return status != null && ("ANNULE".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status));
                })
                .count();
        long total = confirmed + pending + cancelled;

        if (total == 0) {
            weeklyStatusPieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Aucune donnée", 1)));
            return;
        }

        weeklyStatusPieChart.setData(FXCollections.observableArrayList(
                createPieData("Confirmés", confirmed, total),
                createPieData("En attente", pending, total),
                createPieData("Annulés", cancelled, total)));
    }

    private void refreshUrgencyChart() {
        if (urgencyBarChart == null) {
            return;
        }

        urgencyBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Long> counts = loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .collect(Collectors.groupingBy(
                        appointment -> normalizeUrgency(appointment.getUrgency()),
                        LinkedHashMap::new,
                        Collectors.counting()));

        List<String> order = List.of("NORMAL", "URGENT", "ELEVEE", "CRITIQUE");
        for (String urgency : order) {
            String label = switch (urgency) {
                case "URGENT" -> "Urgent";
                case "ELEVEE" -> "Elevée";
                case "CRITIQUE" -> "Critique";
                default -> "Normal";
            };
            series.getData().add(new XYChart.Data<>(label, counts.getOrDefault(urgency, 0L)));
        }

        urgencyBarChart.getData().add(series);
    }

    private PieChart.Data createPieData(String name, long count, long total) {
        double percentage = total == 0 ? 0 : (count * 100.0) / total;
        String label = String.format("%s : %d (%.1f%%)", name, count, percentage);
        return new PieChart.Data(label, count);
    }

    private long appointmentsTodayCount() {
        LocalDate today = LocalDate.now();
        return loadedAppointments.stream()
                .filter(appointment -> today.equals(appointment.getDate()))
                .count();
    }

    private long appointmentsLastSevenDaysCount() {
        LocalDate start = recentStartDate();
        LocalDate today = LocalDate.now();
        return loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(start))
                .filter(appointment -> !appointment.getDate().isAfter(today))
                .count();
    }

    private long newUsersLastSevenDaysCount() {
        LocalDate start = recentStartDate();
        LocalDate today = LocalDate.now();
        return loadedUsers.stream()
                .filter(user -> user.getDate() != null)
                .map(user -> user.getDate().toLocalDate())
                .filter(date -> !date.isBefore(start))
                .filter(date -> !date.isAfter(today))
                .count();
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

    private long activeDoctorsCount() {
        return loadedDoctors.stream()
                .filter(doctor -> doctor.getDoctorId() > 0)
                .count();
    }

    private long urgentAppointmentsCount() {
        return loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .filter(appointment -> {
                    String urgency = normalizeUrgency(appointment.getUrgency());
                    return "URGENT".equals(urgency) || "ELEVEE".equals(urgency) || "CRITIQUE".equals(urgency);
                })
                .count();
    }

    private long teleconsultationsCount() {
        return loadedAppointments.stream()
                .filter(appointment -> appointment.getDate() != null)
                .filter(appointment -> !appointment.getDate().isBefore(recentStartDate()))
                .filter(appointment -> !appointment.getDate().isAfter(LocalDate.now()))
                .filter(Appointment::isTeleconsultation)
                .count();
    }

    private List<LocalDate> lastSevenDays() {
        LocalDate today = LocalDate.now();
        return IntStream.rangeClosed(6, 0)
                .mapToObj(today::minusDays)
                .toList();
    }

    private LocalDate recentStartDate() {
        return LocalDate.now().minusDays(6);
    }

    private String dayLabel(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + date.getDayOfMonth();
    }

    private String normalizeUrgency(String urgency) {
        if (urgency == null || urgency.isBlank()) {
            return "NORMAL";
        }

        String normalized = urgency.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NORMAL", "URGENT", "ELEVEE", "CRITIQUE" -> normalized;
            default -> "NORMAL";
        };
    }
}
