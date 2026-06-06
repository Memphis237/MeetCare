package org.example.meetcare.dao;

import org.example.meetcare.model.Doctor;
import java.util.Optional;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {

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
}
