package org.example.meetcare.utils;

import org.example.meetcare.model.Appointment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AppointmentStore {
    public static final String STATUS_CONFIRMED = "CONFIRME";
    public static final String STATUS_CANCELLED = "ANNULE";
    public static final String STATUS_COMPLETED = "TERMINE";

    private static final List<Appointment> APPOINTMENTS = new ArrayList<>();
    private static boolean initialized;
    private static int nextId = 1;

    private AppointmentStore() {
    }

    public static List<Appointment> getAppointments() {
        initializeDefaults();
        return new ArrayList<>(APPOINTMENTS);
    }

    public static void addAppointment(Appointment appointment) {
        initializeDefaults();
        APPOINTMENTS.add(0, appointment);
    }

    public static Appointment createAppointment(String doctorName, String speciality, String city, String address,
                                                String status, String details, boolean teleconsultation,
                                                LocalDate date, String time) {
        return new Appointment(nextId++, doctorName, speciality, city, address, status, details, teleconsultation, date, time);
    }

    public static void updateStatus(int appointmentId, String newStatus) {
        initializeDefaults();
        for (Appointment appointment : APPOINTMENTS) {
            if (appointment.getId() == appointmentId) {
                appointment.setStatus(newStatus);
                return;
            }
        }
    }

    private static void initializeDefaults() {
        if (initialized) {
            return;
        }

        APPOINTMENTS.add(createAppointment(
                "Dr. Sarah Martin",
                "Cardiologue",
                "Paris",
                "15 Avenue des Champs-Elysees, 75008 Paris",
                STATUS_CONFIRMED,
                "Motif: Consultation de suivi",
                false,
                LocalDate.of(2026, 5, 13),
                "14:30"
        ));

        APPOINTMENTS.add(createAppointment(
                "Dr. Marc Dubois",
                "Generaliste",
                "Lyon",
                "23 Rue de la Republique, 69002 Lyon",
                STATUS_COMPLETED,
                "Motif: Consultation generale",
                true,
                LocalDate.of(2026, 5, 16),
                "10:00"
        ));

        APPOINTMENTS.add(createAppointment(
                "Dr. Julie Bernard",
                "Dermatologue",
                "Paris",
                "8 Boulevard Saint-Germain, 75005 Paris",
                STATUS_CANCELLED,
                "Motif: Controle dermatologique",
                false,
                LocalDate.of(2026, 5, 18),
                "16:00"
        ));

        initialized = true;
    }
}
