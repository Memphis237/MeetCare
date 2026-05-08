package org.example.meetcare.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.example.meetcare.utils.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

public class UsersDao {

    private final String sqlReg =
        "INSERT INTO users(username, email, phone, password) " +
        "VALUES(?, ?, ?, ?)";

    public void registerUser(String name, String surname, String email,
                             String phone, String confirm_password) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlReg)) {

            String hashPass = BCrypt.hashpw(confirm_password, BCrypt.gensalt());
            String username = name.trim() + " " + surname.trim();
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, hashPass); 

            statement.executeUpdate();
            System.out.println("Enregistrement reussi");
        }
        catch (SQLException e) {
             e.printStackTrace();          // ✅ e. et () obligatoires
             throw e;                      // ✅ remonte l'erreur au service
        }
    }
}