package org.example.meetcare.dao;

import org.example.meetcare.model.Doctor;
import org.example.meetcare.model.DoctorAdminEntry;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import java.util.Optional;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {
    private static volatile boolean schemaChecked;

    public DoctorDao() {
        ensureSchema();
    }

    private void ensureSchema() {
        if (schemaChecked) {
            return;
        }

        synchronized (DoctorDao.class) {
            if (schemaChecked) {
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                ensureColumn(conn, "doctors", "speciality", "VARCHAR(100) NULL");
                ensureColumn(conn, "doctors", "address", "VARCHAR(255) NULL");
                ensureColumn(conn, "doctors", "experience", "INT NULL");
                ensureColumn(conn, "doctors", "consultation_price", "DECIMAL(10,2) NULL");
                ensureColumn(conn, "doctors", "biography", "TEXT NULL");
                ensureColumn(conn, "doctors", "sector", "VARCHAR(20) NULL");
                ensureColumn(conn, "doctors", "accepts_vitale", "BOOLEAN NOT NULL DEFAULT FALSE");
                ensureColumn(conn, "doctors", "teleconsultation", "BOOLEAN NOT NULL DEFAULT FALSE");
                ensureColumn(conn, "doctors", "public_profile_completed", "BOOLEAN NOT NULL DEFAULT FALSE");
                schemaChecked = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Impossible de verifier le schema des medecins.", ex);
            }
        }
    }

    public Optional<Integer> findDoctorIdByUserId(int userId) {
        ensureSchema();
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

    public Optional<Integer> findUserIdByDoctorId(int doctorId) {
        ensureSchema();
        String sql = "SELECT id_users FROM doctors WHERE id_doctors = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id_users"));
                }
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public int ensureDoctorIdForUser(int userId) {
        ensureSchema();
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
        ensureSchema();
        ensureDoctorProfileForUser(userId);

        String sql = """
            SELECT COALESCE(d.id_doctors, 0) AS id_doctors,
                   u.username,
                   u.email,
                   u.city,
                   COALESCE(d.speciality, 'Medecine generale') AS speciality,
                   COALESCE(d.address, u.address, '') AS address,
                   COALESCE(d.sector, 'Secteur 1') AS sector,
                   COALESCE(d.consultation_price, 0) AS consultation_price,
                   COALESCE(d.accepts_vitale, FALSE) AS accepts_vitale,
                   COALESCE(d.teleconsultation, FALSE) AS teleconsultation,
                   d.experience,
                   d.biography,
                   COALESCE(d.public_profile_completed, FALSE) AS public_profile_completed
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
                    rs.getBoolean("accepts_vitale"),
                    rs.getBoolean("teleconsultation"),
                    getInteger(rs, "experience"),
                    rs.getString("biography"),
                    rs.getBoolean("public_profile_completed")
            ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public Optional<DoctorAdminEntry> findPublicProfileByUserId(int userId) {
        ensureSchema();
        ensureDoctorProfileForUser(userId);
        return findPublicProfile("u.id_users = ?", userId);
    }

    public Optional<DoctorAdminEntry> findPublicProfileByDoctorId(int doctorId) {
        ensureSchema();
        return findPublicProfile("d.id_doctors = ?", doctorId);
    }

    public void updateProfile(int userId, String speciality, String address, String sector,
                              String consultationPrice, String experience, String biography,
                              boolean acceptsVitale, boolean teleconsultation) {
        ensureSchema();
        ensureDoctorProfileForUser(userId);

        String sql = """
                UPDATE doctors
                SET speciality = ?,
                    address = ?,
                    sector = ?,
                    consultation_price = ?,
                    experience = ?,
                    biography = ?,
                    accepts_vitale = ?,
                    teleconsultation = ?
                WHERE id_users = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeValue(speciality, "Medecine generale"));
            stmt.setString(2, normalizeValue(address, ""));
            stmt.setString(3, normalizeValue(sector, "Secteur 1"));
            setOptionalDecimal(stmt, 4, consultationPrice);
            setOptionalInteger(stmt, 5, experience);
            stmt.setString(6, normalizeValue(biography, ""));
            stmt.setBoolean(7, acceptsVitale);
            stmt.setBoolean(8, teleconsultation);
            stmt.setInt(9, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour le profil medecin.", e);
        }
    }

    private Optional<DoctorAdminEntry> findPublicProfile(String whereClause, int value) {
        ensureSchema();
        String sql = """
                SELECT u.id_users,
                       COALESCE(d.id_doctors, 0) AS id_doctors,
                       u.prenom,
                       u.username,
                       u.email,
                       u.phone,
                       u.city,
                       u.address,
                       COALESCE(d.speciality, 'Medecine generale') AS speciality,
                       COALESCE(d.address, u.address, '') AS doctor_address,
                       d.experience,
                       d.consultation_price,
                       d.biography,
                       d.sector,
                       COALESCE(d.teleconsultation, FALSE) AS teleconsultation,
                       COALESCE(d.accepts_vitale, FALSE) AS accepts_vitale,
                       COALESCE(d.public_profile_completed, FALSE) AS public_profile_completed,
                       COALESCE(u.account_status, 'PENDING') AS account_status
                FROM users u
                LEFT JOIN doctors d ON d.id_users = u.id_users
                WHERE """ + whereClause + """
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPublicProfile(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le profil public du medecin.", e);
        }
    }

    public List<Doctor> searchDoctors(String keyword, String city) {
        List<Doctor> doctors = new ArrayList<>();

        ensureSchema();
        ensureDoctorProfilesForMedicalUsers();

        String sql = """
            SELECT COALESCE(d.id_doctors, 0) AS id_doctors,
                   u.username,
                   u.email,
                   u.city,
                   COALESCE(d.speciality, 'Medecine generale') AS speciality,
                   COALESCE(d.address, u.address, '') AS address,
                   COALESCE(d.sector, 'Secteur 1') AS sector,
                   COALESCE(d.consultation_price, 0) AS consultation_price,
                   COALESCE(d.accepts_vitale, FALSE) AS accepts_vitale,
                   COALESCE(d.teleconsultation, FALSE) AS teleconsultation,
                   d.experience,
                   d.biography,
                   COALESCE(d.public_profile_completed, FALSE) AS public_profile_completed
            FROM users u
            LEFT JOIN doctors d ON d.id_users = u.id_users
            WHERE u.role = 'MEDECIN'
              AND u.account_status = 'VERIFIED'
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
                        rs.getBoolean("accepts_vitale"),
                        rs.getBoolean("teleconsultation"),
                        getInteger(rs, "experience"),
                        rs.getString("biography"),
                        rs.getBoolean("public_profile_completed")
                );

                doctors.add(doctor);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return doctors;
    }

    private void ensureDoctorProfilesForMedicalUsers() {
        ensureSchema();
        String sql = """
                SELECT u.id_users
                FROM users u
                LEFT JOIN doctors d ON d.id_users = u.id_users
                WHERE u.role = 'MEDECIN'
                  AND d.id_doctors IS NULL
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ensureDoctorIdForUser(rs.getInt("id_users"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de synchroniser les dossiers medecins.", e);
        }
    }

    private void ensureDoctorProfileForUser(int userId) {
        ensureSchema();
        String sql = """
                SELECT 1
                FROM doctors
                WHERE id_users = ?
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de verifier le dossier medecin.", e);
        }

        ensureDoctorIdForUser(userId);
    }

    private void setOptionalDecimal(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(index, Types.DECIMAL);
            return;
        }

        try {
            stmt.setDouble(index, Double.parseDouble(value.trim().replace(',', '.')));
        } catch (NumberFormatException ex) {
            stmt.setNull(index, Types.DECIMAL);
        }
    }

    private void setOptionalInteger(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        try {
            stmt.setInt(index, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    private String normalizeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public List<DoctorAdminEntry> findAllAdminEntries() {
        return findAdminEntries("");
    }

    public List<DoctorAdminEntry> findAdminEntries(String query) {
        ensureDoctorProfilesForMedicalUsers();

        String sql = """
                SELECT u.id_users,
                       COALESCE(d.id_doctors, 0) AS id_doctors,
                       u.prenom,
                       u.username,
                       u.email,
                       u.phone,
                       u.city,
                       u.address,
                       COALESCE(d.speciality, 'Médecine générale') AS speciality,
                       COALESCE(d.address, u.address, '') AS doctor_address,
                       d.experience,
                       d.consultation_price,
                       d.biography,
                       d.sector,
                       COALESCE(d.teleconsultation, FALSE) AS teleconsultation,
                       COALESCE(d.accepts_vitale, FALSE) AS accepts_vitale,
                       COALESCE(d.public_profile_completed, FALSE) AS public_profile_completed,
                       COALESCE(u.account_status, 'PENDING') AS account_status
                FROM users u
                LEFT JOIN doctors d ON d.id_users = u.id_users
                WHERE u.role = 'MEDECIN'
                  AND (
                        ? = ''
                        OR LOWER(u.username) LIKE LOWER(?)
                        OR LOWER(u.prenom) LIKE LOWER(?)
                        OR LOWER(u.email) LIKE LOWER(?)
                        OR LOWER(COALESCE(d.speciality, '')) LIKE LOWER(?)
                        OR LOWER(COALESCE(u.city, '')) LIKE LOWER(?)
                  )
                ORDER BY
                    CASE COALESCE(u.account_status, 'PENDING')
                        WHEN 'PENDING' THEN 0
                        WHEN 'VERIFIED' THEN 1
                        WHEN 'REJECTED' THEN 2
                        WHEN 'SUSPENDED' THEN 3
                        ELSE 4
                    END,
                    u.prenom,
                    u.username
                """;

        String normalizedQuery = query == null ? "" : query.trim();
        String like = "%" + normalizedQuery + "%";

        List<DoctorAdminEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedQuery);
            stmt.setString(2, like);
            stmt.setString(3, like);
            stmt.setString(4, like);
            stmt.setString(5, like);
            stmt.setString(6, like);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new DoctorAdminEntry(
                            rs.getInt("id_users"),
                            rs.getInt("id_doctors"),
                            rs.getString("prenom"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("city"),
                            rs.getString("address"),
                            rs.getString("speciality"),
                            rs.getString("doctor_address"),
                            getInteger(rs, "experience"),
                            getDouble(rs, "consultation_price"),
                            rs.getString("biography"),
                            rs.getString("sector"),
                            rs.getBoolean("teleconsultation"),
                            rs.getBoolean("accepts_vitale"),
                            rs.getBoolean("public_profile_completed"),
                            AccountStatus.fromDatabaseValue(rs.getString("account_status"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les medecins.", e);
        }
        return entries;
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
        ensureSchema();
        String sql = "UPDATE doctors SET public_profile_completed = TRUE WHERE id_doctors = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour le profil public.", e);
        }
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private DoctorAdminEntry mapPublicProfile(ResultSet rs) throws SQLException {
        return new DoctorAdminEntry(
                rs.getInt("id_users"),
                rs.getInt("id_doctors"),
                rs.getString("prenom"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getString("speciality"),
                rs.getString("doctor_address"),
                getInteger(rs, "experience"),
                getDouble(rs, "consultation_price"),
                rs.getString("biography"),
                rs.getString("sector"),
                rs.getBoolean("teleconsultation"),
                rs.getBoolean("accepts_vitale"),
                rs.getBoolean("public_profile_completed"),
                AccountStatus.fromDatabaseValue(rs.getString("account_status"))
        );
    }

    private Double getDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
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
}
