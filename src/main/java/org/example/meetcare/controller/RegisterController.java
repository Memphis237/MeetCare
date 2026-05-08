package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;        // ✅ ajouté
import javafx.scene.Scene;             // ✅ ajouté
import javafx.stage.Stage;             // ✅ ajouté
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.meetcare.service.RegisterService;
import org.example.meetcare.dao.UsersDao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegisterController {

    @FXML
    private TextField nameField;
    @FXML
    private TextField surnameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;

    private final RegisterService registerService = new RegisterService(new UsersDao());

    @FXML
    private void handleRegister() {
        // Réinitialiser le message d'erreur
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: red;");
        errorLabel.setText("");

        String name = nameField.getText();
        String surname = surnameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String confirm_password = confirmPasswordField.getText();

        String result = registerService.register(
                name, surname, email, phone, password, confirm_password
        );

        if (result.equals("Registration successful")) {
            // Afficher en vert en cas de succès
            errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: green;");
            errorLabel.setText("Compte créé avec succès !");

            // Optionnel : vider les champs après inscription
            clearFields();
        }
        
        else  {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText(result);
        }
    }

    @FXML
   private void handleBack() {
     try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
     } catch (Exception e) {
        e.printStackTrace();
     }
   }
    @FXML
    private void clearFields() {
        nameField.clear();
        surnameField.clear();
        emailField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}
