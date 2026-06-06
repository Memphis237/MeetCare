package org.example.meetcare.dao;

import org.example.meetcare.model.Users;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessagingDao {
    public MessagingDao() {
        ensureSchema();
    }

    public List<ChatContact> getPatientContacts(int patientUserId) {
        Map<Integer, ChatContact> contacts = new LinkedHashMap<>();
        String sql = """
                SELECT DISTINCT du.id_users AS contact_id,
                       du.username AS contact_name,
                       du.is_online,
                       d.speciality,
                       du.last_seen
                FROM appointments a
                JOIN doctors d ON d.id_doctors = a.id_doctors
                JOIN users du ON du.id_users = d.id_users
                WHERE a.id_users = ?
                ORDER BY du.username
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int contactId = resultSet.getInt("contact_id");
                    contacts.put(contactId, new ChatContact(
                            contactId,
                            formatDoctorName(resultSet.getString("contact_name")),
                            safe(resultSet.getString("speciality"), "Medecin"),
                            resultSet.getBoolean("is_online"),
                            buildInitials(resultSet.getString("contact_name")),
                            readTimestamp(resultSet, "last_seen")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }

        return sortContacts(contacts.values());
    }

    public List<ChatContact> getDoctorContacts(int doctorUserId) {
        Map<Integer, ChatContact> contacts = new LinkedHashMap<>();
        String sql = """
                SELECT DISTINCT pu.id_users AS contact_id,
                       pu.prenom,
                       pu.username,
                       pu.is_online,
                       pu.last_seen
                FROM appointments a
                JOIN doctors d ON d.id_doctors = a.id_doctors
                JOIN users pu ON pu.id_users = a.id_users
                WHERE d.id_users = ?
                ORDER BY pu.prenom, pu.username
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int contactId = resultSet.getInt("contact_id");
                    String displayName = buildPersonName(resultSet.getString("prenom"), resultSet.getString("username"));
                    contacts.put(contactId, new ChatContact(
                            contactId,
                            displayName,
                            "Patient",
                            resultSet.getBoolean("is_online"),
                            buildInitials(displayName),
                            readTimestamp(resultSet, "last_seen")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }

        return sortContacts(contacts.values());
    }

    public int ensureDirectConversation(int currentUserId, int otherUserId) {
        String findSql = """
                SELECT cp1.id_conversations
                FROM conversation_participants cp1
                JOIN conversation_participants cp2
                  ON cp1.id_conversations = cp2.id_conversations
                GROUP BY cp1.id_conversations
                HAVING SUM(CASE WHEN cp1.id_users = ? THEN 1 ELSE 0 END) > 0
                   AND SUM(CASE WHEN cp2.id_users = ? THEN 1 ELSE 0 END) > 0
                   AND COUNT(*) = 2
                LIMIT 1
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(findSql)) {
            statement.setInt(1, currentUserId);
            statement.setInt(2, otherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException ignored) {
        }

        int conversationId = createConversation();
        addParticipant(conversationId, currentUserId);
        addParticipant(conversationId, otherUserId);
        return conversationId;
    }

    public List<DirectMessage> getConversationMessages(int conversationId) {
        String sql = """
                SELECT m.id_messages,
                       m.content,
                       m.message_type,
                       m.is_read,
                       m.sent_at,
                       m.id_users,
                       u.prenom,
                       u.username
                FROM messages m
                JOIN users u ON u.id_users = m.id_users
                WHERE m.id_conversations = ?
                ORDER BY m.sent_at ASC, m.id_messages ASC
                """;

        List<DirectMessage> messages = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new DirectMessage(
                            resultSet.getInt("id_messages"),
                            resultSet.getInt("id_users"),
                            buildPersonName(resultSet.getString("prenom"), resultSet.getString("username")),
                            safe(resultSet.getString("content"), ""),
                            safe(resultSet.getString("message_type"), "TEXT"),
                            resultSet.getBoolean("is_read"),
                            readTimestamp(resultSet, "sent_at")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return messages;
    }

    public DirectMessage sendMessage(int conversationId, int senderUserId, String content) {
        String sql = """
                INSERT INTO messages (content, message_type, is_read, sent_at, id_conversations, id_users)
                VALUES (?, 'TEXT', FALSE, CURRENT_TIMESTAMP, ?, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, content);
            statement.setInt(2, conversationId);
            statement.setInt(3, senderUserId);
            statement.executeUpdate();

            int id = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }

            Users sender = new UsersDao().findById(senderUserId).orElse(null);
            String senderName = sender == null ? "Utilisateur" : buildPersonName(sender.getPrenom(), sender.getUsername());

            return new DirectMessage(id, senderUserId, senderName, content, "TEXT", false, LocalDateTime.now());
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'envoyer le message.", e);
        }
    }

    public void markConversationRead(int conversationId, int currentUserId) {
        String sql = """
                UPDATE messages
                SET is_read = TRUE
                WHERE id_conversations = ?
                  AND id_users <> ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, currentUserId);
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private int createConversation() {
        String sql = "INSERT INTO conversations (created_at) VALUES (CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer la conversation.", e);
        }
        throw new IllegalStateException("Impossible de creer la conversation.");
    }

    private void addParticipant(int conversationId, int userId) {
        String sql = """
                INSERT INTO conversation_participants (id_conversations, id_users)
                VALUES (?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter le participant.", e);
        }
    }

    private void ensureSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS conversation_participants (
                    id_conversations INT NOT NULL,
                    id_users INT NOT NULL,
                    PRIMARY KEY (id_conversations, id_users),
                    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                )
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException ignored) {
        }
    }

    private List<ChatContact> sortContacts(Iterable<ChatContact> contacts) {
        List<ChatContact> result = new ArrayList<>();
        contacts.forEach(result::add);
        result.sort(Comparator.comparing(ChatContact::online).reversed()
                .thenComparing(ChatContact::displayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private String buildPersonName(String prenom, String username) {
        String first = safe(prenom, "").trim();
        String last = safe(username, "").trim();
        if (first.isEmpty() && last.isEmpty()) {
            return "Utilisateur";
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private String formatDoctorName(String username) {
        String display = safe(username, "Medecin");
        return display.startsWith("Dr.") ? display : "Dr. " + display;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "??";
        }
        String sanitized = displayName.replace("Dr. ", "").trim();
        String[] parts = sanitized.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.FRENCH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.FRENCH);
    }

    private LocalDateTime readTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public record ChatContact(
            int userId,
            String displayName,
            String subtitle,
            boolean online,
            String initials,
            LocalDateTime lastSeen
    ) {}

    public record DirectMessage(
            int id,
            int senderUserId,
            String senderName,
            String content,
            String messageType,
            boolean read,
            LocalDateTime sentAt
    ) {}
}
