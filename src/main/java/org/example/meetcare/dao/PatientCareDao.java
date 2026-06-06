package org.example.meetcare.dao;

import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.AvailabilitySlot;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatientCareDao {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DoctorDao doctorDao = new DoctorDao();

    public List<DoctorSearchRecord> searchDoctors(String keyword, String city, LocalDate selectedDate) {
        List<DoctorSearchRecord> results = new ArrayList<>();
        LocalDate referenceDate = selectedDate == null ? LocalDate.now() : selectedDate;

        for (Doctor doctor : doctorDao.searchDoctors(keyword, city)) {
            List<AvailabilitySlot> slots = getAvailableSlotsForDoctor(doctor.getId(), selectedDate);
            if (selectedDate != null && slots.isEmpty()) {
                continue;
            }
            if (selectedDate == null) {
                slots = slots.stream()
                        .filter(slot -> !slot.getDate().isBefore(referenceDate))
                        .limit(5)
                        .toList();
            }
            results.add(new DoctorSearchRecord(doctor, slots));
        }
        return results;
    }

    public List<AvailabilitySlot> getAvailableSlotsForDoctor(int doctorId, LocalDate selectedDate) {
        List<AvailabilitySlot> slots = new ArrayList<>();
        String sql = """
                SELECT s.id_availability_slots,
                       s.id_doctors,
                       s.slot_date,
                       s.start_time,
                       s.end_time,
                       s.status,
                       COALESCE(s.notes, '') AS notes
                FROM doctor_availability_slots s
                WHERE s.id_doctors = ?
                  AND UPPER(s.status) = 'DISPONIBLE'
                  AND s.slot_date >= ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM appointments a
                      WHERE a.id_doctors = s.id_doctors
                        AND a.appointment_date = s.slot_date
                        AND a.appointment_time = s.start_time
                        AND UPPER(COALESCE(a.status, 'CONFIRME')) IN ('CONFIRME', 'EN_ATTENTE')
                  )
                """;
        if (selectedDate != null) {
            sql += " AND s.slot_date = ? ";
        }
        sql += " ORDER BY s.slot_date, s.start_time ";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            statement.setDate(2, Date.valueOf(selectedDate == null ? LocalDate.now() : selectedDate));
            if (selectedDate != null) {
                statement.setDate(3, Date.valueOf(selectedDate));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    slots.add(new AvailabilitySlot(
                            resultSet.getInt("id_availability_slots"),
                            resultSet.getInt("id_doctors"),
                            resultSet.getDate("slot_date").toLocalDate(),
                            resultSet.getTime("start_time").toLocalTime(),
                            resultSet.getTime("end_time").toLocalTime(),
                            resultSet.getString("status"),
                            resultSet.getString("notes"),
                            false,
                            null
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return slots;
    }

    public boolean createAppointment(int doctorId, AvailabilitySlot slot, boolean teleconsultation) {
        if (slot == null) {
            return false;
        }

        int patientId = resolveCurrentPatientId();
        if (patientId <= 0 || !isSlotStillAvailable(doctorId, slot.getDate(), slot.getStartTime())) {
            return false;
        }

        String sql = """
                INSERT INTO appointments (appointment_date, appointment_time, status, notes, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(slot.getDate()));
            statement.setTime(2, Time.valueOf(slot.getStartTime()));
            statement.setString(3, "CONFIRME");
            statement.setString(4, "Consultation reservee|" + (teleconsultation ? "Teleconsultation" : "Cabinet"));
            statement.setInt(5, doctorId);
            statement.setInt(6, patientId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    public List<Appointment> getPatientAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       a.notes,
                       u.username AS doctor_name,
                       u.city,
                       d.speciality,
                       d.address,
                       d.teleconsultation
                FROM appointments a
                JOIN doctors d ON d.id_doctors = a.id_doctors
                JOIN users u ON u.id_users = d.id_users
                WHERE a.id_users = ?
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resolveCurrentPatientId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    appointments.add(new Appointment(
                            resultSet.getInt("id_appointments"),
                            formatDoctorName(resultSet.getString("doctor_name")),
                            safe(resultSet.getString("speciality"), "Specialite non renseignee"),
                            safe(resultSet.getString("city"), "Ville non renseignee"),
                            safe(resultSet.getString("address"), "Adresse non renseignee"),
                            normalizeAppointmentStatus(resultSet.getString("status")),
                            buildDetails(resultSet.getString("notes")),
                            resultSet.getBoolean("teleconsultation"),
                            resultSet.getDate("appointment_date").toLocalDate(),
                            resultSet.getTime("appointment_time").toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return appointments;
    }

    public PatientDashboardData getPatientDashboardData() {
        List<Appointment> appointments = getPatientAppointments();
        List<PatientDocumentRecord> documents = getPatientDocuments();

        long upcomingAppointments = appointments.stream()
                .filter(appointment -> "CONFIRME".equals(appointment.getStatus()))
                .filter(appointment -> !appointment.getDate().isBefore(LocalDate.now()))
                .count();

        InvoiceStats invoiceStats = getInvoiceStats();
        Appointment nextAppointment = appointments.stream()
                .filter(appointment -> "CONFIRME".equals(appointment.getStatus()))
                .filter(appointment -> !appointment.getDate().isBefore(LocalDate.now()))
                .sorted((left, right) -> {
                    int dateComparison = left.getDate().compareTo(right.getDate());
                    if (dateComparison != 0) {
                        return dateComparison;
                    }
                    return left.getTime().compareTo(right.getTime());
                })
                .findFirst()
                .orElse(null);

        List<DashboardNotification> notifications = buildNotifications(nextAppointment, documents);

        return new PatientDashboardData(
                (int) upcomingAppointments,
                documents.size(),
                invoiceStats.pendingInvoices(),
                getUnreadMessagesCount(),
                nextAppointment,
                notifications
        );
    }

    public void updateAppointmentStatus(int appointmentId, String status) {
        String sql = """
                UPDATE appointments
                SET status = ?
                WHERE id_appointments = ?
                  AND id_users = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, appointmentId);
            statement.setInt(3, resolveCurrentPatientId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public List<PatientDocumentRecord> getPatientDocuments() {
        List<PatientDocumentRecord> documents = new ArrayList<>();
        String sql = """
                SELECT m.id_medical_records,
                       m.title,
                       m.description,
                       m.date,
                       COALESCE(m.file_path, '') AS file_path,
                       COALESCE(m.document_type, '') AS document_type,
                       u.username AS doctor_name,
                       d.speciality
                FROM medical_records m
                JOIN doctors d ON d.id_doctors = m.id_doctors
                JOIN users u ON u.id_users = d.id_users
                WHERE m.id_users = ?
                  AND LOWER(COALESCE(m.title, '')) <> 'note medicale'
                ORDER BY m.date DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resolveCurrentPatientId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String title = safe(resultSet.getString("title"), "Document medical");
                    String type = safe(resultSet.getString("document_type"), inferDocumentType(title));
                    documents.add(new PatientDocumentRecord(
                            resultSet.getInt("id_medical_records"),
                            title,
                            formatDoctorName(resultSet.getString("doctor_name")),
                            safe(resultSet.getString("speciality"), "Specialite non renseignee"),
                            resultSet.getTimestamp("date") == null
                                    ? ""
                                    : DATE_FORMATTER.format(resultSet.getTimestamp("date").toLocalDateTime().toLocalDate()),
                            type,
                            safe(resultSet.getString("description"), ""),
                            safe(resultSet.getString("file_path"), "")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return documents;
    }

    private InvoiceStats getInvoiceStats() {
        String sql = """
                SELECT COUNT(*) AS total_count,
                       SUM(CASE WHEN UPPER(COALESCE(status, 'EN_ATTENTE')) = 'PAYEE' THEN 1 ELSE 0 END) AS paid_count,
                       SUM(CASE WHEN UPPER(COALESCE(status, 'EN_ATTENTE')) <> 'PAYEE' THEN 1 ELSE 0 END) AS pending_count
                FROM invoices
                WHERE id_users = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resolveCurrentPatientId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new InvoiceStats(
                            resultSet.getInt("total_count"),
                            resultSet.getInt("paid_count"),
                            resultSet.getInt("pending_count")
                    );
                }
            }
        } catch (SQLException ignored) {
        }
        return new InvoiceStats(0, 0, 0);
    }

    private int getUnreadMessagesCount() {
        String sql = """
                SELECT COUNT(*)
                FROM messages
                WHERE id_users = ?
                  AND is_read = FALSE
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resolveCurrentPatientId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    private List<DashboardNotification> buildNotifications(Appointment nextAppointment, List<PatientDocumentRecord> documents) {
        List<DashboardNotification> notifications = new ArrayList<>();
        if (nextAppointment != null) {
            notifications.add(new DashboardNotification(
                    "Rappel de rendez-vous",
                    "Votre prochaine consultation est prevue le " + DATE_FORMATTER.format(nextAppointment.getDate()) + " a " + nextAppointment.getTime() + "."
            ));
        }
        if (!documents.isEmpty()) {
            PatientDocumentRecord latestDocument = documents.get(0);
            notifications.add(new DashboardNotification(
                    "Nouveau document",
                    latestDocument.title() + " a ete ajoute par " + latestDocument.doctorName() + "."
            ));
        }
        if (notifications.isEmpty()) {
            notifications.add(new DashboardNotification(
                    "Aucune alerte",
                    "Vos prochaines notifications apparaitront ici."
            ));
        }
        return notifications;
    }

    private boolean isSlotStillAvailable(int doctorId, LocalDate date, LocalTime startTime) {
        String sql = """
                SELECT COUNT(*)
                FROM doctor_availability_slots s
                WHERE s.id_doctors = ?
                  AND s.slot_date = ?
                  AND s.start_time = ?
                  AND UPPER(s.status) = 'DISPONIBLE'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM appointments a
                      WHERE a.id_doctors = s.id_doctors
                        AND a.appointment_date = s.slot_date
                        AND a.appointment_time = s.start_time
                        AND UPPER(COALESCE(a.status, 'CONFIRME')) IN ('CONFIRME', 'EN_ATTENTE')
                  )
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(startTime));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    private int resolveCurrentPatientId() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser != null && currentUser.getIdUsers() > 0) {
            return currentUser.getIdUsers();
        }

        String sql = "SELECT id_users FROM users WHERE role = 'USER' ORDER BY id_users LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt("id_users");
            }
        } catch (SQLException ignored) {
        }
        return -1;
    }

    private String formatDoctorName(String name) {
        if (name == null || name.isBlank()) {
            return "Dr. Inconnu";
        }
        return name.startsWith("Dr.") ? name : "Dr. " + name;
    }

    private String normalizeAppointmentStatus(String status) {
        if (status == null) {
            return "CONFIRME";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ANNULE", "CANCELLED" -> "ANNULE";
            case "TERMINE", "TERMINEE", "COMPLETED" -> "TERMINE";
            default -> "CONFIRME";
        };
    }

    private String buildDetails(String notes) {
        if (notes == null || notes.isBlank()) {
            return "Motif: Consultation reservee";
        }
        String[] parts = notes.split("\\|");
        return "Motif: " + parts[0].trim();
    }

    private String inferDocumentType(String title) {
        String lower = safe(title, "").toLowerCase(Locale.ROOT);
        if (lower.contains("ordonnance")) return "Ordonnance";
        if (lower.contains("compte rendu")) return "Compte rendu";
        if (lower.contains("analyse") || lower.contains("resultat") || lower.contains("ecg")) return "Analyse";
        if (lower.contains("radio")) return "Radio";
        if (lower.contains("scan") || lower.contains("scanner")) return "Scan";
        return "Document";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record DoctorSearchRecord(Doctor doctor, List<AvailabilitySlot> availableSlots) {
    }

    public record PatientDocumentRecord(
            int id,
            String title,
            String doctorName,
            String speciality,
            String date,
            String documentType,
            String description,
            String filePath
    ) {
    }

    public record PatientDashboardData(
            int upcomingAppointments,
            int documentsCount,
            int pendingInvoices,
            int unreadMessages,
            Appointment nextAppointment,
            List<DashboardNotification> notifications
    ) {
    }

    public record DashboardNotification(
            String title,
            String content
    ) {
    }

    private record InvoiceStats(
            int totalInvoices,
            int paidInvoices,
            int pendingInvoices
    ) {
    }
}
