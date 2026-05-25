package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminAppointmentsController {
    @FXML private TextField searchField;
    @FXML private DatePicker dateFilterPicker;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label appointmentsCountLabel;
    @FXML private VBox appointmentsContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("appointments");
        statusFilterComboBox.getItems().setAll("Tous les statuts", "Confirme", "Annule", "Termine");
        statusFilterComboBox.setValue("Tous les statuts");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshAppointments());
        dateFilterPicker.valueProperty().addListener((obs, oldValue, newValue) -> refreshAppointments());
        statusFilterComboBox.setOnAction(event -> refreshAppointments());
        refreshAppointments();
    }

    private void refreshAppointments() {
        String query = normalize(searchField.getText());
        String status = statusFilterComboBox.getValue();
        LocalDate date = dateFilterPicker.getValue();

        List<AdminWorkspaceDao.AdminAppointmentRecord> appointments = adminWorkspaceDao.getAppointments().stream()
                .filter(appointment -> query.isBlank()
                        || normalize(appointment.patientName()).contains(query)
                        || normalize(appointment.doctorName()).contains(query))
                .filter(appointment -> date == null || appointment.date().equals(formatDate(date)))
                .filter(appointment -> "Tous les statuts".equals(status) || appointment.status().equalsIgnoreCase(status))
                .toList();

        appointmentsCountLabel.setText(appointments.size() + (appointments.size() > 1 ? " rendez-vous" : " rendez-vous"));
        appointmentsContainer.getChildren().clear();
        for (AdminWorkspaceDao.AdminAppointmentRecord appointment : appointments) {
            appointmentsContainer.getChildren().add(buildAppointmentCard(appointment));
        }
    }

    private VBox buildAppointmentCard(AdminWorkspaceDao.AdminAppointmentRecord appointment) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("admin-list-item");

        HBox row = new HBox(18);
        VBox details = new VBox(6);
        Label title = new Label(appointment.date() + " à " + appointment.time());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        Label status = new Label(appointment.status());
        status.getStyleClass().addAll("admin-badge", badgeForStatus(appointment.status()));
        HBox head = new HBox(10, title, status);
        Label patient = new Label("Patient: " + appointment.patientName());
        patient.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");
        Label doctor = new Label("Médecin: " + appointment.doctorName() + " (" + appointment.speciality() + ")");
        doctor.getStyleClass().add("admin-muted");
        Label purpose = new Label("Motif: " + appointment.purpose());
        purpose.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");
        details.getChildren().addAll(head, patient, doctor, purpose);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button viewButton = buildButton("Voir détails", "admin-secondary-button");
        viewButton.setOnAction(event -> showDetails(appointment));
        Button editButton = buildButton("Modifier", "admin-secondary-button");
        editButton.setOnAction(event -> editAppointment(appointment));
        actions.getChildren().addAll(viewButton, editButton);
        if (!"Annule".equalsIgnoreCase(appointment.status())) {
            Button cancelButton = buildButton("Annuler", "admin-danger-button");
            cancelButton.setOnAction(event -> {
                adminWorkspaceDao.cancelAppointment(appointment.id());
                refreshAppointments();
            });
            actions.getChildren().add(cancelButton);
        }

        row.getChildren().addAll(details, actions);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private void showDetails(AdminWorkspaceDao.AdminAppointmentRecord appointment) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du rendez-vous");
        alert.setHeaderText(appointment.patientName() + " • " + appointment.date() + " " + appointment.time());
        alert.setContentText(
                "Médecin: " + appointment.doctorName() + "\n" +
                "Spécialité: " + appointment.speciality() + "\n" +
                "Motif: " + appointment.purpose() + "\n" +
                "Statut: " + appointment.status()
        );
        alert.showAndWait();
    }

    private void editAppointment(AdminWorkspaceDao.AdminAppointmentRecord appointment) {
        List<String> statuses = List.of("CONFIRME", "ANNULE", "TERMINE");
        String nextStatus = switch (appointment.status()) {
            case "Confirme" -> "TERMINE";
            case "Termine" -> "CONFIRME";
            default -> "CONFIRME";
        };
        adminWorkspaceDao.updateAppointment(
                appointment.id(),
                parseDate(appointment.date()),
                appointment.time(),
                statuses.contains(nextStatus) ? nextStatus : "CONFIRME",
                appointment.purpose()
        );
        refreshAppointments();
    }

    private Button buildButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private String badgeForStatus(String status) {
        return switch (status.toLowerCase(Locale.FRENCH)) {
            case "annule" -> "admin-badge-danger";
            case "termine" -> "admin-badge-info";
            default -> "admin-badge-active";
        };
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.FRENCH).trim();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private LocalDate parseDate(String date) {
        String[] parts = Optional.ofNullable(date).orElse("01/01/2026").split("/");
        return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
    }
}
