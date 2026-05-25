package org.example.meetcare.dao;

import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DoctorAccountDao {
    public DoctorPublicProfileRecord getDoctorPublicProfile() {
        String sql = """
                SELECT u.id_users, u.prenom, u.username, u.email, u.phone, u.avatar, u.city,
                       u.account_status,
                       d.id_doctors, d.speciality, d.address, d.sector, d.consultation_price,
                       d.biography, d.experience, d.accepts_vitale, d.teleconsultation,
                       COALESCE(d.payment_methods, '') AS payment_methods,
                       COALESCE(d.spoken_languages, '') AS spoken_languages,
                       COALESCE(d.office_hours, '') AS office_hours,
                       d.public_profile_completed
                FROM users u
                JOIN doctors d ON d.id_users = u.id_users
                WHERE u.id_users = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveCurrentDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String fullName = safe(resultSet.getString("username"), "");
                    String firstName = safe(resultSet.getString("prenom"), "");
                    String lastName = extractLastName(fullName, firstName);
                    return new DoctorPublicProfileRecord(
                            resultSet.getInt("id_users"),
                            resultSet.getInt("id_doctors"),
                            firstName,
                            lastName,
                            safe(resultSet.getString("email"), ""),
                            safe(resultSet.getString("phone"), ""),
                            resultSet.getString("avatar"),
                            safe(resultSet.getString("speciality"), ""),
                            safe(resultSet.getString("city"), ""),
                            safe(resultSet.getString("address"), ""),
                            safe(resultSet.getString("biography"), ""),
                            safe(resultSet.getString("sector"), ""),
                            resultSet.getDouble("consultation_price"),
                            resultSet.getBoolean("accepts_vitale"),
                            resultSet.getBoolean("teleconsultation"),
                            safe(resultSet.getString("payment_methods"), ""),
                            safe(resultSet.getString("spoken_languages"), ""),
                            safe(resultSet.getString("office_hours"), ""),
                            resultSet.getInt("experience"),
                            resultSet.getBoolean("public_profile_completed"),
                            safe(resultSet.getString("account_status"), "ACTIVE")
                    );
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public DoctorSettingsRecord getDoctorSettings() {
        String sql = """
                SELECT u.id_users, u.prenom, u.username, u.email, u.phone, u.avatar, u.account_status,
                       COALESCE(u.notify_email, TRUE) AS notify_email,
                       COALESCE(u.notify_sms, FALSE) AS notify_sms,
                       COALESCE(u.notify_new_appointments, TRUE) AS notify_new_appointments,
                       COALESCE(u.notify_cancellations, TRUE) AS notify_cancellations,
                       COALESCE(u.notify_new_messages, TRUE) AS notify_new_messages
                FROM users u
                WHERE u.id_users = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, resolveCurrentDoctorUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new DoctorSettingsRecord(
                            resultSet.getInt("id_users"),
                            safe(resultSet.getString("prenom"), ""),
                            safe(resultSet.getString("username"), ""),
                            safe(resultSet.getString("email"), ""),
                            safe(resultSet.getString("phone"), ""),
                            resultSet.getString("avatar"),
                            resultSet.getBoolean("notify_email"),
                            resultSet.getBoolean("notify_sms"),
                            resultSet.getBoolean("notify_new_appointments"),
                            resultSet.getBoolean("notify_cancellations"),
                            resultSet.getBoolean("notify_new_messages"),
                            safe(resultSet.getString("account_status"), "ACTIVE")
                    );
                }
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public void updateDoctorPublicProfile(DoctorPublicProfileForm form) {
        String updateUserSql = """
                UPDATE users
                SET prenom = ?, username = ?, email = ?, phone = ?, city = ?
                WHERE id_users = ?
                """;
        String updateDoctorSql = """
                UPDATE doctors
                SET speciality = ?, address = ?, biography = ?, sector = ?, consultation_price = ?,
                    accepts_vitale = ?, teleconsultation = ?, payment_methods = ?, spoken_languages = ?,
                    office_hours = ?, experience = ?, public_profile_completed = TRUE
                WHERE id_users = ?
                """;

        try (Connection connection = getConnection()) {
            try (PreparedStatement userStatement = connection.prepareStatement(updateUserSql)) {
                userStatement.setString(1, form.firstName());
                userStatement.setString(2, buildUsername(form.firstName(), form.lastName()));
                userStatement.setString(3, form.email());
                userStatement.setString(4, form.phone());
                userStatement.setString(5, form.city());
                userStatement.setInt(6, resolveCurrentDoctorUserId());
                userStatement.executeUpdate();
            }

            try (PreparedStatement doctorStatement = connection.prepareStatement(updateDoctorSql)) {
                doctorStatement.setString(1, form.speciality());
                doctorStatement.setString(2, form.address());
                doctorStatement.setString(3, form.biography());
                doctorStatement.setString(4, form.sector());
                doctorStatement.setDouble(5, form.consultationPrice());
                doctorStatement.setBoolean(6, form.acceptsVitale());
                doctorStatement.setBoolean(7, form.teleconsultation());
                doctorStatement.setString(8, form.paymentMethods());
                doctorStatement.setString(9, form.spokenLanguages());
                doctorStatement.setString(10, form.officeHours());
                doctorStatement.setInt(11, form.experience());
                doctorStatement.setInt(12, resolveCurrentDoctorUserId());
                doctorStatement.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    public void updateDoctorContactSettings(String email, String phone) {
        String sql = """
                UPDATE users
                SET email = ?, phone = ?
                WHERE id_users = ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, phone);
            statement.setInt(3, resolveCurrentDoctorUserId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void updateNotificationSettings(boolean notifyEmail, boolean notifySms,
                                           boolean notifyNewAppointments, boolean notifyCancellations,
                                           boolean notifyNewMessages) {
        String sql = """
                UPDATE users
                SET notify_email = ?, notify_sms = ?, notify_new_appointments = ?,
                    notify_cancellations = ?, notify_new_messages = ?
                WHERE id_users = ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setBoolean(1, notifyEmail);
            statement.setBoolean(2, notifySms);
            statement.setBoolean(3, notifyNewAppointments);
            statement.setBoolean(4, notifyCancellations);
            statement.setBoolean(5, notifyNewMessages);
            statement.setInt(6, resolveCurrentDoctorUserId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void updateAccountStatus(String status) {
        String sql = "UPDATE users SET account_status = ? WHERE id_users = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, resolveCurrentDoctorUserId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private int resolveCurrentDoctorUserId() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser != null && currentUser.getIdUsers() > 0) {
            return currentUser.getIdUsers();
        }
        return 1;
    }

    private String extractLastName(String fullName, String firstName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String trimmed = fullName.trim();
        if (firstName != null && !firstName.isBlank() && trimmed.startsWith(firstName.trim())) {
            String suffix = trimmed.substring(firstName.trim().length()).trim();
            return suffix.isBlank() ? trimmed : suffix;
        }
        String[] parts = trimmed.split("\\s+", 2);
        return parts.length > 1 ? parts[1] : trimmed;
    }

    private String buildUsername(String firstName, String lastName) {
        String left = safe(firstName, "").trim();
        String right = safe(lastName, "").trim();
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + " " + right;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public record DoctorPublicProfileRecord(
            int userId,
            int doctorId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String avatarPath,
            String speciality,
            String city,
            String address,
            String biography,
            String sector,
            double consultationPrice,
            boolean acceptsVitale,
            boolean teleconsultation,
            String paymentMethods,
            String spokenLanguages,
            String officeHours,
            int experience,
            boolean publicProfileCompleted,
            String accountStatus
    ) {
    }

    public record DoctorPublicProfileForm(
            String firstName,
            String lastName,
            String email,
            String phone,
            String speciality,
            String city,
            String address,
            String biography,
            String sector,
            double consultationPrice,
            boolean acceptsVitale,
            boolean teleconsultation,
            String paymentMethods,
            String spokenLanguages,
            String officeHours,
            int experience
    ) {
    }

    public record DoctorSettingsRecord(
            int userId,
            String firstName,
            String fullName,
            String email,
            String phone,
            String avatarPath,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyNewAppointments,
            boolean notifyCancellations,
            boolean notifyNewMessages,
            String accountStatus
    ) {
    }
}
