package org.example.meetcare.model;

import java.time.LocalDate;

public class Appointment {
    private final int id;
    private final String doctorName;
    private final String patientName;
    private final String speciality;
    private final String city;
    private final String address;
    private String status;
    private final String details;
    private final boolean teleconsultation;
    private final LocalDate date;
    private final String time;

    public Appointment(int id, String doctorName, String speciality, String city, String address, String status,
                       String details, boolean teleconsultation, LocalDate date, String time) {
        this(id, doctorName, speciality, city, address, status, details, teleconsultation, date, time, null);
    }

    public Appointment(int id, String doctorName, String speciality, String city, String address, String status,
                       String details, boolean teleconsultation, LocalDate date, String time, String patientName) {
        this.id = id;
        this.doctorName = doctorName;
        this.patientName = patientName;
        this.speciality = speciality;
        this.city = city;
        this.address = address;
        this.status = status;
        this.details = details;
        this.teleconsultation = teleconsultation;
        this.date = date;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getSpeciality() {
        return speciality;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public boolean isTeleconsultation() {
        return teleconsultation;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
