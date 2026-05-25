package org.example.meetcare.dao;

import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.DatabaseConnection;
import org.example.meetcare.utils.PasswordUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminWorkspaceDao {
    private static final Locale FRENCH = Locale.FRENCH;

    public DashboardSummary getDashboardSummary() {
        return new DashboardSummary(
                count("SELECT COUNT(*) FROM users WHERE role = 'USER'"),
                count("SELECT COUNT(*) FROM users WHERE role = 'MEDECIN'"),
                count("""
                        SELECT COUNT(*)
                        FROM appointments
                        WHERE UPPER(COALESCE(status, '')) = 'CONFIRME'
                          AND YEAR(appointment_date) = YEAR(CURDATE())
                          AND MONTH(appointment_date) = MONTH(CURDATE())
                        """),
                count("""
                        SELECT COUNT(*)
                        FROM appointments
                        WHERE UPPER(COALESCE(status, '')) = 'ANNULE'
                          AND YEAR(appointment_date) = YEAR(CURDATE())
                          AND MONTH(appointment_date) = MONTH(CURDATE())
                        """),
                count("SELECT COUNT(*) FROM appointments WHERE appointment_date = CURDATE()"),
                count("""
                        SELECT COUNT(*)
                        FROM doctors d
                        JOIN users u ON u.id_users = d.id_users
                        WHERE COALESCE(d.public_profile_completed, FALSE) = FALSE
                           OR UPPER(COALESCE(u.account_status, '')) = 'PENDING'
                        """)
        );
    }

    public List<MonthlyAppointmentStat> getMonthlyAppointmentStats() {
        Map<Integer, int[]> stats = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            stats.put(month, new int[]{0, 0});
        }

        String sql = """
                SELECT MONTH(appointment_date) AS month_number,
                       SUM(CASE WHEN UPPER(COALESCE(status, '')) = 'ANNULE' THEN 1 ELSE 0 END) AS cancelled_count,
                       SUM(CASE WHEN UPPER(COALESCE(status, '')) <> 'ANNULE' THEN 1 ELSE 0 END) AS confirmed_count
                FROM appointments
                WHERE YEAR(appointment_date) = YEAR(CURDATE())
                GROUP BY MONTH(appointment_date)
                ORDER BY MONTH(appointment_date)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int month = resultSet.getInt("month_number");
                stats.put(month, new int[]{
                        resultSet.getInt("confirmed_count"),
                        resultSet.getInt("cancelled_count")
                });
            }
        } catch (SQLException ignored) {
        }

        List<MonthlyAppointmentStat> result = new ArrayList<>();
        for (Map.Entry<Integer, int[]> entry : stats.entrySet()) {
            int month = entry.getKey();
            int[] values = entry.getValue();
            String label = YearMonth.of(LocalDate.now().getYear(), month)
                    .getMonth()
                    .getDisplayName(TextStyle.FULL, FRENCH);
            result.add(new MonthlyAppointmentStat(capitalize(label), values[0], values[1]));
        }
        return result;
    }

    public List<ActivityRecord> getRecentActivity() {
        List<ActivityRecord> activity = new ArrayList<>();
        String sql = """
                SELECT 'USER' AS source_type,
                       u.username AS title,
                       CONCAT('Compte ', LOWER(COALESCE(u.role, 'user')), ' cree') AS subtitle,
                       u.date AS event_date,
                       'Nouveau' AS badge
                FROM users u
                UNION ALL
                SELECT 'APPOINTMENT' AS source_type,
                       COALESCE(p.username, 'Patient') AS title,
                       CONCAT('Rendez-vous avec ', COALESCE(duser.username, 'Medecin')) AS subtitle,
                       a.created_at AS event_date,
                       UPPER(COALESCE(a.status, 'CONFIRME')) AS badge
                FROM appointments a
                LEFT JOIN users p ON p.id_users = a.id_users
                LEFT JOIN doctors d ON d.id_doctors = a.id_doctors
                LEFT JOIN users duser ON duser.id_users = d.id_users
                UNION ALL
                SELECT 'DOCUMENT' AS source_type,
                       COALESCE(m.title, 'Document') AS title,
                       CONCAT('Ajoute pour ', COALESCE(u.username, 'Patient')) AS subtitle,
                       m.date AS event_date,
                       COALESCE(m.document_type, 'Document') AS badge
                FROM medical_records m
                LEFT JOIN users u ON u.id_users = m.id_users
                ORDER BY event_date DESC
                LIMIT 10
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("event_date");
                activity.add(new ActivityRecord(
                        safe(resultSet.getString("title"), "Activite"),
                        safe(resultSet.getString("subtitle"), ""),
                        timestamp == null ? "" : timestamp.toLocalDateTime().toString().replace('T', ' '),
                        safe(resultSet.getString("badge"), "Nouveau")
                ));
            }
        } catch (SQLException ignored) {
        }
        return activity;
    }

    public List<AdminUserRecord> getUsers() {
        List<AdminUserRecord> users = new ArrayList<>();
        String sql = """
                SELECT u.id_users,
                       u.username,
                       u.email,
                       u.phone,
                       u.account_status,
                       COUNT(a.id_appointments) AS appointments_count,
                       MAX(a.appointment_date) AS last_appointment_date
                FROM users u
                LEFT JOIN appointments a ON a.id_users = u.id_users
                WHERE u.role = 'USER'
                GROUP BY u.id_users, u.username, u.email, u.phone, u.account_status
                ORDER BY u.username
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Date lastAppointment = resultSet.getDate("last_appointment_date");
                String name = safe(resultSet.getString("username"), "Patient");
                users.add(new AdminUserRecord(
                        resultSet.getInt("id_users"),
                        name,
                        safe(resultSet.getString("email"), ""),
                        safe(resultSet.getString("phone"), ""),
                        safe(resultSet.getString("account_status"), "ACTIVE"),
                        initials(name),
                        resultSet.getInt("appointments_count"),
                        lastAppointment == null ? "Aucun RDV" : formatDate(lastAppointment.toLocalDate())
                ));
            }
        } catch (SQLException ignored) {
        }
        return users;
    }

    public List<AdminDoctorRecord> getDoctors() {
        List<AdminDoctorRecord> doctors = new ArrayList<>();
        String sql = """
                SELECT d.id_doctors,
                       u.id_users,
                       u.username,
                       u.email,
                       u.city,
                       u.account_status,
                       d.speciality,
                       d.sector,
                       d.accepts_vitale,
                       d.teleconsultation,
                       d.public_profile_completed
                FROM doctors d
                JOIN users u ON u.id_users = d.id_users
                ORDER BY u.username
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                boolean profileCompleted = resultSet.getBoolean("public_profile_completed");
                String accountStatus = safe(resultSet.getString("account_status"), "ACTIVE").toUpperCase(FRENCH);
                String status = "INACTIVE".equals(accountStatus)
                        ? "Inactif"
                        : (profileCompleted ? "Valide" : "En attente");
                String name = formatDoctorName(resultSet.getString("username"));
                doctors.add(new AdminDoctorRecord(
                        resultSet.getInt("id_doctors"),
                        resultSet.getInt("id_users"),
                        name,
                        safe(resultSet.getString("email"), ""),
                        safe(resultSet.getString("speciality"), "Specialite"),
                        safe(resultSet.getString("city"), "Ville"),
                        safe(resultSet.getString("sector"), "Secteur non renseigne"),
                        resultSet.getBoolean("accepts_vitale"),
                        resultSet.getBoolean("teleconsultation"),
                        status,
                        initials(name)
                ));
            }
        } catch (SQLException ignored) {
        }
        return doctors;
    }

    public List<AdminAppointmentRecord> getAppointments() {
        List<AdminAppointmentRecord> appointments = new ArrayList<>();
        String sql = """
                SELECT a.id_appointments,
                       a.appointment_date,
                       a.appointment_time,
                       a.status,
                       a.notes,
                       p.username AS patient_name,
                       duser.username AS doctor_name,
                       d.speciality
                FROM appointments a
                LEFT JOIN users p ON p.id_users = a.id_users
                LEFT JOIN doctors d ON d.id_doctors = a.id_doctors
                LEFT JOIN users duser ON duser.id_users = d.id_users
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Date date = resultSet.getDate("appointment_date");
                Time time = resultSet.getTime("appointment_time");
                String rawNotes = safe(resultSet.getString("notes"), "Consultation");
                appointments.add(new AdminAppointmentRecord(
                        resultSet.getInt("id_appointments"),
                        date == null ? "" : formatDate(date.toLocalDate()),
                        time == null ? "" : time.toLocalTime().toString(),
                        safe(resultSet.getString("patient_name"), "Patient"),
                        formatDoctorName(resultSet.getString("doctor_name")),
                        safe(resultSet.getString("speciality"), "Specialite"),
                        extractPurpose(rawNotes),
                        normalizeAppointmentStatus(resultSet.getString("status"))
                ));
            }
        } catch (SQLException ignored) {
        }
        return appointments;
    }

    public List<AdminDocumentRecord> getDocuments() {
        List<AdminDocumentRecord> documents = new ArrayList<>();
        String sql = """
                SELECT m.id_medical_records,
                       m.title,
                       m.document_type,
                       m.description,
                       m.file_path,
                       m.date,
                       p.username AS patient_name,
                       duser.username AS doctor_name
                FROM medical_records m
                LEFT JOIN users p ON p.id_users = m.id_users
                LEFT JOIN doctors d ON d.id_doctors = m.id_doctors
                LEFT JOIN users duser ON duser.id_users = d.id_users
                WHERE LOWER(COALESCE(m.title, '')) <> 'note medicale'
                ORDER BY m.date DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp date = resultSet.getTimestamp("date");
                documents.add(new AdminDocumentRecord(
                        resultSet.getInt("id_medical_records"),
                        safe(resultSet.getString("title"), "Document"),
                        safe(resultSet.getString("document_type"), "Document"),
                        safe(resultSet.getString("patient_name"), "Patient"),
                        formatDoctorName(resultSet.getString("doctor_name")),
                        date == null ? "" : formatDate(date.toLocalDateTime().toLocalDate()),
                        safe(resultSet.getString("description"), ""),
                        safe(resultSet.getString("file_path"), "")
                ));
            }
        } catch (SQLException ignored) {
        }
        return documents;
    }

    public List<AdminInvoiceRecord> getInvoices() {
        List<AdminInvoiceRecord> invoices = new ArrayList<>();
        String sql = """
                SELECT i.id_invoices,
                       i.amount,
                       i.description,
                       i.status,
                       i.payment_method,
                       i.paid_at,
                       u.username AS patient_name
                FROM invoices i
                LEFT JOIN users u ON u.id_users = i.id_users
                ORDER BY COALESCE(i.paid_at, CURRENT_TIMESTAMP) DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp paidAt = resultSet.getTimestamp("paid_at");
                invoices.add(new AdminInvoiceRecord(
                        resultSet.getInt("id_invoices"),
                        safe(resultSet.getString("patient_name"), "Patient"),
                        safe(resultSet.getString("description"), "Facture"),
                        resultSet.getBigDecimal("amount") == null ? BigDecimal.ZERO : resultSet.getBigDecimal("amount"),
                        safe(resultSet.getString("status"), "En attente"),
                        safe(resultSet.getString("payment_method"), "Non renseigne"),
                        paidAt == null ? "" : formatDate(paidAt.toLocalDateTime().toLocalDate())
                ));
            }
        } catch (SQLException ignored) {
        }
        return invoices;
    }

    public List<NotificationRecord> getNotifications() {
        List<NotificationRecord> notifications = new ArrayList<>();
        String sql = """
                SELECT n.id_notifications,
                       n.title,
                       n.message,
                       n.type,
                       n.is_read,
                       n.date,
                       u.username
                FROM notifications n
                LEFT JOIN users u ON u.id_users = n.id_users
                ORDER BY n.date DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp date = resultSet.getTimestamp("date");
                notifications.add(new NotificationRecord(
                        resultSet.getInt("id_notifications"),
                        safe(resultSet.getString("title"), "Notification"),
                        safe(resultSet.getString("message"), ""),
                        safe(resultSet.getString("type"), "Systeme"),
                        resultSet.getBoolean("is_read"),
                        safe(resultSet.getString("username"), "Plateforme"),
                        date == null ? "" : date.toLocalDateTime().toString().replace('T', ' ')
                ));
            }
        } catch (SQLException ignored) {
        }
        return notifications;
    }

    public SettingsRecord getAdminSettings() {
        Users user = AppSession.getCurrentUser();
        if (user == null) {
            return new SettingsRecord("", "", true, false, true, true, true);
        }

        String sql = """
                SELECT email, phone, notify_email, notify_sms, notify_new_appointments, notify_cancellations, notify_new_messages
                FROM users
                WHERE id_users = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, user.getIdUsers());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new SettingsRecord(
                            safe(resultSet.getString("email"), ""),
                            safe(resultSet.getString("phone"), ""),
                            resultSet.getBoolean("notify_email"),
                            resultSet.getBoolean("notify_sms"),
                            resultSet.getBoolean("notify_new_appointments"),
                            resultSet.getBoolean("notify_cancellations"),
                            resultSet.getBoolean("notify_new_messages")
                    );
                }
            }
        } catch (SQLException ignored) {
        }
        return new SettingsRecord(user.getEmail(), user.getPhone(), true, false, true, true, true);
    }

    public void updateUserStatus(int userId, String status) {
        update("""
                UPDATE users
                SET account_status = ?
                WHERE id_users = ? AND role <> 'ADMIN'
                """, statement -> {
            statement.setString(1, status);
            statement.setInt(2, userId);
        });
    }

    public void deleteUser(int userId) {
        update("DELETE FROM users WHERE id_users = ? AND role <> 'ADMIN'", statement -> statement.setInt(1, userId));
    }

    public void validateDoctor(int doctorId) {
        update("""
                UPDATE doctors d
                JOIN users u ON u.id_users = d.id_users
                SET d.public_profile_completed = TRUE,
                    u.account_status = 'ACTIVE'
                WHERE d.id_doctors = ?
                """, statement -> statement.setInt(1, doctorId));
    }

    public void refuseDoctor(int doctorId) {
        update("""
                UPDATE doctors d
                JOIN users u ON u.id_users = d.id_users
                SET d.public_profile_completed = FALSE,
                    u.account_status = 'INACTIVE'
                WHERE d.id_doctors = ?
                """, statement -> statement.setInt(1, doctorId));
    }

    public void deleteDoctorUser(int userId) {
        deleteUser(userId);
    }

    public void cancelAppointment(int appointmentId) {
        update("UPDATE appointments SET status = 'ANNULE' WHERE id_appointments = ?", statement -> statement.setInt(1, appointmentId));
    }

    public void updateAppointment(int appointmentId, LocalDate date, String time, String status, String notes) {
        update("""
                UPDATE appointments
                SET appointment_date = ?, appointment_time = ?, status = ?, notes = ?
                WHERE id_appointments = ?
                """, statement -> {
            statement.setDate(1, Date.valueOf(date));
            statement.setTime(2, Time.valueOf(time.length() == 5 ? time + ":00" : time));
            statement.setString(3, status);
            statement.setString(4, notes);
            statement.setInt(5, appointmentId);
        });
    }

    public void deleteDocument(int documentId) {
        update("DELETE FROM medical_records WHERE id_medical_records = ?", statement -> statement.setInt(1, documentId));
    }

    public void deleteInvoice(int invoiceId) {
        update("DELETE FROM invoices WHERE id_invoices = ?", statement -> statement.setInt(1, invoiceId));
    }

    public void markNotificationRead(int notificationId, boolean read) {
        update("UPDATE notifications SET is_read = ? WHERE id_notifications = ?", statement -> {
            statement.setBoolean(1, read);
            statement.setInt(2, notificationId);
        });
    }

    public void updateAdminContact(String email, String phone) {
        Users currentUser = requireCurrentUser();
        update("UPDATE users SET email = ?, phone = ? WHERE id_users = ?", statement -> {
            statement.setString(1, email);
            statement.setString(2, phone);
            statement.setInt(3, currentUser.getIdUsers());
        });
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
    }

    public boolean updateAdminPassword(String currentPassword, String newPassword) {
        Users currentUser = requireCurrentUser();
        String sql = "SELECT password FROM users WHERE id_users = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, currentUser.getIdUsers());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && PasswordUtil.matches(currentPassword, resultSet.getString("password"))) {
                    update("UPDATE users SET password = ? WHERE id_users = ?", updateStatement -> {
                        updateStatement.setString(1, PasswordUtil.hash(newPassword));
                        updateStatement.setInt(2, currentUser.getIdUsers());
                    });
                    return true;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public void updateAdminNotifications(SettingsRecord settings) {
        Users currentUser = requireCurrentUser();
        update("""
                UPDATE users
                SET notify_email = ?,
                    notify_sms = ?,
                    notify_new_appointments = ?,
                    notify_cancellations = ?,
                    notify_new_messages = ?
                WHERE id_users = ?
                """, statement -> {
            statement.setBoolean(1, settings.notifyEmail());
            statement.setBoolean(2, settings.notifySms());
            statement.setBoolean(3, settings.notifyNewAppointments());
            statement.setBoolean(4, settings.notifyCancellations());
            statement.setBoolean(5, settings.notifyNewMessages());
            statement.setInt(6, currentUser.getIdUsers());
        });
    }

    private int count(String sql) {
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private void update(String sql, SqlConsumer consumer) {
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            consumer.accept(statement);
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private Users requireCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Aucun administrateur connecte.");
        }
        return user;
    }

    private String normalizeAppointmentStatus(String rawStatus) {
        String status = safe(rawStatus, "CONFIRME").toUpperCase(FRENCH);
        return switch (status) {
            case "ANNULE" -> "Annule";
            case "TERMINE" -> "Termine";
            default -> "Confirme";
        };
    }

    private String extractPurpose(String notes) {
        if (notes == null || notes.isBlank()) {
            return "Consultation";
        }
        int pipeIndex = notes.indexOf('|');
        return pipeIndex >= 0 ? notes.substring(0, pipeIndex).trim() : notes.trim();
    }

    private String formatDoctorName(String name) {
        String safeName = safe(name, "Medecin");
        return safeName.startsWith("Dr.") ? safeName : "Dr. " + safeName;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) {
            return "??";
        }
        String[] parts = name.trim().replace("Dr. ", "").split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(FRENCH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(FRENCH);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(FRENCH) + value.substring(1);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement statement) throws SQLException;
    }

    public record DashboardSummary(
            int totalPatients,
            int totalDoctors,
            int confirmedThisMonth,
            int cancelledThisMonth,
            int todayAppointments,
            int waitlistCount
    ) {}

    public record MonthlyAppointmentStat(String monthLabel, int confirmedCount, int cancelledCount) {
        public int total() {
            return confirmedCount + cancelledCount;
        }
    }

    public record ActivityRecord(String title, String subtitle, String dateTime, String badge) {}

    public record AdminUserRecord(
            int id,
            String name,
            String email,
            String phone,
            String status,
            String initials,
            int appointmentsCount,
            String lastAppointment
    ) {}

    public record AdminDoctorRecord(
            int doctorId,
            int userId,
            String name,
            String email,
            String speciality,
            String city,
            String sector,
            boolean acceptsVitale,
            boolean teleconsultation,
            String status,
            String initials
    ) {}

    public record AdminAppointmentRecord(
            int id,
            String date,
            String time,
            String patientName,
            String doctorName,
            String speciality,
            String purpose,
            String status
    ) {}

    public record AdminDocumentRecord(
            int id,
            String title,
            String type,
            String patientName,
            String doctorName,
            String date,
            String description,
            String filePath
    ) {}

    public record AdminInvoiceRecord(
            int id,
            String patientName,
            String description,
            BigDecimal amount,
            String status,
            String paymentMethod,
            String paidAt
    ) {}

    public record NotificationRecord(
            int id,
            String title,
            String message,
            String type,
            boolean isRead,
            String targetUser,
            String dateTime
    ) {}

    public record SettingsRecord(
            String email,
            String phone,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyNewAppointments,
            boolean notifyCancellations,
            boolean notifyNewMessages
    ) {}
}
