package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.NavigationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MedecinAppointmentsController {
    private static final DateTimeFormatter RANGE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AppointmentDao appointmentDao = new AppointmentDao();

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label rangeLabel;

    @FXML
    private Label weekAppointmentsCountLabel;

    @FXML
    private Label weekAppointmentsMetaLabel;

    @FXML
    private Label confirmedCountLabel;

    @FXML
    private Label confirmedMetaLabel;

    @FXML
    private Label completedCountLabel;

    @FXML
    private Label completedMetaLabel;

    @FXML
    private Label teleconsultationsCountLabel;

    @FXML
    private Label teleconsultationsMetaLabel;

    @FXML
    private VBox appointmentsContainer;

    @FXML
    private Button todayFilterButton;

    @FXML
    private Button allFilterButton;

    @FXML
    private Button confirmedFilterButton;

    @FXML
    private Button completedFilterButton;

    @FXML
    private Button cancelledFilterButton;

    @FXML
    private Button teleconsultationFilterButton;

    private String activeFilter = "ALL";
    private List<Appointment> weekAppointments = List.of();

    @FXML
    private void initialize() {
        refreshView();
    }

    @FXML
    private void handleTodayFilter(ActionEvent event) {
        activeFilter = "TODAY";
        renderAppointments();
    }

    @FXML
    private void handleAllFilter(ActionEvent event) {
        activeFilter = "ALL";
        renderAppointments();
    }

    @FXML
    private void handleConfirmedFilter(ActionEvent event) {
        activeFilter = "CONFIRME";
        renderAppointments();
    }

    @FXML
    private void handleCompletedFilter(ActionEvent event) {
        activeFilter = "TERMINE";
        renderAppointments();
    }

    @FXML
    private void handleCancelledFilter(ActionEvent event) {
        activeFilter = "ANNULE";
        renderAppointments();
    }

    @FXML
    private void handleTeleconsultationFilter(ActionEvent event) {
        activeFilter = "TELECONSULTATION";
        renderAppointments();
    }

    private void refreshView() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUsers() <= 0) {
            weekAppointments = List.of();
            updateStats();
            renderAppointments();
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);

        subtitleLabel.setText("Agenda du " + today.format(RANGE_FORMATTER) + " au " + weekEnd.format(RANGE_FORMATTER));
        rangeLabel.setText("Du " + today.format(RANGE_FORMATTER) + " au " + weekEnd.format(RANGE_FORMATTER));

        weekAppointments = appointmentDao.findByDoctorUserId(currentUser.getIdUsers()).stream()
                .filter(appointment -> appointment.getDate() != null
                        && !appointment.getDate().isBefore(today)
                        && !appointment.getDate().isAfter(weekEnd))
                .sorted(Comparator.comparing(Appointment::getDate)
                        .thenComparing(MedecinAppointmentsController::appointmentTimeAsLocalTime))
                .collect(Collectors.toList());

        updateStats();
        renderAppointments();
    }

    private void updateStats() {
        long confirmed = countByStatus("CONFIRME");
        long completed = countByStatus("TERMINE");
        long teleconsultations = weekAppointments.stream().filter(Appointment::isTeleconsultation).count();

        weekAppointmentsCountLabel.setText(String.valueOf(weekAppointments.size()));
        weekAppointmentsMetaLabel.setText(weekAppointments.isEmpty() ? "Aucun rendez-vous cette semaine" : "Consultations planifiees");
        confirmedCountLabel.setText(String.valueOf(confirmed));
        confirmedMetaLabel.setText("Consultations valides");
        completedCountLabel.setText(String.valueOf(completed));
        completedMetaLabel.setText("Rendez-vous clotures");
        teleconsultationsCountLabel.setText(String.valueOf(teleconsultations));
        teleconsultationsMetaLabel.setText("A distance");
        updateFilterStyles();
    }

    private void renderAppointments() {
        appointmentsContainer.getChildren().clear();

        List<Appointment> filtered = filterAppointments();
        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 18;");
            Label title = new Label("Aucun rendez-vous pour ce filtre");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Label subtitle = new Label("Change de filtre pour voir les autres rendez-vous de la semaine.");
            subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            emptyState.getChildren().addAll(title, subtitle);
            appointmentsContainer.getChildren().add(emptyState);
            return;
        }

        for (Appointment appointment : filtered) {
            appointmentsContainer.getChildren().add(createAppointmentCard(appointment));
        }
    }

    private HBox createAppointmentCard(Appointment appointment) {
        HBox card = new HBox(16);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 16;");

        VBox slot = new VBox(4);
        slot.setMinWidth(86);
        Label day = new Label(formatShortDate(appointment.getDate()));
        day.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label time = new Label(formatTime(appointment.getTime()));
        time.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        slot.getChildren().addAll(day, time);

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label patient = new Label(safeText(appointment.getPatientName()));
        patient.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label speciality = new Label(safeText(appointment.getSpeciality()));
        speciality.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Label details = new Label(safeText(appointment.getDetails()));
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        details.setWrapText(true);
        info.getChildren().addAll(patient, speciality, details);

        VBox right = new VBox(8);
        right.setMinWidth(190);
        Label status = new Label(getDisplayStatus(appointment.getStatus()));
        status.setStyle(getStatusStyle(appointment.getStatus()));
        Label location = new Label(appointment.isTeleconsultation() ? "Téléconsultation" : safeText(appointment.getAddress()));
        location.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        location.setWrapText(true);
        Button detailsButton = new Button("Voir détails");
        detailsButton.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-background-radius: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 14;");
        detailsButton.setOnAction(event -> showAppointmentDetails(appointment));
        right.getChildren().addAll(status, location, detailsButton);

        card.getChildren().addAll(slot, info, right);
        return card;
    }

    private void showAppointmentDetails(Appointment appointment) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du rendez-vous");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 18; -fx-background-color: white;");

        Label title = new Label(safeText(appointment.getPatientName()));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lines = new Label(
                "Date: " + formatDate(appointment.getDate()) + "\n" +
                "Heure: " + formatTime(appointment.getTime()) + "\n" +
                "Statut: " + getDisplayStatus(appointment.getStatus()) + "\n" +
                "Type: " + (appointment.isTeleconsultation() ? "Téléconsultation" : "Cabinet") + "\n" +
                "Motif: " + safeText(appointment.getDetails()) + "\n" +
                "Lieu: " + safeText(appointment.getAddress())
        );
        lines.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 4;");
        lines.setWrapText(true);

        content.getChildren().addAll(title, lines);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private List<Appointment> filterAppointments() {
        LocalDate today = LocalDate.now();
        return weekAppointments.stream()
                .filter(appointment -> switch (activeFilter) {
                    case "TODAY" -> appointment.getDate() != null && appointment.getDate().isEqual(today);
                    case "CONFIRME", "TERMINE", "ANNULE" -> activeFilter.equalsIgnoreCase(safeStatus(appointment));
                    case "TELECONSULTATION" -> appointment.isTeleconsultation();
                    default -> true;
                })
                .collect(Collectors.toList());
    }

    private long countByStatus(String status) {
        return weekAppointments.stream().filter(appointment -> status.equalsIgnoreCase(safeStatus(appointment))).count();
    }

    private void updateFilterStyles() {
        applyFilterStyle(allFilterButton, "ALL");
        applyFilterStyle(todayFilterButton, "TODAY");
        applyFilterStyle(confirmedFilterButton, "CONFIRME");
        applyFilterStyle(completedFilterButton, "TERMINE");
        applyFilterStyle(cancelledFilterButton, "ANNULE");
        applyFilterStyle(teleconsultationFilterButton, "TELECONSULTATION");
    }

    private void applyFilterStyle(Button button, String filter) {
        if (button == null) {
            return;
        }
        boolean active = filter.equals(activeFilter);
        button.setStyle(active
                ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 999; -fx-border-color: #bfdbfe; -fx-border-radius: 999; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-background-color: white; -fx-text-fill: #334155; -fx-background-radius: 999; -fx-border-color: #d1d5db; -fx-border-radius: 999; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private static LocalTime appointmentTimeAsLocalTime(Appointment appointment) {
        try {
            return appointment == null || appointment.getTime() == null ? LocalTime.MIN : LocalTime.parse(appointment.getTime());
        } catch (Exception ex) {
            return LocalTime.MIN;
        }
    }

    private String safeStatus(Appointment appointment) {
        return appointment == null || appointment.getStatus() == null ? "" : appointment.getStatus().trim();
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "-" : date.format(RANGE_FORMATTER);
    }

    private String formatTime(String time) {
        return time == null || time.isBlank() ? "-" : time;
    }

    private String getStatusStyle(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private String getDisplayStatus(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "Annule";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "Termine";
        }
        return "Confirme";
    }
}
