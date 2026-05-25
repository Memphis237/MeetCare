package org.example.meetcare.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AvailabilitySlot {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int id;
    private final int doctorId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String status;
    private final String notes;
    private final boolean booked;
    private final String patientName;

    public AvailabilitySlot(int id, int doctorId, LocalDate date, LocalTime startTime, LocalTime endTime,
                            String status, String notes, boolean booked, String patientName) {
        this.id = id;
        this.doctorId = doctorId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.notes = notes;
        this.booked = booked;
        this.patientName = patientName;
    }

    public int getId() {
        return id;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isBooked() {
        return booked;
    }

    public String getPatientName() {
        return patientName;
    }

    public String toDisplayLabel() {
        return DATE_FORMATTER.format(date) + " - " + TIME_FORMATTER.format(startTime) + " a " + TIME_FORMATTER.format(endTime);
    }

    public String toShortDisplayLabel() {
        return TIME_FORMATTER.format(startTime) + " - " + TIME_FORMATTER.format(endTime);
    }
}
