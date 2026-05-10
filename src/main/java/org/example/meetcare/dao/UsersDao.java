package org.example.meetcare.dao;

import org.example.meetcare.exception.DatabaseException;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

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
                INSERT INTO users (username, prenom, email, password, phone, avatar, role, address, city, code_postal, country, is_online, last_seen)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPrenom());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPassword());
            statement.setString(5, user.getPhone());
            statement.setString(6, user.getAvatar());
            statement.setString(7, user.getRole() == null ? Role.USER.name() : user.getRole().name());
            statement.setString(8, user.getAddress());
            statement.setString(9, user.getCity());
            statement.setString(10, user.getCodePostal());
            statement.setString(11, user.getCountry());
            statement.setBoolean(12, user.isOnline());
            if (user.getLastSeen() != null) {
                statement.setTimestamp(13, Timestamp.valueOf(user.getLastSeen()));
            } else {
                statement.setTimestamp(13, null);
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
                SELECT id_users, username, prenom, email, password, phone, avatar, role, address, city,
                       code_postal, country, is_online, last_seen, `date`
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

    public Optional<Users> findById(int userId) {
        String sql = """
                SELECT id_users, username, prenom, email, password, phone, avatar, role, address, city,
                       code_postal, country, is_online, last_seen, `date`
                FROM users
                WHERE id_users = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
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

    public void updateProfile(Users user) {
        String sql = """
                UPDATE users
                SET prenom = ?, username = ?, email = ?, phone = ?, address = ?, city = ?, code_postal = ?, country = ?
                WHERE id_users = ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, user.getPrenom());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPhone());
            statement.setString(5, user.getAddress());
            statement.setString(6, user.getCity());
            statement.setString(7, user.getCodePostal());
            statement.setString(8, user.getCountry());
            statement.setInt(9, user.getIdUsers());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la mise à jour du profil.", e);
        }
    }

    public void updatePassword(int userId, String hashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE id_users = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, hashedPassword);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la mise à jour du mot de passe.", e);
        }
    }

    public void updateAvatar(int userId, String avatarPath) {
        String sql = "UPDATE users SET avatar = ? WHERE id_users = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, avatarPath);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la mise à jour de la photo de profil.", e);
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

        if (isBlank(user.getAvatar())) {
            String restoredAvatar = restoreAvatarFromDisk(user.getIdUsers());
            if (!isBlank(restoredAvatar)) {
                user.setAvatar(restoredAvatar);
                updateAvatar(user.getIdUsers(), restoredAvatar);
            }
        }

        return user;
    }

    private String restoreAvatarFromDisk(int userId) {
        Path avatarsDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
        if (!Files.isDirectory(avatarsDir)) {
            return null;
        }

        String prefix = "user-" + userId + "-";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(avatarsDir, prefix + "*")) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
