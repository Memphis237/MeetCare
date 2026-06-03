package org.example.meetcare.dao;

import org.example.meetcare.exception.DatabaseException;
import org.example.meetcare.model.DoctorDocumentEntry;
import org.example.meetcare.utils.DatabaseConnection;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DoctorDocumentDao {
    private final DoctorDao doctorDao = new DoctorDao();

    public int saveUploadedDocument(int userId, File file, String documentType) {
        int doctorId = doctorDao.ensureDoctorIdForUser(userId);
        String sql = """
                INSERT INTO doctor_documents (document_type, file_name, file_path, verification_status, uploaded_at, id_doctors)
                VALUES (?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, documentType);
            stmt.setString(2, file.getName());
            stmt.setString(3, file.getAbsolutePath());
            stmt.setInt(4, doctorId);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible d'enregistrer le document.", e);
        }
        return 0;
    }

    public List<DoctorDocumentEntry> findByDoctorUserId(int userId) {
        String sql = """
                SELECT dd.id_doctor_documents, dd.document_type, dd.file_name, dd.file_path,
                       dd.verification_status, dd.uploaded_at, dd.id_doctors,
                       u.id_users, u.username, u.email
                FROM doctor_documents dd
                JOIN doctors d ON d.id_doctors = dd.id_doctors
                JOIN users u ON u.id_users = d.id_users
                WHERE u.id_users = ?
                ORDER BY dd.uploaded_at DESC, dd.id_doctor_documents DESC
                """;
        List<DoctorDocumentEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de charger les documents medecin.", e);
        }
        return entries;
    }

    public List<DoctorDocumentEntry> findPendingDocumentsForAdmin() {
        String sql = """
                SELECT dd.id_doctor_documents, dd.document_type, dd.file_name, dd.file_path,
                       dd.verification_status, dd.uploaded_at, dd.id_doctors,
                       u.id_users, u.username, u.email
                FROM doctor_documents dd
                JOIN doctors d ON d.id_doctors = dd.id_doctors
                JOIN users u ON u.id_users = d.id_users
                WHERE u.role = 'MEDECIN'
                  AND EXISTS (
                        SELECT 1
                        FROM doctor_documents pending_dd
                        WHERE pending_dd.id_doctors = dd.id_doctors
                          AND pending_dd.verification_status = 'PENDING'
                  )
                ORDER BY u.username, dd.uploaded_at DESC, dd.id_doctor_documents DESC
                """;
        List<DoctorDocumentEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                entries.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de charger les validations en attente.", e);
        }
        return entries;
    }

    public void setVerificationStatusForDoctor(int doctorId, String status) {
        String sql = "UPDATE doctor_documents SET verification_status = ? WHERE id_doctors = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, doctorId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de mettre a jour le statut des documents.", e);
        }
    }

    public void setVerificationStatusForDocuments(List<Integer> documentIds, String status) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(documentIds.size(), "?"));
        String sql = "UPDATE doctor_documents SET verification_status = ? WHERE id_doctor_documents IN (" + placeholders + ")";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            for (int i = 0; i < documentIds.size(); i++) {
                stmt.setInt(i + 2, documentIds.get(i));
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de mettre a jour le statut des documents selectionnes.", e);
        }
    }

    public int countPendingDoctorsForAdmin() {
        String sql = """
                SELECT COUNT(DISTINCT dd.id_doctors) AS pending_count
                FROM doctor_documents dd
                WHERE dd.verification_status = 'PENDING'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("pending_count");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de compter les dossiers a valider.", e);
        }
        return 0;
    }

    public int countRejectedDocumentsForDoctorUserId(int userId) {
        String sql = """
                SELECT COUNT(*) AS rejected_count
                FROM doctor_documents dd
                JOIN doctors d ON d.id_doctors = dd.id_doctors
                WHERE d.id_users = ?
                  AND dd.verification_status = 'REJECTED'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rejected_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de compter les documents refuses.", e);
        }
        return 0;
    }

    private DoctorDocumentEntry map(ResultSet rs) throws SQLException {
        DoctorDocumentEntry entry = new DoctorDocumentEntry();
        entry.setIdDoctorDocument(rs.getInt("id_doctor_documents"));
        entry.setDocumentType(rs.getString("document_type"));
        entry.setFileName(rs.getString("file_name"));
        entry.setFilePath(rs.getString("file_path"));
        entry.setVerificationStatus(rs.getString("verification_status"));
        entry.setDoctorId(rs.getInt("id_doctors"));
        entry.setUserId(rs.getInt("id_users"));
        entry.setUsername(rs.getString("username"));
        entry.setEmail(rs.getString("email"));
        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        if (uploadedAt != null) {
            entry.setUploadedAt(uploadedAt.toLocalDateTime());
        }
        return entry;
    }
}
