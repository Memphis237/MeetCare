package org.example.meetcare.dao;

import org.example.meetcare.model.ChatbotMessage;
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
import java.util.Optional;

public class ChatbotMessageDao {
    public int ensureConversationForUser(int userId) {
        return findLatestConversationIdByUserId(userId)
                .orElseGet(this::createConversation);
    }

    public Optional<Integer> findLatestConversationIdByUserId(int userId) {
        String sql = """
                SELECT id_conversations
                FROM chatbot_messages
                WHERE id_users = ?
                ORDER BY date DESC, id_chatbot_messages DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("id_conversations"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de recuperer la conversation IA.", e);
        }
    }

    public int createConversation() {
        String sql = "INSERT INTO conversations (created_at) VALUES (CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new IllegalStateException("Impossible de creer la conversation IA.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer la conversation IA.", e);
        }
    }

    public List<ChatbotMessage> findMessagesByConversationAndUser(int conversationId, int userId) {
        String sql = """
                SELECT id_chatbot_messages, question, response, model, date, id_conversations, id_users
                FROM chatbot_messages
                WHERE id_conversations = ? AND id_users = ?
                ORDER BY date ASC, id_chatbot_messages ASC
                """;

        List<ChatbotMessage> messages = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger l'historique du chatbot.", e);
        }
        return messages;
    }

    public ChatbotMessage saveExchange(int conversationId, int userId, String question, String response, String model) {
        String sql = """
                INSERT INTO chatbot_messages (question, response, model, id_conversations, id_users)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, question);
            statement.setString(2, response);
            statement.setString(3, model);
            statement.setInt(4, conversationId);
            statement.setInt(5, userId);

            statement.executeUpdate();

            int generatedId = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

            ChatbotMessage message = new ChatbotMessage();
            message.setIdChatbotMessages(generatedId);
            message.setQuestion(question);
            message.setResponse(response);
            message.setModel(model);
            message.setConversationId(conversationId);
            message.setUserId(userId);
            message.setDate(LocalDateTime.now());
            return message;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'enregistrer le message IA.", e);
        }
    }

    private ChatbotMessage mapMessage(ResultSet resultSet) throws SQLException {
        ChatbotMessage message = new ChatbotMessage();
        message.setIdChatbotMessages(resultSet.getInt("id_chatbot_messages"));
        message.setQuestion(resultSet.getString("question"));
        message.setResponse(resultSet.getString("response"));
        message.setModel(resultSet.getString("model"));
        message.setConversationId(resultSet.getInt("id_conversations"));
        message.setUserId(resultSet.getInt("id_users"));

        Timestamp timestamp = resultSet.getTimestamp("date");
        if (timestamp != null) {
            message.setDate(timestamp.toLocalDateTime());
        }
        return message;
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
