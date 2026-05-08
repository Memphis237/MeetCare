package org.example.meetcare.service;

import org.example.meetcare.dao.UsersDao;
import java.sql.SQLException;

public class RegisterService {

    private UsersDao usersDao;

    public RegisterService() {
        this(new UsersDao());
    }
    
    public RegisterService(UsersDao usersDao){
        this.usersDao = usersDao;
    }
 
    public String register(String name, String surname, String email, String phone, String password, String confirm_password) {
        if (name == null || name.trim().isEmpty()|| surname == null || surname.trim().isEmpty()||email==null||email.trim().isEmpty()||phone==null||phone.trim().isEmpty()||password==null||password.trim().isEmpty()||confirm_password==null||confirm_password.trim().isEmpty()) {
            return "All fields are required";
        }
        if(!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")){
            return "Invalid email format";
        }
        if(!phone.matches("^\\+?[0-9]{10,15}$")){
            return "Invalid phone number format";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if(!password.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).+$")){
            return "Password must contain at least one letter and one number";
        }
        if (!confirm_password.equals(password)) {
            return "Passwords do not match";
        }
        try {
            usersDao.registerUser(name, surname, email, phone, confirm_password);
            return "Registration successful";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Registration failed: " + e.getMessage();
        }
    }
    
}
