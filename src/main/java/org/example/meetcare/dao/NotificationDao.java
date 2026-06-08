package org.example.meetcare.dao;

import org.example.meetcare.exception.DatabaseException;
import org.example.meetcare.model.NotificationEntry;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NotificationDao {
    private static volatile boolean schemaChecked;

    public NotificationDao() {
        ensureSchema();
    }

    public int createNotification(int userId, String title, String message, String type) {
        String sql = """
                INSERT INTO notifications (title, message, type, is_read, date, id_users)
                VALUES (?, ?, ?, FALSE, CURRENT_TIMESTAMP, ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, safeText(title, "Notification"));
            statement.setString(2, safeText(message, ""));
            statement.setString(3, safeText(type, "SYSTEM"));
            statement.setInt(4, userId);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de creer la notification.", e);
        }
    }

    public void createNotificationForUsers(Collection<Integer> userIds, String title, String message, String type) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Integer userId : userIds) {
            if (userId == null || userId <= 0) {
                continue;
            }
            createNotification(userId, title, message, type);
        }
    }

    public List<NotificationEntry> findNotificationsForUser(int userId, boolean unreadOnly) {
        String sql = """
                SELECT id_notifications, title, message, type, is_read, date, id_users
                FROM notifications
                WHERE id_users = ?
                %s
                ORDER BY date DESC, id_notifications DESC
                """.formatted(unreadOnly ? "AND is_read = FALSE" : "");

        List<NotificationEntry> notifications = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(map(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de charger les notifications.", e);
        }
        return notifications;
    }

    public int countUnreadForUser(int userId) {
        String sql = """
                SELECT COUNT(*) AS unread_count
                FROM notifications
                WHERE id_users = ?
                  AND is_read = FALSE
                """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("unread_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de compter les notifications non lues.", e);
        }
        return 0;
    }

    public void markAsRead(int notificationId, int userId) {
        String sql = """
                UPDATE notifications
                SET is_read = TRUE
                WHERE id_notifications = ?
                  AND id_users = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de marquer la notification comme lue.", e);
        }
    }

    public void markAllAsRead(int userId) {
        String sql = """
                UPDATE notifications
                SET is_read = TRUE
                WHERE id_users = ?
                  AND is_read = FALSE
                """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de marquer les notifications comme lues.", e);
        }
    }

    public void ensureSchema() {
        if (schemaChecked) {
            return;
        }

        synchronized (NotificationDao.class) {
            if (schemaChecked) {
                return;
            }

            String sql = """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id_notifications INT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255),
                        message TEXT,
                        type VARCHAR(50),
                        is_read BOOLEAN DEFAULT FALSE,
                        date DATETIME DEFAULT CURRENT_TIMESTAMP,
                        id_users INT,
                        FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                    )
                    """;

            try (Connection conn = getConnection();
                 Statement statement = conn.createStatement()) {
                statement.executeUpdate(sql);
                schemaChecked = true;
            } catch (SQLException e) {
                throw new DatabaseException("Impossible de preparer le schema des notifications.", e);
            }
        }
    }

    private NotificationEntry map(ResultSet resultSet) throws SQLException {
        NotificationEntry notification = new NotificationEntry();
        notification.setIdNotification(resultSet.getInt("id_notifications"));
        notification.setTitle(resultSet.getString("title"));
        notification.setMessage(resultSet.getString("message"));
        notification.setType(resultSet.getString("type"));
        notification.setRead(resultSet.getBoolean("is_read"));
        notification.setUserId(resultSet.getInt("id_users"));

        Timestamp timestamp = resultSet.getTimestamp("date");
        if (timestamp != null) {
            notification.setCreatedAt(timestamp.toLocalDateTime());
        }
        return notification;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
