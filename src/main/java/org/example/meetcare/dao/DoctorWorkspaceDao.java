package org.example.meetcare.dao;

import org.example.meetcare.model.AvailabilitySlot;
import org.example.meetcare.model.MedicalDocumentData;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.DatabaseConnection;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class DoctorWorkspaceDao {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DoctorSummary getDoctorSummary() {
        String sql = """
                SELECT u.username, d.speciality
                FROM users u
                JOIN doctors d ON d.id_users = u.id_users
                WHERE u.id_users = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new DoctorSummary(
                            formatDoctorName(resultSet.getString("username")),
                            safe(resultSet.getString("speciality"), "Specialite")
                    );
                }
            }
        } catch (SQLException ignored) {
        }
        return new DoctorSummary("Dr. Medecin", "Specialite");
    }

    public List<AppointmentRecord> getAppointments() {
        List<AppointmentRecord> appointments = new ArrayList<>();
        String sql = """
                SELECT p.id_users AS patient_id,
                       p.username AS patient_name,
                       a.notes,
                       a.appointment_date,
                       a.appointment_time,
                       a.status
                FROM appointments a
                JOIN users p ON p.id_users = a.id_users
                JOIN doctors d ON d.id_doctors = a.id_doctors
                WHERE d.id_users = ?
                ORDER BY a.appointment_date, a.appointment_time
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String notes = safe(resultSet.getString("notes"), "Consultation reservee|Cabinet");
                    appointments.add(new AppointmentRecord(
                            resultSet.getInt("patient_id"),
                            safe(resultSet.getString("patient_name"), "Patient"),
                            extractPurpose(notes),
                            resultSet.getDate("appointment_date").toLocalDate(),
                            resultSet.getTime("appointment_time").toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                            extractMode(notes),
                            mapAppointmentStatus(resultSet.getString("status")),
                            estimateDurationMinutes(notes)
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return appointments;
    }

    public List<PatientRecord> getPatients() {
        List<PatientRecord> patients = new ArrayList<>();
        String sql = """
                SELECT u.id_users,
                       u.username,
                       u.email,
                       u.phone,
                       u.address,
                       u.city,
                       COUNT(DISTINCT a.id_appointments) AS appointment_count,
                       MAX(a.appointment_date) AS last_appointment_date
                FROM users u
                JOIN appointments a ON a.id_users = u.id_users
                JOIN doctors d ON d.id_doctors = a.id_doctors
                WHERE u.role = 'USER'
                  AND d.id_users = ?
                GROUP BY u.id_users, u.username, u.email, u.phone, u.address, u.city
                ORDER BY u.username
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String name = safe(resultSet.getString("username"), "Patient");
                    Date lastDate = resultSet.getDate("last_appointment_date");
                    patients.add(new PatientRecord(
                            resultSet.getInt("id_users"),
                            name,
                            safe(resultSet.getString("email"), "Email non renseigne"),
                            safe(resultSet.getString("phone"), "Telephone non renseigne"),
                            buildAddress(resultSet.getString("address"), resultSet.getString("city")),
                            buildInitials(name),
                            "Non renseignee",
                            resultSet.getInt("appointment_count"),
                            lastDate == null ? "Aucun rendez-vous" : DISPLAY_DATE.format(lastDate.toLocalDate())
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return patients;
    }

    public PatientDetail getPatientDetail(int patientId) {
        PatientRecord overview = getPatients().stream()
                .filter(patient -> patient.id() == patientId)
                .findFirst()
                .orElse(null);
        if (overview == null) {
            return null;
        }

        return new PatientDetail(
                overview,
                getPatientAppointments(patientId),
                getPatientDocuments(patientId),
                getPatientInvoices(patientId),
                getPatientNotes(patientId)
        );
    }

    public List<DocumentRecord> getDocuments() {
        List<DocumentRecord> documents = new ArrayList<>();
        String sql = """
                SELECT m.title,
                       m.description,
                       COALESCE(m.document_type, '') AS document_type,
                       COALESCE(m.file_path, '') AS file_path,
                       u.username AS patient_name,
                       m.date
                FROM medical_records m
                JOIN users u ON u.id_users = m.id_users
                JOIN doctors d ON d.id_doctors = m.id_doctors
                WHERE d.id_users = ?
                  AND LOWER(m.title) <> 'note medicale'
                ORDER BY m.date DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String title = safe(resultSet.getString("title"), "Document medical");
                    documents.add(new DocumentRecord(
                            title,
                            safe(resultSet.getString("patient_name"), "Patient"),
                            safe(resultSet.getString("document_type"), inferDocumentType(title)),
                            resultSet.getTimestamp("date") == null
                                    ? ""
                                    : DISPLAY_DATE.format(resultSet.getTimestamp("date").toLocalDateTime().toLocalDate()),
                            "Envoye",
                            safe(resultSet.getString("description"), ""),
                            safe(resultSet.getString("file_path"), "")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return documents;
    }

    public List<InvoiceRecord> getInvoices() {
        List<InvoiceRecord> invoices = new ArrayList<>();
        String sql = """
                SELECT u.username AS patient_name,
                       i.description,
                       i.amount,
                       i.status,
                       i.paid_at
                FROM invoices i
                JOIN users u ON u.id_users = i.id_users
                WHERE EXISTS (
                    SELECT 1
                    FROM appointments a
                    WHERE a.id_users = i.id_users
                      AND a.id_doctors = ?
                )
                ORDER BY COALESCE(i.paid_at, CURRENT_TIMESTAMP) DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp paidAt = resultSet.getTimestamp("paid_at");
                    invoices.add(new InvoiceRecord(
                            safe(resultSet.getString("patient_name"), "Patient"),
                            safe(resultSet.getString("description"), "Facture consultation"),
                            resultSet.getBigDecimal("amount").stripTrailingZeros().toPlainString() + " EUR",
                            mapInvoiceStatus(resultSet.getString("status")),
                            paidAt == null ? "" : DISPLAY_DATE.format(paidAt.toLocalDateTime().toLocalDate())
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return invoices;
    }

    public void insertMedicalDocument(int patientId, MedicalDocumentData document) {
        String resolvedPath = safe(document.getFilePath(), buildDocumentPath(document.getType(), document.getTitle()));
        String sql = """
                INSERT INTO medical_records (title, document_type, description, file_path, date, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, document.getTitle());
            statement.setString(2, document.getType());
            statement.setString(3, document.getDescription());
            statement.setString(4, resolvedPath);
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(6, resolveDoctorId());
            statement.setInt(7, patientId);
            statement.executeUpdate();
            ensureMedicalDocumentFile(resolvedPath, document);
        } catch (SQLException ignored) {
        }
    }

    public void insertInvoice(int patientId, BigDecimal amount) {
        String sql = """
                INSERT INTO invoices (amount, description, status, payment_method, paid_at, id_users)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setBigDecimal(1, amount);
            statement.setString(2, "Facture consultation");
            statement.setString(3, "PAYEE");
            statement.setString(4, "Cabinet");
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(6, patientId);
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void insertNote(int patientId, String content) {
        String sql = """
                INSERT INTO medical_records (title, document_type, description, file_path, date, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, "Note medicale");
            statement.setString(2, "Note");
            statement.setString(3, content);
            statement.setString(4, "notes/" + System.currentTimeMillis() + ".txt");
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(6, resolveDoctorId());
            statement.setInt(7, patientId);
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public List<AvailabilitySlot> getAvailabilitySlots(LocalDate date) {
        List<AvailabilitySlot> slots = new ArrayList<>();
        String sql = """
                SELECT s.id_availability_slots,
                       s.id_doctors,
                       s.slot_date,
                       s.start_time,
                       s.end_time,
                       s.status,
                       COALESCE(s.notes, '') AS notes,
                       COALESCE(booked.patient_name, '') AS booked_patient_name
                FROM doctor_availability_slots s
                LEFT JOIN (
                    SELECT a.id_doctors,
                           a.appointment_date,
                           a.appointment_time,
                           u.username AS patient_name
                    FROM appointments a
                    JOIN users u ON u.id_users = a.id_users
                    WHERE UPPER(COALESCE(a.status, 'CONFIRME')) IN ('CONFIRME', 'EN_ATTENTE')
                ) booked
                  ON booked.id_doctors = s.id_doctors
                 AND booked.appointment_date = s.slot_date
                 AND booked.appointment_time = s.start_time
                WHERE s.id_doctors = ?
                """;
        if (date != null) {
            sql += " AND s.slot_date = ? ";
        }
        sql += " ORDER BY s.slot_date, s.start_time ";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorId());
            if (date != null) {
                statement.setDate(2, Date.valueOf(date));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String bookedPatientName = resultSet.getString("booked_patient_name");
                    slots.add(new AvailabilitySlot(
                            resultSet.getInt("id_availability_slots"),
                            resultSet.getInt("id_doctors"),
                            resultSet.getDate("slot_date").toLocalDate(),
                            resultSet.getTime("start_time").toLocalTime(),
                            resultSet.getTime("end_time").toLocalTime(),
                            safe(resultSet.getString("status"), "DISPONIBLE"),
                            resultSet.getString("notes"),
                            bookedPatientName != null && !bookedPatientName.isBlank(),
                            bookedPatientName
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return slots;
    }

    public void addAvailabilitySlot(LocalDate date, LocalTime startTime, LocalTime endTime, String status, String notes) {
        String sql = """
                INSERT INTO doctor_availability_slots (slot_date, start_time, end_time, status, notes, id_doctors)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(date));
            statement.setTime(2, Time.valueOf(startTime));
            statement.setTime(3, Time.valueOf(endTime));
            statement.setString(4, status);
            statement.setString(5, notes);
            statement.setInt(6, resolveDoctorId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void updateAvailabilityStatus(int slotId, String status) {
        String sql = """
                UPDATE doctor_availability_slots
                SET status = ?
                WHERE id_availability_slots = ?
                  AND id_doctors = ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, slotId);
            statement.setInt(3, resolveDoctorId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void replaceDayWithUnavailability(LocalDate date) {
        String deleteSql = "DELETE FROM doctor_availability_slots WHERE id_doctors = ? AND slot_date = ?";
        String insertSql = """
                INSERT INTO doctor_availability_slots (slot_date, start_time, end_time, status, notes, id_doctors)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = getConnection()) {
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                deleteStatement.setInt(1, resolveDoctorId());
                deleteStatement.setDate(2, Date.valueOf(date));
                deleteStatement.executeUpdate();
            }
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setDate(1, Date.valueOf(date));
                insertStatement.setTime(2, Time.valueOf(LocalTime.of(8, 0)));
                insertStatement.setTime(3, Time.valueOf(LocalTime.of(18, 0)));
                insertStatement.setString(4, "INDISPONIBLE");
                insertStatement.setString(5, "Journee bloquee");
                insertStatement.setInt(6, resolveDoctorId());
                insertStatement.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    public void clearDayUnavailability(LocalDate date) {
        String sql = """
                DELETE FROM doctor_availability_slots
                WHERE id_doctors = ?
                  AND slot_date = ?
                  AND UPPER(status) = 'INDISPONIBLE'
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorId());
            statement.setDate(2, Date.valueOf(date));
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private List<AppointmentHistoryRecord> getPatientAppointments(int patientId) {
        List<AppointmentHistoryRecord> appointments = new ArrayList<>();
        String sql = """
                SELECT appointment_date, appointment_time, notes, status
                FROM appointments
                WHERE id_users = ?
                  AND id_doctors = ?
                ORDER BY appointment_date DESC, appointment_time DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, resolveDoctorId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String notes = safe(resultSet.getString("notes"), "Consultation reservee|Cabinet");
                    appointments.add(new AppointmentHistoryRecord(
                            extractPurpose(notes),
                            DISPLAY_DATE.format(resultSet.getDate("appointment_date").toLocalDate()),
                            mapHistoryStatus(resultSet.getString("status"))
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return appointments;
    }

    private List<DocumentHistoryRecord> getPatientDocuments(int patientId) {
        List<DocumentHistoryRecord> documents = new ArrayList<>();
        String sql = """
                SELECT title, date, COALESCE(document_type, '') AS document_type
                FROM medical_records
                WHERE id_users = ?
                  AND id_doctors = ?
                  AND LOWER(title) <> 'note medicale'
                ORDER BY date DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, resolveDoctorId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String title = safe(resultSet.getString("title"), "Document");
                    Timestamp date = resultSet.getTimestamp("date");
                    documents.add(new DocumentHistoryRecord(
                            title,
                            date == null ? "" : DISPLAY_DATE.format(date.toLocalDateTime().toLocalDate()),
                            safe(resultSet.getString("document_type"), inferDocumentType(title))
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return documents;
    }

    private List<InvoiceHistoryRecord> getPatientInvoices(int patientId) {
        List<InvoiceHistoryRecord> invoices = new ArrayList<>();
        String sql = """
                SELECT amount, description, status, paid_at
                FROM invoices
                WHERE id_users = ?
                ORDER BY COALESCE(paid_at, CURRENT_TIMESTAMP) DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp paidAt = resultSet.getTimestamp("paid_at");
                    invoices.add(new InvoiceHistoryRecord(
                            safe(resultSet.getString("description"), "Facture consultation"),
                            paidAt == null ? "" : DISPLAY_DATE.format(paidAt.toLocalDateTime().toLocalDate()),
                            resultSet.getBigDecimal("amount").stripTrailingZeros().toPlainString() + " EUR",
                            mapInvoiceStatus(resultSet.getString("status"))
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return invoices;
    }

    private List<NoteHistoryRecord> getPatientNotes(int patientId) {
        List<NoteHistoryRecord> notes = new ArrayList<>();
        String sql = """
                SELECT description, date
                FROM medical_records
                WHERE id_users = ?
                  AND id_doctors = ?
                  AND LOWER(title) = 'note medicale'
                ORDER BY date DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, resolveDoctorId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp date = resultSet.getTimestamp("date");
                    notes.add(new NoteHistoryRecord(
                            date == null ? "" : DISPLAY_DATE.format(date.toLocalDateTime().toLocalDate()),
                            safe(resultSet.getString("description"), "Note medicale")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return notes;
    }

    private int resolveDoctorId() {
        String sql = "SELECT id_doctors FROM doctors WHERE id_users = ? LIMIT 1";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_doctors");
                }
            }
        } catch (SQLException ignored) {
        }
        return 1;
    }

    private int resolveDoctorUserId() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser != null && currentUser.getIdUsers() > 0) {
            return currentUser.getIdUsers();
        }
        return 1;
    }

    private String extractPurpose(String notes) {
        if (notes.contains("|")) {
            return notes.split("\\|")[0].trim();
        }
        return notes.trim();
    }

    private String extractMode(String notes) {
        if (notes.contains("|")) {
            return notes.split("\\|")[1].trim();
        }
        return "Cabinet";
    }

    private int estimateDurationMinutes(String notes) {
        String lower = safe(notes, "").toLowerCase(Locale.ROOT);
        if (lower.contains("premiere")) return 60;
        if (lower.contains("suivi")) return 45;
        return 30;
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

    private String mapAppointmentStatus(String status) {
        if (status == null) return "Confirme";
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "EN_ATTENTE" -> "En attente";
            case "TERMINE", "TERMINEE" -> "Termine";
            case "ANNULE" -> "Annule";
            default -> "Confirme";
        };
    }

    private String mapHistoryStatus(String status) {
        if (status == null) return "Confirme";
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "EN_ATTENTE" -> "En attente";
            case "ANNULE" -> "Annule";
            case "CONFIRME" -> "Confirme";
            default -> "Termine";
        };
    }

    private String mapInvoiceStatus(String status) {
        if (status == null) return "Payee";
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "EN_ATTENTE" -> "En attente";
            default -> "Payee";
        };
    }

    private String buildInitials(String name) {
        String[] parts = safe(name, "P").trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String buildAddress(String address, String city) {
        if (address == null || address.isBlank()) {
            return safe(city, "Adresse non renseignee");
        }
        if (city == null || city.isBlank() || address.toLowerCase(Locale.ROOT).contains(city.toLowerCase(Locale.ROOT))) {
            return address;
        }
        return address + ", " + city;
    }

    private String formatDoctorName(String username) {
        if (username == null || username.isBlank()) {
            return "Dr. Inconnu";
        }
        return username.startsWith("Dr.") ? username : "Dr. " + username;
    }

    private String buildDocumentPath(String type, String title) {
        return "records/" + slugify(type) + "-" + slugify(title) + ".txt";
    }

    private void ensureMedicalDocumentFile(String filePath, MedicalDocumentData document) {
        try {
            Path resolvedPath = Path.of(System.getProperty("user.dir")).resolve(filePath).normalize();
            Path parent = resolvedPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(resolvedPath)) {
                Files.writeString(resolvedPath, document.getDescription(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    private String slugify(String value) {
        return safe(value, "document")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public record DoctorSummary(String displayName, String speciality) {
    }

    public record AppointmentRecord(
            int patientId,
            String patientName,
            String purpose,
            LocalDate date,
            String time,
            String mode,
            String status,
            int durationMinutes
    ) {
    }

    public record PatientRecord(
            int id,
            String name,
            String email,
            String phone,
            String address,
            String initials,
            String birthDate,
            int appointmentCount,
            String lastVisit
    ) {
    }

    public record PatientDetail(
            PatientRecord patient,
            List<AppointmentHistoryRecord> appointments,
            List<DocumentHistoryRecord> documents,
            List<InvoiceHistoryRecord> invoices,
            List<NoteHistoryRecord> notes
    ) {
    }

    public record AppointmentHistoryRecord(
            String title,
            String date,
            String status
    ) {
    }

    public record DocumentRecord(
            String title,
            String patientName,
            String type,
            String date,
            String status,
            String description,
            String filePath
    ) {
    }

    public record DocumentHistoryRecord(
            String title,
            String date,
            String type
    ) {
    }

    public record InvoiceHistoryRecord(
            String title,
            String date,
            String amount,
            String status
    ) {
    }

    public record NoteHistoryRecord(
            String date,
            String content
    ) {
    }

    public record InvoiceRecord(
            String patientName,
            String description,
            String amount,
            String status,
            String date
    ) {
    }
}
