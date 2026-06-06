package org.example.meetcare.dao;

import org.example.meetcare.model.Doctor;
import java.util.Optional;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {

    public Optional<Integer> findDoctorIdByUserId(int userId) {
        String sql = "SELECT id_doctors FROM doctors WHERE id_users = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id_doctors"));
                }
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public int ensureDoctorIdForUser(int userId) {
        Optional<Integer> existing = findDoctorIdByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String sql = "INSERT INTO doctors (id_users) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer le dossier medecin.", e);
        }
        throw new IllegalStateException("Impossible de creer le dossier medecin.");
    }

    public Optional<Doctor> findByUserId(int userId) {
        String sql = """
            SELECT d.id_doctors, u.username, u.email, u.city,
                   d.speciality, d.address, d.sector,
                   d.consultation_price, d.accepts_vitale, d.teleconsultation
            FROM doctors d
            JOIN users u ON d.id_users = u.id_users
            WHERE d.id_users = ?
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Doctor(
                            rs.getInt("id_doctors"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("city"),
                            rs.getString("speciality"),
                            rs.getString("address"),
                            rs.getString("sector"),
                            rs.getDouble("consultation_price"),
                            0,
                            "",
                            rs.getBoolean("accepts_vitale"),
                            rs.getBoolean("teleconsultation")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<Doctor> searchDoctors(String keyword, String city) {
        List<Doctor> doctors = new ArrayList<>();

        String sql = """
            SELECT d.id_doctors, u.username, u.email, u.city,
                   d.speciality, d.address, d.sector,
                   d.consultation_price, d.experience, d.biography,
                   d.accepts_vitale, d.teleconsultation
            FROM doctors d
            JOIN users u ON d.id_users = u.id_users
            WHERE u.role = 'MEDECIN'
            AND (
                ? = ''
                OR LOWER(u.username) LIKE LOWER(?)
                OR LOWER(d.speciality) LIKE LOWER(?)
            )
            AND (
                ? = ''
                OR LOWER(u.city) LIKE LOWER(?)
            )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String keywordLike = "%" + keyword + "%";
            String cityLike = "%" + city + "%";

            stmt.setString(1, keyword);
            stmt.setString(2, keywordLike);
            stmt.setString(3, keywordLike);
            stmt.setString(4, city);
            stmt.setString(5, cityLike);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Doctor doctor = new Doctor(
                        rs.getInt("id_doctors"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("city"),
                        rs.getString("speciality"),
                        rs.getString("address"),
                        rs.getString("sector"),
                        rs.getDouble("consultation_price"),
                        rs.getInt("experience"),
                        rs.getString("biography"),
                        rs.getBoolean("accepts_vitale"),
                        rs.getBoolean("teleconsultation")
                );

                doctors.add(doctor);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return doctors;
    }

    public void saveVerificationDecision(int doctorId, int adminId, String decision, String comment) {
        String sql = "INSERT INTO doctor_verifications (decision, comment, id_doctors, id_admin) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, decision);
            stmt.setString(2, comment);
            stmt.setInt(3, doctorId);
            stmt.setInt(4, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'enregistrer la validation.", e);
        }
    }

    public java.util.Optional<String> findLatestRejectionReasonByUserId(int userId) {
        String sql = """
                SELECT dv.comment
                FROM doctor_verifications dv
                JOIN doctors d ON d.id_doctors = dv.id_doctors
                WHERE d.id_users = ?
                  AND dv.decision = 'REJECTED'
                ORDER BY dv.created_at DESC, dv.id_verification DESC
                LIMIT 1
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String comment = rs.getString("comment");
                    if (comment != null && !comment.isBlank()) {
                        return java.util.Optional.of(comment.trim());
                    }
                }
            }
        } catch (SQLException e) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.empty();
    }

    public void markPublicProfileCompleted(int doctorId) {
        String sql = "UPDATE doctors SET public_profile_completed = TRUE WHERE id_doctors = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour le profil public.", e);
        }
    }
}
