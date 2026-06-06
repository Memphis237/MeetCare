package org.example.meetcare.dao;

import org.example.meetcare.exception.DatabaseException;
import org.example.meetcare.model.ChatConversation;
import org.example.meetcare.model.Role;
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
import java.util.List;
import java.util.Optional;

public class ChatConversationDao {
    private final UsersDao usersDao = new UsersDao();

    public ChatConversationDao() {
        ensureSchema();
    }

    public int ensureConversationBetween(int userId, int otherUserId) {
        Optional<Integer> existingConversation = findConversationBetween(userId, otherUserId);
        if (existingConversation.isPresent()) {
            return existingConversation.get();
        }

        String insertConversation = "INSERT INTO conversations (created_at) VALUES (CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = getConnection().prepareStatement(insertConversation, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();

            int conversationId;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("Impossible de creer la conversation.");
                }
                conversationId = keys.getInt(1);
            }

            insertParticipant(conversationId, userId);
            insertParticipant(conversationId, otherUserId);
            return conversationId;
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de creer la conversation.", e);
        }
    }

    public Optional<Integer> findConversationBetween(int userId, int otherUserId) {
        String sql = """
                SELECT c.id_conversations
                FROM conversations c
                JOIN conversation_participants p1 ON p1.id_conversations = c.id_conversations
                JOIN conversation_participants p2 ON p2.id_conversations = c.id_conversations
                WHERE p1.id_users = ?
                  AND p2.id_users = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, otherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("id_conversations"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de rechercher la conversation.", e);
        }
        return Optional.empty();
    }

    public List<ChatConversation> findConversationsForUser(int userId, Role targetRole, String query) {
        List<ChatConversation> conversations = new ArrayList<>();
        List<ConversationPair> pairs = loadConversationPairs(userId);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        for (ConversationPair pair : pairs) {
            Users otherUser = pair.otherUser();
            if (otherUser == null) {
                continue;
            }

            if (targetRole != null && otherUser.getRole() != targetRole) {
                continue;
            }

            if (!normalizedQuery.isBlank() && !matchesSearch(otherUser, pair.lastMessagePreview(), normalizedQuery)) {
                continue;
            }

            ChatConversation conversation = new ChatConversation();
            conversation.setIdConversation(pair.conversationId());
            conversation.setOtherUser(otherUser);
            conversation.setLastMessagePreview(pair.lastMessagePreview());
            conversation.setLastMessageAt(pair.lastMessageAt());
            conversation.setUnreadCount(pair.unreadCount());
            conversation.setTyping(isOtherUserTyping(pair.conversationId(), userId));
            conversation.setTypingAt(getTypingTimestamp(pair.conversationId(), userId));
            conversations.add(conversation);
        }

        conversations.sort(Comparator.comparing(ChatConversation::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return conversations;
    }

    public List<Users> findSelectableUsersForChat(int currentUserId, Role targetRole, String query) {
        List<Users> users;
        if (targetRole == null) {
            users = query == null || query.isBlank()
                    ? usersDao.findAllUsers()
                    : usersDao.searchAll(query);
        } else {
            users = query == null || query.isBlank()
                    ? usersDao.findAllByRole(targetRole)
                    : usersDao.searchByRole(targetRole, query);
        }

        users.removeIf(user -> user.getIdUsers() == currentUserId);
        users.sort(Comparator.comparing(this::buildDisplayName, String.CASE_INSENSITIVE_ORDER));
        return users;
    }

    public void markTyping(int conversationId, int userId, boolean typing) {
        String sql = """
                INSERT INTO conversation_typing (id_conversations, id_users, is_typing, typing_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE is_typing = VALUES(is_typing), typing_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, userId);
            statement.setBoolean(3, typing);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Impossible d'enregistrer l'etat de saisie.", e);
        }
    }

    public boolean isOtherUserTyping(int conversationId, int currentUserId) {
        String sql = """
                SELECT is_typing, typing_at
                FROM conversation_typing
                WHERE id_conversations = ?
                  AND id_users <> ?
                ORDER BY typing_at DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, currentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean typing = resultSet.getBoolean("is_typing");
                    Timestamp typingAt = resultSet.getTimestamp("typing_at");
                    if (typing && typingAt != null) {
                        return typingAt.toLocalDateTime().isAfter(LocalDateTime.now().minusSeconds(6));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de lire l'etat de saisie.", e);
        }
        return false;
    }

    public LocalDateTime getTypingTimestamp(int conversationId, int currentUserId) {
        String sql = """
                SELECT typing_at
                FROM conversation_typing
                WHERE id_conversations = ?
                  AND id_users <> ?
                ORDER BY typing_at DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, currentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp("typing_at");
                    if (timestamp != null) {
                        return timestamp.toLocalDateTime();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de lire l'horodatage de saisie.", e);
        }
        return null;
    }

    public int countUnreadMessages(int userId) {
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

    public void ensureSchema() {
        String createParticipants = """
                CREATE TABLE IF NOT EXISTS conversation_participants (
                    id_conversation_participants INT AUTO_INCREMENT PRIMARY KEY,
                    id_conversations INT NOT NULL,
                    id_users INT NOT NULL,
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_conversation_user (id_conversations, id_users),
                    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                )
                """;
        String createTyping = """
                CREATE TABLE IF NOT EXISTS conversation_typing (
                    id_conversation_typing INT AUTO_INCREMENT PRIMARY KEY,
                    id_conversations INT NOT NULL,
                    id_users INT NOT NULL,
                    is_typing BOOLEAN DEFAULT FALSE,
                    typing_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_typing_conversation_user (id_conversations, id_users),
                    FOREIGN KEY (id_conversations) REFERENCES conversations(id_conversations) ON DELETE CASCADE,
                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                )
                """;

        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(createParticipants);
            statement.executeUpdate(createTyping);
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de preparer le schema de messagerie.", e);
        }
    }

    private List<ConversationPair> loadConversationPairs(int userId) {
        String sql = """
                SELECT c.id_conversations,
                       other_user.id_users AS other_user_id,
                       other_user.username,
                       other_user.prenom,
                       other_user.email,
                       other_user.password,
                       other_user.phone,
                       other_user.avatar,
                       other_user.role,
                       other_user.address,
                       other_user.city,
                       other_user.code_postal,
                       other_user.country,
                       other_user.is_online,
                       other_user.last_seen,
                       other_user.`date`,
                       (
                           SELECT m.content
                           FROM messages m
                           WHERE m.id_conversations = c.id_conversations
                           ORDER BY m.sent_at DESC, m.id_messages DESC
                           LIMIT 1
                       ) AS last_message,
                       (
                           SELECT m.sent_at
                           FROM messages m
                           WHERE m.id_conversations = c.id_conversations
                           ORDER BY m.sent_at DESC, m.id_messages DESC
                           LIMIT 1
                       ) AS last_message_at,
                       (
                           SELECT COUNT(*)
                           FROM messages m
                           WHERE m.id_conversations = c.id_conversations
                             AND m.id_users <> ?
                             AND m.is_read = FALSE
                       ) AS unread_count
                FROM conversations c
                JOIN conversation_participants me ON me.id_conversations = c.id_conversations
                JOIN conversation_participants other ON other.id_conversations = c.id_conversations
                JOIN users other_user ON other_user.id_users = other.id_users
                WHERE me.id_users = ?
                  AND other.id_users <> ?
                ORDER BY last_message_at DESC, c.id_conversations DESC
                """;

        List<ConversationPair> pairs = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Users otherUser = mapUser(resultSet);
                    ConversationPair pair = new ConversationPair(
                            resultSet.getInt("id_conversations"),
                            otherUser,
                            resultSet.getString("last_message"),
                            toLocalDateTime(resultSet.getTimestamp("last_message_at")),
                            resultSet.getInt("unread_count")
                    );
                    pairs.add(pair);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de charger les conversations.", e);
        }
        return pairs;
    }

    private boolean matchesSearch(Users user, String lastMessage, String query) {
        return contains(user.getPrenom(), query)
                || contains(user.getUsername(), query)
                || contains(user.getEmail(), query)
                || contains(lastMessage, query);
    }

    private boolean contains(String value, String query) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase().contains(query);
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (prenom.isEmpty() && username.isEmpty()) {
            return "Utilisateur";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private void insertParticipant(int conversationId, int userId) throws SQLException {
        String sql = "INSERT INTO conversation_participants (id_conversations, id_users) VALUES (?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, conversationId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private Users mapUser(ResultSet resultSet) throws SQLException {
        Users user = new Users();
        user.setIdUsers(resultSet.getInt("other_user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPrenom(resultSet.getString("prenom"));
        user.setEmail(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setPhone(resultSet.getString("phone"));
        user.setAvatar(resultSet.getString("avatar"));
        user.setRole(Role.fromDatabaseValue(resultSet.getString("role")));
        user.setAddress(resultSet.getString("address"));
        user.setCity(resultSet.getString("city"));
        user.setCodePostal(resultSet.getString("code_postal"));
        user.setCountry(resultSet.getString("country"));
        user.setOnline(resultSet.getBoolean("is_online"));

        Timestamp lastSeen = resultSet.getTimestamp("last_seen");
        if (lastSeen != null) {
            user.setLastSeen(lastSeen.toLocalDateTime());
        }
        Timestamp createdAt = resultSet.getTimestamp("date");
        if (createdAt != null) {
            user.setDate(createdAt.toLocalDateTime());
        }
        return user;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    private record ConversationPair(
            int conversationId,
            Users otherUser,
            String lastMessagePreview,
            LocalDateTime lastMessageAt,
            int unreadCount
    ) {
    }
}
