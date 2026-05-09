package org.example.meetcare.dao;

import org.example.meetcare.exception.DatabaseException;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

public class UsersDao {
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la vérification de l'email.", e);
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la vérification du nom d'utilisateur.", e);
        }
    }

    public int insert(Users user) {
        String sql = """
                INSERT INTO users (username, email, password, phone, avatar, role, city, code_postal, is_online, last_seen)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getPhone());
            statement.setString(5, user.getAvatar());
            statement.setString(6, user.getRole() == null ? Role.USER.name() : user.getRole().name());
            statement.setString(7, user.getCity());
            statement.setString(8, user.getCodePostal());
            statement.setBoolean(9, user.isOnline());
            if (user.getLastSeen() != null) {
                statement.setTimestamp(10, Timestamp.valueOf(user.getLastSeen()));
            } else {
                statement.setTimestamp(10, null);
            }

            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new DatabaseException("L'inscription a échoué.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de l'inscription.", e);
        }
    }

    public Optional<Users> findByIdentifier(String identifier) {
        String sql = """
                SELECT id_users, username, email, password, phone, avatar, role, city, code_postal,
                       is_online, last_seen, `date`
                FROM users
                WHERE username = ? OR email = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la récupération de l'utilisateur.", e);
        }
    }

    public void updateOnlineStatus(int userId, boolean online) {
        String sql = "UPDATE users SET is_online = ?, last_seen = ? WHERE id_users = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setBoolean(1, online);
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(3, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la mise à jour du statut en ligne.", e);
        }
    }

    private Users mapUser(ResultSet resultSet) throws SQLException {
        Users user = new Users();
        user.setIdUsers(resultSet.getInt("id_users"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setPhone(resultSet.getString("phone"));
        user.setAvatar(resultSet.getString("avatar"));
        user.setRole(Role.fromDatabaseValue(resultSet.getString("role")));
        user.setCity(resultSet.getString("city"));
        user.setCodePostal(resultSet.getString("code_postal"));
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

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
