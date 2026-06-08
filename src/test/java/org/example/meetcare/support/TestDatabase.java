package org.example.meetcare.support;

import org.h2.tools.RunScript;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.NotificationDao;
import org.example.meetcare.utils.DatabaseConnection;
import org.example.meetcare.utils.PasswordUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestDatabase {
    private TestDatabase() {
    }

    public static SeedData initialize(String databaseName) {
        String url = "jdbc:h2:mem:" + sanitize(databaseName)
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        System.setProperty("MEETCARE_DB_URL", url);
        System.setProperty("MEETCARE_DB_USER", "sa");
        System.setProperty("MEETCARE_DB_PASSWORD", "");

        resetSchemaFlags();
        DatabaseConnection.closeConnection();
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            runSchema(connection);
            SeedData seed = seed(connection);
            new DoctorDao();
            new AppointmentDao();
            new NotificationDao();
            DatabaseConnection.closeConnection();
            return seed;
        } catch (Exception ex) {
            DatabaseConnection.closeConnection();
            throw new IllegalStateException("Impossible d'initialiser la base de test.", ex);
        }
    }

    public static void resetConnection() {
        DatabaseConnection.closeConnection();
    }

    private static void resetSchemaFlags() {
        resetFlag("org.example.meetcare.dao.DoctorDao", "schemaChecked");
        resetFlag("org.example.meetcare.dao.AppointmentDao", "schemaChecked");
        resetFlag("org.example.meetcare.dao.AvailabilityBlockDao", "schemaChecked");
        resetFlag("org.example.meetcare.dao.NotificationDao", "schemaChecked");
        resetFlag("org.example.meetcare.dao.MedicalRecordDao", "schemaChecked");
    }

    private static void resetFlag(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            java.lang.reflect.Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(null, false);
        } catch (Exception ignored) {
            // Best effort only. Some classes may not exist in all builds.
        }
    }

    private static void runSchema(Connection connection) throws Exception {
        try (InputStream inputStream = TestDatabase.class.getResourceAsStream("/sql/h2-schema.sql")) {
            if (inputStream == null) {
                throw new IllegalStateException("Le schema de test est introuvable.");
            }
            RunScript.execute(connection, new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
    }

    private static SeedData seed(Connection connection) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        int adminUserId = insertUser(connection, "admin", "Alice", "admin@meetcare.test", "admin-pass", "0600000000",
                "admin-avatar.png", Role.ADMIN.name(), AccountStatus.VERIFIED.name(),
                "1 rue de l'Admin", "Paris", "75000", "France", now.minusDays(12), now.minusDays(12));
        int patientUserId = insertUser(connection, "patient", "Paul", "patient@meetcare.test", "patient-pass", "0600000001",
                "patient-avatar.png", Role.USER.name(), AccountStatus.VERIFIED.name(),
                "2 rue du Patient", "Lyon", "69000", "France", now.minusDays(8), now.minusDays(8));
        int verifiedDoctorUserId = insertUser(connection, "martin", "Antoine", "dr.martin@meetcare.test", "doctor-pass", "0600000002",
                "doctor-avatar.png", Role.MEDECIN.name(), AccountStatus.VERIFIED.name(),
                "12 rue Victor-Hugo", "Paris", "75001", "France", now.minusDays(10), now.minusDays(10));
        int pendingDoctorUserId = insertUser(connection, "dupont", "Claire", "dr.dupont@meetcare.test", "doctor-pass", "0600000003",
                "doctor-avatar.png", Role.MEDECIN.name(), AccountStatus.PENDING.name(),
                "8 avenue Foch", "Lyon", "69002", "France", now.minusDays(5), now.minusDays(5));
        int rejectedDoctorUserId = insertUser(connection, "faure", "Laura", "dr.faure@meetcare.test", "doctor-pass", "0600000004",
                "doctor-avatar.png", Role.MEDECIN.name(), AccountStatus.REJECTED.name(),
                "6 avenue du Parc", "Nice", "06000", "France", now.minusDays(4), now.minusDays(4));
        int autoDoctorUserId = insertUser(connection, "newdoc", "Nadia", "dr.new@meetcare.test", "doctor-pass", "0600000005",
                "doctor-avatar.png", Role.MEDECIN.name(), AccountStatus.VERIFIED.name(),
                "18 rue des Tests", "Toulouse", "31000", "France", now.minusDays(2), now.minusDays(2));

        int verifiedDoctorId = insertDoctor(connection, verifiedDoctorUserId);
        int pendingDoctorId = insertDoctor(connection, pendingDoctorUserId);
        int rejectedDoctorId = insertDoctor(connection, rejectedDoctorUserId);
        insertDoctor(connection, autoDoctorUserId);

        int appointmentTodayId = insertAppointment(connection, verifiedDoctorId, patientUserId, today, "09:00",
                "CONFIRME", "Contrôle annuel");
        int appointmentPendingId = insertAppointment(connection, verifiedDoctorId, patientUserId, today.minusDays(1), "10:30",
                "EN_ATTENTE", "Premier rendez-vous");
        int appointmentCancelledId = insertAppointment(connection, verifiedDoctorId, patientUserId, today.minusDays(2), "11:15",
                "ANNULE", "Annulé par le patient");
        int appointmentOldId = insertAppointment(connection, verifiedDoctorId, patientUserId, today.minusDays(8), "15:00",
                "CONFIRME", "Hors fenêtre");
        int appointmentUrgentId = insertAppointment(connection, verifiedDoctorId, patientUserId, today.minusDays(3), "16:45",
                "CONFIRME", "Douleur aiguë");

        int unreadNotificationId = insertNotification(connection, patientUserId, "Nouveau message",
                "Vous avez reçu un message.", "MESSAGE", false, now.minusHours(2));
        insertNotification(connection, patientUserId, "Rendez-vous confirmé",
                "Votre rendez-vous est validé.", "APPOINTMENT", true, now.minusDays(1));
        insertNotification(connection, patientUserId, "Facture disponible",
                "Une nouvelle facture est disponible.", "PAYMENT", false, now.minusMinutes(30));

        int invoicePendingId = insertInvoice(connection, verifiedDoctorId, patientUserId, 70.00,
                "Consultation cardiologie", "EN_ATTENTE", null, null, now.minusDays(1));
        int invoicePaidId = insertInvoice(connection, verifiedDoctorId, patientUserId, 35.00,
                "Suivi", "PAYEE", "CB", now.minusHours(6), now.minusDays(3));

        int approvedDocumentId = insertDoctorDocument(connection, verifiedDoctorId, "CARTE_VITALE",
                "carte-vitale.pdf", "/tmp/carte-vitale.pdf", "APPROVED", now.minusDays(7));
        int pendingDocumentId = insertDoctorDocument(connection, pendingDoctorId, "DIPLOMA",
                "diplome.pdf", "/tmp/diplome.pdf", "PENDING", now.minusDays(1));
        int rejectedDocumentId = insertDoctorDocument(connection, rejectedDoctorId, "LICENSE",
                "license.pdf", "/tmp/license.pdf", "REJECTED", now.minusDays(2));
        int secondPendingDocumentId = insertDoctorDocument(connection, pendingDoctorId, "ID_CARD",
                "id-card.pdf", "/tmp/id-card.pdf", "PENDING", now.minusHours(12));

        insertVerification(connection, rejectedDoctorId, adminUserId, "REJECTED",
                "Dossier incomplet", now.minusDays(2));

        return new SeedData(
                adminUserId,
                patientUserId,
                verifiedDoctorUserId,
                pendingDoctorUserId,
                rejectedDoctorUserId,
                autoDoctorUserId,
                verifiedDoctorId,
                pendingDoctorId,
                rejectedDoctorId,
                appointmentTodayId,
                appointmentPendingId,
                appointmentCancelledId,
                appointmentOldId,
                appointmentUrgentId,
                unreadNotificationId,
                invoicePendingId,
                invoicePaidId,
                approvedDocumentId,
                pendingDocumentId,
                rejectedDocumentId,
                secondPendingDocumentId
        );
    }

    private static int insertUser(Connection connection,
                                  String username,
                                  String prenom,
                                  String email,
                                  String password,
                                  String phone,
                                  String avatar,
                                  String role,
                                  String accountStatus,
                                  String address,
                                  String city,
                                  String codePostal,
                                  String country,
                                  LocalDateTime lastSeen,
                                  LocalDateTime createdAt) throws Exception {
        String sql = """
                INSERT INTO users (username, prenom, email, password, phone, avatar, role, account_status, address, city, code_postal, country, is_online, last_seen, `date`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, prenom);
            statement.setString(3, email);
            statement.setString(4, PasswordUtil.hash(password));
            statement.setString(5, phone);
            statement.setString(6, avatar);
            statement.setString(7, role);
            statement.setString(8, accountStatus);
            statement.setString(9, address);
            statement.setString(10, city);
            statement.setString(11, codePostal);
            statement.setString(12, country);
            statement.setTimestamp(13, java.sql.Timestamp.valueOf(lastSeen));
            statement.setTimestamp(14, java.sql.Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int insertDoctor(Connection connection,
                                    int userId) throws Exception {
        String sql = """
                INSERT INTO doctors (id_users)
                VALUES (?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int insertAppointment(Connection connection,
                                         int doctorId,
                                         int patientUserId,
                                         LocalDate date,
                                         String time,
                                         String status,
                                         String notes) throws Exception {
        String sql = """
                INSERT INTO appointments (appointment_date, appointment_time, status, notes, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDate(1, Date.valueOf(date));
            statement.setTime(2, java.sql.Time.valueOf(java.time.LocalTime.parse(time)));
            statement.setString(3, status);
            statement.setString(4, notes);
            statement.setInt(5, doctorId);
            statement.setInt(6, patientUserId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int insertNotification(Connection connection,
                                          int userId,
                                          String title,
                                          String message,
                                          String type,
                                          boolean isRead,
                                          LocalDateTime createdAt) throws Exception {
        String sql = """
                INSERT INTO notifications (title, message, type, is_read, `date`, id_users)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, message);
            statement.setString(3, type);
            statement.setBoolean(4, isRead);
            statement.setTimestamp(5, java.sql.Timestamp.valueOf(createdAt));
            statement.setInt(6, userId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int insertInvoice(Connection connection,
                                     int doctorId,
                                     int patientUserId,
                                     double amount,
                                     String description,
                                     String status,
                                     String paymentMethod,
                                     LocalDateTime paidAt,
                                     LocalDateTime createdAt) throws Exception {
        String sql = """
                INSERT INTO invoices (amount, description, status, payment_method, paid_at, `date`, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setBigDecimal(1, java.math.BigDecimal.valueOf(amount));
            statement.setString(2, description);
            statement.setString(3, status);
            statement.setString(4, paymentMethod);
            if (paidAt == null) {
                statement.setNull(5, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(5, java.sql.Timestamp.valueOf(paidAt));
            }
            statement.setTimestamp(6, java.sql.Timestamp.valueOf(createdAt));
            statement.setInt(7, doctorId);
            statement.setInt(8, patientUserId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int insertDoctorDocument(Connection connection,
                                            int doctorId,
                                            String documentType,
                                            String fileName,
                                            String filePath,
                                            String status,
                                            LocalDateTime uploadedAt) throws Exception {
        String sql = """
                INSERT INTO doctor_documents (document_type, file_name, file_path, verification_status, uploaded_at, id_doctors)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, documentType);
            statement.setString(2, fileName);
            statement.setString(3, filePath);
            statement.setString(4, status);
            statement.setTimestamp(5, java.sql.Timestamp.valueOf(uploadedAt));
            statement.setInt(6, doctorId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int insertVerification(Connection connection,
                                          int doctorId,
                                          int adminUserId,
                                          String decision,
                                          String comment,
                                          LocalDateTime createdAt) throws Exception {
        String sql = """
                INSERT INTO doctor_verifications (decision, comment, created_at, id_doctors, id_admin)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, decision);
            statement.setString(2, comment);
            statement.setTimestamp(3, java.sql.Timestamp.valueOf(createdAt));
            statement.setInt(4, doctorId);
            statement.setInt(5, adminUserId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static int generatedId(PreparedStatement statement) throws Exception {
        try (ResultSet resultSet = statement.getGeneratedKeys()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        throw new IllegalStateException("Aucune cle generee.");
    }

    private static String sanitize(String name) {
        return name == null || name.isBlank() ? "meetcare_test" : name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public record SeedData(
            int adminUserId,
            int patientUserId,
            int verifiedDoctorUserId,
            int pendingDoctorUserId,
            int rejectedDoctorUserId,
            int autoDoctorUserId,
            int verifiedDoctorId,
            int pendingDoctorId,
            int rejectedDoctorId,
            int appointmentTodayId,
            int appointmentPendingId,
            int appointmentCancelledId,
            int appointmentOldId,
            int appointmentUrgentId,
            int unreadNotificationId,
            int invoicePendingId,
            int invoicePaidId,
            int approvedDocumentId,
            int pendingDocumentId,
            int rejectedDocumentId,
            int secondPendingDocumentId
    ) {
    }
}
