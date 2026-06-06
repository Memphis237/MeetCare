package org.example.meetcare.dao;

import org.example.meetcare.model.MedicalRecord;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDao {
    private static volatile boolean schemaChecked;

    public MedicalRecordDao() {
        ensureSchema();
    }

    private void ensureSchema() {
        if (schemaChecked) {
            return;
        }

        synchronized (MedicalRecordDao.class) {
            if (schemaChecked) {
                return;
            }

            String tableName = "medical_records";
            String columnName = "document_type";

            try (Connection conn = DatabaseConnection.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
                    if (columns.next()) {
                        schemaChecked = true;
                        return;
                    }
                }

                try (Statement statement = conn.createStatement()) {
                    statement.executeUpdate(
                            "ALTER TABLE medical_records ADD COLUMN document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER' AFTER description"
                    );
                }
                schemaChecked = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Impossible de verifier ou mettre a jour le schema medical_records.", ex);
            }
        }
    }

    public int insert(MedicalRecord record) throws SQLException {
        String sql = "INSERT INTO medical_records(title, description, document_type, file_path, id_doctors, id_users) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getTitle());
            ps.setString(2, record.getDescription());
            ps.setString(3, record.getDocumentType());
            ps.setString(4, record.getFilePath());
            if (record.getIdDoctors() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, record.getIdDoctors());
            }
            ps.setInt(6, record.getIdUsers());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<MedicalRecord> findByUserId(int userId) throws SQLException {
        String sql = "SELECT id_medical_records, title, description, document_type, file_path, date, id_doctors, id_users FROM medical_records WHERE id_users = ? ORDER BY date DESC";
        List<MedicalRecord> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MedicalRecord r = new MedicalRecord();
                    r.setId(rs.getInt("id_medical_records"));
                    r.setTitle(rs.getString("title"));
                    r.setDescription(rs.getString("description"));
                    r.setDocumentType(rs.getString("document_type"));
                    r.setFilePath(rs.getString("file_path"));
                    Timestamp ts = rs.getTimestamp("date");
                    if (ts != null) {
                        r.setDate(ts.toLocalDateTime());
                    }
                    int did = rs.getInt("id_doctors");
                    if (rs.wasNull()) {
                        r.setIdDoctors(null);
                    } else {
                        r.setIdDoctors(did);
                    }
                    r.setIdUsers(rs.getInt("id_users"));
                    results.add(r);
                }
            }
        }
        return results;
    }
}
