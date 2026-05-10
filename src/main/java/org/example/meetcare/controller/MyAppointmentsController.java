package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.utils.AppointmentStore;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MyAppointmentsController {
    @FXML
    private VBox appointmentsContainer;

    @FXML
    private Button confirmedTabButton;

    @FXML
    private Button completedTabButton;

    @FXML
    private Button cancelledTabButton;

    private String activeStatus = AppointmentStore.STATUS_CONFIRMED;

    @FXML
    private void initialize() {
        bindTabActions();
        refreshView();
    }

    private void bindTabActions() {
        confirmedTabButton.setOnAction(event -> switchTab(AppointmentStore.STATUS_CONFIRMED));
        completedTabButton.setOnAction(event -> switchTab(AppointmentStore.STATUS_COMPLETED));
        cancelledTabButton.setOnAction(event -> switchTab(AppointmentStore.STATUS_CANCELLED));
    }

    private void switchTab(String status) {
        activeStatus = status;
        refreshView();
    }

    private void refreshView() {
        List<Appointment> appointments = AppointmentStore.getAppointments();
        updateTabCounts(appointments);
        updateTabStyles();
        renderAppointments(filterAppointments(appointments, activeStatus));
    }

    private void updateTabCounts(List<Appointment> appointments) {
        confirmedTabButton.setText("Confirmes (" + countByStatus(appointments, AppointmentStore.STATUS_CONFIRMED) + ")");
        completedTabButton.setText("Termines (" + countByStatus(appointments, AppointmentStore.STATUS_COMPLETED) + ")");
        cancelledTabButton.setText("Annules (" + countByStatus(appointments, AppointmentStore.STATUS_CANCELLED) + ")");
    }

    private long countByStatus(List<Appointment> appointments, String status) {
        return appointments.stream().filter(appointment -> status.equals(appointment.getStatus())).count();
    }

    private void updateTabStyles() {
        applyTabStyle(confirmedTabButton, AppointmentStore.STATUS_CONFIRMED.equals(activeStatus));
        applyTabStyle(completedTabButton, AppointmentStore.STATUS_COMPLETED.equals(activeStatus));
        applyTabStyle(cancelledTabButton, AppointmentStore.STATUS_CANCELLED.equals(activeStatus));
    }

    private void applyTabStyle(Button button, boolean active) {
        button.getStyleClass().removeAll("tab-active", "tab-button");
        button.getStyleClass().add(active ? "tab-active" : "tab-button");
    }

    private List<Appointment> filterAppointments(List<Appointment> appointments, String status) {
        return appointments.stream()
                .filter(appointment -> status.equals(appointment.getStatus()))
                .collect(Collectors.toList());
    }

    private void renderAppointments(List<Appointment> appointments) {
        appointmentsContainer.getChildren().clear();

        if (appointments.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("appointment-card");

            Label title = new Label("Aucune consultation dans cette categorie");
            title.getStyleClass().add("doctor-name");

            Label subtitle = new Label("Change d'onglet pour voir les autres rendez-vous.");
            subtitle.getStyleClass().add("doctor-info");

            emptyState.getChildren().addAll(title, subtitle);
            appointmentsContainer.getChildren().add(emptyState);
            return;
        }

        for (Appointment appointment : appointments) {
            appointmentsContainer.getChildren().add(createAppointmentCard(appointment));
        }
    }

    private HBox createAppointmentCard(Appointment appointment) {
        HBox card = new HBox(14);
        card.getStyleClass().add("appointment-card");

        Label avatar = new Label(getInitial(appointment.getDoctorName()));
        avatar.getStyleClass().add("avatar");

        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label name = new Label(appointment.getDoctorName());
        name.getStyleClass().add("doctor-name");

        Label speciality = new Label(appointment.getSpeciality());
        speciality.getStyleClass().add("doctor-speciality");

        Label date = new Label("Date: " + formatDate(appointment.getDate()));
        date.getStyleClass().add("doctor-info");

        Label time = new Label("Heure: " + appointment.getTime());
        time.getStyleClass().add("doctor-info");

        String locationText = appointment.isTeleconsultation()
                ? "Teleconsultation disponible - " + appointment.getAddress()
                : appointment.getAddress();
        Label location = new Label(locationText);
        location.getStyleClass().add("doctor-info");

        Label details = new Label(appointment.getDetails());
        details.setStyle("-fx-font-size: 14px;");

        infoBox.getChildren().addAll(name, speciality, date, time, location, details);

        VBox actions = new VBox(6);
        Label status = new Label(getDisplayStatus(appointment.getStatus()));
        status.getStyleClass().add(getStatusStyleClass(appointment.getStatus()));

        actions.getChildren().add(status);

        if (AppointmentStore.STATUS_CONFIRMED.equals(appointment.getStatus())) {
            Button completeButton = new Button("Terminer");
            completeButton.getStyleClass().add("secondary-button");
            completeButton.setOnAction(event -> {
                AppointmentStore.updateStatus(appointment.getId(), AppointmentStore.STATUS_COMPLETED);
                refreshView();
            });

            Button cancelButton = new Button("Annuler");
            cancelButton.getStyleClass().add("danger-button");
            cancelButton.setOnAction(event -> {
                AppointmentStore.updateStatus(appointment.getId(), AppointmentStore.STATUS_CANCELLED);
                refreshView();
            });

            actions.getChildren().addAll(completeButton, cancelButton);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        actions.getChildren().add(spacer);

        card.getChildren().addAll(avatar, infoBox, actions);
        return card;
    }

    private String getStatusStyleClass(String status) {
        if (AppointmentStore.STATUS_CANCELLED.equals(status)) {
            return "badge-orange";
        }
        if (AppointmentStore.STATUS_COMPLETED.equals(status)) {
            return "badge-blue";
        }
        return "badge-green";
    }

    private String getDisplayStatus(String status) {
        if (AppointmentStore.STATUS_CANCELLED.equals(status)) {
            return "Annule";
        }
        if (AppointmentStore.STATUS_COMPLETED.equals(status)) {
            return "Termine";
        }
        return "Confirme";
    }

    private String getInitial(String doctorName) {
        if (doctorName == null || doctorName.length() < 5) {
            return "D";
        }
        return doctorName.substring(4, 5).toUpperCase(Locale.ROOT);
    }

    private String formatDate(LocalDate date) {
        return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear();
    }
}
