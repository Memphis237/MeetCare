package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.model.AvailabilitySlot;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StatistiquesMedecinController {
    @FXML private Label totalPatientsValueLabel;
    @FXML private Label totalAppointmentsValueLabel;
    @FXML private Label fillRateValueLabel;
    @FXML private Label currentYearLabel;
    @FXML private VBox monthlyAppointmentsContainer;
    @FXML private Label confirmedSummaryLabel;
    @FXML private Label cancelledSummaryLabel;
    @FXML private Label newPatientsGrowthLabel;
    @FXML private Label newAppointmentsGrowthLabel;
    @FXML private Region confirmedSummaryBar;
    @FXML private Region cancelledSummaryBar;
    @FXML private Region newPatientsGrowthBar;
    @FXML private Region newAppointmentsGrowthBar;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        List<DoctorWorkspaceDao.AppointmentRecord> appointments = doctorWorkspaceDao.getAppointments();
        List<DoctorWorkspaceDao.PatientRecord> patients = doctorWorkspaceDao.getPatients();
        List<AvailabilitySlot> slots = doctorWorkspaceDao.getAvailabilitySlots(null);

        totalPatientsValueLabel.setText(String.valueOf(patients.size()));
        totalAppointmentsValueLabel.setText(String.valueOf(appointments.size()));

        long bookedSlots = slots.stream().filter(AvailabilitySlot::isBooked).count();
        double fillRate = slots.isEmpty() ? 0 : (bookedSlots * 100.0 / slots.size());
        fillRateValueLabel.setText(Math.round(fillRate) + "%");
        currentYearLabel.setText("Cette annee");

        renderMonthlyAppointments(appointments);
        renderSummaryBlocks(appointments, patients);
    }

    private void renderMonthlyAppointments(List<DoctorWorkspaceDao.AppointmentRecord> appointments) {
        monthlyAppointmentsContainer.getChildren().clear();

        List<MonthMetrics> metrics = buildMonthMetrics(appointments);
        int maxTotal = metrics.stream().map(MonthMetrics::total).max(Comparator.naturalOrder()).orElse(1);

        for (MonthMetrics metric : metrics) {
            VBox block = new VBox(6);

            HBox titleRow = new HBox();
            Label monthLabel = new Label(metric.monthName());
            monthLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label totalLabel = new Label(metric.total() + " RDV");
            totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            titleRow.getChildren().addAll(monthLabel, spacer, totalLabel);

            HBox bar = new HBox(2);
            bar.setPrefHeight(24);

            Region confirmedBar = new Region();
            confirmedBar.setStyle("-fx-background-color: #16a34a; -fx-background-radius: 6 0 0 6;");
            confirmedBar.setPrefWidth(widthFor(metric.confirmed(), maxTotal));
            confirmedBar.setMinHeight(24);

            Region cancelledBar = new Region();
            cancelledBar.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 0 6 6 0;");
            cancelledBar.setPrefWidth(widthFor(metric.cancelled(), maxTotal));
            cancelledBar.setMinHeight(24);

            bar.getChildren().addAll(confirmedBar, cancelledBar);

            HBox legend = new HBox(12);
            Label confirmedLegend = new Label("Confirmes: " + metric.confirmed());
            confirmedLegend.setStyle("-fx-font-size: 12px; -fx-text-fill: #16a34a;");
            Label cancelledLegend = new Label("Annules: " + metric.cancelled());
            cancelledLegend.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
            legend.getChildren().addAll(confirmedLegend, cancelledLegend);

            block.getChildren().addAll(titleRow, bar, legend);
            monthlyAppointmentsContainer.getChildren().add(block);
        }
    }

    private void renderSummaryBlocks(List<DoctorWorkspaceDao.AppointmentRecord> appointments,
                                     List<DoctorWorkspaceDao.PatientRecord> patients) {
        long confirmedCount = appointments.stream()
                .filter(appointment -> !"Annule".equalsIgnoreCase(appointment.status()))
                .count();
        long cancelledCount = appointments.stream()
                .filter(appointment -> "Annule".equalsIgnoreCase(appointment.status()))
                .count();
        long total = Math.max(appointments.size(), 1);

        int confirmedPercent = (int) Math.round((confirmedCount * 100.0) / total);
        int cancelledPercent = (int) Math.round((cancelledCount * 100.0) / total);

        confirmedSummaryLabel.setText(confirmedCount + " (" + confirmedPercent + "%)");
        cancelledSummaryLabel.setText(cancelledCount + " (" + cancelledPercent + "%)");
        confirmedSummaryBar.setPrefWidth(Math.max(18, confirmedPercent * 3.0));
        cancelledSummaryBar.setPrefWidth(Math.max(18, cancelledPercent * 3.0));

        long currentMonthAppointments = appointments.stream()
                .filter(appointment -> appointment.date().getMonth() == LocalDate.now().getMonth()
                        && appointment.date().getYear() == LocalDate.now().getYear())
                .count();
        long currentMonthNewPatients = patients.stream()
                .filter(patient -> !patient.lastVisit().equals("Aucun rendez-vous")
                        && parseMonth(patient.lastVisit()) == LocalDate.now().getMonthValue())
                .count();

        newPatientsGrowthLabel.setText("+" + currentMonthNewPatients);
        newAppointmentsGrowthLabel.setText("+" + currentMonthAppointments);
        newPatientsGrowthBar.setPrefWidth(Math.max(18, currentMonthNewPatients * 28.0));
        newAppointmentsGrowthBar.setPrefWidth(Math.max(18, currentMonthAppointments * 18.0));
    }

    private List<MonthMetrics> buildMonthMetrics(List<DoctorWorkspaceDao.AppointmentRecord> appointments) {
        List<MonthMetrics> metrics = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int monthValue = 1; monthValue <= now.getMonthValue(); monthValue++) {
            final int currentMonthValue = monthValue;
            Month month = Month.of(monthValue);
            int confirmed = (int) appointments.stream()
                    .filter(appointment -> appointment.date().getYear() == now.getYear())
                    .filter(appointment -> appointment.date().getMonthValue() == currentMonthValue)
                    .filter(appointment -> !"Annule".equalsIgnoreCase(appointment.status()))
                    .count();
            int cancelled = (int) appointments.stream()
                    .filter(appointment -> appointment.date().getYear() == now.getYear())
                    .filter(appointment -> appointment.date().getMonthValue() == currentMonthValue)
                    .filter(appointment -> "Annule".equalsIgnoreCase(appointment.status()))
                    .count();
            metrics.add(new MonthMetrics(
                    month.getDisplayName(TextStyle.FULL, Locale.FRENCH),
                    confirmed,
                    cancelled,
                    confirmed + cancelled
            ));
        }
        return metrics;
    }

    private double widthFor(int value, int max) {
        if (value <= 0) {
            return 0;
        }
        return Math.max(20, (760.0 * value) / Math.max(max, 1));
    }

    private int parseMonth(String date) {
        try {
            String[] parts = date.split("/");
            return Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private record MonthMetrics(String monthName, int confirmed, int cancelled, int total) {
    }
}
