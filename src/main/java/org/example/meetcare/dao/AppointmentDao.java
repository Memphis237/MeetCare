package org.example.meetcare.dao;

import org.example.meetcare.model.Appointment;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentDao {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static volatile boolean schemaChecked;

    private final AvailabilityBlockDao availabilityBlockDao = new AvailabilityBlockDao();

    public AppointmentDao() {
        ensureSchema();
    }

    public int insertAppointment(int doctorId, int userId, LocalDate date, String time, String notes) {
        String sql = """
                INSERT INTO appointments (appointment_date, appointment_time, status, urgency, notes, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setTime(2, Time.valueOf(LocalTime.parse(time)));
            stmt.setString(3, "CONFIRME");
            stmt.setString(4, "NORMAL");
            stmt.setString(5, notes);
            stmt.setInt(6, doctorId);
            stmt.setInt(7, userId);
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
                if (!rs.next() || rs.getInt("total") > 0) {
                    return false;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de verifier la disponibilite du creneau.", ex);
        }

        Integer doctorUserId = findDoctorUserId(doctorId);
        if (doctorUserId != null && availabilityBlockDao.isBlocked(doctorUserId, date, time)) {
            return false;
        }

        return true;
    }

    public List<Appointment> findByUserId(int userId) {
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       COALESCE(a.urgency, 'NORMAL') AS urgency,
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
                            "Dr. " + safeText(rs.getString("doctor_name")),
                            rs.getString("speciality"),
                            rs.getString("doctor_city"),
                            rs.getString("doctor_address"),
                            rs.getString("status"),
                            rs.getString("urgency"),
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
                       COALESCE(a.urgency, 'NORMAL') AS urgency,
                       a.notes,
                       a.id_users,
                       d.teleconsultation,
                       COALESCE(d.address, doctor_user.address, '') AS doctor_address,
                       d.speciality,
                       doctor_user.username AS doctor_username,
                       doctor_user.prenom AS doctor_prenom,
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
                            buildDoctorLabel(rs.getString("doctor_prenom"), rs.getString("doctor_username")),
                            rs.getString("speciality"),
                            rs.getString("patient_city"),
                            rs.getString("doctor_address"),
                            rs.getString("status"),
                            rs.getString("urgency"),
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

    public List<Appointment> findAllAppointments() {
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       COALESCE(a.urgency, 'NORMAL') AS urgency,
                       a.notes,
                       a.id_users,
                       d.teleconsultation,
                       COALESCE(d.address, doctor_user.address, '') AS doctor_address,
                       d.speciality,
                       doctor_user.username AS doctor_username,
                       doctor_user.prenom AS doctor_prenom,
                       patient_user.username AS patient_username,
                       patient_user.prenom AS patient_prenom,
                       patient_user.city AS patient_city
                FROM appointments a
                JOIN doctors d ON a.id_doctors = d.id_doctors
                JOIN users doctor_user ON d.id_users = doctor_user.id_users
                JOIN users patient_user ON a.id_users = patient_user.id_users
                ORDER BY a.appointment_date ASC, a.appointment_time ASC
                """;

        List<Appointment> appointments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Appointment appointment = new Appointment(
                        rs.getInt("id_appointments"),
                        buildDoctorLabel(rs.getString("doctor_prenom"), rs.getString("doctor_username")),
                        rs.getString("speciality"),
                        rs.getString("patient_city"),
                        rs.getString("doctor_address"),
                        rs.getString("status"),
                        rs.getString("urgency"),
                        rs.getString("notes"),
                        rs.getBoolean("teleconsultation"),
                        rs.getDate("appointment_date").toLocalDate(),
                        rs.getTime("appointment_time").toLocalTime().format(TIME_FORMATTER),
                        buildDisplayName(rs.getString("patient_prenom"), rs.getString("patient_username"))
                );
                appointments.add(appointment);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger tous les rendez-vous.", ex);
        }
        return appointments;
    }

    public void updateStatus(int appointmentId, String status) {
        String sql = "UPDATE appointments SET status = ? WHERE id_appointments = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeStatus(status));
            stmt.setInt(2, appointmentId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de mettre a jour le rendez-vous.", ex);
        }
    }

    public void updateStatusAndUrgency(int appointmentId, String status, String urgency) {
        String sql = """
                UPDATE appointments
                SET status = ?, urgency = ?
                WHERE id_appointments = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeStatus(status));
            stmt.setString(2, normalizeUrgency(urgency));
            stmt.setInt(3, appointmentId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de mettre a jour le rendez-vous.", ex);
        }
    }

    public Optional<AppointmentParticipantInfo> findParticipantInfoByAppointmentId(int appointmentId) {
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       COALESCE(a.urgency, 'NORMAL') AS urgency,
                       a.notes,
                       doctor_user.id_users AS doctor_user_id,
                       patient_user.id_users AS patient_user_id,
                       doctor_user.username AS doctor_username,
                       doctor_user.prenom AS doctor_prenom,
                       patient_user.username AS patient_username,
                       patient_user.prenom AS patient_prenom
                FROM appointments a
                JOIN doctors d ON a.id_doctors = d.id_doctors
                JOIN users doctor_user ON d.id_users = doctor_user.id_users
                JOIN users patient_user ON a.id_users = patient_user.id_users
                WHERE a.id_appointments = ?
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, appointmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new AppointmentParticipantInfo(
                            rs.getInt("doctor_user_id"),
                            rs.getInt("patient_user_id"),
                            buildDoctorLabel(rs.getString("doctor_prenom"), rs.getString("doctor_username")),
                            buildDisplayName(rs.getString("patient_prenom"), rs.getString("patient_username")),
                            rs.getDate("appointment_date").toLocalDate(),
                            rs.getTime("appointment_time").toLocalTime().format(TIME_FORMATTER),
                            rs.getString("status"),
                            rs.getString("urgency"),
                            rs.getString("notes")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de recuperer les participants du rendez-vous.", ex);
        }
        return Optional.empty();
    }

    private void ensureSchema() {
        if (schemaChecked) {
            return;
        }

        synchronized (AppointmentDao.class) {
            if (schemaChecked) {
                return;
            }

            String sql = """
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'appointments'
                      AND COLUMN_NAME = 'urgency'
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                boolean hasUrgencyColumn = rs.next() && rs.getInt(1) > 0;
                if (!hasUrgencyColumn) {
                    try (Statement alter = conn.createStatement()) {
                        alter.executeUpdate("ALTER TABLE appointments ADD COLUMN urgency VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER status");
                    }
                }
                schemaChecked = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Impossible de verifier la structure des rendez-vous.", ex);
            }
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

    private String buildDoctorLabel(String prenom, String username) {
        String displayName = buildDisplayName(prenom, username);
        return "-".equals(displayName) ? "Dr." : "Dr. " + displayName;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "CONFIRME";
        }
        return status.trim().toUpperCase();
    }

    private String normalizeUrgency(String urgency) {
        if (urgency == null || urgency.isBlank()) {
            return "NORMAL";
        }

        String normalized = urgency.trim().toUpperCase();
        return switch (normalized) {
            case "NORMAL", "URGENT", "ELEVEE", "CRITIQUE" -> normalized;
            default -> "NORMAL";
        };
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private Integer findDoctorUserId(int doctorId) {
        String sql = "SELECT id_users FROM doctors WHERE id_doctors = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_users");
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de resoudre le medecin associe au creneau.", ex);
        }
    }

    public record AppointmentParticipantInfo(
            int doctorUserId,
            int patientUserId,
            String doctorDisplayName,
            String patientDisplayName,
            LocalDate date,
            String time,
            String status,
            String urgency,
            String notes
    ) {
    }
}
