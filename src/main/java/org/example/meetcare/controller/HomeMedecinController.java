package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;

import java.time.LocalDate;
import java.util.List;

public class HomeMedecinController {
    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label todayAppointmentsValueLabel;
    @FXML private Label weekAppointmentsValueLabel;
    @FXML private Label patientsValueLabel;
    @FXML private Label documentsValueLabel;
    @FXML private VBox todayAppointmentsContainer;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        DoctorWorkspaceDao.DoctorSummary summary = doctorWorkspaceDao.getDoctorSummary();
        welcomeLabel.setText("Bienvenue, " + summary.displayName());
        subtitleLabel.setText(summary.speciality() + " - Vue d'ensemble de votre activite medicale");

        List<DoctorWorkspaceDao.AppointmentRecord> appointments = doctorWorkspaceDao.getAppointments();
        List<DoctorWorkspaceDao.PatientRecord> patients = doctorWorkspaceDao.getPatients();
        List<DoctorWorkspaceDao.DocumentRecord> documents = doctorWorkspaceDao.getDocuments();

        long todayCount = appointments.stream().filter(a -> a.date().equals(LocalDate.now())).count();
        long weekCount = appointments.stream()
                .filter(a -> !a.date().isBefore(LocalDate.now()) && !a.date().isAfter(LocalDate.now().plusDays(7)))
                .count();

        todayAppointmentsValueLabel.setText(String.valueOf(todayCount));
        weekAppointmentsValueLabel.setText(String.valueOf(weekCount));
        patientsValueLabel.setText(String.valueOf(patients.size()));
        documentsValueLabel.setText(String.valueOf(documents.size()));

        todayAppointmentsContainer.getChildren().clear();
        List<DoctorWorkspaceDao.AppointmentRecord> todayAppointments = appointments.stream()
                .filter(a -> a.date().equals(LocalDate.now()))
                .toList();

        if (todayAppointments.isEmpty()) {
            VBox emptyCard = new VBox(6);
            emptyCard.getStyleClass().add("doctor-card");
            Label title = new Label("Aucun rendez-vous aujourd'hui");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label subtitle = new Label("Ouvrez des disponibilites pour commencer a recevoir des reservations.");
            subtitle.getStyleClass().add("doctor-subtitle");
            emptyCard.getChildren().addAll(title, subtitle);
            todayAppointmentsContainer.getChildren().add(emptyCard);
            return;
        }

        todayAppointments.forEach(appointment -> {
            VBox card = new VBox(6);
            card.getStyleClass().add("doctor-card");

            Label title = new Label(appointment.time() + " - " + appointment.patientName());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            Label subtitle = new Label(appointment.purpose() + " - " + appointment.mode());
            subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

            Label status = new Label(appointment.status());
            status.getStyleClass().add(switch (appointment.status()) {
                case "En attente" -> "doctor-badge-orange";
                case "Annule" -> "doctor-badge-neutral";
                default -> "doctor-badge-green";
            });

            card.getChildren().addAll(title, subtitle, status);
            todayAppointmentsContainer.getChildren().add(card);
        });
    }
}
