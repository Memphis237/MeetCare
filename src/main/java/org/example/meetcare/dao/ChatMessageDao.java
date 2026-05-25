package org.example.meetcare.dao;

import org.example.meetcare.exception.DatabaseException;
import org.example.meetcare.model.ChatMessage;
import org.example.meetcare.model.ChatMessageAttachment;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDao {
    public ChatMessageDao() {
        ensureSchema();
    }

    public ChatMessage saveMessage(int conversationId, int senderId, String content, String messageType) {
        String sql = """
                INSERT INTO messages (content, message_type, is_read, sent_at, id_conversations, id_users)
                VALUES (?, ?, FALSE, CURRENT_TIMESTAMP, ?, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, content);
            statement.setString(2, messageType == null ? "TEXT" : messageType);
            statement.setInt(3, conversationId);
            statement.setInt(4, senderId);
            statement.executeUpdate();

            int generatedId = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

            ChatMessage message = new ChatMessage();
            message.setIdMessage(generatedId);
            message.setConversationId(conversationId);
            message.setSenderId(senderId);
            message.setContent(content);
            message.setMessageType(messageType == null ? "TEXT" : messageType);
            message.setRead(false);
            message.setSentAt(LocalDateTime.now());
            return message;
        } catch (SQLException e) {
            throw new DatabaseException("Impossible d'enregistrer le message.", e);
        }
    }

    public void saveAttachments(int messageId, List<ChatMessageAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO attachments (file_name, file_path, file_type, file_size, date, id_messages)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            for (ChatMessageAttachment attachment : attachments) {
                statement.setString(1, attachment.getFileName());
                statement.setString(2, attachment.getFilePath());
                statement.setString(3, attachment.getFileType());
                statement.setLong(4, attachment.getFileSize());
                statement.setInt(5, messageId);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible d'enregistrer les pieces jointes.", e);
        }
    }

    public List<ChatMessage> findMessages(int conversationId) {
        String sql = """
                SELECT id_messages, content, message_type, is_read, sent_at, id_conversations, id_users
                FROM messages
                WHERE id_conversations = ?
                ORDER BY sent_at ASC, id_messages ASC
                """;

        List<ChatMessage> messages = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ChatMessage message = mapMessage(resultSet);
                    message.getAttachments().addAll(findAttachments(message.getIdMessage()));
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de charger les messages.", e);
        }
        return messages;
    }

    public void markConversationAsRead(int conversationId, int readerUserId) {
        String sql = """
                UPDATE messages
                SET is_read = TRUE
                WHERE id_conversations = ?
                  AND id_users <> ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, readerUserId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de marquer les messages comme lus.", e);
        }
    }

    public int countUnreadTotalForUser(int userId) {
        String sql = """
                SELECT COUNT(*) AS unread_count
                FROM messages m
                JOIN conversation_participants cp ON cp.id_conversations = m.id_conversations
                WHERE cp.id_users = ?
                  AND m.id_users <> ?
                  AND m.is_read = FALSE
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("unread_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de compter les messages non lus.", e);
        }
        return 0;
    }

    public int countUnreadForConversation(int conversationId, int userId) {
        String sql = """
                SELECT COUNT(*) AS unread_count
                FROM messages
                WHERE id_conversations = ?
                  AND id_users <> ?
                  AND is_read = FALSE
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("unread_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de compter les messages de la conversation.", e);
        }
        return 0;
    }

    public boolean isLastMessageFromCurrentUserRead(int conversationId, int currentUserId) {
        String sql = """
                SELECT is_read
                FROM messages
                WHERE id_conversations = ?
                  AND id_users = ?
                ORDER BY sent_at DESC, id_messages DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, currentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean("is_read");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de lire le statut du dernier message.", e);
        }
        return false;
    }

    public List<ChatMessageAttachment> findAttachments(int messageId) {
        String sql = """
                SELECT id_attachments, file_name, file_path, file_type, file_size, date, id_messages
                FROM attachments
                WHERE id_messages = ?
                ORDER BY date ASC, id_attachments ASC
                """;

        List<ChatMessageAttachment> attachments = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ChatMessageAttachment attachment = new ChatMessageAttachment();
                    attachment.setIdAttachment(resultSet.getInt("id_attachments"));
                    attachment.setFileName(resultSet.getString("file_name"));
                    attachment.setFilePath(resultSet.getString("file_path"));
                    attachment.setFileType(resultSet.getString("file_type"));
                    attachment.setFileSize(resultSet.getLong("file_size"));
                    attachment.setMessageId(resultSet.getInt("id_messages"));

                    Timestamp timestamp = resultSet.getTimestamp("date");
                    if (timestamp != null) {
                        attachment.setCreatedAt(timestamp.toLocalDateTime());
                    }
                    attachments.add(attachment);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de charger les pieces jointes.", e);
        }
        return attachments;
    }

    public void ensureSchema() {
        String createMessages = """
                CREATE TABLE IF NOT EXISTS messages (
                    id_messages INT AUTO_INCREMENT PRIMARY KEY,
                    content TEXT,
                    message_type VARCHAR(50),
                    is_read BOOLEAN DEFAULT FALSE,
                    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    id_conversations INT,
                    id_users INT,
                    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                )
                """;
        String createAttachments = """
                CREATE TABLE IF NOT EXISTS attachments (
                    id_attachments INT AUTO_INCREMENT PRIMARY KEY,
                    file_name VARCHAR(255),
                    file_path VARCHAR(255),
                    file_type VARCHAR(100),
                    file_size BIGINT,
                    date DATETIME DEFAULT CURRENT_TIMESTAMP,
                    id_messages INT,
                    FOREIGN KEY (id_messages) REFERENCES messages(id_messages) ON DELETE CASCADE
                )
                """;

        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(createMessages);
            statement.executeUpdate(createAttachments);
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de preparer le schema des messages.", e);
        }
    }

    private ChatMessage mapMessage(ResultSet resultSet) throws SQLException {
        ChatMessage message = new ChatMessage();
        message.setIdMessage(resultSet.getInt("id_messages"));
        message.setContent(resultSet.getString("content"));
        message.setMessageType(resultSet.getString("message_type"));
        message.setRead(resultSet.getBoolean("is_read"));
        message.setConversationId(resultSet.getInt("id_conversations"));
        message.setSenderId(resultSet.getInt("id_users"));

        Timestamp timestamp = resultSet.getTimestamp("sent_at");
        if (timestamp != null) {
            message.setSentAt(timestamp.toLocalDateTime());
        }
        return message;
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
