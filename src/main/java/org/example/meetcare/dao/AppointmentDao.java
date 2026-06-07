package org.example.meetcare.dao;

import org.example.meetcare.model.Appointment;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public int insertAppointment(int doctorId, int userId, LocalDate date, String time, String notes) {
        String sql = """
                INSERT INTO appointments (appointment_date, appointment_time, status, notes, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setTime(2, Time.valueOf(LocalTime.parse(time)));
            stmt.setString(3, "CONFIRME");
            stmt.setString(4, notes);
            stmt.setInt(5, doctorId);
            stmt.setInt(6, userId);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible d'enregistrer le rendez-vous.", ex);
        }
    }

    public boolean isSlotAvailable(int doctorId, LocalDate date, String time) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM appointments
                WHERE id_doctors = ?
                  AND appointment_date = ?
                  AND appointment_time = ?
                  AND status <> 'ANNULE'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setTime(3, Time.valueOf(LocalTime.parse(time)));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") == 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de verifier la disponibilite du creneau.", ex);
        }
    }

    public List<Appointment> findByUserId(int userId) {
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       a.notes,
                       a.id_users,
                       d.teleconsultation,
                       d.address AS doctor_address,
                       d.speciality,
                       u.username AS doctor_name,
                       u.city AS doctor_city
                FROM appointments a
                JOIN doctors d ON a.id_doctors = d.id_doctors
                JOIN users u ON d.id_users = u.id_users
                WHERE a.id_users = ?
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;

        List<Appointment> appointments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Appointment appointment = new Appointment(
                            rs.getInt("id_appointments"),
                            "Dr. " + rs.getString("doctor_name"),
                            rs.getString("speciality"),
                            rs.getString("doctor_city"),
                            rs.getString("doctor_address"),
                            rs.getString("status"),
                            rs.getString("notes"),
                            rs.getBoolean("teleconsultation"),
                            rs.getDate("appointment_date").toLocalDate(),
                            rs.getTime("appointment_time").toLocalTime().format(TIME_FORMATTER)
                    );
                    appointments.add(appointment);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger les rendez-vous.", ex);
        }
        return appointments;
    }

    public List<Appointment> findByDoctorUserId(int userId) {
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       a.notes,
                       a.id_users,
                       d.teleconsultation,
                       COALESCE(d.address, doctor_user.address, '') AS doctor_address,
                       d.speciality,
                       doctor_user.username AS doctor_username,
                       patient_user.username AS patient_username,
                       patient_user.prenom AS patient_prenom,
                       patient_user.city AS patient_city
                FROM appointments a
                JOIN doctors d ON a.id_doctors = d.id_doctors
                JOIN users doctor_user ON d.id_users = doctor_user.id_users
                JOIN users patient_user ON a.id_users = patient_user.id_users
                WHERE doctor_user.id_users = ?
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;

        List<Appointment> appointments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Appointment appointment = new Appointment(
                            rs.getInt("id_appointments"),
                            "Dr. " + safeText(rs.getString("doctor_username")),
                            rs.getString("speciality"),
                            rs.getString("patient_city"),
                            rs.getString("doctor_address"),
                            rs.getString("status"),
                            rs.getString("notes"),
                            rs.getBoolean("teleconsultation"),
                            rs.getDate("appointment_date").toLocalDate(),
                            rs.getTime("appointment_time").toLocalTime().format(TIME_FORMATTER),
                            buildDisplayName(rs.getString("patient_prenom"), rs.getString("patient_username"))
                    );
                    appointments.add(appointment);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger l'agenda du medecin.", ex);
        }
        return appointments;
    }

    public void updateStatus(int appointmentId, String status) {
        String sql = "UPDATE appointments SET status = ? WHERE id_appointments = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, appointmentId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de mettre a jour le rendez-vous.", ex);
        }
    }

    private String buildDisplayName(String prenom, String username) {
        String first = safeText(prenom);
        String second = safeText(username);
        if ("-".equals(first) && "-".equals(second)) {
            return "-";
        }
        if ("-".equals(first)) {
            return second;
        }
        if ("-".equals(second)) {
            return first;
        }
        return first + " " + second;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
