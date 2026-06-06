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
        if (active) {
            button.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-font-size: 13px; -fx-padding: 6 10;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-font-size: 13px; -fx-padding: 6 10;");
        }
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
            emptyState.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 14;");

            Label title = new Label("Aucune consultation dans cette categorie");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            Label subtitle = new Label("Change d'onglet pour voir les autres rendez-vous.");
            subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 14;");

        Label avatar = new Label(getInitial(appointment.getDoctorName()));
        avatar.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center; -fx-min-width: 60; -fx-min-height: 60; -fx-background-radius: 30;");

        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label name = new Label(appointment.getDoctorName());
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label speciality = new Label(appointment.getSpeciality());
        speciality.setStyle("-fx-font-size: 15px; -fx-text-fill: #4b5563;");

        Label date = new Label("Date: " + formatDate(appointment.getDate()));
        date.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        Label time = new Label("Heure: " + appointment.getTime());
        time.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        String locationText = appointment.isTeleconsultation()
                ? "Teleconsultation disponible - " + appointment.getAddress()
                : appointment.getAddress();
        Label location = new Label(locationText);
        location.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        Label details = new Label(appointment.getDetails());
        details.setStyle("-fx-font-size: 14px;");

        infoBox.getChildren().addAll(name, speciality, date, time, location, details);

        VBox actions = new VBox(6);
        Label status = new Label(getDisplayStatus(appointment.getStatus()));
        status.setStyle(getStatusStyle(appointment.getStatus()));

        actions.getChildren().add(status);

        if (AppointmentStore.STATUS_CONFIRMED.equals(appointment.getStatus())) {
            Button completeButton = new Button("Terminer");
            completeButton.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 15px; -fx-padding: 10 20;");
            completeButton.setOnAction(event -> {
                AppointmentStore.updateStatus(appointment.getId(), AppointmentStore.STATUS_COMPLETED);
                refreshView();
            });

            Button cancelButton = new Button("Annuler");
            cancelButton.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: red; -fx-font-size: 13px; -fx-padding: 6 10;");
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

    private String getStatusStyle(String status) {
        if (AppointmentStore.STATUS_CANCELLED.equals(status)) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c; -fx-background-radius: 10; -fx-padding: 5 12;";
        }
        if (AppointmentStore.STATUS_COMPLETED.equals(status)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-background-radius: 10; -fx-padding: 5 12;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 10; -fx-padding: 5 12;";
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
