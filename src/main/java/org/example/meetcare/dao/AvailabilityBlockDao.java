package org.example.meetcare.dao;

import org.example.meetcare.model.AvailabilityBlock;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityBlockDao {
    private static volatile boolean schemaChecked;

    public AvailabilityBlockDao() {
        ensureSchema();
    }

    public List<AvailabilityBlock> findByDoctorUserId(int doctorUserId) {
        String sql = """
                SELECT id_blocks, id_users, block_date, start_time, end_time, whole_day, reason
                FROM doctor_availability_blocks
                WHERE id_users = ?
                ORDER BY block_date DESC, whole_day DESC, start_time ASC, id_blocks DESC
                """;

        List<AvailabilityBlock> blocks = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    blocks.add(mapBlock(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de charger les disponibilites bloquees.", ex);
        }
        return blocks;
    }

    public int addBlock(int doctorUserId, LocalDate date, String startTime, String endTime, boolean wholeDay, String reason) {
        String sql = """
                INSERT INTO doctor_availability_blocks (id_users, block_date, start_time, end_time, whole_day, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, doctorUserId);
            stmt.setDate(2, Date.valueOf(date));
            if (wholeDay) {
                stmt.setNull(3, java.sql.Types.VARCHAR);
                stmt.setNull(4, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(3, normalizeTime(startTime));
                stmt.setString(4, normalizeTime(endTime));
            }
            stmt.setBoolean(5, wholeDay);
            stmt.setString(6, normalizeReason(reason));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible d'enregistrer le blocage de disponibilite.", ex);
        }
    }

    public void deleteById(int blockId, int doctorUserId) {
        String sql = "DELETE FROM doctor_availability_blocks WHERE id_blocks = ? AND id_users = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, blockId);
            stmt.setInt(2, doctorUserId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de supprimer le blocage de disponibilite.", ex);
        }
    }

    public boolean isBlocked(int doctorUserId, LocalDate date, String time) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM doctor_availability_blocks
                WHERE id_users = ?
                  AND block_date = ?
                  AND (
                    whole_day = TRUE
                    OR (
                        start_time IS NOT NULL
                        AND end_time IS NOT NULL
                        AND ? >= start_time
                        AND ? < end_time
                    )
                  )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorUserId);
            stmt.setDate(2, Date.valueOf(date));
            java.sql.Time sqlTime = java.sql.Time.valueOf(normalizeTime(time) + ":00");
            stmt.setTime(3, sqlTime);
            stmt.setTime(4, sqlTime);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de verifier le blocage de disponibilite.", ex);
        }
    }

    public boolean isWholeDayBlocked(int doctorUserId, LocalDate date) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM doctor_availability_blocks
                WHERE id_users = ?
                  AND block_date = ?
                  AND whole_day = TRUE
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorUserId);
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Impossible de verifier le blocage journalier.", ex);
        }
    }

    public List<String> findBlockedTimesForDate(int doctorUserId, LocalDate date) {
        List<String> blockedTimes = new ArrayList<>();
        for (AvailabilityBlock block : findByDoctorUserId(doctorUserId)) {
            if (block.getBlockDate() == null || !block.getBlockDate().equals(date)) {
                continue;
            }
            if (block.isWholeDay()) {
                return List.of("ALL");
            }
            if (block.getStartTime() != null && block.getEndTime() != null) {
                blockedTimes.add(block.getStartTime() + "|" + block.getEndTime());
            }
        }
        return blockedTimes;
    }

    private void ensureSchema() {
        if (schemaChecked) {
            return;
        }

        synchronized (AvailabilityBlockDao.class) {
            if (schemaChecked) {
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                if (!tableExists(conn, "doctor_availability_blocks")) {
                    try (Statement statement = conn.createStatement()) {
                        statement.executeUpdate("""
                                CREATE TABLE doctor_availability_blocks (
                                    id_blocks INT AUTO_INCREMENT PRIMARY KEY,
                                    id_users INT NOT NULL,
                                    block_date DATE NOT NULL,
                                    start_time TIME NULL,
                                    end_time TIME NULL,
                                    whole_day BOOLEAN DEFAULT FALSE,
                                    reason VARCHAR(255),
                                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                                )
                                """);
                    }
                }
                schemaChecked = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Impossible de verifier le schema des disponibilites.", ex);
            }
        }
    }

    private AvailabilityBlock mapBlock(ResultSet rs) throws SQLException {
        AvailabilityBlock block = new AvailabilityBlock();
        block.setId(rs.getInt("id_blocks"));
        block.setDoctorUserId(rs.getInt("id_users"));
        Date blockDate = rs.getDate("block_date");
        if (blockDate != null) {
            block.setBlockDate(blockDate.toLocalDate());
        }
        block.setStartTime(rs.getString("start_time"));
        block.setEndTime(rs.getString("end_time"));
        block.setWholeDay(rs.getBoolean("whole_day"));
        block.setReason(rs.getString("reason"));
        return block;
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        }
    }

    private String normalizeTime(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeReason(String value) {
        return value == null || value.isBlank() ? "Blocage de disponibilite" : value.trim();
    }
}
