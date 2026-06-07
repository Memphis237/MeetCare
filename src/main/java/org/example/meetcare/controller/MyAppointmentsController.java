package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MyAppointmentsController {
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private VBox appointmentsContainer;

    @FXML
    private Button confirmedTabButton;

    @FXML
    private Button completedTabButton;

    @FXML
    private Button cancelledTabButton;

    private String activeStatus = "CONFIRME";

    @FXML
    private void initialize() {
        bindTabActions();
        refreshView();
    }

    @FXML
    private void handleOpenBookingPage(ActionEvent event) {
        NavigationUtil.openPatientAppointmentBooking(event);
    }

    private void bindTabActions() {
        confirmedTabButton.setOnAction(event -> switchTab("CONFIRME"));
        completedTabButton.setOnAction(event -> switchTab("TERMINE"));
        cancelledTabButton.setOnAction(event -> switchTab("ANNULE"));
    }

    private void switchTab(String status) {
        activeStatus = status;
        refreshView();
    }

    private void refreshView() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUsers() <= 0) {
            renderAppointments(List.of());
            updateTabCounts(List.of());
            return;
        }

        List<Appointment> appointments = appointmentDao.findByUserId(currentUser.getIdUsers());
        updateTabCounts(appointments);
        updateTabStyles();
        renderAppointments(filterAppointments(appointments, activeStatus));
    }

    private void updateTabCounts(List<Appointment> appointments) {
        confirmedTabButton.setText("Confirmes (" + countByStatus(appointments, "CONFIRME") + ")");
        completedTabButton.setText("Termines (" + countByStatus(appointments, "TERMINE") + ")");
        cancelledTabButton.setText("Annules (" + countByStatus(appointments, "ANNULE") + ")");
    }

    private long countByStatus(List<Appointment> appointments, String status) {
        return appointments.stream().filter(appointment -> status.equals(appointment.getStatus())).count();
    }

    private void updateTabStyles() {
        applyTabStyle(confirmedTabButton, "CONFIRME".equals(activeStatus));
        applyTabStyle(completedTabButton, "TERMINE".equals(activeStatus));
        applyTabStyle(cancelledTabButton, "ANNULE".equals(activeStatus));
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

        if ("CONFIRME".equals(appointment.getStatus())) {
            Button completeButton = new Button("Terminer");
            completeButton.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 15px; -fx-padding: 10 20;");
            completeButton.setOnAction(event -> {
                appointmentDao.updateStatus(appointment.getId(), "TERMINE");
                refreshView();
            });

            Button cancelButton = new Button("Annuler");
            cancelButton.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: red; -fx-font-size: 13px; -fx-padding: 6 10;");
            cancelButton.setOnAction(event -> {
                appointmentDao.updateStatus(appointment.getId(), "ANNULE");
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
        if ("ANNULE".equals(status)) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c; -fx-background-radius: 10; -fx-padding: 5 12;";
        }
        if ("TERMINE".equals(status)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-background-radius: 10; -fx-padding: 5 12;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 10; -fx-padding: 5 12;";
    }

    private String getDisplayStatus(String status) {
        if ("ANNULE".equals(status)) {
            return "Annule";
        }
        if ("TERMINE".equals(status)) {
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
        return date == null ? "-" : date.format(DATE_FORMATTER);
    }
}
