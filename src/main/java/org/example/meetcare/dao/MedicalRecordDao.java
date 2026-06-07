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

            try (Connection conn = DatabaseConnection.getConnection()) {
                ensureColumn(conn, "medical_records", "document_type",
                        "VARCHAR(50) NOT NULL DEFAULT 'OTHER'");
                ensureColumn(conn, "medical_records", "share_consent",
                        "BOOLEAN NOT NULL DEFAULT FALSE");
                ensureColumn(conn, "medical_records", "id_shared_doctors",
                        "INT NULL");
                ensureColumn(conn, "medical_records", "shared_at",
                        "DATETIME DEFAULT CURRENT_TIMESTAMP");
                ensureColumn(conn, "medical_records", "prescription_diagnosis",
                        "TEXT NULL");
                ensureColumn(conn, "medical_records", "prescription_treatment",
                        "TEXT NULL");
                ensureColumn(conn, "medical_records", "prescription_instructions",
                        "TEXT NULL");
                ensureColumn(conn, "medical_records", "prescription_validity",
                        "VARCHAR(50) NULL");
                schemaChecked = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Impossible de verifier ou mettre a jour le schema medical_records.", ex);
            }
        }
    }

    public int insert(MedicalRecord record) throws SQLException {
        String sql = """
                INSERT INTO medical_records(
                    title, description, document_type, file_path, id_doctors, id_users,
                    share_consent, id_shared_doctors, prescription_diagnosis,
                    prescription_treatment, prescription_instructions, prescription_validity
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
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
            ps.setBoolean(7, record.isShareConsent());
            if (record.getSharedDoctorId() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, record.getSharedDoctorId());
            }
            ps.setString(9, record.getPrescriptionDiagnosis());
            ps.setString(10, record.getPrescriptionTreatment());
            ps.setString(11, record.getPrescriptionInstructions());
            ps.setString(12, record.getPrescriptionValidity());

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
        String sql = """
                SELECT mr.id_medical_records, mr.title, mr.description, mr.document_type, mr.file_path,
                       mr.date, mr.id_doctors, mr.id_users, mr.share_consent, mr.id_shared_doctors,
                       mr.prescription_diagnosis, mr.prescription_treatment, mr.prescription_instructions, mr.prescription_validity,
                       p.username AS patient_username, p.prenom AS patient_prenom,
                       sd.username AS shared_doctor_username, sd.prenom AS shared_doctor_prenom
                FROM medical_records mr
                JOIN users p ON p.id_users = mr.id_users
                LEFT JOIN doctors dsd ON dsd.id_doctors = mr.id_shared_doctors
                LEFT JOIN users sd ON sd.id_users = dsd.id_users
                WHERE mr.id_users = ?
                ORDER BY mr.date DESC, mr.id_medical_records DESC
                """;
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
                    r.setShareConsent(rs.getBoolean("share_consent"));
                    int sharedDoctorId = rs.getInt("id_shared_doctors");
                    if (rs.wasNull()) {
                        r.setSharedDoctorId(null);
                    } else {
                        r.setSharedDoctorId(sharedDoctorId);
                    }
                    r.setPatientName(buildDisplayName(rs.getString("patient_prenom"), rs.getString("patient_username")));
                    r.setSharedDoctorName(buildDisplayName(rs.getString("shared_doctor_prenom"), rs.getString("shared_doctor_username")));
                    r.setPrescriptionDiagnosis(rs.getString("prescription_diagnosis"));
                    r.setPrescriptionTreatment(rs.getString("prescription_treatment"));
                    r.setPrescriptionInstructions(rs.getString("prescription_instructions"));
                    r.setPrescriptionValidity(rs.getString("prescription_validity"));
                    results.add(r);
                }
            }
        }
        return results;
    }

    public List<MedicalRecord> findSharedByDoctorUserId(int userId) throws SQLException {
        String sql = """
                SELECT mr.id_medical_records, mr.title, mr.description, mr.document_type, mr.file_path,
                       mr.date, mr.id_doctors, mr.id_users, mr.share_consent, mr.id_shared_doctors,
                       mr.prescription_diagnosis, mr.prescription_treatment, mr.prescription_instructions, mr.prescription_validity,
                       p.username AS patient_username, p.prenom AS patient_prenom,
                       sd.username AS shared_doctor_username, sd.prenom AS shared_doctor_prenom
                FROM medical_records mr
                JOIN doctors shared_doctor ON shared_doctor.id_doctors = mr.id_shared_doctors
                JOIN users sd ON sd.id_users = shared_doctor.id_users
                JOIN users p ON p.id_users = mr.id_users
                WHERE sd.id_users = ?
                ORDER BY mr.date DESC, mr.id_medical_records DESC
                """;
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
                    r.setShareConsent(rs.getBoolean("share_consent"));
                    int sharedDoctorId = rs.getInt("id_shared_doctors");
                    if (rs.wasNull()) {
                        r.setSharedDoctorId(null);
                    } else {
                        r.setSharedDoctorId(sharedDoctorId);
                    }
                    r.setPatientName(buildDisplayName(rs.getString("patient_prenom"), rs.getString("patient_username")));
                    r.setSharedDoctorName(buildDisplayName(rs.getString("shared_doctor_prenom"), rs.getString("shared_doctor_username")));
                    r.setPrescriptionDiagnosis(rs.getString("prescription_diagnosis"));
                    r.setPrescriptionTreatment(rs.getString("prescription_treatment"));
                    r.setPrescriptionInstructions(rs.getString("prescription_instructions"));
                    r.setPrescriptionValidity(rs.getString("prescription_validity"));
                    results.add(r);
                }
            }
        }
        return results;
    }

    public void update(MedicalRecord record) throws SQLException {
        String sql = """
                UPDATE medical_records
                SET title = ?,
                    description = ?,
                    document_type = ?,
                    file_path = ?,
                    id_doctors = ?,
                    id_users = ?,
                    share_consent = ?,
                    id_shared_doctors = ?,
                    prescription_diagnosis = ?,
                    prescription_treatment = ?,
                    prescription_instructions = ?,
                    prescription_validity = ?
                WHERE id_medical_records = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
            ps.setBoolean(7, record.isShareConsent());
            if (record.getSharedDoctorId() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, record.getSharedDoctorId());
            }
            ps.setString(9, record.getPrescriptionDiagnosis());
            ps.setString(10, record.getPrescriptionTreatment());
            ps.setString(11, record.getPrescriptionInstructions());
            ps.setString(12, record.getPrescriptionValidity());
            ps.setInt(13, record.getId());
            ps.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM medical_records WHERE id_medical_records = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void ensureColumn(Connection conn, String tableName, String columnName, String columnDefinition) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            if (columns.next()) {
                return;
            }
        }

        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private String buildDisplayName(String prenom, String username) {
        String first = prenom == null ? "" : prenom.trim();
        String second = username == null ? "" : username.trim();
        if (first.isEmpty() && second.isEmpty()) {
            return "";
        }
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + " " + second;
    }
}
